package de.baum2dev.baum2;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityModelLayerRegistry;
import net.minecraft.client.render.entity.EntityRendererFactories;
import net.minecraft.client.render.entity.model.ModelTransformer;
import de.baum2dev.baum2.entity.HulkingCocoonStoneEntityModel;
import de.baum2dev.baum2.entity.SpiderQueenEntityModel;
import de.baum2dev.baum2.entity.SpiderQueenEntityRenderer;
import de.baum2dev.baum2.entity.StoneOfSpidersEntityRenderer;
import de.baum2dev.baum2.entity.StoneOfZombiesEntityRenderer;
import de.baum2dev.baum2.entity.ZombieColossusEntityModel;
import de.baum2dev.baum2.entity.ZombieColossusEntityRenderer;
import de.baum2dev.baum2.networking.ClientNetworkingHandler;
import de.baum2dev.baum2.registry.ModEntities;
import de.baum2dev.baum2.ui.Baum2KeyBindings;
import de.baum2dev.baum2.ui.MobNameplateHud;
import de.baum2dev.baum2.ui.SpellCastKeyBindings;
import de.baum2dev.baum2.ui.VitalsHud;

public class Baum2Client implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        Baum2.LOGGER.info("Baum2 client initializing...");
        ClientNetworkingHandler.registerClientHandlers();

        VitalsHud.register();
        Baum2KeyBindings.register();
        SpellCastKeyBindings.register();
        MobNameplateHud.register();

        EntityModelLayerRegistry.registerModelLayer(
                StoneOfSpidersEntityRenderer.LAYER, HulkingCocoonStoneEntityModel::getTexturedModelData);
        EntityRendererFactories.register(ModEntities.STONE_OF_SPIDERS, StoneOfSpidersEntityRenderer::new);

        EntityModelLayerRegistry.registerModelLayer(
                StoneOfZombiesEntityRenderer.LAYER, HulkingCocoonStoneEntityModel::getTexturedModelData);
        EntityRendererFactories.register(ModEntities.STONE_OF_ZOMBIES, StoneOfZombiesEntityRenderer::new);

        EntityModelLayerRegistry.registerModelLayer(
                SpiderQueenEntityRenderer.LAYER,
                () -> SpiderQueenEntityModel.getTexturedModelData().transform(ModelTransformer.scaling(3.0F)));
        EntityRendererFactories.register(ModEntities.SPIDER_QUEEN, SpiderQueenEntityRenderer::new);

        EntityModelLayerRegistry.registerModelLayer(
                ZombieColossusEntityRenderer.LAYER,
                () -> ZombieColossusEntityModel.getTexturedModelData().transform(ModelTransformer.scaling(3.0F)));
        EntityRendererFactories.register(ModEntities.ZOMBIE_COLOSSUS, ZombieColossusEntityRenderer::new);
    }
}
