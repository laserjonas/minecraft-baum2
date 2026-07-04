package de.baum2dev.baum2.progression;

public class ExperienceManager {

    public static void addExperience(PlayerProgressData data, long amount) {
        data.setExperience(data.getExperience() + amount);
        while (data.getExperience() >= data.getExperienceForNextLevel()) {
            levelUp(data);
        }
    }

    private static void levelUp(PlayerProgressData data) {
        long excess = data.getExperience() - data.getExperienceForNextLevel();
        data.setLevel(data.getLevel() + 1);
        data.setExperience(excess);
        long nextLevelRequirement = calculateExperienceForLevel(data.getLevel() + 1);
        data.setExperienceForNextLevel(nextLevelRequirement);
    }

    public static long calculateExperienceForLevel(int level) {
        if (level <= 0) return 0;

        int prevLevel = level - 1;
        long xpForPrevLevel = getTotalXpForLevel(prevLevel);
        long xpForCurrentLevel = getTotalXpForLevel(level);

        return xpForCurrentLevel - xpForPrevLevel;
    }

    private static long getTotalXpForLevel(int level) {
        if (level <= 15) {
            return (long) (level * level + 6L * level);
        } else if (level <= 31) {
            return (long) (2.5 * level * level - 40.5 * level + 360);
        } else {
            return (long) (4.5 * level * level - 162.5 * level + 2220);
        }
    }

    public static int getMaxLevel() {
        return 100;
    }
}
