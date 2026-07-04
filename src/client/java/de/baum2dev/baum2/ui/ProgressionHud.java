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
        int xPos = 10;
        int yPos = 10;
        int boxWidth = 200;
        int boxHeight = 60;

        int backgroundColor = 0xDD1A1A1A;
        int borderColor = 0xFFFFAA00;

        drawContext.fill(xPos, yPos, xPos + boxWidth, yPos + boxHeight, backgroundColor);
        drawContext.fill(xPos, yPos, xPos + boxWidth, yPos + 2, borderColor);
        drawContext.fill(xPos, yPos + boxHeight - 2, xPos + boxWidth, yPos + boxHeight, borderColor);
        drawContext.fill(xPos, yPos, xPos + 2, yPos + boxHeight, borderColor);
        drawContext.fill(xPos + boxWidth - 2, yPos, xPos + boxWidth, yPos + boxHeight, borderColor);

        int textX = xPos + 8;
        int textY = yPos + 5;

        drawContext.drawText(textRenderer, Text.literal("§6BAUM2 PROGRESSION"), textX, textY, 0xFFFFFF, true);

        textY += 12;
        drawContext.drawText(textRenderer, Text.literal("§fLevel: 1 | XP: 0/100"), textX, textY, 0xFFFFFF, true);

        textY += 12;
        int barWidth = boxWidth - 16;
        int barHeight = 12;
        int barX = xPos + 8;
        int barY = textY;

        drawContext.fill(barX, barY, barX + barWidth, barY + barHeight, 0xFF333333);
        drawContext.fill(barX + 1, barY + 1, barX + (int)(barWidth * 0.5f) - 1, barY + barHeight - 1, 0xFF00FF00);
        drawContext.fill(barX, barY, barX + barWidth, barY + 1, 0xFFFFFFFF);
        drawContext.fill(barX, barY + barHeight - 1, barX + barWidth, barY + barHeight, 0xFFFFFFFF);
    }
}
