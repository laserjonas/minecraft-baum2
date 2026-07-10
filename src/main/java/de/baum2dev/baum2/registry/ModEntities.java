package de.baum2dev.baum2.registry;

import java.util.LinkedHashMap;
import java.util.Map;

import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;
import de.baum2dev.baum2.entity.DarkWaveProjectileEntity;
import de.baum2dev.baum2.entity.DrevathisEntity;
import de.baum2dev.baum2.entity.FallenCometStoneDefinition;
import de.baum2dev.baum2.entity.FallenCometStoneEntity;
import de.baum2dev.baum2.entity.MountHorseEntity;
import de.baum2dev.baum2.mounts.MountTier;
import de.baum2dev.baum2.entity.SilverfishBroodcallerEntity;
import de.baum2dev.baum2.entity.SpiderQueenEntity;
import de.baum2dev.baum2.entity.ZombieColossusEntity;

public class ModEntities {

    /**
     * Every fallen-comet-stone mini-boss, registered from the {@link FallenCometStones#ALL}
     * definition table (insertion-ordered so /summon tab-completion groups sensibly). All
     * stones share one entity class/geometry/renderer and differ by definition + texture.
     * makeFireImmune(): a meteor that survived atmospheric entry doesn't burn - concretely,
     * Stone of Blazes/Stone of Magma Cubes must not be killable by their own waves' fire
     * (the matching explosion immunity for Creeper/Ghast waves lives in
     * FallenCometStoneEntity.damage()).
     */
    public static final Map<FallenCometStoneDefinition, EntityType<FallenCometStoneEntity>> FALLEN_COMET_STONES =
            registerFallenCometStones();

    private static Map<FallenCometStoneDefinition, EntityType<FallenCometStoneEntity>> registerFallenCometStones() {
        Map<FallenCometStoneDefinition, EntityType<FallenCometStoneEntity>> stones = new LinkedHashMap<>();
        for (FallenCometStoneDefinition definition : FallenCometStones.ALL) {
            RegistryKey<EntityType<?>> key =
                    RegistryKey.of(RegistryKeys.ENTITY_TYPE, Identifier.of("baum2", definition.name()));
            EntityType<FallenCometStoneEntity> type = Registry.register(
                    Registries.ENTITY_TYPE,
                    key,
                    EntityType.Builder
                            .<FallenCometStoneEntity>create(
                                    (entityType, world) -> new FallenCometStoneEntity(entityType, world, definition),
                                    SpawnGroup.MONSTER)
                            .dimensions(3.0F, 3.0F)
                            .makeFireImmune()
                            .build(key));
            stones.put(definition, type);
        }
        return java.util.Collections.unmodifiableMap(stones);
    }

    /**
     * The three summonable player mounts (mount system), one EntityType per MountTier -
     * shared class + per-tier definition, same pattern as the fallen comet stones above.
     * SpawnGroup.MISC: summoned-only via the horse flutes, never naturally spawned. Hitbox
     * scales with the tier's render scale off a vanilla-horse-like 1.4x1.6 base.
     */
    public static final Map<MountTier, EntityType<MountHorseEntity>> MOUNT_HORSES = registerMountHorses();

    private static Map<MountTier, EntityType<MountHorseEntity>> registerMountHorses() {
        Map<MountTier, EntityType<MountHorseEntity>> mounts = new LinkedHashMap<>();
        for (MountTier tier : MountTier.values()) {
            RegistryKey<EntityType<?>> key =
                    RegistryKey.of(RegistryKeys.ENTITY_TYPE, Identifier.of("baum2", tier.id()));
            float width = 1.4F * tier.renderScale();
            float height = 1.6F * tier.renderScale();
            EntityType<MountHorseEntity> type = Registry.register(
                    Registries.ENTITY_TYPE,
                    key,
                    EntityType.Builder
                            .<MountHorseEntity>create(
                                    (entityType, world) -> new MountHorseEntity(entityType, world, tier),
                                    SpawnGroup.MISC)
                            .dimensions(width, height)
                            .eyeHeight(height * 0.85F)
                            // Saddle top sits at 1.5625 blocks at natural scale (see
                            // docs/visual-style-guide.md 22.6); the rider sinks slightly in,
                            // matching vanilla horse convention (back 1.4, attachment 1.44).
                            .passengerAttachments(1.5F * tier.renderScale())
                            .build(key));
            mounts.put(tier, type);
        }
        return java.util.Collections.unmodifiableMap(mounts);
    }

    /** The silverfish boss guarding the west grand cave mouth (3x-scale vanilla model). */
    public static final RegistryKey<EntityType<?>> SILVERFISH_BROODCALLER_KEY =
            RegistryKey.of(RegistryKeys.ENTITY_TYPE, Identifier.of("baum2", "silverfish_broodcaller"));

    public static final EntityType<SilverfishBroodcallerEntity> SILVERFISH_BROODCALLER = Registry.register(
            Registries.ENTITY_TYPE,
            SILVERFISH_BROODCALLER_KEY,
            EntityType.Builder.create(SilverfishBroodcallerEntity::new, SpawnGroup.MONSTER)
                    // 3x a vanilla silverfish's 0.4x0.3 hitbox, matching the renderer scale
                    .dimensions(1.2F, 0.9F)
                    .build(SILVERFISH_BROODCALLER_KEY)
    );

    public static final RegistryKey<EntityType<?>> SPIDER_QUEEN_KEY =
            RegistryKey.of(RegistryKeys.ENTITY_TYPE, Identifier.of("baum2", "spider_queen"));

    public static final EntityType<SpiderQueenEntity> SPIDER_QUEEN = Registry.register(
            Registries.ENTITY_TYPE,
            SPIDER_QUEEN_KEY,
            EntityType.Builder.create(SpiderQueenEntity::new, SpawnGroup.MONSTER)
                    .dimensions(4.2F, 2.7F)
                    .eyeHeight(1.95F)
                    .build(SPIDER_QUEEN_KEY)
    );

    public static final RegistryKey<EntityType<?>> ZOMBIE_COLOSSUS_KEY =
            RegistryKey.of(RegistryKeys.ENTITY_TYPE, Identifier.of("baum2", "zombie_colossus"));

    public static final EntityType<ZombieColossusEntity> ZOMBIE_COLOSSUS = Registry.register(
            Registries.ENTITY_TYPE,
            ZOMBIE_COLOSSUS_KEY,
            EntityType.Builder.create(ZombieColossusEntity::new, SpawnGroup.MONSTER)
                    .dimensions(1.8F, 5.85F)
                    .build(ZOMBIE_COLOSSUS_KEY)
    );

    public static final RegistryKey<EntityType<?>> DREVATHIS_KEY =
            RegistryKey.of(RegistryKeys.ENTITY_TYPE, Identifier.of("baum2", "drevathis"));

    public static final EntityType<DrevathisEntity> DREVATHIS = Registry.register(
            Registries.ENTITY_TYPE,
            DREVATHIS_KEY,
            EntityType.Builder.create(DrevathisEntity::new, SpawnGroup.MONSTER)
                    // 1.8x a normal player's 0.6x1.8 hitbox - matches the 1.8x model scale
                    // applied in Baum2Client (same "scale derived from vanilla base x factor"
                    // convention ZombieColossusEntity's 3x-zombie dimensions already use).
                    .dimensions(1.08F, 3.24F)
                    .eyeHeight(2.916F)
                    .build(DREVATHIS_KEY)
    );

    /** Drevathis's basic-attack projectile. MISC spawn group (not a mob); rendered as nothing
     *  client-side (EmptyEntityRenderer) - the visible wave is server-spawned particles. */
    public static final RegistryKey<EntityType<?>> DARK_WAVE_KEY =
            RegistryKey.of(RegistryKeys.ENTITY_TYPE, Identifier.of("baum2", "dark_wave"));

    public static final EntityType<DarkWaveProjectileEntity> DARK_WAVE = Registry.register(
            Registries.ENTITY_TYPE,
            DARK_WAVE_KEY,
            EntityType.Builder.<DarkWaveProjectileEntity>create(DarkWaveProjectileEntity::new, SpawnGroup.MISC)
                    .dimensions(1.0F, 1.0F)
                    .build(DARK_WAVE_KEY)
    );

    public static void registerAttributes() {
        FALLEN_COMET_STONES.forEach((definition, type) ->
                FabricDefaultAttributeRegistry.register(type, FallenCometStoneEntity.createAttributes(definition)));
        MOUNT_HORSES.forEach((tier, type) ->
                FabricDefaultAttributeRegistry.register(type, MountHorseEntity.createMountAttributes(tier)));
        FabricDefaultAttributeRegistry.register(SILVERFISH_BROODCALLER,
                SilverfishBroodcallerEntity.createBroodcallerAttributes());
        FabricDefaultAttributeRegistry.register(SPIDER_QUEEN, SpiderQueenEntity.createSpiderQueenAttributes());
        FabricDefaultAttributeRegistry.register(ZOMBIE_COLOSSUS, ZombieColossusEntity.createZombieColossusAttributes());
        FabricDefaultAttributeRegistry.register(DREVATHIS, DrevathisEntity.createDrevathisAttributes());
    }

    /** No-op - calling this forces this class (and its static EntityType registrations) to load. */
    public static void bootstrap() {
    }
}
