package de.baum2dev.baum2.ui;

import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.text.Text;

public class ProgressionHud {

    public static void registerHud() {
        HudRenderCallback.EVENT.register(ProgressionHud::renderHud);
    }

    private static void renderHud(DrawContext drawContext, RenderTickCounter tickCounter) {
        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayerEntity player = client.player;

        if (player == null || client.options.hudHidden) {
            return;
        }

        TextRenderer textRenderer = client.textRenderer;
        int screenHeight = client.getWindow().getScaledHeight();

        int padding = 10;
        int xPos = padding;
        int yPos = screenHeight - 50;

        int boxWidth = 150;
        int boxHeight = 40;
        int backgroundColor = 0xAA000000;
        int borderColor = 0xFFFFAA00;

        drawContext.fill(xPos, yPos, xPos + boxWidth, yPos + boxHeight, backgroundColor);
        drawContext.fill(xPos, yPos, xPos + boxWidth, yPos + 1, borderColor);
        drawContext.fill(xPos, yPos + boxHeight - 1, xPos + boxWidth, yPos + boxHeight, borderColor);
        drawContext.fill(xPos, yPos, xPos + 1, yPos + boxHeight, borderColor);
        drawContext.fill(xPos + boxWidth - 1, yPos, xPos + boxWidth, yPos + boxHeight, borderColor);

        int textX = xPos + 5;
        int textY = yPos + 5;

        drawContext.drawText(textRenderer, Text.literal("§6⭐ Baum2 ⭐"), textX, textY, 0xFFFFAA, true);

        textY += 12;
        drawContext.drawText(textRenderer, Text.literal("Type: /baum2 level"), textX, textY, 0xFFFFFF, true);

        textY += 10;
        int barWidth = boxWidth - 10;
        int barHeight = 6;
        int barX = xPos + 5;
        int barY = textY;

        drawContext.fill(barX, barY, barX + barWidth, barY + barHeight, 0xFF333333);
        drawContext.fill(barX, barY, barX + (int)(barWidth * 0.5f), barY + barHeight, 0xFF00FF00);
        drawContext.fill(barX, barY, barX + barWidth, barY + 1, 0xFFFFFFFF);
    }
}
