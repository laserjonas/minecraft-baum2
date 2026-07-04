# Handoff

Read this first. It reflects the state as of the latest commit so a fresh Claude Code
session (yours or a co-author's) can pick up work without re-deriving context from
`git log`. Updated after every commit — see "Git Rules" in `CLAUDE.md`.

## Current state

This reflects `jonas_workbranch` after merging `origin/master` (Fischey's Vitals/Attribute/
Character-Stats work) and then adding a Skill System + Class Sub-specializations on top — see
"Last change" below for both.

- Fabric mod builds successfully (`./gradlew build` passes).
- Client runs: `./gradlew runClient` loads, reaches the main menu, and joins a world cleanly
  (verified clean boot, no Mixin/payload/HUD-registration errors; user-confirmed working
  in-game for spell casting via keybind and command).
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
  - **No `combat/` package exists yet** — none of Strength/Intelligence/Dexterity's derived
    stats affect actual damage/defence/speed/crit in-game; display-only plumbing for now, same
    as Mana. **This is the biggest open gap in the progression stack** — see "Next recommended
    step".
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
- **Two real, confirmed-not-guessed bugs were fixed during this system's development** (both
  found via actual user screenshots, not caught by build/boot verification alone): the tab
  header overlapping content (root cause: `GridScreenTab`'s default centering anchor goes
  negative once content is taller than the tab area — fixed via a `refreshGrid` override),
  and bottom rows being unreachable at high GUI Scale (fixed via `ScrollableLayoutWidget`).
  **General lesson for future custom `Screen`s in this codebase**: override `refreshGrid`
  (or equivalent) for any content that might grow past a single screen's worth of rows, and
  don't assume build success + clean boot means the UI actually renders/scrolls correctly —
  it doesn't catch layout bugs.

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

### Custom UI v1 — Class tab merged into Character Stats Screen, top-left HUD removed

- **Started from a request for a "Metin2 look"**, rejected as conflicting with
  `MASTERPROMPT.md`'s "no MMORPG UI imitation" rule; resolved with the user in favor of an
  original look using only generic, non-distinctive genre conventions —
  `docs/visual-style-guide.md` section 0 records why.
- `docs/visual-style-guide.md` — "Deepwood & Verdigris" art direction (flat, square-cornered
  panels, slate/verdigris/rune-cyan palette), per-class accent colors/icon motifs. **This doc
  was independently created on both branches the same day and merged together at merge time**
  — it has a "two owners, two palettes" reconciliation note in its own Section 1 worth reading
  before further UI work (the Vitals/Stats-screen colors and the Class-System colors are not
  yet unified).
- **The standalone `ClassScreen` ('K' keybind) and the top-left `PlayerStatusHud` are both
  gone**, resolving the "two competing identity screens" structural question a prior session
  flagged: `CharacterStatsScreen` ('C' key) now has a second tab, **"Class"**, built the same
  way as its existing "Stats" tab (a `GridScreenTab` + `TabNavigationWidget`, per-class-card
  `ClickableWidget`s in a single-column `GridWidget`) — same click-to-select behavior the old
  `ClassScreen` had (icon/name/description/bonus per card, "Aktiv" tag + colored border/wash
  on the selected card), just relocated. The top-left HUD panel was removed outright rather
  than kept as a level-only indicator, since vanilla's own XP bar already shows level/XP and
  the panel's only other job (showing class) moved into the new tab.
  - `networking/ClassSelectPayload.java` (C2S, unchanged) still backs the tab's
    click-to-select.
- Dead code removed earlier this session (during the master-merge): a duplicate,
  never-registered `Baum2Client` (in a `client` subpackage), and the old unwired
  `ui/ProgressionHud.java` prototype.

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
  - **Fallback/testing: `/baum2 cast <spell>`** — kept for direct testing access (e.g. casting
    a spell not on your current class's slot 0/1 for debugging), same pattern as this project
    keeping both a command and a GUI for class selection.
- This started from an explicit request to "get inspired by Metin2's Warrior/Sura/Shaman/Ninja
  classes and their sub-specs" — that was rejected as direct IP copying (Metin2 named
  specifically), and resolved with the user in favor of building original spells/sub-specs
  from this project's *own* already-established class identities instead. See the Class
  Sub-specializations entry below for the naming-compliance detail (3 of the sub-spec names
  were independently caught resembling specific WoW spec names and renamed before landing).

### Class Sub-specializations v1 — 2 per class, layered on top of the base class bonus

- `classes/ClassSubspec.java` (enum, 8 values, 2 per `PlayerClass`) + `classes/
  SubspecDefinition.java` (record, mirrors `ClassDefinition`) + `classes/SubspecRegistry.java`.
  `ClassManager` gained a second `AttachmentType` (`SELECTED_SUBSPEC`, same
  persistent+copyOnDeath+syncWith pattern as `SELECTED_CLASS`) and `selectSubspec(...)`, which
  validates the chosen sub-spec actually belongs to the player's current class and clears the
  old sub-spec's bonus/attachment whenever the base class changes (a stale sub-spec would
  otherwise silently linger, invalid, after switching classes).
- Commands: `/baum2 class subspec list`, `/baum2 class subspec select <subspec>` — no GUI yet
  (see "Next recommended step").
- Final names, after one naming-compliance round caught 3 problems (see below): Eisenwächter
  (Bollwerk +armor, Stahlfaust +attack damage), Schattenläufer (Schattenpirscher +attack
  damage, Sturmklinge +attack speed), Runenwirker (Splitterrune +attack damage, Glücksrune
  +luck), Wesenswahrer (Wurzelwall +knockback resistance, Wesensfülle +max health).
- **`ip-naming-compliance-checker` flagged and got 3 of the original 8 sub-spec names
  renamed** before anything shipped — these weren't generic-word overlaps, they were exact or
  near-exact matches to specific, well-known WoW German spec/spell names: "Vergelter" (→
  Stahlfaust — matched WoW Paladin's actual "Vergeltung"/community "Vergelter-Paladin" shorthand,
  same "defensive class turns offensive" concept too), "Wildwuchs" (→ Wesensfülle — exact,
  unmodified match to WoW Druid's real spell "Wild Growth"), "Zerstörungsrune" (→
  Splitterrune — contained WoW Warlock's actual "Zerstörung"/Destruction spec name; the
  replacement reuses this project's own pre-existing "Splitter" motif from Rissobelisk/
  Sternsplitter/Splitterwächter instead). All 3 replacements re-checked clean. Also
  double-checked: the "4 classes × 2 sub-specs" *structure* itself doesn't read as
  Metin2-specific (that shape is shared by multiple genre-wide games), and each class's
  specific offense/defense-or-utility split was deliberately varied per class rather than
  applying one uniform template, so it doesn't read as "Metin2's 4 classes with new names."
- **`balance-reviewer` found and fixed one real bug**: Wurzelwall was `ADD_MULTIPLIED_BASE`
  while Wesenswahrer's own class bonus (which it's meant to stack with, like Glücksrune
  correctly does with Runenwirker's luck) is `ADD_VALUE` — vanilla's attribute formula means
  those two operations don't combine the way "+10% on top of +10%" suggests (nets ~11%, not
  20%). Fixed to `ADD_VALUE` to match, and to match Glücksrune's already-correct pattern.
- **`balance-reviewer` also found Lebensband (the "heal" spell) was non-functional**: a flat
  6 HP heal against Life's new post-Vitals-rework scale (500-2480, see Vitals System above) is
  outpaced by passive Life Regen after just ~3 Endurance points — the dedicated heal spell was
  *worse than doing nothing* for most of the game. Fixed to heal 12% of max life instead of a
  flat amount, so it scales with the player.
- **Logged, not fixed (design/judgment calls)**: no spell costs Mana despite a Mana system
  existing specifically to be spent (currently 100% inert/display-only); sub-spec reselection
  is free/instant/uncapped, same known gap as base-class reselection; none of the 4 AoE/damage
  spells do a line-of-sight/raycast check, so they can hit through walls; `Nebelschritt`'s dash
  has no collision check (can clip through thin walls or land inside terrain); AoE effects
  (`Geisterwoge`, `Schildstoß`, `Klingenwirbel`, `Arkaner Kreis`) don't filter out other
  players, so they hit friend and foe alike; `Arkaner Kreis`'s 25s cooldown / 15s duration lets
  2 coordinated players keep its Luck buff at 100% uptime (currently low-impact only because
  Luck has no loot-system consumer yet — revisit once one exists).

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

## Last change (on `jonas_workbranch`)

Three things in one session, building on the merge below: (1) moved the standalone
`ClassScreen` ('K') into `CharacterStatsScreen` as a new "Class" tab and removed the top-left
`PlayerStatusHud`, resolving the two-competing-screens question the merge had flagged; (2)
built Skill System v1 (8 spells) and Class Sub-specialization v1 (8 sub-specs) from scratch,
after explicitly declining a request to base them on Metin2's actual class roster; (3) added
keybind casting (V/B) after `/baum2 cast <spell>`-only turned out to not make sense as the
primary way to fight. Used `fabric-docs-researcher` (combat/effect API research — a genuine
knockback-sync gotcha found, see "Networking API reference" below), `ip-naming-compliance-
checker` (caught 3 WoW-spec-name collisions in the sub-spec names before they shipped), and
`balance-reviewer` (found and fixed a stacking-operation bug and a non-functional heal spell)
proactively per `CLAUDE.md`. Also fixed a real self-found bug during testing: the cooldown
tracker's original "never cast yet" sentinel was `Long.MIN_VALUE`, which overflows signed
64-bit arithmetic in the elapsed-time subtraction and made every spell falsely report as
already on cooldown (with a nonsense multi-trillion-second remaining time) on its very first
cast — fixed by using `Long.MIN_VALUE / 2` instead, which leaves enough headroom that the
subtraction can't overflow for any realistic server uptime.

Verified: `./gradlew build` passes; user tested in a real client and confirmed casting via
both V/B keybinds and the command path work.

Earlier, still on `jonas_workbranch`: **merged `origin/master` into `jonas_workbranch`.** Master had moved substantially since the
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
`ClassSelectPayload.java` / `SpendAttributePointPayload.java` / `CastSpellPayload.java` (C2S)
as templates.

**Combat/skill-effect APIs** (dealing damage, AoE entity queries, knockback, status effects,
healing, dashing — used by the new Skill System's `SpellEffects.java`) are documented
separately in `docs/fabric-modding.md`'s "Combat / Skill effects" section, not duplicated
here. The one gotcha worth calling out at this level since it's easy to reintroduce: pushing a
**player** target with `takeKnockback(...)`/`addVelocity(...)` alone silently never reaches
that player's own client (only nearby *observers* see them fly back) — you must additionally
send `new EntityVelocityUpdateS2CPacket(targetPlayer)` to `targetPlayer.networkHandler`
yourself. `SpellEffects.pushAwayFrom(...)` already does this; reuse it rather than calling
`takeKnockback` directly for any future spell/effect.

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

1. **Human decisions needed on the Skill System's logged (not auto-fixed) balance findings** —
   see "Class Sub-specializations v1" above for full detail: (a) should spells cost Mana now
   that a real consumer for it would exist, or stay cooldown-only by design; (b) should
   sub-spec reselection get a cost/cooldown (same open question as base-class reselection);
   (c) should the 4 AoE/damage spells gain a line-of-sight check, and should `Nebelschritt`'s
   dash gain a collision check, or are both acceptable as "short blink through thin obstacles"
   for v1; (d) should AoE effects exclude other players (friendly fire currently on); (e)
   revisit `Arkaner Kreis`'s cooldown/duration once Luck has an actual loot-system consumer.
2. **Fine-grained `/attribute` verification of the Class System** (older, still-pending item):
   `/baum2 class select eisenwaechter` → `/attribute @s minecraft:max_health modifier value
   get baum2:class_bonus/eisenwaechter_max_health` (expect `4.0`) → `/attribute @s
   minecraft:max_health get` (expect `24.0`) → disconnect/rejoin → `/kill @s` and respawn
   (confirm `.copyOnDeath()` holds). Spot-check `wesenswahrer`'s
   `minecraft:generic.knockback_resistance` (expect `0.1`).
3. **Fresh `balance-reviewer` pass on the new `ProgressionCurve` + attribute system together**
   — the old curve was reviewed, the new one hasn't been, and the attribute system's own
   caps/formulas were only reviewed via a workaround-agent, not the real subagent.
4. **The biggest gap in the whole progression stack**: no `combat/` package exists, so Base
   Attack/Magic Attack/Physical/Magic Defence/Attack-Cast Speed/Crit Chance (and now the Skill
   System's damage numbers) are all separate from each other — a real combat system should
   unify them (e.g. spell damage scaling off Strength/Intelligence) rather than staying two
   parallel, non-interacting number sets.
5. **Sub-spec selection has no GUI yet** (`/baum2 class subspec list|select` only) — the new
   "Class" tab in `CharacterStatsScreen` shows/selects the base class but not sub-specs; a
   natural next step once the tab's card layout is proven out.
6. Get real (non-placeholder) art for the 4 class icons, and consider whether the 8 spells/8
   sub-specs want icons too once there's a GUI surface to show them in — see
   `docs/visual-style-guide.md` section 9.
7. Remaining Priority 1 items per `CLAUDE.md`: first custom item, first weapon, first
   world-event block (the first active skill is now done — see Skill System v1 above). Consult
   `fabric-docs-researcher` / `docs/fabric-modding.md` before implementing any of these if the
   relevant Fabric API is unclear. Use `graphics-designer` for the texture/model/icon each of
   these will need.
