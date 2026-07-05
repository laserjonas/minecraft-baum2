package de.baum2dev.baum2.ui;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;
import de.baum2dev.baum2.networking.CastSpellPayload;

/**
 * Registers the "Cast Spell 1"/"Cast Spell 2" keybindings - the primary way to cast a spell.
 * Two keys, not one per spell: each class only ever has 2 spells, so pressing "slot 1" always
 * casts whichever spell is first for the player's *current* class (see
 * SpellCaster.spellForSlot) - `/baum2 cast <spell>` still exists as a fallback/testing path,
 * but typing a command mid-combat doesn't make sense as the primary way to fight.
 */
public class SpellCastKeyBindings {
    private static final KeyBinding CAST_SPELL_1 = KeyBindingHelper.registerKeyBinding(
        new KeyBinding("key.baum2.cast_spell_1", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_V, KeyBinding.Category.MISC)
    );
    private static final KeyBinding CAST_SPELL_2 = KeyBindingHelper.registerKeyBinding(
        new KeyBinding("key.baum2.cast_spell_2", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_B, KeyBinding.Category.MISC)
    );

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (CAST_SPELL_1.wasPressed()) {
                ClientPlayNetworking.send(new CastSpellPayload(0));
            }
            while (CAST_SPELL_2.wasPressed()) {
                ClientPlayNetworking.send(new CastSpellPayload(1));
            }
        });
    }

    private SpellCastKeyBindings() {
    }
}
