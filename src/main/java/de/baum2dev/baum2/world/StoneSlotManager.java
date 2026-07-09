package de.baum2dev.baum2.world;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.Uuids;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import de.baum2dev.baum2.Baum2;
import de.baum2dev.baum2.entity.FallenCometStoneEntity;
import de.baum2dev.baum2.registry.ModEntities;

/**
 * The authoritative "which Fallen Comet Stones exist in Heimgrund" table, implementing
 * the world rule <i>a stone only spawns again when a stone is destroyed</i>: a fixed
 * population of slots, each holding one stone of a fixed type; a destroyed stone flips
 * its slot to pending and the SAME stone type respawns {@link #RESPAWN_DELAY_TICKS}
 * (3 seconds) later at a NEARBY randomized position - never the exact same spot (user
 * rule) - sampled around the slot's fixed anchor so the population never drifts. No
 * free-running repopulation: the total stone count is always the slot count.
 *
 * <p>Slot positions are generated once per world by a fixed-seed scatter over
 * {@link ZoneLayout} (pure math - no chunks needed), then frozen in a persistent world
 * attachment, so later tuning of the scatter never moves existing worlds' stones.
 *
 * <p>The tick driver only acts where {@code shouldTickEntityAt} is true: spawns happen
 * when a player is close enough for the chunk to tick entities, and "stone vanished
 * without a death event" reconciliation can't false-positive on an unloaded chunk whose
 * entities simply aren't deserialized yet. Time is world time ({@code world.getTime()}),
 * which persists across restarts - a pending respawn timer survives a relog.
 */
public final class StoneSlotManager {

    /** 5 minutes. Long enough that a kill visibly empties the spot, short enough to farm. */
    public static final long RESPAWN_DELAY_TICKS = 60L;  // 3 seconds (user-decided)

    /** No slot closer to the village center than this - the clearing stays safe. */
    private static final int MIN_RADIUS = 100;
    /** Minimum distance between two slots. */
    private static final int MIN_SLOT_SPACING = 40;
    /** Mountain-zone slots stay on the climbable inner ramp, below the cliff band. */
    private static final int MOUNTAIN_SLOT_MAX_RADIUS = ZoneLayout.CLIFF_RADIUS - 10;
    private static final int SCATTER_ATTEMPTS = 4000;
    /** A respawn lands this far from the slot's anchor - never the exact same spot. */
    private static final int RESPAWN_MIN_OFFSET = 12;
    private static final int RESPAWN_MAX_OFFSET = 40;
    private static final int RESPAWN_ATTEMPTS = 40;

    /**
     * {@code anchor} is the slot's fixed home from the initial scatter; {@code pos} is where
     * the current/next stone actually stands. Respawns sample a fresh position around the
     * ANCHOR (not the last position), so a slot wanders per-respawn but can't drift out of
     * its zone over many respawns. {@code anchor} is optional in the codec for saves written
     * before it existed (defaults to {@code pos}).
     */
    public record StoneSlot(String stoneName, BlockPos pos, Optional<BlockPos> anchor,
            boolean alive, Optional<UUID> entityUuid, long respawnAtTime) {

        public static final Codec<StoneSlot> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.fieldOf("stone_name").forGetter(StoneSlot::stoneName),
                BlockPos.CODEC.fieldOf("pos").forGetter(StoneSlot::pos),
                BlockPos.CODEC.optionalFieldOf("anchor").forGetter(StoneSlot::anchor),
                Codec.BOOL.fieldOf("alive").forGetter(StoneSlot::alive),
                Uuids.CODEC.optionalFieldOf("entity_uuid").forGetter(StoneSlot::entityUuid),
                Codec.LONG.fieldOf("respawn_at_time").forGetter(StoneSlot::respawnAtTime)
        ).apply(instance, StoneSlot::new));

        static StoneSlot initial(String stoneName, BlockPos pos) {
            // Starts pending with respawnAtTime 0: the tick driver spawns it the first
            // time a player gets close enough - no special first-spawn code path.
            return new StoneSlot(stoneName, pos, Optional.of(pos), false, Optional.empty(), 0L);
        }

        BlockPos anchorPos() {
            return anchor.orElse(pos);
        }

        StoneSlot spawnedAt(BlockPos spawnPos, UUID uuid) {
            return new StoneSlot(stoneName, spawnPos, Optional.of(anchorPos()), true,
                    Optional.of(uuid), 0L);
        }

        StoneSlot pending(long respawnAt) {
            return new StoneSlot(stoneName, pos, Optional.of(anchorPos()), false,
                    Optional.empty(), respawnAt);
        }
    }

    private static final AttachmentType<List<StoneSlot>> SLOTS = AttachmentRegistry.create(
            Identifier.of("baum2", "stone_slots"),
            builder -> builder.persistent(StoneSlot.CODEC.listOf())
    );

    /** Definition-name -> EntityType, resolved from the existing per-definition registrations. */
    private static final Map<String, EntityType<FallenCometStoneEntity>> TYPES_BY_NAME = typesByName();

    private static Map<String, EntityType<FallenCometStoneEntity>> typesByName() {
        Map<String, EntityType<FallenCometStoneEntity>> byName = new LinkedHashMap<>();
        ModEntities.FALLEN_COMET_STONES.forEach((definition, type) -> byName.put(definition.name(), type));
        return byName;
    }

    /** No-op force-load; see PlayerLevelSystem.bootstrap() for why every AttachmentType holder needs one. */
    public static void bootstrap() {
    }

    public static void registerEvents() {
        ServerTickEvents.END_WORLD_TICK.register(world -> {
            if (!Baum2WorldKeys.isHeimgrund(world) || world.getTime() % 20 != 0) {
                return;
            }
            tickSlots(world);
        });

        ServerLivingEntityEvents.AFTER_DEATH.register((entity, damageSource) -> {
            if (!(entity instanceof FallenCometStoneEntity)
                    || !(entity.getEntityWorld() instanceof ServerWorld world)
                    || !Baum2WorldKeys.isHeimgrund(world)) {
                return;
            }
            List<StoneSlot> slots = world.getAttached(SLOTS);
            if (slots == null) {
                return;
            }
            UUID deadUuid = entity.getUuid();
            List<StoneSlot> updated = new ArrayList<>(slots.size());
            boolean changed = false;
            for (StoneSlot slot : slots) {
                if (slot.alive() && slot.entityUuid().map(deadUuid::equals).orElse(false)) {
                    updated.add(slot.pending(world.getTime() + RESPAWN_DELAY_TICKS));
                    changed = true;
                } else {
                    updated.add(slot);
                }
            }
            if (changed) {
                world.setAttached(SLOTS, List.copyOf(updated));
            }
        });
    }

    public static List<StoneSlot> getSlots(ServerWorld world) {
        List<StoneSlot> slots = world.getAttached(SLOTS);
        if (slots == null) {
            slots = generateSlots();
            world.setAttached(SLOTS, slots);
            Baum2.LOGGER.info("Generated {} fallen-comet-stone slots for Heimgrund", slots.size());
        }
        return slots;
    }

    private static void tickSlots(ServerWorld world) {
        List<StoneSlot> slots = getSlots(world);
        List<StoneSlot> updated = new ArrayList<>(slots.size());
        boolean changed = false;
        for (StoneSlot slot : slots) {
            StoneSlot next = tickSlot(world, slot);
            changed |= next != slot;
            updated.add(next);
        }
        if (changed) {
            world.setAttached(SLOTS, List.copyOf(updated));
        }
    }

    private static StoneSlot tickSlot(ServerWorld world, StoneSlot slot) {
        if (!world.shouldTickEntityAt(slot.pos())) {
            return slot;
        }
        if (slot.alive()) {
            Entity entity = slot.entityUuid().map(world::getEntity).orElse(null);
            if (entity instanceof FallenCometStoneEntity && entity.isAlive()) {
                return slot;
            }
            // Vanished without a death event (e.g. discarded by a command) - re-pend
            // immediately; the next driver pass respawns it.
            return slot.pending(world.getTime());
        }
        if (world.getTime() < slot.respawnAtTime()) {
            return slot;
        }
        EntityType<FallenCometStoneEntity> type = TYPES_BY_NAME.get(slot.stoneName());
        if (type == null) {
            Baum2.LOGGER.error("Stone slot references unknown stone '{}' - leaving it dormant", slot.stoneName());
            return slot;
        }
        BlockPos spawnPos = pickRespawnPos(world, slot);
        if (spawnPos == null) {
            return slot;  // no valid ticking spot this pass - retried every second
        }
        FallenCometStoneEntity stone = type.spawn(world, spawnPos, SpawnReason.MOB_SUMMONED);
        if (stone == null) {
            return slot;
        }
        return slot.spawnedAt(spawnPos, stone.getUuid());
    }

    /**
     * A fresh position near the slot's anchor - never the previous spot (user rule: a killed
     * stone respawns NEARBY, not in the same place). Candidates must stay in the slot's own
     * zone (a desert stone can't wander onto meadow grass), respect the world-geometry
     * limits, and land where entities tick so the spawn actually happens. Falls back to the
     * anchor itself if no offset candidate validates (e.g. an anchor on a small desert patch).
     */
    private static BlockPos pickRespawnPos(ServerWorld world, StoneSlot slot) {
        BlockPos anchor = slot.anchorPos();
        ZoneLayout.Zone zone = ZoneLayout.zoneAt(anchor.getX(), anchor.getZ());
        Random random = world.getRandom();
        for (int attempt = 0; attempt < RESPAWN_ATTEMPTS; attempt++) {
            double angle = random.nextDouble() * 2.0 * Math.PI;
            int offset = random.nextBetween(RESPAWN_MIN_OFFSET, RESPAWN_MAX_OFFSET);
            int x = anchor.getX() + (int) Math.round(Math.cos(angle) * offset);
            int z = anchor.getZ() + (int) Math.round(Math.sin(angle) * offset);
            double r = ZoneLayout.radius(x, z);
            if (r < MIN_RADIUS || ZoneLayout.zoneAt(x, z) != zone) {
                continue;
            }
            if (zone == ZoneLayout.Zone.MOUNTAIN && r > MOUNTAIN_SLOT_MAX_RADIUS) {
                continue;
            }
            BlockPos candidate = new BlockPos(x, ZoneLayout.surfaceHeight(x, z) + 1, z);
            if (world.shouldTickEntityAt(candidate)) {
                return candidate;
            }
        }
        return world.shouldTickEntityAt(anchor) ? anchor : null;
    }

    /**
     * Fixed-seed scatter, independent of the world seed (like all of Heimgrund).
     * Zone-matched to the biome spawner lists: silverfish stones in the meadow (the
     * weakest ring, nearest the village), zombie stones in the desert patches.
     * NO stones in the mountains (user rule, 2nd playtest) - spiders there stay
     * stone-less natural monsters. Five silverfish anchors form a deliberate ring
     * around the stone hot spot the south pathway leads to.
     */
    private static List<StoneSlot> generateSlots() {
        Random random = Random.create(ZoneLayout.FIXED_SEED ^ 0x57_0E5L);
        List<StoneSlot> slots = new ArrayList<>();
        // The stone-POI destinations of the road network get deliberate rings; the rest
        // is scattered between the roads.
        ring(slots, "stone_of_silverfish", ZoneLayout.STONE_HOTSPOT_X, ZoneLayout.STONE_HOTSPOT_Z, 5, 16);
        ring(slots, "stone_of_silverfish", ZoneLayout.WEST_CLUSTER_X, ZoneLayout.WEST_CLUSTER_Z, 3, 14);
        ring(slots, "stone_of_silverfish", ZoneLayout.NORTH_CLUSTER_X, ZoneLayout.NORTH_CLUSTER_Z, 3, 14);
        ring(slots, "stone_of_zombies", ZoneLayout.DESERT_POCKET_X, ZoneLayout.DESERT_POCKET_Z, 3, 14);
        scatter(slots, random, "stone_of_silverfish", 7, ZoneLayout.Zone.MEADOW);
        scatter(slots, random, "stone_of_zombies", 9, ZoneLayout.Zone.DESERT);
        return List.copyOf(slots);
    }

    private static void ring(List<StoneSlot> slots, String stoneName, int centerX, int centerZ,
            int count, int ringRadius) {
        for (int i = 0; i < count; i++) {
            double angle = i * (2.0 * Math.PI / count);
            int x = centerX + (int) Math.round(Math.cos(angle) * ringRadius);
            int z = centerZ + (int) Math.round(Math.sin(angle) * ringRadius);
            slots.add(StoneSlot.initial(stoneName,
                    new BlockPos(x, ZoneLayout.surfaceHeight(x, z) + 1, z)));
        }
    }

    private static void scatter(List<StoneSlot> slots, Random random, String stoneName, int count,
            ZoneLayout.Zone zone) {
        for (int i = 0; i < count; i++) {
            BlockPos pos = findSpot(slots, random, zone);
            if (pos == null) {
                // Deterministic given the fixed seed, so if this happens it happens in
                // dev, not surprisingly in some player's world.
                Baum2.LOGGER.warn("Could not scatter a {} slot into {} - skipping", stoneName, zone);
                continue;
            }
            slots.add(StoneSlot.initial(stoneName, pos));
        }
    }

    private static BlockPos findSpot(List<StoneSlot> existing, Random random, ZoneLayout.Zone zone) {
        int maxRadius = zone == ZoneLayout.Zone.MOUNTAIN ? MOUNTAIN_SLOT_MAX_RADIUS : ZoneLayout.MEADOW_OUTER_RADIUS;
        for (int attempt = 0; attempt < SCATTER_ATTEMPTS; attempt++) {
            int x = random.nextBetween(-maxRadius, maxRadius);
            int z = random.nextBetween(-maxRadius, maxRadius);
            double r = ZoneLayout.radius(x, z);
            if (r < MIN_RADIUS || r > maxRadius || ZoneLayout.zoneAt(x, z) != zone) {
                continue;
            }
            if (!farFromOthers(existing, x, z)) {
                continue;
            }
            return new BlockPos(x, ZoneLayout.surfaceHeight(x, z) + 1, z);
        }
        return null;
    }

    private static boolean farFromOthers(List<StoneSlot> existing, int x, int z) {
        for (StoneSlot slot : existing) {
            int dx = slot.pos().getX() - x;
            int dz = slot.pos().getZ() - z;
            if (dx * dx + dz * dz < MIN_SLOT_SPACING * MIN_SLOT_SPACING) {
                return false;
            }
        }
        return true;
    }

    private StoneSlotManager() {
    }
}
