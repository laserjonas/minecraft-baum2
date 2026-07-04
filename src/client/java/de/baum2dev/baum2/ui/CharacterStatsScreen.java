package de.baum2dev.baum2.ui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.ScreenRect;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.tab.GridScreenTab;
import net.minecraft.client.gui.tab.TabManager;
import net.minecraft.client.gui.widget.TextWidget;
import net.minecraft.client.gui.widget.TabNavigationWidget;
import net.minecraft.client.input.KeyInput;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.text.Text;
import de.baum2dev.baum2.networking.ClientNetworkingHandler;

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
    private static final int BASE_DAMAGE_COLOR = 0xD98A3D;
    private static final int BASE_MAGIC_DAMAGE_COLOR = 0x9B5FE0;

    private final TabManager tabManager = new TabManager(this::addDrawableChild, this::remove);
    private TabNavigationWidget tabNavigationWidget;
    private StatsTab statsTab;

    public CharacterStatsScreen() {
        super(Text.literal("Character Stats"));
    }

    @Override
    protected void init() {
        this.statsTab = new StatsTab(this.textRenderer);
        this.tabNavigationWidget = TabNavigationWidget.builder(this.tabManager, this.width)
                .tabs(this.statsTab)
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
    public void render(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
        // Do not call renderBackground() here - the framework already renders the screen
        // background before invoking render() (confirmed by a crash: calling it again here
        // double-applies the background blur, which throws "Can only blur once per frame").
        this.statsTab.refreshValues();
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
        private final TextWidget lifeValue;
        private final TextWidget manaValue;
        private final TextWidget baseDamageValue;
        private final TextWidget baseMagicDamageValue;

        StatsTab(TextRenderer textRenderer) {
            super(Text.literal("Stats"));
            this.grid.setRowSpacing(6);
            this.grid.setColumnSpacing(6);

            this.lifeValue = new TextWidget(70, 9, Text.empty(), textRenderer);
            this.manaValue = new TextWidget(70, 9, Text.empty(), textRenderer);
            this.baseDamageValue = new TextWidget(40, 9, Text.empty(), textRenderer);
            this.baseMagicDamageValue = new TextWidget(40, 9, Text.empty(), textRenderer);

            this.grid.add(label("Life", textRenderer), 0, 0);
            this.grid.add(this.lifeValue, 0, 1);
            this.grid.add(label("Mana", textRenderer), 1, 0);
            this.grid.add(this.manaValue, 1, 1);
            // Extra-height spacer row to widen the gap between the resource-stat pair above
            // and the offense-stat pair below, per the style guide's "~14px between pairs".
            this.grid.add(new TextWidget(0, 8, Text.empty(), textRenderer), 2, 0, 1, 2);
            this.grid.add(label("Base Damage", textRenderer), 3, 0);
            this.grid.add(this.baseDamageValue, 3, 1);
            this.grid.add(label("Base Magic Damage", textRenderer), 4, 0);
            this.grid.add(this.baseMagicDamageValue, 4, 1);
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

            this.baseDamageValue.setMessage(colored(
                    String.format("%.1f", ClientNetworkingHandler.getCurrentBaseDamage()), BASE_DAMAGE_COLOR));
            this.baseMagicDamageValue.setMessage(colored(
                    String.format("%.1f", ClientNetworkingHandler.getCurrentBaseMagicDamage()), BASE_MAGIC_DAMAGE_COLOR));
        }

        private static TextWidget label(String text, TextRenderer textRenderer) {
            return new TextWidget(colored(text, LABEL_COLOR), textRenderer);
        }

        private static Text colored(String text, int color) {
            return Text.literal(text).styled(style -> style.withColor(color));
        }
    }
}
