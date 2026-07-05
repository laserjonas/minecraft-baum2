package de.baum2dev.baum2.entity;

import java.util.EnumSet;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.ActiveTargetGoal;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.ai.goal.LookAroundGoal;
import net.minecraft.entity.ai.goal.LookAtEntityGoal;
import net.minecraft.entity.ai.goal.RevengeGoal;
import net.minecraft.entity.ai.goal.WanderAroundFarGoal;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.TypeFilter;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.LocalDifficulty;
import net.minecraft.world.World;
import de.baum2dev.baum2.combat.DarkWaveEffect;
import de.baum2dev.baum2.registry.ModItems;

/**
 * Drevathis, the Cursed Sovereign - this project's fifth boss and current top tier (level 40,
 * above Zombie Colossus's 25). Unlike every prior boss, it has <b>no normal attack</b> at all -
 * its entire kit is four skills (Dash of Death, Chain of Death, Wave of Darkness, Thunder of
 * Darkness), each its own {@code Goal}, same "one Goal per skill" shape as
 * {@code ZombieColossusEntity}. Extends {@code HostileEntity} directly (not a themed vanilla
 * mob subtype) since this is an original demon-lord design, not a reskin.
 *
 * <p>Passive: while a player is within {@link #DARKNESS_AURA_RADIUS} blocks, their vision is
 * darkened via vanilla's own {@code StatusEffects.DARKNESS} (the real Warden/Sculk-Shrieker
 * effect) - it already darkens regardless of light level/time of day, so "even if it is day"
 * needs no custom rendering work, just a continuous per-tick proximity refresh.
 */
public class DrevathisEntity extends HostileEntity implements MonsterLevelProvider {
    private static final int LEVEL = 40;
    private static final double DARKNESS_AURA_RADIUS = 12.0;
    private static final int DARKNESS_EFFECT_DURATION_TICKS = 30;

    private static final TrackedData<Integer> DASH_WINDUP_TICKS =
            DataTracker.registerData(DrevathisEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Integer> CHAIN_EFFECT_TICKS =
            DataTracker.registerData(DrevathisEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Integer> WAVE_CAST_TICKS =
            DataTracker.registerData(DrevathisEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Integer> THUNDER_CHANNEL_TICKS =
            DataTracker.registerData(DrevathisEntity.class, TrackedDataHandlerRegistry.INTEGER);

    private int dashCooldownTicks = 0;
    private int chainCooldownTicks = 0;
    private int waveCooldownTicks = 0;
    private int thunderCooldownTicks = 0;

    public DrevathisEntity(EntityType<? extends HostileEntity> entityType, World world) {
        super(entityType, world);
    }

    public static DefaultAttributeContainer.Builder createDrevathisAttributes() {
        return HostileEntity.createHostileAttributes()
                .add(EntityAttributes.MAX_HEALTH, 1200.0)
                .add(EntityAttributes.ATTACK_DAMAGE, 0.0);
    }

    @Override
    protected void initDataTracker(DataTracker.Builder builder) {
        super.initDataTracker(builder);
        builder.add(DASH_WINDUP_TICKS, 0);
        builder.add(CHAIN_EFFECT_TICKS, 0);
        builder.add(WAVE_CAST_TICKS, 0);
        builder.add(THUNDER_CHANNEL_TICKS, 0);
    }

    public int getDashWindupTicks() {
        return this.dataTracker.get(DASH_WINDUP_TICKS);
    }

    void setDashWindupTicks(int ticks) {
        this.dataTracker.set(DASH_WINDUP_TICKS, ticks);
    }

    public int getChainEffectTicks() {
        return this.dataTracker.get(CHAIN_EFFECT_TICKS);
    }

    void setChainEffectTicks(int ticks) {
        this.dataTracker.set(CHAIN_EFFECT_TICKS, ticks);
    }

    public int getWaveCastTicks() {
        return this.dataTracker.get(WAVE_CAST_TICKS);
    }

    void setWaveCastTicks(int ticks) {
        this.dataTracker.set(WAVE_CAST_TICKS, ticks);
    }

    public int getThunderChannelTicks() {
        return this.dataTracker.get(THUNDER_CHANNEL_TICKS);
    }

    void setThunderChannelTicks(int ticks) {
        this.dataTracker.set(THUNDER_CHANNEL_TICKS, ticks);
    }

    /** Always wields its signature sword, two-handed on the model (see client render code). */
    @Override
    protected void initEquipment(Random random, LocalDifficulty localDifficulty) {
        this.equipStack(EquipmentSlot.MAINHAND, new ItemStack(ModItems.DREVATHIS_CURSED_BLADE));
        this.setEquipmentDropChance(EquipmentSlot.MAINHAND, 0.0F);
    }

    @Override
    protected void initGoals() {
        this.goalSelector.add(1, new DashOfDeathGoal(this));
        this.goalSelector.add(2, new ThunderOfDarknessGoal(this));
        this.goalSelector.add(3, new WaveOfDarknessGoal(this));
        this.goalSelector.add(4, new ChainOfDeathGoal(this));
        this.goalSelector.add(5, new ApproachGoal(this));
        this.goalSelector.add(6, new WanderAroundFarGoal(this, 1.0));
        this.goalSelector.add(7, new LookAtEntityGoal(this, PlayerEntity.class, 20.0F));
        this.goalSelector.add(7, new LookAroundGoal(this));
        this.targetSelector.add(1, new RevengeGoal(this));
        this.targetSelector.add(2, new ActiveTargetGoal<>(this, PlayerEntity.class, true));
    }

    @Override
    public void tick() {
        super.tick();
        if (!getEntityWorld().isClient()) {
            if (dashCooldownTicks > 0) {
                dashCooldownTicks--;
            }
            if (chainCooldownTicks > 0) {
                chainCooldownTicks--;
            }
            if (waveCooldownTicks > 0) {
                waveCooldownTicks--;
            }
            if (thunderCooldownTicks > 0) {
                thunderCooldownTicks--;
            }
            tickDarknessAura();
        }
    }

    /** Refreshes vanilla Darkness on every player within range, every tick they stay in range -
     *  a short effect duration means it fades naturally ~1.5s after a player leaves, rather than
     *  needing an explicit "left the aura" removal call. */
    private void tickDarknessAura() {
        if (!(getEntityWorld() instanceof ServerWorld serverWorld)) {
            return;
        }
        Box auraBox = Box.of(getEntityPos(), DARKNESS_AURA_RADIUS * 2, DARKNESS_AURA_RADIUS * 2, DARKNESS_AURA_RADIUS * 2);
        for (PlayerEntity player : serverWorld.getEntitiesByType(TypeFilter.instanceOf(PlayerEntity.class), auraBox, PlayerEntity::isAlive)) {
            if (player.getEntityPos().distanceTo(getEntityPos()) <= DARKNESS_AURA_RADIUS) {
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.DARKNESS, DARKNESS_EFFECT_DURATION_TICKS, 0, false, false));
            }
        }
    }

    @Override
    protected void dropLoot(ServerWorld world, DamageSource damageSource, boolean causedByPlayer) {
        this.dropStack(world, new ItemStack(ModItems.DREVATHIS_CURSED_BLADE));
    }

    @Override
    public int getMonsterLevel() {
        return LEVEL;
    }

    /** Pushes {@code target} straight upward and sends the sync packet players otherwise never
     *  receive for a server-side velocity change - same gotcha documented in
     *  skills/SpellEffects.java and docs/fabric-modding.md. */
    private static void launchUpward(LivingEntity target, double verticalVelocity) {
        target.setVelocity(0.0, verticalVelocity, 0.0);
        if (target instanceof ServerPlayerEntity targetPlayer) {
            targetPlayer.networkHandler.sendPacket(new EntityVelocityUpdateS2CPacket(targetPlayer));
        }
    }

    /**
     * Teleports behind the target, a short 0.5s wind-up later launches them ~10 blocks into the
     * sky and deals magic damage. The vertical velocity below is derived from this game's real
     * per-tick gravity/drag (vy = (vy - 0.08) * 0.98, see HANDOFF.md's note on how Spider
     * Queen's leap arc was derived the same way) for an *approximate* 10-block peak height -
     * flagged in the plan as likely needing one empirical tweak after a real playtest, same as
     * every other boss's launch/leap numbers in this codebase.
     */
    private static class DashOfDeathGoal extends Goal {
        private static final double TELEPORT_OFFSET = 3.0;
        private static final int WINDUP_TICKS = 10; // 0.5s
        private static final int COOLDOWN_TICKS = 400; // 20s
        private static final float DAMAGE = 50.0F;
        private static final double LAUNCH_VELOCITY = 1.45;

        private final DrevathisEntity boss;
        private int windupTicksRemaining;
        private LivingEntity windupTarget;

        DashOfDeathGoal(DrevathisEntity boss) {
            this.boss = boss;
            this.setControls(EnumSet.of(Goal.Control.MOVE, Goal.Control.LOOK));
        }

        @Override
        public boolean canStart() {
            if (boss.dashCooldownTicks > 0) {
                return false;
            }
            LivingEntity target = boss.getTarget();
            return target != null && target.isAlive();
        }

        @Override
        public boolean shouldContinue() {
            return windupTicksRemaining > 0 && windupTarget != null && windupTarget.isAlive();
        }

        @Override
        public void start() {
            LivingEntity target = boss.getTarget();
            windupTarget = target;
            boss.getNavigation().stop();

            Vec3d behind = new Vec3d(-target.getRotationVector().x, 0.0, -target.getRotationVector().z);
            if (behind.lengthSquared() < 1.0E-7) {
                behind = new Vec3d(1.0, 0.0, 0.0);
            }
            behind = behind.normalize().multiply(TELEPORT_OFFSET);
            boss.requestTeleport(target.getX() + behind.x, target.getY(), target.getZ() + behind.z);

            windupTicksRemaining = WINDUP_TICKS;
            boss.setDashWindupTicks(windupTicksRemaining);
        }

        @Override
        public void stop() {
            windupTicksRemaining = 0;
            boss.setDashWindupTicks(0);
            windupTarget = null;
        }

        @Override
        public void tick() {
            if (windupTarget == null) {
                return;
            }
            boss.getLookControl().lookAt(windupTarget, 90.0F, 90.0F);
            windupTicksRemaining--;
            boss.setDashWindupTicks(windupTicksRemaining);
            if (windupTicksRemaining <= 0) {
                if (windupTarget.isAlive() && boss.getEntityWorld() instanceof ServerWorld serverWorld) {
                    windupTarget.damage(serverWorld, boss.getDamageSources().indirectMagic(boss, boss), DAMAGE);
                    launchUpward(windupTarget, LAUNCH_VELOCITY);
                }
                boss.dashCooldownTicks = COOLDOWN_TICKS;
            }
        }
    }

    /**
     * Instantly applies vanilla Slowness at the exact amplifier that yields a 75% speed
     * reduction (Slowness V: {@code 1 - 0.15*(amplifier+1)} = {@code 1 - 0.15*5} = 0.25 - a
     * confirmed exact match to vanilla's own formula, not an approximation), then holds a
     * chain-link particle visual between boss and target for the full duration.
     */
    private static class ChainOfDeathGoal extends Goal {
        private static final double MAX_RANGE = 20.0;
        private static final int SLOW_DURATION_TICKS = 100; // 5s
        private static final int SLOWNESS_AMPLIFIER = 4; // Slowness V = exactly -75% speed
        private static final int COOLDOWN_TICKS = 200; // 10s

        private final DrevathisEntity boss;
        private int effectTicksRemaining;
        private LivingEntity chainTarget;

        ChainOfDeathGoal(DrevathisEntity boss) {
            this.boss = boss;
            this.setControls(EnumSet.of(Goal.Control.LOOK));
        }

        @Override
        public boolean canStart() {
            if (boss.chainCooldownTicks > 0) {
                return false;
            }
            LivingEntity target = boss.getTarget();
            return target != null && target.isAlive() && boss.distanceTo(target) <= MAX_RANGE;
        }

        @Override
        public boolean shouldContinue() {
            return effectTicksRemaining > 0 && chainTarget != null && chainTarget.isAlive();
        }

        @Override
        public void start() {
            LivingEntity target = boss.getTarget();
            chainTarget = target;
            target.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, SLOW_DURATION_TICKS, SLOWNESS_AMPLIFIER));
            effectTicksRemaining = SLOW_DURATION_TICKS;
            boss.setChainEffectTicks(effectTicksRemaining);
            boss.chainCooldownTicks = COOLDOWN_TICKS;
        }

        @Override
        public void stop() {
            effectTicksRemaining = 0;
            boss.setChainEffectTicks(0);
            chainTarget = null;
        }

        @Override
        public void tick() {
            if (chainTarget == null) {
                return;
            }
            boss.getLookControl().lookAt(chainTarget, 90.0F, 90.0F);
            spawnChainParticles(chainTarget);
            effectTicksRemaining--;
            boss.setChainEffectTicks(effectTicksRemaining);
        }

        private void spawnChainParticles(LivingEntity target) {
            if (!(boss.getEntityWorld() instanceof ServerWorld serverWorld)) {
                return;
            }
            Vec3d from = boss.getEntityPos().add(0.0, boss.getHeight() * 0.5, 0.0);
            Vec3d to = target.getEntityPos().add(0.0, target.getHeight() * 0.5, 0.0);
            int links = (int) Math.ceil(from.distanceTo(to));
            for (int i = 0; i <= links; i++) {
                double fraction = links == 0 ? 0.0 : (double) i / links;
                Vec3d point = from.lerp(to, fraction);
                serverWorld.spawnParticles(ParticleTypes.SOUL_FIRE_FLAME, point.x, point.y, point.z, 1, 0.0, 0.0, 0.0, 0.0);
            }
        }
    }

    /**
     * A 0.5s cast-time telegraph, then an instant rectangular wave (20 blocks long, 8 wide)
     * aimed at wherever the target was when casting started - see {@link DarkWaveEffect} for the
     * shared rectangle-hit-test math also used by Drevathis's Cursed Blade's on-hit proc.
     */
    private static class WaveOfDarknessGoal extends Goal {
        private static final double MAX_RANGE = 20.0;
        private static final int CAST_TICKS = 10; // 0.5s
        private static final int COOLDOWN_TICKS = 40; // 2s
        private static final float DAMAGE = 65.0F;
        private static final double WAVE_RANGE = 20.0;
        private static final double WAVE_WIDTH = 8.0;

        private final DrevathisEntity boss;
        private int castTicksRemaining;
        private Vec3d aimDirection;

        WaveOfDarknessGoal(DrevathisEntity boss) {
            this.boss = boss;
            this.setControls(EnumSet.of(Goal.Control.LOOK));
        }

        @Override
        public boolean canStart() {
            if (boss.waveCooldownTicks > 0) {
                return false;
            }
            LivingEntity target = boss.getTarget();
            return target != null && target.isAlive() && boss.distanceTo(target) <= MAX_RANGE;
        }

        @Override
        public boolean shouldContinue() {
            return castTicksRemaining > 0;
        }

        @Override
        public void start() {
            LivingEntity target = boss.getTarget();
            Vec3d direction = target.getEntityPos().subtract(boss.getEntityPos());
            aimDirection = direction.lengthSquared() > 1.0E-7 ? direction.normalize() : boss.getRotationVec(1.0F);
            castTicksRemaining = CAST_TICKS;
            boss.setWaveCastTicks(castTicksRemaining);
        }

        @Override
        public void stop() {
            castTicksRemaining = 0;
            boss.setWaveCastTicks(0);
        }

        @Override
        public void tick() {
            LivingEntity target = boss.getTarget();
            if (target != null) {
                boss.getLookControl().lookAt(target, 90.0F, 90.0F);
            }
            castTicksRemaining--;
            boss.setWaveCastTicks(castTicksRemaining);
            if (castTicksRemaining <= 0 && boss.getEntityWorld() instanceof ServerWorld serverWorld) {
                DarkWaveEffect.cast(serverWorld, boss, boss.getEntityPos(), aimDirection, DAMAGE, WAVE_RANGE, WAVE_WIDTH);
                boss.waveCooldownTicks = COOLDOWN_TICKS;
            }
        }
    }

    /**
     * Over a 3-second channel, strikes land near the target's *current* (re-sampled each strike)
     * position - this is what "follows the player" means, in contrast to Wave of Darkness which
     * aims once. Strike frequency (every 10 ticks / 0.5s, 6 strikes total) isn't specified by the
     * original design brief - a documented assumption, easy to retune, flagged for
     * balance-reviewer.
     */
    private static class ThunderOfDarknessGoal extends Goal {
        private static final double MAX_RANGE = 20.0;
        private static final int[] STRIKE_TICKS = {0, 10, 20, 30, 40, 50}; // 6 strikes over 60 ticks (3s)
        private static final double SPAWN_VARIANCE_RADIUS = 5.0;
        private static final double STRIKE_HIT_RADIUS = 1.0;
        private static final float STRIKE_DAMAGE = 70.0F;
        private static final int COOLDOWN_TICKS = 200; // 10s

        private final DrevathisEntity boss;
        private int elapsedTicks;
        private int nextStrikeIndex;

        ThunderOfDarknessGoal(DrevathisEntity boss) {
            this.boss = boss;
            this.setControls(EnumSet.of(Goal.Control.LOOK));
        }

        @Override
        public boolean canStart() {
            if (boss.thunderCooldownTicks > 0) {
                return false;
            }
            LivingEntity target = boss.getTarget();
            return target != null && target.isAlive() && boss.distanceTo(target) <= MAX_RANGE;
        }

        @Override
        public boolean shouldContinue() {
            return nextStrikeIndex < STRIKE_TICKS.length;
        }

        @Override
        public void start() {
            elapsedTicks = 0;
            nextStrikeIndex = 0;
            boss.setThunderChannelTicks(STRIKE_TICKS[STRIKE_TICKS.length - 1] + 1);
        }

        @Override
        public void stop() {
            boss.setThunderChannelTicks(0);
            boss.thunderCooldownTicks = COOLDOWN_TICKS;
        }

        @Override
        public void tick() {
            LivingEntity target = boss.getTarget();
            if (target != null) {
                boss.getLookControl().lookAt(target, 90.0F, 90.0F);
            }

            int remaining = Math.max(0, (STRIKE_TICKS[STRIKE_TICKS.length - 1] + 1) - elapsedTicks);
            boss.setThunderChannelTicks(remaining);

            if (target != null && target.isAlive()
                    && nextStrikeIndex < STRIKE_TICKS.length && elapsedTicks >= STRIKE_TICKS[nextStrikeIndex]) {
                nextStrikeIndex++;
                strike(target);
            }
            elapsedTicks++;
        }

        private void strike(LivingEntity target) {
            if (!(boss.getEntityWorld() instanceof ServerWorld serverWorld)) {
                return;
            }
            double angle = boss.getRandom().nextDouble() * Math.PI * 2.0;
            double radius = boss.getRandom().nextDouble() * SPAWN_VARIANCE_RADIUS;
            double strikeX = target.getX() + Math.cos(angle) * radius;
            double strikeZ = target.getZ() + Math.sin(angle) * radius;
            double strikeY = target.getY();

            serverWorld.spawnParticles(ParticleTypes.ELECTRIC_SPARK, strikeX, strikeY + 1.0, strikeZ, 12, 0.2, 0.6, 0.2, 0.02);

            Box hitBox = new Box(
                    strikeX - STRIKE_HIT_RADIUS, strikeY - 1.0, strikeZ - STRIKE_HIT_RADIUS,
                    strikeX + STRIKE_HIT_RADIUS, strikeY + 2.0, strikeZ + STRIKE_HIT_RADIUS);
            DamageSource source = boss.getDamageSources().indirectMagic(boss, boss);
            for (PlayerEntity player : serverWorld.getEntitiesByType(TypeFilter.instanceOf(PlayerEntity.class), hitBox, PlayerEntity::isAlive)) {
                player.damage(serverWorld, source, STRIKE_DAMAGE);
            }
        }
    }

    /**
     * Closes distance whenever the target is beyond every skill's own engagement range - without
     * this, a player who simply stays past {@code ENGAGE_RANGE} is fully exempt from Chain of
     * Death, Wave of Darkness, Thunder of Darkness, and the Darkness aura, taking only Dash of
     * Death's flat 50 damage every 20s (balance-reviewer finding: a severe kiting dead-zone,
     * since nothing else in {@code initGoals()} ever moves the boss toward its target). Lowest
     * priority of the five combat goals, so any skill goal wanting to run still preempts it via
     * the shared {@code Control.MOVE}/{@code Control.LOOK} flags, same arbitration
     * {@code ZombieColossusEntity}'s own goal-priority stack already relies on.
     */
    private static class ApproachGoal extends Goal {
        private static final double ENGAGE_RANGE = 20.0; // matches every skill's own MAX_RANGE
        private static final double MOVE_SPEED = 1.15;

        private final DrevathisEntity boss;

        ApproachGoal(DrevathisEntity boss) {
            this.boss = boss;
            this.setControls(EnumSet.of(Goal.Control.MOVE, Goal.Control.LOOK));
        }

        @Override
        public boolean canStart() {
            LivingEntity target = boss.getTarget();
            return target != null && target.isAlive() && boss.distanceTo(target) > ENGAGE_RANGE;
        }

        @Override
        public boolean shouldContinue() {
            LivingEntity target = boss.getTarget();
            return target != null && target.isAlive() && boss.distanceTo(target) > ENGAGE_RANGE * 0.75;
        }

        @Override
        public void tick() {
            LivingEntity target = boss.getTarget();
            if (target == null) {
                return;
            }
            boss.getLookControl().lookAt(target, 30.0F, 30.0F);
            boss.getNavigation().startMovingTo(target, MOVE_SPEED);
        }

        @Override
        public void stop() {
            boss.getNavigation().stop();
        }
    }
}
