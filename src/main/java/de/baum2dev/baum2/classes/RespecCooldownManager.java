package de.baum2dev.baum2.classes;

import com.mojang.serialization.Codec;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

/**
 * Tracks class and sub-spec respec cooldowns, independently per player, on two separate tracks.
 * Backed by wall-clock ({@link System#currentTimeMillis()}) persistent Attachments, not
 * {@code skills.SkillCooldownManager}'s in-memory/{@code server.getTicks()} shape - a respec
 * gate (30 minutes / 5 minutes) is long enough that an in-memory map would let a player bypass
 * it just by restarting their singleplayer world (which restarts the integrated server's JVM,
 * wiping any in-memory tracker) - `balance-reviewer` finding. Persistent Attachments survive
 * that restart the same way {@link ClassManager}'s own {@code SELECTED_CLASS}/
 * {@code SELECTED_SUBSPEC} do. Wall-clock time (not {@code server.getTicks()}) is used
 * deliberately, since tick count itself resets to 0 on every server start and would be
 * meaningless once persisted across a restart.
 *
 * <p>"First pick is free" needs no state here - {@link ClassManager} only calls the
 * {@code isXOnCooldown} checks below when the player already has a class/sub-spec selected.
 */
final class RespecCooldownManager {
    // Same non-Long.MIN_VALUE sentinel reasoning as SkillCooldownManager.NEVER_CAST - avoids
    // signed 64-bit overflow in `System.currentTimeMillis() - last`.
    private static final long NEVER_SELECTED = Long.MIN_VALUE / 2;

    static final long CLASS_RESPEC_COOLDOWN_MILLIS = 30L * 60 * 1000; // 30 minutes
    static final long SUBSPEC_RESPEC_COOLDOWN_MILLIS = 5L * 60 * 1000; // 5 minutes

    private static final AttachmentType<Long> LAST_CLASS_SELECT_TIME = AttachmentRegistry.create(
        Identifier.of("baum2", "last_class_select_time"),
        builder -> builder.persistent(Codec.LONG).copyOnDeath()
    );

    private static final AttachmentType<Long> LAST_SUBSPEC_SELECT_TIME = AttachmentRegistry.create(
        Identifier.of("baum2", "last_subspec_select_time"),
        builder -> builder.persistent(Codec.LONG).copyOnDeath()
    );

    /** No-op call that forces this class (and its AttachmentType fields) to load - see ClassManager's own force-load gotcha in HANDOFF.md. */
    static void bootstrap() {
    }

    static boolean isClassOnCooldown(ServerPlayerEntity player) {
        long last = lastClassSelectMillis(player);
        return System.currentTimeMillis() - last < CLASS_RESPEC_COOLDOWN_MILLIS;
    }

    /** Returned in ticks (1 tick = 50ms) to match the rest of this codebase's cooldown-reporting convention (see SkillCooldownManager). */
    static long remainingClassCooldownTicks(ServerPlayerEntity player) {
        long last = lastClassSelectMillis(player);
        long remainingMillis = Math.max(0, CLASS_RESPEC_COOLDOWN_MILLIS - (System.currentTimeMillis() - last));
        return remainingMillis / 50;
    }

    static void recordClassSelect(ServerPlayerEntity player) {
        player.setAttached(LAST_CLASS_SELECT_TIME, System.currentTimeMillis());
    }

    static boolean isSubspecOnCooldown(ServerPlayerEntity player) {
        long last = lastSubspecSelectMillis(player);
        return System.currentTimeMillis() - last < SUBSPEC_RESPEC_COOLDOWN_MILLIS;
    }

    static long remainingSubspecCooldownTicks(ServerPlayerEntity player) {
        long last = lastSubspecSelectMillis(player);
        long remainingMillis = Math.max(0, SUBSPEC_RESPEC_COOLDOWN_MILLIS - (System.currentTimeMillis() - last));
        return remainingMillis / 50;
    }

    static void recordSubspecSelect(ServerPlayerEntity player) {
        player.setAttached(LAST_SUBSPEC_SELECT_TIME, System.currentTimeMillis());
    }

    private static long lastClassSelectMillis(ServerPlayerEntity player) {
        Long last = player.getAttached(LAST_CLASS_SELECT_TIME);
        return last != null ? last : NEVER_SELECTED;
    }

    private static long lastSubspecSelectMillis(ServerPlayerEntity player) {
        Long last = player.getAttached(LAST_SUBSPEC_SELECT_TIME);
        return last != null ? last : NEVER_SELECTED;
    }

    private RespecCooldownManager() {
    }
}
