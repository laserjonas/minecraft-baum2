"""Rebuilds spider_queen.geo.json + spider_queen.png.

GEOMETRY: faithfully matches VANILLA's real SpiderEntityModel proportions (user feedback: earlier
bespoke geometry "doesn't look like a spider" - so the standard Minecraft spider skeleton is kept,
transcribed from the actual decompiled SpiderEntityModel.java), PLUS two fang bones (chelicerae)
parented to the head - additive detail, not a reshape of the vanilla silhouette. Head stays a
plain cube (two corner-chamfer attempts both failed on aesthetics, see
docs/spider-queen-fable-handoff.md - the menace now comes from the painted face, not the shape).

TEXTURE: a real pixel-art atlas at 2 px per model unit with per-face painting (chitin shading,
plate seams, a structured jumping-spider eye layout painted on the face, banded legs with dark
claw tips, the abdomen's organic red blotch marking kept from the previous version - it was the
one element that never drew negative feedback). This replaces the old uniform 12x12 noisy-fill
cells, which produced the "grey box with legs" look (and had a real bug: every leg face sampled
the cell's top highlight strip, washing the legs out). The old protruding eye-cubes and the
full-black face plate are gone - eyes are painted into the face at native pixel resolution now.

Verify visually BEFORE building: `python tools/render_geckolib_preview.py` renders this
script's outputs to a PNG contact sheet using GeckoLib's own confirmed transform - no more blind
iteration (see docs/spider-queen-fable-handoff.md for why that matters).

Run from the repo root: `python tools/gen_spider_queen.py` (requires Pillow). It writes
spider_queen.geo.json and spider_queen.png into the CURRENT working directory - copy them into
src/main/resources/assets/baum2/geckolib/models/entity/spider_queen.geo.json and
src/main/resources/assets/baum2/textures/entity/spider_queen.png respectively, then
`./gradlew build`.

Coordinate conversion (vanilla ModelPart space -> Bedrock/GeckoLib space), verified via
Bedrock Wiki + JOML source, not assumed:
- Bedrock: +Y up from the feet. Vanilla ModelPart: +Y DOWN from each part's own pivot (legacy
  convention). The only difference is a single Y-axis reflection (Y' = GROUND - Y) - confirmed
  both systems otherwise share the same X/Z axes and "-Z is front" convention, and the same
  rotation *order* (both apply Rz * Ry * Rx to a point).
- Reflecting a single axis (Y) turns a right-handed system left-handed, so any rotation whose
  plane involves Y must have its angle NEGATED in the reflected frame: pitch -> -pitch,
  yaw -> yaw (unchanged), roll -> -roll.

IMPORTANT for whoever edits this next (see docs/spider-queen-fable-handoff.md for the full
context): the coordinate/rotation math below (GROUND reflection, bedrock_pivot, fix_origin_x,
the leg rotation formula, and the self-check at the bottom) is verified correct against
GeckoLib's actual confirmed source behavior. If you add NEW geometry (a new bone), reuse
bedrock_pivot()/fix_origin_x() for its position, prefer a new BONE with its own rotation over
per-cube rotation (per-cube is a different GeckoLib code path that produced a broken result
once), and re-run this script's self-check + the preview renderer before shipping.
"""
import itertools
import json
import math
import random
from PIL import Image, ImageDraw

random.seed(20260706)

# ============================================================================
# Palette. Primary body color pinned to #898989 per direct user spec. ONE red family
# (Widow Red #FF3B1E +- brightness) reused for eyes, abdomen marking, and the small leg-joint
# accents - do not reintroduce a second red family (explicitly rejected earlier).
# ============================================================================
GREY_MID = (0x89, 0x89, 0x89)     # primary, exact hex as specified (head + front body)
ABD_BASE = (0x6E, 0x6E, 0x72)     # abdomen: one step darker so the rear reads heavy
GREY_LEG = (0x5A, 0x5A, 0x5D)     # legs: darkest large surface -> silhouette grounds the body
GREY_LIGHT = (0xB5, 0xB5, 0xB8)
PLATE_DARK = (0x3A, 0x3A, 0x3E)   # joints, fangs, plate seams
NEAR_BLACK = (0x22, 0x20, 0x24)   # claws, sockets, deep shadow
BONE_PALE = (0xC6, 0xC2, 0xBA)    # fang tips (neutral grey-bone, not a new hue family)
WIDOW_RED = (0xFF, 0x3B, 0x1E)    # the one red
RED_HI = tuple(min(255, c + 70) for c in WIDOW_RED)
RED_LO = tuple(max(0, c - 60) for c in WIDOW_RED)
RED_DEEP = tuple(max(0, c - 110) for c in WIDOW_RED)

PIX = 2  # texture pixels per model unit


# ============================================================================
# Atlas: simple shelf packer + per-region pixel painters
# ============================================================================
class Atlas:
    def __init__(self, width=128, height=256):
        self.img = Image.new("RGBA", (width, height), (0, 0, 0, 0))
        self.draw = ImageDraw.Draw(self.img)
        self.w = width
        self.x = 0
        self.y = 0
        self.row_h = 0
        self.used_h = 0

    def alloc(self, w, h):
        if self.x + w > self.w:
            self.x = 0
            self.y += self.row_h
            self.row_h = 0
        rect = (self.x, self.y, w, h)
        self.x += w
        self.row_h = max(self.row_h, h)
        self.used_h = max(self.used_h, self.y + h)
        return rect

    def crop(self):
        h = self.used_h
        self.img = self.img.crop((0, 0, self.w, h))
        return self.w, h


atlas = Atlas()


def px(rect, x, y, color):
    atlas.draw.point((rect[0] + x, rect[1] + y), fill=color + (255,))


def fill_noise(rect, base, jitter=7, vgrad=(1.06, 0.88)):
    """Noisy chitin fill with a vertical light->dark gradient (baked top-light shading)."""
    x0, y0, w, h = rect
    top, bottom = vgrad
    for yy in range(h):
        mul = top + (bottom - top) * (yy / max(1, h - 1))
        for xx in range(w):
            j = random.randint(-jitter, jitter)
            c = tuple(max(0, min(255, int(ch * mul) + j)) for ch in base)
            atlas.draw.point((x0 + xx, y0 + yy), fill=c + (255,))


def speckle(rect, color, density=0.06):
    x0, y0, w, h = rect
    for yy in range(h):
        for xx in range(w):
            if random.random() < density:
                atlas.draw.point((x0 + xx, y0 + yy), fill=color + (255,))


def hline(rect, yy, color, x_from=0, x_to=None):
    x0, y0, w, h = rect
    if x_to is None:
        x_to = w - 1
    atlas.draw.line([(x0 + x_from, y0 + yy), (x0 + x_to, y0 + yy)], fill=color + (255,))


def rect_fill(rect, x, y, w, h, color):
    x0, y0 = rect[0] + x, rect[1] + y
    atlas.draw.rectangle([x0, y0, x0 + w - 1, y0 + h - 1], fill=color + (255,))


def uv(rect):
    return {"uv": [rect[0], rect[1]], "uv_size": [rect[2], rect[3]]}


def mirrored_copy(rect):
    """Allocate a new region containing a horizontal mirror of an existing one."""
    x0, y0, w, h = rect
    out = atlas.alloc(w, h)
    flipped = atlas.img.crop((x0, y0, x0 + w, y0 + h)).transpose(Image.FLIP_LEFT_RIGHT)
    atlas.img.paste(flipped, (out[0], out[1]))
    return out


# ============================================================================
# Region painters (all sizes in texture px = model units * PIX)
# ============================================================================
def paint_eye(rect, x, y, size, bright=True):
    """A readable glowing eye: pure-black socket rim, hot red body, near-white-hot core."""
    rect_fill(rect, x - 1, y - 1, size + 2, size + 2, (0x0A, 0x08, 0x0A))   # socket
    rect_fill(rect, x, y, size, size, WIDOW_RED)
    if size >= 3:
        rect_fill(rect, x, y + size - 1, size, 1, RED_LO)      # dark lower lid edge
        rect_fill(rect, x + 1, y + 1, 2, 2, RED_HI)            # hot core
        px(rect, x + 1, y + 1, (0xFF, 0xC8, 0xB4))             # glint
    elif size == 2:
        px(rect, x, y, RED_HI if bright else WIDOW_RED)
        px(rect, x + 1, y + 1, RED_LO)
    else:
        px(rect, x, y, WIDOW_RED)


def paint_head_front():
    """16x16. Structured jumping-spider face: dark eye band across the upper half with a
    2-large + 2-medium + 2-small eye arrangement, chitin below with mandible creases."""
    r = atlas.alloc(16, 16)
    fill_noise(r, GREY_MID)
    # brow ridge highlight above the band
    hline(r, 1, tuple(min(255, c + 25) for c in GREY_MID))
    # eye band: dark, with a slightly irregular lower edge so it isn't a hard rectangle
    for xx in range(16):
        band_bottom = 7 + (1 if xx % 5 == 2 else 0)
        for yy in range(2, band_bottom + 1):
            j = random.randint(-4, 4)
            c = tuple(max(0, min(255, ch + j)) for ch in (0x2B, 0x27, 0x29))
            atlas.draw.point((r[0] + xx, r[1] + yy), fill=c + (255,))
    # primary pair (large, centered) - the predatory stare
    paint_eye(r, 3, 3, 4)
    paint_eye(r, 10, 3, 4)
    # medium outer pair, slightly lower, hugging the face edge (continues onto the side faces)
    paint_eye(r, 0, 5, 2)
    paint_eye(r, 14, 5, 2)
    # small top pair, dimmer
    paint_eye(r, 5, 0, 1, bright=False)
    paint_eye(r, 10, 0, 1, bright=False)
    # clypeus: thin light edge right under the band (band casts a "shadow" -> depth)
    hline(r, 9, tuple(min(255, c + 18) for c in GREY_MID))
    # mandible creases + slightly darker mouth area between them
    mouth = tuple(max(0, c - 22) for c in GREY_MID)
    for yy in range(11, 16):
        for xx in range(6, 10):
            j = random.randint(-5, 5)
            c = tuple(max(0, min(255, ch + j)) for ch in mouth)
            atlas.draw.point((r[0] + xx, r[1] + yy), fill=c + (255,))
    for yy in range(10, 16):
        px(r, 5, yy, PLATE_DARK)
        px(r, 10, yy, PLATE_DARK)
    # fang-root shadows at the bottom corners (the fang bones hang right below these)
    rect_fill(r, 1, 13, 3, 3, tuple(max(0, c - 30) for c in GREY_MID))
    rect_fill(r, 12, 13, 3, 3, tuple(max(0, c - 30) for c in GREY_MID))
    return r


def paint_head_top():
    """16x16. Carapace: median fovea groove + radiating lines. Texture TOP = model BACK
    (up-face V axis runs back->front), so the brow shading sits at the region bottom."""
    r = atlas.alloc(16, 16)
    fill_noise(r, GREY_MID, vgrad=(1.10, 1.0))
    groove = tuple(max(0, c - 45) for c in GREY_MID)
    rect_fill(r, 7, 5, 2, 6, groove)
    for dx, dy in ((-3, -2), (3, -2), (-4, 1), (4, 1)):
        px(r, 7 + dx, 7 + dy, groove)
        px(r, 8 + dx, 8 + dy, groove)
    hline(r, 14, tuple(max(0, c - 25) for c in GREY_MID))
    hline(r, 15, tuple(max(0, c - 40) for c in GREY_MID))
    return r


def paint_head_side_east():
    """16x16 for the EAST face, where texture-RIGHT = model FRONT: lateral eyes near the right
    edge. The west face gets a mirrored copy (its front is texture-LEFT)."""
    r = atlas.alloc(16, 16)
    fill_noise(r, GREY_MID)
    # dark cheek band continuing the front eye band around the corner
    for xx in range(10, 16):
        for yy in range(2, 8):
            j = random.randint(-4, 4)
            c = tuple(max(0, min(255, ch + j)) for ch in (0x2B, 0x27, 0x29))
            atlas.draw.point((r[0] + xx, r[1] + yy), fill=c + (255,))
    paint_eye(r, 13, 3, 2)           # lateral eye
    paint_eye(r, 11, 6, 1, bright=False)
    # jaw seam
    for xx in range(3, 13):
        px(r, xx, 11 + (xx % 4 == 0), PLATE_DARK)
    speckle(r, tuple(max(0, c - 30) for c in GREY_MID), 0.05)
    return r


def paint_plain(w, h, base, vgrad=(1.04, 0.9), dens=0.05):
    r = atlas.alloc(w, h)
    fill_noise(r, base, vgrad=vgrad)
    speckle(r, tuple(max(0, c - 30) for c in base), dens)
    return r


def paint_body0_top():
    """12x12 cephalothorax carapace: fovea dot + grooves."""
    r = atlas.alloc(12, 12)
    fill_noise(r, GREY_MID, vgrad=(1.10, 0.98))
    groove = tuple(max(0, c - 45) for c in GREY_MID)
    rect_fill(r, 5, 5, 2, 2, groove)
    for dx, dy in ((-2, -3), (2, -3), (-3, 2), (3, 2)):
        px(r, 5 + dx, 5 + dy, groove)
    return r


def blotch(draw, w, h, cx_f, cy_f, rx_f, ry_f, color, n=9, jitter_f=0.08):
    """Organic jittered blob onto an overlay image, coordinates as fractions of the overlay size
    (ports the previous version's proven abdomen-marking recipe; reference for shape only - a
    real garden spider's mottled marking - colors are this project's own)."""
    cx, cy = w * cx_f, h * cy_f
    jit = jitter_f * min(w, h)
    pts = []
    for i in range(n):
        ang = 2 * math.pi * i / n
        rr_x = w * rx_f + random.uniform(-jit, jit)
        rr_y = h * ry_f + random.uniform(-jit, jit)
        pts.append((cx + math.cos(ang) * rr_x, cy + math.sin(ang) * rr_y))
    draw.polygon(pts, fill=color + (255,))


def symmetric_marking(rect, paint_fn):
    """Draw a marking via paint_fn(draw, w, h) on a transparent overlay, then composite the
    UNION of the overlay and its horizontal mirror onto the atlas - guarantees the marking is
    bilaterally symmetric (organic-jittered but biological-looking, not random splatter)."""
    x0, y0, w, h = rect
    overlay = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    paint_fn(ImageDraw.Draw(overlay), w, h)
    both = Image.alpha_composite(overlay.transpose(Image.FLIP_LEFT_RIGHT), overlay)
    atlas.img.alpha_composite(both, (x0, y0))
    atlas.draw = ImageDraw.Draw(atlas.img)


def paint_abd_top():
    """20x24 abdomen top. Texture TOP = model REAR. The organic red marking - the one visual
    element that has never drawn negative feedback - larger toward the rear."""
    r = atlas.alloc(20, 24)
    fill_noise(r, ABD_BASE, vgrad=(1.08, 0.92))
    speckle(r, tuple(max(0, c - 25) for c in ABD_BASE), 0.07)

    def marking(d, w, h):
        blotch(d, w, h, 0.5, 0.26, 0.17, 0.13, WIDOW_RED)      # big rear blob
        blotch(d, w, h, 0.5, 0.55, 0.12, 0.13, WIDOW_RED)      # middle of the spine
        blotch(d, w, h, 0.5, 0.80, 0.08, 0.07, RED_LO)         # tapering toward the waist
        blotch(d, w, h, 0.24, 0.38, 0.08, 0.09, RED_LO)        # side lobe (auto-mirrored)
        for fy in (0.24, 0.50, 0.72):                          # spine highlight dots
            d.point((w // 2, int(h * fy)), fill=RED_HI + (255,))
            d.point((w // 2, int(h * fy) + 1), fill=RED_HI + (255,))
    symmetric_marking(r, marking)
    return r


def paint_abd_rear():
    """20x16 rear face: the marking wraps down the back."""
    r = atlas.alloc(20, 16)
    fill_noise(r, ABD_BASE, vgrad=(1.0, 0.86))
    speckle(r, tuple(max(0, c - 25) for c in ABD_BASE), 0.07)

    def marking(d, w, h):
        blotch(d, w, h, 0.5, 0.28, 0.15, 0.18, WIDOW_RED)
        blotch(d, w, h, 0.26, 0.55, 0.08, 0.11, RED_LO)        # side lobe (auto-mirrored)
        d.point((w // 2, int(h * 0.3)), fill=RED_HI + (255,))
    symmetric_marking(r, marking)
    # spinneret hint at the bottom
    rect_fill(r, 8, 13, 4, 2, tuple(max(0, c - 35) for c in ABD_BASE))
    return r


def paint_abd_side():
    """24x16 flank: faint diagonal hair striping + two satellite dots of the top marking."""
    r = atlas.alloc(24, 16)
    fill_noise(r, ABD_BASE, vgrad=(1.05, 0.88))
    stripe = tuple(max(0, c - 22) for c in ABD_BASE)
    for i in range(4):
        for t in range(9):
            xx, yy = 3 + i * 6 + t // 2, 4 + t
            if 0 <= xx < 24 and 0 <= yy < 16:
                px(r, xx, yy, stripe)
    speckle(r, tuple(max(0, c - 30) for c in ABD_BASE), 0.06)
    # two small satellite dashes of the top marking, spilling over the top edge
    rect_fill(r, 8, 0, 3, 2, RED_LO)
    px(r, 9, 0, WIDOW_RED)
    rect_fill(r, 14, 0, 2, 2, RED_LO)
    return r


def paint_leg_strip():
    """32x4 leg side. Longitudinally symmetric (dark claw/coxa at BOTH ends, joint rings at
    1/3 and 2/3) so the same region is valid for every face's U direction on both mirrored
    leg cuboids - per-face U orientation flips between north/south faces, so an asymmetric
    design would render reversed on half the faces."""
    r = atlas.alloc(32, 4)
    fill_noise(r, GREY_LEG, vgrad=(1.12, 0.82))
    femur = tuple(min(255, c + 12) for c in GREY_LEG)
    for xx in range(12, 20):                      # slightly lighter middle segment
        for yy in range(4):
            j = random.randint(-6, 6)
            c = tuple(max(0, min(255, ch + j)) for ch in femur)
            atlas.draw.point((r[0] + xx, r[1] + yy), fill=c + (255,))
    for x0 in (10, 21):                           # joint rings
        rect_fill(r, x0, 0, 2, 4, PLATE_DARK)
        px(r, x0, 3, NEAR_BLACK)
    rect_fill(r, 0, 0, 3, 4, NEAR_BLACK)          # claw ends
    rect_fill(r, 29, 0, 3, 4, NEAR_BLACK)
    px(r, 1, 1, PLATE_DARK)
    px(r, 30, 1, PLATE_DARK)
    speckle(r, tuple(max(0, c - 35) for c in GREY_LEG), 0.10)   # bristles
    return r


def paint_fang_side():
    """3x8, texture BOTTOM = fang tip: dark chitin shading into a pale piercing tip."""
    r = atlas.alloc(3, 8)
    fill_noise(r, PLATE_DARK, jitter=5, vgrad=(1.1, 0.8))
    rect_fill(r, 0, 5, 3, 1, tuple(max(0, c - 15) for c in PLATE_DARK))
    rect_fill(r, 0, 6, 3, 2, BONE_PALE)
    px(r, 1, 7, tuple(min(255, c + 25) for c in BONE_PALE))
    px(r, 0, 6, tuple(max(0, c - 40) for c in BONE_PALE))
    return r


# ---- paint all regions ----
R_HEAD_FRONT = paint_head_front()
R_HEAD_TOP = paint_head_top()
R_HEAD_SIDE_E = paint_head_side_east()
R_HEAD_SIDE_W = mirrored_copy(R_HEAD_SIDE_E)
R_HEAD_BACK = paint_plain(16, 16, tuple(max(0, c - 12) for c in GREY_MID))
R_HEAD_DOWN = paint_plain(16, 16, tuple(max(0, c - 45) for c in GREY_MID), vgrad=(1.0, 1.0))
R_BODY0_TOP = paint_body0_top()
R_BODY0_SIDE = paint_plain(12, 12, GREY_MID)
R_BODY0_DOWN = paint_plain(12, 12, tuple(max(0, c - 45) for c in GREY_MID), vgrad=(1.0, 1.0))
R_ABD_TOP = paint_abd_top()
R_ABD_REAR = paint_abd_rear()
R_ABD_SIDE = paint_abd_side()
R_ABD_FRONT = paint_plain(20, 16, tuple(max(0, c - 15) for c in ABD_BASE))
R_ABD_DOWN = paint_plain(20, 24, tuple(max(0, c - 40) for c in ABD_BASE), vgrad=(1.0, 1.0))
R_LEG_STRIP = paint_leg_strip()
R_LEG_END = paint_plain(4, 4, NEAR_BLACK, vgrad=(1.0, 1.0), dens=0.0)
R_FANG_SIDE = paint_fang_side()
R_FANG_END = paint_plain(3, 3, PLATE_DARK, vgrad=(1.0, 1.0), dens=0.0)

TEX_W, TEX_H = atlas.crop()


GROUND = None  # filled in once every point is known, see the "Pass 1" block below


# ============================================================================
# Vanilla data, transcribed verbatim from SpiderEntityModel.getTexturedModelData()
# ============================================================================
# (pivot_x, pivot_y, pivot_z), local cuboid (ox,oy,oz,dx,dy,dz)
HEAD = {"pivot": (0.0, 15.0, -3.0), "cuboid": (-4.0, -4.0, -8.0, 8.0, 8.0, 8.0)}
BODY0 = {"pivot": (0.0, 15.0, 0.0), "cuboid": (-3.0, -3.0, -3.0, 6.0, 6.0, 6.0)}
BODY1 = {"pivot": (0.0, 15.0, 9.0), "cuboid": (-5.0, -4.0, -6.0, 10.0, 8.0, 12.0)}

PI = math.pi
# name -> (pivot, mirrored?, yaw_rad, roll_rad)  [pitch is 0 for every vanilla spider leg]
LEGS = {
    "right_hind_leg": ((-4.0, 15.0, 2.0), False, PI / 4, -PI / 4),
    "left_hind_leg": ((4.0, 15.0, 2.0), True, -PI / 4, PI / 4),
    "right_middle_hind_leg": ((-4.0, 15.0, 1.0), False, PI / 8, -0.58119464),
    "left_middle_hind_leg": ((4.0, 15.0, 1.0), True, -PI / 8, 0.58119464),
    "right_middle_front_leg": ((-4.0, 15.0, 0.0), False, -PI / 8, -0.58119464),
    "left_middle_front_leg": ((4.0, 15.0, 0.0), True, PI / 8, 0.58119464),
    "right_front_leg": ((-4.0, 15.0, -1.0), False, -PI / 4, -PI / 4),
    "left_front_leg": ((4.0, 15.0, -1.0), True, PI / 4, PI / 4),
}
# Unmirrored local cuboid: origin(-15,-1,-1) size(16,2,2). Mirrored: origin(-1,-1,-1) size(16,2,2).
LEG_CUBOID_RIGHT = (-15.0, -1.0, -1.0, 16.0, 2.0, 2.0)
LEG_CUBOID_LEFT = (-1.0, -1.0, -1.0, 16.0, 2.0, 2.0)

# Fangs (chelicerae) - NEW, not vanilla: hang from the head's front-bottom edge, one bone each
# so they can animate (idle chew, wind-up flare). Authored in the same vanilla-space convention
# as everything else. Vanilla Y is DOWN: head bottom edge is abs y 19, front face z -11.
FANG_SIZE = (1.5, 4.0, 1.5)
# name -> (vanilla pivot at the fang's top hinge, splay roll degrees in authored JSON terms)
FANGS = {
    "right_fang": ((-2.0, 18.6, -10.1), -10.0),
    "left_fang": ((2.0, 18.6, -10.1), 10.0),
}


def rotate_yaw_roll(point, yaw, roll):
    """Applies vanilla's Rz(roll) * Ry(yaw) to a point (pitch is 0 for all spider legs, so the
    Rx term is identity and omitted), matching JOML's rotationZYX(roll, yaw, 0) exactly."""
    x, y, z = point
    x1 = x * math.cos(yaw) + z * math.sin(yaw)
    z1 = -x * math.sin(yaw) + z * math.cos(yaw)
    y1 = y
    x2 = x1 * math.cos(roll) - y1 * math.sin(roll)
    y2 = x1 * math.sin(roll) + y1 * math.cos(roll)
    z2 = z1
    return (x2, y2, z2)


def leg_local_corners(unmirrored):
    ox, oy, oz, dx, dy, dz = LEG_CUBOID_RIGHT if unmirrored else LEG_CUBOID_LEFT
    corners = []
    for cx in (ox, ox + dx):
        for cy in (oy, oy + dy):
            for cz in (oz, oz + dz):
                corners.append((cx, cy, cz))
    return corners


# ---- Pass 1: find GROUND (lowest point in vanilla-space = the largest raw Y, since vanilla Y
# increases downward) across every relevant point, so the Bedrock model's feet sit near Y=0. ----
max_vanilla_y = -1e9
for name, (pivot, mirrored, yaw, roll) in LEGS.items():
    for corner in leg_local_corners(not mirrored):
        rx, ry, rz = rotate_yaw_roll(corner, yaw, roll)
        abs_y = pivot[1] + ry
        max_vanilla_y = max(max_vanilla_y, abs_y)
for part in (HEAD, BODY0, BODY1):
    py = part["pivot"][1]
    oy, dy = part["cuboid"][1], part["cuboid"][4]
    max_vanilla_y = max(max_vanilla_y, py + oy, py + oy + dy)

GROUND = max_vanilla_y
print(f"GROUND (vanilla-space Y of lowest point) = {GROUND:.3f}")

# ============================================================================
# Build bones
# ============================================================================
bones = []


def bedrock_pivot(pivot):
    # GeckoLib negates X at JSON-load time for every bone pivot (confirmed from the real
    # BakedModelFactory.constructBone source: "(float)-pivot.x"). Pre-negating here cancels
    # that out so the authored X ends up matching the intended absolute position.
    px, py, pz = pivot
    return [-px, GROUND - py, pz]


def fix_origin_x(origin, size):
    """Same X-negation fix as bedrock_pivot, but for a cube's min-corner "origin" + "size" - since
    negating a MIN-corner X value produces the new MAX, this recomputes the new min correctly
    (new_min_x = -(old_min_x + size_x)) rather than just flipping the sign in place."""
    return [-(origin[0] + size[0]), origin[1], origin[2]]


def make_cube(origin, size, faces):
    """faces: dict face-name -> region rect."""
    return {"origin": [round(v, 3) for v in origin], "size": [round(v, 3) for v in size],
            "uv": {f: uv(r) for f, r in faces.items()}}


def part_cube(part, faces):
    px_, py_, pz_ = part["pivot"]
    ox, oy, oz, dx, dy, dz = part["cuboid"]
    origin = [px_ + ox, GROUND - (py_ + oy + dy), pz_ + oz]
    size = [dx, dy, dz]
    return make_cube(fix_origin_x(origin, size), size, faces)


bones.append({
    "name": "body0", "pivot": bedrock_pivot(BODY0["pivot"]),
    "cubes": [part_cube(BODY0, {
        "north": R_BODY0_SIDE, "south": R_BODY0_SIDE, "east": R_BODY0_SIDE,
        "west": R_BODY0_SIDE, "up": R_BODY0_TOP, "down": R_BODY0_DOWN})],
})

bones.append({
    "name": "body1", "pivot": bedrock_pivot(BODY1["pivot"]),
    "cubes": [part_cube(BODY1, {
        "north": R_ABD_FRONT, "south": R_ABD_REAR, "east": R_ABD_SIDE,
        "west": R_ABD_SIDE, "up": R_ABD_TOP, "down": R_ABD_DOWN})],
})

bones.append({
    "name": "head", "pivot": bedrock_pivot(HEAD["pivot"]),
    "cubes": [part_cube(HEAD, {
        "north": R_HEAD_FRONT, "south": R_HEAD_BACK, "east": R_HEAD_SIDE_E,
        "west": R_HEAD_SIDE_W, "up": R_HEAD_TOP, "down": R_HEAD_DOWN})],
})

# Head corner-rounding: intentionally NOT attempted - see docs/spider-queen-fable-handoff.md.
# Two prior attempts both still looked wrong in-game. The head is a plain cube; the painted
# face + fangs carry the menace instead.

# Fangs: children of the head bone (they follow head-turn and head animation automatically).
# Cube origins/pivots stay in absolute model space - Bedrock hierarchy only chains transforms.
for name, ((fpx, fpy, fpz), splay_deg) in FANGS.items():
    sx, sy, sz = FANG_SIZE
    # vanilla-space cuboid: top overlaps 0.8 into the head so the hinge is hidden
    origin_van = [fpx - sx / 2, fpy - 0.8, fpz - sz / 2]
    # bedrock origin: min-corner y = GROUND - (top + height)
    origin = [origin_van[0], GROUND - (origin_van[1] + sy), origin_van[2]]
    size = [sx, sy, sz]
    bones.append({
        "name": name, "parent": "head", "pivot": bedrock_pivot((fpx, fpy, fpz)),
        "rotation": [0.0, 0.0, splay_deg],
        "cubes": [make_cube(fix_origin_x(origin, size), size, {
            "north": R_FANG_SIDE, "south": R_FANG_SIDE, "east": R_FANG_SIDE,
            "west": R_FANG_SIDE, "up": R_FANG_END, "down": R_FANG_END})],
    })

for name, (pivot, mirrored, yaw, roll) in LEGS.items():
    ox, oy, oz, dx, dy, dz = LEG_CUBOID_RIGHT if not mirrored else LEG_CUBOID_LEFT
    origin = [ox, -(oy + dy), oz]  # local (pre-rotation) authoring, pivot-relative, Y flipped
    size = [dx, dy, dz]
    px_, py_, pz_ = pivot
    bedrock_piv = bedrock_pivot(pivot)
    # absolute cube origin = pivot + local offset (Bedrock cubes are authored in absolute space,
    # pre-rotation, then the whole bone rotates around its own pivot) - X fixed via fix_origin_x
    abs_origin_prefix = [px_ + origin[0], GROUND - py_ + origin[1], pz_ + origin[2]]
    abs_origin = fix_origin_x(abs_origin_prefix, size)
    bones.append({
        "name": name, "pivot": bedrock_piv,
        "rotation": [round(math.degrees(-0.0), 3), round(-math.degrees(yaw), 3), round(math.degrees(-roll), 3)],
        "cubes": [make_cube(abs_origin, size, {
            "north": R_LEG_STRIP, "south": R_LEG_STRIP, "up": R_LEG_STRIP,
            "down": R_LEG_STRIP, "east": R_LEG_END, "west": R_LEG_END})],
    })


# ============================================================================
# Self-check: simulates GeckoLib's ACTUAL, CONFIRMED load/render transform - read directly from
# the real BakedModelFactory.constructBone/constructCube source (see
# docs/fabric-modding.md's "GeckoLib integration" section, part H, for the full derivation and
# why an EARLIER version of this self-check passed with zero error while still being WRONG - it
# validated internal consistency, not correctness against GeckoLib's real behavior).
#
# ALWAYS re-run this after any change to bone pivots, cube origins, or rotations, and confirm it
# still PASSes before trusting the visual result. It CANNOT catch pure aesthetic problems (wrong
# color, wrong size, ugly shape) - only "is the geometry where I think it is."
# ============================================================================
def rotate_std(point, rx_deg, ry_deg, rz_deg):
    rx, ry, rz = map(math.radians, (rx_deg, ry_deg, rz_deg))
    x, y, z = point
    y1 = y * math.cos(rx) - z * math.sin(rx)
    z1 = y * math.sin(rx) + z * math.cos(rx)
    x1 = x
    x2 = x1 * math.cos(ry) + z1 * math.sin(ry)
    z2 = -x1 * math.sin(ry) + z1 * math.cos(ry)
    y2 = y1
    x3 = x2 * math.cos(rz) - y2 * math.sin(rz)
    y3 = x2 * math.sin(rz) + y2 * math.cos(rz)
    z3 = z2
    return (x3, y3, z3)


max_error = 0.0
for name, (pivot, mirrored, yaw, roll) in LEGS.items():
    bone = next(b for b in bones if b["name"] == name)
    json_pivot = bone["pivot"]
    rx_json, ry_json, rz_json = bone["rotation"]
    cube = bone["cubes"][0]
    json_origin = cube["origin"]
    json_size = cube["size"]
    ox, oy, oz, dx, dy, dz = LEG_CUBOID_RIGHT if not mirrored else LEG_CUBOID_LEFT

    targets = []
    reconstructed = []
    for cx, cy, cz in itertools.product((0, dx), (0, dy), (0, dz)):
        local_corner = (ox + cx, oy + cy, oz + cz)
        vx, vy, vz = rotate_yaw_roll(local_corner, yaw, roll)
        vanilla_abs = (pivot[0] + vx, pivot[1] + vy, pivot[2] + vz)
        targets.append((vanilla_abs[0], GROUND - vanilla_abs[1], vanilla_abs[2]))

        # GeckoLib's real cube-origin transform is size-aware and gives a DIFFERENT result for
        # the "near" (offset 0) vs "far" (offset size) end of each axis - apply it per corner.
        internal_x = -(json_origin[0] + json_size[0]) if cx == 0 else -json_origin[0]
        internal_corner = (internal_x, json_origin[1] + cy, json_origin[2] + cz)
        internal_pivot = (-json_pivot[0], json_pivot[1], json_pivot[2])
        L = tuple(a - b for a, b in zip(internal_corner, internal_pivot))
        pitch_v, yaw_v, roll_v = -rx_json, -ry_json, rz_json
        Lrot = rotate_std(L, pitch_v, yaw_v, roll_v)
        reconstructed.append(tuple(a + b for a, b in zip(internal_pivot, Lrot)))

    for t in targets:
        nearest = min(math.dist(t, r) for r in reconstructed)
        max_error = max(max_error, nearest)

print(f"Self-check (full 8-corner box match vs. GeckoLib's real confirmed transform) max error: "
      f"{max_error:.6f} units ({'PASS' if max_error < 1e-3 else 'FAIL - DO NOT TRUST THIS GEOMETRY'})")
# Tolerance is 1e-3, not 1e-6: bone/cube coordinates are rounded to 3 decimals by make_cube
# before this check re-reads them back out of `bones`, so a small rounding residual (order
# 1e-4) is expected and not a sign of an actual formula error.
assert max_error < 1e-3, "Rotation sign/order derivation is wrong - stopping before writing bad geometry"

geo = {
    "format_version": "1.12.0",
    "minecraft:geometry": [
        {
            "description": {
                "identifier": "geometry.spider_queen",
                "texture_width": TEX_W,
                "texture_height": TEX_H,
                "visible_bounds_width": 4,
                "visible_bounds_height": 3,
                "visible_bounds_offset": [0, 1, 0],
            },
            "bones": bones,
        }
    ],
}

with open("spider_queen.geo.json", "w", encoding="utf-8") as f:
    json.dump(geo, f, indent=2)
atlas.img.save("spider_queen.png")
print(f"bones: {len(bones)}, cubes: {sum(len(b['cubes']) for b in bones)}")
print(f"texture: {TEX_W}x{TEX_H}")
