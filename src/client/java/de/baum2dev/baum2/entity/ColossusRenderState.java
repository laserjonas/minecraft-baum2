package de.baum2dev.baum2.entity;

import net.minecraft.client.render.entity.state.LivingEntityRenderState;

/**
 * Render state for the GeckoLib-based Zombie Colossus renderer. Deliberately EMPTY, exactly
 * like SpiderQueenRenderState (see that class's javadoc for the full story): GeckoLib's own
 * EntityRenderStateMixin already injects working GeoRenderState support into every vanilla
 * EntityRenderState - re-declaring "implements GeoRenderState" or overriding getDataMap() here
 * causes a real, already-diagnosed crash (two out-of-sync data maps). The old version's
 * leapWindupTicks/rageWindupTicks fields are gone: animation selection happens in
 * ZombieColossusEntity's own GeckoLib controller from synced entity data, not in the render
 * state.
 */
public class ColossusRenderState extends LivingEntityRenderState {
}
