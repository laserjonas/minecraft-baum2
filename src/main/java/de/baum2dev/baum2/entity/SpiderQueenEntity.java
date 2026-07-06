package de.baum2dev.baum2.entity;

import java.util.EnumSet;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.MovementType;
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
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import de.baum2dev.baum2.registry.ModItems;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animatable.manager.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.animation.object.PlayState;
import software.bernie.geckolib.animation.state.AnimationTest;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * A mobile boss (unlike the stationary "Stone of" mini-bosses): a giant, mutated spider that
 * fights with a fast melee bite plus a long-range leap attack that closes almost any gap,
 * making kiting/fleeing far less effective than against a normal mob. Extends vanilla
 * SpiderEntity directly (not HostileEntity) to inherit wall-climbing and SpiderNavigation for
 * free - only the goals, attributes, and drop are overridden. Visual size (3x a normal spider)
 * is baked into the registered EntityType's dimensions plus a GeoEntityRenderer.withScale(3.0F)
 * on the GeckoLib renderer (see SpiderQueenEntityRenderer) - the model/animation itself is now
 * GeckoLib-driven (SpiderQueenGeoModel/spider_queen.geo.json/animation.json), not a reused
 * vanilla SpiderEntityModel, following this project's GeckoLib migration (see
 * docs/fabric-modding.md's "GeckoLib integration" section). GeckoLib is a rendering/animation
 * library only - the leap's actual trajectory math below is completely unaffected by this and
 * stays exactly as it was.
 */
public class SpiderQueenEntity extends SpiderEntity implements MonsterLevelProvider, GeoEntity {
    private static final int LEVEL = 15;

    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);

    private static final RawAnimation IDLE_ANIM = RawAnimation.begin().thenLoop("animation.spider_queen.idle");
    private static final RawAnimation WALK_ANIM = RawAnimation.begin().thenLoop("animation.spider_queen.walk");
    private static final RawAnimation LEAP_WINDUP_ANIM =
            RawAnimation.begin().thenPlay("animation.spider_queen.leap_windup");
    private static final RawAnimation LEAP_FLIGHT_ANIM =
            RawAnimation.begin().thenPlay("animation.spider_queen.leap_flight");

    /** How long the telegraphed wind-up before a leap lasts, in ticks - shared with the
     *  client-side model so its crouch pose interpolates over the exact same window. */
    public static final int LEAP_WINDUP_DURATION_TICKS = 15;

    // Precomputed per-tick trajectory tables (see LeapAttackGoal's javadoc for how these were
    // derived) - index 0 is the first flight tick, last index is the tick she lands on.
    // HEIGHTS are absolute Y offsets in blocks; FRACTIONS are cumulative horizontal progress,
    // normalized so the final entry is always 1.0 (100% of that leap's actual horizontal
    // distance, whatever it is at launch time - see performLeapFlightStep()).
    private static final double[] HORIZONTAL_HEIGHTS = {
            0.5450, 1.0007, 1.3689, 1.6513, 1.8497, 1.9657, 2.0010, 1.9572,
            1.8358, 1.6385, 1.3667, 1.0220, 0.6058, 0.1194, -0.4356
    };
    private static final double[] HORIZONTAL_FRACTIONS = {
            0.1189, 0.2271, 0.3255, 0.4151, 0.4967, 0.5709, 0.6384, 0.6998,
            0.7557, 0.8066, 0.8529, 0.8950, 0.9334, 0.9683, 1.0000
    };
    private static final double[] VERTICAL_HEIGHTS = {
            1.4905, 2.8728, 4.1490, 5.3214, 6.3918, 7.3625, 8.2353, 9.0123,
            9.6954, 10.2864, 10.7872, 11.1995, 11.5252, 11.7660, 11.9236, 11.9996,
            11.9957, 11.9135, 11.7545, 11.5204, 11.2124, 10.8323, 10.3814, 9.8610,
            9.2727, 8.6177, 7.8975, 7.1132, 6.2663, 5.3579, 4.3892, 3.3615,
            2.2760, 1.1338, -0.0640
    };
    private static final double[] VERTICAL_FRACTIONS = {
            0.0934, 0.1785, 0.2559, 0.3263, 0.3904, 0.4487, 0.5017, 0.5500,
            0.5940, 0.6339, 0.6703, 0.7034, 0.7336, 0.7610, 0.7860, 0.8087,
            0.8293, 0.8481, 0.8652, 0.8808, 0.8950, 0.9079, 0.9196, 0.9303,
            0.9400, 0.9489, 0.9569, 0.9642, 0.9709, 0.9769, 0.9825, 0.9875,
            0.9921, 0.9962, 1.0000
    };

    private static final TrackedData<Integer> LEAP_WINDUP_TICKS =
            DataTracker.registerData(SpiderQueenEntity.class, TrackedDataHandlerRegistry.INTEGER);
    /** Synced so the client's GeckoLib animation predicate can tell "telegraphing" apart from
     *  "actually airborne" - the old hand-coded model only ever needed the wind-up counter, but
     *  a real leap_flight animation (see spider_queen.animation.json) needs its own signal since
     *  leapFlightTicksRemaining itself is server-only physics state, never synced. */
    private static final TrackedData<Boolean> LEAP_FLIGHT_ACTIVE =
            DataTracker.registerData(SpiderQueenEntity.class, TrackedDataHandlerRegistry.BOOLEAN);

    private int leapCooldownTicks = 0;

    // Leap flight state - owned by the entity (not the goal) because it's driven from an
    // override of travel() itself, see the class javadoc on LeapAttackGoal for why.
    private int leapFlightTicksRemaining = 0;
    private int leapFlightTotalTicks = 0;
    private double leapFlightHorizontalDistance = 0.0;
    private boolean leapFlightVerticalProfile = false;
    private double leapFlightPrevFraction = 0.0;
    private double leapFlightPrevHeight = 0.0;
    private Vec3d leapFlightDirection = Vec3d.ZERO;

    public SpiderQueenEntity(EntityType<? extends SpiderEntity> entityType, World world) {
        super(entityType, world);
    }

    public static DefaultAttributeContainer.Builder createSpiderQueenAttributes() {
        return SpiderEntity.createSpiderAttributes()
                .add(EntityAttributes.MAX_HEALTH, 350.0)
                .add(EntityAttributes.ATTACK_DAMAGE, 10.0)
                .add(EntityAttributes.MOVEMENT_SPEED, 0.4);
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
        builder.add(LEAP_FLIGHT_ACTIVE, false);
    }

    /** How many wind-up ticks remain before her next leap launches (0 = not preparing one) -
     *  read client-side by the leap AnimationController to select the leap_windup animation. */
    public int getLeapWindupTicks() {
        return this.dataTracker.get(LEAP_WINDUP_TICKS);
    }

    private void setLeapWindupTicks(int ticks) {
        this.dataTracker.set(LEAP_WINDUP_TICKS, ticks);
    }

    /** Whether she's currently airborne mid-leap - read client-side by the leap
     *  AnimationController to select the leap_flight animation. See {@link #LEAP_FLIGHT_ACTIVE}. */
    public boolean isLeapFlightActive() {
        return this.dataTracker.get(LEAP_FLIGHT_ACTIVE);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.geoCache;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>("leap_controller", 5, this::handleAnimationState));
    }

    private PlayState handleAnimationState(AnimationTest<SpiderQueenEntity> test) {
        if (test.animatable().getLeapWindupTicks() > 0) {
            return test.setAndContinue(LEAP_WINDUP_ANIM);
        }
        if (test.animatable().isLeapFlightActive()) {
            return test.setAndContinue(LEAP_FLIGHT_ANIM);
        }
        if (test.isMoving()) {
            return test.setAndContinue(WALK_ANIM);
        }
        return test.setAndContinue(IDLE_ANIM);
    }

    /**
     * Starts a directly-driven leap: exact horizontal distance and height come from a
     * precomputed per-tick table (see the static arrays above), applied via {@link #travel}
     * every tick for as long as the flight lasts. This bypasses vanilla's velocity+gravity
     * integration entirely during the leap - a prior version set velocity once via
     * {@code setVelocity()} and trusted vanilla physics to carry her the rest of the way, but
     * in practice the leap barely covered any distance (most likely residual AI/navigation
     * state fighting the impulse in ways that were hard to pin down without a live client to
     * test against). Directly driving position tick-by-tick removes that uncertainty: whatever
     * this table says the offset should be at tick N is exactly what she moves, full stop.
     */
    void startLeapFlight(boolean verticalProfile, double horizontalDistance, Vec3d initialDirection) {
        this.leapFlightVerticalProfile = verticalProfile;
        this.leapFlightTotalTicks = verticalProfile ? VERTICAL_FRACTIONS.length : HORIZONTAL_FRACTIONS.length;
        this.leapFlightTicksRemaining = this.leapFlightTotalTicks;
        this.leapFlightHorizontalDistance = horizontalDistance;
        this.leapFlightPrevFraction = 0.0;
        this.leapFlightPrevHeight = 0.0;
        this.leapFlightDirection = initialDirection;
        this.dataTracker.set(LEAP_FLIGHT_ACTIVE, true);
    }

    void stopLeapFlight() {
        this.leapFlightTicksRemaining = 0;
        this.dataTracker.set(LEAP_FLIGHT_ACTIVE, false);
    }

    int getLeapFlightTicksRemaining() {
        return this.leapFlightTicksRemaining;
    }

    @Override
    public void travel(Vec3d movementInput) {
        if (!getEntityWorld().isClient() && leapFlightTicksRemaining > 0) {
            performLeapFlightStep();
            return;
        }
        super.travel(movementInput);
    }

    /** One tick of the directly-driven leap - see {@link #startLeapFlight} for why this exists
     *  instead of just setting velocity once. Continuously re-aims toward the target's current
     *  position every tick (not just once or twice), which is deliberate: a boss that's meant
     *  to make "escape is impossible" - and this session's whole reason for existing is a user
     *  report that she was trivially easy to kite - so the leap now actively tracks the player
     *  through its entire flight rather than committing to a single stale direction. */
    private void performLeapFlightStep() {
        int index = leapFlightTotalTicks - leapFlightTicksRemaining;
        double[] heights = leapFlightVerticalProfile ? VERTICAL_HEIGHTS : HORIZONTAL_HEIGHTS;
        double[] fractions = leapFlightVerticalProfile ? VERTICAL_FRACTIONS : HORIZONTAL_FRACTIONS;

        double heightNow = heights[index];
        double fractionNow = fractions[index];
        double deltaHeight = heightNow - leapFlightPrevHeight;
        double deltaFraction = fractionNow - leapFlightPrevFraction;
        leapFlightPrevHeight = heightNow;
        leapFlightPrevFraction = fractionNow;

        LivingEntity target = this.getTarget();
        if (target != null) {
            Vec3d toTarget = new Vec3d(target.getX() - getX(), 0.0, target.getZ() - getZ());
            if (toTarget.lengthSquared() > 1.0E-7) {
                leapFlightDirection = toTarget.normalize();
            }
        }

        double horizontalStep = leapFlightHorizontalDistance * deltaFraction;
        Vec3d delta = new Vec3d(
                leapFlightDirection.x * horizontalStep, deltaHeight, leapFlightDirection.z * horizontalStep);
        this.move(MovementType.SELF, delta);
        this.velocityDirty = true;

        leapFlightTicksRemaining--;
    }

    /** She shouldn't take fall damage from landing her own signature attack. */
    @Override
    public boolean handleFallDamage(double fallDistance, float damageMultiplier, DamageSource damageSource) {
        return false;
    }

    @Override
    public void tick() {
        super.tick();
        if (!getEntityWorld().isClient() && leapCooldownTicks > 0) {
            leapCooldownTicks--;
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
     * The signature "prepare, then jump fast and threatening" attack. Two phases: a telegraphed
     * wind-up (movement/pathing frozen, synced to the client via {@link #LEAP_WINDUP_TICKS} so
     * SpiderQueenEntityModel can play a real crouch/coil pose - "prepare to jump" - instead of
     * just standing there, plus a low ambient-sound growl for threat), then the leap itself.
     * Triggers when the target is out of melee range but within leap range and off cooldown.
     *
     * Two trajectory profiles, chosen by how elevated the target is:
     * - HORIZONTAL: target roughly level with her (or below) - peaks 2 blocks high, ~15 ticks
     *   (0.75s) flight. This is the common case and the fast, threatening one.
     * - VERTICAL: target significantly elevated (e.g. standing on a ledge) - peaks 12 blocks
     *   high, ~35 ticks (1.75s) flight. Slower by necessity (a 12-block-high arc takes longer
     *   under any real gravity model) but lets her reach up to a player who tried to escape by
     *   climbing.
     * Both profiles' *shapes* (the per-tick height/fraction tables on the entity) were derived
     * by simulating this game's actual per-tick entity physics (gravity 0.08/tick, 0.98
     * vertical / 0.91 horizontal drag per tick while airborne, confirmed against decompiled
     * LivingEntity.travelMidAir/getEffectiveGravity/EntityAttributes.GRAVITY) rather than
     * guessed - see SpiderQueenEntity's table javadoc. The actual leap execution, however, no
     * longer trusts vanilla's velocity+gravity pipeline to reproduce that shape at runtime -
     * see startLeapFlight()'s javadoc for why (the previous velocity-based version barely
     * covered any distance in practice). This goal only decides *when* to leap and *which*
     * profile to use; SpiderQueenEntity.travel() actually drives the motion every tick.
     */
    private static class LeapAttackGoal extends Goal {
        private static final double MIN_RANGE = 4.0;
        private static final double MAX_RANGE = 20.0;
        private static final double LEVEL_VERTICAL_GAP = 3.0;
        private static final double MAX_VERTICAL_REACH = 12.0;
        private static final double VERTICAL_CASE_MIN_HORIZONTAL = 1.0;
        private static final double VERTICAL_CASE_MAX_HORIZONTAL = 8.0;
        private static final int COOLDOWN_TICKS = 90;
        private static final float LEAP_DAMAGE = 75.0F;

        private final SpiderQueenEntity queen;
        private int windupTicksRemaining;
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
            double horizontalDistance = horizontalDistanceTo(target);
            double verticalGap = target.getY() - queen.getY();
            if (verticalGap > LEVEL_VERTICAL_GAP) {
                // Elevated target - only the steeper vertical profile can reach them, and only
                // within its own realistic horizontal/height envelope.
                return verticalGap <= MAX_VERTICAL_REACH
                        && horizontalDistance >= VERTICAL_CASE_MIN_HORIZONTAL
                        && horizontalDistance <= VERTICAL_CASE_MAX_HORIZONTAL;
            }
            // Roughly level, or target below her (falling the extra distance is easy) - the
            // standard fast horizontal leap. Range widened to 20 blocks (beyond the 12-block
            // trajectory the horizontal profile was originally tuned around) specifically so
            // she can still launch at a player who's kiting at range - the leap covers whatever
            // the actual distance is, it isn't capped to 12.
            return horizontalDistance >= MIN_RANGE && horizontalDistance <= MAX_RANGE;
        }

        @Override
        public boolean shouldContinue() {
            LivingEntity target = queen.getTarget();
            return (windupTicksRemaining > 0 || queen.getLeapFlightTicksRemaining() > 0)
                    && target != null && target.isAlive();
        }

        @Override
        public void start() {
            queen.getNavigation().stop();
            windupTicksRemaining = LEAP_WINDUP_DURATION_TICKS;
            hasDealtDamageThisLeap = false;
            queen.setLeapWindupTicks(windupTicksRemaining);
            // Low-pitched growl during the telegraph - a deliberate threat cue, not just a
            // cosmetic pose, per the "players should be scared by this creature" brief.
            queen.playSound(SoundEvents.ENTITY_SPIDER_AMBIENT, 1.5F, 0.5F);
        }

        @Override
        public void stop() {
            windupTicksRemaining = 0;
            queen.setLeapWindupTicks(0);
            queen.stopLeapFlight();
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

            if (queen.getLeapFlightTicksRemaining() > 0
                    && !hasDealtDamageThisLeap && target.isAlive()
                    && queen.getBoundingBox().expand(0.3).intersects(target.getBoundingBox())) {
                target.damage(getServerWorld(queen), queen.getDamageSources().mobAttack(queen), LEAP_DAMAGE);
                hasDealtDamageThisLeap = true;
            }
        }

        private void launch(LivingEntity target) {
            queen.getNavigation().stop();
            double horizontalDistance = horizontalDistanceTo(target);
            double verticalGap = target.getY() - queen.getY();
            boolean verticalLeap = verticalGap > LEVEL_VERTICAL_GAP;
            Vec3d direction = new Vec3d(target.getX() - queen.getX(), 0.0, target.getZ() - queen.getZ());
            direction = direction.lengthSquared() > 1.0E-7 ? direction.normalize() : queen.getRotationVec(1.0F);
            queen.startLeapFlight(verticalLeap, horizontalDistance, direction);
            queen.leapCooldownTicks = COOLDOWN_TICKS;
        }

        private double horizontalDistanceTo(LivingEntity target) {
            double dx = target.getX() - queen.getX();
            double dz = target.getZ() - queen.getZ();
            return Math.sqrt(dx * dx + dz * dz);
        }
    }
}
