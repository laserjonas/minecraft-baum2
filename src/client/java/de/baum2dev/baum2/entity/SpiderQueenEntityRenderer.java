package de.baum2dev.baum2.entity;

import net.minecraft.client.render.entity.EntityRendererFactory;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

/**
 * GeckoLib renderer for Spider Queen, replacing the old hand-written MobEntityRenderer subclass.
 * withScale(3.0F) is GeckoLib's own equivalent of the old ModelTransformer.scaling(3.0F) call
 * (GeckoLib doesn't use EntityModelLayerRegistry at all, see docs/fabric-modding.md's "GeckoLib
 * integration" section, part D).
 */
public class SpiderQueenEntityRenderer extends GeoEntityRenderer<SpiderQueenEntity, SpiderQueenRenderState> {
    public SpiderQueenEntityRenderer(EntityRendererFactory.Context context) {
        super(context, new SpiderQueenGeoModel());
        this.withScale(3.0F);
    }

    @Override
    public SpiderQueenRenderState createRenderState(SpiderQueenEntity animatable, @Nullable Void relatedObject) {
        return new SpiderQueenRenderState();
    }
}
