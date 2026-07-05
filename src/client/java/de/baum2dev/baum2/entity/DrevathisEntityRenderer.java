package de.baum2dev.baum2.entity;

import net.minecraft.client.render.entity.BipedEntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.render.entity.model.EntityModelLayer;
import net.minecraft.util.Identifier;

/**
 * Extends {@code MobEntityRenderer} directly (not {@code BipedEntityRenderer}), same reasoning
 * as {@code ZombieColossusEntityRenderer}: this class calls
 * {@code BipedEntityRenderer.updateBipedRenderState(...)} itself instead. Adds
 * {@link DrevathisHeldWeaponFeatureRenderer} instead of vanilla's {@code HeldItemFeatureRenderer}
 * (see that class's javadoc) so the mainhand sword renders oversized and two-handed rather than
 * at vanilla's normal one-hand offset/scale.
 */
public class DrevathisEntityRenderer extends MobEntityRenderer<DrevathisEntity, DrevathisRenderState, DrevathisEntityModel> {
    public static final EntityModelLayer LAYER = new EntityModelLayer(Identifier.of("baum2", "drevathis"), "main");
    private static final Identifier TEXTURE = Identifier.of("baum2", "textures/entity/drevathis.png");
    private static final float SHADOW_RADIUS = 0.5F;

    public DrevathisEntityRenderer(EntityRendererFactory.Context context) {
        super(context, new DrevathisEntityModel(context.getPart(LAYER)), SHADOW_RADIUS);
        this.addFeature(new DrevathisHeldWeaponFeatureRenderer<>(this));
    }

    @Override
    public Identifier getTexture(DrevathisRenderState state) {
        return TEXTURE;
    }

    @Override
    public DrevathisRenderState createRenderState() {
        return new DrevathisRenderState();
    }

    @Override
    public void updateRenderState(DrevathisEntity entity, DrevathisRenderState state, float tickDelta) {
        super.updateRenderState(entity, state, tickDelta);
        BipedEntityRenderer.updateBipedRenderState(entity, state, tickDelta, this.itemModelResolver);
        state.dashWindupTicks = entity.getDashWindupTicks();
        state.chainEffectTicks = entity.getChainEffectTicks();
        state.waveCastTicks = entity.getWaveCastTicks();
        state.thunderChannelTicks = entity.getThunderChannelTicks();
    }
}
