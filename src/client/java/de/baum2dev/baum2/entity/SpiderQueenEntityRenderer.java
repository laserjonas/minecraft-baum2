package de.baum2dev.baum2.entity;

import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.render.entity.model.EntityModelLayer;
import net.minecraft.util.Identifier;

/**
 * Uses a dedicated SpiderQueenEntityModel/SpiderQueenRenderState pair (not vanilla's own
 * SpiderEntityModel/LivingEntityRenderState) so the leap attack's telegraphed wind-up can drive
 * a real crouch pose - see SpiderQueenEntityModel's own javadoc for why that requires a custom
 * render state rather than reusing vanilla's. Written by hand instead of extending vanilla's
 * generic SpiderEntityRenderer<T> because that class hardcodes a non-scaled 0.8F shadow radius
 * with no constructor to override it. Note: vanilla's SpiderEyesFeatureRenderer (the glowing
 * eyes overlay regular spiders get) is generically bound to SpiderEntityModel specifically and
 * can't be attached to this custom model type - dropped rather than forced, so Spider Queen
 * currently has no eye-glow overlay.
 */
public class SpiderQueenEntityRenderer
        extends MobEntityRenderer<SpiderQueenEntity, SpiderQueenRenderState, SpiderQueenEntityModel> {
    public static final EntityModelLayer LAYER =
            new EntityModelLayer(Identifier.of("baum2", "spider_queen"), "main");
    private static final Identifier TEXTURE = Identifier.of("baum2", "textures/entity/spider_queen.png");
    private static final float SCALE = 3.0F;

    public SpiderQueenEntityRenderer(EntityRendererFactory.Context context) {
        super(context, new SpiderQueenEntityModel(context.getPart(LAYER)), 0.8F * SCALE);
    }

    @Override
    protected float getLyingPositionRotationDegrees() {
        return 180.0F;
    }

    @Override
    public Identifier getTexture(SpiderQueenRenderState state) {
        return TEXTURE;
    }

    @Override
    public SpiderQueenRenderState createRenderState() {
        return new SpiderQueenRenderState();
    }

    @Override
    public void updateRenderState(SpiderQueenEntity entity, SpiderQueenRenderState state, float tickDelta) {
        super.updateRenderState(entity, state, tickDelta);
        state.leapWindupTicks = entity.getLeapWindupTicks();
    }
}
