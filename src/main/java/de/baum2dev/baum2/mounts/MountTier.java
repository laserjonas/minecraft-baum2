package de.baum2dev.baum2.mounts;

/**
 * The three mount tiers, one per horse-flute item. Each tier defines both the rules while
 * mounted (may the rider fight? does a melee hit splash into an AOE?) and the summoned
 * horse's own stats/size. All numbers are original balance values (see the Balancing Rules
 * in CLAUDE.md) - reviewed for internal consistency by balance-reviewer, not copied from
 * anywhere.
 *
 * Naming: the tiers are named after their horses (Wanderross/Eisenross/Schlachtross - all
 * cleared by ip-naming-compliance-checker), deliberately NOT "Basic/Advanced/Military" -
 * the checker found "Military Horse" is the literal translation of Metin2's real top mount
 * tier ("Militärpferd") and the generic basic/advanced/military labels mapped 1:1 onto that
 * game's documented tier structure, which CLAUDE.md forbids even in translated form.
 *
 * Visual identity per tier (see docs/visual-style-guide.md): the three tiers share ONE
 * GeckoLib geometry (including saddle-armor and body-armor cubes) and differ by texture
 * only - Wanderross's texture leaves the armor cubes fully transparent, Eisenross paints
 * the iron saddle armor, Schlachtross paints the full black plate. Size differences come
 * from the per-tier renderer scale + hitbox dimensions.
 */
public enum MountTier {

    /** Plain riding horse - transport only, the rider cannot attack while mounted. */
    WANDERROSS("wanderross", false, 0.0, 0.0F, 0.25, 40.0, 1.0F),

    /** Iron-armored saddle; the rider may fight normally while mounted. */
    EISENROSS("eisenross", true, 0.0, 0.0F, 0.25, 60.0, 1.1F),

    /** Fully black-armored warhorse - faster, and the rider's melee hits splash AOE. */
    SCHLACHTROSS("schlachtross", true, 2.5, 0.5F, 0.32, 80.0, 1.25F);

    private final String id;
    private final boolean canFightMounted;
    private final double meleeAoeRadius;
    private final float meleeAoeDamageFraction;
    private final double movementSpeed;
    private final double maxHealth;
    private final float renderScale;

    MountTier(String id, boolean canFightMounted, double meleeAoeRadius,
              float meleeAoeDamageFraction, double movementSpeed, double maxHealth,
              float renderScale) {
        this.id = id;
        this.canFightMounted = canFightMounted;
        this.meleeAoeRadius = meleeAoeRadius;
        this.meleeAoeDamageFraction = meleeAoeDamageFraction;
        this.movementSpeed = movementSpeed;
        this.maxHealth = maxHealth;
        this.renderScale = renderScale;
    }

    /** Registry id of the mount entity ({@code baum2:wanderross}) and stem of the flute item id. */
    public String id() {
        return id;
    }

    /** May the rider attack at all while sitting on this mount? */
    public boolean canFightMounted() {
        return canFightMounted;
    }

    /** Radius (blocks, around the struck target) of the mounted-melee AOE splash; 0 = no AOE. */
    public double meleeAoeRadius() {
        return meleeAoeRadius;
    }

    /** Fraction of the original melee damage each AOE splash victim takes. */
    public float meleeAoeDamageFraction() {
        return meleeAoeDamageFraction;
    }

    public double movementSpeed() {
        return movementSpeed;
    }

    public double maxHealth() {
        return maxHealth;
    }

    /** GeckoLib renderer scale - hitbox dimensions in ModEntities scale to match. */
    public float renderScale() {
        return renderScale;
    }
}
