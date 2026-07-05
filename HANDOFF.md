# Handoff

Read this first. It reflects the state as of the latest commit so a fresh Claude Code
session (yours or a co-author's) can pick up work without re-deriving context from
`git log`. Updated after every commit — see "Git Rules" in `CLAUDE.md`.

## Current state

This reflects `jonas_workbranch` (fast-forwarded into `master` too, both branches at the same
commit) after merging `origin/master` twice this session (Fischey's Vitals/Attribute/
Character-Stats work, then his Combat System v1 + four mini-boss mobs), adding a Skill System +
Class Sub-specializations on top, then a "Class Overhaul v2" pass (spell scaling, Mana costs,
sub-spec spell forks, respec cooldowns), a same-session GUI follow-up (sub-specs and spells
added to the Class tab), and a Visual/Art Pass (8 sub-spec icons, two long-open palette
decisions resolved) — see "Last change" below for detail on the merges and each feature pass.

- Fabric mod builds successfully (`./gradlew build` passes).
- Client runs: `./gradlew runClient` loads, reaches the main menu, and joins a world cleanly
  (verified clean boot 4 times this session, no Mixin/payload/HUD-registration
  errors/exceptions in the log). **Both the Class Overhaul v2 feature set (spell scaling, Mana
  costs, sub-spec forks, respec cooldowns) and its GUI follow-up (Class tab sub-spec/spell
  cards) are user-confirmed working in-game** ("worked fine" / "works"). Not itemized back
  against every line of the original playtest checklist (see "Next recommended step" item 0,
  kept as a reference in case any specific sub-item still needs a closer look), so treat this
  as "no reported breakage from real play," not an exhaustive per-mechanic sign-off. **The
  Visual/Art Pass's 8 sub-spec icons are now also user-confirmed rendering correctly in-game**,
  including the subtlest pair (Schattenpirscher vs. Sturmklinge).
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
  geometry — only the texture differs. Any future stone-shaped mini-boss should follow this
  pattern.
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

## Last change (on `jonas_workbranch`, fast-forwarded into `master`)

**Visual/Art Pass**: fast-forwarded `master` to match `jonas_workbranch` (pure fast-forward,
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
2. **In-game verification of all four new mini-bosses** (none have been played, only built) —
   see each mob's own section above for its exact playtest checklist:
   `/summon baum2:stone_of_spiders`, `baum2:stone_of_zombies`, `baum2:spider_queen`,
   `baum2:zombie_colossus`. Also **confirm combat actually feels right** on any mob — attack
   something and check damage/attack-speed/crits are happening, not just that nothing crashes
   — and confirm the new "Class" tab in `CharacterStatsScreen` and V/B spell-cast keybinds
   still work correctly alongside all the new mob-rendering registrations.
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
9. No natural spawn path exists for any of the four mini-bosses yet (`/summon`-only) — decide
   when/how this mob family should actually appear in the world (a structure? a `dungeons/`-
   package encounter? natural biome spawn?).
10. Get real (non-placeholder) art for the 4 class icons, 8 sub-spec icons, and the four
    mini-bosses/items — see `docs/visual-style-guide.md` sections 9/9.1 and each mob's own
    section above. Confirmed this session: this needs an actual human artist or an external
    image-generation tool — `graphics-designer` cannot produce it itself (text-based agent, no
    image-generation capability).
11. Remaining Priority 1 items per `CLAUDE.md`: first world-event block (item, weapon, and
    first active skill are now all done — see Stone of Spiders/Skill System v1 above). Consult
    `fabric-docs-researcher` / `docs/fabric-modding.md` before implementing if the relevant
    Fabric API is unclear. Use `graphics-designer` for the texture/model/icon it will need.
