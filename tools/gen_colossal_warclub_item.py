"""Builds the Colossal Warclub's 3D in-hand item model (player-held version of the boss club).

Brief: "rework the model of the club item for player - like the boss weapon but good
proportion to the player." The boss's club is GeckoLib entity geometry and can't be reused
directly by the item pipeline, so this generates a vanilla ELEMENT-based item model (the same
JSON cuboid format block models use) with the same design language: faceted wood shaft with
leather grip wraps, lumpy two-cube head, embedded metal studs, one blood smear - all in
docs/visual-style-guide.md Section 18.4's pinned Colossal Warclub palette.

Key trick (how 3D-sword resource packs work): the club is authored VERTICALLY (+Y), then every
element carries a -45deg Z rotation around the model center (8,8,8), laying it along the exact
diagonal a vanilla sword sprite occupies - so the stock `minecraft:item/handheld` display
transforms position it correctly in first/third person with no hand-tuned display block.
Player proportion: ~27.5 units along the diagonal ≈ 1.7 blocks of visual club - a hefty
two-hander against a 1.8-block player, clearly "the boss's weapon scaled down."

The GUI/ground/fixed contexts keep the existing, approved flat 16x16 icon: the item-model
definition (assets/baum2/items/colossal_warclub.json) selects by display context, exactly
vanilla's own trident.json schema (verified from the real 1.21.11 client jar).

Outputs (CWD): colossal_warclub_in_hand.json, colossal_warclub_3d.png, plus a preview-only
_preview.geo.json so tools/render_geckolib_preview.py can render the shape/texture
(`python tools/render_geckolib_preview.py --geo colossal_warclub_preview.geo.json --tex
colossal_warclub_3d.png` - the preview shows the pre-rotation vertical pose, which is fine for
judging proportions/paint; the -45 diagonal only matters in-hand). Copy to:
  src/main/resources/assets/baum2/models/item/colossal_warclub_in_hand.json
  src/main/resources/assets/baum2/textures/item/colossal_warclub_3d.png
"""
import json
import random
from PIL import Image, ImageDraw

random.seed(20260706)

# Palette - pinned in docs/visual-style-guide.md 18.4
WOOD_DARK = (0x3E, 0x2A, 0x1A)
WOOD_MID = (0x5A, 0x3D, 0x24)
GRIP = (0x24, 0x18, 0x0F)
CLUB_BASE = (0x6B, 0x5A, 0x42)
CLUB_HI = (0x8C, 0x78, 0x54)
CLUB_SHADOW = (0x46, 0x38, 0x23)
STUD = (0x8A, 0x8A, 0x82)
STUD_SHADOW = (0x4A, 0x4A, 0x44)
SMEAR = (0x6B, 0x2A, 0x1E)

# 32x32 texture; element UVs are in 0..16 space, so 1 uv unit = 2 px
TEX = 32
img = Image.new("RGBA", (TEX, TEX), (0, 0, 0, 0))
draw = ImageDraw.Draw(img)


def noisy(x0, y0, w, h, base, jitter=6, vgrad=(1.08, 0.85)):
    for yy in range(h):
        mul = vgrad[0] + (vgrad[1] - vgrad[0]) * (yy / max(1, h - 1))
        for xx in range(w):
            j = random.randint(-jitter, jitter)
            c = tuple(max(0, min(255, int(ch * mul) + j)) for ch in base)
            draw.point((x0 + xx, y0 + yy), fill=c + (255,))


def paint_shaft(x0, y0, w, h):
    """Faceted wood strip; texture BOTTOM = handle end -> grip wraps in the lower third."""
    for yy in range(h):
        for xx in range(w):
            base = WOOD_DARK if (xx + yy // 3) % 2 == 0 else WOOD_MID
            j = random.randint(-6, 6)
            c = tuple(max(0, min(255, ch + j)) for ch in base)
            draw.point((x0 + xx, y0 + yy), fill=c + (255,))
    for gy in (h - 4, h - 8):  # leather grip bands
        for xx in range(w):
            draw.point((x0 + xx, y0 + gy), fill=GRIP + (255,))
            draw.point((x0 + xx, y0 + gy + 1), fill=GRIP + (255,))
        draw.point((x0 + w // 2, y0 + gy), fill=tuple(min(255, c + 25) for c in GRIP) + (255,))


def paint_head(x0, y0, w, h, smear=True):
    noisy(x0, y0, w, h, CLUB_BASE)
    for _ in range(3):  # organic lumps
        lx, ly = x0 + random.randint(1, w - 4), y0 + random.randint(1, h - 4)
        draw.rectangle([lx, ly, lx + 2, ly + 1], fill=CLUB_HI + (255,))
        draw.rectangle([lx + 1, ly + 2, lx + 3, ly + 2], fill=CLUB_SHADOW + (255,))
    if smear:
        draw.rectangle([x0 + w // 2, y0 + h - 3, x0 + w // 2 + 2, y0 + h - 2], fill=SMEAR + (255,))


def paint_stud(x0, y0, w, h):
    noisy(x0, y0, w, h, STUD, jitter=5, vgrad=(1.15, 0.75))
    draw.point((x0 + w - 1, y0 + h - 1), fill=STUD_SHADOW + (255,))
    draw.point((x0, y0), fill=tuple(min(255, c + 30) for c in STUD) + (255,))


# --- atlas regions (px) -> uv rects (0..16 space = px / 2) ---
paint_shaft(0, 0, 4, 32)          # uv [0,0,2,16]
paint_head(4, 0, 14, 16)          # head face      uv [2,0,9,8]
paint_head(4, 16, 14, 14, smear=False)  # head top/bottom uv [2,8,9,15]
paint_head(18, 0, 11, 11)         # lump           uv [9,0,14.5,5.5]
paint_stud(18, 12, 4, 4)          # stud           uv [9,6,11,8]
noisy(22, 12, 6, 3, WOOD_DARK)    # pommel         uv [11,6,14,7.5]

img.save("colossal_warclub_3d.png")

# ============================================================================
# Elements (vertical authoring; -45deg Z rotation lays it along the sword diagonal).
# Grip point is pre-rotation y=0 -> lands at the sprite-space (~4,4) a sword handle occupies.
# ============================================================================
ROT = {"origin": [8, 8, 8], "axis": "z", "angle": -45}
SHAFT_UV = [0, 0, 2, 16]
HEAD_UV = [2, 0, 9, 8]
HEAD_CAP_UV = [2, 8, 9, 15]
LUMP_UV = [9, 0, 14.5, 5.5]
STUD_UV = [9, 6, 11, 8]
POMMEL_UV = [11, 6, 14, 7.5]


def element(from_, to, uv_side, uv_cap=None, uv_override=None):
    uv_cap = uv_cap or uv_side
    faces = {}
    for f in ("north", "south", "east", "west"):
        faces[f] = {"uv": list(uv_side), "texture": "#club"}
    for f in ("up", "down"):
        faces[f] = {"uv": list(uv_cap), "texture": "#club"}
    if uv_override:
        for f, u in uv_override.items():
            faces[f] = {"uv": list(u), "texture": "#club"}
    return {"from": list(from_), "to": list(to), "rotation": dict(ROT), "faces": faces}


elements = [
    # pommel knob below the grip
    element([6.5, -4.5, 6.5], [9.5, -3.0, 9.5], POMMEL_UV),
    # shaft: grip zone around y=0, running up into the head
    element([6.9, -4.0, 6.9], [9.1, 15.0, 9.1], SHAFT_UV, uv_cap=POMMEL_UV),
    # main head mass
    element([4.5, 13.0, 4.5], [11.5, 21.0, 11.5], HEAD_UV, uv_cap=HEAD_CAP_UV),
    # offset lump for an irregular silhouette
    element([6.5, 17.0, 6.0], [12.0, 22.5, 11.5], LUMP_UV),
    # studs: one out the front face, one out the side
    element([7.1, 17.0, 2.7], [8.9, 18.8, 4.5], STUD_UV),
    element([11.5, 14.5, 7.1], [13.3, 16.3, 8.9], STUD_UV),
]

model = {
    "parent": "minecraft:item/handheld",
    "texture_size": [TEX, TEX],
    "textures": {"club": "baum2:item/colossal_warclub_3d",
                 "particle": "baum2:item/colossal_warclub_3d"},
    "elements": elements,
}
with open("colossal_warclub_in_hand.json", "w", encoding="utf-8") as f:
    json.dump(model, f, indent=2)

# preview-only geo.json (vertical pose, rotation dropped) for render_geckolib_preview.py
Y_OFF = 5.0  # lift above the preview's ground plane
geo_bones = [{
    "name": "club", "pivot": [0, 0, 0],
    "cubes": [
        {
            "origin": [e["from"][0] - 8, e["from"][1] + Y_OFF, e["from"][2] - 8],
            "size": [e["to"][i] - e["from"][i] for i in range(3)],
            "uv": {f: {"uv": [u["uv"][0] * 2, u["uv"][1] * 2],
                       "uv_size": [(u["uv"][2] - u["uv"][0]) * 2, (u["uv"][3] - u["uv"][1]) * 2]}
                   for f, u in e["faces"].items()},
        }
        for e in elements
    ],
}]
geo = {"format_version": "1.12.0", "minecraft:geometry": [{
    "description": {"identifier": "geometry.warclub_preview", "texture_width": TEX,
                    "texture_height": TEX},
    "bones": geo_bones}]}
with open("colossal_warclub_preview.geo.json", "w", encoding="utf-8") as f:
    json.dump(geo, f, indent=2)

print(f"wrote colossal_warclub_in_hand.json ({len(elements)} elements), "
      f"colossal_warclub_3d.png ({TEX}x{TEX}), colossal_warclub_preview.geo.json")
