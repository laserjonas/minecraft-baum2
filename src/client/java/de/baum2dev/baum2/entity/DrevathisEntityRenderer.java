package de.baum2dev.baum2.entity;

import net.minecraft.client.render.entity.EntityRendererFactory;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

/**
 * GeckoLib renderer for the reworked Drevathis (third GeckoLib boss after Spider Queen and
 * Zombie Colossus), replacing the old MobEntityRenderer + hand-written DrevathisEntityModel +
 * DrevathisHeldWeaponFeatureRenderer stack. The cursed blade is real geometry in the GeckoLib
 * model now (a "blade" bone on the right forearm), so no held-item feature is attached - the
 * entity equips nothing (see DrevathisEntity.dropLoot for the item drop). withScale(1.8F)
 * replaces the old ModelTransformer.scaling(1.8F) model-layer transform, matching the
 * unchanged 1.08x3.24 hitbox.
 */
public class DrevathisEntityRenderer extends GeoEntityRenderer<DrevathisEntity, DrevathisRenderState> {
    public DrevathisEntityRenderer(EntityRendererFactory.Context context) {
        super(context, new DrevathisGeoModel());
        this.withScale(1.8F);
    }

    @Override
    public DrevathisRenderState createRenderState(DrevathisEntity animatable, @Nullable Void relatedObject) {
        return new DrevathisRenderState();
    }
}
