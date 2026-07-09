package de.baum2dev.baum2.world;

import net.minecraft.util.math.noise.PerlinNoiseSampler;
import net.minecraft.util.math.random.Random;

/**
 * The authored radial map of Heimgrund as pure static math - no Minecraft-world
 * dependencies. Everything that needs to agree on "what is at (x, z)?" reads THIS class
 * (chunk generator, biome source, stone-slot scatter, village stamper), so terrain,
 * biomes, and spawn placement can never drift apart.
 *
 * <p>All noise is seeded from a hardcoded {@link #FIXED_SEED}, deliberately ignoring the
 * world seed: every Heimgrund is the same authored map, like an MMO zone, not a random
 * Minecraft world.
 *
 * <p>Layout, from the center outward (radii are tunable constants below):
 * <ul>
 *   <li>{@code r < 60} - the village clearing: perfectly flat grass at y=64.</li>
 *   <li>{@code 60..380} - meadow: gentle hills, lake basins (water at sea level 62), and
 *       desert patches (only beyond r=180, so the softest zone stays nearest the village).</li>
 *   <li>{@code 380..500} - the mountain ring: ramps steeply upward; its outer band is a
 *       sheer cliff (radial height steps >= 2 blocks, unjumpable) so the wall is genuinely
 *       unclimbable even though it is natural terrain.</li>
 *   <li>{@code r >= 500} - solid mountain crest forever; chunks past the world border are
 *       just more wall.</li>
 * </ul>
 */
public final class ZoneLayout {

    /** Deliberately NOT the world seed - every Heimgrund is the same authored map. */
    public static final long FIXED_SEED = 0x8A1DE2026_07_09L;

    public static final int SEA_LEVEL = 62;
    public static final int CLEARING_SURFACE_Y = 64;
    public static final int MIN_Y = 0;
    public static final int WORLD_HEIGHT = 256;

    public static final int CLEARING_RADIUS = 60;
    /** Clearing blends into meadow across this band so there is no terrain seam. */
    public static final int CLEARING_BLEND_RADIUS = 90;
    /** Desert patches only occur beyond this radius (tier 2 sits farther out than tier 1). */
    public static final int DESERT_MIN_RADIUS = 180;
    public static final int MEADOW_OUTER_RADIUS = 380;
    /** Start of the unclimbable cliff band inside the mountain ring. */
    public static final int CLIFF_RADIUS = 460;
    public static final int WORLD_RADIUS = 500;

    private static final int MOUNTAIN_CREST_Y = 170;

    // Independent samplers, each salted differently off the fixed seed. PerlinNoiseSampler
    // returns roughly [-1, 1].
    private static final PerlinNoiseSampler HILL_NOISE =
            new PerlinNoiseSampler(Random.create(FIXED_SEED));
    private static final PerlinNoiseSampler LAKE_NOISE =
            new PerlinNoiseSampler(Random.create(FIXED_SEED ^ 0x1A5E5L));
    private static final PerlinNoiseSampler DESERT_NOISE =
            new PerlinNoiseSampler(Random.create(FIXED_SEED ^ 0xD35E27L));
    private static final PerlinNoiseSampler RIDGE_NOISE =
            new PerlinNoiseSampler(Random.create(FIXED_SEED ^ 0x51D6EL));

    public enum Zone {
        CLEARING,
        MEADOW,
        LAKE,
        DESERT,
        MOUNTAIN
    }

    public static Zone zoneAt(int x, int z) {
        double r = radius(x, z);
        if (r < CLEARING_RADIUS) {
            return Zone.CLEARING;
        }
        if (r >= MEADOW_OUTER_RADIUS) {
            return Zone.MOUNTAIN;
        }
        if (isLake(x, z, r)) {
            return Zone.LAKE;
        }
        if (isDesert(x, z, r)) {
            return Zone.DESERT;
        }
        return Zone.MEADOW;
    }

    /** Y of the highest solid terrain block of the column (water, where present, sits above it). */
    public static int surfaceHeight(int x, int z) {
        double r = radius(x, z);

        // Meadow shape is the baseline everything else blends against.
        double meadow = meadowHeight(x, z);

        if (isLake(x, z, r)) {
            // Lake basins dip below sea level; lakeDepth eases from 0 at the shore to full
            // depth at the basin center so shores slope naturally.
            meadow = Math.min(meadow, SEA_LEVEL - 1 - 3.0 * lakeDepth(x, z));
        }

        double height;
        if (r < CLEARING_RADIUS) {
            height = CLEARING_SURFACE_Y;
        } else if (r < CLEARING_BLEND_RADIUS) {
            double t = smoothstep((r - CLEARING_RADIUS) / (double) (CLEARING_BLEND_RADIUS - CLEARING_RADIUS));
            height = lerp(t, CLEARING_SURFACE_Y, meadow);
        } else if (r < MEADOW_OUTER_RADIUS) {
            height = meadow;
        } else {
            height = mountainHeight(x, z, r, meadow);
        }

        return (int) Math.round(clamp(height, MIN_Y + 4, WORLD_HEIGHT - 8));
    }

    private static double meadowHeight(int x, int z) {
        // Two octaves: broad rolling hills plus small surface detail; y 62..~75.
        double broad = HILL_NOISE.sample(x * 0.012, 0.0, z * 0.012);
        double detail = HILL_NOISE.sample(x * 0.05, 100.0, z * 0.05);
        return 67.0 + broad * 5.5 + detail * 1.5;
    }

    private static double mountainHeight(int x, int z, double r, double meadow) {
        double ridge = RIDGE_NOISE.sample(x * 0.02, 0.0, z * 0.02) * 8.0;
        if (r >= WORLD_RADIUS) {
            return MOUNTAIN_CREST_Y + ridge;
        }
        if (r >= CLIFF_RADIUS) {
            // Cliff band: from the ramp top straight up to the crest across 40 blocks -
            // a radial gain of ~2.5 blocks per block, an unjumpable natural wall.
            double rampTop = rampHeight(x, z, CLIFF_RADIUS, meadow);
            double t = (r - CLIFF_RADIUS) / (double) (WORLD_RADIUS - CLIFF_RADIUS);
            return lerp(t, rampTop, MOUNTAIN_CREST_Y + ridge);
        }
        return rampHeight(x, z, r, meadow);
    }

    /** The walkable (steep but climbable) inner slope of the ring, r in [380, 460]. */
    private static double rampHeight(int x, int z, double r, double meadow) {
        double t = smoothstep((r - MEADOW_OUTER_RADIUS) / (double) (CLIFF_RADIUS - MEADOW_OUTER_RADIUS));
        double ridge = RIDGE_NOISE.sample(x * 0.03, 50.0, z * 0.03) * 6.0 * t;
        return lerp(t, meadow, 120.0) + ridge;
    }

    private static boolean isLake(int x, int z, double r) {
        // Lakes only in the open meadow band (not in the clearing blend, not in deserts,
        // not in the mountains).
        if (r < CLEARING_BLEND_RADIUS || r >= MEADOW_OUTER_RADIUS || isDesert(x, z, r)) {
            return false;
        }
        return lakeDepth(x, z) > 0.0;
    }

    /** 0 outside a lake; eases up to 1 toward a basin center. */
    private static double lakeDepth(int x, int z) {
        double v = LAKE_NOISE.sample(x * 0.008, 0.0, z * 0.008);
        return v <= 0.45 ? 0.0 : smoothstep((v - 0.45) / 0.25);
    }

    private static boolean isDesert(int x, int z, double r) {
        if (r < DESERT_MIN_RADIUS || r >= MEADOW_OUTER_RADIUS) {
            return false;
        }
        return DESERT_NOISE.sample(x * 0.006, 0.0, z * 0.006) > 0.5;
    }

    public static double radius(int x, int z) {
        return Math.sqrt((double) x * x + (double) z * z);
    }

    // --- Mountain caves ------------------------------------------------------------------
    //
    // Deterministic "worm" tunnels bored into the mountain ring: each starts at the ring's
    // inner face (its first spheres cut the slope open, forming a visible cave mouth) and
    // wanders outward/downward into the rock, never past CAVE_MAX_RADIUS - so no tunnel can
    // breach the outer wall. Carved as a bake-once list of spheres bucketed by chunk, which
    // the chunk generator consults per block.

    /** Tunnels stay strictly inside this radius; the wall beyond stays solid. */
    public static final int CAVE_MAX_RADIUS = WORLD_RADIUS - 12;
    private static final int CAVE_COUNT = 8;
    private static final int CAVE_STEPS = 110;
    private static final double CAVE_STEP_LENGTH = 1.4;

    private record CaveSphere(double x, double y, double z, double radius) {
    }

    private static final java.util.Map<Long, java.util.List<CaveSphere>> CAVES_BY_CHUNK = carveCaves();

    private static java.util.Map<Long, java.util.List<CaveSphere>> carveCaves() {
        Random random = Random.create(FIXED_SEED ^ 0xCA7E5L);
        java.util.Map<Long, java.util.List<CaveSphere>> byChunk = new java.util.HashMap<>();
        for (int i = 0; i < CAVE_COUNT; i++) {
            // Spread the mouths around the ring, with some jitter so it doesn't look dialed.
            double angle = (i + random.nextDouble() * 0.6) * (2.0 * Math.PI / CAVE_COUNT);
            double x = Math.cos(angle) * (MEADOW_OUTER_RADIUS + 8);
            double z = Math.sin(angle) * (MEADOW_OUTER_RADIUS + 8);
            double y = surfaceHeight((int) x, (int) z) + 1.5;
            // Initial heading: outward into the rock, gently downward.
            double yaw = angle;
            double pitch = -(0.05 + random.nextDouble() * 0.15);
            for (int step = 0; step < CAVE_STEPS; step++) {
                double sphereRadius = 2.0 + random.nextDouble() * 1.2;
                addSphere(byChunk, new CaveSphere(x, y, z, sphereRadius));
                yaw += (random.nextDouble() - 0.5) * 0.45;
                pitch += (random.nextDouble() - 0.5) * 0.25;
                pitch = Math.max(-0.5, Math.min(0.25, pitch));
                x += Math.cos(yaw) * Math.cos(pitch) * CAVE_STEP_LENGTH;
                z += Math.sin(yaw) * Math.cos(pitch) * CAVE_STEP_LENGTH;
                y += Math.sin(pitch) * CAVE_STEP_LENGTH;
                y = Math.max(MIN_Y + 8, y);
                if (radius((int) x, (int) z) > CAVE_MAX_RADIUS) {
                    // Bounce back inward instead of breaching the wall.
                    yaw += Math.PI * (0.75 + random.nextDouble() * 0.5);
                }
            }
        }
        return byChunk;
    }

    private static void addSphere(java.util.Map<Long, java.util.List<CaveSphere>> byChunk, CaveSphere sphere) {
        int minChunkX = (int) Math.floor((sphere.x() - sphere.radius()) / 16.0);
        int maxChunkX = (int) Math.floor((sphere.x() + sphere.radius()) / 16.0);
        int minChunkZ = (int) Math.floor((sphere.z() - sphere.radius()) / 16.0);
        int maxChunkZ = (int) Math.floor((sphere.z() + sphere.radius()) / 16.0);
        for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
            for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
                byChunk.computeIfAbsent(chunkKey(chunkX, chunkZ), key -> new java.util.ArrayList<>()).add(sphere);
            }
        }
    }

    private static long chunkKey(int chunkX, int chunkZ) {
        return ((long) chunkX << 32) ^ (chunkZ & 0xFFFFFFFFL);
    }

    /** True if this block is inside a carved mountain tunnel. */
    public static boolean isCaveAir(int x, int y, int z) {
        java.util.List<CaveSphere> spheres = CAVES_BY_CHUNK.get(chunkKey(x >> 4, z >> 4));
        if (spheres == null) {
            return false;
        }
        for (CaveSphere sphere : spheres) {
            double dx = sphere.x() - x;
            double dy = sphere.y() - y;
            double dz = sphere.z() - z;
            if (dx * dx + dy * dy + dz * dz <= sphere.radius() * sphere.radius()) {
                return true;
            }
        }
        return false;
    }

    private static double smoothstep(double t) {
        t = clamp(t, 0.0, 1.0);
        return t * t * (3.0 - 2.0 * t);
    }

    private static double lerp(double t, double from, double to) {
        return from + t * (to - from);
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private ZoneLayout() {
    }
}
