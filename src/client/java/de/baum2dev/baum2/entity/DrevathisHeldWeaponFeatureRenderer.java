package de.baum2dev.baum2.entity;

import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.render.entity.model.ModelWithArms;
import net.minecraft.client.render.entity.state.ArmedEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.RotationAxis;

/**
 * Renders Drevathis's mainhand weapon oversized and centered, as a two-handed grip, instead of
 * vanilla's normal per-hand {@code HeldItemFeatureRenderer} (which renders each hand's item
 * independently at that hand's own small offset - fine for a one-handed weapon, wrong for "one
 * big sword gripped by both hands"). Reuses the render state's already-populated
 * {@code rightHandItemState} (populated by {@code BipedEntityRenderer.updateBipedRenderState}
 * in {@code DrevathisEntityRenderer.updateRenderState}, same as vanilla's own feature does) -
 * this class only changes *how* that already-baked item is positioned/scaled, not whether it
 * gets populated. See docs/fabric-modding.md "Custom two-handed held-item rendering" for the
 * full API research this is based on (this version's rendering pipeline has no
 * {@code VertexConsumerProvider}/{@code ItemRenderer.renderItem} at the point of use anymore -
 * confirmed via decompile, not assumed from older tutorials).
 *
 * <p>Attached instead of {@code HeldItemFeatureRenderer} - never both, or the mainhand item
 * would render twice.
 */
public class DrevathisHeldWeaponFeatureRenderer<S extends ArmedEntityRenderState, M extends EntityModel<S> & ModelWithArms<S>>
        extends FeatureRenderer<S, M> {
    private static final float EXTRA_SCALE = 1.8F;

    public DrevathisHeldWeaponFeatureRenderer(FeatureRendererContext<S, M> context) {
        super(context);
    }

    @Override
    public void render(MatrixStack matrices, OrderedRenderCommandQueue queue, int light, S state, float limbAngle, float limbDistance) {
        if (state.rightHandItemState.isEmpty()) {
            return;
        }
        matrices.push();
        this.getContextModel().getRootPart().applyTransform(matrices);
        // Re-orient from "held flat" (the baked item's default pose) to upright, same base
        // reorientation HeldItemFeatureRenderer itself applies before its own per-hand offset.
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-90.0F));
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180.0F));
        // Centered in front of the torso (not off to one side per-hand) and scaled up so it
        // reads as a much larger, two-handed weapon than the dropped item's own size.
        matrices.translate(0.0, 0.3, -0.9);
        matrices.scale(EXTRA_SCALE, EXTRA_SCALE, EXTRA_SCALE);
        state.rightHandItemState.render(matrices, queue, light, OverlayTexture.DEFAULT_UV, state.outlineColor);
        matrices.pop();
    }
}
