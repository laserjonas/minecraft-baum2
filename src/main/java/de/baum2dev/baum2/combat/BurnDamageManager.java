package de.baum2dev.baum2.combat;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;

/**
 * A configurable burn damage-over-time tracker, independent of vanilla's own fixed-rate fire
 * damage - needed because Zombie Colossus's fire wave deals an exact "2 damage/sec for 5
 * seconds" per its spec, which vanilla's own fire-tick damage can't reproduce without a Mixin.
 * Kept minimal (a plain UUID->countdown map ticked once a second) rather than a bigger status-
 * effect framework, since nothing else needs a custom-rate burn yet.
 */
public class BurnDamageManager {
    private static final Map<UUID, BurnState> BURNING_PLAYERS = new HashMap<>();

    public static void registerEvents() {
        ServerTickEvents.END_SERVER_TICK.register(BurnDamageManager::onServerTick);
    }

    /** Starts (or refreshes) a burn on a player, dealing damagePerTick every intervalTicks
     *  until durationTicks have elapsed. */
    public static void applyBurn(PlayerEntity player, int durationTicks, float damagePerTick, int intervalTicks) {
        BURNING_PLAYERS.put(player.getUuid(), new BurnState(durationTicks, damagePerTick, intervalTicks));
    }

    private static void onServerTick(MinecraftServer server) {
        if (BURNING_PLAYERS.isEmpty()) {
            return;
        }
        Iterator<Map.Entry<UUID, BurnState>> iterator = BURNING_PLAYERS.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, BurnState> entry = iterator.next();
            BurnState state = entry.getValue();
            state.ticksRemaining--;

            ServerPlayerEntity player = server.getPlayerManager().getPlayer(entry.getKey());
            if (player == null || !player.isAlive()) {
                iterator.remove();
                continue;
            }
            // Respect the standard vanilla counterplay to any fire-flavored attack - this
            // tracker is independent of vanilla's own fire-tick damage (that's the whole reason
            // it exists, see class javadoc), so it needs its own explicit immunity check rather
            // than assuming the shared onFire() DamageSource already covers it (balance-reviewer
            // finding: it didn't).
            if (state.ticksRemaining % state.intervalTicks == 0
                    && !player.isFireImmune()
                    && !player.hasStatusEffect(StatusEffects.FIRE_RESISTANCE)) {
                player.damage(player.getEntityWorld(), player.getDamageSources().onFire(), state.damagePerTick);
            }
            if (state.ticksRemaining <= 0) {
                iterator.remove();
            }
        }
    }

    private static class BurnState {
        int ticksRemaining;
        final float damagePerTick;
        final int intervalTicks;

        BurnState(int durationTicks, float damagePerTick, int intervalTicks) {
            this.ticksRemaining = durationTicks;
            this.damagePerTick = damagePerTick;
            this.intervalTicks = intervalTicks;
        }
    }
}
