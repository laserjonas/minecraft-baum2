# Baum2 Visual Style Guide

Persistent visual-identity reference for Baum2. Treat this the same way
`docs/fabric-modding.md` is treated for Fabric API findings: **append and correct in place,
don't let decisions evaporate.** Whoever adds a new item, class, faction, rarity tier, or UI
screen should check here first, and add to it once new visual identity is established.

Owner/maintainer role: the `graphics-designer` subagent (see `CLAUDE.md` -> "Project Agents").
This file did not exist before 2026-07-04; it was created from scratch alongside the first
custom HUD/GUI work (see "Changelog" at the bottom).

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
  tooltips, etc.) should reuse rather than reinvent.
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
`docs/fabric-modding.md` "Rendering / HUD").

**Important existing-code note:** `src/client/java/de/baum2dev/baum2/ui/ProgressionHud.java`
already exists but (a) is never actually registered/called from `Baum2Client` today (dead
code - confirmed via search, `registerHud()` has no caller), and (b) hardcodes a fake static
50%-filled green bar that duplicates vanilla's own XP bar look, which is exactly the
redundant pattern this new spec is meant to avoid. `docs/fabric-modding.md` also already
records that an earlier attempt at this file hit unreliable `DrawContext` text-rendering and
the project fell back to driving vanilla's bar instead - **whoever implements this spec should
verify the correct 1.21.11 `DrawContext` text-drawing method before starting** (this is a
Fabric-API question, not a design one - use `fabric-docs-researcher` per `CLAUDE.md`, and
update `docs/fabric-modding.md`'s "Rendering / HUD" section with the confirmed API once
found). This spec's recommendation is to replace `ProgressionHud`'s contents with the layout
below (same file/class is fine, or rename to something like `PlayerStatusHud` - implementer's
call) rather than adding a second, competing HUD element.

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
  kept in sync by `ClientNetworkingHandler` - see `docs/fabric-modding.md`). Class name/icon:
  needs the selected class to reach the client - today `ClassManager`/`SELECTED_CLASS` is
  server-side-only (an `AttachmentType`); this HUD will need either a new small C2S/S2C sync
  packet (mirroring `ExperienceSyncPayload`'s existing pattern) or an attachment sync
  mechanism, whichever `fabric-docs-researcher` confirms is the current idiomatic approach -
  this is a networking implementation detail, not a visual one, but flagging it here since the
  HUD literally cannot show class data without it.

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
  the implementer wants an explicit mouse-clickable close affordance too.

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
      (`TextRenderer.trimToWidth` or equivalent - implementer to confirm exact 1.21.11 method
      name against `docs/fabric-modding.md` / `fabric-docs-researcher` if unclear).
    - Line 3 (`card_y + 28`): passive bonus, formatted as `"+" + <human-readable value>` (e.g.
      `"+4 Leben"`, `"+10% Lauftempo"`, `"+1 Glück"`, `"+10% Rückstoßresistenz"` - deriving the
      unit/label text from `ClassDefinition.bonusAttribute()`/`bonusOperation()` is an
      implementation detail, not specified further here), always in Rune Cyan (`#7FD8E0`) per
      the universal "+value = cyan" convention (Section 5) regardless of class.

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

- Each full card is clickable (hit-test the card's bounds, or wrap it in a full-card-sized
  invisible `ButtonWidget` - implementer's choice) and triggers class selection, mirroring
  `/baum2 class select <class>` - this needs a C2S packet (none exists yet for this purpose;
  `ClassManager.selectClass` today only runs server-side from the command handler). Follow
  the existing `ExperienceSyncPayload` pattern in `networking/` for the new payload type.
- **Hover (unselected cards only):** lighten the card border to Verdigris Glow at ~50% alpha
  as a hover affordance (Minecraft GUIs don't change cursor shape, so this is the primary
  "this is clickable" signal, consistent with vanilla button hover treatment).
- Clicking the already-selected card is a harmless no-op / redundant reselect - no special
  guard needed, since `ClassManager.selectClass` already safely handles reselecting the same
  class (remove-then-reapply).
- Not specified here (implementation/UX detail, not visual): how the screen is opened
  (keybinding vs. a `/baum2 class gui`-style command) - implementer's call, out of this
  agent's scope.

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

1. Verify the current 1.21.11 `DrawContext` text-drawing method before writing HUD code -
   `docs/fabric-modding.md` already flags that an earlier attempt hit reliability problems
   here (see Section 7's note). Run `fabric-docs-researcher` first.
2. `ProgressionHud.java` is currently dead code with a hardcoded fake 50% bar - replace its
   contents per Section 7 rather than adding a second competing HUD registration. Confirm the
   `de.baum2dev.baum2.client.Baum2Client` (empty, unused `ClientModInitializer`) vs.
   `de.baum2dev.baum2.Baum2Client` (the one actually wired up in `fabric.mod.json`, and where
   `HudRenderCallback`/screen-open registration should actually go) duplication while in
   there - out of this agent's scope to fix (not a visual concern), but worth flagging since
   it directly affects where new HUD registration code should live.
3. Class data needs to reach the client for Section 7's HUD (currently server-side-only via
   `ClassManager`'s `AttachmentType`) and class-switching needs a new C2S packet for Section
   8's screen (currently only reachable via the server command handler) - both are networking
   implementation work, follow the existing `ExperienceSyncPayload` pattern.
4. Exact human-readable formatting of each class's passive bonus text (Section 8.2, Line 3) -
   e.g. mapping `EntityAttributes.MOVEMENT_SPEED` + `ADD_MULTIPLIED_BASE` + `0.10` to
   `"+10% Lauftempo"` - is left to the implementer; this guide specifies the *display color*
   and *position* only.

---

## Changelog

- **2026-07-04** - Initial creation. Established base UI chrome palette, rarity-tier palette,
  and the 4 playable classes' accent colors/icon motifs; wrote the HUD overlay ("Player Status
  Overlay") and Class Screen ("Klassenübersicht") layout specs; generated 4 placeholder class
  icon PNGs. Context: first custom HUD/GUI work for the mod, after explicitly rejecting a
  "Metin2 look" request in favor of generic MMORPG UI conventions in an original palette (see
  Section 0).
