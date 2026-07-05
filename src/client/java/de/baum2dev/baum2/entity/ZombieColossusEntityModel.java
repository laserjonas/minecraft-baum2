package de.baum2dev.baum2.entity;

import net.minecraft.client.model.Dilation;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.model.TexturedModelData;
import net.minecraft.client.render.entity.model.AbstractZombieModel;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.render.entity.state.ZombieEntityRenderState;

/**
 * Reuses vanilla's exact plain biped/zombie geometry - {@link BipedEntityModel#getModelData}, the
 * same {@code TexturedModelData} factory vanilla's own {@code EntityModels.getModels()} uses for
 * both {@code EntityModelLayers.ZOMBIE} and {@code EntityModelLayers.GIANT} (confirmed by
 * decompiling that class directly, not guessed) - applied at 3x scale via a {@code
 * ModelTransformer} at model-layer registration time (see {@code Baum2Client}). This mirrors
 * vanilla's own {@code GiantEntityModel}/{@code GiantEntityRenderer} mechanism exactly: vanilla's
 * Giant is itself "a 6x-scaled reskinned zombie boss that holds an oversized item," the same
 * archetype this boss is, and {@code GiantEntityModel} likewise contains no scaling logic of its
 * own (the shared {@code TexturedModelData} is scaled once, centrally) and reuses vanilla's own
 * {@link ZombieEntityRenderState} directly rather than a bespoke render-state subclass, because
 * this boss needs no custom pose beyond the standard zombie walk/attack cycle that {@link
 * AbstractZombieModel#setAngles} already drives unchanged - unlike Spider Queen's leap-crouch
 * pose, nothing here requires new render-state fields.
 */
public class ZombieColossusEntityModel extends AbstractZombieModel<ZombieEntityRenderState> {
    public ZombieColossusEntityModel(ModelPart modelPart) {
        super(modelPart);
    }

    public static TexturedModelData getTexturedModelData() {
        return TexturedModelData.of(BipedEntityModel.getModelData(Dilation.NONE, 0.0F), 64, 64);
    }
}
