package de.baum2dev.baum2.entity;

import net.minecraft.client.model.ModelData;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.model.ModelPartBuilder;
import net.minecraft.client.model.ModelPartData;
import net.minecraft.client.model.ModelTransform;
import net.minecraft.client.model.TexturedModelData;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.render.entity.model.EntityModelLayer;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.util.Identifier;

/**
 * Seven overlapping cuboids (fused rock base, egg-sac body, off-center upper lump, two
 * web-strand/crack accents, two glow-vein bumps) - see docs/visual-style-guide.md Section 13.2
 * for the full shape rationale and exact measurements this class implements. Model space uses
 * Minecraft's standard mob-model convention (confirmed via LivingEntityRenderer's fixed
 * -1.501-block translate, applied identically to every vanilla mob) where Y=24 is ground level
 * and smaller Y values are higher up.
 */
public class StoneOfSpidersEntityModel extends EntityModel<EntityRenderState> {
    public static final EntityModelLayer LAYER =
            new EntityModelLayer(Identifier.of("baum2", "stone_of_spiders"), "main");

    public StoneOfSpidersEntityModel(ModelPart root) {
        super(root);
    }

    public static TexturedModelData getTexturedModelData() {
        ModelData modelData = new ModelData();
        ModelPartData root = modelData.getRoot();

        root.addChild("base",
                ModelPartBuilder.create().uv(0, 0).cuboid(-22.0F, 12.0F, -22.0F, 44, 12, 44),
                ModelTransform.NONE);

        root.addChild("body",
                ModelPartBuilder.create().uv(0, 56).cuboid(-17.0F, -10.0F, -17.0F, 34, 24, 34),
                ModelTransform.NONE);

        root.addChild("cap",
                ModelPartBuilder.create().uv(0, 114).cuboid(-2.0F, -22.4F, -5.0F, 20, 16, 20),
                ModelTransform.NONE);

        root.addChild("web_strand_1",
                ModelPartBuilder.create().uv(0, 150).cuboid(-13.0F, -1.0F, -1.0F, 26, 2, 2),
                ModelTransform.of(0.0F, -2.0F, 17.0F,
                        0.0F, (float) Math.toRadians(25.0), (float) Math.toRadians(-15.0)));

        root.addChild("web_strand_2",
                ModelPartBuilder.create().uv(0, 154).cuboid(-10.0F, -1.0F, -1.0F, 20, 2, 2),
                ModelTransform.of(12.0F, 4.0F, 12.0F,
                        (float) Math.toRadians(20.0), (float) Math.toRadians(-20.0), 0.0F));

        root.addChild("glow_bump_body",
                ModelPartBuilder.create().uv(0, 158).cuboid(-3.0F, -1.5F, -3.0F, 6, 3, 6),
                ModelTransform.origin(0.0F, -4.0F, 18.0F));

        root.addChild("glow_bump_cap",
                ModelPartBuilder.create().uv(0, 167).cuboid(-2.5F, -1.5F, -2.5F, 5, 3, 5),
                ModelTransform.origin(8.0F, -14.0F, 16.0F));

        return TexturedModelData.of(modelData, 176, 176);
    }
}
