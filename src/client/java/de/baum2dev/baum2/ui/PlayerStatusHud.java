package de.baum2dev.baum2.ui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import de.baum2dev.baum2.classes.ClassManager;
import de.baum2dev.baum2.classes.PlayerClass;

/**
 * "Player Status Overlay" — compact HUD showing selected class, level, and a slim
 * XP-to-next-level indicator. Layout/colors per docs/visual-style-guide.md section 7.
 *
 * Supplements (never replaces) vanilla's own hotbar XP bar — deliberately smaller, a
 * different color (cyan vs. vanilla green), and shows no percentage text, so it reads as a
 * distinct secondary indicator rather than a redundant copy.
 */
public final class PlayerStatusHud {
    private static final int PANEL_X = 6;
    private static final int PANEL_Y = 6;
    private static final int PANEL_WIDTH = 118;
    private static final int PANEL_HEIGHT = 30;

    private static final int BORDER_OUTER = 0xFF0B0E0D;
    private static final int BORDER_INNER = 0xFF4E6B5C;
    private static final int BACKGROUND = 0xE6161B19;
    private static final int XP_TRACK = 0xFF1F2622;
    private static final int XP_FILL = 0xFF7FD8E0;
    private static final int LEVEL_TEXT = 0xFFD9B36C;

    public static void render(DrawContext context, RenderTickCounter tickCounter) {
        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayerEntity player = client.player;

        if (player == null || client.options.hudHidden) {
            return;
        }

        PlayerClass playerClass = player.getAttached(ClassManager.SELECTED_CLASS);
        if (playerClass == null) {
            return;
        }

        drawPanel(context);

        int contentX = PANEL_X + 4;
        int contentY = PANEL_Y + 4;
        int contentWidth = PANEL_WIDTH - 8;

        drawStatusRow(context, client, playerClass, player, contentX, contentY, contentWidth);
        drawXpBar(context, player, contentX, contentY + 18, contentWidth);
    }

    private static void drawPanel(DrawContext context) {
        context.fill(PANEL_X, PANEL_Y, PANEL_X + PANEL_WIDTH, PANEL_Y + PANEL_HEIGHT, BORDER_OUTER);
        context.fill(PANEL_X + 1, PANEL_Y + 1, PANEL_X + PANEL_WIDTH - 1, PANEL_Y + PANEL_HEIGHT - 1, BORDER_INNER);
        context.fill(PANEL_X + 2, PANEL_Y + 2, PANEL_X + PANEL_WIDTH - 2, PANEL_Y + PANEL_HEIGHT - 2, BACKGROUND);
    }

    private static void drawStatusRow(
        DrawContext context,
        MinecraftClient client,
        PlayerClass playerClass,
        ClientPlayerEntity player,
        int contentX,
        int contentY,
        int contentWidth
    ) {
        Identifier icon = ClassIcons.of(playerClass);
        context.drawTexture(
            RenderPipelines.GUI_TEXTURED,
            icon,
            contentX, contentY,
            0.0F, 0.0F,
            16, 16,
            16, 16
        );

        Text nameText = Text.literal(ClassIcons.displayName(playerClass)).formatted(Formatting.BOLD);
        context.drawText(client.textRenderer, nameText, contentX + 20, contentY + 4, ClassIcons.accentColor(playerClass), true);

        Text levelText = Text.literal("Lv. " + player.experienceLevel);
        int levelWidth = client.textRenderer.getWidth(levelText);
        context.drawText(client.textRenderer, levelText, contentX + contentWidth - levelWidth, contentY + 4, LEVEL_TEXT, true);
    }

    private static void drawXpBar(DrawContext context, ClientPlayerEntity player, int x, int y, int width) {
        context.fill(x, y, x + width, y + 3, XP_TRACK);
        int fillWidth = (int) (width * player.experienceProgress);
        if (fillWidth > 0) {
            context.fill(x, y, x + fillWidth, y + 3, XP_FILL);
        }
    }

    private PlayerStatusHud() {
    }
}
