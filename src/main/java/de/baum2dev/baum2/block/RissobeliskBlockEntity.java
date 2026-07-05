package de.baum2dev.baum2.block;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.mob.SilverfishEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.util.math.BlockPos;
import de.baum2dev.baum2.events.LevelUpHandler;
import de.baum2dev.baum2.progression.PlayerLevelSystem;
import de.baum2dev.baum2.registry.ModBlockEntities;
import de.baum2dev.baum2.registry.ModItems;

/**
 * Tracks a Rissobelisk's own hit-point pool, wave-spawn thresholds, and spawned-mob cascade -
 * direct structural port of {@code entity.StoneOfSpidersEntity}'s own fields/logic, since a
 * Block has no built-in health/damage() to hook into (see docs/fabric-modding.md "Custom
 * Blocks and BlockEntitys"). Plain fields, not the Attachment API - this is a from-scratch
 * BlockEntity this project owns, so there's no persistence-safety win to reach for it here.
 */
public class RissobeliskBlockEntity extends BlockEntity {
    public static final int MAX_HEALTH = 200;
    private static final float HEALTH_STEP_RATIO = 0.10F;
    private static final int SILVERFISH_PER_WAVE = 3;
    private static final double SPAWN_RADIUS_MIN = 2.0;
    private static final double SPAWN_RADIUS_MAX = 4.0;
    private static final long XP_REWARD = 10L + MAX_HEALTH / 2; // matches events.MobDeathHandler's own per-mob formula

    private int health = MAX_HEALTH;
    private int wavesTriggered = 0;
    private final Set<UUID> spawnedMobIds = new HashSet<>();

    public RissobeliskBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.RISSOBELISK, pos, state);
    }

    /** Applies damage, spawns any newly-crossed wave thresholds, and destroys+rewards the attacker once health hits 0. */
    public void damage(ServerWorld world, ServerPlayerEntity attacker, float amount) {
        if (this.health <= 0) {
            return;
        }

        this.health = Math.max(0, this.health - Math.round(amount));
        markDirty();
        trySpawnWaves(world);

        if (this.health <= 0) {
            destroyAndReward(world, attacker);
        }
    }

    /** Spawns one wave of Silverfish per full 10%-of-max-health increment lost, cumulative. */
    private void trySpawnWaves(ServerWorld world) {
        float missingHealthRatio = 1.0F - ((float) this.health / MAX_HEALTH);
        int thresholdsReached = (int) Math.floor(missingHealthRatio / HEALTH_STEP_RATIO + 1.0e-4);
        while (this.wavesTriggered < thresholdsReached && this.health > 0) {
            this.wavesTriggered++;
            spawnWave(world);
        }
    }

    private void spawnWave(ServerWorld world) {
        for (int i = 0; i < SILVERFISH_PER_WAVE; i++) {
            SilverfishEntity silverfish = EntityType.SILVERFISH.spawn(world, randomNearbyGroundPos(world), SpawnReason.REINFORCEMENT);
            if (silverfish != null) {
                spawnedMobIds.add(silverfish.getUuid());
            }
        }
    }

    private BlockPos randomNearbyGroundPos(ServerWorld world) {
        double angle = world.getRandom().nextDouble() * Math.PI * 2.0;
        double radius = SPAWN_RADIUS_MIN + world.getRandom().nextDouble() * (SPAWN_RADIUS_MAX - SPAWN_RADIUS_MIN);
        double x = getPos().getX() + 0.5 + Math.cos(angle) * radius;
        double z = getPos().getZ() + 0.5 + Math.sin(angle) * radius;
        return BlockPos.ofFloored(x, getPos().getY(), z);
    }

    private void destroyAndReward(ServerWorld world, ServerPlayerEntity attacker) {
        killSpawnedMobs(world);
        world.removeBlock(getPos(), false);

        PlayerLevelSystem.addExperience(attacker, XP_REWARD);
        LevelUpHandler.checkLevelUp(attacker);
        Block.dropStack(world, getPos(), new ItemStack(ModItems.RISSSPLITTER));
    }

    private void killSpawnedMobs(ServerWorld world) {
        for (UUID id : spawnedMobIds) {
            if (world.getEntity(id) instanceof LivingEntity mob && mob.isAlive()) {
                mob.kill(world);
            }
        }
        spawnedMobIds.clear();
    }

    @Override
    protected void readData(ReadView view) {
        super.readData(view);
        this.health = view.getInt("health", MAX_HEALTH);
        this.wavesTriggered = view.getInt("waves_triggered", 0);
    }

    @Override
    protected void writeData(WriteView view) {
        super.writeData(view);
        view.putInt("health", this.health);
        view.putInt("waves_triggered", this.wavesTriggered);
    }
}
