package de.baum2dev.baum2.economy;

import com.mojang.serialization.Codec;

import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentSyncPredicate;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

/**
 * The mod's currency: "Baum Credits". The balance is a persistent player Attachment (same
 * pattern as PlayerLevelSystem's progression data - survives restarts, disconnects, and
 * death) and is synced to the owning client via {@code syncWith} (same pattern as
 * ClassManager's SELECTED_CLASS), so the inventory-screen overlay
 * (client {@code ui/BaumCreditsInventoryOverlay}) can read it with no extra payload.
 *
 * <p>Deliberately NOT an inventory item (an earlier version pinned a wallet ItemStack into a
 * slot - user-rejected): the balance renders as text + coin icon on the inventory screen.
 */
public class BaumCreditsManager {

    public static final AttachmentType<Long> CREDITS = AttachmentRegistry.create(
            Identifier.of("baum2", "baum_credits"),
            builder -> builder
                    .persistent(Codec.LONG)
                    .copyOnDeath()
                    .initializer(() -> 0L)
                    .syncWith(PacketCodecs.VAR_LONG, AttachmentSyncPredicate.targetOnly())
    );

    /**
     * Forces this class (and the CREDITS attachment registration) to load during mod init,
     * before any player can join - same deserialization gotcha as PlayerLevelSystem.bootstrap().
     */
    public static void bootstrap() {
    }

    public static long getCredits(ServerPlayerEntity player) {
        return player.getAttachedOrCreate(CREDITS);
    }

    /**
     * Client-safe read (synced value; 0 until the server first sets the attachment).
     * The server-side code paths use {@link #getCredits} instead.
     */
    public static long getDisplayCredits(PlayerEntity player) {
        Long credits = player.getAttached(CREDITS);
        return credits != null ? credits : 0L;
    }

    public static void addCredits(ServerPlayerEntity player, long amount) {
        setCredits(player, getCredits(player) + amount);
    }

    /** @return true if the player could afford it (balance never goes negative). */
    public static boolean spendCredits(ServerPlayerEntity player, long amount) {
        long balance = getCredits(player);
        if (amount < 0 || balance < amount) {
            return false;
        }
        setCredits(player, balance - amount);
        return true;
    }

    private static void setCredits(ServerPlayerEntity player, long amount) {
        player.setAttached(CREDITS, Math.max(0, amount));
    }
}
