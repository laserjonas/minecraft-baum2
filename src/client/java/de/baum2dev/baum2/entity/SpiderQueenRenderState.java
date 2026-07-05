package de.baum2dev.baum2.entity;

import net.minecraft.client.render.entity.state.LivingEntityRenderState;

/** Adds the leap wind-up countdown (synced from SpiderQueenEntity) so the model can play a
 *  real crouch/coil pose while she's telegraphing a leap, not just stand still. */
public class SpiderQueenRenderState extends LivingEntityRenderState {
    public int leapWindupTicks;
}
