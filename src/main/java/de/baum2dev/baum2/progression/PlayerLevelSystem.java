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

    /**
     * Forces this class to load (and therefore registers {@link #PROGRESSION}) during mod init.
     * Must be called from {@code Baum2.onInitialize()}. Without this, the class only loads on
     * the first actual call to one of its methods, which happens inside event/command callback
     * bodies that only run after a player has already joined — by which point Fabric has already
     * tried to deserialize that player's saved attachment NBT and found no matching registered
     * type, silently dropping it. That looked exactly like "progress resets on every rebuild"
     * even with a stable player UUID.
     */
    public static void bootstrap() {
    }

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
        player.totalExperience = (int) (ProgressionCurve.getTotalXpForLevel(customLevel) + currentExp);
    }
}
