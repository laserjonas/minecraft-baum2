package de.baum2dev.baum2.progression;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public class PlayerProgressData {
    public static final Codec<PlayerProgressData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.fieldOf("Level").forGetter(PlayerProgressData::getLevel),
            Codec.LONG.fieldOf("Experience").forGetter(PlayerProgressData::getExperience),
            Codec.LONG.fieldOf("ExperienceForNextLevel").forGetter(PlayerProgressData::getExperienceForNextLevel)
    ).apply(instance, PlayerProgressData::new));

    private int level;
    private long experience;
    private long experienceForNextLevel;

    public PlayerProgressData() {
        this.level = 1;
        this.experience = 0;
        this.experienceForNextLevel = 100;
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
}
