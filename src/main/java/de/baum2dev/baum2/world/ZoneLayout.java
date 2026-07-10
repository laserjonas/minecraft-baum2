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
 *   <li>{@code 100..380} - meadow: gentle hills, four authored lakes (water at sea level
 *       62), and three authored desert territories - all traced from the user's map.</li>
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

    public static final int CLEARING_RADIUS = 100;
    /** Clearing blends into meadow across this band so there is no terrain seam. */
    public static final int CLEARING_BLEND_RADIUS = 130;
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
    /** Biases the road router's step cost so roads meander instead of running straight. */
    private static final PerlinNoiseSampler WIND_NOISE =
            new PerlinNoiseSampler(Random.create(FIXED_SEED ^ 0x3EADL));

    // The authored lake/desert shapes MUST be declared before ROADS_BY_CHUNK below: road
    // routing runs at class init and treats water as an obstacle, so it reads these spines.

    /** Per lake: spine vertices of {x, z, halfWidth} (traced from the user's map). */
    private static final double[][][] LAKE_SPINES = {
            // north-west lake (wide southern bulge, tapering north arm)
            {{-181, -295, 10}, {-177, -260, 30}, {-172, -210, 42}, {-176, -150, 48},
                    {-205, -128, 60}, {-212, -100, 52}, {-206, -80, 38}, {-198, -52, 14}},
            // south-east lake (long diagonal; the bridge crosses its widest middle)
            {{221, -35, 9}, {219, 10, 15}, {206, 38, 20}, {184, 62, 29}, {160, 93, 33},
                    {136, 126, 28}, {114, 160, 19}, {84, 190, 19}, {68, 218, 26}, {64, 238, 12}},
            // west pond
            {{-287, 63, 16}, {-294, 85, 18}},
            // north pond (south-east of the zombie boss clearing)
            {{-88, -352, 14}, {-90, -344, 14}},
    };

    /** How far the terrain-easing shore band reaches inland from the waterline. */
    private static final double SHORE_BAND_WIDTH = 12.0;

    /** Per territory (index-aligned with {@link Territory}): spine of {x, z, halfWidth}. */
    private static final double[][][] DESERT_SPINES = {
            // zombie territory (north-east, nearly round)
            {{143, -196, 55}, {143, -172, 55}},
            // silverfish territory (south-west, tilted north-west to south-east)
            {{-185, 140, 52}, {-105, 215, 52}},
            // spider territory (south-east, elongated NNE-SSW along the mountain foot)
            {{268, 88, 50}, {200, 228, 50}},
    };

    public enum Zone {
        CLEARING,
        MEADOW,
        LAKE,
        DESERT,
        MOUNTAIN
    }

    // --- Hot spots & pathways ------------------------------------------------------------
    //
    // POI layout per the user's hand-drawn rework map (5th playtest iteration,
    // run/heimgrund_rework_map.png): three stone territories, three boss spawns, curved
    // roads, one bridge over the southeast lake.

    // All positions below are measured off the user's map pixel-exactly (the dots/patches
    // were extracted programmatically), so the in-game world matches the drawing.

    /** Zombie-stone territory (north-east sand patch, orange dot on the map). */
    public static final int ZOMBIE_STONES_X = 142;
    public static final int ZOMBIE_STONES_Z = -186;
    public static final int ZOMBIE_STONES_APRON_RADIUS = 10;

    /** Silverfish-stone territory (south-west sand patch). */
    public static final int SILVERFISH_STONES_X = -146;
    public static final int SILVERFISH_STONES_Z = 178;
    public static final int SILVERFISH_STONES_APRON_RADIUS = 10;

    /** Spider-stone territory (south-east sand patch, across the bridge). */
    public static final int SPIDER_STONES_X = 246;
    public static final int SPIDER_STONES_Z = 158;
    public static final int SPIDER_STONES_APRON_RADIUS = 10;

    /** East grand cave mouth - the road into the spider boss's cave chamber. */
    public static final int CAVE_HOTSPOT_X = 378;
    public static final int CAVE_HOTSPOT_Z = 0;
    public static final int CAVE_HOTSPOT_APRON_RADIUS = 10;

    /** West grand cave mouth - the road into the silverfish boss's cave chamber. */
    public static final int WEST_CAVE_X = -378;
    public static final int WEST_CAVE_Z = -30;
    public static final int WEST_CAVE_APRON_RADIUS = 10;

    /** Zombie boss (Zombie Colossus) spawn clearing, north-west meadow. */
    public static final int ZOMBIE_BOSS_X = -102;
    public static final int ZOMBIE_BOSS_Z = -284;
    public static final int ZOMBIE_BOSS_APRON_RADIUS = 8;

    // --- Boss cave chambers ---------------------------------------------------------------
    //
    // The spider and silverfish bosses live INSIDE their caves (user rule), so each grand
    // cave gets a carved boss chamber: a flat-floored dome big enough for a large GeckoLib
    // boss to move and jump freely, connected to its cave mouth by a straight, gently
    // descending corridor (guaranteed connectivity - the wandering tunnel from the same
    // mouth adds depth but is not relied on).

    public static final int EAST_BOSS_ROOM_X = 424;
    public static final int EAST_BOSS_ROOM_Z = 0;
    public static final int WEST_BOSS_ROOM_X = -424;
    public static final int WEST_BOSS_ROOM_Z = -30;
    /** First AIR y of a chamber - the boss spawns standing at this height. */
    public static final int BOSS_ROOM_FLOOR_Y = 60;
    public static final int BOSS_ROOM_RADIUS = 16;
    public static final int BOSS_ROOM_HEIGHT = 11;

    /**
     * The bridge over the southeast lake (the violet line on the user's map): a fixed,
     * straight road segment allowed to cross water; the generator renders a plank deck
     * over any span where the terrain is below the waterline. Endpoints sit a few blocks
     * up the banks so the routed roads can reach them on dry land.
     */
    public static final int BRIDGE_WEST_X = 128;
    public static final int BRIDGE_WEST_Z = 68;
    public static final int BRIDGE_EAST_X = 188;
    public static final int BRIDGE_EAST_Z = 128;
    public static final int BRIDGE_DECK_Y = SEA_LEVEL + 2;

    // Road junctions traced from the user's map (the points where its drawn roads fork).
    private static final int[] JUNCTION_WEST = {-200, 20};
    private static final int[] JUNCTION_EAST = {116, -4};
    private static final int[] JUNCTION_NORTH = {-10, -110};

    /**
     * The road NETWORK, edge-for-edge the network the user drew: an east-west main road
     * straight through the village (west cave to east cave, arcing north around the
     * southeast lake's finger), a north road forking to the zombie stones and the zombie
     * boss, a southwest fork to the silverfish stones, a southeast fork over the bridge
     * to the spider stones, and a south arc linking the silverfish and spider territories
     * below the lake. No south gate - the Great Hall occupies the village's south.
     * Every segment is A*-routed around lakes/mountains at class-init, with a noise bias
     * on the step cost so roads meander organically instead of running straight.
     */
    private static final int[][][] ROAD_EDGES = {
            // east-west main road (through the village between the west and east gates)
            {{-46, 0}, JUNCTION_WEST},
            {JUNCTION_WEST, {WEST_CAVE_X, WEST_CAVE_Z}},
            {{46, 0}, JUNCTION_EAST},
            {JUNCTION_EAST, {CAVE_HOTSPOT_X, CAVE_HOTSPOT_Z}},
            // southwest fork to the silverfish territory
            {JUNCTION_WEST, {SILVERFISH_STONES_X, SILVERFISH_STONES_Z}},
            // southeast fork over the bridge to the spider territory
            {JUNCTION_EAST, {BRIDGE_WEST_X, BRIDGE_WEST_Z}},
            {{BRIDGE_EAST_X, BRIDGE_EAST_Z}, {SPIDER_STONES_X, SPIDER_STONES_Z}},
            // north road, forking to the zombie stones and the zombie boss
            {{0, -46}, JUNCTION_NORTH},
            {JUNCTION_NORTH, {ZOMBIE_STONES_X, ZOMBIE_STONES_Z}},
            {JUNCTION_NORTH, {ZOMBIE_BOSS_X, ZOMBIE_BOSS_Z}},
            // south arc below the southeast lake
            {{SILVERFISH_STONES_X, SILVERFISH_STONES_Z}, {SPIDER_STONES_X, SPIDER_STONES_Z}},
    };
    private static final double PATH_HALF_WIDTH = 1.6;
    private static final int ROUTE_GRID_STEP = 2;
    /** Routed roads keep this far from water and other obstacles. */
    private static final int ROUTE_OBSTACLE_MARGIN = 4;

    /** Routed road segments bucketed by chunk (same trick as the cave spheres). */
    private static final java.util.Map<Long, java.util.List<double[]>> ROADS_BY_CHUNK = routeRoads();

    /** True if (x,z) lies on one of the routed roads. */
    public static boolean isPath(int x, int z) {
        return pathDistance(x, z) <= PATH_HALF_WIDTH;
    }

    private static double pathDistance(int x, int z) {
        double best = Double.MAX_VALUE;
        for (int chunkX = (x - 8) >> 4; chunkX <= (x + 8) >> 4; chunkX++) {
            for (int chunkZ = (z - 8) >> 4; chunkZ <= (z + 8) >> 4; chunkZ++) {
                java.util.List<double[]> segments = ROADS_BY_CHUNK.get(chunkKey(chunkX, chunkZ));
                if (segments == null) {
                    continue;
                }
                for (double[] seg : segments) {
                    best = Math.min(best, distanceToSegment(x, z, seg[0], seg[1], seg[2], seg[3]));
                }
            }
        }
        return best;
    }

    /** True inside any hot-spot/POI gravel apron circle. */
    public static boolean isHotspotApron(int x, int z) {
        return sqDist(x, z, ZOMBIE_STONES_X, ZOMBIE_STONES_Z)
                <= ZOMBIE_STONES_APRON_RADIUS * ZOMBIE_STONES_APRON_RADIUS
                || sqDist(x, z, SILVERFISH_STONES_X, SILVERFISH_STONES_Z)
                <= SILVERFISH_STONES_APRON_RADIUS * SILVERFISH_STONES_APRON_RADIUS
                || sqDist(x, z, SPIDER_STONES_X, SPIDER_STONES_Z)
                <= SPIDER_STONES_APRON_RADIUS * SPIDER_STONES_APRON_RADIUS
                || sqDist(x, z, CAVE_HOTSPOT_X, CAVE_HOTSPOT_Z)
                <= CAVE_HOTSPOT_APRON_RADIUS * CAVE_HOTSPOT_APRON_RADIUS
                || sqDist(x, z, WEST_CAVE_X, WEST_CAVE_Z)
                <= WEST_CAVE_APRON_RADIUS * WEST_CAVE_APRON_RADIUS
                || sqDist(x, z, ZOMBIE_BOSS_X, ZOMBIE_BOSS_Z)
                <= ZOMBIE_BOSS_APRON_RADIUS * ZOMBIE_BOSS_APRON_RADIUS;
    }

    /** True on the bridge line over the southeast lake. */
    public static boolean isBridge(int x, int z) {
        return distanceToSegment(x, z, BRIDGE_WEST_X, BRIDGE_WEST_Z, BRIDGE_EAST_X, BRIDGE_EAST_Z)
                <= PATH_HALF_WIDTH;
    }

    // --- Road routing (A*, runs once at class init; fully deterministic) -----------------

    private static java.util.Map<Long, java.util.List<double[]>> routeRoads() {
        java.util.Map<Long, java.util.List<double[]>> byChunk = new java.util.HashMap<>();
        for (int[][] edge : ROAD_EDGES) {
            java.util.List<int[]> route = routeEdge(edge[0], edge[1]);
            for (int i = 0; i < route.size() - 1; i++) {
                addRoadSegment(byChunk, route.get(i), route.get(i + 1));
            }
        }
        // The bridge is a fixed straight segment allowed over water (rendered as a deck).
        addRoadSegment(byChunk, new int[]{BRIDGE_WEST_X, BRIDGE_WEST_Z},
                new int[]{BRIDGE_EAST_X, BRIDGE_EAST_Z});
        return byChunk;
    }

    private static void addRoadSegment(java.util.Map<Long, java.util.List<double[]>> byChunk,
            int[] a, int[] b) {
        double[] seg = {a[0], a[1], b[0], b[1]};
        int minChunkX = (Math.min(a[0], b[0]) - 8) >> 4;
        int maxChunkX = (Math.max(a[0], b[0]) + 8) >> 4;
        int minChunkZ = (Math.min(a[1], b[1]) - 8) >> 4;
        int maxChunkZ = (Math.max(a[1], b[1]) + 8) >> 4;
        for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
            for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
                byChunk.computeIfAbsent(chunkKey(chunkX, chunkZ), key -> new java.util.ArrayList<>())
                        .add(seg);
            }
        }
    }

    /** A* on a 2-block grid; lakes (+margin) and the mountain ring are obstacles. */
    private static java.util.List<int[]> routeEdge(int[] from, int[] to) {
        long start = nodeKey(Math.floorDiv(from[0], ROUTE_GRID_STEP), Math.floorDiv(from[1], ROUTE_GRID_STEP));
        int goalX = Math.floorDiv(to[0], ROUTE_GRID_STEP);
        int goalZ = Math.floorDiv(to[1], ROUTE_GRID_STEP);
        long goal = nodeKey(goalX, goalZ);

        java.util.Map<Long, Long> cameFrom = new java.util.HashMap<>();
        java.util.Map<Long, Double> gScore = new java.util.HashMap<>();
        java.util.Set<Long> closed = new java.util.HashSet<>();
        // Entries: {fScore, nodeKeyAsDouble-safe via index} - store key in a parallel long
        // via ordering trick: use double[]{f, keyHigh, keyLow} to stay allocation-simple.
        java.util.PriorityQueue<long[]> open = new java.util.PriorityQueue<>(
                java.util.Comparator.comparingLong(entry -> entry[0]));
        gScore.put(start, 0.0);
        open.add(new long[]{0L, start});

        while (!open.isEmpty()) {
            long current = open.poll()[1];
            if (!closed.add(current)) {
                continue;  // stale queue entry
            }
            if (current == goal) {
                return reconstruct(cameFrom, current);
            }
            int cx = nodeX(current);
            int cz = nodeZ(current);
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    if (dx == 0 && dz == 0) {
                        continue;
                    }
                    long next = nodeKey(cx + dx, cz + dz);
                    if (closed.contains(next)
                            || routeBlocked((cx + dx) * ROUTE_GRID_STEP, (cz + dz) * ROUTE_GRID_STEP, to)) {
                        continue;
                    }
                    // Step cost carries a noise bias (1.0 .. ~1.8): the router follows
                    // noise valleys, which is what makes the roads curve organically
                    // (user request: "no straight paths"). Heuristic stays admissible
                    // because the multiplier never drops below 1.
                    double wind = 1.0 + 0.4 * (WIND_NOISE.sample(
                            (cx + dx) * ROUTE_GRID_STEP * 0.012, 0.0,
                            (cz + dz) * ROUTE_GRID_STEP * 0.012) + 1.0);
                    double tentative = gScore.get(current) + Math.hypot(dx, dz) * wind;
                    if (tentative < gScore.getOrDefault(next, Double.MAX_VALUE)) {
                        gScore.put(next, tentative);
                        cameFrom.put(next, current);
                        double f = tentative + Math.hypot(cx + dx - goalX, cz + dz - goalZ);
                        // scale f to a long for a stable comparator (µ-block precision)
                        open.add(new long[]{(long) (f * 1000.0), next});
                    }
                }
            }
        }
        // Should never happen on this map; a straight line beats a missing road.
        return java.util.List.of(from, to);
    }

    private static long nodeKey(int x, int z) {
        return ((long) x << 32) ^ (z & 0xFFFFFFFFL);
    }

    private static int nodeX(long key) {
        return (int) (key >> 32);
    }

    private static int nodeZ(long key) {
        return (int) ((key >> 32) << 32 ^ key);
    }

    private static java.util.List<int[]> reconstruct(java.util.Map<Long, Long> cameFrom, long end) {
        java.util.ArrayList<int[]> points = new java.util.ArrayList<>();
        Long current = end;
        while (current != null) {
            points.add(new int[]{nodeX(current) * ROUTE_GRID_STEP, nodeZ(current) * ROUTE_GRID_STEP});
            current = cameFrom.get(current);
        }
        java.util.Collections.reverse(points);
        return simplify(points);
    }

    /** Collapse collinear runs so the segment lists stay small. */
    private static java.util.List<int[]> simplify(java.util.List<int[]> points) {
        if (points.size() < 3) {
            return points;
        }
        java.util.ArrayList<int[]> out = new java.util.ArrayList<>();
        out.add(points.get(0));
        for (int i = 1; i < points.size() - 1; i++) {
            int[] prev = out.get(out.size() - 1);
            int[] here = points.get(i);
            int[] next = points.get(i + 1);
            int cross = (here[0] - prev[0]) * (next[1] - prev[1]) - (here[1] - prev[1]) * (next[0] - prev[0]);
            if (cross != 0) {
                out.add(here);
            }
        }
        out.add(points.get(points.size() - 1));
        return out;
    }

    private static boolean routeBlocked(int x, int z, int[] goal) {
        double r = radius(x, z);
        if (r > WORLD_RADIUS - 5) {
            return true;
        }
        // Mountains are off-limits except the final approach to a cave-mouth POI.
        if (r > MEADOW_OUTER_RADIUS - 2 && sqDist(x, z, goal[0], goal[1]) > 24 * 24) {
            return true;
        }
        // Keep roads out of the village interior (they start at the three gates, which sit
        // on the 91x91 template's perimeter wall).
        if (r < 44) {
            return true;
        }
        // Water plus a safety margin: sample the cell and its margin ring.
        return lakeAtOrNear(x, z);
    }

    private static boolean lakeAtOrNear(int x, int z) {
        return rawLake(x, z)
                || rawLake(x + ROUTE_OBSTACLE_MARGIN, z) || rawLake(x - ROUTE_OBSTACLE_MARGIN, z)
                || rawLake(x, z + ROUTE_OBSTACLE_MARGIN) || rawLake(x, z - ROUTE_OBSTACLE_MARGIN);
    }

    /** Lake test for the router (which must see all water). */
    private static boolean rawLake(int x, int z) {
        return isLake(x, z, radius(x, z));
    }

    private static double distanceToSegment(double px, double pz, double ax, double az,
            double bx, double bz) {
        double dx = bx - ax;
        double dz = bz - az;
        double lengthSq = dx * dx + dz * dz;
        double t = lengthSq == 0 ? 0 : clamp(((px - ax) * dx + (pz - az) * dz) / lengthSq, 0.0, 1.0);
        double cx = ax + t * dx;
        double cz = az + t * dz;
        return Math.hypot(px - cx, pz - cz);
    }

    private static long sqDist(int x, int z, int ox, int oz) {
        long dx = x - ox;
        long dz = z - oz;
        return dx * dx + dz * dz;
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

        // (Fords were removed after the 4th playtest: roads are A*-routed AROUND water at
        // class-init, so a road never touches a lake in the first place.)

        // Shore easing (playtest feedback: lakes had cliff edges you couldn't climb out of).
        // Where the exit gate passes, terrain drops to water level (walk straight out);
        // elsewhere it only eases to a low ~2-block bank - so every lake has SEVERAL easy
        // exits rather than one uniform beach ring (user request: "not everywhere, but
        // multiple chances to get back").
        double shore = shoreFactor(x, z);
        if (shore > 0.0) {
            double target = isShoreExit(x, z) ? SEA_LEVEL + 0.4 : SEA_LEVEL + 2.4;
            if (meadow > target) {
                meadow = lerp(shore, meadow, target);
            }
        }

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

    // --- Authored lakes --------------------------------------------------------------------
    //
    // The four lakes are traced from the user's rework map (spine polylines with a
    // half-width per vertex, extracted from the drawing's water pixels), instead of the
    // old free noise blobs - the user's rule is "match the seas". A small noise wobble on
    // the edge distance keeps shorelines organic.

    /**
     * Signed distance from (x,z) to the nearest lake's shoreline: negative inside water,
     * positive on land. Interpolates the spine's per-vertex half-width and adds a noise
     * wobble so the edge is organic rather than a mathematical capsule.
     */
    private static double lakeEdgeDistance(int x, int z) {
        double best = Double.MAX_VALUE;
        for (double[][] spine : LAKE_SPINES) {
            for (int i = 0; i < spine.length - 1; i++) {
                double[] a = spine[i];
                double[] b = spine[i + 1];
                double dx = b[0] - a[0];
                double dz = b[1] - a[1];
                double lengthSq = dx * dx + dz * dz;
                double t = lengthSq == 0 ? 0.0
                        : clamp(((x - a[0]) * dx + (z - a[1]) * dz) / lengthSq, 0.0, 1.0);
                double dist = Math.hypot(x - (a[0] + t * dx), z - (a[1] + t * dz));
                best = Math.min(best, dist - lerp(t, a[2], b[2]));
            }
        }
        return best + LAKE_NOISE.sample(x * 0.05, 0.0, z * 0.05) * 4.0;
    }

    private static boolean isLake(int x, int z, double r) {
        // The clearing blend and the mountain ring stay dry no matter what the authored
        // shapes say (safety clip; the spines are authored well inside the meadow band).
        if (r < CLEARING_BLEND_RADIUS || r >= MEADOW_OUTER_RADIUS) {
            return false;
        }
        return lakeDepth(x, z) > 0.0;
    }

    /** 0 outside a lake; eases up to 1 toward a basin center. */
    private static double lakeDepth(int x, int z) {
        double edge = lakeEdgeDistance(x, z);
        return edge >= 0.0 ? 0.0 : smoothstep(Math.min(1.0, -edge / 18.0));
    }

    /** 0 away from lakes; rises to 1 approaching the waterline (the beach band). */
    private static double shoreFactor(int x, int z) {
        double edge = lakeEdgeDistance(x, z);
        if (edge <= 0.0 || edge > SHORE_BAND_WIDTH) {
            return 0.0;
        }
        return smoothstep((SHORE_BAND_WIDTH - edge) / SHORE_BAND_WIDTH);
    }

    /**
     * Gates which stretches of a lake's shoreline are flat walk-out exits (roughly half of
     * it, in several separate arcs) versus a low bank - independent noise so the exits are
     * scattered around each lake, not one contiguous side.
     */
    private static boolean isShoreExit(int x, int z) {
        return HILL_NOISE.sample(x * 0.03, 300.0, z * 0.03) > -0.05;
    }

    /** Sand fringes the lakes (drawn that way on the user's map), widest at walk-out exits. */
    public static boolean isBeach(int x, int z) {
        double r = radius(x, z);
        if (r < CLEARING_BLEND_RADIUS || r >= MEADOW_OUTER_RADIUS || isDesert(x, z, r)) {
            return false;
        }
        if (isLake(x, z, r)) {
            return false;
        }
        double edge = lakeEdgeDistance(x, z);
        return edge <= 4.0 || (edge <= SHORE_BAND_WIDTH * 0.7 && isShoreExit(x, z));
    }

    // --- Authored desert territories --------------------------------------------------------
    //
    // The user's map has exactly three sand patches - one per stone territory (zombies NE,
    // silverfish SW, spiders SE) - and no random desert anywhere else, so the old noise
    // patches are gone. Same capsule-with-wobble shape math as the lakes.

    /** Which monster territory a desert capsule belongs to. */
    public enum Territory {
        ZOMBIES,
        SILVERFISH,
        SPIDERS
    }

    /**
     * The territory whose sand patch covers (x,z), or null outside all three. Used by the
     * stone scatter and the spawn director so a territory's ambient monsters match its
     * stones even though all three patches share the DESERT zone/biome.
     */
    public static Territory territoryAt(int x, int z) {
        double r = radius(x, z);
        if (r >= MEADOW_OUTER_RADIUS) {
            return null;
        }
        Territory best = null;
        double bestEdge = 0.0;
        for (int i = 0; i < DESERT_SPINES.length; i++) {
            double edge = desertEdgeDistance(DESERT_SPINES[i], x, z);
            if (edge < bestEdge) {
                bestEdge = edge;
                best = Territory.values()[i];
            }
        }
        return best;
    }

    /** Signed distance to one desert capsule's edge (negative inside), with noise wobble. */
    private static double desertEdgeDistance(double[][] spine, int x, int z) {
        double best = Double.MAX_VALUE;
        for (int i = 0; i < spine.length - 1; i++) {
            double[] a = spine[i];
            double[] b = spine[i + 1];
            double dx = b[0] - a[0];
            double dz = b[1] - a[1];
            double lengthSq = dx * dx + dz * dz;
            double t = lengthSq == 0 ? 0.0
                    : clamp(((x - a[0]) * dx + (z - a[1]) * dz) / lengthSq, 0.0, 1.0);
            double dist = Math.hypot(x - (a[0] + t * dx), z - (a[1] + t * dz));
            best = Math.min(best, dist - lerp(t, a[2], b[2]));
        }
        return best + DESERT_NOISE.sample(x * 0.03, 0.0, z * 0.03) * 8.0;
    }

    private static boolean isDesert(int x, int z, double r) {
        if (r >= MEADOW_OUTER_RADIUS) {
            return false;
        }
        return territoryAt(x, z) != null;
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

        // The "grand mouths" at the two cave hot spots: guaranteed, extra-wide entrances
        // exactly where the road network meets the mountain foot, so a road visibly leads
        // INTO the mountain. Bored first so the ordinary tunnels can't crowd them out.
        carveTunnel(byChunk, random,
                CAVE_HOTSPOT_X + 6, surfaceHeight(CAVE_HOTSPOT_X + 6, CAVE_HOTSPOT_Z) + 2.0,
                CAVE_HOTSPOT_Z, 0.0, 3.4, 120);
        carveTunnel(byChunk, random,
                WEST_CAVE_X - 6, surfaceHeight(WEST_CAVE_X - 6, WEST_CAVE_Z) + 2.0,
                WEST_CAVE_Z, Math.PI, 3.4, 120);

        // Straight corridors from each grand mouth down into its boss chamber, so the
        // chamber is guaranteed reachable no matter where the wandering tunnels went.
        carveCorridor(byChunk,
                CAVE_HOTSPOT_X + 6, surfaceHeight(CAVE_HOTSPOT_X + 6, CAVE_HOTSPOT_Z) + 2.0,
                CAVE_HOTSPOT_Z,
                EAST_BOSS_ROOM_X - BOSS_ROOM_RADIUS + 4, BOSS_ROOM_FLOOR_Y + 2.5, EAST_BOSS_ROOM_Z);
        carveCorridor(byChunk,
                WEST_CAVE_X - 6, surfaceHeight(WEST_CAVE_X - 6, WEST_CAVE_Z) + 2.0,
                WEST_CAVE_Z,
                WEST_BOSS_ROOM_X + BOSS_ROOM_RADIUS - 4, BOSS_ROOM_FLOOR_Y + 2.5, WEST_BOSS_ROOM_Z);

        for (int i = 0; i < CAVE_COUNT; i++) {
            // Spread the mouths around the ring, with some jitter so it doesn't look dialed.
            double angle = (i + random.nextDouble() * 0.6) * (2.0 * Math.PI / CAVE_COUNT);
            double x = Math.cos(angle) * (MEADOW_OUTER_RADIUS + 8);
            double z = Math.sin(angle) * (MEADOW_OUTER_RADIUS + 8);
            double y = surfaceHeight((int) x, (int) z) + 1.5;
            carveTunnel(byChunk, random, x, y, z, angle, 2.0, CAVE_STEPS);
        }
        return byChunk;
    }

    private static void carveTunnel(java.util.Map<Long, java.util.List<CaveSphere>> byChunk,
            Random random, double x, double y, double z, double startYaw, double baseRadius,
            int steps) {
        double yaw = startYaw;
        // Initial heading: outward into the rock, gently downward.
        double pitch = -(0.05 + random.nextDouble() * 0.15);
        for (int step = 0; step < steps; step++) {
            double sphereRadius = baseRadius + random.nextDouble() * 1.2;
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

    /** A straight run of spheres from A to B - deterministic, no wander. */
    private static void carveCorridor(java.util.Map<Long, java.util.List<CaveSphere>> byChunk,
            double x, double y, double z, double toX, double toY, double toZ) {
        double length = Math.sqrt((toX - x) * (toX - x) + (toY - y) * (toY - y) + (toZ - z) * (toZ - z));
        int steps = (int) Math.ceil(length / 2.0);
        for (int step = 0; step <= steps; step++) {
            double t = step / (double) steps;
            addSphere(byChunk, new CaveSphere(
                    lerp(t, x, toX), lerp(t, y, toY), lerp(t, z, toZ), 3.4));
        }
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

    /** True if this block is inside a carved mountain tunnel or a boss chamber. */
    public static boolean isCaveAir(int x, int y, int z) {
        if (isBossRoomAir(x, y, z, EAST_BOSS_ROOM_X, EAST_BOSS_ROOM_Z)
                || isBossRoomAir(x, y, z, WEST_BOSS_ROOM_X, WEST_BOSS_ROOM_Z)) {
            return true;
        }
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

    /**
     * A boss chamber: flat floor at {@link #BOSS_ROOM_FLOOR_Y}, vertical walls for the
     * lower half, then a dome closing at {@link #BOSS_ROOM_HEIGHT} - a room a large boss
     * can move and jump in without clipping the ceiling.
     */
    private static boolean isBossRoomAir(int x, int y, int z, int centerX, int centerZ) {
        int dy = y - BOSS_ROOM_FLOOR_Y;
        if (dy < 0 || dy > BOSS_ROOM_HEIGHT) {
            return false;
        }
        double vertical = dy / (double) BOSS_ROOM_HEIGHT;
        double shrink = vertical <= 0.5 ? 1.0
                : Math.sqrt(Math.max(0.0, 1.0 - (vertical - 0.5) * 2.0 * (vertical - 0.5) * 2.0));
        double roomRadius = BOSS_ROOM_RADIUS * shrink;
        long dx = x - centerX;
        long dz = z - centerZ;
        return dx * dx + dz * dz <= roomRadius * roomRadius;
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
