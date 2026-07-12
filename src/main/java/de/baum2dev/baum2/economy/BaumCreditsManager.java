package de.baum2dev.baum2.economy;

import com.mojang.serialization.Codec;

import de.baum2dev.baum2.registry.ModItems;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

/**
 * The mod's currency: "Baum Credits". The balance itself is a persistent player Attachment
 * (same pattern as PlayerLevelSystem's progression data - survives restarts, disconnects,
 * and death). The wallet ItemStack in the player's inventory is DISPLAY ONLY: it permanently
 * occupies one inventory slot and shows the balance in its item name, but the stack carries
 * no value of its own - losing or duplicating the stack can never lose or duplicate money.
 *
 * <p>Wallet slot upkeep runs on join, after respawn (death drops the wallet like any item -
 * see UndroppableItem's javadoc for why the death-drop path is not cancelled), and on a
 * once-per-second server tick pass that also removes any extra wallet copies a player picked
 * up off the ground.
 */
public class BaumCreditsManager {

    private static final AttachmentType<Long> CREDITS = AttachmentRegistry.create(
            Identifier.of("baum2", "baum_credits"),
            builder -> builder
                    .persistent(Codec.LONG)
                    .copyOnDeath()
                    .initializer(() -> 0L)
    );

    private static final int WALLET_UPKEEP_INTERVAL_TICKS = 20;

    /**
     * Forces this class (and the CREDITS attachment registration) to load during mod init,
     * before any player can join - same deserialization gotcha as PlayerLevelSystem.bootstrap().
     */
    public static void bootstrap() {
    }

    public static void registerEvents() {
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) ->
                ensureWallet(handler.getPlayer()));
        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) ->
                ensureWallet(newPlayer));
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (server.getTicks() % WALLET_UPKEEP_INTERVAL_TICKS != 0) {
                return;
            }
            for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                ensureWallet(player);
            }
        });
    }

    public static long getCredits(ServerPlayerEntity player) {
        return player.getAttachedOrCreate(CREDITS);
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
        ensureWallet(player);
    }

    /**
     * Keeps the inventory's wallet stack in shape: exactly one wallet (extras removed, one
     * inserted if missing - retried by the tick pass if the inventory happened to be full),
     * with its displayed name always matching the current balance.
     */
    private static void ensureWallet(ServerPlayerEntity player) {
        ItemStack wallet = ItemStack.EMPTY;
        for (int slot = 0; slot < player.getInventory().size(); slot++) {
            ItemStack stack = player.getInventory().getStack(slot);
            if (!stack.isOf(ModItems.BAUM_CREDITS)) {
                continue;
            }
            if (wallet.isEmpty()) {
                wallet = stack;
            } else {
                player.getInventory().removeStack(slot);
            }
        }
        if (wallet.isEmpty()) {
            wallet = new ItemStack(ModItems.BAUM_CREDITS);
            if (!player.getInventory().insertStack(wallet)) {
                return;  // inventory full - the once-per-second upkeep pass retries
            }
        }
        wallet.set(DataComponentTypes.ITEM_NAME,
                Text.literal("Baum Credits: " + getCredits(player)));
    }
}
