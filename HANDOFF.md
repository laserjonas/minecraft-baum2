# Handoff

Read this first. It reflects the state as of the latest commit so a fresh Claude Code
session (yours or a co-author's) can pick up work without re-deriving context from
`git log`. Updated after every commit — see "Git Rules" in `CLAUDE.md`.

## Current state

This is the state after merging `origin/master` (Fischey's Vitals/Attribute/Character-Stats
work) into `jonas_workbranch` (Class System v1 + Custom UI v1) — see "Last change" below for
the merge itself.

- Fabric mod builds successfully (`./gradlew build` passes).
- Client runs: `./gradlew runClient` loads, reaches the main menu, and joins a world cleanly
  (verified clean boot, no Mixin/payload/HUD-registration errors, both pre- and post-merge).
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
  levels") is **substantially addressed by the curve rebalance above**, per
  `merge-integration-reviewer`'s check during this merge: a max single-kill reward (160 XP,
  Wither) against a level-1 character now causes exactly one level-up with 32 XP carried
  over, not a multi-level vault. Caveat: a single very large `/baum2 addxp` grant (700+) can
  still vault ~3 levels — addressed for the actual mob-kill case, not structurally eliminated
  for all lump-sum grants. **Worth a fresh `balance-reviewer` pass against the new curve
  combined with the attribute system below** (not done yet) rather than treating this as
  fully closed.
- Other balance-reviewer findings from before the rebalance, still relevant: `ExperienceManager
  .getMaxLevel()`'s 100-level cap is declared but not enforced anywhere; free/instant class
  reselection (see Class System below) lets a player capture all four classes' bonuses
  contextually; Runenwirker's luck bonus has no loot system yet to act on.

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
    "Combat System v1" below. Magic Attack/Magic Defence/Physical Defence remain display-only
    (no spell-casting or incoming-damage-reduction system exists yet).
- Networking: `networking/AttributeSyncPayload.java` (S2C, every tick, carries only the 4 raw
  ints + unspent points — derived stats are computed client-side from the same `VitalsCurve`
  methods, not synced separately). `networking/SpendAttributePointPayload.java` — **this
  project's first C2S payload** (`PayloadTypeRegistry.playC2S()` +
  `ServerPlayNetworking.registerGlobalReceiver`), sent when a Stats-screen "+1" button is
  clicked. Old `Base Damage`/`Base Magic Damage` flat fields and `CombatStatsSyncPayload` are
  gone, fully superseded by these formulas.
- UI: pressing **C** (`ui/Baum2KeyBindings.java`, `KeyBinding.Category.MISC`) opens
  `ui/CharacterStatsScreen.java` — a full menu `Screen` built on vanilla's real
  `Tab`/`TabManager`/`TabNavigationWidget`/`GridScreenTab` system, one tab ("Stats") with 15
  rows (Life, Mana, Unspent Points, then each attribute interleaved with its derived stats).
  Content wrapped in vanilla's `ScrollableLayoutWidget` (fixes a real bug where bottom rows
  were unreachable at high GUI Scale) and given an opaque panel background (fixes a real bug
  where vanilla's default darkening was too weak against a bright sky, and content overlapped
  the tab header — see git history for both root causes). Row order/colors in
  `docs/visual-style-guide.md` section 12.
- **"+1" attribute buttons polished per user feedback**: label shortened to "+", size reduced
  18x18 → 12x12. Clicking now updates the displayed attribute/points values **immediately**
  client-side (`ClientNetworkingHandler.predictAttributeSpend`), rather than waiting for the
  next `AttributeSyncPayload` tick — the server stays authoritative and corrects this within
  ~1 tick if the optimistic update was ever wrong (e.g. a race where points ran out), so there's
  no real desync risk, just a smoother-feeling button. When `Unspent Points` is 0, the buttons
  are now set `.visible = false` (fully hidden, not just greyed out via `.active`) — confirmed
  `ClickableWidget.visible` skips both rendering and click handling, not just one or the other.
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
pure display values, see "Attribute System" above). Wired all three into real vanilla combat
mechanics rather than building a parallel damage system:

- **Base Attack (Strength)**: `VitalsManager.applyBaseAttack` adds a persistent `ADD_VALUE`
  modifier (fixed `Identifier`, `overwritePersistentModifier` — same pattern as the Class
  System's own attribute bonuses) to the player's real `EntityAttributes.ATTACK_DAMAGE`. Stacks
  additively with whatever weapon they're holding (vanilla weapons add their own modifiers to
  the same attribute) rather than overriding it.
- **Attack Speed Multiplier (Dexterity)**: `VitalsManager.applyAttackSpeed` adds a persistent
  `ADD_MULTIPLIED_TOTAL` modifier to `EntityAttributes.ATTACK_SPEED`, value =
  `(multiplier - 1.0)` **not** the multiplier itself — confirmed via
  `EntityAttributeInstance.computeValue()`'s actual decompiled body that this operation computes
  `total *= 1.0 + value`, so a "+50%" bonus needs modifier value `0.5`.
- Both modifiers are **persistent** (survive relogin/restart on their own, part of the entity's
  own attribute-container NBT) — only re-applied on join and immediately after a successful
  attribute-point spend (`Baum2Networking`'s C2S receiver), **not every tick** like Life's
  `setBaseValue` approach needs, since modifiers don't need constant reapplication once set.
- **Crit Chance (Dexterity)**: new Mixin `mixin/PlayerAttackDamageMixin.java`. No Fabric API
  event can modify a melee attack's final damage float (`AttackEntityCallback` is cancel/allow
  only, fires before damage is computed; nothing else in `fabric-entity-events-v1`/
  `fabric-events-interaction-v0` fits) — confirmed by reading both packages' actual source, not
  assumed. Uses a plain Sponge `@ModifyArg` (not MixinExtras — no advantage here for a
  single-argument case) targeting the `Entity.sidedDamage(DamageSource, float)` call site inside
  `PlayerEntity.attack(Entity)`, rolls `Math.random()*100 < critChance` server-side only
  (guarded by `instanceof ServerPlayerEntity`), and multiplies the final damage by a flat
  `CRIT_DAMAGE_MULTIPLIER = 1.5f` — **a number I picked myself** (the user didn't specify one),
  anchored to vanilla's own fall-crit multiplier as a "no worse than what already exists"
  reference point.
- **`balance-reviewer` finding, real and not yet acted on — flagging for a human decision
  rather than silently changing already-approved formulas:** because Base Attack (linear),
  Attack Speed (linear → more attacks/sec), and Crit Chance (linear EV) all multiply together
  in actual combat, **total DPS grows superlinearly with investment** — computed: ~8x baseline
  DPS at 25 points invested in each of Strength/Dexterity, ~46x baseline at max level (99 points
  each). Concretely, an iron sword deals ~11 damage at start, ~36 at 30 Strength, ~110 at max
  Strength (104) — a zombie/skeleton/player has 20 HP, so **characters one-shot most vanilla
  mobs from roughly 25 invested Strength points onward, well before max level**. The underlying
  per-stat formulas were already reviewed/approved in an earlier session (for their own sake,
  before combat existed to consume them) — this compounding effect is a genuine new
  consequence of wiring them together, not something introduced by a formula change this
  session. Did not unilaterally rebalance them, since the user asked to make these stats "have
  an effect," not to redesign values that were already accepted — flagging clearly instead so a
  human can decide whether the power curve matches the intended feel (this project's XP curve
  was deliberately designed as a "hardcore grind" where reaching max level matters — a combat
  curve that trivializes fights by the mid-game may be in tension with that).
- **Also flagged, not fixed (minor, likely-intentional edge case)**: vanilla's own fall-based
  critical hit (1.5x) is already folded into the damage float by the time our Mixin runs, so a
  fall-attack crit + a successful Dexterity crit roll on the same swing stacks to `1.5 × 1.5 =
  2.25x`, uncapped. Not guarded against — reads as an acceptable rare-but-fun outcome given the
  two crit systems are explicitly independent by design, but wasn't an explicit decision anyone
  wrote down, so noting it here.
- Verified: `./gradlew build` passes, `runClient` boots cleanly with the new Mixin applied (no
  Mixin apply/target-resolution errors at launch — those show up at boot, not compile time).
  **Not verified in an actual fight** — same no-GUI-automation limitation as every UI change
  this session; the next person to actually attack a mob should confirm damage/speed/crits feel
  right, not just that nothing crashes.

### Target nameplate — mob name + level + health bar, top-center

Client-only HUD element, `ui/MobNameplateHud.java`, registered via
`HudElementRegistry.addLast(...)` (a new, independent element — not attached to or replacing
`VanillaHudElements.BOSS_BAR`). Shows the name, level, and a current/max health bar of a
targeted living entity.

- **Fixed a real bug, reported by the user**: attacking a `minecraft:spider` didn't show its
  name. Root cause — the original version only read live `MinecraftClient.crosshairTarget`
  each render frame; fast/erratic mobs (spiders climb walls, jump unpredictably) often aren't
  precisely under the crosshair by the time a frame samples it, even though the hit landed.
  Fixed by also registering `net.fabricmc.fabric.api.event.player.AttackEntityCallback`
  (confirmed via decompiled Fabric API source that this **does** fire client-side — it's
  Mixin'd into `ClientPlayerInteractionManager.attackEntity`, the real "attack packet about to
  send" moment, not a render-frame sample; it also fires server-side in singleplayer, so the
  listener is guarded with `world.isClient()` to avoid double-processing) and caching the
  attacked entity for 5 seconds. `resolveTarget()` now prefers this recently-attacked entity,
  falling back to live crosshair-target if nothing was attacked recently — so the nameplate
  shows reliably right when you land a hit, not just while precisely aimed.
- **Added "Lvl. X" after the name**, per user request. No mob-leveling system exists in this
  codebase yet, so `getMonsterLevelText()` currently always returns `"Lvl. 1"` for every
  entity — a single, clearly-marked placeholder method to extend once a real per-mob level
  concept exists, not a full system built prematurely.
- No networking needed — confirmed via decompiled `LivingEntity`/`DataTracker` source that
  `HEALTH` is an ordinary `TrackedData<Float>`, synced to every client tracking that entity
  (not just boss-bar-eligible ones), so `getHealth()`/`getMaxHealth()`/`getDisplayName()` are
  reliably readable client-side for any nearby entity.
- Positioned at vanilla's own boss-bar starting y (12px from top, confirmed via decompiled
  `BossBarHud.render()`), horizontally centered — not dynamically avoided if a real boss bar is
  also active (known simplification). `ui/PlayerStatusHud.java` sits top-left, no collision.
- Bar styling reuses the Life bar's exact ember/coral hexes (`#E2574B`/`#8E1F1F`) — "red =
  health" stays consistent whether it's the player's own Life bar or a targeted mob's. Shows
  both the bar and the literal `current / max` number (unlike the player's own HUD bars, which
  are bar-only per the style guide) since the user explicitly asked to see the actual number.
- **Second real bug, found immediately after the fix above**: user tested and reported "Monster
  name and level was not shown. No Crash." — i.e. the attack-trigger fix alone wasn't enough;
  text never rendered at all (the health bar, drawn via plain `fill()`, presumably still did,
  since only the two `drawText*`-based elements were reported missing). **Root cause: this
  project's own `docs/fabric-modding.md` already documented the exact bug** (found and written
  down in an earlier session, from the dead `ui/ProgressionHud.java`'s investigation) —
  `DrawContext`'s `drawText*` methods **silently no-op with no exception** if the passed
  color's alpha byte is `0`. Both of `MobNameplateHud`'s text calls passed `0xFFFFFF` (plain
  RGB, alpha byte `0x00`) instead of `0xFFFFFFFF` (opaque white) — the exact documented
  mistake, made fresh in new code despite being written down. Fixed by using
  `net.minecraft.util.Colors.WHITE` (a pre-built, already-correct constant vanilla's own
  `BossBarHud` uses for the same purpose) instead of a hand-typed hex literal, which both
  fixes this instance and removes the temptation to retype a bare hex value in future text
  calls in this file. **General lesson, worth internalizing rather than re-discovering a third
  time**: any raw `int` color passed directly as an argument to a `DrawContext.drawText*`
  method (not applied via `Text.styled(style -> style.withColor(...))`, which is a separate,
  unaffected code path already used safely elsewhere in this project, e.g.
  `CharacterStatsScreen`) must include a non-zero alpha byte — prefer `Colors.WHITE` or an
  explicit `0xFFxxxxxx` literal, never a bare 6-digit hex.
- Verified: builds, boots cleanly, played a short manual session with no crashes after both
  fixes. **Not yet re-confirmed against an actual spider with the alpha fix applied** — next
  person should attack one in-game and confirm the name and "Lvl. 1" both actually render this
  time, not just that nothing crashes.

### Class System v1 — 4 classes, command + GUI selection

- `classes/` package: `PlayerClass` (enum: `EISENWAECHTER`, `SCHATTENLAEUFER`, `RUNENWIRKER`,
  `WESENSWAHRER`), `ClassDefinition` (record), `ClassRegistry` (static lookup), `ClassManager`
  (persistence + apply/remove bonus + its own join listener).
- Commands: `/baum2 class list`, `/baum2 class info [<class>]`, `/baum2 class select <class>`.
- Persistence via Fabric's Attachment API, **now synced to the client** too
  (`.syncWith(PacketCodecs.STRING.xmap(...), AttachmentSyncPredicate.targetOnly())` — added
  during the Custom UI work below, since the HUD/Screen need to know the client's own class).
- Passive bonuses: Eisenwächter +4 max health, Schattenläufer +10% movement speed, Runenwirker
  +1 luck, Wesenswahrer +10% knockback resistance — each a stable-`Identifier`
  `EntityAttributeModifier`, swapped cleanly on reselection/rejoin.
- **4th class renamed `Seelenhüter` → `Wesenswahrer`** (`ip-naming-compliance-checker` found
  `Seelenhüter` was an exact match to *Echo of Soul*'s player-character title). **Wesenswahrer's
  bonus attribute changed from `MAX_ABSORPTION` (a confirmed no-op — nothing in the mod grants
  absorption hearts) to `KNOCKBACK_RESISTANCE`** (`balance-reviewer` finding).
- **Fine-grained `/attribute` verification is still the one open gap** for this system (see
  "Next recommended step") — a quick manual check confirmed class selection/switching *works*
  end-to-end via both the command and the new GUI (see below), but the exact numeric modifier
  values (`/attribute @s ... modifier value get baum2:class_bonus/...`) haven't been checked.
- **Known, logged, not fixed (design/judgment calls)**: free, instant, zero-cooldown class
  reselection lets a player capture all four bonuses' benefit contextually; Runenwirker's luck
  has no loot system yet to act on.

### Custom UI v1 — HUD overlay + Class Screen

- **Started from a request for a "Metin2 look"**, rejected as conflicting with
  `MASTERPROMPT.md`'s "no MMORPG UI imitation" rule; resolved with the user in favor of an
  original look using only generic, non-distinctive genre conventions —
  `docs/visual-style-guide.md` section 0 records why.
- `docs/visual-style-guide.md` — "Deepwood & Verdigris" art direction (flat, square-cornered
  panels, slate/verdigris/rune-cyan palette), per-class accent colors/icon motifs, HUD and
  Class Screen layout specs. **This doc was independently created on both branches this same
  day and merged together at merge time** — see "Last change" below; it now has a "two owners,
  two palettes" reconciliation note in its own Section 1 that's worth reading before further
  UI work (the Vitals/Stats-screen colors and the Class-System colors are not yet unified).
- `ui/PlayerStatusHud.java` — top-left HUD panel: class icon/name (accent-colored), level, a
  slim 3px rune-cyan XP sliver. Distinct region from `VitalsHud` (top-left vs. left status-bar
  column near the hotbar) — confirmed no collision.
- `ui/ClassScreen.java` — "Klassenübersicht", a full hand-drawn `Screen` listing all 4 classes
  with click-to-select, opened via a new **K** keybind (own `KeyBinding.Category.create
  (baum2:main)`, separate convention from `CharacterStatsScreen`'s vanilla `MISC` category —
  see the structural question below).
  - New C2S `networking/ClassSelectPayload.java` (mirrors `ExperienceSyncPayload`'s pattern in
    the other direction) lets clicking a card actually select a class server-side.
- Dead code removed: a duplicate, never-registered `Baum2Client` (in a `client` subpackage),
  and the old unwired `ui/ProgressionHud.java` prototype (superseded by `PlayerStatusHud`).
  **Both branches independently deleted these same two files** — confirmed clean convergent
  double-delete during the merge.
- **Open structural question, flagged by `merge-integration-reviewer` during this merge, not
  resolved**: the mod now has two "per-player identity/build" screens on separate keybinds
  ('K' → `ClassScreen`, hand-drawn chrome; 'C' → `CharacterStatsScreen`, vanilla tab/scroll
  chrome). `CharacterStatsScreen`'s own design already anticipated more tabs ("Skills, Class,
  etc."). Whether `ClassScreen`'s content should become a tab inside `CharacterStatsScreen`,
  or whether keeping them permanently separate is the right call, is a product decision for
  the contributors — not blocking, both work fine independently today.

- Repo: https://github.com/laserjonas/minecraft-baum2 (public).
- **Branches**: `jonas_workbranch` just merged `origin/master` (which had already absorbed all
  of `fischey_workbranch`'s recent work — `master` and `fischey_workbranch` were at the same
  commit at merge time). `jonas_workbranch` is now ahead of `master` by this merge commit plus
  its own prior Class-System/Custom-UI commits; push this branch, then decide whether/when to
  fast-forward or merge it back into `master`.
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
  session at `D:\Baum2\Baum2`.** Confirmed fixed as of this session — both `merge-integration-
  reviewer` calls during this merge ran as the real subagent type, not a workaround.
- **Known limitation (environment-specific, reported from at least one other environment)**:
  in at least one setup (the VS Code extension host per a prior session's note), custom
  `.claude/agents/` subagents were never available via the `Agent` tool even in a fresh
  session with the files present on disk — only built-in types resolved. Workaround used
  there: read the target `.claude/agents/<name>.md` file yourself and dispatch a
  general-purpose agent reproducing its role/instructions verbatim. Not encountered in this
  session (custom agent types resolved normally here) — if it recurs, that workaround remains
  available.
- **No GUI-automation tool exists in this environment** for the native Minecraft/LWJGL window
  (unlike a browser/Electron app) — every UI bug found in the Vitals/Attribute/Stats-screen
  work was only caught because a human played manually and reported back (screenshots, or a
  direct "it worked" / "it's broken" report), not by build/boot verification. Expect the same
  for any future UI work: build passing and clean boot are necessary but not sufficient checks.

## Last change (on `fischey_workbranch`, based on the merged commit above)

Wired Physical Attack/Attack Speed/Crit Chance into real combat, and added a target
nameplate HUD element — see "Combat System v1" and "Target nameplate" above for full detail.
User report: "Physical Attack, Critical Hit and Attack Speed had no effect when I attack a
monster" (correct — they were display-only) plus a request to show a targeted mob's name and
current life at top-center. Researched the exact 1.21.11 APIs before implementing (persisted
to `docs/fabric-modding.md`'s new "Combat / Damage" and "Target nameplate" sections): confirmed
no Fabric API event can modify a melee attack's final damage float (checked both plausible
candidates), confirmed the exact `EntityAttributeModifier.Operation.ADD_MULTIPLIED_TOTAL`
semantics via `EntityAttributeInstance.computeValue()`'s real decompiled body rather than
assuming, and confirmed mob health is ordinarily tracked-data-synced client-side with no
special-casing (no networking needed for the nameplate). A `balance-reviewer` pass on the
newly-wired combat effects found a real, non-blocking issue — Base Attack/Attack Speed/Crit
Chance compound multiplicatively into ~46x baseline DPS at max investment, trivializing most
mob fights well before max level — flagged clearly in "Combat System v1" above rather than
silently rebalancing formulas that were already approved in an earlier session for their own
sake. Verified: `./gradlew build` passes, `runClient` boots with the new Mixin applying
cleanly (no target-resolution errors). Not verified in an actual fight or by looking at a mob
in-game — same no-GUI-automation limitation as every UI/gameplay-feel check this session.

## Last change (on `jonas_workbranch`)

**Merged `origin/master` into `jonas_workbranch`.** Master had moved substantially since the
last sync — Fischey's branch added progression persistence, an XP curve rebalance +
`VanillaXpFormula`→`ProgressionCurve` rename, a full Vitals (Life/Mana) system with a HUD
rework, and a 4-attribute system with a Character Stats Screen — while `jonas_workbranch` had
added the Class System and this session's Custom UI work. Used `merge-integration-reviewer`
twice: once before this scope was fully visible (master kept moving mid-review), and again
with the full picture once it settled, including a real trial merge in a disposable worktree
to get ground truth on exact conflicts rather than guessing from diff stats.

Conflicts resolved:
- **`progression/PlayerProgressData.java`** — master fully rewrote this class (Codec-based
  persistence, 5 new attribute/mana fields, 9-arg constructor). Took master's structure as the
  base and re-applied jonas's one-line fix into it (the no-arg constructor's
  `experienceForNextLevel` now correctly calls `ProgressionCurve.getXpRequiredForLevel(level +
  1)` instead of a hardcoded `100`, which — unfixed — would have undercounted the first
  level-up by ~48% against the new, steeper curve). Dropped jonas's `writeNbt`/`readNbt`
  entirely (dead code once master's Codec-based persistence superseded manual NBT).
- **`networking/Baum2Networking.java`** — both branches restructured the same method. Kept
  master's payload-type registrations, consolidated all C2S receivers (jonas's
  `ClassSelectPayload`, master's `SpendAttributePointPayload`) into one
  `registerServerReceivers()` method for consistency, since `Baum2.java` already calls both
  `registerServerPayloads()` and `registerServerReceivers()` post-merge anyway.
- **`Baum2.java`, `Baum2Client.java`, `en_us.json`** — mechanical, kept both sides' additions
  (no functional overlap: different registrations, different keybindings, different lang
  keys).
- **`CLAUDE.md`** — both branches independently added an identical `graphics-designer` agent
  file (confirmed same blob hash) but described it in slightly different prose in this file;
  kept jonas's wording (marginally more detailed).
- **`docs/visual-style-guide.md`** — a real add/add conflict (both branches created this file
  from scratch, same day, no common ancestor to diff against). This was a content
  reconciliation, not a mechanical conflict: kept jonas's version as the frame (numbered
  sections, IP-compliance guardrails, base palette) and folded master's two screen specs in as
  new Sections 11-12, correcting one now-false claim (master's "art direction not yet formally
  defined" — it now is, in Section 1) and adding an explicit reconciliation note that the two
  sides' color palettes are not yet unified (a deliberate open follow-up, not an oversight).
- **`HANDOFF.md`** — rewritten fresh from both sides' state (this file), per established
  practice for this project's prior cross-branch merges.
- Two dead-code deletions (`client/Baum2Client.java`, `ui/ProgressionHud.java`) turned out to
  be convergent — both branches deleted the exact same files independently, no conflict.

Verified: `./gradlew build` passes after all resolutions. Not yet re-verified in-game post-merge
— see "Next recommended step".

Why: user asked to merge `master` into `jonas_workbranch` to bring both contributors' recent
work together. Used the project's own `merge-integration-reviewer` agent proactively, per
`CLAUDE.md`'s rule, given the scope of parallel work on both branches touching progression,
networking, and UI at the same time.

Earlier, on `jonas_workbranch`: ran `ip-naming-compliance-checker` and `balance-reviewer` for
real against Class System v1, then built Custom UI v1 (HUD overlay + Class Screen) with
`graphics-designer` and `fabric-docs-researcher`. Full detail on both of these (naming
rename, balance fixes, style guide creation, HUD/Screen implementation, dead-code cleanup) is
folded into "Current state" above under "Class System v1" and "Custom UI v1" — see
`git log -p HANDOFF.md` for the original blow-by-blow narrative if needed.

Earlier, on `fischey_workbranch` (now merged): added the 4-attribute system + Character Stats
Screen rework (including the scrolling and header-overlap bug fixes), the Vitals (Life/Mana)
system + HUD rework, the XP curve rebalance/rename, and progression persistence via the
Attachment API (including two real bugs found and fixed: a lazy-class-loading bug that
silently reset progress on join, and a dev-environment random-username bug that looked like
the same symptom but wasn't). Full detail folded into "Current state" above under
"Progression System", "Vitals System", and "Attribute System" — see `git log -p HANDOFF.md`
for the original blow-by-blow narrative (multiple detailed entries, each with root-cause
analysis) if needed.

Earlier, on `master`: merged both contributors' branches together for the first time (fast-
forward of `jonas_workbranch`'s agent/docs work, merge commit for `fischey_workbranch`'s
progression/networking/mixin work) — see `git log -p HANDOFF.md` for that merge's detail.

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
`ClassSelectPayload.java` / `SpendAttributePointPayload.java` (C2S) as templates.

## Attachment API reference (persistent per-player/entity data)

For any future "store custom data on a player/entity/block-entity/chunk that should survive
restarts" need, use `fabric-data-attachment-api-v1` (already a dependency) rather than a
hand-rolled `HashMap<UUID, ...>` + manual save/load. Reference implementations:
`progression/PlayerLevelSystem.java` (`PROGRESSION` field) + `progression/PlayerProgressData.java`
(`CODEC`), and `classes/ClassManager.java` (`SELECTED_CLASS`, including client sync via
`.syncWith(...)`).

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
`SELECTED_CLASS` field to initialize as a side effect of Java's class-init rules) but doesn't
document this the way `PlayerLevelSystem.bootstrap()`'s Javadoc does — worth a defensive
comment if `registerEvents()` is ever refactored to be lazy.

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

1. **In-game verification of everything merged this session** — no GUI-automation tool exists
   here (see "Current state"), so this needs a human: confirm the Class Screen ('K') and
   Character Stats Screen ('C') both still open correctly and don't visually collide with each
   other or with the three HUD elements (`PlayerStatusHud` top-left, `VitalsHud` left status-bar
   column, the new `MobNameplateHud` top-center); confirm the Stats screen's scrollbar/`+1`
   buttons still work; confirm class selection still works via both the command and
   `ClassScreen`'s click-to-select; **confirm combat actually feels right** — attack a mob and
   check damage/attack-speed/crits are happening (not just that nothing crashes), and check the
   new top-center nameplate shows a targeted mob's name/health correctly.
1a. **Balance decision needed, not yet made**: the newly-wired combat stats compound into ~46x
    baseline DPS at max investment (see "Combat System v1") — decide whether this power curve
    is intended (matches this project's own "hardcore grind" framing for leveling, or
    contradicts it by trivializing combat well before max level) and, if not, which formula(s)
    to temper: `VitalsCurve.getBaseAttack`'s linear-uncapped scaling is the dominant
    contributor, not the crit multiplier.
2. **Fine-grained `/attribute` verification of the Class System** (older, still-pending item):
   `/baum2 class select eisenwaechter` → `/attribute @s minecraft:max_health modifier value
   get baum2:class_bonus/eisenwaechter_max_health` (expect `4.0`) → `/attribute @s
   minecraft:max_health get` (expect `24.0`) → disconnect/rejoin → `/kill @s` and respawn
   (confirm `.copyOnDeath()` holds). Spot-check `wesenswahrer`'s
   `minecraft:generic.knockback_resistance` (expect `0.1`).
3. **Fresh `balance-reviewer` pass on the new `ProgressionCurve` + attribute system together**
   — the old curve was reviewed, the new one hasn't been, and the attribute system's own
   caps/formulas were only reviewed via a workaround-agent, not the real subagent.
4. **The `ClassScreen`/`CharacterStatsScreen` structural question** (see "Current state") —
   needs a product decision from the contributors, not more code.
5. **Partially resolved**: Physical Attack/Attack Speed/Crit Chance now affect real combat (see
   "Combat System v1"). Still purely display-only: Base Magic Attack/Magic Defence (no
   spell-casting system exists to consume them) and Physical/Magic Defence (no incoming-damage
   reduction wired — the user's request was specifically about attacking, not being attacked;
   worth doing symmetrically once a real `combat/` package exists for either side).
6. **Class-name banner + level-diamond badge** (deferred in an earlier Vitals-work session
   because "no `classes/` package exists yet") — that's no longer true; worth revisiting now
   that the Class System exists, possibly as a `CharacterStatsScreen` tab per point 4 above.
7. `ip-naming-compliance-checker` hasn't been run against the newer player-facing strings
   (Stats screen row labels, attribute names) — only the Class System's names have been
   checked so far.
8. Get real (non-placeholder) art for the 4 class icons — see `docs/visual-style-guide.md`
   section 9.
9. Remaining Priority 1 items per `CLAUDE.md`: first custom item, first weapon, first active
   skill with a cooldown manager, first world-event block. Consult `fabric-docs-researcher` /
   `docs/fabric-modding.md` before implementing any of these if the relevant Fabric API is
   unclear. Use `graphics-designer` for the texture/model/icon each of these will need.
