package de.baum2dev.baum2.entity;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import de.baum2dev.baum2.mounts.MountTier;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animatable.manager.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.animation.object.PlayState;
import software.bernie.geckolib.animation.state.AnimationTest;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * The summoned, player-steered mount (one EntityType per MountTier, all sharing this class -
 * same definition-injection pattern as FallenCometStoneEntity). Extends PathAwareEntity, NOT
 * AbstractHorseEntity, deliberately: the horse base class drags in taming/breeding/saddle-bag
 * GUI/saddle-requires-tame gating that a summoned always-rideable mount has to fight against
 * (see docs/fabric-modding.md "Custom rideable mount entity"). The four controlling-passenger
 * overrides below are the entire vanilla riding contract - WASD/sprint input plumbing and
 * passenger sync are vanilla-automatic, and getStepHeight() already forces a 1.0 step-up
 * while player-controlled (the attribute is still set explicitly for the unridden case).
 *
 * Ephemeral by design: removePassenger discards the mount on ANY dismount path (sneak,
 * death, teleport, disconnect) - the flute in the equipment slot is the persistent mount,
 * not this entity. No AI goals on purpose: it never exists without a rider for more than an
 * instant, so it needs no wander/look behavior.
 */
public class MountHorseEntity extends PathAwareEntity implements GeoEntity {

    private static final RawAnimation IDLE_ANIM = RawAnimation.begin().thenLoop("animation.mount_horse.idle");
    private static final RawAnimation WALK_ANIM = RawAnimation.begin().thenLoop("animation.mount_horse.walk");
    private static final RawAnimation ATTACK_ANIM = RawAnimation.begin().thenPlay("animation.mount_horse.attack");
    private static final String CONTROLLER_NAME = "mount_controller";
    private static final String ATTACK_TRIGGER = "attack";

    private final MountTier tier;
    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);

    public MountHorseEntity(EntityType<? extends MountHorseEntity> entityType, World world, MountTier tier) {
        super(entityType, world);
        this.tier = tier;
    }

    public static DefaultAttributeContainer.Builder createMountAttributes(MountTier tier) {
        return MobEntity.createMobAttributes()
                .add(EntityAttributes.MAX_HEALTH, tier.maxHealth())
                .add(EntityAttributes.MOVEMENT_SPEED, tier.movementSpeed())
                // Vanilla-horse convention; only matters while unridden (ridden mounts get a
                // free 1.0 floor from LivingEntity.getStepHeight()).
                .add(EntityAttributes.STEP_HEIGHT, 1.0);
    }

    public MountTier tier() {
        return tier;
    }

    // ---- vanilla controlling-passenger contract ----

    @Override
    @Nullable
    public LivingEntity getControllingPassenger() {
        // No saddle gating - a summoned mount is always controllable by its rider.
        return this.getFirstPassenger() instanceof PlayerEntity player ? player : null;
    }

    @Override
    protected Vec3d getControlledMovementInput(PlayerEntity controllingPlayer, Vec3d movementInput) {
        float sideways = controllingPlayer.sidewaysSpeed * 0.5F;
        float forward = controllingPlayer.forwardSpeed;
        if (forward <= 0.0F) {
            forward *= 0.25F; // backing up is slow, matching the feel of every rideable vanilla mob
        }
        return new Vec3d(sideways, 0.0, forward);
    }

    @Override
    protected float getSaddledSpeed(PlayerEntity controllingPlayer) {
        // Sprint scaling is automatic: the player's own sprint state already scales the
        // forward/sideways input before it reaches the vehicle.
        return (float) this.getAttributeValue(EntityAttributes.MOVEMENT_SPEED);
    }

    @Override
    protected void tickControlled(PlayerEntity controllingPlayer, Vec3d movementInput) {
        super.tickControlled(controllingPlayer, movementInput);
        this.setYaw(controllingPlayer.getYaw());
        this.setPitch(controllingPlayer.getPitch() * 0.5F);
        this.bodyYaw = this.headYaw = this.getYaw();
    }

    @Override
    public boolean isPushable() {
        return !this.hasPassengers();
    }

    /** Fires for every dismount path (sneak, teleport, rider death/disconnect, vehicle death)
     *  - the summoned mount never outlives its rider's ride. */
    @Override
    protected void removePassenger(Entity passenger) {
        super.removePassenger(passenger);
        if (passenger instanceof PlayerEntity && !this.getEntityWorld().isClient()) {
            this.discard();
        }
    }

    // ---- GeckoLib ----

    /** Server-side trigger for the one-shot attack animation - GeckoLib broadcasts it to all
     *  tracking clients automatically (docs/fabric-modding.md, "triggerable one-shot animations"). */
    public void playAttackAnimation() {
        this.triggerAnim(CONTROLLER_NAME, ATTACK_TRIGGER);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(CONTROLLER_NAME, 5, this::handleAnimationState)
                .triggerableAnim(ATTACK_TRIGGER, ATTACK_ANIM));
    }

    private PlayState handleAnimationState(AnimationTest<MountHorseEntity> test) {
        return test.setAndContinue(test.isMoving() ? WALK_ANIM : IDLE_ANIM);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.geoCache;
    }
}
