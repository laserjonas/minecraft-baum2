package de.baum2dev.baum2.entity;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.LazyEntityReference;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.Angerable;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.tag.DamageTypeTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animatable.manager.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * THE stone mini-boss class: a fallen comet stone that cannot move, spawns a wave of its
 * definition's mobs every time it loses another 10% of its max health (cumulative, one-shot
 * per threshold - at most 10 waves; thresholds crossed by the KILLING blow deliberately spawn
 * nothing, because the isAlive() guard runs after super.damage() has already processed death.
 * That is intended behavior twice over: overpowering a stone is rewarded with fewer adds, and
 * a post-death wave would spawn after the death-cascade already ran, leaving orphaned mobs
 * nothing ever cleans up), kills every mob it spawned when it dies, and drops its definition's
 * loot. One class serves every stone: which monster it "belongs" to,
 * its level/health, wave composition, and drops all come from the
 * {@link FallenCometStoneDefinition} injected at construction (see
 * {@code registry.FallenCometStones} for the table). Replaces the original hand-written
 * StoneOfSpidersEntity/StoneOfZombiesEntity pair, preserving their exact wave math.
 *
 * Differences from those originals, both deliberate (this-session rebalance):
 * - Immune to explosion damage: otherwise Stone of Creepers' own waves (and Stone of Ghasts'
 *   fireballs) would damage the stone, triggering further waves in a player-less death spiral.
 *   Thematically the meteor already survived an impact. (Fire immunity is on the EntityType
 *   itself - see ModEntities - for the same reason: Blaze/Magma Cube stones must not burn.)
 * - Wave mobs immediately target whoever damaged the stone, including neutral-until-provoked
 *   monsters (Endermen, Zombified Piglins) via their real anger mechanic - so every stone's
 *   waves actually fight instead of standing around waiting to notice the player.
 */
public class FallenCometStoneEntity extends HostileEntity implements MonsterLevelProvider, GeoEntity {
    private static final float HEALTH_STEP_RATIO = 0.10F;
    private static final double SPAWN_RADIUS_MIN = 2.0;
    private static final double SPAWN_RADIUS_MAX = 4.0;
    private static final long WAVE_MOB_ANGER_TICKS = 600L;

    private final FallenCometStoneDefinition definition;
    private int wavesTriggered = 0;
    private final Set<UUID> spawnedMobIds = new HashSet<>();

    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);

    public FallenCometStoneEntity(EntityType<? extends HostileEntity> entityType, World world,
            FallenCometStoneDefinition definition) {
        super(entityType, world);
        this.definition = definition;
    }

    public static DefaultAttributeContainer.Builder createAttributes(FallenCometStoneDefinition definition) {
        return HostileEntity.createHostileAttributes()
                .add(EntityAttributes.MAX_HEALTH, definition.maxHealth())
                .add(EntityAttributes.MOVEMENT_SPEED, 0.0)
                .add(EntityAttributes.KNOCKBACK_RESISTANCE, 1.0);
    }

    @Override
    public boolean damage(ServerWorld world, DamageSource source, float amount) {
        if (source.isIn(DamageTypeTags.IS_EXPLOSION)) {
            return false;
        }
        boolean damaged = super.damage(world, source, amount);
        if (damaged) {
            LivingEntity aggroTarget = source.getAttacker() instanceof LivingEntity living ? living : null;
            trySpawnWaves(world, aggroTarget);
        }
        return damaged;
    }

    /** Spawns one wave per full 10%-of-max-health increment lost, cumulative. */
    private void trySpawnWaves(ServerWorld world, @Nullable LivingEntity aggroTarget) {
        float missingHealthRatio = 1.0F - (getHealth() / getMaxHealth());
        int thresholdsReached = (int) Math.floor(missingHealthRatio / HEALTH_STEP_RATIO + 1.0e-4);
        while (wavesTriggered < thresholdsReached && isAlive()) {
            wavesTriggered++;
            spawnWave(world, aggroTarget);
        }
    }

    private void spawnWave(ServerWorld world, @Nullable LivingEntity aggroTarget) {
        for (FallenCometStoneDefinition.WaveMob<?> waveMob : definition.wave()) {
            for (int i = 0; i < waveMob.count(); i++) {
                spawnTracked(world, waveMob, aggroTarget);
            }
        }
    }

    private <T extends MobEntity> void spawnTracked(ServerWorld world,
            FallenCometStoneDefinition.WaveMob<T> spec, @Nullable LivingEntity aggroTarget) {
        T mob = spec.type().spawn(world, spawned -> {
            if (spec.customizer() != null) {
                spec.customizer().accept(spawned);
            }
            if (aggroTarget != null) {
                spawned.setTarget(aggroTarget);
                if (spawned instanceof Angerable angerable) {
                    angerable.setAngerDuration(WAVE_MOB_ANGER_TICKS);
                    angerable.setAngryAt(LazyEntityReference.of(aggroTarget));
                }
            }
        }, randomNearbyGroundPos(), SpawnReason.REINFORCEMENT, false, false);
        if (mob != null) {
            spawnedMobIds.add(mob.getUuid());
        }
    }

    private BlockPos randomNearbyGroundPos() {
        double angle = this.random.nextDouble() * Math.PI * 2.0;
        double radius = SPAWN_RADIUS_MIN + this.random.nextDouble() * (SPAWN_RADIUS_MAX - SPAWN_RADIUS_MIN);
        double x = getX() + Math.cos(angle) * radius;
        double z = getZ() + Math.sin(angle) * radius;
        return BlockPos.ofFloored(x, getY(), z);
    }

    @Override
    public void onDeath(DamageSource damageSource) {
        super.onDeath(damageSource);
        if (getEntityWorld() instanceof ServerWorld serverWorld) {
            killSpawnedMobs(serverWorld);
        }
    }

    private void killSpawnedMobs(ServerWorld world) {
        for (UUID mobId : spawnedMobIds) {
            if (world.getEntity(mobId) instanceof LivingEntity mob && mob.isAlive()) {
                mob.kill(world);
            }
        }
        spawnedMobIds.clear();
    }

    @Override
    protected void dropLoot(ServerWorld world, DamageSource damageSource, boolean causedByPlayer) {
        for (ItemStack stack : definition.drops().get()) {
            this.dropStack(world, stack);
        }
    }

    /** Cosmetic ambient particles (client-side only), if the definition has any. */
    @Override
    public void tickMovement() {
        super.tickMovement();
        if (definition.ambientParticle() != null && getEntityWorld().isClient()) {
            for (int i = 0; i < 2; i++) {
                double x = getX() + (this.random.nextDouble() - 0.5) * 2.5;
                double y = getY() + this.random.nextDouble() * 2.5;
                double z = getZ() + (this.random.nextDouble() - 0.5) * 2.5;
                getEntityWorld().addParticleClient(definition.ambientParticle(), x, y, z, 0.0, 0.02, 0.0);
            }
        }
    }

    /** A fallen comet stone cannot move - no walking, no gravity, no knockback drift. */
    @Override
    public void travel(Vec3d movementInput) {
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean canImmediatelyDespawn(double distanceSquared) {
        return false;
    }

    @Override
    public int getMonsterLevel() {
        return definition.level();
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>("main", 0,
                test -> test.setAndContinue(FallenCometStoneAnimations.IDLE)));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.geoCache;
    }
}
