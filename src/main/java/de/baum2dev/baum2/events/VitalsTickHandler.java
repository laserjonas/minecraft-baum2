package de.baum2dev.baum2.events;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import de.baum2dev.baum2.networking.Baum2Networking;
import de.baum2dev.baum2.progression.PlayerLevelSystem;
import de.baum2dev.baum2.progression.PlayerProgressData;
import de.baum2dev.baum2.progression.VitalsCurve;
import de.baum2dev.baum2.progression.VitalsManager;

/**
 * Keeps Life/Life Regen (real max-health attribute, scaled by Endurance) and Mana (custom,
 * persisted, scaled by level) in sync with player attributes/level, and pushes Mana and raw
 * attribute values to the client every tick for HUD/Stats-screen display (Life itself needs
 * no custom sync - it's real vanilla health, already synced by the game itself; derived
 * combat stats need no sync either - the client computes them from the synced raw
 * attributes via the shared VitalsCurve formulas).
 */
public class VitalsTickHandler {
    private static final int REGEN_INTERVAL_TICKS = 20;
    private static int tickCounter = 0;

    public static void registerEvents() {
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayerEntity player = handler.getPlayer();
            PlayerProgressData progress = PlayerLevelSystem.getPlayerProgress(player);
            VitalsManager.applyMaxLife(player, progress.getEndurance());
            VitalsManager.applyBaseAttack(player, progress.getStrength());
            VitalsManager.applyAttackSpeed(player, progress.getDexterity());
        });

        ServerTickEvents.END_SERVER_TICK.register(VitalsTickHandler::onServerTick);
    }

    private static void onServerTick(MinecraftServer server) {
        tickCounter++;
        boolean regenTick = tickCounter % REGEN_INTERVAL_TICKS == 0;

        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            PlayerProgressData progress = PlayerLevelSystem.getPlayerProgress(player);
            int level = progress.getLevel();
            int endurance = progress.getEndurance();

            VitalsManager.applyMaxLife(player, endurance);
            VitalsManager.clampMana(progress, level);
            if (regenTick) {
                VitalsManager.regenMana(progress, level);
                VitalsManager.regenLife(player, endurance);
            }
            PlayerLevelSystem.savePlayerProgress(player, progress);

            Baum2Networking.syncPlayerMana(player, progress.getMana(), VitalsCurve.getMaxMana(level));
            Baum2Networking.syncPlayerAttributes(player, progress);
        }
    }
}
