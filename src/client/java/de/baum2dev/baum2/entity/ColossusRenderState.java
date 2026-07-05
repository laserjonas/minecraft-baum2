package de.baum2dev.baum2.entity;

import net.minecraft.client.render.entity.state.ZombieEntityRenderState;

/**
 * Adds the leap and rage wind-up countdowns (synced from {@code ZombieColossusEntity} via
 * {@code getLeapWindupTicks()}/{@code getRageWindupTicks()}) so the model can play real telegraph
 * poses - a crouch-and-coil before the leap, an overhead club-raise before the rage combo -
 * instead of standing frozen during those wind-ups. Same pattern {@code SpiderQueenRenderState}
 * already established for her leap crouch, just with two counters instead of one since this boss
 * has two separate telegraphed attacks.
 *
 * <p>Extends {@code ZombieEntityRenderState} (not plain {@code LivingEntityRenderState}, unlike
 * {@code SpiderQueenRenderState}) because this boss's model still drives its base walk/attack
 * animation through the real {@code AbstractZombieModel}/{@code BipedEntityModel} inheritance
 * chain (see {@link ZombieColossusEntityModel}), which needs the zombie-specific {@code attacking}
 * field plus every armed/biped field {@code ZombieEntityRenderState} already carries.
 */
public class ColossusRenderState extends ZombieEntityRenderState {
    public int leapWindupTicks;
    public int rageWindupTicks;
}
