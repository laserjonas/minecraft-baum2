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
            long maxExp = progress.getExperienceForNextLevel();

            player.setExperienceLevel(customLevel);
            player.totalExperience = (int) (customLevel * 100 + currentExp);
        }
    }
}
