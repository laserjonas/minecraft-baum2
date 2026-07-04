# Baum2 Visual Style Guide

Persistent visual identity reference, maintained by the `graphics-designer` agent. Treat this
the same way `fabric-docs-researcher` treats `docs/fabric-modding.md`: append and correct in
place as new visual systems are introduced, don't let decisions evaporate into commit history.

No visual assets exist in the mod yet beyond HUD bars (no items/blocks/mobs textured, no
class/rarity/faction color systems defined). This document currently covers only the HUD
resource-bar system below; add new sections as each new visual system is designed rather than
starting a second document.

## Art direction (working baseline)

Not yet formally defined beyond what's implied by the HUD work below — original fantasy
MMORPG tone, muted/desaturated base tones with a saturated accent used sparingly (seen below as
"dark track + bright banded fill"). Revisit this section once the first items/mobs get a
creative brief; until then, new HUD/UI work should stay consistent with the palette below
rather than inventing new hues.

## HUD: Life bar & Mana bar (first HUD system, replaces vanilla health bar)

**Status**: spec finalized, not yet implemented. Values (from progression design, not this
agent's concern): Life = 500 + 10×level (510–1500), Mana = 100 + 5×level (105–600).

### Why this treatment

Vanilla represents health/hunger/air as rows of 9×9 icons (hearts, drumsticks, bubbles) — a
discrete icon-segment system. `DrawContext.fill` (solid rectangles only; see
`docs/fabric-modding.md` → "Rendering / HUD") rules out replicating that icon approach for a
smooth 0–max resource, so Life/Mana use a **continuous filled percentage bar** instead —
mechanically the same idea as vanilla's icon segments (fill proportional to current/max) but
rendered as a solid bar, which is an unclaimed genre convention (used across countless original
UIs, not tied to any specific existing game's exact chrome). To avoid looking like a single flat
"WoW-style" or "generic asset-flip" color-bar, each bar uses a **two-band vertical gradient
illusion** (a lighter band over a darker band of the same hue, simulating a top-lit gradient
using only flat fills) plus a distinct dark border/track color per resource — this is the
specific original treatment, not the generic "colored bar" idea itself.

### Color palette (original hex values — verified not copy-pasted from a known game's exact bar
chrome; WoW/similar MMORPGs typically use flatter, more saturated pure-red/pure-blue tones than
these coral/ember and azure/sapphire two-tones)

**Life bar** (warm ember/garnet, not pure vanilla-heart red `#FF0000`-ish or a flat "class red"):
| Element | Hex | Notes |
|---|---|---|
| Shell border | `#170A0B` | near-black brick, 1px outline |
| Empty track (unfilled portion) | `#2B1416` | dark garnet-brown, visible behind the fill as it depletes |
| Fill — top band | `#E2574B` | warm coral-ember |
| Fill — bottom band | `#8E1F1F` | deep garnet |

**Mana bar** (muted azure/sapphire, not a bright flat "mana blue"):
| Element | Hex | Notes |
|---|---|---|
| Shell border | `#0A1220` | near-black navy, 1px outline |
| Empty track (unfilled portion) | `#141E33` | dark indigo, visible behind the fill as it depletes |
| Fill — top band | `#5E9BE0` | bright azure |
| Fill — bottom band | `#1F3F8A` | deep sapphire |

Both bars share the same *structure* (border → track → two-band fill) so they read as one
system; only the hue shifts. Reuse this exact structure for any future resource bar (stamina,
a boss's health bar, etc.) rather than inventing a new chrome style per resource — extend this
table with a new row pair instead.

### Layout spec

- **Per-bar shell (outer, includes border)**: 83 px wide × 8 px tall.
- **Per-bar interior (border inset 1 px on all sides)**: 81 px wide × 6 px tall.
  - 81 px matches vanilla's own hunger/health icon-row span (9 icons × 9 px with 1 px overlap
    each = 81 px), so the new bars read as the same "width class" as the hunger bar that
    remains in place — nothing looks under- or over-scaled next to it.
- **Two-band fill split**: interior height 6 px → top 3 px = lighter band, bottom 3 px = darker
  band. Fill width = `interiorWidth × (current / max)`, both bands share that same width (i.e.
  the split is horizontal-progress, banding is vertical/decorative only — don't band the
  *progress* itself).
- **Empty vs. full treatment**: at 0%, only the border + empty-track color show (no bands
  drawn) — reads as "drained," not "broken/missing." At 100%, the fill spans the full 81 px
  interior with both bands showing full-width, no track color visible. This directly mirrors
  how vanilla's own hunger/air bars empty out segment-by-segment, just continuously instead of
  in 9 discrete steps.
- **Vertical gap between the two bars' shells**: 1 px.
- **Combined block footprint**: 83 px wide × 17 px tall (8 + 1 + 8).
- **Stacking order**: **Life bar is the lower/inner bar** (occupies the exact row vanilla
  hearts used to sit in, closest to the hotbar — preserves the "most vital stat nearest your
  hands" muscle memory vanilla players already have). **Mana bar sits directly above it**
  (occupying the row vanilla's armor icons used to use, which was blank whenever a player had
  no armor — now always meaningfully filled). This is a design decision, not yet a verified
  pixel-perfect render — see "Open items" below.
- **Horizontal position**: left-side status-bar column, i.e. the same screen region vanilla
  hearts occupied — anchored via `attachElementBefore(VanillaHudElements.ARMOR_BAR, ...)` per
  `docs/fabric-modding.md`. Does **not** collide with the hunger bar, which stays in its own
  unmodified right-side column (`FOOD_BAR` untouched) — collision is only a risk within the
  same column, and Life/Mana only occupy the left one.
- **`StatusBarHeightProvider`**: register one for the combined element (both bars + the 1px
  gap = 17 px total) via `HudStatusBarHeightRegistry.addLeft(...)`, using the same
  `Identifier` as the `HudElement` registration, so anything else that stacks in that column
  (if ever added) lays out correctly beneath/above it instead of overlapping.

### Open items (expected, not blockers)

- Exact pixel `x`/`y` anchor and whether `attachElementBefore(ARMOR_BAR, ...)` actually lands
  Life directly in the vanilla hearts' row vs. one row off needs a quick in-game visual check
  once implemented — this is normal "eyeball it once it renders" polish, not a design gap.
- No numeric text (e.g. "420/510") is drawn anywhere on the bars per the no-text-rendering
  constraint — if/when text rendering is revisited and proven reliable (see
  `docs/fabric-modding.md`), consider a tooltip-on-hover or a togglable numeric readout as an
  *addition*, not a change to the bar visuals themselves.
- Class-name banner and level-diamond badge are explicitly deferred (out of scope for this
  spec) — when they're designed, keep them visually subordinate to (not competing with) this
  bar system; they'll likely need their own section here.

### Naming/asset conventions established here

- No new texture/model files needed for this system — it's pure `DrawContext.fill` rectangles,
  no PNGs. If a future pass adds icon glyphs next to the bars (e.g. a small life/mana icon),
  follow standard 16×16 `textures/gui/` placement and document it here.
- HUD element `Identifier`s live in the `baum2` namespace, e.g.
  `Identifier.of("baum2", "life_mana_bars")` per `docs/fabric-modding.md`.

## Character Stats Screen (full-screen, opened/closed with 'C')

**Status**: spec finalized, not yet implemented. Built on vanilla's real tab-navigation widget
system (`Tab` / `TabManager` / `TabNavigationWidget` / `GridScreenTab`), starting with exactly
one tab ("Stats") so more tabs (Skills, Class, etc.) can be added later without redesigning the
chrome. Content is a `GridWidget` inside the `GridScreenTab`, populated with `TextWidget` rows,
refreshed every frame from live data. This is a full menu `Screen` (like vanilla's own
Statistics/options screens) — normal `renderBackground` darkening applies behind it, and text
renders through the normal `DrawContext.drawText` pipeline (reliable, unlike the ad-hoc HUD
overlay text problem noted in the HUD section above).

### Why this treatment

The HUD bars above deliberately carry **no numeric text** (bar-only, per the HUD section's open
items). This screen is the "togglable numeric readout" that section anticipated, but built
properly as a menu screen using vanilla's tab system rather than bolted onto the HUD overlay.
Splitting it this way keeps the HUD minimal/glanceable and gives numeric detail a dedicated,
low-frequency-access screen — a standard, unclaimed genre convention (every MMORPG has *a*
character/stats panel; the specific chrome and palette here are original).

### Row order and format

Two-column layout per row: a label (column 0) and a value (column 1), left-aligned label /
value pair rather than a single inlined "Life: 340/510" string — keeps values scannable in a
vertical list and lets label vs. value carry separate colors (see below).

| # | Label text | Value format | Example |
|---|---|---|---|
| 1 | `Life` | `current / max`, integers, no unit suffix | `412 / 510` |
| 2 | `Mana` | `current / max`, integers, no unit suffix | `180 / 320` |
| 3 | `Base Damage` | 1 decimal place, always shown (`%.1f`) | `12.5` |
| 4 | `Base Magic Damage` | 1 decimal place, always shown (`%.1f`) | `8.0` |

Rationale for the order: Life/Mana (resource stats, matching HUD stacking order — Life first)
come before Base Damage/Base Magic Damage (offense stats), grouping "what sustains you" before
"what you deal." Always render the `.1f` decimal (even for whole numbers like `8.0`, not `8`)
so the four value strings stay vertically aligned as a column instead of jittering width frame
to frame — cheap consistency win since values refresh every frame anyway. Life/Mana intentionally
carry no unit suffix ("hp"/"mp"), consistent with how the HUD bars themselves never labeled units
either.

### Color treatment

Labels all share one muted, low-saturation tone so only the *values* carry hue-coded meaning —
scanning down the value column tells you at a glance which stat is which, even without reading
the label:

| Row | Label hex | Value hex | Notes |
|---|---|---|---|
| Life | `#9C9186` (muted warm stone-grey, shared by all 4 labels) | `#E2574B` | Reuses the Life bar's top-band ember/coral exactly — same identity as the HUD bar, not just "same hue family." |
| Mana | `#9C9186` | `#5E9BE0` | Reuses the Mana bar's top-band azure exactly, same rationale. |
| Base Damage | `#9C9186` | `#D98A3D` | **New** — warm bronze-amber. Deliberately *not* red: red is already Life's identity on this same screen, and reusing it for "damage" would visually collide with the Life row two lines up. Amber/bronze reads as "physical/metal/weapon" without the generic red-damage cliché. |
| Base Magic Damage | `#9C9186` | `#9B5FE0` | **New** — arcane violet. Deliberately *not* blue: blue is Mana's identity here, and magic-damage-as-blue would make it look like a Mana sub-stat rather than its own thing. Violet sits clearly between Life's red and Mana's blue on the wheel, reads as "arcane/magic" via widely-used genre convention (violet/purple = magic) without copying any specific game's exact spell-damage color. |

All four value hexes are pure reuses or new additions from this session — none were copy-pasted
from a known game's exact stat-screen palette. The amber/violet pair was chosen specifically to
avoid the "damage = red, magic = blue" cliché pairing that would otherwise visually collide with
Life/Mana directly above them on the same screen.

### Layout guidance

- **Grouping via spacing, not a divider widget**: keep it to the `GridWidget`'s built-in row
  spacing rather than adding a separate divider/line widget for a 4-row list — simplicity per
  `CLAUDE.md`'s "do not create large systems before a minimal version works." Use a smaller
  vertical gap *within* each pair and a larger gap *between* the two pairs so the grouping
  (resource stats vs. offense stats) reads visually without extra widgets:
  - Within a pair (Life↔Mana, Base Damage↔Base Magic Damage): ~6 px row spacing.
  - Between the two pairs: ~14 px (roughly double the within-pair gap).
  - Label→value column gap: ~4–6 px (enough that they don't visually merge into one string).
- **No extra header inside the tab content.** The `TabNavigationWidget` button itself already
  reads "Stats" — repeating a "Stats" header inside the `GridScreenTab` content would be
  redundant. If a second tab is added later and the content still feels like it needs a title,
  revisit this then rather than pre-building it now.
- **Alignment**: label column left-aligned, value column left-aligned immediately after it
  (not right-aligned/tabular-number-style) — matches vanilla's own options-list and statistics
  screens rather than inventing a spreadsheet-like layout.
- Exact pixel row-spacing values above are reasonable defaults to hand to implementation, not
  verified in-game yet — normal "eyeball it once it renders and nudge ±2-4px" polish, not a
  design gap blocking implementation.

### Naming/asset conventions established here

- No new texture/model/icon files — this screen is entirely vanilla widget composition
  (`GridWidget` + `TextWidget`) plus text color, no PNGs.
- If a second tab is added later (e.g. "Skills"), give it its own subsection here documenting
  any tab-specific palette rather than assuming it inherits this one's amber/violet pair —
  those two hexes are specifically Base Damage/Base Magic Damage's identity, not a general
  "second accent pair" free for reuse elsewhere.

## Changelog

- 2026-07-04: Added "Character Stats Screen" section — row order/format and label/value color
  spec for the 'C'-key full-screen stats tab (Life, Mana, Base Damage, Base Magic Damage).
  Reuses the HUD's exact Life/Mana hexes for those two rows; introduces a new amber
  (`#D98A3D`) / violet (`#9B5FE0`) pair for Base Damage / Base Magic Damage, chosen specifically
  to avoid colliding with Life's red or Mana's blue on the same screen.
- 2026-07-04: Initial creation of this document. Defined the Life bar / Mana bar HUD color
  palette, layout, and stacking spec (this section). No prior visual identity existed to
  reconcile with.
