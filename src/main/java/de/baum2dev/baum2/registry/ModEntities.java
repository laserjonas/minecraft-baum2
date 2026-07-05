package de.baum2dev.baum2.registry;

import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;
import de.baum2dev.baum2.entity.DrevathisEntity;
import de.baum2dev.baum2.entity.SpiderQueenEntity;
import de.baum2dev.baum2.entity.StoneOfSpidersEntity;
import de.baum2dev.baum2.entity.StoneOfZombiesEntity;
import de.baum2dev.baum2.entity.ZombieColossusEntity;

public class ModEntities {

    public static final RegistryKey<EntityType<?>> STONE_OF_SPIDERS_KEY =
            RegistryKey.of(RegistryKeys.ENTITY_TYPE, Identifier.of("baum2", "stone_of_spiders"));

    public static final EntityType<StoneOfSpidersEntity> STONE_OF_SPIDERS = Registry.register(
            Registries.ENTITY_TYPE,
            STONE_OF_SPIDERS_KEY,
            EntityType.Builder.create(StoneOfSpidersEntity::new, SpawnGroup.MONSTER)
                    .dimensions(3.0F, 3.0F)
                    .build(STONE_OF_SPIDERS_KEY)
    );

    public static final RegistryKey<EntityType<?>> STONE_OF_ZOMBIES_KEY =
            RegistryKey.of(RegistryKeys.ENTITY_TYPE, Identifier.of("baum2", "stone_of_zombies"));

    public static final EntityType<StoneOfZombiesEntity> STONE_OF_ZOMBIES = Registry.register(
            Registries.ENTITY_TYPE,
            STONE_OF_ZOMBIES_KEY,
            EntityType.Builder.create(StoneOfZombiesEntity::new, SpawnGroup.MONSTER)
                    .dimensions(3.0F, 3.0F)
                    .build(STONE_OF_ZOMBIES_KEY)
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

    public static void registerAttributes() {
        FabricDefaultAttributeRegistry.register(STONE_OF_SPIDERS, StoneOfSpidersEntity.createStoneOfSpidersAttributes());
        FabricDefaultAttributeRegistry.register(STONE_OF_ZOMBIES, StoneOfZombiesEntity.createStoneOfZombiesAttributes());
        FabricDefaultAttributeRegistry.register(SPIDER_QUEEN, SpiderQueenEntity.createSpiderQueenAttributes());
        FabricDefaultAttributeRegistry.register(ZOMBIE_COLOSSUS, ZombieColossusEntity.createZombieColossusAttributes());
        FabricDefaultAttributeRegistry.register(DREVATHIS, DrevathisEntity.createDrevathisAttributes());
    }

    /** No-op - calling this forces this class (and its static EntityType registrations) to load. */
    public static void bootstrap() {
    }
}
