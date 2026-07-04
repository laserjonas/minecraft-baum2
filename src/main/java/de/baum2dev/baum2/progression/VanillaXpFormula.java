package de.baum2dev.baum2.progression;

/**
 * Minecraft's vanilla non-linear XP curve. Used both to drive our custom
 * progression requirements and to compute the totalExperience value that
 * makes the vanilla level bar render the correct fill percentage.
 */
public class VanillaXpFormula {

    public static long getTotalXpForLevel(int level) {
        if (level <= 0) {
            return 0;
        } else if (level <= 15) {
            return (long) (level * level + 6L * level);
        } else if (level <= 31) {
            return (long) (2.5 * level * level - 40.5 * level + 360);
        } else {
            return (long) (4.5 * level * level - 162.5 * level + 2220);
        }
    }

    public static long getXpRequiredForLevel(int level) {
        if (level <= 0) {
            return 0;
        }
        return getTotalXpForLevel(level) - getTotalXpForLevel(level - 1);
    }
}
