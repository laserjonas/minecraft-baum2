# Handoff

Read this first. It reflects the state as of the latest commit so a fresh Claude Code
session (yours or a co-author's) can pick up work without re-deriving context from
`git log`. Updated after every commit — see "Git Rules" in `CLAUDE.md`.

## Current state

This reflects `fischey_workbranch`, which fast-forward-merged `origin/master` (jonas's side —
Skill System + Class Sub-specializations, "Class Overhaul v2," the Class-tab GUI follow-up, a
Visual/Art Pass, and the mod's first custom `Block`, "Rissobelisk") and then, on top of that,
the **GeckoLib migrations/reworks** (Spider Queen, Zombie Colossus, Drevathis, both stone
mini-bosses remodeled as a shared "Fallen Comet Stone" template, and the 33-stone ladder),
**Heimgrund, the mod's starting dimension** (finite authored world, protected
village hub, zone-tiered daylight monster spawns, respawning stone slots; see the "Heimgrund"
section above), and — newest, 2026-07-11, uncommitted working tree — **the Mount System v1**
(three summonable rideable horses via flute items, Ctrl+H toggle, equipment inventory with a
mount slot, mounted-combat tier rules, GeckoLib-animated; see the "Mount System v1" section
below) **plus the GeckoLib Sword Template v1** (the mod's first animated GeckoLib ITEM: a
reusable sword line — shared geometry/animations, per-sword texture — whose first sword is
the wooden "Espenklinge": idle/attack/mounted-attack animations, undroppable, uncraftable;
see the "GeckoLib Sword Template v1" section below). `master` is fast-forwarded to match this
branch up to the Heimgrund commit. See "Last change" below for detail.

- Fabric mod builds successfully (`./gradlew build` passes).
- Client runs: `./gradlew runClient` loads, reaches the main menu, and joins a world cleanly
  (verified clean boot 6 times this session, no Mixin/payload/HUD-registration
  errors/exceptions in the log). **Both the Class Overhaul v2 feature set (spell scaling, Mana
  costs, sub-spec forks, respec cooldowns) and its GUI follow-up (Class tab sub-spec/spell
  cards) are user-confirmed working in-game** ("worked fine" / "works"). Not itemized back
  against every line of the original playtest checklist (see "Next recommended step" item 0,
  kept as a reference in case any specific sub-item still needs a closer look), so treat this
  as "no reported breakage from real play," not an exhaustive per-mechanic sign-off. **The
  Visual/Art Pass's 8 sub-spec icons are also user-confirmed rendering correctly in-game**,
  including the subtlest pair (Schattenpirscher vs. Sturmklinge). **Rissobelisk's core mechanic
  (place, attack, wave-spawn, destroy) is user-confirmed working** — the user's own playtest
  caught 2 real bugs (missing Risssplitter icon; the block's own `BlockItem` showing its raw
  untranslated name instead of "Rissobelisk"), both fixed and build-verified, but **the fixes
  themselves have not yet been re-confirmed in a live session** — see "Next recommended step".
- Package: `de.baum2dev.baum2` / Main: `Baum2` / Client: `Baum2Client`.
- Minecraft 1.21.11 / Yarn 1.21.11+build.6 / Fabric API 0.141.4+1.21.11 / Fabric Loom 1.17.13 / Java 21.

### Progression System — FULLY WORKING, including persistence

- Custom progression uses our own XP curve, centralized in `progression/ProgressionCurve.java`
  (renamed from `VanillaXpFormula` — it stopped being vanilla's actual formula, see below;
  single source of truth for `ExperienceManager`, `ProgressionTickHandler`,
  `PlayerLevelSystem`, `LevelUpHandler`, and the client packet handler):
  `xpRequiredForLevel(L) = 80 + 40L + 8L²` — a deliberately steeper "hardcore grind" pace than
  vanilla's own curve, chosen because our `MobDeathHandler` grants 10-60+ XP per kill (vanilla
  orb pickups are 1-7 XP), so vanilla's gentle curve let a handful of kills jump a player 1→8.
  Concretely: level 1 costs 128 XP (~6 kills), level 10 costs 1280 XP (~64 kills), level 100
  costs 84,080 XP for that one level (~4,200 kills); cumulative total to level 100 from
  scratch is ~2.9M XP.
- Features: `/baum2 addxp <amount>`, `/baum2 level`, mob XP drops (10 + max_health/2), level-up
  broadcasts, vanilla XP orb drops disabled via Mixin.
- **Real-time client sync** via a custom S2C packet sent every server tick (see "Networking
  API reference" below).
- **Persistence now works** via Fabric's Data Attachment API (`fabric-data-attachment-api-v1`)
  — progression survives server restarts, disconnects, and (via `.copyOnDeath()`) death. See
  "Attachment API reference" below. Replaced the old in-memory `HashMap<UUID, ...>` approach.
- **`balance-reviewer` reviewed the old (pre-rebalance) curve and old mob-XP interaction** —
  its logged finding ("a single strong kill can vault a low-level character through many
  levels") is **substantially addressed by the curve rebalance above**. Caveat: a single very
  large `/baum2 addxp` grant (700+) can still vault ~3 levels — addressed for the actual
  mob-kill case, not structurally eliminated for all lump-sum grants. **Worth a fresh
  `balance-reviewer` pass against the new curve combined with the attribute/combat system
  below** (not done yet) rather than treating this as fully closed.
- Other balance-reviewer findings, still relevant: `ExperienceManager.getMaxLevel()`'s
  100-level cap is declared but not enforced anywhere; free/instant class reselection (see
  Class System below) lets a player capture all four classes' bonuses contextually;
  Runenwirker's luck bonus has no loot system yet to act on.

### Vitals System — Life, Mana, HUD rework (replaces vanilla heart bar)

- `progression/VitalsCurve.java` is the single source of truth. **Life is Endurance-driven**
  (not level-based — an earlier formula was explicitly replaced per user decision):
  `getMaxLife(endurance) = 500 + 20*(endurance-5)` (500 at start, 2480 at max Endurance 104).
  Still real vanilla `MAX_HEALTH`, rescaled via `VitalsManager.applyMaxLife` so combat/damage/
  death/regen keep working unchanged. Vanilla clamps `MAX_HEALTH` at 1024 by default — widened
  to **4096** via an accessor Mixin (`mixin/ClampedEntityAttributeAccessor.java`), applied
  once at mod init (`VitalsManager.widenMaxHealthCeiling()`, called first in
  `Baum2.onInitialize()`, **before** `PlayerLevelSystem.bootstrap()` — order matters, see
  "Attachment API reference"). Endurance also drives Life Regen (0.25/sec at start).
- Mana is level-based (`100 + 5*level`), unchanged, not attribute-driven.
- HUD: `ui/VitalsHud.java` replaces the vanilla heart bar in place
  (`HudElementRegistry.replaceElement(VanillaHudElements.HEALTH_BAR, ...)`, **not**
  `removeElement` — removing entirely crashes the client, see `docs/fabric-modding.md`) and
  adds a Mana bar above it. Colors/dimensions in `docs/visual-style-guide.md` section 11.
- Networking: `networking/ManaSyncPayload.java` (S2C, every tick).

### Attribute System — 4 attributes, Character Stats Screen ('C' key)

- `progression/AttributeType.java` (enum: Endurance/Intelligence/Strength/Dexterity),
  `progression/AttributeManager.java` (grants 1 unspent point per level-up via
  `ExperienceManager.levelUp()`, validates spending server-side). Each attribute starts at 5;
  max attainable via pure leveling is 5+99=104 (max level 100).
  - **Strength** → Base Attack + Physical Defence (`5 + 1.0*(str-5)` each).
  - **Intelligence** → Base Magic Attack + Magic Defence (same shape).
  - **Dexterity** → Attack/Cast Speed Multiplier (`1.0 + 0.01*(dex-5)`, capped at 3.0x) and
    Crit Chance (`5 + 0.5*(dex-5)`, capped at 75%). Neither cap actually binds via pure
    leveling (max reachable Crit Chance is 54.5%) — intentional headroom for a future
    gear/skill system, not a currently-reachable limit.
  - **Physical Attack, Attack Speed, and Crit Chance are now wired into real combat** — see
    "Combat System v1" below. **Magic Attack/Magic Defence remain display-only** — the new
    Skill System (see below) deals spell damage via flat, hardcoded numbers, not scaled by
    Intelligence yet; Physical Defence also remains display-only (no incoming-damage-reduction
    system exists).
- Networking: `networking/AttributeSyncPayload.java` (S2C, every tick, carries only the 4 raw
  ints + unspent points — derived stats are computed client-side from the same `VitalsCurve`
  methods, not synced separately). `networking/SpendAttributePointPayload.java` — **this
  project's first C2S payload** (`PayloadTypeRegistry.playC2S()` +
  `ServerPlayNetworking.registerGlobalReceiver`), sent when a Stats-screen "+1" button is
  clicked; also immediately re-applies the real `ATTACK_DAMAGE`/`ATTACK_SPEED` modifiers via
  `VitalsManager.applyBaseAttack`/`applyAttackSpeed` so the new Strength/Dexterity value takes
  effect without needing a relog. Old `Base Damage`/`Base Magic Damage` flat fields and
  `CombatStatsSyncPayload` are gone, fully superseded by these formulas.
- UI: pressing **C** (`ui/Baum2KeyBindings.java`, `KeyBinding.Category.MISC`) opens
  `ui/CharacterStatsScreen.java` — a full menu `Screen` built on vanilla's real
  `Tab`/`TabManager`/`TabNavigationWidget`/`GridScreenTab` system. Now **two tabs**: "Stats"
  (15 rows: Life, Mana, Unspent Points, then each attribute interleaved with its derived
  stats) and "Class" (added this session — see "Skill System v1 & Custom UI" below). Content
  wrapped in vanilla's `ScrollableLayoutWidget` (fixes a real bug where bottom rows were
  unreachable at high GUI Scale) and given an opaque panel background (fixes a real bug where
  vanilla's default darkening was too weak against a bright sky, and content overlapped the
  tab header — see git history for both root causes). Row order/colors in
  `docs/visual-style-guide.md` section 12.
- **"+1" attribute buttons polished per user feedback**: label shortened to "+", size reduced
  18x18 → 12x12. Clicking now updates the displayed attribute/points values **immediately**
  client-side (`ClientNetworkingHandler.predictAttributeSpend`), rather than waiting for the
  next `AttributeSyncPayload` tick — the server stays authoritative and corrects this within
  ~1 tick if the optimistic update was ever wrong, so there's no real desync risk, just a
  smoother-feeling button. When `Unspent Points` is 0, the buttons are now `.visible = false`
  (fully hidden, not just greyed out via `.active`).
- **Two real, confirmed-not-guessed bugs were fixed during this system's development** (both
  found via actual user screenshots, not caught by build/boot verification alone): the tab
  header overlapping content (root cause: `GridScreenTab`'s default centering anchor goes
  negative once content is taller than the tab area — fixed via a `refreshGrid` override),
  and bottom rows being unreachable at high GUI Scale (fixed via `ScrollableLayoutWidget`).
  **General lesson for future custom `Screen`s in this codebase**: override `refreshGrid`
  (or equivalent) for any content that might grow past a single screen's worth of rows, and
  don't assume build success + clean boot means the UI actually renders/scrolls correctly —
  it doesn't catch layout bugs.

### Combat System v1 — Physical Attack/Attack Speed/Crit Chance now affect real combat

User reported these three stats had zero effect when attacking a monster (correct — they were
pure display values). Wired all three into real vanilla combat mechanics rather than building
a parallel damage system:

- **Base Attack (Strength)**: `VitalsManager.applyBaseAttack` adds a persistent `ADD_VALUE`
  modifier (fixed `Identifier`, `overwritePersistentModifier` — same pattern as the Class
  System's own attribute bonuses) to the player's real `EntityAttributes.ATTACK_DAMAGE`.
  Stacks additively with whatever weapon they're holding.
- **Attack Speed Multiplier (Dexterity)**: `VitalsManager.applyAttackSpeed` adds a persistent
  `ADD_MULTIPLIED_TOTAL` modifier to `EntityAttributes.ATTACK_SPEED`, value =
  `(multiplier - 1.0)` **not** the multiplier itself — confirmed via
  `EntityAttributeInstance.computeValue()`'s actual decompiled body that this operation
  computes `total *= 1.0 + value`, so a "+50%" bonus needs modifier value `0.5`.
- Both modifiers are **persistent** (survive relogin/restart on their own) — only re-applied
  on join and immediately after a successful attribute-point spend, not every tick.
- **Crit Chance (Dexterity)**: new Mixin `mixin/PlayerAttackDamageMixin.java`. No Fabric API
  event can modify a melee attack's final damage float (`AttackEntityCallback` is cancel/allow
  only, fires before damage is computed) — confirmed by reading the actual event package
  source. Uses a plain Sponge `@ModifyArg` targeting `Entity.sidedDamage(DamageSource, float)`
  inside `PlayerEntity.attack(Entity)`, rolls `Math.random()*100 < critChance` server-side only
  (guarded by `instanceof ServerPlayerEntity`), multiplies by a flat
  `CRIT_DAMAGE_MULTIPLIER = 1.5f` (a number picked to match vanilla's own fall-crit
  multiplier, not user-specified).
- **`balance-reviewer` finding, real and escalating, flagged for a human decision — see "Next
  recommended step"**: Base Attack (linear), Attack Speed (linear → more attacks/sec), and
  Crit Chance (linear EV) all multiply together in actual combat, so **total DPS grows
  superlinearly with investment** — ~8x baseline DPS at 25 points invested in each of
  Strength/Dexterity, ~46x baseline at max level (99 points each). An iron sword deals ~11
  damage at start, ~36 at 30 Strength, ~110 at max Strength — **characters one-shot most
  vanilla mobs (20 HP) from roughly 25 invested Strength points onward**, well before max
  level. The per-stat formulas were already reviewed/approved before combat existed to consume
  them — this compounding effect is a genuine new consequence of wiring them together.
- **Escalated further by two boss-weapon drops in a row** (see "Stone of Zombies" below): Gold
  Sword nudged the ceiling +12.5% (its attack-speed tuning is faster than vanilla for its
  damage tier), Poison Dagger pushed it another +145% (reaching ~110x baseline at max
  investment), because Dexterity's Attack-Speed modifier is multiplicative on top of whatever
  speed the weapon already has — a fast weapon's speed advantage survives to max investment
  essentially undiluted, while flat damage differences between weapons get swamped by
  Strength's own large additive bonus. **This is now a "Balance decision needed" item spanning
  three separate findings (base combat wiring + 2 weapons) — see "Next recommended step".**
- Also flagged, not fixed (minor, likely-intentional): vanilla's own fall-crit (1.5x) already
  applies before this Mixin runs, so a fall-attack crit + a Dexterity crit roll stacks to
  `1.5 × 1.5 = 2.25x`, uncapped — reads as an acceptable rare-but-fun outcome, not written down
  as an explicit decision.
- Verified: build passes, boots cleanly with the Mixin applied. **Not verified in an actual
  fight** by the session that wrote it — see per-mob "Next recommended step" items below.

### Target nameplate — mob name + level + health bar, top-center

Client-only HUD element, `ui/MobNameplateHud.java`, registered via `HudElementRegistry.
addLast(...)` (independent element, not attached to `VanillaHudElements.BOSS_BAR`). Shows
name/level/health bar of a targeted living entity.

- Prefers a recently-**attacked** entity (cached 5s, via `AttackEntityCallback` — confirmed
  this fires client-side too, Mixin'd into `ClientPlayerInteractionManager.attackEntity`) over
  the live crosshair target, since fast/erratic mobs (e.g. spiders) often aren't precisely
  under the crosshair the instant a hit lands.
- "Lvl. X" via a new `entity.MonsterLevelProvider` interface — currently only implemented by
  the mini-bosses below; every other mob still shows a hardcoded "Lvl. 1" placeholder.
- **Real bug found and fixed, same root cause `docs/fabric-modding.md` had already documented
  from an earlier session's dead HUD prototype**: `DrawContext.drawText*` silently no-ops on a
  color with a zero alpha byte. This file's text calls originally passed bare `0xFFFFFF`
  instead of `0xFFFFFFFF` — the exact already-documented mistake, made fresh anyway. Fixed via
  `net.minecraft.util.Colors.WHITE`. **General lesson worth internalizing**: any raw color
  `int` passed directly to `drawText*` (not via `Text.styled(style -> style.withColor(...))`,
  which is unaffected) needs a non-zero alpha byte.
- Bar styling reuses the Life bar's exact ember/coral hexes. Positioned at vanilla's boss-bar
  starting y, top-center; `PlayerStatusHud`'s old top-left slot is gone (see "Custom UI"
  below) so there's no collision risk there anymore.

### Class System v1 — 4 classes, command + GUI selection

- `classes/` package: `PlayerClass` (enum: `EISENWAECHTER`, `SCHATTENLAEUFER`, `RUNENWIRKER`,
  `WESENSWAHRER`), `ClassDefinition` (record), `ClassRegistry` (static lookup), `ClassManager`
  (persistence + apply/remove bonus + its own join listener).
- Commands: `/baum2 class list`, `/baum2 class info [<class>]`, `/baum2 class select <class>`.
- Persistence via Fabric's Attachment API, synced to the client too
  (`.syncWith(PacketCodecs.STRING.xmap(...), AttachmentSyncPredicate.targetOnly())`).
- Passive bonuses: Eisenwächter +4 max health, Schattenläufer +10% movement speed, Runenwirker
  +1 luck, Wesenswahrer +10% knockback resistance — each a stable-`Identifier`
  `EntityAttributeModifier`, swapped cleanly on reselection/rejoin.
- **4th class renamed `Seelenhüter` → `Wesenswahrer`** (`ip-naming-compliance-checker` found
  `Seelenhüter` was an exact match to *Echo of Soul*'s player-character title). **Wesenswahrer's
  bonus attribute changed from `MAX_ABSORPTION` (a confirmed no-op) to `KNOCKBACK_RESISTANCE`**
  (`balance-reviewer` finding).
- **Fine-grained `/attribute` verification is still the one open gap** for this system (see
  "Next recommended step") — a quick manual check confirmed class selection/switching *works*
  end-to-end via both the command and the GUI, but the exact numeric modifier values
  (`/attribute @s ... modifier value get baum2:class_bonus/...`) haven't been checked.
- **Class reselection is no longer free/instant** — see "Class Overhaul v2" below (30-minute
  respec cooldown). **Still logged, not fixed**: Runenwirker's luck has no loot system yet to
  act on.

### Skill System v1 — 8 active spells, keybind + command casting

- `skills/Spell.java` (enum, 8 values, 2 per class) + `skills/SpellEffects.java` (the actual
  gameplay effects) + `skills/SkillCooldownManager.java` (in-memory per-player-per-spell
  cooldown tracker, deliberately not Attachment-backed — cooldowns are supposed to reset on
  relog/restart) + `skills/SpellCaster.java` (shared cast-attempt logic used by both entry
  points below, so they can't drift apart).
- **Names are this project's own original placeholders from `MASTERPROMPT.md`'s own
  "Skill-System" section**, now actually implemented for the first time: Eisenwächter
  (Schildstoß, Standhafte Aura), Schattenläufer (Nebelschritt, Klingenwirbel), Runenwirker
  (Runenfunke, Arkaner Kreis), Wesenswahrer (Lebensband, Geisterwoge).
- **Two ways to cast, both calling the same `SpellCaster.attemptCast`**:
  - **Primary: keybinds.** `ui/SpellCastKeyBindings.java` registers **V** ("Cast Spell 1") and
    **B** ("Cast Spell 2") — pressing one casts whichever spell is slot 0/1 for the player's
    *current* class (`SpellCaster.spellForSlot`), not a fixed spell — so only 2 keybinds are
    needed total, not 8. New C2S `networking/CastSpellPayload.java` (carries just a slot int)
    backs this.
  - **Fallback/testing: `/baum2 cast <spell>`** — kept for direct testing access, same pattern
    as this project keeping both a command and a GUI for class selection.
- This started from an explicit request to "get inspired by Metin2's Warrior/Sura/Shaman/Ninja
  classes and their sub-specs" — that was rejected as direct IP copying (Metin2 named
  specifically), and resolved with the user in favor of building original spells/sub-specs
  from this project's *own* already-established class identities instead.
- **Damage/duration/distance now scale off the caster's attributes, and spells cost Mana** —
  see "Class Overhaul v2" below. (No spell crits yet — that remains a deliberate future
  decision, not done.)
- **New design-adjacency risk, found by `merge-integration-reviewer` while merging in master's
  combat/mob work, not yet resolved**: `combat/PoisonDaggerHandler.java` (master) listens on
  `ServerLivingEntityEvents.AFTER_DAMAGE` and applies Poison to *any* damage a player holding
  the Poison Dagger causes — it doesn't check the hit was a melee swing. The Skill System's
  three damage spells (`schildstoss`, `klingenwirbel`, `runenfunke`) deal damage via
  `target.damage(world, indirectMagic(player, player), amount)`, whose `DamageSource.
  getAttacker()` also resolves to the player. **Net effect: a player holding a Poison Dagger
  who casts one of those three spells will also poison every target the spell hits** — an
  emergent interaction neither branch intended or tested. Not a build break. Worth a conscious
  decision: scope the handler to melee-only (e.g. check a damage-type tag), or accept it as an
  intentional-feeling combo.

### Class Sub-specializations v1 — 2 per class, layered on top of the base class bonus

- `classes/ClassSubspec.java` (enum, 8 values, 2 per `PlayerClass`) + `classes/
  SubspecDefinition.java` (record, mirrors `ClassDefinition`) + `classes/SubspecRegistry.java`.
  `ClassManager` gained a second `AttachmentType` (`SELECTED_SUBSPEC`, same
  persistent+copyOnDeath+syncWith pattern as `SELECTED_CLASS`) and `selectSubspec(...)`, which
  validates the chosen sub-spec actually belongs to the player's current class and clears the
  old sub-spec's bonus/attachment whenever the base class changes.
- Commands: `/baum2 class subspec list`, `/baum2 class subspec select <subspec>` — no GUI yet
  (see "Next recommended step").
- Final names, after one naming-compliance round caught 3 problems (see below): Eisenwächter
  (Bollwerk +armor, Stahlfaust +attack damage), Schattenläufer (Schattenpirscher +attack
  damage, Sturmklinge +attack speed), Runenwirker (Splitterrune +attack damage, Glücksrune
  +luck), Wesenswahrer (Wurzelwall +knockback resistance, Wesensfülle +max health).
- **`ip-naming-compliance-checker` flagged and got 3 of the original 8 sub-spec names
  renamed** before anything shipped — these weren't generic-word overlaps, they were exact or
  near-exact matches to specific, well-known WoW German spec/spell names: "Vergelter" (→
  Stahlfaust — matched WoW Paladin's actual "Vergeltung"/community "Vergelter-Paladin"
  shorthand), "Wildwuchs" (→ Wesensfülle — exact, unmodified match to WoW Druid's real spell
  "Wild Growth"), "Zerstörungsrune" (→ Splitterrune — contained WoW Warlock's actual
  "Zerstörung"/Destruction spec name; the replacement reuses this project's own pre-existing
  "Splitter" motif). All 3 replacements re-checked clean. Also double-checked: the "4 classes
  × 2 sub-specs" *structure* doesn't read as Metin2-specific, and each class's specific split
  was deliberately varied per class rather than one uniform template.
- **`balance-reviewer` found and fixed one real bug**: Wurzelwall was `ADD_MULTIPLIED_BASE`
  while Wesenswahrer's own class bonus (which it's meant to stack with, like Glücksrune
  correctly does with Runenwirker's luck) is `ADD_VALUE` — vanilla's attribute formula means
  those two operations don't combine the way "+10% on top of +10%" suggests (nets ~11%, not
  20%). Fixed to `ADD_VALUE` to match.
- **`balance-reviewer` also found Lebensband (the "heal" spell) was non-functional**: a flat
  6 HP heal against Life's new post-Vitals-rework scale (500-2480) is outpaced by passive Life
  Regen after just ~3 Endurance points. Fixed to heal 12% of max life instead of a flat amount.
- **Mana costs and respec cooldowns are no longer missing** — see "Class Overhaul v2" below.
  **Still logged, not fixed**: none of the 4 AoE/damage spells do a line-of-sight/raycast check,
  so they can hit through walls; `Nebelschritt`'s dash has no collision check; AoE effects don't
  filter out other players, so they hit friend and foe alike; `Arkaner Kreis`'s 25s cooldown /
  15s duration lets 2 coordinated players keep its Luck buff at 100% uptime (currently
  low-impact only because Luck has no loot-system consumer yet).

### Class Overhaul v2 — spell scaling, Mana costs, sub-spec spell forks, respec cooldowns

Triggered by direct user feedback ("the classes are still lame") after Skill System v1/
Sub-specializations v1 shipped: classes were one flat stat bonus each, spells never scaled
with the character, Mana had zero consumers, sub-specs only differed by which stat they bumped,
and class/sub-spec reselection was free/instant/unlimited. Planned and approved via Plan Mode
before implementation (4 independently-buildable steps); see `git log -p HANDOFF.md` for the
original plan narrative if needed.

- **1. Spell scaling** (`skills/SpellEffects.java`): each class's damage/heal spell now scales
  off its identity attribute via a new `scaledDamage(...)` helper reading pure
  `VitalsCurve.getBaseAttack`/`getBaseMagicAttack` functions — Schildstoß/Klingenwirbel off
  Strength, Runenfunke off Intelligence, Lebensband unchanged (already a %-of-max-life heal).
  The 4 utility/buff spells' duration/distance now scale off their own flavor's attribute
  (Standhafte Aura/Geisterwoge off Endurance, Nebelschritt off Dexterity, Arkaner Kreis off
  Intelligence), each capped and reproducing today's old flat value at starting stats (5) — no
  regression for a fresh character. **Deliberately reads only `VitalsCurve`'s pure stat
  functions, never the player's live `EntityAttributeInstance`** (documented in a class-level
  javadoc) — spell damage is dealt via `target.damage(world, source, amount)` directly, never
  `PlayerEntity.attack()`, so it structurally bypasses the crit Mixin and the real
  `ATTACK_SPEED` attribute; reading the live attribute here would let spells inherit the
  already-flagged melee "DPS ceiling" (see "Combat System v1" above) on top of their own
  scaling. `balance-reviewer` confirmed numerically: at max investment (104), spells top out at
  ~4.7-16 DPS per target vs. melee's own ~260-280+ DPS ceiling (before the Poison Dagger
  escalation) — spells stay an order of magnitude below melee, as intended.
- **2. Mana costs** (`skills/Spell.java`, `SpellCaster.java`): first real consumer of Mana
  anywhere in the project. Flat per-cast cost (Runenfunke 20, Schildstoß/Klingenwirbel 25,
  Nebelschritt 15, Geisterwoge 20, Lebensband/Standhafte Aura 30, Arkaner Kreis 40), charged on
  every attempt that passes class+cooldown+Mana checks (even a whiff) — matches the existing
  precedent that cooldown is already spent unconditionally. New `SpellCaster.Result
  .INSUFFICIENT_MANA`, surfaced by both the command and the V/B keybind path.
- **3. Sub-spec spell forks** (`skills/SubspecSpellEffects.java`, `SpellVariantRegistry.java`,
  new files): each class's 2 sub-specs now fork that class's damage/heal spell into genuinely
  different behavior, not just a different stat — Bollwerk/Wurzelwall (defensive per class) add
  a self-Resistance follow-up; the offensive sub-spec per class gets a distinct mechanic:
  Stahlfaust trades Schildstoß's knockback for +damage, Sturmklinge makes Klingenwirbel hit
  twice (5 ticks apart, new `skills/DelayedSpellEffectScheduler.java` — a generic one-shot
  delayed-effect list, same tick-tracked shape as `combat/BurnDamageManager`), Schattenpirscher
  adds Weakness to Klingenwirbel's targets, Splitterrune makes Runenfunke also strike a second
  nearby enemy, Glücksrune gives Runenfunke a 20% chance to skip its own cooldown, Wesensfülle
  extends Lebensband's heal to nearby allies. Resolution is one `flatMap` lookup inside
  `SpellCaster.attemptCast` — a base-spell cast with no matching fork just falls through to the
  spell's own default effect, so both existing entry points (command + keybind) can't drift
  apart, and a 9th forked spell later needs no new branch, just a map entry.
- **4. Respec cooldowns** (`classes/RespecCooldownManager.java`, new; `ClassManager.java`):
  class reselection now gated by a 30-minute cooldown, sub-spec reselection by 5 minutes —
  independent tracks, so a fresh class pick's immediately-following first sub-spec pick stays
  free (the "first pick free" rule reuses the existing `Optional.empty()` check, no new state
  needed). `ClassManager.selectClass`/`selectSubspec` changed from `void`/`boolean` to a
  `SelectAttempt(SelectResult, remainingCooldownTicks)` record (mirrors `SpellCaster
  .CastAttempt`'s shape); `Baum2Commands` and `Baum2Networking`'s `ClassSelectPayload` receiver
  updated to report the cooldown. No currency/gold system exists in this project to build a
  cost-based respec instead, and respec is the *only* way to ever try a different class, so the
  duration was chosen to add real friction without blocking experimentation outright.
- **`balance-reviewer` found and fixed two real bugs before this shipped**:
  1. **Glücksrune's "skip cooldown" fork was dead code.** `SpellCaster.attemptCast` called
     `SkillCooldownManager.recordCast(...)` *after* the resolved effect ran — so on Glücksrune's
     20% roll, its own `clearCooldown(...)` call (inside the effect) got immediately overwritten
     by that unconditional `recordCast`, meaning Runenfunke always ended up on the same cooldown
     regardless of the roll. **Fixed** by reordering `recordCast` to run *before* the effect,
     so a fork's own cooldown manipulation is the last write, not the first.
  2. **Respec cooldowns were trivially bypassable via a singleplayer world restart.**
     `RespecCooldownManager` originally mirrored `SkillCooldownManager`'s in-memory
     `Map<UUID, Long>`/`server.getTicks()` shape — fine for spell cooldowns (6-25 seconds,
     restarting the game isn't faster than waiting) but not for a 30-minute gate: quitting and
     relaunching a singleplayer world restarts the integrated server's JVM, wiping the map,
     while `SELECTED_CLASS`/`SELECTED_SUBSPEC` themselves survive via persistent Attachments —
     letting a player bypass the entire feature in ~20-30 seconds, repeatably. **Fixed** by
     switching `RespecCooldownManager` to two new persistent, wall-clock (`System
     .currentTimeMillis()`, not `server.getTicks()` — tick count itself resets across a
     restart) Attachments, same pattern as `ClassManager`'s own `SELECTED_CLASS`. Force-loaded
     via a `bootstrap()` no-op called from `ClassManager.registerEvents()` (same package, same
     "must eagerly touch the class before any player can join" gotcha documented below under
     "Attachment API reference").
- **`balance-reviewer` findings, logged, not fixed (design/judgment calls)**: (a) Mana only
  creates real scarcity for the 3 "nuke" spells (Schildstoß/Klingenwirbel/Runenfunke), and even
  those become fully regen-sustainable by level ~20-30 out of the 100-level curve — the other 5
  spells are already sustainable solo from level 1, so Mana reads as "matters early, cosmetic
  later" rather than a lasting resource; (b) Wesenswahrer specifically never experiences Mana
  scarcity at all, even at level 1 (both its spells are regen-sustainable from the start) — the
  other 3 classes each have one Mana-constrained spell, so this is a cross-class asymmetry if
  equal Mana tension across classes was intended; (c) 3 of the 4 damage-spell forks are
  uncapped-target-count AoE while melee is single-target, so "spells stay below melee's DPS
  ceiling" is only guaranteed per-target, not for total output against a large enough group —
  plausibly an intentional AoE-vs-single-target tradeoff, not verified as a deliberate one.
- **User-confirmed working in-game** ("worked fine") after this shipped — the exact
  feature-level checklist that was pending (cast each spell, watch Mana drop, exercise the two
  fixed bugs specifically) has not been itemized back point-by-point, so treat this as "no
  reported breakage" rather than an exhaustive per-item confirmation.

### Class Overhaul v2 follow-up — sub-specs and spells added to the Class tab GUI

Immediate follow-up request after the above shipped: sub-spec selection and spell casting were
still command-only (`/baum2 class subspec select`, `/baum2 cast`) with zero GUI presence,
despite the Class tab already existing for base-class selection.

- **New C2S payload** `networking/SubspecSelectPayload.java` — mirrors `ClassSelectPayload`
  exactly (a `ClassSubspec` ordinal), registered in `Baum2Networking`; the server receiver
  calls `ClassManager.selectSubspec` and reports an `ON_COOLDOWN` result the same way the
  existing `ClassSelectPayload`/`CastSpellPayload` receivers already do (`WRONG_CLASS` is
  silently ignored — the GUI only ever offers sub-specs belonging to the player's current
  class, so it can only happen for a stale client render, not a real user action).
- **`CharacterStatsScreen.java`'s "Class" tab** now has 2 new sections below the existing class
  card list: **Sub-specializations** (2 clickable cards, same visual language as the class
  cards — now including a 16x16 icon, see "Visual/Art Pass" below — sends `SubspecSelectPayload`
  on click) and **Spells** (2 clickable cards showing name/Mana cost/cooldown, sends
  `CastSpellPayload(slot)` on click — **identical payload the V/B keybinds already send**, so a
  GUI click and a keypress are indistinguishable to the server, no parallel cast path to keep
  in sync). Both sections show a placeholder message instead when no class is selected yet.
- **Implementation choice**: rather than pre-building all 8 sub-spec cards / 8 spell cards and
  toggling visibility (which would need the grid to reserve worst-case space, or dynamic
  row insertion), exactly 2 sub-spec-card and 2 spell-card widgets exist and get *repointed* at
  whichever definitions belong to the currently selected class every `refreshValues()` call —
  `SubspecRegistry.forClass(...)` and `SpellCaster.spellForSlot(...)` (both pre-existing) supply
  the 2-per-class data directly, no new registry/lookup needed.
- **`ClassCardWidget`'s `formatBonus`/`attributeLabel` helpers were widened**, not duplicated —
  changed from taking a whole `ClassDefinition` to taking the 3 raw fields
  (`attribute, operation, amount`), since `SubspecDefinition` needed the identical formatting
  logic. Also added 3 missing German attribute labels the sub-spec cards needed that the class
  cards never exercised (`armor`→"Rüstung", `attack_damage`→"Angriffsschaden",
  `attack_speed`→"Angriffstempo").
- **`ClassTab` is now wrapped in a `ScrollableLayoutWidget`**, matching `StatsTab`'s own fix for
  the exact same class of bug (see "Attribute System" above, "two real bugs" note) — the tab
  now has enough rows (4 class + 2 sub-spec + 2 spell cards plus headers/spacers) to plausibly
  overflow a single screen at high GUI Scale, so it gets the same treatment proactively instead
  of waiting for another screenshot to prove it.
- **`ip-naming-compliance-checker`: clear** — all new strings ("Sub-specializations", "Spells",
  the placeholder sentence, the 3 new German attribute labels) are generic UI chrome/dictionary
  words, not names; no new item/skill/mob/boss/faction names were introduced.
- **User-confirmed working in-game** ("works") — build passes, client boots cleanly, and the
  new Class tab sections were manually tested.
- **Sub-spec icon art was the deferred item here — now done, see "Visual/Art Pass" below.**
  Still deferred: live cooldown-remaining display on spell cards (currently shows the *static*
  cooldown length, not time-until-ready, since no client-side cooldown-sync payload exists — `SkillCooldownManager`
  is server-only in-memory).

### Visual/Art Pass — 8 sub-spec icons, two long-open palette decisions resolved

User asked to "plan the next steps" after the above shipped; offered a choice of tracks
(balance decision, mini-boss playtest, new Priority-1 content, visual/art) and the user picked
**visual/art pass**. Planned via Plan Mode, then dispatched `graphics-designer` for the actual
asset/doc work (its job per `CLAUDE.md`; Java UI wiring is not, so that part was done directly
afterward). **Scoping note carried through the whole pass**: `graphics-designer` is text-based
with no image-generation tool — it can produce specs and simple flat-fill placeholder textures,
not genuinely hand-painted art. "Real" (non-placeholder) art for the class icons/mini-bosses/
items still needs a human artist or an external image tool; not attempted here.

- **8 new sub-spec icons** (`assets/baum2/textures/gui/subspec/*.png`, one per `ClassSubspec`) —
  each is its parent class's *exact* existing 16x16 icon (same pixel mask, same fill/outline
  hex, Section 3.3) plus one small overlay detail reflecting that sub-spec's own bonus/flavor
  text (e.g. Bollwerk = a second armor-plate seam; Stahlfaust = a diagonal impact-crack; full
  table in `docs/visual-style-guide.md` Section 9.1). No new colors introduced anywhere.
- **Verified before trusting the deliverable** — the dispatching agent's own completion note
  flagged that the safety classifier was unavailable to review the subagent's work, so a manual
  check was done rather than taking the summary at face value: confirmed all 8 PNGs exist,
  16x16 RGBA, exactly 3 colors per file (transparent/fill/outline matching the parent), and ran
  a pixel-level diff confirming every sub-spec icon is genuinely different from its parent (1-10
  differing pixels) *and* from its sibling sub-spec (4-14 differing pixels) — not a copy-paste
  fabrication. Schattenpirscher/Sturmklinge's 4-pixel sibling difference is the most subtle of
  the four pairs; acceptable for a placeholder tier, worth a closer look if this ever gets a
  real-art pass.
- **Wired into the UI**: new `ui/SubspecIcons.java` (mirrors `ClassIcons.of(PlayerClass)`).
  `SubspecCardWidget` in `CharacterStatsScreen.java` now draws the icon at the same position/
  size `ClassCardWidget` uses (`x+6,y+4`, 32x32 on-screen from the 16x16 source) — required
  bumping `SubspecCardWidget.CARD_HEIGHT` 34→40 to match `ClassCardWidget` exactly so the icon
  doesn't overflow the card, and shifting text start x from `x+6` to `x+46` to make room.
- **Two long-open palette questions resolved as firm decisions, not further deferrals**
  (documentation-only — no Java/hex constants touched), both in `docs/visual-style-guide.md`:
  - **Section 1.1**: the "Deepwood & Verdigris" menu-chrome palette and the independently-
    designed "Vitals & Attributes" combat-HUD/Stats-screen palette (open since an earlier
    merge) are **kept formally separate**, not unified — with an explicit rule for which
    governs which future UI element (structure/chrome/identity → Deepwood & Verdigris; live
    resource bars/attribute-family stat coding → Vitals & Attributes). Reskinning was judged a
    large cosmetic rework of already-shipped, user-approved UI for a coherence gain the current
    neutral-frame/vivid-data split already delivers — flagged as a legitimate future choice if
    ever wanted, just not executed here.
  - **Section 1.2**: the "one bespoke palette per mini-boss" pattern (`HANDOFF.md` had flagged
    this as "keeps deferring") is **ratified as the deliberate rule going forward**, not
    replaced — every boss-tier mob gets its own new, cross-checked palette even when reusing
    another boss's model (as Stone of Zombies already does); a boss's own item drops decide
    their palette per-item, not by automatic inheritance. **New forward guidance for the actual
    gap**: future *common/trash* mobs (none exist yet) should default to reusing palettes/
    vanilla textures instead, so the pattern doesn't spiral once regular mob variety grows.
- Build passes; client boots cleanly. **Icon rendering itself needs the same manual in-game
  confirmation every other UI change in this project has needed** (no GUI-automation tool
  exists) — see "Next recommended step".

### Custom UI v1 — Class tab merged into Character Stats Screen, top-left HUD removed

- **Started from a request for a "Metin2 look"**, rejected as conflicting with
  `MASTERPROMPT.md`'s "no MMORPG UI imitation" rule; resolved with the user in favor of an
  original look using only generic, non-distinctive genre conventions —
  `docs/visual-style-guide.md` section 0 records why.
- `docs/visual-style-guide.md` — "Deepwood & Verdigris" art direction (flat, square-cornered
  panels, slate/verdigris/rune-cyan palette), per-class accent colors/icon motifs. **Still has
  an unresolved "which palette" question, now bigger, not smaller — see the note at the end of
  this section.**
- **The standalone `ClassScreen` ('K' keybind) and the top-left `PlayerStatusHud` are both
  gone.** `CharacterStatsScreen` ('C' key) now has a second tab, **"Class"**, built the same
  way as its existing "Stats" tab (a `GridScreenTab` + `TabNavigationWidget`, per-class-card
  `ClickableWidget`s in a single-column `GridWidget`) — same click-to-select behavior the old
  `ClassScreen` had (icon/name/description/bonus per card, "Aktiv" tag + colored border/wash
  on the selected card), just relocated. The top-left HUD panel was removed outright rather
  than kept as a level-only indicator, since vanilla's own XP bar already shows level/XP and
  the panel's only other job (showing class) moved into the new tab.
  - `networking/ClassSelectPayload.java` (C2S, unchanged) still backs the tab's
    click-to-select.
- **Important: this was a unilateral resolution of an open question, not a joint decision —
  flag to Fischey.** A prior session's HANDOFF explicitly logged "the `ClassScreen`/
  `CharacterStatsScreen` structural question... needs a product decision from the
  contributors, not more code." This session went ahead and merged them anyway (see rationale
  above) without that being a joint call. Worth a heads-up conversation, even though the
  outcome is probably what would have been decided anyway.
- Dead code removed earlier this session (during the first master-merge): a duplicate,
  never-registered `Baum2Client` (in a `client` subpackage), and the old unwired
  `ui/ProgressionHud.java` prototype.
- **Both visual-style-guide palette questions below are now resolved — see "Visual/Art Pass"
  further down for the decisions.** (Kept this note for the historical framing of how the
  question grew.) What started as a two-palette question (Deepwood & Verdigris menu chrome vs.
  Vitals/Combat-HUD coral-ember/azure) grew into "every new mob/item gets its own bespoke
  palette by design" — Spider Queen's "Royal Carapace", Stone of Zombies'/Poison Dagger's
  "Toxic Bloom", Zombie Colossus' "Ashen Brute" were each declared "distinct from every other
  palette in the mod." Both were resolved as firm decisions (kept separate; bosses keep getting
  new palettes, future common mobs should reuse) rather than a unification or a cap.

### Stone of Spiders — first custom mob, first custom item, first custom entity model/renderer

`baum2:stone_of_spiders` (level 10, 200 HP, 3x3-block immobile mini-boss) and `baum2:gold_sword`
(its guaranteed drop) — this project's first custom `EntityType`, first custom `Item`, and
first custom `EntityModel`/`EntityRenderer`. New `registry/` package (`ModEntities`, `ModItems`)
and `entity/` package (split main/client the same way `networking/` already is).

- **Mechanics**: extends `HostileEntity`. Immobile via an empty `travel(Vec3d)` override plus
  `MOVEMENT_SPEED=0`/`KNOCKBACK_RESISTANCE=1.0` and `isPushable()→false`; never despawns. No
  attack goals registered at all — **the stone itself cannot attack the player directly**,
  confirmed by `balance-reviewer`; all danger comes from spider waves it spawns. Reads as an
  intentional "objective/totem" boss pattern, not an oversight.
  - **Spider waves**: after each successful hit, spawns one wave of 3 vanilla
    `EntityType.SPIDER` per full 10%-of-max-HP increment lost, cumulative and one-shot per
    threshold. Worst case (full depletion, no healing) is exactly 30 spiders total — bounded,
    confirmed by `balance-reviewer`.
  - **Death cascade**: spawned spiders' UUIDs tracked in-memory (not NBT-persisted — acceptable
    for a single-sitting boss fight); `onDeath` force-kills every still-alive tracked spider.
  - **Drop**: overrides `dropLoot(...)` directly (no loot-table JSON) to drop exactly one
    `baum2:gold_sword`.
  - **Level display**: new `entity.MonsterLevelProvider` interface — first real per-mob level
    in the codebase; every mob not implementing it still shows "Lvl. 1".
  - **Not yet done**: no natural spawn path — `/summon` only, for now.
- **Gold Sword**: plain `Item` (not a `SwordItem` subclass — **`SwordItem`/`ToolItem` no longer
  exist in 1.21.11**, vanilla moved to `Item.Settings.sword(ToolMaterial, attackDamage,
  attackSpeed)`), built on `ToolMaterial.GOLD` with `.sword(ToolMaterial.GOLD, 5.0F, -2.2F)`.
  **`balance-reviewer` finding, flagged not fixed**: because `ToolMaterial.GOLD`'s own
  `attackDamageBonus` is 0.0 (vs Iron's 2.0), this sword's *effective* total damage nets out
  identical to vanilla Iron despite the higher-looking argument — the real effect is a flat
  +12.5% attack speed over every vanilla sword tier, which is the first contribution to the
  now-escalating Combat System v1 DPS-ceiling issue (see "Combat System v1" above).
- **`ip-naming-compliance-checker`: clear** — generic names, "stationary egg-sac boss spawning
  adds" is a widely-used genre archetype, not a specific-game match.
- Visual identity: `docs/visual-style-guide.md` Sections 13-14 — original "Fused Stone/Cocoon
  Husk/Spun Silk/Larval Glow" palette family, placeholder textures (flat programmatic fills).
- Verified: build passes clean. **Not yet verified in an actual game session** — see "Next
  recommended step" for the exact playtest checklist.

### Stone of Zombies — second mini-boss, shares Stone of Spiders' shape/model

`baum2:stone_of_zombies` (level 20, 400 HP, same silhouette, reskinned green with continuous
toxic smoke) and `baum2:poison_dagger` (guaranteed drop, applies Poison on hit). Structurally a
near-exact sibling of Stone of Spiders — reviewed here mainly for what's different.

- **Shared geometry refactor**: `StoneOfSpidersEntityModel` renamed to
  `HulkingCocoonStoneEntityModel` since both stone bosses share the exact same 7-cuboid
  geometry — only the texture differs. **Superseded 2026-07-09**: that class is deleted; the
  shared model is now the GeckoLib "Fallen Comet Stone" template — see the rework section
  below.
- **Zombie waves**: same trigger math as Stone of Spiders, but each wave spawns 2 normal
  zombies + 1 baby zombie. Worst case still 30 total adds — flagged by `balance-reviewer` as a
  judgment call (HP/level ratio scaled consistently with Stone of Spiders, add-pressure per
  wave did not).
- **Ambient smoke**: cosmetic-only client-side particle loop, no networking needed (`HEALTH`
  and similar are already synced `TrackedData`, and particles are a pure client-tick effect).
- **Poison Dagger**: `ToolMaterial.IRON`, `.sword(ToolMaterial.IRON, 1.0F, 0.0F)` — low raw
  damage (4.0 total) but very fast (4.0 total attack speed, 2.5x vanilla's uniform 1.6). New
  `combat/PoisonDaggerHandler.java` (first use of the `combat/` package) applies Poison via
  `ServerLivingEntityEvents.AFTER_DAMAGE` — see the Skill-System-interaction note logged above.
- **`balance-reviewer` finding, escalating the same issue Gold Sword started**: because
  Dexterity's Attack-Speed modifier multiplies whatever speed the weapon already has, at max
  investment (104 Str/104 Dex) Poison Dagger reaches ~2.45x a vanilla iron sword's DPS and
  ~2.18x Gold Sword's DPS *at the same investment level* — pushing the Combat System v1
  ceiling to roughly **~110x baseline**. Two weapons in a row escalating the same ceiling, not
  two separate local issues — see "Next recommended step".
- **`ip-naming-compliance-checker`: clear** — generic descriptive compounds, reskin-the-
  same-boss-per-element is generic genre convention.
- Visual identity: `docs/visual-style-guide.md` Sections 15-16 — new "Toxic Bloom" palette,
  explicitly distinct from Stone of Spiders'.
- Verified: build passes clean. **Not yet verified in an actual game session.**

### Fallen Comet Stone rework (2026-07-09) — both stone bosses remodeled as a crashed comet, on GeckoLib, via one shared template

User request: remodel Stone of Spiders/Stone of Zombies to look like "a comet which has
fallen down," as a reusable template for future stones, using GeckoLib. (The user's visual
reference was a Metin2 screenshot — the asset itself was NOT copied per the IP rules; only
the generic "tilted crashed rock with a glowing impact aura" concept was kept, with a fully
original design.) **Mechanics untouched** — both entities only gained the standard GeoEntity
boilerplate (instance cache + one idle controller); waves/drops/immobility code is unchanged.

- **New shape**: a ~3-block jagged monolith tilted ~23° with its lower end buried (sub-ground
  cubes deliberately clip into the terrain — "embedded by impact"), crater rubble ring + two
  upturned rim slabs, glowing energy veins with heat pooling near the buried end, and three
  glow-rimmed shards that orbit/bob/self-spin in a 12s idle loop (a crashed rock can't walk;
  the shards are what make it read alive). Full design + palette-role notes in
  `docs/visual-style-guide.md` 13.5/15.4; both stones keep their ratified palettes (13.3
  Fused Stone/Larval Glow, 15.2 Blight Stone/Plague Glow).
- **Template contract** (adding a third stone boss later): ONE shared geometry+animation pair
  (`assets/baum2/geckolib/{models,animations}/entity/fallen_comet_stone.*`) for all stones,
  per-stone texture only. New stone = new 6-role palette in `tools/gen_fallen_comet_stone.py`
  (regenerates all textures with a pixel-identical atlas layout, asserted in-script) + a
  `FallenCometStoneEntityRenderer<>(context, "<entity_name>")` registration + the GeoEntity
  boilerplate referencing `FallenCometStoneAnimations.IDLE`. No new
  geometry/animation/model/renderer classes.
- **Java**: new shared client classes `FallenCometStoneGeoModel` (uses `DefaultedGeoModel`'s
  real `withAltModel`/`withAltAnimations` — confirmed against GeckoLib 5.4.5 sources — so the
  model/animation paths point at the shared files while the texture stays per-entity by
  convention), `FallenCometStoneEntityRenderer` (generic over `HostileEntity & GeoEntity`; no
  `withScale`, geometry is authored at full size), `FallenCometStoneRenderState` (empty on
  purpose, per the documented part-G crash), and main-side `FallenCometStoneAnimations` (the
  shared idle `RawAnimation` constant, so a typo can't desync one stone). Deleted:
  `HulkingCocoonStoneEntityModel`, `StoneOfSpidersEntityRenderer`,
  `StoneOfZombiesEntityRenderer`; `Baum2Client`'s two `EntityModelLayerRegistry` calls are
  gone (GeckoLib needs none). No head bone in the geometry on purpose — a rock must not track
  the player.
- **Verified**: `./gradlew build` passes; model/animation/both textures visually verified via
  `tools/render_geckolib_preview.py` (shape, tilt, vein glow, shard orbit at t=0/3/6/9s, both
  palettes). **Not yet verified in a live client** — same standing caveat as every renderer
  change in this project (a GeckoLib render-state mistake only crashes at render time, not at
  build time), so `/summon` both stones before trusting it; see "Next recommended step" 2.

### Stone ladder (2026-07-09, same day, follow-up) — one stone per vanilla monster, 33 total, fully config-driven

Immediate follow-up user request: "for every monstery entity add a stone... make the level/
name meaning which makes sense; ignore boss monsters; probably every 5 levels; you may
rebalance existing stones." Implemented as a **config-table system**, not 31 new classes:

- **`entity/FallenCometStoneEntity.java` (new)** — THE stone class, replacing the deleted
  `StoneOfSpidersEntity`/`StoneOfZombiesEntity`: same wave math (one wave per full 10% max-HP
  lost, cumulative, max 10), death-cascade, no-despawn/immobile behavior, GeoEntity idle —
  everything per-stone (level, health = 20×level, wave composition, drops, optional ambient
  particle) injected via **`entity/FallenCometStoneDefinition.java` (new record)**.
- **`registry/FallenCometStones.java` (new)** — the whole table: 33 stones, levels 5-95 in
  5-level steps (two per tier from 20 up), one per **normal vanilla hostile monster**
  (authoritative roster read from the decompiled 1.21.11 `EntityType` registry — includes the
  post-training-data mobs `parched`, `camel_husk`, `zombie_nautilus`). **Excluded, with
  reasons in the class javadoc**: bosses (Ender Dragon/Wither/Warden/Elder Guardian per the
  brief), Guardian (flops helplessly on land; Zombie Nautilus covers the niche with real
  on-land behavior), Creaking (invulnerability is heart-block-bound), Giant/Illusioner/Zombie
  Horse (unused leftovers), Zombie Villager (gameplay duplicate of Zombie). Drops are themed
  vanilla loot ("paid in the monster's own currency" — blaze rods, shulker shells, a totem
  from Evokers...); the two original stones keep their custom weapon drops and exact waves.
- **Registration is 3 loops** (`ModEntities.FALLEN_COMET_STONES` map, `registerAttributes`,
  `Baum2Client`) — adding stone #34 = one table row + one palette in
  `tools/gen_fallen_comet_stone.py` + one lang entry.
- **Two deliberate mechanical rebalances (new behavior, affects the original stones too)**:
  (1) stones are now **immune to explosions** (else Stone of Creepers' own waves / Stone of
  Ghasts' fireballs damage the stone → self-triggering wave chain with no player involved)
  and **fire-immune** via `makeFireImmune()` (Blaze/Magma Cube waves must not burn their own
  stone); (2) **wave mobs immediately target whoever damaged the stone**, including
  neutral-until-provoked mobs (Endermen, Zombified Piglins) via the real `Angerable` anger
  mechanic (`setAngerDuration` + `setAngryAt(LazyEntityReference.of(...))` — the 1.21.11
  anger API, verified from decompiled source), and Piglins/Hoglins/Brutes spawn
  zombification-immune so nether-mob waves don't dissolve mid-fight in the overworld.
- 31 new palettes (6 roles each) pinned in `docs/visual-style-guide.md` **13.6/13.7** — one
  visual family, per-monster color accent; known glow-hue crowding documented there.
- **`ip-naming-compliance-checker`: all 31 names clear** — verified zero lexical overlap with
  Metin2's actual stone names (abstract nouns: "Metin of Sorrow/Greed/Soul/..."), and the
  names are literally the vanilla mobs each stone spawns. It logged one system-level
  observation for the humans (not a rename issue): the stationary-leveled-stone-spawns-waves
  *mechanic* is structurally closer to Metin2's core loop than the generic-mechanics examples
  CLAUDE.md lists as safe — the pattern was already established/accepted with the original two
  stones, so this batch adds variety, not new resemblance; flagged for awareness, decision
  stays with the contributors.
- **`balance-reviewer` ran over the full table; findings triaged as follows.** FIXED: (a) a
  real cross-system XP gap — `MobDeathHandler`'s old `instanceof HostileEntity` check paid
  zero XP for Slimes/Magma Cubes/Ghasts/Phantoms/Shulkers/Hoglins/Camel Husks/Zombie
  Nautiluses (8 of 33 stones' waves were XP-dead); eligibility is now `instanceof Monster OR
  spawn group == MONSTER`, which also means those mobs now grant XP *everywhere*, not just at
  stones; (b) add-count inversion — level-5 Silverfish stone was the single swarmiest stone
  in the game (50 worst-case adds), trimmed 5→4/wave (Endermites 4→3); (c) Slime/Magma Cube
  waves now spawn at fixed size 2 — consistent pressure, one bounded split generation (split
  children still escape the death-cascade; known, documented, size-capped leak). RULED
  INTENDED, docs updated: the killing blow's own wave thresholds spawn nothing (rewards
  overpowering a stone; a post-death wave would orphan adds past the cascade — the class
  javadoc now explains this instead of claiming "worst case exactly 10 waves"); stones have
  no offense of their own at any level (the original "objective/totem" design, re-confirmed).
  LOGGED, NOT FIXED (decisions needed before stones ever spawn naturally): guaranteed
  rare-item drops (Evoker totem, Piglin Brute netherite scrap, Shulker shells) are
  deterministic no-cooldown farms once spawning isn't command-gated — decide chance-based vs.
  cooldown then; Camel Husk may barely fight (mount hierarchy) and Zombie Nautilus is
  brain-driven so `setTarget` aggro may not stick — both are explicit playtest items;
  "burn the stone, ignore the adds" is the XP-optimal strategy since cascade kills grant
  nothing (correct anti-exploit, but it means waves gate difficulty, not reward).
- Build passes (re-verified after the balance fixes); `stone_of_blazes` texture
  preview-verified. **Same live-client caveat as the section above, now ×33** — spot-check
  several stones across tiers, not just the original two.

### Heimgrund (2026-07-09) — the starting dimension: finite authored world, protected village hub, zone spawns, stone slots

User brief: the game starts in another dimension — village hub at the center, wild low-tier
monsters outside it (weakest nearest the village), stones spawning in matching monster zones
(a stone only respawns when one is destroyed), grass/lakes/desert landscape (zombies,
silverfish) plus mountains with caves (spiders), finite world ringed by impassable mountains,
no block breaking/placing anywhere. User-confirmed decisions: Heimgrund IS the entire game
(vanilla dims unreachable, no portals), ~500-block radius, whole-dimension protection, village
to be hand-built in creative and shipped as structure templates. All names cleared by
`ip-naming-compliance-checker` before use: **Heimgrund** (dimension), biomes **Dorfanger**
(village clearing), **Lichtwiese** (meadow), **Dörrsand** (desert), **Felskranz** (mountain
ring).

- **New package `world/`** (9 classes) + the mod's **first `data/` datapack content**
  (`data/baum2/dimension/heimgrund.json`, `dimension_type/heimgrund.json`,
  `worldgen/biome/*.json` ×4, `structure/village_placeholder.nbt`).
- **Terrain**: `HeimgrundChunkGenerator` (codec-registered via `ModWorldgen.bootstrap()`,
  which must run before any world deserializes — it's near the top of `Baum2.onInitialize()`)
  derives every block from `ZoneLayout` — pure static math, **fixed seed, world seed ignored**
  (every Heimgrund is the same authored map). Radial bands: flat clearing r<60 at y64; meadow
  60-380 (noise hills, lake basins at sea level 62, desert patches only past r=180 so the
  softest ring hugs the village); mountain ring 380-500 with a climbable inner ramp to r=460
  and then a **sheer cliff band (~2.5 blocks gained per block radially — unjumpable) to the
  crest (~170)**; r>=500 is solid crest forever, so chunks past the border are just more wall.
  min_y 0, height 256, bedrock floor. **Mountain caves are authored worm tunnels** baked in
  `ZoneLayout` (8 tunnels, mouths cut into the inner face, hard-capped at r<=488 so they can
  never breach the wall) — `carve()` stays a permanent no-op; vanilla carvers are not used.
- **Biomes**: 4 datapack biomes placed by `HeimgrundBiomeSource`, which calls the same
  `ZoneLayout` zone function as the generator, so biome and terrain cannot disagree. Spawner
  lists carry the tier map (clearing: none — safe zone; meadow: silverfish; desert: zombie 80 /
  silverfish 20; mountain: spider 80 / cave_spider 20). Daylight spawning via the custom
  dimension_type (`monster_spawn_light_level: 15`) with `minecraft:gameplay/monsters_burn:
  false` so daytime zombies don't self-ignite. **Real bug found by live server verify, fixed**:
  vanilla builds ONE global feature order across all biomes in a source — two biomes listing
  the same placed features in different relative order crash chunk gen with "Feature order
  cycle found". Keep shared features in the same relative order in every biome JSON.
- **1.21.11 API notes discovered here** (several old APIs are gone in this version):
  permission checks are now `CommandManager.requirePermissionLevel(CommandManager
  .GAMEMASTERS_CHECK)` (no more `hasPermissionLevel(2)`); gamerules moved to
  `net.minecraft.world.rule.GameRules` with typed constants
  (`DO_MOB_GRIEFING`/`TNT_EXPLODES`/`FIRE_SPREAD_RADIUS_AROUND_PLAYER`/`SPAWN_PHANTOMS` —
  all four set defensively on world load, since Fabric has NO explosion/piston/fire events);
  WorldBorder is per-world persistent state (border diameter 1000 centered 0,0 set idempotently
  on LOAD); dimension_type JSON uses the new environment-`attributes` map (copied from the
  vanilla jar's own `data/minecraft/dimension_type/overworld.json` — online examples are wrong
  for 1.21.11); the vanilla jar ships all its worldgen JSONs, so extract references from there.
- **Start/respawn**: `PlayerStartHandler` — persistent per-player attachment flag + 
  `ServerPlayerEvents.JOIN` (first join → teleport to 0/65/0) and `AFTER_RESPAWN` (re-route any
  respawn that resolved outside Heimgrund). Known cosmetic: brief overworld flicker on the very
  first join. Datapack dimensions bake at world creation — **old saves don't have Heimgrund**
  (`/baum2 world tp` reports this instead of crashing); always test in a fresh world.
- **Protection** (`WorldProtectionHandler`): `PlayerBlockBreakEvents.BEFORE` +
  `BlockEvents.USE_ITEM_ON` (new in this Fabric version, placement-specific) +
  `UseItemCallback` (buckets place fluids via `use()`, not `useOnBlock()`). Creative bypasses
  everything — that's how the village gets built. **Logged design conflict, needs a decision**:
  Rissobelisk's own place-attack-destroy mechanic is impossible for survival players inside
  Heimgrund (placement is blocked dimension-wide).
- **Village pipeline** (`VillageStamper` + `/baum2 structure save <name> <from> <to>`):
  op command captures a world region to template .nbt (auto-tiles into 48-block-grid files
  past the template limit; prints each file + its placement offset), files land in the world's
  `generated/baum2/structures/` (plural) and ship from `data/baum2/structure/` (singular — the
  1.21 datapack path). Stamping runs on the world's first tick (not LOAD — chunk system isn't
  fully live there), guarded by a persistent world-attachment flag; missing templates log and
  skip, never crash. **Round-trip verified live** (built via console → saved → bundled → fresh
  world auto-stamped it). **The shipped village is no longer a placeholder** —
  `data/baum2/structure/village_heimgrund.nbt` is the full 47x47 "Dorfanger Hub" (arrival
  plaza + Heimstein monument standing-stones AROUND the spawn cell so the player never spawns
  inside a block, copper-crowned Gathering Hall opposite the main south gate, workshop with
  open lean-to bay, 2 Fachwerk cottages, herb-garden court, low wall/hedge perimeter with 2
  soul-lantern gates), generated by **`tools/gen_village.py`** (voxel-dict builder + isometric
  PNG previewer + its own structure-NBT writer; regenerate with `--nbt`, previews land in
  `tools/preview_village_*.png`, not committed). Design spec: `docs/visual-style-guide.md`
  section 21 (graphics-designer pass — "Deepwood & Verdigris" translated to vanilla blocks,
  real-world Fachwerk/Rundling anchors, 3 signature motifs incl. "copper roof = importance"
  and "soul lantern = threshold"). Gotchas baked into the script: hedge leaves carry
  `persistent=true` (they'd decay otherwise); fence AND wall connection blockstates are
  computed by the script (walls use none/low + up, not booleans), not left to neighbor
  updates. Hand-refinement stays possible: edit in creative, re-capture with `/baum2
  structure save`, replace the .nbt.
- **Stone slots** (`StoneSlotManager`): the user rule "stones only spawn again when a stone is
  destroyed" = fixed population. **Respawn semantics (user-decided, 2nd playtest): 3 seconds
  (60 ticks) after a stone dies, the SAME type respawns at a NEARBY randomized position
  (12-40 blocks), never the same spot.** Positions sample around each slot's fixed `anchor`
  (not the last position — so slots wander per-respawn but never drift out of their zone);
  candidates must match the anchor's zone, respect r>=100/mountain-ramp caps, and land where
  entities tick, else the driver retries every second. `anchor` is an `optionalFieldOf` codec
  field (defaults to `pos`) per the Attachment new-field gotcha, so older saves still decode.
  Live-verified: kill at anchor (324,69,37) → replacement alive at (302,69,57) seconds later.
  This resolves the respawn half of balance decision (a); with a 3s timer the guaranteed-drop
  farm is now gated purely by kill speed — the drop-table half (guaranteed vs. chance-based)
  is still the open lever. Driver: `END_WORLD_TICK` pass (every 20 ticks) gated on
  `world.shouldTickEntityAt(pos)` — no spawn/reconcile action in chunks whose entities aren't
  loaded, which also means a stone only (re)appears when a player is near. Slot table = 30
  slots, **meadow/desert only — NO stones on mountains (user rule, 2nd playtest)**: 18
  silverfish L5 (5 of them a deliberate pentagon around the stone hot spot at (30,240)) + 12
  zombies L20 in the desert patches; spider/cave-spider stones removed (spiders remain
  stone-less natural mountain monsters). Scattered once per world by fixed-seed rejection
  sampling (min 40 apart, min r=100), then **frozen in a persistent world attachment**
  (first use of a World-target attachment in this project — works, verified across restart).
  Death detection via `ServerLivingEntityEvents.AFTER_DEATH`; entities that vanish without a
  death event re-pend when their chunk next entity-ticks. Debug: `/baum2 stones list` (op).
- **Verified headlessly** (dedicated-server console driving, no GUI): dimension loads; biome
  per ring correct (`execute if biome`); grass y64 clearing / solid wall to y140+ at r480 /
  crest stone at r520 / bedrock floor; border reports 1000 wide; village round trip; slot
  table generates with correct zone placement; kill → 5-min pending → **countdown survives a
  full server restart**; deep-rock probes found tunnel air only where tunnels should be.
  **Not yet play-verified in a real client** — see "Next recommended step": zone spawn
  density/daylight behavior, protection feel, first-join/respawn routing, cave mouths, and
  stone respawn-on-approach all need a human session.

### Spider Queen — first mobile boss, first armor set (`baum2:spider_queen`)

Level 15, 350 HP giant (3x-scale) spider boss with a fast melee bite (10 dmg, 2 attacks/sec)
and a signature long-range leap attack (75 dmg, 4-12 block trigger range, 7s cooldown). First
mobile boss (built on vanilla `SpiderEntity` directly, not `HostileEntity` — inherits
wall-climbing/navigation/model for free). Drops a full 4-piece "Queen Spider Set" armor — this
mod's first armor system.

- **3x visual + hitbox scale**: same two-part mechanism vanilla's own Giant uses —
  `EntityType.Builder.dimensions()` plus `ModelTransformer.scaling(3.0F)` on the model, not the
  newer `EntityAttributes.SCALE` attribute (which Giant doesn't use either).
- **Leap attack**: `balance-reviewer` found a real *mechanical* gap (not numeric) and it was
  fixed in the same pass: the leap originally only aimed once at launch, so a player could beat
  it with a single sidestep, directly contradicting the "escape is impossible" design brief.
  Fixed by re-aiming mid-flight toward the target's current position.
- **Queen Spider Set**: `Item.Settings.armor(ArmorMaterial, EquipmentType)`. Confirmed the worn
  3D look is a *separate system* from the item icon — a client resource JSON at
  `assets/baum2/equipment/queen_spider.json` (plain resource-pack asset, not a datapack/
  dynamic-registry entry despite the type signature suggesting otherwise). No custom rendering
  code needed for the worn look — vanilla's `PlayerEntityRenderer` picks it up automatically.
- **`balance-reviewer` findings, logged not fixed**: (a) HP/level ratio (~23.3) drifts ~17%
  from both Stone bosses' consistent 20 HP/level, in the "harder to farm" direction; (b) armor
  toughness (1.0) makes zero actual difference against the boss's own 75-damage leap (the
  `armor × 0.2` floor binds regardless of toughness at that damage level); (c) defense-total
  and enchantability are Diamond-*equal*, not Diamond-*adjacent*, despite only
  durability/toughness sitting between Iron and Diamond; (d) repairs via cheapest-ore-tier
  tags is a real mismatch against Diamond-equal defense, no dedicated repair material exists
  yet; (e) 75 flat leap damage is fair at starting HP but fades fast as Endurance is invested
  (same "high investment trivializes content" direction as Combat System v1).
- **`ip-naming-compliance-checker`: clear, with a future-content watch-item** — "Spider Queen"
  is a genre-wide trope (multiple unrelated games ship their own), and also the D&D/Forgotten
  Realms epithet for Lolth; nothing here pulls in Lolth-specific content, so it clears on its
  own, but a future drow/web-pit/spider-goddess *bundle* would start reading as Lolth
  specifically — worth remembering if this boss's lore gets expanded.

**Playtest fixes (real user testing surfaced 3 real bugs — clean builds ≠ working features):**
1. "The jump did not work" — the leap goal now explicitly stops navigation at wind-up start
   *and every wind-up tick* (killing residual pathing from whichever lower-priority goal was
   active), not just once.
2. "I want a real jump animation" — implemented a genuine two-phase telegraphed wind-up
   (15 ticks, synced via a new `TrackedData<Integer>`) with a dedicated render pipeline
   (`SpiderQueenRenderState`/`SpiderQueenEntityModel`). Trade-off: this broke compatibility
   with vanilla's `SpiderEyesFeatureRenderer` (dropped; the replacement texture paints eye-glow
   directly instead).
3. "The Spider Armor set requires images for the inventar" — all 4 item model JSONs referenced
   a non-existent `"minecraft:item/generic"` parent (the real vanilla constant is `GENERATED`
   → `"minecraft:item/generated"` — confirmed against decompiled source, there is no
   `GENERIC`). Fixed.

Then, per further feedback ("make it more green... like mutant spider... greenly smoke"): the
entity texture was reworked to a new "Mutant Ichor" palette (deliberately distinct from Stone
of Zombies' existing "Toxic Bloom") with a continuous `ParticleTypes.WITCH` toxic aura. **The
armor's own "Royal Carapace" palette was deliberately left unchanged** per explicit user
direction — the boss and her drop intentionally use two different palettes now, flagged
clearly in the style guide so this isn't "fixed" by accident later.

**Leap trajectory rework (second follow-up)**: user gave an exact spec ("2 Y-blocks high and
12 X-blocks wide" normally, "12 Y-blocks high and 5 wide" for an elevated target). Rather than
hand-tune by feel, **the actual launch velocities were derived by simulating this game's real
per-tick physics in a standalone script** (confirmed via decompiled `travelMidAir()`/
`getEffectiveGravity()` that velocity decays `vy=(vy-0.08)*0.98`, `vx=vx*0.91` per tick,
semi-implicit Euler), then binary-searched for the two requested trajectories. See git history
for the exact velocity table if this technique needs to be reused for a future leap/projectile.

Verified throughout: build passes clean. **Still not independently verified in an actual game
session** — every fix above was driven by the user's own real playtest report; the next
playtest should confirm all the specific things listed in each fix above.

### Zombie Colossus — third mini-boss, first mob with an AoE + custom-rate DoT

Level 25, 750 HP, 3x-scaled melee zombie boss wielding a real held weapon (the "Colossal
Warclub" drop). Same mobile-boss family as Spider Queen (extends `ZombieEntity` directly), but
the first boss with three genuinely different attacks: a slow heavy base hit, a leap ending in
a ground AoE, and a 3-hit burst combo.

- **Base attack**: 100 damage, exactly 2-block range, fully custom `Goal` (not a
  `MeleeAttackGoal` subclass — confirmed via `javap` that class has no overridable
  attack-range hook in this version).
- **Leap attack**: reuses Spider Queen's proven direct-position-control `travel()` override
  verbatim — deliberately did **not** re-attempt a velocity-based leap, since this exact
  codebase already burned two rounds finding that approach unreliable.
- **Fire wave** (new mechanic, first AoE-that-isn't-a-single-hit in the mod): an expanding ring
  from the leap's landing point, 5 blocks/sec outward to 10 blocks, 25 damage + 5s burn per
  player hit once.
- **Burn DoT** (`combat/BurnDamageManager.java`, new): vanilla's own fire-tick rate is fixed
  and can't hit the spec's exact "2 damage/sec for 5s" without a Mixin — a minimal
  `Map<UUID, ticks-remaining>` ticked off `ServerTickEvents.END_SERVER_TICK` instead (same
  shape as `VitalsTickHandler`/`PoisonDaggerHandler`).
- **Rage attack**: 3 strikes of 100 damage each, own cooldown, higher goal priority so it
  periodically preempts the base attack.
- **Held club**: a real equipped `ItemStack`, not cosmetic — copied vanilla's own `GiantEntity`
  mechanism (a 6x zombie holding an oversized item) since vanilla's normal zombie renderer
  hardcodes a non-scaled shadow radius, same problem Spider Queen's renderer hit.
- **`balance-reviewer` found two genuine mechanical bugs, both fixed in the same pass**: (1)
  the rage attack originally had **zero telegraph** — 300 damage (60% of a fresh character's
  starting HP) with no warning, the sharpest "didn't see it coming" burst reviewed in this mod;
  fixed with an 8-tick wind-up + growl. (2) A real **dead zone between 2 and 3 blocks** where
  neither the base attack (≤2) nor the leap (originally ≥3) could land; fixed by lowering the
  leap's minimum trigger range to 2.0.
- **Also found and fixed (correctness, not just balance)**: the fire wave's burn DoT ignored
  Fire Resistance/fire immunity entirely (it's an independent tracker from vanilla's own
  fire-tick damage, which does respect those). Fixed by checking immunity before each tick.
- **Findings logged, not changed**: (a) 750/25 = 30 HP/level is a new high point, extending an
  already-drifting trend (Stone bosses 20, Spider Queen ~23.3, this one 30 — three bosses in a
  row drifting the same direction); (b) the Colossal Warclub's flat damage floor one-shots any
  ~20-HP vanilla mob at zero Strength investment — but simulated concretely that this weapon's
  *shape* (low speed) doesn't add to the escalating max-investment DPS ceiling the way Gold
  Sword/Poison Dagger did (16.7x baseline vs. vanilla iron's own 24.7x — actually sits below
  the existing ceiling).
- **`ip-naming-compliance-checker`**: "Zombie Colossus" cleared. **The original drop name,
  "Colossus Club", was flagged and renamed to "Colossal Warclub"** — an exact 1:1 match to an
  existing (minor, non-iconic) EverQuest 2 item, not coincidental overlap. Renamed before any
  other work depended on the old name.
- Visual identity: `docs/visual-style-guide.md` Section 18 — new "Ashen Brute" palette.
- Verified: build passes clean. **Not yet verified in an actual game session.**
- **Playtest fix round (on `fischey_workbranch`, after the user actually fought this boss)**:
  reported the leap/fire-wave combo "is perfect" (no fix needed - first part of this boss's kit
  independently confirmed live); attack/jump animations looked missing and the model "moving
  very static"; the model "has no muscles"; and the attack range was too short. Root causes,
  verified against the real decompiled 1.21.11 client jar: vanilla's `MobEntity.isAttacking()`
  is only ever set by vanilla's own attack-goal machinery, which this boss's fully custom attack
  `Goal`s never call, so swings always used the calmer non-attacking pose - fixed client-side
  (`state.attacking = entity.isAttacking() || state.handSwingProgress > 0.0F`). Neither the leap
  nor the rage combo had a telegraph *pose* at all - added a new `ColossusRenderState` (two
  synced wind-up counters off new `getLeapWindupTicks()`/`getRageWindupTicks()` `TrackedData`
  fields) driving a crouch-and-coil pose and an overhead club-raise pose in
  `ZombieColossusEntityModel`. Replaced the reused, unmodified `BipedEntityModel.getModelData()`
  geometry with bespoke bulkier cuboids (thicker arms/legs/torso) plus a reshaded texture for
  the "no muscles" complaint. Widened the base/rage attack range and the leap's minimum trigger
  range from 2.0 → 4.5 in lockstep (`Entity.distanceTo()` is center-to-center, not edge-to-edge,
  so 2.0 against a 1.8-block-wide giant with a long club looked disconnected). Verified:
  `./gradlew build` passes clean. **Still not independently re-verified in a live client** - see
  "Last change (on `fischey_workbranch`)" below for the exact next-playtest checklist. Not
  re-run: `ip-naming-compliance-checker`/`balance-reviewer` - no new names, and the range
  widening is a proportional fix matching the model's real size, not a fresh balance concern.

### Drevathis, the Cursed Sovereign — fifth boss, current top tier, first boss with no normal attack

Level 40, 1200 HP (continuing the existing 20→23.3→20→30 HP/level drift as the new top boss),
`baum2:drevathis` (`entity/DrevathisEntity.java`). Unlike every prior boss, it has **no normal
melee attack at all** — its entire kit is four skills, each its own `Goal`, plus a passive aura.

- **Naming**: went through four rounds of `ip-naming-compliance-checker` before landing here.
  The user's own first three picks were each rejected: "Demon King" (verbatim match to a real
  Metin2 boss, plus "Demon Sword" echoing Metin2's "Demon Blade" and its drop pattern), "Ahriman"
  (a recurring FFXI monster name and a named Warhammer 40k character), "Malzeroth" (shares WoW's
  distinctive "-zeroth" tail from "Azeroth" and closely mirrors "Malgrazoth", an actual WoW demon
  NPC). **"Drevathis, the Cursed Sovereign"** (drop: **"Drevathis's Cursed Blade"**) cleared,
  including a final ensemble pass over all five names together. The four skill names ("Dash of
  Death", "Chain of Death", "Wave of Darkness", "Thunder of Darkness") cleared on the first pass.
- **Passive darkening aura**: reuses vanilla's own `StatusEffects.DARKNESS` (the real
  Warden/Sculk-Shrieker effect), refreshed every tick on any player within 12 blocks (an
  assumption — "near" wasn't numerically specified). Works regardless of light level/time of
  day with zero custom rendering code, satisfying "even if it is day" for free.
- **Dash of Death**: teleports 3 blocks behind the target, 0.5s wind-up, then 50 magic damage +
  launches the target ~10 blocks up. The vertical launch velocity is an approximation derived
  from this game's real per-tick gravity/drag (same technique documented for Spider Queen's leap
  arc) — flagged as likely needing one empirical tweak after a real playtest. Cooldown 20s. The
  only skill with unlimited range (see kiting note below).
- **Chain of Death**: instant vanilla Slowness V — confirmed via vanilla's own amplifier formula
  (`1 - 0.15×(amplifier+1)`) this is *exactly* -75% speed, not an approximation — for 5s, no
  damage, held with a sustained "gripping a taut chain" pose and a particle link between boss
  and target for the whole duration. Cooldown 10s.
- **Wave of Darkness**: 0.5s cast-time telegraph (dodgeable via lateral movement, no re-aim at
  impact), then an instant 20×8-block rectangular hit for 65 damage via a new shared
  `combat/DarkWaveEffect.java` helper (rotated-rectangle hit test: AABB broad-phase +
  along/across dot-product filter). Cooldown 2s — deliberately the kit's spammy filler; the
  balance-reviewer's own encounter simulation found it fires ~3x as often as Thunder and ~5x as
  often as Dash over a sustained fight.
- **Thunder of Darkness**: over 3s, 6 strikes (one every 0.5s — an assumption; the original
  design brief gave duration and per-strike damage but not frequency) land within 5 blocks of
  the target's *current* position (re-sampled each strike — this is what "follows the player"
  means, unlike Wave which aims once), each hitting a 1-block radius for 70 damage. Cooldown 10s.
  **Balance-reviewer note**: the naive "6×70=420" ceiling is a near-impossible outcome, not the
  realistic damage — radius is sampled uniformly (not area-uniformly), so a stationary target has
  only a ~20%-per-strike hit chance; simulated expected damage is closer to ~84 per full cast.
  Reads as a dodgeable area-denial effect in practice, not a guaranteed burst — left as-is rather
  than "fixed" since it's unclear this wasn't already the intended feel.
- **Drevathis's Cursed Blade** (`registry/ModItems.java`): 0 total Attack Damage / 0.5 attacks
  per second (`ToolMaterial.WOOD, -1.0F, -3.5F` — cancels the innate 1.0 unarmed base and trims
  the 4.0 base attack speed down to exactly 0.5). Its entire purpose is an on-hit proc
  (`combat/DrevathisCursedBladeHandler.java`): 10% of the wielder's live Attack Damage attribute,
  fired as a Wave of Darkness aimed at whatever was actually hit, no cast-time telegraph.
  **Balance-reviewer caught a real bug before this shipped**: the first version aimed along the
  wielder's look vector (not at the hit target) and `DarkWaveEffect` only queried `PlayerEntity`
  targets — meaning the proc could never damage the mob you actually hit and would only ever hit
  *other nearby players* instead. Fixed by aiming at the hit entity and widening
  `DarkWaveEffect`'s target filter to any `LivingEntity` except the caster (matching
  `skills/SpellEffects.java`'s own AoE convention). **Logged, not fixed**: this proc scales off
  the wielder's *live* Attack Damage attribute, so it automatically inherits whatever the
  existing "Combat System v1 DPS ceiling" (see that section above) ends up being — the first proc
  in the mod that multiplies directly off that same escalating number, worth folding into that
  same eventual balance decision rather than treating as a separate issue.
- **Balance-reviewer also caught a real mechanical gap (kiting dead-zone), fixed in the same
  pass**: Chain/Wave/Thunder and the Darkness aura all require the target within 20/20/20/12
  blocks respectively, Dash of Death has no range limit at all, and nothing in `initGoals()` ever
  moved the boss toward its target — a player who simply stays past 20 blocks was fully exempt
  from 3 of 4 skills and the aura, taking only Dash's flat 50 damage every 20s. Fixed by adding a
  new `ApproachGoal` (lowest priority of the five combat goals, so any skill still preempts it)
  that closes distance whenever the target exceeds the shared 20-block engagement range — same
  "mechanical gap, not numeric tuning, gets fixed in the same pass" precedent Spider Queen's own
  leap-escape bug already established.
- **Visuals** (`graphics-designer`, new "Abyssal Sovereign" palette, `docs/visual-style-guide.md`
  Section 19): a bespoke `BipedEntityModel`-based model (not a zombie/spider reskin) — horns,
  trailing cape, scaled 1.8x. Cool slate-gray flesh (deliberately the cool inverse of Zombie
  Colossus's warm "Ashen Brute"), near-black wine-plum robes, deep wine-crimson trim, and a
  signature pale icy-cyan "Grave Frost" glow distinct from every other mob's yellow-green/amber
  glow family in this mod. Placeholder textures only (flat programmatic fills), same as every
  prior boss.
- **Animation**: all four skills are telegraphed via synced `DataTracker` counters (extending
  `ZombieColossusEntityModel`'s two-counter precedent to four) — Dash's reach-upward, Chain's
  throw-into-sustained-hold, Wave's energy-gather, Thunder's sustained overhead channel. The boss
  also visibly wields its own drop, oversized (~1.8x extra scale) and centered as a two-handed
  grip — this needed a bespoke `DrevathisHeldWeaponFeatureRenderer` rather than vanilla's stock
  `HeldItemFeatureRenderer` (which only renders one-handed, per-hand-offset items). This is
  genuinely new territory for this codebase: the actual rendering pipeline in 1.21.11 has no
  `VertexConsumerProvider`/`ItemRenderer.renderItem(...)` at the point of use anymore (both
  replaced by `OrderedRenderCommandQueue` + a pre-baked `ItemRenderState.render(...)`) — verified
  by decompiling this project's own `genSources` output and written up as a new
  `docs/fabric-modding.md` section ("Custom two-handed held-item rendering") before writing the
  actual renderer, since no prior code in this repo had touched this API surface.
- **Not yet done**: the exact reach/scale/rotation numbers on the held-weapon renderer and the
  Dash launch velocity are both flagged as needing an empirical tuning pass after a real
  playtest, same as this project's established pattern for every prior boss's leap/launch
  numbers. No natural spawn path (`/summon baum2:drevathis`-only, same as all four other bosses).
- **First real playtest round found three more issues, all fixed in a follow-up pass** (the user
  actually summoned and fought/inspected the boss): "the demon boss has no weapon and looks like
  a hobbit", "the weapon does not look like it comes from a demon cursed blade", and "the skill
  itself is not visible... also not from the boss". Root causes, in order of how much they
  mattered:
  1. **A real, structural bug — the sword was never actually equipped.** Confirmed by decompile:
     `MobEntity.initialize(...)` does **not** call `initEquipment(...)` in this version at all;
     only specific vanilla subclasses (`ZombieEntity`, `PiglinEntity`,
     `AbstractSkeletonEntity`, etc.) call it themselves from their own overridden
     `initialize()`. `ZombieColossusEntity` got this for free by extending `ZombieEntity`;
     `DrevathisEntity` extends `HostileEntity` directly, which has no such override — so the
     mainhand slot was genuinely empty, not a rendering problem. Fixed by adding an
     `initialize(...)` override that calls `this.initEquipment(...)` explicitly, the same pattern
     the vanilla subclasses use internally.
  2. **Skill effects were real but nearly imperceptible.** None of the four skills played any
     sound at all, and `DarkWaveEffect`'s particle burst was a sparse 3-line outline fired for a
     single tick (an instant, one-frame effect with ~60 total particles across a 20-block
     rectangle). Fixed by adding a distinct sound cue to every skill (teleport/impact sounds for
     Dash, a chain-hit sound for Chain, evoker-cast/wither-shoot sounds for Wave, warden-roar/
     lightning-impact sounds for Thunder) and substantially densifying the particle effects
     (Wave's rectangle is now filled edge-to-edge rather than outlined; Thunder's strikes are now
     a tall vertical column, not a single ground-level puff, so they're visible from a distance).
  3. **The model geometry itself read as "a reskinned player."** v1's body/arm/leg cuboids were
     essentially unmodified vanilla biped proportions plus two small 2x6x2 horns — the same class
     of complaint Zombie Colossus got fixed for ("no muscles") by moving away from vanilla-default
     sizes. Fixed with a v2 pass: broadened chest (8x12x4 → 9x13x5) and arms (4x12x4 → 5x13x5),
     much longer/more dramatically swept horns (2x6x2 → 2x9x2), a substantially bigger cape
     (9x16x1 → 10x18x1), and new small clawed fingertips on both hands — a silhouette that no
     longer reads as human-proportioned even before the texture is considered.
  - `graphics-designer` then regenerated both the entity texture (to match the new UV layout
    from the geometry pass) and the sword's item texture, pushing much harder on "cursed/
    demonic" execution within the same already-cleared "Abyssal Sovereign" palette (no hex
    changes): glowing eye sockets, asymmetric rune-cracks, a jagged/tattered cape hem instead of
    a plain rectangle, a Sovereign-Blood circlet band on the hat layer, and — for the sword — an
    asymmetric serrated blade edge, a curling hook-guard, and both signature glow colors (Grave
    Frost cyan + Sovereign Blood crimson) visible on the blade at once, so it can no longer be
    mistaken for a reskinned iron sword. Both textures remain explicit placeholders (programmatic
    flat/gradient fills), same as every other boss — a human-artist pass is still the
    recommended next step given this boss's top-tier status.
  - Verified: `./gradlew build` passes clean after all three fixes. **Still not independently
    re-verified in a live client** — the fixes were driven directly by the user's own real
    playtest report; the next playtest should specifically confirm the weapon now renders, the
    boss reads as clearly non-human/imposing, and each skill's new sound/particle cue is
    actually noticeable in a real fight.
- **Second playtest round found the held-weapon fix from the round above actually looked worse,
  not just unpolished**: the user reported the model "looks like it has a penis" and asked for
  the sword to actually fit the hand. Root cause, found by re-reading
  `DrevathisHeldWeaponFeatureRenderer` rather than guessing: v1 of that renderer anchored the
  weapon only at `getRootPart()` and translated it to one fixed point directly in front of the
  torso's center at roughly hip height, with **no arm-following rotation at all** — a static rod
  projecting straight out from the middle of the body regardless of any arm pose, which is
  exactly the silhouette that reads badly. (Checked the entity texture pixel-by-pixel first to
  rule out a texture bug — the UV layout and colors documented in `docs/visual-style-guide.md`
  19.2 are correct and match the model; the horn region's olive-brown "Cursed Bone" tone was a
  red herring, not the cause.) Fixed by anchoring at the right arm's own current transform via
  `setArmAngle(...)` — the exact mechanism vanilla's own `HeldItemFeatureRenderer` uses — so the
  weapon now inherits the arm's existing "grip inward" bend and moves with every skill's
  telegraph pose, then offsetting down the arm's own length toward the actual hand instead of the
  torso, adding a -35° tilt so the blade presents at an angle instead of dead-straight-forward,
  and trimming the extra scale 1.8x → 1.6x. Verified: `./gradlew build` passes clean. **Still not
  re-verified in a live client** — exact numeric offsets here are reasoned from the arm's cuboid
  dimensions and vanilla's own reference offsets, not visually tuned (no live-render tooling
  available in this environment), so this is the best-effort mechanism fix; a further numeric
  tweak pass may still be needed after the next real playtest.
- Verified: `./gradlew build` passes clean after every step, including after the two
  balance-reviewer fixes above. **Not yet verified in an actual game session** — same caveat
  every other boss in this project carries (see "No GUI-automation tool" note below).

### GeckoLib migration — Spider Queen rebuilt on GeckoLib (first mob to adopt it)

Triggered by direct user feedback that hand-coded vanilla `EntityModel`/per-tick pose math was
limiting mob quality, plus a specific ask to improve Spider Queen's leap attack. Extensive
research (`fabric-docs-researcher`, persisted to `docs/fabric-modding.md`'s "GeckoLib
integration" section, parts A-F) preceded implementation — see that doc for full detail;
summarized here:

- **What GeckoLib actually changes, and what it doesn't**: confirmed via GeckoLib's own docs and
  source that it is a rendering/animation library only — it does not move entities, so **the
  leap's actual trajectory/physics (`SpiderQueenEntity.travel()`/`performLeapFlightStep()`/
  `LeapAttackGoal`) is 100% unchanged**. What changed is the *pose*: the wind-up crouch and
  airborne pose are now real keyframed animations (`spider_queen.animation.json`) instead of
  hand-coded per-tick angle math in a now-deleted `SpiderQueenEntityModel`.
- **The user also found a Sketchfab model ("Voided Spider" by RedVoid_, CC BY 4.0) and asked to
  use it for Spider Queen.** Direct mesh import was investigated and found **not feasible**:
  neither vanilla's `EntityModel` nor GeckoLib's `GeoModel` can consume an arbitrary triangle
  mesh regardless of source format (confirmed via GeckoLib's own docs plus real engine-source
  verification that this is a cuboid/bone-hierarchy limitation, not a polycount one) — a genuine
  glTF-mesh-plus-skeletal-animation Fabric renderer would be a multi-week from-scratch or
  fork-an-abandoned-library undertaking (`MCglTF`/`CustomglTF` dead since 2023, `Suspicious
  Shapes` has no skeletal animation, `BlazeRod` is player-model-only and undocumented for reuse)
  — out of scope for this pass. Also checked and ruled out: "ModelEngine" (Ticxo's), which turned
  out to be a Bukkit/Spigot/Paper **server plugin**, architecturally incompatible with a Fabric
  client mod regardless of any of this. **Resolution, explicitly decided with the user**: the
  Sketchfab renders were used as a **silhouette/proportion reference only** (its dramatically tall
  leg stance, angular multi-segment legs, frayed abdomen tufts) to inform new original geometry —
  no mesh or pixels were imported/copied. This keeps the work compliant with this project's
  "no copied assets" rule while still capturing the visual improvement the user was after.
  Attribution recorded in a new root `CREDITS.md` (CC BY 4.0 requires it regardless of the
  reference-only framing) — new `jar { from("CREDITS.md") }` line in `build.gradle` bundles it
  into the built mod.
- **New bespoke geometry** (`assets/baum2/geckolib/models/entity/spider_queen.geo.json`,
  generated via a one-off Python script for numeric consistency across 8 mirrored/rotated legs,
  not committed): 19 bones, 30 cuboids — a genuine two-segment (upper+lower) leg per limb
  (computed via real trigonometry: `θ1=55°` upper-leg lift, `θ2` equivalent via a `-130°` local
  child rotation for the lower segment) instead of vanilla's single straight-box legs, producing
  the reference-inspired tall stance where each leg arcs above body height before angling back
  down. New texture (`spider_queen.png`, 120x120, up from the old 64x32 vanilla-UV-matched
  canvas) uses a programmatic noise+edge-shading atlas (one small cell per cuboid, same
  technique as this project's other placeholder textures) — colors unchanged, still "Mutant
  Ichor" (green), deliberately not shifted to the reference's black/red since the reference's
  job was shape only. Full detail: `docs/visual-style-guide.md` sections 17.1.1/17.2.
- **New animations** (`assets/baum2/geckolib/animations/entity/spider_queen.animation.json`):
  `idle` (subtle body bob), `walk` (simple alternating-side leg swing, not a full biological
  tripod gait — a deliberate scope cut, see "Not yet done" below), `leap_windup` (crouch + leg
  tension, duration kept in lockstep with `LEAP_WINDUP_DURATION_TICKS`), `leap_flight` (trailing/
  splayed legs held during the airborne phase). A new synced `LEAP_FLIGHT_ACTIVE` `TrackedData
  <Boolean>` was added (`SpiderQueenEntity`) so the client can tell "telegraphing" apart from
  "actually airborne" — the old code only ever needed the wind-up counter.
- **Dependency**: `software.bernie.geckolib:geckolib-fabric-1.21.11:5.4.5` via Cloudsmith's
  Maven, `include()`d (jar-in-jar) so players don't need it installed separately. Verified
  against the actual published jar (not the wiki, which currently shows a stale Maven group)
  that this satisfies this project's pinned Loader/Fabric API/Java versions with no conflicts.
  One open, unresolved risk flagged in `docs/fabric-modding.md`: Fabric's nested-jar mechanism
  loads an `include`d dependency as a first-class mod on the shared classpath, and neither
  Fabric's nor GeckoLib's docs state what happens if a different mod the player installs bundles
  a different GeckoLib version — narrow, not encountered, not fully resolved.
- **Migration mechanics** (for the next mob that adopts GeckoLib, so this doesn't need
  re-deriving): `SpiderQueenEntity` now `implements GeoEntity` (adds an `AnimatableInstanceCache`
  field via `GeckoLibUtil.createInstanceCache(this)`, `registerControllers(...)` wiring one
  `AnimationController` whose predicate reads the synced wind-up/flight state, same
  `getAnimatableInstanceCache()` accessor). `SpiderQueenEntityModel` (hand-coded vanilla-derived
  class) is deleted, replaced by `SpiderQueenGeoModel extends DefaultedEntityGeoModel<...>` (zero
  abstract overrides needed — resolves `geo.json`/`animation.json`/texture purely from the
  entity's own registry name via convention, and auto-handles head-turning given a bone literally
  named `"head"`). `SpiderQueenEntityRenderer` now `extends GeoEntityRenderer<...>`, scaled via
  `.withScale(3.0F)` (GeckoLib's own equivalent of the old `ModelTransformer.scaling(3.0F)` —
  GeckoLib doesn't use `EntityModelLayerRegistry` at all, so that registration call was removed
  from `Baum2Client.java` entirely, not just changed). `SpiderQueenRenderState` is now an
  **empty** `extends LivingEntityRenderState` — see the real crash below for why it must stay
  empty, not gain a custom `GeoRenderState` implementation.
- **Real crash found on the first live test, root-caused and fixed (not guessed)**: the game
  crashed instantly on rendering Spider Queen —
  `NullPointerException` in `AnimationProcessor.extractControllerStates`,
  `Objects.requireNonNull(renderState.getGeckolibData(DataTickets.ANIMATABLE_MANAGER))` finding
  null despite that exact data being set moments earlier in the very same render call. Root cause
  (confirmed by reading GeckoLib's actual `EntityRenderStateMixin.java` source, not re-deriving
  the call chain by hand again): **GeckoLib already Mixins full `GeoRenderState` support directly
  into vanilla's `EntityRenderState` as concrete methods** — every `EntityRenderState`/
  `LivingEntityRenderState` is automatically duck-typed as a `GeoRenderState`, confirmed by the
  official wiki's own example using plain `EntityRenderState` with **no custom class at all**.
  This project's original `SpiderQueenRenderState` redundantly re-declared `implements
  GeoRenderState` and overrode **only** `getDataMap()`. Java's "a subclass's own override always
  wins, but only for methods it actually overrides" rule then split the data path: `addGeckolibData`
  (write, called by `captureDefaultRenderState`) wasn't overridden, so it resolved to the Mixin's
  inherited concrete method → wrote into the Mixin's own private field; `getGeckolibData` (read,
  called by `extractControllerStates`) is a pure interface default that calls `getDataMap()` →
  resolved to *this subclass's* override → read from a different, permanently-empty map. Two
  maps, write to one, read from the other, guaranteed null. **Fixed** by stripping
  `SpiderQueenRenderState` down to an empty `extends LivingEntityRenderState` — no
  `implements GeoRenderState`, no `getDataMap()` override, letting the Mixin handle everything.
  Full writeup: `docs/fabric-modding.md`'s "GeckoLib integration" section, part G — **the single
  highest-value gotcha for the next mob that adopts GeckoLib in this project**, since it compiles
  and passes `./gradlew build` fine either way and only crashes at actual render time.
- **Not yet done / scope cuts, flagged deliberately rather than silently skipped**: the `walk`
  animation is a simple two-phase alternating-side swing, not a full per-leg biological gait —
  reads as "walking" but is a simplification, worth a closer look after a real playtest. No
  attempt was made to reproduce the Sketchfab reference's black/red glass-shard color scheme (by
  design, see above). The exact rotation-sign conventions used for the mirrored leg trigonometry
  (right-side `+angle`, left-side `-angle`) are reasoned from Bedrock's documented rotate-around-
  pivot convention, not verified against a live render — **this project still has no live-render
  tooling**, so, consistent with every other boss's numeric/geometric tuning in this document,
  treat the stance/animation as reasoned-not-visually-confirmed until a real playtest happens.
  `ip-naming-compliance-checker`/`balance-reviewer` were not re-run — no new names, and no
  balance-relevant numeric changes (the leap's damage/range/cooldown are untouched).
- Verified: `./gradlew build` passes clean after the full migration. **Not yet verified in an
  actual game session** — same standing caveat as every other boss/feature in this document.

### GeckoLib migration follow-up 1 — first live-test crash, root-caused and fixed

The user ran `runClient` and the game crashed instantly rendering Spider Queen (`NullPointerException`
in `AnimationProcessor.extractControllerStates`). Root-caused by reading GeckoLib's actual Mixin
source rather than re-deriving the render call chain by hand: GeckoLib already Mixins full
`GeoRenderState` support into vanilla's `EntityRenderState` as **concrete** methods; this
project's custom `SpiderQueenRenderState` redundantly re-declared `implements GeoRenderState` and
overrode only `getDataMap()`, which (per Java's "subclass override wins, but only for methods
actually overridden" rule) split writes (resolved to the Mixin's inherited concrete
`addGeckolibData`, writing to the Mixin's own field) from reads (a pure interface default calling
`getDataMap()`, resolved to this project's own always-empty override) — two disconnected maps,
guaranteed null. **Fixed** by stripping `SpiderQueenRenderState` to an empty `extends
LivingEntityRenderState` — the Mixin already handles everything, no custom implementation needed
or wanted. Full writeup: `docs/fabric-modding.md`'s GeckoLib section, part G — **the single
highest-value gotcha for the next mob that adopts GeckoLib in this project**, since it compiles
fine either way and only crashes at actual render time. Verified: `./gradlew build` passes;
user confirmed the crash itself is fixed.

### GeckoLib migration follow-up 2 — bespoke geometry reverted to vanilla-accurate, palette reworked to grey/redstone-red

With the crash fixed, the user could actually see Spider Queen for the first time and reported
the bespoke 17.1.1-era geometry "is not looking like a spider... completely out of place," asking
to "use the standard minecraft spider" instead (tripled in size, unchanged), "colored grey and
shiny red eyes," with the abdomen "also shiny red but... kinda pulsing," and "the red similar
like redstone."

- **Geometry**: discarded the invented tall/2-segment-leg proportions entirely and transcribed
  vanilla's own real `net.minecraft.client.render.entity.model.SpiderEntityModel
  .getTexturedModelData()` — read directly from this project's own decompiled Minecraft sources,
  not from memory — into GeckoLib's geo.json format bone-for-bone (8 real single-segment legs,
  real head/body0/body1 cuboid sizes, real per-leg pivot/rotation angles). The coordinate-system
  conversion (vanilla's legacy Y-down-from-pivot vs. GeckoLib/Bedrock's Y-up-from-feet, and the
  resulting sign flip needed on rotations that touch the Y axis — pitch/roll negated, yaw
  unchanged — derived from first-principles rotation-matrix algebra) was **numerically
  self-checked in the generator script itself** before writing the final file: reconstructed each
  leg's endpoint two independent ways and asserted they matched to 1e-6 units, which they did on
  the first attempt. Full derivation: `docs/visual-style-guide.md` section 17.1.2.
- **Animations updated to match**: the old `spider_queen.animation.json` referenced the deleted
  2-segment leg bone names — regenerated against the new real vanilla leg names
  (`right_hind_leg`, `left_middle_front_leg`, etc.), same 4 animations (`idle`/`walk`/
  `leap_windup`/`leap_flight`), same architecture, no change to the leap's actual physics.
- **Palette replaced**: "Mutant Ichor" (green) → new **"Redstone Widow"** palette (grey body,
  shiny redstone-red eyes and abdomen) — full hex table in `docs/visual-style-guide.md` section
  17.3. The "pulsing" ask specifically can't be done with a static texture alone: `SpiderQueenEntity
  .spawnPulsingAbdomenGlow()` (replacing the old green `ParticleTypes.WITCH` aura) spawns
  vanilla's own `DustParticleEffect` — literally redstone dust's own particle, directly
  satisfying "the red similar like redstone" — near the abdomen, with count and color lerped by a
  sine wave over the entity's age for a genuine brightening/dimming cycle, not a flat color.
- Verified: `./gradlew build` passes clean after both the geometry and animation regeneration.

### GeckoLib migration follow-up 3 — two more real bugs from the second live test: invisible eyes, and abdomen was a flat red block instead of a marking

The user actually saw the vanilla-accurate geometry render (screenshot) and reported two things:
"No Eyes" and "the red body part should more like a have pattern instead a full red block."

- **Invisible eyes, root-caused, not guessed**: the eye cubes were placed at `head_front_z +
  0.1`. Since the head's front face sits at the *more negative* end of its Z-range, `+0.1` moved
  the eyes to a Z value that's *behind* that face — fully embedded inside the head's own solid
  opaque geometry, hence completely invisible. **Fixed** to `head_front_z - 0.15` (actually
  protruding past the face). Noted in `docs/visual-style-guide.md` 17.3 as a general lesson for
  this generator script: near a face on the negative-Z side, `+` moves *into* the model, not
  away from it.
- **Abdomen recolored from a flat red block to a pattern**: base color changed to the same grey
  used for the legs, with the red confined to a hand-drawn widow-hourglass marking (two triangles
  meeting point-to-point) painted onto just the top and rear-facing UV cells via a dedicated
  24x24 texture region — on-theme with the "Redstone Widow" name (real black widow spiders have
  exactly this marking on an otherwise dark abdomen, not a solid red underside).
- **A real packing bug was found and fixed while building the marking texture, worth noting for
  future atlas-generator scripts in this project**: the first attempt reserved space for the new
  24x24 marking region by skipping 2 slots in the linear per-cell counter, which only accounted
  for the extra *width* the bigger region needed, not the extra *row of height* — silently
  overlapping and corrupting whatever cell a later cube's color happened to land on directly
  below it in the atlas. Fixed by reserving the marking region in its own dedicated strip below
  the regular grid entirely, decoupled from the linear counter, rather than trying to make one
  counter track two different cell sizes.
- Verified: `./gradlew build` passes clean after this pass too. **Not yet re-verified in an
  actual game session** for this exact round — same standing caveat, though the eye/abdomen
  fixes are both root-caused from a real screenshot rather than guessed blind, same as every
  other fix in this document that came from actual user-provided evidence.

### GeckoLib migration follow-up 4 — the real bug: the whole model was upside down / floating, plus a full palette/pattern rework

Same live-test round as follow-up 3. Beyond the invisible eyes and flat-red abdomen, the user
also reported the actual root problem: "the spider floats and does not walk on legs... the whole
spider would have to be rotated 180 degrees." This turned out to be a genuinely serious bug, not
a small tuning miss — full technical writeup in `docs/fabric-modding.md`'s GeckoLib section, part
H, since it's the single most valuable lesson for the next mob that adopts GeckoLib in this
project. Summary:

- **Root cause**: this project's own geo.json generator script had derived its own rotation-sign
  rule for converting vanilla's legacy Y-down model space into Bedrock's Y-up convention, purely
  from first-principles reasoning, and "validated" it with a self-check that compared the
  script's own reconstruction function against another function it *also* wrote — both based on
  the same incomplete understanding. **That self-check passed with zero error and was still
  wrong**, because reflections don't decompose into simple per-Euler-angle sign flips for
  *compound* (yaw+roll) rotations the way they do for single-axis ones — this needed the same
  kind of "we validated something, but not against reality" lesson `merge-integration-reviewer`
  and other reviewers in this project exist to catch, just for geometry math instead of code.
- **Real fix**: read GeckoLib's actual `BakedModelFactory.constructBone`/`constructCube` loader
  source directly (confirmed: it negates pivot.x and cube-origin.x — the cube one is size-aware,
  `-(origin+size)`, not a plain negation — and negates rotation.x/rotation.y but *not*
  rotation.z, all undocumented in any wiki/tutorial found), then re-derived the correct rotation
  using actual 3x3 rotation matrices (`R_desired = M_Y · R_vanilla · M_Y`, a similarity
  transform) rather than guessing angle signs, extracting equivalent Euler angles numerically
  (`numpy`, installed fresh for this purpose). Verified this time by comparing the **full
  8-corner box as a set** (nearest-corner matching) rather than "does the JSON origin corner
  match vanilla's own origin corner" — the single-corner comparison is a **guaranteed false
  failure** here even for correct geometry, since X and Y each have different net
  corner-correspondence behavior after their respective fixes/reflections, and no longer
  correspond to one single named vanilla corner as a unit. This new self-check passes to
  floating-point rounding precision (~1e-4, from `round(v,3)` calls, not a residual error).
- **In the same round, the user also sent a real reference photo** (a garden spider's mottled
  abdomen marking) and asked for "pattern like this," explicitly said "do not use redstone red,"
  and asked to reuse the eyes' own red for the abdomen pattern too, plus specified an exact
  primary body hex (`#898989`). Palette fully reworked: primary grey pinned to that exact hex,
  the earlier "Redstone Widow" name and its multi-tone red family dropped in favor of one single
  `Widow Red` (`#FF3B1E`) reused for both eyes and the abdomen pattern, and the abdomen marking
  redrawn from a clean two-triangle hourglass into a small cluster of irregular, jittered
  polygon blotches (mirrored left-right) inspired by the reference photo's organic layout — colors
  and actual pixel data are still this project's own procedural output, not copied from the
  photo (same "reference informs an original asset" boundary as the Sketchfab model). Full
  detail: `docs/visual-style-guide.md` section 17.3.
- Verified: `./gradlew build` passes clean after this pass. **Not yet re-verified in an actual
  game session** — but this is the first round in this whole GeckoLib effort where the fix is
  backed by a numerically self-checked match against GeckoLib's *actual confirmed* behavior
  (read from its real source) rather than a self-consistency check against this project's own
  assumptions, so confidence is meaningfully higher than the "reasoned, not verified" caveat
  usually implies.

### GeckoLib migration follow-up 5 — several more visual-polish rounds on Spider Queen's face/eyes/head, and a head-rounding feature ultimately abandoned

Full detail in `docs/visual-style-guide.md` section 17.3 (this is a visual-polish history, not
more geometry-math bugs — worth reading there if picking up further work on this mob's face).
Summary of what actually shipped, in order: eyes went from a flat-texture bug (drawn correctly
but never actually sampled onto the tiny cube face — a real UV-sizing bug, fixed) → merged
together (no minimum-spacing check, fixed with rejection sampling) → too small (sizes bumped up)
→ "not dangerous" (redesigned again: a near-black face-plate on the head's front face for
contrast, plus a deliberate 2-large-"primary"-eyes-plus-scattered-secondary-cluster structure,
replacing pure randomness). **Head corner-rounding was attempted twice and abandoned both
times** — the first attempt used GeckoLib's per-cube rotation/pivot (an untested code path) and
produced oversized disconnected slabs; the second properly reused the already-verified
bone-level rotation mechanism instead (each bevel became its own small bone, numerically
confirmed centered on its own pivot before shipping) and *still* looked wrong in-game
("malformed"). That second result is the important data point: **the geometry math was
verified correct, and it still looked bad** — meaning this specific cosmetic goal isn't a math
problem to keep re-solving, and further attempts should start from an actual design idea for
what "rounder" should look like on this silhouette, not another transform-verification pass. The
head is currently a plain, unmodified cube. Also removed entirely this round: the pulsing red
particle "smoke" effect (`spawnPulsingAbdomenGlow` and its supporting code deleted outright, not
disabled) per direct feedback that it "looks like trash."

- Repo: https://github.com/laserjonas/minecraft-baum2 (public).
- **Branches**: `jonas_workbranch` has merged `origin/master` twice this session and is now
  ahead of it by this merge plus the Skill System/Sub-specialization commits and the Class
  Overhaul v2 commit; `master` and `fischey_workbranch` are kept at the same commit by
  convention (push jonas_workbranch, then decide when to fast-forward `master` to match, per
  established practice).
- `.vscode/` is checked in (extensions.json, settings.json, tasks.json) so fresh checkout gets
  Java+Gradle recommendations and "Run Minecraft Client" task (`Ctrl+Shift+B`) out of the box.
- Five subagents under `.claude/agents/` (shared via git): `fabric-docs-researcher`,
  `ip-naming-compliance-checker`, `balance-reviewer`, `merge-integration-reviewer`,
  `graphics-designer` (the exception that writes files — maintains
  `docs/visual-style-guide.md`). The first four report findings only. See `CLAUDE.md` ->
  "Project Agents" for exact trigger conditions; use them proactively.
- **Known limitation (workspace root)**: the harness discovers project agents from its primary
  working directory, not a nested repo root — a session opened at `D:\Baum2` (the parent of
  this repo) instead of `D:\Baum2\Baum2` sees none of the five project agents. **Fix: open the
  session at `D:\Baum2\Baum2`.**
- **Known limitation (environment-specific, reported from at least one other environment)**:
  in at least one setup (the VS Code extension host per a prior session's note), custom
  `.claude/agents/` subagents were never available via the `Agent` tool even in a fresh
  session with the files present on disk — only built-in types resolved. Workaround: read the
  target `.claude/agents/<name>.md` file yourself and dispatch a general-purpose agent
  reproducing its role/instructions verbatim. Confirmed still needed as of Fischey's latest
  session (recurred there); not encountered in this session (custom agent types resolved
  normally here) — environment-dependent, re-test each fresh session rather than assuming
  either way.
- **No GUI-automation tool exists in this environment** for the native Minecraft/LWJGL window
  (unlike a browser/Electron app) — nearly every UI/gameplay bug found across both
  contributors' sessions was only caught because a human played manually and reported back,
  not by build/boot verification. Expect the same for any future work: build passing and clean
  boot are necessary but not sufficient checks. All four new mini-bosses in this update are
  explicitly **not yet verified in an actual game session** for exactly this reason.

### Rissobelisk — first custom `Block`, first World-Event (`baum2:rissobelisk`)

User asked to "plan the next features" (not another fix/art pass). Checked
`MASTERPROMPT.md`'s "Entwicklungsprioritäten" against this file's own state: every Priority 1
item was done except the last one, **"Erster Event-Block."** Section 4 ("Welt-Events") already
specifies the brief in full (rare destructible world object, attacking spawns waves, destroying
grants XP/loot/rare materials, "**first version may be a manually placeable Block**") — this
implements it as a literal `Block`, the mod's first one ever (every prior piece of custom
content was an `Entity` or `Item`).

- **New research needed first**: no `Block`/`BlockEntity` pattern existed anywhere in this
  codebase. Dispatched `fabric-docs-researcher` to confirm, via fresh `gradlew genSources` +
  decompiling the exact bundled Fabric API submodule jars (not guessed from training data):
  `net.fabricmc.fabric.api.event.player.AttackBlockCallback` exists and fires at the `HEAD` of
  the server's block-break handling — before *both* survival mining and the creative-mode
  instant-break branch — so a listener returning non-`PASS` for a specific block cancels all
  vanilla destruction paths in one hook; `AbstractBlock.Settings.strength(-1.0F, 3_600_000.0F)`
  (bedrock's own values) blocks survival mining/explosions but **not** creative instant-break on
  its own (a real, verified vanilla behavior — the `AttackBlockCallback` hook closes that gap);
  a `BlockEntity` is needed for the mutable HP/wave-state (plain fields, not the Attachment API
  — no persistence-safety win for a from-scratch type this project already owns the source of);
  `BlockEntity`'s NBT-equivalent override points are `readData(ReadView)`/`writeData(WriteView)`
  in this version, not the older `readNbt`/`writeNbt` shape. Findings persisted to
  `docs/fabric-modding.md`'s new "Custom `Block`s and `BlockEntity`s" section — reusable for any
  future block work (e.g. the next `MASTERPROMPT.md` world-event example: Chaosmonolith,
  Sternsplitter, etc.).
- **Mechanics** (`block/RissobeliskBlock.java`, `block/RissobeliskBlockEntity.java`): 200 HP
  pool (matches `StoneOfSpidersEntity`'s own "first of its kind" precedent exactly), damaged by
  a player left-clicking it directly — damage-per-hit is the attacker's live
  `EntityAttributes.ATTACK_DAMAGE` value (so Strength/weapon investment matters here too, same
  as real melee). Every full 10%-of-max-health lost spawns a wave of 3 vanilla Silverfish
  nearby, cumulative and one-shot per threshold — a **direct structural port of
  `StoneOfSpidersEntity`'s own wave-spawn/UUID-tracking/death-cascade logic**, ported from
  entity-health to block-health. On reaching 0 HP: force-destroys itself
  (`world.removeBlock`), kills any still-alive spawned Silverfish, grants 110 XP (reuses
  `MobDeathHandler`'s own `10 + maxHealth/2` formula verbatim), and drops 1 `Risssplitter`.
- **Naming**: "Rissobelisk" (block) and "Risssplitter" (the material it drops) are both
  `MASTERPROMPT.md`'s own pre-listed examples (Section 4, Section 7 respectively), picked
  together because they share the "Riss" (crack/rift) root — the obelisk cracks apart into
  splinters when destroyed. `ip-naming-compliance-checker` cleared both explicitly (checked
  against Metin2 by name per `CLAUDE.md`'s callout, and generally) — **but flagged a secondary,
  non-blocking observation worth a human decision**: the underlying mechanic itself (stationary
  object, spawns escalating waves as it's damaged, drops loot on destruction) is conceptually
  close to Metin2's signature "Metin Stone" mechanic. This was already reviewed and cleared once
  for `StoneOfSpidersEntity`/`StoneOfZombiesEntity` as "a widely-used genre archetype, not a
  specific-game match" — Rissobelisk just reapplies that already-cleared pattern a third/fourth
  time, now to a `Block` instead of an `Entity`. Not re-flagged as a blocker, but worth a
  deliberate revisit given how many mini-bosses now share this one mechanic shape.
- **`balance-reviewer` findings, logged, not fixed (design/judgment calls, no bugs found)**:
  (a) time-to-destroy collapses to **2 hits at max Strength/weapon investment** regardless of
  weapon choice (Strength's flat +99 bonus dwarfs any weapon's own flat damage) — reads as the
  trivial end for a "mini-boss," a direct symptom of the already-tracked Combat System v1 DPS
  ceiling (see "Next recommended step" below), not a new problem; (b) because block-damage never
  routes through `PlayerEntity.attack()`, **Dexterity (Attack Speed + Crit Chance) has zero
  effect on Rissobelisk** — every other combat encounter in this mod has all three stats
  compounding, so a Dexterity-invested player gets nothing extra here; (c) at max investment a
  single hit can cross 5-6 of the 10 wave-thresholds at once, spawning up to 18 Silverfish in
  one synchronous call — today's numbers don't allow an actual one-hit 200→0 kill (max single-hit
  ~120-125 damage `<` 200), so this doesn't currently produce a spawn-then-instantly-kill wash,
  but the margin (~75-80 damage) is closer than it looks given the ceiling has climbed twice
  already (Gold Sword, Poison Dagger); (d) the block is currently creative/`/give`-only, so the
  "rare, one-off" framing behind the 110 XP + guaranteed-drop reward is only true because it
  isn't survival-obtainable yet — if a future recipe/drop ever makes `RISSOBELISK_ITEM`
  obtainable, the 2-hit max-investment kill turns this into a repeatable farm with only mild
  Silverfish friction, worth a placement limiter (cooldown/charges) before that happens, not
  after.
- **Visual identity**: dispatched `graphics-designer` for the block's first-ever
  blockstate/block-model/item-model JSON chain in this project (verified against real vanilla
  1.21.11 data via decompiled `stone.json`/`iron_block.json`/`sandstone.json`, not guessed) plus
  a placeholder texture. New "Riftstone" palette (cool blue-gray stone + magenta "Rift Glow"
  crack accent) — `docs/visual-style-guide.md` Section 20 (renumbered from an initial Section 19
  during the merge with `fischey_workbranch`, which independently claimed 19 for Drevathis the
  same day). **Palette-bucket judgment call**: Section 1.2's just-ratified boss-vs-common
  palette rule wasn't written with a non-fighting `Block` in mind; treated Rissobelisk as
  boss-tier anyway (rare, unique, one-off — the same properties Section 1.2's rule actually
  cares about, not literally "can it fight back"), reasoning recorded in Section 20.1.
- **Two real bugs found by the user's own in-game test, both fixed**: (1) `Risssplitter` had
  **zero visual assets** — registered in Java but nobody had produced its icon (an oversight in
  scoping the first `graphics-designer` dispatch, which only covered the block itself) — fixed
  with a second `graphics-designer` dispatch, reusing the block's own "Riftstone" palette so the
  material reads as a literal fragment of the obelisk. (2) The `RissobeliskBlock` `BlockItem`
  displayed its raw untranslated key (`item.baum2.rissobelisk`) instead of "Rissobelisk" —
  caused by omitting `Item.Settings.useBlockPrefixedTranslationKey()`, which vanilla's own
  `Items.register(Block, ...)` helper always calls so a `BlockItem` reads its parent block's
  `block.<namespace>.<path>` lang key instead of needing its own separate one (this exact
  vanilla behavior had already been confirmed in the `fabric-docs-researcher` findings above —
  the bug was in not applying it, not a documentation gap). Fixed in `registry/ModBlocks.java`.
- **Build passes; the fixes above are build-verified but not yet re-confirmed in a live
  session** (the client run after fixing them closed at the main menu without a world being
  joined) — see "Next recommended step".

### Mount System v1 (2026-07-11) — flutes, equipment slot, 3 rideable GeckoLib horses, mounted-combat rules

User brief: horse flutes in 3 tiers (ride-only / ride+fight / faster+melee-AOE), Ctrl+H to
mount/dismount, flute equipped in a new "advanced inventory", no knockback while mounted,
every horse attack animated, GeckoLib prioritized, per-tier size/armor visuals.

- **Naming: the user's requested tier names were CHANGED for IP compliance — flag to the user.**
  `ip-naming-compliance-checker` (web-verified) found "Military Horse" is the literal
  translation of Metin2's real top mount tier ("Militärpferd") and the basic/advanced/military
  labels mapped 1:1 onto Metin2's documented ride-only→combat→military breakpoints — forbidden
  by CLAUDE.md even translated. The *mechanics* are unchanged; the tiers are named after their
  horses instead (all cleared, incl. against WoW-de): **Wanderross** (ride-only), **Eisenross**
  (iron-armored saddle, may fight mounted), **Schlachtross** (full black plate, faster, melee
  AOE splash). "Horse flute" itself verified generic (Stardew Valley et al.); Metin2 uses
  seals/books, not flutes. Items: `baum2:wanderross_flute/eisenross_flute/schlachtross_flute`.
- **Core files**: `mounts/MountTier.java` (per-tier rules/stats), `entity/MountHorseEntity.java`
  (ONE class, 3 EntityTypes `baum2:wanderross/eisenross/schlachtross` — the FallenCometStone
  definition-injection pattern), `mounts/MountManager.java` (summon/dismiss),
  `mounts/MountEquipmentManager.java` (persistent `ItemStack` attachment, `OPTIONAL_CODEC` —
  plain `CODEC` can't encode EMPTY, real gotcha), `mounts/MountEquipmentScreenHandler.java` +
  `ui/MountEquipmentScreen.java` (the equipment inventory: one flute-only slot,
  write-through-persists on every change, opened server-side via `OpenMountEquipmentPayload`,
  **G** key), `ui/MountKeyBindings.java` (**Ctrl+H** toggle → `ToggleMountPayload`; vanilla
  keybinds can't chord, so bound to H + `InputUtil.isKeyPressed` Ctrl check —
  `Screen.hasControlDown()` NO LONGER EXISTS in 1.21.11, found by compile error),
  `mounts/MountedCombatHandler.java` (tier combat rules), `mixin/MountKnockbackMixin.java`.
- **Riding**: extends `PathAwareEntity` + the 4 vanilla controlling-passenger overrides — NOT
  `AbstractHorseEntity` (taming/breeding/saddle-GUI baggage; full research in
  `docs/fabric-modding.md`, 5 new dated sections). Dismount-by-any-path despawns the mount
  (`removePassenger` override); `startRiding(mount, true, true)` (force) bypasses the sneak/
  riding-cooldown silent-fail; seat via `.passengerAttachments(1.5F * renderScale)`.
- **Combat rules** (`MountedCombatHandler`): Wanderross cancels melee via
  `AttackEntityCallback`; every landed mounted hit triggers the horse's one-shot GeckoLib
  attack animation server-side (`triggerAnim` auto-syncs, zero networking); Schlachtross
  splashes 50% of dealt damage in r=2.5 around the struck target — scoped to
  `DamageTypes.PLAYER_ATTACK` (spells don't splash), excludes players/mounts, reentrancy-
  guarded, **capped at 5 nearest victims** (balance-reviewer critical finding: uncapped, it
  multiplied the known ~110x melee DPS ceiling by unbounded target count).
- **Knockback immunity while mounted**: cancellable HEAD inject on
  `LivingEntity.takeKnockback` (no Fabric event exists — confirmed), scoped to riders of our
  mounts only.
- **Real pre-existing bug found & fixed** (balance-reviewer): `DrevathisCursedBladeHandler`
  had NO reentrancy guard — its wave's own damage (indirectMagic, attacker=player) re-entered
  the handler, recursive waves per hit, quadratic under mount AOE. Guarded now, same
  boolean-flag pattern as the mount splash.
- **Balance findings logged, NOT fixed (judgment calls)**: (a) Poison Dagger's unscoped
  AFTER_DAMAGE handler now also poisons every AOE-splash victim — second independent path
  into the already-logged unscoped-handler issue; (b) summon/dismiss is free/instant/
  uncooldowned (full-HP mount re-summonable mid-fight; most boss abilities target the player
  directly so meat-shielding is limited, but the project's own respec-cooldown precedent
  suggests a small cooldown); (c) knockback immunity removes routine-melee interrupts on top
  of the DPS ceiling — user-specified, kept; (d) mount HP 40/60/80 is trash-scale only (boss
  hits are ~100 flat).
- **Assets** (`graphics-designer`, preview-verified via `tools/render_geckolib_preview.py`):
  shared `geckolib/{models,animations}/entity/mount_horse.{geo,animation}.json` (8 bones/18
  cubes incl. ALL armor cubes; `animation.mount_horse.idle/walk/attack`), per-tier
  `textures/entity/{wanderross,eisenross,schlachtross}.png` (armor cubes alpha-0 on lower
  tiers — cutout discards them; alpha contract verified programmatically), 3 flute item
  textures, generator `tools/gen_mount_horse.py`, style guide Section 22. Per-tier size =
  renderer `withScale` (1.0/1.1/1.25) + matching hitboxes.
- **Verified**: `./gradlew build` passes. **NOT yet play-verified** — same standing GeckoLib
  caveat as every renderer (render-state mistakes crash only at render time): `/give` each
  flute, equip via G, Ctrl+H each tier, ride/steer/sprint, sneak-dismount despawn, Wanderross
  attack-block message, Eisenross normal hits + horse attack animation, Schlachtross AOE
  splash + speed, knockback immunity vs. a zombie crowd, relog with flute equipped.

### GeckoLib Sword Template v1 (2026-07-11, same day, after Mount System v1) — animated sword line, first sword "Espenklinge"

User brief: a beautiful reusable sword TEMPLATE (python-generator pipeline like the prior
asset work), first sword in wood with better-than-vanilla GeckoLib animation, a distinct
animation when fighting on horseback, not droppable/craftable for now (visualization/
animation focus). Visual reference was a real-world museum longsword photo (historical
artifact — silhouette only, no game IP).

- **This is the mod's first GeckoLib ITEM** (everything GeckoLib so far was entities). The
  1.21.11 pipeline, verified against GeckoLib 5.4.5 sources (not docs/memory): item JSON
  (`assets/baum2/items/espenklinge.json`) selects by display context — gui/ground/fixed/
  on_shelf → flat icon model, everything else → `{"type":"minecraft:special","base":
  "baum2:item/espenklinge_base","model":{"type":"geckolib:geckolib"}}` (GeckoLib registers
  that special-renderer type via its own SpecialModelRenderersMixin; resolution goes
  item → GeoRenderProvider → GeoItemRenderer). The `_base` model is display-transforms-only,
  exactly like vanilla `trident_in_hand.json`. GeoItemRenderer centers the geo origin in the
  item cube (`adjustRenderPose` +0.5/+0.51/+0.5), so the geometry is authored with origin =
  grip = fist, and display rotations pivot the fist.
- **Template contract** (same shape as Fallen Comet Stone/Mount Horse): ONE shared
  `geckolib/{models,animations}/item/sword_template.*` for ALL future swords; per sword only
  a palette entry in `tools/gen_sword_template.py` (emits texture/icon/3 JSONs, atlas layout
  asserted identical) + one `TemplateSwordItem(settings, "<asset_name>")` registration + a
  lang line. Full spec: `docs/visual-style-guide.md` Section 23.
- **Java (main)**: `items/TemplateSwordItem.java` (GeoItem; idle controller + 2 triggerable
  one-shots `attack`/`attack_mounted`; `registerSyncedAnimatable` in ctor; client renderer
  injected via static factory because of splitEnvironmentSourceSets), `items/
  UndroppableItem.java` (marker interface + bound message), `combat/SwordAnimationHandler
  .java` (AFTER_DAMAGE + `DamageTypes.PLAYER_ATTACK` + skip-blocked — deliberately identical
  scoping to MountedCombatHandler's horse-attack anim, so a mounted hit animates horse and
  blade in the same tick; mounted = `getVehicle() instanceof MountHorseEntity` → cavalry
  sweep, else moulinet), `mixin/ServerPlayerEntityDropGuardMixin.java` (see below).
  **Java (client)**: `items/TemplateSwordItemRenderer.java` (shared geo/anim via
  `DefaultedItemGeoModel` + `withAltTexture` per sword), factory hookup in `Baum2Client`.
- **Espenklinge** (`baum2:espenklinge`, name compliance-checked CLEAR — checker fetched
  Metin2's actual full sword list + WoW-de/ESO/GW2/EQ/etc.; "[German tree]+klinge" noted as
  a safe reusable pattern for the line): wooden training longsword, stats EXACTLY vanilla
  wooden sword (`.sword(ToolMaterial.WOOD, 3.0F, -2.4F)` — baseline tier, no new balance
  surface, so no balance-reviewer pass was run on it) + `UNBREAKABLE` component (showcase
  item shouldn't break mid-demo). **No recipe by design; in the COMBAT creative tab; /give
  only in survival.**
- **No-drop**: no Fabric event exists for item drops → new Mixin on `ServerPlayerEntity`:
  `dropSelectedItem` (hotbar Q/Ctrl+Q) cancelled at HEAD pre-removal (zero loss risk; client
  prediction snaps back on next inventory sync — sub-tick blip), and `dropItem(ItemStack,ZZ)`
  (inventory-screen throws) re-inserts + cancels ONLY if the re-insert succeeded, falls
  through to vanilla drop if the inventory is full (losing the guarantee in that edge case
  beats deleting the stack). `isDead()` exempts the death-drop path on purpose — cancelling
  there would delete the item since the inventory is already being cleared (dying drops it
  like vanilla; that's the documented intent, not an oversight). Method descriptors were
  javap-verified against the mapped 1.21.11 jar.
- **Animations** (all preview-verified via `tools/render_geckolib_preview.py`, which gained
  backwards-compatible `--fit`/`--no-floor` flags because the mob-tuned auto-framing crops
  tall thin item models): 6s idle breathing loop with a late-loop grip-settle accent (the
  horses' accent-event idiom), 0.55s 360° forward moulinet ending at -360°≡0° so the idle
  handoff can't pop, 0.7s mounted cavalry sweep (left-shoulder windup → low forward pass →
  wide right cut; one keyframe sign bug — mid-sweep leaning toward the rider instead of the
  target — was caught in preview and fixed before anything shipped).
- **Verified**: `./gradlew build` passes; **headless dedicated-server boot is clean** (mod
  init, item registration, mixin config, GeoItem sync registration — no errors, world gen
  fine). **NOT yet play-verified** (standing GeckoLib caveat, plus two sword-specific ones):
  (a) the drop-guard mixin's injection points only actually apply when ServerPlayerEntity
  first CLASS-LOADS, i.e. on first player join — descriptors are javap-verified but the
  first join is the real proof; (b) the display transforms in `espenklinge_base.json` are
  geometry-derived ballparks (docs/visual-style-guide.md 23.5) — expect one in-game nudge
  pass on how the sword sits in hand. Playtest checklist: `/give @s baum2:espenklinge` →
  hotbar icon is the flat sprite; hold it (1st + F5 3rd person) → 3D wooden longsword with
  the subtle idle sway; hit a mob → moulinet spin flourish; summon Eisenross/Schlachtross
  flute, mount, hit a mob → cavalry sweep INSTEAD of the moulinet (and the horse's own
  attack anim in the same instant; Wanderross blocks the attack entirely, so no anim —
  correct); press Q / Ctrl+Q → actionbar "This blade is bound to you…", item stays; open
  inventory, drag it out of the screen → same; die with it → it DOES drop (intended); pick
  any anim moment → confirm no pop back to idle.

## Last change (on `fischey_workbranch`)

**GeckoLib Sword Template v1 + Espenklinge (2026-07-11, uncommitted, same working tree as
Mount System v1)** — see the section directly above. Also in this change: `tools/
gen_sword_template.py` (new generator), `--fit`/`--no-floor` flags on `tools/
render_geckolib_preview.py`, `docs/visual-style-guide.md` Section 23.

**Mount System v1 (2026-07-11, uncommitted)** — see the "Mount System v1" section above for
the full narrative; this entry exists so the next session knows the working tree contains it.
The previous committed change (`15dacb2`, map rework) follows below.

**Map rework, drawing-faithful pass (2026-07-10, follow-up) — seas, roads, village, and boss
placement now match the user's map exactly.** The first implementation of the user's rework
map (see "Previous change") kept the old noise-driven lakes and relocated the bridge to
wherever water happened to exist; the user re-sent the map stressing two things it had
missed: **"match the seas, path way"**, and **the spider/silverfish bosses are INSIDE the
caves — give the caves more room so bosses can move and jump**. This pass makes the drawing
authoritative: every feature below was measured off `run/heimgrund_rework_map.png`
programmatically (pixel→world), not eyeballed.

1. **Authored lakes** (`ZoneLayout.LAKE_SPINES`): the noise-blob lakes are GONE. Four lakes
   traced from the drawing — NW big lake (bulge to half-width 60), SE long diagonal lake,
   W pond, N pond near the zombie boss — each a spine polyline with per-vertex half-width
   plus a noise wobble on the edge so shorelines stay organic. `lakeDepth`/`shoreFactor`/
   `isBeach` are signed-distance-based now; sand fringes every lake (drawn that way) and
   the flat walk-out exit arcs are kept. **Static-init order gotcha found the hard way**:
   `ROADS_BY_CHUNK = routeRoads()` runs at class init and reads the lake shapes (water is a
   router obstacle), so `LAKE_SPINES`/`DESERT_SPINES` MUST be declared above it — declaring
   them below left them null during routing and crashed chunk gen with
   `ExceptionInInitializerError` (caught by the headless server run, fixed by moving the
   declarations up; a comment in the file now warns about this).
2. **Bridge back at the DRAWN crossing**: (128,68)-(188,128) across the SE lake's widest
   middle (the user's violet line bbox is (134,74)-(182,122); endpoints extended a few
   blocks up the banks so the routed roads reach them on dry land). The first pass's
   relocated bridge at (218,176) is gone.
3. **Authored desert territories**: the drawing has exactly three sand patches — one per
   stone territory — and no random desert anywhere else, so the free `DESERT_NOISE` patches
   are gone too. Three wobbled capsules (zombies NE ~(143,-186) r55, silverfish SW
   (-185,140)-(-105,215) r52, spiders SE (268,88)-(200,228) r50) + new
   `ZoneLayout.Territory` enum / `territoryAt()`. Stone-territory centers nudged to the
   measured dot positions ((142,-186), (-146,178), (246,158)).
4. **Road network = the drawn topology, edge for edge**: an E-W main road THROUGH the
   village (west cave ↔ east cave; it arcs north around the SE lake's finger on its own,
   since the lake is an A* obstacle), a north road forking at (-10,-110) to the zombie
   stones and the zombie boss, a SW fork at (-200,20) to the silverfish stones, a SE fork
   at (116,-4) over the bridge to the spider stones, and a south arc silverfish↔spider
   below the lake. **No south gate** — the Great Hall occupies the village's south. Gates
   moved to (0,-46)/(±46,0) for the bigger village; router keep-out r 34→44.
5. **Boss chambers INSIDE the caves** (user rule): each grand cave now ends in a carved
   flat-floored dome chamber — radius 16, height 11, floor y60, vertical walls to half
   height then a dome — at (424,0) east / (-424,-30) west, connected to its cave mouth by
   a straight sphere-carved corridor (guaranteed connectivity; the wandering grand tunnel
   from the same mouth stays for depth). `BossSpawnManager`: Spider Queen and Silverfish
   Broodcaller spawn points moved INTO their chambers; Zombie Colossus stays at its open
   NW clearing (drawn in the open).
6. **Village regenerated to the drawn layout** (`tools/gen_village.py`, 71x71 → 91x91,
   stamper origin (-45,64,-45)): gray RECTANGULAR respawn plaza (23x35, world x -11..11 /
   z -27..7) as the road hub the drawing shows, TWO north cottages flanking the north
   road, square west house + east Werkstatt flanking the E-W through-road (doors facing
   the plaza), Great Hall (27x17, copper roof, north entrance) at the south, straight
   through-roads instead of the old ring path, 3-gate perimeter (N/W/E, hall guards S).
7. **Territory-aware spawns**: `ZoneSpawnDirector` gained a silverfish-territory override
   (its sand is DESERT zone now — without this the desert's zombie-heavy ambient mix would
   leak into the silverfish patch; spider override generalized to the same helper), and
   `StoneSlotManager`'s desert scatter is pinned to `Territory.ZOMBIES` so scattered zombie
   stones can't land on the other two patches. Note: the silverfish ambient target (10)
   mirrors the already-reviewed spider territory value — not separately balance-reviewed.
8. **`MapExporter`** now draws the bridge violet over water and the three boss points red —
   the exporter's output uses the user's own annotation language, so future exports compare
   1:1 against their drawings.

**Verified headlessly in a fresh dedicated-server world** (`gradlew runServer` with piped
console commands; one real crash found and fixed on the way — the static-init ordering in
item 1): clean boot, village stamps at (-45,64,-45), 30 stone slots + 3 boss points
generate. With the chamber/bridge areas forceloaded: **all three bosses spawn** (`if
entity` passed for spider_queen, silverfish_broodcaller, zombie_colossus), **both boss
chambers are carved** (air at floor y60 with stone underneath, both sides), both
mouth→chamber corridors are open (fill-probes found 1127/768 air blocks east/west), and
**the bridge stands over real water at the drawn spot** (241 dark-oak plank deck blocks at
y64; 1333 water blocks at y62 beneath it — the first pass's dry-bridge failure mode is
structurally gone since the lake is authored). The exported `run/heimgrund_map.png`
matches the user's drawing feature-for-feature (lakes, territories, road topology, bridge,
boss dots). Client checks pending: chamber size feel in a real fight (sized r16/h11 for
Spider Queen, the largest boss), bridge deck look, the new village interior, road
walkability end-to-end.

Previous change, same day:

**User-designed map rework (2026-07-10) — the user drew the map, this implements it.** The
user annotated the exported layout PNG (their file described as heimgrund_rework_map.png)
with a full redesign: bigger village with the biggest building at its south and a gray
respawn point at center; curved ("no straight") roads; three stone TERRITORIES (zombies N/NE,
silverfish SW, spiders SE — spider stones return to the map, in flatland, mountains still
stone-free); three BOSS spawn points on 3-minute respawn timers (spider boss at the east
cave, zombie boss NW, silverfish boss at the west cave); and a bridge (violet on their map)
crossing the southeast lake. Implemented as:
1. **Geometry** (`ZoneLayout`): clearing r 60→100 (blend →130); POI set replaced (zombie
   stones on a forced desert disc at (130,-194), silverfish stones (-148,170), spider stones
   (256,162), zombie-boss clearing (-102,-284), both grand cave mouths kept); road router
   step-cost now carries a Perlin bias so roads MEANDER (user: "no straight paths" — heuristic
   stays admissible, multiplier >= 1); fixed BRIDGE segment (128,76)-(184,116) allowed over
   water, rendered by the generator as a dark-oak plank deck at SEA_LEVEL+2 with log piles,
   self-adapting (deck only where terrain is below deck height; on land the same line is
   ordinary path). Village keep-out for the router r 20→34, roads start at the 4 gates (0,±36),
   (±36,0).
2. **Stones** (`StoneSlotManager`): 30 slots as three territories (double ring 4@r14 + 3@r26
   each) + light scatter (5 silverfish, 4 zombies); MIN_RADIUS 140.
3. **New boss: Silverfish Broodcaller** (`entity/SilverfishBroodcallerEntity` + client
   renderer): the user's "Mother of Silverfish" concept — L8, 160 HP, 5 dmg, calls 2
   silverfish every 5s in combat (max 20/life, brood cascade-dies with it). **RENAMED from
   the user's proposed "Mother of Silverfish" after `ip-naming-compliance-checker` FLAGGED it
   as a near-identical rename of the existing "Mother Silverfish" CurseForge mod (same
   oversized-silverfish-summons-minions boss)** — flag this rename to the user. Rendering
   uses the recovered PRE-GeckoLib pattern (vanilla SilverfishEntityModel under a 3x-scaled
   EntityModelLayer via ModelTransformer.scaling — dug out of git history commit b9a403c;
   vanilla texture referenced in place, bespoke palette is a future art-pass item).
4. **`world/BossSpawnManager.java` (new)**: StoneSlotManager's persistent-slot machinery for
   the three bosses — fixed positions (cave-mouth aprons east/west, NW clearing), 3600-tick
   respawn, AFTER_DEATH detection, entity-ticking-gated driver, world-attachment persistence.
5. **Village** (`tools/gen_village.py` regenerated, 71x71): gray smooth-stone respawn circle
   at center (spawn cell kept clear), Heimstein stones on its rim, 17x11 copper-roofed Great
   Hall at the SOUTH (user: biggest building at the bottom), werkstatt east, 3 cottages,
   garden court, ring path with 4 cardinal gate spokes matching the road network,
   perimeter r33 with 4 gates. Stamper origin now (-35,64,-35).
Verified headlessly in a fresh world: village stamps; 30 stones; 3 boss points generate; all
three bosses spawn on chunk load (entity tests passed); **Broodcaller kill → respawned after
~176s (3-min timer verified live)**; the bridge was initially DRY (the r=100 clearing erased
the lake arm the user's violet line crossed — found by fill-probing zero water in the old
bridge box), relocated by analyzing the exported map PNG programmatically to the lake
finger at (218,176)-(246,178) west of the spider territory, then re-verified: 38 plank deck
blocks over real water. Client checks pending: Broodcaller's 3x-scale render (render-state
code paths only fail at render time), bridge deck look, hall interior, brood-call fight feel.

**`balance-reviewer` findings on this pass — 3 fixed, 3 logged for a human decision**:
FIXED: (a) ZoneSpawnDirector still sent silverfish/zombie ambients to the new SE spider
territory (cross-file drift) — a 70-radius spider-territory override now supplies ambient
spiders there; (b) Broodcaller was the only boss with ZERO drops — now drops 2 iron ingots +
12 iron nuggets ("paid in the monster's own currency" like the stones); (c) Broodcaller's
0.6 knockback resistance exceeded both stronger siblings, contradicting its "weakest boss"
framing — lowered to 0.3. LOGGED, NOT FIXED (design calls): (1) **brood XP loophole** — brood
silverfish a player kills BEFORE the boss dies pay normal XP (14 each; only the death-cascade
is XP-free), so kiting the weak boss and farming all 20 brood nets ~370 XP/3-min cycle vs the
90 XP boss kill — decide: tag brood as XP-free entirely, or accept "the fight is the reward";
(2) **3-min bosses + unchanged guaranteed full-item drops = repeatable BiS loop** — Spider
Queen drops ALL FOUR armor pieces every kill, Colossus the Warclub every kill; at known road-
connected spots every 3 minutes this is an infinite-gear faucet (frame as a drop-table
decision, e.g. one random piece per repeat kill — the timer itself is user-decided);
(3) **difficulty-vs-distance inverted** — the direction-based territories put L20 zombie
stones as close as L5 silverfish stones and the hardest boss (Colossus) CLOSER than the
weakest (Broodcaller); the layout is the user's own map, so treat as their explicit design
unless they say otherwise.

Previous change, same day:

**Road network rework (2026-07-09, follow-up to the 4th playtest screenshot).** User: roads
must never cross lakes (the ford still read as wrong — "just around the lake"), the network
must cover the WHOLE map ("balanced"), and every road must end at a cave or a stone spot.
Changes:
1. **A*-routed roads** (`ZoneLayout.routeRoads()`, runs once at class init, fully
   deterministic): every edge of the road network is routed on a 2-block grid around
   obstacles — lakes (+4-block margin), the mountain ring (except the final approach to a
   cave POI), and the village interior. Fords are REMOVED: a road geometrically cannot touch
   water anymore. Routed segments are chunk-bucketed (same trick as the cave spheres) so the
   per-column isPath test stays cheap during generation.
2. **Balanced 6-POI ring network**: stone hot spot (S), west stone cluster (SW), NEW west
   grand cave mouth (W, second carveTunnel), NEW north stone cluster (N, forced meadow),
   desert pocket (NE), east grand cave mouth (E) — connected as a CLOSED loop around the map
   (incl. an SE edge; without it the ring was a C-shape) plus the two village-gate spokes.
   Every edge terminates at a cave or stone POI (user rule). Slot table stays 30: rings
   5+3+3 silverfish + 3 zombies, scattered 7 silverfish + 9 zombies.
3. **`world/MapExporter.java` (new dev tool) + `/baum2 world map` (op)**: renders the whole
   authored layout (zones/roads/POIs/beaches, mountains height-shaded) to heimgrund_map.png
   from pure ZoneLayout math — THE way to sanity-check map changes now; both the open-C
   problem and the routed-around-lakes behavior were caught/confirmed on the rendered map
   before any client session.
Verified: build passes; fresh world boots; 30 slots; exported map confirms roads avoid all
water, ring closes, all six POIs connected. In-client: walk a road end-to-end next session.

Previous change, same day:

**Ford/branch/shore fixes (2026-07-09, follow-up to the 3rd playtest screenshot).** The
previous pass's path-through-lake handling produced a walled dirt DAM towering over the water
(the dry corridor kept full meadow height), and the uniform beach easing left 1-2-block
terraced banks. User feedback: paths must be readable/usable, branch paths should fork off
the mains, and lake exits should be ~flush with the water but only in places ("multiple
chances to get back", not everywhere). Changes (all `ZoneLayout` + `StoneSlotManager`):
1. **Fords, not dams**: a path crossing a would-be lake now flattens its corridor (3.5 wide)
   to exactly 1 above water level - guarded to only trigger where a lake would truly exist
   (lake band, not desert), not wherever lake noise is high.
2. **Gated shore exits**: shoreline within the shore band drops to water level (walk straight
   out) only where an independent noise gate passes (~half the shoreline in scattered arcs,
   marked by a sand strip); the rest eases to a low ~2-block bank.
3. **Branch paths + two new stone POIs**: WEST_BRANCH forks at the south path's midpoint to a
   3-silverfish-stone ring at (-140,180) (forced meadow, small apron); DESERT_BRANCH forks at
   the east path's midpoint to a **forced desert disc** at (240,-140) (r=30, overrides the
   noise mask so the POI exists on any seed) with a 3-zombie-stone ring + apron. Slot table
   stays 30 total (rings 5+3+3, scattered 10 silverfish + 9 zombies).
Headless-verified in a fresh world: 30 slots incl. both new rings at the right coordinates,
`baum2:doerrsand` biome at the forced pocket, `baum2:lichtwiese` at the west cluster, apron
gravel present. The ford itself is construction-verified only (lake crossings can't be
located headless) - check one in the next client session.

Previous change, same day:

**Map-design pass (2026-07-09, follow-up to the 2nd playtest).** User feedback: still minutes
of searching for any monster or stone ("it should be more dangerous"), the map needs pathways
leading to destinations ("Stone hot spot, Cave hot spot"), lakes have cliff edges you can't
climb out of, and — new hard rule — **no stones may spawn on the mountains**. Changes:
1. **`world/ZoneSpawnDirector.java` (new)** — active per-player danger floor. Vanilla natural
   spawning spreads one global cap probabilistically; this director instead checks every 5s
   how many zone-appropriate monsters are within 48 blocks of each player and tops up the
   deficit (max 4/pass) at 20-44 blocks distance, zone-matched, `SpawnReason.NATURAL` so
   vanilla despawn cleans up. Targets: meadow 8 silverfish, desert 12 (zombie 4:1 silverfish),
   mountain 12 (spider 4:1 cave spider), clearing none. **Cannot be verified headless** (needs
   a real player nearby) — this is THE thing the next playtest must judge, and the targets are
   single constants in `POPULATIONS` for easy retuning.
2. **Stone slots: 30 total, meadow/desert ONLY** (user rule: never on mountains — spiders stay
   stone-less natural monsters there): 18 silverfish (5 of them a deliberate pentagon ring
   around the stone hot spot) + 12 zombies; spacing 40. Spider/cave-spider stones removed.
3. **Pathways & hot spots** (`ZoneLayout` + generator): two authored dirt-path/gravel routes —
   south gate → **stone hot spot** (gravel apron at (30,240), the 5-stone ring around it),
   east gate → **cave hot spot** (gravel apron at (378,0) + a guaranteed extra-wide "grand
   mouth" tunnel bored into the ring exactly where the path ends). Lakes are masked off paths
   (a path over a would-be lake becomes a dry causeway); the stone hot spot area is always
   meadow. Paths keep the biome they cross (only the surface block changes — no biome slivers).
4. **Lake shores fixed**: terrain within the shore band is pulled down to ~1.5 above water
   (`shoreFactor` in `ZoneLayout.surfaceHeight`) and a sand strip marks the waterline
   (`isBeach`) — every lake is now walk-in/walk-out all the way around.
Headless-verified in a fresh world: 30 slots / 0 mountain stones / hotspot pentagon present;
dirt_path on the south route; gravel apron + 491-block tunnel air at the grand mouth. NOT yet
verified: spawn-director density (needs a player), shore feel in-game.

Previous change, same day:

**"More life" density pass (2026-07-09, follow-up) — first real playtest feedback acted on.**
User played Heimgrund and reported the map feels empty outside the village, with monsters
visible only in the mountains. Root causes found and fixed:
1. **Real bug: zero passive animals.** `HeimgrundChunkGenerator.populateEntities` was an empty
   no-op — but that vanilla hook is what seeds biome `creature` lists at chunk generation, and
   post-generation creature spawning is nearly nonexistent (tiny cap, persistent animals).
   Now runs vanilla `SpawnHelper.populateEntities` (NoiseChunkGenerator's exact pattern:
   `ChunkRandom` + `setPopulationSeed`). Verified headlessly: sheep/cows generate in meadow
   chunks. Mountain biome also gained goats (creature list).
2. **Stone slots doubled 11 → 21** (silverfish 7, zombies 6, spiders 5, cave spiders 3),
   spacing 60→50 so the desert patches fit their share. Existing worlds keep their frozen
   11-slot table — only NEW worlds get 21 (slot tables never regenerate by design).
3. **Denser monster packs** (meadow silverfish 1-3, desert zombies 2-4, mountain spiders 2-4 /
   cave spiders 1-3) and **~10x meadow tree density** via the mod's first custom placed
   feature (`data/baum2/worldgen/placed_feature/lichtwiese_trees.json`, ~0.55 trees/chunk vs
   plains' ~0.05; safe from the "Feature order cycle" crash because it's unique to one biome).
   Trees are cosmetic-only (block breaking is disabled dimension-wide).
Natural-spawn DENSITY still needs a live-client re-check (headless servers have no player, and
vanilla only spawns monsters near players) — see "Next recommended step".

Previous change, same day:

**Dorfanger Hub village (2026-07-09, follow-up) — the starting village is now a real build,
not a placeholder.** User brief: "can you build a village on your own? → yes, do it."
`graphics-designer` produced the architecture spec (`docs/visual-style-guide.md` section 21);
`tools/gen_village.py` (new) authors the village as code with rendered isometric previews for
visual iteration, and writes the structure-NBT itself (own minimal NBT writer, DataVersion
read from a game-saved template). `VillageStamper` now stamps `village_heimgrund` at
(-23,64,-23); the old 9x9 placeholder .nbt is deleted. Verified in a fresh world via the
established headless-server method (stamp log + block probes: monument, copper ridge, plaza,
spawn-cell air, perimeter wall/gate). Balance-irrelevant (no numbers), naming already covered
(Heimstein/Dorfanger are documented in section 21; "Heimstein" is a new player-visible-ish
lore name only in docs so far — if it ever appears as in-game text, run the naming checker).

Previous change, same day:

**Heimgrund (2026-07-09, third commit of the day) — the starting dimension.** User brief: the
world starts in another dimension — central village (blocks indestructible/unplaceable), wild
low-tier monsters outside (weakest nearest), stones spawning in matching monster zones and
only respawning when one is destroyed, grass/lakes/desert (zombies, silverfish) + mountains
with caves (spiders), finite world walled by impassable mountains, village kept empty of NPCs
for now. Full detail (design, per-class responsibilities, verified 1.21.11 API changes, the
"Feature order cycle" biome-JSON gotcha, and the headless-verification method) in the
"Heimgrund" section above. Everything from dimension JSON to stone-slot respawn was verified
against a live dedicated server via console driving (`runServer` + piped commands) — a new
verification technique for this repo worth reusing: it catches runtime-only failures (biome
JSON crashes, worldgen bugs) without a GUI. **Not yet play-tested in a real client** — that is
the top "Next recommended step" item now.

Previous change, same day:

**Stone ladder (2026-07-09, second commit of the day) — 33 fallen-comet stones, one per
normal vanilla hostile monster, config-driven.** User brief: "for every monstery entity add a
stone; make level/name which makes sense; ignore boss monsters; probably every 5 levels; you
may rebalance existing stones." Full detail in the "Stone ladder" section above and
`docs/visual-style-guide.md` 13.6/13.7. Key facts: one generic `FallenCometStoneEntity` +
`FallenCometStoneDefinition` record replaces the two hand-written stone classes (deleted);
the whole family lives in `registry/FallenCometStones.java` (levels 5-95 in 5-steps, HP =
20×level, themed vanilla drops); registration/attributes/renderers are 3 loops; 31 new
palettes/textures via `tools/gen_fallen_comet_stone.py`. Rebalances: stones
explosion+fire-immune; wave mobs insta-aggro the attacker (real Angerable anger API);
Piglin/Hoglin waves zombification-immune; Slime/Magma Cube waves fixed size 2;
`MobDeathHandler` XP eligibility widened from `instanceof HostileEntity` to Monster-or-
MONSTER-spawn-group (fixed a real gap: 8 stones' wave mobs paid zero XP — affects all mob
kills mod-wide, not just stones). `ip-naming-compliance-checker`: all 31 names clear (one
system-level Metin2-mechanic-resemblance observation logged for the humans, see the section
above). `balance-reviewer`: findings triaged fixed/intended/logged in the section above —
notably the guaranteed rare drops (totem/netherite scrap/shulker shells) need a decision
before stones ever spawn naturally. Merged to `master` as a fast-forward (no divergent work
on the other side). **Live-client spot-check across tiers still pending** — see "Next
recommended step" 2.

Previous change, same day:

**Fallen Comet Stone rework (2026-07-09) — both stone mini-bosses remodeled as a crashed
comet on GeckoLib, via one shared reusable template.** User brief: "remodel these stones,
make it more accurate like a comet which has fallen down; create a template stone; use
GeckoLib; mechanics keep the same." Full detail in the "Fallen Comet Stone rework" section
above and `docs/visual-style-guide.md` 13.5/15.4. Key facts: mechanics untouched (entities
only gained GeoEntity boilerplate); one shared geo+idle-animation pair
(`fallen_comet_stone.*`) for all stone bosses, per-stone texture only; new
`tools/gen_fallen_comet_stone.py` generates geometry, animation, and every stone texture
(pixel-identical atlas layout across palettes, asserted); old `HulkingCocoonStoneEntityModel`
+ both per-stone renderers deleted. Build passes; model/animation/both palettes
preview-verified offline; **not yet seen in a live client** (a GeckoLib render-state mistake
only crashes at render time) — `/summon` both stones, see "Next recommended step" 2. Merged
to `master` as a fast-forward (no divergent work existed on the other side, so no
merge-integration review was applicable).

Previous change, kept for context:

**Drevathis: complete rework (2026-07-07) — GeckoLib demon-lord model, new 4-skill kit,
per-player storm passive, blade item rework.** User brief: total freedom to remodel; "a demon
which is born to kill you, bigger than the player, black blade with dark smoke (also the
droppable player item, proportional), all attacks GeckoLib-animated" + exact skill specs.
Visual detail in `docs/visual-style-guide.md` **Section 19.7** (supersedes 19.1-19.6; new
"Umbral Sovereign" palette). Build passes; **needs an in-game look + playtest** (spawn
`baum2:drevathis`, check model/animations/each skill/the storm passive, and hold the dropped
blade in first/third person).

- **Third GeckoLib boss** (pipeline: `tools/gen_drevathis.py` model+texture,
  `tools/gen_drevathis_anims.py` six animations, every pose preview-verified). Old
  `DrevathisEntityModel`/`DrevathisHeldWeaponFeatureRenderer` deleted;
  renderer/render-state/geo-model follow the Colossus pattern exactly (empty render state -
  crash gotcha documented there). The black blade is a real `blade` bone on the right forearm
  (base rotation [24,0,14]); its "dark smoke" is a server-side SMOKE/SQUID_INK wreath
  (`tickBladeSmoke()`), scaled down in the new `items/CursedBladeItem.inventoryTick()` for the
  player-held drop.
- **Old kit deleted** (Dash of Death / Chain of Death / Wave of Darkness / Thunder of
  Darkness + Darkness aura). `combat/DarkWaveEffect` is KEPT - the blade item's on-hit proc
  (`DrevathisCursedBladeHandler`, unchanged) still uses it; the boss no longer does.
- **New kit** (all numbers user-specified unless marked assumption):
  - *Passive "Sovereign's Storm"*: players within 100 blocks get a personal thunderstorm
    (dark sky/rain/thunder) via per-player `GameStateChangeS2CPacket`s - client illusion only,
    re-sent every second so it wins against real weather broadcasts, restored on leaving the
    aura and in `remove()` (boss death/discard). Random thunder rolls + no real lightning.
  - *Basic attack*: NO melee (ATTACK_DAMAGE 0). Throws a `DarkWaveProjectileEntity`
    (`baum2:dark_wave`, EmptyEntityRenderer + server particle crescent): 50 dmg, 2s CD, 0.85
    blocks/tick straight line aimed at cast-time position - dodgeable by strafing. Launch at
    tick 6 of the 0.6s throw_wave animation (timing contracts in the anims generator).
  - *Curse Ground*: 15s CD, 1.6s channel (blade raised then scythed into the ground, zone
    erupts at tick 20): 8-block zone for 8s (radius/duration = my assumption), burn 10 dmg
    per second-tick with fire-resistance counterplay (BurnDamageManager's convention,
    implemented inline because that manager's refresh semantics don't fit a standing zone) +
    exact -25% move speed via attribute modifier (`baum2:drevathis_dread_slow`,
    ADD_MULTIPLIED_TOTAL -0.25; vanilla Slowness has no 25% step). Zone state lives on the
    ENTITY (outlives the goal); slow add/remove is one central per-tick reconciliation
    (`tickDreadSlow`) shared with The End is Near so overlapping sources can't double-apply
    or leak the modifier.
  - *Stampede*: 18s CD, three horns-first charge passes (direction locked per pass, direct
    per-tick velocity - navigation is too clumsy at 0.85 blocks/tick), 50 dmg + skyward
    launch per hit, one hit per player PER PASS (spec's "up to 3 hits"), dark burst on
    impact; `stampede_run` anim loops via synced STAMPEDE_ACTIVE TrackedData.
  - *The End is Near*: 36s CD, triggers at <=5 blocks, 5s channel (= the 5.0s end_channel
    anim, arms spread skyward, trembling): pull 0.09 blocks/tick every 2nd tick within 9
    blocks + the shared -25% slow; walking out still beats the pull (escape hatch per spec).
    Fire comets every 8 ticks land 1.5-4.5 blocks OFF a random player (visible 12-tick
    FLAME/SMOKE fall, then 30 dmg in 2.2 blocks - comet damage is my assumption, spec gave
    none). Channel end: anyone still inside takes 100 dmg + 2s of terror (random velocity
    shoves every 5 ticks + Nausea/Darkness - closest server-side approximation of "runs in
    random directions"; true movement control would need a client mixin).
- Misc: `entity.baum2.drevathis` / `entity.baum2.dark_wave` / `item.baum2.drevathis_cursed_blade`
  lang entries added (the first and last were MISSING before this rework - raw keys showed
  in-game). Old duplicate `src/client/resources/.../drevathis.png` deleted (caused a jar
  duplicate-entry build failure; GeckoLib boss textures live in `src/main/resources`). New
  flat icon + 3D in-hand model for the blade (`tools/gen_drevathis_blade_item.py`,
  display-context select like the warclub). `docs/fabric-modding.md`'s two-handed
  held-item-rendering section marked superseded.
- `ip-naming-compliance-checker`: **clear** on all new names ("Curse Ground", "Stampede",
  "The End is Near", "dark wave", "Sovereign's Storm", "Umbral Sovereign", "Voidsteel" - all
  generic/original; "Sovereign's Storm" flagged only for a routine re-check if it ever
  becomes a UI string).
- `balance-reviewer` (it re-simulated goal arbitration, dodge windows, and escape math):
  - **Applied fix 1**: cooldown arming was inconsistent (wave/curse armed at the effect tick,
    stampede/end in `stop()`) - preemption could silently swallow wave/curse casts with no
    cooldown cost. All four goals now arm their cooldown in `stop()`.
  - **Applied fix 2**: comets launched after channel-tick 88 could never land (the goal's
    `stop()` cleared them mid-air) - launches now cut off at `CHANNEL - FALL` ticks.
  - **Confirmed clean**: no kiting dead zone (ApproachGoal covers all trigger-range gaps
    including 20-24); The End is Near escape math checks out (even from 1 block away, slowed
    WALKING clears the 9-block edge with ~1.3s of the 5s channel to spare); dark wave
    confirmed strafe-dodgeable at every range; no unavoidable 100-to-0 combo; blade drop
    unaffected; curse burn's fire-resistance counterplay correct from the start.
  - **Open judgment call 1 (stacking window)**: the Curse Ground ZONE outlives its goal (8s),
    so The End is Near can start inside a live zone -> overlapping burn + pull + comets +
    finale reaches ~190-220 of 500 HP in one window. Not lethal, each piece still dodgeable,
    but an unintended overlap - options: shorter zone life, or gate End Is Near while a zone
    is live. Left as-is pending user decision.
  - **Open judgment call 2 (tier shape)**: Drevathis (L40) has a LOWER practical unavoidable
    DPS ceiling than the L25 Colossus, because every attack is a dodge/positioning check by
    design ("no melee at all" per the user spec) while the Colossus has a raw 50 dmg/s melee
    tick. Skill-check boss vs. brawler boss - flagged so the tier pacing is a conscious
    choice, not an accident. HP/level ratio stayed flat at 30 (1200/40, same as 750/25).

Previous change on this branch:

**Colossal Warclub: 3D in-hand item model (2026-07-06, follow-up to the boss rework below).**
The club a PLAYER holds is now a real 3D cuboid model matching the boss's club design
(`tools/gen_colossal_warclub_item.py` -> `models/item/colossal_warclub_in_hand.json` +
`textures/item/colossal_warclub_3d.png`, ~1.7 blocks long in hand), using the
element-rotation-onto-the-sword-diagonal trick so stock `item/handheld` display transforms
apply. `items/colossal_warclub.json` now selects by display context (vanilla trident.json's
exact schema): GUI/ground/fixed/on_shelf keep the previously approved flat icon, hands get the
3D model. Details in `docs/visual-style-guide.md` 18.4 addendum. Build passes; **needs one
in-game look** (hold the club in first + third person; the context-select JSON is
runtime-validated only).

Previous change on this branch:

**Zombie Colossus GeckoLib rework + new "Earthquake" skill (2026-07-06).** Full visual detail
in `docs/visual-style-guide.md` section 18.7. **User-confirmed in-game ("the zombie model
itself looks really good" → after one fix, "perfect!")** — the one round of visual feedback was
the club's rest carry direction (was back-over-the-shoulder, now forward-down at the hip; the
swing-pose keyframes were offset-compensated so impacts are unchanged). Summary:

- **Second GeckoLib mob** (same pipeline as Spider Queen's redesign below): old hand-written
  `ZombieColossusEntityModel` deleted; new bespoke muscular geometry + pixel-art texture from
  `tools/gen_zombie_colossus.py` (Ashen Brute palette kept - Section 18.3 is ratified), full
  animation set from `tools/gen_zombie_colossus_anims.py` (idle/walk/smash/rage/leap_windup/
  leap_flight/earthquake), every pose verified in `tools/render_geckolib_preview.py` before
  shipping. **The Colossal Warclub is real model geometry now** (a `club` bone on the right
  forearm; two-segment arms so elbows bend) - the entity equips no ItemStack anymore
  (`initEquipment` is deliberately empty; the guaranteed club drop in `dropLoot` is
  unchanged, and the item icon was not touched).
- **Animation/server timing contracts** (documented in the anim generator's docstring, the
  Goals must stay in lockstep): smash damage now lands 6 ticks after the swing starts
  (was: instant on a 0.8s-later-looking swing); rage strikes at 8/13/17 ticks after its
  trigger; leap windup 10 ticks / flight 14 ticks (flight now synced via a new
  `LEAP_FLIGHT_ACTIVE` TrackedData; the old `RAGE_WINDUP_TICKS` TrackedData is gone -
  GeckoLib's own trigger sync covers one-shot anims: `triggerableAnim` + `triggerAnim`, a
  mechanism Spider Queen didn't need but is now proven here).
- **New skill "Earthquake"** (user spec: 100 damage, 18s cooldown; radius 9 was my choice):
  goal priority 1, 0.75s sky-high wind-up telegraph, then an AoE slam - damage + upward buck
  to all players in radius, expanding `DUST_PILLAR` ground-particle rings (no blocks modified)
  + velocity jolts for 0.6s so the shake is physically felt, mace-heavy-smash + explosion
  sound. `ip-naming-compliance-checker`: **clear** (generic dictionary word, code-internal
  only today; suggested picking a more distinctive fantasy name if it ever gets a UI string).
- **`balance-reviewer` findings on the reworked kit** (it simulated the actual goal-priority
  arbitration tick-by-tick):
  1. **Fixed in the same pass**: `ColossusAttackGoal`'s cooldown lived on the Goal instance
     and reset to 0 in `start()` every time rage/leap/earthquake preempted and returned
     control - quietly inflating real burst beyond the "100 dmg / 2s" spec. Moved to an
     entity-level `attackCooldownTicks` ticked centrally (same as the other three), and a
     `stop()` override now drops any in-flight delayed strike so it can't fire stale on
     resume.
  2. **Open judgment call (lethality cliff)**: worst-case opening burst in tight melee
     (earthquake slam + rage combo + one base attack ≈ 500 dmg in ~3.4s, only the quake
     telegraphed) kills a fresh 500-HP character with essentially no error budget, while an
     Endurance-built 2480-HP character gets a sane ~34s fight - much steeper than Spider
     Queen (lvl 15, 75-dmg max hit) for a 10-level gap. Nothing gates who can reach this
     boss. Confirm intended or add gating/scaling.
  3. **Open judgment call (range asymmetry)**: a player camping just outside the 4.5 melee
     range can dodge every base attack for free (6-tick damage delay + only +1.0 range grace
     vs. sprint speed) and outrun Earthquake's 9-block radius during its 0.75s windup, while
     a point-blank player can escape neither - ranged-adjacent play trivializes, tight melee
     risks near-instant death. Confirm this is the intended identity for a slow heavy brute.
  4. Cleared: the shake-phase velocity jolts are too small to exploit or ledge-shove; the
     boss freezing for the full 2.2s earthquake animation on a dodged slam is a fair miss
     punish, noted as intentional-feeling.
- Build passes; model/carry confirmed by the user in-game. Fine-grained per-attack playtest
  (rage combo timing feel, Earthquake damage/jolts from the receiving end) has not been
  itemized point-by-point - treat as "no reported breakage," same convention as every other
  boss here.

Previous change on this branch, kept for context:

**Spider Queen visual redesign + real leap animations (2026-07-06, committed as `92f9794`
and pushed, user-confirmed in-game: "Looks very good").** Executed the redesign `docs/spider-queen-fable-handoff.md`
asked for — full design detail in `docs/visual-style-guide.md` section 17.6. Highlights:

- **New capability that changes how visual work happens here: `tools/render_geckolib_preview.py`**
  (born as `render_spider_queen_preview.py`, then generalized in the same session: any GeckoLib
  model via `--model <name>` file-discovery, animation short-name lookup without a hardcoded
  prefix, camera auto-fitted from the posed model's bounding box — future GeckoLib mobs get
  this workflow for free; vanilla-`ModelPart` mobs would need a GeckoLib migration first)
  renders the GeckoLib model + texture + any animation pose to a PNG contact sheet, using
  GeckoLib 5.4.5's own transform/UV/animation-sign logic transcribed from its sources jar (in
  the Gradle cache) — model/texture/animation changes can now be SEEN and iterated on without
  booting the game. This directly solved what the handoff doc called the core problem (every
  prior visual judgment was made blind). Rendering the old committed model with it immediately
  showed why it read as "a grey box with legs" — including a real texture bug (every leg face
  sampled its atlas cell's top highlight strip, washing the legs out to near-white).
- **Texture**: `tools/gen_spider_queen.py`'s texture half rewritten — packed pixel-art atlas
  (128x48, 2px/model-unit, per-face painting): structured jumping-spider eye layout painted on
  the face (eye-cubes + full-black face plate removed), banded legs with claw tips, mirror-
  symmetric abdomen marking (kept, refined), chitin shading everywhere. Pinned palette
  decisions unchanged (`#898989` primary, one red family `#FF3B1E`).
- **Geometry**: vanilla-accurate skeleton kept, head stays a plain cube; two fang bones
  (chelicerae) added as children of `head` (GeckoLib parent-bone support confirmed from
  source). Coordinate-math self-check still PASSes; no Java changes needed anywhere.
- **Animations**: `spider_queen.animation.json` regenerated via new
  `tools/gen_spider_queen_anims.py` (one gait table → mirrored keyframes for all 8 legs +
  2 fangs; sign conventions documented in its docstring, verified against GeckoLib source and
  visually in the preview renderer): real idle (breathing/head scan/fang chew/leg tap),
  alternating-tetrapod walk, a genuinely telegraphed leap wind-up (crouch, abdomen cock,
  raised-front-legs threat pose, fang flare, tremor, recoil — 0.75s, must stay equal to
  `SpiderQueenEntity.LEAP_WINDUP_DURATION_TICKS`), and a two-stage leap flight (legs swept
  trailing at launch, front pairs whip forward mid-flight to grab, holds last frame).
- **Verified**: generator self-check PASS, `./gradlew build` passes, every pose reviewed in the
  preview renderer. **Not yet user-confirmed in a live session** — that is the next step
  (`./gradlew runClient`, `/summon baum2:spider_queen`, watch idle/walk, then get hit by the
  leap). The old red-particle-smoke removal and every other prior rejection remains respected.

Previous change on this branch, kept for context:

**GeckoLib migration — Spider Queen rebuilt on GeckoLib, then two live-test follow-up fixes**,
full detail in "Current state" above under the three "GeckoLib migration..." sections. Short
version: fast-forward-merged `origin/master` (jonas's Skill System/Class Overhaul v2/Rissobelisk
work, zero conflicts) and pushed, then added GeckoLib as a dependency and rebuilt Spider Queen's
model/animation on it to improve the leap attack's wind-up/flight pose quality (the leap's actual
physics are unchanged — GeckoLib is rendering/animation only). A Sketchfab spider model the user
found was investigated for possible reuse, found not importable as a mesh into either vanilla's
or GeckoLib's cuboid/bone-only model systems, and used instead as a proportions/silhouette
reference only (attribution recorded in new `CREDITS.md` per its CC BY 4.0 license).

**Then two real, user-reported problems on the first actual live tests** (both now fixed): (1)
an instant crash rendering Spider Queen — root-caused to this project's custom
`SpiderQueenRenderState` fighting GeckoLib's own Mixin-injected `GeoRenderState` support instead
of just using it, fixed by emptying that class out; (2) once rendering worked, the bespoke
17.1.1-era geometry "doesn't look like a spider," fixed by discarding the invented proportions
and transcribing vanilla's own real spider geometry bone-for-bone instead (self-checked
numerically, not just visually assumed-correct), plus a full palette rework to grey/shiny
redstone-red per explicit user spec, including a genuinely pulsing (not static) redstone-dust
particle glow on the abdomen. Verified: `./gradlew build` passes clean after every step. **Not
yet re-verified in an actual game session** for this latest geometry/palette pass specifically —
no live-render tooling exists in this environment, same standing caveat as every other boss in
this document, though confidence is higher than usual here given the geometry is a verified
transcription of known-correct vanilla proportions rather than an invented shape. `master` is not
yet fast-forwarded to include any of this work (only the earlier jonas-merge portion is on
`master`) — next step for whoever picks this up is to decide when to push that.

## Last change (on `jonas_workbranch`, fast-forwarded into `master`)

**Merged `origin/master` into `jonas_workbranch`** (Fischey's Drevathis, the Cursed Sovereign
boss + a Zombie Colossus playtest-fix round — see the folded-in entries below for full detail on
each) at the user's request ("pull all and merge to jonas_workbranch"). Used
`merge-integration-reviewer` proactively per `CLAUDE.md`, including a real trial merge in a
disposable worktree before touching the actual repo. 4 files had real textual conflicts, all
resolved by keeping both sides' content (never "pick one"): `registry/ModItems.java` (both
branches inserted a new item block — `RISSSPLITTER`, `DREVATHIS_CURSED_BLADE` — at the same
anchor point), `docs/fabric-modding.md` and `HANDOFF.md` (both appended a new section/entry at
the same point), and `docs/visual-style-guide.md` (asset-listing table rows + Changelog
entries). One **design conflict with zero textual markers**, caught only by the reviewer's own
read-through: both branches independently claimed `## 19.` as their new top-level section in
`docs/visual-style-guide.md` (Drevathis vs. Rissobelisk), with colliding `19.1`-`19.6`
sub-numbering and ~20+ internal cross-references each — resolved by renumbering Rissobelisk's
whole block to `## 20.` (it had more self-references to fix than Drevathis, but none of them
were already "locked in" from outside the file the way one of Drevathis's `HANDOFF.md`
references was). Ran a final `ip-naming-compliance-checker` ensemble pass over the combined
name set (Drevathis, Drevathis's Cursed Blade, Rissobelisk, Risssplitter — plus "Wave of
Darkness," Drevathis's on-hit proc name, checked specifically since it's a skill name not a
proper noun) per the reviewer's own recommendation, since each was only checked individually
before. **All 5 clear individually and in combination** — no new resemblance emerges from the
two unrelated systems appearing together. The only thing re-surfaced was the same
already-logged, non-blocking mechanic-level observation (see the "Rissobelisk" section above and
"Next recommended step" item 13) — Rissobelisk is now the third/fourth reuse of the
wave-spawn-on-HP-threshold pattern, and the "deliberate human decision" on whether to keep
reusing it still hasn't been made. The reviewer suggested a concrete alternative for *next*
time this pattern would otherwise get reused: decouple wave-spawns from "percent of HP lost" and
drive them off something structurally different instead (a fixed/random timer independent of
damage, a shrinking-safe-radius effect, or interrupting a periodic self-heal pulse). **Bug found
during review, not
fixed here (belongs to the other side)**: `en_us.json` has zero lang entries for
`entity.baum2.drevathis`/`item.baum2.drevathis_cursed_blade` — the exact same bug class this
session's own Rissobelisk work found and fixed for `Risssplitter` earlier today (raw
untranslated key shown in-game) — flagged here for Fischey rather than fixed unilaterally, since
it's entirely his branch's content and not part of this merge's own conflict set.

Earlier, on `fischey_workbranch` (now merged): **second Drevathis fix round** (same session,
immediately after the round below): the user reported the held weapon "looks like it has a
penis" and asked for it to actually fit the hand. Root cause was the held-weapon renderer
anchoring at the model root only and translating to one fixed point in front of the torso with
no arm-following rotation, producing a static rod projecting from the body's center regardless
of pose. Fixed by anchoring at the right arm's own live transform (the same mechanism vanilla's
`HeldItemFeatureRenderer` uses) so the weapon now follows the arm's grip pose and every skill's
animation, offset toward the actual hand, tilted off dead-straight-forward, and scaled down
slightly. Ruled out a texture bug first by inspecting the entity texture directly pixel-by-pixel
before concluding the actual cause was the render code, not the art.

Earlier, on `fischey_workbranch` (now merged): **playtest fix round on Drevathis** (immediately
after the commit below, same session): the user summoned and inspected the boss and reported
three things — "the demon boss has no weapon and looks like a hobbit", "the weapon does not look
like it comes from a demon cursed blade", and "the skill itself is not visible... also not from
the boss." In short: (1) a real structural bug, not a render issue — `MobEntity.initialize(...)`
never calls `initEquipment(...)` in this version except from specific vanilla subclasses' own
overrides, and `DrevathisEntity` (extending `HostileEntity` directly, unlike every zombie/
spider-based prior boss) had no such override, so the sword's mainhand slot was genuinely empty;
(2) none of the four skills played any sound and `DarkWaveEffect`'s particle burst was a sparse
single-tick outline, both fixed with real audio cues and denser/taller particle effects; (3) the
model geometry itself was unmodified vanilla biped proportions plus two small horns, fixed with
a broader chest/arms, much longer horns, a bigger cape, and clawed fingertips, followed by
`graphics-designer` regenerating both textures to match and read as far more clearly demonic/
cursed within the same already-cleared palette.

Earlier, on `fischey_workbranch` (now merged): **added Drevathis, the Cursed Sovereign** — this
project's fifth boss and current top tier (level 40, above Zombie Colossus's 25), and its first
boss with no normal melee attack at all. See "Current state" above under "Drevathis, the Cursed
Sovereign" for the complete skill kit, the naming journey (three of the user's own name picks
rejected by `ip-naming-compliance-checker` before "Drevathis" cleared), and two real bugs
`balance-reviewer` caught and got fixed before this shipped: the sword's on-hit proc originally
aimed at the wielder's look direction and only ever hit other players (never the mob you actually
hit) due to a `PlayerEntity`-only target filter; and a kiting dead-zone where 3 of the 4 skills
plus the passive aura were all range-limited to 20/12 blocks with nothing ever moving the boss
toward a distant target, fixed with a new lowest-priority `ApproachGoal`. Also researched and
documented a genuinely new API surface for this codebase — 1.21.11's held-item rendering
pipeline (`docs/fabric-modding.md`'s "Custom two-handed held-item rendering" section) — needed
so the boss could visibly wield an oversized, two-handed version of its own drop.
`graphics-designer` produced a new "Abyssal Sovereign" palette (cool slate-gray flesh, wine-plum
robes, icy-cyan "Grave Frost" glow) plus placeholder entity/item textures, checked hex-by-hex
against every existing palette for collisions.

Earlier, on `fischey_workbranch` (now merged): **playtest fix round on Zombie Colossus.** The
user actually summoned and fought the boss and reported four things: the leap/fire-wave combo
"is perfect"; attack/jump animations were missing and the model looked static; the model "has no
muscles"; and the attack range was too short. Root causes, verified against the real decompiled
1.21.11 client jar rather than guessed: the walk-cycle/attack-swing plumbing was already correct
in v1, but vanilla's `MobEntity.isAttacking()` is only ever set by vanilla's own attack-goal
machinery, which this boss's fully custom attack `Goal`s never call — fixed client-side only.
Neither the leap nor the rage combo had a telegraph pose at all — added a new
`ColossusRenderState` (two synced wind-up counters) driving real crouch-coil/overhead-raise
poses. The "no muscles" complaint was fixed by replacing the reused, unmodified
`BipedEntityModel.getModelData()` geometry with bespoke bulkier cuboids plus a reshaded texture.
The short-range complaint was a real gameplay-logic issue (`Entity.distanceTo()` is
center-to-center, not edge-to-edge, so 2.0 against a 1.8-block-wide giant with a long club looked
disconnected) — widened to 4.5 across base attack, rage attack, and the leap's minimum trigger
range, kept in lockstep to preserve balance-reviewer's earlier dead-zone fix.

Earlier, still on `jonas_workbranch`: **Rissobelisk**: the mod's first custom `Block` and first
World-Event, closing `MASTERPROMPT.md`'s last remaining Priority 1 item — full detail in
"Current state" above under
"Rissobelisk". Planned via Plan Mode after checking the project's own roadmap priorities;
`fabric-docs-researcher` dispatched first since no `Block`/`BlockEntity` pattern existed in this
codebase (findings in `docs/fabric-modding.md`); implementation built clean on the first
attempt. Dispatched `graphics-designer` (visuals), `ip-naming-compliance-checker` (names, both
cleared, one non-blocking observation logged), and `balance-reviewer` (numbers, no bugs, several
design tensions logged) in parallel. **User's own in-game test caught 2 real bugs the review
agents didn't** — a completely missing item icon (a scoping miss: the first `graphics-designer`
dispatch only covered the block, not its drop) and a missing
`useBlockPrefixedTranslationKey()` call (vanilla's own established pattern, already documented
in this session's own research findings, just not applied) — both fixed and build-verified, not
yet re-confirmed live.

Earlier, still on `jonas_workbranch`: **Visual/Art Pass**: fast-forwarded `master` to match
`jonas_workbranch` (pure fast-forward,
zero conflicts — `master` had no commits `jonas_workbranch` didn't already have from this
session's earlier merges), then did a visual/art pass per the user's "plan the next steps"
request. Dispatched `graphics-designer` for 8 new sub-spec icons (`assets/baum2/textures/gui/
subspec/*.png`) and firm resolutions to 2 long-open palette questions (both doc-only, no
Java/hex changes) — full detail in "Current state" above under "Visual/Art Pass". **Verified
the agent's own deliverable manually** (its completion note flagged the safety classifier was
unavailable during review) via a pixel-level diff confirming genuine per-icon uniqueness, not a
fabrication. Wired the icons into `SubspecCardWidget` myself (asset/doc work is
`graphics-designer`'s job, Java UI wiring is not) via a new `ui/SubspecIcons.java`.

Earlier, still on `jonas_workbranch`: **added sub-specs and spells to the Class tab GUI**,
immediate follow-up to Class Overhaul v2
after the user confirmed it worked in-game and asked for sub-classes/spells to be wired into
the 'C'-key Class tab rather than staying command-only — full detail in "Current state" above
under "Class Overhaul v2 follow-up". New `SubspecSelectPayload` C2S packet; 2 mutable sub-spec
cards + 2 mutable spell cards in `CharacterStatsScreen`'s `ClassTab`, repointed at whichever
definitions belong to the currently selected class; spell cards cast via the same
`CastSpellPayload` the V/B keybinds already send. `ip-naming-compliance-checker` cleared the
new UI strings (generic chrome text, no new names). **User-confirmed working in-game**
("works").

Earlier, still on `jonas_workbranch`: **implemented "Class Overhaul v2"** (spell scaling, Mana
costs, sub-spec spell forks, respec cooldowns) in response to direct feedback that the classes
felt "lame" — full detail in "Current state" above under "Class Overhaul v2". Planned via Plan
Mode (4 independently buildable steps, approved before implementation), built and verified
(`./gradlew build`) after each step. Ran `balance-reviewer` on the final numbers, which found
and both fixes were applied before this commit: Glücksrune's cooldown-skip fork was dead code
(a cooldown-record ordering bug in `SpellCaster.attemptCast` overwrote it every time), and
respec cooldowns were trivially bypassable via a singleplayer world restart (switched from an
in-memory tick-based tracker to persistent wall-clock Attachments). **User-confirmed working
in-game** ("worked fine").

Earlier, still on `jonas_workbranch`: **merged `origin/master` into `jonas_workbranch` a second
time this session.** Master had
moved again since the first merge — 8 new commits from Fischey adding this project's first
`combat/` package (wiring the previously-display-only Physical Attack/Attack Speed/Crit Chance
into real vanilla combat via persistent attribute modifiers + a crit-roll Mixin), a mob
nameplate HUD, and four new mini-boss mobs with custom models/renderers/AI/drops (Stone of
Spiders, Stone of Zombies, Spider Queen, Zombie Colossus) — while `jonas_workbranch` had just
added the Skill System + Class Sub-specializations. Used `merge-integration-reviewer`
proactively per `CLAUDE.md`'s rule, including a real trial merge in a disposable worktree.

Conflicts resolved:
- **`Baum2Client.java`** — the one conflict where naively "accepting both sides" would not
  compile: master's tree still referenced the now-deleted `ClassScreen`/`PlayerStatusHud`
  (its branch never touched that code path). Resolved by dropping those stale references
  entirely and keeping both sides' actual new registrations (jonas's `SpellCastKeyBindings`,
  master's `MobNameplateHud` + four `EntityModelLayerRegistry`/`EntityRendererFactories`
  blocks for the new mobs).
- **`en_us.json`** — dropped the two lang keys orphaned by the same `ClassScreen` deletion
  (`key.category.baum2.main`, `key.baum2.class_screen`), kept every other key from both sides.
- **`Baum2Networking.java`** — trivial, adjacent new-import-line conflict only; both branches'
  actual additions (jonas's `CastSpellPayload` registration/receiver, master's
  `VitalsManager.applyBaseAttack/applyAttackSpeed` calls inside the existing attribute-spend
  receiver) had already landed on non-overlapping lines and merged cleanly on their own.
- **`docs/fabric-modding.md`** — both branches appended a new section at the same insertion
  point (jonas's "Combat / Skill effects", master's "Target nameplate / health-bar HUD") — no
  actual content overlap, kept both.
- **`HANDOFF.md`** — rewritten fresh from both sides' state (this file). Two things corrected
  while doing so, not just merged verbatim: master's own text said the `ClassScreen`/
  `CharacterStatsScreen` question "needs a product decision from the contributors" — jonas's
  branch had already unilaterally resolved it by deleting `ClassScreen`, which is now flagged
  explicitly in "Custom UI v1" above as a heads-up for Fischey rather than silently overwritten;
  and jonas's own prior claim "no `combat/` package exists" is now false and was removed.
- `CharacterStatsScreen.java` and `docs/visual-style-guide.md` merged with **zero conflict
  markers** — non-overlapping regions of the same file in the first case (master's "+1" button
  polish never touched the new "Class" tab code), and `docs/visual-style-guide.md` specifically
  because `jonas_workbranch` never touched it since the last merge, so master's ~854-line
  mob/item-palette addition applied as a clean, if large, addition.

**New design questions surfaced by this merge, logged in "Current state" above rather than
silently resolved**: (1) the Poison Dagger's on-hit-poison handler doesn't check for a melee
swing, so it also triggers off the Skill System's three damage spells — an untested emergent
interaction; (2) now that master has a real (if narrow) combat pipeline and jonas's spells deal
flat, unscaled damage, both branches are independently raising "should spell damage eventually
route through a shared calculation with real combat" — worth aligning on deliberately before a
third damage-dealing system appears, rather than each branch guessing separately.

Verified: `./gradlew build` passes after all resolutions.

Earlier, still on `jonas_workbranch` this session: added Skill System v1 (8 spells) and Class
Sub-specializations v1 (8 sub-specs), merged the standalone Class Screen into
`CharacterStatsScreen` as a new tab, removed `PlayerStatusHud`, added V/B keybind casting. Full
detail folded into "Current state" above under "Skill System v1", "Class Sub-specializations
v1", and "Custom UI v1" — see `git log -p HANDOFF.md` for the original commit narrative
(naming-compliance renames, balance fixes, a self-found cooldown-overflow bug) if needed.

Earlier, still on `jonas_workbranch`: merged `origin/master` into `jonas_workbranch` for the
first time this session (Fischey's Vitals/Attribute/Character-Stats/persistence work + jonas's
own Class System/Custom-UI work) — full detail folded into "Current state" above, original
merge-conflict narrative in `git log -p HANDOFF.md` if needed.

Earlier, on `fischey_workbranch` (now merged): added Zombie Colossus, Spider Queen (plus two
playtest-driven follow-up fix rounds), Stone of Zombies, Stone of Spiders, Combat System v1,
the mob nameplate HUD, and the underlying Vitals/Attribute/Character-Stats/persistence systems
before that. Full detail folded into "Current state" above under each system's own heading —
see `git log -p HANDOFF.md` for the original blow-by-blow narrative (extensive root-cause
analysis on nearly every fix) if needed.

See `git log -p HANDOFF.md` for the full detail on all earlier revisions.

## Networking API reference for this exact version (Fabric 0.141.4+1.21.11 / Yarn 1.21.11+build.6)

Found by decompiling the actual mapped jars in `~/.gradle/caches/fabric-loom/minecraftMaven/`
and the fabric-api jar — worth keeping here since it's easy to reach for the wrong API name
(most online examples/docs use different mapping conventions, e.g. NeoForge/Mojmap names):

| Concept | Wrong name (don't use) | Correct Yarn 1.21.11 name |
|---|---|---|
| Packet codec type | `StreamCodec` | `net.minecraft.network.codec.PacketCodec` |
| Composing a codec from fields | `StreamCodec.composite(...)` | `PacketCodec.tuple(...)` |
| Registry-aware buffer | `RegistryFriendlyByteBuf` | `net.minecraft.network.RegistryByteBuf` |
| Fabric's older packet wrapper | `FabricPacket` / `PacketType` | doesn't exist in this version — use vanilla `CustomPayload` + `PayloadTypeRegistry` directly |
| Setting server player's level | (works fine) `ServerPlayerEntity.setExperienceLevel(int)` | only exists on `ServerPlayerEntity`, not common `PlayerEntity` |
| Setting client player's level/progress | `ClientPlayerEntity.setExperienceLevel(int)` — **does not exist, will not compile** | `ClientPlayerEntity.setExperience(float progress, int totalExperience, int level)` |

Registration pattern that actually compiles:
- `PayloadTypeRegistry.playS2C().register(MyPayload.TYPE, MyPayload.CODEC)` (or `.playC2S()`
  for the other direction) — call from common code (`Baum2.onInitialize`), works for both
  logical sides since `splitEnvironmentSourceSets()` puts `main` on `client`'s classpath.
- Server sends: `ServerPlayNetworking.send(serverPlayerEntity, payload)`. Client sends:
  `ClientPlayNetworking.send(payload)`.
- Client receives: `ClientPlayNetworking.registerGlobalReceiver(MyPayload.TYPE, (payload, context) -> {...})`
  — runs on the client thread, no extra `execute()` wrapping needed. Server receives:
  `ServerPlayNetworking.registerGlobalReceiver(MyPayload.TYPE, (payload, context) -> {...})`
  — `context.player()` gives the sending `ServerPlayerEntity`.

If you add more custom payloads, follow `ExperienceSyncPayload.java` (S2C) or
`ClassSelectPayload.java` / `SpendAttributePointPayload.java` / `CastSpellPayload.java` (C2S)
as templates.

**Combat/skill-effect APIs** (dealing damage, AoE entity queries, knockback, status effects,
healing, dashing — used by the Skill System's `SpellEffects.java`) are documented separately
in `docs/fabric-modding.md`'s "Combat / Skill effects" section, not duplicated here. The one
gotcha worth calling out at this level since it's easy to reintroduce: pushing a **player**
target with `takeKnockback(...)`/`addVelocity(...)` alone silently never reaches that player's
own client (only nearby *observers* see them fly back) — you must additionally send `new
EntityVelocityUpdateS2CPacket(targetPlayer)` to `targetPlayer.networkHandler` yourself.
`SpellEffects.pushAwayFrom(...)` already does this; reuse it rather than calling
`takeKnockback` directly for any future spell/effect.

## Attachment API reference (persistent per-player/entity data)

For any future "store custom data on a player/entity/block-entity/chunk that should survive
restarts" need, use `fabric-data-attachment-api-v1` (already a dependency) rather than a
hand-rolled `HashMap<UUID, ...>` + manual save/load. Reference implementations:
`progression/PlayerLevelSystem.java` (`PROGRESSION` field) + `progression/PlayerProgressData.java`
(`CODEC`), and `classes/ClassManager.java` (`SELECTED_CLASS`/`SELECTED_SUBSPEC`, including
client sync via `.syncWith(...)`).

**Critical gotcha, learned the hard way (cost a full debugging cycle):** the class holding
your `AttachmentType` static field must be force-loaded during `Baum2.onInitialize()`, via an
explicit call to some no-op method on it (see `PlayerLevelSystem.bootstrap()`). If the class
is only ever referenced from inside an event callback *body* (a lambda registered at init time
but not executed until later), Java's lazy class initialization means the `AttachmentType`
won't actually be registered until the first time a player triggers that callback — by which
point Fabric may have already tried and failed to deserialize that player's persisted
attachment data (silently), permanently losing it on the next save. Symptom: progress "resets"
but only sometimes, and looks exactly like a persistence failure even though writes are
working fine. `classes/ClassManager` avoids this incidentally (its `registerEvents()` is
called directly and unconditionally from `Baum2.onInitialize()`, which forces its static
fields to initialize as a side effect of Java's class-init rules) but doesn't document this
the way `PlayerLevelSystem.bootstrap()`'s Javadoc does — worth a defensive comment if
`registerEvents()` is ever refactored to be lazy.

**Second gotcha, also cost a real debugging cycle:** adding a new field to an already-
`persistent()` attachment Codec must use `Codec.optionalFieldOf(name, default)`, never
`fieldOf(name)` — `RecordCodecBuilder`'s `instance.group(...)` fails to decode the **entire**
record if any required field is missing, so a single new mandatory field silently discards
every other field too for any save predating that field's introduction (confirmed: a
level-45 test character was reset to level 1 by exactly this bug before the fix). All of
`PlayerProgressData`'s Mana/attribute fields use `optionalFieldOf` for this reason.

Key classes, all in `net.fabricmc.fabric.api.attachment.v1` (stable across mapping sets):
- `AttachmentType<A>` — the "key" for a piece of attached data.
- `AttachmentRegistry.create(Identifier, Consumer<Builder<A>>)` — **not**
  `AttachmentRegistry.builder()` (that's `@Deprecated`, confirmed via `javap -v`).
- Builder methods: `.persistent(Codec<A>)` (save/load), `.copyOnDeath()` (survives death/
  respawn), `.initializer(Supplier<A>)` (default for entities that never had it set),
  `.syncWith(PacketCodec<? super RegistryByteBuf, A>, AttachmentSyncPredicate)` (pushes the
  value to the client automatically on change — `AttachmentSyncPredicate.targetOnly()` for
  "only the owning player needs to know").
- `AttachmentTarget` — implemented by `Entity`/`ServerPlayerEntity`/`BlockEntity`/`Chunk`/
  `World` via Fabric's build-time interface injection. Methods: `getAttached(type)`,
  `getAttachedOrCreate(type)`, `setAttached(type, value)`, `removeAttached(type)`.

The `.persistent(Codec<A>)` codec is `com.mojang.serialization.Codec<A>` (Mojang's
DataFixerUpper library) — **not** the `PacketCodec` used for networking above; unrelated codec
systems despite the similar name. Build one with `RecordCodecBuilder.create(...)`.

Persisted attachment data is stored as part of the target's own save data (e.g. a player's
`playerdata/<uuid>.dat`), written/read automatically by vanilla's existing save cycle — no
manual `JOIN`/`DISCONNECT` save/load hooks needed for persistence itself.

## Decisions worth knowing about

- Minecraft 26.2 is the actual latest stable release, but Yarn has not published mappings for
  it yet (confirmed against `meta.fabricmc.net`). We target 1.21.11 instead — the newest
  version with full Yarn + Fabric API support. Bump `minecraft_version`, `yarn_mappings`, and
  `fabric_version` in `gradle.properties` together once 26.2 mappings exist.
- Fabric Loom pinned to stable `1.17.13` (the generated template shipped a floating
  `1.17-SNAPSHOT`, which is unsafe to keep).
- Gradle wrapper bumped from 9.2.1 to 9.5.1 — Loom 1.17.13 requires Gradle's plugin
  `api-version` >= 9.5.0, so 9.2.1 cannot resolve it.
- Java target set to 21 (matches what Minecraft 1.21.11 needs). Some contributor machines have
  other JDKs installed (e.g. a JDK 25 under an IDE-managed `.jdks` folder) — the toolchain pin
  in `build.gradle` is now unconditional specifically to defend against those being picked up
  by accident.
- A local, gitignored SSH-style keypair (`baum2_key`, `baum2_key.pub`) has appeared in some
  working copies of this repo. It has never been committed. If you don't know what it's for,
  don't commit it and ask before deleting it — another contributor may depend on it locally.
- Our XP curve is centralized in `ProgressionCurve` (originally `VanillaXpFormula`, renamed
  once it stopped actually being vanilla's formula) — **do not** reimplement it elsewhere. The
  numbers are also deliberately rebalanced away from vanilla's own for this mod's lump-sum
  reward economy — see "Current state" above for the current formula and why.
- Vanilla XP orb drops from hostile mobs are disabled via `LivingEntityMixin`. Experience
  bottles were reported to still spawn orbs in an earlier session — not re-verified since; low
  priority.
- Progression persistence uses Fabric's Data Attachment API — **do not** reintroduce a manual
  `HashMap<UUID, ...>`, that was the previous approach and it's why data was lost on restart.
- Fischey's dev environment pins a fixed dev username/UUID (`Baum2Dev`) in `build.gradle`'s
  `loom { runs { client { ... } } }` block, since Loom otherwise assigns a fresh random
  username (and therefore UUID) on every `runClient` launch — without this, per-player
  persistence looks broken in dev even when it isn't (every launch is a "new" player). Has no
  effect on a real server/launcher session.

## Next recommended step

**New (2026-07-11, latest): playtest the GeckoLib Sword Template v1** — checklist at the end
of its section above. Natural to combine with the mount playtest below (the cavalry-sweep
animation needs a mount anyway). The two things a build can't prove: the drop-guard mixin
applying on first join, and how the sword sits in hand (display transforms may need one nudge
pass — the exact numbers live in `assets/baum2/models/item/espenklinge_base.json`, tweakable
without touching Java or the generator).

**New (2026-07-11): playtest the Mount System v1** — the full checklist is at the end of the
"Mount System v1" section above; it's unplayed code with GeckoLib renderers, so treat the
first `/give` + Ctrl+H as the real verification, not the passing build. Also decide the two
logged mount-balance judgment calls (summon cooldown or not; scope Poison Dagger's handler to
melee-only, which would fix its spell interaction AND the new AOE-splash interaction at once).

**Highest priority: first real client session in Heimgrund** (everything below was verified
headlessly via dedicated-server console only — no human has walked this world yet). Create a
NEW world (datapack dimensions bake at creation), then: confirm first join lands at the
village plaza (0/65/0) and `/kill` respawns there; walk the rings — clearing safe, silverfish
in the meadow by day, zombies+silverfish in the desert patches (they must NOT burn in
daylight), spiders/cave spiders on the mountain ramp; try to break/place blocks in survival
(must fail everywhere) and as a creative op (must work); check the mountain wall is actually
unclimbable at the cliff band and the border stops flight past r=500; find a cave mouth on
the inner ring face; use `/baum2 stones list` (op) to walk to a stone slot, watch the stone
appear as you approach, kill it, confirm the 5-minute respawn at the same spot; sanity-check
spawn DENSITY (daylight spawning doubles spawn-eligible time — weights may need tuning after
real play).

**Heimgrund decisions needed (new `balance-reviewer` findings, logged not fixed)**:
(a) **High — the previously-logged "deterministic no-cooldown farm" risk is now LIVE, and the
21-slot density pass made it resonant**: stone slots respawn unconditionally every 5 min at
fixed positions with guaranteed drops (Poison Dagger, Gold Sword, iron nuggets, cobwebs/eyes
for the 4 low-tier stones; totem/netherite-scrap/shulker-shell stones will inherit this the
moment they get slots). `balance-reviewer` simulated the new scatter geometry: a sprint loop
of the meadow (7 slots) or desert (6 slots) ring now takes ~299-301s — near-exact resonance
with the 300s respawn timer, so a circling player meets every stone freshly respawned with
zero idle wait (~1,000 iron nuggets/hour meadow; an L20 zombie-stone kill every ~50s desert;
mountain is off-resonance at ~7.2 min/lap). The old logged decision — chance-based drops vs.
longer/RANDOMIZED respawn (randomizing also breaks the resonance) — must be made now, not
later.
(b) Spider stones (L10) always sit FARTHER out than zombie stones (L20) because spiders live
in the mountain ring per the user's own terrain theming — accept the terrain-over-level
inversion or retune levels (e.g. swap spider/zombie stone levels).
(c) Silverfish (L5) and zombie (L20) slot ranges overlap across r 180-380 and the sampler
skews outward, so "weakest nearest" is not guaranteed within the meadow band — the seed is
fixed, so check the actual 11 generated positions once and, if needed, constrain silverfish
slots to r < 250.
(d) Rissobelisk cannot be placed by survival players inside Heimgrund (whole-dimension
protection) — decide how world-event blocks should work there (op-placed only? a protection
whitelist? a consecrated plot?).

**Also still pending, build-verified only, not yet re-confirmed live**: `/give @s
baum2:rissobelisk`, confirm the item's name shows as "Rissobelisk" (not the raw
`item.baum2.rissobelisk` key), place it, attack it to destroy, and confirm the dropped
`Risssplitter` shows its new icon (not a missing-texture placeholder) — these are the exact 2
bugs the user's own last playtest caught and a previous session fixed. (In Heimgrund this now
requires creative/op due to the protection — see decision (d) above.) Also worth a full
mechanic re-check while there: waves spawn at each 10% threshold, normal mining/explosions
can't destroy it, XP is granted on destruction.

0. **Done**: the 8 sub-spec icons (including the subtle Schattenpirscher/Sturmklinge pair) are
   user-confirmed rendering correctly, alongside Class Overhaul v2 + its GUI follow-up. Kept
   the itemized checklist below only as a reference in case a specific sub-item needs closer
   verification later, not as an open task: cast each of the 8 spells and confirm Mana drops by
   its listed cost and a
   rejection message appears at 0 Mana; invest attribute points and confirm scaled spells hit
   harder / utility spells last longer; select each of the 8 sub-specs in turn (now doable via
   the Class tab GUI, not just the command) and confirm its specific forked behavior fires
   (especially **Glücksrune** — confirm Runenfunke's cooldown skips roughly 1 in 5 casts now,
   not every time or never); try to respec twice in a row and confirm the second is rejected
   with a remaining-time message, then fully quit and relaunch the client and confirm the
   cooldown message still shows correctly rather than resetting (the exact bypass that was
   fixed). Also confirm spell cards in the GUI actually cast (not just visually present).
1. **Balance decision needed, now spanning three separate findings, not yet made**: Combat
   System v1's base ~46x baseline-DPS-at-max-investment issue has been escalated further by
   two consecutive boss-weapon drops — Gold Sword (+12.5%) and Poison Dagger (+145%, pushing
   the effective ceiling to ~110x) — because both lean on attack speed, and Dexterity's
   Attack-Speed modifier multiplies (not adds to) whatever speed a weapon already has. Worth
   deciding before a third weapon repeats the pattern: cap/temper how much a single item's
   attack speed can contribute at high Dexterity investment, or accept this as the mod's
   intended "gear matters a lot" power curve. The Colossal Warclub (low speed) is a useful
   counter-example that this isn't unavoidable — see "Zombie Colossus" above.
2. **In-game verification of all bosses** (none have been played, only built) — see each
   mob's own section above for its exact playtest checklist: `/summon baum2:spider_queen`,
   `baum2:zombie_colossus`, `baum2:drevathis`, and the stone family.
   **All 33 stones render via the GeckoLib fallen-comet-stone template (2026-07-09,
   preview-verified only)** — spot-check several across tiers (e.g. `/summon
   baum2:stone_of_silverfish`, `baum2:stone_of_creepers`, `baum2:stone_of_blazes`,
   `baum2:stone_of_ravagers` plus the original two): the tilted comet + crater renders at all
   (a render-state mistake crashes only at render time), the three shards orbit smoothly,
   each stone shows its own palette, waves aggro the attacker immediately (including
   Endermen/Zombified Piglins), Creeper explosions and Blaze fire do NOT damage their own
   stone, and Piglin/Hoglin waves don't zombify in the overworld.
   Also **confirm combat actually feels right** on any mob — attack something and check
   damage/attack-speed/crits are happening, not just that nothing crashes — and confirm the new
   "Class" tab in `CharacterStatsScreen` and V/B spell-cast keybinds still work correctly
   alongside all the new mob-rendering registrations. **For Drevathis specifically**: confirm
   all four skills fire and look distinct (Dash's teleport+launch, Chain's slow + sustained
   chain-hold pose, Wave's telegraph + rectangular hit, Thunder's 3-second multi-strike), the
   Darkness aura toggles on proximity, the boss visibly wields an oversized two-handed sword
   (the riskiest untested piece — `DrevathisHeldWeaponFeatureRenderer`'s exact scale/position was
   written against decompiled API research, not tuned against a live render), the Dash launch
   height is roughly the intended ~10 blocks (a flagged approximation), and the Cursed Blade's
   on-hit proc now actually damages the *mob* you hit (the bug `balance-reviewer` found and this
   session fixed).
3. **Design questions logged, not yet decided**: (a) does the Poison Dagger's on-hit-poison
   handler need a melee-only guard so it doesn't also trigger on spell damage; (b) should spell
   damage eventually get crits too, now that it scales with attributes; (c) Mana currently only
   creates real scarcity for 3 of the 8 spells, and only up to roughly level 20-30 out of 100 -
   is "matters early, cosmetic later" the intended pacing, or should `getMaxMana`'s slope/spell
   costs be retuned; (d) Wesenswahrer's 2 spells are both Mana-sustainable from level 1, unlike
   every other class's - intentional or an asymmetry worth closing; (e) 3 of the 4 damage-spell
   sub-spec forks are uncapped-AoE while melee is single-target, so "spells stay below melee's
   ceiling" only holds per-target, not for total output against a large group.
4. **Fine-grained `/attribute` verification of the Class System** (older, still-pending item):
   `/baum2 class select eisenwaechter` → `/attribute @s minecraft:max_health modifier value
   get baum2:class_bonus/eisenwaechter_max_health` (expect `4.0`) → `/attribute @s
   minecraft:max_health get` (expect `24.0`) → disconnect/rejoin → `/kill @s` and respawn
   (confirm `.copyOnDeath()` holds). Spot-check `wesenswahrer`'s
   `minecraft:generic.knockback_resistance` (expect `0.1`).
5. **Fresh `balance-reviewer` pass on the new `ProgressionCurve` + attribute + combat system
   together** — the old XP curve was reviewed pre-combat, the attribute formulas were reviewed
   pre-combat, and combat itself has now been reviewed in isolation per weapon — nobody has
   looked at all three together as one system.
6. **A conversation with Fischey about the unilateral `ClassScreen`/`CharacterStatsScreen`
   merge** (see "Custom UI v1" above) — not urgent, but a previously-logged "needs a joint
   decision" item was resolved without one.
7. **Done**: the visual-style-guide's palette-unification question and the "one bespoke palette
   per mob/item" pattern are both resolved — see "Visual/Art Pass" above.
8. **Done**: sub-spec selection and spell casting now have a GUI (Class tab), and sub-spec cards
   now have icon art too (see "Class Overhaul v2 follow-up" and "Visual/Art Pass" above).
   Remaining gap: spell cards show static cooldown length, not live time-remaining (no
   client-side cooldown-sync payload exists yet).
9. **Partially done via Heimgrund**: the 4 low-tier stones (silverfish/spiders/zombies/cave
   spiders) now spawn naturally through `StoneSlotManager`'s slot system — the pattern to
   extend to higher-tier stones as the world grows (more rings/zones, or future dungeons).
   The named bosses (Spider Queen, Zombie Colossus, Drevathis) still have no natural spawn
   path (`/summon`-only).
10. Get real (non-placeholder) art for the 4 class icons, 8 sub-spec icons, and the four
    mini-bosses/items — see `docs/visual-style-guide.md` sections 9/9.1 and each mob's own
    section above. Confirmed this session: this needs an actual human artist or an external
    image-generation tool — `graphics-designer` cannot produce it itself (text-based agent, no
    image-generation capability).
11. **Done**: every Priority 1 item per `MASTERPROMPT.md` is now complete (first world-event
    block was the last one — see "Rissobelisk" above). Priority 2 is also mostly done
    (Klassen-System, Skill-Auswahl, bessere Persistenz); remaining Priority 2 items:
    Gegner-Spawns (natural spawn paths — see item 9 above), Loot-System (generalized, beyond
    each boss's own hardcoded single-item drop), Upgrade-Materialien (a real system to spend
    `Risssplitter`/future materials on — none exists yet). Priority 3
    (Dungeons/Fraktionen/Quests/Bossmechaniken/Balancing/Konfiguration) is next after that.
12. A second `MASTERPROMPT.md` world-event block (Chaosmonolith, Sternsplitter, Verderbter
    Altar, Echosäule, or Sturmsiegel — the other 5 pre-listed examples) would now be fast to
    build: `docs/fabric-modding.md`'s new "Custom Blocks and BlockEntitys" section has every API
    answer already researched, and `RissobeliskBlockEntity`'s wave-spawn/HP-threshold logic is
    generic enough to likely be reusable rather than re-ported from `StoneOfSpidersEntity` again.
13. The `ip-naming-compliance-checker`'s secondary observation on Rissobelisk, re-surfaced again
    in the post-merge ensemble pass (see "Rissobelisk" above) — the wave-spawning-stone
    mechanic itself now underlies 3-4 mini-bosses/blocks and reads as conceptually close to
    Metin2's core "Metin Stone" mechanic, even though each individual case has been cleared as
    "genre archetype, not a specific match." Worth a deliberate human decision on whether that
    pattern should keep being reused a 5th time, now that it's this project's single
    most-repeated mechanic shape. **Concrete alternative direction if the answer is "don't reuse
    it again unchanged"** (from the ensemble reviewer, not yet implemented): decouple the next
    such content's wave-spawns from "percent of max-HP lost so far" and drive them off something
    structurally different instead — a fixed/random timer independent of damage taken, a
    shrinking-safe-radius/corruption-spread effect, or requiring players to interrupt a periodic
    self-heal pulse rather than "chip HP down through tiers, each tier releases monsters."
