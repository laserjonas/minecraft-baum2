package de.baum2dev.baum2.progression;

import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

public class PlayerLevelSystem {
    private static final AttachmentType<PlayerProgressData> PROGRESSION = AttachmentRegistry.create(
            Identifier.of("baum2", "progression"),
            builder -> builder
                    .persistent(PlayerProgressData.CODEC)
                    .copyOnDeath()
                    .initializer(PlayerProgressData::new)
    );

    public static PlayerProgressData getPlayerProgress(ServerPlayerEntity player) {
        return player.getAttachedOrCreate(PROGRESSION);
    }

    public static void savePlayerProgress(ServerPlayerEntity player, PlayerProgressData progress) {
        player.setAttached(PROGRESSION, progress);
    }

    public static void clearPlayerProgress(ServerPlayerEntity player) {
        player.removeAttached(PROGRESSION);
    }

    public static void addExperience(ServerPlayerEntity player, long amount) {
        PlayerProgressData progress = getPlayerProgress(player);
        ExperienceManager.addExperience(progress, amount);
        savePlayerProgress(player, progress);
        syncVanillaLevelDisplay(player, progress);
    }

    public static int getPlayerLevel(ServerPlayerEntity player) {
        return getPlayerProgress(player).getLevel();
    }

    public static long getPlayerExperience(ServerPlayerEntity player) {
        return getPlayerProgress(player).getExperience();
    }

    private static void syncVanillaLevelDisplay(ServerPlayerEntity player, PlayerProgressData progress) {
        int customLevel = progress.getLevel();
        long currentExp = progress.getExperience();

        player.setExperienceLevel(customLevel);
        player.totalExperience = (int) (VanillaXpFormula.getTotalXpForLevel(customLevel) + currentExp);
    }
}
