package de.baum2dev.baum2.skills;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;

/**
 * Tracks the last-cast server tick per (player, spell) pair. Deliberately a plain in-memory
 * map, not an Attachment - cooldowns are supposed to reset on relog/restart, unlike persistent
 * progression/class data, so the Attachment API's persistence would be fighting the tool. See
 * docs/fabric-modding.md "Combat / Skill effects" for the full reasoning.
 *
 * Keyed against {@link MinecraftServer#getTicks()} (total ticks since server start), not world
 * time - immune to /time set or sleep-skipped nights, unlike ServerWorld.getTime().
 */
public final class SkillCooldownManager {
    // Sentinel for "never cast" - deliberately NOT Long.MIN_VALUE: `server.getTicks() - last`
    // below would overflow signed 64-bit arithmetic and wrap around to a negative number,
    // making a spell that was never cast incorrectly report as already on cooldown. This value
    // still leaves ~4.6e18 ticks of headroom, far beyond any server's realistic uptime
    // (server.getTicks() is an int, capped at ~2^31), so the subtraction never overflows.
    private static final long NEVER_CAST = Long.MIN_VALUE / 2;

    private static final Map<UUID, Map<Spell, Long>> LAST_CAST_TICK = new HashMap<>();

    public static boolean isOnCooldown(ServerPlayerEntity player, Spell spell, MinecraftServer server) {
        long last = LAST_CAST_TICK.getOrDefault(player.getUuid(), Map.of()).getOrDefault(spell, NEVER_CAST);
        return server.getTicks() - last < spell.cooldownTicks();
    }

    public static long remainingCooldownTicks(ServerPlayerEntity player, Spell spell, MinecraftServer server) {
        long last = LAST_CAST_TICK.getOrDefault(player.getUuid(), Map.of()).getOrDefault(spell, NEVER_CAST);
        return Math.max(0, spell.cooldownTicks() - (server.getTicks() - last));
    }

    public static void recordCast(ServerPlayerEntity player, Spell spell, MinecraftServer server) {
        LAST_CAST_TICK.computeIfAbsent(player.getUuid(), id -> new HashMap<>())
            .put(spell, (long) server.getTicks());
    }

    private SkillCooldownManager() {
    }
}
