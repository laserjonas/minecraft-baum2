package de.baum2dev.baum2.progression;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public class PlayerProgressData {
    // Mana/attribute fields all use optionalFieldOf with a fallback default, not fieldOf:
    // this Codec deserializes existing save data from before these fields existed, and
    // fieldOf requires every field present or the WHOLE record fails to decode (confirmed -
    // an existing save missing only "Mana" once silently discarded Level/Experience too,
    // resetting a level-45 test character back to level 1). The four attributes default to
    // VitalsCurve.STARTING_ATTRIBUTE_POINTS (5) and UnspentAttributePoints defaults to 0 for
    // any save predating this system.
    public static final Codec<PlayerProgressData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.fieldOf("Level").forGetter(PlayerProgressData::getLevel),
            Codec.LONG.fieldOf("Experience").forGetter(PlayerProgressData::getExperience),
            Codec.LONG.fieldOf("ExperienceForNextLevel").forGetter(PlayerProgressData::getExperienceForNextLevel),
            Codec.INT.optionalFieldOf("Mana", 100).forGetter(PlayerProgressData::getMana),
            Codec.INT.optionalFieldOf("Endurance", VitalsCurve.STARTING_ATTRIBUTE_POINTS).forGetter(PlayerProgressData::getEndurance),
            Codec.INT.optionalFieldOf("Intelligence", VitalsCurve.STARTING_ATTRIBUTE_POINTS).forGetter(PlayerProgressData::getIntelligence),
            Codec.INT.optionalFieldOf("Strength", VitalsCurve.STARTING_ATTRIBUTE_POINTS).forGetter(PlayerProgressData::getStrength),
            Codec.INT.optionalFieldOf("Dexterity", VitalsCurve.STARTING_ATTRIBUTE_POINTS).forGetter(PlayerProgressData::getDexterity),
            Codec.INT.optionalFieldOf("UnspentAttributePoints", 0).forGetter(PlayerProgressData::getUnspentAttributePoints)
    ).apply(instance, PlayerProgressData::new));

    private int level;
    private long experience;
    private long experienceForNextLevel;
    private int mana;
    private int endurance;
    private int intelligence;
    private int strength;
    private int dexterity;
    private int unspentAttributePoints;

    public PlayerProgressData() {
        this.level = 1;
        this.experience = 0;
        this.experienceForNextLevel = 100;
        this.mana = VitalsCurve.getMaxMana(this.level);
        this.endurance = VitalsCurve.STARTING_ATTRIBUTE_POINTS;
        this.intelligence = VitalsCurve.STARTING_ATTRIBUTE_POINTS;
        this.strength = VitalsCurve.STARTING_ATTRIBUTE_POINTS;
        this.dexterity = VitalsCurve.STARTING_ATTRIBUTE_POINTS;
        this.unspentAttributePoints = 0;
    }

    public PlayerProgressData(int level, long experience, long experienceForNextLevel, int mana,
                               int endurance, int intelligence, int strength, int dexterity, int unspentAttributePoints) {
        this.level = level;
        this.experience = experience;
        this.experienceForNextLevel = experienceForNextLevel;
        this.mana = mana;
        this.endurance = endurance;
        this.intelligence = intelligence;
        this.strength = strength;
        this.dexterity = dexterity;
        this.unspentAttributePoints = unspentAttributePoints;
    }

    public int getMana() {
        return mana;
    }

    public void setMana(int mana) {
        this.mana = mana;
    }

    public int getEndurance() {
        return endurance;
    }

    public void setEndurance(int endurance) {
        this.endurance = endurance;
    }

    public int getIntelligence() {
        return intelligence;
    }

    public void setIntelligence(int intelligence) {
        this.intelligence = intelligence;
    }

    public int getStrength() {
        return strength;
    }

    public void setStrength(int strength) {
        this.strength = strength;
    }

    public int getDexterity() {
        return dexterity;
    }

    public void setDexterity(int dexterity) {
        this.dexterity = dexterity;
    }

    public int getUnspentAttributePoints() {
        return unspentAttributePoints;
    }

    public void setUnspentAttributePoints(int unspentAttributePoints) {
        this.unspentAttributePoints = unspentAttributePoints;
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
