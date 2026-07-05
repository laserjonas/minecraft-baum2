package de.baum2dev.baum2.entity;

import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.render.entity.model.EntityModelLayer;
import net.minecraft.client.render.entity.state.LivingEntityRenderState;
import net.minecraft.util.Identifier;

public class StoneOfZombiesEntityRenderer
        extends MobEntityRenderer<StoneOfZombiesEntity, LivingEntityRenderState, HulkingCocoonStoneEntityModel> {
    public static final EntityModelLayer LAYER =
            new EntityModelLayer(Identifier.of("baum2", "stone_of_zombies"), "main");
    private static final Identifier TEXTURE = Identifier.of("baum2", "textures/entity/stone_of_zombies.png");

    public StoneOfZombiesEntityRenderer(EntityRendererFactory.Context context) {
        super(context, new HulkingCocoonStoneEntityModel(context.getPart(LAYER)), 0.9F);
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
