package de.baum2dev.baum2.progression;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public class PlayerProgressData {
    // Mana uses optionalFieldOf with a fallback default, not fieldOf: this Codec is used to
    // deserialize existing save data from before the Mana field existed. fieldOf requires
    // every field to be present or the WHOLE record fails to decode (confirmed - an existing
    // save missing only "Mana" silently discarded Level/Experience too, resetting a level-45
    // test character back to level 1). VitalsManager re-clamps/regenerates Mana every tick
    // regardless, so the fallback value here only matters for the first tick after upgrade.
    // Base Damage/Base Magic Damage are flat starting values (not level-scaled), same
    // optionalFieldOf-not-fieldOf rule as Mana above applies to them too.
    public static final Codec<PlayerProgressData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.fieldOf("Level").forGetter(PlayerProgressData::getLevel),
            Codec.LONG.fieldOf("Experience").forGetter(PlayerProgressData::getExperience),
            Codec.LONG.fieldOf("ExperienceForNextLevel").forGetter(PlayerProgressData::getExperienceForNextLevel),
            Codec.INT.optionalFieldOf("Mana", 100).forGetter(PlayerProgressData::getMana),
            Codec.FLOAT.optionalFieldOf("BaseDamage", VitalsCurve.getBaseDamage()).forGetter(PlayerProgressData::getBaseDamage),
            Codec.FLOAT.optionalFieldOf("BaseMagicDamage", VitalsCurve.getBaseMagicDamage()).forGetter(PlayerProgressData::getBaseMagicDamage)
    ).apply(instance, PlayerProgressData::new));

    private int level;
    private long experience;
    private long experienceForNextLevel;
    private int mana;
    private float baseDamage;
    private float baseMagicDamage;

    public PlayerProgressData() {
        this.level = 1;
        this.experience = 0;
        this.experienceForNextLevel = 100;
        this.mana = VitalsCurve.getMaxMana(this.level);
        this.baseDamage = VitalsCurve.getBaseDamage();
        this.baseMagicDamage = VitalsCurve.getBaseMagicDamage();
    }

    public PlayerProgressData(int level, long experience, long experienceForNextLevel, int mana, float baseDamage, float baseMagicDamage) {
        this.level = level;
        this.experience = experience;
        this.experienceForNextLevel = experienceForNextLevel;
        this.mana = mana;
        this.baseDamage = baseDamage;
        this.baseMagicDamage = baseMagicDamage;
    }

    public int getMana() {
        return mana;
    }

    public void setMana(int mana) {
        this.mana = mana;
    }

    public float getBaseDamage() {
        return baseDamage;
    }

    public void setBaseDamage(float baseDamage) {
        this.baseDamage = baseDamage;
    }

    public float getBaseMagicDamage() {
        return baseMagicDamage;
    }

    public void setBaseMagicDamage(float baseMagicDamage) {
        this.baseMagicDamage = baseMagicDamage;
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
