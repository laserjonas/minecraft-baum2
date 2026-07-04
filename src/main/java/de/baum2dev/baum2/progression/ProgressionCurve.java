package de.baum2dev.baum2.progression;

/**
 * Our custom XP curve (deliberately much steeper than vanilla Minecraft's own leveling
 * formula, since our mob/command rewards are 10-700+ XP per grant versus vanilla's 1-7).
 * Chosen pace: a "hardcore grind" curve where reaching max level is a serious long-term
 * achievement. Every level requires strictly more XP than the last.
 */
public class ProgressionCurve {

    public static long getXpRequiredForLevel(int level) {
        if (level <= 0) {
            return 0;
        }
        return 80L + 40L * level + 8L * level * level;
    }

    public static long getTotalXpForLevel(int level) {
        long total = 0;
        for (int l = 1; l <= level; l++) {
            total += getXpRequiredForLevel(l);
        }
        return total;
    }
}
