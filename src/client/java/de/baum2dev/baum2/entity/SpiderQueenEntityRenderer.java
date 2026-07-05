package de.baum2dev.baum2.entity;

import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.render.entity.feature.SpiderEyesFeatureRenderer;
import net.minecraft.client.render.entity.model.EntityModelLayer;
import net.minecraft.client.render.entity.model.SpiderEntityModel;
import net.minecraft.client.render.entity.state.LivingEntityRenderState;
import net.minecraft.util.Identifier;

/**
 * Reuses vanilla's own SpiderEntityModel geometry (registered under its own 3x-scaled
 * EntityModelLayer - see Baum2Client) rather than a custom model, same "reskin + scale a
 * vanilla model" approach as vanilla's own Giant does with Zombie. Written by hand instead of
 * extending vanilla's generic SpiderEntityRenderer<T> because that class hardcodes a
 * non-scaled 0.8F shadow radius with no constructor to override it.
 */
public class SpiderQueenEntityRenderer
        extends MobEntityRenderer<SpiderQueenEntity, LivingEntityRenderState, SpiderEntityModel> {
    public static final EntityModelLayer LAYER =
            new EntityModelLayer(Identifier.of("baum2", "spider_queen"), "main");
    private static final Identifier TEXTURE = Identifier.of("baum2", "textures/entity/spider_queen.png");
    private static final float SCALE = 3.0F;

    public SpiderQueenEntityRenderer(EntityRendererFactory.Context context) {
        super(context, new SpiderEntityModel(context.getPart(LAYER)), 0.8F * SCALE);
        this.addFeature(new SpiderEyesFeatureRenderer<>(this));
    }

    @Override
    protected float getLyingPositionRotationDegrees() {
        return 180.0F;
    }

    @Override
    public Identifier getTexture(LivingEntityRenderState state) {
        return TEXTURE;
    }

    @Override
    public LivingEntityRenderState createRenderState() {
        return new LivingEntityRenderState();
    }
}
