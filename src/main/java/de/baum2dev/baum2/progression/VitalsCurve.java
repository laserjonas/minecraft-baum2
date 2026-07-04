package de.baum2dev.baum2.progression;

/**
 * Level-scaled Life and Mana formulas. Single source of truth, mirroring how
 * {@link ProgressionCurve} centralizes the XP curve.
 */
public class VitalsCurve {

    public static double getMaxLife(int level) {
        return 500.0 + 10.0 * level;
    }

    public static int getMaxMana(int level) {
        return 100 + 5 * level;
    }
}
