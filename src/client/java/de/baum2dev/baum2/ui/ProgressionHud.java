package de.baum2dev.baum2.ui;

import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
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

        TextRenderer textRenderer = client.textRenderer;
        int xPos = 10;
        int yPos = 10;

        drawContext.drawText(textRenderer, "§6Baum2 Progression", xPos, yPos, 0xFFFFFF, false);
        yPos += 12;

        String levelText = "Level: 1 | XP: 0/100";
        drawContext.drawText(textRenderer, levelText, xPos, yPos, 0xFFFFFF, false);
        yPos += 12;

        int barWidth = 100;
        int barHeight = 8;
        int barX = xPos;
        int barY = yPos;

        drawContext.fill(barX, barY, barX + barWidth, barY + barHeight, 0xFF333333);
        drawContext.fill(barX, barY, barX + (barWidth / 2), barY + barHeight, 0xFF00FF00);
    }
}
