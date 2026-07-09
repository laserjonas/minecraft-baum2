package de.baum2dev.baum2.entity;

import net.minecraft.client.render.entity.state.LivingEntityRenderState;

/**
 * Render state for the GeckoLib-based fallen-comet-stone renderer. Deliberately EMPTY,
 * exactly like SpiderQueenRenderState/ColossusRenderState/DrevathisRenderState (see their
 * javadocs and docs/fabric-modding.md part G for the full story): GeckoLib's own
 * EntityRenderStateMixin already injects working GeoRenderState support into every vanilla
 * EntityRenderState - re-declaring "implements GeoRenderState" or overriding getDataMap()
 * here causes a real, already-diagnosed crash (two out-of-sync data maps).
 */
public class FallenCometStoneRenderState extends LivingEntityRenderState {
}
