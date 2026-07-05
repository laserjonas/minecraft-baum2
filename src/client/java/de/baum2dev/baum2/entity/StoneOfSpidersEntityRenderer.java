package de.baum2dev.baum2.entity;

import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.render.entity.state.LivingEntityRenderState;
import net.minecraft.util.Identifier;

public class StoneOfSpidersEntityRenderer
        extends MobEntityRenderer<StoneOfSpidersEntity, LivingEntityRenderState, StoneOfSpidersEntityModel> {
    private static final Identifier TEXTURE = Identifier.of("baum2", "textures/entity/stone_of_spiders.png");

    public StoneOfSpidersEntityRenderer(EntityRendererFactory.Context context) {
        super(context, new StoneOfSpidersEntityModel(context.getPart(StoneOfSpidersEntityModel.LAYER)), 0.9F);
    }

    @Override
    public Identifier getTexture(LivingEntityRenderState state) {
        return TEXTURE;
    }

    @Override
    public LivingEntityRenderState createRenderState() {
        return new LivingEntityRenderState();
    }
}
