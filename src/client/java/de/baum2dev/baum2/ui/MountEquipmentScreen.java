package de.baum2dev.baum2.ui;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.Text;
import de.baum2dev.baum2.mounts.MountEquipmentScreenHandler;

/**
 * Client screen for the equipment inventory (mount/flute slot). Drawn with flat fills in the
 * "Deepwood & Verdigris" chrome palette (docs/visual-style-guide.md section 1.1 rule:
 * structure/chrome -> Deepwood & Verdigris) rather than a background texture - same
 * texture-less approach as CharacterStatsScreen, whose exact panel/border hexes are reused.
 */
public class MountEquipmentScreen extends HandledScreen<MountEquipmentScreenHandler> {

    private static final int PANEL_BACKGROUND_COLOR = 0xF0161014;
    private static final int PANEL_BORDER = 0xFF33443B;
    private static final int SLOT_BACKGROUND = 0xF01F2622;
    private static final int SLOT_BORDER = 0xFF5FA98C;
    private static final int LABEL_COLOR = 0xFF9C9186;

    public MountEquipmentScreen(MountEquipmentScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
        this.backgroundWidth = 176;
        this.backgroundHeight = 140;
        // Vanilla draws the inventory label at playerInventoryTitleY; keep it above the 27er grid.
        this.playerInventoryTitleY = this.backgroundHeight - 94;
    }

    @Override
    protected void drawBackground(DrawContext context, float delta, int mouseX, int mouseY) {
        int left = this.x;
        int top = this.y;

        context.fill(left, top, left + this.backgroundWidth, top + this.backgroundHeight, PANEL_BACKGROUND_COLOR);
        drawBorder(context, left, top, this.backgroundWidth, this.backgroundHeight, PANEL_BORDER);

        // Frame every slot (the flute slot gets the accent border so the one special slot
        // reads as special; player slots get the plain chrome border).
        drawSlotFrame(context, left + 80, top + 24, SLOT_BORDER);
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                drawSlotFrame(context, left + 8 + col * 18, top + 58 + row * 18, PANEL_BORDER);
            }
        }
        for (int col = 0; col < 9; col++) {
            drawSlotFrame(context, left + 8 + col * 18, top + 116, PANEL_BORDER);
        }

        Text slotLabel = Text.literal("Mount");
        int labelWidth = this.textRenderer.getWidth(slotLabel);
        context.drawText(this.textRenderer, slotLabel, left + 89 - labelWidth / 2, top + 14, LABEL_COLOR, false);
    }

    private static void drawSlotFrame(DrawContext context, int slotX, int slotY, int borderColor) {
        context.fill(slotX - 1, slotY - 1, slotX + 17, slotY + 17, SLOT_BACKGROUND);
        drawBorder(context, slotX - 1, slotY - 1, 18, 18, borderColor);
    }

    private static void drawBorder(DrawContext context, int x, int y, int width, int height, int color) {
        context.fill(x, y, x + width, y + 1, color);
        context.fill(x, y + height - 1, x + width, y + height, color);
        context.fill(x, y, x + 1, y + height, color);
        context.fill(x + width - 1, y, x + width, y + height, color);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        this.drawMouseoverTooltip(context, mouseX, mouseY);
    }
}
