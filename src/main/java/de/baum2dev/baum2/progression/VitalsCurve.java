package de.baum2dev.baum2.progression;

/**
 * Level-scaled Life and Mana formulas, plus flat starting combat stats. Single source of
 * truth, mirroring how {@link ProgressionCurve} centralizes the XP curve.
 */
public class VitalsCurve {
    // Flat starting values, not level-scaled - future gear/skills/talents are meant to
    // increase these, not this formula.
    private static final float BASE_DAMAGE = 5.0f;
    private static final float BASE_MAGIC_DAMAGE = 5.0f;

    public static double getMaxLife(int level) {
        return 500.0 + 10.0 * level;
    }

    public static int getMaxMana(int level) {
        return 100 + 5 * level;
    }

    public static float getBaseDamage() {
        return BASE_DAMAGE;
    }

    public static float getBaseMagicDamage() {
        return BASE_MAGIC_DAMAGE;
    }
}
