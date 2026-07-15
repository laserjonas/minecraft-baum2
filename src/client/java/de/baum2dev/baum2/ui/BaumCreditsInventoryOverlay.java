package de.baum2dev.baum2.ui;

import de.baum2dev.baum2.economy.BaumCreditsManager;
import de.baum2dev.baum2.mixin.client.HandledScreenAccessor;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.util.Colors;
import net.minecraft.util.Identifier;

/**
 * Draws the player's Baum Credits balance on the survival inventory screen as
 * "Baum Credits: N" followed by the coin icon - text information + icon, per user decision
 * (an earlier version was a wallet ITEM occupying a slot; rejected). Anchored just above the
 * inventory panel's top-left corner, read fresh each frame via {@link HandledScreenAccessor}
 * so the recipe book's panel shift is followed automatically.
 *
 * <p>The balance comes straight off the synced player Attachment
 * ({@link BaumCreditsManager#getDisplayCredits}) - no payload, no client cache.
 * Note the documented drawText gotcha: raw color ints need a non-zero alpha byte, hence
 * {@link Colors#WHITE}.
 */
public final class BaumCreditsInventoryOverlay {

    private static final Identifier COIN_TEXTURE =
            Identifier.of("baum2", "textures/gui/baum_credits.png");
    /** Drawn size; the source PNG is 16x16, scaled down to sit next to the 8px text line. */
    private static final int ICON_SIZE = 12;
    private static final int TEXT_ICON_GAP = 3;

    public static void register() {
        ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
            if (!(screen instanceof InventoryScreen inventoryScreen)) {
                return;
            }
            ScreenEvents.afterRender(screen).register((screen1, context, mouseX, mouseY, tickDelta) -> {
                if (client.player == null) {
                    return;
                }
                HandledScreenAccessor panel = (HandledScreenAccessor) inventoryScreen;
                String text = "Baum Credits: " + BaumCreditsManager.getDisplayCredits(client.player);
                int textX = panel.baum2$getPanelX() + 1;
                int textY = panel.baum2$getPanelY() - 12;
                context.drawText(client.textRenderer, text, textX, textY, Colors.WHITE, true);
                int iconX = textX + client.textRenderer.getWidth(text) + TEXT_ICON_GAP;
                int iconY = textY - (ICON_SIZE - 8) / 2;  // center the icon on the text line
                context.drawTexture(RenderPipelines.GUI_TEXTURED, COIN_TEXTURE,
                        iconX, iconY, 0.0F, 0.0F, ICON_SIZE, ICON_SIZE, 16, 16);
            });
        });
    }
}
