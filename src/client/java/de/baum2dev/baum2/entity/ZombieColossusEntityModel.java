package de.baum2dev.baum2.entity;

import net.minecraft.client.model.Dilation;
import net.minecraft.client.model.ModelData;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.model.ModelPartBuilder;
import net.minecraft.client.model.ModelPartData;
import net.minecraft.client.model.ModelTransform;
import net.minecraft.client.model.TexturedModelData;
import net.minecraft.client.render.entity.model.AbstractZombieModel;

/**
 * Bespoke, visibly-muscular geometry for this boss - replaces v1's unmodified reuse of {@link
 * net.minecraft.client.render.entity.model.BipedEntityModel#getModelData}, which (per direct
 * playtest feedback: "that zombie looks like it has no muscles") couldn't sell "great strength" no
 * matter how the texture was painted, since vanilla's plain biped is a uniformly-thin skeleton.
 * Keeps the exact same standard part names/hierarchy ("head"/"hat"/"body"/"right_arm"/"left_arm"/
 * "right_leg"/"left_leg", head has child "hat") that {@code BipedEntityModel}'s constructor binds
 * via {@code ModelPart.getChild(name)} - only each cuboid's own size/origin changed (broader
 * chest/shoulders, thicker arms and legs), so the inherited {@code AbstractZombieModel}/{@code
 * BipedEntityModel} walk-cycle and attack-swing angle math (which only ever rotates/repositions
 * these named parts, never assumes a specific size) keeps working unmodified - same "custom
 * geometry, not a straight vanilla reuse, but same part-naming contract" approach already
 * established for the two stone mini-bosses' shared {@code HulkingCocoonStoneEntityModel}. Total
 * model height (head top at y=-8 to feet at y=24) is kept identical to vanilla's own biped so the
 * boss doesn't clip into the ground - see {@code docs/visual-style-guide.md} Section 18.2 for the
 * exact measurements and UV table this class implements.
 *
 * <p>Also layers two client-only telegraph poses on top of the inherited walk/attack animation,
 * driven by {@code ColossusRenderState.leapWindupTicks}/{@code rageWindupTicks} (synced from
 * {@code ZombieColossusEntity.getLeapWindupTicks()}/{@code getRageWindupTicks()}) - a crouch-and-
 * coil-with-club-drawn-back pose easing in as the leap's 10-tick wind-up counts to zero, and an
 * overhead club-raise easing in as the rage combo's 8-tick wind-up counts to zero, both resolving
 * to neutral exactly as the strike/launch fires server-side. Same "telegraph the wind-up instead
 * of standing frozen" fix already applied to Spider Queen's leap crouch
 * ({@code SpiderQueenEntityModel}), adapted for a biped club-swinger instead of an 8-legged
 * pouncer.
 */
public class ZombieColossusEntityModel extends AbstractZombieModel<ColossusRenderState> {
    // Mirrors ZombieColossusEntity's own private LeapAttackGoal.WINDUP_TICKS/RageAttackGoal.
    // WINDUP_TICKS constants (10 and 8) - not accessible from here (private, and this task's brief
    // explicitly asks not to touch that gameplay code), so duplicated here as documented,
    // client-only constants instead of exposing them.
    private static final int LEAP_WINDUP_DURATION_TICKS = 10;
    private static final int RAGE_WINDUP_DURATION_TICKS = 8;

    private static final float CROUCH_AMOUNT = 3.0F;
    private static final float BODY_LEAN_RADIANS = 0.35F;
    private static final float LEG_CROUCH_BEND_RADIANS = 0.4F;
    private static final float ARM_WINDBACK_RADIANS = 2.0F;
    private static final float OVERHEAD_RAISE_RADIANS = 2.6F;

    public ZombieColossusEntityModel(ModelPart modelPart) {
        super(modelPart);
    }

    public static TexturedModelData getTexturedModelData() {
        ModelData modelData = new ModelData();
        ModelPartData root = modelData.getRoot();

        // Head - unchanged from vanilla's own biped proportions. A hulking brute reading with a
        // comparatively small head next to a much broader body/limbs is itself part of the
        // "muscle-bound" silhouette, not an oversight.
        ModelPartData head = root.addChild("head",
                ModelPartBuilder.create().uv(0, 0).cuboid(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F, Dilation.NONE),
                ModelTransform.origin(0.0F, 0.0F, 0.0F));
        head.addChild("hat",
                ModelPartBuilder.create().uv(32, 0).cuboid(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F, Dilation.NONE.add(0.5F)),
                ModelTransform.NONE);

        // Body - broad barrel chest: width 8 -> 14, depth 4 -> 6, height unchanged (12) so the
        // ground-anchor math below (legs must still end exactly at y=24) doesn't need adjusting.
        root.addChild("body",
                ModelPartBuilder.create().uv(0, 16).cuboid(-7.0F, 0.0F, -3.0F, 14.0F, 12.0F, 6.0F, Dilation.NONE),
                ModelTransform.origin(0.0F, 0.0F, 0.0F));

        // Arms - thick, long, club-swinging limbs: 4x12x4 -> 6x14x6. Inner face flush against the
        // body's new wider edge (x=-7/+7), same flush-no-gap convention vanilla's own arms use.
        root.addChild("right_arm",
                ModelPartBuilder.create().uv(40, 16).cuboid(-6.0F, -2.0F, -3.0F, 6.0F, 14.0F, 6.0F, Dilation.NONE),
                ModelTransform.origin(-7.0F, 2.0F, 0.0F));
        root.addChild("left_arm",
                ModelPartBuilder.create().uv(40, 16).mirrored().cuboid(0.0F, -2.0F, -3.0F, 6.0F, 14.0F, 6.0F, Dilation.NONE),
                ModelTransform.origin(7.0F, 2.0F, 0.0F));

        // Legs - thick pillars: 4x12x4 -> 6x12x6, height unchanged so the feet still land exactly
        // at y=24 (confirmed ground-level convention, see HulkingCocoonStoneEntityModel's javadoc)
        // and don't clip into the floor. Small 0.2-unit inner overlap (origin +-2.9 against
        // half-width 3) mirrors vanilla's own +-1.9-against-half-width-2 trick to avoid a seam.
        root.addChild("right_leg",
                ModelPartBuilder.create().uv(0, 34).cuboid(-3.0F, 0.0F, -3.0F, 6.0F, 12.0F, 6.0F, Dilation.NONE),
                ModelTransform.origin(-2.9F, 12.0F, 0.0F));
        root.addChild("left_leg",
                ModelPartBuilder.create().uv(0, 34).mirrored().cuboid(-3.0F, 0.0F, -3.0F, 6.0F, 12.0F, 6.0F, Dilation.NONE),
                ModelTransform.origin(2.9F, 12.0F, 0.0F));

        return TexturedModelData.of(modelData, 64, 64);
    }

    @Override
    public void setAngles(ColossusRenderState state) {
        // Vanilla walk cycle (BipedEntityModel.setAngles) + the base attack swing
        // (AbstractZombieModel's ArmPosing.zombieArms, keyed off state.handSwingProgress/
        // attacking) - confirmed via decompiled source this plumbing was already correctly wired
        // in v1 (see ZombieColossusEntityRenderer's javadoc); it now just drives this much bulkier
        // geometry, so the same angular swing sweeps a far larger silhouette change and reads as
        // real weight instead of a subtle wiggle.
        super.setAngles(state);

        if (state.leapWindupTicks > 0) {
            applyLeapWindupPose(state);
        }
        if (state.rageWindupTicks > 0) {
            applyRageWindupPose(state);
        }
    }

    /** "Prepare to jump": crouches the stance, lowers the head/torso, bends both legs, and winds
     *  the club-arm back and up ready to swing forward the instant the leap launches. Progress 0 =
     *  wind-up just started, 1 = about to launch - eases back to neutral automatically as
     *  leapWindupTicks counts down to 0 (the moment the actual leap fires server-side). */
    private void applyLeapWindupPose(ColossusRenderState state) {
        float progress = 1.0F - (state.leapWindupTicks / (float) LEAP_WINDUP_DURATION_TICKS);
        float crouch = progress * CROUCH_AMOUNT;
        this.body.originY += crouch;
        this.head.originY += crouch * 0.6F;
        this.body.pitch += progress * BODY_LEAN_RADIANS;

        float legBend = progress * LEG_CROUCH_BEND_RADIANS;
        this.rightLeg.pitch -= legBend;
        this.leftLeg.pitch -= legBend;

        // Main (club) hand winds back and up, ready to drive forward on launch.
        this.rightArm.pitch -= progress * ARM_WINDBACK_RADIANS;
        this.rightArm.roll += progress * 0.3F;
    }

    /** Overhead club-raise: the main arm rises high and back, the off-arm follows partway for
     *  a two-handed read, and the torso leans back slightly - ready to slam down into the rage
     *  combo's first strike right as rageWindupTicks reaches 0. */
    private void applyRageWindupPose(ColossusRenderState state) {
        float progress = 1.0F - (state.rageWindupTicks / (float) RAGE_WINDUP_DURATION_TICKS);
        this.rightArm.pitch -= progress * OVERHEAD_RAISE_RADIANS;
        this.leftArm.pitch -= progress * (OVERHEAD_RAISE_RADIANS * 0.5F);
        this.body.pitch -= progress * BODY_LEAN_RADIANS * 0.6F;
    }
}
