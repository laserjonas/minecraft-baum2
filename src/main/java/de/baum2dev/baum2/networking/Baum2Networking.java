package de.baum2dev.baum2.networking;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.network.ServerPlayerEntity;
import de.baum2dev.baum2.classes.ClassManager;

/**
 * Central networking registry and utility class for all Baum2 custom packets.
 * Handles payload type registration and packet sending.
 *
 * Payloads are registered using StreamCodec with RegistryFriendlyByteBuf (Minecraft 1.21.11 standard).
 */
public class Baum2Networking {
    /**
     * Register all custom payload types (both S2C and C2S). Must run in the common
     * entrypoint so both logical sides know how to en-/decode each payload.
     */
    public static void registerServerPayloads() {
        PayloadTypeRegistry.playS2C().register(
                ExperienceSyncPayload.TYPE,
                ExperienceSyncPayload.CODEC
        );
        PayloadTypeRegistry.playC2S().register(
                ClassSelectPayload.TYPE,
                ClassSelectPayload.CODEC
        );
    }

    /**
     * Register server-side packet receivers. Must be called from the mod's common
     * (server-reachable) entrypoint.
     */
    public static void registerServerReceivers() {
        ServerPlayNetworking.registerGlobalReceiver(ClassSelectPayload.TYPE, (payload, context) ->
            ClassManager.selectClass(context.player(), payload.playerClass()));
    }

    /**
     * Send experience level data to a player.
     * Thread-safe: can be called from any thread, will be executed on server thread.
     *
     * @param player the target player
     * @param level the custom experience level
     * @param experience the current experience amount
     * @param maxExperience the max experience for the current level
     */
    public static void syncPlayerExperience(ServerPlayerEntity player, int level, long experience, long maxExperience) {
        ExperienceSyncPayload payload = new ExperienceSyncPayload(level, experience, maxExperience);
        ServerPlayNetworking.send(player, payload);
    }
}
