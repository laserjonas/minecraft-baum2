"""Builds zombie_colossus.geo.json + zombie_colossus.png (GeckoLib migration of the boss).

GEOMETRY: bespoke muscular giant-zombie biped, authored directly in this file (no vanilla
transcription needed - unlike the Spider Queen work that pioneered this pipeline, the shape is
original; correctness is verified visually via tools/render_geckolib_preview.py, which
implements GeckoLib's exact confirmed transform). Design goals from the brief: "giant zombie
with big muscle" - boulder shoulders, oversized gorilla forearms, hunched upper back, underbite
jaw, thick pillar legs, and the Colossal Warclub modeled as REAL GEOMETRY (a bone parented to
the right forearm) instead of the old scaled held-item render, so attack/earthquake animations
swing an actual club with weight.

Two-segment arms (shoulder bone -> forearm child -> club child) exist specifically so the
animations can bend elbows - single-stick limbs were a big part of why the old model read
stiff/"static".

TEXTURE: pixel-art atlas at 2 px per model unit, per-face painting, using the RATIFIED "Ashen
Brute" palette from docs/visual-style-guide.md Section 18.3 (ashen skin head/back, exposed
red-brown musculature torso/arms, tattered leg wraps, dull amber eye glow) and Section 18.4's
"Colossal Warclub" wood/stud palette for the club - the palettes are pinned decisions; this
rework changes the SHAPES and the paint quality, not the color identity.

Bone-name contract (referenced by zombie_colossus.animation.json and ZombieColossusEntity's
animation controller): body, head (GeckoLib auto head-turn target), right_arm, right_forearm,
club, left_arm, left_forearm, right_leg, left_leg.

Run from the repo root: `python tools/gen_zombie_colossus.py` (requires Pillow), preview with
`python tools/render_geckolib_preview.py --model zombie_colossus`, then copy the outputs to:
  src/main/resources/assets/baum2/geckolib/models/entity/zombie_colossus.geo.json
  src/main/resources/assets/baum2/textures/entity/zombie_colossus.png

Coordinate conventions: authored space has +Y up (feet at 0), -Z front, +X = the entity's
LEFT. The pivot()/origin() helpers apply the same X-negation GeckoLib cancels at load time
(same verified mechanism as tools/gen_spider_queen.py's bedrock_pivot/fix_origin_x - see that
file and docs/fabric-modding.md part H; do not hand-negate anything).
"""
import json
import random
from PIL import Image, ImageDraw

random.seed(20260706)

# ============================================================================
# Palette - pinned in docs/visual-style-guide.md 18.3 (Ashen Brute) / 18.4 (Colossal Warclub)
# ============================================================================
HIDE_SHADOW = (0x33, 0x2C, 0x22)
HIDE = (0x5C, 0x51, 0x42)
HIDE_PALE = (0x7D, 0x71, 0x5C)
MUSCLE = (0x7A, 0x2E, 0x24)
MUSCLE_SHEEN = (0xB2, 0x4A, 0x3A)
MUSCLE_DUSK = (0x4A, 0x17, 0x12)
WOUND = (0x1F, 0x1A, 0x16)
GLARE = (0xD9, 0xC2, 0x4A)
SOCKET = (0x14, 0x0F, 0x0A)
WRAP = (0x2E, 0x25, 0x1C)
WRAP_FOLD = (0x4A, 0x3C, 0x2C)
WRAP_DUSK = (0x18, 0x0F, 0x09)
BONE = (0xD8, 0xCF, 0xC0)
# club (18.4)
WOOD_DARK = (0x3E, 0x2A, 0x1A)
WOOD_MID = (0x5A, 0x3D, 0x24)
GRIP = (0x24, 0x18, 0x0F)
CLUB_BASE = (0x6B, 0x5A, 0x42)
CLUB_HI = (0x8C, 0x78, 0x54)
CLUB_SHADOW = (0x46, 0x38, 0x23)
STUD = (0x8A, 0x8A, 0x82)
STUD_SHADOW = (0x4A, 0x4A, 0x44)
SMEAR = (0x6B, 0x2A, 0x1E)

PIX = 2  # texture pixels per model unit


# ============================================================================
# Atlas + painter helpers (same pattern as tools/gen_spider_queen.py - kept as a local copy on
# purpose: these are one-shot generators, and refactoring the spider script would reshuffle its
# random-call sequence and silently change its committed output)
# ============================================================================
class Atlas:
    def __init__(self, width=256, height=256):
        self.img = Image.new("RGBA", (width, height), (0, 0, 0, 0))
        self.draw = ImageDraw.Draw(self.img)
        self.w = width
        self.x = 0
        self.y = 0
        self.row_h = 0
        self.used_h = 0
        self.used_w = 0

    def alloc(self, w, h):
        if self.x + w > self.w:
            self.x = 0
            self.y += self.row_h
            self.row_h = 0
        rect = (self.x, self.y, w, h)
        self.x += w
        self.row_h = max(self.row_h, h)
        self.used_h = max(self.used_h, self.y + h)
        self.used_w = max(self.used_w, self.x)
        return rect

    def crop(self):
        w = self.used_w
        h = self.used_h
        self.img = self.img.crop((0, 0, w, h))
        return w, h


atlas = Atlas()


def px(rect, x, y, color):
    atlas.draw.point((rect[0] + x, rect[1] + y), fill=color + (255,))


def fill_noise(rect, base, jitter=7, vgrad=(1.06, 0.88)):
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


def rect_fill(rect, x, y, w, h, color):
    x0, y0 = rect[0] + x, rect[1] + y
    atlas.draw.rectangle([x0, y0, x0 + w - 1, y0 + h - 1], fill=color + (255,))


def uv(rect):
    return {"uv": [rect[0], rect[1]], "uv_size": [rect[2], rect[3]]}


def mirrored_copy(rect):
    x0, y0, w, h = rect
    out = atlas.alloc(w, h)
    flipped = atlas.img.crop((x0, y0, x0 + w, y0 + h)).transpose(Image.FLIP_LEFT_RIGHT)
    atlas.img.paste(flipped, (out[0], out[1]))
    return out


# ============================================================================
# Surface painters
# ============================================================================
def paint_skin(w, h, scars=1, stitches=0):
    """Ashen dead hide: noisy, sun-bleached, with optional scar lines and crude stitches."""
    r = atlas.alloc(w, h)
    fill_noise(r, HIDE)
    speckle(r, HIDE_SHADOW, 0.07)
    speckle(r, HIDE_PALE, 0.04)
    for _ in range(scars):
        sx, sy = random.randint(1, max(1, w - 6)), random.randint(1, max(1, h - 6))
        for i in range(random.randint(3, 5)):
            px(r, min(w - 1, sx + i), min(h - 1, sy + i), WOUND)
    for _ in range(stitches):
        sx, sy = random.randint(2, max(2, w - 8)), random.randint(2, max(2, h - 4))
        for i in range(5):
            px(r, sx + i, sy, WOUND)
            if i % 2 == 0:
                px(r, sx + i, sy - 1, WOUND)
                px(r, sx + i, sy + 1, WOUND)
    return r


def paint_muscle(w, h, vertical=True):
    """Raw exposed musculature: base red-brown with fiber striations (sheen/dusk streaks)."""
    r = atlas.alloc(w, h)
    fill_noise(r, MUSCLE, vgrad=(1.05, 0.85))
    length, breadth = (h, w) if vertical else (w, h)
    pos = 0
    while pos < breadth - 1:
        band = random.randint(2, 3)
        tone = MUSCLE_SHEEN if random.random() < 0.45 else MUSCLE_DUSK
        for t in range(length):
            off = (t // 5) % 2  # slight wiggle so fibers aren't ruler-straight
            b = pos + off
            if b < breadth:
                if vertical:
                    if random.random() < 0.75:
                        px(r, b, t, tuple((a + c) // 2 for a, c in zip(tone, MUSCLE)))
                else:
                    if random.random() < 0.75:
                        px(r, t, b, tuple((a + c) // 2 for a, c in zip(tone, MUSCLE)))
        pos += band + random.randint(1, 2)
    speckle(r, MUSCLE_DUSK, 0.05)
    return r


def paint_chest_front():
    """34x18 (17w x 9h units): the centerpiece - pecs, sternum groove, torn skin collar."""
    r = atlas.alloc(34, 18)
    fill_noise(r, MUSCLE, vgrad=(1.08, 0.88))
    # torn ashen-skin collar along the top edge (ragged boundary between hide and raw muscle)
    for xx in range(34):
        depth = 1 + (1 if xx % 4 in (1, 2) else 0)
        for yy in range(depth):
            j = random.randint(-6, 6)
            c = tuple(max(0, min(255, ch + j)) for ch in HIDE)
            atlas.draw.point((r[0] + xx, r[1] + yy), fill=c + (255,))
        px(r, xx, depth, WOUND)  # torn edge line
    # pecs: two big chunky masses - raised lighter block, bright top edge, dark under-line
    pec_lit = tuple((a + b) // 2 for a, b in zip(MUSCLE, MUSCLE_SHEEN))
    for x0 in (2, 18):
        for yy in range(4, 11):
            for xx in range(x0, x0 + 14):
                j = random.randint(-6, 6)
                c = tuple(max(0, min(255, ch + j)) for ch in pec_lit)
                atlas.draw.point((r[0] + xx, r[1] + yy), fill=c + (255,))
        rect_fill(r, x0, 4, 14, 1, MUSCLE_SHEEN)      # lit top edge
        rect_fill(r, x0, 10, 14, 1, MUSCLE_DUSK)      # heavy underside
        rect_fill(r, x0 + 2, 11, 10, 1, WOUND)        # crease shadow below the mass
        px(r, x0 + 3, 6, MUSCLE_SHEEN)                # small specular blips
        px(r, x0 + 10, 5, MUSCLE_SHEEN)
    # sternum groove
    rect_fill(r, 16, 4, 2, 13, MUSCLE_DUSK)
    for yy in range(4, 17, 2):
        px(r, 16, yy, WOUND)
    # lower-edge shadow rows (reads as ribcage overhang above the belly)
    for xx in range(0, 34, 2):
        px(r, xx, 17, MUSCLE_DUSK)
    return r


def paint_belly_front():
    """24x14 (12w x 7h units): abs grid with wound-edge striations."""
    r = atlas.alloc(24, 14)
    fill_noise(r, MUSCLE, vgrad=(1.02, 0.86))
    # 2x3 ab grid: highlight top-left of each ab, dark separation lines
    for row in range(3):
        y0 = 1 + row * 4
        for col in range(2):
            x0 = 3 + col * 10
            rect_fill(r, x0, y0, 7, 3, MUSCLE)
            rect_fill(r, x0, y0, 6, 1, MUSCLE_SHEEN)
            px(r, x0, y0 + 1, MUSCLE_SHEEN)
            rect_fill(r, x0, y0 + 3, 7, 1, MUSCLE_DUSK)
        rect_fill(r, 11, y0, 2, 4, MUSCLE_DUSK)  # linea alba
    for yy in range(1, 13, 2):
        px(r, 11, yy, WOUND)
    # dark oblique edges
    for yy in range(14):
        px(r, 0, yy, MUSCLE_DUSK)
        px(r, 1, yy, MUSCLE_DUSK)
        px(r, 22, yy, MUSCLE_DUSK)
        px(r, 23, yy, MUSCLE_DUSK)
    return r


def paint_back():
    """34x18: ashen hide back with spine groove, a torn muscle patch and stitches."""
    r = atlas.alloc(34, 18)
    fill_noise(r, HIDE)
    speckle(r, HIDE_SHADOW, 0.08)
    rect_fill(r, 16, 0, 2, 18, HIDE_SHADOW)          # spine groove
    for yy in range(1, 17, 3):
        px(r, 16, yy, WOUND)
        px(r, 14, yy + 1, HIDE_SHADOW)               # rib hints
        px(r, 19, yy + 1, HIDE_SHADOW)
    # torn patch lower-left revealing muscle
    for yy in range(10, 17):
        for xx in range(3, 11 - (yy - 10) // 2):
            j = random.randint(-6, 6)
            c = tuple(max(0, min(255, ch + j)) for ch in MUSCLE)
            atlas.draw.point((r[0] + xx, r[1] + yy), fill=c + (255,))
    for i, xx in enumerate(range(3, 11)):
        px(r, xx, 9 + (i % 2), WOUND)
    # crude stitches upper-right
    for i in range(6):
        px(r, 24 + i, 4, WOUND)
        if i % 2 == 0:
            px(r, 24 + i, 3, WOUND)
            px(r, 24 + i, 5, WOUND)
    return r


def paint_head_front():
    """16x15: heavy brow, sunken amber eyes (asymmetric), flat nose, grim cheek scar."""
    r = atlas.alloc(16, 15)
    fill_noise(r, HIDE)
    # heavy brow ridge: dark band with a lighter ridge line above
    rect_fill(r, 1, 3, 14, 2, HIDE_SHADOW)
    for xx in range(1, 15):
        px(r, xx, 2, HIDE_PALE)
    # eye sockets + dull amber glare (right eye bigger - asymmetric undead read)
    rect_fill(r, 2, 5, 4, 3, SOCKET)
    rect_fill(r, 3, 5, 2, 2, GLARE)
    px(r, 3, 5, tuple(min(255, c + 30) for c in GLARE))
    rect_fill(r, 10, 5, 4, 3, SOCKET)
    px(r, 11, 6, GLARE)
    # flat rotted nose: shadow wedge
    rect_fill(r, 7, 6, 2, 3, HIDE_SHADOW)
    px(r, 7, 8, WOUND)
    px(r, 8, 8, WOUND)
    # sunken cheeks
    for yy in range(9, 12):
        px(r, 2, yy, HIDE_SHADOW)
        px(r, 13, yy, HIDE_SHADOW)
    # cheek scar
    for i in range(4):
        px(r, 11 + i % 2, 9 + i, WOUND)
    # upper-lip shadow at the very bottom (the jaw cube sits below this face)
    for xx in range(3, 13):
        px(r, xx, 14, HIDE_SHADOW)
    return r


def paint_jaw_front():
    """18x5: underbite jaw - dark mouth gap, crooked bone teeth, two tusks at the corners."""
    r = atlas.alloc(18, 5)
    fill_noise(r, HIDE, vgrad=(1.0, 0.85))
    rect_fill(r, 2, 0, 14, 2, SOCKET)                # open mouth shadow
    for xx in range(3, 15, 2):                        # crooked teeth row
        px(r, xx, 0 if xx % 4 == 1 else 1, BONE)
    rect_fill(r, 1, 0, 2, 3, BONE)                   # tusks
    rect_fill(r, 15, 0, 2, 3, BONE)
    px(r, 1, 3, tuple(max(0, c - 40) for c in BONE))
    px(r, 16, 3, tuple(max(0, c - 40) for c in BONE))
    return r


def paint_wrap(w, h, hem=False):
    """Tattered cloth leg wraps: folds + optional ragged hem (transparent notches)."""
    r = atlas.alloc(w, h)
    fill_noise(r, WRAP, vgrad=(1.08, 0.85))
    for yy in range(2, h, 4):
        for xx in range(w):
            if (xx + yy) % 7 < 4:
                px(r, xx, yy, WRAP_FOLD)
    speckle(r, WRAP_DUSK, 0.08)
    if hem:
        for xx in range(w):
            if xx % 5 in (1, 2):
                atlas.draw.point((r[0] + xx, r[1] + h - 1), fill=(0, 0, 0, 0))
            elif xx % 5 == 3:
                px(r, xx, h - 1, WRAP_DUSK)
    return r


def paint_club_shaft():
    """5x36 strip along the shaft: faceted wood + leather grip bands near the bottom."""
    r = atlas.alloc(5, 36)
    for yy in range(36):
        for xx in range(5):
            base = WOOD_DARK if (xx + yy // 3) % 2 == 0 else WOOD_MID
            j = random.randint(-6, 6)
            c = tuple(max(0, min(255, ch + j)) for ch in base)
            atlas.draw.point((r[0] + xx, r[1] + yy), fill=c + (255,))
    for y0 in (28, 32):                               # grip wraps near the held end (bottom)
        rect_fill(r, 0, y0, 5, 2, GRIP)
        px(r, 2, y0, tuple(min(255, c + 25) for c in GRIP))
    return r


def paint_club_head(w, h):
    """Lumpy club-head mass: top-lit lumps, embedded studs, one blood smear."""
    r = atlas.alloc(w, h)
    fill_noise(r, CLUB_BASE, vgrad=(1.1, 0.82))
    for _ in range(4):                                # organic lumps
        lx, ly = random.randint(1, w - 4), random.randint(1, h - 4)
        rect_fill(r, lx, ly, 3, 2, CLUB_HI)
        rect_fill(r, lx + 1, ly + 2, 3, 1, CLUB_SHADOW)
    for sx, sy in ((w // 4, h // 3), (2 * w // 3, 2 * h // 3)):
        px(r, sx, sy, STUD)
        px(r, sx + 1, sy, STUD)
        px(r, sx, sy + 1, STUD_SHADOW)
    rect_fill(r, w // 2, h - 3, 3, 2, SMEAR)          # battle smear low on the head
    speckle(r, CLUB_SHADOW, 0.08)
    return r


def paint_stud_cube():
    r = atlas.alloc(4, 4)
    fill_noise(r, STUD, jitter=5, vgrad=(1.15, 0.75))
    px(r, 3, 3, STUD_SHADOW)
    px(r, 0, 0, tuple(min(255, c + 30) for c in STUD))
    return r


def paint_fist(w, h):
    """Skin fist with pale knuckle row."""
    r = atlas.alloc(w, h)
    fill_noise(r, HIDE, vgrad=(1.05, 0.85))
    for xx in range(1, w - 1, 3):
        px(r, xx, 1, HIDE_PALE)
        px(r, xx + 1, 1, HIDE_PALE)
        px(r, xx, 2, HIDE_SHADOW)
    return r


# ---- paint all regions ----
R_HEAD_FRONT = paint_head_front()
R_HEAD_SIDE = paint_skin(16, 15, scars=1)
R_HEAD_SIDE_M = mirrored_copy(R_HEAD_SIDE)
R_HEAD_TOP = paint_skin(16, 16, scars=2, stitches=1)
R_HEAD_BACK = paint_skin(16, 15, stitches=1)
R_HEAD_BOTTOM = paint_skin(16, 16, scars=0)
R_JAW_FRONT = paint_jaw_front()
R_JAW_SIDE = paint_skin(16, 5, scars=0)
R_JAW_BOTTOM = paint_skin(18, 16, scars=0)

R_CHEST_FRONT = paint_chest_front()
R_CHEST_BACK = paint_back()
R_CHEST_SIDE = paint_muscle(18, 18)
R_CHEST_TOP = paint_skin(34, 18, scars=2, stitches=1)   # traps/shoulder skin seen from above
R_CHEST_BOTTOM = paint_muscle(34, 18)
R_BELLY_FRONT = paint_belly_front()
R_BELLY_SIDE = paint_muscle(14, 14)
R_BELLY_BACK = paint_skin(24, 14, stitches=1)
R_BELLY_BOTTOM = paint_muscle(24, 14)
R_HUMP_TOP = paint_skin(20, 8, scars=1, stitches=1)
R_HUMP_BACK = paint_skin(20, 9, scars=1)
R_HUMP_SIDE = paint_skin(8, 9, scars=0)

R_SHOULDER = paint_skin(16, 15, scars=1)                # skin cap over the shoulder boulder
R_SHOULDER_TOP = paint_skin(16, 16, scars=1)
R_UPPER_ARM = paint_muscle(12, 16)
R_FOREARM = paint_muscle(16, 20)
R_FIST = paint_fist(12, 7)
R_FIST_BOTTOM = paint_fist(12, 14)

R_LEG = paint_wrap(14, 18, hem=True)
R_LEG_TOP = paint_wrap(14, 14)
R_FOOT = paint_skin(15, 6, scars=0)
R_FOOT_TOP = paint_skin(15, 17, scars=1)

R_SHAFT = paint_club_shaft()
R_SHAFT_END = paint_club_head(5, 5)
R_CLUBHEAD_A = paint_club_head(11, 18)
R_CLUBHEAD_A_SIDE = paint_club_head(11, 18)
R_CLUBHEAD_A_TOP = paint_club_head(11, 11)
R_CLUBHEAD_B = paint_club_head(10, 10)
R_STUD = paint_stud_cube()

TEX_W, TEX_H = atlas.crop()

# ============================================================================
# Geometry (authored space: +Y up from feet at 0, -Z front, +X = entity's left)
# ============================================================================


def pivot(p):
    """GeckoLib negates X at load time (BakedModelFactory.constructBone) - pre-negate to cancel."""
    return [-p[0], p[1], p[2]]


def cube(min_corner, size, faces, name_note=None):
    """min_corner/size in authored space; converts X the same way (new_min = -(min+size))."""
    x, y, z = min_corner
    sx, sy, sz = size
    c = {"origin": [-(x + sx), y, z], "size": [sx, sy, sz],
         "uv": {f: uv(r) for f, r in faces.items()}}
    return c


bones = []

# --- body (pivot at the hip line so crouch/lean pitches naturally) ---
bones.append({
    "name": "body", "pivot": pivot((0, 13, 0)),
    "cubes": [
        # belly
        cube((-6, 13, -3.5), (12, 6.5, 7), {
            "north": R_BELLY_FRONT, "south": R_BELLY_BACK, "east": R_BELLY_SIDE,
            "west": R_BELLY_SIDE, "down": R_BELLY_BOTTOM}),
        # barrel chest
        cube((-8.5, 19, -4.5), (17, 9, 9), {
            "north": R_CHEST_FRONT, "south": R_CHEST_BACK, "east": R_CHEST_SIDE,
            "west": R_CHEST_SIDE, "up": R_CHEST_TOP, "down": R_CHEST_BOTTOM}),
        # hunched upper back
        cube((-5, 26, 1.5), (10, 4.5, 4), {
            "south": R_HUMP_BACK, "up": R_HUMP_TOP, "east": R_HUMP_SIDE, "west": R_HUMP_SIDE}),
    ],
})

# --- head (child of body; GeckoLib's auto head-turn drives this bone by name) ---
bones.append({
    "name": "head", "parent": "body", "pivot": pivot((0, 28, -1.5)),
    "cubes": [
        cube((-4, 27.5, -7.5), (8, 7.5, 8), {
            "north": R_HEAD_FRONT, "south": R_HEAD_BACK, "east": R_HEAD_SIDE,
            "west": R_HEAD_SIDE_M, "up": R_HEAD_TOP, "down": R_HEAD_BOTTOM}),
        # underbite jaw, wider than the skull
        cube((-4.5, 25.5, -8), (9, 2.5, 8), {
            "north": R_JAW_FRONT, "east": R_JAW_SIDE, "west": R_JAW_SIDE,
            "south": R_JAW_SIDE, "down": R_JAW_BOTTOM}),
    ],
})

# --- arms: shoulder bone -> forearm child (-X side = the entity's RIGHT, holds the club).
# A 0.25-unit gap between the chest edge (8.5) and the arm's inner face (8.75) keeps the limbs
# reading as separate masses instead of merging into one slab (v1's mistake). ---
for side_name, s in (("right", -1), ("left", 1)):
    def sx(lo, size):
        """Authored min-corner x for a cube spanning [lo, lo+size] on the right side, mirrored
        to the left side when s=+1."""
        return lo if s == -1 else -(lo + size)

    arm_cubes = [
        cube((sx(-16.5, 8), 23, -4), (8, 7.5, 8), {
            "north": R_SHOULDER, "south": R_SHOULDER, "east": R_SHOULDER,
            "west": R_SHOULDER, "up": R_SHOULDER_TOP}),
        cube((sx(-15.5, 6.5), 16, -3.5), (6.5, 9, 7), {
            "north": R_UPPER_ARM, "south": R_UPPER_ARM, "east": R_UPPER_ARM,
            "west": R_UPPER_ARM}),
    ]
    bones.append({
        "name": f"{side_name}_arm", "pivot": pivot((s * 12, 26, 0)),
        "cubes": arm_cubes,
    })
    forearm_cubes = [
        cube((sx(-16.5, 8), 8, -4), (8, 9, 8), {
            "north": R_FOREARM, "south": R_FOREARM, "east": R_FOREARM,
            "west": R_FOREARM, "down": R_FOREARM}),
        cube((sx(-15.5, 6.5), 4.5, -3.5), (6.5, 3.5, 7), {
            "north": R_FIST, "south": R_FIST, "east": R_FIST, "west": R_FIST,
            "down": R_FIST_BOTTOM}),
    ]
    bones.append({
        "name": f"{side_name}_forearm", "parent": f"{side_name}_arm",
        "pivot": pivot((s * 12.5, 17, 0)),
        "cubes": forearm_cubes,
    })

# --- club: child of the right forearm, carried tilted FORWARD-and-outward (user feedback:
# the back-over-the-shoulder carry read as the weapon pointing the wrong way) ---
bones.append({
    "name": "club", "parent": "right_forearm", "pivot": pivot((-13, 6.5, 0)),
    "rotation": [52.0, 0.0, -32.0],
    "cubes": [
        cube((-14.5, 5, -1.5), (3, 18, 3), {
            "north": R_SHAFT, "south": R_SHAFT, "east": R_SHAFT, "west": R_SHAFT,
            "down": R_SHAFT_END}),
        # main head mass + offset lump for an irregular silhouette
        cube((-16.25, 20, -3.25), (6.5, 10, 6.5), {
            "north": R_CLUBHEAD_A, "south": R_CLUBHEAD_A, "east": R_CLUBHEAD_A_SIDE,
            "west": R_CLUBHEAD_A_SIDE, "up": R_CLUBHEAD_A_TOP, "down": R_CLUBHEAD_A_TOP}),
        cube((-14.75, 23.5, -0.25), (5.5, 5.5, 4.5), {
            "north": R_CLUBHEAD_B, "south": R_CLUBHEAD_B, "east": R_CLUBHEAD_B,
            "west": R_CLUBHEAD_B, "up": R_CLUBHEAD_B}),
        # two studs poking out of the head
        cube((-14, 26, -4.5), (2, 2, 2), {
            "north": R_STUD, "east": R_STUD, "west": R_STUD, "up": R_STUD, "down": R_STUD}),
        cube((-17.8, 22, 0.5), (2, 2, 2), {
            "north": R_STUD, "south": R_STUD, "east": R_STUD, "up": R_STUD, "down": R_STUD}),
    ],
})

# --- legs (thick pillars + forward feet, tall enough to read under the torso) ---
for side_name, s in (("right", -1), ("left", 1)):
    def sx(lo, size):
        return lo if s == -1 else -(lo + size)

    bones.append({
        "name": f"{side_name}_leg", "pivot": pivot((s * 4.4, 13, 0)),
        "cubes": [
            cube((sx(-8.4, 7.5), 3, -3.5), (7.5, 10.5, 7), {
                "north": R_LEG, "south": R_LEG, "east": R_LEG, "west": R_LEG,
                "up": R_LEG_TOP}),
            cube((sx(-8.4, 8), 0, -5.5), (8, 3, 9), {
                "north": R_FOOT, "south": R_FOOT, "east": R_FOOT, "west": R_FOOT,
                "up": R_FOOT_TOP, "down": R_FOOT_TOP}),
        ],
    })

# sanity: nothing may dip below the ground plane
lowest = min(c["origin"][1] for b in bones for c in b["cubes"])
assert lowest >= 0.0, f"geometry dips below ground: {lowest}"

geo = {
    "format_version": "1.12.0",
    "minecraft:geometry": [
        {
            "description": {
                "identifier": "geometry.zombie_colossus",
                "texture_width": TEX_W,
                "texture_height": TEX_H,
                "visible_bounds_width": 6,
                "visible_bounds_height": 7,
                "visible_bounds_offset": [0, 3, 0],
            },
            "bones": bones,
        }
    ],
}

with open("zombie_colossus.geo.json", "w", encoding="utf-8") as f:
    json.dump(geo, f, indent=2)
atlas.img.save("zombie_colossus.png")
print(f"bones: {len(bones)}, cubes: {sum(len(b['cubes']) for b in bones)}")
print(f"texture: {TEX_W}x{TEX_H}")
