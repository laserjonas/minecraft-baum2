package de.baum2dev.baum2.entity;

import net.minecraft.client.render.entity.EntityRendererFactory;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

/**
 * GeckoLib renderer for Zombie Colossus, replacing the old MobEntityRenderer + hand-written
 * ZombieColossusEntityModel + HeldItemFeatureRenderer stack. The Colossal Warclub is real
 * geometry in the GeckoLib model now (a "club" bone on the right forearm), so no held-item
 * feature is attached - the entity equips nothing (see ZombieColossusEntity.initEquipment).
 * withScale(3.0F) replaces the old ModelTransformer.scaling(3.0F) model-layer transform, same
 * as SpiderQueenEntityRenderer.
 */
public class ZombieColossusEntityRenderer extends GeoEntityRenderer<ZombieColossusEntity, ColossusRenderState> {
    public ZombieColossusEntityRenderer(EntityRendererFactory.Context context) {
        super(context, new ZombieColossusGeoModel());
        this.withScale(3.0F);
    }

    @Override
    public ColossusRenderState createRenderState(ZombieColossusEntity animatable, @Nullable Void relatedObject) {
        return new ColossusRenderState();
    }
}
