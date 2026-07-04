package de.baum2dev.baum2.networking;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import de.baum2dev.baum2.progression.ProgressionCurve;
import de.baum2dev.baum2.progression.VitalsCurve;

/**
 * Client-side networking handler for Baum2 custom packets.
 * Registers payload receivers and processes incoming data.
 * All handlers execute on the render thread (thread-safe for client-side operations).
 */
@Environment(EnvType.CLIENT)
public class ClientNetworkingHandler {
    /**
     * Register all client-side packet receivers.
     * Must be called from Baum2Client.onInitializeClient().
     */
    private static volatile int currentMana = 0;
    private static volatile int currentMaxMana = 1;
    private static volatile int currentEndurance = VitalsCurve.STARTING_ATTRIBUTE_POINTS;
    private static volatile int currentIntelligence = VitalsCurve.STARTING_ATTRIBUTE_POINTS;
    private static volatile int currentStrength = VitalsCurve.STARTING_ATTRIBUTE_POINTS;
    private static volatile int currentDexterity = VitalsCurve.STARTING_ATTRIBUTE_POINTS;
    private static volatile int currentUnspentAttributePoints = 0;

    public static void registerClientHandlers() {
        // Register receiver for experience sync payload
        ClientPlayNetworking.registerGlobalReceiver(
                ExperienceSyncPayload.TYPE,
                ClientNetworkingHandler::handleExperienceSyncPayload
        );

        // Register receiver for mana sync payload
        ClientPlayNetworking.registerGlobalReceiver(
                ManaSyncPayload.TYPE,
                ClientNetworkingHandler::handleManaSyncPayload
        );

        // Register receiver for attribute sync payload
        ClientPlayNetworking.registerGlobalReceiver(
                AttributeSyncPayload.TYPE,
                ClientNetworkingHandler::handleAttributeSyncPayload
        );
    }

    private static void handleManaSyncPayload(ManaSyncPayload payload, ClientPlayNetworking.Context context) {
        currentMana = payload.mana();
        currentMaxMana = payload.maxMana();
    }

    private static void handleAttributeSyncPayload(AttributeSyncPayload payload, ClientPlayNetworking.Context context) {
        currentEndurance = payload.endurance();
        currentIntelligence = payload.intelligence();
        currentStrength = payload.strength();
        currentDexterity = payload.dexterity();
        currentUnspentAttributePoints = payload.unspentAttributePoints();
    }

    public static int getCurrentMana() {
        return currentMana;
    }

    public static int getCurrentMaxMana() {
        return currentMaxMana;
    }

    public static int getCurrentEndurance() {
        return currentEndurance;
    }

    public static int getCurrentIntelligence() {
        return currentIntelligence;
    }

    public static int getCurrentStrength() {
        return currentStrength;
    }

    public static int getCurrentDexterity() {
        return currentDexterity;
    }

    public static int getCurrentUnspentAttributePoints() {
        return currentUnspentAttributePoints;
    }

    /**
     * Handle incoming experience sync payload.
     * Executed on the render thread - thread-safe for all client operations.
     *
     * @param payload the received payload containing level, experience, and maxExperience
     * @param context the client networking context with client and player access
     */
    private static void handleExperienceSyncPayload(ExperienceSyncPayload payload, ClientPlayNetworking.Context context) {
        MinecraftClient client = context.client();
        ClientPlayerEntity player = client.player;

        if (player == null) {
            return;
        }

        // ClientPlayerEntity has no setExperienceLevel(int) — that only exists on
        // ServerPlayerEntity. The client-side equivalent is setExperience(progress, total, level),
        // the exact method vanilla itself calls when it receives an experience sync packet.
        // It takes the fill percentage directly, so no curve-reversal is needed here, and it
        // also triggers the bar's fill-flash animation like vanilla level-ups do.
        float progress = payload.maxExperience() > 0
                ? (float) payload.experience() / payload.maxExperience()
                : 0f;
        int totalExperience = (int) (ProgressionCurve.getTotalXpForLevel(payload.level()) + payload.experience());

        player.setExperience(progress, totalExperience, payload.level());
    }
}
