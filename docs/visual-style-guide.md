# Baum2 Visual Style Guide

Persistent visual-identity reference for Baum2. Treat this the same way
`docs/fabric-modding.md` is treated for Fabric API findings: **append and correct in place,
don't let decisions evaporate.** Whoever adds a new item, class, faction, rarity tier, or UI
screen should check here first, and add to it once new visual identity is established.

Owner/maintainer role: the `graphics-designer` subagent (see `CLAUDE.md` -> "Project Agents").
This file did not exist before 2026-07-04; it was created from scratch alongside the first
custom HUD/GUI work (see "Changelog" at the bottom).

**Note on this document's history**: it was created independently and near-simultaneously on
two branches (`jonas_workbranch`'s Class System UI, `fischey_workbranch`'s Vitals/Character
Stats System UI) that were merged together after both were already written. Sections 1-10
below are the original Class-System-side framework (art direction, base palette, panel
conventions, HUD/Class-Screen specs). Sections 11-12 are the Vitals/Character-Stats-side
specs, folded in as-is at merge time. **The two sides used independently-invented colors that
are not yet unified** (see the reconciliation note after Section 1) — that's a deliberate,
flagged follow-up, not an oversight.

---

## 0. IP-compliance guardrails (read first)

This section exists because the UI work that prompted this guide started from a request for
a "Metin2 look" that was explicitly rejected for conflicting with `MASTERPROMPT.md` section
10 ("Keine Nachahmung bekannter MMORPG-UIs" - no imitation of known MMORPG UIs, not just no
asset copying). Everything below is designed to the safe alternative that was chosen instead:
**original styling using only generic, non-distinctive MMORPG UI conventions.**

Concretely, for all Baum2 UI work:

- **Allowed** (industry-wide genre convention, not any one game's IP): bottom-center hotbar/
  skill bar, corner minimap, health/mana/XP bars, grid inventories, color-coded item rarity,
  color-coded class identity, panels with a border and a title. These layouts are shared
  across dozens of unrelated games and are not recognizable as any specific one.
- **Not allowed**: anything that reads as "inspired by Metin2" (or any other specific existing
  MMORPG) to a player who knows that game. Concretely excluded from Baum2's palette and
  motifs: wood-panel backgrounds, gold-leaf/gold-trim ornamental borders, dragon motifs,
  oriental-fantasy (East Asian wuxia/xianxia) styling, or reproducing any specific game's
  exact color branding.
- Baum2's own visual identity (this document) is a **cool slate/verdigris/rune-cyan fantasy
  palette** with flat, crisp, geometric panel chrome - deliberately distinct in every one of
  those excluded dimensions (no wood, no gold-leaf ornament, no dragons, no oriental styling).
- Class-color-coding (Section 3.3) inevitably lands somewhere on a shared color wheel with a
  small number of hues (any 4-class system will). None of the four chosen hues are exact or
  near-exact matches to a specific existing game's exact class-color branding, and none pair
  color with copied iconography - see the caveat note at the end of Section 3.3 for the one
  case worth double-checking later (Runenwirker's teal, if that class's kit grows a strong
  visual identity of its own).
- If any future contributor is unsure whether a proposed color/icon is "too close" to a real
  game's specific branding (as opposed to a genre convention), the correct move is to say so
  explicitly and propose a more distinct alternative, not to guess. Same bar as naming.
- This same bar applies to the Vitals/Character-Stats-Screen work in Sections 11-12: those
  sections' own text already argues their bar/screen chrome is a generic, unclaimed genre
  convention rather than any specific game's look — consistent with the rule here.

---

## 1. Art direction

Working name for the look: **"Deepwood & Verdigris."** Baum2's fantasy world is inferred from
its existing original naming (Eisenwächter/iron, Schattenläufer/shadow, Runenwirker/rune,
Wesenswahrer/nature-being; world-event names like Rissobelisk, Sternsplitter, Runenkern) -
an old, overgrown, rune-marked world where iron, forest, and half-forgotten magic overlap.
The UI should feel like it belongs to *that* world: weathered metal and stone, moss/patina
green, and cold rune-light - not a generic default-blue game engine UI, and not a
"treasure chest" gold-and-wood fantasy UI (that reads as generic MMORPG cliché and edges
toward the excluded Metin2-adjacent look).

Guiding principles:

- **Flat and crisp, not skeuomorphic.** Panels are flat-filled rectangles with a simple
  two-tone line border (Section 4) - no carved-stone bevels, no wood grain, no gold
  ornamentation. This is both an original-identity choice and a practical one (Minecraft's
  `DrawContext.fill` primitives make flat panels trivial and reliable; ornate textures need
  hand-drawn art, which this guide does not fake - see Section 9).
- **Color carries meaning, consistently.** Once a color means something (Section 3), it
  means that thing everywhere: rune-cyan always marks a numeric stat bonus, muted gray-green
  always marks disabled/unavailable, a class's accent color always marks that class, regardless
  of which screen or HUD element it appears on.
- **Square corners, no rounding.** Deliberate, not a limitation - it reads as "ironwork/
  rune-stone ledger," distinct from soft rounded-corner mobile-game UI and from vanilla
  Minecraft's beveled stone GUI.
- **Legible over decorative.** Text contrast and consistent spacing take priority over visual
  flourish. This is a functional MMORPG HUD, not a diegetic art piece.

**Reconciliation note (added at merge time)**: Sections 11-12 below (Vitals Life/Mana bars,
Character Stats Screen) were designed independently, before this section existed to check
against, and use their own hex palette (warm coral-ember/garnet life, azure/sapphire mana,
plus a per-attribute-family amber/violet/jade-green/gold system on the Stats screen) rather
than this section's Deepwood/Verdigris/Rune-Cyan palette. Neither palette copies any existing
game's specific branding (each section argues this independently), so there's no IP concern —
but the mod now has two parallel, un-unified UI color systems rather than one. Whether to
reskin Sections 11-12 onto this section's palette, or to keep vitals/combat-stat colors as
their own deliberately-separate "combat HUD" identity distinct from the "menu chrome" identity
here, is an open decision for a future pass — not resolved as part of this merge. Don't treat
either palette as more "correct" than the other in the meantime; both are documented in full
below.

---

## 2. Color palette overview

All colors given as 6-digit hex (`#RRGGBB`) for design reference, plus the Minecraft ARGB
`0xAARRGGBB` int form where opacity matters (Minecraft's `DrawContext.fill`/text color
arguments take packed ARGB ints).

### 2.1 UI chrome (panel backgrounds, borders, base text)

| Role | Name | Hex | ARGB (typical use) |
|---|---|---|---|
| Panel background (HUD, semi-transparent) | Deepwood Ink | `#161B19` | `0xE6161B19` (~90% opacity) |
| Panel background (full-screen GUI, near-opaque) | Deepwood Ink | `#161B19` | `0xF5161B19` (~96% opacity) |
| Nested/recessed card background (unselected) | Deepwood Ink, lighter | `#1F2622` | `0xF01F2622` |
| Border, outer line (shade) | Void Seam | `#0B0E0D` | `0xFF0B0E0D` |
| Border, inner line (highlight) | Verdigris | `#4E6B5C` | `0xFF4E6B5C` |
| Nested-card border (unselected) | Verdigris, muted | `#33443B` | `0xFF33443B` |

### 2.2 Accent colors

| Role | Name | Hex | Notes |
|---|---|---|---|
| Primary accent / selection / interactive highlight | Verdigris Glow | `#5FA98C` | Selected-state borders, hover states, active toggle |
| Secondary accent / magic-XP-rune fill | Rune Cyan | `#7FD8E0` | XP-style progress fills, bonus/stat numeric values |
| Emphasis accent (sparse use only) | Aged Brass | `#D9B36C` | Level numbers, screen titles' single emphasis word, Mythisch rarity tier - **not** a border/trim color (that would drift toward the excluded gold-trim look) |
| Cost/negative-value accent | Rust Ember | `#D9776C` | Reserved for future upgrade-cost / malus text; not used by class/HUD work in this pass |

### 2.3 Text colors

| Role | Hex | Usage |
|---|---|---|
| Header / title | `#EDE6D6` (Parchment White) | Screen titles, panel headers |
| Body text | `#B9C4BE` (Sage Gray) | Descriptions, general copy |
| Bonus/stat value | `#7FD8E0` (Rune Cyan) | Any positive numeric stat/bonus display, always prefixed `+` |
| Cost/malus value (reserved) | `#D9776C` (Rust Ember) | Future upgrade costs / negative values |
| Muted/disabled | `#5B655F` (Dim Moss) | Hint text, locked/unavailable entries, footer captions |
| Emphasis / level numbers | `#D9B36C` (Aged Brass) | Level number in HUD, single-word emphasis in headers |

### 2.3.1 Font

Use Minecraft's default built-in text renderer/font for all v1 UI (`Text.literal(...)` with
the client's default `TextRenderer`, `drawWithShadow` for readability over the game world).
**No custom font file in this pass** - keep scope minimal; a bespoke Baum2 font (via a resource
pack font provider) is a reasonable future enhancement but is not required and would be a
separate, larger piece of work. Use `Formatting.BOLD` (vanilla supports this on the default
font) to distinguish headers/class names from body text rather than a different font weight.

### 2.4 Rarity tiers

Per `MASTERPROMPT.md`'s five example rarities (Gewöhnlich -> Astral). Color-coded item rarity
is a standard, IP-free genre convention (explicitly called out as fine in the graphics-designer
brief); the specific hues/order below are original and were deliberately chosen to *not*
reproduce any one existing game's known rarity-color sequence (no white/green/blue/purple/
orange WoW-style ladder, no white/blue/yellow/orange Diablo-style ladder - see note below).

| Tier | Hex | Name | Notes |
|---|---|---|---|
| Gewöhnlich (common) | `#B7BDB9` | Stone Gray | Neutral, low saturation |
| Selten (rare) | `#5B93C4` | Slate Blue | Cool, clear |
| Veredelt (refined/mid-high) | `#A868C9` | Amethyst | Violet-magenta, not the same hue as any class color |
| Mythisch (mythic) | `#D98F3E` | Bronze Ember | Muted amber-bronze, deliberately *not* saturated "legendary orange" - ties back to the Aged Brass UI accent for world-cohesion |
| Astral (highest) | `#D9CFFF` on `#2E2A5C` | Starlight | Pale lavender-white text/glow on a deep indigo undertone - a distinct two-tone "cosmic" treatment rather than just the next rainbow step, reinforcing Astral as a tier above the normal ladder, not just "brighter orange" |

*Compliance note:* gray -> blue -> violet -> bronze -> starlight-lavender does not match any
specific existing game's known rarity ladder tone-for-tone. This is a genre-convention
mechanic (color = rarity) executed with original hues, per the brief's own guidance that this
specific mechanic is explicitly fine to use.

### 2.5 Faction colors

**Not defined yet.** `MASTERPROMPT.md` lists three *example* faction names (Haus Solvyr, Orden
Myrkan, Bund Avarra) but the faction system itself is not implemented in code yet (no
`factions/` package exists as of this writing). Do not assign specific hex colors to those
example names prematurely - they may change before the faction system actually lands, and
committing colors to placeholder lore names risks having to un-teach an association later.

Forward guidance for whoever implements factions: pick 3 saturated, well-separated hues that
are visually distinct from *both* the 4 class colors (Section 3.3) and the 5 rarity colors
(Section 2.4), so a player can never confuse "this is tinted by my class" / "this is a rarity
color" / "this is a faction color" at a glance. Update this section (not a separate document)
once faction names and colors are actually decided.

---

## 3. Class identity

### 3.1 Source of truth

Class names, descriptions, and passive bonuses come from
`src/main/java/de/baum2dev/baum2/classes/ClassRegistry.java` - this style guide's job is to
give each class a consistent *visual* identity on top of that data, not to re-decide the
data itself. If `ClassRegistry` changes (new class, renamed class, changed bonus), update the
table below in the same change.

### 3.2 Fixed class order

Wherever multiple classes are listed together (Class Screen, `/baum2 class list`), use the
`PlayerClass` enum's declared order consistently: **Eisenwächter, Schattenläufer, Runenwirker,
Wesenswahrer.** Don't re-sort alphabetically or by anything else - a stable order across every
UI surface is itself a small usability/consistency win.

### 3.3 Per-class accent color + icon motif

| Class | Accent color | Hex | Icon motif (placeholder, see Section 9) | Theme rationale |
|---|---|---|---|---|
| Eisenwächter | Steel Blue | `#8FA3B3` | Pentagon shield silhouette with a horizontal "plate seam" line | Iron, defense, calm resilience |
| Schattenläufer | Twilight Violet | `#7C5CA0` | Solid double-chevron ("fast-forward"/dash) shape | Shadow, speed, motion |
| Runenwirker | Rune Cyan | `#66C4C2` | Diamond with an inset rune-cross (plus-shaped) engraving | Rune magic, arcane luck |
| Wesenswahrer | Moss Green | `#7FA65C` | Rounded leaf/teardrop silhouette with a center vein line | Nature, protection, "being" |

Darker outline tone for each icon (used for the 1px pixel-art outline stroke, not for text):
Eisenwächter `#4A5A66`, Schattenläufer `#3E2E52`, Runenwirker `#2E5C5A`, Wesenswahrer `#3F5A2E`
(each is roughly the accent color at ~40% value, same hue).

These four accent colors are used consistently for: the class's name text in the HUD
(Section 8) and Class Screen (Section 9), the class's icon (always rendered in its own
accent, never recolored per-context), and — distinctly from *selection* state, which uses the
universal Verdigris Glow accent instead (Section 9.4) so class identity and "this is my
current class" are never conflated into a single color meaning two different things.

*Compliance caveat:* these four hues (steel-blue-gray, violet, teal-cyan, moss-green) were
chosen to be maximally spread around the color wheel for at-a-glance distinctness, which is
also just how most game class-color systems end up looking with 4 slots. None of them are a
close match to a specific real game's *exact* class hex values as far as I could verify, and
none are paired with that game's iconography (our icons are abstract geometric shapes, not
weapon/spell glyphs). The one worth re-checking later: Runenwirker's teal-cyan is in the same
general family as some existing games' single "arcane/nature-caster" class colors. Low
confidence, not a hard conflict today - flagging per this agent's own instructions to say so
rather than guess, worth another look only if Runenwirker's kit grows enough unique visual
material (a portrait, a distinct spell-effect color, etc.) that a closer comparison becomes
possible.

---

## 4. Panel & border treatment (applies to every panel, HUD or GUI)

- **Corners:** square, unrounded. No corner texture/mask - a plain rectangle.
- **Border composition:** always a 2px total border made of two 1px lines:
  - Outer line: Void Seam `#0B0E0D` (near-black, reads as a hard edge against any world
    background).
  - Inner line: Verdigris `#4E6B5C` (the "glow" line that gives panels their identity color).
  - Implementation note: draw as two nested `DrawContext.fill` rectangles (outer border rect,
    then inner border rect 1px smaller on each side, then the content-background rect 1px
    smaller again) rather than a texture - consistent with the flat/crisp direction in
    Section 1 and avoids needing a hand-drawn 9-slice texture for v1.
- **Background opacity:**
  - HUD overlay panels (Section 8): ~90% opacity (`0xE6` alpha) - readable over any world
    backdrop while still clearly reading as an "overlay," not a solid game element.
  - Full-screen GUI panels (Section 9): ~96% opacity (`0xF5` alpha) - screens already dim the
    world behind them via the standard `Screen` background dim, so the panel itself can be
    closer to opaque without looking out of place.
- **Nested cards** (e.g. each class entry on the Class Screen) get a *lighter* recessed
  background (`#1F2622`) and a *thinner, single-tone* 1px border (`#33443B`, no two-tone
  treatment) - this establishes a "frame within a frame" hierarchy: the outer screen panel
  gets the full two-tone treatment, nested content cards inside it get a visually quieter
  single-line treatment, so nesting depth is always readable from border weight alone.
- **Selected/active state** (Section 9.4) always adds a 2px solid Verdigris Glow (`#5FA98C`)
  border *replacing* the card's normal 1px border, plus a low-alpha (~18%) background tint
  wash of the same color - never conveyed by border color alone without a matching text/shape
  cue (see Section 9.4), for colorblind-accessibility.

---

## 5. Iconography & motifs established so far

- **Geometric, not pictorial.** Class icons (Section 3.3) are abstract flat shapes (shield,
  chevron, diamond, leaf), not miniature weapon/character portraits. Keep following this
  convention for future icon needs (skills, items) unless a specific deliverable calls for
  more detail (at which point it becomes a creative brief per this agent's own scope, not a
  quick placeholder).
- **"+value" = Rune Cyan.** Any place a numeric bonus/stat is shown, prefix it with `+` and
  color it Rune Cyan (`#7FD8E0`), regardless of which class/item/system it comes from. This is
  the one color-to-meaning mapping every future numeric-bonus display (upgrade screen, skill
  tooltips, etc.) should reuse rather than reinvent. (Section 11-12's Vitals/Stats-screen work
  uses its own, separate per-attribute-family color system instead — see the reconciliation
  note in Section 1.)
- **"Aktiv" tag = current selection, always text + border, never color alone.** Established in
  Section 9.4; reuse for any future "this is your current X" UI (e.g. an equipped item, an
  active skill loadout slot).

---

## 6. Asset naming & folder conventions

Standard Minecraft 1.21.11 resource-pack layout under `src/main/resources/assets/baum2/`:

```
assets/baum2/
  textures/
    gui/
      class/
        eisenwaechter.png       (16x16, class icon - see Section 9)
        schattenlaeufer.png
        runenwirker.png
        wesenswahrer.png
    item/        (future: item icons, 16x16 unless an item type calls for otherwise)
    block/       (future: block textures)
  models/
    item/        (future)
    block/       (future)
  blockstates/   (future)
```

Conventions:

- File/folder names: lowercase, ASCII only, matching the `PlayerClass` enum's `name()` in
  lowercase (`EISENWAECHTER` -> `eisenwaechter.png`) - keeps texture `Identifier`s derivable
  directly from the enum without a separate lookup table.
- GUI-only chrome (icons/sprites drawn directly by custom HUD/Screen Java code, not bound to
  an item/block) lives under `textures/gui/`, subdivided by concept (`gui/class/` for
  class icons; a future `gui/rarity/` if rarity ever gets icon badges instead of just text
  color, etc.) - mirrors vanilla's own `textures/gui/sprites/...` convention of grouping
  UI-only textures separately from in-world item/block textures.
- Every placeholder texture file must be called out as a placeholder in the commit/PR that
  adds it and in this document (Section 9) - per `MASTERPROMPT.md`'s asset rule ("Kennzeichne
  Platzhalter klar als eigene temporäre Platzhalter"). Placeholders are fine to ship
  (functionality shouldn't block on final art) but must never be silently mistaken for
  finished art later.

---

## 7. HUD overlay spec: "Player Status Overlay"

Purpose: show player level, a compact XP-to-next-level indicator, and the player's selected
class (name + icon) - small and unobtrusive, supplementing (never replacing or duplicating)
vanilla's existing hotbar XP/level bar, which the mod already relies on
(`ClientNetworkingHandler` drives it via `ClientPlayerEntity.setExperience(...)`; see
`docs/fabric-modding.md` "Rendering / HUD"). Renders in the **top-left** corner; the Vitals
Life/Mana bars (Section 11) render in the **left status-bar column near the hotbar** — the two
occupy different screen regions and don't collide (confirmed at merge time).

**Implementation status (as of the merge that brought this doc's two halves together): this
spec is implemented** as `ui/PlayerStatusHud.java`, replacing the old dead
`ui/ProgressionHud.java` prototype this section originally described as needing replacement.

### 7.1 Position & size

- **Anchor:** top-left corner of the screen, in scaled GUI coordinates
  (`drawContext`'s coordinate space, not raw pixels - same space vanilla HUD elements use).
- **Offset:** 6px from the left edge, 6px from the top edge. Top-left is chosen because it's
  otherwise-empty vanilla HUD space: the hotbar/health/food/XP bar cluster is bottom-center,
  vanilla status-effect icons default to top-right, and boss health bars (when present) sit
  top-center - top-left is clear in all normal play. It sits behind the F3 debug overlay when
  that's open, same as every other HUD mod's overlays; that's expected and not a design flaw.
- **Panel size:** 118px wide x 30px tall (outer edge, including the 2px border from Section 4).
  Sized to comfortably fit the longest class name (`Schattenläufer`, 14 characters) at default
  font scale without truncation.

### 7.2 Internal layout

Padding: 4px on all sides inside the border, giving an inner content area of
110 x 22px starting at (panel_x+4, panel_y+4).

**Row 1** (top row, 16px tall - matches icon height):
- Class icon: 16x16, drawn at native size (no upscale) at the row's left edge
  (`inner_x`, `inner_y`).
- Class name: text starting 4px right of the icon (`inner_x + 16 + 4`), vertically centered
  against the icon row. Color = that class's accent color (Section 3.3), `Formatting.BOLD`.
- Level: right-aligned within the row, ending flush with the inner content area's right edge
  (`inner_x + 110`). Format: `"Lv. " + level` (e.g. `"Lv. 12"`). Color = Aged Brass (`#D9B36C`).

**Row 2** (bottom row, 2px gap below row 1, 3px tall): compact XP-to-next-level indicator -
a single thin horizontal progress bar, *not* a redraw of vanilla's XP bar:
- Track: full inner width (110px), 3px tall, filled with a dark recessed tone
  (`0xFF1F2622`, same as the nested-card background in Section 4) as the "empty" background.
- Fill: Rune Cyan (`#7FD8E0`), width = `track_width * (currentXp / xpForNextLevel)`, same
  3px height, drawn on top of the track from the left edge.
- Deliberately no percentage/number text on this bar (keeps it "compact" per the brief) and
  deliberately a different color (cyan vs. vanilla's green) and different shape (thin 3px
  sliver vs. vanilla's thick bar) so it reads as a distinct, smaller supplementary indicator
  rather than a duplicate of the vanilla bar.
- Data source: same level/XP values already available client-side via `ExperienceSyncPayload`
  / `player.experienceProgress` + `player.experienceLevel` (vanilla client fields, already
  kept in sync by `ClientNetworkingHandler` - see `docs/fabric-modding.md`). Class name/icon
  come from `ClassManager.SELECTED_CLASS`, which is synced to the client via the Attachment
  API's `.syncWith(...)` (see `docs/fabric-modding.md` "Custom UI (HUD / Screens)").

### 7.3 Visibility rules

- Hide entirely when `client.options.hudHidden` is true (same rule `ProgressionHud` already
  correctly applies) or when the player has no class selected yet (Row 1's class slot would
  otherwise show nothing meaningful) - in the no-class-selected case, hide the whole panel
  rather than showing an empty/placeholder class row.

---

## 8. Class Screen spec: "Klassenübersicht"

A full `Screen` (not a `HandledScreen`/container screen - no player inventory grid involved),
GUI-native alternative to `/baum2 class select <class>`, backed by the same
`classes/ClassRegistry` (for the static list) and `classes/ClassManager` (for reading/writing
the player's current selection). Lists all 4 classes with name, one-line description, and
passive bonus; highlights the current selection; lets the player click an entry to switch.
Opened via a dedicated **K** keybind, independent of the Character Stats Screen's **C** keybind
(Section 12) — see the note at the end of Section 12 about whether these two screens should
eventually be unified.

**Implementation status: implemented** as `ui/ClassScreen.java`.

### 8.1 Panel

- **Size:** 220px wide x 238px tall.
- **Position:** centered on screen (`x = (screenWidth - 220) / 2`, `y = (screenHeight - 238) / 2`).
- **Background/border:** full-screen-GUI treatment from Section 4 (2px two-tone border,
  ~96% opacity Deepwood Ink background). Standard vanilla `Screen` world-dimming behind it.
- **Title:** `"Klassenübersicht"` (plain descriptive German UI label, consistent with the
  mod's existing German class/skill naming - not lore text, so no naming-compliance concern),
  centered horizontally, 6px from the panel's top inner edge, Header color (`#EDE6D6`),
  `Formatting.BOLD`.
- **Optional close control:** a 12x12 "x" glyph, top-right corner of the panel (6px inset from
  top and right edges), Dim Moss (`#5B655F`) normal / Verdigris Glow (`#5FA98C`) on hover.
  Optional because `Screen.shouldCloseOnEsc()` already closes it by default - include only if
  the implementer wants an explicit mouse-clickable close affordance too. (Not implemented in
  v1 — Escape-to-close only.)

### 8.2 Class entry list

Fixed order per Section 3.2 (Eisenwächter, Schattenläufer, Runenwirker, Wesenswahrer).

- **Card size:** 204px wide x 40px tall (204 = 220 panel width - 8px padding each side).
- **Card position:** `x = panel_x + 8`; first card `y = panel_y + 28` (below the 20px header
  band); each subsequent card `y += 46` (40px card + 6px gap).
- **Card background/border (default/unselected):** Section 4's "nested card" treatment -
  `#1F2622` background, 1px `#33443B` border.
- **Card internal layout** (6px padding inside the card):
  - Icon: the class's 16x16 texture (Section 3.3/9), upscaled to 32x32, at
    (`card_x + 6`, `card_y + 4`) - vertically centered in the 40px card.
  - Text block starts at `card_x + 6 + 32 + 8 = card_x + 46`, available width 152px
    (`204 - 46 - 6` right padding):
    - Line 1 (`card_y + 6`): class display name, that class's accent color, `Formatting.BOLD`.
    - Line 2 (`card_y + 17`): one-line description (`ClassDefinition.description()` verbatim),
      Body color (`#B9C4BE`); truncate with an ellipsis if it would exceed 152px
      (`TextRenderer.trimToWidth`, confirmed present in 1.21.11).
    - Line 3 (`card_y + 28`): passive bonus, formatted as `"+" + <human-readable value>` (e.g.
      `"+4 Leben"`, `"+10% Lauftempo"`, `"+1 Glück"`, `"+10% Rückstoßresistenz"`), always in
      Rune Cyan (`#7FD8E0`) per the universal "+value = cyan" convention (Section 5) regardless
      of class.

### 8.3 Selected-state treatment

For whichever class matches `ClassManager.getSelectedClass(player)`:

- Card border: 2px solid Verdigris Glow (`#5FA98C`), replacing the default 1px `#33443B`
  border (card grows by 1px outward on each edge, or the card's base size reserves the extra
  1px so overall card footprint doesn't shift - implementer's call).
- Card background: same `#1F2622` base, with an additional ~18%-alpha Verdigris Glow color
  wash on top (`0x2E5FA98C` as a packed ARGB fill over the card background).
- **Text tag**, top-right corner of the card (`card_x + 204 - 6 - textWidth`, `card_y + 6`):
  the word `"Aktiv"`, Verdigris Glow color, `Formatting.BOLD`. This is the redundant
  non-color cue - selection is never communicated by border/wash color alone, so it stays
  legible for colorblind players and at a glance even if someone's display renders the accent
  color oddly.

### 8.4 Interaction

- Each full card is clickable and sends a new C2S `ClassSelectPayload` (mirrors the existing
  `ExperienceSyncPayload` pattern), triggering `ClassManager.selectClass` server-side. The
  screen stays open and updates live once the sync round-trips (no explicit "saving..." state
  needed).
- **Hover (unselected cards only):** lighten the card border to Verdigris Glow at ~50% alpha
  as a hover affordance (Minecraft GUIs don't change cursor shape, so this is the primary
  "this is clickable" signal, consistent with vanilla button hover treatment).
- Clicking the already-selected card is a harmless no-op / redundant reselect - no special
  guard needed, since `ClassManager.selectClass` already safely handles reselecting the same
  class (remove-then-reapply).

### 8.5 Footer

- Hint text, centered horizontally, `panel_y + 238 - 8 - 10`: `"Klicke eine Klasse an, um sie
  auszuwählen."` - Dim Moss color (`#5B655F`), regular weight.

---

## 9. Placeholder class icons (produced this pass)

**These are explicitly temporary placeholders**, per `MASTERPROMPT.md`'s asset rule
("Kennzeichne Platzhalter klar als eigene temporäre Platzhalter") - flat-color, simple
geometric shapes generated programmatically (Python/Pillow), not hand-drawn final art. Good
enough to build and test the HUD/Screen against; should be replaced by a human artist (or a
follow-up dedicated art pass) before this is considered finished visual content. No traced,
extracted, or downloaded source material was used - shapes are original basic geometry
(pentagon, chevron, diamond, ellipse+triangle).

| File | Class | Shape | Fill | Outline |
|---|---|---|---|---|
| `assets/baum2/textures/gui/class/eisenwaechter.png` | Eisenwächter | Pentagon shield + seam line | `#8FA3B3` | `#4A5A66` |
| `assets/baum2/textures/gui/class/schattenlaeufer.png` | Schattenläufer | Solid double-chevron (fast-forward/dash) | `#7C5CA0` | `#3E2E52` |
| `assets/baum2/textures/gui/class/runenwirker.png` | Runenwirker | Diamond + rune-cross engraving | `#66C4C2` | `#2E5C5A` |
| `assets/baum2/textures/gui/class/wesenswahrer.png` | Wesenswahrer | Leaf/teardrop + center vein | `#7FA65C` | `#3F5A2E` |

All four: 16x16, RGBA with transparent background, no anti-aliasing (flat pixel-art edges),
generated by a one-off script (not checked into the repo - reproducible from the specs in
this table if regeneration is ever needed).

---

## 10. Open items for the implementer

Non-visual details this spec deliberately leaves open (implementation choices, not design
choices) - listed here so they aren't lost:

1. Exact human-readable formatting of each class's passive bonus text (Section 8.2, Line 3) -
   e.g. mapping `EntityAttributes.MOVEMENT_SPEED` + `ADD_MULTIPLIED_BASE` + `0.10` to
   `"+10% Lauftempo"` - was resolved ad hoc in `ClassScreen`'s implementation; if a 5th class
   or a new attribute type is added later, extend that mapping rather than inventing a new one.
2. **Real (non-placeholder) art for the 4 class icons** is still outstanding — see Section 9.
3. **Unify or deliberately keep-separate the Section 1 vs. Section 11/12 color palettes** — see
   the reconciliation note in Section 1. Not blocking, but shouldn't be forgotten.
4. **Whether `ClassScreen` (Section 8) should become a tab inside the Character Stats Screen
   (Section 12)** rather than a fully separate screen — see the note at the end of Section 12.
   A product decision, not inferable from the code alone.

---

## 11. HUD: Life bar & Mana bar (Vitals system, replaces vanilla health bar)

**Status**: spec finalized, implemented. Values (from progression design, not this agent's
concern): Life = 500 + 10×level (510–1500), Mana = 100 + 5×level (105–600).

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
  no armor — now always meaningfully filled).
- **Horizontal position**: left-side status-bar column, i.e. the same screen region vanilla
  hearts occupied — anchored via `attachElementBefore(VanillaHudElements.ARMOR_BAR, ...)` per
  `docs/fabric-modding.md`. Does **not** collide with the hunger bar, which stays in its own
  unmodified right-side column (`FOOD_BAR` untouched) — collision is only a risk within the
  same column, and Life/Mana only occupy the left one. Also does not collide with the
  `PlayerStatusHud` (Section 7), which occupies the top-left corner, a different region.
- **`StatusBarHeightProvider`**: register one for the combined element (both bars + the 1px
  gap = 17 px total) via `HudStatusBarHeightRegistry.addLeft(...)`, using the same
  `Identifier` as the `HudElement` registration, so anything else that stacks in that column
  (if ever added) lays out correctly beneath/above it instead of overlapping.

### Open items (expected, not blockers)

- No numeric text (e.g. "420/510") is drawn anywhere on the bars per the no-text-rendering
  constraint that predated this doc's confirmation (Section 7's implementation / the
  `docs/fabric-modding.md` alpha-channel finding) that reliable text rendering is actually
  possible — if/when revisited, consider a tooltip-on-hover or a togglable numeric readout as
  an *addition*, not a change to the bar visuals themselves.
- Class-name banner and level-diamond badge are explicitly deferred (out of scope for this
  spec) — when they're designed, keep them visually subordinate to (not competing with) this
  bar system; they'll likely need their own section here.

### Naming/asset conventions established here

- No new texture/model files needed for this system — it's pure `DrawContext.fill` rectangles,
  no PNGs. If a future pass adds icon glyphs next to the bars (e.g. a small life/mana icon),
  follow standard 16×16 `textures/gui/` placement and document it here.
- HUD element `Identifier`s live in the `baum2` namespace, e.g.
  `Identifier.of("baum2", "life_mana_bars")` per `docs/fabric-modding.md`.

---

## 12. Character Stats Screen (full-screen, opened/closed with 'C')

**Status**: implemented, including the attribute-system expansion (4 core attributes with
`+1` investment buttons, 8 derived stats, an Unspent Points counter — 15 rows total). Built on
vanilla's real tab-navigation widget system (`Tab` / `TabManager` / `TabNavigationWidget` /
`GridScreenTab`), starting with exactly one tab ("Stats") so more tabs (Skills, Class, etc.)
can be added later without redesigning the chrome. Content is a `GridWidget` inside the
`GridScreenTab`, populated with `TextWidget` rows, refreshed every frame from live data. This
is a full menu `Screen` (like vanilla's own Statistics/options screens) — normal
`renderBackground` darkening applies behind it, and text renders through the normal
`DrawContext.drawText` pipeline (reliable — see `docs/fabric-modding.md`'s alpha-channel
finding for why an earlier ad-hoc HUD text attempt looked unreliable).

### Why this treatment

The HUD bars above (Section 11) deliberately carry **no numeric text** (bar-only). This screen
is the "togglable numeric readout" that section anticipated, but built properly as a menu
screen using vanilla's tab system rather than bolted onto the HUD overlay. Splitting it this
way keeps the HUD minimal/glanceable and gives numeric detail a dedicated, low-frequency-access
screen — a standard, unclaimed genre convention (every MMORPG has *a* character/stats panel;
the specific chrome and palette here are original).

### Row order and format

Three-column layout per row: a label (column 0), a value (column 1), and an optional action
column (column 2) that only the 4 attribute rows populate (a small `+1` `ButtonWidget`) — every
other row leaves column 2 empty.

**`Base Damage` and `Base Magic Damage` are named `Base Attack` and `Base Magic Attack`**
(same concept, same hexes) to read correctly now that Physical/Magic Defence exist as their
opposites on the same screen ("Attack" vs "Defence" reads as a pair; "Damage" vs "Defence"
doesn't).

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
The `Unspent Points` counter sits once, up front, right after Mana and before the first
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
hue-coded meaning — this rule is unchanged and extends to every row, including the attribute
and Unspent Points rows.

**Core design system: one hex per attribute family, reused identically across the attribute's
own value and every derived stat it drives.** This is what makes the interleaved grouping above
actually legible — the causal link between "Strength" and "Base Attack" and "Physical Defence"
is visually a single repeated hue running down 3 consecutive rows, not something you have to
read the labels to piece together.

| Row | Label hex | Value hex | Notes |
|---|---|---|---|
| Life | `#9C9186` | `#E2574B` | Reuses the Life bar's top-band ember/coral exactly. |
| Mana | `#9C9186` | `#5E9BE0` | Reuses the Mana bar's top-band azure exactly. Mana isn't driven by any of the 4 attributes, so it keeps its own standalone identity rather than joining a family. |
| Unspent Points | `#9C9186` | **Dynamic**: `#F2C94C` (warm gold) when N > 0, `#6B6459` (dim muted stone) when N = 0 | The only row whose value color changes at runtime. Gold when actionable draws the eye to "you have something to do here"; dimming to near-invisible at 0 lets the row recede once there's nothing to act on. |
| Endurance | `#9C9186` | `#E2574B` | **Reused, not new.** Endurance drives Life and Life Regen, so it takes Life's exact ember/coral — the attribute row visually announces "I am the source of the red family below/above me." |
| Life Regen | `#9C9186` | `#E2574B` | Same family as Endurance/Life. |
| Intelligence | `#9C9186` | `#9B5FE0` | **Reused, not new.** Intelligence drives Base Magic Attack and Magic Defence, so it takes the existing arcane violet. |
| Base Magic Attack | `#9C9186` | `#9B5FE0` | Hex unchanged from the original "Base Magic Damage" naming. |
| Magic Defence | `#9C9186` | `#9B5FE0` | Same violet family as Intelligence/Base Magic Attack. |
| Strength | `#9C9186` | `#D98A3D` | **Reused, not new.** Strength drives Base Attack and Physical Defence, so it takes the existing bronze-amber. |
| Base Attack | `#9C9186` | `#D98A3D` | Hex unchanged from the original "Base Damage" naming. |
| Physical Defence | `#9C9186` | `#D98A3D` | Same amber family as Strength/Base Attack. |
| Dexterity | `#9C9186` | `#4CBB7A` | **New** — jade/verdant green. Dexterity drives 3 stats (Attack Speed, Cast Speed, Crit Chance) that don't cleanly fit any existing family, so it gets its own new hue rather than 3 separate ones. Green is a widely-used, unclaimed genre convention for agility/speed-type stats, distinct from any specific existing game's branding. |
| Attack Speed Multiplier | `#9C9186` | `#4CBB7A` | Same green family as Dexterity. |
| Cast Speed Multiplier | `#9C9186` | `#4CBB7A` | Same green family as Dexterity. |
| Crit Chance | `#9C9186` | `#4CBB7A` | Same green family as Dexterity. |

Every hex introduced by this system (`#4CBB7A` jade-green, `#F2C94C`/`#6B6459` for Unspent
Points, `#E2574B`, `#5E9BE0`, `#D98A3D`, `#9B5FE0`) is original and was not copy-pasted from a
known game's stat-screen palette.

### Attribute investment: the `+1` buttons

- Use a plain vanilla `ButtonWidget`, message text `+1`, sized small and square (~20×20 —
  vanilla's 150×20 default is not appropriate here). Do **not** design a custom button
  texture/9-slice for this — vanilla's built-in button sprite (the same beveled light-grey
  chrome used by every other button in the game, including this same screen's tab navigation)
  already handles default/hovered/disabled states correctly, and reusing it keeps this one
  small control consistent with the rest of Minecraft's own UI chrome (`CLAUDE.md`: "do not
  create large systems before a minimal version works").
- **Enabled** (Unspent Points > 0): standard vanilla active button — default light-grey chrome,
  brightens/highlights on hover, plays the vanilla click sound.
- **Disabled** (Unspent Points = 0): set `button.active = false` and let vanilla's built-in
  disabled rendering handle it (desaturated/darkened chrome, dimmed text, no hover highlight,
  not clickable). No custom color override needed.
- Do **not** color the `+1` button's own text/background to match its row's family hue — the
  family color lives in the value column; the button stays neutral vanilla chrome.

### Layout guidance

- **Grouping via spacing, not a divider widget**: keep it to the `GridWidget`'s built-in row
  spacing rather than adding a separate divider/line widget:
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
  revisit this then.
- **Alignment**: label column left-aligned, value column left-aligned immediately after it (not
  right-aligned/tabular-number-style) — matches vanilla's own options-list and statistics
  screens. Button column follows immediately after the value, also left-aligned.
- Content is wrapped in vanilla's own `ScrollableLayoutWidget` (automatic scrollbar/wheel/drag)
  with an opaque panel background, fixing an earlier bug where bottom rows became unreachable
  at high GUI Scale once the row count grew to 15.

### Naming/asset conventions established here

- No new texture/model/icon files — this screen is entirely vanilla widget composition
  (`GridWidget` + `TextWidget` + `ButtonWidget`) plus text color, no PNGs. The `+1` buttons use
  vanilla's stock button sprite, not a custom one.
- `Base Damage` / `Base Magic Damage` are retired names — use `Base Attack` / `Base Magic
  Attack` in all code, UI text, and future references; the amber/violet hexes carry over
  unchanged.
- Family hex-reuse is the established rule for any *future* attribute-driven stat too: if a 5th
  attribute or a new derived stat is ever added under an existing attribute, it inherits that
  attribute's existing hex rather than getting a new one.
- If a second tab is added later (e.g. "Skills", or a "Class" tab — see the note below), give
  it its own subsection here documenting any tab-specific palette rather than assuming it
  inherits this tab's hexes — the reds/blues/ambers/violets/greens/gold above are this Stats
  tab's identity specifically, not a general palette free for reuse elsewhere.

### Open items (expected, not blockers)

- **Gold vs. amber legibility**: confirm `#F2C94C` (Unspent Points, actionable) and `#D98A3D`
  (Strength family) read as clearly distinct colors side-by-side in-game.
- **Disabled-button tooltip**: whether a tooltip on a disabled (`active = false`) `ButtonWidget`
  renders out of the box or needs manual `Screen.renderTooltip` wiring.

**Follow-up structural question (flagged at merge time, not resolved here)**: this screen and
`ClassScreen` (Section 8) are both "per-player identity/build" screens on separate keybinds
('C' vs 'K') using two different chrome systems (vanilla tab/scroll widgets here vs. a
hand-drawn panel there). This screen's own design already anticipated growing more tabs
("Skills, Class, etc."). Whether `ClassScreen`'s content should eventually become a tab here,
or whether keeping them as two permanently-separate screens is the right call, is a product
decision for the contributors to make once — not inferable from the code alone, and not
blocking for either screen to keep working as-is in the meantime.

---

## Changelog

- **2026-07-04** — Merged two independently-written versions of this document (one from each
  contributor's branch) into one. No content was removed; Sections 1-10 are the original
  Class-System-side framework, Sections 11-12 are the Vitals/Character-Stats-side specs
  folded in as-is. Corrected one now-stale claim (the Vitals side's "art direction not yet
  formally defined" no longer applies once Section 1 exists) and added an explicit
  reconciliation note (Section 1) flagging that the two sides used independently-invented,
  not-yet-unified color palettes — a deliberate open follow-up, not an oversight.
- **2026-07-04** — Expanded "Character Stats Screen" for the new attribute system: 4 core
  attributes (Endurance/Intelligence/Strength/Dexterity) with `+1` investment buttons, an
  Unspent Points counter, and 8 derived stats — 15 rows total, still one tab. Renamed `Base
  Damage`/`Base Magic Damage` to `Base Attack`/`Base Magic Attack` (hexes unchanged). Adopted
  a "one hex per attribute family, reused by every derived stat it drives" color system.
  Content wrapped in vanilla's `ScrollableLayoutWidget` to fix a high-GUI-Scale scrolling bug.
- **2026-07-04** — Added "Character Stats Screen" section — row order/format and label/value
  color spec for the 'C'-key full-screen stats tab (Life, Mana, Base Damage, Base Magic
  Damage). Reuses the HUD's exact Life/Mana hexes for those two rows; introduces a new amber
  (`#D98A3D`) / violet (`#9B5FE0`) pair for Base Damage / Base Magic Damage.
- **2026-07-04** — Initial creation of the Vitals HUD half of this document. Defined the Life
  bar / Mana bar HUD color palette, layout, and stacking spec.
- **2026-07-04** — Initial creation of the Class-System half of this document. Established
  base UI chrome palette, rarity-tier palette, and the 4 playable classes' accent colors/icon
  motifs; wrote the HUD overlay ("Player Status Overlay") and Class Screen ("Klassenübersicht")
  layout specs; generated 4 placeholder class icon PNGs. Context: first custom HUD/GUI work
  for the mod, after explicitly rejecting a "Metin2 look" request in favor of generic MMORPG UI
  conventions in an original palette (see Section 0).
