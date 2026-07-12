package de.baum2dev.baum2.items;

import net.minecraft.item.Item;

/**
 * The Baum Credits wallet - the display item BaumCreditsManager keeps pinned in every
 * player's inventory. Pure marker class: the balance lives in the player Attachment, the
 * stack only shows it (via ITEM_NAME). Undroppable so the "currency slot" can't be thrown
 * away by accident; a fresh wallet reappears after death/respawn either way.
 */
public class BaumCreditsItem extends Item implements UndroppableItem {

    public BaumCreditsItem(Settings settings) {
        super(settings);
    }

    @Override
    public String boundMessage() {
        return "Your Baum Credits are bound to you and cannot be dropped.";
    }
}
