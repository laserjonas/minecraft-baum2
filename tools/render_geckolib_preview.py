"""Offline preview renderer for GeckoLib geo.json models + textures (+ optional animation pose).

Works for ANY GeckoLib model in this project, not just Spider Queen (it was born as
render_spider_queen_preview.py during her redesign - see docs/spider-queen-fable-handoff.md for
why: every visual judgment before it had been made blind, confirmed only by booting the game).
It renders the model to a PNG contact sheet (several camera angles, optionally posed by an
animation at given times) so design changes can be SEEN before asking anyone to run the client.

Faithfulness: this is not an approximation of "some Bedrock-ish renderer" - the vertex layout,
per-face UV corner assignment (including the non-mirrored U-swap), bone pivot/rotation signs,
animation keyframe sign mapping (rot x/y negated + degrees->radians, z positive; position x
negated), and the additive base+animation rotation applied in Z*Y*X order are all transcribed
from GeckoLib 5.4.5's own sources (BakedModelFactory.java, GeoQuad.java, FaceUV.java,
BakedAnimationsAdapter.java lines 221-223, AnimationProcessor.java, RenderUtil
.translateAndRotateMatrixForBone, BoneSnapshot.rotate/translate). If GeckoLib is bumped past
5.4.5, re-verify those before trusting this again.

It renders LIGHTING approximately (simple directional shading) and does not render GeckoLib
head-turn/entity tilt - it shows the model + authored animations only. The camera auto-fits to
the posed model's bounding box, so models of any size frame themselves.

Not supported (fine for this project so far, extend if a model ever needs it): bone/cube
`inflate`, per-cube rotation (the code path that's deliberately avoided anyway - see
tools/gen_spider_queen.py), box-UV cubes (only per-face UV dicts), Molang expressions in
keyframes, and non-linear keyframe easing (eased channels preview as linear).

Usage (from repo root, needs Pillow):
  python tools/render_geckolib_preview.py                                # spider_queen, rest pose
  python tools/render_geckolib_preview.py --model my_new_boss
  python tools/render_geckolib_preview.py --anim walk --times 0,0.15,0.3
  python tools/render_geckolib_preview.py --anim leap_windup --times 0,0.4,0.75 --views front,side

File discovery for --model NAME: prefers NAME.geo.json / NAME.png / NAME.animation.json in the
CURRENT directory (i.e. a gen script's just-written output, so you preview BEFORE copying into
assets), falling back to the committed assets under src/main/resources/assets/baum2/.
--geo/--tex/--anim-file override discovery entirely. --anim accepts the short name ("walk") if
it's unambiguous in the file, or the full key ("animation.spider_queen.walk").
"""
import argparse
import json
import math
import os
from PIL import Image, ImageDraw

# ---------------------------------------------------------------------------
# Small matrix/vector helpers (column-vector convention, 4x4 affine)
# ---------------------------------------------------------------------------


def mat_identity():
    return [[1.0 if i == j else 0.0 for j in range(4)] for i in range(4)]


def mat_mul(a, b):
    return [[sum(a[i][k] * b[k][j] for k in range(4)) for j in range(4)] for i in range(4)]


def mat_translate(tx, ty, tz):
    m = mat_identity()
    m[0][3], m[1][3], m[2][3] = tx, ty, tz
    return m


def mat_rot_x(r):
    c, s = math.cos(r), math.sin(r)
    return [[1, 0, 0, 0], [0, c, -s, 0], [0, s, c, 0], [0, 0, 0, 1]]


def mat_rot_y(r):
    c, s = math.cos(r), math.sin(r)
    return [[c, 0, s, 0], [0, 1, 0, 0], [-s, 0, c, 0], [0, 0, 0, 1]]


def mat_rot_z(r):
    c, s = math.cos(r), math.sin(r)
    return [[c, -s, 0, 0], [s, c, 0, 0], [0, 0, 1, 0], [0, 0, 0, 1]]


def mat_apply(m, p):
    x, y, z = p
    return (
        m[0][0] * x + m[0][1] * y + m[0][2] * z + m[0][3],
        m[1][0] * x + m[1][1] * y + m[1][2] * z + m[1][3],
        m[2][0] * x + m[2][1] * y + m[2][2] * z + m[2][3],
    )


def v_sub(a, b):
    return (a[0] - b[0], a[1] - b[1], a[2] - b[2])


def v_cross(a, b):
    return (a[1] * b[2] - a[2] * b[1], a[2] * b[0] - a[0] * b[2], a[0] * b[1] - a[1] * b[0])


def v_norm(a):
    l = math.sqrt(a[0] ** 2 + a[1] ** 2 + a[2] ** 2) or 1.0
    return (a[0] / l, a[1] / l, a[2] / l)


def v_dot(a, b):
    return a[0] * b[0] + a[1] * b[1] + a[2] * b[2]


# ---------------------------------------------------------------------------
# Geometry loading - mirrors BakedModelFactory.Builtin.constructBone/constructCube
# ---------------------------------------------------------------------------


def load_geo(path):
    with open(path, encoding="utf-8") as f:
        data = json.load(f)
    desc = data["minecraft:geometry"][0]
    bones = {}
    for raw in desc["bones"]:
        px, py, pz = raw.get("pivot", [0, 0, 0])
        rx, ry, rz = raw.get("rotation", [0, 0, 0])
        bones[raw["name"]] = {
            "name": raw["name"],
            "parent": raw.get("parent"),
            # constructBone: pivot x negated; rotation mapped (-x, -y, +z), degrees -> radians
            "pivot": (-px, py, pz),
            "base_rot": (math.radians(-rx), math.radians(-ry), math.radians(rz)),
            "cubes": raw.get("cubes", []),
        }
    return bones, desc["description"]["texture_width"], desc["description"]["texture_height"]


# VertexSet corner names from BakedModelFactory (internal space: min-x face = "Back"):
def vertex_set(cube):
    ox, oy, oz = cube["origin"]
    sx, sy, sz = cube["size"]
    oxi = -(ox + sx)  # constructCube: origin.x -> -(x + sizeX)
    return {
        "BLB": (oxi, oy, oz), "BRB": (oxi, oy, oz + sz),
        "TLB": (oxi, oy + sy, oz), "TRB": (oxi, oy + sy, oz + sz),
        "TLF": (oxi + sx, oy + sy, oz), "TRF": (oxi + sx, oy + sy, oz + sz),
        "BLF": (oxi + sx, oy, oz), "BRF": (oxi + sx, oy, oz + sz),
    }


QUAD_VERTS = {  # VertexSet.quadWest()/quadEast()/... vertex orders, non-mirrored
    "west": ("TRB", "TLB", "BLB", "BRB"),
    "east": ("TLF", "TRF", "BRF", "BLF"),
    "north": ("TLB", "TLF", "BLF", "BLB"),
    "south": ("TRF", "TRB", "BRB", "BRF"),
    "up": ("TRB", "TRF", "TLF", "TLB"),
    "down": ("BLB", "BLF", "BRF", "BRB"),
}


def face_uv_corners(face):
    """GeoQuad.build with mirror=false + Rotation.NONE: u and u+uSize are SWAPPED, then corners
    are assigned (u,v),(uW,v),(uW,vH),(u,vH) to vertices 0..3 - net effect below."""
    u, v = face["uv"]
    us, vs = face["uv_size"]
    u_left, v_top = u, v
    u_right, v_bottom = u + us, v + vs
    # after the swap: vertex0=(uRight,vTop) vertex1=(uLeft,vTop) vertex2=(uLeft,vBottom) v3=(uRight,vBottom)
    return [(u_right, v_top), (u_left, v_top), (u_left, v_bottom), (u_right, v_bottom)]


# ---------------------------------------------------------------------------
# Animation loading/sampling - mirrors BakedAnimationsAdapter + AnimationProcessor
# ---------------------------------------------------------------------------


def load_anim(path, name):
    with open(path, encoding="utf-8") as f:
        data = json.load(f)
    anims = data["animations"]
    if name in anims:
        return anims[name]
    matches = [k for k in anims if k.endswith("." + name)]
    if len(matches) == 1:
        return anims[matches[0]]
    reason = "ambiguous" if matches else "not found"
    raise SystemExit(f"animation {name!r} {reason}; available: {list(anims)}")


def sample_channel(channel, t):
    """Linear interpolation over a Bedrock keyframe channel ({time: [x,y,z]} or bare [x,y,z])."""
    if isinstance(channel, list):
        return list(channel)
    keys = sorted((float(k), val) for k, val in channel.items())
    vals = [(k, v["post"] if isinstance(v, dict) else v) for k, v in keys]
    if t <= vals[0][0]:
        return list(vals[0][1])
    if t >= vals[-1][0]:
        return list(vals[-1][1])
    for (t0, v0), (t1, v1) in zip(vals, vals[1:]):
        if t0 <= t <= t1:
            f = 0.0 if t1 == t0 else (t - t0) / (t1 - t0)
            return [a + (b - a) * f for a, b in zip(v0, v1)]
    return list(vals[-1][1])


def pose_from_anim(anim, t):
    """Returns {bone: (anim_rot_radians_internal, anim_pos_internal)} using GeckoLib's sign
    mapping: rotation (-x,-y,+z) deg->rad (BakedAnimationsAdapter), position (-x,+y,+z)
    (BoneSnapshot.translate)."""
    pose = {}
    length = float(anim.get("animation_length", 0.0)) or None
    if length is not None and anim.get("loop") is True:
        t = t % length
    elif length is not None:
        t = min(t, length)
    for bone, channels in anim.get("bones", {}).items():
        rot = (0.0, 0.0, 0.0)
        pos = (0.0, 0.0, 0.0)
        if "rotation" in channels:
            x, y, z = sample_channel(channels["rotation"], t)
            rot = (math.radians(-x), math.radians(-y), math.radians(z))
        if "position" in channels:
            x, y, z = sample_channel(channels["position"], t)
            pos = (-x, y, z)
        pose[bone] = (rot, pos)
    return pose


# ---------------------------------------------------------------------------
# Bone matrix chain - mirrors RenderUtil.prepMatrixForBone + BoneSnapshot.rotate/translate
# ---------------------------------------------------------------------------


def bone_matrix(bones, name, pose, cache):
    if name in cache:
        return cache[name]
    bone = bones[name]
    m = bone_matrix(bones, bone["parent"], pose, cache) if bone["parent"] else mat_identity()
    anim_rot, anim_pos = pose.get(name, ((0, 0, 0), (0, 0, 0)))
    px, py, pz = bone["pivot"]
    rx = bone["base_rot"][0] + anim_rot[0]
    ry = bone["base_rot"][1] + anim_rot[1]
    rz = bone["base_rot"][2] + anim_rot[2]
    m = mat_mul(m, mat_translate(*anim_pos))          # frameSnapshot.translate
    m = mat_mul(m, mat_translate(px, py, pz))         # translateToPivotPoint
    m = mat_mul(m, mat_rot_z(rz))                     # BoneSnapshot.rotate: Z, then Y, then X
    m = mat_mul(m, mat_rot_y(ry))
    m = mat_mul(m, mat_rot_x(rx))
    m = mat_mul(m, mat_translate(-px, -py, -pz))      # translateAwayFromPivotPoint
    cache[name] = m
    return m


# ---------------------------------------------------------------------------
# Rendering
# ---------------------------------------------------------------------------

VIEWS = {  # name -> (azimuth degrees around +Y, elevation degrees)
    "front": (180.0, 12.0),
    "three_quarter": (215.0, 18.0),
    "side": (270.0, 10.0),
    "back_quarter": (325.0, 18.0),
    "top": (200.0, 55.0),
}


def render_view(polys, size, azim_deg, elev_deg, center, dist):
    """polys: list of (world_quad_4pts, rgba). Painter's algorithm, weak perspective."""
    az, el = math.radians(azim_deg), math.radians(elev_deg)
    # camera position orbiting center; model front faces -Z, so azim 180 = camera on -Z side
    cam = (
        center[0] + dist * math.cos(el) * math.sin(az),
        center[1] + dist * math.sin(el),
        center[2] + dist * math.cos(el) * math.cos(az),
    )
    fwd = v_norm(v_sub(center, cam))
    right = v_norm(v_cross(fwd, (0, 1, 0)))
    up = v_cross(right, fwd)
    fl = size * 2.1  # focal length in pixels

    def project(p):
        d = v_sub(p, cam)
        cx, cy, cz = v_dot(d, right), v_dot(d, up), v_dot(d, fwd)
        cz = max(cz, 1.0)
        return (size / 2 + fl * cx / cz, size / 2 - fl * cy / cz, cz)

    img = Image.new("RGBA", (size, size), (26, 26, 30, 255))
    drw = ImageDraw.Draw(img, "RGBA")
    light = v_norm((-0.45, 0.85, -0.3))
    drawlist = []
    for quad, rgba in polys:
        pts = [project(p) for p in quad]
        depth = sum(p[2] for p in pts) / 4
        n = v_norm(v_cross(v_sub(quad[1], quad[0]), v_sub(quad[2], quad[0])))
        # two-sided shading (cheap): flip normal toward camera
        view_dir = v_norm(v_sub(cam, quad[0]))
        if v_dot(n, view_dir) < 0:
            n = (-n[0], -n[1], -n[2])
        shade = 0.52 + 0.48 * max(0.0, v_dot(n, light))
        col = (int(rgba[0] * shade), int(rgba[1] * shade), int(rgba[2] * shade), rgba[3])
        drawlist.append((depth, [(p[0], p[1]) for p in pts], col))
    drawlist.sort(key=lambda e: -e[0])
    for _, pts, col in drawlist:
        drw.polygon(pts, fill=col)
    return img


def build_polys(bones, tex, tex_w, tex_h, pose, max_subdiv=40):
    """Tessellate every cube face into texel-sized quads in posed world space."""
    px_x = tex.width / tex_w  # texture image pixels per UV unit (usually 1.0)
    px_y = tex.height / tex_h
    cache = {}
    polys = []
    for name, bone in bones.items():
        m = bone_matrix(bones, name, pose, cache)
        for cube in bone["cubes"]:
            vs = vertex_set(cube)
            faces = cube["uv"]
            for face_name, order in QUAD_VERTS.items():
                if face_name not in faces:
                    continue
                uvc = face_uv_corners(faces[face_name])
                world = [mat_apply(m, vs[v]) for v in order]
                # subdivision counts from the UV rect's pixel size
                us, vsz = faces[face_name]["uv_size"]
                nu = max(1, min(max_subdiv, round(abs(us) * px_x)))
                nv = max(1, min(max_subdiv, round(abs(vsz) * px_y)))

                def lerp2(a, b, f):
                    return tuple(x + (y - x) * f for x, y in zip(a, b))

                for i in range(nu):
                    for j in range(nv):
                        f0, f1 = i / nu, (i + 1) / nu
                        g0, g1 = j / nv, (j + 1) / nv
                        # bilinear on the quad (vertex order 0,1,2,3 = the UV corner order)
                        def bl(f, g):
                            top = lerp2(world[0], world[1], f)
                            bot = lerp2(world[3], world[2], f)
                            return lerp2(top, bot, g)
                        cell = [bl(f0, g0), bl(f1, g0), bl(f1, g1), bl(f0, g1)]
                        # sample texture at the cell center
                        uc = uvc[0][0] + (uvc[1][0] - uvc[0][0]) * (f0 + f1) / 2
                        vc = uvc[0][1] + (uvc[3][1] - uvc[0][1]) * (g0 + g1) / 2
                        tx = min(tex.width - 1, max(0, int(uc * px_x)))
                        ty = min(tex.height - 1, max(0, int(vc * px_y)))
                        rgba = tex.getpixel((tx, ty))
                        if rgba[3] < 8:
                            continue
                        polys.append((cell, rgba))
    return polys


def fit_camera(rows_polys):
    """Auto-fit camera center + orbit distance from the union bounding box of every sampled
    pose, so any model size frames itself (the constants reproduce the framing that was
    hand-tuned for Spider Queen, the tool's first subject)."""
    pts = [p for polys in rows_polys for quad, _ in polys for p in quad]
    mins = [min(p[i] for p in pts) for i in range(3)]
    maxs = [max(p[i] for p in pts) for i in range(3)]
    center = tuple((mins[i] + maxs[i]) / 2 for i in range(3))
    radius = max(1.0, math.dist(mins, maxs) / 2)
    return center, radius * 3.6


def add_floor(polys, extent, tile):
    for ix in range(-extent, extent, tile):
        for iz in range(-extent, extent, tile):
            c = (52, 52, 58, 255) if (ix // tile + iz // tile) % 2 == 0 else (43, 43, 48, 255)
            polys.append(([(ix, 0, iz), (ix + tile, 0, iz), (ix + tile, 0, iz + tile), (ix, 0, iz + tile)], c))


def discover(explicit, candidates, kind):
    if explicit:
        return explicit
    for path in candidates:
        if os.path.exists(path):
            return path
    raise SystemExit(f"no {kind} found - looked for: {candidates} (use the explicit flag)")


def main():
    ap = argparse.ArgumentParser(description="Offline preview renderer for GeckoLib models")
    ap.add_argument("--model", default="spider_queen",
                    help="model name for file discovery (default: spider_queen)")
    ap.add_argument("--geo", default=None, help="explicit geo.json path (overrides --model)")
    ap.add_argument("--tex", default=None, help="explicit texture path (overrides --model)")
    ap.add_argument("--anim-file", default=None, help="explicit animation.json path (overrides --model)")
    ap.add_argument("--anim", default=None,
                    help="animation to pose: short name if unambiguous (e.g. walk) or full key")
    ap.add_argument("--times", default="0.0", help="comma-separated sample times (seconds), one row each")
    ap.add_argument("--views", default="front,three_quarter,side,back_quarter",
                    help=f"comma-separated, from: {','.join(VIEWS)}")
    ap.add_argument("--size", type=int, default=430, help="per-view image size in px")
    ap.add_argument("--out", default="preview.png")
    args = ap.parse_args()

    assets = "src/main/resources/assets/baum2"
    geo_path = discover(args.geo, [f"{args.model}.geo.json",
                                   f"{assets}/geckolib/models/entity/{args.model}.geo.json"], "geo.json")
    tex_path = discover(args.tex, [f"{args.model}.png",
                                   f"{assets}/textures/entity/{args.model}.png"], "texture")
    bones, tex_w, tex_h = load_geo(geo_path)
    tex = Image.open(tex_path).convert("RGBA")
    anim = None
    if args.anim:
        anim_path = discover(args.anim_file, [f"{args.model}.animation.json",
                                              f"{assets}/geckolib/animations/entity/{args.model}.animation.json"],
                             "animation.json")
        anim = load_anim(anim_path, args.anim)
    times = [float(t) for t in args.times.split(",")]
    views = [v.strip() for v in args.views.split(",")]

    rows_polys = [build_polys(bones, tex, tex_w, tex_h, pose_from_anim(anim, t) if anim else {})
                  for t in times]
    center, dist = fit_camera(rows_polys)
    floor_extent = int(max(24, dist * 0.45))
    rows = []
    for polys in rows_polys:
        add_floor(polys, floor_extent, max(2, floor_extent // 7))
        rows.append([render_view(list(polys), args.size, *VIEWS[v], center, dist) for v in views])

    sheet = Image.new("RGBA", (args.size * len(views), args.size * len(rows)))
    for r, row in enumerate(rows):
        for c, im in enumerate(row):
            sheet.paste(im, (c * args.size, r * args.size))
    sheet.save(args.out)
    label = f"anim={args.anim} times={times}" if anim else "rest pose"
    print(f"wrote {args.out} ({sheet.width}x{sheet.height}) - {geo_path}, {label}, views={views}")


if __name__ == "__main__":
    main()
