package de.baum2dev.baum2.ui;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;
import de.baum2dev.baum2.networking.OpenMountEquipmentPayload;
import de.baum2dev.baum2.networking.ToggleMountPayload;

/**
 * Mount-system keybindings:
 * - Ctrl+H toggles the mount (summon the equipped flute's horse / dismiss the ridden one).
 *   Vanilla keybinds can't express a modifier chord, so the binding is plain H and the Ctrl
 *   requirement is checked via InputUtil.isKeyPressed at press time (Screen.hasControlDown()
 *   no longer exists in the 1.21.11 mapping).
 * - G opens the equipment inventory (the screen holding the flute slot). Server-opened
 *   (real item slots need a ScreenHandler), hence a payload rather than client.setScreen.
 */
public class MountKeyBindings {
    private static final KeyBinding TOGGLE_MOUNT = KeyBindingHelper.registerKeyBinding(
        new KeyBinding("key.baum2.toggle_mount", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_H, KeyBinding.Category.MISC)
    );
    private static final KeyBinding OPEN_EQUIPMENT = KeyBindingHelper.registerKeyBinding(
        new KeyBinding("key.baum2.open_equipment", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_G, KeyBinding.Category.MISC)
    );

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (TOGGLE_MOUNT.wasPressed()) {
                if (isControlDown(client) && client.player != null) {
                    ClientPlayNetworking.send(new ToggleMountPayload());
                }
            }
            while (OPEN_EQUIPMENT.wasPressed()) {
                if (client.player != null) {
                    ClientPlayNetworking.send(new OpenMountEquipmentPayload());
                }
            }
        });
    }

    private static boolean isControlDown(MinecraftClient client) {
        return InputUtil.isKeyPressed(client.getWindow(), GLFW.GLFW_KEY_LEFT_CONTROL)
                || InputUtil.isKeyPressed(client.getWindow(), GLFW.GLFW_KEY_RIGHT_CONTROL);
    }

    private MountKeyBindings() {
    }
}
