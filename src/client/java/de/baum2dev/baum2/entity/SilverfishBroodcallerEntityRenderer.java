package de.baum2dev.baum2.entity;

import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.render.entity.model.EntityModelLayer;
import net.minecraft.client.render.entity.model.SilverfishEntityModel;
import net.minecraft.client.render.entity.state.LivingEntityRenderState;
import net.minecraft.util.Identifier;

/**
 * Reuses vanilla's SilverfishEntityModel under a 3x-scaled EntityModelLayer (registered in
 * Baum2Client) - the same "reskin + scale a vanilla model" approach the original
 * pre-GeckoLib Spider Queen used (and vanilla's own Giant). The texture is vanilla's
 * silverfish skin referenced in place, not copied; a bespoke palette is a future art-pass
 * item (docs/visual-style-guide.md conventions for boss palettes).
 */
public class SilverfishBroodcallerEntityRenderer
        extends MobEntityRenderer<SilverfishBroodcallerEntity, LivingEntityRenderState, SilverfishEntityModel> {

    public static final EntityModelLayer LAYER =
            new EntityModelLayer(Identifier.of("baum2", "silverfish_broodcaller"), "main");
    private static final Identifier TEXTURE = Identifier.ofVanilla("textures/entity/silverfish.png");
    private static final float SCALE = 3.0F;

    public SilverfishBroodcallerEntityRenderer(EntityRendererFactory.Context context) {
        super(context, new SilverfishEntityModel(context.getPart(LAYER)), 0.3F * SCALE);
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
