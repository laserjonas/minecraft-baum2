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

    // --- Hot spots & pathways ------------------------------------------------------------
    //
    // Two authored destinations (2nd-playtest feedback: the map needs places to head FOR),
    // each reached by a visible path from a village gate:
    //  - STONE hot spot: a gravel clearing in the south meadow where several stone slots
    //    cluster (StoneSlotManager anchors a ring of stones here).
    //  - CAVE hot spot: a gravel apron at the mountain foot due east, where carveCaves()
    //    bores a guaranteed, extra-wide "grand mouth" tunnel into the ring.

    public static final int STONE_HOTSPOT_X = 30;
    public static final int STONE_HOTSPOT_Z = 240;
    /** Lakes/desert are masked out this close to the stone hot spot - it is always meadow. */
    public static final int STONE_HOTSPOT_CLEAR_RADIUS = 28;
    public static final int STONE_HOTSPOT_APRON_RADIUS = 12;

    /** Where the east path meets the mountain ring; the grand cave mouth opens here. */
    public static final int CAVE_HOTSPOT_X = 378;
    public static final int CAVE_HOTSPOT_Z = 0;
    public static final int CAVE_HOTSPOT_APRON_RADIUS = 10;

    /** Branch-path destination: a smaller stone cluster in the west meadow. */
    public static final int WEST_CLUSTER_X = -140;
    public static final int WEST_CLUSTER_Z = 180;
    public static final int WEST_CLUSTER_CLEAR_RADIUS = 24;
    public static final int WEST_CLUSTER_APRON_RADIUS = 8;

    /**
     * Branch-path destination: a FORCED desert disc (the noise mask is overridden here),
     * so a zombie-stone destination exists regardless of where the noise puts patches.
     */
    public static final int DESERT_POCKET_X = 240;
    public static final int DESERT_POCKET_Z = -140;
    public static final int DESERT_POCKET_RADIUS = 30;
    public static final int DESERT_POCKET_APRON_RADIUS = 8;

    /** New (4th playtest): a second stone cluster due north, balancing the compass. */
    public static final int NORTH_CLUSTER_X = -40;
    public static final int NORTH_CLUSTER_Z = -240;
    public static final int NORTH_CLUSTER_CLEAR_RADIUS = 24;
    public static final int NORTH_CLUSTER_APRON_RADIUS = 8;

    /** New (4th playtest): a second grand cave mouth due west, balancing the east one. */
    public static final int WEST_CAVE_X = -378;
    public static final int WEST_CAVE_Z = -30;
    public static final int WEST_CAVE_APRON_RADIUS = 10;

    /**
     * The road NETWORK (4th playtest: paths must cover the whole map, never cross water,
     * and every road must end at a cave or a stone spot). Edges connect the six POIs into
     * a ring around the map plus the two village-gate spokes; each edge is ROUTED around
     * lakes at class-init time by A* over the deterministic terrain (2-block grid, lakes
     * with margin and the mountain ring as obstacles), so no path ever enters water.
     */
    private static final int[][][] ROAD_EDGES = {
            // village gates -> nearest POI
            {{0, 24}, {STONE_HOTSPOT_X, STONE_HOTSPOT_Z}},           // south gate -> stone hot spot
            {{24, 0}, {CAVE_HOTSPOT_X, CAVE_HOTSPOT_Z}},             // east gate -> east cave
            // the ring road, POI to POI around the compass
            {{STONE_HOTSPOT_X, STONE_HOTSPOT_Z}, {WEST_CLUSTER_X, WEST_CLUSTER_Z}},
            {{WEST_CLUSTER_X, WEST_CLUSTER_Z}, {WEST_CAVE_X, WEST_CAVE_Z}},
            {{WEST_CLUSTER_X, WEST_CLUSTER_Z}, {NORTH_CLUSTER_X, NORTH_CLUSTER_Z}},
            {{NORTH_CLUSTER_X, NORTH_CLUSTER_Z}, {DESERT_POCKET_X, DESERT_POCKET_Z}},
            {{DESERT_POCKET_X, DESERT_POCKET_Z}, {CAVE_HOTSPOT_X, CAVE_HOTSPOT_Z}},
            {{DESERT_POCKET_X, DESERT_POCKET_Z}, {24, 0}},           // pocket also links to the east spoke
            // closes the ring through the southeast quadrant (it was a C-shape without this)
            {{STONE_HOTSPOT_X, STONE_HOTSPOT_Z}, {CAVE_HOTSPOT_X, CAVE_HOTSPOT_Z}},
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
        return sqDist(x, z, STONE_HOTSPOT_X, STONE_HOTSPOT_Z)
                <= STONE_HOTSPOT_APRON_RADIUS * STONE_HOTSPOT_APRON_RADIUS
                || sqDist(x, z, CAVE_HOTSPOT_X, CAVE_HOTSPOT_Z)
                <= CAVE_HOTSPOT_APRON_RADIUS * CAVE_HOTSPOT_APRON_RADIUS
                || sqDist(x, z, WEST_CLUSTER_X, WEST_CLUSTER_Z)
                <= WEST_CLUSTER_APRON_RADIUS * WEST_CLUSTER_APRON_RADIUS
                || sqDist(x, z, DESERT_POCKET_X, DESERT_POCKET_Z)
                <= DESERT_POCKET_APRON_RADIUS * DESERT_POCKET_APRON_RADIUS
                || sqDist(x, z, NORTH_CLUSTER_X, NORTH_CLUSTER_Z)
                <= NORTH_CLUSTER_APRON_RADIUS * NORTH_CLUSTER_APRON_RADIUS
                || sqDist(x, z, WEST_CAVE_X, WEST_CAVE_Z)
                <= WEST_CAVE_APRON_RADIUS * WEST_CAVE_APRON_RADIUS;
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
                    double tentative = gScore.get(current) + Math.hypot(dx, dz);
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
        // Keep roads out of the village interior (they start at the gates).
        if (r < 20) {
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

    /** Lake test WITHOUT the path exclusion (used by routing, which must see all water). */
    private static boolean rawLake(int x, int z) {
        double r = radius(x, z);
        if (r < CLEARING_BLEND_RADIUS || r >= MEADOW_OUTER_RADIUS || isDesert(x, z, r)) {
            return false;
        }
        return lakeDepth(x, z) > 0.0;
    }

    private static double distanceToPolyline(int[][] points, int x, int z) {
        double best = Double.MAX_VALUE;
        for (int i = 0; i < points.length - 1; i++) {
            best = Math.min(best, distanceToSegment(x, z,
                    points[i][0], points[i][1], points[i + 1][0], points[i + 1][1]));
        }
        return best;
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

    private static boolean isLake(int x, int z, double r) {
        // Lakes only in the open meadow band (not in the clearing blend, not in deserts,
        // not in the mountains) - and never on a stone-POI clearing. Roads need no
        // exclusion: they are routed around water and can't touch it.
        if (r < CLEARING_BLEND_RADIUS || r >= MEADOW_OUTER_RADIUS || isDesert(x, z, r)) {
            return false;
        }
        if (sqDist(x, z, STONE_HOTSPOT_X, STONE_HOTSPOT_Z)
                        <= (long) STONE_HOTSPOT_CLEAR_RADIUS * STONE_HOTSPOT_CLEAR_RADIUS
                || sqDist(x, z, WEST_CLUSTER_X, WEST_CLUSTER_Z)
                        <= (long) WEST_CLUSTER_CLEAR_RADIUS * WEST_CLUSTER_CLEAR_RADIUS
                || sqDist(x, z, NORTH_CLUSTER_X, NORTH_CLUSTER_Z)
                        <= (long) NORTH_CLUSTER_CLEAR_RADIUS * NORTH_CLUSTER_CLEAR_RADIUS) {
            return false;
        }
        return lakeDepth(x, z) > 0.0;
    }

    /** 0 outside a lake; eases up to 1 toward a basin center. */
    private static double lakeDepth(int x, int z) {
        double v = LAKE_NOISE.sample(x * 0.008, 0.0, z * 0.008);
        return v <= 0.45 ? 0.0 : smoothstep((v - 0.45) / 0.25);
    }

    /** 0 away from lakes; rises to 1 approaching the waterline (the beach band). */
    private static double shoreFactor(int x, int z) {
        double v = LAKE_NOISE.sample(x * 0.008, 0.0, z * 0.008);
        if (v <= 0.33 || v > 0.45) {
            return 0.0;
        }
        return smoothstep((v - 0.33) / 0.12);
    }

    /**
     * Gates which stretches of a lake's shoreline are flat walk-out exits (roughly half of
     * it, in several separate arcs) versus a low bank - independent noise so the exits are
     * scattered around each lake, not one contiguous side.
     */
    private static boolean isShoreExit(int x, int z) {
        return HILL_NOISE.sample(x * 0.03, 300.0, z * 0.03) > -0.05;
    }

    /** Sand marks the flat walk-out shore stretches (visual "you can exit here" signal). */
    public static boolean isBeach(int x, int z) {
        double r = radius(x, z);
        if (r < CLEARING_BLEND_RADIUS || r >= MEADOW_OUTER_RADIUS || isDesert(x, z, r)) {
            return false;
        }
        return shoreFactor(x, z) > 0.5 && isShoreExit(x, z) && !isLake(x, z, r);
    }

    private static boolean isDesert(int x, int z, double r) {
        if (r < DESERT_MIN_RADIUS || r >= MEADOW_OUTER_RADIUS) {
            return false;
        }
        // The desert-branch destination is a FORCED desert disc, so the zombie-stone POI
        // exists regardless of where the noise puts patches.
        if (sqDist(x, z, DESERT_POCKET_X, DESERT_POCKET_Z)
                <= (long) DESERT_POCKET_RADIUS * DESERT_POCKET_RADIUS) {
            return true;
        }
        // Stone POI clearings are always meadow; paths however KEEP the desert biome they
        // cross (only the surface material changes) - a 3-wide meadow biome sliver through
        // a desert patch would look broken.
        if (sqDist(x, z, STONE_HOTSPOT_X, STONE_HOTSPOT_Z)
                <= (long) STONE_HOTSPOT_CLEAR_RADIUS * STONE_HOTSPOT_CLEAR_RADIUS
                || sqDist(x, z, WEST_CLUSTER_X, WEST_CLUSTER_Z)
                        <= (long) WEST_CLUSTER_CLEAR_RADIUS * WEST_CLUSTER_CLEAR_RADIUS
                || sqDist(x, z, NORTH_CLUSTER_X, NORTH_CLUSTER_Z)
                        <= (long) NORTH_CLUSTER_CLEAR_RADIUS * NORTH_CLUSTER_CLEAR_RADIUS) {
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

        // The "grand mouths" at the two cave hot spots: guaranteed, extra-wide entrances
        // exactly where the road network meets the mountain foot, so a road visibly leads
        // INTO the mountain. Bored first so the ordinary tunnels can't crowd them out.
        carveTunnel(byChunk, random,
                CAVE_HOTSPOT_X + 6, surfaceHeight(CAVE_HOTSPOT_X + 6, CAVE_HOTSPOT_Z) + 2.0,
                CAVE_HOTSPOT_Z, 0.0, 3.4, 120);
        carveTunnel(byChunk, random,
                WEST_CAVE_X - 6, surfaceHeight(WEST_CAVE_X - 6, WEST_CAVE_Z) + 2.0,
                WEST_CAVE_Z, Math.PI, 3.4, 120);

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
