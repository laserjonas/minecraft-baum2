package de.baum2dev.baum2.entity;

import net.minecraft.entity.Entity;
import net.minecraft.util.Identifier;
import software.bernie.geckolib.animatable.GeoAnimatable;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;

/**
 * Shared GeckoLib model for every "fallen comet stone" mini-boss. All stone bosses use ONE
 * geometry file and ONE animation file (fallen_comet_stone.geo.json / .animation.json, via
 * withAltModel/withAltAnimations) and are distinguished only by their own texture, which
 * still resolves per-entity by convention (textures/entity/<entityName>.png) - the GeckoLib
 * successor of the old HulkingCocoonStoneEntityModel's "shared geometry, per-stone texture"
 * pattern. No head-turn bone is configured on purpose: a crashed rock must not track the
 * player. Adding a third stone boss = new palette in tools/gen_fallen_comet_stone.py + this
 * class with its entity name; no new geometry/animation/model classes.
 */
public class FallenCometStoneGeoModel<T extends Entity & GeoAnimatable> extends DefaultedEntityGeoModel<T> {
    private static final Identifier SHARED_ASSETS = Identifier.of("baum2", "fallen_comet_stone");

    public FallenCometStoneGeoModel(String entityName) {
        super(Identifier.of("baum2", entityName));
        withAltModel(SHARED_ASSETS);
        withAltAnimations(SHARED_ASSETS);
    }
}
