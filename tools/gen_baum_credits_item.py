"""Generates the 16x16 placeholder icon for Baum Credits, the mod's currency
(see docs/visual-style-guide.md Section 24). Not an item: the coin renders as a GUI icon
next to the balance text on the inventory screen (client ui/BaumCreditsInventoryOverlay).

Concept: a flat brass coin (bevel = outline / rim / base / highlight, 4 flat tones, no
anti-aliasing/noise - same "flat fills, small palette, outline" convention every other item
icon in this doc follows) stamped with a tiny two-tone tree glyph in the center - literally
"Baum" (tree), the mod's own namesake, not any existing game's currency symbol.

Palette decision (documented in the style guide, Section 24): reuses the established
"Deepwood & Verdigris" UI-chrome palette (Section 2) rather than inventing a new bespoke
"currency" palette - Aged Brass for the coin body (already the UI's "gold-associated" emphasis
accent, Section 2.2) and Verdigris Glow/Verdigris-muted for the stamped tree glyph (literally
pairing "gold coin" + "verdigris patina," the two words the whole art-direction name comes
from). This mirrors the precedent Gold Sword set in Section 14.1 (reusing Aged Brass for its
pommel) and follows Section 1.2's guidance that bespoke new palette families are reserved for
boss-tier content - Baum Credits is a common, global, UI-adjacent utility item, not a boss or
its drop, so it ties back into the mod's existing chrome palette instead.

Output (CWD): baum_credits.png (16x16 RGBA). Copy to:
  src/main/resources/assets/baum2/textures/gui/baum_credits.png
"""
from PIL import Image, ImageDraw

# Palette - see docs/visual-style-guide.md Section 24
OUTLINE = (0x0B, 0x0E, 0x0D, 255)  # Void Seam (Section 2.1) - coin outer edge
RIM = (0xB8, 0x93, 0x4F, 255)      # darker brass bevel ring (in-family with Aged Brass)
BASE = (0xD9, 0xB3, 0x6C, 255)     # Aged Brass (Section 2.2) exactly - coin body
HI = (0xF0, 0xD9, 0x99, 255)       # light brass highlight, upper-left "shine"
LEAF = (0x5F, 0xA9, 0x8C, 255)     # Verdigris Glow (Section 2.2) - stamped tree canopy
TRUNK = (0x33, 0x44, 0x3B, 255)    # Verdigris, muted (Section 2.1) - stamped tree trunk/groove

img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
d = ImageDraw.Draw(img)

# Coin bevel: outline -> rim -> base, each inset 1px, flat (non-antialiased) fills.
d.ellipse([0, 0, 15, 15], fill=OUTLINE)
d.ellipse([1, 1, 14, 14], fill=RIM)
d.ellipse([2, 2, 13, 13], fill=BASE)

# Upper-left "shine" highlight, kept clear of the center glyph.
d.ellipse([2, 3, 4, 5], fill=HI)

# Center-stamped tree glyph ("Baum" = tree), symmetric on the canvas's x=7/8 center seam.
canopy = [(7, 5), (8, 5), (6, 6), (7, 6), (8, 6), (9, 6)]
trunk = [(7, 7), (8, 7), (7, 8), (8, 8), (7, 9), (8, 9)]
for x, y in canopy:
    d.point((x, y), fill=LEAF)
for x, y in trunk:
    d.point((x, y), fill=TRUNK)

img.save("baum_credits.png")
print("wrote baum_credits.png", img.size, img.mode)
