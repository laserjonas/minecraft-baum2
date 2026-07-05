package de.baum2dev.baum2.entity;

import net.minecraft.client.model.Dilation;
import net.minecraft.client.model.ModelData;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.model.ModelPartBuilder;
import net.minecraft.client.model.ModelPartData;
import net.minecraft.client.model.ModelTransform;
import net.minecraft.client.model.TexturedModelData;
import net.minecraft.client.render.entity.model.BipedEntityModel;

/**
 * Bespoke "cursed sovereign" geometry - a tall, regal demonic figure (horns, a trailing cape)
 * rather than a muscle-bound brute like {@code ZombieColossusEntityModel}. Extends
 * {@code BipedEntityModel} directly (not a themed vanilla subtype like
 * {@code AbstractZombieModel}) since Drevathis isn't a reskin of an existing vanilla mob.
 * Keeps the standard part hierarchy/names {@code BipedEntityModel} itself defines, only adding
 * two new purely-cosmetic child parts (horns on the head, a cape on the body) so the inherited
 * walk-cycle math (which only ever rotates the named parts, never assumes a specific silhouette)
 * keeps working unmodified - same "bespoke geometry, same part contract" approach
 * {@code ZombieColossusEntityModel}/{@code HulkingCocoonStoneEntityModel} already established.
 *
 * <p>Every skill gets its own telegraph pose (Dash/Chain/Wave/Thunder), driven by
 * {@code DrevathisRenderState}'s four counters - "every skill must be animated" per the design
 * brief. A baseline "gripping a two-handed weapon" arm-bend is also applied every frame (not
 * just during skills), since the boss visibly wields its oversized sword at all times - see
 * {@code DrevathisHeldWeaponFeatureRenderer} for the weapon's own scale/position.
 */
public class DrevathisEntityModel extends BipedEntityModel<DrevathisRenderState> {
    // Mirrors DrevathisEntity's own private Goal WINDUP/DURATION constants (not accessible from
    // here - private, same documented reasoning as ZombieColossusEntityModel's own duplicated
    // constants) as client-only pose-duration constants.
    private static final int DASH_WINDUP_DURATION_TICKS = 10;
    private static final int CHAIN_EFFECT_DURATION_TICKS = 100;
    private static final int WAVE_CAST_DURATION_TICKS = 10;
    private static final int THUNDER_CHANNEL_DURATION_TICKS = 60;

    private static final float GRIP_INWARD_RADIANS = 0.5F;
    private static final float DASH_REACH_RADIANS = 2.2F;
    private static final float CHAIN_THROW_RADIANS = 1.4F;
    private static final float WAVE_GATHER_RADIANS = 1.0F;
    private static final float THUNDER_CHANNEL_RADIANS = 2.4F;

    public DrevathisEntityModel(ModelPart modelPart) {
        super(modelPart);
    }

    /**
     * v2 geometry - v1 (unmodified vanilla biped proportions plus two small 2x6x2 horns) read as
     * "a reskinned player" in an actual playtest ("looks like a hobbit"), the same class of
     * complaint Zombie Colossus got fixed for ("no muscles") by moving away from vanilla-default
     * cuboid sizes. This pass broadens the torso/arms, lengthens the horns substantially, adds a
     * much larger cape, and adds small clawed fingertips on both hands - a silhouette that no
     * longer reads as human-proportioned even before the texture is considered.
     */
    public static TexturedModelData getTexturedModelData() {
        ModelData modelData = new ModelData();
        ModelPartData root = modelData.getRoot();

        ModelPartData head = root.addChild("head",
                ModelPartBuilder.create().uv(0, 0).cuboid(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F, Dilation.NONE),
                ModelTransform.origin(0.0F, 0.0F, 0.0F));
        head.addChild("hat",
                ModelPartBuilder.create().uv(32, 0).cuboid(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F, Dilation.NONE.add(0.5F)),
                ModelTransform.NONE);
        // Long, dramatically back-swept horns (2x9x2, up from 2x6x2) - purely cosmetic children
        // of the head, no effect on vanilla's head-tracking rotation math.
        head.addChild("horn_right",
                ModelPartBuilder.create().uv(36, 36).cuboid(-1.0F, -5.0F, 0.0F, 2.0F, 9.0F, 2.0F, Dilation.NONE),
                ModelTransform.of(-3.0F, -8.0F, 1.0F, 0.7F, 0.0F, -0.4F));
        head.addChild("horn_left",
                ModelPartBuilder.create().uv(44, 36).cuboid(-1.0F, -5.0F, 0.0F, 2.0F, 9.0F, 2.0F, Dilation.NONE),
                ModelTransform.of(3.0F, -8.0F, 1.0F, 0.7F, 0.0F, 0.4F));

        // Broad chest (8x12x4 -> 9x13x5) - vanilla-default proportions were the "hobbit" problem
        // in the first place; this is deliberately closer to Zombie Colossus's own "broad body"
        // fix than to vanilla's stock biped size.
        ModelPartData body = root.addChild("body",
                ModelPartBuilder.create().uv(0, 16).cuboid(-4.5F, 0.0F, -2.5F, 9.0F, 13.0F, 5.0F, Dilation.NONE),
                ModelTransform.origin(0.0F, 0.0F, 0.0F));
        // Trailing cape - substantially larger (10x18x1, up from 9x16x1) and starting higher on
        // the back, for a "sovereign's cloak" silhouette rather than a small back-flap.
        body.addChild("cape",
                ModelPartBuilder.create().uv(28, 16).cuboid(-5.0F, -1.0F, 0.0F, 10.0F, 18.0F, 1.0F, Dilation.NONE),
                ModelTransform.of(0.0F, -1.0F, 2.7F, 0.18F, 0.0F, 0.0F));

        ModelPartData rightArm = root.addChild("right_arm",
                ModelPartBuilder.create().uv(0, 36).cuboid(-3.0F, -2.0F, -2.5F, 5.0F, 13.0F, 5.0F, Dilation.NONE),
                ModelTransform.origin(-5.5F, 2.0F, 0.0F));
        ModelPartData leftArm = root.addChild("left_arm",
                ModelPartBuilder.create().uv(0, 36).mirrored().cuboid(-2.0F, -2.0F, -2.5F, 5.0F, 13.0F, 5.0F, Dilation.NONE),
                ModelTransform.origin(5.5F, 2.0F, 0.0F));
        // Clawed fingertips - tiny cosmetic children at each hand, the cheapest possible way to
        // read as "not a human hand" in silhouette.
        rightArm.addChild("claw_right",
                ModelPartBuilder.create().uv(52, 36).cuboid(-1.5F, 0.0F, -1.0F, 3.0F, 2.0F, 2.0F, Dilation.NONE),
                ModelTransform.origin(0.0F, 11.0F, 0.0F));
        leftArm.addChild("claw_left",
                ModelPartBuilder.create().uv(52, 36).mirrored().cuboid(-1.5F, 0.0F, -1.0F, 3.0F, 2.0F, 2.0F, Dilation.NONE),
                ModelTransform.origin(0.0F, 11.0F, 0.0F));

        root.addChild("right_leg",
                ModelPartBuilder.create().uv(20, 36).cuboid(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F, Dilation.NONE),
                ModelTransform.origin(-1.9F, 12.0F, 0.0F));
        root.addChild("left_leg",
                ModelPartBuilder.create().uv(20, 36).mirrored().cuboid(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F, Dilation.NONE),
                ModelTransform.origin(1.9F, 12.0F, 0.0F));

        return TexturedModelData.of(modelData, 64, 64);
    }

    @Override
    public void setAngles(DrevathisRenderState state) {
        super.setAngles(state);

        // Baseline two-handed grip: both arms bend inward toward a shared central point in
        // front of the torso, every frame - the boss always visibly wields its oversized sword
        // with both hands, not just mid-skill. DrevathisHeldWeaponFeatureRenderer anchors the
        // actual weapon render at the root/right-arm transform this produces.
        this.rightArm.pitch -= GRIP_INWARD_RADIANS;
        this.rightArm.yaw -= GRIP_INWARD_RADIANS * 0.6F;
        this.leftArm.pitch -= GRIP_INWARD_RADIANS;
        this.leftArm.yaw += GRIP_INWARD_RADIANS * 0.6F;

        if (state.dashWindupTicks > 0) {
            applyDashWindupPose(state);
        }
        if (state.chainEffectTicks > 0) {
            applyChainEffectPose(state);
        }
        if (state.waveCastTicks > 0) {
            applyWaveCastPose(state);
        }
        if (state.thunderChannelTicks > 0) {
            applyThunderChannelPose(state);
        }
    }

    /** Reach-and-grab-upward: both arms sweep up and forward, easing in as the post-teleport
     *  0.5s wind-up counts down to the moment the target launches skyward. */
    private void applyDashWindupPose(DrevathisRenderState state) {
        float progress = 1.0F - (state.dashWindupTicks / (float) DASH_WINDUP_DURATION_TICKS);
        this.rightArm.pitch -= progress * DASH_REACH_RADIANS;
        this.leftArm.pitch -= progress * DASH_REACH_RADIANS;
        this.body.pitch -= progress * 0.2F;
    }

    /** A throw motion (right arm extends out) easing into a sustained "gripping a taut chain"
     *  hold for the entire 5s slow duration, not just the initial throw instant. */
    private void applyChainEffectPose(DrevathisRenderState state) {
        float total = CHAIN_EFFECT_DURATION_TICKS;
        float elapsed = total - state.chainEffectTicks;
        float throwProgress = Math.min(1.0F, elapsed / 6.0F); // quick initial throw, ~0.3s
        this.rightArm.pitch -= throwProgress * CHAIN_THROW_RADIANS;
        this.rightArm.roll += throwProgress * 0.4F;
    }

    /** Arms gather energy inward/upward during the 0.5s cast-time telegraph. */
    private void applyWaveCastPose(DrevathisRenderState state) {
        float progress = 1.0F - (state.waveCastTicks / (float) WAVE_CAST_DURATION_TICKS);
        this.rightArm.pitch -= progress * WAVE_GATHER_RADIANS;
        this.leftArm.pitch -= progress * WAVE_GATHER_RADIANS;
        this.head.pitch -= progress * 0.15F;
    }

    /** Sustained overhead channeling pose held for the entire 3s Thunder of Darkness duration. */
    private void applyThunderChannelPose(DrevathisRenderState state) {
        float total = THUNDER_CHANNEL_DURATION_TICKS;
        float elapsed = total - state.thunderChannelTicks;
        float progress = Math.min(1.0F, elapsed / 8.0F); // quick ease-in, then held
        this.rightArm.pitch -= progress * THUNDER_CHANNEL_RADIANS;
        this.leftArm.pitch -= progress * THUNDER_CHANNEL_RADIANS;
        this.body.pitch -= progress * 0.25F;
        this.head.pitch -= progress * 0.3F;
    }
}
