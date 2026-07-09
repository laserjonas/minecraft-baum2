package de.baum2dev.baum2.world;

import com.mojang.serialization.Codec;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.TeleportTarget;

/**
 * Makes the game start (and stay) in Heimgrund. Vanilla has no API for "the world spawn
 * is in a custom dimension", so this routes players there at the two moments vanilla
 * would otherwise put them in the overworld:
 *
 * <ul>
 *   <li><b>First join ever</b> - teleports to the village center and sets a persistent
 *       per-player attachment flag, so later joins stay wherever the player logged out.
 *       (A brief overworld flicker on the very first join is a known, accepted cosmetic.)</li>
 *   <li><b>Respawn after death</b> - if the respawn resolved to a world other than
 *       Heimgrund (no bed/anchor: vanilla falls back to overworld spawn), re-routes to
 *       the village center.</li>
 * </ul>
 */
public final class PlayerStartHandler {

    /** True once the player has been placed in Heimgrund for the first time. */
    private static final AttachmentType<Boolean> HEIMGRUND_INITIALIZED = AttachmentRegistry.create(
            Identifier.of("baum2", "heimgrund_initialized"),
            builder -> builder
                    .persistent(Codec.BOOL)
                    .copyOnDeath()
    );

    /**
     * No-op force-load, same reason as {@code PlayerLevelSystem.bootstrap()}: the
     * AttachmentType must be registered before any player's saved data is deserialized.
     */
    public static void bootstrap() {
    }

    public static void registerEvents() {
        ServerPlayerEvents.JOIN.register(player -> {
            if (Boolean.TRUE.equals(player.getAttached(HEIMGRUND_INITIALIZED))) {
                return;
            }
            ServerWorld heimgrund = heimgrundOrNull(player);
            if (heimgrund == null) {
                return;
            }
            player.setAttached(HEIMGRUND_INITIALIZED, true);
            teleportToVillageCenter(player, heimgrund);
        });

        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> {
            if (Baum2WorldKeys.isHeimgrund(newPlayer.getEntityWorld())) {
                return;
            }
            ServerWorld heimgrund = heimgrundOrNull(newPlayer);
            if (heimgrund != null) {
                teleportToVillageCenter(newPlayer, heimgrund);
            }
        });
    }

    /** Null in saves created before the mod was installed (datapack dimensions bake at creation). */
    private static ServerWorld heimgrundOrNull(ServerPlayerEntity player) {
        return player.getEntityWorld().getServer().getWorld(Baum2WorldKeys.HEIMGRUND);
    }

    private static void teleportToVillageCenter(ServerPlayerEntity player, ServerWorld heimgrund) {
        player.teleportTo(new TeleportTarget(
                heimgrund,
                new Vec3d(0.5, ZoneLayout.CLEARING_SURFACE_Y + 1, 0.5),
                Vec3d.ZERO,
                player.getYaw(),
                player.getPitch(),
                TeleportTarget.NO_OP
        ));
    }

    private PlayerStartHandler() {
    }
}
