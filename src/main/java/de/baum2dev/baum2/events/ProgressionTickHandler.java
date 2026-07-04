package de.baum2dev.baum2.events;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import de.baum2dev.baum2.progression.PlayerLevelSystem;
import de.baum2dev.baum2.progression.PlayerProgressData;

public class ProgressionTickHandler {

    public static void registerEvents() {
        ServerTickEvents.END_SERVER_TICK.register(ProgressionTickHandler::onServerTick);
    }

    private static void onServerTick(MinecraftServer server) {
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            PlayerProgressData progress = PlayerLevelSystem.getPlayerProgress(player);
            int customLevel = progress.getLevel();
            long currentExp = progress.getExperience();

            player.setExperienceLevel(customLevel);

            int totalXpForLevel = getExperienceForLevel(customLevel);
            int totalExp = totalXpForLevel + (int) currentExp;
            player.totalExperience = totalExp;
        }
    }

    private static int getExperienceForLevel(int level) {
        if (level <= 15) {
            return (int) (level * level + 6 * level);
        } else if (level <= 31) {
            return (int) (2.5 * level * level - 40.5 * level + 360);
        } else {
            return (int) (4.5 * level * level - 162.5 * level + 2220);
        }
    }
}
