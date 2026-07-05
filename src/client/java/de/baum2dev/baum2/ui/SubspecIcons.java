package de.baum2dev.baum2.ui;

import net.minecraft.util.Identifier;
import de.baum2dev.baum2.classes.ClassSubspec;

/**
 * Per-sub-spec icon lookup, mirrors {@link ClassIcons#of(de.baum2dev.baum2.classes.PlayerClass)}.
 * Icons/colors per docs/visual-style-guide.md Section 9.1 — each is its parent class's own icon
 * (see {@link ClassIcons}) plus one small overlay detail, so the accent color is always the
 * parent class's ({@link ClassIcons#accentColor}), not a separate per-sub-spec color.
 */
final class SubspecIcons {
    static Identifier of(ClassSubspec subspec) {
        String path = "textures/gui/subspec/" + subspec.name().toLowerCase(java.util.Locale.ROOT) + ".png";
        return Identifier.of("baum2", path);
    }

    private SubspecIcons() {
    }
}
