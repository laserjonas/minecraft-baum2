"""Builds the Drevathis Cursed Blade's item assets (Drevathis boss rework).

Two outputs, same split as tools/gen_colossal_warclub_item.py established:
1. drevathis_cursed_blade.png - a NEW flat 16x16 icon (gui/ground/fixed contexts), replacing
   the pre-rework icon so the item matches the reworked boss weapon: a pure-black greatsword
   on the sword diagonal, cold grey-violet edge light, dark leather grip, plus two faint smoke
   wisps curling off the blade (the item's held-form smoke effect, hinted in the icon).
2. drevathis_cursed_blade_in_hand.json + drevathis_cursed_blade_3d.png - a real 3D element
   model for first/third person: ~1.6 blocks of black blade at player proportions ("same as
   the boss weapon, but proportional to the player"). Authored VERTICALLY, every element
   carrying a -45deg Z rotation around (8,8,8) onto the sword diagonal so the stock
   `minecraft:item/handheld` display transforms apply unchanged (the proven warclub trick).

Palette: "Umbral Sovereign" blade colors, pinned in docs/visual-style-guide.md Section 19
(BLADE_BLACK/BLADE_GREY/BLADE_EDGE + grip browns) - the runtime dark-smoke wreath is
CursedBladeItem.inventoryTick(), not texture.

The item-model definition (assets/baum2/items/drevathis_cursed_blade.json) selects by display
context, exactly like colossal_warclub.json (vanilla trident.json schema).

Run from the repo root (requires Pillow); copy outputs to:
  src/main/resources/assets/baum2/textures/item/drevathis_cursed_blade.png
  src/main/resources/assets/baum2/textures/item/drevathis_cursed_blade_3d.png
  src/main/resources/assets/baum2/models/item/drevathis_cursed_blade_in_hand.json
"""
import json
import random
from PIL import Image, ImageDraw

random.seed(20260707)

# Palette - docs/visual-style-guide.md Section 19 (Umbral Sovereign)
BLADE_BLACK = (0x0C, 0x0A, 0x10)
BLADE_GREY = (0x23, 0x20, 0x30)
BLADE_EDGE = (0x4E, 0x4A, 0x5E)
BLADE_EDGE_LIT = (0x6A, 0x66, 0x7E)
GRIP_DARK = (0x1C, 0x14, 0x10)
GRIP_LIT = (0x33, 0x26, 0x1D)
GUARD = (0x2B, 0x27, 0x38)
SMOKE = (0x3A, 0x35, 0x44)

# ============================================================================
# 1) flat 16x16 icon: black greatsword on the standard sword diagonal
# ============================================================================
icon = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
idraw = ImageDraw.Draw(icon)


def ipx(x, y, c):
    if 0 <= x < 16 and 0 <= y < 16:
        idraw.point((x, y), fill=c + (255,))


# blade: 2px-wide diagonal from (4,11) up to (13,2); edge (lower-left row) lit
for i in range(10):
    x = 4 + i
    y = 11 - i
    j = random.randint(-4, 4)
    body = tuple(max(0, min(255, c + j)) for c in BLADE_BLACK)
    ipx(x, y, body)                       # core
    ipx(x + 1, y, tuple(max(0, min(255, c + j)) for c in BLADE_GREY))  # spine
    ipx(x, y + 1, BLADE_EDGE if i % 3 else BLADE_EDGE_LIT)             # cutting edge
# tip pixel
ipx(14, 1, BLADE_EDGE_LIT)
ipx(13, 1, BLADE_BLACK)
# crossguard: short counter-diagonal bar
for d in (-1, 0, 1, 2):
    ipx(3 + d, 10 - d - 2, GUARD)
    ipx(4 + d, 11 - d - 2, GUARD if d != 0 else BLADE_EDGE)
# grip down-left from the guard
for i in range(3):
    ipx(2 - i + 1, 12 + i, GRIP_DARK if i % 2 == 0 else GRIP_LIT)
ipx(0, 15, GUARD)  # pommel
# two faint smoke wisps curling off the blade (edge-tone so they survive dark UI backgrounds)
for i, (wx, wy) in enumerate(((6, 6), (7, 5), (10, 4), (10, 3))):
    ipx(wx, wy, BLADE_EDGE if i % 2 == 0 else SMOKE)

icon.save("drevathis_cursed_blade.png")

# ============================================================================
# 2) 3D in-hand model - 32x32 texture; element UVs in 0..16 space (1 uv unit = 2 px)
# ============================================================================
TEX = 32
img = Image.new("RGBA", (TEX, TEX), (0, 0, 0, 0))
draw = ImageDraw.Draw(img)


def noisy(x0, y0, w, h, base, jitter=4, vgrad=(1.08, 0.88)):
    for yy in range(h):
        mul = vgrad[0] + (vgrad[1] - vgrad[0]) * (yy / max(1, h - 1))
        for xx in range(w):
            j = random.randint(-jitter, jitter)
            c = tuple(max(0, min(255, int(ch * mul) + j)) for ch in base)
            draw.point((x0 + xx, y0 + yy), fill=c + (255,))


def paint_blade_face(x0, y0, w, h):
    """Broad face: near-black, faint diagonal sheen, bright edge column on the LEFT."""
    noisy(x0, y0, w, h, BLADE_BLACK, jitter=3)
    for start in range(-h, w, 6):
        for i in range(h):
            xx = x0 + start + i // 2
            if x0 + 1 <= xx < x0 + w - 1 and random.random() < 0.55:
                draw.point((xx, y0 + i), fill=BLADE_GREY + (255,))
    for yy in range(h):
        draw.point((x0, y0 + yy), fill=(BLADE_EDGE_LIT if yy % 5 == 0 else BLADE_EDGE) + (255,))
        draw.point((x0 + w - 1, y0 + yy), fill=BLADE_GREY + (255,))
    for ry in (h // 4, 2 * h // 3):
        draw.point((x0 + w // 2, y0 + ry), fill=BLADE_EDGE + (255,))


def paint_thin(x0, y0, w, h):
    noisy(x0, y0, w, h, BLADE_BLACK, jitter=3, vgrad=(1.02, 0.92))
    for yy in range(0, h, 4):
        draw.point((x0, y0 + yy), fill=BLADE_GREY + (255,))


def paint_grip(x0, y0, w, h):
    noisy(x0, y0, w, h, GRIP_DARK, jitter=4)
    for yy in range(1, h, 3):
        draw.rectangle([x0, y0 + yy, x0 + w - 1, y0 + yy], fill=GRIP_LIT + (255,))


def paint_guard(x0, y0, w, h):
    noisy(x0, y0, w, h, GUARD, jitter=4, vgrad=(1.15, 0.8))
    for xx in range(w):
        draw.point((x0 + xx, y0), fill=BLADE_EDGE + (255,))


# atlas regions (px) -> uv rects (0..16 space = px / 2)
paint_blade_face(0, 0, 9, 32)    # face      uv [0,0,4.5,16]
paint_thin(10, 0, 3, 32)         # thin edge uv [5,0,6.5,16]
paint_grip(14, 0, 4, 16)         # grip      uv [7,0,9,8]
paint_guard(14, 18, 8, 5)        # guard     uv [7,9,11,11.5]
noisy(19, 0, 4, 4, GUARD)        # pommel    uv [9.5,0,11.5,2]
paint_blade_face(24, 0, 6, 10)   # tip       uv [12,0,15,5]

img.save("drevathis_cursed_blade_3d.png")

FACE_UV = [0, 0, 4.5, 16]
THIN_UV = [5, 0, 6.5, 16]
GRIP_UV = [7, 0, 9, 8]
GUARD_UV = [7, 9, 11, 11.5]
POMMEL_UV = [9.5, 0, 11.5, 2]
TIP_UV = [12, 0, 15, 5]
TIP_THIN_UV = [5, 0, 6.5, 5]

ROT = {"origin": [8, 8, 8], "axis": "z", "angle": -45}


def element(from_, to, uv_side, uv_cap=None, uv_ew=None):
    """north/south get uv_side, east/west get uv_ew (default uv_side), up/down get uv_cap."""
    uv_cap = uv_cap or uv_side
    uv_ew = uv_ew or uv_side
    faces = {}
    for f in ("north", "south"):
        faces[f] = {"uv": list(uv_side), "texture": "#blade"}
    for f in ("east", "west"):
        faces[f] = {"uv": list(uv_ew), "texture": "#blade"}
    for f in ("up", "down"):
        faces[f] = {"uv": list(uv_cap), "texture": "#blade"}
    return {"from": list(from_), "to": list(to), "rotation": dict(ROT), "faces": faces}


# Vertical authoring, grip zone around pre-rotation y=0 (sword-sprite grip position).
# Total reach -4.2 .. 25.2 -> ~29.4 units ~= 1.65 blocks of visible weapon in hand.
elements = [
    # pommel
    element([7.1, -4.2, 7.1], [8.9, -2.9, 8.9], POMMEL_UV),
    # grip
    element([7.3, -3.0, 7.3], [8.7, 3.4, 8.7], GRIP_UV),
    # crossguard: wide across the blade plane (z), thin in x
    element([7.2, 3.4, 5.6], [8.8, 4.8, 10.4], GUARD_UV),
    # main blade slab: broad faces east/west, cutting edge -Z (front-left after -45 rotation)
    element([7.45, 4.8, 5.9], [8.55, 21.6, 10.1], THIN_UV, uv_cap=GUARD_UV, uv_ew=FACE_UV),
    # tapered tip, shifted toward the cutting edge
    element([7.45, 21.6, 6.3], [8.55, 25.2, 9.1], TIP_THIN_UV, uv_cap=TIP_THIN_UV, uv_ew=TIP_UV),
]

model = {
    "parent": "minecraft:item/handheld",
    "texture_size": [TEX, TEX],
    "textures": {"blade": "baum2:item/drevathis_cursed_blade_3d",
                 "particle": "baum2:item/drevathis_cursed_blade_3d"},
    "elements": elements,
}
with open("drevathis_cursed_blade_in_hand.json", "w", encoding="utf-8") as f:
    json.dump(model, f, indent=2)

print(f"wrote drevathis_cursed_blade.png (16x16 icon), "
      f"drevathis_cursed_blade_3d.png ({TEX}x{TEX}), "
      f"drevathis_cursed_blade_in_hand.json ({len(elements)} elements)")
