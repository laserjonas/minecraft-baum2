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
      drevathis_cursed_blade.png (16x16, item icon - see Section 19.4)
      risssplitter.png         (16x16, item icon - see Section 20.6)
      wanderross_flute.png     (16x16, item icon - see Section 22.5)
      eisenross_flute.png      (16x16, item icon - see Section 22.5)
      schlachtross_flute.png   (16x16, item icon - see Section 22.5)
    block/
      rissobelisk.png          (16x16, side/bottom face - see Section 20)
      rissobelisk_top.png      (16x16, top face - see Section 20)
    entity/
      stone_of_spiders.png     (176x176 box-UV sheet - see Section 13)
      stone_of_zombies.png     (176x176 box-UV sheet, same layout as above - see Section 15)
      spider_queen.png         (64x32, vanilla spider UV layout - see Section 17)
      zombie_colossus.png      (64x64, vanilla biped/zombie UV layout - see Section 18.2)
      drevathis.png            (64x64, bespoke biped UV layout, client-only resource - see Section 19.2)
      wanderross.png           (254x152 GeckoLib atlas, shared mount_horse geometry, armor
                                cubes fully transparent - see Section 22)
      eisenross.png            (254x152, same atlas layout, saddle-armor group painted - see Section 22)
      schlachtross.png         (254x152, same atlas layout, both armor groups painted - see Section 22)
      equipment/
        humanoid/
          queen_spider.png     (64x32, vanilla classic armor-layer UV - see Section 17.4)
        humanoid_leggings/
          queen_spider.png     (64x32, same UV convention - see Section 17.4)
  geckolib/
    models/entity/
      fallen_comet_stone.geo.json  (shared by all 33 stone mini-bosses - see Section 13.5)
      spider_queen.geo.json        (see Section 17)
      zombie_colossus.geo.json     (see Section 18)
      drevathis.geo.json           (client-only resource - see Section 19.7)
      mount_horse.geo.json         (shared by all 3 mount tiers - see Section 22)
    animations/entity/
      fallen_comet_stone.animation.json  (see Section 13.5)
      spider_queen.animation.json        (see Section 17)
      zombie_colossus.animation.json     (see Section 18)
      drevathis.animation.json           (client-only resource - see Section 19.7)
      mount_horse.animation.json         (idle/walk/attack - see Section 22)
  models/
    item/
      gold_sword.json          (see Section 14)
      poison_dagger.json       (see Section 16)
      queen_spider_helmet.json, queen_spider_chestplate.json, queen_spider_leggings.json,
      queen_spider_boots.json  (see Section 17.4)
      colossal_warclub.json    (see Section 18.3)
      drevathis_cursed_blade.json (see Section 19.4)
      risssplitter.json        (see Section 20.6)
      wanderross_flute.json, eisenross_flute.json, schlachtross_flute.json
                               (see Section 22.5 - plain `minecraft:item/generated` + layer0,
                                already present before this pass, textures added by this pass)
    block/
      rissobelisk.json         (see Section 20.2 - vanilla `cube_bottom_top` parent, no
                                custom geometry in this first pass)
  items/
    gold_sword.json            (1.21.11 item-model-definition entry point - see Section 14)
    poison_dagger.json         (1.21.11 item-model-definition entry point - see Section 16)
    queen_spider_helmet.json, queen_spider_chestplate.json, queen_spider_leggings.json,
    queen_spider_boots.json    (1.21.11 item-model-definition entry points - see Section 17.4)
    colossal_warclub.json      (1.21.11 item-model-definition entry point - see Section 18.3)
    drevathis_cursed_blade.json (1.21.11 item-model-definition entry point - see Section 19.4)
    rissobelisk.json           (1.21.11 item-model-definition entry point - see Section 20.2;
                                points directly at `models/block/rissobelisk.json`, no
                                intervening `models/item/rissobelisk.json` - see that section
                                for why this differs from every other item's two-file pattern)
    risssplitter.json          (1.21.11 item-model-definition entry point - see Section 20.6;
                                plain `Item`, so it follows the normal two-file pattern above,
                                not Rissobelisk's own divergent block-item pattern)
    wanderross_flute.json, eisenross_flute.json, schlachtross_flute.json
                               (1.21.11 item-model-definition entry points - see Section 22.5;
                                already present before this pass, textures added by this pass)
  equipment/
    queen_spider.json          (1.21.11 equipment-texture definition - see Section 17.4)
  blockstates/
    rissobelisk.json           (simplest single-variant form - see Section 20.2)
```

Note: `drevathis.png` is the first entity texture placed under `src/client/resources/assets/baum2/`
rather than `src/main/resources/assets/baum2/` (where every prior entity texture in this table
lives) - intentional, since `DrevathisEntityModel`/`DrevathisEntityRenderer` are themselves
client-only classes (`src/client/java/...`) with no server-side model/renderer counterpart;
Loom merges `src/main/resources` and `src/client/resources` into the same `assets/baum2/`
namespace at build time, so the resulting `Identifier` (`baum2:textures/entity/drevathis.png`)
resolves identically either way - this is a source-set-placement choice, not a different asset
convention.

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

### 13.5 "Fallen Comet Stone" rework (2026-07-09) — shared GeckoLib template, supersedes 13.1/13.2/13.4

The cocoon-husk shape above was replaced per user direction ("more accurate, like a comet
which has fallen down") with a shared **fallen comet stone** template used by every stone
mini-boss — Sections 13.1, 13.2, and 13.4 above are kept for history only;
`HulkingCocoonStoneEntityModel` is deleted. Section 13.3's palette **remains the ratified
Stone of Spiders palette** — the rework changes shape, not color identity — with two role
changes: the husk/silk roles (Cocoon Husk*/Spun Silk) are retired (the shape is pure rock
now), and the previously "reserved" Fused Stone Pale highlight is finally in use.

- **Shape concept (original design, not sourced from any game)**: a tall angular monolith
  tilted ~23° off vertical with its lower end buried below ground level (the entity's
  sub-ground cubes clip into the terrain and sell the impact), built from five staggered
  cubes across three chained bones (`comet`/`comet_mid`/`comet_top`, each adding a small
  extra bend so the column reads as one jagged crystal, not a stacked pyramid); a rubble/
  crater ring plus two upturned rim slabs around the impact point; glowing energy veins up
  the rock faces with heat pooling near the buried end; and three small glow-rimmed shards
  that float around the stone. "Meteorite/comet monolith embedded in a crater" is a
  genre-generic fantasy trope; every measurement, the crater layout, the vein painting, and
  the palette are this project's own.
- **Idle animation** (`animation.fallen_comet_stone.idle`, 12s seamless loop): the monolith
  itself never moves (it's a crashed rock) — the three shards orbit it (parent `shards` bone,
  one revolution per loop) while bobbing and self-spinning at staggered phases/speeds.
- **Template contract** (how the next stone boss gets its skin): geometry and animation are
  ONE shared pair of files for all stone bosses —
  `assets/baum2/geckolib/models/entity/fallen_comet_stone.geo.json` +
  `geckolib/animations/entity/fallen_comet_stone.animation.json` — resolved via
  `FallenCometStoneGeoModel`'s `withAltModel`/`withAltAnimations`; only the texture is
  per-entity (`textures/entity/<entity_name>.png`). A new stone needs: a 6-role palette
  (ROCK_SHADOW/ROCK/ROCK_PALE/FISSURE/GLOW/GLOW_DIM) added to
  `tools/gen_fallen_comet_stone.py`, a rerun of that script, and a renderer registration —
  no new geometry, animation, model, or renderer classes. The generator reseeds its RNG per
  palette so every variant's atlas layout is pixel-identical.
- **Files**: generator `tools/gen_fallen_comet_stone.py` (geometry + animation + all stone
  textures in one script); preview via
  `python tools/render_geckolib_preview.py --model fallen_comet_stone --tex stone_of_spiders.png`.
  Textures are pixel-art at 2 px per model unit (~250x182 atlas), same placeholder tier as
  the other GeckoLib bosses' generated atlases.

### 13.6 One stone per vanilla monster (2026-07-09) — the full stone ladder

The template proved itself immediately: the stone family was expanded from 2 to **33 stones —
one per normal vanilla hostile monster** (bosses and a few broken-on-land/unused mobs
excluded; full roster + exclusion reasoning in `registry/FallenCometStones.java`'s javadoc),
on a difficulty ladder in 5-level steps (levels 5-95, two stones per tier from level 20 up).
Every stone uses the Section 13.5 geometry/idle animation unchanged; each has only its own
palette (Section 13.7) and texture. Visual-consistency rule: this is deliberately ONE visual
family — the Section 1.2 "bespoke palette per boss" rule is applied *within* a fixed 6-role
template (rock triplet, fissure, glow pair), so 33 stones read as one coherent system with a
per-monster color accent, not 33 unrelated designs.

### 13.7 Stone palette table (all 33, pinned)

Roles per row: rock shadow/mid/pale, fissure, glow core, glow dim — same painting treatment
for every stone (`tools/gen_fallen_comet_stone.py` is the single source of truth; this table
mirrors it). Rock family is tinted toward the monster's body color, glow toward its most
recognizable accent. **Known, accepted crowding**: several glow hues are near-neighbors
(three pale blues: Strays/Breezes/Vexes; several greens: Zombies/Slimes/Bogged/Creepers/
Phantoms; two golds: Piglins/Evokers/Piglin Brutes) — the rock tint is the primary
differentiator there, acceptable at placeholder tier and flagged for any future real-art pass.

| Stone | Rock (shadow/mid/pale) | Fissure | Glow (core/dim) |
|---|---|---|---|
| Silverfish | `4A4A52` `6E6E78` `92929E` | `26262C` | `C8D4E8` / `6E7A94` |
| Endermites | `3A2E44` `54425F` `6F5A7E` | `1E1426` | `C77FE8` / `7B3FA0` |
| Spiders (13.3) | `3A362E` `5C574A` `7A7566` | `2A251C` | `C4E064` / `6E8A2E` |
| Skeletons | `4E4A40` `787265` `A29A88` | `2A2620` | `E8E2C8` / `948E6E` |
| Zombies (15.2) | `26301F` `435930` `647A47` | `17200F` | `3DFF7E` / `1B8A45` |
| The Parched | `4E4636` `746A52` `9C9070` | `28221A` | `F0E0A0` / `A89858` |
| Cave Spiders | `2E3A38` `435754` `5F7A75` | `15201E` | `4FE0C4` / `2A8A76` |
| The Drowned | `2C3E44` `40606A` `5C8894` | `14242A` | `62D8E8` / `2E7E8E` |
| Zombie Nautiluses | `2A3A36` `3E5650` `587670` | `121E1B` | `E0A8C0` / `8E5876` |
| Husks | `4A4030` `6E5F45` `94805F` | `261F14` | `E8C468` / `94793A` |
| Strays | `3C4450` `56626F` `7A8896` | `1E242C` | `B8E8F0` / `5E8FA6` |
| The Bogged | `34382A` `4C543C` `687252` | `191D12` | `8FA63C` / `4F5E20` |
| Camel Husks | `483A2C` `6A5640` `8E7458` | `241C12` | `D8A868` / `8E6830` |
| Creepers | `3E4A3A` `566853` `748A6F` | `1E2419` | `F0F4E0` / `8A9478` |
| Piglins | `4A2E2A` `6E4239` `8E5A4C` | `26120E` | `F0C048` / `9E7820` |
| Witches | `3A2C3E` `554060` `74587E` | `1C1220` | `E060D0` / `8A3480` |
| Breezes | `3A3E4E` `525A70` `707A94` | `1A1E28` | `B8C4F8` / `6A7AA8` |
| Slimes | `3A4A34` `52684A` `6E8A62` | `1C2618` | `8EE85E` / `4A9430` |
| Hoglins | `4A332E` `6E4A40` `926355` | `261510` | `F09858` / `A05A28` |
| Phantoms | `2E3248` `424866` `5C648A` | `141728` | `96F0A8` / `46945C` |
| Zombified Piglins | `44322E` `5E4A42` `7A6154` | `221512` | `E8D890` / `8E824A` |
| Endermen | `241C2C` `362A42` `4A3A5A` | `100A16` | `D24AF0` / `7C24A0` |
| Magma Cubes | `2E1E1A` `46302A` `5E443A` | `140C0A` | `FF8830` / `A84A10` |
| Pillagers | `3A3E3A` `545954` `707670` | `1C1E1C` | `48B8A8` / `247E70` |
| Blazes | `3A2E20` `564430` `745C40` | `1A130C` | `FFD838` / `B08608` |
| Vindicators | `3E3830` `5A5248` `7A7062` | `1E1A14` | `C8CCD8` / `787E8E` |
| Ghasts | `4E4A4A` `787070` `A09898` | `282424` | `F06060` / `942E2E` |
| Vexes | `3E4450` `5A6274` `7A8498` | `1E222A` | `8CB4E8` / `5678A8` |
| Wither Skeletons | `26262A` `3A3A40` `525258` | `0E0E12` | `D8D8E0` / `76767E` |
| Shulkers | `463A4E` `645472` `857495` | `221A28` | `E8C8F8` / `9A6EB8` |
| Evokers | `34343C` `4C4C58` `686876` | `18181E` | `FFE070` / `B89428` |
| Piglin Brutes | `3E2622` `5E3A32` `7E4E42` | `1E0F0C` | `FFB020` / `A86A00` |
| Ravagers | `3C3630` `58504A` `766A60` | `1C1815` | `E84848` / `8E2020` |

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

### 15.4 "Fallen Comet Stone" rework (2026-07-09) — same template as Section 13.5

Stone of Zombies was reworked together with Stone of Spiders onto the shared fallen-comet-
stone GeckoLib template — see Section 13.5 for the full shape/animation/template contract;
Sections 15.1 and 15.3 above are history only. Section 15.2's "Toxic Bloom" palette remains
the ratified identity (same role mapping as 13.5: husk/ooze roles retired, Blight Stone Pale
now in use). Its texture is generated from the identical atlas layout as
`stone_of_spiders.png` by the same `tools/gen_fallen_comet_stone.py` run, so the two stones
stay reskins of one geometry exactly as before — just comet-shaped now. The entity's own
client-side `LARGE_SMOKE` ambient particle loop is unchanged and now reads as impact smoke.

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

**Superseded 2026-07-05 — see 17.1.1 below.** Originally, Spider Queen reused vanilla's own
`SpiderEntityModel` geometry unchanged, scaled 3x via a `ModelTransformer` at model-layer
registration. Only the texture was original. This has since been rebuilt on GeckoLib with
bespoke geometry — the paragraph above is kept for history, not current behavior.

*Compliance note:* "giant reskinned version of a normal enemy, scaled up, as a boss" is a
generic, widely-used monster-variant convention (colossal/elite versions of common enemies
appear across countless unrelated games, and vanilla Minecraft does this exact thing with
Giant/Zombie) — genre convention, not IP. The palette, markings, and armor design below are
original and were not modeled on any specific existing creature or game's actual design.

### 17.1.1 GeckoLib rebuild (2026-07-05) — bespoke geometry, reference-informed, still original work

Spider Queen was the first mob migrated to GeckoLib (see `docs/fabric-modding.md`'s "GeckoLib
integration" section for the full technical research). Two motivations, addressed together:

- **Animation quality**: the leap attack's wind-up/flight poses were previously hand-coded
  per-tick angle math in `SpiderQueenEntityModel`/`SpiderQueenRenderState`. They're now real
  keyframed GeckoLib animations (`spider_queen.animation.json`: `idle`, `walk`, `leap_windup`,
  `leap_flight`), driven by an `AnimationController` reading the same already-synced
  `LEAP_WINDUP_TICKS` plus a new `LEAP_FLIGHT_ACTIVE` synced flag. **Important, don't
  misattribute this**: GeckoLib is a rendering/animation library only — the leap's actual
  trajectory/physics (`SpiderQueenEntity.travel()`/`performLeapFlightStep()`) is completely
  unchanged by this migration.
- **Model quality**: the vanilla-derived geometry was flat/plain (a straight reskin, no
  silhouette work). The new geometry is bespoke — 19 bones, 30 cuboids, a genuine two-segment
  (upper+lower) leg per limb instead of vanilla's single straight-box legs, computed via real
  trigonometry (not hand-guessed) for a much taller stance where each leg arcs up above body
  height before angling back down to the ground, plus small frayed spike-tuft cuboids on the
  abdomen and a painted eye cluster.

**On the Sketchfab reference (read this before assuming it was imported):** the user found
["Voided Spider" by RedVoid_](https://sketchfab.com/3d-models/voided-spider-08013f2480d949a18642507cf6d96693)
(CC BY 4.0, commercial use + modification explicitly permitted, confirmed via Sketchfab's own
API) and asked to use it for Spider Queen. **The mesh itself was not imported** — confirmed via
research that neither vanilla's `EntityModel` nor GeckoLib's `GeoModel` can consume an arbitrary
organic triangle mesh regardless of source format (glTF/OBJ/USDZ/FBX all normalize to a mesh;
the target frameworks are strictly cuboid/bone hierarchies), and building a from-scratch
glTF-mesh-plus-skeletal-animation entity renderer for Fabric 1.21.11 was assessed as a
multi-week undertaking with no current maintained library support — out of scope for this pass.
Instead, the reference's actual renders were viewed directly and used as a **silhouette/
proportion reference only**: its dramatically tall leg stance (legs arc above body height, unlike
vanilla's flat spread), angular multi-segment legs, and small frayed tendril/antenna tufts on the
abdomen informed the new geometry's shape. **No pixels or mesh data were copied** — the shipped
geometry (cuboid coordinates, computed via trigonometry) and texture (programmatic noise +
edge-shading fills, same technique as every other placeholder texture in this document) are
original work, keeping this compliant with this project's "no copied assets" rule. Colors were
deliberately **not** changed to match the reference (which is black/red) — Mutant Ichor's
established green palette (17.3, unchanged) was kept, since the reference's job was shape
inspiration only, not a palette swap. Attribution recorded in `CREDITS.md` per the CC BY 4.0
license's requirement, regardless of the "reference only" framing.

*Compliance note (unchanged from above):* this remains an original, from-scratch model —
computed geometry and a from-scratch texture, not an imported or traced asset.

### 17.1.2 Reverted to vanilla-accurate geometry (2026-07-06) — the bespoke geometry above "didn't look like a spider," fixed by transcribing real vanilla proportions

Direct user feedback after the first live test of 17.1.1's bespoke geometry: "the spider is not
looking like a spider. It looks completely out of place... can we use the standard minecraft
spider." The tall reference-inspired stance and 2-segment legs read as unrecognizable rather than
"an improved spider." **Fixed by discarding the invented proportions entirely** and instead
transcribing vanilla's own real `SpiderEntityModel.getTexturedModelData()` (read directly from
this project's own decompiled Minecraft sources, not from memory) into GeckoLib's geo.json
format bone-for-bone: same 8 single-segment legs (not 2-segment), same head/body0/body1 cuboid
sizes, same per-leg pivot positions and rotation angles as real vanilla spiders — so the
silhouette is now, deliberately, indistinguishable from a normal Minecraft spider (just 3x
scaled, unchanged from before, and recolored — see 17.3).

**Coordinate conversion, verified not guessed**: vanilla's legacy `ModelPart` format uses Y
*down* from each part's own pivot; GeckoLib/Bedrock format uses Y *up* from the entity's feet
(confirmed via Bedrock Wiki, not assumed). Both were also confirmed (Bedrock Wiki + this
project's own `ModelPart.java`/JOML source) to apply bone rotations in the *same order* (Z then Y
then X). Reflecting a single axis (Y) turns a right-handed system left-handed, so — derived via
first-principles rotation-matrix algebra, not guessed — any rotation whose plane involves Y
(rotations around X or Z) needs its angle **negated** to represent the same physical orientation
after the flip; rotation around Y itself is unaffected. Concretely: vanilla pitch → `-pitch`,
yaw → `yaw` (unchanged), roll → `-roll`. **This was numerically self-checked before writing the
final geometry file**: a Python script reconstructed each of the 8 legs' endpoint positions two
independent ways (vanilla's own rotation formula reflected, vs. the derived Bedrock pivot+rotation
reconstruction) and asserted they matched to within 1e-6 units before proceeding — they matched
exactly on the first attempt, which is the actual basis for confidence here, not just "the logic
looks right."

**Still genuinely original, not a copied asset**: only the *proportions/angles* (pure geometric
facts about box sizes and joint angles, not copyrightable expression) were transcribed; the
texture is a from-scratch programmatic noise+edge-shading atlas in this project's own colors (see
17.3), not vanilla's own spider texture. GeckoLib's animation system (idle/walk/leap_windup/
leap_flight, from 17.1.1) is retained — the bone *names* were updated to match vanilla's real leg
names (`right_hind_leg`, `left_middle_front_leg`, etc., 8 single-segment legs instead of the old
invented 16 two-segment ones) but the animation architecture and the leap's underlying physics
are unaffected by this geometry swap.

### 17.2 Entity texture: `spider_queen.png`

**Rebuilt 2026-07-06 alongside the vanilla-accurate geometry revert (17.1.2) — dimensions and
palette below are current, superseding both the old vanilla-UV table and the 17.1.1 "Mutant
Ichor" version.**

- **File:** `assets/baum2/textures/entity/spider_queen.png`.
- **Canvas:** **120x72 px** — a programmatically-generated atlas, one small flat-color+noise
  cell per cuboid (13 cells used of a 10x6 grid, 12x12 px each), not a hand-unwrapped box-UV
  layout — smaller than 17.1.1's canvas since vanilla-accurate geometry has far fewer cuboids
  (13 vs. 30). Each cuboid's 6 faces all point at the same cell with a darkened border (fake
  ambient occlusion) and a lightened top edge (fake rim light); abdomen and eye cells additionally
  get a bright highlight streak for a "shiny/glossy" read (see palette below). Generated by a
  Python script (not committed — one-off tooling, same as every other prior boss's placeholder
  texture).

### 17.3 Entity palette: "Widow" (boss's own texture — distinct from the armor's palette; see divergence note below)

**Revised twice on 2026-07-06.** First pass (superseding "Mutant Ichor" green entirely) after
the vanilla-accurate geometry rebuild (17.1.2): user asked for "colored grey" with "shiny red
eyes," a "shiny" red abdomen that's "kinda pulsing." **Second pass, immediately after**: the user
sent a real reference photo of a garden-spider's mottled abdomen marking and said "the spider
should have pattern like this. Do not use redstone red. Use the red from the spider's eyes. Also
use it on the pattern back," and separately specified an exact primary hex, `#898989`. The
palette below is the current, post-both-passes state — the "Redstone Widow" name and its darker/
brighter red-family hexes from the first pass no longer exist in the shipped texture.

| Role | Name | Hex | Notes |
|---|---|---|---|
| Primary body color | Widow Grey | `#898989` | Head/thorax base color — exact hex per direct user spec |
| Carapace highlight | Widow Grey Pale | `#B5B5B8` | Reserved for a future highlight pass — current texture uses the noise+rim-light technique instead |
| Carapace shadow | Widow Grey Dusk | `#5A5A5D` | Legs and the abdomen's own base (slightly darker than the head/thorax, for contrast) |
| Accent red | Widow Red | `#FF3B1E` | The **one** red used for both eyes and the abdomen pattern accents — deliberately the same hex in both places, not two related-but-different reds (the first pass's separate darker "abdomen red" and brighter "eye red" were merged into this single color per the user's explicit "use the red from the eyes" instruction) |
| Eye socket | Widow Eye Socket | `#141212` | Dark base under the small bright-red eye cubes |

*Compliance note:* "grey spider with a red marking" is a broad genre/nature convention (evokes
real widow spiders' red marking generally, not any specific game or a literal copy of the
reference photo's own coloring — the photo was used for pattern/shape inspiration only, see
below). Checked distinct from every other palette in this document: still the mod's only
grey-dominant palette, and its red accent is a single flat hex rather than a multi-tone "family"
the way every other palette in this document builds a red/green/violet family — a deliberate
simplification per the "use the same red as the eyes" instruction, not an oversight.

**Abdomen pattern — reworked from a flat block, then reworked again from a simple hourglass to
an organic multi-blotch pattern.** First real fix: the abdomen was originally a solid flat red
cuboid face; changed to a grey base with red confined to a **hand-drawn widow-hourglass marking**
(two triangles meeting point-to-point). **Second fix, this same day**: the user sent a real photo
of a garden spider (Araneus-type) and asked for "pattern like this" instead — its abdomen shows
an irregular, organic cluster of blotches, not a clean geometric hourglass. The marking was
redrawn as a small cluster of jittered, irregular polygon "blobs" (mirrored left-right for a
natural symmetric look) plus a few small highlight dots down the spine, still confined to the
"up" and "south" (Blockbench convention: `+Z`, the rear-facing tip) UV faces of the abdomen via a
dedicated 32x32 texture region (up from 24x24, for more room to render the extra shapes clearly).
**This stays a reference, not a copy**: only the general *organic, multi-blotch layout idea* came
from the photo; the actual drawn shapes are procedurally jittered polygons in this project's own
grey/red palette (not the photo's real cream/brown coloring), generated by a Python script, same
"reference informs an original asset" boundary already established for the Sketchfab model in
17.1.1 above.

**Pulsing red glow, implemented in Java, not just texture:** a static texture alone can't
"pulse." `SpiderQueenEntity.spawnPulsingAbdomenGlow()` spawns vanilla's own `DustParticleEffect`
(the same particle type real redstone dust emits, though the *color* used is now `Widow Red`,
not a "redstone-branded" hex — the particle *type* choice is a technical implementation detail,
independent of the naming/color feedback above) clustered near the abdomen, with both particle
count and color lerped via a sine wave over the entity's age for a genuine brightening/dimming
cycle.

**On the eyes — no vanilla glow overlay, same as before:** vanilla's `SpiderEyesFeatureRenderer`
still isn't attached (bound to vanilla's own `SpiderEntityModel`/render-state type, incompatible
with this project's custom GeckoLib-based renderer). The eye read comes from small bright
`Widow Red` cubes placed directly on the head geometry's front face in the geo.json itself, a
genuine 3D geometric detail, not a painted texture patch.

**Redesigned from 2 eyes to 8, per direct feedback ("some eyes are missing, two eyes are not
spider-like").** Two large forward-facing eyes read as a mammal face, not a spider — real
spiders have a tight cluster of small eyes near the front-bottom of the cephalothorax, typically
in two rows. Replaced with an 8-eye cluster: a posterior row of 4 smaller eyes and an anterior
row of 4 slightly larger eyes, evenly spaced across a tight ~2.6-3.4 unit span, all small
(0.55-0.75 units) rather than the old 1.6-unit pair.

**Follow-up fix, same day: "that does not look like spider eyes."** The 8-eye cluster from the
fix above still read wrong — each eye was a *flat, solid-color square* face, which looks like a
tiled red keypad/QR pattern rather than eyes, no matter how small or how many. Fixed at the
*texture* level, not the geometry: each eye cube now gets a dedicated small texture cell (via a
new `alloc_eye_cell` helper) painted as a dark socket background with a small **round** red dot
plus a tiny glossy highlight — the cube itself is still a flat-faced cuboid (Minecraft/GeckoLib
can't render true curved geometry), but a round-painted texture on a small square face reads as
an eye in a way a flat solid fill never can. General lesson for any future small "detail" cube
in this project (eyes, spots, gems, etc.): a flat `alloc_cell` fill is fine for a large
body-panel color, but for anything meant to read as a distinct *feature* rather than a body
panel, paint an actual shape (round, in this case) into its own dedicated cell instead.

**Real bug in that same fix: the round dots were drawn correctly but still didn't render** —
the eyes showed as flat black squares in-game even after the texture change above. Root cause:
the shared `face_uv()` helper caps its `uv_size` to `min(cube_size, CELL)`, which is harmless
for a flat solid fill (any sampled sub-region of a uniform color is still that color) but wrong
for a *painted* texture — for a tiny 0.55-0.75-unit eye cube, that only samples a sliver in the
corner of the 12x12 texture cell, missing the drawn circle entirely and showing background only.
Fixed by giving eye cubes their own UV-face construction that always samples the *full* cell
(matching how the abdomen's marking cell already did this correctly via its own dedicated
`uv_size`, not the generic capped helper).

**Eyes redesigned a third time: from a uniform round-dot grid to a scattered, varied-size
cluster.** After the round-dot fix above still didn't land ("can you make eyes like these?",
pointing at the abdomen's own scattered blotch pattern from 17.1.2 in a screenshot), the eye
cluster was rebuilt again: 9 small flat-colored cuboids (0.4-0.95 units, three `Widow Red`
shades — base, brighter, darker) at randomized sizes and jittered positions within a tight
cluster area, matching the same "organic, irregular, varied-size" quality that made the
abdomen's own pattern read well, rather than either the 2-large-eye or the uniform-round-dot-grid
attempts. Dropped the dedicated dark-socket-plus-round-dot texture from the previous fix (now
unused code, removed) in favor of plain colored facets — consistent with the mod's own blocky
aesthetic, and the thing the user pointed at approvingly was the *irregularity*, not roundness.

**Real bug on that same attempt, fixed: the eyes merged into one blob.** Pure random placement
(no minimum-spacing check) packed up to 9 eyes, some as large as 0.95 units, into a ~3.4x1.8
cluster area — direct feedback ("eyes looks like they are merged together") confirmed by the
screenshot showing overlapping red shapes rather than distinct eyes. Fixed with simple rejection
sampling: each candidate eye re-rolls its position/size until it's far enough from every
already-placed eye (center distance > half the summed sizes, plus a fixed `0.35` gap) to read as
a separate shape, giving up on that slot after 200 failed attempts rather than looping forever
(typically places 8 of the 9 requested eyes in the available cluster area — an acceptable, still
fully spider-like result, not something to force to exactly 9 by shrinking the gap back down).

**Head chamfer attempt 1 REVERTED — made it look worse, not better.** The corner-bevel technique
described above (inset main cube + 4 cuboids rotated 45° at the vertical edges, using per-cube
`rotation`+`pivot`) rendered as oversized, disconnected slabs sticking out from the head, not a
subtle rounded edge — direct feedback: "looks more weird." Root cause not fully confirmed at the
time: per-cube rotation+pivot is a different code path than the (rigorously matrix-verified)
bone-level rotation used everywhere else in this file, and it hadn't been cross-checked
numerically before use. Reverted to a plain cube rather than guess again immediately.

**Head chamfer attempt 2 — investigated properly instead of giving up, reused the verified
bone-level mechanism instead of the untested cube-level one.** Direct follow-up ("no possibility
to improve?") prompted actually reading `GeoCube.java`'s own `render()`/`translateToPivotPoint()`
methods rather than treating attempt 1's failure as a dead end. Confirmed: cube-level
`pivot`/`rotation` really is a distinct, less-common code path (`translateToPivotPoint` divides
`pivot()` by 16 at *render* time, separately from `origin()`'s own `/16` at *construction*
time) — real, but not the actual bug found; what mattered was that this path had simply never
been validated the way bone rotation had. Rather than debug the untested path further, the fix
reuses the **already-proven bone-level rotation mechanism** (the same one the legs use) instead:
each corner bevel is now its own small bone — a single square cuboid, full head height, rotated
45° around Y — using the identical `pivot`/`rotation`-array pattern already confirmed correct.
A pure Y-axis (yaw-only) rotation is also the simplest possible case for this project's
Y-reflection rule: rotating purely around Y commutes with reflecting Y (they don't share an
axis), so — unlike the legs' compound yaw+roll case — no sign correction is even needed, and for
a square cross-section `+45`/`-45` look identical anyway, so this is doubly safe. The main head
cube is left full-size (not inset) so this doesn't touch the already-working eye placement, which
is anchored to the head's original front-face position — these bevels add a small diagonal facet
at each vertical edge rather than a true geometric chamfer, a smaller, safer version of the
original idea. **Verified before shipping**: computed each bevel cube's center offset from its
own pivot through GeckoLib's actual confirmed transform (not assumed) — all four came out at
~0 offset, confirming each bevel rotates cleanly in place at its corner rather than drifting, the
same class of check (external verification, not self-consistency) established after the leap
orientation bug.

**Eyes: bumped up in size** ("eyes still small") — size range `0.4-0.8` → `0.7-1.3`, cluster
span widened to match (`2.4` → `3.2`) so the bigger eyes still have room under the same
minimum-spacing rule from the merged-eyes fix.

**Eyes redesigned a fourth time, plus a new dark face-plate: "eyes still not looks dangerous."**
Diagnosis: it wasn't count or spacing this time, it was contrast and structure. Two fixes
together:
- **Dark face-plate**: the head's front ("north", Blockbench convention `-Z`) face now gets its
  own near-black `Face Shadow` (`#151212`) fill, distinct from the other 5 faces which stay
  plain `Widow Grey` — bright red against near-black reads as far more menacing than red against
  mid-grey, the same principle real/fictional predators' eye-shine relies on. Implemented as a
  direct per-face UV override on the head's own cube (no new bone/cube needed).
- **Primary + secondary eye structure**, replacing the fully-random scattered cluster: 2 larger
  (`1.5` unit) bright `Widow Red` "primary" eyes placed symmetrically — the actual predatory
  glare — plus 5-7 smaller (`0.45-0.75`) secondary eyes scattered below/around them (same
  rejection-sampling spacing rule as the merged-eyes fix, now checked against the primaries too)
  for the spider-appropriate many-eyes read. Pure randomness read as chaotic, not threatening;
  a deliberate focal pair plus a supporting cluster reads as both "spider" and "dangerous."

**Head corner-rounding — dropped, not reattempted a third time.** Both prior attempts (per-cube
rotation/pivot, then a bone-based rebuild whose transform was verified numerically against
GeckoLib's actual confirmed behavior before shipping) still read as wrong in-game — the first as
"oversized disconnected slabs," the second as "head is malformed." The second attempt's *math*
checked out (each bevel cube centered on its own pivot with ~0 offset, confirmed before
shipping) — this means the remaining problem is aesthetic, not a transform bug, and isn't
something more formula-verification can fix. Not worth a third attempt on a purely cosmetic
request; the head is back to a plain, unmodified cube. Revisit only with an actual design idea
for what "rounder" should look like on this silhouette, not another geometry-math pass.

**Pulsing red particle "smoke" — removed entirely**, per direct feedback ("the red smoke should
be removed... looks like trash"). `SpiderQueenEntity.spawnPulsingAbdomenGlow()` and its
supporting constants/imports (`DustParticleEffect`, `MathHelper`, `ColorHelper`) are deleted
outright, not disabled — nothing calls it, nothing references it. The abdomen's own texture
marking (17.1.2/above) is unaffected; only the particle effect layered on top of it is gone.

**Real bug found on the second live test, fixed: the eyes were invisible.** The first version
placed the eye cubes at `head_z_front + 0.1` — since the head's front face sits at the *more
negative* end of its Z-range (`hz+hoz = -11`), adding `+0.1` moved the eyes to `-10.9`, which is
*behind* the front face, i.e. fully embedded inside the head's own solid opaque cuboid and
therefore completely hidden. Fixed to `head_z_front - 0.15`, which actually protrudes past the
front face. General lesson for this geo.json generator script: "in front of a face at the
model's negative-Z end" means *subtracting* further, not adding — a `+` offset moves *into* the
model, not away from it, whenever the reference face is on the negative side of that axis.

**A far more serious bug, found on the same live test round, is documented separately in
`docs/fabric-modding.md`'s "GeckoLib integration" section, part H: the entire model was upside
down / floating, legs pointing up instead of down.** This wasn't a geometry-shape mistake like
the ones above — it was a wrong understanding of how GeckoLib actually converts geo.json
pivot/rotation values into rendered positions, root-caused by reading GeckoLib's real loader
source code rather than continuing to guess. See that doc for the full technical writeup; the
short version is that the geometry here has been regenerated with the corrected transform and
numerically self-checked (comparing the full 8-corner box shape, not a single named corner)
before being written.

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

### 17.6 Visual redesign pass (2026-07-06) — pixel-art texture atlas, fangs, real leap animation

The pass that ended the blind-iteration cycle documented in `docs/spider-queen-fable-handoff.md`.
Key enabler: **`tools/render_geckolib_preview.py`** (generalized shortly after this pass from
its original `render_spider_queen_preview.py` form; works for any GeckoLib model via
`--model <name>`, with an auto-fitting camera), an offline renderer that draws the
GeckoLib model + texture + animation poses to a contact-sheet PNG using GeckoLib 5.4.5's own
transcribed transform/UV logic — every change below was *seen* before being shipped, instead of
guessed at and confirmed via `runClient` screenshots after the fact.

- **Texture** (`tools/gen_spider_queen.py`, rewritten): a packed pixel-art atlas (128x48, 2 px
  per model unit) with per-face painting, replacing the old uniform noisy 12x12 cells (which
  produced the rejected "grey box with legs" look, and had a real bug — every leg face sampled
  its cell's top highlight strip, washing the legs out to near-white).
  - *Face*: the full-black face plate and protruding eye-cubes are gone. The face is `#898989`
    chitin with a dark **eye band** across the upper half holding a structured jumping-spider
    eye arrangement — 2 large primaries (black socket, `#FF3B1E` body, hot core + glint),
    2 medium outer eyes continuing around onto the side faces as lateral eyes, 2 small dim top
    eyes. Below: mandible creases and fang-root shadows.
  - *Legs*: dark `#5A5A5D` with two `#3A3A3E` joint rings, a slightly lighter femur segment,
    bristle speckle, and near-black claw ends. Deliberately **longitudinally symmetric** (per-
    face U direction flips between north/south faces, so an asymmetric strip would render
    reversed on half the faces). A tried-and-removed variant had 1px red joint gleams — they
    repeated across all four faces of all eight legs and read as scattered wound-splatter.
  - *Abdomen*: base darkened one step (`#6E6E72`) so the rear reads heavy; the organic red
    blotch marking (the one element that never drew negative feedback) is kept but now drawn
    via a **mirror-symmetric overlay** (organic jitter, but bilaterally symmetric like a real
    spider's marking — the previous fully-random placement read as ketchup splatter in preview).
    Flanks get faint hair striping + two satellite dashes; rear face gets a wrapped-down blotch
    and a spinneret hint.
  - Palette unchanged in its pinned decisions: primary `#898989`, ONE red family (`#FF3B1E`
    ± brightness). New neutrals only: `#6E6E72`, `#3A3A3E`, `#22..24`, bone-pale `#C6C2BA`
    fang tips.
- **Geometry**: vanilla-accurate skeleton kept, head stays a plain cube (per the two failed
  chamfer attempts — the menace comes from the painted face now). One addition: two **fang
  bones** (`left_fang`/`right_fang`, chelicerae, 1.5x4x1.5 each) parented to the `head` bone —
  they follow head-turn automatically and are animated. GeckoLib's `GeometryTree` parent-field
  support confirmed from its source.
- **Animations** (`tools/gen_spider_queen_anims.py`, new — generates the animation JSON so the
  8 legs' mirrored signs come from one gait table): `idle` = breathing bob + abdomen pulse +
  slow head scan + creepy fang chew + one impatient front-leg tap; `walk` = alternating-
  tetrapod gait with swing-phase leg lifts; **`leap_windup`** (0.75s, must stay equal to
  `SpiderQueenEntity.LEAP_WINDUP_DURATION_TICKS`) = crouch + abdomen cock + face tilts onto
  the target + front legs raised high in a threat pose + fang flare + tremor + final recoil;
  **`leap_flight`** = nose-up launch snap with all legs swept trailing, then the front two leg
  pairs whip forward mid-flight to grab, holding the final frame until landing.

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

### 18.2 Entity model and texture: bespoke muscular geometry (playtest fix, v2)

**v1 reused vanilla's exact `BipedEntityModel.getModelData()` unmodified** (just scaled 3x). Direct
playtest feedback ("that zombie looks like it has no muscles") confirmed this was a real geometry
problem, not a texture-painting problem: a uniformly-thin biped skeleton can't read as "hulking and
strong" regardless of how its texture is painted. **v2 replaces the geometry with bespoke, visibly
bulkier cuboids** - broader chest/shoulders, thicker arms, thicker legs - while deliberately keeping
the exact same standard part names/hierarchy (`head` with child `hat`, `body`, `right_arm`,
`left_arm`, `right_leg`, `left_leg`) that `BipedEntityModel`'s constructor binds via
`ModelPart.getChild(name)`. Only each cuboid's size/origin changed; the inherited
`AbstractZombieModel`/`BipedEntityModel` walk-cycle and attack-swing angle math (which only ever
rotates/repositions these named parts, never assumes a specific size) keeps working unmodified -
the same "custom geometry, but same part-naming contract" approach already established for the two
stone mini-bosses' shared `HulkingCocoonStoneEntityModel`. Total model height (head top at y=-8 to
feet at y=24) is kept identical to vanilla's own biped so the boss doesn't clip into the ground
(confirmed ground-level convention: `LivingEntityRenderer`'s fixed `-1.501`-block translate, see
Section 13.2's rationale).

- **File:** `assets/baum2/textures/entity/zombie_colossus.png`, still 64x64 px, but now painted
  against the model's own bespoke UV footprints below (verified against the actual box-UV math in
  `ModelPart.Cuboid`'s constructor, decompiled 1.21.11 client jar, not assumed) rather than
  vanilla's stock biped layout.

  | Part | Cuboid size (dx,dy,dz) | Origin (pivot) | UV origin | Box footprint (x, y, w, h) | Notes |
  |---|---|---|---|---|---|
  | Head (inner) | 8 x 8 x 8 | (0, 0, 0) | (0, 0) | (0, 0, 32, 16) | Unchanged from v1 - deliberately small relative to the new bulkier body, itself part of the "brute" silhouette. Front face: eye sockets + amber glare dots, jaw line, tusks |
  | Hat (outer head overlay) | 8 x 8 x 8, dilated +0.5 | child of head, `ModelTransform.NONE` | (32, 0) | (32, 0, 32, 16) | **Left fully transparent**, matching vanilla `zombie.png`'s own convention |
  | Body | 14 x 12 x 6 (was 8x12x4) | (0, 0, 0) | (0, 16) | (0, 16, 40, 18) | Broad barrel chest/shoulders - the primary "muscular" silhouette change. Front face: sternum groove + pec/ab highlight-and-shadow striations |
  | Arm (shared/mirrored, both arms) | 6 x 14 x 6 (was 4x12x4) | right (-7, 2, 0), left (7, 2, 0) | (40, 16) | (40, 16, 24, 20) | Thick, slightly longer club-swinging limb, flush against the body's new wider edge. Front face: bicep bulge highlight + elbow-crease shadow line |
  | Leg (shared/mirrored, both legs) | 6 x 12 x 6 (was 4x12x4) | right (-2.9, 12, 0), left (2.9, 12, 0) | (0, 34) | (0, 34, 24, 18) | Thick pillar legs, height unchanged so feet still land exactly at y=24 (no floor clipping). Front face: tattered-wrap fold lines + ragged hem |

  Arm and leg regions remain genuinely shared between left/right via `.mirrored()` (same vanilla
  mechanism as before) - painting one side's region paints both limbs automatically. Canvas stays
  well within 64x64 (tallest used row ends at y=52, 12px of the canvas below that is unused/
  transparent).

**Muscle-definition shading, added this pass per the brief** ("touch up the palette with visible
muscle-definition shading if that helps sell it further"): the body's front face now has a dark
sternum groove flanked by highlight blocks (pecs) and two horizontal shadow striations across the
midsection (abs), each followed by another highlight block below - reads as segmented muscle mass
rather than a flat fill. The arm's front face has a highlight block near the shoulder (bicep bulge)
and a dark line partway down (elbow crease) separating it from a shadow block below (forearm). This
is still a **placeholder-effort treatment** (flat programmatic fills plus a handful of hand-placed
accent rectangles, generated via a small Java/`ImageIO` tool - no Python/ImageMagick available in
this environment, same placeholder-effort level as every other texture in this document), not
hand-painted surface detail - a human artist pass would meaningfully raise the ceiling here, same
caveat as 18.5 below.

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
| Wound/seam accent | Wound Edge | `#1F1A16` | Near-black - sternum groove, ab striation lines, jaw line, elbow-crease line, ragged hems (torso and leg) |
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

**Addendum (2026-07-06): 3D in-hand model.** When a player holds the club it now renders as a
real 3D cuboid model echoing the boss's own club (faceted wood shaft with leather grip bands,
lumpy two-cube head, two metal studs, blood smear - this same palette), generated by
`tools/gen_colossal_warclub_item.py`: a vanilla element model
(`models/item/colossal_warclub_in_hand.json` + 32x32 `textures/item/colossal_warclub_3d.png`)
authored vertically and laid along the sword-sprite diagonal via a -45° per-element Z rotation,
so stock `item/handheld` display transforms hold it correctly (~1.7 blocks of visual club - a
hefty two-hander at player scale). The item-model definition (`items/colossal_warclub.json`)
selects by display context - vanilla's own trident.json schema, verified from the 1.21.11
client jar - so **the approved flat 16x16 icon above is unchanged in GUI/ground/fixed/
on_shelf**; only the held contexts use the 3D model.

### 18.6 Playtest fixes (v2): animation diagnosis + leap/rage telegraph poses

The user actually fought this boss (not just build/compile-checked it) and reported: (1) "the
attack and jump animation are missing, the zombie model is moving very static," and (2) "that
zombie looks like it has no muscles" (18.2/18.3 above cover the muscle fix). This subsection covers
the animation diagnosis and the telegraph-pose fix.

**Diagnosis, verified against the decompiled 1.21.11 client jar rather than guessed** (not the
older-MC-version-recall this project's `HANDOFF.md` repeatedly warns against): the base walk-cycle
and attack-swing plumbing v1 already had was **not actually broken**. Confirmed by decompiling
`LivingEntityRenderer.updateRenderState` (populates `limbSwingAnimationProgress`/
`limbSwingAmplitude` generically for every mob, not just Bipeds - reached via `super.
updateRenderState()`, since `MobEntityRenderer` doesn't override it) and `BipedEntityRenderer.
updateBipedRenderState`'s actual body (the static helper this boss's renderer already called in
v1) - its very first line calls `ArmedEntityRenderState.updateRenderState`, which sets
`handSwingProgress`/`swingAnimationType` from `entity.getHandSwingProgress(tickDelta)`. Both were
already correctly wired; `AbstractZombieModel.setAngles` → `ArmPosing.zombieArms` already reads
`handSwingProgress` to drive the swing on every real `swingHand()` call (`ColossusAttackGoal`/
`RageAttackGoal` both already call it). **One real, fixable gap found**: vanilla's `MobEntity.
isAttacking()` is only ever set `true` by vanilla's own `MeleeAttackGoal`/`ZombieAttackGoal`
machinery, which this boss's fully custom attack `Goal`s never call (and per this task's scope,
that gameplay code wasn't to be touched) - so `state.attacking` always read `false`, meaning every
swing used `ArmPosing.zombieArms`' calmer non-attacking pose baseline even mid-strike. **Fixed
purely client-side**, no gameplay code touched: `ZombieColossusEntityRenderer.updateRenderState`
now also treats an in-progress hand swing as "attacking" (`state.attacking = entity.isAttacking()
|| state.handSwingProgress > 0.0F`).

The bigger, real contributor to "very static": the leap's 10-tick wind-up and the rage combo's
8-tick wind-up were **completely frozen** in v1 - navigation stopped, zero pose change, for up to
half a second each. Combined with the boss's intentionally slow 0.5-attacks/sec base cadence
(gameplay-approved, not touched here), long stretches of a fight showed no visible motion at all.
Fixed with real telegraph poses, same pattern already proven for Spider Queen's leap crouch
(`SpiderQueenRenderState`/`SpiderQueenEntityModel`):

- **New `ColossusRenderState`** (extends vanilla's `ZombieEntityRenderState`, not plain
  `LivingEntityRenderState` - this boss's model still needs the zombie-specific `attacking` field
  and every biped/armed field for its base animation) carries `leapWindupTicks`/`rageWindupTicks`,
  populated in the renderer from `ZombieColossusEntity.getLeapWindupTicks()`/
  `getRageWindupTicks()` (already wired server-side, synced via `TrackedData<Integer>` - the exact
  same mechanism `SpiderQueenEntity.LEAP_WINDUP_TICKS` established).
- **`ZombieColossusEntityModel.setAngles`** now layers two poses on top of the inherited walk/
  attack animation, both easing in as their counter counts toward 0 and resolving to neutral right
  as the real strike/launch fires server-side:
  - **Leap wind-up** ("prepare to jump"): crouches the torso/head down, bends both legs, and winds
    the main (club) arm back and up - a coiled, about-to-spring read.
  - **Rage wind-up** ("prepare to slam"): raises the main arm high overhead (off-arm follows
    partway for a two-handed read) with a slight backward torso lean - an overhead club-raise read.
  - Duration constants (10 and 8 ticks) are duplicated as client-only constants in the model class
    rather than exposed from the entity, since the real constants live on `private static final
    int WINDUP_TICKS` fields inside `ZombieColossusEntity`'s private inner `Goal` classes, and this
    task's scope explicitly excludes touching that gameplay code.

**Not yet independently re-verified in an actual game session by this pass** (no GUI-automation
tool exists in this environment, per every other UI/animation fix logged in `HANDOFF.md`) - next
playtest should confirm: normal walking limb-swing while chasing, a visible club swing on every
base attack and rage strike, the new crouch-and-coil pose during the leap wind-up, and the
overhead-raise pose during the rage wind-up, both resolving right as the attack actually fires.

### 18.7 Files produced this pass

Bespoke model geometry/UV rework (v2) plus the v1 placeholder texture regenerated against the new
UV footprints, and two new client-only render classes for the telegraph poses. Texture generated
via a small Java/`ImageIO` command-line tool (no Python/ImageMagick available in this environment -
same placeholder-effort convention as every other texture in this document, just a different tool
than the PowerShell/`System.Drawing` approach used for v1). No traced, extracted, or downloaded
source material was used.

- `assets/baum2/textures/entity/zombie_colossus.png` (64x64, placeholder - flat box-UV fills plus
  hand-placed muscle-striation/eye/jaw/elbow-crease accent pixels, regenerated against the new
  bespoke UV footprints in 18.2; same placeholder effort level as every other texture in this
  document, now with added muscle-definition shading per this pass's brief)
- `src/client/java/de/baum2dev/baum2/entity/ZombieColossusEntityModel.java` (real Java, rewritten -
  bespoke bulkier geometry replacing v1's unmodified `BipedEntityModel.getModelData()` reuse, plus
  the leap/rage telegraph-pose logic)
- `src/client/java/de/baum2dev/baum2/entity/ZombieColossusEntityRenderer.java` (real Java, updated -
  now creates/populates `ColossusRenderState` instead of vanilla's plain `ZombieEntityRenderState`,
  plus the `state.attacking` animation fix)
- `src/client/java/de/baum2dev/baum2/entity/ColossusRenderState.java` (new, real Java - carries the
  leap/rage wind-up counters, same pattern as `SpiderQueenRenderState`)

**Unchanged from v1, not re-touched this pass:** `assets/baum2/textures/item/colossal_warclub.png`,
`assets/baum2/models/item/colossal_warclub.json`, `assets/baum2/items/colossal_warclub.json`,
`Baum2Client.java`'s registration block (still calls `ZombieColossusEntityModel::getTexturedModelData`/
`ZombieColossusEntityRenderer::new` with the same `ModelTransformer.scaling(3.0F)` wrapper - no
signature changes were needed there).

**Not yet done, flagged for a future art pass** (same caveat as every prior placeholder in this
document): real hand-drawn surface detail (skin texture, fabric weave on the leg-wrap, wood grain
on the club) - this pass proves the new bulkier UV layout, establishes muscle-definition shading,
and adds the telegraph poses, but a human artist pass would meaningfully raise the ceiling here.

---

## 19. Boss visual identity: "Drevathis, the Cursed Sovereign" (`baum2:drevathis`) and "Drevathis's Cursed Blade"

The mod's **current top-tier boss** (level 40, above Zombie Colossus's 25) - an ancient cursed/
demonic sovereign, not another oversized-common-enemy variant like Sections 17/18. Unlike Spider
Queen and Zombie Colossus (both reused/rescaled a vanilla shared model), `DrevathisEntityModel` is
a fully bespoke `BipedEntityModel` subclass built from scratch for this boss - tall biped body,
two backward-curving horns, a trailing cape - so this section's job covers both a wholly new
model's texture *and* the palette, not just a re-theme of borrowed geometry. Its passive
(darkens nearby players' vision via vanilla's Darkness effect, even in daylight) and its skills
("Dash of Death," "Chain of Death," "Wave of Darkness," "Thunder of Darkness") set a **dark,
regal, ancient-evil register** - deliberately distinct from Zombie Colossus's brutish/feral
"Ashen Brute" register (Section 18.3), even though both are "boss" tier.

### 18.7 GeckoLib rework (2026-07-06): bespoke muscular model, real geometry club, full animation set, Earthquake skill

The user asked for a full visual rework ("the current model is ugly... giant zombie with big
muscle... the big club should be also reworked... everything visually more beautiful,
animation, model"). Zombie Colossus is now the second GeckoLib mob (after Spider Queen,
Section 17.6), built with the same see-before-you-ship pipeline
(`tools/render_geckolib_preview.py`) - every shape/pose below was reviewed as a rendered image
before landing.

- **Geometry** (`tools/gen_zombie_colossus.py`, new; old `ZombieColossusEntityModel.java`
  deleted): original muscular biped, no vanilla-biped part reuse - boulder skin-capped
  shoulders, barrel chest with a hunched upper-back mass, underbite jaw with painted tusks
  (Bone `#D8CFC0`), oversized gorilla forearms on **two-segment arms** (shoulder bone ->
  forearm child, so elbows bend in animations - single-stick limbs were a root cause of the
  old "very static" read), thick wrap-clad pillar legs with forward feet. **The Colossal
  Warclub is real model geometry now** - a `club` bone parented to `right_forearm` (shaft +
  lumpy two-cube head + two studs, Section 18.4's exact wood/stud/smear palette), carried
  angled forward-down-outward at the hip, ready to swing (a first back-over-the-shoulder
  carry was user-rejected: "the weapon is showing in the wrong direction") - replacing the
  old scaled held-`ItemStack` render (the
  entity now equips nothing; the item drop is unchanged). The item icon
  (`colossal_warclub.png`) was NOT retouched - Section 18.4's approved icon still matches the
  new 3D club's palette.
- **Texture**: pixel-art atlas (246x72, 2px/unit, per-face painting), Section 18.3's ratified
  "Ashen Brute" palette unchanged - ashen-hide head/back/shoulders with scars + crude
  stitches, exposed red-brown musculature (fiber striations, chunky pec masses, 2x3 ab grid
  with wound-edge creases, a torn skin patch on the back), tattered leg wraps with ragged
  alpha-notched hems, asymmetric dull-amber eyes (one large, one dim) under a heavy brow.
- **Animations** (`tools/gen_zombie_colossus_anims.py`, new): idle (breathing, slow scan,
  club shoulder-bounce), heavy stomping walk (footfall body dips, counter-swinging arms, club
  inertia wobble), `smash` (club overhead -> slam, impact keyed to the server's 6-tick damage
  delay), `rage` (two-handed overhead combo, three impacts keyed to the goal's exact
  8/13/17-tick strikes), `leap_windup`/`leap_flight` (crouch-coil, then club raised airborne),
  and `earthquake` (see below). One-shots are GeckoLib server-triggered animations; leap poses
  are state-driven off synced TrackedData - both mechanisms documented in the entity class.
- **New skill "Earthquake"** (user-specified: 100 damage, 18s cooldown): sky-high two-handed
  wind-up (0.75s telegraph), then the club slams the ground - all players within 9 blocks take
  the hit and get bucked upward, and for the next 0.6s expanding rings of `DUST_PILLAR`
  particles erupt from the actual ground blocks (the mob-independent "ground jumping" particle
  vanilla's mace smash uses - purely cosmetic, no blocks are modified) while players in the
  radius get small velocity jolts so the shaking is physically felt. Sound: heavy mace
  ground-smash + low explosion. Skill name is currently code-internal only (no UI string).

### 19.1 Why this palette direction

Every hostile-mob/boss palette already in this document leans on one of: sickly/toxic green
(Sections 13.3, 15.2, 17.3), deep violet-and-gold "regal insect" (Section 17.4), or ashen
brown-and-exposed-red-muscle (Section 18.3). None of those fit "ancient cursed sovereign, cold
and regal, associated with darkness" - so this section introduces **two new hue families not yet
used as a dominant tone anywhere else in this document**: a cool slate-gray "cursed flesh" and a
near-black wine-plum "shroud/robe," both accented by a pale icy cyan-white "curse glow" (used for
eyes, rune markings, and the sword's fuller) and a deep wine-crimson "sovereign's blood" trim.
The icy pale-cyan glow is the deliberate signature choice: every prior mob's eye-glow/bioluminescent
accent in this document (Larval Glow `#C4E064`, Toxic Eye `#E8FF6B`, Brute Glare `#D9C24A`) sits in
a yellow-green-to-amber family reading as "toxic/diseased." Drevathis's glow reads as "cold/
spectral/cursed" instead - a different genre register entirely, matching the Darkness-effect
lore rather than reusing the mod's existing "toxic monster" visual language.

*Compliance note:* "dark regal undead/demon sovereign with cold spectral glow" is a broad,
unclaimed genre convention (ancient cursed royalty/liches/demon lords with an eerie glow accent
appear across countless unrelated games, books, and films), not IP tied to any one game. The
exact palette below was checked hex-by-hex against every existing palette in this document
(Sections 2, 3.3, 11, 12, 13.3, 15.2, 17.3, 17.4, 18.3) and shares no hex values with any of
them; see the per-table compliance notes below for the closest neighbors and why they remain
distinguishable.

### 19.2 Entity model and texture: bespoke biped, 64x64 (playtest fix, v2)

**v1** (unmodified vanilla-biped-proportioned cuboids, a plain rectangular cape, no claws) was
actually playtested and reported back bluntly: **"the demon boss... looks like a hobbit."** The fix
had two halves, both documented here since they're inseparable in practice - a texture pass alone
can't sell "cursed demon sovereign" on vanilla-biped proportions, and new geometry alone reads as
flat/plain without matching surface detail:

1. `DrevathisEntityModel` (Java) moved off vanilla-biped proportions: broadened chest/arms, much
   longer/more dramatic back-swept horns, a substantially bigger trailing cape, and small clawed
   fingertips on both hands (see that file's own v2 doc-comment for the exact cuboid deltas).
2. This section's texture is **regenerated from scratch against the new UV layout** below (not
   patched in place - the old UV layout no longer lines up with the new cuboids at all), and pushed
   substantially further on surface detail: glowing rune-crack patterns across skin and robe, an
   asymmetric/jagged (not plain-rectangle) cape hem, stronger 3-band shadow-shading on every major
   face to sell the broadened silhouette, a larger/brighter glowing-core eye treatment, and a new
   thin "circlet" band on the hat layer for a cheap extra sovereign/crown cue.

- **File:** `assets/baum2/textures/entity/drevathis.png` (placed under
  `src/client/resources/assets/baum2/...` - see the note in Section 6 on why this one entity
  texture lives in the client source set rather than main).
- **Canvas:** 64x64 px, matching `DrevathisEntityModel`'s `TexturedModelData.of(modelData, 64, 64)`
  registration exactly.
- **UV layout** (derived directly from the box-UV cuboid sizes/origins given in this boss's own
  model class, using the standard Minecraft box-UV unwrap formula - not guessed; re-derived in full
  for v2, since every part except the head/hat changed size and/or UV origin):

  | Part | Cuboid size (dx,dy,dz) | UV origin | Box footprint (x, y, w, h) | Notes |
  |---|---|---|---|---|
  | Head (inner) | 8x8x8 | (0, 0) | (0, 0, 32, 16) | Unchanged from v1 |
  | Hat (outer overlay) | 8x8x8 | (32, 0) | (32, 0, 32, 16) | **v2:** no longer fully transparent - carries a thin circlet band, see below |
  | Body | 9x13x5 (was 8x12x4) | (0, 16) | (0, 16, 28, 18) (was 16,16,24,16) | Broader robed torso |
  | Cape | 10x18x1 (was 9x16x1) | (28, 16) | (28, 16, 22, 19) (was 24,32,20,17) | Substantially larger; moved next to the body in UV space |
  | Arms (shared/mirrored) | 5x13x5 (was 4x12x4) | (0, 36) | (0, 36, 20, 18) (was 40,16,16,16) | Broader sleeve + bare hand at the wrist |
  | Legs (shared/mirrored) | 4x12x4 | (20, 36) | (20, 36, 16, 16) (was 0,16,16,16) | Robe leg-wrap, size unchanged, UV origin moved |
  | Horn (right) | 2x9x2 (was 2x6x2) | (36, 36) | (36, 36, 8, 11) (was 0,32,8,8) | Longer, more dramatic back-swept horn |
  | Horn (left) | 2x9x2 (was 2x6x2) | (44, 36) | (44, 36, 8, 11) (was 8,32,8,8) | Second horn, mirrored position |
  | Claw (right, **new in v2**) | 3x2x2 | (52, 36) | (52, 36, 10, 4) | Small clawed fingertip |
  | Claw (left, **new in v2**, shares the claw UV box mirrored) | 3x2x2 | (52, 36) | (52, 36, 10, 4) | Same UV box as the right claw, mirrored at render time - same sharing convention arms/legs already use |

  All regions still fit the 64x64 canvas with no overlap (tallest region, the arms, ends at y=54;
  remaining canvas space stays unused/transparent margin, same convention as Section 18.2's
  `zombie_colossus.png`).

- **Face-by-face treatment**, following this document's established "top=highlight, bottom=shadow,
  front=detail, sides=mid-tone" convention (Sections 13.3, 18.2), with v2's added detail called out:
  - **Head:** top/back = pale/shadow Cursed Hide tones. Front face carries a 2x2 hollow-socket
    patch per eye (up from a single flat dot in v1) with a bright Grave Frost core pixel plus a
    Grave Frost Dim "bleed" pixel beside it for a visibly glowing (not just colored-in) eye, a
    Sovereign Blood jaw-line accent, and **(v2)** one short asymmetric Grave Frost crack per face
    (front/right/left/back each get a differently-shaped crack, not a mirrored pair) - painted
    eye-glow and cracks both, no vanilla overlay involved (this model has no vanilla eye-feature-
    renderer to rely on, same situation Section 17.3 already documented for Spider Queen).
  - **Hat (v2 addition):** a thin 2px Sovereign Blood "circlet" band wraps the right/front/left/back
    faces at brow height, with a Grave Frost + Grave Frost Pale gem accent centered on the front -
    a crown/coronet read that needed no new geometry, sitting on the already-existing (if
    previously blank) hat overlay layer.
  - **Body:** Shroud tones with **(v2)** an explicit 3-band vertical gradient (Shroud Pale top /
    Shroud mid / Shroud Void bottom) on every side face, to sell the new 9-wide broadened chest's
    contour instead of a flat fill reading as flat. A widened 2px Sovereign Blood sash runs down
    the front, flanked by two small plus-shaped Grave Frost rune marks, each now trailing its own
    asymmetric crack toward the sash (the left one longer than the right - deliberately not
    mirrored), plus one extra crack each on the side faces.
  - **Cape (the headline fix):** an irregular **per-column hem cutoff**, cut via alpha rather than
    left as a straight rectangle edge, so the cape reads as a jagged/tattered "sovereign's torn
    cloak" silhouette instead of a plain rectangle - the concrete fix for "looks generic." Same
    3-band vertical shading as the body, Sovereign Blood edge piping down both long edges, the
    existing small Sovereign Blood diamond crest with a Grave Frost accent dot near the top, and
    **(v2)** two new asymmetric Grave Frost crack lines running from near the crest down toward the
    hem at different lengths/paths on the left vs. right half.
  - **Arms:** Shroud sleeve on the upper ~2/3 of the front face, Cursed Hide (bare skin) on the
    lower ~1/3 (hand/wrist showing), separated by a thin Sovereign Blood cuff band - **(v2)** now
    with a small Grave Frost crack + Grave Frost Dim bleed pixel painted directly onto the bare
    hand/wrist on the front face (the curse-glow bleeding into flesh itself, not just the robe) and
    one crack on the back face.
  - **Claws (new in v2):** small Cursed Hide Shadow fill with a single Grave Frost Dim glint on
    each fingertip's front face - the cheapest possible palette-level cue that these are claws, not
    human fingers.
  - **Legs:** Shroud tones with a Sovereign Blood hem trim line near the bottom, **(v2)** now with
    the same 3-band vertical shading as the body/arms and one small Grave Frost Dim crack accent on
    the front face only (kept sparse - legs are the smallest, least-seen faces, so a single accent
    stays legible rather than over-cluttering a tiny UV area).
  - **Horns:** Cursed Bone tones (not the flesh/shroud palette - horns are a separate bone-like
    material). **(v2)** the tip glow widened from a single accent dot to a full 2px Grave Frost Dim
    band near the tip end, a clearer "the tip glows" read now that the horns are 50% longer.

### 19.3 Color palette: "Abyssal Sovereign"

| Role | Name | Hex | Notes |
|---|---|---|---|
| Flesh shadow | Cursed Hide Shadow | `#1E2028` | Head bottom/back faces |
| Flesh mid-tone | Cursed Hide | `#3E4250` | Head side faces; bare hand/wrist on the arms |
| Flesh highlight | Cursed Hide Pale | `#656B7D` | Head top face |
| Robe shadow | Shroud Void | `#1B0C14` | Body/arm/leg bottom faces; cape back face and hem borders |
| Robe mid-tone | Shroud | `#331A26` | Dominant robe/cape/sleeve fill |
| Robe highlight | Shroud Pale | `#4F2B3D` | Body/arm/leg top faces; cape attachment sliver |
| Regal trim / accent | Sovereign Blood | `#6B1330` | Sash, cuff band, hem trim, cape edge piping, jaw-line accent, cape emblem |
| Curse glow (signature) | Grave Frost | `#AEE8F5` | Eye-glow dots, chest rune marks, cape emblem accent dot |
| Curse glow, dim | Grave Frost Dim | `#4E7A8C` | Horn-tip glow accent (subtler than the eye/rune glow) |
| Curse glow, pale (glint) | Grave Frost Pale | `#D9F5FA` | Reserved for sparse bright highlight pixels only (sword pommel/tip - Section 19.4) |
| Eye-socket base | Hollow Socket | `#0A0710` | Dark base under each painted eye-glow dot |
| Bone (horns) | Cursed Bone | `#5C5340` | Horn mid-tone |
| Bone shadow (horns) | Cursed Bone Dusk | `#2B2620` | Horn bottom/back faces |

*Compliance note:* checked hex-by-hex against every palette already in this document. Nearest
neighbors and why they stay distinguishable: Shroud's near-black wine-plum family (`#1B0C14`/
`#331A26`/`#4F2B3D`) is deliberately warmer/more red-shifted than both Royal Carapace's saturated
blue-violet (`#4B2170`/`#7A46A6`, Section 17.4 - much lighter and more saturated, a genuinely
different violet) and Astral rarity's indigo undertone (`#2E2A5C`, Section 2.4 - that one is
blue-leaning, Shroud is red-leaning); Sovereign Blood (`#6B1330`) is a cooler, more magenta-shifted
crimson than both the Life bar's warm coral-ember (`#E2574B`, Section 11) and Exposed Muscle's
brick-brown-red (`#7A2E24`, Section 18.3); Grave Frost (`#AEE8F5`) is a paler, whiter, more
blue-shifted cyan than Rune Cyan (`#7FD8E0`, Section 2.2 - noticeably more saturated/green) and a
different hue family entirely from Astral's lavender-white (`#D9CFFF` on indigo, Section 2.4).
Cursed Hide's cool blue-gray family is the inverse of Ashen Brute's warm brown-gray (Section
18.3's `#332C22`/`#5C5142`/`#7D715C` all have R>G>B; Cursed Hide's `#1E2028`/`#3E4250`/`#656B7D`
all have B>R>G) - same "ashen dead flesh" genre idea, opposite temperature, so the mod's two
"long-dead skin" bosses don't read as recolors of each other.

### 19.4 Weapon visual identity: "Drevathis's Cursed Blade" (`baum2:drevathis_cursed_blade`) (playtest fix, v2)

**v1's** straight, parallel-edged diagonal blade was reported back just as bluntly as the entity:
**"the weapon does not look like it comes from a demon cursed blade"** - the silhouette read as a
generic straight iron sword despite already using the boss's own palette; palette alone wasn't
doing the job, execution needed to change. **v2 keeps the exact same six hexes** (nothing in the
palette was the problem, only the shape/detail built from it) and pushes the *silhouette* itself
much further:

Follows Section 14/16/18.4's exact item conventions (plain `Item`, `minecraft:item/handheld`
parent, the same `assets/baum2/items/<name>.json` + `assets/baum2/models/item/<name>.json` entry-
point pair) - **unchanged in v2**: the model/parent JSON was reviewed, not modified. Vanilla's
`minecraft:item/handheld` (inheriting `item/generated`) already extrudes the flat icon texture
into a thin 3D shape using that texture's own alpha silhouette - the same built-in mechanism every
vanilla sword icon's in-hand "thickness" comes from. That means a jagged, asymmetric *alpha* shape
in the 16x16 texture alone is sufficient to read as a jagged 3D silhouette in hand, in the GUI, and
on the ground; no model/geometry change was necessary to fix this complaint, only the texture.

The item still has 0 base combat stats (pure support - its value is the on-hit dark AoE wave proc,
not stat lines), so the visual still has to carry the "this is special" read entirely through
silhouette/palette rather than any stat-comparison affordance.

- **Silhouette (v2):** same established bottom-left-pommel-to-top-right-tip diagonal convention as
  Gold Sword/Poison Dagger/Colossal Warclub, but the blade's two edges are now **deliberately
  asymmetric** rather than a mirrored taper: the lower/left edge is a smooth, clean taper (the
  "cutting edge"), while the upper/right edge's width oscillates row-by-row (2/1/2/1/2/1/2/3 rather
  than a straight line), reading as a chipped, jagged, unnatural spine rather than a factory-clean
  blade. A continuous Grave Frost fuller/glow groove still runs the blade's centerline, now joined
  by **two separate Sovereign Blood crimson vein-branch pixels** breaking off the fuller partway
  down the blade - both signature glow colors visible on the blade at once, not just cyan alone.
  The crossguard is no longer a straight symmetric bar: it keeps its Sovereign Blood flare and two
  Ebon Steel Sheen accent pixels, but now also grows a **curling, asymmetric hook-horn accent on
  one side only** (a Sovereign Blood shaft ending in an Ebon Steel Sheen glint at its curled tip) -
  the concrete "wicked curved hilt" execution the brief asked for. The pommel gets one deliberate
  notch cut out of its block shape so even the hilt end reads as slightly irregular rather than a
  clean rectangle. Deliberately shares its palette with the boss's own texture (Sovereign Blood
  crossguard/pommel/hook, Grave Frost fuller and vein-branches) so the blade the boss wields
  (rendered ~1.8x oversized by `DrevathisHeldWeaponFeatureRenderer`) and the same blade as a
  dropped/held item read as one consistent object at both scales, rather than two independently-
  designed weapons that happen to share a name.
- **Parts, bottom-left to top-right:** a Sovereign Blood pommel (with one notch cut out and a
  single Grave Frost Pale glint pixel at the very corner), a short dark Shroud grip, a Sovereign
  Blood crossguard flare (two Ebon Steel Sheen accent pixels, plus the asymmetric curling hook
  accent described above extending off the upper side only), then the jagged tapering blade -
  Ebon Steel Sheen edge pixels on a jagged/asymmetric outline, Ebon Steel fill, a continuous Grave
  Frost fuller line down its center with two Sovereign Blood vein-branch pixels breaking off it,
  narrowing to a single-pixel taper at the tip, ending in a Grave Frost Pale glint pixel at the very
  point.

| Role | Hex | Notes |
|---|---|---|
| Pommel / crossguard / trim | `#6B1330` (Sovereign Blood) | Reused from the boss's own palette exactly, for cross-object cohesion |
| Grip | `#331A26` (Shroud) | Reused from the boss's own palette |
| Blade base / edge | `#14151C` (Ebon Steel) | New - near-black cold steel, distinct from Colossal Warclub's warm wood-brown (`#3E2A1A`/`#5A3D24`) and Gold Sword's warm bronze-gold |
| Blade edge sheen | `#3A3D4A` (Ebon Steel Sheen) | New - a cool steel highlight, close in value but distinct hue role from Cursed Hide (kept as a separate named role since it's steel, not flesh) |
| Fuller / curse-glow groove | `#AEE8F5` (Grave Frost) | Reused from the boss's own palette exactly |
| Glint (pommel + tip) | `#D9F5FA` (Grave Frost Pale) | Reused from the boss's own palette exactly |

*Compliance note:* this is an original 6-color diagonal-greatsword treatment sharing its palette
family with Section 19.3's Abyssal Sovereign entity palette by design (not accidental overlap) -
does not reproduce any existing game's specific sword icon, and shares no hex values with any of
this mod's three other existing weapon palettes (Gold Sword's bronze/gold, Poison Dagger's
green-tinged steel, Colossal Warclub's wood-and-stud brown).

### 19.5 Files produced this pass (all explicitly temporary placeholders)

Per `MASTERPROMPT.md`'s asset rule - flat-color/gradient pixel fills generated programmatically
(Python + Pillow), not hand-drawn final art. No traced, extracted, or downloaded source material
was used. **v2 regenerates both PNGs from scratch** (not patched in place) against the new UV
layout/silhouette described in 19.2/19.4; the two item-model-definition JSON files below were
reviewed and are unchanged (no model/geometry change was needed for either fix - see 19.4's
alpha-silhouette note for why the sword didn't need one).

- `assets/baum2/textures/entity/drevathis.png` (64x64, placeholder - flat/gradient box-UV fills
  plus crack/eye-glow/jagged-cape-hem accent pixels per the v2 face-by-face treatment in Section
  19.2; regenerated against the new UV footprints in `DrevathisEntityModel` v2; placed under
  `src/client/resources/...`, see the Section 6 note on why)
- `assets/baum2/textures/item/drevathis_cursed_blade.png` (16x16, placeholder - jagged/asymmetric
  blade silhouette with a curling hook-guard accent, per Section 19.4 v2)
- `assets/baum2/models/item/drevathis_cursed_blade.json` (unchanged - reviewed, still the correct
  `minecraft:item/handheld` parent pattern as every other weapon in this document; its built-in
  alpha-based flat-item extrusion is what makes the jagged v2 silhouette work with no model edit)
- `assets/baum2/items/drevathis_cursed_blade.json` (unchanged - the 1.21.11 item-model-definition
  entry point, required alongside the file above per the gotcha Section 14.3 already verified
  against the decompiled vanilla client jar)

**Not yet done, flagged for a future art pass** (same caveat as every prior placeholder in this
document): real hand-drawn surface detail (cape fabric weave, horn texture/ridges, robe folds,
blade edge highlights beyond the flat/gradient fills used here) - this pass substantially raises
the "reads as a cursed demon sovereign, not a reskinned player" bar per direct playtest feedback,
but a human artist pass would still meaningfully raise the ceiling here given this boss's
"current top-tier" status.

### 19.6 Why this v2 pass exists: playtest feedback

The user actually played against this boss (not just build-checked it) and gave two blunt, distinct
complaints, both addressed above:

1. **"the demon boss has no weapon and looks like a hobbit"** - the "no weapon" half was a separate
   real code bug (a missing `initEquipment()` call meant the sword was never actually equipped),
   not a visual-design issue, and isn't this agent's concern. The **"looks like a hobbit"** half is
   the visual complaint this pass addresses: the boss read as a small, plain, generic humanoid, not
   an imposing cursed demon lord. Fixed by (a) the model geometry broadening/lengthening described
   in `DrevathisEntityModel`'s own v2 doc-comment, and (b) this document's Section 19.2 texture
   rework - stronger shadow-shading, glowing rune-cracks, a jagged cape hem, a brighter glowing-eye
   treatment, and a new crown-band cue - so the silhouette and surface detail both stopped reading
   as "reskinned player."
2. **"the weapon does not look like it comes from a demon cursed blade"** - addressed entirely in
   Section 19.4: the palette wasn't the problem (kept exactly as-is), the straight/generic
   silhouette was, so v2 makes the blade's own edges asymmetric/jagged, adds a curling hook-guard
   accent, and adds a second glow color (crimson vein-branches alongside the cyan fuller) so the
   weapon can no longer be mistaken for a plain iron sword reskin.

Same "Abyssal Sovereign" palette identity throughout (Section 19.3) - per the task brief, the
palette itself was never the problem and needed no changes; only the concrete texture-level
execution built from it did.

---

### 19.7 Full rework (2026-07-07): GeckoLib demon-lord model + "Umbral Sovereign" palette — SUPERSEDES 19.1-19.6

The complete Drevathis rework (user brief: "a demon which is born to kill you, bigger than the
player, black blade with dark smoke, all attacks GeckoLib-animated") replaced the old
BipedEntityModel robe-sovereign design entirely. **Sections 19.1-19.6 above are historical**:
the "Abyssal Sovereign" palette (19.3), the robed model (19.2), and the v2 flat blade sprite
(19.4) no longer ship. What ships now:

**Model** (tools/gen_drevathis.py -> drevathis.geo.json, 13 bones / 38 cubes, ~31 units to the
skull, ~36 with horns, renderer withScale(1.8F) on the unchanged 1.08x3.24 hitbox): hulking
horned demon - swept-back ridged temple horns (2 stepped cubes each, bone-rotated), boulder
shoulders with two-step bone spikes, gaunt plated chest with an ember sternum crack, corded
two-segment arms ending in clawed fists, digitigrade-suggesting stepped legs (thigh/set-back
shin/cloven hoof), barbed two-bone tail, and the CURSED BLADE AS REAL GEOMETRY (a "blade" bone
on the right forearm, carried tip-up-outward at base rotation [24, 0, 14]).

**Animations** (tools/gen_drevathis_anims.py -> drevathis.animation.json): idle (breath + tail
sway), walk (0.9s prowl), throw_wave (0.6s palm-thrust, projectile spawns tick 6), curse_ground
(1.6s sky-raise -> ground-scythe, zone erupts tick 20), stampede_run (0.5s horns-first gallop
loop, state-driven), end_channel (5.0s arms-spread-skyward tremble -> collapse burst). The
script's docstring records the blade-alignment gotcha (canceling the base rotation does NOT
track a raised arm) and all server-timing contracts.

**Palette: "Umbral Sovereign"** (original; replaces "Abyssal Sovereign"):

| Role | Name | Hex | Notes |
|---|---|---|---|
| Hide shadow | Umbral Hide Deep | `#1B1418` | speckle/shadow tone |
| Hide mid-tone | Umbral Hide | `#2B2027` | dominant body fill (violet-tinted near-black) |
| Hide highlight | Umbral Hide Lit | `#3E2E38` | muscle striations |
| Bone plate | Dread Plate | `#4A3B3E` | chest/shoulder/brow plates |
| Bone plate lit | Dread Plate Lit | `#5E4C4E` | plate top edges |
| Horn/keratin dark | Horn Dusk | `#2E2620` | horn ring bands, hooves |
| Horn/keratin mid | Horn | `#55483F` | horn fill, small teeth |
| Horn/keratin lit | Horn Pale | `#6E6157` | horn tips, fangs, claws |
| Glow (signature) | Sovereign Ember | `#FF7A26` | eyes, sternum crack (ORANGE - not Spider Queen's `#FF3B1E` red, not Ashen Brute's dull amber `#D9C24A`) |
| Glow core | Ember Core | `#FFB84D` | brightest 1-2 px per glow feature |
| Glow deep | Ember Deep | `#C6431C` | skin fissures, crack falloff |
| Blade black | Voidsteel | `#0C0A10` | blade fill - pure blacks, NO ember (the blade is darkness, the body is fire) |
| Blade sheen | Voidsteel Grey | `#232030` | diagonal sheen streaks, spine |
| Blade edge | Voidsteel Edge | `#4E4A5E` | cold grey-violet edge light |
| Grip dark/lit | - | `#1C1410`/`#33261D` | leather grip bands |

The "dark smoke" of the blade is runtime particles, not texture: SMOKE+SQUID_INK wreath at the
carry position, server-spawned (DrevathisEntity.tickBladeSmoke() on the boss; scaled-down in
CursedBladeItem.inventoryTick() for the player-held drop).

**Item assets** (tools/gen_drevathis_blade_item.py): new flat 16x16 icon (black greatsword on
the sword diagonal, Voidsteel Edge light, two smoke wisps) for gui/ground/fixed/on_shelf, plus
a 5-element 3D in-hand model (~1.65 blocks, -45deg-Z diagonal authoring, stock item/handheld
transforms) - same display-context-select pattern the Colossal Warclub established.

**Skill VFX color language** (all vanilla particles, no custom types): dark wave = SQUID_INK +
SCULK_SOUL crescent; Curse Ground = SQUID_INK/ASH carpet + SOUL_FIRE_FLAME boundary ring +
FLAME burn ticks; Stampede = SQUID_INK ground rip + SQUID_INK/SCULK_SOUL burst on hit; The End
is Near = SCULK_SOUL boundary + inward-drifting SQUID_INK + FLAME/LAVA/LARGE_SMOKE comets +
SONIC_BOOM finale. Passive storm: per-player weather packets (rain + thunder gradients 1.0),
no visual assets at all.

## 20. World-event landmark visual identity: "Rissobelisk" (`baum2:rissobelisk`)

The mod's **first custom `Block`** (everything visual before this section was either an
`Entity`/`EntityModel` — Sections 13/15/17/18 — or a flat 2D item icon — Sections 14/16/18.4).
A rare, ordinarily-unbreakable stone landmark placed as a "world event" (`RissobeliskBlock.java`
+ `RissobeliskBlockEntity.java`, `strength(-1.0F, 3_600_000.0F)` — unbreakable by mining/
explosions/creative instant-break, per `MASTERPROMPT.md`'s "Welt-Events" spec). Players whittle
down a tracked 200 HP pool by left-clicking it; every 10% lost spawns a wave of 3 Silverfish
("cracks in the stone spew forth vermin"); destroying it fully grants XP and drops
Risssplitter, a rare crafting material sharing the "Riss"/crack root with the obelisk's own
name (the fiction: the obelisk cracks apart into splinters when destroyed). It cannot move or
attack back — a stationary target, not a mobile boss like Sections 17/18.

### 20.1 Palette-bucket decision: treated as boss-tier, not common-mob-tier (2026-07-05)

Section 1.2 (hostile mobs/bosses and their item drops) ratified a rule with two buckets:
boss-tier mobs always get their own new, cross-checked bespoke palette; future *common*/
frequently-spawned mobs should default to reusing a small number of shared palettes instead, so
bespoke palettes stay special. Rissobelisk fits neither bucket cleanly as written: it can't
fight back (not "boss-tier" in the mobile-mini-boss sense the rule's examples all share — Stone
of Spiders/Zombies, Spider Queen, Zombie Colossus all deal damage), but it is also obviously not
a common/trash mob either — it's an explicitly rare, hand-placed, one-off "world event" landmark
(`MASTERPROMPT.md` names it as one of a short list of *example* world events, alongside
Sternsplitter/Runenkern, not a generic environment block that spawns anywhere).

**Decision: treat it like a boss for palette purposes — bespoke original "Riftstone" palette
(Section 20.3), cross-checked against every existing palette in this document, same discipline
as Sections 15.2/17.3/18.3.** Reasoning:

- **The rule's own stated intent is about rarity/memorability, not mobility or combat
  capability.** Section 1.2's justification for the common-mob exception was scale ("giving
  each one its own bespoke palette... would not scale and would dilute what makes boss palettes
  feel special") — a concern about spawn *frequency*, which doesn't apply here at all.
  Rissobelisk is a low-frequency, hand-placed, designed set-piece encounter, the exact opposite
  of a frequently-spawned common mob.
- **Precedent already treats "is this a boss-adjacent set-piece" as the real boundary, not
  "is this specifically a mobile, damage-dealing `Entity`."** Section 1.2's own drops sub-rule
  already extends bespoke-palette treatment to non-mob content (Gold Sword, Poison Dagger,
  Colossal Warclub, the Queen Spider Set — none of these fight back either, they're items) —
  so "must be able to attack" was never the actual operative test in practice, even though every
  bespoke-palette example built so far happened to be either a boss or a boss's own drop.
- **The counter-consideration (it truly cannot move or fight back) is real but goes to combat/
  balance classification, not to the visual-identity question this document owns.** A player
  encountering a landmark clearly designed to look "ominous... a strange, dangerous
  destination worth traveling to" (this task's own brief) benefits from the same "instantly
  recognizable, doesn't look like a reused/common asset" treatment a mobile boss gets, regardless
  of whether it happens to swing back.
- This is a documented, deliberate reading of Section 1.2's *intent* extended to a content type
  (`Block`, not `Entity`) that section wasn't written with in mind — flagged explicitly here per
  this agent's own standing instruction to record judgment calls rather than resolve them
  silently. If a future common, frequently-placed decorative/landmark block type is ever added
  (as opposed to a rare hand-placed world event), that would be the point to revisit whether it
  should default to a shared/reused palette instead, the way common mobs do.

### 20.2 Block model chain — verified against decompiled vanilla 1.21.11 assets, not guessed

This is the mod's first block-adjacent asset of this type (blockstate/block-model JSON), so the
schema was verified directly against vanilla's own real, shipped 1.21.11 resources (extracted
from the deobfuscated client jar under `.gradle/loom-cache/minecraftMaven/...`, the same
ground-truth method Section 14.3 established for item models) rather than assumed from
potentially-stale training data or copied from an older Minecraft version's conventions.

- **Blockstate** (`assets/baum2/blockstates/rissobelisk.json`) — Rissobelisk has no
  `BlockState` properties (no facing/waterlogged/etc.), so this uses the simplest possible
  single-variant form, confirmed identical to vanilla's own `iron_block.json` (a property-less
  full block):
  ```json
  { "variants": { "": { "model": "baum2:block/rissobelisk" } } }
  ```
- **Block model** (`assets/baum2/models/block/rissobelisk.json`) — uses a standard vanilla
  parent rather than authoring custom multi-element geometry, per this pass's explicit scope
  (a custom tall-obelisk shape extending outside the 0-16 cube is a reasonable future upgrade,
  not attempted here — no custom geometry has been proven to work in this project yet for
  blocks). Distinct top vs. side/bottom textures were chosen (`minecraft:block/cube_bottom_top`,
  not `cube_all`) — verified against vanilla's own `sandstone.json` for the exact 3-texture-key
  schema (`top`/`bottom`/`side`) — because a bespoke landmark reads better with a distinct
  "business end" glowing rune-sigil visible from above (Section 20.4) than a uniform 6-face
  cube would; `bottom` intentionally reuses the same texture file as `side` (rarely seen,
  no need for a third unique file):
  ```json
  {
    "parent": "minecraft:block/cube_bottom_top",
    "textures": {
      "top": "baum2:block/rissobelisk_top",
      "bottom": "baum2:block/rissobelisk",
      "side": "baum2:block/rissobelisk"
    }
  }
  ```
- **Item model — deliberately no separate `models/item/rissobelisk.json`, diverging from
  every prior item's two-file pattern (Section 14.3 etc.) after verifying vanilla's own actual
  convention for plain block items.** Extracted and read `assets/minecraft/items/stone.json`,
  `.../iron_block.json`, and `.../furnace.json` directly from the decompiled client jar: all
  three point their `assets/minecraft/items/<name>.json` entry point **straight at the block
  model** (`"model": "minecraft:block/stone"` etc.) with **no intervening
  `models/item/<name>.json` file at all** — vanilla doesn't create one for ordinary full-block
  items, because the item-model-definition's `"model"` field can reference any model identifier
  directly, and a `cube`-derived block model already inherits full `display` transforms
  (gui/ground/fixed/thirdperson/firstperson + `"gui_light": "side"`, confirmed by reading
  `assets/minecraft/models/block/block.json`) needed to render correctly as a 3D isometric icon
  in inventory/hand — unlike a flat `item/generated` icon, which is what every prior item in
  this document actually needed the extra file for. Rissobelisk's item follows this exact
  verified vanilla pattern rather than adding a redundant pass-through file purely for
  superficial consistency with this mod's *other* items (which are all plain `Item`s with a
  `minecraft:item/handheld`/generated-style parent, a genuinely different case):
  ```json
  { "model": { "type": "minecraft:model", "model": "baum2:block/rissobelisk" } }
  ```
  Net effect: the block renders as a real 3D cube icon (with the top-face rune-sigil visible)
  in the inventory and in-hand, not a flat 2D sprite — the correct, verified behavior for a
  block item, confirmed structurally (not just by inference) against three independent vanilla
  examples.

### 20.3 Color palette: "Riftstone" (original, distinct from every other palette in this document)

| Role | Name | Hex | Notes |
|---|---|---|---|
| Stone shadow | Riftstone Shadow | `#14161C` | Deterministic darker speckle pixels across the stone base |
| Stone mid-tone | Riftstone | `#2B2F3A` | Dominant fill — a cool, dark blue-gray slate |
| Stone highlight | Riftstone Pale | `#4A4F5E` | Lighter speckle pixels, simulates weathered stone grain |
| Crack channel | Fissure Void | `#0A0610` | The physical crack line itself — near-black with a cool violet undertone, not pure black |
| Glow, far/dim | Rift Glow Dim | `#4A1440` | Halo pixels further from a crack's energetic center — energy reads as dissipating with distance |
| Glow, near/edge | Rift Glow Edge | `#7A1F66` | Halo pixels closer to a crack's center or branch point |
| Glow, core | Rift Glow Core | `#F23DBE` | The crack's brightest sustained glow — vivid magenta-pink |
| Glow, hottest point | Rift Glow Pale | `#FCE0F5` | Sparse single/small-cluster highlight only (the top texture's rune core, a couple of kink-point pixels on the side texture) — same "one hot highlight, used sparingly" convention as Gold Sword's blade tip (Section 14.2) or Colossal Warclub's studs |

*Compliance note:* "ancient cracked stone monument with glowing rift/rune energy that spawns
vermin when damaged" is a broad, unclaimed fantasy trope (corrupted monuments, rift stones, and
glowing-crack set-pieces appear across countless unrelated games and other fantasy media) — a
genre convention, not IP tied to any specific existing game. The palette itself was cross-checked
against every other bespoke palette already in this document:

- **The base stone tone is a new, cooler hue family than every existing stone palette.**
  Riftstone (`#2B2F3A`) is a genuinely cool blue-gray slate (hue ≈ 224°); Section 13.3's Fused
  Stone (`#5C574A`, warm tan-brown, hue ≈ 35°), Section 15.2's Blight Stone (`#435930`, olive
  green, hue ≈ 95°), and Section 18.3's Ashen Hide (`#5C5142`, warm brown-gray, hue ≈ 35°) are
  all warm or olive. No existing "stone" family in this mod sits in the cool blue-gray range
  before now.
- **The glow color is a new hue slice for the mod, not a reuse of either existing violet or red
  family.** Rift Glow Core (`#F23DBE`) sits at hue ≈ 317° (vivid magenta-pink) — clearly
  separated from both of this mod's existing violet families (Royal Carapace `#4B2170`/Regal
  gold armor, hue ≈ 275°; Schattenläufer/Intelligence violet `#7C5CA0`/`#9B5FE0`, hue ≈ 268°,
  a cooler blue-violet) and its red/ember families (Life `#E2574B`, Rust Ember `#D9776C`, hue ≈
  5°, orange-red). 317° sits in the previously-unused gap between those two families on the
  color wheel — not adjacent enough to either to read as a recolor of an existing identity.
  It's also unrelated to every green glow already in the document (Larval Glow, Plague Glow,
  Toxic Eye, Bile Blotch, Brute Glare all sit in a yellow-green/chartreuse family; this is a
  magenta-pink, the opposite side of the wheel).
- **Lower-confidence flag, per this agent's own standing instruction to say so rather than
  guess:** a glowing-magenta "corrupted rift/crack" cue is not, as far as could be verified, a
  specific existing MMORPG's signature exact branding (unlike, say, a very particular
  saturated teal or a very particular green that some specific franchises are closely
  associated with) — but if a future pass builds this motif out further (e.g. a whole "Rift"
  enemy faction or a matching weapon set reusing this exact magenta), it's worth a second,
  more specific check at that point, the same caveat this document already carries for
  Runenwirker's teal (Section 3.3).

### 20.4 Placeholder textures (produced this pass)

**Explicitly temporary placeholders**, per `MASTERPROMPT.md`'s asset rule ("Kennzeichne
Platzhalter klar als eigene temporäre Platzhalter") — flat-fill pixel art, no anti-aliasing,
generated programmatically (Python + Pillow, available in this environment unlike prior passes
in this document that fell back to PowerShell + `System.Drawing`), not hand-drawn final art. No
traced, extracted, or downloaded source material was used — every crack/glow pixel placement is
original hand-authored coordinate data.

- `assets/baum2/textures/block/rissobelisk.png` — 16x16 RGBA, **fully opaque** (alpha 255
  throughout; unlike item icons, a block face texture should never have transparency). Used for
  the side and bottom faces (`cube_bottom_top`'s `side`/`bottom` texture slots). A deterministic
  speckled stone base (a fixed `(x*7 + y*13) % 11` formula selects shadow/pale speckle pixels,
  not a saved random seed — reproducible without external state) with a branching network of 3
  jagged fissures (one main diagonal crack, one branch off it, one small isolated secondary
  crack) rendered as a `Fissure Void` line with a `Rift Glow Edge`/`Rift Glow Dim` halo bleeding
  onto adjacent pixels (nearer segments get the brighter Edge tone, farther segments the dimmer
  Dim tone, so the glow reads as radiating from a couple of energetic points rather than being
  uniform along the whole crack), plus two sparse `Rift Glow Pale` hot-point pixels at kink
  joints.
- `assets/baum2/textures/block/rissobelisk_top.png` — 16x16 RGBA, fully opaque. Used for the
  top face only. Same speckled stone base, with a radiating rune-sigil motif instead of parallel
  diagonal cracks: a small central diamond-shaped `Fissure Void` cavity around a 2x2 `Rift Glow
  Pale` core, with 4 diagonal fissure rays extending from the diamond's corners toward each
  canvas corner (same near/far Edge-then-Dim halo falloff as the side texture) — reads as a
  glowing rune carved into the stone, visible from above, distinct from the side faces so the
  block doesn't look like a uniform reskinned building-block.
- Generation script (not checked into the repo, reproducible from Section 20.3's hex table plus
  this description if regeneration is ever needed): defines the palette, the deterministic
  speckle formula, and explicit hand-placed crack/ray coordinate lists, then paints each crack
  pixel `Fissure Void` and its unclaimed orthogonal neighbors a glow tone based on distance from
  the crack/ray's start.
- **Not yet done, flagged for a future art pass** (same caveat as every prior placeholder in
  this document): real hand-drawn surface detail (finer stone grain, a more deliberate carved
  rune glyph rather than a generic radiating-crack shape, actual emissive/glow rendering via a
  resource-pack overlay or shader if the project ever adds one) — this pass proves the model/
  blockstate chain resolves correctly and gives the block something thematically appropriate to
  look at in-game, not final art.

### 20.5 Files produced this pass

- `src/main/resources/assets/baum2/textures/block/rissobelisk.png` (16x16, placeholder)
- `src/main/resources/assets/baum2/textures/block/rissobelisk_top.png` (16x16, placeholder)
- `src/main/resources/assets/baum2/blockstates/rissobelisk.json` (real, verified schema)
- `src/main/resources/assets/baum2/models/block/rissobelisk.json` (real, verified schema)
- `src/main/resources/assets/baum2/items/rissobelisk.json` (real, verified schema — points
  directly at the block model, no separate `models/item/` file; see Section 20.2)
- `src/main/resources/assets/baum2/lang/en_us.json` — added `"block.baum2.rissobelisk":
  "Rissobelisk"` (an asset/translation file, not gameplay code; `BlockItem`'s translation key
  resolves to the block's own key by default, so this single entry covers both the in-world
  block's tooltip/highlight name and the dropped item's inventory name)

No Java/gameplay code was changed. `./gradlew build` confirmed passing after these changes
(no Java was touched, so this only re-confirms nothing in the resource pipeline broke).

### 20.6 Follow-up fix: Risssplitter drop icon (2026-07-05)

`Risssplitter` (`baum2:risssplitter`, `ModItems.java`) — the rare crafting material this block
drops — was registered as a plain `Item` in Java but shipped with **no visual assets at all**
(no texture, no item model, no item-model-definition wrapper), so it rendered with a missing/
blank texture in-game. This is a bugfix follow-up to Section 20, not a new content unit — it
gets a subsection here rather than its own top-level numbered section, and it **reuses the
Riftstone palette (20.3) unchanged, introducing zero new colors**, per this fiction's own
framing (`HANDOFF.md`): the obelisk cracks apart into splinters when destroyed, so the drop is
literally a fragment of the obelisk's own stone/crack material, not a separate design.

- **Design**: a jagged, angular shard/fragment silhouette (not a clean gem-cut) — hand-authored
  per-row pixel mask, tapering to a point near the top and bottom with an irregular width
  (2–8px) down its length, reading as a broken chunk rather than a polished crystal. Filled with
  the same deterministic speckled-stone formula as the block face textures (`(x*7 + y*13) % 11`
  selecting Riftstone Shadow/Mid/Pale), split by a single hand-placed `Fissure Void` crack line
  running most of the shard's length, with a `Rift Glow Edge` halo and one `Rift Glow Pale` hot
  pixel concentrated only around the crack's midpoint (rows 4–6) — **deliberately no halo on the
  rest of the crack's length**, a restrained departure from the block texture's fuller near/far
  Edge-then-Dim halo treatment (Section 20.4), because this icon's shard body is only 2–8px wide
  at any row; an early draft that copied the block texture's wider two-ring halo directly
  swallowed the entire stone body in magenta and had to be redrawn narrower to keep the glow a
  genuine "thin crack/edge accent" (this task's own brief) rather than the dominant color. The
  shard's outline reuses `Fissure Void` doing double duty as both crack-channel color and the
  1px silhouette outline stroke, matching the "shape + darker outline" convention already
  established for the class/subspec icons (Section 3.3/9.1).
- **Technical**: 16x16 RGBA, transparent background, flat-fill, no anti-aliasing — same
  placeholder tier as every other icon in this document, generated programmatically (Python +
  Pillow). No traced, extracted, or downloaded source material was used. Follows the exact
  two-file `minecraft:item/generated` + `layer0` pattern established for this mod's other plain
  flat-icon items (Gold Sword, Section 14; Poison Dagger, Section 16) — **not** Rissobelisk's
  own divergent block-item pattern (Section 20.2), since Risssplitter is a plain `Item`, not a
  `BlockItem`.
- **Files produced this pass**:
  - `src/main/resources/assets/baum2/textures/item/risssplitter.png` (16x16, placeholder)
  - `src/main/resources/assets/baum2/items/risssplitter.json` (item-model-definition wrapper,
    `minecraft:model` → `baum2:item/risssplitter`)
  - `src/main/resources/assets/baum2/models/item/risssplitter.json` (`minecraft:item/generated`
    parent, `layer0` → `baum2:item/risssplitter`)
  - `src/main/resources/assets/baum2/lang/en_us.json` — added `"item.baum2.risssplitter":
    "Risssplitter"` (was also missing; without it the item would show its raw translation key
    as its inventory name even once the texture was fixed)
- No Java/gameplay code was changed. `./gradlew build` confirmed passing after these changes.

---

## 21. Heimgrund village architecture: "Dorfanger Hub" style spec

Design-only spec for the starting village that will be hand-built in creative and captured as
`VillageStamper` structure templates (see `HANDOFF.md`'s "Heimgrund" and "Village pipeline"
notes). Covers the **starting hub only** — the flat `Dorfanger` clearing at Heimgrund's center
(usable area: a circle of radius ~55 around 0,0, flat grass at y=64), stamped from a template
roughly 46x46 and up to ~16 tall. **This section produces no Java or `.nbt` files** — it is the
architectural reference for whoever builds the template (human, in creative) or scripts it.
The village ships **deliberately empty of NPCs/gameplay** — it is the safe hub players spawn
into at (0,65,0), not a quest hub yet.

### 21.1 Direction: translating "Deepwood & Verdigris" into vanilla blocks, plus a real-world architectural anchor

Section 1's UI mood — "weathered metal and stone, moss/patina green, and cold rune-light" — is
the mod's only existing visual-identity statement, so this village should feel like it belongs
to the same world without literally reusing UI hex codes (vanilla blocks don't carry arbitrary
hex fills anyway). The translation, block-family by block-family:

| UI-side concept (Section 1) | Vanilla-block translation | Why it works |
|---|---|---|
| Weathered stone | Cobblestone / Stone Bricks / Deepslate family | Same cool, aged-masonry read as "Deepwood Ink"/"Void Seam" without copying a hex |
| Verdigris (patina green) | **Oxidized Copper family** (`oxidized_cut_copper`, stairs, slabs) | Copper blocks *literally* weather from orange to blue-green patina in vanilla — a direct, unforced material match to the name "Verdigris," not a reference to any game |
| Cold rune-light | **Soul Lantern** (blue-white flame) vs. ordinary amber **Lantern** | Soul Lantern's built-in cold-blue glow is the closest vanilla equivalent to "Rune Cyan" light without needing a custom texture |

On top of that translation, the actual **construction style** is anchored to a real-world,
IP-safe reference rather than invented from nothing: **German half-timbered ("Fachwerk")
building construction** (dark timber frame over pale plaster infill) for individual buildings,
and the **"Rundling"** — a real historical Central-European circular village form (houses ringed
around a shared central green/common) — for the settlement's overall plan. Both are real-world
vernacular architecture, not any specific game's IP, and both tie naturally to the German
naming already established (`Heimgrund`, `Dorfanger` = literally "village green/common" in
German). This is also the concrete answer to "must not read as a vanilla-village clone": vanilla
villages use organic, un-planned scatter and plank/cobblestone box houses with no timber-frame
motif and no radial plan — this village is deliberately planned, radial, and timber-framed,
which reads differently at a glance.

*Compliance note:* half-timbered construction and circular common-green village plans are
centuries-old real-world architectural/settlement patterns (found across German-speaking
Europe), not content originated by or associated with any specific existing video game — safe
under the same "genre convention vs. specific IP" bar this document already applies elsewhere
(Section 0). Nothing here is traced, measured from, or intentionally modeled after a specific
existing game's hub-town layout.

### 21.2 Block palette

All vanilla, no custom blocks/textures. Grouped by role:

**Primary wall (stone shell + foundation courses)**

| Role | Block ID(s) |
|---|---|
| Main wall shell | `minecraft:stone_bricks` |
| Foundation / lower courses (weathering read) | `minecraft:cobblestone`, `minecraft:mossy_cobblestone` (sparse, ~1-in-6 blocks in the bottom 1-2 courses only) |
| Corner pillars / quoins | `minecraft:polished_andesite` |
| Weathering accent (used sparingly, not a full-wall material) | `minecraft:mossy_stone_bricks`, `minecraft:vine` (climbing corners/monument only) |

**Timber / Fachwerk accent**

| Role | Block ID(s) |
|---|---|
| Structural framing (vertical posts, diagonal braces, header beams) | `minecraft:dark_oak_log` (posts, upright), `minecraft:dark_oak_wood` (long horizontal/diagonal runs) |
| Infill panels between timber (the "plaster") | `minecraft:diorite` (primary), `minecraft:polished_diorite` (thin trim strips only, 1-block accents) |
| Doorframes, window sills, interior floors, shutters | `minecraft:dark_oak_planks` |
| Window grilles / railings | `minecraft:dark_oak_fence` |

**Roof (stairs + slab family — straight gable runs only)**

| Role | Block ID(s) | Used on |
|---|---|---|
| Ordinary roof | `minecraft:deepslate_tiles`, `minecraft:deepslate_tile_stairs`, `minecraft:deepslate_tile_slab` (ridge cap) | Every building except the two called out below |
| Landmark roof ("Verdigris crown," see 21.4) | `minecraft:oxidized_cut_copper`, `minecraft:oxidized_cut_copper_stairs`, `minecraft:oxidized_cut_copper_slab` | Gathering Hall + central Monument capstones only |
| Eave/underside trim | `minecraft:dark_oak_stairs` (upside-down, lining the roof underside overhang) | Every building |

**Path & plaza material**

| Role | Block ID(s) |
|---|---|
| Central plaza paving | `minecraft:polished_andesite` (main fill) alternated with `minecraft:andesite` in a simple 2-block checker/banding pattern, bordered by a `minecraft:stone_brick_slab` curb |
| Ring path / spoke paths | `minecraft:cobblestone` main fill, `minecraft:mossy_cobblestone` scattered sparsely (deterministic ~1-in-8, not random per stamp) |
| Informal garden path | `minecraft:dirt_path` |

**Greenery**

| Role | Block ID(s) |
|---|---|
| Hedges (perimeter, garden borders) | `minecraft:azalea_leaves`, `minecraft:flowering_azalea_leaves` (sparse, ~1-in-10 of hedge blocks, for a color accent) |
| Ground cover / weathering | `minecraft:moss_block`, `minecraft:moss_carpet` |
| Wall climbing accent | `minecraft:vine` |
| Garden bed accents | `minecraft:flower_pot` with `minecraft:potted_allium` / `minecraft:potted_lily_of_the_valley` |
| Pond edge (monument, static placement only — no moving-water contraption) | `minecraft:water` (source blocks only), `minecraft:lily_pad` |

**Light sources**

| Role | Block ID(s) | Placement rhythm |
|---|---|---|
| Ordinary path/village lighting | `minecraft:lantern` (hanging from a `minecraft:dark_oak_fence` post or an eave bracket) | Every 6-8 blocks along the ring path (see 21.4) |
| Threshold / important-place lighting ("cold rune-light") | `minecraft:soul_lantern` | Central monument (flanking) + every perimeter gate archway only — never used for ordinary path lighting, so it stays a meaningful signal (see 21.5, motif 3) |

**Fences / walls (connections computed by the builder, per this task's own constraint)**

| Role | Block ID(s) |
|---|---|
| Perimeter low wall | `minecraft:cobblestone_wall`, capped with `minecraft:stone_brick_slab` |
| Gate flanking pillars | `minecraft:andesite_wall` topped with a `minecraft:lantern` or `minecraft:soul_lantern` (gates only, per above) |
| Garden fencing | `minecraft:dark_oak_fence` |

### 21.3 Building program (46x46 template)

Six structures/elements, sized to comfortably fit a 46x46 footprint with room for the plaza,
paths, and perimeter margin. All entrances are **open archways or doorway-shaped gaps framed in
stone brick or dark oak planks — no vanilla door blocks**, consistent with the "no NPCs yet,
script-simple" brief. All roofs are **straight gables** (two parallel stair-and-slab runs
meeting at a ridge slab) — no hipped/pyramid roofs, so no inner/outer stair corners are needed
anywhere in the template.

| # | Structure | Footprint (approx.) | Visual identity |
|---|---|---|---|
| 1 | **Arrival Plaza + "Heimstein" monument** | ~11x11 plaza, monument ~3x3 core | The dead center of the whole template, where the player spawns. A short cluster of standing `chiseled_deepslate`/`polished_deepslate` monoliths (1-3 blocks, uneven heights, not a single tower) ringed by a shallow static lily-pad pond, flanked by 2 Soul Lanterns on `andesite_wall` posts. Reads as an old waystone/meeting-stone, not a statue of a person — keeps it lore-neutral since no faction/NPC content exists yet. |
| 2 | **Versammlungshalle (Gathering Hall)** | ~11x9 footprint, ~9 tall to the ridge | The village's one large building — Fachwerk walls, **oxidized-copper gable roof** (the template's second Verdigris-crown building), a wide open double-archway entrance facing the plaza. Positioned on one cardinal spoke, distinguishable from the smaller houses purely by scale and roof material, no signage needed. |
| 3 | **Werkstatt (Workshop)** | ~7x7, single-story with a lean-to (single-slope) roof extension | A smaller utility building — same Fachwerk walls, deepslate-tile roof, an open-sided lean-to bay (no wall on one side, just posts) suggesting workbenches/storage for a future crafting tie-in, plus a `minecraft:campfire` for a chimney/smoke read (cosmetic only). |
| 4 | **Wohnhäuser (cottages), x2-3** | ~6x6 to 7x8 each | Smallest Fachwerk buildings, plain deepslate-tile gable roofs, one open doorway + 1-2 small punched windows (2x1 gaps with a `dark_oak_fence` grille) each. Deliberately the most repeated/simplest element — reads as "ordinary dwelling," never gets the copper roof. |
| 5 | **Kräutergarten (herb garden court)** | ~8x8 | An open, low-walled or hedged court (cobblestone-wall or azalea-hedge border, waist-height) with a simple bed grid marked out in `moss_block`/`dirt_path` and a few potted-flower accents. The one "soft," non-building element in the program — deliberately not gated or roofed, to read as an approachable green space rather than a locked garden. |
| 6 | **Perimeter treatment** | Ring at the template's outer edge | Low `cobblestone_wall` (2 high) alternating with short azalea-hedge sections, broken by 2-4 gate openings (archway, ~3 wide x 4 tall) aligned to the spoke paths continuing out into the wider Lichtwiese meadow. Deliberately low/open rather than a fortress wall — the brief is "safe hub," not "besieged keep." |

### 21.4 Layout guidance

> **SUPERSEDED by the user's own hand-drawn village plan (2026-07-10,
> `run/heimgrund_rework_map.png`), which is authoritative over this section's radial-ring
> proposal.** The shipped layout (see `tools/gen_village.py`) is: a gray RECTANGULAR
> respawn plaza just north of the exact center acting as the road hub; a straight east-west
> through-road and a north road replacing the ring path + spokes; two cottages north of the
> plaza flanking the north road; one square house west and the Werkstatt east of the plaza
> on the through-road (doors facing the plaza); and the Great Hall — the biggest building —
> at the village's south, with NO south gate (3 gates: N/W/E). Materials, motifs (21.5),
> and the building visual identities in 21.3 still apply unchanged; only the plan geometry
> below is historical.

- **Plan type: radial "Rundling"-style ring, not a scatter and not a rigid 4-cardinal-shops
  hub.** A circular **ring path** (3 blocks wide, `cobblestone`) runs around the Arrival Plaza
  at a fixed radius (~14-16 blocks from center); each of the 5 non-plaza structures above sits
  just outside that ring, connected to it by a short **spoke path** (2 blocks wide). The
  Gathering Hall (the largest, most important building) sits on its own spoke directly opposite
  the main gate spoke, so a player arriving through the primary gate sees it immediately across
  the plaza — the one deliberate sightline axis in an otherwise even radial arrangement.
- **Why radial-ring rather than a cardinal-cross hub:** a strict 4-building-at-4-compass-points
  layout is the closer-to-generic-MMORPG-hub read this task explicitly warns against; an
  unevenly-spaced ring (6 elements around one plaza, not a clean 4-way symmetric cross) reads
  as an organically-settled common green instead, and matches the real "Rundling" reference in
  21.1 rather than a game-hub template.
- **Path widths:** ring path 3 wide; spokes 2 wide; the 2-4 perimeter gate paths widen back to 3
  wide as they pass through the wall, so the gate itself doesn't feel like a bottleneck.
- **Lighting rhythm:** ordinary `lantern`s every 6-8 blocks along the ring path (post-mounted on
  `dark_oak_fence`), plus one at the start of each spoke path. Soul Lanterns are reserved
  entirely for the monument and the perimeter gates (see palette table above) — this is a
  deliberate rhythm break, not decoration for its own sake (see motif 3, below).
- **Village edge → open meadow transition:** the perimeter wall (21.3 item 6) is intentionally
  low (2 blocks) and gapped with hedges rather than a continuous high wall — from outside, a
  player approaching from the Lichtwiese meadow should be able to see rooftops over the wall
  before reaching a gate. This supports the "safe, welcoming hub" read the brief calls for,
  distinct from a fortified compound.

### 21.5 Signature motifs (original to Baum2, not from any existing game)

1. **Verdigris-crown roofs mark importance.** Only the two most significant structures in the
   template — the Gathering Hall and the central Monument — get an oxidized-copper roof/
   capstone; every ordinary building uses plain deepslate tile. This directly mirrors the mod's
   own established convention of color-coding meaning consistently (Section 1's "color carries
   meaning" principle, Section 5's rarity/class-color conventions) — translated here into "which
   *material*, not just which color, marks importance" for a vanilla-block-only context.
2. **Fachwerk rhythm + andesite corner quoins.** Every building in the village (not just some)
   repeats the same construction pattern: dark-oak diagonal/vertical timber over pale diorite
   infill, with a polished-andesite pillar reinforcing each building corner. This is the
   village's recurring "handwriting" — a player should recognize a Heimgrund building at a
   glance from this rhythm alone, the same way the mod's class icons share a recognizable family
   resemblance (Section 3.3/9.1).
3. **Cold rune-light only at meaningful thresholds.** Soul Lanterns (cold blue-white glow) are
   never used for ordinary lighting — only at the central monument and the perimeter gates, i.e.
   the two kinds of place where a player is "arriving" or "departing" the safe zone. Ordinary
   amber Lanterns handle everyday path lighting everywhere else. This gives the village a subtle
   but consistent "cold light = significant place" language, echoing the UI's own "+value =
   Rune Cyan" universal-meaning convention (Section 5) in an architectural form.

### 21.6 Open items for whoever builds the template

- This section is a **design spec only** — no `.nbt` template, blockstate, or Java was produced
  or touched by this pass. Building it (by hand in creative, per `HANDOFF.md`'s stated plan, or
  via a future generation script) and capturing it with `/baum2 structure save` is a separate,
  follow-up task.
- Exact placement/rotation of the 2-4 perimeter gates relative to Heimgrund's biome geometry
  (which directions the meadow/desert/mountain zones actually lie in) isn't specified here —
  align gates to whichever compass directions make sense once the builder checks `ZoneLayout`/
  `HeimgrundBiomeSource`, so a gate doesn't face directly into an awkward spot (e.g. straight at
  a lake basin).
- Interior furnishing of the Gathering Hall/Workshop/cottages is explicitly out of scope (the
  village ships NPC-less) — leave interiors simple/empty rather than inventing furniture that
  will need to be redesigned once NPCs or quest-givers are actually added.
- If a 7th program element ever becomes necessary (e.g. a future quest-giver's own building),
  slot it onto the ring the same way as the existing 5 outer elements — don't break the radial
  pattern established here.

---

## 22. Mount visual identity: "Mount Horses" (`baum2:wanderross` / `eisenross` / `schlachtross`)

The mod's first player-summonable, rideable mounts — three tiers of horse, all built on ONE
shared GeckoLib geometry + animation set (`mount_horse.geo.json` / `mount_horse.animation.json`),
following exactly the "Fallen Comet Stone" shared-template pattern established in Section 13.5:
one geometry/animation pair resolved via `withAltModel`/`withAltAnimations`, per-tier identity
carried entirely by texture. Names (Wanderross/Eisenross/Schlachtross) already cleared by
`ip-naming-compliance-checker` — see `MountTier.java`'s own javadoc for why the generic
"Basic/Advanced/Military Horse" labeling was rejected (it mapped 1:1 onto Metin2's real,
documented tier structure even in translated form).

### 22.1 Design direction

A plain riding horse whose *single* shared geometry already contains every armor cube any tier
could need — saddle, 2 flank plates (the "saddle-armor" group), plus a chamfron, neck wrap,
chest plate, and rump plate (the "body-armor" group) — with each tier's own texture deciding
which of those 7 cubes actually render (fully-transparent alpha-0 pixels for an unused cube,
verified programmatically — see 22.4). This is deliberately the cheapest possible way to get 3
distinctly-silhouetted mounts without 3 separate models: Wanderross reads as an unarmored horse,
Eisenross as the same horse with a fitted iron saddle, Schlachtross as the same horse in full
black barding — purely because of which cubes' pixels are opaque.

Beyond armor, each tier also gets its **own coat color** (not just "armor added to an identical
horse") so the three read as distinct animals at a glance even from behind/underneath where
armor isn't visible: warm chestnut-brown (Wanderross, a plain unremarkable riding horse),
dappled steel-grey (Eisenross, visually rhyming with "iron" before the rider even sees the
saddle), and black (Schlachtross, a conventional "warhorse" color pairing with its black plate).
None of these are a specific real-world horse breed's exact registered color name or any
existing game's specific mount-tier color branding — plain color-word/tone choices.

### 22.2 Color palette

Per-tier, 4 coat roles (base/shadow/highlight/dark-detail for mane-tail-hooves) plus the 2 armor
groups (each either painted, with 3 roles, or left `None` = fully transparent):

| Tier | Coat base | Coat shadow | Coat highlight | Coat dark (mane/tail/hoof) |
|---|---|---|---|---|
| Wanderross | `#8A5A3C` | `#5E3B24` | `#B98058` | `#3A2416` |
| Eisenross | `#8A929C` | `#5C646E` | `#B8C0C8` | `#34383E` |
| Schlachtross | `#2A2A2E` | `#16161A` | `#46464C` | `#0A0A0C` |

| Tier | Saddle-armor group (saddle + 2 flank plates) | Body-armor group (chamfron/neck/chest/rump) |
|---|---|---|
| Wanderross | *(transparent — no armor)* | *(transparent — no armor)* |
| Eisenross | base `#6E7680` / shadow `#454C54` / highlight `#ACB4BC` (iron) | *(transparent)* |
| Schlachtross | same iron triple as Eisenross (a warhorse still needs a real saddle under the plate) | base `#202226` / shadow `#101114` / highlight `#3A3E46` / accent `#6E1F1F` (black plate, thin dark-red trim seam) |

*Compliance note:* black plate armor with a red trim accent is a very common, generic
"dark/battle knight" fantasy convention (used across countless unrelated properties and stock
fantasy art, not one specific recognizable game's exact branding) — flagging this explicitly per
this agent's own instruction to say so rather than silently assume it's fine. If a future pass
gives Schlachtross more visual real estate (a boss-tier reskin, a promotional render, etc.) and
the red accent starts feeling like it's converging on one specific game's known "black armor +
red" branding, swap the accent hue (e.g. to a cold blue-white "frost" line instead) rather than
leaving it unexamined. Low confidence of an actual conflict today, same bar as every other
caveat in this document.

### 22.3 Geometry — shared "Mount Horse" GeckoLib template

**Bones**: `body` (root; carries the main torso cube plus saddle/flank-plate/chest-plate/
rump-plate armor cubes), `neck` (parent `body`, base-pose rotation bends it up-and-forward;
carries the neck-armor cube), `head` (parent `neck`; carries the main head box, a forward
muzzle box, 2 small ears, and the chamfron); `tail` (parent `body`, hangs down-back);
`leg_front_left`/`leg_front_right`/`leg_back_left`/`leg_back_right` (parent `body`, pivot at the
hip/shoulder attachment, no base rotation — this is what the walk/attack animations rotate).
18 cubes total across 8 bones. **No head-turn/look-at-player bone or controller** — not in the
brief, and a rideable mount's head shouldn't independently track the camera the way a hostile
mob's might.

Armor cubes are attached to the bone they physically cover (chamfron -> `head`, neck armor ->
`neck`, everything else -> `body`) rather than getting their own bones, since none of them need
independent motion beyond whatever bone they're welded to already does.

**Shape concept (original, not sourced from any game)**: natural horse proportions — main body
top ("rump") at 22 units (1.375 blocks, ~"1.4 blocks tall at the back" per the brief), standing
on 4 legs that reach the ground plane (y=0), a neck/head reaching up to ~2.8 blocks at the ears
for an alert, head-up stance, and a tail hanging behind. "Horse" is an unavoidably generic shape
(four legs, neck, head, tail) — every specific measurement, proportion choice, and the armor-cube
layout are this project's own, verified visually via `tools/render_geckolib_preview.py`, not
traced or derived from any existing game/mount asset.

**Template contract (how a hypothetical 4th tier would be added)**: geometry and all 3 animations
stay ONE shared pair of files — `assets/baum2/geckolib/models/entity/mount_horse.geo.json` +
`geckolib/animations/entity/mount_horse.animation.json`, resolved the same way as the Fallen
Comet Stone family (a `MountHorseGeoModel<T>` following `FallenCometStoneGeoModel`'s exact
`withAltModel`/`withAltAnimations` pattern — **not written by this pass**, see 22.6 for what the
Java side still needs to do). A new tier needs only: a coat + (optional) armor-group palette
dict added to `TIERS` in `tools/gen_mount_horse.py`, a rerun, and a renderer registration — no
new geometry, animation, or model class. The generator reseeds its RNG per tier so all 3
(eventually N) atlases stay pixel-identical in layout (verified by the script's own `assert`,
same discipline as `gen_fallen_comet_stone.py`).

**Per-tier SIZE** (renderer scale 1.0 / 1.1 / 1.25 for Wanderross/Eisenross/Schlachtross) is
**not** baked into the geometry at all — it's a `GeoEntityRenderer.withScale(...)` call on the
Java side (already anticipated by `MountTier.renderScale()`, which is implemented and pinned to
these exact 3 numbers) plus matching per-tier `EntityType.Builder.dimensions(...)` hitbox scaling,
exactly the pattern `docs/fabric-modding.md`'s GeckoLib section part D already documents for a
scaled `GeoEntity` (Spider Queen's 3x scale via the same mechanism).

### 22.4 Textures — verified per-tier armor-cube transparency

254x152 px GeckoLib atlas (2 texture px per model unit, same ratio as the Fallen Comet Stone
family), pixel-art placeholder tier (deterministic noise/speckle fill via Python/Pillow, no
anti-aliasing, no traced/downloaded source material) — good enough to prove the model/animations
and give the mount a coherent in-game look, not final hand-painted art (flagged per
`MASTERPROMPT.md`'s placeholder rule, same as every other GeckoLib atlas in this document).

**Verified programmatically** (not just asserted) that the armor-cube alpha contract in the task
brief holds exactly, by sampling the generator's own in-memory atlas before saving:

| Cube group | Wanderross | Eisenross | Schlachtross |
|---|---|---|---|
| Saddle-armor (saddle + 2 flank plates) | alpha **0** (all sampled pixels) | alpha 160-255 (painted) | alpha 160-255 (painted) |
| Body-armor (chamfron/neck/chest/rump) | alpha **0** | alpha **0** | alpha 180-255 (painted) |

This is exactly the brief's contract (Wanderross plain, Eisenross saddle-only, Schlachtross
both groups) and confirms the cutout render layer will correctly discard the unused cubes'
pixels rather than rendering a stray gray/black box where an inactive armor cube's UV happens to
sample default texture data.

### 22.5 Item icons: 3 summon-flute textures

16x16, RGBA, transparent background — same diagonal-tool-icon reading convention Section 14.1
established for Gold Sword (blade-tip-at-top-right / grip-at-bottom-left), reused here for a
simple carved pipe/whistle rather than a blade: a 2px-thick diagonal wood-toned shaft, a dark
mouthpiece cap at the bottom-left end, 3 small dark finger-hole dots along the shaft, and one
small tier-accent "binding wrap" detail tying each flute back to its horse's own palette (warm
brown for Wanderross, steel-blue-grey for Eisenross, dark red for Schlachtross — the same red as
Schlachtross's own armor trim). Item-model-definition JSONs (`assets/baum2/items/*.json`) and
model JSONs (`assets/baum2/models/item/*.json`, plain `minecraft:item/generated` + `layer0`)
already existed before this pass with no texture behind them; this pass only added the 3 PNGs.

### 22.6 Files produced this pass, and what the Java side still needs

**Produced by this pass** (`tools/gen_mount_horse.py` is the single source of truth for all of
it, mirroring `gen_fallen_comet_stone.py`'s role — rerun it to regenerate everything
deterministically):
- `assets/baum2/geckolib/models/entity/mount_horse.geo.json`
- `assets/baum2/geckolib/animations/entity/mount_horse.animation.json` — exactly
  `animation.mount_horse.idle` (4s loop: subtle body/neck breathing sway plus one head-dip
  event partway through the loop), `animation.mount_horse.walk` (0.8s loop: diagonal-pair trot,
  `leg_front_left`+`leg_back_right` swinging opposite `leg_front_right`+`leg_back_left`, plus a
  small synced body bob/neck bob/tail sway), `animation.mount_horse.attack` (0.7s one-shot:
  forward hoof strike — both front legs kick up-and-forward, head/neck lunge down into the
  strike, body dips slightly, tail flicks, then everything returns to rest — plays every time
  the rider lands a melee hit while mounted, per the brief).
- `assets/baum2/textures/entity/wanderross.png`, `eisenross.png`, `schlachtross.png`
- `assets/baum2/textures/item/wanderross_flute.png`, `eisenross_flute.png`, `schlachtross_flute.png`

**Verified via `tools/render_geckolib_preview.py`** (rest pose, all 4 camera angles; `walk` and
`attack` sampled across several keyframes; `idle` sampled across the full loop): the model stands
upright on all 4 legs with hooves on the ground plane, faces the correct direction (head/neck
lean toward `-Z`, matching the renderer's own "front" camera and the coordinate convention every
other GeckoLib asset in this project already uses), the saddle/flank plates sit visibly on the
back/sides in the Eisenross render, and the chamfron/neck-armor/chest-plate/rump-plate all sit
visibly on their respective bones with the red accent trim reading clearly in the Schlachtross
render. `walk` shows a clear alternating 4-leg gait; `attack` shows a clear, readable front-leg
strike with head lunge, distinct from both `idle` and `walk` at a glance.

**Not written by this pass — flagged explicitly so it isn't lost** (this agent's scope is
assets/specs, not gameplay Java, per `CLAUDE.md`'s own division of labor):
- A `MountHorseGeoModel<T extends Entity & GeoAnimatable> extends DefaultedEntityGeoModel<T>`
  class, following `FallenCometStoneGeoModel`'s exact constructor pattern (`super(Identifier.of
  ("baum2", entityName)); withAltModel(SHARED_ASSETS); withAltAnimations(SHARED_ASSETS);` where
  `SHARED_ASSETS = Identifier.of("baum2", "mount_horse")`).
- A `MountHorseEntityRenderer` (or reuse of `FallenCometStoneEntityRenderer`'s generic shape) that
  additionally calls `.withScale(tier.renderScale())` per `MountTier`'s already-implemented
  1.0/1.1/1.25 values, plus the actual `MountHorseEntity`/`ModEntities` registration — none of
  which exists yet (only `MountTier`, `HorseFluteItem`, the 3 flute `Item`s, and the mount
  networking payloads/keybindings are implemented so far, confirmed by inspecting the repo before
  this pass).
- **Measurements the Java side needs, all at the geometry's own natural/unscaled size** (multiply
  by the tier's `renderScale` for the actually-rendered size, the same way GeckoLib's
  `withScale(...)` already composes with the model per `docs/fabric-modding.md`'s GeckoLib
  section part D):
  - Rump/back top: y = 22/16 = **1.375 blocks** (the "~1.4 blocks tall at the back" the brief
    asked for; the very top of the silhouette including the rump armor plate is y = 23/16 =
    1.4375 blocks).
  - Poll/ear top (head up, highest point overall): y = 45/16 = **2.8125 blocks**.
  - Nose tip (muzzle/chamfron front): z = -31/16 = **1.9375 blocks forward** of the entity origin.
  - Tail tip: z = 16/16 = 1.0 block behind origin, hanging down to y = 4/16 = 0.25 blocks.
  - Footprint across the 4 legs: x -8..8 (**1.0 block wide**), z -15..15 (**~1.875 blocks deep**
    across the front/back leg stance) — a reasonable starting point for
    `EntityType.Builder.dimensions(...)` per tier (scaled by `renderScale`), though the final
    hitbox call is a Java-side decision, not dictated by this pass.
  - **Suggested rider seat position** (for `getPassengerRidingPos`): the top of the `saddle` cube
    sits at x=0, y=25/16=1.5625, z=2/16=0.125 blocks (slightly behind the body's own horizontal
    center — right where a real saddle sits, between withers and rump) at the geometry's natural
    (Wanderross, scale-1.0) size. **This point should scale linearly with the tier's
    `renderScale`** the same way the rest of the model does (i.e. Eisenross's seat sits at
    `1.1x` this offset, Schlachtross's at `1.25x`), so the rider doesn't end up floating above or
    sinking into a bigger/smaller horse — this is the one number in this list that actively needs
    per-tier scaling applied by whoever implements `getPassengerRidingPos`, not just a fixed
    constant.

---

## 23. Weapon-line visual identity: the GeckoLib "sword template" and "Espenklinge" (`baum2:espenklinge`)

The mod's first ANIMATED weapon line, and the template every future sword of the line reuses —
the item-side sibling of the Fallen Comet Stone / Mount Horse shared-template pattern
(Sections 13.5 / 22): **ONE geometry + animation set for all swords, per-sword texture only.**

### 23.1 Template contract (adding sword #2, #3, ...)

- Shared, never per-sword: `geckolib/models/item/sword_template.geo.json` +
  `geckolib/animations/item/sword_template.animation.json` (generator:
  `tools/gen_sword_template.py`; per-face-UV atlas layout is asserted identical across
  palettes in-script, so the shared geometry stays valid for every texture).
- Per-sword (all emitted by the same generator run — add one palette entry to its `SWORDS`
  table): `textures/item/<name>_geo.png` (the 64x64 3D atlas), `textures/item/<name>.png`
  (flat 16x16 GUI icon), `models/item/<name>.json` (icon model), `models/item/<name>_base.json`
  (display transforms), `items/<name>.json` (display-context select), one
  `TemplateSwordItem(..., "<name>")` registration in `ModItems`, one lang line. No new Java
  classes, geometry, or animation work.
- Held contexts render the GeckoLib model (`minecraft:special` → `geckolib:geckolib`, base =
  `<name>_base.json`); `gui`/`ground`/`fixed`/`on_shelf` select the flat icon — exactly
  vanilla's own trident split.

### 23.2 Shape (shared geometry: 4 bones / 11 cubes, reach −6.6..21.0 = ~1.7 blocks)

Silhouette follows a real-world late-medieval longsword (museum-piece reference supplied by
the user — historical artifact, no game IP): straight double-edged blade tapering in two
width-steps to the point, raised **midrib ridge** running most of the blade (reads as a
diamond cross-section + catches light as its own strip), straight slender **crossguard** with
slightly flared end caps, two-hand tapering **wrapped grip**, and the signature **disc
("wheel") pommel with a small protruding boss** at its center. Length sits deliberately in
family with the Colossal Warclub (~1.6) and Drevathis blade (~1.65).

Bones (origin = grip center = the wielder's fist; GeoItemRenderer centers the geo origin in
the item cube, so display transforms and all animations pivot around the fist): `root` (grip
cubes) → `guard` / `blade` (3 blade steps + ridge) / `pommel` (disc + boss). A future sword
wanting an extra animated flourish (glow shard, tassel) hangs it off `blade`/`pommel` — but
new cubes mean a new template version for ALL swords, so prefer texture-only variation.

### 23.3 "Espenholz" palette (Espenklinge — pinned)

| Role | Hex | Notes |
|---|---|---|
| BLADE / DARK / LIT | `#CFB68C` / `#A48960` / `#E8D5AC` | pale aspen, long vertical grain, NO speckle (flecked versions read as dirt at 2px/unit) |
| EDGE | `#E0CA9E` | sanded lighter cutting edges (east/west faces) |
| RIDGE / LIT | `#B29466` / `#D5BC8C` | darker lacquer strip + spine highlight |
| GUARD family | `#5E452A` / `#402D1A` / `#7B5E3B` | walnut, horizontal grain (guard, pommel disc) |
| GRIP family | `#7E5230` / `#593820` / `#9C6D44` | leather, diagonal wrap bands at (x+y)%4 |
| BOSS family | `#B8893D` / `#DAAE62` / `#8F662A` | brass medallion — the single metal accent |

Original palette, checked distinct from every prior weapon palette (Sections 14/16/18/19).
Wood-material rendering of a "real" sword shape = the line's tier-zero/training identity;
future swords express tier via material palette on the SAME shape.

### 23.4 Animations (shared keys `animation.sword_template.*`, preview-verified)

- `idle` (6.0s loop): ±1° breathing sway + slow rise, with ONE late-loop accent (t≈4.2–5.2, a
  2.6° wrist roll — "adjusting the grip"), the same accent-event idiom as the horses' idle
  head dip. Amplitudes intentionally tiny: it plays inside a held hand.
- `attack` (0.55s one-shot, trigger `attack`): vertical **moulinet** — 8° back-cock, full
  360° forward circle around the fist (X-axis), +8° overshoot, settle. Final key = −360° ≡ 0°
  so the blend back to idle can't pop.
- `attack_mounted` (0.7s one-shot, trigger `attack_mounted`): **cavalry sweep** — windup
  horizontal over the left shoulder (rz −60/ry −35), low forward pass (rx −60 at 0.22s), wide
  cut across to the right (rz +80 at 0.42s) with a y-dip + forward reach, follow-through with
  a small negative overshoot. Reads clearly distinct from the moulinet in both perspectives.
- Both one-shots are server-triggered on LANDED melee hits only (`SwordAnimationHandler`,
  same AFTER_DAMAGE + PLAYER_ATTACK scoping as the horses' own attack animation — a mounted
  hit animates horse and blade in the same tick). `blade` bone carries small whip-lag
  counter-rotations in all three.

### 23.5 Display transforms (`<name>_base.json` — first in-game tuning pass pending)

Authored vertical (blade +Y), so `rotation:[x,−90,z]` maps the broad face where a vanilla
sprite sword's face ends up (rotationXYZ order: Z→Y→X; derivation in the generator's
docstring): thirdperson_r `[−10,−90,0] t[0,3.5,1] s0.85`, firstperson_r `[20,−95,5] t[−1,3,1]
s0.68`, lefthand mirrored explicitly (trident-style), `head` for fun. These are
geometry-derived ballparks matched to vanilla's net handheld orientation — they frame
correctly by construction, but the fine feel (how high in view, exact lean) is the one thing
the offline preview can't judge; expect one small in-game nudge pass.

### 23.6 Icon (16x16)

Standard bottom-left→top-right tool diagonal: pale 2px blade with lit edge + ridge shadow +
tip glint, perpendicular walnut guard bar crossing at the blade base, 2 leather grip cells,
2x2 walnut pommel block with ONE brass glint pixel (the palette's single metal accent,
mirroring the 3D boss).

---

## 24. Currency visual identity: "Baum Credits" (`baum2:baum_credits`)

The mod's first currency item — a wallet/pouch-style item meant to permanently occupy one
inventory slot per player, with its live balance written into the item's `ITEM_NAME` data
component (dynamic display text, not a texture concern). Monsters drop credits on death;
credits will later buy items. Java/data-component wiring for this item is being written in a
parallel session — this section covers texture + palette only.

### 24.1 Design direction

A flat brass coin, not a pouch/bag silhouette — coins read unambiguously as "currency" at
16x16 in a hotbar/inventory slot, where a cinched-sack silhouette tends to blur into a blob at
that resolution. Concept and rendering are original: a round token with a simple 3-tone bevel
(outline / rim / base, plus one highlight) and a small stamped glyph in the center — not a
recolor or trace of any existing game's coin/currency icon (no Metin2 yang symbol, no WoW gold
piece, no generic "G"/"$"-styled token). The center glyph is a tiny two-pixel-tall tree
silhouette (canopy over trunk) — a literal visual pun on **Baum** ("tree"), the mod's own
namesake — rather than a generic coin motif borrowed from genre convention.

### 24.2 Palette decision: reuses Deepwood & Verdigris chrome, not a new bespoke palette

Per Section 1.1's palette-governance rule, the first question for any new visual element is
"is this frame/structure/stable-identity, or a live resource/stat value read moment-to-moment?"
Baum Credits is neither exactly — it's an ordinary **in-world item icon** (like Gold Sword,
Poison Dagger, the mount flutes), not HUD chrome and not a Vitals-style resource bar. The
relevant precedent is instead Section 1.2's boss-palette rule: bespoke, wholly-new palette
families are reserved for boss-tier mobs and their drops, specifically because those are
memorable, low-frequency set pieces where a player recognizing "this is a different boss" at a
glance matters. Baum Credits is the opposite case — a single, ever-present, global utility item
with no boss/class/faction affiliation to encode.

**Decision: reuse the existing Deepwood & Verdigris chrome palette (Section 2) rather than
invent a new "currency" palette family**, following the precedent Gold Sword already set in
Section 14.1 (reusing Aged Brass for its pommel "for cross-system cohesion between this weapon
and the mod's established chrome, rather than inventing an unrelated gold tone"). Concretely:
the coin body reuses **Aged Brass (`#D9B36C`)** exactly — already the UI's "gold-associated"
emphasis accent (Section 2.2, used for level numbers and the Mythisch rarity tier) — and the
stamped tree glyph reuses **Verdigris Glow (`#5FA98C`)** and muted **Verdigris (`#33443B`)**
from Sections 2.1/2.2. The result literally pairs "gold coin" with "verdigris patina" — the two
words the whole art-direction name ("Deepwood & Verdigris") is built from — which is a stronger
cohesion argument than inventing an unrelated new hue family for a single always-present item.

### 24.3 Color palette

| Role | Hex | Notes |
|---|---|---|
| Outline | `#0B0E0D` | Void Seam (Section 2.1), reused exactly — coin's outer 1px edge |
| Rim (bevel ring) | `#B8934F` | New, in-family darker-brass tone between outline and base |
| Coin body | `#D9B36C` | Aged Brass (Section 2.2), reused exactly |
| Highlight | `#F0D999` | Light brass "shine," upper-left, kept clear of the center glyph |
| Tree glyph — canopy | `#5FA98C` | Verdigris Glow (Section 2.2), reused exactly |
| Tree glyph — trunk | `#33443B` | Verdigris, muted (Section 2.1), reused exactly — reads as a recessed engraving groove |

*Compliance note:* a round brass coin with a center-stamped glyph is a generic, IP-free
currency-icon convention (same bar as color-coded rarity — the mechanic/format is free, the
specific execution is original). No hex, silhouette, or stamped symbol here matches any
specific existing game's currency branding; the tree glyph is unique to this mod's own name.

### 24.4 Files produced this pass

- **Texture** (placeholder, per `MASTERPROMPT.md`'s asset rule — flat pixel art, no
  anti-aliasing, generated via `tools/gen_baum_credits_item.py`/Pillow, not hand-drawn final
  art): `assets/baum2/textures/gui/baum_credits.png`, 16x16, RGBA, transparent background
  outside the coin's circular silhouette. Verified 16x16 RGBA via Pillow after writing.
- **Superseded (2026-07-12, same day)**: Baum Credits is NOT an item anymore — the user
  rejected the wallet-item approach ("should not be an item; text in the inventory, icon at
  the end"). The texture moved from `textures/item/` to `textures/gui/` and is drawn by the
  client's inventory-screen overlay (`ui/BaumCreditsInventoryOverlay`) after the balance
  text; the item-model/item-definition JSON pair and `ModItems` registration were removed.

---

## Changelog

- **2026-07-12** — Added Section 24: the mod's first currency item, "Baum Credits"
  (`baum2:baum_credits`) — a 16x16 flat-brass-coin placeholder icon stamped with a small
  tree glyph (a pun on "Baum," the mod's namesake). Palette deliberately reuses the existing
  Deepwood & Verdigris chrome palette (Aged Brass coin body, Verdigris Glow/muted-Verdigris
  glyph) rather than inventing a new bespoke "currency" palette, per Section 1.1/1.2's
  governance rules (bespoke palettes are for boss-tier content; this is a global utility item).
  Texture only — no model/item-definition JSON or Java, per the requesting session's explicit
  scope split (those are being written in parallel elsewhere).
- **2026-07-11 (later)** — Added Section 23: the GeckoLib "sword template" weapon line and its
  first sword, the wooden training longsword "Espenklinge" (name compliance-checked CLEAR
  against Metin2's full sword list/WoW-de/ESO/GW2 et al.). One shared item geometry (4 bones/
  11 cubes, longsword silhouette from a real-world museum reference) + 3 shared animations
  (`idle` breathing loop with grip-settle accent, `attack` 360° moulinet, `attack_mounted`
  cavalry sweep — all preview-verified via `tools/render_geckolib_preview.py`, which gained
  `--fit`/`--no-floor` flags for tall thin item models), per-sword "Espenholz" texture +
  icon + 3 tiny JSONs from `tools/gen_sword_template.py`. First use of GeckoLib's item
  pipeline (`minecraft:special` → `geckolib:geckolib`) in this mod.
- **2026-07-11** — Added Section 22: full visual asset pass for the mount system's 3 rideable
  horses (Wanderross/Eisenross/Schlachtross), sharing ONE GeckoLib geometry + animation set
  (`mount_horse.geo.json`/`.animation.json`, `tools/gen_mount_horse.py`) per the "Fallen Comet
  Stone" shared-template pattern (Section 13.5), differing only by per-tier texture (own coat
  color + which of 7 baked-in armor cubes are painted vs. left alpha-0) and by a Java-side
  `withScale(...)` renderer scale (1.0/1.1/1.25, already pinned in the pre-existing
  `MountTier.java`). Produced/verified the geo.json (18 cubes/8 bones, natural horse proportions,
  ~1.4 blocks tall at the back), 3 named animations (`idle`/`walk`/`attack`, verified via
  `tools/render_geckolib_preview.py` across rest pose + keyframes for all 3), 3 entity textures
  (verified programmatically that the armor-cube alpha contract holds exactly per tier), and 3
  flute item icons. Explicitly flagged what the Java side (a `MountHorseGeoModel`/
  `MountHorseEntityRenderer`/`MountHorseEntity` + `ModEntities` registration, none of which exist
  yet) still needs to do, plus the exact model measurements (rump height, seat position, etc.)
  it needs from this pass.
- **2026-07-09** — Added Section 21: architectural/visual style spec for the Heimgrund starting
  village ("Dorfanger Hub"), covering the ~46x46 template stamped into the flat clearing at
  world center (see `HANDOFF.md`'s "Heimgrund"/"Village pipeline" notes). **Design-only pass —
  no `.nbt`/Java produced**, per this task's explicit scope; a follow-up build/capture pass is
  still needed. Establishes a full vanilla-only block palette translating Section 1's "Deepwood
  & Verdigris" mood into real material analogues (oxidized copper for "Verdigris," Soul Lantern
  for "cold rune-light"), anchors the construction style to real-world German half-timbered
  ("Fachwerk") building and the historical circular-common-green ("Rundling") village plan —
  both real-world vernacular references, not any specific game's IP — a 6-element building
  program (arrival plaza + monument, gathering hall, workshop, 2-3 cottages, herb garden,
  perimeter wall/hedge with gates), radial-ring layout guidance, and 3 original signature
  motifs (copper "Verdigris-crown" roofs marking importance, a repeated Fachwerk-plus-andesite-
  quoin construction rhythm, and Soul-Lantern cold-light reserved for meaningful thresholds
  only). Deliberately avoids both a vanilla-village-clone read (planned/radial vs. vanilla's
  organic scatter, timber-frame vs. plank-box houses) and a generic 4-cardinal-shops MMORPG-hub
  read (uneven 6-element ring vs. a symmetric cross).
- **2026-07-07** — Added Section 19.7: full Drevathis rework (GeckoLib demon-lord model with
  the blade as real geometry, six-animation set, "Umbral Sovereign" palette superseding 19.3's
  "Abyssal Sovereign", new flat icon + 3D in-hand model for Drevathis's Cursed Blade, and the
  vanilla-particle VFX color language for the reworked skill kit + per-player storm passive).
  Sections 19.1-19.6 are retained as historical record only.

- **2026-07-05** — Added Section 20.6 (bugfix follow-up): "Risssplitter" (`baum2:risssplitter`,
  the crafting material the Rissobelisk world-event block drops) had been registered as a plain
  `Item` in Java with **zero visual assets** — no texture, no item model, no item-model-
  definition wrapper — and rendered with a missing/blank texture in-game. Produced a 16x16
  placeholder icon (jagged obelisk-fragment shard silhouette, deterministic speckled Riftstone
  stone fill, single `Fissure Void` crack with a restrained `Rift Glow Edge`/`Rift Glow Pale`
  halo concentrated only at the crack's midpoint — a narrower halo treatment than the block
  texture's, deliberately kept thin so it reads as an accent rather than swallowing the shard's
  stone body) reusing Section 20.3's "Riftstone" palette unchanged (zero new colors), plus the
  standard `minecraft:item/generated` + `layer0` two-file model pattern (Gold Sword/Poison
  Dagger's pattern, not Rissobelisk's own divergent block-item pattern). Also added the missing
  `item.baum2.risssplitter` lang entry (the item had none, so it would have shown its raw
  translation key as its inventory name even with the texture fixed). No Java/gameplay code
  changed; `./gradlew build` confirmed passing. Updated Section 6's folder listing.
- **2026-07-05** — Added Section 20 (world-event landmark visual identity: "Rissobelisk",
  `baum2:rissobelisk` — the mod's **first custom `Block`**, as opposed to every prior visual
  asset which was either an `Entity`/`EntityModel` or a flat item icon). Renumbered from an
  initial Section 19 to 20 during the merge with `fischey_workbranch`, which independently
  claimed Section 19 for Drevathis the same day — see this file's own merge note if this section
  number ever looks surprising relative to git history. Produced the mod's first-ever blockstate
  JSON, block model JSON, and block-item-model JSON, all verified against vanilla's own real
  1.21.11 assets extracted from the decompiled client jar rather than guessed: the blockstate
  uses the simplest property-less single-variant form (confirmed identical to vanilla's
  `iron_block.json`); the block model uses `minecraft:block/cube_bottom_top` (confirmed
  3-texture-key schema against vanilla's `sandstone.json`) for a distinct top-face rune-sigil
  vs. side/bottom crack texture; the item model **deliberately omits** the
  `models/item/rissobelisk.json` file every prior item in this document has, after verifying
  vanilla's own `stone.json`/`iron_block.json`/`furnace.json` all point their item-model-
  definition entry point straight at the block model with no intervening file — see Section
  20.2 for the full reasoning (this is a *divergence* from the item-model pattern Section 14.3
  established, not an inconsistency with it: that pattern was for plain `Item`s with a flat-icon
  parent, a different case). Made and documented an explicit palette-bucket judgment call
  (Section 20.1): Rissobelisk doesn't cleanly fit either bucket Section 1.2's boss-vs-common-mob
  rule was written for (can't fight back, but is a rare hand-placed landmark, not a common mob)
  — decided to treat it like a boss for palette purposes (bespoke, cross-checked "Riftstone"
  palette) since Section 1.2's actual rationale was about spawn frequency/memorability, not
  mobility or combat capability, and precedent (item drops already getting bespoke palettes)
  already shows "is this rare/memorable set-piece content" was the real operative test.
  Established a new "Riftstone" palette — a cool blue-gray stone family (distinct from every
  existing warm/olive stone palette in this document) with a vivid magenta-pink "Rift Glow"
  crack/rune-energy accent (a new, previously-unused hue slice on the mod's color wheel, checked
  distinct from both existing violet families and every existing green/red glow, including
  Drevathis's own new "Grave Frost" - see Section 19 below). Produced two 16x16 placeholder
  textures (side/bottom + top) via Python + Pillow (available in this environment, unlike prior
  passes in this document which fell back to PowerShell + `System.Drawing`) — deterministic
  speckled stone base plus hand-placed branching-fissure/radiating-rune-sigil crack coordinates
  with a near/far glow-halo falloff. Added the `block.baum2.rissobelisk` lang entry (asset file,
  not code). Updated Section 6's folder listing. No Java/gameplay code touched; `./gradlew
  build` confirmed passing.
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
- **2026-07-05** — Drevathis playtest fixes (v2), reworked Section 19.2/19.4/19.5, added Section
  19.6: user actually fought the boss and reported "the demon boss... looks like a hobbit" (visual
  half of a two-part complaint; the other half - the sword never being equipped - was a real code
  bug, not a visual issue, fixed separately) and "the weapon does not look like it comes from a
  demon cursed blade." Kept the "Abyssal Sovereign" palette exactly as-is (Section 19.3 unchanged,
  not the problem per the user's own framing) and instead pushed the *execution*: regenerated
  `drevathis.png` (64x64) from scratch against `DrevathisEntityModel`'s new v2 UV layout (broader
  chest/arms, longer horns, bigger cape, new claw parts) with substantially more surface detail -
  glowing Grave Frost rune-cracks across skin and robe, an asymmetric/jagged (alpha-cut, not
  rectangular) cape hem, 3-band shadow-shading on every major face, a larger glowing-core eye
  treatment, and a new Sovereign-Blood circlet band on the previously-blank hat layer. Regenerated
  `drevathis_cursed_blade.png` (16x16) with a deliberately asymmetric jagged blade edge (oscillating
  width vs. a smooth opposite edge), a curling one-sided hook-guard accent, and Sovereign Blood
  crimson vein-branches breaking off the existing Grave Frost fuller groove - same six hexes as v1,
  new silhouette. No model/parent JSON changes were needed for the sword: verified
  `minecraft:item/handheld`'s built-in flat-item alpha extrusion already turns a jagged alpha
  silhouette into a jagged in-hand/GUI/ground shape. `./gradlew build` not re-run by this agent
  (asset/doc-only change, no Java touched); not yet re-verified in an actual game session.
- **2026-07-05** — Added Section 19 (boss visual identity: "Drevathis, the Cursed Sovereign",
  `baum2:drevathis` — the mod's current top-tier boss, level 40, above Zombie Colossus's 25 — and
  its guaranteed drop "Drevathis's Cursed Blade," `baum2:drevathis_cursed_blade`). Unlike Spider
  Queen/Zombie Colossus (both reused/rescaled a shared vanilla model), `DrevathisEntityModel` is a
  wholly bespoke `BipedEntityModel` (tall biped, two backward-curving horns, a trailing cape), so
  this pass covers a new model's UV/texture from scratch rather than a re-theme. Established a new
  "Abyssal Sovereign" palette (cool slate-gray "cursed flesh," near-black wine-plum "shroud" robes,
  a deep wine-crimson "Sovereign Blood" trim, and a pale icy-cyan "Grave Frost" curse-glow signature
  color) — deliberately not another green (unlike most of the mod's other hostile mobs) and
  deliberately not another saturated violet (unlike Royal Carapace) or warm ashen-brown (unlike
  Ashen Brute), to give this dark/regal/ancient-evil-register boss its own distinct identity;
  checked hex-by-hex against every existing palette in this document with no collisions. The
  Grave Frost curse-glow is also a deliberate departure from every prior mob's yellow-green/amber
  eye-glow family (Larval Glow, Toxic Eye, Brute Glare), matching this boss's Darkness-effect lore
  instead of the mod's existing "toxic monster" visual language. The guaranteed-drop sword shares
  its palette with the boss's own texture by design (Sovereign Blood hilt, Grave Frost fuller-glow)
  so the item reads as the same object at both its ~1.8x oversized on-boss render scale and normal
  handheld/inventory scale, per Section 14/16/18.4's established item-model-definition JSON
  pattern. Produced a placeholder 64x64 entity texture (Python + Pillow, installed into this
  environment for this pass) under `src/client/resources/...` (this boss's Java render classes are
  themselves client-only) and a placeholder 16x16 item icon, plus the real, verified 1.21.11
  model/item-definition JSON pair for the sword. Updated Section 6's folder listing. No Java/
  game-logic code was touched — visual assets and this document only.
- **2026-07-05** — Zombie Colossus playtest fixes (v2), added Section 18.6/18.7: user actually
  fought the boss and reported the attack/jump animation looked missing ("moving very static")
  and the model "has no muscles." Diagnosed the animation complaint against the decompiled
  1.21.11 client jar rather than guessing: the base walk-cycle and attack-swing plumbing was
  already correctly wired in v1 (confirmed `LivingEntityRenderer.updateRenderState` and
  `BipedEntityRenderer.updateBipedRenderState`'s real bodies both already populate the fields
  needed); the one real gap was `state.attacking` never reading `true` (this boss's custom attack
  `Goal`s never call vanilla's `setAttacking`), fixed client-side by also treating a live hand-
  swing as "attacking." The bigger real contributor was the leap/rage wind-ups being completely
  frozen — fixed with real telegraph poses (crouch-and-coil for the leap, overhead club-raise for
  rage), same pattern as Spider Queen's leap crouch, via a new `ColossusRenderState` carrying the
  two wind-up counters `ZombieColossusEntity` already exposes. Separately, replaced v1's unmodified
  reuse of `BipedEntityModel.getModelData()` with bespoke bulkier geometry (broader chest/
  shoulders, thicker arms/legs) that keeps the same standard part names so the animation logic
  above still targets the right parts — the actual fix for "no muscles," since a texture alone
  can't sell strength on a uniformly-thin vanilla skeleton. Regenerated the entity texture against
  the new UV footprints with added muscle-definition shading (sternum groove, pec/ab striations,
  bicep highlight, elbow crease). `./gradlew build` confirmed passing. Not yet re-verified in an
  actual game session (no GUI-automation tool in this environment) — flagged in 18.6 for the next
  playtest.
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
