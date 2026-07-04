package de.baum2dev.baum2.progression;

/**
 * Grants and spends unspent attribute points. Does not apply any effects itself (see
 * {@link VitalsManager} for that) - just mutates the raw attribute/point counts on
 * {@link PlayerProgressData}.
 */
public class AttributeManager {

    public static void grantLevelUpPoint(PlayerProgressData progress) {
        progress.setUnspentAttributePoints(progress.getUnspentAttributePoints() + VitalsCurve.ATTRIBUTE_POINTS_PER_LEVEL);
    }

    /**
     * Spends one unspent point into the given attribute. Returns false (no-op) if the
     * player has no unspent points - callers must not assume this always succeeds.
     */
    public static boolean trySpendPoint(PlayerProgressData progress, AttributeType type) {
        if (progress.getUnspentAttributePoints() <= 0) {
            return false;
        }

        progress.setUnspentAttributePoints(progress.getUnspentAttributePoints() - 1);
        switch (type) {
            case ENDURANCE -> progress.setEndurance(progress.getEndurance() + 1);
            case INTELLIGENCE -> progress.setIntelligence(progress.getIntelligence() + 1);
            case STRENGTH -> progress.setStrength(progress.getStrength() + 1);
            case DEXTERITY -> progress.setDexterity(progress.getDexterity() + 1);
        }
        return true;
    }
}
