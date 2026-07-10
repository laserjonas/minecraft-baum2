package de.baum2dev.baum2.entity;

import net.minecraft.client.render.entity.state.LivingEntityRenderState;

/**
 * Render state for the GeckoLib mount renderer. Deliberately EMPTY like every other GeoEntity
 * render state in this project (see FallenCometStoneRenderState's javadoc and
 * docs/fabric-modding.md part G): GeckoLib Mixins GeoRenderState support into vanilla's
 * EntityRenderState - re-declaring the interface or overriding getDataMap() here causes a
 * real, already-diagnosed render-time crash.
 */
public class MountHorseRenderState extends LivingEntityRenderState {
}
