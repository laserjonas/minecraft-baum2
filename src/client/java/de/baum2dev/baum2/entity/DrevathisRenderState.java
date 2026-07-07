package de.baum2dev.baum2.entity;

import net.minecraft.client.render.entity.state.LivingEntityRenderState;

/**
 * Render state for the GeckoLib-based Drevathis renderer. Deliberately EMPTY, exactly like
 * SpiderQueenRenderState/ColossusRenderState (see their javadocs for the full story):
 * GeckoLib's own EntityRenderStateMixin already injects working GeoRenderState support into
 * every vanilla EntityRenderState - re-declaring "implements GeoRenderState" or overriding
 * getDataMap() here causes a real, already-diagnosed crash (two out-of-sync data maps). The
 * old version's four per-skill counters are gone: animation selection happens in
 * DrevathisEntity's own GeckoLib controller from synced entity data / triggered animations.
 */
public class DrevathisRenderState extends LivingEntityRenderState {
}
