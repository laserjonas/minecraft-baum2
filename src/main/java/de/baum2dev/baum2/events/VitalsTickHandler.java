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
 * Keeps Life (real max-health attribute) and Mana (custom, persisted) in sync with
 * player level, and pushes Mana to the client every tick for HUD display (Life needs no
 * custom sync - it's real vanilla health, already synced by the game itself).
 */
public class VitalsTickHandler {
    private static final int MANA_REGEN_INTERVAL_TICKS = 20;
    private static int tickCounter = 0;

    public static void registerEvents() {
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayerEntity player = handler.getPlayer();
            PlayerProgressData progress = PlayerLevelSystem.getPlayerProgress(player);
            VitalsManager.applyMaxLife(player, progress.getLevel());
            // Base Damage/Magic Damage are flat, not level-scaled - unlike Mana, nothing
            // changes them per tick, so a single sync on join is enough.
            Baum2Networking.syncPlayerCombatStats(player, progress.getBaseDamage(), progress.getBaseMagicDamage());
        });

        ServerTickEvents.END_SERVER_TICK.register(VitalsTickHandler::onServerTick);
    }

    private static void onServerTick(MinecraftServer server) {
        tickCounter++;
        boolean regenTick = tickCounter % MANA_REGEN_INTERVAL_TICKS == 0;

        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            PlayerProgressData progress = PlayerLevelSystem.getPlayerProgress(player);
            int level = progress.getLevel();

            VitalsManager.applyMaxLife(player, level);
            VitalsManager.clampMana(progress, level);
            if (regenTick) {
                VitalsManager.regenMana(progress, level);
            }
            PlayerLevelSystem.savePlayerProgress(player, progress);

            Baum2Networking.syncPlayerMana(player, progress.getMana(), VitalsCurve.getMaxMana(level));
        }
    }
}
