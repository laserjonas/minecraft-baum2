package de.baum2dev.baum2.combat;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.TypeFilter;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

/**
 * Shared "Wave of Darkness" rectangular AoE - a rotated rectangle (not an axis-aligned box)
 * {@code range} blocks long by {@code width} blocks wide, extending from {@code origin} along
 * {@code direction}. Used both by {@code DrevathisEntity}'s own skill (65 flat damage) and by
 * Drevathis's Cursed Blade's on-hit proc (10% of the wielder's live Attack Damage) - kept as one
 * static helper so the two callers can't drift apart on the hit-test math.
 *
 * <p>Broad-phase query via an axis-aligned {@code Box} big enough to contain the rotated
 * rectangle (same "AABB broad-phase, precise math filter" shape already used by
 * {@code ZombieColossusEntity.tickFireWave()}), then an exact along/across dot-product test
 * against the rectangle's own axes. Hits any {@code LivingEntity} except the caster (same
 * "generic AoE, not player-only" convention {@code skills/SpellEffects.java}'s own AoE spells
 * already use) - needed so the sword's on-hit proc actually damages hostile mobs, not just other
 * players (balance-reviewer finding: an earlier player-only filter meant the proc could never
 * damage the thing you actually hit).
 */
public final class DarkWaveEffect {

    public static void cast(
            ServerWorld world, Entity caster, Vec3d origin, Vec3d direction, float damage, double range, double width) {
        Vec3d flatDirection = new Vec3d(direction.x, 0.0, direction.z);
        if (flatDirection.lengthSquared() < 1.0E-7) {
            return;
        }
        Vec3d forward = flatDirection.normalize();
        Vec3d across = new Vec3d(-forward.z, 0.0, forward.x);

        Vec3d center = origin.add(forward.multiply(range / 2.0));
        double halfDiagonal = Math.sqrt((range / 2.0) * (range / 2.0) + (width / 2.0) * (width / 2.0)) + 1.0;
        Box searchBox = Box.of(center, halfDiagonal * 2.0, 4.0, halfDiagonal * 2.0);

        DamageSource source = world.getDamageSources().indirectMagic(caster, caster);
        for (LivingEntity target : world.getEntitiesByType(
                TypeFilter.instanceOf(LivingEntity.class), searchBox, e -> e.isAlive() && e != caster)) {
            Vec3d relative = target.getEntityPos().subtract(origin);
            double along = relative.dotProduct(forward);
            double side = relative.dotProduct(across);
            if (along >= 0.0 && along <= range && Math.abs(side) <= width / 2.0) {
                target.damage(world, source, damage);
            }
        }

        spawnWaveParticles(world, origin, forward, across, range, width);
        world.playSound(null, origin.x, origin.y, origin.z, SoundEvents.ENTITY_WITHER_SHOOT, SoundCategory.HOSTILE, 1.2F, 0.8F);
    }

    /**
     * Fills the whole rectangle densely (several rows across the width, a couple of vertical
     * layers) rather than a sparse 3-line outline - an instant, one-tick effect with too few
     * particles was easy to miss entirely (playtest finding: "the skill itself is not visible").
     */
    private static void spawnWaveParticles(ServerWorld world, Vec3d origin, Vec3d forward, Vec3d across, double range, double width) {
        int alongSteps = (int) Math.ceil(range * 1.5);
        int acrossSteps = Math.max(2, (int) Math.ceil(width));
        for (int i = 0; i <= alongSteps; i++) {
            double along = (range * i) / alongSteps;
            Vec3d rowCenter = origin.add(forward.multiply(along));
            for (int j = 0; j <= acrossSteps; j++) {
                double offset = width * ((double) j / acrossSteps - 0.5);
                Vec3d point = rowCenter.add(across.multiply(offset));
                world.spawnParticles(ParticleTypes.SQUID_INK, point.x, point.y + 0.15, point.z, 2, 0.15, 0.3, 0.15, 0.01);
                world.spawnParticles(ParticleTypes.SCULK_SOUL, point.x, point.y + 0.6, point.z, 1, 0.15, 0.3, 0.15, 0.01);
            }
        }
    }

    private DarkWaveEffect() {
    }
}
