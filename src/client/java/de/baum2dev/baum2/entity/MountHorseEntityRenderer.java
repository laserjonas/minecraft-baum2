package de.baum2dev.baum2.entity;

import net.minecraft.client.render.entity.EntityRendererFactory;
import org.jetbrains.annotations.Nullable;
import de.baum2dev.baum2.mounts.MountTier;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

/**
 * Shared GeckoLib renderer for all three mount tiers - one class, constructed per tier with
 * that tier's texture id and render scale (the geometry is authored at Wanderross size; the
 * bigger tiers scale up, matching their scaled hitbox dimensions in ModEntities).
 */
public class MountHorseEntityRenderer extends GeoEntityRenderer<MountHorseEntity, MountHorseRenderState> {

    public MountHorseEntityRenderer(EntityRendererFactory.Context context, MountTier tier) {
        super(context, new MountHorseGeoModel(tier.id()));
        this.withScale(tier.renderScale());
    }

    @Override
    public MountHorseRenderState createRenderState(MountHorseEntity animatable, @Nullable Void relatedObject) {
        return new MountHorseRenderState();
    }
}
