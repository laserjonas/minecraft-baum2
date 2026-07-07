package de.baum2dev.baum2.entity;

import net.minecraft.util.Identifier;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;

/**
 * GeckoLib model for Drevathis - resolves drevathis.geo.json/animation.json/texture by
 * convention from the entity name (same pattern as SpiderQueenGeoModel and
 * ZombieColossusGeoModel). "head" matches the geo.json bone name, enabling GeckoLib's
 * automatic head-turning (the head bone is a child of "body"; GeckoLib finds it by name
 * regardless of depth).
 */
public class DrevathisGeoModel extends DefaultedEntityGeoModel<DrevathisEntity> {
    public DrevathisGeoModel() {
        super(Identifier.of("baum2", "drevathis"), "head");
    }
}
