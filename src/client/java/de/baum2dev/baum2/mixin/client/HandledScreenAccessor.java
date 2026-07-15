package de.baum2dev.baum2.mixin.client;

import net.minecraft.client.gui.screen.ingame.HandledScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * HandledScreen's panel origin (the top-left corner of the 176x166 background) is a
 * protected field with no public getter in Yarn 1.21.11, and Fabric's Screens utility
 * doesn't expose it either (confirmed, see docs/fabric-modding.md "Overlay on a vanilla
 * HandledScreen"). Used by ui/BaumCreditsInventoryOverlay to anchor the credits text to
 * the inventory panel. Read fresh every frame: the recipe book reassigns {@code x} live
 * when toggled, so no extra recipe-book handling is needed.
 */
@Mixin(HandledScreen.class)
public interface HandledScreenAccessor {

    @Accessor("x")
    int baum2$getPanelX();

    @Accessor("y")
    int baum2$getPanelY();
}
