package de.baum2dev.baum2.entity;

import net.minecraft.util.Identifier;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;

/**
 * GeckoLib model for Spider Queen - resolves spider_queen.geo.json/animation.json/texture by
 * convention from just the entity name (see DefaultedEntityGeoModel's own javadoc: it auto-sorts
 * into the "entity" subfolder), replacing the old hand-coded SpiderQueenEntityModel that reused
 * vanilla's SpiderEntityModel geometry. "head" matches the geo.json bone name, enabling
 * GeckoLib's automatic head-turning.
 */
public class SpiderQueenGeoModel extends DefaultedEntityGeoModel<SpiderQueenEntity> {
    public SpiderQueenGeoModel() {
        super(Identifier.of("baum2", "spider_queen"), "head");
    }
}
