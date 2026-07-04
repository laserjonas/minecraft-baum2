package de.baum2dev.baum2.ui;

import java.util.List;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import de.baum2dev.baum2.classes.ClassDefinition;
import de.baum2dev.baum2.classes.ClassManager;
import de.baum2dev.baum2.classes.ClassRegistry;
import de.baum2dev.baum2.classes.PlayerClass;
import de.baum2dev.baum2.networking.ClassSelectPayload;

/**
 * "Klassenübersicht" — GUI-native alternative to `/baum2 class select`.
 * Layout/colors per docs/visual-style-guide.md section 8.
 */
public final class ClassScreen extends Screen {
    private static final int PANEL_WIDTH = 220;
    private static final int PANEL_HEIGHT = 238;
    private static final int CARD_WIDTH = 204;
    private static final int CARD_HEIGHT = 40;
    private static final int CARD_GAP = 6;

    private static final int BORDER_OUTER = 0xFF0B0E0D;
    private static final int BORDER_INNER = 0xFF4E6B5C;
    private static final int PANEL_BACKGROUND = 0xF5161B19;
    private static final int CARD_BACKGROUND = 0xF01F2622;
    private static final int CARD_BORDER = 0xFF33443B;
    private static final int SELECTED_BORDER = 0xFF5FA98C;
    private static final int SELECTED_WASH = 0x2E5FA98C;
    private static final int HOVER_BORDER = 0x805FA98C;

    private static final int HEADER_COLOR = 0xFFEDE6D6;
    private static final int BODY_COLOR = 0xFFB9C4BE;
    private static final int BONUS_COLOR = 0xFF7FD8E0;
    private static final int MUTED_COLOR = 0xFF5B655F;

    private final List<ClassDefinition> classes = List.copyOf(ClassRegistry.all());

    public ClassScreen() {
        super(Text.literal("Klassenübersicht"));
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
        int panelX = (width - PANEL_WIDTH) / 2;
        int panelY = (height - PANEL_HEIGHT) / 2;

        drawPanel(context, panelX, panelY);
        context.drawCenteredTextWithShadow(textRenderer, title, panelX + PANEL_WIDTH / 2, panelY + 6, HEADER_COLOR);

        PlayerClass selected = client.player != null ? client.player.getAttached(ClassManager.SELECTED_CLASS) : null;

        int cardX = panelX + 8;
        int cardY = panelY + 28;
        for (ClassDefinition definition : classes) {
            boolean isSelected = definition.playerClass() == selected;
            boolean isHovered = isMouseOver(mouseX, mouseY, cardX, cardY);
            drawCard(context, definition, cardX, cardY, isSelected, isHovered);
            cardY += CARD_HEIGHT + CARD_GAP;
        }

        Text hint = Text.literal("Klicke eine Klasse an, um sie auszuwählen.");
        context.drawCenteredTextWithShadow(textRenderer, hint, panelX + PANEL_WIDTH / 2, panelY + PANEL_HEIGHT - 18, MUTED_COLOR);

        super.render(context, mouseX, mouseY, deltaTicks);
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        int panelX = (width - PANEL_WIDTH) / 2;
        int panelY = (height - PANEL_HEIGHT) / 2;
        int cardX = panelX + 8;
        int cardY = panelY + 28;

        for (ClassDefinition definition : classes) {
            if (isMouseOver((int) click.x(), (int) click.y(), cardX, cardY)) {
                ClientPlayNetworking.send(new ClassSelectPayload(definition.playerClass()));
                return true;
            }
            cardY += CARD_HEIGHT + CARD_GAP;
        }

        return super.mouseClicked(click, doubled);
    }

    private boolean isMouseOver(int mouseX, int mouseY, int cardX, int cardY) {
        return mouseX >= cardX && mouseX < cardX + CARD_WIDTH && mouseY >= cardY && mouseY < cardY + CARD_HEIGHT;
    }

    private void drawPanel(DrawContext context, int x, int y) {
        context.fill(x, y, x + PANEL_WIDTH, y + PANEL_HEIGHT, BORDER_OUTER);
        context.fill(x + 1, y + 1, x + PANEL_WIDTH - 1, y + PANEL_HEIGHT - 1, BORDER_INNER);
        context.fill(x + 2, y + 2, x + PANEL_WIDTH - 2, y + PANEL_HEIGHT - 2, PANEL_BACKGROUND);
    }

    private void drawCard(DrawContext context, ClassDefinition definition, int x, int y, boolean isSelected, boolean isHovered) {
        int borderColor = isSelected ? SELECTED_BORDER : (isHovered ? HOVER_BORDER : CARD_BORDER);
        int borderWidth = isSelected ? 2 : 1;

        context.fill(x, y, x + CARD_WIDTH, y + CARD_HEIGHT, borderColor);
        context.fill(x + borderWidth, y + borderWidth, x + CARD_WIDTH - borderWidth, y + CARD_HEIGHT - borderWidth, CARD_BACKGROUND);
        if (isSelected) {
            context.fill(x + borderWidth, y + borderWidth, x + CARD_WIDTH - borderWidth, y + CARD_HEIGHT - borderWidth, SELECTED_WASH);
        }

        PlayerClass playerClass = definition.playerClass();
        Identifier icon = ClassIcons.of(playerClass);
        context.drawTexture(RenderPipelines.GUI_TEXTURED, icon, x + 6, y + 4, 0.0F, 0.0F, 32, 32, 16, 16);

        int textX = x + 46;
        int textWidth = CARD_WIDTH - 46 - 6;

        Text nameText = Text.literal(definition.displayName()).formatted(Formatting.BOLD);
        context.drawText(textRenderer, nameText, textX, y + 6, ClassIcons.accentColor(playerClass), true);

        String description = textRenderer.trimToWidth(definition.description(), textWidth);
        context.drawText(textRenderer, Text.literal(description), textX, y + 17, BODY_COLOR, true);

        Text bonusText = Text.literal(formatBonus(definition));
        context.drawText(textRenderer, bonusText, textX, y + 28, BONUS_COLOR, true);

        if (isSelected) {
            Text activeTag = Text.literal("Aktiv").formatted(Formatting.BOLD);
            int tagWidth = textRenderer.getWidth(activeTag);
            context.drawText(textRenderer, activeTag, x + CARD_WIDTH - 6 - tagWidth, y + 6, SELECTED_BORDER, true);
        }
    }

    private static String formatBonus(ClassDefinition definition) {
        String amount = switch (definition.bonusOperation()) {
            case ADD_VALUE -> String.format("+%.0f", definition.bonusAmount());
            case ADD_MULTIPLIED_BASE, ADD_MULTIPLIED_TOTAL -> String.format("+%.0f%%", definition.bonusAmount() * 100);
        };
        return amount + " " + attributeLabel(definition.bonusAttribute());
    }

    private static String attributeLabel(RegistryEntry<EntityAttribute> attribute) {
        String path = attribute.getKey().map(key -> key.getValue().getPath()).orElse("");
        return switch (path) {
            case "max_health" -> "Leben";
            case "movement_speed" -> "Lauftempo";
            case "luck" -> "Glück";
            case "knockback_resistance" -> "Rückstoßresistenz";
            default -> path;
        };
    }
}
