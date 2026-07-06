package de.baum2dev.baum2.entity;

import net.minecraft.util.Identifier;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;

/**
 * GeckoLib model for Zombie Colossus - resolves zombie_colossus.geo.json/animation.json/texture
 * by convention from the entity name (same pattern as SpiderQueenGeoModel). "head" matches the
 * geo.json bone name, enabling GeckoLib's automatic head-turning (the head bone is a child of
 * "body" in the geometry; GeckoLib finds it by name regardless of depth).
 */
public class ZombieColossusGeoModel extends DefaultedEntityGeoModel<ZombieColossusEntity> {
    public ZombieColossusGeoModel() {
        super(Identifier.of("baum2", "zombie_colossus"), "head");
    }
}
