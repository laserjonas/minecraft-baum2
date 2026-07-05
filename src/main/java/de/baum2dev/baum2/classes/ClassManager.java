package de.baum2dev.baum2.classes;

import com.mojang.serialization.Codec;
import java.util.Optional;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentSyncPredicate;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import de.baum2dev.baum2.Baum2;

/**
 * Owns class and sub-specialization selection: persistence, and applying/removing the passive
 * attribute bonuses.
 *
 * Persistent {@link EntityAttributeModifier}s are restored from entity NBT automatically (same
 * as vanilla equipment/potion modifiers) — no per-tick or per-join reapplication is needed the
 * way the XP bar sync needs it. But by the time {@code JOIN} fires for a returning player, their
 * previous session's modifier is already back, so {@link #applyModifier} always
 * removes-then-adds rather than just adding, or a rejoin would throw / silently duplicate.
 */
public final class ClassManager {

    public enum SelectResult {
        SUCCESS,
        ON_COOLDOWN,
        WRONG_CLASS
    }

    public record SelectAttempt(SelectResult result, long remainingCooldownTicks) {
    }

    public static final AttachmentType<PlayerClass> SELECTED_CLASS = AttachmentRegistry.create(
        Identifier.of("baum2", "selected_class"),
        builder -> builder
            .persistent(Codec.STRING.xmap(PlayerClass::valueOf, PlayerClass::name))
            .copyOnDeath()
            .syncWith(PacketCodecs.STRING.xmap(PlayerClass::valueOf, PlayerClass::name), AttachmentSyncPredicate.targetOnly())
    );

    public static final AttachmentType<ClassSubspec> SELECTED_SUBSPEC = AttachmentRegistry.create(
        Identifier.of("baum2", "selected_subspec"),
        builder -> builder
            .persistent(Codec.STRING.xmap(ClassSubspec::valueOf, ClassSubspec::name))
            .copyOnDeath()
            .syncWith(PacketCodecs.STRING.xmap(ClassSubspec::valueOf, ClassSubspec::name), AttachmentSyncPredicate.targetOnly())
    );

    public static void registerEvents() {
        // Forces RespecCooldownManager's AttachmentType fields to class-init before any player
        // can join - see that class's own bootstrap() doc and the Attachment API force-load
        // gotcha in HANDOFF.md.
        RespecCooldownManager.bootstrap();

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) ->
            resyncOnJoin(handler.getPlayer()));
    }

    public static Optional<PlayerClass> getSelectedClass(ServerPlayerEntity player) {
        return Optional.ofNullable(player.getAttached(SELECTED_CLASS));
    }

    public static Optional<ClassSubspec> getSelectedSubspec(ServerPlayerEntity player) {
        return Optional.ofNullable(player.getAttached(SELECTED_SUBSPEC));
    }

    /**
     * Free on a player's first-ever class pick (mirrors the existing "no class selected yet"
     * check via {@link Optional#isEmpty()}); a respec while a class is already selected is
     * gated by {@link RespecCooldownManager} so players can't swap classes on a whim.
     */
    public static SelectAttempt selectClass(ServerPlayerEntity player, PlayerClass newClass) {
        Optional<PlayerClass> currentClass = getSelectedClass(player);

        if (currentClass.isPresent() && RespecCooldownManager.isClassOnCooldown(player)) {
            return new SelectAttempt(SelectResult.ON_COOLDOWN, RespecCooldownManager.remainingClassCooldownTicks(player));
        }

        currentClass.ifPresent(oldClass -> removeClassBonus(player, oldClass));
        player.setAttached(SELECTED_CLASS, newClass);
        applyModifier(player, ClassRegistry.get(newClass));
        RespecCooldownManager.recordClassSelect(player);

        // A sub-spec only makes sense for the class it belongs to - clear it (and its bonus)
        // whenever the base class changes, rather than leaving a stale, now-invalid selection.
        // This also leaves the sub-spec respec track untouched-but-empty, so the very next
        // sub-spec pick for the new class is free too (see selectSubspec's own doc).
        getSelectedSubspec(player).ifPresent(oldSubspec -> {
            removeSubspecBonus(player, oldSubspec);
            player.removeAttached(SELECTED_SUBSPEC);
        });

        return new SelectAttempt(SelectResult.SUCCESS, 0);
    }

    /**
     * Returns {@link SelectResult#WRONG_CLASS} if {@code newSubspec} doesn't belong to the
     * player's currently selected class. Free on the first sub-spec pick for the current class
     * (including right after a class change, since {@link #selectClass} already cleared the old
     * sub-spec) - a respec while a sub-spec is already selected is gated by
     * {@link RespecCooldownManager}.
     */
    public static SelectAttempt selectSubspec(ServerPlayerEntity player, ClassSubspec newSubspec) {
        Optional<PlayerClass> currentClass = getSelectedClass(player);
        if (currentClass.isEmpty() || newSubspec.parentClass() != currentClass.get()) {
            return new SelectAttempt(SelectResult.WRONG_CLASS, 0);
        }

        Optional<ClassSubspec> currentSubspec = getSelectedSubspec(player);
        if (currentSubspec.isPresent() && RespecCooldownManager.isSubspecOnCooldown(player)) {
            return new SelectAttempt(SelectResult.ON_COOLDOWN, RespecCooldownManager.remainingSubspecCooldownTicks(player));
        }

        currentSubspec.ifPresent(oldSubspec -> removeSubspecBonus(player, oldSubspec));
        player.setAttached(SELECTED_SUBSPEC, newSubspec);
        applyModifier(player, SubspecRegistry.get(newSubspec));
        RespecCooldownManager.recordSubspecSelect(player);
        return new SelectAttempt(SelectResult.SUCCESS, 0);
    }

    private static void resyncOnJoin(ServerPlayerEntity player) {
        getSelectedClass(player).ifPresent(playerClass -> applyModifier(player, ClassRegistry.get(playerClass)));
        getSelectedSubspec(player).ifPresent(subspec -> applyModifier(player, SubspecRegistry.get(subspec)));
    }

    private static void applyModifier(ServerPlayerEntity player, ClassDefinition definition) {
        applyModifier(player, definition.bonusAttribute(), definition.bonusModifierId(), definition.toModifier());
    }

    private static void applyModifier(ServerPlayerEntity player, SubspecDefinition definition) {
        applyModifier(player, definition.bonusAttribute(), definition.bonusModifierId(), definition.toModifier());
    }

    private static void applyModifier(
        ServerPlayerEntity player,
        RegistryEntry<EntityAttribute> attribute,
        Identifier modifierId,
        EntityAttributeModifier modifier
    ) {
        EntityAttributeInstance instance = player.getAttributeInstance(attribute);
        if (instance == null) {
            Baum2.LOGGER.warn("Player {} has no attribute instance for {}", player.getName().getString(), attribute);
            return;
        }

        instance.removeModifier(modifierId);
        instance.addPersistentModifier(modifier);
    }

    private static void removeClassBonus(ServerPlayerEntity player, PlayerClass playerClass) {
        removeModifier(player, ClassRegistry.get(playerClass).bonusAttribute(), ClassRegistry.get(playerClass).bonusModifierId());
    }

    private static void removeSubspecBonus(ServerPlayerEntity player, ClassSubspec subspec) {
        removeModifier(player, SubspecRegistry.get(subspec).bonusAttribute(), SubspecRegistry.get(subspec).bonusModifierId());
    }

    private static void removeModifier(ServerPlayerEntity player, RegistryEntry<EntityAttribute> attribute, Identifier modifierId) {
        EntityAttributeInstance instance = player.getAttributeInstance(attribute);
        if (instance == null) {
            return;
        }

        instance.removeModifier(modifierId);
    }

    private ClassManager() {
    }
}
