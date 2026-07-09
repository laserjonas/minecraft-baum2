package de.baum2dev.baum2.world;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.stream.Stream;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.source.BiomeSource;
import net.minecraft.world.biome.source.util.MultiNoiseUtil;

/**
 * Places Heimgrund's four biomes by calling the exact same {@link ZoneLayout} function
 * the chunk generator builds terrain from, so biome and terrain can never disagree.
 * The four biome entries come from the dimension JSON (baum2:dorfanger etc.), so spawn
 * lists and visuals are retunable without touching Java.
 *
 * <p>Note {@link #getBiome} receives biome coordinates (block >> 2), hence the << 2.
 * LAKE has no biome of its own - a lake is meadow terrain with water on it.
 */
public class HeimgrundBiomeSource extends BiomeSource {

    public static final MapCodec<HeimgrundBiomeSource> CODEC = RecordCodecBuilder.mapCodec(instance ->
            instance.group(
                    Biome.REGISTRY_CODEC.fieldOf("clearing").forGetter(source -> source.clearing),
                    Biome.REGISTRY_CODEC.fieldOf("meadow").forGetter(source -> source.meadow),
                    Biome.REGISTRY_CODEC.fieldOf("desert").forGetter(source -> source.desert),
                    Biome.REGISTRY_CODEC.fieldOf("mountain").forGetter(source -> source.mountain)
            ).apply(instance, HeimgrundBiomeSource::new));

    private final RegistryEntry<Biome> clearing;
    private final RegistryEntry<Biome> meadow;
    private final RegistryEntry<Biome> desert;
    private final RegistryEntry<Biome> mountain;

    public HeimgrundBiomeSource(RegistryEntry<Biome> clearing, RegistryEntry<Biome> meadow,
            RegistryEntry<Biome> desert, RegistryEntry<Biome> mountain) {
        this.clearing = clearing;
        this.meadow = meadow;
        this.desert = desert;
        this.mountain = mountain;
    }

    @Override
    protected MapCodec<? extends BiomeSource> getCodec() {
        return CODEC;
    }

    @Override
    protected Stream<RegistryEntry<Biome>> biomeStream() {
        return Stream.of(clearing, meadow, desert, mountain);
    }

    @Override
    public RegistryEntry<Biome> getBiome(int biomeX, int biomeY, int biomeZ,
            MultiNoiseUtil.MultiNoiseSampler noise) {
        return switch (ZoneLayout.zoneAt(biomeX << 2, biomeZ << 2)) {
            case CLEARING -> clearing;
            case MEADOW, LAKE -> meadow;
            case DESERT -> desert;
            case MOUNTAIN -> mountain;
        };
    }
}
