package de.baum2dev.baum2.progression;

import net.minecraft.nbt.NbtCompound;

public class PlayerProgressData {
    private int level;
    private long experience;
    private long experienceForNextLevel;

    public PlayerProgressData() {
        this.level = 1;
        this.experience = 0;
        this.experienceForNextLevel = VanillaXpFormula.getXpRequiredForLevel(level + 1);
    }

    public PlayerProgressData(int level, long experience, long experienceForNextLevel) {
        this.level = level;
        this.experience = experience;
        this.experienceForNextLevel = experienceForNextLevel;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public long getExperience() {
        return experience;
    }

    public void setExperience(long experience) {
        this.experience = experience;
    }

    public long getExperienceForNextLevel() {
        return experienceForNextLevel;
    }

    public void setExperienceForNextLevel(long experienceForNextLevel) {
        this.experienceForNextLevel = experienceForNextLevel;
    }

    public float getExperienceProgress() {
        return experienceForNextLevel > 0 ? (float) experience / experienceForNextLevel : 0;
    }

    public NbtCompound writeNbt(NbtCompound tag) {
        tag.putInt("Level", level);
        tag.putLong("Experience", experience);
        tag.putLong("ExperienceForNextLevel", experienceForNextLevel);
        return tag;
    }

    public static PlayerProgressData readNbt(NbtCompound tag) {
        int level = tag.contains("Level") ? tag.getInt("Level").orElse(1) : 1;
        long experience = tag.contains("Experience") ? tag.getLong("Experience").orElse(0L) : 0;
        long fallbackExperienceForNextLevel = VanillaXpFormula.getXpRequiredForLevel(level + 1);
        long experienceForNextLevel = tag.contains("ExperienceForNextLevel")
            ? tag.getLong("ExperienceForNextLevel").orElse(fallbackExperienceForNextLevel)
            : fallbackExperienceForNextLevel;
        return new PlayerProgressData(level, experience, experienceForNextLevel);
    }
}
