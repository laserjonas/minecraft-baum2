package de.baum2dev.baum2.world;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.random.ChunkRandom;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.math.random.RandomSeed;
import net.minecraft.world.ChunkRegion;
import net.minecraft.world.HeightLimitView;
import net.minecraft.world.Heightmap;
import net.minecraft.world.SpawnHelper;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.source.BiomeAccess;
import net.minecraft.world.biome.source.BiomeSource;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.gen.StructureAccessor;
import net.minecraft.world.gen.chunk.Blender;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import net.minecraft.world.gen.chunk.VerticalBlockSample;
import net.minecraft.world.gen.noise.NoiseConfig;

/**
 * Generates Heimgrund's fixed, authored terrain. Every block is a pure function of
 * (x, y, z) via {@link ZoneLayout} - no world-seed input, no structures, no vanilla
 * noise router. Referenced from data/baum2/dimension/heimgrund.json as
 * {@code "type": "baum2:heimgrund"}; the biome source is a codec field, so swapping the
 * placeholder fixed biome for the real radial biome source is a JSON-only change.
 */
public class HeimgrundChunkGenerator extends ChunkGenerator {

    public static final MapCodec<HeimgrundChunkGenerator> CODEC = RecordCodecBuilder.mapCodec(instance ->
            instance.group(
                    BiomeSource.CODEC.fieldOf("biome_source").forGetter(generator -> generator.biomeSource)
            ).apply(instance, HeimgrundChunkGenerator::new));

    public HeimgrundChunkGenerator(BiomeSource biomeSource) {
        super(biomeSource);
    }

    @Override
    protected MapCodec<? extends ChunkGenerator> getCodec() {
        return CODEC;
    }

    @Override
    public CompletableFuture<Chunk> populateNoise(Blender blender, NoiseConfig noiseConfig,
            StructureAccessor structureAccessor, Chunk chunk) {
        Heightmap oceanFloor = chunk.getHeightmap(Heightmap.Type.OCEAN_FLOOR_WG);
        Heightmap worldSurface = chunk.getHeightmap(Heightmap.Type.WORLD_SURFACE_WG);
        ChunkPos chunkPos = chunk.getPos();
        BlockPos.Mutable pos = new BlockPos.Mutable();

        int bottomY = chunk.getBottomY();
        for (int localX = 0; localX < 16; localX++) {
            for (int localZ = 0; localZ < 16; localZ++) {
                int x = chunkPos.getStartX() + localX;
                int z = chunkPos.getStartZ() + localZ;
                int surfaceY = ZoneLayout.surfaceHeight(x, z);
                ZoneLayout.Zone zone = ZoneLayout.zoneAt(x, z);
                int columnTop = Math.max(surfaceY, ZoneLayout.SEA_LEVEL);
                for (int y = bottomY; y <= columnTop; y++) {
                    BlockState state = stateAt(zone, x, y, z, surfaceY, bottomY);
                    if (state.isAir()) {
                        continue;
                    }
                    pos.set(localX, y, localZ);
                    chunk.setBlockState(pos, state);
                    oceanFloor.trackUpdate(localX, y, localZ, state);
                    worldSurface.trackUpdate(localX, y, localZ, state);
                }
            }
        }
        return CompletableFuture.completedFuture(chunk);
    }

    /**
     * The single material rule shared by {@link #populateNoise} and
     * {@link #getColumnSample}: bedrock floor, stone body, zone-specific surface,
     * water up to sea level in lake basins.
     */
    private BlockState stateAt(ZoneLayout.Zone zone, int x, int y, int z, int surfaceY, int bottomY) {
        if (y <= bottomY + 2) {
            return Blocks.BEDROCK.getDefaultState();
        }
        if (y > surfaceY) {
            if (zone == ZoneLayout.Zone.LAKE && y <= ZoneLayout.SEA_LEVEL) {
                return Blocks.WATER.getDefaultState();
            }
            return Blocks.AIR.getDefaultState();
        }
        if (zone == ZoneLayout.Zone.MOUNTAIN && ZoneLayout.isCaveAir(x, y, z)) {
            return Blocks.AIR.getDefaultState();
        }
        if (y == surfaceY && zone != ZoneLayout.Zone.MOUNTAIN && zone != ZoneLayout.Zone.LAKE) {
            // Authored surface overrides: hot-spot gravel aprons, gate-to-hotspot pathways,
            // and the lake beach strips. Order matters (apron wins over path).
            if (ZoneLayout.isHotspotApron(x, z)) {
                return (x * 31 + z * 17) % 5 == 0
                        ? Blocks.COBBLESTONE.getDefaultState()
                        : Blocks.GRAVEL.getDefaultState();
            }
            if (ZoneLayout.isPath(x, z)) {
                return zone == ZoneLayout.Zone.DESERT
                        ? Blocks.GRAVEL.getDefaultState()
                        : Blocks.DIRT_PATH.getDefaultState();
            }
            if (ZoneLayout.isBeach(x, z)) {
                return Blocks.SAND.getDefaultState();
            }
        }
        return switch (zone) {
            case MOUNTAIN -> Blocks.STONE.getDefaultState();
            case DESERT -> y >= surfaceY - 1 ? Blocks.SAND.getDefaultState()
                    : y >= surfaceY - 4 ? Blocks.SANDSTONE.getDefaultState()
                    : Blocks.STONE.getDefaultState();
            case LAKE -> y >= surfaceY - 2 ? Blocks.DIRT.getDefaultState()
                    : Blocks.STONE.getDefaultState();
            case CLEARING, MEADOW -> y == surfaceY ? Blocks.GRASS_BLOCK.getDefaultState()
                    : y >= surfaceY - 3 ? Blocks.DIRT.getDefaultState()
                    : Blocks.STONE.getDefaultState();
        };
    }

    @Override
    public void buildSurface(ChunkRegion region, StructureAccessor structures, NoiseConfig noiseConfig, Chunk chunk) {
        // Surface materials are already placed in populateNoise - one material rule, one pass.
    }

    @Override
    public void carve(ChunkRegion chunkRegion, long seed, NoiseConfig noiseConfig, BiomeAccess biomeAccess,
            StructureAccessor structureAccessor, Chunk chunk) {
        // No vanilla carvers; the mountain caves will be authored tunnels in ZoneLayout instead.
    }

    @Override
    public void populateEntities(ChunkRegion region) {
        // Vanilla's chunk-population pass - this is what seeds the biome "creature" lists
        // (meadow sheep/cows/chickens, mountain goats) at generation time. Without it the
        // world ships with zero passive animals: creature spawning barely happens after
        // generation (the tiny creature cap is instantly filled by persistent animals).
        ChunkPos chunkPos = region.getCenterPos();
        RegistryEntry<Biome> biome = region.getBiome(chunkPos.getStartPos().withY(ZoneLayout.SEA_LEVEL));
        ChunkRandom random = new ChunkRandom(Random.create(RandomSeed.getSeed()));
        random.setPopulationSeed(region.getSeed(), chunkPos.getStartX(), chunkPos.getStartZ());
        SpawnHelper.populateEntities(region, biome, chunkPos, random);
    }

    @Override
    public int getWorldHeight() {
        return ZoneLayout.WORLD_HEIGHT;
    }

    @Override
    public int getSeaLevel() {
        return ZoneLayout.SEA_LEVEL;
    }

    @Override
    public int getMinimumY() {
        return ZoneLayout.MIN_Y;
    }

    @Override
    public int getHeight(int x, int z, Heightmap.Type heightmap, HeightLimitView world, NoiseConfig noiseConfig) {
        int surfaceY = ZoneLayout.surfaceHeight(x, z);
        if (heightmap.getBlockPredicate().test(Blocks.WATER.getDefaultState())) {
            surfaceY = Math.max(surfaceY, ZoneLayout.SEA_LEVEL);
        }
        return surfaceY + 1;
    }

    @Override
    public VerticalBlockSample getColumnSample(int x, int z, HeightLimitView world, NoiseConfig noiseConfig) {
        int bottomY = world.getBottomY();
        int surfaceY = ZoneLayout.surfaceHeight(x, z);
        ZoneLayout.Zone zone = ZoneLayout.zoneAt(x, z);
        BlockState[] states = new BlockState[world.getHeight()];
        for (int i = 0; i < states.length; i++) {
            states[i] = stateAt(zone, x, bottomY + i, z, surfaceY, bottomY);
        }
        return new VerticalBlockSample(bottomY, states);
    }

    @Override
    public void appendDebugHudText(List<String> text, NoiseConfig noiseConfig, BlockPos pos) {
        text.add("Heimgrund zone: " + ZoneLayout.zoneAt(pos.getX(), pos.getZ())
                + " r=" + Math.round(ZoneLayout.radius(pos.getX(), pos.getZ())));
    }
}
