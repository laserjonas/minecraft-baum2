"""Derives the "Tempered"/"Perfect" weapon-upgrade-tier texture variants from each weapon's
existing base texture (see docs/visual-style-guide.md Section 25 for the full writeup).

This is a pure image-transform pass, NOT a repaint: every output pixel is derived from the
corresponding base pixel via one shared, deterministic HSV transform (two tiers of parameters,
see TIERS below). No new geometry/silhouette is drawn and no per-weapon special-casing happens
- adding tier variants for a new weapon later is "add one entry to WEAPONS below and rerun."

Per the visual brief (silhouette IDENTICAL per tier; Excellent = "a slight silver tiny shiny";
Perfect = "a stronger shiny effect and slightly more golden"), each tier applies two layered
passes to every opaque pixel, as straight-line RGB blends (NOT an HSV hue rotation - an early
version of this script hue-rotated from each weapon's native hue toward a target hue, but the
hue wheel's shortest path from a warm gold/brown weapon color to the cool "silver" target
hue passes THROUGH saturated green/cyan, which read as a visible green tinge on mid-brightness
pixels - a straight RGB blend toward a fixed target color never leaves the line between the two
colors, so it can't produce an off-path hue like that):

  1. Global tint  - a WEAK linear blend toward the tier's target tint color (cool pale
     blue-gray for silver, warm amber for gold) applied to every opaque pixel a little, so the
     whole weapon reads as "slightly silver/golden" without repainting it - satisfies "slightly
     more golden coloring" as an overall cast rather than a hard-masked region (no per-weapon
     blade/head mask exists, or could exist, without breaking the "one shared transform"
     contract). This pass's blend fraction is itself scaled DOWN on a pixel's own shadow depth
     (see `TINT_SHADOW_FLOOR` below) - a flat blend fraction applied equally to near-black
     shadow pixels and bright lit pixels made very dark palettes (e.g. Drevathis's Cursed
     Blade, near-black base color) visibly lighten toward tan/gold across the whole silhouette,
     which broke "keep each weapon's own palette identity recognizable." Scaling the tint by
     brightness keeps shadow pixels close to their original near-black tone (matte, unlit) and
     concentrates the cast on already-lit surfaces, which also reads as a more physically
     plausible metallic sheen.
  2. Highlight shine - a STRONGER blend toward a brighter glint color, gated by a weight that
     only turns on for pixels that are already bright relative to that weapon's own brightness
     range (i.e. the existing edge-highlight pixels every item icon in this mod already paints
     lighter, e.g. `BLADE_LIT`/`EDGE`-style tones - see tools/gen_sword_template.py's icon
     highlight cells for the established convention this targets), plus a "screen" brightening
     step for the specular glint itself. Perfect's shine threshold is lower and its blend/
     brighten amounts are larger, so more of the sprite catches the glint and it reads brighter
     than Tempered's - "stronger shiny effect."

Per-image min/max brightness normalization means the highlight threshold adapts to each
weapon's own palette instead of a fixed absolute brightness cutoff.

Idempotent by construction: every run reads ONLY the base texture (never a previous
_tempered/_perfect output), applies the fixed TIERS parameters, and overwrites the two output
files - re-running with unchanged inputs reproduces byte-identical outputs.

Usage: `python tools/gen_weapon_tier_textures.py` from anywhere (paths are resolved relative to
the script's own location, not the CWD). Writes directly into
`src/main/resources/assets/baum2/textures/item/` - no manual copy step.
"""
import colorsys
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parents[1]
ITEM_TEX_DIR = ROOT / "src" / "main" / "resources" / "assets" / "baum2" / "textures" / "item"

# ============================================================================
# Weapons in scope - one entry per BASE texture that needs tier variants.
# "3d" entries additionally get a *_3d variant (32x32 in-hand textures); the GUI-only
# icon (16x16, no "_geo"/"_3d" suffix) always gets a variant too, where present.
# Adding weapon #N later: add its texture filename(s) here and rerun.
# ============================================================================
WEAPONS = [
    ["gold_sword.png"],
    ["poison_dagger.png"],
    ["colossal_warclub.png", "colossal_warclub_3d.png"],
    ["drevathis_cursed_blade.png", "drevathis_cursed_blade_3d.png"],
    ["espenklinge.png"],  # GUI sprite only this pass - espenklinge_geo.png is out of scope
]

# ============================================================================
# Tier transform parameters (pinned - see docs/visual-style-guide.md Section 25.2 for the
# rationale behind each value). Colors are target RGB tuples; blend/brighten amounts are
# 0..1 fractions used as straight-line RGB lerp / screen-brighten factors.
# ============================================================================
TIERS = {
    "tempered": {
        "label": "Silver Sheen",
        # global tint - weak, whole-sprite blend toward a cool pale silver
        "tint_color": (0xC7, 0xD2, 0xDC),
        "tint_blend": 0.10,       # 10% global blend, brightness-scaled (see TINT_SHADOW_FLOOR)
        # highlight shine - stronger, brightness-gated blend + screen-brighten
        "shine_weight_lo": 0.62,  # normalized-brightness threshold where shine starts fading in
        "shine_color": (0xF2, 0xF6, 0xFA),  # near-white cool sparkle
        "shine_blend": 0.55,      # up to 55% blend toward shine_color at full weight
        "shine_brighten": 0.12,   # up to +12% screen-brighten at full weight
    },
    "perfect": {
        "label": "Gilded Shine",
        "tint_color": (0xE8, 0xB4, 0x54),
        "tint_blend": 0.20,       # stronger global blend than Tempered - "more golden"
        "shine_weight_lo": 0.50,  # lower threshold -> shine covers more of the sprite
        "shine_color": (0xFF, 0xE9, 0xA8),  # warm bright gold-white sparkle
        "shine_blend": 0.75,
        "shine_brighten": 0.22,   # stronger brighten than Tempered - "stronger shiny effect"
    },
}

# Global-tint brightness scaling: effective_tint_blend = tint_blend * (FLOOR + (1-FLOOR)*v_norm)
# v_norm=0 (darkest pixel in the sprite) still gets FLOOR fraction of the full tint (a faint
# cast, not zero); v_norm=1 (brightest) gets the full tint_blend. Keeps shadow/near-black
# pixels close to their original tone instead of visibly lightening toward the tint color.
TINT_SHADOW_FLOOR = 0.35


def _clamp255(v):
    return 0 if v < 0 else (255 if v > 255 else v)


def _lerp_rgb(c, target, t):
    return tuple(c[i] + (target[i] - c[i]) * t for i in range(3))


def _screen_brighten_rgb(c, amt):
    return tuple(c[i] + (255.0 - c[i]) * amt for i in range(3))


def apply_tier(img: Image.Image, params: dict) -> Image.Image:
    """Derives one tier variant from a base RGBA image via two straight-line RGB blends
    (global tint, then brightness-gated highlight shine) - see module docstring for why RGB
    blending is used instead of HSV hue rotation. Reads ONLY `img` - never a previous variant -
    so repeated runs are idempotent."""
    img = img.convert("RGBA")
    w, h = img.size
    src = img.load()

    # Per-image brightness range (opaque pixels only, HSV V channel) - the shine pass's
    # threshold is relative to THIS weapon's own palette, not a fixed absolute value, so it
    # adapts across weapons of different overall brightness while using identical parameters.
    v_values = []
    for y in range(h):
        for x in range(w):
            r, g, b, a = src[x, y]
            if a == 0:
                continue
            _, _, v = colorsys.rgb_to_hsv(r / 255.0, g / 255.0, b / 255.0)
            v_values.append(v)
    if not v_values:
        return img.copy()
    v_min, v_max = min(v_values), max(v_values)
    v_range = max(v_max - v_min, 1e-6)

    tint_color = params["tint_color"]
    shine_color = params["shine_color"]
    weight_lo = params["shine_weight_lo"]

    out = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    dst = out.load()

    for y in range(h):
        for x in range(w):
            r, g, b, a = src[x, y]
            if a == 0:
                dst[x, y] = (0, 0, 0, 0)
                continue
            _, _, vv = colorsys.rgb_to_hsv(r / 255.0, g / 255.0, b / 255.0)
            v_norm = (vv - v_min) / v_range

            # --- pass 1: global tint (weak, applies everywhere, scaled down on shadow pixels
            # so a very dark palette doesn't visibly lighten toward the tint color) ---
            tint_scale = TINT_SHADOW_FLOOR + (1.0 - TINT_SHADOW_FLOOR) * v_norm
            step1 = _lerp_rgb((r, g, b), tint_color, params["tint_blend"] * tint_scale)

            # --- pass 2: highlight shine (weighted by this pixel's own brightness) ---
            weight = max(0.0, min(1.0, (v_norm - weight_lo) / (1.0 - weight_lo)))
            step2 = _lerp_rgb(step1, shine_color, params["shine_blend"] * weight)
            step3 = _screen_brighten_rgb(step2, params["shine_brighten"] * weight)

            dst[x, y] = (
                _clamp255(round(step3[0])),
                _clamp255(round(step3[1])),
                _clamp255(round(step3[2])),
                a,
            )

    return out


def variant_name(base_filename: str, tier: str) -> str:
    stem = base_filename[:-4]  # strip ".png"
    return f"{stem}_{tier}.png"


def main():
    rows = []
    for group in WEAPONS:
        for base_filename in group:
            base_path = ITEM_TEX_DIR / base_filename
            base_img = Image.open(base_path)
            for tier, params in TIERS.items():
                out_img = apply_tier(base_img, params)
                out_name = variant_name(base_filename, tier)
                out_path = ITEM_TEX_DIR / out_name

                assert out_img.size == base_img.size, (
                    f"{out_name}: size {out_img.size} != base size {base_img.size}"
                )
                assert out_img.mode == "RGBA", f"{out_name}: mode {out_img.mode} != RGBA"

                out_img.save(out_path)

                # re-load from disk to verify the write round-trips correctly
                reloaded = Image.open(out_path).convert("RGBA")
                base_rgba = base_img.convert("RGBA")
                diff_count = sum(
                    1
                    for y in range(reloaded.height)
                    for x in range(reloaded.width)
                    if reloaded.getpixel((x, y)) != base_rgba.getpixel((x, y))
                )
                rows.append(
                    {
                        "file": out_name,
                        "size_ok": reloaded.size == base_img.size,
                        "size": f"{reloaded.width}x{reloaded.height}",
                        "mode": reloaded.mode,
                        "diff_px": diff_count,
                    }
                )

    # ---- verification table ----
    header = f"{'file':40} {'size':8} {'mode':5} {'size_ok':8} {'diff_px>0':10}"
    print(header)
    print("-" * len(header))
    all_ok = True
    for row in rows:
        ok = row["size_ok"] and row["mode"] == "RGBA" and row["diff_px"] > 0
        all_ok &= ok
        print(
            f"{row['file']:40} {row['size']:8} {row['mode']:5} "
            f"{str(row['size_ok']):8} {row['diff_px']:>10}"
        )
    print("-" * len(header))
    print(f"ALL CHECKS PASSED: {all_ok}  ({len(rows)} files)")


if __name__ == "__main__":
    main()
