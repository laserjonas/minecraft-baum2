package de.baum2dev.baum2.ui;

import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Colors;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.MathHelper;

/**
 * Shows the name, level, and current/max health of the entity the player most recently
 * attacked (held for a few seconds), falling back to whatever living entity the crosshair is
 * currently over. Attack-triggered rather than pure live-crosshair-only: fast/erratic mobs
 * (e.g. spiders climbing/jumping) often aren't precisely under the crosshair by the time a
 * render() call samples it, even though the hit landed - see docs/fabric-modding.md's
 * "Target nameplate" section for the confirmed AttackEntityCallback wiring.
 */
public class MobNameplateHud {
    private static final Identifier ID = Identifier.of("baum2", "mob_nameplate");
    private static final long DISPLAY_DURATION_MILLIS = 5000;

    private static final int NAME_Y = 12;
    private static final int BAR_Y = NAME_Y + 10;
    private static final int BAR_WIDTH = 120;
    private static final int BAR_HEIGHT = 7;

    private static final int BORDER_COLOR = 0xFF170A0B;
    private static final int TRACK_COLOR = 0xFF2B1416;
    private static final int FILL_TOP_COLOR = 0xFFE2574B;
    private static final int FILL_BOTTOM_COLOR = 0xFF8E1F1F;

    private static LivingEntity lastAttackedEntity;
    private static long lastAttackedExpiryMillis;

    public static void register() {
        HudElementRegistry.addLast(ID, MobNameplateHud::render);

        // Fires once client-side per attack (also fires server-side in singleplayer - the
        // world.isClient() guard keeps only the client-side occurrence) - the actual moment
        // an attack lands, not a render-frame crosshair sample. See docs/fabric-modding.md.
        AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (world.isClient() && entity instanceof LivingEntity living) {
                lastAttackedEntity = living;
                lastAttackedExpiryMillis = System.currentTimeMillis() + DISPLAY_DURATION_MILLIS;
            }
            return ActionResult.PASS;
        });
    }

    private static void render(DrawContext context, RenderTickCounter tickCounter) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.options.hudHidden) {
            return;
        }

        LivingEntity target = resolveTarget(client);
        if (target == null) {
            return;
        }

        int centerX = context.getScaledWindowWidth() / 2;
        int barX = centerX - BAR_WIDTH / 2;

        Text nameWithLevel = target.getDisplayName().copy()
                .append(Text.literal(" " + getMonsterLevelText(target)));
        context.drawCenteredTextWithShadow(client.textRenderer, nameWithLevel, centerX, NAME_Y, Colors.WHITE);

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
        context.drawCenteredTextWithShadow(client.textRenderer, healthText, centerX, BAR_Y + BAR_HEIGHT + 2, Colors.WHITE);
    }

    /** Prefers a recently-attacked entity over the live crosshair target - see class javadoc. */
    private static LivingEntity resolveTarget(MinecraftClient client) {
        if (lastAttackedEntity != null && lastAttackedEntity.isAlive()
                && System.currentTimeMillis() < lastAttackedExpiryMillis) {
            return lastAttackedEntity;
        }
        return getCrosshairLivingEntity(client);
    }

    private static LivingEntity getCrosshairLivingEntity(MinecraftClient client) {
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

    /**
     * No mob-leveling system exists yet - every monster defaults to "Lvl. 1" until one does.
     * Single point to extend once real monster levels exist.
     */
    private static String getMonsterLevelText(LivingEntity entity) {
        int level = 1;
        return "Lvl. " + level;
    }
}
