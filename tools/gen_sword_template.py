"""Builds the "sword template" - the mod's reusable GeckoLib SWORD line - plus its first
sword, the wooden "Espenklinge".

Same template contract as tools/gen_fallen_comet_stone.py established for the stone bosses,
now applied to weapons: ONE shared geometry + animation pair for every sword of the line,
per-sword texture (+ flat icon + the two tiny per-sword JSONs) only. Adding a second sword
later = add a palette entry to SWORDS below and rerun; the geometry/animation files are
byte-identical across swords (asserted in-script via the shared-atlas-layout check).

Shape (visual reference: a real-world late-medieval longsword, museum piece - historical
artifact, not game IP; only the generic silhouette is used): straight double-edged blade
tapering in two steps to the point, raised midrib ridge along the blade, straight slender
crossguard with slightly flared end caps, two-hand tapering wrapped grip, and the signature
disc ("wheel") pommel with a small brass boss at its center. The Espenklinge renders it all
in pale aspen wood + walnut + leather - a wooden training longsword.

Authoring conventions (identical to gen_mount_horse.py, see comments there):
- Y-up, origin (0,0,0) = grip center = the wielder's fist. GeckoLib's GeoItemRenderer
  translates the geo origin to the item-cube center (adjustRenderPose: +0.5,+0.51,+0.5,
  verified against GeckoLib 5.4.5 sources), so display transforms in the base model rotate
  around the FIST, which is what you want for a held weapon.
- cube origins pre-mirrored in X, bone pivots pre-negated in X (GeckoLib load-time X flip).
- per-face pixel UV rects, texture_width/height declared; 2 px per geo unit here (the blade
  is thin - 1 px/unit would leave no room for wood grain).

Animations (all on the shared "animation.sword_template.*" keys):
- idle          6.0s loop  - breathing sway + one late-loop "grip settle" wrist roll accent
                             (same "accent event" idiom as mount_horse idle's head dip).
- attack        0.55s shot - vertical moulinet: short back-cock, full 360deg forward spin
                             around the fist, overshoot settle. Ends at -360 == 0 so the
                             handoff back to idle is seamless.
- attack_mounted 0.7s shot - cavalry sweep: high left-shoulder windup, wide horizontal
                             cut across the body with a forward reach, follow-through.
Both attacks are server-triggered one-shots (triggerableAnim), mirroring the horse's own
attack trigger so horse + blade animate in the same instant on a mounted hit.

Run from the repo root (requires Pillow); copy outputs to:
  sword_template.geo.json        -> src/main/resources/assets/baum2/geckolib/models/item/
  sword_template.animation.json  -> src/main/resources/assets/baum2/geckolib/animations/item/
  <sword>_geo.png                -> src/main/resources/assets/baum2/textures/item/
  <sword>.png                    -> src/main/resources/assets/baum2/textures/item/
  <sword>_base.json              -> src/main/resources/assets/baum2/models/item/
  <sword>_icon_model.json        -> src/main/resources/assets/baum2/models/item/<sword>.json
  <sword>_item_def.json          -> src/main/resources/assets/baum2/items/<sword>.json
"""
import json
import random
from PIL import Image

random.seed(20260711)

# ============================================================================
# Per-sword palette table - THE extension point. One entry per sword of the line.
# Roles: blade body/shadow/light + edge (the two cutting edges & sanded borders),
# ridge (midrib, slightly darker lacquer), guard family, grip leather family,
# pommel reuses guard, boss = the disc pommel's center medallion.
# ============================================================================
SWORDS = {
    "espenklinge": {
        "BLADE": (0xCF, 0xB6, 0x8C),
        "BLADE_DARK": (0xA4, 0x89, 0x60),
        "BLADE_LIT": (0xE8, 0xD5, 0xAC),
        "EDGE": (0xE0, 0xCA, 0x9E),
        "RIDGE": (0xB2, 0x94, 0x66),
        "RIDGE_LIT": (0xD5, 0xBC, 0x8C),
        "GUARD": (0x5E, 0x45, 0x2A),
        "GUARD_DARK": (0x40, 0x2D, 0x1A),
        "GUARD_LIT": (0x7B, 0x5E, 0x3B),
        "GRIP": (0x7E, 0x52, 0x30),
        "GRIP_DARK": (0x59, 0x38, 0x20),
        "GRIP_LIT": (0x9C, 0x6D, 0x44),
        "BOSS": (0xB8, 0x89, 0x3D),
        "BOSS_LIT": (0xDA, 0xAE, 0x62),
        "BOSS_DARK": (0x8F, 0x66, 0x2A),
    },
}

# ============================================================================
# Geometry constants (geo units; 16 = 1 block). Total reach -6.4..21.0 = ~1.7 blocks,
# in family with the Colossal Warclub (~1.6) and Drevathis blade (~1.65).
# x = blade width axis (edges face east/west), z = thickness (flats face north/south).
# ============================================================================
CUBES = {
    #                x0     y0     z0     w     h     t
    "grip_upper":  (-0.8,  -0.4,  -0.8,  1.6,  3.6,  1.6),
    "grip_lower":  (-0.7,  -3.4,  -0.7,  1.4,  3.0,  1.4),
    "guard_bar":   (-4.4,   3.2,  -0.7,  8.8,  1.4,  1.4),
    "guard_cap_l": (-5.4,   3.05, -0.8,  1.0,  1.7,  1.6),
    "guard_cap_r": ( 4.4,   3.05, -0.8,  1.0,  1.7,  1.6),
    "blade_lower": (-1.2,   4.6,  -0.5,  2.4,  7.0,  1.0),
    "blade_upper": (-0.95, 11.6,  -0.45, 1.9,  5.8,  0.9),
    "blade_tip":   (-0.55, 17.4,  -0.4,  1.1,  3.6,  0.8),
    "ridge":       (-0.35,  4.6,  -0.65, 0.7, 11.4,  1.3),
    "pommel_disc": (-1.7,  -6.6,  -0.5,  3.4,  3.2,  1.0),
    "pommel_boss": (-0.5,  -5.5,  -0.7,  1.0,  1.0,  1.4),
}

BONES = {
    # bone -> (parent, pivot, [cube names])
    "root":   (None,   (0.0,  0.0, 0.0), ["grip_upper", "grip_lower"]),
    "guard":  ("root", (0.0,  3.9, 0.0), ["guard_bar", "guard_cap_l", "guard_cap_r"]),
    "blade":  ("root", (0.0,  4.6, 0.0), ["blade_lower", "blade_upper", "blade_tip", "ridge"]),
    "pommel": ("root", (0.0, -3.4, 0.0), ["pommel_disc", "pommel_boss"]),
}

PX_PER_UNIT = 2
TEX = 64


def px(u):
    """geo units -> texture px, always at least 1."""
    return max(1, round(u * PX_PER_UNIT))


# ============================================================================
# Atlas: one painted rect per (cube, face-group). Layout packed left-to-right rows;
# identical across palettes by construction (same packer, same order), which the
# template contract relies on and the main loop asserts.
# ============================================================================
class Atlas:
    def __init__(self):
        self.img = Image.new("RGBA", (TEX, TEX), (0, 0, 0, 0))
        self.x, self.y, self.row_h = 0, 0, 0
        self.rects = {}

    def place(self, name, w, h):
        if self.x + w > TEX:
            self.x, self.y = 0, self.y + self.row_h + 1
            self.row_h = 0
        assert self.y + h <= TEX, f"atlas overflow at {name}"
        self.rects[name] = (self.x, self.y, w, h)
        self.x += w + 1
        self.row_h = max(self.row_h, h)
        return self.rects[name]


def clamp(c):
    return tuple(max(0, min(255, v)) for v in c)


def jit(c, j):
    d = random.randint(-j, j)
    return clamp(tuple(v + d for v in c))


def shift(c, d):
    return clamp(tuple(v + d for v in c))


def build_texture(pal):
    atlas = Atlas()
    img = atlas.img
    put = img.putpixel

    def wood_vertical(rect, base, dark, lit):
        """Clean pale wood: coherent vertical grain streaks (per-column tone held down the
        whole column), gentle per-pixel jitter, and one long lit streak. Deliberately NO
        speckle - an earlier version used 5% dark flecks and the blade read as dirty."""
        x0, y0, w, h = rect
        for xx in range(w):
            t = random.random()
            col = shift(base, -12) if t < 0.18 else (shift(base, 10) if t > 0.82 else base)
            for yy in range(h):
                c = jit(col, 3)
                if random.random() < 0.02:
                    c = shift(dark, 6)  # very sparse grain fleck
                put((x0 + xx, y0 + yy), c + (255,))
        # one long lit streak for life
        gx = random.randint(0, max(0, w - 1))
        for yy in range(0, h, 2):
            if random.random() < 0.7:
                put((x0 + gx, y0 + yy), jit(lit, 4) + (255,))

    def walnut(rect, base, dark, lit):
        """Dark wood, horizontal grain, lit top row / dark bottom row."""
        x0, y0, w, h = rect
        for yy in range(h):
            row = jit(base, 6)
            if yy == 0:
                row = lit
            elif yy == h - 1:
                row = dark
            for xx in range(w):
                c = jit(row, 4)
                if random.random() < 0.08:
                    c = shift(dark, random.randint(-4, 4))
                put((x0 + xx, y0 + yy), c + (255,))

    def leather_wrap(rect, base, dark, lit):
        """Diagonal wrap bands - reads as spiral-wrapped leather at 2px/unit."""
        x0, y0, w, h = rect
        for yy in range(h):
            for xx in range(w):
                m = (xx + yy) % 4
                c = dark if m == 0 else (lit if m == 2 else base)
                put((x0 + xx, y0 + yy), jit(c, 3) + (255,))

    def flat(rect, base, jitter=5):
        x0, y0, w, h = rect
        for yy in range(h):
            for xx in range(w):
                put((x0 + xx, y0 + yy), jit(base, jitter) + (255,))

    def brass(rect, base, lit, dark):
        x0, y0, w, h = rect
        flat(rect, base, 6)
        put((x0, y0), lit + (255,))
        if w > 1 and h > 1:
            put((x0 + w - 1, y0 + h - 1), dark + (255,))
            put((x0 + 1, y0), shift(lit, -12) + (255,))

    def disc(rect, base, dark, lit):
        """Pommel disc broad face: dark rim ring, walnut center."""
        x0, y0, w, h = rect
        for yy in range(h):
            for xx in range(w):
                rim = xx == 0 or yy == 0 or xx == w - 1 or yy == h - 1
                c = dark if rim else jit(base, 5)
                put((x0 + xx, y0 + yy), (jit(c, 3) if rim else c) + (255,))
        if w > 4 and h > 4:
            put((x0 + w // 2, y0 + 1), lit + (255,))  # top-of-disc catchlight

    P = pal
    r = {}

    def face(cube, group, w_units, h_units):
        return atlas.place(f"{cube}:{group}", px(w_units), px(h_units))

    # ---- blade segments: ns = broad flat (clean grain), ew = cutting edge (lighter,
    # sanded), cap = up/down. The tip's top rows go lit - a sharpened point catchlight. ----
    for seg in ("blade_lower", "blade_upper", "blade_tip"):
        _, _, _, w, h, t = CUBES[seg]
        r[f"{seg}:ns"] = face(seg, "ns", w, h)
        r[f"{seg}:ew"] = face(seg, "ew", t, h)
        r[f"{seg}:cap"] = face(seg, "cap", w, t)
        wood_vertical(r[f"{seg}:ns"], P["BLADE"], P["BLADE_DARK"], P["BLADE_LIT"])
        wood_vertical(r[f"{seg}:ew"], P["EDGE"], P["BLADE_DARK"], P["BLADE_LIT"])
        flat(r[f"{seg}:cap"], P["BLADE"])
        if seg == "blade_tip":
            x0, y0, wpx, hpx = r[f"{seg}:ns"]
            for xx in range(wpx):
                put((x0 + xx, y0), jit(P["BLADE_LIT"], 3) + (255,))
            x0, y0, wpx, hpx = r[f"{seg}:ew"]
            for xx in range(wpx):
                put((x0 + xx, y0), jit(P["BLADE_LIT"], 3) + (255,))

    # ---- ridge (midrib): darker lacquer strip, lit spine line on the broad face ----
    _, _, _, w, h, t = CUBES["ridge"]
    r["ridge:ns"] = face("ridge", "ns", w, h)
    r["ridge:ew"] = face("ridge", "ew", t, h)
    r["ridge:cap"] = face("ridge", "cap", w, t)
    wood_vertical(r["ridge:ns"], P["RIDGE"], P["BLADE_DARK"], P["RIDGE_LIT"])
    x0, y0, wpx, hpx = r["ridge:ew"]
    flat(r["ridge:ew"], P["RIDGE"])
    for yy in range(hpx):  # the protruding sides catch a spine highlight
        put((x0 + wpx // 2, y0 + yy), jit(P["RIDGE_LIT"], 4) + (255,))
    flat(r["ridge:cap"], P["RIDGE"])

    # ---- guard: walnut bar + flared caps ----
    _, _, _, w, h, t = CUBES["guard_bar"]
    r["guard_bar:ns"] = face("guard_bar", "ns", w, h)
    r["guard_bar:ew"] = face("guard_bar", "ew", t, h)
    r["guard_bar:ud"] = face("guard_bar", "ud", w, t)
    walnut(r["guard_bar:ns"], P["GUARD"], P["GUARD_DARK"], P["GUARD_LIT"])
    walnut(r["guard_bar:ew"], P["GUARD"], P["GUARD_DARK"], P["GUARD_LIT"])
    walnut(r["guard_bar:ud"], shift(P["GUARD"], 8), P["GUARD_DARK"], P["GUARD_LIT"])
    _, _, _, w, h, t = CUBES["guard_cap_l"]
    r["guard_cap:ns"] = face("guard_cap", "ns", w, h)
    r["guard_cap:ew"] = face("guard_cap", "ew", t, h)
    r["guard_cap:ud"] = face("guard_cap", "ud", w, t)
    for k in ("guard_cap:ns", "guard_cap:ew", "guard_cap:ud"):
        walnut(r[k], shift(P["GUARD"], -6), P["GUARD_DARK"], P["GUARD_LIT"])

    # ---- grips: leather wrap all around, plain caps ----
    for seg in ("grip_upper", "grip_lower"):
        _, _, _, w, h, t = CUBES[seg]
        r[f"{seg}:side"] = face(seg, "side", w, h)
        r[f"{seg}:ud"] = face(seg, "ud", w, t)
        leather_wrap(r[f"{seg}:side"], P["GRIP"], P["GRIP_DARK"], P["GRIP_LIT"])
        flat(r[f"{seg}:ud"], P["GRIP_DARK"])

    # ---- pommel: walnut disc with rim + brass boss ----
    _, _, _, w, h, t = CUBES["pommel_disc"]
    r["pommel_disc:ns"] = face("pommel_disc", "ns", w, h)
    r["pommel_disc:ew"] = face("pommel_disc", "ew", t, h)
    r["pommel_disc:ud"] = face("pommel_disc", "ud", w, t)
    disc(r["pommel_disc:ns"], P["GUARD"], P["GUARD_DARK"], P["GUARD_LIT"])
    walnut(r["pommel_disc:ew"], P["GUARD"], P["GUARD_DARK"], P["GUARD_LIT"])
    walnut(r["pommel_disc:ud"], P["GUARD"], P["GUARD_DARK"], P["GUARD_LIT"])
    _, _, _, w, h, t = CUBES["pommel_boss"]
    r["pommel_boss:ns"] = face("pommel_boss", "ns", w, h)
    r["pommel_boss:ew"] = face("pommel_boss", "ew", t, h)
    brass(r["pommel_boss:ns"], P["BOSS"], P["BOSS_LIT"], P["BOSS_DARK"])
    brass(r["pommel_boss:ew"], P["BOSS"], P["BOSS_LIT"], P["BOSS_DARK"])

    return atlas.rects, atlas.img, r


# ============================================================================
# Geometry
# ============================================================================
def pivot(p):
    """GeckoLib negates X at load time (BakedModelFactory.constructBone) - pre-negate."""
    return [-p[0], p[1], p[2]]


def make_geo(rects):
    def uv(rect):
        return {"uv": [rect[0], rect[1]], "uv_size": [rect[2], rect[3]]}

    def cube(name, faces):
        x, y, z, sx, sy, sz = CUBES[name]
        return {"origin": [-(x + sx), y, z], "size": [sx, sy, sz],
                "uv": {f: uv(rects[rname]) for f, rname in faces.items()}}

    def std_faces(cube_name, ns, ew, cap):
        return {"north": ns, "south": ns, "east": ew, "west": ew, "up": cap, "down": cap}

    face_map = {
        "grip_upper": std_faces("grip_upper", "grip_upper:side", "grip_upper:side", "grip_upper:ud"),
        "grip_lower": std_faces("grip_lower", "grip_lower:side", "grip_lower:side", "grip_lower:ud"),
        "guard_bar": std_faces("guard_bar", "guard_bar:ns", "guard_bar:ew", "guard_bar:ud"),
        "guard_cap_l": std_faces("guard_cap_l", "guard_cap:ns", "guard_cap:ew", "guard_cap:ud"),
        "guard_cap_r": std_faces("guard_cap_r", "guard_cap:ns", "guard_cap:ew", "guard_cap:ud"),
        "blade_lower": std_faces("blade_lower", "blade_lower:ns", "blade_lower:ew", "blade_lower:cap"),
        "blade_upper": std_faces("blade_upper", "blade_upper:ns", "blade_upper:ew", "blade_upper:cap"),
        "blade_tip": std_faces("blade_tip", "blade_tip:ns", "blade_tip:ew", "blade_tip:cap"),
        "ridge": std_faces("ridge", "ridge:ns", "ridge:ew", "ridge:cap"),
        "pommel_disc": std_faces("pommel_disc", "pommel_disc:ns", "pommel_disc:ew", "pommel_disc:ud"),
        "pommel_boss": std_faces("pommel_boss", "pommel_boss:ns", "pommel_boss:ew", "pommel_boss:ns"),
    }

    bones = []
    for bone_name, (parent, piv, cube_names) in BONES.items():
        bone = {"name": bone_name, "pivot": pivot(piv)}
        if parent:
            bone["parent"] = parent
        bone["cubes"] = [cube(n, face_map[n]) for n in cube_names]
        bones.append(bone)
    return bones


# ============================================================================
# Animations
# ============================================================================
def keys(*pairs):
    return {f"{t:g}": v for t, v in pairs}


def make_animation():
    # --- idle: 6s loop. Breathing sway; late-loop grip-settle wrist roll (the accent). ---
    idle = {
        "root": {
            "rotation": keys(
                (0.0, [0.0, 0.0, 0.0]), (1.5, [-0.8, 0.0, 0.6]), (3.0, [0.2, 0.0, 1.1]),
                (4.2, [-0.4, 1.8, 0.5]), (4.7, [0.1, 2.6, 0.3]), (5.2, [0.0, 0.8, 0.2]),
                (6.0, [0.0, 0.0, 0.0]),
            ),
            "position": keys((0.0, [0, 0, 0]), (3.0, [0, 0.3, 0]), (6.0, [0, 0, 0])),
        },
        "blade": {  # soft lag so the blade feels like it has mass
            "rotation": keys(
                (0.0, [0.0, 0.0, 0.0]), (1.7, [-0.4, 0.0, 0.3]), (3.2, [0.15, 0.0, 0.5]),
                (4.45, [0.0, 0.9, 0.2]), (6.0, [0.0, 0.0, 0.0]),
            ),
        },
        "pommel": {
            "rotation": keys((0.0, [0, 0, 0]), (3.0, [0, 0, -0.4]), (4.5, [0, -0.6, 0]),
                             (6.0, [0, 0, 0])),
        },
    }

    # --- attack: 0.55s moulinet. Back-cock, full forward circle, overshoot, settle.
    # Ends on -360 (== 0) so returning to idle can't pop. ---
    attack = {
        "root": {
            "rotation": keys(
                (0.0, [8.0, 0.0, 0.0]), (0.1, [-80.0, 0.0, -3.0]), (0.2, [-190.0, 0.0, -5.0]),
                (0.3, [-300.0, 0.0, -3.0]), (0.38, [-360.0, 0.0, 0.0]),
                (0.47, [-352.0, 0.0, 0.0]), (0.55, [-360.0, 0.0, 0.0]),
            ),
            "position": keys(
                (0.0, [0, 0, 0]), (0.1, [0, 0.4, -0.3]), (0.3, [0, -0.3, 0.9]),
                (0.55, [0, 0, 0]),
            ),
        },
        "blade": {  # whip-lag against the spin
            "rotation": keys(
                (0.0, [0, 0, 0]), (0.12, [6.0, 0, 0]), (0.3, [-5.0, 0, 0]),
                (0.45, [2.0, 0, 0]), (0.55, [0, 0, 0]),
            ),
        },
    }

    # --- attack_mounted: 0.7s cavalry sweep. Windup over the left shoulder, wide
    # horizontal cut with forward reach, follow-through back to rest. ---
    attack_mounted = {
        "root": {
            "rotation": keys(
                (0.0, [0.0, 0.0, 0.0]), (0.14, [-10.0, -35.0, -60.0]),
                (0.22, [-60.0, -12.0, -18.0]), (0.3, [-45.0, 8.0, 28.0]),
                (0.42, [10.0, 30.0, 80.0]),
                (0.55, [-3.0, -5.0, -8.0]), (0.7, [0.0, 0.0, 0.0]),
            ),
            "position": keys(
                (0.0, [0, 0, 0]), (0.14, [0, 0.5, -0.4]), (0.3, [0, -0.5, 1.4]),
                (0.42, [0, -0.7, 0.8]), (0.7, [0, 0, 0]),
            ),
        },
        "blade": {
            "rotation": keys(
                (0.0, [0, 0, 0]), (0.17, [0, -6.0, -8.0]), (0.34, [4.0, 3.0, 10.0]),
                (0.5, [0, 2.0, 4.0]), (0.7, [0, 0, 0]),
            ),
        },
    }

    return {
        "format_version": "1.8.0",
        "animations": {
            "animation.sword_template.idle": {
                "loop": True, "animation_length": 6.0, "bones": idle,
            },
            "animation.sword_template.attack": {
                "loop": False, "animation_length": 0.55, "bones": attack,
            },
            "animation.sword_template.attack_mounted": {
                "loop": False, "animation_length": 0.7, "bones": attack_mounted,
            },
        },
    }


# ============================================================================
# Flat 16x16 GUI icon: diagonal longsword, grip bottom-left (established icon axis).
# ============================================================================
def make_icon(pal):
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))

    def put(x, y, c):
        if 0 <= x < 16 and 0 <= y < 16:
            img.putpixel((x, y), c + (255,))

    P = pal
    # blade: anti-diagonal cells (5,10)..(13,2), body + ridge shadow upper-right,
    # lit cutting edge lower-left - same axis convention as gold_sword/the flutes
    for i in range(9):
        x, y = 5 + i, 10 - i
        put(x, y, jit(P["BLADE"], 5))
        put(x + 1, y, jit(P["RIDGE"], 4))
        put(x, y + 1, P["EDGE"] if i % 3 else P["BLADE_LIT"])
    put(14, 1, P["BLADE_LIT"])                  # tip glint
    put(13, 1, jit(P["BLADE"], 4))
    # crossguard: straight bar PERPENDICULAR to the blade axis, crossing it at (4,11)
    for d in (-2, -1, 0, 1, 2):
        c = P["GUARD_LIT"] if d == 0 else (P["GUARD_DARK"] if abs(d) == 2 else P["GUARD"])
        put(4 + d, 11 + d, c)
    # grip: two leather cells continuing the blade axis below the guard
    put(3, 12, P["GRIP_LIT"])
    put(2, 13, P["GRIP"])
    # disc pommel: 2x2 walnut block at the very end, one brass glint pixel
    put(0, 14, P["GUARD"])
    put(1, 15, P["GUARD"])
    put(0, 15, P["GUARD_DARK"])
    put(1, 14, P["BOSS_LIT"])
    return img


# ============================================================================
# Per-sword JSONs: display-transform base model (the "trident_in_hand of this sword"),
# flat icon model, and the item model definition (select by display context - same
# schema as vanilla items/trident.json and our own drevathis_cursed_blade.json).
#
# Display transforms rotate around the item-cube center == the geo origin == the fist
# (see module docstring). rotationXYZ order: Z applied first, then Y, then X.
# [_, -90, _] turns the blade's broad face to match where a vanilla sprite sword's
# face ends up; the X entry is the forward lean. Derived from vanilla handheld's
# net orientation, then tuned against tools/render_item_display_preview.py renders.
# ============================================================================
def make_base_model(name):
    return {
        "gui_light": "front",
        "textures": {"particle": f"baum2:item/{name}_geo"},
        "display": {
            "thirdperson_righthand": {
                "rotation": [-10, -90, 0], "translation": [0, 3.5, 1],
                "scale": [0.85, 0.85, 0.85],
            },
            "thirdperson_lefthand": {
                "rotation": [-10, 90, 0], "translation": [0, 3.5, 1],
                "scale": [0.85, 0.85, 0.85],
            },
            "firstperson_righthand": {
                "rotation": [20, -95, 5], "translation": [-1, 3, 1],
                "scale": [0.68, 0.68, 0.68],
            },
            "firstperson_lefthand": {
                "rotation": [20, 85, -5], "translation": [1, 3, 1],
                "scale": [0.68, 0.68, 0.68],
            },
            "head": {
                "rotation": [0, -90, 0], "translation": [0, 7, 0], "scale": [1, 1, 1],
            },
        },
    }


def make_icon_model(name):
    return {
        "parent": "minecraft:item/handheld",
        "textures": {"layer0": f"baum2:item/{name}"},
    }


def make_item_definition(name):
    return {
        "model": {
            "type": "minecraft:select",
            "property": "minecraft:display_context",
            "cases": [
                {
                    "when": ["gui", "ground", "fixed", "on_shelf"],
                    "model": {"type": "minecraft:model", "model": f"baum2:item/{name}"},
                }
            ],
            "fallback": {
                "type": "minecraft:special",
                "base": f"baum2:item/{name}_base",
                "model": {"type": "geckolib:geckolib"},
            },
        }
    }


# ============================================================================
# Build everything
# ============================================================================
first_layout = None
for sword, pal in SWORDS.items():
    random.seed(20260711)  # identical noise per sword -> only the palette differs
    layout, img, rects = build_texture(pal)
    if first_layout is None:
        first_layout = layout
        geo_rects = rects
    else:
        assert layout == first_layout, f"atlas layout diverged for {sword}"
    img.save(f"{sword}_geo.png")
    make_icon(pal).save(f"{sword}.png")
    with open(f"{sword}_base.json", "w", encoding="utf-8") as f:
        json.dump(make_base_model(sword), f, indent=2)
    with open(f"{sword}_icon_model.json", "w", encoding="utf-8") as f:
        json.dump(make_icon_model(sword), f, indent=2)
    with open(f"{sword}_item_def.json", "w", encoding="utf-8") as f:
        json.dump(make_item_definition(sword), f, indent=2)

bones = make_geo(geo_rects)
geo = {
    "format_version": "1.12.0",
    "minecraft:geometry": [
        {
            "description": {
                "identifier": "geometry.sword_template",
                "texture_width": TEX,
                "texture_height": TEX,
                "visible_bounds_width": 3,
                "visible_bounds_height": 4,
                "visible_bounds_offset": [0, 0.5, 0],
            },
            "bones": bones,
        }
    ],
}
with open("sword_template.geo.json", "w", encoding="utf-8") as f:
    json.dump(geo, f, indent=2)

with open("sword_template.animation.json", "w", encoding="utf-8") as f:
    json.dump(make_animation(), f, indent=2)

total_cubes = sum(len(b["cubes"]) for b in bones)
reach = (min(c[1] for c in CUBES.values()),
         max(c[1] + c[4] for c in CUBES.values()))
print(f"bones: {len(bones)}, cubes: {total_cubes}, reach y {reach[0]}..{reach[1]} "
      f"({(reach[1] - reach[0]) / 16:.2f} blocks)")
print(f"swords: {', '.join(SWORDS)} (texture {TEX}x{TEX}, icon 16x16, 3 JSONs each)")
