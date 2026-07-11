package de.baum2dev.baum2.items;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

import net.minecraft.item.Item;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.client.GeoRenderProvider;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animatable.manager.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * The GeckoLib "sword template" item - the shared class for every sword of the animated
 * sword line, mirroring the FallenCometStone/MountHorse template pattern on the item side:
 * ONE geometry + animation set ({@code geckolib/models|animations/item/sword_template.*}),
 * per-sword texture selected by {@code assetName} ({@code textures/item/<assetName>_geo.png},
 * resolved client-side in {@code TemplateSwordItemRenderer}). A second sword of the line is
 * a new palette entry in tools/gen_sword_template.py plus one registration in ModItems with
 * a different assetName - no new Java classes, geometry, or animations.
 *
 * Animation contract (all keys authored in sword_template.animation.json):
 * - idle: gentle always-on loop (breathing sway + late-loop grip-settle accent).
 * - attack / attack_mounted: one-shot triggerable anims, fired SERVER-side by
 *   {@code combat/SwordAnimationHandler} on a landed melee hit - on foot vs. riding one of
 *   our mounts respectively. GeckoLib syncs triggered anims to all tracking clients itself
 *   (same zero-networking pattern as MountHorseEntity.playAttackAnimation).
 *
 * First sword of the line: "Espenklinge" (wooden training longsword). By design it has no
 * crafting recipe yet, and it cannot be manually dropped ({@link UndroppableItem}) - the
 * current focus is visualization/animation, so it is obtained via /give or the creative
 * inventory only.
 */
public class TemplateSwordItem extends Item implements GeoItem, UndroppableItem {

    public static final String CONTROLLER_NAME = "sword_controller";
    public static final String ATTACK_TRIGGER = "attack";
    public static final String ATTACK_MOUNTED_TRIGGER = "attack_mounted";

    private static final RawAnimation IDLE_ANIM =
            RawAnimation.begin().thenLoop("animation.sword_template.idle");
    private static final RawAnimation ATTACK_ANIM =
            RawAnimation.begin().thenPlay("animation.sword_template.attack");
    private static final RawAnimation ATTACK_MOUNTED_ANIM =
            RawAnimation.begin().thenPlay("animation.sword_template.attack_mounted");

    /**
     * Client-side renderer hookup, injected by Baum2Client at client init. Lives behind a
     * static factory because the actual GeoItemRenderer subclass is in the client source set
     * (splitEnvironmentSourceSets), which this main-side class cannot reference directly.
     * Remains null on a dedicated server, where GeckoLib never calls createGeoRenderer.
     */
    private static BiConsumer<TemplateSwordItem, Consumer<GeoRenderProvider>> clientRenderProviderFactory;

    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);
    private final String assetName;

    public TemplateSwordItem(Settings settings, String assetName) {
        super(settings);
        this.assetName = assetName;
        // Required for server-side triggerAnim calls to reach clients.
        GeoItem.registerSyncedAnimatable(this);
    }

    /** The per-sword asset name, e.g. "espenklinge" -> textures/item/espenklinge_geo.png. */
    public String assetName() {
        return this.assetName;
    }

    public static void setClientRenderProviderFactory(
            BiConsumer<TemplateSwordItem, Consumer<GeoRenderProvider>> factory) {
        clientRenderProviderFactory = factory;
    }

    @Override
    public void createGeoRenderer(Consumer<GeoRenderProvider> consumer) {
        if (clientRenderProviderFactory != null) {
            clientRenderProviderFactory.accept(this, consumer);
        }
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(CONTROLLER_NAME, 2,
                test -> test.setAndContinue(IDLE_ANIM))
                .triggerableAnim(ATTACK_TRIGGER, ATTACK_ANIM)
                .triggerableAnim(ATTACK_MOUNTED_TRIGGER, ATTACK_MOUNTED_ANIM));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.geoCache;
    }
}
