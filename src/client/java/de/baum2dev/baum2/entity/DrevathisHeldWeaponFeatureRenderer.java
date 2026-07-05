package de.baum2dev.baum2.entity;

import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.render.entity.model.ModelWithArms;
import net.minecraft.client.render.entity.state.ArmedEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Arm;
import net.minecraft.util.math.RotationAxis;

/**
 * Renders Drevathis's mainhand weapon oversized, gripped at the actual right-hand position,
 * instead of vanilla's normal per-hand {@code HeldItemFeatureRenderer} (which renders each hand's
 * item independently at that hand's own small offset - fine for a one-handed weapon, wrong for
 * "one big sword gripped by both hands"). Reuses the render state's already-populated
 * {@code rightHandItemState} (populated by {@code BipedEntityRenderer.updateBipedRenderState}
 * in {@code DrevathisEntityRenderer.updateRenderState}, same as vanilla's own feature does) -
 * this class only changes *how* that already-baked item is positioned/scaled, not whether it
 * gets populated. See docs/fabric-modding.md "Custom two-handed held-item rendering" for the
 * full API research this is based on (this version's rendering pipeline has no
 * {@code VertexConsumerProvider}/{@code ItemRenderer.renderItem} at the point of use anymore -
 * confirmed via decompile, not assumed from older tutorials).
 *
 * <p><b>Playtest fix:</b> v1 anchored only at {@code getRootPart()} and translated to a fixed
 * point directly in front of the torso's center at roughly hip height with no arm-following
 * rotation - a static "rod" sticking straight out from the middle of the body regardless of arm
 * pose, which read badly ("looks like it has a penis") and didn't actually look held. Fixed by
 * anchoring at the right arm's own current transform via {@code setArmAngle(...)} (the exact
 * mechanism vanilla's own {@code HeldItemFeatureRenderer} uses - see the class it's modeled on),
 * so the weapon inherits the arm's own "grip inward" bend from {@code DrevathisEntityModel
 * .setAngles} and moves naturally with every skill's arm-pose animation, then offset down the
 * arm's own length to the hand instead of left floating at the shoulder/torso.
 *
 * <p>Attached instead of {@code HeldItemFeatureRenderer} - never both, or the mainhand item
 * would render twice.
 */
public class DrevathisHeldWeaponFeatureRenderer<S extends ArmedEntityRenderState, M extends EntityModel<S> & ModelWithArms<S>>
        extends FeatureRenderer<S, M> {
    private static final float EXTRA_SCALE = 1.6F;

    public DrevathisHeldWeaponFeatureRenderer(FeatureRendererContext<S, M> context) {
        super(context);
    }

    @Override
    public void render(MatrixStack matrices, OrderedRenderCommandQueue queue, int light, S state, float limbAngle, float limbDistance) {
        if (state.rightHandItemState.isEmpty()) {
            return;
        }
        matrices.push();
        // Anchors at root + the right arm's own current pose (same call
        // HeldItemFeatureRenderer.renderItem itself makes) - the weapon now follows the arm's
        // "grip inward" bend and every skill's telegraph pose, instead of being glued to a fixed
        // point in front of the torso.
        this.getContextModel().setArmAngle(state, Arm.RIGHT, matrices);
        // Re-orient from "held flat" (the baked item's default pose) to upright, same base
        // reorientation HeldItemFeatureRenderer itself applies before its own per-hand offset.
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-90.0F));
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180.0F));
        // Slide down the arm's own local axis to roughly the hand/grip point (the arm cuboid is
        // ~13/16 of a block long) rather than the shoulder, nudge slightly toward center for a
        // two-handed read, and tilt the blade off the dead-straight-forward axis so it presents
        // like a raised, angled greatsword rather than a straight rod projecting from the body.
        matrices.translate(-0.05, 0.15, -0.75);
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-35.0F));
        matrices.scale(EXTRA_SCALE, EXTRA_SCALE, EXTRA_SCALE);
        state.rightHandItemState.render(matrices, queue, light, OverlayTexture.DEFAULT_UV, state.outlineColor);
        matrices.pop();
    }
}
