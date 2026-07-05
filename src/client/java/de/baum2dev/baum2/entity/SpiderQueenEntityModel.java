package de.baum2dev.baum2.entity;

import net.minecraft.client.model.ModelPart;
import net.minecraft.client.model.TexturedModelData;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.render.entity.model.EntityModelPartNames;
import net.minecraft.client.render.entity.model.SpiderEntityModel;
import net.minecraft.util.math.MathHelper;

/**
 * Reuses vanilla's exact SpiderEntityModel geometry (same TexturedModelData factory, applied
 * at 3x scale via ModelTransformer in Baum2Client) but replaces its EntityModel<T> wrapper so
 * this mob can play a telegraphed crouch/coil pose while winding up a leap attack -
 * LivingEntityRenderState (what plain SpiderEntityModel is hardcoded to) has no field for
 * that, so this needs its own render state (SpiderQueenRenderState) and therefore its own
 * EntityModel subclass, not just a reused SpiderEntityModel instance. The walk-cycle leg-swing
 * math below is copied unchanged from vanilla's SpiderEntityModel.setAngles() since Java
 * generics don't allow reusing that method across two different EntityModel<T> type
 * parameters.
 */
public class SpiderQueenEntityModel extends EntityModel<SpiderQueenRenderState> {
    private static final float CROUCH_AMOUNT = 2.5F;
    private static final float LEG_BEND_RADIANS = 0.25F;

    private final ModelPart head;
    private final ModelPart body0;
    private final ModelPart body1;
    private final ModelPart rightHindLeg;
    private final ModelPart leftHindLeg;
    private final ModelPart rightMiddleLeg;
    private final ModelPart leftMiddleLeg;
    private final ModelPart rightMiddleFrontLeg;
    private final ModelPart leftMiddleFrontLeg;
    private final ModelPart rightFrontLeg;
    private final ModelPart leftFrontLeg;

    public SpiderQueenEntityModel(ModelPart modelPart) {
        super(modelPart);
        this.head = modelPart.getChild(EntityModelPartNames.HEAD);
        this.body0 = modelPart.getChild("body0");
        this.body1 = modelPart.getChild("body1");
        this.rightHindLeg = modelPart.getChild(EntityModelPartNames.RIGHT_HIND_LEG);
        this.leftHindLeg = modelPart.getChild(EntityModelPartNames.LEFT_HIND_LEG);
        this.rightMiddleLeg = modelPart.getChild("right_middle_hind_leg");
        this.leftMiddleLeg = modelPart.getChild("left_middle_hind_leg");
        this.rightMiddleFrontLeg = modelPart.getChild("right_middle_front_leg");
        this.leftMiddleFrontLeg = modelPart.getChild("left_middle_front_leg");
        this.rightFrontLeg = modelPart.getChild(EntityModelPartNames.RIGHT_FRONT_LEG);
        this.leftFrontLeg = modelPart.getChild(EntityModelPartNames.LEFT_FRONT_LEG);
    }

    public static TexturedModelData getTexturedModelData() {
        return SpiderEntityModel.getTexturedModelData();
    }

    @Override
    public void setAngles(SpiderQueenRenderState state) {
        super.setAngles(state);
        this.head.yaw = state.relativeHeadYaw * (float) (Math.PI / 180.0);
        this.head.pitch = state.pitch * (float) (Math.PI / 180.0);

        float f = state.limbSwingAnimationProgress * 0.6662F;
        float g = state.limbSwingAmplitude;
        float h = -(MathHelper.cos(f * 2.0F + 0.0F) * 0.4F) * g;
        float i = -(MathHelper.cos(f * 2.0F + (float) Math.PI) * 0.4F) * g;
        float j = -(MathHelper.cos(f * 2.0F + (float) (Math.PI / 2)) * 0.4F) * g;
        float k = -(MathHelper.cos(f * 2.0F + (float) (Math.PI * 3.0 / 2.0)) * 0.4F) * g;
        float l = Math.abs(MathHelper.sin(f + 0.0F) * 0.4F) * g;
        float m = Math.abs(MathHelper.sin(f + (float) Math.PI) * 0.4F) * g;
        float n = Math.abs(MathHelper.sin(f + (float) (Math.PI / 2)) * 0.4F) * g;
        float o = Math.abs(MathHelper.sin(f + (float) (Math.PI * 3.0 / 2.0)) * 0.4F) * g;
        this.rightHindLeg.yaw += h;
        this.leftHindLeg.yaw -= h;
        this.rightMiddleLeg.yaw += i;
        this.leftMiddleLeg.yaw -= i;
        this.rightMiddleFrontLeg.yaw += j;
        this.leftMiddleFrontLeg.yaw -= j;
        this.rightFrontLeg.yaw += k;
        this.leftFrontLeg.yaw -= k;
        this.rightHindLeg.roll += l;
        this.leftHindLeg.roll -= l;
        this.rightMiddleLeg.roll += m;
        this.leftMiddleLeg.roll -= m;
        this.rightMiddleFrontLeg.roll += n;
        this.leftMiddleFrontLeg.roll -= n;
        this.rightFrontLeg.roll += o;
        this.leftFrontLeg.roll -= o;

        if (state.leapWindupTicks > 0) {
            // "Prepare to jump": coils down and plants her legs as the wind-up counts toward
            // zero, so the leap reads as a deliberate, telegraphed pounce rather than an
            // instant teleport. Progress 0 = just started telegraphing, 1 = about to launch.
            float progress = 1.0F - (state.leapWindupTicks / (float) SpiderQueenEntity.LEAP_WINDUP_DURATION_TICKS);
            float crouch = progress * CROUCH_AMOUNT;
            this.body0.originY += crouch;
            this.body1.originY += crouch;
            this.head.originY += crouch * 0.6F;

            float legBend = progress * LEG_BEND_RADIANS;
            this.rightHindLeg.pitch += legBend;
            this.leftHindLeg.pitch += legBend;
            this.rightMiddleLeg.pitch += legBend;
            this.leftMiddleLeg.pitch += legBend;
            this.rightMiddleFrontLeg.pitch += legBend;
            this.leftMiddleFrontLeg.pitch += legBend;
            this.rightFrontLeg.pitch += legBend;
            this.leftFrontLeg.pitch += legBend;
        }
    }
}
