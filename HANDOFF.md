# Handoff

Read this first. It reflects the state as of the latest commit so a fresh Claude Code
session (yours or a co-author's) can pick up work without re-deriving context from
`git log`. Updated after every commit — see "Git Rules" in `CLAUDE.md`.

## Current state

- Fabric mod builds successfully (`./gradlew build` passes).
- Client runs: `./gradlew runClient` loads, reaches the main menu, and joins a world cleanly
  (verified clean boot and clean world-join, no Mixin/payload/HUD-registration errors).
- **Life and Mana systems — implemented, HUD reworked, vanilla heart removed:**
  - `progression/VitalsCurve.java`: `getMaxLife(level) = 500 + 10*level` (510 at level 1, 1500
    at level 100), `getMaxMana(level) = 100 + 5*level` (105 to 600).
  - Life is real vanilla health (`EntityAttributes.MAX_HEALTH`), rescaled via
    `progression/VitalsManager.applyMaxLife` — deliberate choice so combat/damage/death/regen
    keep working unchanged; only the attribute's base value and the HUD are touched. **Vanilla
    clamps this attribute's effective value at 1024**, which the formula would silently exceed
    past level ~53 (every level after that would compute a bigger number but have zero actual
    effect) — fixed via an accessor Mixin (`mixin/ClampedEntityAttributeAccessor.java`) that
    widens the clamp to 2048 once at mod init (`VitalsManager.widenMaxHealthCeiling()`, called
    first thing in `Baum2.onInitialize()`, before `PlayerLevelSystem.bootstrap()`'s guarantee
    matters for players). If a future balance change pushes Life past 2048, raise this
    constant too.
  - Mana is new, persisted in `progression/PlayerProgressData.java` (`mana` field, `Codec.INT`
    via `optionalFieldOf("Mana", 100)` — **must stay optional, not `fieldOf`**, see "Last
    change" for why), clamped/regenerated every tick in `events/VitalsTickHandler.java`
    (~2% of max per second, `VitalsManager.regenMana`), synced to the client via a new
    `networking/ManaSyncPayload.java` (same `PacketCodec`/`RegistryByteBuf` pattern as
    `ExperienceSyncPayload`). No skill/system consumes Mana yet — this is future-use plumbing.
  - HUD: `ui/VitalsHud.java` replaces the vanilla heart bar in place (`HudElementRegistry
    .replaceElement(VanillaHudElements.HEALTH_BAR, ...)` — **not** `removeElement`, see "Last
    change") and adds a new Mana bar directly above it (`attachElementBefore(ARMOR_BAR, ...)`),
    both left-aligned where vanilla hearts used to sit. Colors/dimensions are documented in the
    new `docs/visual-style-guide.md`. Old dead `ui/ProgressionHud.java` (never wired up, see
    prior "Rendering / HUD" note in `docs/fabric-modding.md`) was deleted and replaced by this.
  - A fifth subagent, `graphics-designer`, was added (pulled in from `jonas_workbranch`, which
    had it but hadn't merged it yet) and used to produce the Life/Mana bar visual spec — see
    `docs/visual-style-guide.md`.
- Package: `de.baum2dev.baum2` / Main: `Baum2` / Client: `Baum2Client`.
- Minecraft 1.21.11 / Yarn 1.21.11+build.6 / Fabric API 0.141.4+1.21.11 / Fabric Loom 1.17.13 / Java 21.
- **Progression System — FULLY WORKING, including real-time client display:**
  - Custom progression uses our own XP curve, centralized in `progression/ProgressionCurve.java`
    (single source of truth — `ExperienceManager`, `ProgressionTickHandler`, `PlayerLevelSystem`,
    `LevelUpHandler`, and the client packet handler all call into it instead of each having
    their own copy): `xpRequiredForLevel(L) = 80 + 40L + 8L²` — a "hardcore grind" pace chosen
    deliberately steeper than vanilla Minecraft's own curve (see "Last change" below for why).
  - Features: `/baum2 addxp <amount>`, `/baum2 level`, mob XP drops (10 + max_health/2), level-up
    broadcasts, vanilla XP orb drops disabled via Mixin.
  - **Real-time client sync now works** via a custom S2C packet sent every server tick — see
    "Networking API reference" section below for the exact API and why earlier attempts failed.
  - **Persistence now works** via Fabric's Data Attachment API (`fabric-data-attachment-api-v1`,
    already a dependency) — progression survives server restarts, disconnects, and death. See
    "Last change" and "Attachment API reference" below.
- Repo: https://github.com/laserjonas/minecraft-baum2 (public).
- **Branches**: `master` is now the merge of both work branches (see "Last change" below).
  `fischey_workbranch` and `jonas_workbranch` still exist and still track their respective
  remotes for any follow-up work, but master has absorbed everything from both as of this
  commit — start new work from `master` rather than the old work branches unless there's a
  reason to keep working on one specifically.
- `.vscode/` is checked in (extensions.json, settings.json, tasks.json) so fresh checkout gets Java+Gradle
  recommendations and "Run Minecraft Client" task (`Ctrl+Shift+B`) out of the box.
- Five subagents under `.claude/agents/` (shared via git, so both contributors get them):
  `fabric-docs-researcher` (Fabric/MC API research -> `docs/fabric-modding.md`),
  `ip-naming-compliance-checker` (reviews new names/text against IP/naming rules),
  `balance-reviewer` (internal-consistency/exploit review of numeric balance values),
  `merge-integration-reviewer` (pre-merge overlap/design-conflict check between branches),
  `graphics-designer` (produces visual/HUD specs and assets, maintains
  `docs/visual-style-guide.md` — the exception that *does* write files, see below).
  The first four report findings only — they don't edit files. See `CLAUDE.md` -> "Project
  Agents" for exact trigger conditions; use them proactively, don't wait to be asked.
  Still not yet run against the progression system's balance values / player-facing strings —
  see "Next recommended step".
- **Known limitation**: a running Claude Code session loads its available agent list at
  startup, so newly added `.claude/agents/*.md` files aren't picked up mid-session — they
  become available the next time a session starts fresh (restart, or a fresh session after
  pulling). If an agent invocation fails with "Agent type not found" right after one was
  added, that's why — not a bug in the agent definition.
  - Update: in at least one environment (the VS Code extension host), custom `.claude/agents/`
    subagents were **never** available via the `Agent` tool's `subagent_type`, even in a
    freshly started session with the files already present on disk — only the built-in types
    (`claude`, `claude-code-guide`, `Explore`, `general-purpose`, `Plan`, `statusline-setup`)
    showed up. Workaround used successfully: read the target `.claude/agents/<name>.md` file
    yourself, then dispatch a `general-purpose` agent whose prompt reproduces that file's role/
    instructions/context verbatim. This gets the same review quality without needing the
    dedicated subagent type. If a future session finds custom types *do* resolve, prefer that
    directly — this workaround is a fallback, not the preferred path.

## Last change (on `fischey_workbranch`, not yet merged to `master`)

Added Life and Mana systems plus a HUD rework, replacing the vanilla heart bar:

- User request: rework Life, add Mana "for future use," show both on the HUD, remove the
  vanilla heart. Formulas given explicitly: Life = 500 + 10/level, Mana = 100 + 5/level.
- Scoping decisions made with the user up front (see conversation, not re-derivable from code):
  no class-name banner/level-diamond badge yet (no `classes/` package exists — deferred, out of
  scope for this change); Life scales the *real* vanilla max-health attribute rather than being
  a fully decoupled custom stat (keeps combat/damage/death/regen working with zero extra code);
  Mana gets passive regen now even though nothing consumes it yet (a rate, not zero, so the
  system isn't inert — see `VitalsManager.regenMana`).
- New files: `progression/VitalsCurve.java` (formulas), `progression/VitalsManager.java`
  (applies Life, clamps/regens Mana, widens the health-attribute clamp), `events/
  VitalsTickHandler.java` (per-tick apply/regen/sync + join hook), `networking/
  ManaSyncPayload.java` (S2C sync, mirrors `ExperienceSyncPayload`), `ui/VitalsHud.java`
  (the actual HUD element, replaces dead `ui/ProgressionHud.java`), `mixin/
  ClampedEntityAttributeAccessor.java` (see bug below), `docs/visual-style-guide.md` (new,
  written by the `graphics-designer` agent), `.claude/agents/graphics-designer.md` (pulled in
  from `jonas_workbranch`, which had added it but hadn't merged it into `master` yet — zero
  conflict, docs-only file, brought over directly rather than via a branch merge).
- **Bug #1, caught by static/formula review (the project's own `balance-reviewer` agent, run
  via the workaround above since the subagent type doesn't resolve in this environment): vanilla
  hardcodes `EntityAttributes.MAX_HEALTH` as a `ClampedEntityAttribute` with max 1024**
  (confirmed via decompiled source, not assumption). The Life formula reaches 1500 at level
  100, so everything past level ~53 would have silently computed a bigger number with zero
  actual in-game effect — the health bar would just stop growing, with nothing logged. Fixed
  with `mixin/ClampedEntityAttributeAccessor.java` (a `@Mutable @Accessor("maxValue")` on
  `ClampedEntityAttribute`), widening the clamp to 2048 once via `VitalsManager
  .widenMaxHealthCeiling()`, called first thing in `Baum2.onInitialize()`. If a future balance
  pass pushes Life past 2048, raise that constant too — it's not derived from `VitalsCurve`
  automatically, they're two separate numbers that both need to stay ahead of the real max.
- **Bug #2, caught only by actually running `./gradlew runClient` and joining a save** (the
  `verify` workflow — this would **not** have been caught by `./gradlew build` alone, since it's
  a runtime data-loss bug, not a compile error): the initial `PlayerProgressData` Codec used
  `Codec.INT.fieldOf("Mana")` (a *required* field) for the new `mana` field. Loading any
  pre-existing save (one predating this change) failed to decode the **entire** attachment —
  not just the missing field — because `RecordCodecBuilder`'s `instance.group(...)` fails the
  whole record if any required field is absent. Confirmed directly: loaded a local level-45 dev
  test save, saw `[fabric-data-attachment-api-v1] Skipping invalid attachments: No key Mana in
  MapLike[...]` in the log, and the player fell back to the initializer default (level 1) —
  the exact same silent-reset failure mode documented earlier in this file for a completely
  different root cause (lazy class loading). **General lesson, in addition to the existing one
  about force-loading attachment classes: adding a new field to an already-`persistent()`
  attachment Codec must use `optionalFieldOf(name, default)`, never `fieldOf(name)`, or every
  save predating the new field silently loses *all* of its data, not just the new field.** Fixed
  by switching to `Codec.INT.optionalFieldOf("Mana", 100)`; re-verified with the same save file
  that the codec now decodes successfully (no "Skipping invalid attachments" warning) and
  `VitalsManager` corrects the fallback value up/down within a tick or two regardless.
- The local dev-sandbox save this was tested against (`run/saves/...`, gitignored, disposable
  test data per the project's existing convention — see the `usercache.json`/`Baum2Dev` note
  elsewhere in this file) had its level-45 test character actually reset to level 1 by Bug #2
  during verification, before the fix landed. Not attempted to recover (scratch data, not
  source-controlled) — flagging only so it isn't mistaken for a report of the bug still being
  live; the fix was verified against the same save afterward and no further resets occurred.
- HUD implementation notes (Fabric API 0.141.4+1.21.11 specifics, verified via decompiled
  sources — see `docs/fabric-modding.md`'s new "HUD element registry" section for full detail):
  the old `HudLayerRegistrationCallback`/`IdentifiedLayer` API from older Fabric API versions
  **does not exist** in this version; the current API is `HudElementRegistry` +
  `VanillaHudElements` + `HudStatusBarHeightRegistry`, all in
  `net.fabricmc.fabric.api.client.rendering.v1.hud`. Tried `HudElementRegistry.removeElement
  (VanillaHudElements.HEALTH_BAR)` first — **this crashes the client at startup** with
  `IllegalStateException: Unregistered hud elements: [minecraft:health_bar]`, because the
  framework validates that every id with a registered `StatusBarHeightProvider` still has a
  matching `HudElement`, and removing health_bar entirely leaves its built-in height provider
  dangling. Fix: use `replaceElement(VanillaHudElements.HEALTH_BAR, old -> ourRenderer)`
  instead (keeps the vanilla id "occupied" so the built-in height-provider entry stays valid),
  and explicitly re-register that id's height provider too via `HudStatusBarHeightRegistry
  .addLeft(VanillaHudElements.HEALTH_BAR, ...)` — vanilla's own default height provider for
  that id does a dynamic multi-row heart-count calculation that no longer means anything once
  hearts aren't being drawn there. The new Mana bar is a genuinely new element (own
  `Identifier`), inserted via `attachElementBefore(VanillaHudElements.ARMOR_BAR, ...)` so armor
  still renders (untouched) and gets pushed up to make room.
- Verified: `./gradlew build` passes; `./gradlew runClient` boots cleanly and joins a world
  with no Mixin/HUD/attachment errors (post-fix). **Not visually confirmed** — this environment
  has no GUI-automation tool that can drive the actual Minecraft window (it's a native LWJGL
  app, not a browser/Electron app the `run`/Playwright tooling can screenshot), so the on-
  screen bar positions/colors from `docs/visual-style-guide.md` haven't been eyeballed in-game
  yet. That's expected manual follow-up, not a gap in this session's verification.

Earlier, still on `fischey_workbranch`:

Rebalanced the XP curve and renamed `VanillaXpFormula` → `ProgressionCurve`:

- Why: with persistence and real-time sync finally both working (see entries below), the
  underlying numbers became visible as a real problem — vanilla's own curve is calibrated for
  vanilla's tiny XP sources (1-7 XP per pickup). Our `MobDeathHandler` grants 10-60+ XP per
  kill and testing routinely used `/baum2 addxp` with amounts in the hundreds, so under the old
  curve a handful of kills could jump a player from level 1 to level 8. User asked for the curve
  to be rebalanced with the explicit requirement that every level cost strictly more than the
  last (the old curve was *technically* already monotonic, just far too gently scaled for our
  reward economy — see the numbers below).
- Asked the user to pick a pacing philosophy with concrete kills-to-level numbers (assuming
  ~20 XP/kill) rather than guessing — they picked "slow/hardcore grind."
- New formula, in `progression/ProgressionCurve.java`:
  `xpRequiredForLevel(level) = 80 + 40*level + 8*level²`. Concretely: Level 1 costs 128 XP
  (~6 kills), Level 10 costs 1280 XP (~64 kills), Level 50 costs 22,080 XP (~1,104 kills),
  Level 100 costs 84,080 XP (~4,200 kills for that one level). Cumulative total to reach
  level 100 from scratch: ~2,916,800 XP (~146,000 kills lifetime) — reaching max level is meant
  to be a serious, long-term achievement.
  `getTotalXpForLevel(level)` is now a simple loop-sum of `getXpRequiredForLevel(1..level)`
  rather than a closed-form cumulative formula — simpler and safer than deriving/maintaining a
  closed form by hand, and `level` maxes out at 100 (`ExperienceManager.getMaxLevel()`) so the
  loop is trivially cheap even though it's called every server tick per player (for the
  `totalExperience` sync field — see "Networking API reference").
- Renamed the class (and file) because it's no longer vanilla's actual formula — keeping the
  old name would have misled a future reader, especially since this same file used to spell out
  vanilla's real per-tier formula in comments/docs. Updated every reference (`ExperienceManager`,
  `ProgressionTickHandler`, `PlayerLevelSystem`, `LevelUpHandler`, `ClientNetworkingHandler`).
- Note: this change is purely about the *number* of XP required per level. It does not affect
  the vanilla-bar-fill display mechanism at all — the client's bar-fill percentage is computed
  directly from our own current/max XP values (see "Attachment"/"Networking" sections), never
  from vanilla's actual formula, so any future curve rebalance is similarly safe to do freely.

Earlier, still on `fischey_workbranch`: fixed the *actual* persistence bug. The fixed
dev-username change below (previous "Last change"
entry) was real and necessary but **not sufficient** — user re-tested with the stable `Baum2Dev`
identity, gained XP, rebuilt, relaunched, and still lost progress. This is the real root cause:

- Root cause: `PlayerLevelSystem`'s `AttachmentType<PlayerProgressData> PROGRESSION` field is a
  Java static field, initialized lazily the first time something calls a static method on that
  class. Nothing in `Baum2.onInitialize()` touched `PlayerLevelSystem` directly — the only
  references were inside event/command callback *bodies* (e.g. `LevelUpHandler`'s JOIN handler
  calling `PlayerLevelSystem.getPlayerLevel(...)`), which don't execute until a player actually
  joins. But Fabric's attachment deserialization (`EntityMixin.readEntityAttachments`, hooked
  into the entity's own NBT-read process) runs *during* that same join sequence, and by decompiling
  `AttachmentSerializingImpl` it's confirmed this deserialization looks up the `AttachmentType` by
  `Identifier` in a global registry at that exact moment — if our type wasn't registered yet
  (because `PlayerLevelSystem` hadn't loaded yet), the persisted `baum2:progression` NBT entry is
  silently dropped, and the next `getAttachedOrCreate` call falls back to `initializer()`'s fresh
  `PlayerProgressData` (level 1). That fresh default then gets written back on disconnect,
  **overwriting** the real saved progress with the reset value.
- This was confirmed, not guessed: dumped the raw NBT of an affected player file with a small
  ad-hoc NBT reader (gzip + manual tag parsing, no library needed) and found `.dat_old` (the
  previous save) correctly held `Level=5`, while the current `.dat` had been overwritten back to
  `Level=1` — proving the write path was always fine and the *read* was silently failing exactly
  once, then persisting that failure.
- Fix: added `PlayerLevelSystem.bootstrap()` (a no-op method whose only purpose is to force the
  class to load) and call it as the **first line** of `Baum2.onInitialize()`, guaranteeing the
  attachment type is registered before any world or player can possibly load.
- Verified end-to-end, not just via clean-boot logs this time: restored a known
  `Level=5`/`Experience=1` player save as the "current" `.dat`, relaunched with the fix in place,
  and confirmed (both via chat log and by re-dumping the NBT afterward) that the player correctly
  loaded back in at level 5 rather than resetting. The user then independently re-tested manually
  (gain XP → rebuild → relaunch → gain more XP → rebuild → relaunch) and confirmed it holds.
- General lesson for this codebase: **any `AttachmentType` (or similar static-registered thing
  whose registration must happen before world load) must be touched by an explicit call from
  `Baum2.onInitialize()`, not merely referenced inside a lambda/callback body that registers
  itself at init time but only executes later.** If a future attachment/registry addition shows
  the same "works this session, silently resets next load" symptom, check this first.

Earlier, still on `fischey_workbranch`: fixed a dev-environment gotcha that looked exactly like a
persistence bug (**but wasn't the full story** — see above): user reported "level resets when I
rebuild, but not on a plain restart of the same world."

- Root cause: Fabric Loom's `runClient` assigns a fresh random `PlayerNNN` username (and
  therefore a fresh UUID, since offline/dev UUIDs are derived from the username) on **every
  single launch** unless a fixed one is configured. Confirmed empirically —
  `run/usercache.json` had 29 distinct `PlayerNNN` entries after one afternoon of testing.
  Since progression is (correctly) stored per-player-UUID via the attachment added in the change
  below, every fresh `runClient` launch was really a brand new player joining, not the same
  player losing data. A plain "restart the world" without stopping/restarting the Gradle task
  keeps the same client session and thus the same random username, which is why *that* case
  looked fine and only the rebuild-then-relaunch case looked broken.
- Fix: `build.gradle`'s `loom { runs { client { ... } } }` now sets a fixed dev identity via
  `programArg("--username")` / `programArg("Baum2Dev")` / `programArg("--uuid")` /
  `programArg("<fixed-offline-uuid>")`. The UUID is the standard offline-player UUID for that
  username (`UUID.nameUUIDFromBytes(("OfflinePlayer:" + name).getBytes(UTF_8))`), computed once
  and hardcoded — it doesn't need to be recomputed, it just needs to stay stable.
  - Gotcha while implementing: `RunConfigSettings.getProgramArgs()` returns an **immutable**
    `List<String>` in Loom 1.17.13 — calling `.add(...)` on it throws
    `UnsupportedOperationException` with no message. Use the singular `programArg(String)`
    method (one call per argument) instead, not `programArgs.add(...)`.
- Verified: `runClient` log now shows `Setting user: Baum2Dev` and `Baum2Dev joined the game`
  on every launch, instead of a random name.
- Note for contributors: this only fixes identity stability inside *this* dev environment's
  `run/` directory. It has no effect on a real server/launcher session, where usernames/UUIDs
  come from the actual (online-mode or configured-offline) auth flow, not this dev-only arg
  injection — nothing to reconcile there.

Earlier, still on `fischey_workbranch`: persisted progression data via Fabric's Data Attachment
API instead of an in-memory `HashMap`:

- `PlayerProgressData` gained a `com.mojang.serialization.Codec<PlayerProgressData>` (via
  `RecordCodecBuilder`), replacing the old unused manual `writeNbt`/`readNbt` methods (they were
  dead code — never called anywhere).
- `PlayerLevelSystem` now declares `PROGRESSION`, an `AttachmentType<PlayerProgressData>`
  registered via `AttachmentRegistry.create(Identifier, Consumer<Builder<A>>)` with
  `.persistent(CODEC)`, `.copyOnDeath()`, and `.initializer(PlayerProgressData::new)`.
  `getPlayerProgress`/`savePlayerProgress`/`clearPlayerProgress` now delegate to
  `player.getAttachedOrCreate(PROGRESSION)` / `setAttached(...)` / `removeAttached(...)` instead
  of a UUID-keyed map.
- Why: attachments hook directly into vanilla's own player-NBT read/write cycle (the same one
  that saves inventory, health, etc.), so progression now survives disconnects, server restarts,
  and — via `copyOnDeath()` — player death, with no manual join/disconnect save/load code needed.
- Note: used `AttachmentRegistry.create(id, consumer)`, not `AttachmentRegistry.builder()` —
  the latter compiles but is `@Deprecated` (confirmed via `javap -v`, not just IDE guesswork).
  See "Attachment API reference" below.
- Verified: clean build, and a `runClient` boot showed `fabric-data-attachment-api-v1` loading
  and the resource-manager reload starting with no crash — mod init (where the attachment is
  registered) ran successfully. Not yet verified in an actual played session that data survives
  a real restart — see "Next recommended step".

Earlier, merged both active work branches into `master`:

- Merged `origin/jonas_workbranch` (fast-forward, commit `c979769`) — adds the four
  `.claude/agents/*.md` subagents, the "Project Agents" section in `CLAUDE.md`, and
  `docs/fabric-modding.md`.
- Merged `origin/fischey_workbranch` (merge commit, tip `75dd912`) — brings in the
  Mixin-based XP-orb suppression, the shared `VanillaXpFormula`, and the real-time
  client XP-bar sync over a custom S2C packet (see "Networking API reference" below).
- Only conflict was `HANDOFF.md` itself (expected — both branches update it independently);
  resolved by hand-merging both branches' state into this version. No source-code conflicts:
  the two branches touched disjoint files (`jonas_workbranch` only touched docs/agent
  definitions, `fischey_workbranch` only touched progression/networking/mixin code), and a
  manual overlap check (the `merge-integration-reviewer` agent wasn't loaded in this session)
  found no competing design assumptions between them either.
- Why: user requested combining both contributors' branches into `master` now that
  Fischey's XP-sync work had reached a working state.

Earlier, on `fischey_workbranch` before the merge: "Fixed real-time vanilla level/XP bar sync"
— added `VanillaXpFormula.java` to replace three drifting copies of the curve (the actual
cause of "level updates but progress bar doesn't"), and a custom S2C payload
(`networking/ExperienceSyncPayload.java` + `networking/Baum2Networking.java` server-side,
`networking/ClientNetworkingHandler.java` client-side) sent every tick from
`ProgressionTickHandler`. Root cause of the earlier bug: vanilla only pushes experience to the
client on join or on a server-side `setExperienceLevel()`/`addExperience()` call — setting
fields directly on `ServerPlayerEntity` every tick never reaches the client.

Earlier, on `jonas_workbranch` before the merge: `09eefd4` — "Add ip-naming-compliance-checker,
balance-reviewer, and merge-integration-reviewer agents", alongside the pre-existing
`fabric-docs-researcher`. Attempted to run the review agents against the progression system as
a first pass but couldn't — new custom agents aren't picked up until a fresh session starts
(see "Known limitation" above). Still not done; see "Next recommended step".

See `git log -p HANDOFF.md` for the full detail on earlier revisions.

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
- `PayloadTypeRegistry.playS2C().register(MyPayload.TYPE, MyPayload.CODEC)` — call from common
  code (`Baum2.onInitialize`), works for both logical sides since `splitEnvironmentSourceSets()`
  puts `main` on `client`'s classpath.
- Server sends: `ServerPlayNetworking.send(serverPlayerEntity, payload)`.
- Client receives: `ClientPlayNetworking.registerGlobalReceiver(MyPayload.TYPE, (payload, context) -> {...})`
  — the callback already runs on the client thread, no extra `execute()` wrapping needed.

If you add more custom payloads, follow `ExperienceSyncPayload.java` as the template.

## Attachment API reference (persistent per-player/entity data)

For any future "store custom data on a player/entity/block-entity/chunk that should survive
restarts" need, use `fabric-data-attachment-api-v1` (already a dependency) rather than a
hand-rolled `HashMap<UUID, ...>` + manual save/load. Reference implementation:
`progression/PlayerLevelSystem.java` (the `PROGRESSION` field) + `progression/PlayerProgressData.java`
(the `CODEC` field).

**Critical gotcha, learned the hard way (cost a full debugging cycle — see "Last change"):** the
class holding your `AttachmentType` static field must be force-loaded during
`Baum2.onInitialize()`, via an explicit call to some no-op method on it (see
`PlayerLevelSystem.bootstrap()`). If the class is only ever referenced from inside an event
callback *body* (lambda registered at init time but not executed until later), Java's lazy class
initialization means the `AttachmentType` won't actually be registered until the first time a
player triggers that callback — by which point Fabric may have already tried and failed to
deserialize that player's persisted attachment data (silently, no error/warning surfaced to the
log at the level we were checking), permanently losing it on the next save. Symptom: progress
"resets" but only sometimes, and looks exactly like a persistence failure even though writes are
working fine. If you add a new attachment and see this pattern, check that its owning class is
actually being force-loaded at init, not just referenced from a lambda.

Key classes, all in `net.fabricmc.fabric.api.attachment.v1` (Fabric's own names — stable across
mapping sets, unlike the networking classes above):
- `AttachmentType<A>` — the "key" for a piece of attached data.
- `AttachmentRegistry` — creates and registers an `AttachmentType`. Use
  `AttachmentRegistry.create(Identifier, Consumer<Builder<A>>)`, **not**
  `AttachmentRegistry.builder()` — the no-arg `builder()` is `@Deprecated` (verified via
  `javap -v`, the bytecode carries a real `Deprecated` attribute, not just a Javadoc note).
- `AttachmentRegistry.Builder<A>` methods used: `.persistent(Codec<A>)` (enables save/load),
  `.copyOnDeath()` (data survives player death/respawn), `.initializer(Supplier<A>)` (default
  value for entities that have never had this attachment set).
- `AttachmentTarget` — interface that `Entity` (and therefore `ServerPlayerEntity`), `BlockEntity`,
  `Chunk`, and `World` all implement (added via Fabric's build-time interface injection, so it's
  visible at compile time even though the actual `implements` is woven in by a Mixin at runtime —
  you don't need to do anything special to get `player.getAttached(...)` etc. to resolve).
  Methods used: `getAttachedOrCreate(type)`, `setAttached(type, value)`, `removeAttached(type)`.

The `.persistent(Codec<A>)` codec is `com.mojang.serialization.Codec<A>` (Mojang's
DataFixerUpper serialization library, already on the classpath via Minecraft itself) — **not**
the `PacketCodec` used for networking above; they're unrelated codec systems despite the similar
name. For a simple data class, build one with `RecordCodecBuilder.create(...)` as in
`PlayerProgressData.CODEC` — see that file for the exact pattern.

Persisted attachment data is stored as part of the target's own save data (e.g. for a player,
inside their `playerdata/<uuid>.dat`), so it's written/read automatically by vanilla's existing
save cycle — no manual `ServerPlayConnectionEvents.JOIN`/`DISCONNECT` save/load hooks needed for
persistence itself (those events are still useful for other things, e.g. this mod's
`LevelUpHandler` uses `JOIN` to prime its level-up-tracking cache, which is a separate concern
from persistence).

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
  by accident. If you add a new mixin config file, set `"compatibilityLevel": "JAVA_21"`
  explicitly rather than relying on IDE mixin-config generators, which may default to whatever
  JDK generated them locally.
- A local, gitignored SSH-style keypair (`baum2_key`, `baum2_key.pub`) has appeared in some
  working copies of this repo (root directory, gitignored via `baum2_key*`). It has never been
  committed. If you don't know what it's for, don't commit it and ask before deleting it —
  another contributor may depend on it locally.
- Our XP curve is centralized in `ProgressionCurve` (originally `VanillaXpFormula`, renamed once
  it stopped actually being vanilla's formula — see "Last change") — **do not** reimplement it
  elsewhere. The bug that took several iterations to fix (level updated, progress bar didn't) was
  ultimately caused by three separate copies of this formula drifting apart, not a display API
  issue. The curve's actual numbers are also deliberately rebalanced away from vanilla's own —
  see "Last change" for the current formula and why.
- Vanilla XP orb drops from hostile mobs are disabled via `LivingEntityMixin`. Experience bottles
  were reported to still spawn orbs in an earlier session — not re-verified since; low priority.
- Progression persistence uses Fabric's Data Attachment API (see "Attachment API reference"
  above) — **do not** reintroduce a manual `HashMap<UUID, PlayerProgressData>`, that was the
  previous approach and it's why data was lost on restart.

## Next recommended step

1. **Not yet visually confirmed in-game**: open the new Life/Mana bars in an actual play
   session and eyeball them against `docs/visual-style-guide.md` — position (should sit where
   vanilla hearts used to, armor bar pushed up above the Mana bar), colors, and fill-ratio
   behavior as you take damage/heal. This environment has no GUI-automation tool for the native
   Minecraft window, so this genuinely hasn't been seen, only logically verified (clean boot,
   clean world-join, no crashes) — see "Last change" above.
2. Still not visually confirmed from before: the vanilla XP bar animates smoothly as
   `/baum2 addxp <n>` is run repeatedly (not just on level-up), and the level number matches
   `/baum2 level`. Believed correct from a code standpoint but nobody has watched it happen.
3. In a fresh session where the custom subagent types actually resolve (see "Known limitation"
   above — untested whether this environment-specific issue affects other setups too): run
   `balance-reviewer` on the progression system's XP curve/mob-reward formula (already run via
   the workaround for the new Life/Mana formulas — see "Last change" — but not yet for the
   original XP curve) and `ip-naming-compliance-checker` on existing player-facing strings
   (command output, level-up broadcast text) — neither has been reviewed yet.
4. Merge `fischey_workbranch` back into `master` — this session's commits (Life/Mana/HUD, plus
   the earlier XP-rebalance commits) are still only on `fischey_workbranch`.
5. Mana has no consumers yet by design ("future use" per the user's request) — the first skill
   that spends Mana should read `VitalsCurve.getMaxMana`/`PlayerProgressData.getMana` rather
   than inventing a parallel resource field.
6. A class-name banner + level-diamond badge (as pictured in the reference image that prompted
   this change) was explicitly deferred — no `classes/` package exists yet. When that work
   starts, it should sit *above* the Life/Mana bars per `docs/visual-style-guide.md`'s note on
   this, and will need its own `HudElementRegistry`/`HudStatusBarHeightRegistry` entries.
7. Remaining Priority 1 items per `CLAUDE.md`: first custom item, first weapon, first active
   skill with a cooldown manager, first world-event block. Consult `fabric-docs-researcher` /
   `docs/fabric-modding.md` before implementing any of these if the relevant Fabric API is
   unclear.
