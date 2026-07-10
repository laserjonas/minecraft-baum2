package de.baum2dev.baum2.entity;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.ai.goal.ActiveTargetGoal;
import net.minecraft.entity.ai.goal.LookAroundGoal;
import net.minecraft.entity.ai.goal.LookAtEntityGoal;
import net.minecraft.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.entity.ai.goal.RevengeGoal;
import net.minecraft.entity.ai.goal.SwimGoal;
import net.minecraft.entity.ai.goal.WanderAroundFarGoal;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.mob.SilverfishEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * The Silverfish Broodcaller - the weakest of the three roaming bosses (user design, 5th
 * playtest map): an oversized silverfish guarding the WEST grand cave mouth. More health
 * and damage than a normal silverfish, and its signature passive: while it has a target,
 * it keeps calling normal silverfish out of the brood - up to {@link #MAX_BROOD_PER_LIFE}
 * per life. Brood mobs die with the caller (same cascade rule as the stones, so killing
 * the boss ends the fight and cascade kills grant no XP - the boss itself is the reward).
 *
 * <p>Visuals: vanilla silverfish model scaled 3x by the client renderer (the recovered
 * pre-GeckoLib "reskin + scale a vanilla model" approach, like vanilla's Giant) - see
 * SilverfishBroodcallerEntityRenderer. Extends HostileEntity, NOT SilverfishEntity, so it
 * inherits none of the burrow-into-blocks behavior (this boss must stay visible).
 */
public class SilverfishBroodcallerEntity extends HostileEntity implements MonsterLevelProvider {

    private static final int LEVEL = 8;
    public static final int MAX_BROOD_PER_LIFE = 20;
    private static final int BROOD_INTERVAL_TICKS = 100;  // 2 per call, every 5s in combat
    private static final int BROOD_PER_CALL = 2;

    /** In-memory like the stones' wave tracking: acceptable for a single-sitting fight. */
    private final Set<UUID> broodIds = new HashSet<>();
    private int broodSpawned;

    public SilverfishBroodcallerEntity(EntityType<? extends HostileEntity> entityType, World world) {
        super(entityType, world);
        this.experiencePoints = 0;  // custom XP comes from MobDeathHandler like every mob
    }

    public static DefaultAttributeContainer.Builder createBroodcallerAttributes() {
        return HostileEntity.createHostileAttributes()
                .add(EntityAttributes.MAX_HEALTH, 160.0)
                .add(EntityAttributes.ATTACK_DAMAGE, 5.0)
                .add(EntityAttributes.MOVEMENT_SPEED, 0.3)
                // Modest (below both stronger sibling bosses' effective values) - this IS
                // the weakest boss, kiting should work against it (balance-reviewer).
                .add(EntityAttributes.KNOCKBACK_RESISTANCE, 0.3)
                .add(EntityAttributes.FOLLOW_RANGE, 32.0);
    }

    @Override
    protected void initGoals() {
        this.goalSelector.add(1, new SwimGoal(this));
        this.goalSelector.add(2, new MeleeAttackGoal(this, 1.1, false));
        this.goalSelector.add(4, new WanderAroundFarGoal(this, 0.7));
        this.goalSelector.add(5, new LookAtEntityGoal(this, PlayerEntity.class, 10.0F));
        this.goalSelector.add(5, new LookAroundGoal(this));
        this.targetSelector.add(1, new RevengeGoal(this));
        this.targetSelector.add(2, new ActiveTargetGoal<>(this, PlayerEntity.class, true));
    }

    @Override
    public void tick() {
        super.tick();
        if (!(getEntityWorld() instanceof ServerWorld world)) {
            return;
        }
        LivingEntity target = getTarget();
        if (target != null && broodSpawned < MAX_BROOD_PER_LIFE && this.age % BROOD_INTERVAL_TICKS == 0) {
            callBrood(world, target);
        }
    }

    private void callBrood(ServerWorld world, LivingEntity target) {
        int count = Math.min(BROOD_PER_CALL, MAX_BROOD_PER_LIFE - broodSpawned);
        for (int i = 0; i < count; i++) {
            double angle = this.random.nextDouble() * Math.PI * 2.0;
            double radius = 2.0 + this.random.nextDouble() * 3.0;
            BlockPos pos = BlockPos.ofFloored(
                    getX() + Math.cos(angle) * radius, getY(), getZ() + Math.sin(angle) * radius);
            SilverfishEntity brood = EntityType.SILVERFISH.spawn(world,
                    spawned -> spawned.setTarget(target), pos, SpawnReason.REINFORCEMENT, false, false);
            if (brood != null) {
                broodIds.add(brood.getUuid());
                broodSpawned++;
            }
        }
    }

    @Override
    public void onDeath(DamageSource damageSource) {
        super.onDeath(damageSource);
        if (getEntityWorld() instanceof ServerWorld world) {
            for (UUID broodId : broodIds) {
                if (world.getEntity(broodId) instanceof LivingEntity brood && brood.isAlive()) {
                    brood.kill(world);
                }
            }
            broodIds.clear();
        }
    }

    @Override
    protected void dropLoot(ServerWorld world, DamageSource damageSource, boolean causedByPlayer) {
        // "Paid in the monster's own currency" like the stone family - a modest guaranteed
        // drop so the weakest boss isn't the only one paying zero loot (balance-reviewer).
        this.dropStack(world, new net.minecraft.item.ItemStack(net.minecraft.item.Items.IRON_INGOT, 2));
        this.dropStack(world, new net.minecraft.item.ItemStack(net.minecraft.item.Items.IRON_NUGGET, 12));
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
