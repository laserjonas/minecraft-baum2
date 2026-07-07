package de.baum2dev.baum2.entity;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

/**
 * Drevathis's basic attack: a slow-enough-to-dodge wall of darkness flying in a straight line
 * (no gravity). Aimed at the target's position at cast time, so strafing players outrun it -
 * "can miss if the target is fast enough" is the design intent, NOT a homing projectile.
 *
 * <p>Rendered as nothing (client registers {@code EmptyEntityRenderer}); the visible wave is
 * the dense server-spawned particle crescent emitted every tick perpendicular to the flight
 * direction. Damage hits ANY living entity except the owner (same "generic AoE" convention as
 * {@code DarkWaveEffect}), then the wave dissipates.
 */
public class DarkWaveProjectileEntity extends ProjectileEntity {
    /** Blocks per tick; 0.85 ~= 17 m/s - a sprinting player (5.6 m/s) strafes out of the
     *  ~1.4-block half-width in well under the flight time from 10+ blocks away. */
    public static final double SPEED = 0.85;
    private static final float DAMAGE = 50.0F;
    private static final int MAX_LIFETIME_TICKS = 60;
    private static final double CRESCENT_HALF_WIDTH = 1.4;

    private int ageTicks;

    public DarkWaveProjectileEntity(EntityType<? extends DarkWaveProjectileEntity> entityType, World world) {
        super(entityType, world);
    }

    @Override
    protected void initDataTracker(DataTracker.Builder builder) {
        // no synced data - the client never renders this entity directly
    }

    @Override
    public void tick() {
        super.tick();

        HitResult hit = ProjectileUtil.getCollision(this, this::canHit);
        if (hit.getType() != HitResult.Type.MISS) {
            this.hitOrDeflect(hit);
        }
        if (this.isRemoved()) {
            return;
        }

        Vec3d velocity = getVelocity();
        setPosition(getX() + velocity.x, getY() + velocity.y, getZ() + velocity.z);

        if (getEntityWorld() instanceof ServerWorld serverWorld) {
            spawnCrescent(serverWorld);
            if (++ageTicks > MAX_LIFETIME_TICKS) {
                discard();
            }
        }
    }

    @Override
    protected boolean canHit(Entity entity) {
        return super.canHit(entity) && entity != getOwner();
    }

    @Override
    protected void onEntityHit(EntityHitResult hitResult) {
        super.onEntityHit(hitResult);
        if (!(getEntityWorld() instanceof ServerWorld serverWorld)) {
            return;
        }
        if (hitResult.getEntity() instanceof LivingEntity living) {
            living.damage(serverWorld, getDamageSources().indirectMagic(this, getOwner()), DAMAGE);
        }
        burst(serverWorld);
        discard();
    }

    @Override
    protected void onCollision(HitResult hitResult) {
        super.onCollision(hitResult);
        // block hits (and anything else that isn't an entity) just dissipate the wave
        if (!this.isRemoved() && hitResult.getType() == HitResult.Type.BLOCK
                && getEntityWorld() instanceof ServerWorld serverWorld) {
            burst(serverWorld);
            discard();
        }
    }

    /** The visible wave: a vertical-ish crescent of ink/soul particles across the flight path. */
    private void spawnCrescent(ServerWorld world) {
        Vec3d velocity = getVelocity();
        Vec3d flat = new Vec3d(velocity.x, 0.0, velocity.z);
        if (flat.lengthSquared() < 1.0E-7) {
            return;
        }
        Vec3d forward = flat.normalize();
        Vec3d across = new Vec3d(-forward.z, 0.0, forward.x);
        for (int i = -2; i <= 2; i++) {
            // slight backward bow at the flanks - reads as a crescent, not a flat line
            Vec3d point = getEntityPos()
                    .add(across.multiply(i * (CRESCENT_HALF_WIDTH / 2.0)))
                    .add(forward.multiply(-0.18 * Math.abs(i)));
            world.spawnParticles(ParticleTypes.SQUID_INK, point.x, point.y + 0.5, point.z,
                    3, 0.1, 0.45, 0.1, 0.015);
            if ((i + ageTicks) % 2 == 0) {
                world.spawnParticles(ParticleTypes.SCULK_SOUL, point.x, point.y + 0.9, point.z,
                        1, 0.1, 0.3, 0.1, 0.01);
            }
        }
    }

    private void burst(ServerWorld world) {
        world.spawnParticles(ParticleTypes.SQUID_INK, getX(), getY() + 0.6, getZ(),
                25, 0.5, 0.6, 0.5, 0.06);
        world.spawnParticles(ParticleTypes.SCULK_SOUL, getX(), getY() + 0.8, getZ(),
                8, 0.4, 0.5, 0.4, 0.03);
        world.playSound(null, getX(), getY(), getZ(), SoundEvents.ENTITY_WITHER_HURT,
                getSoundCategory(), 0.7F, 1.6F);
    }
}
