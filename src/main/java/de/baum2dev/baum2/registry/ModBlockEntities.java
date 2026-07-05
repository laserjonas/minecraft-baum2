package de.baum2dev.baum2.registry;

import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import de.baum2dev.baum2.block.RissobeliskBlockEntity;

public class ModBlockEntities {

    public static final BlockEntityType<RissobeliskBlockEntity> RISSOBELISK = Registry.register(
            Registries.BLOCK_ENTITY_TYPE,
            Identifier.of("baum2", "rissobelisk"),
            FabricBlockEntityTypeBuilder.create(RissobeliskBlockEntity::new, ModBlocks.RISSOBELISK).build()
    );

    /** No-op - calling this forces this class (and its static BlockEntityType registration) to load. */
    public static void bootstrap() {
    }
}
