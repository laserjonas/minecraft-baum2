package de.baum2dev.baum2.entity;

import net.minecraft.client.render.entity.state.BipedEntityRenderState;

/**
 * Carries the four per-skill telegraph/duration counters (synced from
 * {@code DrevathisEntity.getDashWindupTicks()}/{@code getChainEffectTicks()}/
 * {@code getWaveCastTicks()}/{@code getThunderChannelTicks()}) so the model can play a distinct
 * pose for every skill - "every skill must be animated" per the design brief, extending the same
 * pattern {@code ColossusRenderState} established with two counters to all four here.
 *
 * <p>Extends {@code BipedEntityRenderState} (not plain {@code LivingEntityRenderState}) because
 * {@code DrevathisEntityModel} extends {@code BipedEntityModel<DrevathisRenderState>} directly,
 * and {@code BipedEntityRenderer.updateBipedRenderState(...)} (used the same way
 * {@code ZombieColossusEntityRenderer} already uses it) requires exactly this type.
 */
public class DrevathisRenderState extends BipedEntityRenderState {
    public int dashWindupTicks;
    public int chainEffectTicks;
    public int waveCastTicks;
    public int thunderChannelTicks;
}
