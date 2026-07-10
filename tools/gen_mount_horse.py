"""Builds the shared "Mount Horse" GeckoLib template + all three mount-tier textures.

One shared geometry + one shared animation set (idle/walk/attack) for every summonable riding
horse (Wanderross / Eisenross / Schlachtross) - the horse-family sibling of
tools/gen_fallen_comet_stone.py's "one geometry, per-entity texture" template. Design brief
(docs/visual-style-guide.md, new "Mount Horses" section): a plain riding horse whose geometry
ALSO includes saddle-armor cubes (saddle + 2 flank plates) and body-armor cubes (chamfron, neck
armor, chest plate, rump plate) baked into the one shared model - each tier's texture alone
decides which of those cubes are visible:
  - wanderross: all 7 armor cubes fully transparent (alpha 0) -> plain horse.
  - eisenross:  saddle-armor group (saddle + flank plates) painted iron; body-armor group
                (chamfron/neck/chest/rump) stays transparent.
  - schlachtross: BOTH groups painted, in black plate.
Per-tier SIZE differences (renderer scale 1.0 / 1.1 / 1.25) are a Java-side
GeoEntityRenderer.withScale(...) call, NOT baked into the geometry - see MountTier.java
(already implemented) and this script's own docstring end for the exact numbers to use.

TEMPLATE CONTRACT (how a future 4th tier, if any, would be added):
  - geometry + all 3 animations are SHARED: mount_horse.geo.json / mount_horse.animation.json
    (one file each, resolved via a MountHorseGeoModel following FallenCometStoneGeoModel's
    withAltModel/withAltAnimations pattern - Java not written by this pass, see report).
  - each tier differs ONLY by its texture: add a palette dict below (COAT_BASE/COAT_SHADOW/
    COAT_HIGHLIGHT/COAT_DARK always; SADDLE_BASE/SADDLE_SHADOW/SADDLE_HIGHLIGHT and
    ARMOR_BASE/ARMOR_SHADOW/ARMOR_HIGHLIGHT/ARMOR_ACCENT are each either a color triple or
    None for "fully transparent"), add it to TIERS, rerun this script.
  - the texture atlas layout must stay IDENTICAL across variants (same UV rects) - build_texture()
    reseeds the RNG and rebuilds the atlas from scratch per palette so every alloc happens in the
    same order and rects line up pixel-for-pixel across all 3 PNGs (verified by the script's own
    assert, same discipline as gen_fallen_comet_stone.py).

Bone-name contract (for MountHorseGeoModel / a future MountHorseEntity's animation controller):
body (root), neck (parent body), head (parent neck), tail (parent body), leg_front_left,
leg_front_right, leg_back_left, leg_back_right (all parent body). No "look-at-player" head
bone/controller is wired up on purpose (not in the brief). Armor cubes live inside the bone they
physically cover (saddle/flank plates/chest/rump plate -> body; neck armor -> neck; chamfron ->
head) rather than getting their own bones, since none of them need independent motion.

Coordinate conventions (same as gen_fallen_comet_stone.py, same verified mechanism - see
docs/fabric-modding.md part H): authored space has +Y up (ground at 0), -Z front, +X = the
entity's LEFT. The pivot()/cube() helpers pre-negate X so GeckoLib's own load-time negation
cancels back out to natural coordinates - do not hand-negate anything.

Run from the repo root: `python tools/gen_mount_horse.py` (requires Pillow), preview:
  python tools/render_geckolib_preview.py --model mount_horse --tex wanderross.png
  python tools/render_geckolib_preview.py --model mount_horse --tex schlachtross.png --anim walk --times 0,0.15,0.3,0.45
  python tools/render_geckolib_preview.py --model mount_horse --tex schlachtross.png --anim attack --times 0,0.15,0.3,0.5,0.7
then copy the outputs to:
  src/main/resources/assets/baum2/geckolib/models/entity/mount_horse.geo.json
  src/main/resources/assets/baum2/geckolib/animations/entity/mount_horse.animation.json
  src/main/resources/assets/baum2/textures/entity/wanderross.png
  src/main/resources/assets/baum2/textures/entity/eisenross.png
  src/main/resources/assets/baum2/textures/entity/schlachtross.png

Model measurements the Java side needs (all at the geometry's own natural/unscaled size -
MountTier's renderScale 1.0/1.1/1.25 multiplies ALL of this at render time, per-tier, via
GeoEntityRenderer.withScale(...), not baked in here):
  - rump/back top (main body top surface):        y = 22/16 = 1.375 blocks
  - rump_plate top (very top of the back silhouette): y = 23/16 = 1.4375 blocks  (~"1.4 blocks
    tall at the back", per the brief)
  - poll/ear top (highest point overall, head up):  y = 45/16 = 2.8125 blocks
  - nose tip (muzzle/chamfron front):               z = -31/16 = -1.9375 blocks (forward of origin)
  - tail tip (rearmost/lowest hang):                z = 16/16 = 1.0 block back, y = 4/16 = 0.25
  - overall footprint (legs, the natural "stand on this box" width/depth): x -8..8 (1.0 block
    wide), z -15..15 (~1.875 blocks deep across the leg stance)
  - suggested rider seat (top of saddle, mid-body): x=0, y=25/16=1.5625, z=2/16=0.125 blocks
    (see report for the full writeup of what this implies for getPassengerRidingPos)
"""
import json
import math
import random
from PIL import Image, ImageDraw

SEED = 20260711
PIX = 2  # texture pixels per model unit (same ratio as gen_fallen_comet_stone.py)

# ============================================================================
# Palettes - one per mount tier. COAT_* always painted. SADDLE_* / ARMOR_* are
# either an (r,g,b) triple (painted) or None (left fully transparent, alpha=0).
# ============================================================================
TIERS = {
    "wanderross": dict(
        # Warm chestnut-brown coat - a plain, unarmored riding horse.
        COAT_BASE=(0x8A, 0x5A, 0x3C), COAT_SHADOW=(0x5E, 0x3B, 0x24),
        COAT_HIGHLIGHT=(0xB9, 0x80, 0x58), COAT_DARK=(0x3A, 0x24, 0x16),
        SADDLE_BASE=None, SADDLE_SHADOW=None, SADDLE_HIGHLIGHT=None,
        ARMOR_BASE=None, ARMOR_SHADOW=None, ARMOR_HIGHLIGHT=None, ARMOR_ACCENT=None,
    ),
    "eisenross": dict(
        # Dappled steel-grey coat (ties visually to "iron"), iron-toned saddle armor only.
        COAT_BASE=(0x8A, 0x92, 0x9C), COAT_SHADOW=(0x5C, 0x64, 0x6E),
        COAT_HIGHLIGHT=(0xB8, 0xC0, 0xC8), COAT_DARK=(0x34, 0x38, 0x3E),
        SADDLE_BASE=(0x6E, 0x76, 0x80), SADDLE_SHADOW=(0x45, 0x4C, 0x54),
        SADDLE_HIGHLIGHT=(0xAC, 0xB4, 0xBC),
        ARMOR_BASE=None, ARMOR_SHADOW=None, ARMOR_HIGHLIGHT=None, ARMOR_ACCENT=None,
    ),
    "schlachtross": dict(
        # Black warhorse coat, full black-plate barding (saddle armor group painted the same
        # iron-toned metal as Eisenross's - a real warhorse still needs a functional saddle
        # under the plate - PLUS the black body-armor group on top).
        COAT_BASE=(0x2A, 0x2A, 0x2E), COAT_SHADOW=(0x16, 0x16, 0x1A),
        COAT_HIGHLIGHT=(0x46, 0x46, 0x4C), COAT_DARK=(0x0A, 0x0A, 0x0C),
        SADDLE_BASE=(0x6E, 0x76, 0x80), SADDLE_SHADOW=(0x45, 0x4C, 0x54),
        SADDLE_HIGHLIGHT=(0xAC, 0xB4, 0xBC),
        ARMOR_BASE=(0x20, 0x22, 0x26), ARMOR_SHADOW=(0x10, 0x11, 0x14),
        ARMOR_HIGHLIGHT=(0x3A, 0x3E, 0x46), ARMOR_ACCENT=(0x6E, 0x1F, 0x1F),
    ),
}


# ============================================================================
# Atlas + painter helpers (local copy on purpose, same rationale as the comet
# script: one-shot generators, sharing code would let a refactor reshuffle
# another script's RNG sequence and change its output)
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
        w, h = int(round(w)), int(round(h))
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


def build_texture(pal):
    """Paints the full atlas for one tier's palette. Deterministic: reseeds, allocs in fixed
    order. Returns (rects-by-name dict, Atlas)."""
    random.seed(SEED)
    atlas = Atlas()

    def px(rect, x, y, color, alpha=255):
        if 0 <= x < rect[2] and 0 <= y < rect[3]:
            atlas.draw.point((rect[0] + x, rect[1] + y), fill=color + (alpha,))

    def fill_flat(rect, color, alpha):
        x0, y0, w, h = rect
        if color is None:
            return  # leave fully transparent (alpha already 0 from Atlas init)
        for yy in range(h):
            for xx in range(w):
                atlas.draw.point((x0 + xx, y0 + yy), fill=color + (alpha,))

    def fill_noise(rect, base, jitter=6, vgrad=(1.08, 0.90)):
        if base is None:
            return
        x0, y0, w, h = rect
        top, bottom = vgrad
        for yy in range(h):
            mul = top + (bottom - top) * (yy / max(1, h - 1))
            for xx in range(w):
                j = random.randint(-jitter, jitter)
                c = tuple(max(0, min(255, int(ch * mul) + j)) for ch in base)
                atlas.draw.point((x0 + xx, y0 + yy), fill=c + (255,))

    def speckle(rect, color, density=0.05):
        if color is None:
            return
        _, _, w, h = rect
        for yy in range(h):
            for xx in range(w):
                if random.random() < density:
                    px(rect, xx, yy, color)

    def edge_highlight(rect, color, alpha=255):
        """1px highlight rim along the top+left edge (reads as a beveled plate edge)."""
        if color is None:
            return
        _, _, w, h = rect
        for xx in range(w):
            px(rect, xx, 0, color, alpha)
        for yy in range(h):
            px(rect, 0, yy, color, alpha)

    def hide(w, h, mane=False, socks=False):
        """Coat surface: soft noise fill, occasional dark 'mane/dapple' speckle."""
        r = atlas.alloc(w, h)
        fill_noise(r, pal["COAT_BASE"])
        speckle(r, pal["COAT_SHADOW"], 0.07)
        speckle(r, pal["COAT_HIGHLIGHT"], 0.04)
        if mane:
            speckle(r, pal["COAT_DARK"], 0.28)
        if socks:
            _, _, w2, h2 = r
            for yy in range(max(0, h2 - 3), h2):
                for xx in range(w2):
                    if random.random() < 0.6:
                        px(r, xx, yy, pal["COAT_DARK"])
        return r

    def hoof(w, h):
        r = atlas.alloc(w, h)
        fill_noise(r, pal["COAT_DARK"], jitter=4, vgrad=(1.0, 0.8))
        return r

    def saddle_plate(w, h, rivet=False):
        r = atlas.alloc(w, h)
        base = pal["SADDLE_BASE"]
        if base is None:
            return r  # stays fully transparent
        fill_noise(r, base, jitter=5, vgrad=(1.12, 0.92))
        speckle(r, pal["SADDLE_SHADOW"], 0.10)
        edge_highlight(r, pal["SADDLE_HIGHLIGHT"], 160)
        return r

    def body_armor_plate(w, h):
        r = atlas.alloc(w, h)
        base = pal["ARMOR_BASE"]
        if base is None:
            return r  # stays fully transparent
        fill_noise(r, base, jitter=4, vgrad=(1.15, 0.90))
        speckle(r, pal["ARMOR_SHADOW"], 0.10)
        edge_highlight(r, pal["ARMOR_HIGHLIGHT"], 180)
        # thin accent line down the middle (a plate seam/trim)
        _, _, w2, h2 = r
        accent = pal["ARMOR_ACCENT"]
        if accent is not None:
            xline = w2 // 2
            for yy in range(h2):
                if random.random() < 0.7:
                    px(r, xline, yy, accent)
        return r

    rects = {}

    # --- horse coat cubes: one face-set painter call per logical face group ---
    for name, (sx, sy, sz), mane, socks in HIDE_CUBES:
        w, h, d = sx * PIX, sy * PIX, sz * PIX
        rects[name + ":front"] = hide(w, h, mane=mane, socks=socks)
        rects[name + ":back"] = hide(w, h, mane=mane, socks=socks)
        rects[name + ":left"] = hide(d, h, mane=mane, socks=socks)
        rects[name + ":right"] = hide(d, h, mane=mane, socks=socks)
        rects[name + ":top"] = hide(w, d, mane=mane, socks=False)
        rects[name + ":bottom"] = hoof(w, d) if socks else hide(w, d)

    # --- saddle-armor group (saddle + 2 flank plates) ---
    for name, (sx, sy, sz) in SADDLE_CUBES.items():
        rects[name + ":side"] = saddle_plate(sx * PIX, sy * PIX)
        rects[name + ":top"] = saddle_plate(sx * PIX, sz * PIX)

    # --- body-armor group (chamfron, neck armor, chest plate, rump plate) ---
    for name, (sx, sy, sz) in BODY_ARMOR_CUBES.items():
        rects[name + ":side"] = body_armor_plate(sx * PIX, sy * PIX)
        rects[name + ":top"] = body_armor_plate(sx * PIX, sz * PIX)

    return rects, atlas


# ============================================================================
# Geometry (authored space: +Y up from ground at 0, -Z front, +X = entity's left)
# ============================================================================

# name -> (size, mane/dark-speckle heavier?, dark "sock" band at the bottom face?)
HIDE_CUBES = [
    ("leg_front_left", (5, 15, 5), False, True),
    ("leg_front_right", (5, 15, 5), False, True),
    ("leg_back_left", (5, 15, 5), False, True),
    ("leg_back_right", (5, 15, 5), False, True),
    ("main_body", (12, 9, 29), False, False),
    ("neck_main", (7, 15, 7), True, False),
    ("head_main", (6, 8, 8), False, False),
    ("muzzle", (5, 5, 8), False, False),
    ("ear_left", (1.5, 3, 1.5), False, False),
    ("ear_right", (1.5, 3, 1.5), False, False),
    ("tail_main", (4, 16, 4), True, False),
]
HIDE_POS = {
    "leg_front_left": (3, 0, -15),
    "leg_front_right": (-8, 0, -15),
    "leg_back_left": (3, 0, 10),
    "leg_back_right": (-8, 0, 10),
    "main_body": (-6, 13, -15),
    "neck_main": (-3.5, 20, -17.5),
    "head_main": (-3, 35, -22),
    "muzzle": (-2.5, 36, -30),
    "ear_left": (2, 42, -18),
    "ear_right": (-3.5, 42, -18),
    "tail_main": (-2, 4, 12),
}

SADDLE_CUBES = {  # saddle + 2 flank plates
    "saddle": (10, 3, 12),
    "flank_plate_left": (2, 6, 14),
    "flank_plate_right": (2, 6, 14),
}
SADDLE_POS = {
    "saddle": (-5, 22, -4),
    "flank_plate_left": (6, 15, -6),
    "flank_plate_right": (-8, 15, -6),
}

BODY_ARMOR_CUBES = {  # chamfron (head), neck armor (neck), chest + rump plate (body)
    "chamfron": (6, 7, 3),
    "neck_armor": (8, 8, 8),
    "chest_plate": (10, 6, 3),
    "rump_plate": (12, 4, 6),
}
BODY_ARMOR_POS = {
    "chamfron": (-3, 35, -31),
    "neck_armor": (-4, 26, -18),
    "chest_plate": (-5, 15, -17),
    "rump_plate": (-6, 19, 11),
}
BODY_ARMOR_PARENT_BONE = {  # which bone's cube-list each armor cube belongs to
    "chamfron": "head",
    "neck_armor": "neck",
    "chest_plate": "body",
    "rump_plate": "body",
}

# leg pivot = top-center of each leg cube (hip/shoulder attachment)
LEG_PIVOTS = {
    "leg_front_left": (5.5, 15, -12.5),
    "leg_front_right": (-5.5, 15, -12.5),
    "leg_back_left": (5.5, 15, 12.5),
    "leg_back_right": (-5.5, 15, 12.5),
}

NECK_PIVOT = (0, 20, -14)
NECK_ROTATION = [-30.0, 0.0, 0.0]
HEAD_PIVOT = (0, 35, -14)
HEAD_ROTATION = [12.0, 0.0, 0.0]
TAIL_PIVOT = (0, 20, 14)
TAIL_ROTATION = [30.0, 0.0, 0.0]


def pivot(p):
    """GeckoLib negates X at load time (BakedModelFactory.constructBone) - pre-negate to cancel."""
    return [-p[0], p[1], p[2]]


def make_geo(rects):
    def cube(name, min_corner, size, faces):
        x, y, z = min_corner
        sx, sy, sz = size

        def uv(rect):
            return {"uv": [rect[0], rect[1]], "uv_size": [rect[2], rect[3]]}

        return {"origin": [-(x + sx), y, z], "size": [sx, sy, sz],
                "uv": {f: uv(rects[r]) for f, r in faces.items()}}

    def hide_faces(name):
        return {"north": name + ":front", "south": name + ":back",
                "east": name + ":left", "west": name + ":right",
                "up": name + ":top", "down": name + ":bottom"}

    def plate_faces(name):
        return {"north": name + ":side", "south": name + ":side",
                "east": name + ":side", "west": name + ":side",
                "up": name + ":top", "down": name + ":top"}

    hide_size = {n: s for n, s, _, _ in HIDE_CUBES}

    bones = []

    # --- body: root bone, carries main torso + all body-mounted armor cubes ---
    body_cubes = [cube("main_body", HIDE_POS["main_body"], hide_size["main_body"],
                        hide_faces("main_body"))]
    for name in ("saddle", "flank_plate_left", "flank_plate_right"):
        body_cubes.append(cube(name, SADDLE_POS[name], SADDLE_CUBES[name], plate_faces(name)))
    for name in ("chest_plate", "rump_plate"):
        body_cubes.append(cube(name, BODY_ARMOR_POS[name], BODY_ARMOR_CUBES[name],
                                plate_faces(name)))
    bones.append({"name": "body", "pivot": pivot((0, 0, 0)), "cubes": body_cubes})

    # --- neck: parent body, base-pose rotation bends it up/forward; carries neck armor ---
    neck_cubes = [cube("neck_main", HIDE_POS["neck_main"], hide_size["neck_main"],
                        hide_faces("neck_main"))]
    neck_cubes.append(cube("neck_armor", BODY_ARMOR_POS["neck_armor"],
                            BODY_ARMOR_CUBES["neck_armor"], plate_faces("neck_armor")))
    bones.append({"name": "neck", "parent": "body", "pivot": pivot(NECK_PIVOT),
                  "rotation": NECK_ROTATION, "cubes": neck_cubes})

    # --- head: parent neck, small extra base-pose bend; carries muzzle/ears/chamfron ---
    head_cubes = []
    for name in ("head_main", "muzzle", "ear_left", "ear_right"):
        head_cubes.append(cube(name, HIDE_POS[name], hide_size[name], hide_faces(name)))
    head_cubes.append(cube("chamfron", BODY_ARMOR_POS["chamfron"],
                            BODY_ARMOR_CUBES["chamfron"], plate_faces("chamfron")))
    bones.append({"name": "head", "parent": "neck", "pivot": pivot(HEAD_PIVOT),
                  "rotation": HEAD_ROTATION, "cubes": head_cubes})

    # --- tail: parent body, hangs down/back ---
    bones.append({
        "name": "tail", "parent": "body", "pivot": pivot(TAIL_PIVOT), "rotation": TAIL_ROTATION,
        "cubes": [cube("tail_main", HIDE_POS["tail_main"], hide_size["tail_main"],
                        hide_faces("tail_main"))],
    })

    # --- 4 legs: parent body, pivot at hip/shoulder, no base rotation ---
    for name in ("leg_front_left", "leg_front_right", "leg_back_left", "leg_back_right"):
        bones.append({
            "name": name, "parent": "body", "pivot": pivot(LEG_PIVOTS[name]),
            "cubes": [cube(name, HIDE_POS[name], hide_size[name], hide_faces(name))],
        })

    return bones


# ============================================================================
# Animations: idle (loop), walk (loop), attack (one-shot) - exactly these 3 names.
# ============================================================================
def make_animation():
    def bones_dict(**kw):
        return kw

    def keys(*pairs):
        """pairs: (time, [x,y,z]) -> Bedrock keyframe channel dict."""
        return {f"{t:g}": v for t, v in pairs}

    # --- idle: 4s seamless loop. Breathing (tiny body bob), tail sway, one head dip. ---
    idle_length = 4.0
    idle_bones = bones_dict(
        body={
            "rotation": keys((0, [0, 0, 0]), (2, [-1.2, 0, 0]), (4, [0, 0, 0])),
        },
        neck={
            "rotation": keys((0, [0, 0, 0]), (2, [1.0, 0, 0]), (4, [0, 0, 0])),
        },
        head={
            # occasional head dip: quick nod down mid-loop, otherwise resting
            "rotation": keys(
                (0, [0, 0, 0]), (2.6, [0, 0, 0]), (2.85, [14.0, 0, 0]),
                (3.15, [14.0, 0, 0]), (3.4, [0, 0, 0]), (4, [0, 0, 0]),
            ),
        },
        tail={
            "rotation": keys(
                (0, [0, 0, -6.0]), (1.0, [0, 0, 5.0]), (2.0, [0, 0, -6.0]),
                (3.0, [0, 0, 5.0]), (4.0, [0, 0, -6.0]),
            ),
        },
    )

    # --- walk: 0.8s loop, diagonal-pair trot (front_left+back_right vs front_right+back_left) ---
    walk_length = 0.8
    swing = 28.0

    def leg_swing(phase_offset):
        t0, t1, t2 = 0.0, walk_length / 2, walk_length
        # phase_offset in {0, 0.5} (half-cycle) - just shift which half starts forward
        if phase_offset == 0.0:
            return keys((t0, [swing, 0, 0]), (t1, [-swing, 0, 0]), (t2, [swing, 0, 0]))
        return keys((t0, [-swing, 0, 0]), (t1, [swing, 0, 0]), (t2, [-swing, 0, 0]))

    walk_bones = bones_dict(
        leg_front_left={"rotation": leg_swing(0.0)},
        leg_back_right={"rotation": leg_swing(0.0)},
        leg_front_right={"rotation": leg_swing(0.5)},
        leg_back_left={"rotation": leg_swing(0.5)},
        body={
            "position": keys((0, [0, 0.6, 0]), (0.2, [0, 0, 0]), (0.4, [0, 0.6, 0]),
                              (0.6, [0, 0, 0]), (0.8, [0, 0.6, 0])),
            "rotation": keys((0, [0, 0, 0]), (0.4, [1.5, 0, 0]), (0.8, [0, 0, 0])),
        },
        neck={
            "rotation": keys((0, [0, 0, 0]), (0.4, [-3.0, 0, 0]), (0.8, [0, 0, 0])),
        },
        tail={
            "rotation": keys((0, [0, 0, -4.0]), (0.4, [0, 0, 4.0]), (0.8, [0, 0, -4.0])),
        },
    )

    # --- attack: ~0.7s one-shot. Forward hoof strike: both front legs kick up/forward,
    # head/neck lunge down with the strike, body dips slightly, then everything returns. ---
    attack_length = 0.7
    attack_bones = bones_dict(
        leg_front_left={
            "rotation": keys((0, [0, 0, 0]), (0.15, [-55.0, 0, 0]), (0.35, [35.0, 0, 0]),
                              (0.55, [-10.0, 0, 0]), (0.7, [0, 0, 0])),
        },
        leg_front_right={
            "rotation": keys((0, [0, 0, 0]), (0.15, [-50.0, 0, 0]), (0.35, [38.0, 0, 0]),
                              (0.55, [-8.0, 0, 0]), (0.7, [0, 0, 0])),
        },
        neck={
            "rotation": keys((0, [0, 0, 0]), (0.15, [-14.0, 0, 0]), (0.35, [22.0, 0, 0]),
                              (0.55, [-4.0, 0, 0]), (0.7, [0, 0, 0])),
        },
        head={
            "rotation": keys((0, [0, 0, 0]), (0.15, [-8.0, 0, 0]), (0.35, [26.0, 0, 0]),
                              (0.55, [0, 0, 0]), (0.7, [0, 0, 0])),
        },
        body={
            "rotation": keys((0, [0, 0, 0]), (0.15, [-4.0, 0, 0]), (0.35, [6.0, 0, 0]),
                              (0.7, [0, 0, 0])),
            "position": keys((0, [0, 0, 0]), (0.35, [0, -0.8, 0.6]), (0.7, [0, 0, 0])),
        },
        tail={
            "rotation": keys((0, [0, 0, 0]), (0.35, [0, 0, -12.0]), (0.7, [0, 0, 0])),
        },
    )

    return {
        "format_version": "1.8.0",
        "animations": {
            "animation.mount_horse.idle": {
                "loop": True, "animation_length": idle_length, "bones": idle_bones,
            },
            "animation.mount_horse.walk": {
                "loop": True, "animation_length": walk_length, "bones": walk_bones,
            },
            "animation.mount_horse.attack": {
                "loop": False, "animation_length": attack_length, "bones": attack_bones,
            },
        },
    }


# ============================================================================
# Item icons: 16x16 summon-flute textures, one per tier. Same diagonal-tool-icon
# convention already established by gold_sword.png (Section 14.1 of the style guide) -
# a plain carved wood pipe, diagonal orientation, tapering shape, with one tier-accent
# wrap band tying each flute back to its horse (NOT a recolor of any real instrument icon).
# ============================================================================
FLUTE_ICONS = {
    # (wood_base, wood_shadow, wood_highlight, mouthpiece_dark, accent_band)
    "wanderross_flute": ((0x8A, 0x6E, 0x4A), (0x5A, 0x44, 0x2C), (0xB9, 0x96, 0x6C),
                          (0x2E, 0x22, 0x16), (0x8A, 0x5A, 0x3C)),   # ties to Wanderross's coat
    "eisenross_flute": ((0x74, 0x64, 0x4A), (0x48, 0x3E, 0x2C), (0x9E, 0x8A, 0x66),
                         (0x24, 0x1E, 0x14), (0x6E, 0x76, 0x80)),    # ties to Eisenross's iron
    "schlachtross_flute": ((0x50, 0x40, 0x30), (0x30, 0x26, 0x1A), (0x6E, 0x5A, 0x40),
                            (0x18, 0x12, 0x0C), (0x6E, 0x1F, 0x1F)),  # ties to Schlachtross's red trim
}


def make_flute_icon(wood_base, wood_shadow, wood_highlight, mouthpiece_dark, accent):
    """16x16 RGBA, transparent background. Diagonal pipe, pommel-to-tip bottom-left to
    top-right (same reading convention as gold_sword.png), 3 finger-hole dots, one 2px
    tier-accent binding wrap, dark mouthpiece tip at the bottom-left end."""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    px = img.load()

    def put(x, y, color, alpha=255):
        if 0 <= x < 16 and 0 <= y < 16:
            px[x, y] = color + (alpha,)

    # Diagonal spine from (2,13) [mouthpiece end] to (13,2) [open tip], 2px thick: the
    # thickness pixel is offset "up-left" (toward lower x at same y) for a consistent bevel.
    spine = [(2, 13), (3, 12), (4, 11), (5, 10), (6, 9), (7, 8), (8, 7),
             (9, 6), (10, 5), (11, 4), (12, 3), (13, 2)]
    for (x, y) in spine:
        put(x, y, wood_base)
        put(x + 1, y, wood_highlight)  # thin highlight toward the tip side
        put(x - 1, y, wood_shadow)     # thin shadow toward the mouthpiece side

    # Mouthpiece cap: darker 2x2 block at the bottom-left end
    for (x, y) in ((1, 14), (2, 14), (1, 13)):
        put(x, y, mouthpiece_dark)

    # Open tip highlight (single bright pixel at the very top-right end)
    put(14, 1, wood_highlight)
    put(13, 1, wood_highlight)

    # 3 finger-hole dots along the spine, dark, evenly spaced
    for (x, y) in ((5, 9), (7.5, 7.5), (10, 5)):
        put(round(x), round(y), mouthpiece_dark)

    # One 2px tier-accent binding wrap, perpendicular-ish to the spine, mid-pipe
    for (x, y) in ((7, 9), (8, 8), (8, 9), (9, 8)):
        put(x, y, accent)

    return img


# ============================================================================
# Build everything
# ============================================================================
first_rects = None
atlases = {}
for tier, pal in TIERS.items():
    rects, atlas = build_texture(pal)
    if first_rects is None:
        first_rects = rects
    else:
        assert rects == first_rects, f"atlas layout diverged for {tier}"
    atlases[tier] = atlas

TEX_W = max(first_rects[k][0] + first_rects[k][2] for k in first_rects)
TEX_H = max(first_rects[k][1] + first_rects[k][3] for k in first_rects)

for tier, atlas in atlases.items():
    atlas.img.crop((0, 0, TEX_W, TEX_H)).save(f"{tier}.png")

bones = make_geo(first_rects)
geo = {
    "format_version": "1.12.0",
    "minecraft:geometry": [
        {
            "description": {
                "identifier": "geometry.mount_horse",
                "texture_width": TEX_W,
                "texture_height": TEX_H,
                "visible_bounds_width": 3,
                "visible_bounds_height": 3,
                "visible_bounds_offset": [0, 1.3, 0],
            },
            "bones": bones,
        }
    ],
}
with open("mount_horse.geo.json", "w", encoding="utf-8") as f:
    json.dump(geo, f, indent=2)

with open("mount_horse.animation.json", "w", encoding="utf-8") as f:
    json.dump(make_animation(), f, indent=2)

print(f"bones: {len(bones)}, cubes: {sum(len(b['cubes']) for b in bones)}")
print(f"texture: {TEX_W}x{TEX_H} ({', '.join(TIERS)})")

for name, args in FLUTE_ICONS.items():
    make_flute_icon(*args).save(f"{name}.png")
print(f"flute icons: {', '.join(FLUTE_ICONS)}")
