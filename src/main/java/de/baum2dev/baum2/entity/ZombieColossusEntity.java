package de.baum2dev.baum2.entity;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.MovementType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.ai.goal.ActiveTargetGoal;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.ai.goal.LookAroundGoal;
import net.minecraft.entity.ai.goal.LookAtEntityGoal;
import net.minecraft.entity.ai.goal.RevengeGoal;
import net.minecraft.entity.ai.goal.WanderAroundFarGoal;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.ZombieEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.LocalDifficulty;
import net.minecraft.world.World;
import de.baum2dev.baum2.combat.BurnDamageManager;
import de.baum2dev.baum2.registry.ModItems;

/**
 * A mobile boss (same family as SpiderQueenEntity - a giant, 3x-scaled reskin of a vanilla
 * mob that walks/chases, not a stationary "Stone of" totem): a hulking zombie warlord fighting
 * with a slow, heavy club. Extends vanilla ZombieEntity directly (not HostileEntity) to
 * inherit the zombie model/animation and undead traits for free - only goals, attributes,
 * equipment, and drop are overridden. Visual size (3x a normal zombie) uses the same two-part
 * mechanism as Spider Queen: EntityType.Builder.dimensions() tripled plus a
 * ModelTransformer.scaling(3.0F) applied to the shared vanilla zombie model at model-layer
 * registration (see Baum2Client).
 */
public class ZombieColossusEntity extends ZombieEntity implements MonsterLevelProvider {
    private static final int LEVEL = 25;

    // --- Leap flight arc (see SpiderQueenEntity's HORIZONTAL_HEIGHTS/FRACTIONS javadoc for how
    // this style of table is derived and why direct position control, not setVelocity(), is
    // used - two rounds of velocity-based leaps failed in this exact codebase before that fix).
    // A single "fast forward-and-up" profile is enough here - no elevated-target case was
    // requested for this boss, unlike Spider Queen's two-profile leap.
    private static final double[] LEAP_HEIGHTS = {
            0.35, 0.68, 0.95, 1.15, 1.30, 1.38, 1.40, 1.35, 1.24, 1.06, 0.82, 0.51, 0.14, -0.30
    };
    private static final double[] LEAP_FRACTIONS = {
            0.09, 0.19, 0.29, 0.39, 0.48, 0.57, 0.65, 0.73, 0.80, 0.87, 0.92, 0.96, 0.99, 1.00
    };

    private int leapCooldownTicks = 0;
    private int rageCooldownTicks = 0;

    // Leap flight state - owned by the entity because it's driven from travel(), same pattern
    // as SpiderQueenEntity.
    private int leapFlightTicksRemaining = 0;
    private int leapFlightTotalTicks = 0;
    private double leapFlightHorizontalDistance = 0.0;
    private double leapFlightPrevFraction = 0.0;
    private double leapFlightPrevHeight = 0.0;
    private Vec3d leapFlightDirection = Vec3d.ZERO;

    // Fire wave state - a ground-based expanding ring triggered when the leap lands. Tracked
    // as plain fields ticked from tick(), same "state fields driven every tick" pattern as the
    // leap flight above.
    private static final double FIRE_WAVE_SPEED_PER_TICK = 5.0 / 20.0; // 5 blocks/sec
    private static final double FIRE_WAVE_MAX_RADIUS = 10.0;
    private static final double FIRE_WAVE_BAND_WIDTH = 0.6;
    private static final float FIRE_WAVE_DAMAGE = 25.0F;
    private static final int FIRE_WAVE_BURN_DURATION_TICKS = 100; // 5 seconds
    private static final float FIRE_WAVE_BURN_DAMAGE_PER_TICK = 2.0F;
    private static final int FIRE_WAVE_BURN_INTERVAL_TICKS = 20;

    private boolean fireWaveActive = false;
    private double fireWaveOriginX;
    private double fireWaveOriginY;
    private double fireWaveOriginZ;
    private double fireWaveElapsedTicks;
    private final Set<UUID> fireWaveHitPlayers = new HashSet<>();

    public ZombieColossusEntity(EntityType<? extends ZombieEntity> entityType, World world) {
        super(entityType, world);
    }

    public static DefaultAttributeContainer.Builder createZombieColossusAttributes() {
        return ZombieEntity.createZombieAttributes()
                .add(EntityAttributes.MAX_HEALTH, 750.0)
                .add(EntityAttributes.ATTACK_DAMAGE, 100.0);
    }

    /** A boss shouldn't die to sunlight-camping. */
    @Override
    protected boolean burnsInDaylight() {
        return false;
    }

    /** Always wields its signature club - not vanilla's random per-difficulty gearing. */
    @Override
    protected void initEquipment(Random random, LocalDifficulty localDifficulty) {
        this.equipStack(EquipmentSlot.MAINHAND, new ItemStack(ModItems.COLOSSAL_WARCLUB));
        // Guaranteed drop is handled explicitly in dropLoot() below - zero vanilla's own
        // independent random equipment-drop roll so it can't double-drop a second club.
        this.setEquipmentDropChance(EquipmentSlot.MAINHAND, 0.0F);
    }

    @Override
    protected void initGoals() {
        this.goalSelector.add(1, new RageAttackGoal(this));
        this.goalSelector.add(2, new LeapAttackGoal(this));
        this.goalSelector.add(3, new ColossusAttackGoal(this));
        this.goalSelector.add(4, new WanderAroundFarGoal(this, 1.0));
        this.goalSelector.add(5, new LookAtEntityGoal(this, PlayerEntity.class, 8.0F));
        this.goalSelector.add(5, new LookAroundGoal(this));
        this.targetSelector.add(1, new RevengeGoal(this));
        this.targetSelector.add(2, new ActiveTargetGoal<>(this, PlayerEntity.class, true));
    }

    /**
     * Directly-driven leap, same technique as SpiderQueenEntity.startLeapFlight() - bypasses
     * vanilla's velocity/gravity pipeline entirely and moves exactly per a precomputed table,
     * re-aiming at the live target position every tick.
     */
    void startLeapFlight(double horizontalDistance, Vec3d initialDirection) {
        this.leapFlightTotalTicks = LEAP_FRACTIONS.length;
        this.leapFlightTicksRemaining = this.leapFlightTotalTicks;
        this.leapFlightHorizontalDistance = horizontalDistance;
        this.leapFlightPrevFraction = 0.0;
        this.leapFlightPrevHeight = 0.0;
        this.leapFlightDirection = initialDirection;
    }

    void stopLeapFlight() {
        this.leapFlightTicksRemaining = 0;
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

    private void performLeapFlightStep() {
        int index = leapFlightTotalTicks - leapFlightTicksRemaining;
        double heightNow = LEAP_HEIGHTS[index];
        double fractionNow = LEAP_FRACTIONS[index];
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
        if (leapFlightTicksRemaining == 0) {
            startFireWave();
        }
    }

    /** She shouldn't take fall damage from landing her own signature attack. */
    @Override
    public boolean handleFallDamage(double fallDistance, float damageMultiplier, DamageSource damageSource) {
        return false;
    }

    private void startFireWave() {
        this.fireWaveActive = true;
        this.fireWaveOriginX = getX();
        this.fireWaveOriginY = getY();
        this.fireWaveOriginZ = getZ();
        this.fireWaveElapsedTicks = 0.0;
        this.fireWaveHitPlayers.clear();
    }

    @Override
    public void tick() {
        super.tick();
        if (!getEntityWorld().isClient()) {
            if (leapCooldownTicks > 0) {
                leapCooldownTicks--;
            }
            if (rageCooldownTicks > 0) {
                rageCooldownTicks--;
            }
            if (fireWaveActive) {
                tickFireWave();
            }
        }
    }

    private void tickFireWave() {
        if (!(getEntityWorld() instanceof ServerWorld serverWorld)) {
            return;
        }
        fireWaveElapsedTicks += 1.0;
        double radius = fireWaveElapsedTicks * FIRE_WAVE_SPEED_PER_TICK;
        if (radius >= FIRE_WAVE_MAX_RADIUS) {
            fireWaveActive = false;
            fireWaveHitPlayers.clear();
            return;
        }

        spawnFireWaveRingParticles(serverWorld, radius);

        double innerRadius = Math.max(0.0, radius - FIRE_WAVE_BAND_WIDTH);
        double outerRadius = radius + FIRE_WAVE_BAND_WIDTH;
        Box searchBox = new Box(
                fireWaveOriginX - outerRadius, fireWaveOriginY - 2.0, fireWaveOriginZ - outerRadius,
                fireWaveOriginX + outerRadius, fireWaveOriginY + 3.0, fireWaveOriginZ + outerRadius);
        for (PlayerEntity player : serverWorld.getEntitiesByClass(PlayerEntity.class, searchBox, PlayerEntity::isAlive)) {
            if (fireWaveHitPlayers.contains(player.getUuid())) {
                continue;
            }
            double dx = player.getX() - fireWaveOriginX;
            double dz = player.getZ() - fireWaveOriginZ;
            double horizontalDistance = Math.sqrt(dx * dx + dz * dz);
            if (horizontalDistance >= innerRadius && horizontalDistance <= outerRadius) {
                fireWaveHitPlayers.add(player.getUuid());
                player.damage(serverWorld, this.getDamageSources().inFire(), FIRE_WAVE_DAMAGE);
                player.setOnFireForTicks(FIRE_WAVE_BURN_DURATION_TICKS);
                BurnDamageManager.applyBurn(
                        player, FIRE_WAVE_BURN_DURATION_TICKS, FIRE_WAVE_BURN_DAMAGE_PER_TICK, FIRE_WAVE_BURN_INTERVAL_TICKS);
            }
        }
    }

    private void spawnFireWaveRingParticles(ServerWorld serverWorld, double radius) {
        int points = 24;
        for (int i = 0; i < points; i++) {
            double angle = (Math.PI * 2.0 * i) / points;
            double x = fireWaveOriginX + Math.cos(angle) * radius;
            double z = fireWaveOriginZ + Math.sin(angle) * radius;
            serverWorld.spawnParticles(ParticleTypes.FLAME, x, fireWaveOriginY + 0.1, z, 1, 0.0, 0.0, 0.0, 0.0);
        }
    }

    @Override
    protected void dropLoot(ServerWorld world, DamageSource damageSource, boolean causedByPlayer) {
        this.dropStack(world, new ItemStack(ModItems.COLOSSAL_WARCLUB));
    }

    @Override
    public int getMonsterLevel() {
        return LEVEL;
    }

    /**
     * The slow, heavy base attack - a fully custom Goal rather than a MeleeAttackGoal subclass,
     * since MeleeAttackGoal has no overridable attack-range hook in this version (confirmed via
     * javap against the real 1.21.11 jar) and the spec gives an exact 2-block range number.
     */
    private static class ColossusAttackGoal extends Goal {
        private static final double ATTACK_RANGE = 2.0;
        private static final double MOVE_SPEED = 1.0;
        private static final int ATTACK_COOLDOWN_TICKS = 40; // 0.5 attacks/sec

        private final ZombieColossusEntity colossus;
        private int cooldownTicks;

        ColossusAttackGoal(ZombieColossusEntity colossus) {
            this.colossus = colossus;
            this.setControls(EnumSet.of(Goal.Control.MOVE, Goal.Control.LOOK));
        }

        @Override
        public boolean canStart() {
            LivingEntity target = colossus.getTarget();
            return target != null && target.isAlive();
        }

        @Override
        public boolean shouldContinue() {
            return canStart();
        }

        @Override
        public void start() {
            cooldownTicks = 0;
        }

        @Override
        public void tick() {
            LivingEntity target = colossus.getTarget();
            if (target == null) {
                return;
            }
            colossus.getLookControl().lookAt(target, 30.0F, 30.0F);
            double distance = colossus.distanceTo(target);
            if (distance > ATTACK_RANGE) {
                colossus.getNavigation().startMovingTo(target, MOVE_SPEED);
            } else {
                colossus.getNavigation().stop();
            }

            if (cooldownTicks > 0) {
                cooldownTicks--;
                return;
            }
            if (distance <= ATTACK_RANGE) {
                colossus.swingHand(Hand.MAIN_HAND);
                target.damage(getServerWorld(colossus), colossus.getDamageSources().mobAttack(colossus), 100.0F);
                cooldownTicks = ATTACK_COOLDOWN_TICKS;
            }
        }
    }

    /**
     * The signature leap - a short stationary wind-up (no custom telegraph animation in v1,
     * just navigation frozen plus a growl, matching Spider Queen's "players should be scared"
     * cue) then a directly-driven jump toward the target, dealing damage on landing-contact and
     * spawning the fire wave once the flight ends (see performLeapFlightStep()).
     */
    private static class LeapAttackGoal extends Goal {
        // MIN_RANGE matches ColossusAttackGoal.ATTACK_RANGE exactly - balance-reviewer found a
        // real mechanical dead zone (2-3 blocks) where neither the base attack nor the leap
        // could land, letting a player deny all damage by holding that exact band. No gap
        // between "melee reach ends" and "leap range begins" anymore.
        private static final double MIN_RANGE = 2.0;
        private static final double MAX_RANGE = 15.0;
        private static final int WINDUP_TICKS = 10;
        private static final int COOLDOWN_TICKS = 200; // 10 seconds
        private static final float LEAP_DAMAGE = 100.0F;

        private final ZombieColossusEntity colossus;
        private int windupTicksRemaining;
        private boolean hasDealtDamageThisLeap;

        LeapAttackGoal(ZombieColossusEntity colossus) {
            this.colossus = colossus;
            this.setControls(EnumSet.of(Goal.Control.MOVE, Goal.Control.JUMP, Goal.Control.LOOK));
        }

        @Override
        public boolean canStart() {
            if (colossus.leapCooldownTicks > 0) {
                return false;
            }
            LivingEntity target = colossus.getTarget();
            if (target == null || !target.isAlive()) {
                return false;
            }
            double horizontalDistance = horizontalDistanceTo(target);
            return horizontalDistance >= MIN_RANGE && horizontalDistance <= MAX_RANGE;
        }

        @Override
        public boolean shouldContinue() {
            LivingEntity target = colossus.getTarget();
            return (windupTicksRemaining > 0 || colossus.getLeapFlightTicksRemaining() > 0)
                    && target != null && target.isAlive();
        }

        @Override
        public void start() {
            colossus.getNavigation().stop();
            windupTicksRemaining = WINDUP_TICKS;
            hasDealtDamageThisLeap = false;
            colossus.playSound(SoundEvents.ENTITY_ZOMBIE_AMBIENT, 2.0F, 0.6F);
        }

        @Override
        public void stop() {
            windupTicksRemaining = 0;
            colossus.stopLeapFlight();
        }

        @Override
        public void tick() {
            LivingEntity target = colossus.getTarget();
            if (target == null) {
                return;
            }
            colossus.getLookControl().lookAt(target, 90.0F, 90.0F);

            if (windupTicksRemaining > 0) {
                colossus.getNavigation().stop();
                windupTicksRemaining--;
                if (windupTicksRemaining == 0) {
                    launch(target);
                }
                return;
            }

            if (colossus.getLeapFlightTicksRemaining() > 0
                    && !hasDealtDamageThisLeap && target.isAlive()
                    && colossus.getBoundingBox().expand(0.3).intersects(target.getBoundingBox())) {
                target.damage(getServerWorld(colossus), colossus.getDamageSources().mobAttack(colossus), LEAP_DAMAGE);
                hasDealtDamageThisLeap = true;
            }
        }

        private void launch(LivingEntity target) {
            colossus.getNavigation().stop();
            double horizontalDistance = horizontalDistanceTo(target);
            Vec3d direction = new Vec3d(target.getX() - colossus.getX(), 0.0, target.getZ() - colossus.getZ());
            direction = direction.lengthSquared() > 1.0E-7 ? direction.normalize() : colossus.getRotationVec(1.0F);
            colossus.startLeapFlight(horizontalDistance, direction);
            colossus.leapCooldownTicks = COOLDOWN_TICKS;
        }

        private double horizontalDistanceTo(LivingEntity target) {
            double dx = target.getX() - colossus.getX();
            double dz = target.getZ() - colossus.getZ();
            return Math.sqrt(dx * dx + dz * dz);
        }
    }

    /**
     * Three fast strikes in quick succession, only usable in melee range, on its own long
     * cooldown - a periodic burst that preempts the regular attack cadence (higher goal
     * priority, same Control flags, so the goal system arbitrates which one runs). Gets a
     * short wind-up (growl + brief stationary pause) before the first strike -
     * `balance-reviewer` correctly flagged that 300 damage (60% of a fresh character's starting
     * health) with literally zero telegraph and sub-reactable gaps between strikes was a much
     * sharper "you didn't see it coming" burst than anything else in this boss's kit, unlike the
     * leap's explicit, already-reviewed wind-up. This doesn't make the attack dodgeable mid-
     * combo (same "fully committed" precedent as the leap), just gives the player a fair warning
     * before it starts.
     */
    private static class RageAttackGoal extends Goal {
        private static final double RANGE = 2.0;
        private static final int WINDUP_TICKS = 8;
        private static final int COOLDOWN_TICKS = 200; // 10 seconds
        private static final float STRIKE_DAMAGE = 100.0F;
        private static final int[] STRIKE_TICKS = {0, 5, 9}; // within a 10-tick (0.5s) window, after wind-up

        private final ZombieColossusEntity colossus;
        private int windupTicksRemaining;
        private int elapsedTicks;
        private int nextStrikeIndex;

        RageAttackGoal(ZombieColossusEntity colossus) {
            this.colossus = colossus;
            this.setControls(EnumSet.of(Goal.Control.MOVE, Goal.Control.LOOK));
        }

        @Override
        public boolean canStart() {
            if (colossus.rageCooldownTicks > 0) {
                return false;
            }
            LivingEntity target = colossus.getTarget();
            return target != null && target.isAlive() && colossus.distanceTo(target) <= RANGE;
        }

        @Override
        public boolean shouldContinue() {
            return windupTicksRemaining > 0 || nextStrikeIndex < STRIKE_TICKS.length;
        }

        @Override
        public void start() {
            windupTicksRemaining = WINDUP_TICKS;
            elapsedTicks = 0;
            nextStrikeIndex = 0;
            colossus.getNavigation().stop();
            colossus.playSound(SoundEvents.ENTITY_ZOMBIE_AMBIENT, 2.0F, 0.4F);
        }

        @Override
        public void stop() {
            colossus.rageCooldownTicks = COOLDOWN_TICKS;
        }

        @Override
        public void tick() {
            colossus.getNavigation().stop();
            LivingEntity target = colossus.getTarget();
            if (target != null) {
                colossus.getLookControl().lookAt(target, 90.0F, 90.0F);
            }

            if (windupTicksRemaining > 0) {
                windupTicksRemaining--;
                return;
            }

            if (nextStrikeIndex < STRIKE_TICKS.length && elapsedTicks >= STRIKE_TICKS[nextStrikeIndex]) {
                nextStrikeIndex++;
                if (target != null && target.isAlive() && colossus.distanceTo(target) <= RANGE + 0.5) {
                    colossus.swingHand(Hand.MAIN_HAND);
                    target.damage(getServerWorld(colossus), colossus.getDamageSources().mobAttack(colossus), STRIKE_DAMAGE);
                }
            }
            elapsedTicks++;
        }
    }
}
