# Spider Queen visual design — handoff to Fable

> **STATUS UPDATE (2026-07-06, Fable):** the redesign this handoff asked for has been done —
> see `docs/visual-style-guide.md` section 17.6 for what shipped (pixel-art texture atlas,
> painted structured eyes replacing the eye-cubes/black face plate, fang bones, real
> leap-windup/flight/walk/idle animations). The blind-iteration problem this document warns
> about is solved: `tools/render_geckolib_preview.py` renders the model + any animation
> pose to a PNG contact sheet using GeckoLib's own transcribed transform, so changes can be
> SEEN before shipping. The history below is still accurate and still worth reading before
> making further changes (the rejected-attempts list especially). Awaiting in-game user
> confirmation of the new look.

## Why this document exists

Spider Queen's model was migrated to GeckoLib and is now technically correct — geometry,
orientation, and animation all work and are verified. But the *visual design* (shape, eyes,
face, silhouette) has been redone many times and still isn't landing ("the spider is very
awful"). Sonnet handled the GeckoLib migration and all the coordinate-math bugs, but is not
making further design decisions on this mob — that's what this handoff is for. The user will
add specific direction for what to do next; this document is the "here's where things stand"
context so that direction doesn't need to re-derive everything from scratch.

**Read this whole file before touching anything.** The short version: the *engineering* is
solid and verified; the *art direction* is the open problem, and it's open specifically because
whoever has been iterating on it (Sonnet) cannot see the model render — every visual judgment
has been made blind, from screenshots the user sends back afterward. If you have any way to
actually see a render before proposing changes, that alone would likely outperform everything
tried so far.

## What's confirmed working (do not need to be re-derived or re-verified)

- **Geometry position/orientation**: the model stands upright, legs point down and touch the
  ground, correctly matching real vanilla Minecraft spider proportions (transcribed directly
  from `SpiderEntityModel.java`, not invented). This took several rounds to get right — see
  `docs/fabric-modding.md`'s "GeckoLib integration" section, part H, for the full story if you
  need to touch bone/cube coordinate math. **Do not re-derive the coordinate conversion from
  scratch** — it's in `tools/gen_spider_queen.py` (see below) and is self-checked.
- **3x scale**: unchanged from the original vanilla-reskin version, applied via
  `SpiderQueenEntityRenderer`'s `.withScale(3.0F)`.
- **Leap attack animations**: `idle`/`walk`/`leap_windup`/`leap_flight` GeckoLib animations
  exist and reference the correct bone names. The leap's actual physics/trajectory are
  unaffected by any of this (GeckoLib is rendering-only).
- **Abdomen pattern**: an organic, irregular blotch marking (not a flat block, not a clean
  hourglass) — this is the one visual redesign that has NOT drawn negative feedback since it
  landed. Worth treating as "the thing that's working" for calibration on what reads well in
  this project's blocky style.

## What's NOT settled (the actual open problem)

- **Overall impression**: "not scary", "looks like trash." The user wants something that reads
  as a dangerous boss-tier monster, not just "a grey box with legs."
- **Head shape**: currently a plain, unmodified 8x8x8 cube (in the model's own units). Two
  attempts at rounding/chamfering the corners were both rejected as looking wrong (one produced
  disconnected slabs from a bug; the second was numerically verified correct and *still* looked
  bad — meaning this is a design problem, not a coordinate-math problem). See "What's been tried
  and rejected" below before attempting a third geometry-based rounding pass.
- **Eyes**: gone through five distinct redesigns (2 large eyes → uniform round-dot grid →
  organic scattered cluster → same cluster with spacing fixed → primary-pair + secondary-cluster
  with a dark face-plate behind them). The current version is the last one shipped; it has not
  been confirmed as "good," only as "the last attempt before this handoff was written."
- **Palette**: primary body color is pinned to an exact user-specified hex (`#898989`) and
  should probably stay that way unless directed otherwise. The single red accent (`#FF3B1E`,
  "Widow Red") reused for eyes and the abdomen pattern is also a direct user decision, not
  Sonnet's own choice — the user explicitly rejected a two-red-family "Redstone Widow" palette
  and asked for one shared red instead.
- **The red particle "smoke" effect has been removed entirely** (not just disabled) per direct
  feedback that it looked bad. Don't re-add a particle glow effect without new direction to do
  so.

## What's been tried and rejected (don't repeat these exact attempts)

1. Bespoke, non-vanilla geometry (dramatically tall legs, spike tufts, inspired loosely by a
   Sketchfab reference model) — rejected: "doesn't look like a spider, completely out of
   place." Reverted to vanilla-accurate proportions.
2. Solid flat-color abdomen (red block) — rejected: "should more like a have pattern instead a
   full red block." Replaced with the organic blotch marking that's currently working.
3. Two large forward-facing eyes — rejected: reads as a mammal face, not a spider.
4. A round-dot texture per eye (dark socket + painted circle) — rejected: still didn't read as
   eyes ("that does not look like spider eyes"), and separately had a real UV-sizing bug that
   made them invisible before the visual critique even applied.
5. A uniform 2-row grid of round eyes — rejected for being too rigid/grid-like once the round-dot
   texture bug was fixed.
6. A fully random scattered eye cluster (inspired by the abdomen's own success) — rejected at
   first for overlapping/merging into one blob (fixed with minimum-spacing rejection sampling),
   then for being too small (sizes increased), then for "still not looking dangerous" even once
   fixed and enlarged.
7. **Head corner-chamfer, attempt 1**: per-cube `rotation`+`pivot` on extra cuboids at each
   vertical edge — produced oversized, disconneted slabs. Root cause: this specific GeckoLib
   code path (`GeoCube`'s own render-time pivot/rotation, distinct from bone-level rotation) had
   never been verified, unlike the bone-level rotation math used for the legs.
8. **Head corner-chamfer, attempt 2**: reused the *verified* bone-level rotation mechanism
   instead (each bevel as its own small bone) — the math was confirmed correct numerically
   before shipping (each bevel cube centered exactly on its own pivot), and it **still** looked
   wrong in-game ("head is malformed"). This is the important data point: a geometrically
   correct chamfer still failed on pure aesthetics. Don't just re-verify the math a third time —
   the problem is what "rounder" should actually look like on this silhouette, which needs an
   actual design idea, not another transform derivation.
9. A pulsing red particle "smoke" glow on the abdomen (reusing vanilla's redstone-dust particle)
   — removed entirely per feedback that it "looks like trash."

## How to actually make changes

There is **no live-render/screenshot tool in this environment** for the native Minecraft
window — every visual judgment has to be made blind and confirmed by the user running
`./gradlew runClient` and reporting back or sending a screenshot. Plan for an iterate-and-wait
loop, not instant visual feedback.

The model/texture are **generated programmatically**, not hand-authored in Blockbench:

- **`tools/gen_spider_queen.py`** (in this repo, committed — this used to live in a
  session-scoped scratchpad and would have been lost otherwise) is a Python script (needs
  Pillow: `pip install pillow`) that generates both:
  - `spider_queen.geo.json` (GeckoLib's geometry format — bones, cuboids, UV mapping)
  - `spider_queen.png` (a small procedural texture atlas — flat/noisy color fills per cuboid,
    plus two dedicated hand-drawn regions: the abdomen's organic blotch marking and the head's
    dark face-plate)
- Run it from the repo root (`python tools/gen_spider_queen.py`), then copy its two output files
  into:
  - `src/main/resources/assets/baum2/geckolib/models/entity/spider_queen.geo.json`
  - `src/main/resources/assets/baum2/textures/entity/spider_queen.png`
  - then `./gradlew build` to verify it compiles/packages (this does NOT verify it looks right —
    only that the JSON is well-formed and the build pipeline is happy).
- The script has a **self-check built in** (prints "PASS"/"FAIL" and asserts before writing
  output) that verifies the leg geometry is positioned/rotated correctly according to GeckoLib's
  actual confirmed transform behavior (reverse-engineered from GeckoLib's own source, documented
  in `docs/fabric-modding.md` part H). **Keep this check passing** if you touch any bone pivot,
  cube origin, or rotation value — it catches "is the geometry where I think it is," though it
  cannot catch "does this look good."
- If you want to add genuinely new geometry (not just recolor/resize existing cuboids), reuse
  the `bedrock_pivot()`/`fix_origin_x()` helpers for positioning, and prefer adding a **new bone**
  with its own rotation over per-cube rotation — bone-level rotation is the mechanism proven
  correct here; per-cube rotation/pivot is a different, less-tested GeckoLib code path that
  already produced one broken result (see "attempt 1" above).

## Relevant docs, in order of how much you'll actually need them

1. **This file** — the design history and open problem.
2. `docs/visual-style-guide.md` section 17 (17.1 through 17.4) — the full palette/pattern
   history, hex values, and exactly what was tried for each visual element, in chronological
   order with reasoning for each change.
3. `docs/fabric-modding.md`'s "GeckoLib integration" section (parts A-H) — the technical
   GeckoLib research, including part H's full writeup of the coordinate-system bug and how it
   was fixed. Only needed if you're touching geometry math, not for pure recoloring/reshaping
   within the existing coordinate system.
4. `HANDOFF.md`'s "GeckoLib migration follow-up" sections (1 through 5) — a chronological
   session-by-session log of the same history, useful mainly for dates/context, not for new
   information beyond the other two docs.

## What NOT to do

- Don't re-derive the Y-axis reflection / GeckoLib coordinate transform from scratch — it's
  solved, documented, and self-checked. If the self-check passes, trust the position/rotation
  math and focus entirely on what to actually put where (shapes, sizes, colors).
- Don't reintroduce a two-red-family palette, a different primary color than `#898989`, or a
  particle "smoke"/glow effect without new direction from the user to do so — all three were
  explicit rejections, not open design space.
- Don't assume a visual change "worked" just because `./gradlew build` passes — it only proves
  the JSON/texture are well-formed, not that they look good. Every real confirmation in this
  project's history has come from the user actually running the game.
