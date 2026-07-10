#!/usr/bin/env python3
"""Generates Heimgrund's starting village as a Minecraft structure template.

Outputs:
  - src/main/resources/data/baum2/structure/village_heimgrund.nbt  (the template)
  - tools/preview_village_*.png                                    (isometric previews)

The village is authored as code (a voxel dict) so it can be iterated visually via the
rendered previews without a live client. Style spec: docs/visual-style-guide.md
(Heimgrund starting village section). The template's min corner must be placed by
VillageStamper so that the village center sits at world (0, 64, 0).

Run from the repo root:  python tools/gen_village.py
"""

import gzip
import io
import struct
import sys
from PIL import Image, ImageDraw

DATA_VERSION = 4671  # 1.21.11 (read from a template the game itself saved)

# Template size. Ground layer is y=0 in template space (world y=64: replaces the
# clearing's grass surface); buildings rise above it.
SX, SY, SZ = 91, 19, 91
CX, CZ = SX // 2, SZ // 2  # village center in template space (45, 45)

# ---------------------------------------------------------------------------------
# Voxel model: (x, y, z) -> "minecraft:id" or "minecraft:id[prop=v,prop=v]"
# ---------------------------------------------------------------------------------

blocks = {}


def put(x, y, z, state):
    if 0 <= x < SX and 0 <= y < SY and 0 <= z < SZ:
        blocks[(x, y, z)] = state


def fill(x1, y1, z1, x2, y2, z2, state):
    for x in range(min(x1, x2), max(x1, x2) + 1):
        for y in range(min(y1, y2), max(y1, y2) + 1):
            for z in range(min(z1, z2), max(z1, z2) + 1):
                put(x, y, z, state)


def hollow_walls(x1, z1, x2, z2, y1, y2, state):
    """Four walls of a rectangle footprint (no floor/ceiling)."""
    for y in range(y1, y2 + 1):
        for x in range(x1, x2 + 1):
            put(x, y, z1, state)
            put(x, y, z2, state)
        for z in range(z1, z2 + 1):
            put(x1, y, z, state)
            put(x2, y, z, state)


def clear(x1, y1, z1, x2, y2, z2):
    for x in range(min(x1, x2), max(x1, x2) + 1):
        for y in range(min(y1, y2), max(y1, y2) + 1):
            for z in range(min(z1, z2), max(z1, z2) + 1):
                blocks.pop((x, y, z), None)


# ---------------------------------------------------------------------------------
# NBT writer (just enough of the format for structure templates)
# ---------------------------------------------------------------------------------


def _tag_string(buf, s):
    raw = s.encode("utf-8")
    buf.write(struct.pack(">H", len(raw)))
    buf.write(raw)


def _named(buf, tag_type, name):
    buf.write(struct.pack(">b", tag_type))
    _tag_string(buf, name)


def parse_state(state):
    if "[" not in state:
        return state, {}
    name, props = state[:-1].split("[", 1)
    return name, dict(kv.split("=", 1) for kv in props.split(","))


def write_nbt(path):
    palette = []
    palette_index = {}
    entries = []
    for (x, y, z), state in sorted(blocks.items()):
        if state not in palette_index:
            palette_index[state] = len(palette)
            palette.append(state)
        entries.append((x, y, z, palette_index[state]))

    buf = io.BytesIO()
    _named(buf, 10, "")  # root compound

    _named(buf, 9, "size")
    buf.write(struct.pack(">bi", 3, 3))
    buf.write(struct.pack(">iii", SX, SY, SZ))

    _named(buf, 9, "entities")
    buf.write(struct.pack(">bi", 0, 0))  # empty list

    _named(buf, 9, "blocks")
    buf.write(struct.pack(">bi", 10, len(entries)))
    for x, y, z, idx in entries:
        _named_list_pos(buf, x, y, z, idx)

    _named(buf, 9, "palette")
    buf.write(struct.pack(">bi", 10, len(palette)))
    for state in palette:
        name, props = parse_state(state)
        if props:
            _named(buf, 10, "Properties")
            for key, value in sorted(props.items()):
                _named(buf, 8, key)
                _tag_string(buf, value)
            buf.write(b"\x00")
        _named(buf, 8, "Name")
        _tag_string(buf, name)
        buf.write(b"\x00")  # end of palette-entry compound

    _named(buf, 3, "DataVersion")
    buf.write(struct.pack(">i", DATA_VERSION))

    buf.write(b"\x00")  # end of root compound

    with gzip.open(path, "wb") as f:
        f.write(buf.getvalue())
    print(f"wrote {path}: {len(entries)} blocks, {len(palette)} palette entries")


def _named_list_pos(buf, x, y, z, idx):
    _named(buf, 9, "pos")
    buf.write(struct.pack(">bi", 3, 3))
    buf.write(struct.pack(">iii", x, y, z))
    _named(buf, 3, "state")
    buf.write(struct.pack(">i", idx))
    buf.write(b"\x00")  # end of block compound


# ---------------------------------------------------------------------------------
# Isometric preview renderer (PIL)
# ---------------------------------------------------------------------------------

COLORS = {
    "minecraft:grass_block": (98, 148, 70),
    "minecraft:dirt_path": (148, 122, 65),
    "minecraft:gravel": (128, 124, 122),
    "minecraft:stone_bricks": (122, 122, 122),
    "minecraft:chiseled_stone_bricks": (110, 110, 112),
    "minecraft:cracked_stone_bricks": (105, 105, 105),
    "minecraft:cobblestone": (110, 108, 106),
    "minecraft:mossy_cobblestone": (100, 118, 90),
    "minecraft:andesite": (132, 134, 133),
    "minecraft:polished_andesite": (140, 142, 141),
    "minecraft:spruce_planks": (110, 82, 48),
    "minecraft:spruce_log": (58, 38, 20),
    "minecraft:stripped_spruce_log": (116, 90, 52),
    "minecraft:spruce_stairs": (110, 82, 48),
    "minecraft:spruce_slab": (110, 82, 48),
    "minecraft:spruce_fence": (110, 82, 48),
    "minecraft:spruce_trapdoor": (104, 78, 46),
    "minecraft:dark_oak_planks": (66, 43, 20),
    "minecraft:dark_oak_stairs": (66, 43, 20),
    "minecraft:dark_oak_slab": (66, 43, 20),
    "minecraft:dark_oak_log": (52, 34, 16),
    "minecraft:deepslate_tile_stairs": (54, 54, 58),
    "minecraft:deepslate_tile_slab": (54, 54, 58),
    "minecraft:deepslate_tiles": (54, 54, 58),
    "minecraft:polished_deepslate": (72, 72, 76),
    "minecraft:deepslate_brick_wall": (66, 66, 70),
    "minecraft:cobblestone_wall": (110, 108, 106),
    "minecraft:lantern": (255, 200, 90),
    "minecraft:glass_pane": (200, 225, 230),
    "minecraft:water": (52, 92, 180),
    "minecraft:oak_leaves": (60, 110, 45),
    "minecraft:spruce_leaves": (45, 85, 45),
    "minecraft:azalea_leaves": (70, 125, 55),
    "minecraft:flowering_azalea_leaves": (110, 130, 80),
    "minecraft:moss_block": (85, 115, 60),
    "minecraft:copper_block": (190, 105, 70),
    "minecraft:oxidized_copper": (80, 155, 125),
    "minecraft:weathered_copper": (108, 145, 106),
    "minecraft:oxidized_cut_copper": (85, 150, 122),
    "minecraft:oxidized_cut_copper_stairs": (85, 150, 122),
    "minecraft:oxidized_cut_copper_slab": (85, 150, 122),
    "minecraft:hay_block": (200, 170, 60),
    "minecraft:barrel": (120, 90, 55),
    "minecraft:composter": (105, 75, 45),
    "minecraft:flower_pot": (120, 70, 50),
    "minecraft:torch": (255, 220, 120),
    "minecraft:chain": (60, 65, 75),
    "minecraft:diorite": (222, 222, 224),
    "minecraft:polished_diorite": (230, 230, 232),
    "minecraft:chiseled_deepslate": (60, 60, 64),
    "minecraft:cobbled_deepslate": (70, 70, 74),
    "minecraft:stone_brick_slab": (122, 122, 122),
    "minecraft:andesite_wall": (132, 134, 133),
    "minecraft:dark_oak_wood": (52, 34, 16),
    "minecraft:dark_oak_fence": (66, 43, 20),
    "minecraft:soul_lantern": (120, 200, 255),
    "minecraft:lily_pad": (40, 120, 50),
    "minecraft:campfire": (220, 140, 50),
    "minecraft:potted_allium": (190, 120, 220),
    "minecraft:potted_lily_of_the_valley": (235, 240, 235),
    "minecraft:moss_carpet": (85, 115, 60),
    "minecraft:vine": (60, 105, 45),
    "minecraft:smooth_stone": (158, 158, 158),
}
DEFAULT_COLOR = (200, 60, 200)  # loud magenta: material missing from COLORS


def render_iso(path, view=0):
    """view 0..3 = rotate the model 90° steps so all sides can be checked."""
    def rot(x, z):
        for _ in range(view):
            x, z = z, SX - 1 - x
        return x, z

    tile = 10
    half = tile // 2
    width = (SX + SZ) * half + 2 * tile
    height = (SX + SZ) * half // 2 + SY * half + 3 * tile
    img = Image.new("RGB", (width, height), (24, 26, 30))
    draw = ImageDraw.Draw(img)
    origin_x = SZ * half + tile
    origin_y = tile * 2

    def screen(x, y, z):
        sx = origin_x + (x - z) * half
        sy = origin_y + (x + z) * half // 2 + (SY - y) * half
        return sx, sy

    # Rotate into view space first, then paint back-to-front: ascending x+z layers,
    # ascending y within a layer, so nearer and higher blocks draw last.
    rotated = {}
    for (wx, wy, wz), state in blocks.items():
        x, z = rot(wx, wz)
        rotated[(x, wy, z)] = state
    for x, wy, z in sorted(rotated, key=lambda p: (p[0] + p[2], p[1])):
                state = rotated[(x, wy, z)]
                name, props = parse_state(state)
                color = COLORS.get(name, DEFAULT_COLOR)
                sx, sy = screen(x, wy, z)
                top = tuple(min(255, int(c * 1.15)) for c in color)
                left = tuple(int(c * 0.75) for c in color)
                right = tuple(int(c * 0.55) for c in color)
                # top diamond
                draw.polygon([(sx, sy - half // 2), (sx + half, sy), (sx, sy + half // 2),
                              (sx - half, sy)], fill=top)
                # left face
                draw.polygon([(sx - half, sy), (sx, sy + half // 2), (sx, sy + half // 2 + half),
                              (sx - half, sy + half)], fill=left)
                # right face
                draw.polygon([(sx, sy + half // 2), (sx + half, sy), (sx + half, sy + half),
                              (sx, sy + half // 2 + half)], fill=right)

    img.save(path)
    print(f"wrote {path}")


def render_plan(path):
    """Top-down plan view (highest block wins), 8px per block."""
    scale = 8
    img = Image.new("RGB", (SX * scale, SZ * scale), (24, 26, 30))
    draw = ImageDraw.Draw(img)
    for x in range(SX):
        for z in range(SZ):
            for y in range(SY - 1, -1, -1):
                state = blocks.get((x, y, z))
                if state is None:
                    continue
                name, _ = parse_state(state)
                color = COLORS.get(name, DEFAULT_COLOR)
                shade = 0.7 + 0.3 * (y / SY)
                color = tuple(min(255, int(c * shade)) for c in color)
                draw.rectangle([x * scale, z * scale, x * scale + scale - 1,
                                z * scale + scale - 1], fill=color)
                break
    img.save(path)
    print(f"wrote {path}")


# ---------------------------------------------------------------------------------
# Fence/wall connection post-pass
# ---------------------------------------------------------------------------------

FENCES = ("minecraft:dark_oak_fence", "minecraft:spruce_fence")
WALLS = ("minecraft:cobblestone_wall", "minecraft:andesite_wall")
SOLID_FOR_FENCE = ("planks", "log", "wood", "stone", "cobble", "deepslate", "andesite",
                   "diorite", "copper", "bricks", "moss_block", "barrel", "hay")
DIRECTIONS = (("north", 0, -1), ("south", 0, 1), ("west", -1, 0), ("east", 1, 0))


def _connects(x, y, z, dx, dz):
    neighbor = blocks.get((x + dx, y, z + dz))
    if neighbor is None:
        return False
    n_name, _ = parse_state(neighbor)
    return n_name in FENCES or n_name in WALLS or any(k in n_name for k in SOLID_FOR_FENCE)


def compute_fence_connections():
    for (x, y, z), state in list(blocks.items()):
        name, props = parse_state(state)
        if name in FENCES:
            for prop, dx, dz in DIRECTIONS:
                props[prop] = "true" if _connects(x, y, z, dx, dz) else "false"
        elif name in WALLS:
            connected = set()
            for prop, dx, dz in DIRECTIONS:
                if _connects(x, y, z, dx, dz):
                    props[prop] = "low"
                    connected.add(prop)
                else:
                    props[prop] = "none"
            # post (up=true) unless the wall is a clean straight run with nothing on top
            straight = connected in ({"north", "south"}, {"west", "east"})
            props["up"] = "false" if straight and (x, y + 1, z) not in blocks else "true"
        else:
            continue
        prop_str = ",".join(f"{k}={v}" for k, v in sorted(props.items()))
        blocks[(x, y, z)] = f"{name}[{prop_str}]"


# ---------------------------------------------------------------------------------
# Layout (per docs/visual-style-guide.md Heimgrund-village section)
# ---------------------------------------------------------------------------------


import math


def dist(x, z):
    return math.hypot(x - CX, z - CZ)


def det(x, z, mod):
    """Deterministic pseudo-random 0..mod-1 from coordinates (stable across runs)."""
    return (x * 2654435761 + z * 40503) % mod


def cobble_path(x, z):
    put(x, 0, z, "minecraft:mossy_cobblestone" if det(x, z, 8) == 0 else "minecraft:cobblestone")


def lantern_post(x, z, soul=False):
    put(x, 1, z, "minecraft:dark_oak_fence")
    put(x, 2, z, "minecraft:dark_oak_fence")
    put(x, 3, z, "minecraft:soul_lantern" if soul else "minecraft:lantern")


def hedge(x, z):
    leaf = ("minecraft:flowering_azalea_leaves" if det(x, z, 10) == 0
            else "minecraft:azalea_leaves")
    put(x, 1, z, leaf + "[persistent=true]")
    put(x, 2, z, leaf + "[persistent=true]")


def gable_roof(x1, z1, x2, z2, base_y, tiles, stairs, slab):
    """Straight gable, ridge along x, 1-block overhang on the z sides."""
    lo, hi = z1 - 1, z2 + 1
    y = base_y
    while lo < hi:
        for x in range(x1 - 1, x2 + 2):
            put(x, y, lo, f"{stairs}[facing=south,half=bottom]")
            put(x, y, hi, f"{stairs}[facing=north,half=bottom]")
        lo += 1
        hi -= 1
        y += 1
    if lo == hi:  # odd span: slab ridge cap
        for x in range(x1 - 1, x2 + 2):
            put(x, y, lo, f"{slab}[type=bottom]")
    # gable-end triangles (diorite infill + center timber post, motif 2)
    y = base_y
    lo, hi = z1, z2
    while lo <= hi:
        for z in range(lo, hi + 1):
            for gx in (x1, x2):
                state = ("minecraft:dark_oak_log[axis=y]" if z == (z1 + z2) // 2
                         else "minecraft:diorite")
                put(gx, y, z, state)
        lo += 1
        hi -= 1
        y += 1


def fachwerk_walls(x1, z1, x2, z2, wall_top, door):
    """Stone-brick base course, diorite infill, dark-oak posts, andesite quoins,
    dark-oak header beam. door = (side, positions) with side in NSEW."""
    for y in range(1, wall_top + 1):
        for x in range(x1, x2 + 1):
            for z in (z1, z2):
                put(x, y, z, wall_state(x, y, z, x1, z1, x2, z2, wall_top))
        for z in range(z1, z2 + 1):
            for x in (x1, x2):
                put(x, y, z, wall_state(x, y, z, x1, z1, x2, z2, wall_top))
    # interior floor
    fill(x1 + 1, 0, z1 + 1, x2 - 1, 0, z2 - 1, "minecraft:dark_oak_planks")
    # doorway: 2 wide, 3 tall, plank-framed open archway
    side, positions = door
    for pos in positions:
        dx, dz = door_cell(side, pos, x1, z1, x2, z2)
        for y in range(1, 4):
            clear(dx, y, dz, dx, y, dz)
        put(dx, 4, dz, "minecraft:dark_oak_planks")


def door_cell(side, pos, x1, z1, x2, z2):
    return {"N": (pos, z1), "S": (pos, z2), "W": (x1, pos), "E": (x2, pos)}[side]


def wall_state(x, y, z, x1, z1, x2, z2, wall_top):
    corner = (x in (x1, x2)) and (z in (z1, z2))
    if corner:
        return "minecraft:polished_andesite"  # quoins, motif 2
    if y == 1:
        return ("minecraft:mossy_cobblestone" if det(x, z, 6) == 0
                else "minecraft:stone_bricks")
    if y == wall_top:
        return "minecraft:dark_oak_wood[axis=y]"  # header beam ring
    # vertical timber post every 3rd block, diorite infill between
    along = x if z in (z1, z2) else z
    if along % 3 == 0:
        return "minecraft:dark_oak_log[axis=y]"
    return "minecraft:diorite"


def window(x, z, x2=None, z2=None):
    """1-block fence-grille windows at head height."""
    put(x, 3, z, "minecraft:dark_oak_fence")
    if x2 is not None:
        put(x2, 3, z2 if z2 is not None else z, "minecraft:dark_oak_fence")


def build():
    # Layout traced from the user's rework map (village zoom): a gray RECTANGULAR respawn
    # plaza just north of the exact center acting as the road hub; two small cottages
    # north of it flanking the north road; one square house west and one east of the
    # plaza on the east-west through-road; the GREAT HALL (biggest building) at the
    # south; straight through-roads instead of a ring; and NO south gate - the hall
    # occupies the village's south.

    # --- Gray respawn plaza (user map: "the gray one is the respawn point") ------------
    px1, pz1, px2, pz2 = 34, 18, 56, 52          # world x -11..11, z -27..7
    for x in range(px1, px2 + 1):
        for z in range(pz1, pz2 + 1):
            edge = x in (px1, px2) or z in (pz1, pz2)
            put(x, 0, z, "minecraft:polished_deepslate" if edge else "minecraft:smooth_stone")

    # --- Heimstein monument stones on the plaza (never on the spawn cell) --------------
    put(CX - 7, 1, CZ - 10, "minecraft:polished_deepslate")
    put(CX - 7, 2, CZ - 10, "minecraft:chiseled_deepslate")
    put(CX - 7, 3, CZ - 10, "minecraft:oxidized_cut_copper_slab[type=bottom]")
    put(CX + 7, 1, CZ - 12, "minecraft:polished_deepslate")
    put(CX + 7, 2, CZ - 12, "minecraft:chiseled_deepslate")
    put(CX, 1, CZ - 18, "minecraft:cobbled_deepslate")
    put(CX + 5, 1, CZ + 4, "minecraft:chiseled_deepslate")
    put(CX + 5, 2, CZ + 4, "minecraft:oxidized_cut_copper_slab[type=bottom]")
    for lx, lz in ((px1 + 1, pz1 + 1), (px2 - 1, pz1 + 1), (px1 + 1, pz2 - 1), (px2 - 1, pz2 - 1)):
        put(lx, 1, lz, "minecraft:andesite_wall")
        put(lx, 2, lz, "minecraft:andesite_wall")
        put(lx, 3, lz, "minecraft:soul_lantern")

    # --- Through-roads (user map: E-W road straight through, N road down to the plaza) --
    for x in list(range(1, px1)) + list(range(px2 + 1, SX - 1)):
        for z in (CZ - 1, CZ, CZ + 1):
            cobble_path(x, z)
    for z in range(1, pz1):
        for x in (CX - 1, CX, CX + 1):
            cobble_path(x, z)
    # short connectors: plaza to the hall door, plaza to the west/east house doors
    for z in range(pz2 + 1, 58):
        for x in (CX - 1, CX, CX + 1):
            cobble_path(x, z)
    for x in range(23, px1):
        for z in (30, 31):
            cobble_path(x, z)
    for x in range(px2 + 1, 68):
        for z in (30, 31):
            cobble_path(x, z)
    # road lanterns
    for lx in (8, 20, 70, 82):
        lantern_post(lx, CZ - 2)
    for lz in (5, 12):
        lantern_post(CX + 2, lz)

    # --- GREAT HALL (user: "the building at the bottom is the biggest one"): south, ----
    # 27x17, verdigris-copper roof, wide entrance facing the plaza (north side).
    fachwerk_walls(32, 58, 58, 74, 6, ("N", (44, 45, 46)))
    for x in (36, 40, 50, 54):
        window(x, 58)
        window(x, 74)
    for z in (62, 66, 70):
        window(32, z)
        window(58, z)
    gable_roof(32, 58, 58, 74, 7, "minecraft:oxidized_cut_copper",
               "minecraft:oxidized_cut_copper_stairs", "minecraft:oxidized_cut_copper_slab")

    # --- West house (square, door facing the plaza) --------------------------------------
    fachwerk_walls(8, 24, 22, 38, 5, ("E", (30, 31)))
    window(12, 24)
    window(18, 24)
    window(12, 38)
    window(18, 38)
    window(8, 30)
    gable_roof(8, 24, 22, 38, 6, "minecraft:deepslate_tiles",
               "minecraft:deepslate_tile_stairs", "minecraft:deepslate_tile_slab")

    # --- Werkstatt (east twin of the west house, door facing the plaza) ------------------
    fachwerk_walls(68, 24, 82, 38, 5, ("W", (30, 31)))
    window(72, 24)
    window(78, 24)
    window(72, 38)
    window(78, 38)
    window(82, 30)
    gable_roof(68, 24, 82, 38, 6, "minecraft:deepslate_tiles",
               "minecraft:deepslate_tile_stairs", "minecraft:deepslate_tile_slab")
    put(70, 1, 40, "minecraft:barrel")
    put(71, 1, 40, "minecraft:composter")

    # --- North cottages (two, flanking the north road) -----------------------------------
    fachwerk_walls(28, 6, 38, 15, 4, ("S", (32, 33)))
    window(28, 10)
    window(38, 10)
    window(33, 6)
    gable_roof(28, 6, 38, 15, 5, "minecraft:deepslate_tiles",
               "minecraft:deepslate_tile_stairs", "minecraft:deepslate_tile_slab")

    fachwerk_walls(52, 6, 62, 15, 4, ("S", (57, 58)))
    window(52, 10)
    window(62, 10)
    window(57, 6)
    gable_roof(52, 6, 62, 15, 5, "minecraft:deepslate_tiles",
               "minecraft:deepslate_tile_stairs", "minecraft:deepslate_tile_slab")

    # --- Perimeter: low wall alternating hedge, 3 gates (N/E/W - the hall guards the S) --
    for x in range(SX):
        for z in range(SZ):
            d = dist(x, z)
            if not (43.1 <= d <= 43.9):
                continue
            if abs(x - CX) <= 1 and z < 10:
                continue  # N gate opening
            if abs(z - CZ) <= 1 and (x < 8 or x > SX - 9):
                continue  # W + E gate openings
            bucket = int((math.atan2(z - CZ, x - CX) + math.pi) / (2 * math.pi) * 32)
            if bucket % 3 == 2:
                hedge(x, z)
            else:
                put(x, 1, z, "minecraft:cobblestone_wall")
                put(x, 2, z, "minecraft:cobblestone_wall")
                put(x, 3, z, "minecraft:stone_brick_slab[type=bottom]")
    for gx, gz in ((CX - 2, 1), (CX + 2, 1),
                   (1, CZ - 2), (1, CZ + 2), (SX - 2, CZ - 2), (SX - 2, CZ + 2)):
        for y in range(1, 4):
            put(gx, y, gz, "minecraft:andesite_wall")
        put(gx, 4, gz, "minecraft:soul_lantern")

    # keep the spawn cell and headroom guaranteed clear
    clear(CX, 1, CZ, CX, 4, CZ)


if __name__ == "__main__":
    build()
    compute_fence_connections()
    render_plan("tools/preview_village_plan.png")
    for v in range(4):
        render_iso(f"tools/preview_village_iso{v}.png", view=v)
    if "--nbt" in sys.argv:
        write_nbt("src/main/resources/data/baum2/structure/village_heimgrund.nbt")
