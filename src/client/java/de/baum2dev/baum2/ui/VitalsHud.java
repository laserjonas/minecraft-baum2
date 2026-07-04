package de.baum2dev.baum2.ui;

import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudStatusBarHeightRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import de.baum2dev.baum2.networking.ClientNetworkingHandler;

/**
 * Replaces the vanilla heart/health bar with a custom Life bar, and adds a new Mana bar
 * above it, in the same left-side status-bar column vanilla hearts used to occupy.
 * Colors/layout per docs/visual-style-guide.md.
 */
public class VitalsHud {
    private static final Identifier MANA_ID = Identifier.of("baum2", "mana_bar");

    private static final int SHELL_WIDTH = 83;
    private static final int SHELL_HEIGHT = 8;
    private static final int SHELL_GAP = 1;

    private static final int LIFE_BORDER = 0xFF170A0B;
    private static final int LIFE_TRACK = 0xFF2B1416;
    private static final int LIFE_FILL_TOP = 0xFFE2574B;
    private static final int LIFE_FILL_BOTTOM = 0xFF8E1F1F;

    private static final int MANA_BORDER = 0xFF0A1220;
    private static final int MANA_TRACK = 0xFF141E33;
    private static final int MANA_FILL_TOP = 0xFF5E9BE0;
    private static final int MANA_FILL_BOTTOM = 0xFF1F3F8A;

    public static void register() {
        // replaceElement (not removeElement) keeps the vanilla "health_bar" id registered -
        // the framework validates every id with a height provider still has a matching
        // element, and vanilla's own HEALTH_BAR height provider entry doesn't go away just
        // because we stop drawing hearts. We override that height provider too, since
        // vanilla's is a dynamic multi-row heart calculation that no longer applies here.
        HudElementRegistry.replaceElement(VanillaHudElements.HEALTH_BAR, old -> VitalsHud::renderLife);
        HudStatusBarHeightRegistry.addLeft(VanillaHudElements.HEALTH_BAR, player -> SHELL_HEIGHT);

        HudElementRegistry.attachElementBefore(VanillaHudElements.ARMOR_BAR, MANA_ID, VitalsHud::renderMana);
        HudStatusBarHeightRegistry.addLeft(MANA_ID, player -> SHELL_HEIGHT + SHELL_GAP);
    }

    private static void renderLife(DrawContext context, RenderTickCounter tickCounter) {
        ClientPlayerEntity player = activePlayer();
        if (player == null) {
            return;
        }

        int x = context.getScaledWindowWidth() / 2 - 91;
        int y = context.getScaledWindowHeight() - 39;

        float maxHealth = player.getMaxHealth();
        float ratio = maxHealth > 0 ? player.getHealth() / maxHealth : 0f;
        drawBar(context, x, y, ratio, LIFE_BORDER, LIFE_TRACK, LIFE_FILL_TOP, LIFE_FILL_BOTTOM);
    }

    private static void renderMana(DrawContext context, RenderTickCounter tickCounter) {
        ClientPlayerEntity player = activePlayer();
        if (player == null) {
            return;
        }

        int x = context.getScaledWindowWidth() / 2 - 91;
        int y = context.getScaledWindowHeight() - 39 - (SHELL_HEIGHT + SHELL_GAP);

        int maxMana = ClientNetworkingHandler.getCurrentMaxMana();
        float ratio = maxMana > 0 ? (float) ClientNetworkingHandler.getCurrentMana() / maxMana : 0f;
        drawBar(context, x, y, ratio, MANA_BORDER, MANA_TRACK, MANA_FILL_TOP, MANA_FILL_BOTTOM);
    }

    private static ClientPlayerEntity activePlayer() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.options.hudHidden) {
            return null;
        }
        return client.player;
    }

    private static void drawBar(DrawContext context, int x, int y, float ratio, int borderColor, int trackColor, int fillTopColor, int fillBottomColor) {
        ratio = MathHelper.clamp(ratio, 0f, 1f);

        context.fill(x, y, x + SHELL_WIDTH, y + SHELL_HEIGHT, borderColor);

        int interiorX = x + 1;
        int interiorY = y + 1;
        int interiorWidth = SHELL_WIDTH - 2;
        int interiorHeight = SHELL_HEIGHT - 2;
        context.fill(interiorX, interiorY, interiorX + interiorWidth, interiorY + interiorHeight, trackColor);

        int fillWidth = Math.round(interiorWidth * ratio);
        if (fillWidth > 0) {
            int topBandHeight = interiorHeight / 2;
            context.fill(interiorX, interiorY, interiorX + fillWidth, interiorY + topBandHeight, fillTopColor);
            context.fill(interiorX, interiorY + topBandHeight, interiorX + fillWidth, interiorY + interiorHeight, fillBottomColor);
        }
    }
}
