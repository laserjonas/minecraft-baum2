package de.baum2dev.baum2.entity;

import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.entity.mob.HostileEntity;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

/**
 * Shared GeckoLib renderer for every "fallen comet stone" mini-boss, replacing the old
 * per-stone MobEntityRenderer + hand-written HulkingCocoonStoneEntityModel stack. One
 * generic class serves all stone bosses: they share geometry/animation and differ only by
 * texture (see FallenCometStoneGeoModel), so registration is just
 * {@code ctx -> new FallenCometStoneEntityRenderer<>(ctx, "stone_of_x")}. The geometry is
 * authored at full size (~3 blocks, matching the 3x3 hitbox), so no withScale is needed.
 */
public class FallenCometStoneEntityRenderer<T extends HostileEntity & GeoEntity>
        extends GeoEntityRenderer<T, FallenCometStoneRenderState> {

    public FallenCometStoneEntityRenderer(EntityRendererFactory.Context context, String entityName) {
        super(context, new FallenCometStoneGeoModel<>(entityName));
    }

    @Override
    public FallenCometStoneRenderState createRenderState(T animatable, @Nullable Void relatedObject) {
        return new FallenCometStoneRenderState();
    }
}
