package de.baum2dev.baum2.classes;

import com.mojang.serialization.Codec;
import java.util.Optional;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentSyncPredicate;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import de.baum2dev.baum2.Baum2;

/**
 * Owns class selection: persistence, and applying/removing the passive attribute bonus.
 *
 * Persistent {@link net.minecraft.entity.attribute.EntityAttributeModifier}s are restored from
 * entity NBT automatically (same as vanilla equipment/potion modifiers) — no per-tick or
 * per-join reapplication is needed the way the XP bar sync needs it. But by the time
 * {@code JOIN} fires for a returning player, their previous session's modifier is already
 * back, so {@link #applyBonus} always removes-then-adds rather than just adding, or a rejoin
 * would throw / silently duplicate.
 */
public final class ClassManager {
    public static final AttachmentType<PlayerClass> SELECTED_CLASS = AttachmentRegistry.create(
        Identifier.of("baum2", "selected_class"),
        builder -> builder
            .persistent(Codec.STRING.xmap(PlayerClass::valueOf, PlayerClass::name))
            .copyOnDeath()
            .syncWith(PacketCodecs.STRING.xmap(PlayerClass::valueOf, PlayerClass::name), AttachmentSyncPredicate.targetOnly())
    );

    public static void registerEvents() {
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) ->
            resyncOnJoin(handler.getPlayer()));
    }

    public static Optional<PlayerClass> getSelectedClass(ServerPlayerEntity player) {
        return Optional.ofNullable(player.getAttached(SELECTED_CLASS));
    }

    public static void selectClass(ServerPlayerEntity player, PlayerClass newClass) {
        getSelectedClass(player).ifPresent(oldClass -> removeBonus(player, ClassRegistry.get(oldClass)));
        player.setAttached(SELECTED_CLASS, newClass);
        applyBonus(player, ClassRegistry.get(newClass));
    }

    private static void resyncOnJoin(ServerPlayerEntity player) {
        getSelectedClass(player).ifPresent(playerClass -> applyBonus(player, ClassRegistry.get(playerClass)));
    }

    private static void applyBonus(ServerPlayerEntity player, ClassDefinition definition) {
        EntityAttributeInstance instance = player.getAttributeInstance(definition.bonusAttribute());
        if (instance == null) {
            Baum2.LOGGER.warn("Player {} has no attribute instance for {}", player.getName().getString(), definition.bonusAttribute());
            return;
        }

        instance.removeModifier(definition.bonusModifierId());
        instance.addPersistentModifier(definition.toModifier());
    }

    private static void removeBonus(ServerPlayerEntity player, ClassDefinition definition) {
        EntityAttributeInstance instance = player.getAttributeInstance(definition.bonusAttribute());
        if (instance == null) {
            return;
        }

        instance.removeModifier(definition.bonusModifierId());
    }

    private ClassManager() {
    }
}
