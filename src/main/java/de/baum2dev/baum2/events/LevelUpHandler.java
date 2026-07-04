package de.baum2dev.baum2.events;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import de.baum2dev.baum2.progression.PlayerLevelSystem;
import de.baum2dev.baum2.progression.PlayerProgressData;

public class LevelUpHandler {
    private static final Map<UUID, Integer> playerLevels = new HashMap<>();

    public static void registerEvents() {
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayerEntity player = handler.getPlayer();
            int customLevel = PlayerLevelSystem.getPlayerLevel(player);
            playerLevels.put(player.getUuid(), customLevel);
            syncVanillaLevelDisplay(player);
        });

        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            playerLevels.remove(handler.getPlayer().getUuid());
        });
    }

    private static void syncVanillaLevelDisplay(ServerPlayerEntity player) {
        PlayerProgressData progress = PlayerLevelSystem.getPlayerProgress(player);
        int customLevel = progress.getLevel();
        long currentExp = progress.getExperience();

        player.setExperienceLevel(customLevel);
        player.totalExperience = (int) (customLevel * 100 + currentExp);
    }

    public static void checkLevelUp(ServerPlayerEntity player) {
        UUID uuid = player.getUuid();
        int currentLevel = PlayerLevelSystem.getPlayerLevel(player);
        int previousLevel = playerLevels.getOrDefault(uuid, 1);

        if (currentLevel > previousLevel) {
            playerLevels.put(uuid, currentLevel);
            syncVanillaLevelDisplay(player);
            broadcastLevelUp(player, currentLevel);
        }
    }

    private static void broadcastLevelUp(ServerPlayerEntity player, int level) {
        Text message = Text.literal("§6✦ " + player.getName().getString() + " reached level " + level + "! §6✦");
        player.sendMessage(message, false);
    }
}
