package de.baum2dev.baum2.networking;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.network.ServerPlayerEntity;

/**
 * Central networking registry and utility class for all Baum2 custom packets.
 * Handles payload type registration and packet sending.
 *
 * Payloads are registered using StreamCodec with RegistryFriendlyByteBuf (Minecraft 1.21.11 standard).
 */
public class Baum2Networking {
    /**
     * Register all custom payload types for S2C transmission.
     * Must be called on mod initialization (server-side entry point).
     * Client-side registration happens in Baum2Client.
     *
     * Registers payloads to PayloadTypeRegistry.playS2C() with their StreamCodecs.
     */
    public static void registerServerPayloads() {
        // Register the experience sync payload for S2C transmission
        PayloadTypeRegistry.playS2C().register(
                ExperienceSyncPayload.TYPE,
                ExperienceSyncPayload.CODEC
        );

        // Register the mana sync payload for S2C transmission
        PayloadTypeRegistry.playS2C().register(
                ManaSyncPayload.TYPE,
                ManaSyncPayload.CODEC
        );

        // Register the combat stats sync payload for S2C transmission
        PayloadTypeRegistry.playS2C().register(
                CombatStatsSyncPayload.TYPE,
                CombatStatsSyncPayload.CODEC
        );
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

    /**
     * Send current/max Mana to a player. Thread-safe: can be called from any thread.
     */
    public static void syncPlayerMana(ServerPlayerEntity player, int mana, int maxMana) {
        ManaSyncPayload payload = new ManaSyncPayload(mana, maxMana);
        ServerPlayNetworking.send(player, payload);
    }

    /**
     * Send Base Damage/Base Magic Damage to a player. Thread-safe: can be called from any
     * thread. These are flat values that don't change on their own, so unlike Mana this only
     * needs to be sent on join (or again if a future gear/skill system changes them).
     */
    public static void syncPlayerCombatStats(ServerPlayerEntity player, float baseDamage, float baseMagicDamage) {
        CombatStatsSyncPayload payload = new CombatStatsSyncPayload(baseDamage, baseMagicDamage);
        ServerPlayNetworking.send(player, payload);
    }
}
