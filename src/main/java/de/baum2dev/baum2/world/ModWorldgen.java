package de.baum2dev.baum2.world;

import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

/**
 * Registers Heimgrund's worldgen codecs. Must run early in {@code Baum2.onInitialize()}:
 * the chunk-generator codec has to exist before any world's dimension JSON (which
 * references {@code baum2:heimgrund}) is deserialized.
 */
public final class ModWorldgen {

    public static void bootstrap() {
        Registry.register(Registries.CHUNK_GENERATOR, Identifier.of("baum2", "heimgrund"),
                HeimgrundChunkGenerator.CODEC);
        Registry.register(Registries.BIOME_SOURCE, Identifier.of("baum2", "heimgrund"),
                HeimgrundBiomeSource.CODEC);
    }

    private ModWorldgen() {
    }
}
