package de.baum2dev.baum2.items;

/**
 * Marker interface for items a player must not be able to throw away manually:
 * {@code mixin/ServerPlayerEntityDropGuardMixin} cancels the hotbar Q/Ctrl+Q drop and the
 * inventory-screen throw for any item implementing this. Deliberately NOT death-proof -
 * dying still drops the item like vanilla (cancelling the death-drop path would delete the
 * stack outright, since the inventory is already being cleared at that point).
 */
public interface UndroppableItem {

    /** Overlay (actionbar) message shown when a drop attempt is blocked. */
    String BOUND_MESSAGE = "This blade is bound to you and cannot be dropped.";
}
