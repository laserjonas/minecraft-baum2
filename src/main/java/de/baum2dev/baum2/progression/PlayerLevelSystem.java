package de.baum2dev.baum2.progression;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.network.ServerPlayerEntity;

public class PlayerLevelSystem {
    private static final Map<UUID, PlayerProgressData> playerProgressMap = new HashMap<>();

    public static PlayerProgressData getPlayerProgress(ServerPlayerEntity player) {
        UUID uuid = player.getUuid();
        return playerProgressMap.getOrDefault(uuid, new PlayerProgressData());
    }

    public static void savePlayerProgress(ServerPlayerEntity player, PlayerProgressData progress) {
        UUID uuid = player.getUuid();
        playerProgressMap.put(uuid, progress);
    }

    public static void clearPlayerProgress(ServerPlayerEntity player) {
        UUID uuid = player.getUuid();
        playerProgressMap.remove(uuid);
    }

    public static void addExperience(ServerPlayerEntity player, long amount) {
        PlayerProgressData progress = getPlayerProgress(player);
        ExperienceManager.addExperience(progress, amount);
        savePlayerProgress(player, progress);
    }

    public static int getPlayerLevel(ServerPlayerEntity player) {
        return getPlayerProgress(player).getLevel();
    }

    public static long getPlayerExperience(ServerPlayerEntity player) {
        return getPlayerProgress(player).getExperience();
    }
}
