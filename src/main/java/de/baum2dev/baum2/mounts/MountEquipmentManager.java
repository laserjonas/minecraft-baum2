package de.baum2dev.baum2.mounts;

import java.util.Optional;

import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import de.baum2dev.baum2.items.HorseFluteItem;

/**
 * Owns the persistent "mount slot" of the equipment inventory: the one horse flute a player
 * has equipped. Same persistent-Attachment pattern as ClassManager.SELECTED_CLASS; not
 * synced (the server decides everything mount-related, and the slot's contents reach the
 * client through the ScreenHandler when the equipment screen is open).
 *
 * The stack lives here rather than in a Screen/ScreenHandler so it survives the screen
 * closing, relogs, restarts, and (copyOnDeath) death - MountEquipmentScreenHandler is just a
 * windowed view onto this attachment.
 */
public final class MountEquipmentManager {

    public static final AttachmentType<ItemStack> MOUNT_FLUTE = AttachmentRegistry.create(
        Identifier.of("baum2", "mount_flute"),
        builder -> builder
            .persistent(ItemStack.OPTIONAL_CODEC)
            .copyOnDeath()
    );

    public static ItemStack getFlute(ServerPlayerEntity player) {
        ItemStack stack = player.getAttached(MOUNT_FLUTE);
        return stack == null ? ItemStack.EMPTY : stack;
    }

    public static void setFlute(ServerPlayerEntity player, ItemStack stack) {
        if (stack.isEmpty()) {
            player.removeAttached(MOUNT_FLUTE);
        } else {
            player.setAttached(MOUNT_FLUTE, stack);
        }
    }

    /** The tier of the equipped flute, if a flute is equipped at all. */
    public static Optional<MountTier> equippedTier(ServerPlayerEntity player) {
        if (getFlute(player).getItem() instanceof HorseFluteItem flute) {
            return Optional.of(flute.tier());
        }
        return Optional.empty();
    }

    /** No-op - forces this class (and its AttachmentType registration) to load at mod init,
     *  before any player join can deserialize saved data (same gotcha as PlayerLevelSystem). */
    public static void bootstrap() {
    }

    private MountEquipmentManager() {
    }
}
