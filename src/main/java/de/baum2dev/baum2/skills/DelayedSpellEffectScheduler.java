package de.baum2dev.baum2.skills;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;

/**
 * Runs a spell-effect callback a fixed number of ticks after it's scheduled - needed for
 * Sturmklinge's "Klingenwirbel hits twice, 5 ticks apart" fork. Mirrors
 * {@code combat.BurnDamageManager}'s minimal tick-tracked-list shape rather than pulling in a
 * general task-scheduling library, since nothing else needs delayed one-shot effects yet.
 */
public final class DelayedSpellEffectScheduler {
    private static final List<PendingEffect> PENDING = new ArrayList<>();

    public static void registerEvents() {
        ServerTickEvents.END_SERVER_TICK.register(DelayedSpellEffectScheduler::onServerTick);
    }

    /** Runs {@code effect} on {@code player} after {@code delayTicks} server ticks, if the player is still online and alive. */
    static void schedule(ServerPlayerEntity player, int delayTicks, Consumer<ServerPlayerEntity> effect) {
        PENDING.add(new PendingEffect(player.getUuid(), delayTicks, effect));
    }

    private static void onServerTick(MinecraftServer server) {
        if (PENDING.isEmpty()) {
            return;
        }
        List<PendingEffect> due = new ArrayList<>();
        var iterator = PENDING.iterator();
        while (iterator.hasNext()) {
            PendingEffect pending = iterator.next();
            pending.ticksRemaining--;
            if (pending.ticksRemaining <= 0) {
                due.add(pending);
                iterator.remove();
            }
        }
        for (PendingEffect pending : due) {
            ServerPlayerEntity player = server.getPlayerManager().getPlayer(pending.playerId);
            if (player != null && player.isAlive()) {
                pending.effect.accept(player);
            }
        }
    }

    private static class PendingEffect {
        final java.util.UUID playerId;
        int ticksRemaining;
        final Consumer<ServerPlayerEntity> effect;

        PendingEffect(java.util.UUID playerId, int ticksRemaining, Consumer<ServerPlayerEntity> effect) {
            this.playerId = playerId;
            this.ticksRemaining = ticksRemaining;
            this.effect = effect;
        }
    }

    private DelayedSpellEffectScheduler() {
    }
}
