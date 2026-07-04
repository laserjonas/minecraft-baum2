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

**Status**: Life/Mana rows implemented and live. The attribute-system expansion documented below
(4 core attributes with `+1` investment buttons, 8 derived stats, an Unspent Points counter — 15
rows total) is spec-finalized here, not yet implemented. Built on vanilla's real tab-navigation
widget system (`Tab` / `TabManager` / `TabNavigationWidget` / `GridScreenTab`), starting with
exactly one tab ("Stats") so more tabs (Skills, Class, etc.) can be added later without
redesigning the chrome — this expansion still fits inside that single tab, no new tab needed.
Content is a `GridWidget` inside the `GridScreenTab`, populated with `TextWidget` rows,
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

Three-column layout per row: a label (column 0), a value (column 1), and an optional action
column (column 2) that only the 4 attribute rows populate (a small `+1` `ButtonWidget`) — every
other row leaves column 2 empty. This is a superset of the original two-column layout, not a
redesign: existing Life/Mana rows are untouched, they simply have nothing in column 2.

**`Base Damage` and `Base Magic Damage` are renamed to `Base Attack` and `Base Magic Attack`**
(same concept, same hexes — see below) to read correctly now that Physical/Magic Defence exist
as their opposites on the same screen ("Attack" vs "Defence" reads as a pair; "Damage" vs
"Defence" doesn't).

**Grouping decision — interleaved by attribute family, not blocked by kind.** The alternative
(all 4 attributes together, then all 8 derived stats after) was considered and rejected: with a
brand-new attribute system, the single most important thing this screen must teach at a glance
is *which attribute produces which stat*. Blocking "inputs" and "outputs" into two separate
zones forces the player to cross-reference a mental (or wiki) table to know that, say, Strength
is the one to invest in for more Physical Defence. Placing each attribute directly above the
1–3 derived stats it drives — and color-matching them (see below) — teaches that relationship
for free, through proximity and hue, with no tooltip or extra text required. The `+1` buttons
end up scattered down the list rather than clustered at the top, but that's an acceptable
trade: a player deciding "what do I want more of" reads top-to-bottom by *effect* anyway, and
finds the button right next to the effect they just read.

Family order follows the order attributes were introduced in the design (Endurance,
Intelligence, Strength, Dexterity); within a family, offense-oriented derived stats are listed
before defense-oriented ones (mirrors the existing Base Attack-before-Physical Defence framing).
The new `Unspent Points` counter sits once, up front, right after Mana and before the first
attribute — it's a global summary ("how many points do I have to spend at all"), so it reads
before the player starts scanning individual attributes rather than being buried mid-list.

| # | Label text | Value format | Example | Column 2 |
|---|---|---|---|---|
| 1 | `Life` | `current / max`, integers, no unit suffix | `412 / 510` | — |
| 2 | `Mana` | `current / max`, integers, no unit suffix | `180 / 320` | — |
| 3 | `Unspent Points` | integer | `3` | — |
| 4 | `Endurance` | integer | `7` | `+1` button |
| 5 | `Life Regen` | 2 decimal places (`%.2f`), no unit suffix | `0.35` | — |
| 6 | `Intelligence` | integer | `6` | `+1` button |
| 7 | `Base Magic Attack` | 1 decimal place (`%.1f`) | `8.0` | — |
| 8 | `Magic Defence` | 1 decimal place (`%.1f`) | `6.0` | — |
| 9 | `Strength` | integer | `8` | `+1` button |
| 10 | `Base Attack` | 1 decimal place (`%.1f`) | `12.5` | — |
| 11 | `Physical Defence` | 1 decimal place (`%.1f`) | `10.0` | — |
| 12 | `Dexterity` | integer | `5` | `+1` button |
| 13 | `Attack Speed Multiplier` | 2 decimals + `x` suffix (`%.2fx`) | `1.00x` | — |
| 14 | `Cast Speed Multiplier` | 2 decimals + `x` suffix (`%.2fx`) | `1.00x` | — |
| 15 | `Crit Chance` | 1 decimal + `%` suffix (`%.1f%%`) | `5.0%` | — |

Two deliberate exceptions to the established "no unit suffix" rule: **multipliers get an `x`
suffix** and **Crit Chance gets a `%` suffix**. Life/Mana/Base Attack/Base Magic Attack/defence
values are all bare numbers on a shared implicit scale (points), where a suffix would be noise —
but a bare `1.05` or `5.0` for a multiplier or a percentage is genuinely ambiguous (could read as
a flat point value), so those two stat *types* keep their suffix as a comprehension aid, not a
style inconsistency. Life Regen uses `%.2f` rather than the `%.1f` used elsewhere specifically
because its base value (`0.25`) would round to a misleading `0.3` at 1 decimal, losing the digit
that actually differentiates one Endurance point from the next early on.

### Color treatment

Labels all share the same muted, low-saturation grey (`#9C9186`) so only *values* carry
hue-coded meaning — this rule is unchanged and now extends to every new row, including the
attribute and Unspent Points rows, no exceptions.

**Core design system: one hex per attribute family, reused identically across the attribute's
own value and every derived stat it drives.** This is what makes the interleaved grouping above
actually legible — the causal link between "Strength" and "Base Attack" and "Physical Defence"
is visually a single repeated hue running down 3 consecutive rows, not something you have to
read the labels to piece together.

| Row | Label hex | Value hex | Notes |
|---|---|---|---|
| Life | `#9C9186` | `#E2574B` | Unchanged — reuses the Life bar's top-band ember/coral exactly. |
| Mana | `#9C9186` | `#5E9BE0` | Unchanged — reuses the Mana bar's top-band azure exactly. Mana isn't driven by any of the 4 attributes, so it keeps its own standalone identity rather than joining a family. |
| Unspent Points | `#9C9186` | **New, dynamic**: `#F2C94C` (warm gold) when N > 0, `#6B6459` (dim muted stone) when N = 0 | The only row whose value color changes at runtime. Gold when actionable draws the eye to "you have something to do here"; dimming to near-invisible at 0 lets the row recede once there's nothing to act on, rather than permanently occupying visual weight. Gold is new and distinct from the Strength amber below (`#F2C94C` reads as a bright saturated yellow, `#D98A3D` a duller burnt-orange amber — confirm side-by-side in-game, see open items). |
| Endurance | `#9C9186` | `#E2574B` | **Reused, not new.** Endurance drives Life and Life Regen, so it takes Life's exact ember/coral — the attribute row visually announces "I am the source of the red family below/above me." |
| Life Regen | `#9C9186` | `#E2574B` | Same family as Endurance/Life — user's own instinct in the brief ("Life Regen should read as Endurance/Life-adjacent") confirmed directly. |
| Intelligence | `#9C9186` | `#9B5FE0` | **Reused, not new.** Intelligence drives Base Magic Attack and Magic Defence, so it takes the existing arcane violet. |
| Base Magic Attack | `#9C9186` | `#9B5FE0` | Renamed from Base Magic Damage; hex unchanged. |
| Magic Defence | `#9C9186` | `#9B5FE0` | Same violet family as Intelligence/Base Magic Attack. |
| Strength | `#9C9186` | `#D98A3D` | **Reused, not new.** Strength drives Base Attack and Physical Defence, so it takes the existing bronze-amber. |
| Base Attack | `#9C9186` | `#D98A3D` | Renamed from Base Damage; hex unchanged. |
| Physical Defence | `#9C9186` | `#D98A3D` | Same amber family as Strength/Base Attack. |
| Dexterity | `#9C9186` | `#4CBB7A` | **New** — jade/verdant green. Dexterity drives 3 stats (Attack Speed, Cast Speed, Crit Chance) that don't cleanly fit any existing family, so it gets its own new hue rather than 3 separate ones — one hex per family stays the consistent rule across all 4 attributes, no exception for Dexterity just because it happens to drive more stats. Green is a widely-used, unclaimed genre convention for agility/speed-type stats (distinct from any specific existing game's branding), and sits clearly apart from the red/blue/amber/violet already in use. |
| Attack Speed Multiplier | `#9C9186` | `#4CBB7A` | Same green family as Dexterity. |
| Cast Speed Multiplier | `#9C9186` | `#4CBB7A` | Same green family as Dexterity. |
| Crit Chance | `#9C9186` | `#4CBB7A` | Same green family as Dexterity. |

Every new hex introduced this pass (`#4CBB7A` jade-green, `#F2C94C`/`#6B6459` for Unspent
Points) is original and was not copy-pasted from a known game's stat-screen palette. Reused
hexes (`#E2574B`, `#5E9BE0`, `#D98A3D`, `#9B5FE0`) are deliberate identity carries per the family
system above, not fresh design decisions.

### Attribute investment: the `+1` buttons

- Use a plain vanilla `ButtonWidget`, message text `+1`, sized small and square (~20×20 — vanilla's
  150×20 default is not appropriate here and is explicitly unenforced per the technical brief).
  Do **not** design a custom button texture/9-slice for this — vanilla's built-in button sprite
  (the same beveled light-grey chrome used by every other button in the game, including this
  same screen's tab navigation) already handles default/hovered/disabled states correctly, and
  reusing it keeps this one small control consistent with the rest of Minecraft's own UI chrome
  rather than inventing a second visual language for a single "+1" affordance — over-scoped for
  what's fundamentally a minor interactive control (`CLAUDE.md`: "do not create large systems
  before a minimal version works").
- **Enabled** (Unspent Points > 0): standard vanilla active button — default light-grey chrome,
  brightens/highlights on hover, plays the vanilla click sound.
- **Disabled** (Unspent Points = 0): set `button.active = false` and let vanilla's built-in
  disabled rendering handle it (desaturated/darkened chrome, dimmed text, no hover highlight, not
  clickable). No custom color override needed or wanted — this is the same visual language
  players already read as "can't click this" everywhere else in vanilla menus.
- Do **not** color the `+1` button's own text/background to match its row's family hue (e.g. a
  red-tinted Endurance button) — vanilla `ButtonWidget` doesn't expose that cleanly, and forcing
  it would be exactly the kind of custom chrome this treatment deliberately avoids. The family
  color lives in the value column; the button stays neutral vanilla chrome.
- Optional nice-to-have, not a blocker: a tooltip on the disabled button (e.g. "No unspent
  points") via vanilla's standard tooltip API. Flagged as an open item below since whether
  tooltips render correctly on a disabled `ButtonWidget` without extra wiring needs a quick
  in-game check.

### Layout guidance

- **Grouping via spacing, not a divider widget**: keep it to the `GridWidget`'s built-in row
  spacing rather than adding a separate divider/line widget — simplicity per `CLAUDE.md`'s "do
  not create large systems before a minimal version works," now applied at 15-row scale with
  exactly two spacing constants instead of one-off values per section:
  - **Within a family/pair** (Life↔Mana, Endurance↔Life Regen, Intelligence↔Base Magic
    Attack↔Magic Defence, Strength↔Base Attack↔Physical Defence, Dexterity↔Attack Speed↔Cast
    Speed↔Crit Chance): ~6 px row spacing.
  - **Between families/groups** (Mana→Unspent Points, Unspent Points→Endurance, Life
    Regen→Intelligence, Magic Defence→Strength, Physical Defence→Dexterity): ~14 px (roughly
    double the within-family gap).
  - Label→value column gap: ~4–6 px, value→button column gap: ~4 px (tight — the button reads
    as an inline action on its row, not a separate column of controls).
- **No extra header inside the tab content.** The `TabNavigationWidget` button itself already
  reads "Stats" — repeating a "Stats" header inside the `GridScreenTab` content would be
  redundant. If a second tab is added later and the content still feels like it needs a title,
  revisit this then rather than pre-building it now.
- **Alignment**: label column left-aligned, value column left-aligned immediately after it (not
  right-aligned/tabular-number-style) — matches vanilla's own options-list and statistics
  screens rather than inventing a spreadsheet-like layout. Button column follows immediately
  after the value, also left-aligned, so the row reads left-to-right as "label → current value →
  action" in natural order.
- Exact pixel row-spacing values above are reasonable defaults to hand to implementation, not
  verified in-game yet — normal "eyeball it once it renders and nudge ±2–4px" polish, not a
  design gap blocking implementation.

### Naming/asset conventions established here

- No new texture/model/icon files — this screen is entirely vanilla widget composition
  (`GridWidget` + `TextWidget` + `ButtonWidget`) plus text color, no PNGs. The `+1` buttons use
  vanilla's stock button sprite, not a custom one.
- `Base Damage` / `Base Magic Damage` are retired names — use `Base Attack` / `Base Magic Attack`
  in all code, UI text, and future references; the amber/violet hexes carry over unchanged.
- Family hex-reuse is now the established rule for any *future* attribute-driven stat too: if a
  5th attribute or a new derived stat is ever added under an existing attribute, it inherits that
  attribute's existing hex rather than getting a new one — don't treat this as "4 colors, done,"
  treat it as "1 hex per attribute family, extend the family's row list as needed."
- If a second tab is added later (e.g. "Skills"), give it its own subsection here documenting any
  tab-specific palette rather than assuming it inherits this tab's hexes — the reds/blues/
  ambers/violets/greens/gold above are this Stats tab's identity specifically, not a general
  palette free for reuse elsewhere.

### Open items (expected, not blockers)

- **Gold vs. amber legibility**: confirm `#F2C94C` (Unspent Points, actionable) and `#D98A3D`
  (Strength family) read as clearly distinct colors side-by-side in-game, not just in isolation —
  they're both warm/orange-adjacent hues at different saturation/lightness, should be fine but
  hasn't been rendered yet.
- **15-row total height**: confirm the full row set (with the two-tier 6px/14px spacing) still
  fits comfortably within the `GridScreenTab` viewport at common window sizes without scrolling.
  If it's tight, reduce the 14px between-family gap toward ~10px first rather than the 6px
  within-family gap (collapsing within-family spacing would undercut the grouping this whole
  layout is built around).
- **Disabled-button tooltip**: whether a tooltip on a disabled (`active = false`) `ButtonWidget`
  renders out of the box or needs manual `Screen.renderTooltip` wiring — quick check once
  implemented, not a design gap.

## Changelog

- 2026-07-04: Expanded "Character Stats Screen" for the new attribute system: 4 core attributes
  (Endurance/Intelligence/Strength/Dexterity) with `+1` investment buttons, an Unspent Points
  counter, and 8 derived stats — 15 rows total, still one tab. Renamed `Base Damage`/`Base Magic
  Damage` to `Base Attack`/`Base Magic Attack` (hexes unchanged). Adopted a "one hex per
  attribute family, reused by every derived stat it drives" color system: Endurance/Life Regen
  reuse Life's `#E2574B`, Intelligence/Magic Defence reuse Base Magic Attack's `#9B5FE0`,
  Strength/Physical Defence reuse Base Attack's `#D98A3D`; Dexterity/Attack Speed/Cast Speed/Crit
  Chance get a new jade-green `#4CBB7A` (no existing family fit an agility stat). Unspent Points
  gets a new dynamic gold/dim-grey value color (`#F2C94C` when spendable, `#6B6459` at zero).
  Rows are interleaved by attribute family (not blocked attributes-then-derived-stats) so
  proximity + color teach which attribute drives which stat. `+1` buttons specified as plain
  vanilla `ButtonWidget`s (no custom texture), toggling `active` for the enabled/disabled states
  vanilla already renders correctly.
- 2026-07-04: Added "Character Stats Screen" section — row order/format and label/value color
  spec for the 'C'-key full-screen stats tab (Life, Mana, Base Damage, Base Magic Damage).
  Reuses the HUD's exact Life/Mana hexes for those two rows; introduces a new amber
  (`#D98A3D`) / violet (`#9B5FE0`) pair for Base Damage / Base Magic Damage, chosen specifically
  to avoid colliding with Life's red or Mana's blue on the same screen.
- 2026-07-04: Initial creation of this document. Defined the Life bar / Mana bar HUD color
  palette, layout, and stacking spec (this section). No prior visual identity existed to
  reconcile with.
