"""Builds the shared "Fallen Comet Stone" GeckoLib template + both stone-boss textures.

This is the GeckoLib rework of the two stationary stone mini-bosses (Stone of Spiders, Stone
of Zombies), replacing the old blocky 7-cuboid HulkingCocoonStoneEntityModel. Design brief:
the stone should read as a COMET THAT CRASHED INTO THE GROUND - a tall, angular monolith
tilted hard off vertical with its lower end buried, a rubble/crater ring blasted around the
impact point, glowing energy veins up the rock face, and a few small glowing shards knocked
loose that still float and slowly orbit the stone (the idle animation - a crashed rock can't
walk, so the orbiting shards + tilt are what make it feel alive and "charged").

TEMPLATE CONTRACT (how to add a third stone later):
  - geometry + idle animation are SHARED: fallen_comet_stone.geo.json /
    fallen_comet_stone.animation.json (one file each, used by every stone boss via
    FallenCometStoneGeoModel's withAltModel/withAltAnimations).
  - each stone differs ONLY by its texture: add a palette dict below (6 roles: ROCK_SHADOW /
    ROCK / ROCK_PALE / FISSURE / GLOW / GLOW_DIM - reuse the boss's ratified palette from
    docs/visual-style-guide.md), add it to VARIANTS, rerun this script, copy the new PNG to
    assets/baum2/textures/entity/<entity_name>.png. No Java/geometry/animation changes needed
    beyond registering the entity with the shared renderer.
  - the texture atlas layout must stay IDENTICAL across variants (same UV rects) - that's why
    build_texture() reseeds the RNG and rebuilds the atlas from scratch per palette: every
    alloc happens in the same order, so rects line up pixel-for-pixel across all PNGs.

Bone-name contract (referenced by fallen_comet_stone.animation.json and both stone entities'
animation controllers): stone (root), comet/comet_mid/comet_top (the tilted monolith's three
bend segments), crater, rim_slab_1/2, shards (orbit parent), shard_1/2/3 (bob+spin children).
No "head" bone on purpose - a rock must not head-turn toward the player.

Coordinate conventions: authored space has +Y up (ground at 0), -Z front, +X = the entity's
LEFT. The pivot()/cube() helpers apply the same X-negation GeckoLib cancels at load time
(verified mechanism, see tools/gen_zombie_colossus.py and docs/fabric-modding.md part H - do
not hand-negate anything). Unlike the biped bosses, the comet's buried base deliberately dips
BELOW y=0: the entity stands on the ground, so sub-ground cubes clip into the terrain and sell
the "embedded by impact" look.

Run from the repo root: `python tools/gen_fallen_comet_stone.py` (requires Pillow), preview:
  python tools/render_geckolib_preview.py --model fallen_comet_stone --tex stone_of_spiders.png
  python tools/render_geckolib_preview.py --model fallen_comet_stone --tex stone_of_zombies.png --anim idle --times 0,3,6,9
then copy the outputs to:
  src/main/resources/assets/baum2/geckolib/models/entity/fallen_comet_stone.geo.json
  src/main/resources/assets/baum2/geckolib/animations/entity/fallen_comet_stone.animation.json
  src/main/resources/assets/baum2/textures/entity/stone_of_spiders.png
  src/main/resources/assets/baum2/textures/entity/stone_of_zombies.png
"""
import json
import random
from PIL import Image, ImageDraw

SEED = 20260709
PIX = 2  # texture pixels per model unit

# ============================================================================
# Palettes - both are the ratified stone palettes from docs/visual-style-guide.md
# (13.3 Fused Stone/Larval Glow, 15.2 Blight Stone/Plague Glow). The comet rework
# drops the old husk/silk roles (the shape is pure rock now) and finally uses the
# "Pale" highlight hexes 13.3/15.2 reserved for a non-placeholder pass.
# ============================================================================
PALETTES = {
    "stone_of_spiders": {
        "ROCK_SHADOW": (0x3A, 0x36, 0x2E),
        "ROCK": (0x5C, 0x57, 0x4A),
        "ROCK_PALE": (0x7A, 0x75, 0x66),
        "FISSURE": (0x2A, 0x25, 0x1C),
        "GLOW": (0xC4, 0xE0, 0x64),
        "GLOW_DIM": (0x6E, 0x8A, 0x2E),
    },
    "stone_of_zombies": {
        "ROCK_SHADOW": (0x26, 0x30, 0x1F),
        "ROCK": (0x43, 0x59, 0x30),
        "ROCK_PALE": (0x64, 0x7A, 0x47),
        "FISSURE": (0x17, 0x20, 0x0F),
        "GLOW": (0x3D, 0xFF, 0x7E),
        "GLOW_DIM": (0x1B, 0x8A, 0x45),
    },
}


# ============================================================================
# Atlas + painter helpers (same pattern as the other gen scripts - kept as a
# local copy on purpose: these are one-shot generators, sharing code would let
# a refactor reshuffle another script's RNG sequence and change its output)
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


def build_texture(pal):
    """Paints the full atlas for one palette. Deterministic: reseeds, allocs in fixed order.
    Returns (rects-by-name dict, PIL image, used_w, used_h)."""
    random.seed(SEED)
    atlas = Atlas()

    def px(rect, x, y, color):
        if 0 <= x < rect[2] and 0 <= y < rect[3]:
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
        _, _, w, h = rect
        for yy in range(h):
            for xx in range(w):
                if random.random() < density:
                    px(rect, xx, yy, color)

    def crack_line(rect, color):
        """One jagged dark fissure running mostly downward."""
        _, _, w, h = rect
        x = random.randint(2, max(2, w - 3))
        y = random.randint(0, max(0, h // 4))
        length = random.randint(h // 2, h - 2)
        for _ in range(length):
            px(rect, x, y, color)
            if random.random() < 0.3:
                px(rect, x + random.choice((-1, 1)), y, color)
            x += random.choice((-1, 0, 0, 1))
            x = max(1, min(w - 2, x))
            y += 1
            if y >= h:
                break

    def glow_vein(rect, pal):
        """A jagged glowing energy vein: bright core polyline with dim-edge pixels and a
        couple of short side branches - the comet's 'still charged' cue."""
        _, _, w, h = rect
        x = random.randint(3, max(3, w - 4))
        y = 0
        while y < h:
            px(rect, x, y, pal["GLOW"])
            for dx in (-1, 1):
                if random.random() < 0.75:
                    px(rect, x + dx, y, pal["GLOW_DIM"])
            if random.random() < 0.12:  # short horizontal branch
                bx = x
                for i in range(random.randint(2, 4)):
                    bx += random.choice((-1, 1))
                    px(rect, bx, y, pal["GLOW_DIM"])
            x += random.choice((-1, 0, 0, 1))
            x = max(1, min(w - 2, x))
            y += 1

    def glow_pool(rect, pal, rows=4):
        """Dim glow gathering along the bottom rows (impact heat near the buried end)."""
        _, _, w, h = rect
        for yy in range(max(0, h - rows), h):
            strength = (yy - (h - rows)) / max(1, rows - 1)
            for xx in range(w):
                if random.random() < 0.10 + 0.25 * strength:
                    px(rect, xx, yy, pal["GLOW_DIM"])
                if random.random() < 0.04 * strength:
                    px(rect, xx, yy, pal["GLOW"])

    def rock(w, h, veins=0, cracks=1, pooled=False, vgrad=(1.06, 0.88)):
        r = atlas.alloc(w, h)
        fill_noise(r, pal["ROCK"], vgrad=vgrad)
        speckle(r, pal["ROCK_SHADOW"], 0.08)
        speckle(r, pal["ROCK_PALE"], 0.05)
        for _ in range(cracks):
            crack_line(r, pal["FISSURE"])
        for _ in range(veins):
            glow_vein(r, pal)
        if pooled:
            glow_pool(r, pal)
        return r

    def rock_top(w, h):
        """Sun-catching upward face: brighter, more pale chips, no veins."""
        r = atlas.alloc(w, h)
        fill_noise(r, pal["ROCK"], vgrad=(1.22, 1.02))
        speckle(r, pal["ROCK_PALE"], 0.14)
        speckle(r, pal["ROCK_SHADOW"], 0.04)
        crack_line(r, pal["FISSURE"])
        return r

    def rock_bottom(w, h):
        r = atlas.alloc(w, h)
        fill_noise(r, pal["ROCK_SHADOW"], vgrad=(1.0, 0.86))
        speckle(r, pal["FISSURE"], 0.10)
        return r

    def rubble(w, h, glowing=False):
        """Crater debris: darker broken rock, optionally heat-tinged."""
        r = atlas.alloc(w, h)
        fill_noise(r, pal["ROCK"], vgrad=(1.0, 0.84))
        speckle(r, pal["FISSURE"], 0.12)
        speckle(r, pal["ROCK_SHADOW"], 0.08)
        if glowing:
            speckle(r, pal["GLOW_DIM"], 0.06)
            speckle(r, pal["GLOW"], 0.015)
        return r

    def shard(w, h):
        """Small floating fragment: rock core, glow-rimmed (it broke off the charged comet)."""
        r = atlas.alloc(w, h)
        fill_noise(r, pal["ROCK"], vgrad=(1.1, 0.9))
        for xx in range(w):
            for yy in range(h):
                if xx in (0, w - 1) or yy in (0, h - 1):
                    if random.random() < 0.65:
                        px(r, xx, yy, pal["GLOW_DIM"])
                    if random.random() < 0.18:
                        px(r, xx, yy, pal["GLOW"])
        return r

    rects = {}

    # --- comet monolith faces (5 stacked cubes; front carries the veins) ---
    for name, (sx, sy, sz) in COMET_CUBES.items():
        w, h, d = sx * PIX, sy * PIX, sz * PIX
        lower = name in ("c_base", "c_lower")  # buried/impact end: pooled glow
        rects[name + ":north"] = rock(w, h, veins=2, cracks=1, pooled=lower)
        rects[name + ":south"] = rock(w, h, veins=0, cracks=2)
        rects[name + ":east"] = rock(d, h, veins=1, cracks=1, pooled=lower)
        rects[name + ":west"] = rock(d, h, veins=0, cracks=2)
        rects[name + ":up"] = rock_top(w, d)
        rects[name + ":down"] = rock_bottom(w, d)

    # --- crater rubble (one paint per size class, reused by every face of that chunk) ---
    for name, (sx, sy, sz) in CRATER_CUBES.items():
        rects[name + ":side"] = rubble(sx * PIX, sy * PIX, glowing=True)
        rects[name + ":top"] = rubble(sx * PIX, sz * PIX, glowing=True)

    for name, (sx, sy, sz) in RIM_SLABS.items():
        rects[name + ":side"] = rubble(sx * PIX, sy * PIX, glowing=True)
        rects[name + ":top"] = rubble(sx * PIX, sz * PIX, glowing=True)

    # --- floating shards ---
    for name, (sx, sy, sz) in SHARD_CUBES.items():
        rects[name + ":face"] = shard(int(round(sx * PIX)), int(round(sy * PIX)))

    return rects, atlas


# ============================================================================
# Geometry (authored space: +Y up from ground at 0, -Z front, +X = entity's left)
# ============================================================================
# Monolith cube sizes (name -> size). Positions are authored below; the stack is
# slightly staggered per cube so the silhouette reads as one irregular rock, not
# a wedding cake. Total column ~49 units (~3 blocks) before the tilt.
COMET_CUBES = {
    "c_base": (20, 14, 18),
    "c_lower": (24, 16, 20),
    "c_mid": (20, 15, 16),
    "c_upper": (16, 13, 13),
    "c_tip": (10, 9, 8),
}
COMET_POS = {  # authored min corner per cube (before the segment bones' tilts)
    "c_base": (-10, -4, -9),    # dips below ground on purpose: buried impact end
    "c_lower": (-12, 8, -10),
    "c_mid": (-9, 22, -8.5),    # staggered +x
    "c_upper": (-8.5, 35, -6),  # staggered -x / +z
    "c_tip": (-6, 46, -4.5),
}
# The monolith is split across three chained bones (comet -> comet_mid -> comet_top), each
# adding a small extra rotation: the column bends like one jagged crystal instead of reading
# as a neatly-stacked cube pyramid. Cubes overlap their neighbor segment vertically so the
# joints stay closed under those rotations.
COMET_SEGMENTS = {
    "comet": ("c_base", "c_lower"),
    "comet_mid": ("c_mid",),
    "comet_top": ("c_upper", "c_tip"),
}

CRATER_CUBES = {  # name -> size; ring of flat debris around the impact point
    "k_1": (8, 3, 7),
    "k_2": (7, 2, 6),
    "k_3": (9, 3, 8),
    "k_4": (6, 2, 6),
    "k_5": (7, 3, 7),
    "k_6": (5, 2, 5),
}
CRATER_POS = {
    "k_1": (12, 0, -18),
    "k_2": (-18, 0, -13),
    "k_3": (-16, 0, 10),
    "k_4": (14, 0, 12),
    "k_5": (-3, 0, -21),
    "k_6": (18, 0, -2),
}

RIM_SLABS = {  # upturned ground plates at the crater edge (own bones: they're tilted)
    "rim_slab_1": (11, 2, 8),
    "rim_slab_2": (9, 2, 7),
}

SHARD_CUBES = {  # small knocked-loose fragments, orbiting in the idle animation
    "shard_1": (5, 5, 5),
    "shard_2": (4, 4, 4),
    "shard_3": (3, 3, 3),
}
# orbit-frame placement: pivot = shard center (so the self-spin rotates in place
# while the parent "shards" bone's y-rotation carries it around the stone)
SHARD_PIVOTS = {
    "shard_1": (16, 13, 5),
    "shard_2": (-16, 26, -8),
    "shard_3": (8, 38, -14),
}


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

    bones = []

    # root: lets a future stone animation shake/sink the whole thing at once
    bones.append({"name": "stone", "pivot": pivot((0, 0, 0)), "cubes": []})

    # --- the crashed monolith: three chained segment bones. The root segment carries
    # the big "fell out of the sky at an angle" tilt at the ground contact point; the
    # mid/top segments each add a small extra bend so the silhouette reads as one
    # jagged crystal, not a stacked cube pyramid. ---
    def comet_faces(name):
        return {"north": name + ":north", "south": name + ":south",
                "east": name + ":east", "west": name + ":west",
                "up": name + ":up", "down": name + ":down"}

    segment_setup = {
        "comet": ("stone", (0, 1, 2), [23.0, 0.0, -11.0]),
        "comet_mid": ("comet", (0, 24, 0), [4.0, 7.0, 4.0]),
        "comet_top": ("comet_mid", (0, 37, 0), [6.0, -9.0, 5.0]),
    }
    for seg, cube_names in COMET_SEGMENTS.items():
        parent, piv, rot = segment_setup[seg]
        bones.append({
            "name": seg, "parent": parent, "pivot": pivot(piv), "rotation": rot,
            "cubes": [cube(n, COMET_POS[n], COMET_CUBES[n], comet_faces(n))
                      for n in cube_names],
        })

    # --- crater rubble ring (static, flat on the ground) ---
    crater_cubes = []
    for name, size in CRATER_CUBES.items():
        crater_cubes.append(cube(name, CRATER_POS[name], size, {
            "north": name + ":side", "south": name + ":side",
            "east": name + ":side", "west": name + ":side",
            "up": name + ":top"}))
    bones.append({"name": "crater", "parent": "stone",
                  "pivot": pivot((0, 0, 0)), "cubes": crater_cubes})

    # --- upturned rim slabs (tilted, so they need their own bones) ---
    bones.append({
        "name": "rim_slab_1", "parent": "stone", "pivot": pivot((0, 0, -19)),
        "rotation": [-16.0, 8.0, 0.0],
        "cubes": [cube("rim_slab_1", (-5.5, 0, -23), RIM_SLABS["rim_slab_1"], {
            "north": "rim_slab_1:side", "south": "rim_slab_1:side",
            "east": "rim_slab_1:side", "west": "rim_slab_1:side",
            "up": "rim_slab_1:top", "down": "rim_slab_1:top"})],
    })
    bones.append({
        "name": "rim_slab_2", "parent": "stone", "pivot": pivot((17, 0, 8)),
        "rotation": [10.0, -20.0, -14.0],
        "cubes": [cube("rim_slab_2", (13, 0, 5), RIM_SLABS["rim_slab_2"], {
            "north": "rim_slab_2:side", "south": "rim_slab_2:side",
            "east": "rim_slab_2:side", "west": "rim_slab_2:side",
            "up": "rim_slab_2:top", "down": "rim_slab_2:top"})],
    })

    # --- orbiting shards: parent bone spins around Y (idle anim), children bob+self-spin ---
    bones.append({"name": "shards", "parent": "stone", "pivot": pivot((0, 18, 0)), "cubes": []})
    for name, size in SHARD_CUBES.items():
        cx, cy, cz = SHARD_PIVOTS[name]
        sx, sy, sz = size
        bones.append({
            "name": name, "parent": "shards", "pivot": pivot((cx, cy, cz)),
            "cubes": [cube(name, (cx - sx / 2, cy - sy / 2, cz - sz / 2), size, {
                "north": name + ":face", "south": name + ":face",
                "east": name + ":face", "west": name + ":face",
                "up": name + ":face", "down": name + ":face"})],
        })

    return bones


# ============================================================================
# Idle animation: 12s seamless loop. The monolith itself never moves (it's a
# crashed rock); life comes from the shards orbiting + bobbing + self-spinning.
# ============================================================================
def make_animation():
    def spin(axis_index, turns):
        start = [0, 0, 0]
        end = [0, 0, 0]
        end[axis_index] = 360.0 * turns
        return {"rotation": {"0": start, "12": end}}

    def bob(amplitude, phase):
        """4-key vertical sine, phase-shifted per shard so they don't bob in sync."""
        keys = {}
        for i in range(5):
            t = i * 3.0
            import math
            y = amplitude * math.sin(2 * math.pi * (i / 4.0) + phase)
            keys[f"{t:g}"] = [0, round(y, 3), 0]
        # force exact loop closure
        keys["12"] = keys["0"]
        return {"position": keys}

    def merge(*dicts):
        out = {}
        for d in dicts:
            out.update(d)
        return out

    import math
    bones = {
        "shards": spin(1, 1),  # one full orbit per loop
        "shard_1": merge(spin(0, 2), bob(1.5, 0.0)),
        "shard_2": merge(spin(2, -2), bob(1.2, 2 * math.pi / 3)),
        "shard_3": merge(spin(0, -3), bob(1.0, 4 * math.pi / 3)),
    }
    return {
        "format_version": "1.8.0",
        "animations": {
            "animation.fallen_comet_stone.idle": {
                "loop": True, "animation_length": 12.0, "bones": bones,
            }
        },
    }


# ============================================================================
# Build everything
# ============================================================================
first_rects = None
atlases = {}
for variant, pal in PALETTES.items():
    rects, atlas = build_texture(pal)
    if first_rects is None:
        first_rects = rects
    else:
        assert rects == first_rects, f"atlas layout diverged for {variant}"
    atlases[variant] = atlas

TEX_W = max(first_rects[k][0] + first_rects[k][2] for k in first_rects)
TEX_H = max(first_rects[k][1] + first_rects[k][3] for k in first_rects)

for variant, atlas in atlases.items():
    atlas.img.crop((0, 0, TEX_W, TEX_H)).save(f"{variant}.png")

bones = make_geo(first_rects)
geo = {
    "format_version": "1.12.0",
    "minecraft:geometry": [
        {
            "description": {
                "identifier": "geometry.fallen_comet_stone",
                "texture_width": TEX_W,
                "texture_height": TEX_H,
                "visible_bounds_width": 7,
                "visible_bounds_height": 5,
                "visible_bounds_offset": [0, 2, 0],
            },
            "bones": bones,
        }
    ],
}
with open("fallen_comet_stone.geo.json", "w", encoding="utf-8") as f:
    json.dump(geo, f, indent=2)

with open("fallen_comet_stone.animation.json", "w", encoding="utf-8") as f:
    json.dump(make_animation(), f, indent=2)

print(f"bones: {len(bones)}, cubes: {sum(len(b['cubes']) for b in bones)}")
print(f"texture: {TEX_W}x{TEX_H} ({', '.join(PALETTES)})")
