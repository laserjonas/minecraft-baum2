package de.baum2dev.baum2.networking;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.network.ServerPlayerEntity;
import de.baum2dev.baum2.classes.ClassManager;
import de.baum2dev.baum2.progression.AttributeManager;
import de.baum2dev.baum2.progression.PlayerLevelSystem;
import de.baum2dev.baum2.progression.PlayerProgressData;
import de.baum2dev.baum2.progression.VitalsManager;

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
        PayloadTypeRegistry.playS2C().register(
                ManaSyncPayload.TYPE,
                ManaSyncPayload.CODEC
        );
        PayloadTypeRegistry.playS2C().register(
                AttributeSyncPayload.TYPE,
                AttributeSyncPayload.CODEC
        );
        PayloadTypeRegistry.playC2S().register(
                SpendAttributePointPayload.TYPE,
                SpendAttributePointPayload.CODEC
        );
    }

    /**
     * Register server-side packet receivers. Must be called from the mod's common
     * (server-reachable) entrypoint.
     */
    public static void registerServerReceivers() {
        ServerPlayNetworking.registerGlobalReceiver(ClassSelectPayload.TYPE, (payload, context) ->
            ClassManager.selectClass(context.player(), payload.playerClass()));

        ServerPlayNetworking.registerGlobalReceiver(
                SpendAttributePointPayload.TYPE,
                (payload, context) -> {
                    ServerPlayerEntity player = context.player();
                    PlayerProgressData progress = PlayerLevelSystem.getPlayerProgress(player);
                    if (AttributeManager.trySpendPoint(progress, payload.attribute())) {
                        PlayerLevelSystem.savePlayerProgress(player, progress);
                        // Re-apply the affected real attribute immediately - persistent
                        // modifiers don't need per-tick reapplication, only when the
                        // underlying Strength/Dexterity value actually changes.
                        VitalsManager.applyBaseAttack(player, progress.getStrength());
                        VitalsManager.applyAttackSpeed(player, progress.getDexterity());
                    }
                }
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
     * Send the four raw attributes plus unspent points to a player. Thread-safe: can be
     * called from any thread.
     */
    public static void syncPlayerAttributes(ServerPlayerEntity player, PlayerProgressData progress) {
        AttributeSyncPayload payload = new AttributeSyncPayload(
                progress.getEndurance(), progress.getIntelligence(), progress.getStrength(),
                progress.getDexterity(), progress.getUnspentAttributePoints()
        );
        ServerPlayNetworking.send(player, payload);
    }
}
