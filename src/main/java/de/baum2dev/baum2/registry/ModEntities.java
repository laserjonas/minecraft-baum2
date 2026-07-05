package de.baum2dev.baum2.registry;

import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;
import de.baum2dev.baum2.entity.StoneOfSpidersEntity;

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

    public static void registerAttributes() {
        FabricDefaultAttributeRegistry.register(STONE_OF_SPIDERS, StoneOfSpidersEntity.createStoneOfSpidersAttributes());
    }

    /** No-op - calling this forces this class (and its static EntityType registrations) to load. */
    public static void bootstrap() {
    }
}
