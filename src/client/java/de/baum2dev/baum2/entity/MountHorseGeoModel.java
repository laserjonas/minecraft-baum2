package de.baum2dev.baum2.entity;

import net.minecraft.util.Identifier;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;

/**
 * Shared GeckoLib model for the three player mounts - same "one geometry + one animation
 * file, per-entity texture" template as FallenCometStoneGeoModel: all tiers use
 * mount_horse.geo.json / mount_horse.animation.json via withAltModel/withAltAnimations,
 * while the texture resolves per-entity by convention (textures/entity/<tierId>.png). The
 * tier textures decide which of the shared geometry's armor cubes are visible (fully
 * transparent pixels are cut out), so Wanderross/Eisenross/Schlachtross differ visually
 * without per-tier geometry.
 */
public class MountHorseGeoModel extends DefaultedEntityGeoModel<MountHorseEntity> {
    private static final Identifier SHARED_ASSETS = Identifier.of("baum2", "mount_horse");

    public MountHorseGeoModel(String tierId) {
        super(Identifier.of("baum2", tierId));
        withAltModel(SHARED_ASSETS);
        withAltAnimations(SHARED_ASSETS);
    }
}
