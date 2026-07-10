package de.baum2dev.baum2.mounts;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.resource.featuretoggle.FeatureFlags;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.screen.SimpleNamedScreenHandlerFactory;
import net.minecraft.screen.slot.Slot;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import de.baum2dev.baum2.items.HorseFluteItem;

/**
 * The "equipment inventory": a small HandledScreen container whose single slot holds the
 * player's equipped horse flute. More equipment slots will join it later (per the design
 * brief), which is why this is its own screen rather than a slot grafted onto vanilla's
 * player inventory.
 *
 * The slot's contents live in MountEquipmentManager's persistent attachment, NOT in this
 * handler - the server-side backing inventory below write-through-saves on every change
 * (markDirty fires on any Slot.setStack), so the equipped flute is already persisted the
 * moment it's dropped into the slot, not only on screen close. Client side constructs a
 * plain dummy inventory; vanilla's ScreenHandler slot sync fills it.
 */
public class MountEquipmentScreenHandler extends ScreenHandler {

    public static final ScreenHandlerType<MountEquipmentScreenHandler> TYPE = Registry.register(
            Registries.SCREEN_HANDLER,
            Identifier.of("baum2", "mount_equipment"),
            new ScreenHandlerType<>(MountEquipmentScreenHandler::new, FeatureFlags.VANILLA_FEATURES));

    private static final int FLUTE_SLOT = 0;
    private static final int PLAYER_INVENTORY_START = 1;   // 27 main slots
    private static final int PLAYER_HOTBAR_START = 28;     // 9 hotbar slots
    private static final int SLOT_COUNT = 37;

    private final Inventory equipmentInventory;

    /** Client-side constructor (registered as the ScreenHandlerType factory). */
    public MountEquipmentScreenHandler(int syncId, PlayerInventory playerInventory) {
        this(syncId, playerInventory, new SimpleInventory(1));
    }

    /** Server-side constructor - pass the attachment-backed inventory. */
    public MountEquipmentScreenHandler(int syncId, PlayerInventory playerInventory, Inventory equipmentInventory) {
        super(TYPE, syncId);
        this.equipmentInventory = equipmentInventory;

        // The mount slot, top center - accepts only a horse flute.
        this.addSlot(new Slot(equipmentInventory, 0, 80, 24) {
            @Override
            public boolean canInsert(ItemStack stack) {
                return stack.getItem() instanceof HorseFluteItem;
            }
        });

        // Standard player inventory + hotbar layout (same coordinates every vanilla
        // chest-style container uses, shifted for this screen's 140px-tall top section).
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 58 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInventory, col, 8 + col * 18, 116));
        }
    }

    /** Opens the equipment screen for a player, its slot backed by the persistent attachment. */
    public static void open(ServerPlayerEntity player) {
        player.openHandledScreen(new SimpleNamedScreenHandlerFactory(
                (syncId, inv, p) -> new MountEquipmentScreenHandler(
                        syncId, inv, createBackedInventory((ServerPlayerEntity) p)),
                Text.translatable("container.baum2.equipment")));
    }

    /** A 1-slot inventory that write-through-persists to the player's mount-flute attachment. */
    private static Inventory createBackedInventory(ServerPlayerEntity player) {
        SimpleInventory inventory = new SimpleInventory(1) {
            @Override
            public void markDirty() {
                super.markDirty();
                MountEquipmentManager.setFlute(player, this.getStack(0));
            }
        };
        inventory.setStack(0, MountEquipmentManager.getFlute(player).copy());
        return inventory;
    }

    @Override
    public ItemStack quickMove(PlayerEntity player, int slotIndex) {
        Slot slot = this.slots.get(slotIndex);
        if (!slot.hasStack()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getStack();
        ItemStack original = stack.copy();

        if (slotIndex == FLUTE_SLOT) {
            // Flute slot -> anywhere in the player inventory.
            if (!this.insertItem(stack, PLAYER_INVENTORY_START, SLOT_COUNT, true)) {
                return ItemStack.EMPTY;
            }
        } else {
            // Player inventory -> flute slot only (flutes only; anything else has no target here).
            if (!(stack.getItem() instanceof HorseFluteItem)
                    || !this.insertItem(stack, FLUTE_SLOT, FLUTE_SLOT + 1, false)) {
                return ItemStack.EMPTY;
            }
        }

        if (stack.isEmpty()) {
            slot.setStack(ItemStack.EMPTY);
        } else {
            slot.markDirty();
        }
        return original;
    }

    @Override
    public boolean canUse(PlayerEntity player) {
        return true;
    }

    /** No-op - forces this class (and its ScreenHandlerType registration) to load at mod init. */
    public static void bootstrap() {
    }
}
