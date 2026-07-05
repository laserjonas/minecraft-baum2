package de.baum2dev.baum2;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.EntityModelLayerRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.render.entity.EntityRendererFactories;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;
import de.baum2dev.baum2.entity.HulkingCocoonStoneEntityModel;
import de.baum2dev.baum2.entity.StoneOfSpidersEntityRenderer;
import de.baum2dev.baum2.entity.StoneOfZombiesEntityRenderer;
import de.baum2dev.baum2.networking.ClientNetworkingHandler;
import de.baum2dev.baum2.registry.ModEntities;
import de.baum2dev.baum2.ui.Baum2KeyBindings;
import de.baum2dev.baum2.ui.ClassScreen;
import de.baum2dev.baum2.ui.MobNameplateHud;
import de.baum2dev.baum2.ui.PlayerStatusHud;
import de.baum2dev.baum2.ui.VitalsHud;

public class Baum2Client implements ClientModInitializer {
    private static final KeyBinding.Category CATEGORY = KeyBinding.Category.create(Identifier.of("baum2", "main"));
    private static final KeyBinding OPEN_CLASS_SCREEN = new KeyBinding(
        "key.baum2.class_screen", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_K, CATEGORY
    );

    @Override
    public void onInitializeClient() {
        Baum2.LOGGER.info("Baum2 client initializing...");
        ClientNetworkingHandler.registerClientHandlers();

        HudElementRegistry.attachElementAfter(
            VanillaHudElements.BOSS_BAR,
            Identifier.of("baum2", "player_status_hud"),
            PlayerStatusHud::render
        );

        KeyBindingHelper.registerKeyBinding(OPEN_CLASS_SCREEN);
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (OPEN_CLASS_SCREEN.wasPressed()) {
                client.setScreen(new ClassScreen());
            }
        });

        VitalsHud.register();
        Baum2KeyBindings.register();
        MobNameplateHud.register();

        EntityModelLayerRegistry.registerModelLayer(
                StoneOfSpidersEntityRenderer.LAYER, HulkingCocoonStoneEntityModel::getTexturedModelData);
        EntityRendererFactories.register(ModEntities.STONE_OF_SPIDERS, StoneOfSpidersEntityRenderer::new);

        EntityModelLayerRegistry.registerModelLayer(
                StoneOfZombiesEntityRenderer.LAYER, HulkingCocoonStoneEntityModel::getTexturedModelData);
        EntityRendererFactories.register(ModEntities.STONE_OF_ZOMBIES, StoneOfZombiesEntityRenderer::new);
    }
}
