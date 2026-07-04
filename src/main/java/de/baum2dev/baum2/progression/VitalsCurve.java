package de.baum2dev.baum2.progression;

/**
 * Attribute-scaled Life, Mana, and combat stat formulas. Single source of truth, mirroring
 * how {@link ProgressionCurve} centralizes the XP curve. Shared between server and client
 * (common source set) so the client can compute derived stats locally from synced raw
 * attribute values instead of needing every derived stat synced separately.
 *
 * <p>Every attribute (Endurance/Intelligence/Strength/Dexterity) starts at
 * {@link #STARTING_ATTRIBUTE_POINTS} and grants {@link #ATTRIBUTE_POINTS_PER_LEVEL} unspent
 * point per level-up for the player to invest. Each derived-stat formula is calibrated so
 * that plugging in the starting value (5) reproduces the stat's base value at character
 * creation (e.g. {@code getLifeRegen(5) == 0.25}).
 */
public class VitalsCurve {
    public static final int STARTING_ATTRIBUTE_POINTS = 5;
    public static final int ATTRIBUTE_POINTS_PER_LEVEL = 1;
    // These caps are never reached by pure leveling alone (max attainable attribute via
    // level-up points is 104, e.g. crit chance tops out at 54.5% there) - they exist as a
    // safety ceiling against a future gear/skill system pushing an attribute's *effective*
    // value beyond what leveling alone provides, not as a currently-binding constraint.
    private static final double MAX_CRIT_CHANCE = 75.0;
    private static final float MAX_SPEED_MULTIPLIER = 3.0f;

    public static int getMaxMana(int level) {
        return 100 + 5 * level;
    }

    // Endurance -> Life, Life Regen
    public static double getMaxLife(int endurance) {
        return 500.0 + 20.0 * (endurance - STARTING_ATTRIBUTE_POINTS);
    }

    public static double getLifeRegen(int endurance) {
        return 0.25 + 0.05 * (endurance - STARTING_ATTRIBUTE_POINTS);
    }

    // Strength -> Base Attack, Physical Defence
    public static float getBaseAttack(int strength) {
        return 5.0f + 1.0f * (strength - STARTING_ATTRIBUTE_POINTS);
    }

    public static float getPhysicalDefence(int strength) {
        return 5.0f + 1.0f * (strength - STARTING_ATTRIBUTE_POINTS);
    }

    // Intelligence -> Base Magic Attack, Magic Defence
    public static float getBaseMagicAttack(int intelligence) {
        return 5.0f + 1.0f * (intelligence - STARTING_ATTRIBUTE_POINTS);
    }

    public static float getMagicDefence(int intelligence) {
        return 5.0f + 1.0f * (intelligence - STARTING_ATTRIBUTE_POINTS);
    }

    // Dexterity -> Attack Speed, Cast Speed, Crit Chance
    public static float getAttackSpeedMultiplier(int dexterity) {
        float multiplier = 1.0f + 0.01f * (dexterity - STARTING_ATTRIBUTE_POINTS);
        return Math.min(MAX_SPEED_MULTIPLIER, multiplier);
    }

    public static float getCastSpeedMultiplier(int dexterity) {
        float multiplier = 1.0f + 0.01f * (dexterity - STARTING_ATTRIBUTE_POINTS);
        return Math.min(MAX_SPEED_MULTIPLIER, multiplier);
    }

    public static double getCritChance(int dexterity) {
        double chance = 5.0 + 0.5 * (dexterity - STARTING_ATTRIBUTE_POINTS);
        return Math.min(MAX_CRIT_CHANCE, chance);
    }
}
