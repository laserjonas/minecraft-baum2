package de.baum2dev.baum2.world;

import java.util.List;
import java.util.Map;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.random.Random;

/**
 * Actively keeps Heimgrund's monster zones DANGEROUS (2nd-playtest feedback: minutes of
 * searching to find any monster). Vanilla natural spawning spreads one global mob cap
 * over every loaded chunk and trickles spawns probabilistically - fine for survival
 * Minecraft, far too thin for an authored MMO zone. This director instead enforces a
 * per-player local population: every few seconds, if a player standing in a monster zone
 * has fewer of that zone's monsters around them than the zone's target, the difference
 * is topped up just outside their immediate reach.
 *
 * <p>Spawns use {@code SpawnReason.NATURAL}, so vanilla's own despawn rules clean up
 * whatever players walk away from - the director never grows the world's population
 * beyond "each player's surroundings feel full".
 *
 * <p>The village clearing spawns nothing, and biome spawner lists stay active alongside
 * this (they provide ambient variety; the director provides the guaranteed floor).
 */
public final class ZoneSpawnDirector {

    private static final int CHECK_INTERVAL_TICKS = 100;  // one top-up pass every 5s
    /** Monsters within this radius of the player count toward the zone target. */
    private static final int SCAN_RADIUS = 48;
    /** New monsters appear in this distance band - outside immediate reach, inside sight. */
    private static final int SPAWN_MIN_DIST = 20;
    private static final int SPAWN_MAX_DIST = 44;
    private static final int MAX_SPAWNS_PER_PASS = 4;
    private static final int PLACEMENT_ATTEMPTS = 24;

    private record WeightedType(EntityType<?> type, int weight) {
    }

    private record ZonePopulation(List<WeightedType> types, int target) {
    }

    /** Per-zone monster mix + how many should surround a player standing there. */
    private static final Map<ZoneLayout.Zone, ZonePopulation> POPULATIONS = Map.of(
            ZoneLayout.Zone.MEADOW, new ZonePopulation(
                    List.of(new WeightedType(EntityType.SILVERFISH, 1)), 8),
            ZoneLayout.Zone.LAKE, new ZonePopulation(
                    List.of(new WeightedType(EntityType.SILVERFISH, 1)), 8),
            ZoneLayout.Zone.DESERT, new ZonePopulation(
                    List.of(new WeightedType(EntityType.ZOMBIE, 4),
                            new WeightedType(EntityType.SILVERFISH, 1)), 12),
            ZoneLayout.Zone.MOUNTAIN, new ZonePopulation(
                    List.of(new WeightedType(EntityType.SPIDER, 4),
                            new WeightedType(EntityType.CAVE_SPIDER, 1)), 12)
    );

    /**
     * The spider TERRITORY (user rework map: spider stones live in the SE flatland now, not
     * the mountains) overrides the underlying zone's mix - a player near the spider stones
     * gets ambient spiders, matching the stone waves there (balance-reviewer drift finding).
     */
    private static final ZonePopulation SPIDER_TERRITORY = new ZonePopulation(
            List.of(new WeightedType(EntityType.SPIDER, 4),
                    new WeightedType(EntityType.CAVE_SPIDER, 1)), 10);
    private static final int SPIDER_TERRITORY_RADIUS = 70;

    public static void registerEvents() {
        ServerTickEvents.END_WORLD_TICK.register(world -> {
            if (!Baum2WorldKeys.isHeimgrund(world) || world.getTime() % CHECK_INTERVAL_TICKS != 0) {
                return;
            }
            for (ServerPlayerEntity player : world.getPlayers()) {
                if (!player.isSpectator()) {
                    topUpAround(world, player);
                }
            }
        });
    }

    private static void topUpAround(ServerWorld world, ServerPlayerEntity player) {
        BlockPos playerPos = player.getBlockPos();
        ZoneLayout.Zone zone = ZoneLayout.zoneAt(playerPos.getX(), playerPos.getZ());
        ZonePopulation population = POPULATIONS.get(zone);
        if (population == null) {
            return;  // village clearing: stays safe
        }
        long dxSpider = playerPos.getX() - ZoneLayout.SPIDER_STONES_X;
        long dzSpider = playerPos.getZ() - ZoneLayout.SPIDER_STONES_Z;
        if (dxSpider * dxSpider + dzSpider * dzSpider
                <= (long) SPIDER_TERRITORY_RADIUS * SPIDER_TERRITORY_RADIUS) {
            population = SPIDER_TERRITORY;
        }
        Box scanBox = player.getBoundingBox().expand(SCAN_RADIUS);
        int present = world.getEntitiesByClass(MobEntity.class, scanBox,
                mob -> isZoneType(population, mob.getType())).size();
        int deficit = Math.min(population.target() - present, MAX_SPAWNS_PER_PASS);
        Random random = world.getRandom();
        for (int i = 0; i < deficit; i++) {
            BlockPos spawnPos = pickSpawnPos(world, playerPos, zone, random);
            if (spawnPos != null) {
                pickType(population, random).spawn(world, spawnPos, SpawnReason.NATURAL);
            }
        }
    }

    private static boolean isZoneType(ZonePopulation population, EntityType<?> type) {
        for (WeightedType weighted : population.types()) {
            if (weighted.type() == type) {
                return true;
            }
        }
        return false;
    }

    private static EntityType<?> pickType(ZonePopulation population, Random random) {
        int total = 0;
        for (WeightedType weighted : population.types()) {
            total += weighted.weight();
        }
        int roll = random.nextInt(total);
        for (WeightedType weighted : population.types()) {
            roll -= weighted.weight();
            if (roll < 0) {
                return weighted.type();
            }
        }
        return population.types().get(0).type();
    }

    private static BlockPos pickSpawnPos(ServerWorld world, BlockPos playerPos,
            ZoneLayout.Zone zone, Random random) {
        for (int attempt = 0; attempt < PLACEMENT_ATTEMPTS; attempt++) {
            double angle = random.nextDouble() * 2.0 * Math.PI;
            int dist = random.nextBetween(SPAWN_MIN_DIST, SPAWN_MAX_DIST);
            int x = playerPos.getX() + (int) Math.round(Math.cos(angle) * dist);
            int z = playerPos.getZ() + (int) Math.round(Math.sin(angle) * dist);
            if (ZoneLayout.zoneAt(x, z) != zone) {
                continue;  // don't leak a zone's monsters across its border
            }
            BlockPos candidate = new BlockPos(x, ZoneLayout.surfaceHeight(x, z) + 1, z);
            if (world.shouldTickEntityAt(candidate)) {
                return candidate;
            }
        }
        return null;
    }

    private ZoneSpawnDirector() {
    }
}
