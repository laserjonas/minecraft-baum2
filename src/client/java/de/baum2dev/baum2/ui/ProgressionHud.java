package de.baum2dev.baum2.ui;

import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.RenderTickCounter;

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

        int xPos = 10;
        int yPos = 10;
        int boxWidth = 220;
        int boxHeight = 40;

        int backgroundColor = 0xDD1A1A1A;
        int borderColor = 0xFFFFAA00;

        drawContext.fill(xPos, yPos, xPos + boxWidth, yPos + boxHeight, backgroundColor);
        drawContext.fill(xPos, yPos, xPos + boxWidth, yPos + 2, borderColor);
        drawContext.fill(xPos, yPos + boxHeight - 2, xPos + boxWidth, yPos + boxHeight, borderColor);
        drawContext.fill(xPos, yPos, xPos + 2, yPos + boxHeight, borderColor);
        drawContext.fill(xPos + boxWidth - 2, yPos, xPos + boxWidth, yPos + boxHeight, borderColor);

        int barStartX = xPos + 10;
        int barStartY = yPos + 10;
        int barWidth = boxWidth - 20;
        int barHeight = 20;

        drawContext.fill(barStartX, barStartY, barStartX + barWidth, barStartY + barHeight, 0xFF333333);
        drawContext.fill(barStartX + 1, barStartY + 1, barStartX + (int)(barWidth * 0.5f) - 1, barStartY + barHeight - 1, 0xFF00FF00);
        drawContext.fill(barStartX, barStartY, barStartX + barWidth, barStartY + 1, 0xFFFFFFFF);
        drawContext.fill(barStartX, barStartY + barHeight - 1, barStartX + barWidth, barStartY + barHeight, 0xFFFFFFFF);
    }
}
