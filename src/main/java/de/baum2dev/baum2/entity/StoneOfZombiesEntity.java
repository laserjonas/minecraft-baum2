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
import net.minecraft.entity.mob.ZombieEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import de.baum2dev.baum2.registry.ModItems;

/**
 * A stationary mini-boss: a giant cocoon stone (same shape/size as Stone of Spiders, see
 * HulkingCocoonStoneEntityModel) infested with zombies instead of spiders. Every 10% of max
 * health lost spawns a wave of 2 zombies + 1 baby zombie near itself; killing the stone kills
 * every zombie it has spawned so far. Drops a Poison Dagger on death.
 */
public class StoneOfZombiesEntity extends HostileEntity implements MonsterLevelProvider {
    private static final int LEVEL = 20;
    private static final float HEALTH_STEP_RATIO = 0.10F;
    private static final int ZOMBIES_PER_WAVE = 2;
    private static final int BABY_ZOMBIES_PER_WAVE = 1;
    private static final double SPAWN_RADIUS_MIN = 2.0;
    private static final double SPAWN_RADIUS_MAX = 4.0;

    private int zombieWavesTriggered = 0;
    private final Set<UUID> spawnedZombieIds = new HashSet<>();

    public StoneOfZombiesEntity(EntityType<? extends HostileEntity> entityType, World world) {
        super(entityType, world);
    }

    public static DefaultAttributeContainer.Builder createStoneOfZombiesAttributes() {
        return HostileEntity.createHostileAttributes()
                .add(EntityAttributes.MAX_HEALTH, 400.0)
                .add(EntityAttributes.MOVEMENT_SPEED, 0.0)
                .add(EntityAttributes.KNOCKBACK_RESISTANCE, 1.0);
    }

    @Override
    public boolean damage(ServerWorld world, DamageSource source, float amount) {
        boolean damaged = super.damage(world, source, amount);
        if (damaged) {
            trySpawnZombieWaves(world);
        }
        return damaged;
    }

    /** Spawns one wave of zombies per full 10%-of-max-health increment lost, cumulative. */
    private void trySpawnZombieWaves(ServerWorld world) {
        float missingHealthRatio = 1.0F - (getHealth() / getMaxHealth());
        int thresholdsReached = (int) Math.floor(missingHealthRatio / HEALTH_STEP_RATIO + 1.0e-4);
        while (zombieWavesTriggered < thresholdsReached && isAlive()) {
            zombieWavesTriggered++;
            spawnZombieWave(world);
        }
    }

    private void spawnZombieWave(ServerWorld world) {
        for (int i = 0; i < ZOMBIES_PER_WAVE; i++) {
            spawnTrackedZombie(world, false);
        }
        for (int i = 0; i < BABY_ZOMBIES_PER_WAVE; i++) {
            spawnTrackedZombie(world, true);
        }
    }

    private void spawnTrackedZombie(ServerWorld world, boolean baby) {
        ZombieEntity zombie = EntityType.ZOMBIE.spawn(
                world, entity -> entity.setBaby(baby), randomNearbyGroundPos(), SpawnReason.REINFORCEMENT, false, false);
        if (zombie != null) {
            spawnedZombieIds.add(zombie.getUuid());
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
            killSpawnedZombies(serverWorld);
        }
    }

    private void killSpawnedZombies(ServerWorld world) {
        for (UUID zombieId : spawnedZombieIds) {
            if (world.getEntity(zombieId) instanceof LivingEntity zombie && zombie.isAlive()) {
                zombie.kill(world);
            }
        }
        spawnedZombieIds.clear();
    }

    @Override
    protected void dropLoot(ServerWorld world, DamageSource damageSource, boolean causedByPlayer) {
        this.dropStack(world, new ItemStack(ModItems.POISON_DAGGER));
    }

    /** Ambient smoke drifting off the stone, client-side only - purely cosmetic. */
    @Override
    public void tickMovement() {
        super.tickMovement();
        if (getEntityWorld().isClient()) {
            for (int i = 0; i < 2; i++) {
                double x = getX() + (this.random.nextDouble() - 0.5) * 2.5;
                double y = getY() + this.random.nextDouble() * 2.5;
                double z = getZ() + (this.random.nextDouble() - 0.5) * 2.5;
                getEntityWorld().addParticleClient(ParticleTypes.LARGE_SMOKE, x, y, z, 0.0, 0.02, 0.0);
            }
        }
    }

    /** Stone of Zombies cannot move - no walking, no gravity, no knockback drift. */
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
}
