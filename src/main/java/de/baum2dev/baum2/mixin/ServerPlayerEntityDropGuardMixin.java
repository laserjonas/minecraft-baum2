package de.baum2dev.baum2.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import de.baum2dev.baum2.items.UndroppableItem;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

/**
 * Server-side drop protection for {@link UndroppableItem}s (no Fabric event exists for item
 * drops - confirmed against fabric-events-interaction; same "no event, so Mixin" situation
 * as MountKnockbackMixin). Two guarded paths:
 *
 * 1) dropSelectedItem - the hotbar Q / Ctrl+Q packet handler. Cancelled at HEAD, before the
 *    stack leaves the inventory, so there is zero item-loss risk. (The client predicts the
 *    drop locally; the next inventory sync snaps the stack back - a sub-tick visual blip.)
 *
 * 2) dropItem(ItemStack, boolean, boolean) - the funnel used by inventory-screen throws
 *    (drag-out / throw-key in a Screen). Here the stack has ALREADY been detached from the
 *    inventory/cursor, so simply cancelling would destroy it: instead it is re-inserted, and
 *    only if the re-insert succeeded is the drop cancelled (returning null, vanilla's own
 *    "nothing dropped" value). If the inventory is somehow full, the vanilla drop proceeds -
 *    losing the no-drop guarantee for one edge case beats deleting the item.
 *    Death drops (PlayerInventory.dropAll) also pass through here while the inventory is
 *    being cleared; the isDead() check lets those proceed untouched, so dying keeps exact
 *    vanilla drop behavior (see UndroppableItem's javadoc for why that is intentional).
 */
@Mixin(ServerPlayerEntity.class)
public abstract class ServerPlayerEntityDropGuardMixin {

    @Inject(method = "dropSelectedItem(Z)V", at = @At("HEAD"), cancellable = true)
    private void baum2$blockHotbarDrop(boolean entireStack, CallbackInfo ci) {
        ServerPlayerEntity self = (ServerPlayerEntity) (Object) this;
        if (self.getInventory().getSelectedStack().getItem() instanceof UndroppableItem) {
            self.sendMessage(Text.literal(UndroppableItem.BOUND_MESSAGE), true);
            ci.cancel();
        }
    }

    @Inject(
            method = "dropItem(Lnet/minecraft/item/ItemStack;ZZ)Lnet/minecraft/entity/ItemEntity;",
            at = @At("HEAD"),
            cancellable = true)
    private void baum2$blockScreenDrop(
            ItemStack stack, boolean throwRandomly, boolean retainOwnership,
            CallbackInfoReturnable<ItemEntity> cir) {
        ServerPlayerEntity self = (ServerPlayerEntity) (Object) this;
        if (!(stack.getItem() instanceof UndroppableItem) || self.isDead()) {
            return;
        }
        if (self.getInventory().insertStack(stack)) {
            self.sendMessage(Text.literal(UndroppableItem.BOUND_MESSAGE), true);
            cir.setReturnValue(null);
        }
    }
}
