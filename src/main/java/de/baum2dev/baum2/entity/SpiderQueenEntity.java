package de.baum2dev.baum2.entity;

import java.util.EnumSet;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.ActiveTargetGoal;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.ai.goal.LookAroundGoal;
import net.minecraft.entity.ai.goal.LookAtEntityGoal;
import net.minecraft.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.entity.ai.goal.RevengeGoal;
import net.minecraft.entity.ai.goal.SwimGoal;
import net.minecraft.entity.ai.goal.WanderAroundFarGoal;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.mob.SpiderEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import de.baum2dev.baum2.registry.ModItems;

/**
 * A mobile boss (unlike the stationary "Stone of" mini-bosses): a giant, mutated spider that
 * fights with a fast melee bite plus a long-range leap attack that closes almost any gap,
 * making kiting/fleeing far less effective than against a normal mob. Extends vanilla
 * SpiderEntity directly (not HostileEntity) to inherit wall-climbing, the spider model/
 * animation, and SpiderNavigation for free - only the goals, attributes, and drop are
 * overridden. Visual size (3x a normal spider) is baked into the registered EntityType's
 * dimensions plus a ModelTransformer.scaling(3.0F) applied to the shared vanilla spider model
 * at model-layer registration - the same two-part mechanism vanilla's own GiantEntity uses to
 * look 6x a zombie (confirmed via decompiled EntityModels.java - EntityAttributes.SCALE is a
 * different, newer per-instance mechanism, not what Giant/vanilla actually uses for this).
 */
public class SpiderQueenEntity extends SpiderEntity implements MonsterLevelProvider {
    private static final int LEVEL = 15;

    /** How long the telegraphed wind-up before a leap lasts, in ticks - shared with the
     *  client-side model so its crouch pose interpolates over the exact same window. */
    public static final int LEAP_WINDUP_DURATION_TICKS = 15;

    private static final TrackedData<Integer> LEAP_WINDUP_TICKS =
            DataTracker.registerData(SpiderQueenEntity.class, TrackedDataHandlerRegistry.INTEGER);

    private int leapCooldownTicks = 0;

    public SpiderQueenEntity(EntityType<? extends SpiderEntity> entityType, World world) {
        super(entityType, world);
    }

    public static DefaultAttributeContainer.Builder createSpiderQueenAttributes() {
        return SpiderEntity.createSpiderAttributes()
                .add(EntityAttributes.MAX_HEALTH, 350.0)
                .add(EntityAttributes.ATTACK_DAMAGE, 10.0);
    }

    @Override
    protected void initGoals() {
        this.goalSelector.add(1, new SwimGoal(this));
        this.goalSelector.add(2, new LeapAttackGoal(this));
        this.goalSelector.add(3, new QueenMeleeAttackGoal(this));
        this.goalSelector.add(4, new WanderAroundFarGoal(this, 0.8));
        this.goalSelector.add(5, new LookAtEntityGoal(this, PlayerEntity.class, 8.0F));
        this.goalSelector.add(5, new LookAroundGoal(this));
        this.targetSelector.add(1, new RevengeGoal(this));
        this.targetSelector.add(2, new ActiveTargetGoal<>(this, PlayerEntity.class, true));
    }

    @Override
    protected void initDataTracker(DataTracker.Builder builder) {
        super.initDataTracker(builder);
        builder.add(LEAP_WINDUP_TICKS, 0);
    }

    /** How many wind-up ticks remain before her next leap launches (0 = not preparing one) -
     *  read client-side to drive the crouch/coil pose in SpiderQueenEntityModel. */
    public int getLeapWindupTicks() {
        return this.dataTracker.get(LEAP_WINDUP_TICKS);
    }

    private void setLeapWindupTicks(int ticks) {
        this.dataTracker.set(LEAP_WINDUP_TICKS, ticks);
    }

    @Override
    public void tick() {
        super.tick();
        if (getEntityWorld().isClient()) {
            spawnMutantAuraParticles();
        } else if (leapCooldownTicks > 0) {
            leapCooldownTicks--;
        }
    }

    /** Continuous green toxic-smoke aura, purely cosmetic - reinforces the "mutant" look. */
    private void spawnMutantAuraParticles() {
        for (int i = 0; i < 2; i++) {
            double x = getX() + (this.random.nextDouble() - 0.5) * getWidth();
            double y = getY() + this.random.nextDouble() * getHeight();
            double z = getZ() + (this.random.nextDouble() - 0.5) * getWidth();
            getEntityWorld().addParticleClient(ParticleTypes.WITCH, x, y, z, 0.0, 0.02, 0.0);
        }
    }

    @Override
    protected void dropLoot(ServerWorld world, DamageSource damageSource, boolean causedByPlayer) {
        this.dropStack(world, new ItemStack(ModItems.QUEEN_SPIDER_HELMET));
        this.dropStack(world, new ItemStack(ModItems.QUEEN_SPIDER_CHESTPLATE));
        this.dropStack(world, new ItemStack(ModItems.QUEEN_SPIDER_LEGGINGS));
        this.dropStack(world, new ItemStack(ModItems.QUEEN_SPIDER_BOOTS));
    }

    @Override
    public int getMonsterLevel() {
        return LEVEL;
    }

    /**
     * Fast melee bite, 2 attacks/sec (10-tick cooldown) instead of MeleeAttackGoal's default
     * 1/sec. MeleeAttackGoal's own cooldown field is private with no protected setter, so this
     * shadows it with an independent counter rather than fighting the base class for access -
     * resetCooldown()/isCooledDown() are still called polymorphically by the inherited
     * attack()/canAttack(), so this is a clean override, not a hack.
     */
    private static class QueenMeleeAttackGoal extends MeleeAttackGoal {
        private static final int ATTACK_COOLDOWN_TICKS = 10;
        private int attackCooldownTicks;

        QueenMeleeAttackGoal(SpiderQueenEntity queen) {
            super(queen, 1.0, true);
        }

        @Override
        public void start() {
            super.start();
            this.attackCooldownTicks = 0;
        }

        @Override
        public void tick() {
            super.tick();
            if (this.attackCooldownTicks > 0) {
                this.attackCooldownTicks--;
            }
        }

        @Override
        protected void resetCooldown() {
            this.attackCooldownTicks = ATTACK_COOLDOWN_TICKS;
        }

        @Override
        protected boolean isCooledDown() {
            return this.attackCooldownTicks <= 0;
        }
    }

    /**
     * The signature "prepare, then jump 12 blocks fast, 7s cooldown, 75 damage on hit" attack.
     * Two phases: a telegraphed wind-up (movement/pathing frozen, synced to the client via
     * {@link #LEAP_WINDUP_TICKS} so SpiderQueenEntityModel can play a real crouch/coil pose -
     * "prepare to jump" - instead of just standing there), then the leap itself. Triggers when
     * the target is out of melee range but within leap range and off cooldown.
     *
     * Two real bugs from the first pass are fixed here: (1) the leap only ever aimed once at
     * launch, so a single sidestep beat it every time - now re-aims once more mid-flight, and
     * the whole wind-up phase keeps re-orienting toward the target too; (2) residual vanilla AI
     * movement (pathfinding toward the target, still active from whichever lower-priority goal
     * was running the instant before this one preempts it) could fight the velocity impulse -
     * navigation is now explicitly stopped both at wind-up start and every wind-up tick.
     */
    private static class LeapAttackGoal extends Goal {
        private static final double MIN_RANGE = 4.0;
        private static final double MAX_RANGE = 12.0;
        private static final double MAX_VERTICAL_GAP = 4.0;
        private static final int COOLDOWN_TICKS = 140;
        private static final int LEAP_DURATION_TICKS = 20;
        private static final float LEAP_DAMAGE = 75.0F;
        private static final double LEAP_HORIZONTAL_SPEED = 1.1;
        private static final double LEAP_VERTICAL_SPEED = 0.4;

        private final SpiderQueenEntity queen;
        private int windupTicksRemaining;
        private int leapTicksRemaining;
        private boolean hasDealtDamageThisLeap;

        LeapAttackGoal(SpiderQueenEntity queen) {
            this.queen = queen;
            this.setControls(EnumSet.of(Goal.Control.MOVE, Goal.Control.JUMP, Goal.Control.LOOK));
        }

        @Override
        public boolean canStart() {
            if (queen.leapCooldownTicks > 0) {
                return false;
            }
            LivingEntity target = queen.getTarget();
            if (target == null || !target.isAlive()) {
                return false;
            }
            // Horizontal/vertical checked separately, not raw 3D distance - an elevated target
            // 10 blocks up should not burn the cooldown on a leap that can't physically reach
            // them (the leap's own vertical speed only covers a few blocks of height).
            double horizontalDistance = horizontalDistanceTo(target);
            double verticalDistance = Math.abs(target.getY() - queen.getY());
            return horizontalDistance >= MIN_RANGE && horizontalDistance <= MAX_RANGE
                    && verticalDistance <= MAX_VERTICAL_GAP;
        }

        @Override
        public boolean shouldContinue() {
            LivingEntity target = queen.getTarget();
            return (windupTicksRemaining > 0 || leapTicksRemaining > 0) && target != null && target.isAlive();
        }

        @Override
        public void start() {
            queen.getNavigation().stop();
            windupTicksRemaining = LEAP_WINDUP_DURATION_TICKS;
            leapTicksRemaining = 0;
            hasDealtDamageThisLeap = false;
            queen.setLeapWindupTicks(windupTicksRemaining);
        }

        @Override
        public void stop() {
            windupTicksRemaining = 0;
            leapTicksRemaining = 0;
            queen.setLeapWindupTicks(0);
        }

        @Override
        public void tick() {
            LivingEntity target = queen.getTarget();
            if (target == null) {
                return;
            }
            queen.getLookControl().lookAt(target, 90.0F, 90.0F);

            if (windupTicksRemaining > 0) {
                // Keep her fully planted while telegraphing - no drifting toward the target
                // during wind-up, the leap itself covers the distance.
                queen.getNavigation().stop();
                windupTicksRemaining--;
                queen.setLeapWindupTicks(windupTicksRemaining);
                if (windupTicksRemaining == 0) {
                    launch(target);
                }
                return;
            }

            leapTicksRemaining--;
            if (leapTicksRemaining == LEAP_DURATION_TICKS / 2 && target.isAlive()) {
                aimAt(target);
            }
            if (!hasDealtDamageThisLeap && target.isAlive()
                    && queen.getBoundingBox().expand(0.3).intersects(target.getBoundingBox())) {
                target.damage(getServerWorld(queen), queen.getDamageSources().mobAttack(queen), LEAP_DAMAGE);
                hasDealtDamageThisLeap = true;
            }
        }

        private void launch(LivingEntity target) {
            queen.getNavigation().stop();
            aimAt(target);
            leapTicksRemaining = LEAP_DURATION_TICKS;
            queen.leapCooldownTicks = COOLDOWN_TICKS;
        }

        private void aimAt(LivingEntity target) {
            Vec3d direction = new Vec3d(target.getX() - queen.getX(), 0.0, target.getZ() - queen.getZ());
            if (direction.lengthSquared() > 1.0E-7) {
                direction = direction.normalize().multiply(LEAP_HORIZONTAL_SPEED);
            }
            queen.setVelocity(direction.x, LEAP_VERTICAL_SPEED, direction.z);
            queen.velocityDirty = true;
        }

        private double horizontalDistanceTo(LivingEntity target) {
            double dx = target.getX() - queen.getX();
            double dz = target.getZ() - queen.getZ();
            return Math.sqrt(dx * dx + dz * dz);
        }
    }
}
