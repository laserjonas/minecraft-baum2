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
specs, folded in as-is at merge time. **The two sides used independently-invented colors,
which was originally left as an open reconciliation question — resolved 2026-07-05 as a
deliberate two-system split, not a unification.** See Section 1.1 for the decision and the
rule for which system governs which future UI element.

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
but the mod ended up with two parallel, un-unified UI color systems rather than one. This was
left open at merge time; **see Section 1.1 immediately below for the resolution.**

### 1.1 Resolved: menu-chrome vs. combat-HUD palette relationship (2026-07-05)

**Decision: keep the two palettes formally separate as two named systems, rather than unify
them into one.** This is a firm decision, not a further deferral — restated here so it stops
being re-litigated:

- **"Deepwood & Verdigris"** (Sections 1-2, established first) governs all **UI chrome and
  identity coding**: panel backgrounds/borders on every screen and HUD element, header/body/
  muted text colors, the universal "+value = Rune Cyan" bonus-number convention, rarity-tier
  colors (Section 2.4), and class-accent colors/icon motifs (Section 3.3). Use this palette for
  anything that is *structure* or *stable identity* — the frame, not the data inside it.
- **"Vitals & Attributes"** (Sections 11-12, established independently) governs all **live
  resource bars and attribute-family stat coding**: the Life/Mana bars' banded-fill structure
  and hues (Section 11), and the Character Stats Screen's per-attribute-family color system
  (Section 12 - the amber/violet/jade/gold-per-Unspent-Points scheme). Use this palette for
  anything that is a *live numeric gameplay value* the player is meant to read at a glance and
  mentally group by source stat.
- **Rule for future UI elements**: ask "is this frame/structure/stable-identity, or is it a
  live resource/stat value the player reads moment-to-moment?" Chrome, borders, headers, rarity,
  and class identity -> Deepwood & Verdigris. Any new resource bar (e.g. a future stamina bar or
  a boss health bar) -> extend Section 11's exact banded-bar structure with its own new hue pair,
  the same way Life and Mana already share one structure with two hues. Any new derived combat
  stat added to the Character Stats Screen -> join an existing attribute family's hex (Section
  12) if it's driven by that attribute, or take a new hex only if it doesn't fit any existing
  family (mirrors how Dexterity's stats got a new jade-green rather than 3 separate hues).
- **Rationale**: a neutral/cool chrome frame paired with vivid, higher-saturation functional
  data colors *inside* that frame is itself a normal, deliberate UI pattern (structure recedes,
  data pops) — not an inconsistency needing to be sanded away. Sections 11-12 are extensively
  implemented, tested, and user-confirmed working, with real design intent behind their specific
  hexes (e.g. Section 12's "one hex per attribute family" system, which teaches which attribute
  drives which derived stat purely through color proximity). Reskinning that onto Deepwood &
  Verdigris would be a large, purely-cosmetic rework of already-shipped, approved UI for a
  coherence gain that a neutral-frame/vivid-data split already delivers without one. If a future
  session still wants a full reskin, that's a legitimate follow-up choice to make deliberately —
  but it is a separate, larger task (touching `VitalsHud.java` and `CharacterStatsScreen.java`
  hex constants), not something this pass executes; no Java files or hex constants were changed
  as part of recording this decision.

### 1.2 Resolved: palette-reuse rule for hostile mobs/bosses and their item drops (2026-07-05)

Separate from 1.1: by the time this was written, four hostile mini-bosses existed (Stone of
Spiders, Stone of Zombies, Spider Queen, Zombie Colossus — Sections 13/15/17/18), and each one
(plus, in Spider Queen's case, her own armor drop) had been given its own bespoke palette name
("Fused Stone/Cocoon Husk/Spun Silk/Larval Glow", "Toxic Bloom", "Mutant Ichor" + "Royal
Carapace", "Ashen Brute") with no written rule for when a *new* palette is warranted versus
reusing an existing one. `HANDOFF.md` had flagged this as a question that "keeps deferring."

**Decision: ratify the pattern already being followed in practice, rather than invent a
different one, and extend it with explicit forward guidance for mob types that haven't been
built yet.**

- **Every boss-tier mini-boss gets its own original, bespoke palette family — even when it
  reuses another boss's model/geometry.** This is already exactly what happened: Stone of
  Zombies shares Stone of Spiders' 7-cuboid model verbatim (Section 15.1) but still got a wholly
  new "Toxic Bloom" palette rather than a recolor-in-name-only. This is deliberate, not
  palette-economy-by-accident: bosses are memorable, low-frequency, set-piece encounters, where
  a player immediately recognizing "this is a different boss" matters more than reusing hexes
  would save. **Keep doing this** for every future boss-tier mob.
- **Before finalizing a new boss palette, check it against every existing palette already in
  this document** (hue family, saturation, brightness) and write a compliance note explaining
  the distinction — exactly the pattern Sections 15.2, 17.3, and 18.3 already each do (e.g.
  18.3 explicitly checks its browns/reds against both existing greens). This is what actually
  prevents "keeps inventing palettes" from silently drifting into "several near-duplicate
  palettes nobody checked against each other" — the rule was never "never reuse," it was
  "never reuse *without checking*," which just wasn't written down as a rule until now.
  Continue this discipline for every future boss palette.
- **A boss's own item drops (weapons/armor) are not required to reuse that boss's own palette
  — decide per item, subject to the same distinctness check.** Precedent already goes both
  ways on purpose: Poison Dagger's blade reuses Stone of Zombies' own Toxic Bloom green family
  (Section 16), while Spider Queen's "Royal Carapace" armor was deliberately kept on its own
  violet/gold palette distinct from her body's "Mutant Ichor" green per explicit user direction
  (Section 17, "Deliberate two-palette split"). Both are valid outcomes of the same underlying
  choice: whatever best fits that specific item's own concept, not an automatic inheritance
  rule from the parent mob.
- **New forward guidance for mob types not yet built (this is the actual gap, not a change to
  existing practice): common/"trash" mobs should default to the opposite pattern.** Every
  hostile mob built so far has been boss-tier (unique, named, low-spawn-count). Once ordinary,
  frequently-spawned mob variety is added, giving each one its own bespoke palette the way
  bosses get one would not scale and would dilute what makes boss palettes feel special.
  Default for future common mobs: reuse vanilla textures where a mob is a mechanical reskin of
  a vanilla type, or share a small number of established "common enemy" palettes across similar
  mobs, and reserve new bespoke palette families for boss-tier content as defined above.

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
  uses its own, separate per-attribute-family color system instead — this is by design, not an
  unresolved gap; see Section 1.1, the "Vitals & Attributes" system.)
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
      subspec/
        bollwerk.png             (16x16, sub-spec icon - see Section 9.1)
        stahlfaust.png
        schattenpirscher.png
        sturmklinge.png
        splitterrune.png
        gluecksrune.png
        wurzelwall.png
        wesensfuelle.png
    item/
      gold_sword.png           (16x16, item icon - see Section 14)
      poison_dagger.png        (16x16, item icon - see Section 16)
      colossal_warclub.png     (16x16, item icon - see Section 18.3)
    block/       (future: block textures)
    entity/
      stone_of_spiders.png     (176x176 box-UV sheet - see Section 13)
      stone_of_zombies.png     (176x176 box-UV sheet, same layout as above - see Section 15)
      spider_queen.png         (64x32, vanilla spider UV layout - see Section 17)
      zombie_colossus.png      (64x64, vanilla biped/zombie UV layout - see Section 18.2)
      equipment/
        humanoid/
          queen_spider.png     (64x32, vanilla classic armor-layer UV - see Section 17.4)
        humanoid_leggings/
          queen_spider.png     (64x32, same UV convention - see Section 17.4)
  models/
    item/
      gold_sword.json          (see Section 14)
      poison_dagger.json       (see Section 16)
      queen_spider_helmet.json, queen_spider_chestplate.json, queen_spider_leggings.json,
      queen_spider_boots.json  (see Section 17.4)
      colossal_warclub.json    (see Section 18.3)
    block/       (future)
  items/
    gold_sword.json            (1.21.11 item-model-definition entry point - see Section 14)
    poison_dagger.json         (1.21.11 item-model-definition entry point - see Section 16)
    queen_spider_helmet.json, queen_spider_chestplate.json, queen_spider_leggings.json,
    queen_spider_boots.json    (1.21.11 item-model-definition entry points - see Section 17.4)
    colossal_warclub.json      (1.21.11 item-model-definition entry point - see Section 18.3)
  equipment/
    queen_spider.json          (1.21.11 equipment-texture definition - see Section 17.4)
  blockstates/   (future)
```

Conventions:

- File/folder names: lowercase, ASCII only, matching the `PlayerClass` enum's `name()` in
  lowercase (`EISENWAECHTER` -> `eisenwaechter.png`) - keeps texture `Identifier`s derivable
  directly from the enum without a separate lookup table.
- GUI-only chrome (icons/sprites drawn directly by custom HUD/Screen Java code, not bound to
  an item/block) lives under `textures/gui/`, subdivided by concept (`gui/class/` for
  class icons, `gui/subspec/` for sub-spec icons; a future `gui/rarity/` if rarity ever gets
  icon badges instead of just text color, etc.) - mirrors vanilla's own
  `textures/gui/sprites/...` convention of grouping UI-only textures separately from in-world
  item/block textures.
- Sub-spec file/folder names: lowercase, ASCII only, matching the `ClassSubspec` enum's
  `name()` in lowercase (`BOLLWERK` -> `bollwerk.png`) - same derivation rule as the class
  icons above, so both can be looked up directly from their respective enums.
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

## 9.1 Placeholder sub-spec icons (produced 2026-07-05)

**These are explicitly temporary placeholders**, per `MASTERPROMPT.md`'s asset rule
("Kennzeichne Platzhalter klar als eigene temporäre Platzhalter") - same technical treatment as
Section 9's 4 class icons (flat-color, no anti-aliasing, generated programmatically via
Python/Pillow, not hand-drawn final art). Covers the 8 `ClassSubspec` cards added to the
Character Stats Screen's "Class" tab (`SubspecCardWidget` in `CharacterStatsScreen.java`),
which shipped with no icon at all - see `HANDOFF.md`'s "Class Overhaul v2 follow-up" section.

**Design system**: each sub-spec icon is a *visual variant of its parent class's existing
icon* (Section 3.3/Section 9), not a new motif. Every sub-spec reuses its parent class's exact
16x16 pixel mask, fill hex, and outline hex unchanged, then adds one small overlay detail (drawn
using only that same fill/outline pair - no third color introduced) that reflects the sub-spec's
own mechanical bonus and flavor text (`SubspecRegistry.java`). This directly extends Section
3.3's own stated per-class icon system rather than inventing a new convention: a player should
recognize "this belongs to my class" at a glance from the shared shape/color, and "this is the
[X] sub-spec" from the added detail.

| File | Sub-spec | Parent class | Shape/motif | Fill | Outline |
|---|---|---|---|---|---|
| `assets/baum2/textures/gui/subspec/bollwerk.png` | Bollwerk | Eisenwächter | Pentagon shield + seam line, **plus a second horizontal plate seam** splitting the body into two reinforced bands (extra armor plating, for the Armor+2 defensive spec) | `#8FA3B3` | `#4A5A66` |
| `assets/baum2/textures/gui/subspec/stahlfaust.png` | Stahlfaust | Eisenwächter | Pentagon shield + seam line, **plus a diagonal impact-crack slash** across the shield face (a struck/dented blow mark, for the Attack Damage+1.5 offensive spec) | `#8FA3B3` | `#4A5A66` |
| `assets/baum2/textures/gui/subspec/schattenpirscher.png` | Schattenpirscher | Schattenläufer | Double-chevron, **plus the front chevron's vertex extended one pixel further forward** into a needle point (a single decisive blade-tip strike from hiding, for the Attack Damage+1.5 ambush spec) | `#7C5CA0` | `#3E2E52` |
| `assets/baum2/textures/gui/subspec/sturmklinge.png` | Sturmklinge | Schattenläufer | Double-chevron, **plus 3 staggered trailing dashes** behind the shape (motion/speed-lines from a continuous flurry, for the Attack Speed+10% tempo spec) | `#7C5CA0` | `#3E2E52` |
| `assets/baum2/textures/gui/subspec/splitterrune.png` | Splitterrune | Runenwirker | Diamond + rune-cross, **plus 4 small shard/crack ticks** radiating from the cross's diagonal quadrants (the rune shattering outward with force, for the Attack Damage+1.5 spec) | `#66C4C2` | `#2E5C5A` |
| `assets/baum2/textures/gui/subspec/gluecksrune.png` | Glücksrune | Runenwirker | Diamond + rune-cross, **plus a small detached sparkle/glint mark** beside the rune (a shimmer of fortune, for the Luck+1 spec) | `#66C4C2` | `#2E5C5A` |
| `assets/baum2/textures/gui/subspec/wurzelwall.png` | Wurzelwall | Wesenswahrer | Leaf/teardrop + center vein, **plus small root ticks flanking and widening the base** across 3 rows (roots spreading into the ground for a firm stance, for the Knockback Resistance+10% spec) | `#7FA65C` | `#3F5A2E` |
| `assets/baum2/textures/gui/subspec/wesensfuelle.png` | Wesensfülle | Wesenswahrer | Leaf/teardrop + center vein, **plus a tiny budding leaflet** branching off the main vein near the top (new growth, for the Max Health+4 spec) | `#7FA65C` | `#3F5A2E` |

All eight: 16x16, RGBA with transparent background, no anti-aliasing (flat pixel-art edges),
exactly 3 colors per file (transparent/fill/outline, verified programmatically) - generated by
a one-off script (not checked into the repo - reproducible from the specs in this table plus
the base class icon each row derives from, if regeneration is ever needed). No traced,
extracted, or downloaded source material was used - every overlay is original basic geometry
(a line, a diagonal, a handful of single-pixel ticks) laid over the existing class icon's own
already-original mask.

*Compliance note:* the same 4 class hues/motifs already cleared in Section 3.3's compliance
caveat are reused unchanged here (no new colors), and the 8 overlay details are generic,
abstract pixel-art marks (a seam line, a crack, a chevron tip, dashes, ticks, a sparkle, root
flares, a bud) - not iconography borrowed from any specific existing game's spec/talent-tree
icon set.

---

## 10. Open items for the implementer

Non-visual details this spec deliberately leaves open (implementation choices, not design
choices) - listed here so they aren't lost:

1. Exact human-readable formatting of each class's passive bonus text (Section 8.2, Line 3) -
   e.g. mapping `EntityAttributes.MOVEMENT_SPEED` + `ADD_MULTIPLIED_BASE` + `0.10` to
   `"+10% Lauftempo"` - was resolved ad hoc in `ClassScreen`'s implementation; if a 5th class
   or a new attribute type is added later, extend that mapping rather than inventing a new one.
2. **Real (non-placeholder) art for the 4 class icons and 8 sub-spec icons** is still
   outstanding — see Section 9 and Section 9.1.
3. ~~Unify or deliberately keep-separate the Section 1 vs. Section 11/12 color palettes~~ —
   **RESOLVED 2026-07-05: kept formally separate.** See Section 1.1 for the decision, the
   governing rule for future UI elements, and the rationale.
4. **Whether `ClassScreen` (Section 8) should become a tab inside the Character Stats Screen
   (Section 12)** rather than a fully separate screen — see the note at the end of Section 12.
   A product decision, not inferable from the code alone.
5. ~~No stated rule for when a new mini-boss/item palette is warranted vs. reusing an existing
   one~~ — **RESOLVED 2026-07-05.** See Section 1.2 for the rule (bosses always get their own
   new, cross-checked palette; drops decide per-item; future common mobs default to reuse).

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

## 13. Monster visual identity: "Stone of Spiders" (`baum2:stone_of_spiders`)

The mod's first custom hostile mob — a stationary, level-10 mini-boss: an immobile cocoon/
egg-sac fused into the ground, roughly 3x3x3 blocks (`ModEntities.STONE_OF_SPIDERS` is
registered `.dimensions(3.0F, 3.0F)`), that spawns a wave of 3 spiders per 10%-of-max-health
lost and drops a Gold Sword (Section 14) on death. Full mechanical detail lives in
`entity/StoneOfSpidersEntity.java`; this section covers the visual identity only.

### 13.1 Why this shape

Deliberately **not** a standard biped/quadruped mob silhouette — no legs, arms, or face. The
brief calls for it to read as an ominous "monster nest" object fused with rock, not a creature.
This is also the mod's first monster, so the approach here (organic-mass-as-geometric-volumes,
described below) sets precedent for future stationary/boss-type mobs — reuse this method
(a few overlapping simple cuboids at deliberately asymmetric offsets, rather than a symmetrical
"correct" creature skeleton) rather than reinventing per-mob.

*Compliance note:* "giant stationary egg-sac boss that spawns adds" is a generic, widely-used
monster archetype (bosses with summon mechanics exist across countless unrelated games) —
genre convention, not any specific game's IP. The shape (fused organic-mass-and-rock, cracked/
webbed shell, sickly glow-veins) and the exact palette below are original and were not modeled
on any specific existing creature's actual design.

### 13.2 Cuboid breakdown (for `ModelPartBuilder.cuboid(...)` translation)

Seven cuboids total: three main volumes (base, body, cap) plus four small accent cuboids
(2 web-strand/crack accents, 2 glow-vein bumps). All sizes in model units (16 units = 1 block,
matching the registered 3x3x3 bounding box). Positions below are described in "blocks from the
entity's horizontal center / from the ground up" for conceptual clarity — translating that into
whatever pivot/origin convention the actual `ModelPartBuilder` calls end up using (Minecraft's
own model-part Y axis conventionally increases *downward* from a part's pivot) is an
implementation detail for whoever writes `StoneOfSpidersEntityModel`, not fixed by this spec.

| # | Part | Size (blocks, X x Y x Z) | Size (px, sx x sy x sz) | Position | Role |
|---|---|---|---|---|---|
| A | Fused Rock Base | 2.75 x 0.75 x 2.75 | 44 x 12 x 44 | Centered at (0,0,0), flush on the ground, nearly filling the full 3x3 footprint (slightly inset from the edge) | Hardened stone the sac has fused into — the "monster nest" reads as grown out of the ground, not placed on it |
| B | Egg-Sac Body | 2.125 x 1.5 x 2.125 | 34 x 24 x 34 | Centered horizontally, sunk ~2px into the base (top of base to ~y=2.1 blocks) so the join reads fused, not stacked | The dominant visual mass — most of the mob's silhouette and hit-detection volume |
| C | Upper Lump | 1.25 x 1.0 x 1.25 | 20 x 16 x 20 | Offset **off-center** — shifted toward one side (e.g. +0.5 block X, +0.3 block Z from the body's center), sitting on top of the body around y=1.9-2.9 blocks, slightly embedded into the body's top the same way B embeds into A | The secondary lump that breaks perfect symmetry — this asymmetric offset is what reads as "lumpy organic mass," not a clean 3-tier snowman stack. Total height stays within the 3-block bounding box. |
| D | Web-Strand/Crack Accent 1 | 1.625 x 0.125 x 0.125 | 26 x 2 x 2 | Flush against the body's surface (~0.5px standoff to avoid z-fighting), rotated off-axis (e.g. ~25° around Y, ~-15° around Z) | A jagged crack/silk-strand running diagonally across the shell — not axis-aligned, that's the important part, exact degrees are the implementer's taste |
| E | Web-Strand/Crack Accent 2 | 1.25 x 0.125 x 0.125 | 20 x 2 x 2 | Same treatment as D, different position/rotation (e.g. ~-20° around Y, ~20° around X) for visual variety | Same role as D, second strand so the surface doesn't read as having only one crack |
| F | Glow-Vein Bump 1 | 0.375 x 0.1875 x 0.375 | 6 x 3 x 6 | Flush on the body's front-upper face, poking out ~1-2px | A visible glowing egg-vein bulging through the shell |
| G | Glow-Vein Bump 2 | 0.3125 x 0.1875 x 0.3125 | 5 x 3 x 5 | Flush on the cap's front face, poking out ~1-2px | Second glow-vein, on the cap rather than the body, so the glow reads across the whole silhouette rather than one spot |

### 13.3 Color palette (original, distinct from the UI chrome palette in Section 2)

| Role | Name | Hex | Notes |
|---|---|---|---|
| Rock shadow | Fused Stone Shadow | `#3A362E` | Base cuboid's bottom/back faces |
| Rock mid-tone | Fused Stone | `#5C574A` | Base cuboid's main faces |
| Rock highlight | Fused Stone Pale | `#7A7566` | Reserved for a future non-placeholder pass — not used in the current placeholder texture |
| Shell shadow | Cocoon Husk Shadow | `#6E6850` | Body/cap bottom+back faces |
| Shell mid-tone | Cocoon Husk | `#9C9478` | Body/cap side+front faces |
| Shell highlight | Cocoon Husk Pale | `#BDB495` | Body/cap top faces |
| Web/silk accent | Spun Silk | `#D8D2BE` | Web-strand accent cuboids (D, E) |
| Crack/fissure accent | Fissure Dark | `#2A251C` | Crack-line detail painted onto the shell's front faces, and the underside of the web-strand cuboids |
| Glow-vein core | Larval Glow | `#C4E064` | Bright chartreuse-lime — the glow-vein bumps' visible faces and the small glow blotches painted onto the body/cap |
| Glow-vein edge | Larval Glow Dim | `#6E8A2E` | Darker vein-edge tone, glow bump's hidden/side faces |

*Compliance note:* chosen to be clearly distinct from both the UI chrome palette (Section 1's
verdigris/rune-cyan) and the Vitals HUD reds/blues (Section 11) — no reused hues across
systems. The chartreuse glow-vein color is a genre-generic "eerie bioluminescent organic
monster" cue (seen across many unrelated games/media), not copied from any specific existing
game's exact creature-glow branding.

### 13.4 Placeholder texture (produced this pass)

**Explicitly a temporary placeholder**, per `MASTERPROMPT.md`'s asset rule — flat-color box-UV
fills generated programmatically (PowerShell + `System.Drawing`, no Python/ImageMagick
available in this environment), not hand-drawn final art. No traced, extracted, or downloaded
source material was used.

- **File:** `assets/baum2/textures/entity/stone_of_spiders.png` — matches the registered entity
  id `baum2:stone_of_spiders` (Section 6 naming convention: texture file name mirrors the
  registry id, not a sub-folder-per-mob layout, since this is a single-texture mob with no
  animation-frame variants).
- **Canvas:** 176 x 176 px, RGBA with transparent background outside the seven cuboids' UV
  regions. Size is dictated by the standard Minecraft "box UV" unwrap of the seven cuboids
  above (each cuboid needs `2*sizeX + 2*sizeZ` px wide by `sizeY + sizeZ` px tall of texture
  space) stacked in a single column — not a fixed/conventional size like 16x16 item textures,
  since entity texture sheets are sized to fit whatever cuboids the model actually uses.
- **Exact UV offsets used** (top-left corner of each cuboid's box-UV region on the sheet;
  reproduce these exactly via `.uv(u, v)` before each `.cuboid(...)` call so the placeholder
  texture lines up with the model without further adjustment):

  | Part | UV (u, v) | Box size (w x h) |
  |---|---|---|
  | A - Base | (0, 0) | 176 x 56 |
  | B - Body | (0, 56) | 136 x 58 |
  | C - Cap | (0, 114) | 80 x 36 |
  | D - Web-strand 1 | (0, 150) | 56 x 4 |
  | E - Web-strand 2 | (0, 154) | 44 x 4 |
  | F - Glow bump 1 | (0, 158) | 24 x 9 |
  | G - Glow bump 2 | (0, 167) | 20 x 8 |

- Each volume's faces are flat-filled per Section 13.3 (top=highlight, bottom=shadow, sides/
  front=mid-tone for A/B/C; web-strand cuboids filled solid Spun Silk with a Fissure Dark
  underside; glow bumps filled Larval Glow/Glow Dim). A handful of hand-placed crack-line and
  glow-blotch pixel clusters were painted directly onto the body's and cap's front-face UV
  regions so the placeholder isn't just flat rectangles — still simple pixel-art, no
  anti-aliasing, not final art.
- **Not yet done, flagged for a future art pass**: real hand-drawn surface detail (organic
  cracks, silk texture, glow gradients) across the full shell — the current texture only
  proves the UV layout and gives *something* to look at in-game, per this agent's own scope
  limits (a text-based agent doesn't fake detailed pixel art as final).

---

## 14. Weapon visual identity: "Gold Sword" (`baum2:gold_sword`)

The mod's first custom item — a straightforward reward-drop sword (Section 13's Stone of
Spiders drops it), built on vanilla's `ToolMaterial.GOLD` tier with custom attack numbers
(`ModItems.GOLD_SWORD`, `.sword(ToolMaterial.GOLD, 5.0F, -2.2F)`), plain `Item`, no custom
model class. This section also establishes this mod's first item-icon conventions, since it's
the first custom item overall.

### 14.1 Design direction

Plain, not ornate (per the brief) — a simple, clean diagonal blade silhouette, original in
color and shape, **not a recolor or clone of vanilla's actual golden sword texture**. Vanilla's
own gold sword icon was not referenced pixel-for-pixel; only the generic "diagonal tool icon
in a 16x16 canvas" convention (shared by every tool/weapon item in every version of Minecraft
and most other block-grid games) was reused, which is a technical/genre convention, not IP.

- **Silhouette:** standard diagonal orientation, blade tip at top-right, pommel at bottom-left
  — matches the read-at-a-glance convention every Minecraft tool/weapon icon uses (a technical
  necessity for a 16x16 hotbar icon, not a stylistic choice unique to any one game).
- **Parts, bottom-left to top-right:** a small 2x2 pommel block, a short diagonal grip, a
  perpendicular crossguard accent breaking the diagonal at the grip/blade junction, then a
  tapering 2px-thick blade to a 1px point.
- **Palette ties back to the mod's existing UI accent**: the pommel reuses **Aged Brass
  (`#D9B36C`)** from Section 2.2 exactly — the UI palette's existing "gold-associated" accent
  color — for cross-system cohesion between this weapon and the mod's established chrome,
  rather than inventing an unrelated gold tone.

### 14.2 Color palette

| Role | Hex | Notes |
|---|---|---|
| Pommel | `#D9B36C` | Reuses UI "Aged Brass" (Section 2.2) exactly |
| Grip | `#4A3728` | Dark brown, plain leather-style handle, deliberately unornamented |
| Crossguard | `#8A6E2E` | Muted bronze-gold, distinct from both the pommel and blade tones so the three hilt parts read separately |
| Blade fill | `#E8C25A` | Main blade body |
| Blade shadow edge | `#8A6B1E` | Thickening/shadow pixel along the blade's lower edge |
| Blade highlight | `#FFF0B0` | Single highlight pixel near the tip |

*Compliance note:* this is an original 6-color diagonal-sword treatment; it does not reproduce
vanilla's actual `golden_sword.png` pixel layout or any other specific game's exact sword icon.

### 14.3 Files produced (placeholder texture, real model/item-definition JSON)

- **Texture** (placeholder, per `MASTERPROMPT.md`'s asset rule — flat pixel art, no
  anti-aliasing, generated via PowerShell + `System.Drawing`, not hand-drawn final art):
  `assets/baum2/textures/item/gold_sword.png`, 16x16, RGBA, transparent background.
- **Model JSON** (real, not a placeholder — this is the correct, verified 1.21.11 schema, not
  guessed): `assets/baum2/models/item/gold_sword.json`:
  ```json
  {
    "parent": "minecraft:item/handheld",
    "textures": { "layer0": "baum2:item/gold_sword" }
  }
  ```
  `minecraft:item/handheld` (not `minecraft:item/generated`) is vanilla's own parent for every
  sword/tool item — confirmed by reading vanilla's actual `golden_sword.json` /
  `iron_sword.json` directly out of the decompiled client jar
  (`.gradle/loom-cache/minecraftMaven/net/minecraft/minecraft-clientOnly-...-1.21.11-...-v2.jar`,
  `assets/minecraft/models/item/golden_sword.json`) — not assumed from possibly-stale training
  data, per this agent's hard rule about not guessing schema.
- **Item-model-definition entry point** (real, not a placeholder — **this file is new in
  1.21.x and did not exist in older Minecraft versions**; confirmed by reading vanilla's own
  `assets/minecraft/items/golden_sword.json` from the same jar): `assets/baum2/items/gold_sword.json`:
  ```json
  {
    "model": {
      "type": "minecraft:model",
      "model": "baum2:item/gold_sword"
    }
  }
  ```
  **Both files are required in 1.21.11** — `assets/<ns>/items/<name>.json` is the entry point
  the item's registered id actually resolves to; it in turn points at
  `assets/<ns>/models/item/<name>.json` for the textures/parent. An item defined only under the
  old `models/item/` path (no matching `items/` entry) will fail to resolve a model at all in
  this version — this is the exact gotcha the graphics-designer brief asked to verify against
  ground truth rather than guess, and it's now the confirmed, working shape for any future
  item this mod adds.

---

## 15. Monster visual identity: "Stone of Zombies" (`baum2:stone_of_zombies`)

The mod's second stationary mini-boss, mechanically and geometrically the sibling of Section
13's Stone of Spiders: level 20, 400 HP (`StoneOfZombiesEntity.createStoneOfZombiesAttributes`),
immobile, same 3x3-block cocoon-stone silhouette. Every 10%-of-max-health lost spawns a wave of
2 zombies + 1 baby zombie (`StoneOfZombiesEntity.spawnZombieWave`); killing the stone kills every
zombie it has spawned so far. Drops a Poison Dagger (Section 16) on death. Ambient
`ParticleTypes.LARGE_SMOKE` drifts off it continuously, client-side (`tickMovement`).

### 15.1 Shared geometry — reuses Section 13.2 exactly, no new shape

**This mob does not introduce new geometry.** `HulkingCocoonStoneEntityModel` (see
`src/client/java/de/baum2dev/baum2/entity/HulkingCocoonStoneEntityModel.java`) is literally the
same Java model class shared between Stone of Spiders and Stone of Zombies — same 7 cuboids,
same sizes, same positions, same 176x176 box-UV layout at the exact same UV offsets. Section
13.2's cuboid breakdown table and its rationale ("organic-mass-as-geometric-volumes... reuse
this method rather than reinventing per-mob") apply here unchanged — refer back to it rather
than re-deriving. **Only the texture differs between the two mobs.** No Java model changes were
needed or made for this pass.

*Compliance note:* "cocoon mini-boss reskinned with a different infestation/theme and spawn
type" is itself a generic, widely-used monster-variant convention (recoloring/rethreading a
boss archetype for a second encounter) — genre convention, not IP. The toxic/zombie theme and
palette below are original.

### 15.2 Color palette: "Toxic Bloom" (original, distinct from every other palette in the mod)

| Role | Name | Hex | Notes |
|---|---|---|---|
| Rock shadow | Blight Stone Shadow | `#26301F` | Base cuboid's bottom/back faces |
| Rock mid-tone | Blight Stone | `#435930` | Base cuboid's main faces (toxic-infused rock, reskin of Section 13.3's Fused Stone) |
| Rock highlight | Blight Stone Pale | `#647A47` | Reserved for a future non-placeholder pass — not used in the current placeholder texture, mirrors Section 13.3's own unused "Pale" convention |
| Shell shadow | Plague Husk Shadow | `#4F5A2C` | Body/cap bottom+back faces |
| Shell mid-tone | Plague Husk | `#7C8F49` | Body/cap side+front faces |
| Shell highlight | Plague Husk Pale | `#A8BD70` | Body/cap top faces |
| Ooze accent | Toxic Ooze | `#B8D888` | Web-strand accent cuboids (D, E) — reskin of Section 13.3's silk-strand role as a sickly ooze/drip instead |
| Crack/fissure accent | Fissure Rot | `#17200F` | Crack-line detail on the shell's front faces, and the underside of the ooze-accent cuboids |
| Glow-vein core | Plague Glow | `#3DFF7E` | Bright toxic emerald-green — the glow-vein bumps' visible faces and the small glow blotches painted onto the body/cap |
| Glow-vein edge | Plague Glow Dim | `#1B8A45` | Darker vein-edge tone, glow bump's hidden/side faces |

*Compliance note:* chosen to be clearly distinct from Section 13.3's Fused Stone/Cocoon Husk
(brown/tan) and Larval Glow (`#C4E064`, a warm yellow-lime hue) — Plague Glow is a cooler,
more saturated emerald-green (hue shifted well away from Larval Glow's yellow-green, and much
brighter/more saturated than the muted UI Verdigris Glow `#5FA98C` or the Character-Stats-
Screen's jade Dexterity green `#4CBB7A`, per Section 12), so it reads as its own distinct
"toxic glow" rather than a restyled copy of any existing green in the mod. "Sickly toxic green
monster glow" itself is a genre-generic bioluminescence/poison cue (same reasoning Section
13.3 already applied to its own chartreuse), not copied from any specific existing game's
creature-glow branding.

### 15.3 Placeholder texture (produced this pass)

**Explicitly a temporary placeholder**, per `MASTERPROMPT.md`'s asset rule — flat-color box-UV
fills generated programmatically (PowerShell + `System.Drawing`, same technique as Section
13.4, no Python/ImageMagick available in this environment), not hand-drawn final art. No
traced, extracted, or downloaded source material was used.

- **File:** `assets/baum2/textures/entity/stone_of_zombies.png` (under
  `src/main/resources/`) — matches the registered entity id `baum2:stone_of_zombies`, mirrors
  Section 13.4's naming convention.
- **Canvas:** 176 x 176 px, RGBA, transparent background outside the seven cuboids' UV
  regions — **identical canvas size to `stone_of_spiders.png`**, since the geometry is
  identical (Section 15.1).
- **UV offsets used — identical to Section 13.4's table, reproduced verbatim, not
  redesigned:**

  | Part | UV (u, v) | Box size (w x h) |
  |---|---|---|
  | A - Base | (0, 0) | 176 x 56 |
  | B - Body | (0, 56) | 136 x 58 |
  | C - Cap | (0, 114) | 80 x 36 |
  | D - Web-strand 1 (ooze accent) | (0, 150) | 56 x 4 |
  | E - Web-strand 2 (ooze accent) | (0, 154) | 44 x 4 |
  | F - Glow bump 1 | (0, 158) | 24 x 9 |
  | G - Glow bump 2 | (0, 167) | 20 x 8 |

- Each volume's faces are flat-filled using the standard Minecraft box-UV face sub-layout
  (top=highlight, bottom+back=shadow, remaining sides+front=mid-tone for A/B/C; ooze-accent
  cuboids filled solid Toxic Ooze with a Fissure Rot underside; glow bumps filled Plague
  Glow/Plague Glow Dim on visible/hidden faces respectively) — same method as Section 13.4. A
  handful of hand-placed crack-line and glow-blotch pixel clusters were painted directly onto
  the body's and cap's front-face UV regions, same as Section 13.4's approach.
- **Not yet done, flagged for a future art pass** (same caveat as Section 13.4): real
  hand-drawn surface detail across the full shell — this texture only proves the palette/theme
  swap and gives something to look at in-game.
- **No Java/model changes required** — confirmed the UV table above is pixel-identical to
  Section 13.4's, so `HulkingCocoonStoneEntityModel`'s existing `.uv(...)` calls line up with
  this new texture exactly as they do with `stone_of_spiders.png`.

---

## 16. Weapon visual identity: "Poison Dagger" (`baum2:poison_dagger`)

The mod's second custom item — a reward-drop dagger (Section 15's Stone of Zombies drops it),
built on `ToolMaterial.IRON` with custom low-damage/high-speed args
(`ModItems.POISON_DAGGER`, `.sword(ToolMaterial.IRON, 1.0F, 0.0F)`) — a "dagger" archetype:
fast, weak per-hit, compensated by an on-hit Poison status effect wired in
`combat/PoisonDaggerHandler.java`. Plain `Item`, no custom model class, following Section 14's
precedent exactly.

### 16.1 Design direction

Small, fast, venomous blade — green-tinged metal with a poison sheen/drip accent, **not a
recolor or clone of vanilla's iron sword texture, and not a resize of this mod's own Gold
Sword (Section 14)**. Only the generic "diagonal tool icon in a 16x16 canvas" convention is
reused (same genre-convention reasoning as Section 14.1), but the silhouette is deliberately
**shorter and stubbier** than Gold Sword's full-canvas diagonal, to read as a distinct "dagger"
archetype rather than a small sword: the blade runs from the bottom-left pommel only to roughly
the canvas's center-upper area, leaving the top-right quadrant of the canvas empty/transparent
(Gold Sword's blade tip reaches the top-right corner; this one does not).

- **Silhouette:** same bottom-left-to-top-right diagonal orientation as every tool/weapon icon
  in the mod (Section 14.1's read-at-a-glance convention), but roughly two-thirds the length —
  pommel at (1,13)-(2,14), blade tip at (11,3), instead of spanning the full 16px diagonal.
- **Parts, bottom-left to top-right:** a small 2x2 pommel block, a short 2px diagonal grip, a
  perpendicular crossguard accent breaking the diagonal at the grip/blade junction, then a
  tapering blade to a 1px point — same part vocabulary as Gold Sword (Section 14.1) for
  cross-item consistency, just compressed to the shorter dagger length.
- **New element not present on Gold Sword:** a single poison-sheen/drip accent pixel partway
  up the blade, signaling the on-hit Poison effect visually.
- **Palette is original, does not reuse Gold Sword's Aged Brass** — a dagger dropped by a toxic
  mob calls for its own green-tinged steel identity rather than inheriting Gold Sword's warm
  gold/bronze hilt, so this item gets a fully new palette rather than a partial one.

### 16.2 Color palette

| Role | Hex | Notes |
|---|---|---|
| Pommel | `#4A4F45` | Dark worn iron-green metal cap |
| Grip | `#2E2A22` | Near-black leather-wrap handle, plain and unornamented |
| Crossguard | `#6E7A5E` | Muted sage-metal accent, distinct from both pommel and blade tones |
| Blade fill | `#8FA894` | Pale green-tinged steel |
| Blade shadow edge | `#3F4A3C` | Thickening/shadow pixel along the blade's lower edge |
| Blade highlight | `#D8E8C8` | Highlight pixels near the tip |
| Poison sheen/drip accent | `#5FE06B` | Bright toxic-green accent pixel on the blade, signals the on-hit Poison effect |

*Compliance note:* this is an original 7-color diagonal-dagger treatment; it does not
reproduce vanilla's actual `iron_sword.png` pixel layout, Section 14's Gold Sword palette, or
any other specific game's exact dagger/knife icon.

### 16.3 Files produced (placeholder texture, real model/item-definition JSON)

- **Texture** (placeholder, per `MASTERPROMPT.md`'s asset rule — flat pixel art, no
  anti-aliasing, generated via PowerShell + `System.Drawing`, not hand-drawn final art):
  `assets/baum2/textures/item/poison_dagger.png`, 16x16, RGBA, transparent background.
- **Model JSON** (real, not a placeholder — same verified 1.21.11 schema as Section 14.3's
  Gold Sword, reused exactly): `assets/baum2/models/item/poison_dagger.json`:
  ```json
  {
    "parent": "minecraft:item/handheld",
    "textures": { "layer0": "baum2:item/poison_dagger" }
  }
  ```
- **Item-model-definition entry point** (real, not a placeholder — same 1.21.x schema
  confirmed in Section 14.3): `assets/baum2/items/poison_dagger.json`:
  ```json
  {
    "model": {
      "type": "minecraft:model",
      "model": "baum2:item/poison_dagger"
    }
  }
  ```
  Both files are required in 1.21.11, per the same gotcha Section 14.3 already documented and
  verified against the decompiled vanilla client jar — not re-verified here since Section 14.3
  already established it as ground truth for any future item this mod adds.

---

## 17. Boss visual identity: "Spider Queen" (`baum2:spider_queen`) and the "Queen Spider Set" armor

The mod's **first true mobile boss** (level 15, 350 HP) — every prior hostile mob (Sections 13,
15) was a stationary mini-boss fused to the ground. Spider Queen is a giant spider (literally
3x a normal Minecraft spider) that fights with a fast bite and a long-range leaping lunge
(`SpiderQueenEntity.java`), and drops the mod's **first genuine boss-tier armor set** — the
4-piece "Queen Spider Set" — rather than a single mini-boss trinket (Sections 14, 16). Both of
these are precedents, the same way Section 13 was the precedent for mini-boss visual identity:
future mobile bosses and future armor sets should look to this section first.

**Deliberate two-palette split (read this before "fixing" it):** as of 2026-07-05, the Spider
Queen entity's own texture (Section 17.3, "Mutant Ichor" — sickly/toxic green) and the Queen
Spider Set armor she drops (Section 17.4, "Royal Carapace" — violet/gold) intentionally use two
*different*, unrelated palettes. This is not an oversight or a half-finished reskin — it's
direct user feedback after playtesting: the boss's own look was asked to become "more green and
more like a mutant spider" with a green smoke aura, while the armor drop was explicitly confirmed
to already "look fine" and was asked to stay untouched. Do not unify these two palettes, and do
not treat the mismatch as a bug to silently correct — if a future pass wants to reconcile them,
that's a deliberate design decision to raise with the user first, not an assumed cleanup.

### 17.1 Why this shape/approach

Unlike Stone of Spiders/Stone of Zombies (custom cuboid geometry, Section 13.2), Spider Queen
**reuses vanilla's own `SpiderEntityModel` geometry unchanged**, scaled 3x via a
`ModelTransformer` at model-layer registration (confirmed against decompiled `EntityModels`/
`GiantEntityModel` — the same two-part "bigger dimensions + scaled shared model" mechanism
vanilla's own Giant uses to look 6x a Zombie). Only the *texture* is original — this section's
job is exclusively the re-theme, not new geometry.

*Compliance note:* "giant reskinned version of a normal enemy, scaled up, as a boss" is a
generic, widely-used monster-variant convention (colossal/elite versions of common enemies
appear across countless unrelated games, and vanilla Minecraft does this exact thing with
Giant/Zombie) — genre convention, not IP. The palette, markings, and armor design below are
original and were not modeled on any specific existing creature or game's actual design.

### 17.2 Entity texture: `spider_queen.png`

- **File:** `assets/baum2/textures/entity/spider_queen.png`.
- **Canvas:** **64x32 px, confirmed exactly matching vanilla's own UV layout** — verified by
  reading `SpiderEntityModel.getTexturedModelData()` directly out of the decompiled
  `minecraft-clientOnly-...-sources.jar` in `.gradle/loom-cache/minecraftMaven/` (not assumed):
  `TexturedModelData.of(modelData, 64, 32)`. The Java model/renderer
  (`SpiderQueenEntityRenderer.java`) reuses vanilla's `SpiderEntityModel` unmodified, so this
  texture **must** fill the exact same UV regions vanilla's own spider texture uses — the model
  was not redesigned, only re-themed, per the exact UV table below (derived directly from the
  same source file's `ModelPartBuilder.cuboid(...)`/`.uv(...)` calls, not guessed):

  | Part | UV origin | Cuboid size (dx,dy,dz) | Box region (x, y, w, h) |
  |---|---|---|---|
  | Legs (all 8, shared/mirrored) | (18, 0) | 16 x 2 x 2 | (18, 0, 36, 4) |
  | Thorax (`body0`) | (0, 0) | 6 x 6 x 6 | (0, 0, 24, 12) |
  | Head | (32, 4) | 8 x 8 x 8 | (32, 4, 32, 16) |
  | Abdomen (`body1`) | (0, 12) | 10 x 8 x 12 | (0, 12, 44, 20) |

### 17.3 Entity palette: "Mutant Ichor" (boss's own texture — distinct from the armor's palette; see divergence note below)

**Revised 2026-07-05 after playtesting.** The entity's own texture no longer uses "Royal
Carapace" — that name and violet/gold palette now describes only the Queen Spider Set armor
(Section 17.4, unchanged). Direct playtest feedback asked for the boss itself to read as "more
green and more like a mutant spider," with a green smoke/aura (already implemented in Java via
vanilla's `ParticleTypes.WITCH` swirl — not a texture concern). This section documents the new
entity-only palette; Section 17.4 is untouched.

| Role | Name | Hex | Notes |
|---|---|---|---|
| Carapace mid-tone | Mutant Ichor | `#4C6B5A` | Main body/head/leg front+side faces — a desaturated, sickly gray-teal green (not a clean/saturated "grass green") |
| Carapace highlight | Mutant Ichor Pale | `#7FA893` | Top faces (thorax, abdomen, head) |
| Carapace shadow | Mutant Ichor Dusk | `#263B30` | Bottom/back faces |
| Joint/crack accent | Necrotic Vein | `#241626` | Near-black plum (not pure black) — leg joints and dark venous crack lines painted onto the carapace, the main "diseased" cue |
| Vein accent, pale | Necrotic Vein Pale | `#4A2E4A` | Secondary/lighter vein-crack tone, used sparingly next to Necrotic Vein for a two-tone crack read |
| Diseased blotch | Bile Blotch | `#B8C13A` | Sickly yellow-green pustule/blotch marks — irregular, asymmetrical placement (abdomen bulge, leg mottling) is what sells the "mutant" read over a flat green reskin |
| Eye glow (painted, not vanilla overlay) | Toxic Eye | `#E8FF6B` | See eye note below |
| Eye-socket base | Void Socket | `#100C14` | Dark base under the painted eye cluster |

*Compliance note:* "sickly/mutant green monster" is a broad, unclaimed genre convention (used
across countless unrelated games and other media for corrupted/diseased/mutated creatures), not
IP tied to any one game. This specific palette is also checked distinct from every *other* green
already in this document — Section 15.2's "Toxic Bloom" (Stone of Zombies: `Blight Stone
#435930` / `Plague Husk #7C8F49` / `Toxic Ooze #B8D888` / `Plague Glow #3DFF7E`) is a warm,
yellow-olive/lime-leaning green family. Mutant Ichor is deliberately cooler and grayer (its blue
channel sits noticeably higher relative to red/green than Toxic Bloom's at every matching role —
e.g. mid-tone `#4C6B5A` vs. Blight Stone `#435930`), reading as "diseased gray-flesh" rather than
"toxic plant bloom," so the mod's two green palettes stay visually distinguishable rather than
being near-duplicates. Bile Blotch's yellow-green does sit in a similar family to Toxic Bloom's
accents (both draw on the same "toxic yellow-green highlight" genre convention) but is used only
as a sparse blotch accent, not either palette's dominant tone, so the two identities as a whole
remain distinct. Necrotic Vein's near-black-plum hue is a new addition not reused from any other
palette in this document (distinct from Royal Carapace's saturated violet family and from
Chitin Void's neutral near-black).

**On the eyes — no vanilla glow overlay anymore:** vanilla spiders normally get a glowing-eyes
overlay from `SpiderEyesFeatureRenderer`, a separate fixed vanilla texture rendered additively on
top of the shared spider head geometry. `SpiderQueenEntityRenderer` no longer attaches that
feature renderer — it's generically bound to vanilla's own `SpiderEntityModel`/render-state type
and isn't compatible with `SpiderQueenEntityModel`/`SpiderQueenRenderState`'s custom pair (needed
for the leap-attack crouch pose; see that renderer's own class javadoc), so it was dropped rather
than forced. This means Spider Queen has **no automatic eye-glow overlay at all** — the eye read
has to be painted directly into `spider_queen.png`'s head UV region instead of relying on the
vanilla overlay the way the old "Royal Carapace" version did. The current texture paints a dark
`Void Socket` patch on the head's front face with a small irregular cluster of bright `Toxic Eye`
(`#E8FF6B`) dots on top — a static painted glow (no actual light emission/glowing-in-the-dark;
Minecraft texture pixels don't emit light on their own), but bright/saturated enough against the
darker carapace to read as eyes at normal render distance.

### 17.4 Queen Spider Set armor — first boss-tier armor set

4 pieces (`baum2:queen_spider_helmet/_chestplate/_leggings/_boots`), dropped as a full-set kill
reward (`SpiderQueenEntity.dropLoot`). **Unchanged in the 2026-07-05 mutant-green revision** —
confirmed by the user to already "look fine" as-is; this armor is what "Royal Carapace" refers
to from here on (the entity's own texture moved to "Mutant Ichor," Section 17.3, above). Two
separate visual systems, per 1.21.11's equipment architecture:

**Armor palette: "Royal Carapace"** (unchanged from the original pass; kept here for reference
now that the entity texture has its own separate palette):

| Role | Name | Hex | Notes |
|---|---|---|---|
| Carapace mid-tone | Royal Carapace | `#4B2170` | Main body/head/leg side+front faces |
| Carapace highlight | Royal Carapace Pale | `#7A46A6` | Top faces |
| Carapace shadow | Royal Carapace Dusk | `#2A0F3F` | Bottom/back faces |
| Chitin void | Chitin Void | `#1C0E28` | Legs and other near-black joint/accent tones, boot-leather tone |
| Regal trim | Regal Amber | `#D9A73A` | Markings, joint glints, trim bands |
| Regal trim, dark | Regal Amber Dusk | `#8C6A1E` | Shadow-side trim accents |
| Regal trim, pale | Regal Amber Pale | `#F0C878` | Sparse gem/crest highlight pixels only |
| Eye-socket base | Void Socket | `#0D0609` | Dark base under the helmet's visor slit — unrelated to the entity texture's own separate `Void Socket` `#100C14` (Section 17.3), a near-identical near-black role-name coincidence between the two now-separate palettes, not a shared value |

*Compliance note (unchanged):* deep violet/royal-purple with gold/amber trim is a widely-used
genre convention for "regal/queen" enemy/armor coding (seen across countless unrelated games and
other media, plus real-world regalia associations) — not any specific existing game's exact
branding. Distinct from every other palette in this document, including the entity's own new
"Mutant Ichor" (Section 17.3) — that divergence is intentional, see the note at the top of
Section 17.

**Item icons** (`textures/item/queen_spider_<piece>.png`, 16x16 each) — standard flat inventory
icons following the vanilla armor-icon silhouette convention (helmet dome, chestplate torso +
pauldrons, leggings waistband + twin legs, boots twin ankle/foot shapes) in the Royal
Carapace/Regal Amber palette above, each with a thin gold trim line and a small gem/accent so
the set reads as one cohesive "boss reward" family at a glance in the inventory grid. Model/
item-definition JSON (`assets/baum2/items/queen_spider_*.json` + `models/item/queen_spider_*.json`)
already existed and were verified correct against these exact texture files — no changes needed.

**Worn-on-player textures** (the "looks beautiful when worn" layer) — `assets/baum2/equipment/
queen_spider.json` points at two texture files:
- `assets/baum2/textures/entity/equipment/humanoid/queen_spider.png` (helmet, chestplate, boots)
- `assets/baum2/textures/entity/equipment/humanoid_leggings/queen_spider.png` (leggings)

**Canvas: 64x32 px each, confirmed exactly matching vanilla's own classic armor-layer
convention** (the same UV layout as the pre-1.21.11 `armor/diamond_layer_1.png`/`_layer_2.png`
files — only the file location/lookup mechanism changed in 1.21.11, not the biped model
geometry) — verified by reading `BipedEntityModel.getModelData()` directly out of the
decompiled sources jar (not assumed), which gives this exact UV table:

| Part | UV origin | Cuboid size (dx,dy,dz) | Box region (x, y, w, h) |
|---|---|---|---|
| Head (inner) | (0, 0) | 8 x 8 x 8 | (0, 0, 32, 16) |
| Hat (outer helmet shell) | (32, 0) | 8 x 8 x 8 | (32, 0, 32, 16) |
| Body | (16, 16) | 8 x 12 x 4 | (16, 16, 24, 16) |
| Arms (shared/mirrored) | (40, 16) | 4 x 12 x 4 | (40, 16, 16, 16) |
| Legs (shared/mirrored) | (0, 16) | 4 x 12 x 4 | (0, 16, 16, 16) |

Both files were painted at this exact layout: the `humanoid` (main) layer's head/hat regions
carry the helmet's face-plate + visor slit + a small crest gem, the body/arm regions carry the
chestplate with gold edge trim and a chest gem, and the leg region uses `Chitin Void` (dark
boot-leather tone, distinct from the carapace-violet used elsewhere) with a gold sole trim so
the boots read as their own distinct piece rather than a recolored leg segment. The
`humanoid_leggings` layer's body region (hip band) and leg region (thigh plating) instead reuse
the carapace-violet + a gold belt/knee band, so the leggings read as continuing the chestplate's
plating rather than matching the boots' darker leather tone — the set is designed to look like
one continuous suit of "queen-spider chitin plating" when all 4 pieces are worn together, per
the brief's "should look beautiful" requirement, not four independently-themed pieces that
happen to share a name.

*Compliance note:* a violet-and-gold "regal insect/spider queen" armor identity is, again, a
genre-generic association (does not require or reproduce any specific existing game's exact
armor-set branding); the specific silhouette/trim/gem execution here is original.

### 17.5 Files produced this pass (all explicitly temporary placeholders)

Per `MASTERPROMPT.md`'s asset rule — flat-color pixel fills generated programmatically
(PowerShell + `System.Drawing`, same technique as every prior placeholder in this document; no
Python/ImageMagick available in this environment), not hand-drawn final art. No traced,
extracted, or downloaded source material was used anywhere in this pass.

- `assets/baum2/textures/entity/spider_queen.png` (64x32)
- `assets/baum2/textures/item/queen_spider_helmet.png` (16x16)
- `assets/baum2/textures/item/queen_spider_chestplate.png` (16x16)
- `assets/baum2/textures/item/queen_spider_leggings.png` (16x16)
- `assets/baum2/textures/item/queen_spider_boots.png` (16x16)
- `assets/baum2/textures/entity/equipment/humanoid/queen_spider.png` (64x32)
- `assets/baum2/textures/entity/equipment/humanoid_leggings/queen_spider.png` (64x32)

**Not yet done, flagged for a future art pass** (same caveat as every prior placeholder in this
document): real hand-drawn surface detail (leg segmentation shading, carapace sheen, finer
armor engraving) — this pass proves the UV layouts, establishes the palette, and gives the
mod's first real boss and first armor set something considered to look at in-game, but a human
artist pass would meaningfully raise the ceiling here given the "should look beautiful" bar.

---

## 18. Boss visual identity: "Zombie Colossus" (`baum2:zombie_colossus`) and the "Colossal Warclub"

The mod's **second mobile boss** (level 25, 750 HP) - a hulking, muscular zombie warlord, 3x the
size of a vanilla zombie (`ModEntities.ZOMBIE_COLOSSUS` is `.dimensions(1.8F, 5.85F)`, exactly
triple a vanilla zombie's `0.6F, 1.95F`), that fights with a slow, heavy 2-block-range club attack
plus a leap-and-fire-wave signature move (`ZombieColossusEntity.java`). Its guaranteed drop is the
**Colossal Warclub** (`baum2:colossal_warclub`) - originally drafted as "Colossus Club" but renamed
before any asset work referenced it, after `ip-naming-compliance-checker` found that exact string
is a real (if minor/non-iconic) existing item name in EverQuest 2; only the item's name/id changed,
the entity keeps "Zombie Colossus." This section follows Section 17's precedent (first true mobile
boss, `spider_queen`) as the second data point for that family, and the same reused-vanilla-model-
plus-`ModelTransformer.scaling` mechanism applies again below - see 18.1 for the version-specific
verification.

### 18.1 Why this shape/approach - held-item rendering, not a baked-on club

Two rendering options were on the table (a real held item vs. a permanently-baked cosmetic club
cuboid). **Real held-item rendering was used** - confirmed straightforward to get working
correctly in 1.21.11 by decompiling the actual client jar rather than guessing, because vanilla
already solves this *exact* problem for its own oversized-zombie boss:

- `GiantEntity` (vanilla's 6x-scaled zombie) is rendered by `GiantEntityRenderer`, which:
  - extends `MobEntityRenderer` **directly** (not `BipedEntityRenderer`/`ZombieBaseEntityRenderer`)
    specifically so it can pass its own scaled shadow radius (`0.5F * scale`) to the constructor -
    `ZombieBaseEntityRenderer`'s own constructor hardcodes an unscaled `0.5F` with no override
    hook, the same "vanilla renderer hardcodes a fixed shadow radius" problem
    `SpiderQueenEntityRenderer`'s javadoc already documents for `SpiderEntityRenderer`;
  - reuses vanilla's own `ZombieEntityRenderState` unchanged (no custom render-state subclass -
    Giant needs no bespoke pose, and neither does this boss);
  - reuses `GiantEntityModel`, which is just `AbstractZombieModel<ZombieEntityRenderState>` with
    **no scaling logic of its own** - the scaling instead happens once, centrally, where
    `EntityModels.getModels()` registers `EntityModelLayers.GIANT`: the exact same shared
    `TexturedModelData` used for the plain `ZOMBIE` layer gets `.transform(ModelTransformer.scaling(6.0F))`
    - this is the same "reuse the shared vanilla `TexturedModelData`, scale it once via
    `ModelTransformer` at model-layer registration" mechanism `SpiderQueenEntityModel`/
    `Baum2Client` already established for Spider Queen, now confirmed (by decompiling
    `EntityModels.getModels()` directly, not assumed) to be vanilla's own real mechanism for this
    exact archetype, not just this mod's own convention;
  - adds `HeldItemFeatureRenderer` by hand (since `ZombieBaseEntityRenderer` doesn't add one
    itself - only `GiantEntityRenderer`, for this exact "big zombie holding a big item" case);
  - calls the static helper `BipedEntityRenderer.updateBipedRenderState(entity, state, tickDelta,
    itemModelResolver)` from its own `updateRenderState` override to populate the render state's
    held-item/arm-pose fields, since it no longer extends `BipedEntityRenderer` to get that for
    free.

`ZombieColossusEntityRenderer`/`ZombieColossusEntityModel` copy this exact, vanilla-proven
mechanism verbatim for this boss (see those two classes' own javadoc for the full mapping), rather
than inventing a new one or falling back to option (b)'s baked-on cosmetic club. No `ArmorFeatureRenderer`
is attached - this boss only ever equips a mainhand weapon (`ZombieColossusEntity.initEquipment()`),
never armor, so that plumbing (which `GiantEntityRenderer` does use, for Giant's equippable armor
slots) isn't needed here.

*Compliance note:* "giant reskinned/muscular version of a normal enemy wielding an oversized
weapon" is the same generic, widely-used monster-variant convention Section 17.1 already argued
for Spider Queen (vanilla's own Giant/Zombie is this exact archetype) - genre convention, not IP.
The palette and musculature/club design below are original and not modeled on any specific
existing creature or game's actual design.

### 18.2 Entity texture: `zombie_colossus.png`

- **File:** `assets/baum2/textures/entity/zombie_colossus.png`.
- **Canvas: 64x64 px, confirmed exactly matching vanilla's own plain biped/zombie UV layout** -
  verified by reading `BipedEntityModel.getModelData(Dilation, float)` directly out of the
  decompiled client-sources jar (not assumed): `TexturedModelData.of(BipedEntityModel.getModelData(Dilation.NONE, 0.0F), 64, 64)`,
  the same call `EntityModels.getModels()` uses to build the shared `TexturedModelData` for
  vanilla's own `ZOMBIE` **and** `GIANT` model layers (see 18.1). Only the bottom half of the 64x64
  canvas is used - this base biped call defines just 5 real parts (head, hat-overlay, body, one
  shared mirrored arm region, one shared mirrored leg region), not the additional jacket/sleeves/
  pants overlay cuboids the full player-skin format also supports - so, like vanilla's own
  `zombie.png`, the region below y=32 is unused/transparent.

  | Part | UV origin | Cuboid size (dx,dy,dz) | Box region (x, y, w, h) | Notes |
  |---|---|---|---|---|
  | Head (inner) | (0, 0) | 8 x 8 x 8 | (0, 0, 32, 16) | Face painted with eyes + jaw line (below) |
  | Hat (outer head overlay) | (32, 0) | 8 x 8 x 8, dilated | (32, 0, 32, 16) | **Left fully transparent**, matching vanilla `zombie.png`'s own convention of not using this overlay |
  | Body | (16, 16) | 8 x 12 x 4 | (16, 16, 24, 16) | Bare muscled torso, not a shirt - see 18.3 |
  | Arm (shared/mirrored, both arms) | (40, 16) | 4 x 12 x 4 | (40, 16, 16, 16) | Bare muscled bicep |
  | Leg (shared/mirrored, both legs) | (0, 16) | 4 x 12 x 4 | (0, 16, 16, 16) | Tattered cloth wrap, distinct tone from the bare torso/arms |

  Both the arm and leg regions are genuinely shared between left/right (vanilla mirrors the same
  UV rectangle for both sides via `.mirrored()`, confirmed in the same decompiled source) - painting
  one side's region paints both limbs automatically.

### 18.3 Color palette: "Ashen Brute" (original, distinct from every other palette in this document)

Deliberately **not another green** - every prior hostile-mob palette in this document (Toxic
Bloom, Mutant Ichor) is some shade of sickly/toxic green; this boss instead reads as long-dead,
ashen, sun-bleached flesh with raw exposed red-brown musculature, so it's immediately
distinguishable from the mod's other zombie/spider mobs at a glance rather than being a third green
recolor.

| Role | Name | Hex | Notes |
|---|---|---|---|
| Skin shadow | Ashen Hide Shadow | `#332C22` | Head bottom face |
| Skin mid-tone | Ashen Hide | `#5C5142` | Head side/back faces |
| Skin highlight | Ashen Hide Pale | `#7D715C` | Head top face |
| Muscle base | Exposed Muscle | `#7A2E24` | Torso/arm side/back/front base fill - the dominant "visible muscles" color per the brief |
| Muscle highlight (wet sheen) | Exposed Muscle Sheen | `#B24A3A` | Torso top face, bicep bulge highlight, ab/pec striation highlights |
| Muscle shadow | Exposed Muscle Dusk | `#4A1712` | Torso bottom face, bicep lower-shadow, ab striation shadows |
| Wound/seam accent | Wound Edge | `#1F1A16` | Near-black - sternum groove, side striation lines, jaw line, ragged hem |
| Eye glow (painted, no vanilla overlay involved) | Brute Glare | `#D9C24A` | Dull amber-yellow eye dots on the head's front face |
| Eye-socket base | Deep Socket | `#140F0A` | Dark base under each eye dot |
| Cloth-wrap mid-tone | Tattered Wrap | `#2E251C` | Leg region side/front/back faces - ragged trouser remnants, distinct tone from the bare torso/arms |
| Cloth-wrap highlight | Tattered Wrap Fold | `#4A3C2C` | Leg top face + fold-line accents |
| Cloth-wrap shadow | Tattered Wrap Dusk | `#180F09` | Leg bottom face |
| Bone/tusk accent | Bone | `#D8CFC0` | Two small tooth/tusk pixels at the jaw corners |

*Compliance note:* an ashen/gray-brown "long-dead brute" palette with raw red-brown exposed
muscle is a broad, unclaimed genre convention (undead/berserker "muscle-bound brute" enemies with
visible musculature appear across countless unrelated games and other media), not IP tied to any
one game. Checked distinct from every existing palette in this document: it shares no hue family
with Section 13.3's warm tan/lime (Fused Stone/Larval Glow), Section 15.2's yellow-olive toxic
green (Toxic Bloom), or Section 17.3's cool gray-teal green (Mutant Ichor) - Ashen Brute is
brown/red-based, not green at all, which is the deliberate distinguishing choice. `Brute Glare`'s
dull amber-yellow eye glow is also checked distinct from Section 17.3's brighter chartreuse `Toxic
Eye` (`#E8FF6B`) and Section 13.3's `Larval Glow` (`#C4E064`) - all three sit in a loose
yellow-green family (a common "monster eye glow" genre convention), but Brute Glare is the dullest/
most desaturated of the three and is a minor accent here, not the palette's dominant tone.

### 18.4 Weapon visual identity: "Colossal Warclub" (`baum2:colossal_warclub`)

Follows Section 14/16's exact item conventions (plain `Item`, no custom model class, same
`minecraft:item/handheld` parent + `assets/baum2/items/<name>.json` entry-point pair, both files
required per the gotcha Section 14.3 already verified against the decompiled vanilla client jar).

- **Silhouette:** a deliberate departure from Gold Sword/Poison Dagger's tapering-blade shape,
  since a club needs to read as blunt/heavy rather than sharp: a thin 2px diagonal handle
  (bottom-left, matching the established bottom-left-to-top-right orientation convention) leading
  into a large, lumpy, irregular club-head mass occupying roughly the top-right third of the 16x16
  canvas - asymmetric/organic-looking lumps (not a clean circle/oval) so it reads as a heavy,
  crude weapon rather than a polished mace.
- **Details:** a couple of small metal studs jutting from the head (menace/weight cue), a single
  dulled blood/dirt smear low on the head (battle-worn flavor), and a dark leather-style grip wrap
  crossing the handle partway up.

| Role | Hex | Notes |
|---|---|---|
| Handle wood, dark | `#3E2A1A` | Alternating with the mid-tone along the diagonal for a faceted-wood read |
| Handle wood, mid | `#5A3D24` | |
| Grip wrap | `#24180F` | Near-black leather bands crossing the handle |
| Club-head base | `#6B5A42` | Dominant fill of the lumpy head mass |
| Club-head highlight | `#8C7854` | Upper-left lumps, simulating top-lit shading |
| Club-head shadow | `#463823` | Lower-right lumps |
| Metal stud | `#8A8A82` | Small embedded studs |
| Metal stud shadow | `#4A4A44` | |
| Blood/dirt smear | `#6B2A1E` | Single accent low on the head |

*Compliance note:* this is an original 9-color club treatment - a plain wood-and-stud war club is
a broad, unclaimed fantasy-weapon archetype (not any one game's specific IP), and this palette/
silhouette does not reproduce any existing game's specific club/mace icon, nor either of this mod's
own existing weapon palettes (Gold Sword's bronze/gold hilt, Poison Dagger's green-tinged steel).

### 18.5 Files produced this pass

Placeholder texture + real (verified, not guessed) 1.21.11 model/item-definition JSON, generated
via PowerShell + `System.Drawing` (same technique as every prior placeholder in this document; no
Python/ImageMagick available in this environment). No traced, extracted, or downloaded source
material was used.

- `assets/baum2/textures/entity/zombie_colossus.png` (64x64, placeholder - flat box-UV fills plus
  a handful of hand-placed muscle-striation/eye/jaw accent pixels, same effort level as Sections
  13.4/15.3/17.2's placeholders)
- `assets/baum2/textures/item/colossal_warclub.png` (16x16, placeholder)
- `assets/baum2/models/item/colossal_warclub.json` (real, verified schema)
- `assets/baum2/items/colossal_warclub.json` (real, verified schema)
- `src/client/java/de/baum2dev/baum2/entity/ZombieColossusEntityModel.java` (real Java, not a
  placeholder - reuses vanilla's shared `TexturedModelData` factory, no new geometry)
- `src/client/java/de/baum2dev/baum2/entity/ZombieColossusEntityRenderer.java` (real Java, not a
  placeholder - mirrors vanilla's `GiantEntityRenderer` mechanism, see 18.1)
- `Baum2Client.java`'s registration block, extended with this boss's `EntityModelLayerRegistry`/
  `EntityRendererFactories` pair, mirroring Spider Queen's existing block exactly.

**Not yet done, flagged for a future art pass** (same caveat as every prior placeholder in this
document): real hand-drawn surface detail (skin texture, fabric weave on the leg-wrap, wood grain
on the club) - this pass proves the UV layout, establishes the palette/silhouette, and gives this
boss and its drop something considered to look at in-game, but a human artist pass would meaningfully
raise the ceiling here.

---

## Changelog

- **2026-07-05** — Added Section 9.1 (8 placeholder sub-spec icons for the Character Stats
  Screen's Class-tab `SubspecCardWidget` cards, which shipped with no icon art at all — see
  `HANDOFF.md`'s "Class Overhaul v2 follow-up"). Each icon is its parent class's exact existing
  16x16 icon (Section 9) plus one small overlay detail using only that class's own fill/outline
  hex pair, reflecting the sub-spec's own bonus/flavor text (`SubspecRegistry.java`) — no new
  colors or motifs introduced, a direct extension of Section 3.3's per-class icon system.
  Generated programmatically (Python/Pillow), verified 16x16 RGBA with exactly 3 colors
  (transparent/fill/outline) per file, no anti-aliasing. Updated Section 6's folder listing.
  Also resolved two long-open palette questions as firm decisions rather than further deferrals
  (both documentation-only — no Java files or hex constants touched): **Section 1.1** keeps the
  "Deepwood & Verdigris" menu-chrome palette and the "Vitals & Attributes" combat-HUD palette
  formally separate (not unified), with an explicit rule for which governs which future UI
  element — closes Section 10 item 3. **Section 1.2** ratifies the pattern already followed by
  the 4 existing mini-bosses (every boss-tier mob gets its own new, cross-checked palette, even
  when reusing another boss's model; drops decide per-item; future *common* mobs should default
  to reusing palettes instead) — closes the "keeps deferring" item `HANDOFF.md`'s "Custom UI v1"
  section flagged and adds Section 10 item 5.
- **2026-07-05** — Added Section 18 (boss visual identity: "Zombie Colossus",
  `baum2:zombie_colossus` — the mod's **second** true mobile boss, joining Spider Queen — and its
  "Colossal Warclub" drop, `baum2:colossal_warclub`, renamed from the originally-drafted "Colossus
  Club" after `ip-naming-compliance-checker` found that exact string matches an existing EverQuest
  2 item name; only the item's name/id changed before any asset referenced it, the entity keeps
  its own name). Verified against the decompiled 1.21.11 client jar (not guessed) that vanilla
  already solves the exact "oversized zombie-family boss holding an oversized item" problem via its
  own `GiantEntity`/`GiantEntityRenderer`/`GiantEntityModel` — copied that mechanism directly:
  `ZombieColossusEntityRenderer` extends `MobEntityRenderer` (not `BipedEntityRenderer`, which
  hardcodes an unscaled shadow radius with no override), reuses vanilla's own
  `ZombieEntityRenderState` unchanged, adds `HeldItemFeatureRenderer` by hand, and calls the static
  `BipedEntityRenderer.updateBipedRenderState` helper — giving this boss real held-item rendering
  (the club is a genuine equipped `ItemStack`, not a baked-on cosmetic cuboid) with no new render-
  state class needed. `ZombieColossusEntityModel` reuses vanilla's exact shared
  `BipedEntityModel.getModelData()`/`TexturedModelData` (64x64 canvas, confirmed identical to what
  vanilla's own `ZOMBIE` and `GIANT` model layers both use), scaled 3x via `ModelTransformer` at
  model-layer registration — the same mechanism Spider Queen already established for this mod.
  Established a new "Ashen Brute" palette (brown/red-based, deliberately *not* another green,
  unlike every other hostile mob in this document) with exposed-muscle red-brown accents per the
  "visible muscles" brief, and a 9-color "Colossal Warclub" item palette/silhouette distinct from
  both Gold Sword and Poison Dagger's existing weapon treatments. Produced a placeholder 64x64
  entity texture and 16x16 item texture (PowerShell + `System.Drawing`, no Python/ImageMagick
  available in this environment) plus the real, verified 1.21.11 model/item-definition JSON pair
  for the club. Updated Section 6's folder listing. Wired both new classes into `Baum2Client.java`'s
  registration block, mirroring Spider Queen's existing entry exactly. `./gradlew build` confirmed
  passing after these changes.
- **2026-07-05** — Reworked the Spider Queen **entity's own texture** (Section 17.3) after
  direct playtest feedback: the boss should look "more green and more like a mutant spider,"
  matching its already-implemented green witch-smoke aura particle effect (Java-side, not a
  texture concern). Replaced `assets/baum2/textures/entity/spider_queen.png` in place (same
  64x32 canvas, same exact vanilla-spider UV layout as before — only fill colors/details
  changed, no geometry/UV change). Retired "Royal Carapace" as the *entity's* palette name and
  introduced a new "Mutant Ichor" palette (sickly gray-teal green `#4C6B5A` family, near-black
  plum "Necrotic Vein" crack/joint accents, sickly yellow-green "Bile Blotch" pustule accents,
  irregular/asymmetrical blotch and vein placement for a diseased-mutant read rather than a
  flat green recolor). Also painted a small bright "Toxic Eye" (`#E8FF6B`) glow cluster directly
  into the head UV region for the first time, because `SpiderQueenEntityRenderer` doesn't attach
  vanilla's `SpiderEyesFeatureRenderer` (incompatible with the entity's custom render-state type,
  needed for the leap-attack crouch pose) — there is no automatic eye-glow overlay to rely on
  anymore, unlike ordinary vanilla spiders. **"Royal Carapace" now refers only to the Queen
  Spider Set armor** (Section 17.4, item icons + worn-equipment layers) — per explicit user
  direction, the armor was confirmed to already look correct and was **not** touched in this
  pass; the boss and her armor drop now deliberately use two different, unrelated palettes (see
  the flagged note at the top of Section 17) rather than one shared identity. Generated via
  PowerShell + `System.Drawing`, same technique as every prior placeholder pass in this document.
- **2026-07-05** — Added Section 17 (boss visual identity: "Spider Queen", `baum2:spider_queen`
  — the mod's first true **mobile** boss, as opposed to Sections 13/15's stationary
  mini-bosses — and its "Queen Spider Set" drop, the mod's first genuine boss-tier armor set).
  Spider Queen reuses vanilla's own `SpiderEntityModel` geometry unchanged (scaled 3x at the
  model-transform level, same mechanism as vanilla's Giant/Zombie), so only a re-themed texture
  was needed; its 64x32 canvas and exact per-part UV regions were verified against
  `SpiderEntityModel.getTexturedModelData()` in the decompiled client-sources jar rather than
  assumed. Established a new "Royal Carapace" violet/gold palette, distinct from every other
  palette in this document. Produced 4 boss-armor item icons (16x16) and, for the first time in
  this document, the mod's first **worn-on-player equipment-layer textures**
  (`textures/entity/equipment/humanoid/queen_spider.png` +
  `.../humanoid_leggings/queen_spider.png`) — their 64x32 canvas and UV layout (identical to the
  pre-1.21.11 `armor/diamond_layer_1.png`/`_layer_2.png` convention) were likewise verified
  against decompiled `BipedEntityModel.getModelData()` rather than assumed, per this project's
  standing rule to verify vanilla dimensions/UV layouts against ground truth rather than guess.
  All 7 files generated via PowerShell + `System.Drawing`, same technique as every prior
  placeholder pass. Existing item/model/equipment JSON were checked against the new texture
  identifiers and needed no changes. Updated Section 6's folder listing.
- **2026-07-05** — Added Section 15 (monster visual identity: "Stone of Zombies",
  `baum2:stone_of_zombies` — the mod's second custom hostile mob) and Section 16 (weapon
  visual identity: "Poison Dagger", `baum2:poison_dagger` — the mod's second custom item).
  Stone of Zombies **reuses Section 13.2's 7-cuboid geometry and Section 13.4's UV layout
  exactly, unchanged** (same shared Java model class, `HulkingCocoonStoneEntityModel`) — only a
  new "Toxic Bloom" green/toxic palette (`Blight Stone`/`Plague Husk`/`Toxic Ooze`/`Plague
  Glow` family) and retextured placeholder PNG were produced; no model/Java changes were
  needed. Poison Dagger follows Gold Sword's exact item conventions (same model/item-definition
  JSON schema) with a new original green-tinged-steel palette and a shorter, stubbier
  dagger-specific silhouette (roughly two-thirds of Gold Sword's full-canvas diagonal) so it
  reads as a distinct archetype rather than a small sword. Both placeholder PNGs generated via
  PowerShell + `System.Drawing`. Updated Section 6's folder listing with the two new files.
- **2026-07-05** — Added Section 13 (monster visual identity: "Stone of Spiders",
  `baum2:stone_of_spiders` — the mod's first custom hostile mob) and Section 14 (weapon visual
  identity: "Gold Sword", `baum2:gold_sword` — the mod's first custom item). Section 13
  establishes a 7-cuboid breakdown (base/body/cap + 2 web-strand accents + 2 glow-vein bumps)
  and an original stone/shell/silk/glow-vein palette, distinct from both the UI chrome palette
  (Section 1) and the Vitals HUD palette (Section 11); produced a placeholder 176x176 box-UV
  entity texture. Section 14 establishes the mod's first item-icon convention (diagonal blade,
  16x16) reusing Aged Brass for the pommel for cross-system cohesion; produced a placeholder
  16x16 texture plus the real (verified against the decompiled vanilla client jar, not guessed)
  1.21.11 model/item-definition JSON pair. No Python/ImageMagick available in this environment
  — both placeholder PNGs were generated via PowerShell + `System.Drawing` instead. Updated
  Section 6's folder listing with the new `textures/entity/`, `models/item/`, and `items/`
  paths now in use.
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
