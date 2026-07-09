package de.baum2dev.baum2.entity;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.mob.SpiderEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import de.baum2dev.baum2.registry.ModItems;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animatable.manager.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * A stationary mini-boss: a fallen comet stone crawling with spider energy - it cannot move.
 * Every time it loses another 10% of its max health it spawns a wave of 3 spiders near
 * itself; killing the stone kills every spider it has spawned so far. Drops a Gold Sword on
 * death. Rendered via the shared GeckoLib fallen-comet-stone template (one geometry/idle
 * animation for every stone boss, per-stone texture - see FallenCometStoneAnimations).
 */
public class StoneOfSpidersEntity extends HostileEntity implements MonsterLevelProvider, GeoEntity {
    private static final int LEVEL = 10;
    private static final float HEALTH_STEP_RATIO = 0.10F;
    private static final int SPIDERS_PER_WAVE = 3;
    private static final double SPAWN_RADIUS_MIN = 2.0;
    private static final double SPAWN_RADIUS_MAX = 4.0;

    private int spiderWavesTriggered = 0;
    private final Set<UUID> spawnedSpiderIds = new HashSet<>();

    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);

    public StoneOfSpidersEntity(EntityType<? extends HostileEntity> entityType, World world) {
        super(entityType, world);
    }

    public static DefaultAttributeContainer.Builder createStoneOfSpidersAttributes() {
        return HostileEntity.createHostileAttributes()
                .add(EntityAttributes.MAX_HEALTH, 200.0)
                .add(EntityAttributes.MOVEMENT_SPEED, 0.0)
                .add(EntityAttributes.KNOCKBACK_RESISTANCE, 1.0);
    }

    @Override
    public boolean damage(ServerWorld world, DamageSource source, float amount) {
        boolean damaged = super.damage(world, source, amount);
        if (damaged) {
            trySpawnSpiderWaves(world);
        }
        return damaged;
    }

    /** Spawns one wave of spiders per full 10%-of-max-health increment lost, cumulative. */
    private void trySpawnSpiderWaves(ServerWorld world) {
        float missingHealthRatio = 1.0F - (getHealth() / getMaxHealth());
        int thresholdsReached = (int) Math.floor(missingHealthRatio / HEALTH_STEP_RATIO + 1.0e-4);
        while (spiderWavesTriggered < thresholdsReached && isAlive()) {
            spiderWavesTriggered++;
            spawnSpiderWave(world);
        }
    }

    private void spawnSpiderWave(ServerWorld world) {
        for (int i = 0; i < SPIDERS_PER_WAVE; i++) {
            SpiderEntity spider = EntityType.SPIDER.spawn(world, randomNearbyGroundPos(), SpawnReason.REINFORCEMENT);
            if (spider != null) {
                spawnedSpiderIds.add(spider.getUuid());
            }
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
            killSpawnedSpiders(serverWorld);
        }
    }

    private void killSpawnedSpiders(ServerWorld world) {
        for (UUID spiderId : spawnedSpiderIds) {
            if (world.getEntity(spiderId) instanceof LivingEntity spider && spider.isAlive()) {
                spider.kill(world);
            }
        }
        spawnedSpiderIds.clear();
    }

    @Override
    protected void dropLoot(ServerWorld world, DamageSource damageSource, boolean causedByPlayer) {
        this.dropStack(world, new ItemStack(ModItems.GOLD_SWORD));
    }

    /** Stone of Spiders cannot move - no walking, no gravity, no knockback drift. */
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
        return LEVEL;
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
