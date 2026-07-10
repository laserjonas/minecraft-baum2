package de.baum2dev.baum2.mounts;

import net.minecraft.entity.SpawnReason;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import de.baum2dev.baum2.entity.MountHorseEntity;
import de.baum2dev.baum2.registry.ModEntities;

/**
 * Server-side mount summon/dismiss logic behind the Ctrl+H toggle (ToggleMountPayload).
 * The summoned horse is ephemeral - it exists only while ridden. Dismissing (or sneaking
 * off, or the horse dying) discards it; the flute in the equipment slot is the persistent
 * representation of "owning" the mount, not the entity.
 */
public final class MountManager {

    public static void toggleMount(ServerPlayerEntity player) {
        if (player.getVehicle() instanceof MountHorseEntity mount) {
            player.stopRiding();
            mount.discard();
            return;
        }

        MountEquipmentManager.equippedTier(player).ifPresentOrElse(
            tier -> summon(player, tier),
            () -> player.sendMessage(Text.literal("You need a horse flute in your equipment's mount slot."), true)
        );
    }

    private static void summon(ServerPlayerEntity player, MountTier tier) {
        // Riding anything else (a boat, a vanilla horse) while summoning would leave two
        // vehicles fighting over the player - step off whatever they're on first.
        if (player.hasVehicle()) {
            player.stopRiding();
        }

        ServerWorld world = (ServerWorld) player.getEntityWorld();
        // create(...) + manual positioning instead of the one-call EntityType.spawn(...):
        // the mount must appear exactly at the player (not BlockPos-centered with random yaw)
        // so startRiding feels seamless. See docs/fabric-modding.md "Spawning additional
        // hostile mobs server-side".
        MountHorseEntity mount = ModEntities.MOUNT_HORSES.get(tier).create(world, SpawnReason.MOB_SUMMONED);
        if (mount == null) {
            return;
        }
        mount.refreshPositionAndAngles(player.getX(), player.getY(), player.getZ(), player.getYaw(), 0.0F);
        // Belt-and-braces: lifecycle is handled by removePassenger->discard, so vanilla's
        // distance-despawn logic must never race it (e.g. rider AFK far from other players).
        mount.setPersistent();
        world.spawnEntity(mount);
        // force = true: the plain overload silently fails if the player is sneaking or on the
        // 60-tick post-dismount riding cooldown (docs/fabric-modding.md, startRiding notes).
        if (!player.startRiding(mount, true, true)) {
            mount.discard();
        }
    }

    private MountManager() {
    }
}
