package de.baum2dev.baum2.entity;

import net.minecraft.client.render.entity.state.LivingEntityRenderState;

/**
 * GeckoLib's own EntityRenderStateMixin already injects full GeoRenderState support (a working
 * data map, addGeckolibData/getGeckolibData/getDataMap etc.) directly into vanilla's
 * EntityRenderState as concrete methods - every EntityRenderState (including
 * LivingEntityRenderState) is already duck-typed as a GeoRenderState automatically, no extra
 * work needed. This class exists only so GeoEntityRenderer has a concrete R type to bind to; it
 * must NOT re-declare "implements GeoRenderState" or override getDataMap() itself - doing so
 * once caused a real crash (NullPointerException in AnimationProcessor.extractControllerStates):
 * the mixin's addGeckolibData/hasGeckolibData are *concrete* methods (a class's own concrete
 * method always wins over an inherited one, but only for methods actually overridden), so a
 * subclass that overrides getDataMap() alone - without also overriding addGeckolibData/
 * hasGeckolibData - ends up with writes going through the mixin's own private data map while
 * reads (getGeckolibData, a pure interface default that calls getDataMap()) resolve to the
 * subclass's own empty one. Two different maps, permanently out of sync. Keep this class empty.
 */
public class SpiderQueenRenderState extends LivingEntityRenderState {
}
