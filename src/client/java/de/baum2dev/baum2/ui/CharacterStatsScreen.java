package de.baum2dev.baum2.ui;

import java.util.List;
import java.util.function.Consumer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.ScreenRect;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.screen.narration.NarrationPart;
import net.minecraft.client.gui.tab.GridScreenTab;
import net.minecraft.client.gui.tab.TabManager;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.widget.ScrollableLayoutWidget;
import net.minecraft.client.gui.widget.SimplePositioningWidget;
import net.minecraft.client.gui.widget.TextWidget;
import net.minecraft.client.gui.widget.TabNavigationWidget;
import net.minecraft.client.input.KeyInput;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import de.baum2dev.baum2.classes.ClassDefinition;
import de.baum2dev.baum2.classes.ClassManager;
import de.baum2dev.baum2.classes.ClassRegistry;
import de.baum2dev.baum2.classes.ClassSubspec;
import de.baum2dev.baum2.classes.PlayerClass;
import de.baum2dev.baum2.classes.SubspecDefinition;
import de.baum2dev.baum2.classes.SubspecRegistry;
import de.baum2dev.baum2.networking.CastSpellPayload;
import de.baum2dev.baum2.networking.ClassSelectPayload;
import de.baum2dev.baum2.networking.ClientNetworkingHandler;
import de.baum2dev.baum2.networking.SpendAttributePointPayload;
import de.baum2dev.baum2.networking.SubspecSelectPayload;
import de.baum2dev.baum2.progression.AttributeType;
import de.baum2dev.baum2.progression.VitalsCurve;
import de.baum2dev.baum2.skills.Spell;
import de.baum2dev.baum2.skills.SpellCaster;

/**
 * Character stats screen, opened/closed by the 'C' keybinding (Baum2KeyBindings). Built on
 * vanilla's real Tab/TabManager/TabNavigationWidget system so more tabs can be added later
 * without redesigning the chrome - see docs/visual-style-guide.md's "Character Stats Screen"
 * section for the row order/format/color spec this implements.
 */
public class CharacterStatsScreen extends Screen {
    private static final int LABEL_COLOR = 0x9C9186;
    private static final int LIFE_COLOR = 0xE2574B;
    private static final int MANA_COLOR = 0x5E9BE0;
    private static final int STRENGTH_COLOR = 0xD98A3D;
    private static final int INTELLIGENCE_COLOR = 0x9B5FE0;
    private static final int DEXTERITY_COLOR = 0x4CBB7A;
    private static final int POINTS_AVAILABLE_COLOR = 0xF2C94C;
    private static final int POINTS_NONE_COLOR = 0x6B6459;
    private static final int PANEL_BACKGROUND_COLOR = 0xF0161014;

    private final TabManager tabManager = new TabManager(this::addDrawableChild, this::remove);
    private TabNavigationWidget tabNavigationWidget;
    private StatsTab statsTab;
    private ClassTab classTab;

    public CharacterStatsScreen() {
        super(Text.literal("Character Stats"));
    }

    @Override
    protected void init() {
        this.statsTab = new StatsTab(this.textRenderer);
        this.classTab = new ClassTab();
        this.tabNavigationWidget = TabNavigationWidget.builder(this.tabManager, this.width)
                .tabs(this.statsTab, this.classTab)
                .build();
        this.addDrawableChild(this.tabNavigationWidget);
        this.tabNavigationWidget.selectTab(0, false);
        this.refreshWidgetPositions();
    }

    @Override
    protected void refreshWidgetPositions() {
        if (this.tabNavigationWidget == null) {
            return;
        }
        this.tabNavigationWidget.setWidth(this.width);
        this.tabNavigationWidget.init();
        int headerBottom = this.tabNavigationWidget.getNavigationFocus().getBottom();
        this.tabManager.setTabArea(new ScreenRect(0, headerBottom, this.width, this.height - headerBottom));
    }

    @Override
    protected void renderDarkening(DrawContext context) {
        // Vanilla's own default darkening is a subtle translucent gradient meant to dim the
        // world slightly behind a screen - against a bright sky it barely reads, leaving text
        // hard to scan (see the screenshot that prompted this: sky/clouds/hotbar all clearly
        // visible through the "screen"). Vanilla's own StatsScreen solves this the same way -
        // overriding renderDarkening with its own opaque panel - rather than relying on the
        // default. A solid near-opaque fill reads as a proper full-screen menu instead of text
        // floating over gameplay.
        context.fill(0, 0, this.width, this.height, PANEL_BACKGROUND_COLOR);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
        // Do not call renderBackground() here - the framework already renders the screen
        // background before invoking render() (confirmed by a crash: calling it again here
        // double-applies the background blur, which throws "Can only blur once per frame").
        this.statsTab.refreshValues();
        this.classTab.refreshValues();
        super.render(context, mouseX, mouseY, deltaTicks);
    }

    @Override
    public boolean keyPressed(KeyInput input) {
        if (Baum2KeyBindings.OPEN_STATS_KEY.matchesKey(input)) {
            this.close();
            return true;
        }
        return this.tabNavigationWidget != null && this.tabNavigationWidget.keyPressed(input)
                ? true
                : super.keyPressed(input);
    }

    private static class StatsTab extends GridScreenTab {
        private static final int TOP_PADDING = 8;

        private final ScrollableLayoutWidget scrollable;
        private final TextWidget lifeValue;
        private final TextWidget manaValue;
        private final TextWidget unspentPointsValue;
        private final TextWidget enduranceValue;
        private final TextWidget lifeRegenValue;
        private final TextWidget intelligenceValue;
        private final TextWidget baseMagicAttackValue;
        private final TextWidget magicDefenceValue;
        private final TextWidget strengthValue;
        private final TextWidget baseAttackValue;
        private final TextWidget physicalDefenceValue;
        private final TextWidget dexterityValue;
        private final TextWidget attackSpeedValue;
        private final TextWidget castSpeedValue;
        private final TextWidget critChanceValue;
        private final ButtonWidget enduranceButton;
        private final ButtonWidget intelligenceButton;
        private final ButtonWidget strengthButton;
        private final ButtonWidget dexterityButton;

        StatsTab(TextRenderer textRenderer) {
            super(Text.literal("Stats"));
            this.grid.setRowSpacing(5);
            this.grid.setColumnSpacing(6);

            this.lifeValue = valueWidget(70, textRenderer);
            this.manaValue = valueWidget(70, textRenderer);
            this.unspentPointsValue = valueWidget(30, textRenderer);
            this.enduranceValue = valueWidget(30, textRenderer);
            this.lifeRegenValue = valueWidget(50, textRenderer);
            this.intelligenceValue = valueWidget(30, textRenderer);
            this.baseMagicAttackValue = valueWidget(40, textRenderer);
            this.magicDefenceValue = valueWidget(40, textRenderer);
            this.strengthValue = valueWidget(30, textRenderer);
            this.baseAttackValue = valueWidget(40, textRenderer);
            this.physicalDefenceValue = valueWidget(40, textRenderer);
            this.dexterityValue = valueWidget(30, textRenderer);
            this.attackSpeedValue = valueWidget(50, textRenderer);
            this.castSpeedValue = valueWidget(50, textRenderer);
            this.critChanceValue = valueWidget(50, textRenderer);

            this.enduranceButton = plusOneButton(AttributeType.ENDURANCE);
            this.intelligenceButton = plusOneButton(AttributeType.INTELLIGENCE);
            this.strengthButton = plusOneButton(AttributeType.STRENGTH);
            this.dexterityButton = plusOneButton(AttributeType.DEXTERITY);

            int row = 0;
            this.grid.add(label("Life", textRenderer), row, 0);
            this.grid.add(this.lifeValue, row++, 1);
            this.grid.add(label("Mana", textRenderer), row, 0);
            this.grid.add(this.manaValue, row++, 1);
            row = spacer(textRenderer, row);

            this.grid.add(label("Unspent Points", textRenderer), row, 0);
            this.grid.add(this.unspentPointsValue, row++, 1);
            row = spacer(textRenderer, row);

            this.grid.add(label("Endurance", textRenderer), row, 0);
            this.grid.add(this.enduranceValue, row, 1);
            this.grid.add(this.enduranceButton, row++, 2);
            this.grid.add(label("Life Regen", textRenderer), row, 0);
            this.grid.add(this.lifeRegenValue, row++, 1);
            row = spacer(textRenderer, row);

            this.grid.add(label("Intelligence", textRenderer), row, 0);
            this.grid.add(this.intelligenceValue, row, 1);
            this.grid.add(this.intelligenceButton, row++, 2);
            this.grid.add(label("Base Magic Attack", textRenderer), row, 0);
            this.grid.add(this.baseMagicAttackValue, row++, 1);
            this.grid.add(label("Magic Defence", textRenderer), row, 0);
            this.grid.add(this.magicDefenceValue, row++, 1);
            row = spacer(textRenderer, row);

            this.grid.add(label("Strength", textRenderer), row, 0);
            this.grid.add(this.strengthValue, row, 1);
            this.grid.add(this.strengthButton, row++, 2);
            this.grid.add(label("Base Attack", textRenderer), row, 0);
            this.grid.add(this.baseAttackValue, row++, 1);
            this.grid.add(label("Physical Defence", textRenderer), row, 0);
            this.grid.add(this.physicalDefenceValue, row++, 1);
            row = spacer(textRenderer, row);

            this.grid.add(label("Dexterity", textRenderer), row, 0);
            this.grid.add(this.dexterityValue, row, 1);
            this.grid.add(this.dexterityButton, row++, 2);
            this.grid.add(label("Attack Speed Multiplier", textRenderer), row, 0);
            this.grid.add(this.attackSpeedValue, row++, 1);
            this.grid.add(label("Cast Speed Multiplier", textRenderer), row, 0);
            this.grid.add(this.castSpeedValue, row++, 1);
            this.grid.add(label("Crit Chance", textRenderer), row, 0);
            this.grid.add(this.critChanceValue, row++, 1);

            // Wraps this.grid (unchanged above) in a scrollable viewport: at a high GUI Scale
            // or small window, ~15 rows can be taller than the available tab area, and without
            // this the bottom rows (e.g. Dexterity's derived stats) become completely
            // unreachable - a real bug caught via a real screenshot, not a style preference.
            // Scrollbar rendering, mouse wheel, and drag-to-scroll are all automatic (vanilla's
            // own ScrollableWidget/ContainerWidget machinery) - no manual DrawContext code.
            this.scrollable = new ScrollableLayoutWidget(MinecraftClient.getInstance(), this.grid, 200);
        }

        @Override
        public void forEachChild(Consumer<ClickableWidget> consumer) {
            // Registers exactly one Container widget (the scrollable viewport) instead of the
            // grid's individual rows directly - the viewport forwards input/rendering to the
            // wrapped grid's real widgets internally.
            this.scrollable.forEachChild(consumer);
        }

        @Override
        public void refreshGrid(ScreenRect tabArea) {
            // Top-aligning (relativeY = 0), not GridScreenTab's default 1/6-down centering:
            // that default assumes content shorter than the tab area, and produces a NEGATIVE
            // offset once content is taller (pushing it up into the header) - a bug caught via
            // an earlier screenshot. The ScrollableLayoutWidget below now handles genuine
            // overflow instead of letting it clip into the header or run off-screen.
            ScreenRect paddedArea = new ScreenRect(
                    tabArea.getLeft(), tabArea.getTop() + TOP_PADDING,
                    tabArea.width(), Math.max(0, tabArea.height() - TOP_PADDING)
            );
            this.scrollable.setWidth(paddedArea.width());
            this.scrollable.setHeight(paddedArea.height());
            this.scrollable.refreshPositions();
            SimplePositioningWidget.setPos(this.scrollable, paddedArea, 0.5F, 0.0F);
        }

        void refreshValues() {
            MinecraftClient client = MinecraftClient.getInstance();
            ClientPlayerEntity player = client.player;
            if (player == null) {
                return;
            }

            int life = Math.round(player.getHealth());
            int maxLife = Math.round(player.getMaxHealth());
            this.lifeValue.setMessage(colored(life + " / " + maxLife, LIFE_COLOR));

            int mana = ClientNetworkingHandler.getCurrentMana();
            int maxMana = ClientNetworkingHandler.getCurrentMaxMana();
            this.manaValue.setMessage(colored(mana + " / " + maxMana, MANA_COLOR));

            int unspentPoints = ClientNetworkingHandler.getCurrentUnspentAttributePoints();
            this.unspentPointsValue.setMessage(colored(String.valueOf(unspentPoints),
                    unspentPoints > 0 ? POINTS_AVAILABLE_COLOR : POINTS_NONE_COLOR));

            int endurance = ClientNetworkingHandler.getCurrentEndurance();
            this.enduranceValue.setMessage(colored(String.valueOf(endurance), LIFE_COLOR));
            this.lifeRegenValue.setMessage(colored(String.format("%.2f", VitalsCurve.getLifeRegen(endurance)), LIFE_COLOR));

            int intelligence = ClientNetworkingHandler.getCurrentIntelligence();
            this.intelligenceValue.setMessage(colored(String.valueOf(intelligence), INTELLIGENCE_COLOR));
            this.baseMagicAttackValue.setMessage(colored(String.format("%.1f", VitalsCurve.getBaseMagicAttack(intelligence)), INTELLIGENCE_COLOR));
            this.magicDefenceValue.setMessage(colored(String.format("%.1f", VitalsCurve.getMagicDefence(intelligence)), INTELLIGENCE_COLOR));

            int strength = ClientNetworkingHandler.getCurrentStrength();
            this.strengthValue.setMessage(colored(String.valueOf(strength), STRENGTH_COLOR));
            this.baseAttackValue.setMessage(colored(String.format("%.1f", VitalsCurve.getBaseAttack(strength)), STRENGTH_COLOR));
            this.physicalDefenceValue.setMessage(colored(String.format("%.1f", VitalsCurve.getPhysicalDefence(strength)), STRENGTH_COLOR));

            int dexterity = ClientNetworkingHandler.getCurrentDexterity();
            this.dexterityValue.setMessage(colored(String.valueOf(dexterity), DEXTERITY_COLOR));
            this.attackSpeedValue.setMessage(colored(String.format("%.2fx", VitalsCurve.getAttackSpeedMultiplier(dexterity)), DEXTERITY_COLOR));
            this.castSpeedValue.setMessage(colored(String.format("%.2fx", VitalsCurve.getCastSpeedMultiplier(dexterity)), DEXTERITY_COLOR));
            this.critChanceValue.setMessage(colored(String.format("%.1f%%", VitalsCurve.getCritChance(dexterity)), DEXTERITY_COLOR));

            boolean canSpend = unspentPoints > 0;
            this.enduranceButton.visible = canSpend;
            this.intelligenceButton.visible = canSpend;
            this.strengthButton.visible = canSpend;
            this.dexterityButton.visible = canSpend;
        }

        /** Extra-height spacer row spanning all columns, per the style guide's between-family gap. */
        private int spacer(TextRenderer textRenderer, int row) {
            this.grid.add(new TextWidget(0, 5, Text.empty(), textRenderer), row, 0, 1, 3);
            return row + 1;
        }

        private static TextWidget valueWidget(int width, TextRenderer textRenderer) {
            return new TextWidget(width, 9, Text.empty(), textRenderer);
        }

        private static ButtonWidget plusOneButton(AttributeType type) {
            return ButtonWidget.builder(Text.literal("+"), button -> {
                ClientPlayNetworking.send(new SpendAttributePointPayload(type));
                // Update the displayed values immediately rather than waiting for the next
                // server tick's AttributeSyncPayload - the server remains authoritative and
                // will correct this within ~1 tick if the prediction was ever wrong (e.g. a
                // race where points ran out), but in the normal case this removes the
                // otherwise-visible round-trip delay.
                ClientNetworkingHandler.predictAttributeSpend(type);
            }).dimensions(0, 0, 12, 12).build();
        }

        private static TextWidget label(String text, TextRenderer textRenderer) {
            return new TextWidget(colored(text, LABEL_COLOR), textRenderer);
        }

        private static Text colored(String text, int color) {
            return Text.literal(text).styled(style -> style.withColor(color));
        }
    }

    /**
     * "Class" tab — moved here from the old standalone ClassScreen ('K' keybind, now removed)
     * so class selection lives alongside the rest of a player's build info. Lists all classes
     * as clickable cards (one per row in a single-column grid), then the currently selected
     * class's 2 sub-specializations (click to select) and 2 spells (click to cast, same
     * CastSpellPayload/slot the V/B keybinds use) - added in Class Overhaul v2. Layout/colors
     * follow the old ClassScreen spec in docs/visual-style-guide.md section 8, extended with
     * the same card visual language for the new sections. Wrapped in a ScrollableLayoutWidget
     * like StatsTab, for the same reason StatsTab needed it: this tab now has enough rows to
     * overflow a single screen's worth of space at high GUI Scale.
     */
    private static class ClassTab extends GridScreenTab {
        private static final int TOP_PADDING = 8;
        private static final int NO_CLASS_LABEL_COLOR = 0xFF9C9186;

        private final ScrollableLayoutWidget scrollable;
        private final List<ClassCardWidget> classCards;
        private final SubspecCardWidget subspecCard0;
        private final SubspecCardWidget subspecCard1;
        private final SpellCardWidget spellCard0;
        private final SpellCardWidget spellCard1;
        private final TextWidget noClassLabel;

        ClassTab() {
            super(Text.literal("Class"));
            this.grid.setRowSpacing(6);

            this.classCards = ClassRegistry.all().stream().map(ClassCardWidget::new).toList();
            int row = 0;
            for (ClassCardWidget card : this.classCards) {
                this.grid.add(card, row++, 0);
            }
            row = spacer(row);

            TextRenderer textRenderer = MinecraftClient.getInstance().textRenderer;
            this.grid.add(sectionHeader("Sub-specializations", textRenderer), row++, 0);
            this.subspecCard0 = new SubspecCardWidget();
            this.subspecCard1 = new SubspecCardWidget();
            this.grid.add(this.subspecCard0, row++, 0);
            this.grid.add(this.subspecCard1, row++, 0);
            row = spacer(row);

            this.grid.add(sectionHeader("Spells", textRenderer), row++, 0);
            this.spellCard0 = new SpellCardWidget(0);
            this.spellCard1 = new SpellCardWidget(1);
            this.grid.add(this.spellCard0, row++, 0);
            this.grid.add(this.spellCard1, row++, 0);

            this.noClassLabel = new TextWidget(
                Text.literal("Select a class above to unlock sub-specializations and spells.")
                    .styled(style -> style.withColor(NO_CLASS_LABEL_COLOR)),
                textRenderer
            );
            this.grid.add(this.noClassLabel, row++, 0);

            this.scrollable = new ScrollableLayoutWidget(MinecraftClient.getInstance(), this.grid, 200);
        }

        @Override
        public void forEachChild(Consumer<ClickableWidget> consumer) {
            this.scrollable.forEachChild(consumer);
        }

        @Override
        public void refreshGrid(ScreenRect tabArea) {
            ScreenRect paddedArea = new ScreenRect(
                    tabArea.getLeft(), tabArea.getTop() + TOP_PADDING,
                    tabArea.width(), Math.max(0, tabArea.height() - TOP_PADDING)
            );
            this.scrollable.setWidth(paddedArea.width());
            this.scrollable.setHeight(paddedArea.height());
            this.scrollable.refreshPositions();
            SimplePositioningWidget.setPos(this.scrollable, paddedArea, 0.5F, 0.0F);
        }

        void refreshValues() {
            ClientPlayerEntity player = MinecraftClient.getInstance().player;
            PlayerClass selectedClass = player != null ? player.getAttached(ClassManager.SELECTED_CLASS) : null;
            ClassSubspec selectedSubspec = player != null ? player.getAttached(ClassManager.SELECTED_SUBSPEC) : null;

            for (ClassCardWidget card : this.classCards) {
                card.setSelected(card.definition.playerClass() == selectedClass);
            }

            boolean hasClass = selectedClass != null;
            this.subspecCard0.visible = hasClass;
            this.subspecCard1.visible = hasClass;
            this.spellCard0.visible = hasClass;
            this.spellCard1.visible = hasClass;
            this.noClassLabel.visible = !hasClass;

            if (hasClass) {
                // SubspecRegistry.forClass always returns exactly 2 sub-specs, in a stable
                // order (backed by an EnumMap, iterating in ClassSubspec's own declaration
                // order) - safe to index directly rather than defensively checking size.
                List<SubspecDefinition> subspecs = SubspecRegistry.forClass(selectedClass).stream().toList();
                this.subspecCard0.update(subspecs.get(0), subspecs.get(0).subspec() == selectedSubspec);
                this.subspecCard1.update(subspecs.get(1), subspecs.get(1).subspec() == selectedSubspec);

                SpellCaster.spellForSlot(selectedClass, 0).ifPresent(this.spellCard0::update);
                SpellCaster.spellForSlot(selectedClass, 1).ifPresent(this.spellCard1::update);
            }
        }

        /** Extra-height spacer row, same as StatsTab's own. */
        private int spacer(int row) {
            this.grid.add(new TextWidget(0, 5, Text.empty(), MinecraftClient.getInstance().textRenderer), row, 0);
            return row + 1;
        }

        private static TextWidget sectionHeader(String text, TextRenderer textRenderer) {
            return new TextWidget(Text.literal(text).formatted(Formatting.BOLD), textRenderer);
        }
    }

    /** One clickable class card (icon, name, description, bonus, selected-state tag). */
    private static class ClassCardWidget extends ClickableWidget {
        private static final int CARD_WIDTH = 204;
        private static final int CARD_HEIGHT = 40;

        private static final int CARD_BACKGROUND = 0xF01F2622;
        private static final int CARD_BORDER = 0xFF33443B;
        private static final int SELECTED_BORDER = 0xFF5FA98C;
        private static final int SELECTED_WASH = 0x2E5FA98C;
        private static final int HOVER_BORDER = 0x805FA98C;
        private static final int BODY_COLOR = 0xFFB9C4BE;
        private static final int BONUS_COLOR = 0xFF7FD8E0;

        private final ClassDefinition definition;
        private boolean selected;

        ClassCardWidget(ClassDefinition definition) {
            super(0, 0, CARD_WIDTH, CARD_HEIGHT, Text.literal(definition.displayName()));
            this.definition = definition;
        }

        void setSelected(boolean selected) {
            this.selected = selected;
        }

        @Override
        protected void renderWidget(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
            int x = this.getX();
            int y = this.getY();
            int borderColor = this.selected ? SELECTED_BORDER : (this.isHovered() ? HOVER_BORDER : CARD_BORDER);
            int borderWidth = this.selected ? 2 : 1;

            context.fill(x, y, x + CARD_WIDTH, y + CARD_HEIGHT, borderColor);
            context.fill(x + borderWidth, y + borderWidth, x + CARD_WIDTH - borderWidth, y + CARD_HEIGHT - borderWidth, CARD_BACKGROUND);
            if (this.selected) {
                context.fill(x + borderWidth, y + borderWidth, x + CARD_WIDTH - borderWidth, y + CARD_HEIGHT - borderWidth, SELECTED_WASH);
            }

            PlayerClass playerClass = this.definition.playerClass();
            TextRenderer textRenderer = MinecraftClient.getInstance().textRenderer;
            Identifier icon = ClassIcons.of(playerClass);
            context.drawTexture(RenderPipelines.GUI_TEXTURED, icon, x + 6, y + 4, 0.0F, 0.0F, 32, 32, 16, 16);

            int textX = x + 46;
            int textWidth = CARD_WIDTH - 46 - 6;

            Text nameText = Text.literal(this.definition.displayName()).formatted(Formatting.BOLD);
            context.drawText(textRenderer, nameText, textX, y + 6, ClassIcons.accentColor(playerClass), true);

            String description = textRenderer.trimToWidth(this.definition.description(), textWidth);
            context.drawText(textRenderer, Text.literal(description), textX, y + 17, BODY_COLOR, true);

            Text bonusText = Text.literal(formatBonus(this.definition.bonusAttribute(), this.definition.bonusOperation(), this.definition.bonusAmount()));
            context.drawText(textRenderer, bonusText, textX, y + 28, BONUS_COLOR, true);

            if (this.selected) {
                Text activeTag = Text.literal("Aktiv").formatted(Formatting.BOLD);
                int tagWidth = textRenderer.getWidth(activeTag);
                context.drawText(textRenderer, activeTag, x + CARD_WIDTH - 6 - tagWidth, y + 6, SELECTED_BORDER, true);
            }
        }

        @Override
        public void onClick(Click click, boolean doubled) {
            ClientPlayNetworking.send(new ClassSelectPayload(this.definition.playerClass()));
        }

        @Override
        protected void appendClickableNarrations(NarrationMessageBuilder builder) {
            builder.put(NarrationPart.TITLE, this.getMessage());
        }

        /** Shared with {@link SubspecCardWidget} - class and sub-spec bonuses use the same shape. */
        static String formatBonus(RegistryEntry<EntityAttribute> attribute, EntityAttributeModifier.Operation operation, double amount) {
            String formattedAmount = switch (operation) {
                case ADD_VALUE -> String.format("+%.0f", amount);
                case ADD_MULTIPLIED_BASE, ADD_MULTIPLIED_TOTAL -> String.format("+%.0f%%", amount * 100);
            };
            return formattedAmount + " " + attributeLabel(attribute);
        }

        private static String attributeLabel(RegistryEntry<EntityAttribute> attribute) {
            String path = attribute.getKey().map(key -> key.getValue().getPath()).orElse("");
            return switch (path) {
                case "max_health" -> "Leben";
                case "movement_speed" -> "Lauftempo";
                case "luck" -> "Glück";
                case "knockback_resistance" -> "Rückstoßresistenz";
                case "armor" -> "Rüstung";
                case "attack_damage" -> "Angriffsschaden";
                case "attack_speed" -> "Angriffstempo";
                default -> path;
            };
        }
    }

    /**
     * One clickable sub-spec card - mutable (unlike ClassCardWidget) since only 2 of these
     * widgets exist and get repointed at whichever 2 sub-specs belong to the currently selected
     * class (see ClassTab.refreshValues), rather than pre-building all 8 and toggling
     * visibility. Same visual language as ClassCardWidget, minus the icon (no per-sub-spec
     * icon art exists yet).
     */
    private static class SubspecCardWidget extends ClickableWidget {
        private static final int CARD_WIDTH = 204;
        private static final int CARD_HEIGHT = 34;

        private static final int CARD_BACKGROUND = 0xF01F2622;
        private static final int CARD_BORDER = 0xFF33443B;
        private static final int SELECTED_BORDER = 0xFF5FA98C;
        private static final int SELECTED_WASH = 0x2E5FA98C;
        private static final int HOVER_BORDER = 0x805FA98C;
        private static final int BODY_COLOR = 0xFFB9C4BE;
        private static final int BONUS_COLOR = 0xFF7FD8E0;

        private SubspecDefinition definition;
        private boolean selected;

        SubspecCardWidget() {
            super(0, 0, CARD_WIDTH, CARD_HEIGHT, Text.empty());
            this.definition = SubspecRegistry.get(ClassSubspec.BOLLWERK);
        }

        void update(SubspecDefinition definition, boolean selected) {
            this.definition = definition;
            this.selected = selected;
            this.setMessage(Text.literal(definition.displayName()));
        }

        @Override
        protected void renderWidget(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
            int x = this.getX();
            int y = this.getY();
            int borderColor = this.selected ? SELECTED_BORDER : (this.isHovered() ? HOVER_BORDER : CARD_BORDER);
            int borderWidth = this.selected ? 2 : 1;

            context.fill(x, y, x + CARD_WIDTH, y + CARD_HEIGHT, borderColor);
            context.fill(x + borderWidth, y + borderWidth, x + CARD_WIDTH - borderWidth, y + CARD_HEIGHT - borderWidth, CARD_BACKGROUND);
            if (this.selected) {
                context.fill(x + borderWidth, y + borderWidth, x + CARD_WIDTH - borderWidth, y + CARD_HEIGHT - borderWidth, SELECTED_WASH);
            }

            TextRenderer textRenderer = MinecraftClient.getInstance().textRenderer;
            int accent = ClassIcons.accentColor(this.definition.subspec().parentClass());
            int textX = x + 6;
            int textWidth = CARD_WIDTH - 12;

            Text nameText = Text.literal(this.definition.displayName()).formatted(Formatting.BOLD);
            context.drawText(textRenderer, nameText, textX, y + 4, accent, true);

            String description = textRenderer.trimToWidth(this.definition.description(), textWidth);
            context.drawText(textRenderer, Text.literal(description), textX, y + 14, BODY_COLOR, true);

            Text bonusText = Text.literal(ClassCardWidget.formatBonus(
                this.definition.bonusAttribute(), this.definition.bonusOperation(), this.definition.bonusAmount()
            ));
            context.drawText(textRenderer, bonusText, textX, y + 24, BONUS_COLOR, true);

            if (this.selected) {
                Text activeTag = Text.literal("Aktiv").formatted(Formatting.BOLD);
                int tagWidth = textRenderer.getWidth(activeTag);
                context.drawText(textRenderer, activeTag, x + CARD_WIDTH - 6 - tagWidth, y + 4, SELECTED_BORDER, true);
            }
        }

        @Override
        public void onClick(Click click, boolean doubled) {
            ClientPlayNetworking.send(new SubspecSelectPayload(this.definition.subspec()));
        }

        @Override
        protected void appendClickableNarrations(NarrationMessageBuilder builder) {
            builder.put(NarrationPart.TITLE, this.getMessage());
        }
    }

    /**
     * One clickable spell card - casts the spell on click via the exact same
     * {@link CastSpellPayload}/slot the V/B keybinds send (SpellCastKeyBindings), so a click
     * here and a keypress are indistinguishable to the server. Mutable like
     * {@link SubspecCardWidget}: only 2 widgets exist, repointed at whichever spell currently
     * occupies that slot for the selected class.
     */
    private static class SpellCardWidget extends ClickableWidget {
        private static final int CARD_WIDTH = 204;
        private static final int CARD_HEIGHT = 26;

        private static final int CARD_BACKGROUND = 0xF01F2622;
        private static final int CARD_BORDER = 0xFF33443B;
        private static final int HOVER_BORDER = 0x805FA98C;
        private static final int NAME_COLOR = 0xFF7FD8E0;
        private static final int INFO_COLOR = 0xFF9C9186;

        private final int slot;
        private Spell spell;

        SpellCardWidget(int slot) {
            super(0, 0, CARD_WIDTH, CARD_HEIGHT, Text.empty());
            this.slot = slot;
            this.spell = Spell.values()[0];
        }

        void update(Spell spell) {
            this.spell = spell;
            this.setMessage(Text.literal(spell.displayName()));
        }

        @Override
        protected void renderWidget(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
            int x = this.getX();
            int y = this.getY();
            int borderColor = this.isHovered() ? HOVER_BORDER : CARD_BORDER;

            context.fill(x, y, x + CARD_WIDTH, y + CARD_HEIGHT, borderColor);
            context.fill(x + 1, y + 1, x + CARD_WIDTH - 1, y + CARD_HEIGHT - 1, CARD_BACKGROUND);

            TextRenderer textRenderer = MinecraftClient.getInstance().textRenderer;
            Text nameText = Text.literal(this.spell.displayName()).formatted(Formatting.BOLD);
            context.drawText(textRenderer, nameText, x + 6, y + 4, NAME_COLOR, true);

            String info = String.format("Mana: %d | Cooldown: %.0fs", this.spell.manaCost(), this.spell.cooldownTicks() / 20.0);
            context.drawText(textRenderer, Text.literal(info), x + 6, y + 14, INFO_COLOR, true);
        }

        @Override
        public void onClick(Click click, boolean doubled) {
            ClientPlayNetworking.send(new CastSpellPayload(this.slot));
        }

        @Override
        protected void appendClickableNarrations(NarrationMessageBuilder builder) {
            builder.put(NarrationPart.TITLE, this.getMessage());
        }
    }
}
