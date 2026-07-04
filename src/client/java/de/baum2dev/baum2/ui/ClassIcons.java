package de.baum2dev.baum2.ui;

import net.minecraft.util.Identifier;
import de.baum2dev.baum2.classes.ClassRegistry;
import de.baum2dev.baum2.classes.PlayerClass;

/**
 * Per-class visual identity (icon texture, accent color) layered on top of
 * {@link ClassRegistry}'s gameplay data. Colors/icons per
 * docs/visual-style-guide.md section 3.3 — full-alpha ARGB ints, since
 * DrawContext's drawText silently no-ops on a color with a zero alpha byte
 * (see docs/fabric-modding.md "Custom UI (HUD / Screens)").
 */
final class ClassIcons {
    static Identifier of(PlayerClass playerClass) {
        String path = "textures/gui/class/" + playerClass.name().toLowerCase(java.util.Locale.ROOT) + ".png";
        return Identifier.of("baum2", path);
    }

    static int accentColor(PlayerClass playerClass) {
        return switch (playerClass) {
            case EISENWAECHTER -> 0xFF8FA3B3;
            case SCHATTENLAEUFER -> 0xFF7C5CA0;
            case RUNENWIRKER -> 0xFF66C4C2;
            case WESENSWAHRER -> 0xFF7FA65C;
        };
    }

    static String displayName(PlayerClass playerClass) {
        return ClassRegistry.get(playerClass).displayName();
    }

    private ClassIcons() {
    }
}
