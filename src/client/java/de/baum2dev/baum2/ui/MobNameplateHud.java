package de.baum2dev.baum2.ui;

import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.MathHelper;

/**
 * Shows the name and current/max health of whatever living entity the player is currently
 * looking at (MinecraftClient.crosshairTarget), top-center - a target nameplate, not tied to
 * VanillaHudElements.BOSS_BAR. Positioned near vanilla's own boss-bar start (y=12) since the
 * two rarely coexist; not dynamically avoided if they do (see docs/fabric-modding.md).
 */
public class MobNameplateHud {
    private static final Identifier ID = Identifier.of("baum2", "mob_nameplate");

    private static final int NAME_Y = 12;
    private static final int BAR_Y = NAME_Y + 10;
    private static final int BAR_WIDTH = 120;
    private static final int BAR_HEIGHT = 7;

    private static final int BORDER_COLOR = 0xFF170A0B;
    private static final int TRACK_COLOR = 0xFF2B1416;
    private static final int FILL_TOP_COLOR = 0xFFE2574B;
    private static final int FILL_BOTTOM_COLOR = 0xFF8E1F1F;

    public static void register() {
        HudElementRegistry.addLast(ID, MobNameplateHud::render);
    }

    private static void render(DrawContext context, RenderTickCounter tickCounter) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.options.hudHidden) {
            return;
        }

        LivingEntity target = getTargetedLivingEntity(client);
        if (target == null) {
            return;
        }

        int centerX = context.getScaledWindowWidth() / 2;
        int barX = centerX - BAR_WIDTH / 2;

        context.drawCenteredTextWithShadow(client.textRenderer, target.getDisplayName(), centerX, NAME_Y, 0xFFFFFF);

        float maxHealth = target.getMaxHealth();
        float ratio = maxHealth > 0 ? MathHelper.clamp(target.getHealth() / maxHealth, 0f, 1f) : 0f;

        context.fill(barX, BAR_Y, barX + BAR_WIDTH, BAR_Y + BAR_HEIGHT, BORDER_COLOR);
        int innerX = barX + 1;
        int innerY = BAR_Y + 1;
        int innerWidth = BAR_WIDTH - 2;
        int innerHeight = BAR_HEIGHT - 2;
        context.fill(innerX, innerY, innerX + innerWidth, innerY + innerHeight, TRACK_COLOR);

        int fillWidth = Math.round(innerWidth * ratio);
        if (fillWidth > 0) {
            int topBandHeight = innerHeight / 2;
            context.fill(innerX, innerY, innerX + fillWidth, innerY + topBandHeight, FILL_TOP_COLOR);
            context.fill(innerX, innerY + topBandHeight, innerX + fillWidth, innerY + innerHeight, FILL_BOTTOM_COLOR);
        }

        Text healthText = Text.literal(Math.round(target.getHealth()) + " / " + Math.round(maxHealth));
        context.drawCenteredTextWithShadow(client.textRenderer, healthText, centerX, BAR_Y + BAR_HEIGHT + 2, 0xFFFFFF);
    }

    private static LivingEntity getTargetedLivingEntity(MinecraftClient client) {
        HitResult hit = client.crosshairTarget;
        if (!(hit instanceof EntityHitResult entityHit)) {
            return null;
        }
        Entity entity = entityHit.getEntity();
        if (!(entity instanceof LivingEntity living) || living == client.player || !living.isAlive()) {
            return null;
        }
        return living;
    }
}
