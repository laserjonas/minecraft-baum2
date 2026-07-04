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
        return VanillaXpFormula.getXpRequiredForLevel(level);
    }

    public static int getMaxLevel() {
        return 100;
    }
}
