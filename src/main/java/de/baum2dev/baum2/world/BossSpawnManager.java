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
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.Uuids;
import net.minecraft.util.math.BlockPos;
import de.baum2dev.baum2.Baum2;
import de.baum2dev.baum2.registry.ModEntities;

/**
 * The three roaming-boss spawn points of the user's rework map (5th playtest), each on a
 * 3-minute respawn timer (user-decided): Spider Queen at the EAST grand cave mouth,
 * Silverfish Broodcaller at the WEST grand cave mouth, Zombie Colossus at its north-west
 * clearing. Same persistent-slot machinery as {@link StoneSlotManager} (world attachment,
 * world-time timers that survive restarts, entity-ticking-gated driver), but bosses
 * respawn at their FIXED point - only the stones wander.
 */
public final class BossSpawnManager {

    /** 3 minutes (user-decided). */
    public static final long RESPAWN_DELAY_TICKS = 3600L;

    public record BossSlot(String bossName, BlockPos pos, boolean alive,
            Optional<UUID> entityUuid, long respawnAtTime) {

        public static final Codec<BossSlot> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.fieldOf("boss_name").forGetter(BossSlot::bossName),
                BlockPos.CODEC.fieldOf("pos").forGetter(BossSlot::pos),
                Codec.BOOL.fieldOf("alive").forGetter(BossSlot::alive),
                Uuids.CODEC.optionalFieldOf("entity_uuid").forGetter(BossSlot::entityUuid),
                Codec.LONG.fieldOf("respawn_at_time").forGetter(BossSlot::respawnAtTime)
        ).apply(instance, BossSlot::new));

        static BossSlot initial(String bossName, BlockPos pos) {
            return new BossSlot(bossName, pos, false, Optional.empty(), 0L);
        }

        BossSlot spawned(UUID uuid) {
            return new BossSlot(bossName, pos, true, Optional.of(uuid), 0L);
        }

        BossSlot pending(long respawnAt) {
            return new BossSlot(bossName, pos, false, Optional.empty(), respawnAt);
        }
    }

    private static final AttachmentType<List<BossSlot>> BOSS_SLOTS = AttachmentRegistry.create(
            Identifier.of("baum2", "boss_slots"),
            builder -> builder.persistent(BossSlot.CODEC.listOf())
    );

    private static final Map<String, EntityType<? extends HostileEntity>> BOSS_TYPES = bossTypes();

    private static Map<String, EntityType<? extends HostileEntity>> bossTypes() {
        Map<String, EntityType<? extends HostileEntity>> types = new LinkedHashMap<>();
        types.put("spider_queen", ModEntities.SPIDER_QUEEN);
        types.put("zombie_colossus", ModEntities.ZOMBIE_COLOSSUS);
        types.put("silverfish_broodcaller", ModEntities.SILVERFISH_BROODCALLER);
        return types;
    }

    /** No-op force-load; see PlayerLevelSystem.bootstrap(). */
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
            if (!(entity instanceof HostileEntity)
                    || !(entity.getEntityWorld() instanceof ServerWorld world)
                    || !Baum2WorldKeys.isHeimgrund(world)) {
                return;
            }
            List<BossSlot> slots = world.getAttached(BOSS_SLOTS);
            if (slots == null) {
                return;
            }
            UUID deadUuid = entity.getUuid();
            List<BossSlot> updated = new ArrayList<>(slots.size());
            boolean changed = false;
            for (BossSlot slot : slots) {
                if (slot.alive() && slot.entityUuid().map(deadUuid::equals).orElse(false)) {
                    updated.add(slot.pending(world.getTime() + RESPAWN_DELAY_TICKS));
                    changed = true;
                } else {
                    updated.add(slot);
                }
            }
            if (changed) {
                world.setAttached(BOSS_SLOTS, List.copyOf(updated));
            }
        });
    }

    public static List<BossSlot> getSlots(ServerWorld world) {
        List<BossSlot> slots = world.getAttached(BOSS_SLOTS);
        if (slots == null) {
            slots = generateSlots();
            world.setAttached(BOSS_SLOTS, slots);
            Baum2.LOGGER.info("Generated {} boss spawn points for Heimgrund", slots.size());
        }
        return slots;
    }

    private static void tickSlots(ServerWorld world) {
        List<BossSlot> slots = getSlots(world);
        List<BossSlot> updated = new ArrayList<>(slots.size());
        boolean changed = false;
        for (BossSlot slot : slots) {
            BossSlot next = tickSlot(world, slot);
            changed |= next != slot;
            updated.add(next);
        }
        if (changed) {
            world.setAttached(BOSS_SLOTS, List.copyOf(updated));
        }
    }

    private static BossSlot tickSlot(ServerWorld world, BossSlot slot) {
        if (!world.shouldTickEntityAt(slot.pos())) {
            return slot;
        }
        if (slot.alive()) {
            Entity entity = slot.entityUuid().map(world::getEntity).orElse(null);
            if (entity instanceof HostileEntity && entity.isAlive()) {
                return slot;
            }
            return slot.pending(world.getTime());
        }
        if (world.getTime() < slot.respawnAtTime()) {
            return slot;
        }
        EntityType<? extends HostileEntity> type = BOSS_TYPES.get(slot.bossName());
        if (type == null) {
            Baum2.LOGGER.error("Boss slot references unknown boss '{}' - leaving it dormant", slot.bossName());
            return slot;
        }
        HostileEntity boss = type.spawn(world, slot.pos(), SpawnReason.MOB_SUMMONED);
        if (boss == null) {
            return slot;
        }
        return slot.spawned(boss.getUuid());
    }

    private static List<BossSlot> generateSlots() {
        return List.of(
                BossSlot.initial("spider_queen", apronPos(ZoneLayout.CAVE_HOTSPOT_X - 6, ZoneLayout.CAVE_HOTSPOT_Z)),
                BossSlot.initial("silverfish_broodcaller", apronPos(ZoneLayout.WEST_CAVE_X + 6, ZoneLayout.WEST_CAVE_Z)),
                BossSlot.initial("zombie_colossus", apronPos(ZoneLayout.ZOMBIE_BOSS_X, ZoneLayout.ZOMBIE_BOSS_Z))
        );
    }

    private static BlockPos apronPos(int x, int z) {
        return new BlockPos(x, ZoneLayout.surfaceHeight(x, z) + 1, z);
    }

    private BossSpawnManager() {
    }
}
