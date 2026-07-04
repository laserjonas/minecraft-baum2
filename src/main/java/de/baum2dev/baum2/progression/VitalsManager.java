package de.baum2dev.baum2.progression;

import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.server.network.ServerPlayerEntity;
import de.baum2dev.baum2.mixin.ClampedEntityAttributeAccessor;

/**
 * Applies attribute-scaled Life/Life Regen (real vanilla max-health attribute) and
 * level-scaled Mana (custom, persisted in {@link PlayerProgressData}) to players.
 */
public class VitalsManager {
    private static final int MANA_REGEN_DIVISOR = 50;

    /**
     * Vanilla clamps EntityAttributes.MAX_HEALTH at 1024 (see ClampedEntityAttribute). Since
     * VitalsCurve.getMaxLife can reach ~2480 if a player dumps every level-up point (up to
     * level 100) into Endurance, that clamp would silently cap Life (and stop it growing at
     * all) well before that. Widen the ceiling once during mod init, with headroom above the
     * current theoretical max, so the formula actually holds across the whole range.
     */
    public static void widenMaxHealthCeiling() {
        ClampedEntityAttributeAccessor accessor = (ClampedEntityAttributeAccessor) EntityAttributes.MAX_HEALTH.value();
        accessor.setMaxValue(4096.0);
    }

    /**
     * Rescales the player's real max-health attribute to {@link VitalsCurve#getMaxLife(int)}.
     * If the player was at full health under the old max, tops them up to the new max too
     * (so joining/leveling doesn't leave them stranded at a tiny fraction of a much bigger
     * bar); a player who was already damaged keeps their current health unchanged.
     */
    public static void applyMaxLife(ServerPlayerEntity player, int endurance) {
        EntityAttributeInstance maxHealth = player.getAttributeInstance(EntityAttributes.MAX_HEALTH);
        if (maxHealth == null) {
            return;
        }

        double oldMax = maxHealth.getBaseValue();
        double newMax = VitalsCurve.getMaxLife(endurance);
        if (oldMax == newMax) {
            return;
        }

        boolean wasFullHealth = player.getHealth() >= (float) oldMax;
        maxHealth.setBaseValue(newMax);
        if (wasFullHealth) {
            player.setHealth((float) newMax);
        }
    }

    /** Passive Life regen, scaled by Endurance. Does not revive/heal a dead player. */
    public static void regenLife(ServerPlayerEntity player, int endurance) {
        if (!player.isAlive()) {
            return;
        }
        float maxHealth = player.getMaxHealth();
        if (player.getHealth() >= maxHealth) {
            return;
        }
        float regenAmount = (float) VitalsCurve.getLifeRegen(endurance);
        player.setHealth(Math.min(maxHealth, player.getHealth() + regenAmount));
    }

    public static void clampMana(PlayerProgressData progress, int level) {
        int maxMana = VitalsCurve.getMaxMana(level);
        if (progress.getMana() > maxMana) {
            progress.setMana(maxMana);
        }
    }

    /** Simple passive regen: ~2% of max mana per call, minimum 1. No consumers exist yet. */
    public static void regenMana(PlayerProgressData progress, int level) {
        int maxMana = VitalsCurve.getMaxMana(level);
        if (progress.getMana() >= maxMana) {
            return;
        }
        int regenAmount = Math.max(1, maxMana / MANA_REGEN_DIVISOR);
        progress.setMana(Math.min(maxMana, progress.getMana() + regenAmount));
    }
}
