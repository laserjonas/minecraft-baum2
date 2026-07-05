package de.baum2dev.baum2.entity;

import net.minecraft.client.render.entity.BipedEntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.render.entity.feature.HeldItemFeatureRenderer;
import net.minecraft.client.render.entity.model.EntityModelLayer;
import net.minecraft.util.Identifier;

/**
 * Renders the club-wielding boss via real held-item rendering (option (a) from the design brief),
 * not a baked-on cosmetic club - {@code ZombieColossusEntity.initEquipment()} already puts a real
 * {@code ItemStack} in the mainhand slot, so this only needs to attach the standard held-item
 * feature and the club renders at the boss's hand automatically, scaled up with the rest of the
 * model.
 *
 * <p>Deliberately extends {@code MobEntityRenderer} directly, bypassing both vanilla's {@code
 * ZombieEntityRenderer} and {@code ZombieBaseEntityRenderer} - confirmed by decompiling the latter
 * that it hardcodes a non-scaled {@code 0.5F} shadow radius via a fixed {@code super(...)} call,
 * the exact same "no way to override it" problem the {@code SpiderQueenEntityRenderer} javadoc
 * already documents for vanilla's {@code SpiderEntityRenderer}. Vanilla solves this identical
 * problem for its own oversized-zombie boss ({@code GiantEntity}, 6x a zombie) by having {@code
 * GiantEntityRenderer} extend {@code MobEntityRenderer} directly instead and pass a scaled shadow
 * radius (confirmed by decompiling {@code GiantEntityRenderer} directly, not guessed) - this class
 * copies that exact, vanilla-proven mechanism rather than inventing a new one:
 * <ul>
 *   <li>Uses a bespoke {@link ColossusRenderState} (extends vanilla's own {@code
 *   ZombieEntityRenderState}) instead of the plain vanilla state this class used in v1 - needed to
 *   carry the leap/rage wind-up counters for the model's telegraph poses (see that class's
 *   javadoc).</li>
 *   <li>Adds {@link HeldItemFeatureRenderer} by hand (vanilla's {@code ZombieBaseEntityRenderer}
 *   never adds one itself - only {@code GiantEntityRenderer} does, for this exact "big zombie
 *   holding a big item" case).</li>
 *   <li>Calls the static {@link BipedEntityRenderer#updateBipedRenderState} helper from {@code
 *   updateRenderState} to populate the render state's held-item/arm-pose fields - the same helper
 *   {@code GiantEntityRenderer} calls, since this class no longer extends {@code
 *   BipedEntityRenderer} itself to get that behavior for free.</li>
 * </ul>
 * No armor feature renderer is attached - this boss only ever equips a mainhand weapon, never
 * armor, so {@code ArmorFeatureRenderer}/{@code EquipmentModelData} plumbing (which {@code
 * GiantEntityRenderer} does use, for its equippable armor slots) is unnecessary here.
 *
 * <p><b>Playtest fix, verified against the decompiled 1.21.11 client jar rather than guessed:</b>
 * confirmed that {@code super.updateRenderState()} (reaching {@code LivingEntityRenderer}'s own
 * implementation, since {@code MobEntityRenderer} doesn't override it) already populates
 * {@code limbSwingAnimationProgress}/{@code limbSwingAmplitude} generically for every mob
 * regardless of Biped-specific subclassing, and that the static {@code updateBipedRenderState}
 * helper below already calls {@code ArmedEntityRenderState.updateRenderState} internally, which
 * sets {@code handSwingProgress}/{@code swingAnimationType} - i.e. the walk cycle and the base
 * attack-swing pose were already correctly wired in v1's plumbing; neither actually needed a
 * fix. The one real gap found: vanilla's {@code MobEntity.isAttacking()} is only ever set {@code
 * true} by vanilla's own {@code MeleeAttackGoal}/{@code ZombieAttackGoal} machinery, which this
 * boss's fully custom attack {@code Goal}s (by design, not touched here) never call - so {@code
 * state.attacking} always read {@code false}, meaning every swing used {@code ArmPosing.zombieArms}'
 * calmer non-attacking pose baseline even mid-strike. Fixed client-side only, without touching any
 * gameplay code: treat an in-progress hand swing as "attacking" for pose purposes too.
 */
public class ZombieColossusEntityRenderer
        extends MobEntityRenderer<ZombieColossusEntity, ColossusRenderState, ZombieColossusEntityModel> {
    public static final EntityModelLayer LAYER =
            new EntityModelLayer(Identifier.of("baum2", "zombie_colossus"), "main");
    private static final Identifier TEXTURE = Identifier.of("baum2", "textures/entity/zombie_colossus.png");
    private static final float SCALE = 3.0F;

    public ZombieColossusEntityRenderer(EntityRendererFactory.Context context) {
        // 0.5F is vanilla's own base zombie/giant shadow radius (confirmed via
        // ZombieBaseEntityRenderer/GiantEntityRenderer decompiled source) - scaled by the same
        // 3x factor as the model itself, exactly how GiantEntityRenderer scales it for its own
        // 6x case (0.5F * scale), so the shadow matches this boss's tripled footprint.
        super(context, new ZombieColossusEntityModel(context.getPart(LAYER)), 0.5F * SCALE);
        this.addFeature(new HeldItemFeatureRenderer<>(this));
    }

    @Override
    public Identifier getTexture(ColossusRenderState state) {
        return TEXTURE;
    }

    @Override
    public ColossusRenderState createRenderState() {
        return new ColossusRenderState();
    }

    @Override
    public void updateRenderState(ZombieColossusEntity entity, ColossusRenderState state, float tickDelta) {
        super.updateRenderState(entity, state, tickDelta);
        // Populates equippedHeadStack/rightHandItem/arm poses/handSwingProgress/etc. from the
        // entity's actual equipment and swing state - BipedEntityRenderer normally does this
        // internally on every updateRenderState call, but this class no longer extends
        // BipedEntityRenderer (see class javadoc), so it's invoked directly, same as
        // GiantEntityRenderer does.
        BipedEntityRenderer.updateBipedRenderState(entity, state, tickDelta, this.itemModelResolver);
        // See class javadoc's playtest-fix note: entity.isAttacking() alone is never true for
        // this boss's custom attack Goals, so also treat a live hand-swing as "attacking" for
        // ArmPosing.zombieArms' pose-intensity baseline - purely a render-state read, no
        // gameplay logic touched.
        state.attacking = entity.isAttacking() || state.handSwingProgress > 0.0F;
        state.leapWindupTicks = entity.getLeapWindupTicks();
        state.rageWindupTicks = entity.getRageWindupTicks();
    }
}
