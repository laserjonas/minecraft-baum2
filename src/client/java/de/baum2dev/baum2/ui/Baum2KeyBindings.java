package de.baum2dev.baum2.ui;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

/**
 * Registers the 'C' keybinding that opens the character stats screen. Closing on a second
 * 'C' press is handled by CharacterStatsScreen.keyPressed itself (matching this same
 * KeyBinding), not here - a global KeyBinding poll isn't the reliable way to close a screen
 * that already has input focus, see docs/fabric-modding.md's "Input / Keybindings" section.
 */
public class Baum2KeyBindings {
    public static final KeyBinding OPEN_STATS_KEY = KeyBindingHelper.registerKeyBinding(
            new KeyBinding("key.baum2.open_stats", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_C, KeyBinding.Category.MISC)
    );

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (OPEN_STATS_KEY.wasPressed()) {
                if (client.currentScreen == null) {
                    client.setScreen(new CharacterStatsScreen());
                }
            }
        });
    }
}
