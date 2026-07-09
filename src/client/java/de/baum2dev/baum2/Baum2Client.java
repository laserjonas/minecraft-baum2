package de.baum2dev.baum2;

import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.render.entity.EmptyEntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactories;
import de.baum2dev.baum2.entity.DrevathisEntityRenderer;
import de.baum2dev.baum2.entity.FallenCometStoneEntityRenderer;
import de.baum2dev.baum2.entity.SpiderQueenEntityRenderer;
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

        // Every fallen-comet-stone mini-boss shares the GeckoLib template (one geometry +
        // idle animation, per-stone texture named after the entity) - no model-layer
        // registration needed, GeckoLib resolves its own assets (docs/fabric-modding.md
        // part D). One loop covers all stones in the FallenCometStones definition table.
        ModEntities.FALLEN_COMET_STONES.forEach((definition, type) ->
                EntityRendererFactories.register(type,
                        context -> new FallenCometStoneEntityRenderer<>(context, definition.name())));

        // GeckoLib-based renderer: no EntityModelLayerRegistry call needed (GeckoLib resolves
        // its own geo.json/animation.json/texture assets directly, see docs/fabric-modding.md's
        // "GeckoLib integration" section, part D) - the old ModelTransformer.scaling(3.0F) call
        // is now SpiderQueenEntityRenderer's own withScale(3.0F).
        EntityRendererFactories.register(ModEntities.SPIDER_QUEEN, SpiderQueenEntityRenderer::new);

        // GeckoLib-based, like Spider Queen: no model-layer registration, scale lives in the
        // renderer's own withScale(3.0F).
        EntityRendererFactories.register(ModEntities.ZOMBIE_COLOSSUS, ZombieColossusEntityRenderer::new);

        // GeckoLib-based like Spider Queen/Colossus: no model-layer registration, scale lives
        // in the renderer's own withScale(1.8F).
        EntityRendererFactories.register(ModEntities.DREVATHIS, DrevathisEntityRenderer::new);

        // The dark-wave projectile is pure server-spawned particles - nothing to render.
        EntityRendererFactories.register(ModEntities.DARK_WAVE, EmptyEntityRenderer::new);
    }
}
