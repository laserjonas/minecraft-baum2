"""Builds drevathis.geo.json + drevathis.png (full visual rework of the Drevathis boss).

GEOMETRY: bespoke demon-lord biped, authored directly in this file (same pipeline as
tools/gen_zombie_colossus.py; correctness verified visually via
tools/render_geckolib_preview.py). Design brief: "a demon which is born to kill you, bigger
than the player, with a black blade with dark smoke". Silhouette pillars: swept-back temple
horns, spiked boulder shoulders, gaunt hunched torso with an ember-cracked sternum, clawed
hands, digitigrade-suggesting legs (stepped thigh/shin/hoof cubes), a barbed tail, and the
CURSED BLADE AS REAL GEOMETRY (a bone parented to the right forearm) so attack animations
swing an actual weapon with weight - the lesson from the Colossus rework.

Authored height: ~31 units to the skull top (~36 with horns); the renderer applies
withScale(1.8F) against the existing 1.08x3.24 hitbox - same "visual mass exceeds hitbox"
ratio the Colossus already ships with.

TEXTURE: pixel-art atlas at 2 px per model unit, per-face painting. NEW palette ("Umbral
Sovereign", ratified into docs/visual-style-guide.md Section 19 by this rework): near-black
violet-tinted demon hide, dark bone plates, smoky horn keratin, ember-orange glow (eyes,
sternum crack, skin fissures - deliberately ORANGE, not the Colossus's Ashen-Brute red or the
Spider Queen's #FF3B1E red family), and a pure-black blade with a cold grey-violet edge (the
"dark smoke" is runtime particles, not texture).

Bone-name contract (referenced by drevathis.animation.json and DrevathisEntity's animation
controller): body, head (GeckoLib auto head-turn target), right_horn, left_horn, right_arm,
right_forearm, blade, left_arm, left_forearm, right_leg, left_leg, tail, tail_tip.

Run from the repo root: `python tools/gen_drevathis.py` (requires Pillow), preview with
`python tools/render_geckolib_preview.py --model drevathis`, then copy the outputs to:
  src/main/resources/assets/baum2/geckolib/models/entity/drevathis.geo.json
  src/main/resources/assets/baum2/textures/entity/drevathis.png

Coordinate conventions: authored space has +Y up (feet at 0), -Z front, +X = the entity's
LEFT. The pivot()/cube() helpers apply the same X-negation GeckoLib cancels at load time
(verified mechanism, see docs/fabric-modding.md part H; do not hand-negate anything).
"""
import json
import random
from PIL import Image, ImageDraw

random.seed(20260707)

# ============================================================================
# Palette - "Umbral Sovereign", ratified in docs/visual-style-guide.md Section 19
# ============================================================================
SKIN_DEEP = (0x1B, 0x14, 0x18)
SKIN = (0x2B, 0x20, 0x27)
SKIN_LIT = (0x3E, 0x2E, 0x38)
PLATE = (0x4A, 0x3B, 0x3E)
PLATE_LIT = (0x5E, 0x4C, 0x4E)
HORN_DARK = (0x2E, 0x26, 0x20)
HORN = (0x55, 0x48, 0x3F)
HORN_TIP = (0x6E, 0x61, 0x57)
EMBER_DEEP = (0xC6, 0x43, 0x1C)
EMBER = (0xFF, 0x7A, 0x26)
EMBER_CORE = (0xFF, 0xB8, 0x4D)
SOCKET = (0x0A, 0x07, 0x08)
CLAW = (0x6E, 0x61, 0x57)
BLADE_BLACK = (0x0C, 0x0A, 0x10)
BLADE_GREY = (0x23, 0x20, 0x30)
BLADE_EDGE = (0x4E, 0x4A, 0x5E)
GRIP_DARK = (0x1C, 0x14, 0x10)
GRIP_LIT = (0x33, 0x26, 0x1D)

PIX = 2  # texture pixels per model unit


# ============================================================================
# Atlas + painter helpers (same pattern as tools/gen_zombie_colossus.py - kept as a local copy
# on purpose: these are one-shot generators, and refactoring a committed script would reshuffle
# its random-call sequence and silently change its committed output)
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


def fill_noise(rect, base, jitter=6, vgrad=(1.06, 0.88)):
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
def paint_hide(w, h, fissures=0):
    """Near-black demon hide: violet-tinted noise, deep speckle, optional ember fissures
    (short glowing cracks - the demon's inner fire showing through)."""
    r = atlas.alloc(w, h)
    fill_noise(r, SKIN)
    speckle(r, SKIN_DEEP, 0.08)
    speckle(r, SKIN_LIT, 0.04)
    for _ in range(fissures):
        fx = random.randint(1, max(1, w - 5))
        fy = random.randint(1, max(1, h - 5))
        for i in range(random.randint(2, 4)):
            xx = min(w - 1, fx + i)
            yy = min(h - 1, fy + i + (i % 2))
            px(r, xx, yy, EMBER_DEEP)
        px(r, min(w - 1, fx + 1), min(h - 1, fy + 1), EMBER)
    return r


def paint_muscle_hide(w, h, vertical=True):
    """Hide with faint fiber striations in SKIN_LIT - reads as lean corded muscle."""
    r = atlas.alloc(w, h)
    fill_noise(r, SKIN, vgrad=(1.08, 0.85))
    length, breadth = (h, w) if vertical else (w, h)
    pos = 1
    while pos < breadth - 1:
        for t in range(length):
            off = (t // 4) % 2
            b = pos + off
            if b < breadth and random.random() < 0.7:
                tone = SKIN_LIT if random.random() < 0.6 else SKIN_DEEP
                if vertical:
                    px(r, b, t, tone)
                else:
                    px(r, t, b, tone)
        pos += random.randint(3, 4)
    speckle(r, SKIN_DEEP, 0.05)
    return r


def paint_chest_front():
    """26x16 (13w x 8h units): gaunt plated pecs and the signature ember sternum crack."""
    r = atlas.alloc(26, 16)
    fill_noise(r, SKIN, vgrad=(1.08, 0.86))
    # collar ridge of dark bone plate along the top
    rect_fill(r, 0, 0, 26, 2, PLATE)
    for xx in range(0, 26, 3):
        px(r, xx, 1, PLATE_LIT)
        px(r, xx + 1, 2, SKIN_DEEP)
    # pec plates: two angular bone-armor masses
    for x0 in (2, 15):
        for yy in range(3, 9):
            for xx in range(x0, x0 + 9):
                j = random.randint(-5, 5)
                c = tuple(max(0, min(255, ch + j)) for ch in PLATE)
                atlas.draw.point((r[0] + xx, r[1] + yy), fill=c + (255,))
        rect_fill(r, x0, 3, 9, 1, PLATE_LIT)   # lit top edge
        rect_fill(r, x0, 8, 9, 1, SKIN_DEEP)   # underside shadow
        px(r, x0 + 2, 5, PLATE_LIT)
        px(r, x0 + 6, 4, PLATE_LIT)
    # ember sternum crack: jagged vertical glow between the plates
    cx = 12
    for yy in range(2, 14):
        wob = (yy // 3) % 2
        px(r, cx + wob, yy, EMBER_DEEP)
        if yy % 3 == 1:
            px(r, cx + wob, yy, EMBER)
        if yy == 6:
            px(r, cx + wob, yy, EMBER_CORE)
            px(r, cx + wob - 1, yy, EMBER_DEEP)
            px(r, cx + wob + 1, yy + 1, EMBER_DEEP)
    # lower rib shadows
    for row, y0 in ((0, 11), (1, 13)):
        for xx in range(3 + row, 23 - row, 4):
            px(r, xx, y0, SKIN_DEEP)
            px(r, xx + 1, y0, SKIN_DEEP)
    return r


def paint_belly_front():
    """18x9 (9w x 4.5h units): sunken abdomen with oblique shadows and one ember fissure."""
    r = atlas.alloc(18, 9)
    fill_noise(r, SKIN, vgrad=(1.0, 0.84))
    for yy in range(9):
        px(r, 0, yy, SKIN_DEEP)
        px(r, 1, yy, SKIN_DEEP)
        px(r, 16, yy, SKIN_DEEP)
        px(r, 17, yy, SKIN_DEEP)
    for y0 in (2, 5):
        for xx in range(4, 14, 3):
            px(r, xx, y0, SKIN_DEEP)
    for i in range(3):
        px(r, 6 + i, 6 + (i % 2), EMBER_DEEP)
    px(r, 7, 7, EMBER)
    return r


def paint_head_front():
    """14x12: heavy brow shelf, slanted ember eyes, nasal slits, gaunt cheeks."""
    r = atlas.alloc(14, 12)
    fill_noise(r, SKIN)
    # brow shelf: dark plate band, lit ridge on top
    rect_fill(r, 1, 2, 12, 2, PLATE)
    for xx in range(1, 13):
        px(r, xx, 1, PLATE_LIT)
    px(r, 6, 3, SKIN_DEEP)
    px(r, 7, 3, SKIN_DEEP)
    # slanted ember eyes: inner corners lower than outer (a scowl), 3px wide each
    # left of texture = entity's right eye
    for i, (ex, ey) in enumerate(((2, 5), (3, 4), (4, 4))):
        px(r, ex, ey + 1, SOCKET)
        px(r, ex, ey, EMBER if i != 1 else EMBER_CORE)
    for i, (ex, ey) in enumerate(((9, 4), (10, 4), (11, 5))):
        px(r, ex, ey + 1, SOCKET)
        px(r, ex, ey, EMBER if i != 1 else EMBER_CORE)
    px(r, 3, 6, EMBER_DEEP)   # faint under-glow on the cheekbones
    px(r, 10, 6, EMBER_DEEP)
    # nasal slits (no nose - skull-like)
    px(r, 6, 7, SOCKET)
    px(r, 7, 7, SOCKET)
    px(r, 6, 8, SKIN_DEEP)
    px(r, 7, 8, SKIN_DEEP)
    # gaunt cheek shadows
    for yy in range(7, 11):
        px(r, 1, yy, SKIN_DEEP)
        px(r, 12, yy, SKIN_DEEP)
    # upper-lip shadow (jaw cube sits below)
    for xx in range(3, 11):
        px(r, xx, 11, SKIN_DEEP)
    return r


def paint_jaw_front():
    """15x4: parted jaw - dark mouth gap, upward corner fangs (underbite)."""
    r = atlas.alloc(15, 4)
    fill_noise(r, SKIN, vgrad=(1.0, 0.85))
    rect_fill(r, 2, 0, 11, 2, SOCKET)                 # open mouth shadow
    for xx in range(4, 11, 2):                         # small lower teeth
        px(r, xx, 1, HORN)
    rect_fill(r, 1, 0, 2, 3, HORN_TIP)                # corner fangs, biggest teeth
    rect_fill(r, 12, 0, 2, 3, HORN_TIP)
    px(r, 1, 0, tuple(min(255, c + 25) for c in HORN_TIP))
    px(r, 13, 0, tuple(min(255, c + 25) for c in HORN_TIP))
    return r


def paint_horn(w, h):
    """Ridged keratin: banded rings darkening toward the base."""
    r = atlas.alloc(w, h)
    fill_noise(r, HORN, vgrad=(1.18, 0.75))
    for yy in range(2, h, 3):
        for xx in range(w):
            px(r, xx, yy, HORN_DARK)
    for xx in range(w):
        px(r, xx, 0, HORN_TIP)
    return r


def paint_shoulder(w, h):
    """Bone-plate shoulder cap over hide."""
    r = atlas.alloc(w, h)
    fill_noise(r, PLATE, vgrad=(1.12, 0.8))
    speckle(r, SKIN_DEEP, 0.06)
    for xx in range(w):
        px(r, xx, 0, PLATE_LIT)
    for xx in range(0, w, 4):
        px(r, xx, h - 1, SKIN_DEEP)
    return r


def paint_claw_fist(w, h):
    """Hide fist: pale knuckle ridge + claw-tip hints along the bottom edge."""
    r = atlas.alloc(w, h)
    fill_noise(r, SKIN, vgrad=(1.05, 0.85))
    for xx in range(1, w - 1, 3):
        px(r, xx, 1, SKIN_LIT)
        px(r, xx + 1, 1, SKIN_LIT)
    for xx in range(1, w - 1, 3):
        px(r, xx, h - 2, HORN_DARK)
        px(r, xx, h - 1, HORN)
    return r


def paint_claw_spike():
    r = atlas.alloc(3, 5)
    fill_noise(r, HORN, jitter=5, vgrad=(0.82, 1.05))
    speckle(r, HORN_DARK, 0.15)
    return r


def paint_leg(w, h):
    r = paint_muscle_hide(w, h)
    return r


def paint_hoof(w, h):
    """Cloven hoof front: two keratin toes split by a deep center groove."""
    r = atlas.alloc(w, h)
    fill_noise(r, HORN_DARK, jitter=4, vgrad=(0.92, 0.72))
    mid = w // 2
    for yy in range(h):
        px(r, mid, yy, SOCKET)
    return r


def paint_tail(w, h):
    r = atlas.alloc(w, h)
    fill_noise(r, SKIN, vgrad=(1.06, 0.86))
    for yy in range(0, h, 3):
        for xx in range(w):
            if (xx + yy) % 5 < 2:
                px(r, xx, yy, SKIN_DEEP)
    return r


def paint_tail_barb():
    r = atlas.alloc(6, 6)
    fill_noise(r, HORN, vgrad=(1.15, 0.8))
    px(r, 2, 0, HORN_TIP)
    px(r, 3, 0, HORN_TIP)
    speckle(r, HORN_DARK, 0.1)
    return r


def paint_blade_face(w, h):
    """The black blade's broad face: near-black steel, faint diagonal grey sheen streaks,
    a cold grey-violet edge line along the LEFT texture column (mapped to the cutting edge),
    and three faint rune nicks. No ember - the blade is pure darkness."""
    r = atlas.alloc(w, h)
    fill_noise(r, BLADE_BLACK, jitter=3, vgrad=(1.1, 0.9))
    for start in range(-h, w, 7):
        for i in range(h):
            xx = start + i // 2
            if 1 <= xx < w - 1 and random.random() < 0.6:
                px(r, xx, i, BLADE_GREY)
    for yy in range(h):
        px(r, 0, yy, BLADE_EDGE)
        if yy % 4 == 0:
            px(r, 0, yy, tuple(min(255, c + 25) for c in BLADE_EDGE))
        px(r, w - 1, yy, BLADE_GREY)  # blunt spine
    for ry in (h // 5, h // 2, 4 * h // 5):
        px(r, w // 2, ry, BLADE_EDGE)
        px(r, w // 2, ry + 1, BLADE_GREY)
    return r


def paint_blade_thin(w, h):
    """Blade edge-on strip (east/west faces of the thin slab)."""
    r = atlas.alloc(w, h)
    fill_noise(r, BLADE_BLACK, jitter=3, vgrad=(1.05, 0.9))
    for yy in range(0, h, 5):
        px(r, 0, yy, BLADE_GREY)
    return r


def paint_grip(w, h):
    r = atlas.alloc(w, h)
    fill_noise(r, GRIP_DARK, jitter=4)
    for yy in range(1, h, 3):
        rect_fill(r, 0, yy, w, 1, GRIP_LIT)
    return r


def paint_guard(w, h):
    r = atlas.alloc(w, h)
    fill_noise(r, BLADE_GREY, jitter=4, vgrad=(1.15, 0.8))
    for xx in range(w):
        px(r, xx, 0, BLADE_EDGE)
    return r


# ---- paint all regions ----
R_HEAD_FRONT = paint_head_front()
R_HEAD_SIDE = paint_hide(13, 12, fissures=1)
R_HEAD_SIDE_M = mirrored_copy(R_HEAD_SIDE)
R_HEAD_TOP = paint_hide(14, 13, fissures=1)
R_HEAD_BACK = paint_hide(14, 12)
R_HEAD_BOTTOM = paint_hide(14, 13)
R_JAW_FRONT = paint_jaw_front()
R_JAW_SIDE = paint_hide(12, 4)
R_JAW_BOTTOM = paint_hide(15, 12)

R_CHEST_FRONT = paint_chest_front()
R_CHEST_BACK = paint_hide(26, 16, fissures=2)
R_CHEST_SIDE = paint_muscle_hide(13, 16)
R_CHEST_TOP = paint_hide(26, 13, fissures=1)
R_CHEST_BOTTOM = paint_hide(26, 13)
R_BELLY_FRONT = paint_belly_front()
R_BELLY_SIDE = paint_muscle_hide(10, 9)
R_BELLY_BACK = paint_hide(18, 9, fissures=1)
R_NECK = paint_hide(8, 4)

R_HORN_A = paint_horn(4, 9)
R_HORN_B = paint_horn(3, 7)

R_SHOULDER = paint_shoulder(10, 10)
R_SHOULDER_TOP = paint_shoulder(10, 12)
R_SPIKE_A = paint_horn(5, 4)
R_SPIKE_B = paint_horn(3, 4)
R_UPPER_ARM = paint_muscle_hide(8, 14)
R_FOREARM = paint_muscle_hide(9, 17)
R_FIST = paint_claw_fist(8, 6)
R_FIST_BOTTOM = paint_claw_fist(8, 10)
R_CLAW = paint_claw_spike()

R_THIGH = paint_leg(9, 12)
R_THIGH_TOP = paint_hide(9, 11)
R_SHIN = paint_leg(7, 10)
R_HOOF = paint_hoof(8, 4)
R_HOOF_TOP = paint_hide(8, 11)

R_TAIL_1 = paint_tail(6, 14)
R_TAIL_2 = paint_tail(4, 12)
R_TAIL_BARB = paint_tail_barb()

R_GRIP = paint_grip(3, 12)
R_GUARD = paint_guard(8, 3)
R_BLADE_FACE = paint_blade_face(9, 34)
R_BLADE_FACE_2 = paint_blade_face(9, 34)
R_BLADE_THIN = paint_blade_thin(3, 34)
R_BLADE_TIP = paint_blade_face(6, 8)
R_BLADE_TIP_THIN = paint_blade_thin(3, 8)

TEX_W, TEX_H = atlas.crop()

# ============================================================================
# Geometry (authored space: +Y up from feet at 0, -Z front, +X = entity's left)
# ============================================================================


def pivot(p):
    """GeckoLib negates X at load time (BakedModelFactory.constructBone) - pre-negate to cancel."""
    return [-p[0], p[1], p[2]]


def cube(min_corner, size, faces):
    """min_corner/size in authored space; converts X the same way (new_min = -(min+size))."""
    x, y, z = min_corner
    sx, sy, sz = size
    return {"origin": [-(x + sx), y, z], "size": [sx, sy, sz],
            "uv": {f: uv(r) for f, r in faces.items()}}


bones = []

# --- body (pivot at the hip line) ---
bones.append({
    "name": "body", "pivot": pivot((0, 12.5, 0)),
    "cubes": [
        # sunken abdomen
        cube((-4.5, 11.5, -2.5), (9, 4.5, 5), {
            "north": R_BELLY_FRONT, "south": R_BELLY_BACK, "east": R_BELLY_SIDE,
            "west": R_BELLY_SIDE}),
        # broad plated chest
        cube((-6.5, 15.5, -3.25), (13, 8, 6.5), {
            "north": R_CHEST_FRONT, "south": R_CHEST_BACK, "east": R_CHEST_SIDE,
            "west": R_CHEST_SIDE, "up": R_CHEST_TOP, "down": R_CHEST_BOTTOM}),
        # short thick neck
        cube((-2, 23.5, -2.25), (4, 2, 4), {
            "north": R_NECK, "south": R_NECK, "east": R_NECK, "west": R_NECK}),
    ],
})

# --- head (child of body; GeckoLib's auto head-turn drives this bone by name) ---
bones.append({
    "name": "head", "parent": "body", "pivot": pivot((0, 25.2, -0.25)),
    "cubes": [
        cube((-3.5, 25, -5.25), (7, 6, 6.5), {
            "north": R_HEAD_FRONT, "south": R_HEAD_BACK, "east": R_HEAD_SIDE,
            "west": R_HEAD_SIDE_M, "up": R_HEAD_TOP, "down": R_HEAD_BOTTOM}),
        # parted underbite jaw, slightly wider than the skull
        cube((-3.75, 23.6, -5.6), (7.5, 1.8, 6), {
            "north": R_JAW_FRONT, "east": R_JAW_SIDE, "west": R_JAW_SIDE,
            "south": R_JAW_SIDE, "down": R_JAW_BOTTOM}),
    ],
})

# --- horns: swept back-and-outward from the temples (bone rotation, two stepped cubes each
# so the silhouette reads curved; sign convention verified in the preview render) ---
for side_name, s in (("right", -1), ("left", 1)):
    bones.append({
        "name": f"{side_name}_horn", "parent": "head",
        "pivot": pivot((s * 2.6, 30.2, -1.0)),
        "rotation": [-32.0, 0.0, s * -18.0],
        "cubes": [
            cube((s * 2.6 - 1.0, 29.8, -2.0), (2, 4.5, 2), {
                "north": R_HORN_A, "south": R_HORN_A, "east": R_HORN_A, "west": R_HORN_A,
                "up": R_SPIKE_B}),
            cube((s * 3.0 - 0.7, 34.0, -1.4), (1.4, 3.5, 1.4), {
                "north": R_HORN_B, "south": R_HORN_B, "east": R_HORN_B, "west": R_HORN_B,
                "up": R_SPIKE_B}),
        ],
    })

# --- arms: shoulder bone -> forearm child (-X side = the entity's RIGHT, holds the blade).
# Small gap between chest edge (6.5) and arm inner face (6.75) so the masses stay separate. ---
for side_name, s in (("right", -1), ("left", 1)):
    def sx(lo, size):
        """Authored min-corner x for a cube spanning [lo, lo+size] on the right side,
        mirrored to the left side when s=+1."""
        return lo if s == -1 else -(lo + size)

    bones.append({
        "name": f"{side_name}_arm", "pivot": pivot((s * 8.25, 22, 0)),
        "cubes": [
            # boulder shoulder with a two-step bone spike on top
            cube((sx(-11.75, 5), 19.5, -3), (5, 5, 6), {
                "north": R_SHOULDER, "south": R_SHOULDER, "east": R_SHOULDER,
                "west": R_SHOULDER, "up": R_SHOULDER_TOP}),
            cube((sx(-10.5, 2.5), 24.5, -1.25), (2.5, 2, 2.5), {
                "north": R_SPIKE_A, "south": R_SPIKE_A, "east": R_SPIKE_A,
                "west": R_SPIKE_A, "up": R_SPIKE_B}),
            cube((sx(-9.9, 1.4), 26.5, -0.7), (1.4, 2.2, 1.4), {
                "north": R_SPIKE_B, "south": R_SPIKE_B, "east": R_SPIKE_B,
                "west": R_SPIKE_B, "up": R_SPIKE_B}),
            cube((sx(-11.25, 4), 15, -2.5), (4, 5.5, 5), {
                "north": R_UPPER_ARM, "south": R_UPPER_ARM, "east": R_UPPER_ARM,
                "west": R_UPPER_ARM}),
        ],
    })
    bones.append({
        "name": f"{side_name}_forearm", "parent": f"{side_name}_arm",
        "pivot": pivot((s * 9.25, 15.5, 0)),
        "cubes": [
            cube((sx(-11.5, 4.5), 8.5, -2.75), (4.5, 7.5, 5.5), {
                "north": R_FOREARM, "south": R_FOREARM, "east": R_FOREARM,
                "west": R_FOREARM}),
            # clawed fist
            cube((sx(-11.25, 4), 5.6, -2.4), (4, 3, 4.8), {
                "north": R_FIST, "south": R_FIST, "east": R_FIST, "west": R_FIST,
                "down": R_FIST_BOTTOM}),
            # two claw spikes raking down off the knuckles
            cube((sx(-10.8, 1), 4.2, -3.1), (1, 2.2, 1), {
                "north": R_CLAW, "south": R_CLAW, "east": R_CLAW, "west": R_CLAW,
                "down": R_CLAW}),
            cube((sx(-8.8, 1), 4.2, -3.1), (1, 2.2, 1), {
                "north": R_CLAW, "south": R_CLAW, "east": R_CLAW, "west": R_CLAW,
                "down": R_CLAW}),
        ],
    })

# --- the black blade: child of the right forearm, carried tip-down-forward at the hip (the
# forward-down carry that ratified well on the Colossus - never blocks the chest or face).
# Authored pointing straight UP from the fist; the bone rotation tips it into the carry. ---
bones.append({
    "name": "blade", "parent": "right_forearm", "pivot": pivot((-9.25, 7, 0)),
    "rotation": [24.0, 0.0, 14.0],
    "cubes": [
        # grip below and through the fist
        cube((-10.0, 3.4, -0.75), (1.5, 6, 1.5), {
            "north": R_GRIP, "south": R_GRIP, "east": R_GRIP, "west": R_GRIP,
            "down": R_GRIP}),
        # flared crossguard
        cube((-11.25, 9.4, -1.1), (4, 1.4, 2.2), {
            "north": R_GUARD, "south": R_GUARD, "east": R_GUARD, "west": R_GUARD,
            "up": R_GUARD, "down": R_GUARD}),
        # main blade slab: broad faces east/west, cutting edge -Z (front)
        cube((-9.85, 10.8, -2.5), (1.2, 17, 4.6), {
            "east": R_BLADE_FACE, "west": R_BLADE_FACE_2, "north": R_BLADE_THIN,
            "south": R_BLADE_THIN, "up": R_GUARD}),
        # tapered tip, shifted toward the cutting edge
        cube((-9.85, 27.8, -2.2), (1.2, 4, 2.8), {
            "east": R_BLADE_TIP, "west": R_BLADE_TIP, "north": R_BLADE_TIP_THIN,
            "south": R_BLADE_TIP_THIN, "up": R_BLADE_TIP_THIN}),
    ],
})

# --- legs: stepped thigh/shin/hoof cubes suggest a digitigrade demon leg without per-cube
# rotation (which the preview pipeline doesn't support) ---
for side_name, s in (("right", -1), ("left", 1)):
    def sx(lo, size):
        return lo if s == -1 else -(lo + size)

    bones.append({
        "name": f"{side_name}_leg", "pivot": pivot((s * 3.2, 12.5, 0)),
        "cubes": [
            cube((sx(-5.75, 4.5), 6.5, -3), (4.5, 6.5, 5.5), {
                "north": R_THIGH, "south": R_THIGH, "east": R_THIGH, "west": R_THIGH,
                "up": R_THIGH_TOP}),
            # shin set back (fake ankle joint)
            cube((sx(-5.25, 3.5), 2, -1), (3.5, 5, 3.5), {
                "north": R_SHIN, "south": R_SHIN, "east": R_SHIN, "west": R_SHIN}),
            # cloven hoof reaching forward
            cube((sx(-5.5, 4), 0, -2.75), (4, 2, 5.5), {
                "north": R_HOOF, "south": R_HOOF, "east": R_HOOF, "west": R_HOOF,
                "up": R_HOOF_TOP, "down": R_HOOF_TOP}),
        ],
    })

# --- barbed tail: two chained bones for sway animation ---
bones.append({
    "name": "tail", "parent": "body", "pivot": pivot((0, 12.8, 2.2)),
    "rotation": [-14.0, 0.0, 0.0],
    "cubes": [
        cube((-1.4, 11.6, 2.2), (2.8, 2.6, 7), {
            "north": R_TAIL_1, "south": R_TAIL_1, "east": R_TAIL_1, "west": R_TAIL_1,
            "up": R_TAIL_1, "down": R_TAIL_1}),
    ],
})
bones.append({
    "name": "tail_tip", "parent": "tail", "pivot": pivot((0, 12.9, 9.0)),
    "rotation": [32.0, 0.0, 0.0],
    "cubes": [
        cube((-1.0, 11.8, 9.0), (2, 2.2, 6), {
            "north": R_TAIL_2, "south": R_TAIL_2, "east": R_TAIL_2, "west": R_TAIL_2,
            "up": R_TAIL_2, "down": R_TAIL_2}),
        # arrowhead barb
        cube((-1.5, 11.5, 14.8), (3, 2.8, 2.8), {
            "north": R_TAIL_BARB, "south": R_TAIL_BARB, "east": R_TAIL_BARB,
            "west": R_TAIL_BARB, "up": R_TAIL_BARB, "down": R_TAIL_BARB}),
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
                "identifier": "geometry.drevathis",
                "texture_width": TEX_W,
                "texture_height": TEX_H,
                "visible_bounds_width": 5,
                "visible_bounds_height": 5,
                "visible_bounds_offset": [0, 2, 0],
            },
            "bones": bones,
        }
    ],
}

with open("drevathis.geo.json", "w", encoding="utf-8") as f:
    json.dump(geo, f, indent=2)
atlas.img.save("drevathis.png")
print(f"bones: {len(bones)}, cubes: {sum(len(b['cubes']) for b in bones)}")
print(f"texture: {TEX_W}x{TEX_H}")
