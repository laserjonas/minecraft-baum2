package de.baum2dev.baum2.items;

import net.minecraft.util.Identifier;
import software.bernie.geckolib.model.DefaultedItemGeoModel;
import software.bernie.geckolib.renderer.GeoItemRenderer;

/**
 * Renderer for every {@link TemplateSwordItem}: the shared sword_template geometry and
 * animations with the per-sword texture swapped in - the exact item-side mirror of
 * FallenCometStoneGeoModel/MountHorseGeoModel's withAltModel/withAltAnimations approach,
 * just inverted (here the TEXTURE is the alt path, model/animations stay shared).
 *
 * Resolved paths (DefaultedItemGeoModel, subtype "item"):
 *   model:      assets/baum2/geckolib/models/item/sword_template.geo.json
 *   animations: assets/baum2/geckolib/animations/item/sword_template.animation.json
 *   texture:    assets/baum2/textures/item/<assetName>_geo.png
 *
 * Instantiated lazily per item instance via the GeoRenderProvider that Baum2Client hands to
 * TemplateSwordItem.setClientRenderProviderFactory - the vanilla item-model plumbing reaches
 * it through the "geckolib:geckolib" special model declared in assets/baum2/items/<sword>.json
 * (hand/held contexts only; gui/ground/fixed/on_shelf select the flat icon model instead,
 * same split as vanilla's own trident).
 */
public final class TemplateSwordItemRenderer extends GeoItemRenderer<TemplateSwordItem> {

    public TemplateSwordItemRenderer(String assetName) {
        super(new DefaultedItemGeoModel<TemplateSwordItem>(Identifier.of("baum2", "sword_template"))
                .withAltTexture(Identifier.of("baum2", assetName + "_geo")));
    }
}
