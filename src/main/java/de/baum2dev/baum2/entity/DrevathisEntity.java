package de.baum2dev.baum2.entity;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.ActiveTargetGoal;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.ai.goal.LookAroundGoal;
import net.minecraft.entity.ai.goal.LookAtEntityGoal;
import net.minecraft.entity.ai.goal.RevengeGoal;
import net.minecraft.entity.ai.goal.WanderAroundFarGoal;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
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
import net.minecraft.network.packet.s2c.play.GameStateChangeS2CPacket;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import de.baum2dev.baum2.registry.ModEntities;
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
 * Drevathis, the Cursed Sovereign - full rework (third GeckoLib boss after Spider Queen and
 * Zombie Colossus): a demon born to kill you, bigger than the player, wielding a black blade
 * wreathed in dark smoke (real geometry in the GeckoLib model - see tools/gen_drevathis.py;
 * the smoke is the server-side particle wreath in {@link #tickBladeSmoke()}).
 *
 * <p>Kit (replaces the old Dash/Chain/Wave/Thunder set entirely):
 * <ul>
 * <li><b>Passive - Sovereign's Storm:</b> every player within {@link #WEATHER_AURA_RADIUS}
 *     blocks sees a personal thunderstorm (dark sky, rain, rolling thunder) via per-player
 *     {@link GameStateChangeS2CPacket} weather packets - pure client illusion, the real world
 *     weather is untouched and is restored the moment they leave the aura (or the boss dies).
 * <li><b>Basic attack - dark wave:</b> no melee at all; throws a {@link DarkWaveProjectileEntity}
 *     at the target's position (dodgeable). 50 damage, 2s cooldown.
 * <li><b>Curse Ground:</b> channels, then curses the ground around itself: an 8-block zone
 *     that burns (10 damage per second-tick, fire-resistance counterplay respected, same
 *     convention as combat/BurnDamageManager) and slows (exact -25% movement speed via an
 *     attribute modifier - vanilla Slowness has no 25% step). 15s cooldown.
 * <li><b>Stampede:</b> three horns-first charge passes at high speed; each pass knocks hit
 *     players skyward with a dark burst. 50 damage per hit. 18s cooldown.
 * <li><b>The End is Near:</b> when a target is within 5 blocks: a 5s channel that slowly pulls
 *     players inward (escapable by moving out - the pull loses to walking, even under the
 *     -25% penalty applied in the area) while fire comets rain NEAR players (never aimed dead
 *     center, so they can be seen and sidestepped); anyone still inside at the end takes 100
 *     damage and is struck with terror - 2s of involuntary stumbling in random directions.
 *     36s cooldown.
 * </ul>
 */
public class DrevathisEntity extends HostileEntity implements MonsterLevelProvider, GeoEntity {
    private static final int LEVEL = 40;

    // --- GeckoLib animation wiring (same pattern as ZombieColossusEntity). One controller:
    // state-driven idle/walk/stampede poses, plus three server-triggered one-shots.
    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);

    private static final RawAnimation IDLE_ANIM = RawAnimation.begin().thenLoop("animation.drevathis.idle");
    private static final RawAnimation WALK_ANIM = RawAnimation.begin().thenLoop("animation.drevathis.walk");
    private static final RawAnimation STAMPEDE_ANIM =
            RawAnimation.begin().thenLoop("animation.drevathis.stampede_run");
    private static final RawAnimation THROW_WAVE_ANIM =
            RawAnimation.begin().thenPlay("animation.drevathis.throw_wave");
    private static final RawAnimation CURSE_GROUND_ANIM =
            RawAnimation.begin().thenPlay("animation.drevathis.curse_ground");
    private static final RawAnimation END_CHANNEL_ANIM =
            RawAnimation.begin().thenPlay("animation.drevathis.end_channel");
    static final String MAIN_CONTROLLER = "main";

    private static final TrackedData<Boolean> STAMPEDE_ACTIVE =
            DataTracker.registerData(DrevathisEntity.class, TrackedDataHandlerRegistry.BOOLEAN);

    // --- passive: per-player storm illusion ---
    private static final double WEATHER_AURA_RADIUS = 100.0;
    private static final int WEATHER_SWEEP_INTERVAL_TICKS = 20;

    // --- exact -25% movement speed, shared by Curse Ground and The End is Near ---
    private static final Identifier DREAD_SLOW_ID = Identifier.of("baum2", "drevathis_dread_slow");
    private static final EntityAttributeModifier DREAD_SLOW_MODIFIER = new EntityAttributeModifier(
            DREAD_SLOW_ID, -0.25, EntityAttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);

    // --- Curse Ground zone (owned by the entity: it outlives the casting Goal) ---
    private static final double CURSE_ZONE_RADIUS = 8.0;
    private static final int CURSE_ZONE_DURATION_TICKS = 160; // 8s
    private static final float CURSE_BURN_DAMAGE = 10.0F;
    private static final int CURSE_BURN_INTERVAL_TICKS = 20;

    // --- terror aftermath of The End is Near ---
    private static final int FEAR_IMPULSE_INTERVAL_TICKS = 5;
    private static final double FEAR_IMPULSE_STRENGTH = 0.3;

    private int waveCooldownTicks = 0;
    private int curseCooldownTicks = 0;
    private int stampedeCooldownTicks = 0;
    private int endCooldownTicks = 0;

    private Vec3d curseZoneCenter = null;
    private int curseZoneTicksRemaining = 0;
    /** Set by EndIsNearGoal while its pull is live, so the slow reconciliation includes it. */
    private boolean endPullActive = false;

    private final Map<UUID, Integer> fearTicksByPlayer = new HashMap<>();
    private final Set<UUID> stormedPlayers = new HashSet<>();
    private final Set<UUID> slowedPlayers = new HashSet<>();

    public DrevathisEntity(EntityType<? extends HostileEntity> entityType, World world) {
        super(entityType, world);
    }

    public static DefaultAttributeContainer.Builder createDrevathisAttributes() {
        return HostileEntity.createHostileAttributes()
                .add(EntityAttributes.MAX_HEALTH, 1200.0)
                .add(EntityAttributes.MOVEMENT_SPEED, 0.3)
                .add(EntityAttributes.FOLLOW_RANGE, 48.0)
                .add(EntityAttributes.ATTACK_DAMAGE, 0.0); // no melee by design
    }

    @Override
    protected void initDataTracker(DataTracker.Builder builder) {
        super.initDataTracker(builder);
        builder.add(STAMPEDE_ACTIVE, false);
    }

    public boolean isStampedeActive() {
        return this.dataTracker.get(STAMPEDE_ACTIVE);
    }

    void setStampedeActive(boolean active) {
        this.dataTracker.set(STAMPEDE_ACTIVE, active);
    }

    // ------------------------------------------------------------------ GeckoLib

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(MAIN_CONTROLLER, 4, this::handleAnimationState)
                .triggerableAnim("throw_wave", THROW_WAVE_ANIM)
                .triggerableAnim("curse_ground", CURSE_GROUND_ANIM)
                .triggerableAnim("end_channel", END_CHANNEL_ANIM));
    }

    private PlayState handleAnimationState(AnimationTest<DrevathisEntity> test) {
        if (test.animatable().isStampedeActive()) {
            return test.setAndContinue(STAMPEDE_ANIM);
        }
        if (test.isMoving()) {
            return test.setAndContinue(WALK_ANIM);
        }
        return test.setAndContinue(IDLE_ANIM);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.geoCache;
    }

    // ------------------------------------------------------------------ AI / lifecycle

    @Override
    protected void initGoals() {
        this.goalSelector.add(1, new EndIsNearGoal(this));
        this.goalSelector.add(2, new StampedeGoal(this));
        this.goalSelector.add(3, new CurseGroundGoal(this));
        this.goalSelector.add(4, new ThrowWaveGoal(this));
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
        if (!(getEntityWorld() instanceof ServerWorld serverWorld)) {
            return;
        }
        if (waveCooldownTicks > 0) {
            waveCooldownTicks--;
        }
        if (curseCooldownTicks > 0) {
            curseCooldownTicks--;
        }
        if (stampedeCooldownTicks > 0) {
            stampedeCooldownTicks--;
        }
        if (endCooldownTicks > 0) {
            endCooldownTicks--;
        }
        tickCurseZone(serverWorld);
        tickDreadSlow(serverWorld);
        tickFear(serverWorld);
        if (this.age % WEATHER_SWEEP_INTERVAL_TICKS == 0) {
            tickWeatherAura(serverWorld);
        }
        if (this.age % 3 == 0) {
            tickBladeSmoke(serverWorld);
        }
    }

    /** Undo every lingering per-player manipulation (storm illusion, slow modifiers) no matter
     *  how the boss leaves the world - death, discard, or dimension change. */
    @Override
    public void remove(Entity.RemovalReason reason) {
        if (getEntityWorld() instanceof ServerWorld serverWorld) {
            for (ServerPlayerEntity player : serverWorld.getPlayers()) {
                if (stormedPlayers.contains(player.getUuid())) {
                    sendRealWeather(player, serverWorld);
                }
                removeDreadSlow(player);
            }
            stormedPlayers.clear();
            slowedPlayers.clear();
        }
        super.remove(reason);
    }

    @Override
    protected void dropLoot(ServerWorld world, DamageSource damageSource, boolean causedByPlayer) {
        this.dropStack(world, new ItemStack(ModItems.DREVATHIS_CURSED_BLADE));
    }

    @Override
    public int getMonsterLevel() {
        return LEVEL;
    }

    // ------------------------------------------------------------------ passive: storm aura

    /**
     * Sweeps all players once a second: inside the aura they get (and keep getting - so the
     * illusion also wins against real weather changes the server may broadcast) a full
     * thunderstorm; on leaving they get their real weather back. Thunder rolls at random for
     * stormed players so the storm is heard, not just seen.
     */
    private void tickWeatherAura(ServerWorld world) {
        double radiusSq = WEATHER_AURA_RADIUS * WEATHER_AURA_RADIUS;
        for (ServerPlayerEntity player : world.getPlayers()) {
            boolean inRange = player.isAlive()
                    && player.squaredDistanceTo(this) <= radiusSq
                    && player.getEntityWorld() == world;
            if (inRange) {
                stormedPlayers.add(player.getUuid());
                player.networkHandler.sendPacket(
                        new GameStateChangeS2CPacket(GameStateChangeS2CPacket.RAIN_STARTED, 0.0F));
                player.networkHandler.sendPacket(
                        new GameStateChangeS2CPacket(GameStateChangeS2CPacket.RAIN_GRADIENT_CHANGED, 1.0F));
                player.networkHandler.sendPacket(
                        new GameStateChangeS2CPacket(GameStateChangeS2CPacket.THUNDER_GRADIENT_CHANGED, 1.0F));
                if (this.random.nextFloat() < 0.18F) {
                    world.playSound(null, player.getX(), player.getY() + 24.0, player.getZ(),
                            SoundEvents.ENTITY_LIGHTNING_BOLT_THUNDER, SoundCategory.WEATHER,
                            1.4F, 0.8F + this.random.nextFloat() * 0.4F);
                }
            } else if (stormedPlayers.remove(player.getUuid())) {
                sendRealWeather(player, world);
            }
        }
    }

    private static void sendRealWeather(ServerPlayerEntity player, ServerWorld world) {
        player.networkHandler.sendPacket(new GameStateChangeS2CPacket(
                world.isRaining() ? GameStateChangeS2CPacket.RAIN_STARTED
                        : GameStateChangeS2CPacket.RAIN_STOPPED, 0.0F));
        player.networkHandler.sendPacket(new GameStateChangeS2CPacket(
                GameStateChangeS2CPacket.RAIN_GRADIENT_CHANGED, world.getRainGradient(1.0F)));
        player.networkHandler.sendPacket(new GameStateChangeS2CPacket(
                GameStateChangeS2CPacket.THUNDER_GRADIENT_CHANGED, world.getThunderGradient(1.0F)));
    }

    // ------------------------------------------------------------------ blade smoke

    /** The black blade's dark-smoke wreath: a slow smoke column around the blade's carry
     *  position at the right hip (approximate - the blade is an animated bone, but a smoky
     *  column on that side reads correctly in every pose). */
    private void tickBladeSmoke(ServerWorld world) {
        double yawRad = Math.toRadians(this.bodyYaw);
        // entity's right-hand side = facing x up (see gen_drevathis.py: blade on the right arm)
        double rx = -Math.cos(yawRad);
        double rz = -Math.sin(yawRad);
        double x = getX() + rx * 1.05;
        double z = getZ() + rz * 1.05;
        double y = getY() + 2.1;
        world.spawnParticles(ParticleTypes.SMOKE, x, y, z, 2, 0.22, 0.75, 0.22, 0.004);
        if (this.age % 6 == 0) {
            world.spawnParticles(ParticleTypes.SQUID_INK, x, y + 0.4, z, 1, 0.15, 0.5, 0.15, 0.01);
        }
    }

    // ------------------------------------------------------------------ Curse Ground zone

    void activateCurseZone(ServerWorld world) {
        curseZoneCenter = getEntityPos();
        curseZoneTicksRemaining = CURSE_ZONE_DURATION_TICKS;
        world.playSound(null, getX(), getY(), getZ(), SoundEvents.ENTITY_ELDER_GUARDIAN_CURSE,
                getSoundCategory(), 1.5F, 0.75F);
        // eruption ring so the zone boundary is unmistakable from the first tick
        spawnCurseRing(world, 1.0);
    }

    private void tickCurseZone(ServerWorld world) {
        if (curseZoneTicksRemaining <= 0 || curseZoneCenter == null) {
            return;
        }
        curseZoneTicksRemaining--;

        // the darkened ground: a low, dense ink-and-ash carpet, plus the boundary ring
        for (int i = 0; i < 6; i++) {
            double angle = this.random.nextDouble() * Math.PI * 2.0;
            double dist = Math.sqrt(this.random.nextDouble()) * CURSE_ZONE_RADIUS;
            double px = curseZoneCenter.x + Math.cos(angle) * dist;
            double pz = curseZoneCenter.z + Math.sin(angle) * dist;
            world.spawnParticles(ParticleTypes.SQUID_INK, px, curseZoneCenter.y + 0.1, pz,
                    2, 0.3, 0.05, 0.3, 0.004);
            if (i % 2 == 0) {
                world.spawnParticles(ParticleTypes.ASH, px, curseZoneCenter.y + 0.5, pz,
                        3, 0.3, 0.3, 0.3, 0.01);
            }
            if (i == 0) {
                world.spawnParticles(ParticleTypes.SMALL_FLAME, px, curseZoneCenter.y + 0.15, pz,
                        1, 0.2, 0.05, 0.2, 0.01);
            }
        }
        if (curseZoneTicksRemaining % 10 == 0) {
            spawnCurseRing(world, 0.25);
        }

        // the burn: 10 damage per second-tick to players standing on cursed ground, with the
        // standard fire counterplay (same convention as combat/BurnDamageManager)
        if (curseZoneTicksRemaining % CURSE_BURN_INTERVAL_TICKS == 0) {
            for (ServerPlayerEntity player : world.getPlayers()) {
                if (player.isAlive() && !player.isSpectator() && isInCurseZone(player)) {
                    if (!player.isFireImmune() && !player.hasStatusEffect(StatusEffects.FIRE_RESISTANCE)) {
                        player.damage(world, player.getDamageSources().onFire(), CURSE_BURN_DAMAGE);
                    }
                    world.spawnParticles(ParticleTypes.FLAME, player.getX(), player.getY() + 0.6,
                            player.getZ(), 8, 0.25, 0.4, 0.25, 0.015);
                }
            }
        }
        if (curseZoneTicksRemaining <= 0) {
            curseZoneCenter = null;
        }
    }

    private void spawnCurseRing(ServerWorld world, double intensity) {
        int points = 24;
        for (int i = 0; i < points; i++) {
            double angle = (Math.PI * 2.0 * i) / points;
            double px = curseZoneCenter.x + Math.cos(angle) * CURSE_ZONE_RADIUS;
            double pz = curseZoneCenter.z + Math.sin(angle) * CURSE_ZONE_RADIUS;
            world.spawnParticles(ParticleTypes.SOUL_FIRE_FLAME, px, curseZoneCenter.y + 0.2, pz,
                    (int) Math.max(1, 3 * intensity), 0.05, 0.15, 0.05, 0.01);
        }
    }

    private boolean isInCurseZone(PlayerEntity player) {
        if (curseZoneTicksRemaining <= 0 || curseZoneCenter == null) {
            return false;
        }
        double dx = player.getX() - curseZoneCenter.x;
        double dz = player.getZ() - curseZoneCenter.z;
        return dx * dx + dz * dz <= CURSE_ZONE_RADIUS * CURSE_ZONE_RADIUS
                && Math.abs(player.getY() - curseZoneCenter.y) <= 3.0;
    }

    // ------------------------------------------------------------------ shared -25% slow

    /**
     * One reconciliation pass per tick: players who should currently be slowed (standing on
     * cursed ground, or inside a live The End is Near pull) carry the -25% modifier, everyone
     * else has it removed. Centralized so two overlapping sources can't double-apply or
     * leave a stale modifier behind when one source ends.
     */
    private void tickDreadSlow(ServerWorld world) {
        for (ServerPlayerEntity player : world.getPlayers()) {
            boolean shouldSlow = player.isAlive() && !player.isSpectator() && !player.isCreative()
                    && (isInCurseZone(player)
                        || (endPullActive && player.squaredDistanceTo(this)
                                <= EndIsNearGoal.AREA_RADIUS * EndIsNearGoal.AREA_RADIUS));
            boolean isSlowed = slowedPlayers.contains(player.getUuid());
            if (shouldSlow && !isSlowed) {
                EntityAttributeInstance speed = player.getAttributeInstance(EntityAttributes.MOVEMENT_SPEED);
                if (speed != null && !speed.hasModifier(DREAD_SLOW_ID)) {
                    speed.addTemporaryModifier(DREAD_SLOW_MODIFIER);
                }
                slowedPlayers.add(player.getUuid());
            } else if (!shouldSlow && isSlowed) {
                removeDreadSlow(player);
            }
        }
    }

    private void removeDreadSlow(ServerPlayerEntity player) {
        EntityAttributeInstance speed = player.getAttributeInstance(EntityAttributes.MOVEMENT_SPEED);
        if (speed != null) {
            speed.removeModifier(DREAD_SLOW_ID);
        }
        slowedPlayers.remove(player.getUuid());
    }

    // ------------------------------------------------------------------ terror (fear)

    void startFear(ServerPlayerEntity player, int durationTicks) {
        fearTicksByPlayer.put(player.getUuid(), durationTicks);
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.NAUSEA, 100, 0, false, false));
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.DARKNESS, 60, 0, false, false));
    }

    /** Terror: shove the player in a fresh random direction every few ticks - the closest a
     *  server can get to "running in random directions" without client-side control. */
    private void tickFear(ServerWorld world) {
        if (fearTicksByPlayer.isEmpty()) {
            return;
        }
        Iterator<Map.Entry<UUID, Integer>> iterator = fearTicksByPlayer.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, Integer> entry = iterator.next();
            ServerPlayerEntity player = null;
            for (ServerPlayerEntity candidate : world.getPlayers()) {
                if (candidate.getUuid().equals(entry.getKey())) {
                    player = candidate;
                    break;
                }
            }
            if (player == null || !player.isAlive()) {
                iterator.remove();
                continue;
            }
            int remaining = entry.getValue() - 1;
            if (remaining % FEAR_IMPULSE_INTERVAL_TICKS == 0) {
                double angle = this.random.nextDouble() * Math.PI * 2.0;
                player.setVelocity(Math.cos(angle) * FEAR_IMPULSE_STRENGTH,
                        Math.max(player.getVelocity().y, 0.08),
                        Math.sin(angle) * FEAR_IMPULSE_STRENGTH);
                player.networkHandler.sendPacket(new EntityVelocityUpdateS2CPacket(player));
                world.spawnParticles(ParticleTypes.SMOKE, player.getX(), player.getY() + 1.4,
                        player.getZ(), 3, 0.2, 0.3, 0.2, 0.01);
            }
            if (remaining <= 0) {
                iterator.remove();
            } else {
                entry.setValue(remaining);
            }
        }
    }

    // ------------------------------------------------------------------ helpers

    /** Pushes {@code target} straight upward and sends the sync packet players otherwise never
     *  receive for a server-side velocity change - same gotcha documented in
     *  skills/SpellEffects.java and docs/fabric-modding.md. */
    private static void launchUpward(LivingEntity target, double verticalVelocity) {
        target.setVelocity(target.getVelocity().x * 0.3, verticalVelocity, target.getVelocity().z * 0.3);
        if (target instanceof ServerPlayerEntity targetPlayer) {
            targetPlayer.networkHandler.sendPacket(new EntityVelocityUpdateS2CPacket(targetPlayer));
        }
    }

    // ================================================================== goals

    /**
     * Basic attack: throw a dark wave (projectile) at the target's current position. The 0.6s
     * cast matches the throw_wave animation; the projectile launches at tick 6, on the
     * palm-thrust frame (timing contract in tools/gen_drevathis_anims.py). The cooldown is
     * armed in stop() like every other goal here (balance-reviewer finding: arming it only at
     * the launch tick let goal-priority preemption re-run the wind-up with no cooldown cost -
     * never player-hostile, but inconsistent with the other three skills).
     */
    private static class ThrowWaveGoal extends Goal {
        private static final double MAX_RANGE = 24.0;
        private static final int CAST_LENGTH_TICKS = 12;
        private static final int LAUNCH_TICK = 6;
        private static final int COOLDOWN_TICKS = 40; // 2s

        private final DrevathisEntity boss;
        private int elapsedTicks;
        private LivingEntity aimTarget;

        ThrowWaveGoal(DrevathisEntity boss) {
            this.boss = boss;
            this.setControls(EnumSet.of(Goal.Control.MOVE, Goal.Control.LOOK));
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
            return elapsedTicks < CAST_LENGTH_TICKS && aimTarget != null && aimTarget.isAlive();
        }

        @Override
        public void start() {
            aimTarget = boss.getTarget();
            elapsedTicks = 0;
            boss.getNavigation().stop();
            boss.triggerAnim(MAIN_CONTROLLER, "throw_wave");
            boss.playSound(SoundEvents.ENTITY_EVOKER_CAST_SPELL, 1.4F, 0.75F);
        }

        @Override
        public void stop() {
            aimTarget = null;
            boss.waveCooldownTicks = COOLDOWN_TICKS;
        }

        @Override
        public void tick() {
            if (aimTarget == null) {
                return;
            }
            boss.getLookControl().lookAt(aimTarget, 60.0F, 60.0F);
            elapsedTicks++;
            if (elapsedTicks == LAUNCH_TICK && boss.getEntityWorld() instanceof ServerWorld serverWorld) {
                // (cooldown armed in stop(), not here - see class javadoc)
                Vec3d origin = boss.getEntityPos().add(0.0, 2.3, 0.0);
                Vec3d aim = aimTarget.getEntityPos().add(0.0, aimTarget.getHeight() * 0.5, 0.0)
                        .subtract(origin);
                if (aim.lengthSquared() < 1.0E-7) {
                    aim = boss.getRotationVec(1.0F);
                }
                Vec3d direction = aim.normalize();
                DarkWaveProjectileEntity wave =
                        new DarkWaveProjectileEntity(ModEntities.DARK_WAVE, serverWorld);
                wave.setOwner(boss);
                Vec3d spawnPos = origin.add(direction.multiply(0.9));
                wave.setPosition(spawnPos.x, spawnPos.y, spawnPos.z);
                wave.setVelocity(direction.multiply(DarkWaveProjectileEntity.SPEED));
                serverWorld.spawnEntity(wave);
                boss.playSound(SoundEvents.ENTITY_WITHER_SHOOT, 1.2F, 0.7F);
            }
        }
    }

    /**
     * Curse Ground: a 1.6s channel (the curse_ground animation - blade raised to the sky, then
     * scythed into the earth); the zone erupts at tick 20, the downward-slash frame. Zone
     * effects (burn/slow/visuals) are ticked by the entity itself so they persist after the
     * Goal ends - see tickCurseZone()/tickDreadSlow().
     */
    private static class CurseGroundGoal extends Goal {
        private static final double TRIGGER_RANGE = 12.0;
        private static final int CHANNEL_TICKS = 32;
        private static final int ERUPT_TICK = 20;
        private static final int COOLDOWN_TICKS = 300; // 15s

        private final DrevathisEntity boss;
        private int elapsedTicks;

        CurseGroundGoal(DrevathisEntity boss) {
            this.boss = boss;
            this.setControls(EnumSet.of(Goal.Control.MOVE, Goal.Control.LOOK));
        }

        @Override
        public boolean canStart() {
            if (boss.curseCooldownTicks > 0) {
                return false;
            }
            LivingEntity target = boss.getTarget();
            return target != null && target.isAlive() && boss.distanceTo(target) <= TRIGGER_RANGE;
        }

        @Override
        public boolean shouldContinue() {
            return elapsedTicks < CHANNEL_TICKS;
        }

        @Override
        public void start() {
            elapsedTicks = 0;
            boss.getNavigation().stop();
            boss.triggerAnim(MAIN_CONTROLLER, "curse_ground");
            boss.playSound(SoundEvents.ENTITY_EVOKER_PREPARE_ATTACK, 1.6F, 0.6F);
        }

        @Override
        public void tick() {
            LivingEntity target = boss.getTarget();
            if (target != null) {
                boss.getLookControl().lookAt(target, 30.0F, 30.0F);
            }
            elapsedTicks++;
            if (elapsedTicks == ERUPT_TICK && boss.getEntityWorld() instanceof ServerWorld serverWorld) {
                boss.activateCurseZone(serverWorld);
            }
        }

        /** Cooldown armed here, not at the eruption tick - consistent with the other three
         *  goals (balance-reviewer finding). */
        @Override
        public void stop() {
            boss.curseCooldownTicks = COOLDOWN_TICKS;
        }
    }

    /**
     * Stampede: up to three horns-first charge passes. Each pass locks a direction at the
     * target's current position and dashes along it via direct per-tick velocity (navigation
     * pathing is too clumsy at this speed); players clipped by the charge take 50 damage, get
     * launched skyward, and a dark burst erupts on them - each player can be hit once PER PASS
     * (so the spec's "up to 3 hits" happens only if they're caught by every pass). The
     * stampede_run animation loops while the synced STAMPEDE_ACTIVE flag is up.
     */
    private static class StampedeGoal extends Goal {
        private static final double MIN_RANGE = 5.0;
        private static final double MAX_RANGE = 28.0;
        private static final int PASSES = 3;
        private static final double CHARGE_SPEED = 0.85; // blocks/tick
        private static final double HIT_RADIUS = 2.2;
        private static final float HIT_DAMAGE = 50.0F;
        private static final double LAUNCH_VELOCITY = 0.95;
        private static final int PASS_PAUSE_TICKS = 8;
        private static final int MAX_PASS_TICKS = 30;
        private static final int COOLDOWN_TICKS = 360; // 18s

        private final DrevathisEntity boss;
        private int passesLeft;
        private int chargeTicksRemaining;
        private int pauseTicksRemaining;
        private Vec3d chargeDirection = Vec3d.ZERO;
        private final Set<UUID> hitThisPass = new HashSet<>();

        StampedeGoal(DrevathisEntity boss) {
            this.boss = boss;
            this.setControls(EnumSet.of(Goal.Control.MOVE, Goal.Control.LOOK));
        }

        @Override
        public boolean canStart() {
            if (boss.stampedeCooldownTicks > 0) {
                return false;
            }
            LivingEntity target = boss.getTarget();
            if (target == null || !target.isAlive()) {
                return false;
            }
            double distance = boss.distanceTo(target);
            return distance >= MIN_RANGE && distance <= MAX_RANGE;
        }

        @Override
        public boolean shouldContinue() {
            return passesLeft > 0 || chargeTicksRemaining > 0;
        }

        @Override
        public void start() {
            passesLeft = PASSES;
            pauseTicksRemaining = 0;
            chargeTicksRemaining = 0;
            boss.getNavigation().stop();
            boss.setStampedeActive(true);
            boss.playSound(SoundEvents.ENTITY_RAVAGER_ROAR, 1.8F, 0.75F);
            beginPass();
        }

        @Override
        public void stop() {
            boss.setStampedeActive(false);
            boss.stampedeCooldownTicks = COOLDOWN_TICKS;
            chargeTicksRemaining = 0;
            passesLeft = 0;
        }

        private void beginPass() {
            LivingEntity target = boss.getTarget();
            if (target == null || !target.isAlive()) {
                passesLeft = 0;
                return;
            }
            Vec3d toTarget = target.getEntityPos().subtract(boss.getEntityPos());
            Vec3d flat = new Vec3d(toTarget.x, 0.0, toTarget.z);
            if (flat.lengthSquared() < 1.0E-7) {
                passesLeft = 0;
                return;
            }
            chargeDirection = flat.normalize();
            // overshoot a little past the target so the charge reads as a run-through, not a stop
            chargeTicksRemaining = Math.min(MAX_PASS_TICKS, (int) (flat.length() / CHARGE_SPEED) + 5);
            hitThisPass.clear();
            passesLeft--;
        }

        @Override
        public void tick() {
            if (pauseTicksRemaining > 0) {
                pauseTicksRemaining--;
                LivingEntity target = boss.getTarget();
                if (target != null) {
                    boss.getLookControl().lookAt(target, 60.0F, 60.0F);
                }
                if (pauseTicksRemaining == 0) {
                    beginPass();
                }
                return;
            }
            if (chargeTicksRemaining <= 0) {
                return;
            }

            boss.setVelocity(chargeDirection.x * CHARGE_SPEED, boss.getVelocity().y,
                    chargeDirection.z * CHARGE_SPEED);
            boss.velocityDirty = true;
            Vec3d ahead = boss.getEntityPos().add(chargeDirection.multiply(4.0));
            boss.getLookControl().lookAt(ahead.x, boss.getEyeY(), ahead.z, 90.0F, 90.0F);

            if (boss.getEntityWorld() instanceof ServerWorld serverWorld) {
                serverWorld.spawnParticles(ParticleTypes.SQUID_INK,
                        boss.getX(), boss.getY() + 0.2, boss.getZ(), 4, 0.5, 0.1, 0.5, 0.02);
                for (ServerPlayerEntity player : serverWorld.getPlayers()) {
                    if (player.isAlive() && !player.isSpectator()
                            && !hitThisPass.contains(player.getUuid())
                            && boss.squaredDistanceTo(player) <= HIT_RADIUS * HIT_RADIUS) {
                        hitThisPass.add(player.getUuid());
                        player.damage(serverWorld, boss.getDamageSources().mobAttack(boss), HIT_DAMAGE);
                        launchUpward(player, LAUNCH_VELOCITY);
                        serverWorld.spawnParticles(ParticleTypes.SQUID_INK, player.getX(),
                                player.getY() + 1.0, player.getZ(), 30, 0.5, 0.7, 0.5, 0.08);
                        serverWorld.spawnParticles(ParticleTypes.SCULK_SOUL, player.getX(),
                                player.getY() + 1.2, player.getZ(), 10, 0.4, 0.5, 0.4, 0.04);
                        boss.playSound(SoundEvents.ENTITY_RAVAGER_ATTACK, 1.5F, 0.85F);
                    }
                }
            }

            chargeTicksRemaining--;
            if (chargeTicksRemaining <= 0 && passesLeft > 0) {
                pauseTicksRemaining = PASS_PAUSE_TICKS;
            }
        }
    }

    /**
     * The End is Near: triggered when a target stands within 5 blocks. A 100-tick (5s) channel
     * matching the end_channel animation exactly: players inside the 9-block area are slowly
     * pulled toward the boss (0.09 blocks/tick applied every other tick - beatable by simply
     * walking out, even with the area's -25% penalty, so dashes/speed builds escape easily)
     * while fire comets rain down NEAR players - impact points are offset 1.5-4.5 blocks from
     * their victim, so each comet telegraphs itself during its 12-tick fall and is dodged by
     * watching the sky. Anyone still inside when the channel completes takes 100 damage and
     * flees in terror for 2 seconds.
     */
    private static class EndIsNearGoal extends Goal {
        static final double AREA_RADIUS = 9.0;
        private static final double TRIGGER_RANGE = 5.0;
        private static final int CHANNEL_TICKS = 100; // 5s = end_channel animation length
        private static final double PULL_STRENGTH = 0.09;
        private static final int PULL_INTERVAL_TICKS = 2;
        private static final int COMET_START_TICK = 20;
        private static final int COMET_INTERVAL_TICKS = 8;
        private static final int COMET_FALL_TICKS = 12;
        private static final double COMET_MIN_OFFSET = 1.5;
        private static final double COMET_MAX_OFFSET = 4.5;
        private static final double COMET_HIT_RADIUS = 2.2;
        private static final float COMET_DAMAGE = 30.0F;
        private static final float FINALE_DAMAGE = 100.0F;
        private static final int FEAR_DURATION_TICKS = 40; // 2s
        private static final int COOLDOWN_TICKS = 720; // 36s

        private final DrevathisEntity boss;
        private int elapsedTicks;
        private boolean finished;
        private final List<Comet> comets = new ArrayList<>();

        EndIsNearGoal(DrevathisEntity boss) {
            this.boss = boss;
            this.setControls(EnumSet.of(Goal.Control.MOVE, Goal.Control.LOOK));
        }

        @Override
        public boolean canStart() {
            if (boss.endCooldownTicks > 0) {
                return false;
            }
            LivingEntity target = boss.getTarget();
            return target != null && target.isAlive() && boss.distanceTo(target) <= TRIGGER_RANGE;
        }

        @Override
        public boolean shouldContinue() {
            return !finished;
        }

        @Override
        public void start() {
            elapsedTicks = 0;
            finished = false;
            comets.clear();
            boss.getNavigation().stop();
            boss.endPullActive = true;
            boss.triggerAnim(MAIN_CONTROLLER, "end_channel");
            boss.playSound(SoundEvents.ENTITY_WITHER_SPAWN, 2.0F, 0.65F);
        }

        @Override
        public void stop() {
            boss.endPullActive = false;
            boss.endCooldownTicks = COOLDOWN_TICKS;
            comets.clear();
        }

        @Override
        public void tick() {
            if (!(boss.getEntityWorld() instanceof ServerWorld serverWorld)) {
                finished = true;
                return;
            }
            elapsedTicks++;

            spawnVortexParticles(serverWorld);
            if (elapsedTicks % PULL_INTERVAL_TICKS == 0) {
                pullPlayers(serverWorld);
            }
            // no launches after (CHANNEL - FALL): a comet must land BEFORE the channel ends,
            // or stop()'s comets.clear() would erase it mid-air (balance-reviewer finding)
            if (elapsedTicks >= COMET_START_TICK
                    && elapsedTicks <= CHANNEL_TICKS - COMET_FALL_TICKS
                    && (elapsedTicks - COMET_START_TICK) % COMET_INTERVAL_TICKS == 0) {
                launchComet(serverWorld);
            }
            tickComets(serverWorld);

            if (elapsedTicks >= CHANNEL_TICKS) {
                finale(serverWorld);
                finished = true;
            }
        }

        private void spawnVortexParticles(ServerWorld world) {
            // ring marking the area boundary + souls drifting inward toward the boss
            double angle = (elapsedTicks % 20) / 20.0 * Math.PI * 2.0;
            for (int i = 0; i < 3; i++) {
                double a = angle + i * (Math.PI * 2.0 / 3.0);
                double px = boss.getX() + Math.cos(a) * AREA_RADIUS;
                double pz = boss.getZ() + Math.sin(a) * AREA_RADIUS;
                world.spawnParticles(ParticleTypes.SCULK_SOUL, px, boss.getY() + 0.3, pz,
                        1, 0.05, 0.1, 0.05, 0.0);
                // count 0 = directional: drift the ink inward
                world.spawnParticles(ParticleTypes.SQUID_INK, px, boss.getY() + 1.0, pz,
                        0, -Math.cos(a) * 0.25, 0.02, -Math.sin(a) * 0.25, 1.0);
            }
        }

        private void pullPlayers(ServerWorld world) {
            for (ServerPlayerEntity player : world.getPlayers()) {
                if (!player.isAlive() || player.isSpectator() || player.isCreative()) {
                    continue;
                }
                if (boss.squaredDistanceTo(player) > AREA_RADIUS * AREA_RADIUS) {
                    continue;
                }
                Vec3d toBoss = boss.getEntityPos().subtract(player.getEntityPos());
                Vec3d flat = new Vec3d(toBoss.x, 0.0, toBoss.z);
                if (flat.lengthSquared() < 1.0) {
                    continue; // already at the boss's feet - nothing to pull
                }
                Vec3d pull = flat.normalize().multiply(PULL_STRENGTH);
                player.addVelocity(pull.x, 0.0, pull.z);
                player.networkHandler.sendPacket(new EntityVelocityUpdateS2CPacket(player));
            }
        }

        private void launchComet(ServerWorld world) {
            // aim near a random player in the area; never dead-center on them
            List<ServerPlayerEntity> candidates = new ArrayList<>();
            for (ServerPlayerEntity player : world.getPlayers()) {
                if (player.isAlive() && !player.isSpectator()
                        && boss.squaredDistanceTo(player) <= AREA_RADIUS * AREA_RADIUS) {
                    candidates.add(player);
                }
            }
            double baseX;
            double baseZ;
            double groundY;
            if (candidates.isEmpty()) {
                double a = boss.random.nextDouble() * Math.PI * 2.0;
                double d = boss.random.nextDouble() * AREA_RADIUS;
                baseX = boss.getX() + Math.cos(a) * d;
                baseZ = boss.getZ() + Math.sin(a) * d;
                groundY = boss.getY();
            } else {
                ServerPlayerEntity victim = candidates.get(boss.random.nextInt(candidates.size()));
                double a = boss.random.nextDouble() * Math.PI * 2.0;
                double offset = COMET_MIN_OFFSET
                        + boss.random.nextDouble() * (COMET_MAX_OFFSET - COMET_MIN_OFFSET);
                baseX = victim.getX() + Math.cos(a) * offset;
                baseZ = victim.getZ() + Math.sin(a) * offset;
                groundY = victim.getY();
            }
            comets.add(new Comet(baseX, baseZ, groundY, COMET_FALL_TICKS));
            world.playSound(null, baseX, groundY + 12.0, baseZ, SoundEvents.ENTITY_BLAZE_SHOOT,
                    boss.getSoundCategory(), 1.2F, 0.6F);
        }

        private void tickComets(ServerWorld world) {
            Iterator<Comet> iterator = comets.iterator();
            while (iterator.hasNext()) {
                Comet comet = iterator.next();
                comet.ticksUntilImpact--;
                double height = comet.groundY + 1.0 + comet.ticksUntilImpact * 1.6;
                world.spawnParticles(ParticleTypes.FLAME, comet.x, height, comet.z,
                        5, 0.15, 0.3, 0.15, 0.012);
                world.spawnParticles(ParticleTypes.SMOKE, comet.x, height + 0.6, comet.z,
                        3, 0.15, 0.4, 0.15, 0.008);
                if (comet.ticksUntilImpact <= 0) {
                    world.spawnParticles(ParticleTypes.FLAME, comet.x, comet.groundY + 0.3, comet.z,
                            28, 0.9, 0.3, 0.9, 0.05);
                    world.spawnParticles(ParticleTypes.LAVA, comet.x, comet.groundY + 0.3, comet.z,
                            6, 0.5, 0.2, 0.5, 0.0);
                    world.spawnParticles(ParticleTypes.LARGE_SMOKE, comet.x, comet.groundY + 0.5,
                            comet.z, 10, 0.6, 0.4, 0.6, 0.02);
                    world.playSound(null, comet.x, comet.groundY, comet.z,
                            SoundEvents.ENTITY_GENERIC_EXPLODE.value(), boss.getSoundCategory(),
                            1.1F, 1.15F);
                    DamageSource source = boss.getDamageSources().indirectMagic(boss, boss);
                    for (ServerPlayerEntity player : world.getPlayers()) {
                        if (player.isAlive() && !player.isSpectator()) {
                            double dx = player.getX() - comet.x;
                            double dy = player.getY() - comet.groundY;
                            double dz = player.getZ() - comet.z;
                            if (dx * dx + dz * dz <= COMET_HIT_RADIUS * COMET_HIT_RADIUS
                                    && Math.abs(dy) <= 3.0) {
                                player.damage(world, source, COMET_DAMAGE);
                            }
                        }
                    }
                    iterator.remove();
                }
            }
        }

        private void finale(ServerWorld world) {
            world.spawnParticles(ParticleTypes.SONIC_BOOM, boss.getX(), boss.getY() + 2.0,
                    boss.getZ(), 1, 0.0, 0.0, 0.0, 0.0);
            int points = 28;
            for (int i = 0; i < points; i++) {
                double a = (Math.PI * 2.0 * i) / points;
                world.spawnParticles(ParticleTypes.SQUID_INK,
                        boss.getX() + Math.cos(a) * 2.0, boss.getY() + 1.0,
                        boss.getZ() + Math.sin(a) * 2.0,
                        0, Math.cos(a) * 0.5, 0.1, Math.sin(a) * 0.5, 1.0);
            }
            boss.playSound(SoundEvents.ENTITY_WARDEN_SONIC_BOOM, 2.0F, 0.8F);
            DamageSource source = boss.getDamageSources().indirectMagic(boss, boss);
            for (ServerPlayerEntity player : world.getPlayers()) {
                if (player.isAlive() && !player.isSpectator() && !player.isCreative()
                        && boss.squaredDistanceTo(player) <= AREA_RADIUS * AREA_RADIUS) {
                    player.damage(world, source, FINALE_DAMAGE);
                    boss.startFear(player, FEAR_DURATION_TICKS);
                }
            }
        }

        private static class Comet {
            final double x;
            final double z;
            final double groundY;
            int ticksUntilImpact;

            Comet(double x, double z, double groundY, int ticksUntilImpact) {
                this.x = x;
                this.z = z;
                this.groundY = groundY;
                this.ticksUntilImpact = ticksUntilImpact;
            }
        }
    }

    /**
     * Closes distance whenever the target is beyond the basic attack's range - the same
     * anti-kiting guard the previous kit needed (balance-reviewer finding there): without it,
     * a player beyond every skill's trigger range would never be approached at all.
     */
    private static class ApproachGoal extends Goal {
        private static final double ENGAGE_RANGE = 20.0;
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
