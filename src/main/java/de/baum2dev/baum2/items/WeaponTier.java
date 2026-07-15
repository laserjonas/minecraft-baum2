package de.baum2dev.baum2.items;

import java.util.Optional;

/**
 * The weapon upgrade ladder: BASE -> TEMPERED -> PERFECT. Applies only to this mod's own
 * weapons (see WeaponUpgradeSystem.UPGRADEABLE_WEAPONS), never vanilla items.
 *
 * <p>Naming: the user's original tier names were "Excellent"/"Perfect".
 * ip-naming-compliance-checker flagged "Excellent" as MU Online's (Webzen's) branded
 * item-quality prefix - same word, same name-prefix display pattern - and Webzen is
 * explicitly off-limits per CLAUDE.md, so it became "Tempered". "Perfect" was cleared
 * (generic quality word, no specific-game association).
 *
 * <p>componentValue doubles as the model-select case string in the item-definition JSONs
 * (assets/baum2/items/*.json) and the texture suffix (textures/item/*_tempered.png etc.) -
 * renaming a tier means changing it here AND in those JSONs/filenames together.
 */
public enum WeaponTier {
    BASE(null, "", 1.0),
    TEMPERED("tempered", "Tempered", 1.15),
    PERFECT("perfect", "Perfect", 1.35);

    private final String componentValue;
    private final String displayPrefix;
    private final double damageMultiplier;

    WeaponTier(String componentValue, String displayPrefix, double damageMultiplier) {
        this.componentValue = componentValue;
        this.displayPrefix = displayPrefix;
        this.damageMultiplier = damageMultiplier;
    }

    /** The baum2:weapon_tier component string; null for BASE (component absent). */
    public String componentValue() {
        return componentValue;
    }

    /** Prepended to the weapon's own name, e.g. "Tempered Gold Sword". Empty for BASE. */
    public String displayPrefix() {
        return displayPrefix;
    }

    /** Scales the weapon's own attack-damage modifier (positive values only). */
    public double damageMultiplier() {
        return damageMultiplier;
    }

    public Optional<WeaponTier> next() {
        return switch (this) {
            case BASE -> Optional.of(TEMPERED);
            case TEMPERED -> Optional.of(PERFECT);
            case PERFECT -> Optional.empty();
        };
    }

    public static WeaponTier fromComponentValue(String value) {
        for (WeaponTier tier : values()) {
            if (tier.componentValue != null && tier.componentValue.equals(value)) {
                return tier;
            }
        }
        return BASE;
    }
}
