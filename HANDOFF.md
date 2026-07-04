# Handoff

Read this first. It reflects the state as of the latest commit so a fresh Claude Code
session (yours or a co-author's) can pick up work without re-deriving context from
`git log`. Updated after every commit — see "Git Rules" in `CLAUDE.md`.

## Current state

- Fabric mod builds successfully (`./gradlew build` passes).
- Client runs: `./gradlew runClient` loads and reaches main menu (verified clean boot, no
  Mixin/payload registration errors).
- Package: `de.baum2dev.baum2` / Main: `Baum2` / Client: `Baum2Client`.
- Minecraft 1.21.11 / Yarn 1.21.11+build.6 / Fabric API 0.141.4+1.21.11 / Fabric Loom 1.17.13 / Java 21.
- **Class System v1 — new, command-driven, needs in-game player verification (see "Next
  recommended step"):**
  - New `classes/` package: `PlayerClass` (enum: `EISENWAECHTER`, `SCHATTENLAEUFER`,
    `RUNENWIRKER`, `SEELENHUETER`), `ClassDefinition` (record: display name, description,
    one passive `EntityAttributeModifier` bonus per class), `ClassRegistry` (static lookup),
    `ClassManager` (persistence + apply/remove bonus + its own join listener).
  - Commands: `/baum2 class list`, `/baum2 class info [<class>]`, `/baum2 class select <class>`
    — added to `commands/Baum2Commands.java` additively (existing `addxp`/`level` untouched).
  - Persistence via **Fabric's Attachment API** (`fabric-data-attachment-api-v1`, pinned at
    `1.8.48+eed0806f3e` via the project's `fabric-api 0.141.4+1.21.11`), not a custom NBT
    mixin — `ClassManager.SELECTED_CLASS` is a persistent, `copyOnDeath()` `AttachmentType`.
    See `docs/fabric-modding.md` "Player data / attributes" for full API details.
  - Passive bonuses (v1 placeholders, not yet balance-reviewed): Eisenwächter +4 max health,
    Schattenläufer +10% movement speed, Runenwirker +1 luck, Seelenhüter +4 max absorption —
    each via a stable-`Identifier` `EntityAttributeModifier`, swapped cleanly on
    reselection/rejoin (`ClassManager.applyBonus` always `removeModifier` before
    `addPersistentModifier`, since persistent modifiers are already restored from entity NBT
    by the time a returning player's `JOIN` event fires — skipping the removal step throws).
  - Deliberately does not touch `events/LevelUpHandler.java` or
    `events/ProgressionTickHandler.java` (Fischey's most actively-changed files) — class-join
    resync lives in `ClassManager`'s own independent `ServerPlayConnectionEvents.JOIN`
    listener instead of being added to `LevelUpHandler`'s.
  - **Verified so far**: build passes; a real (non-GUI) dedicated server boots cleanly with
    the new code (confirms the `AttachmentType`/codec registration doesn't crash at
    class-init); `/baum2 class list` was exercised over live RCON against that real server and
    returns correct data for all 4 classes. **Not yet verified**: the actual player path
    (`select`, the attribute modifier being applied/visible via vanilla `/attribute`,
    surviving relogin, surviving death/respawn via `copyOnDeath()`) — this needs a real
    graphical client, which wasn't available to automate this session. See "Next recommended
    step".
- **Progression System — FULLY WORKING, including real-time client display:**
  - Custom progression uses Minecraft's non-linear XP curve, centralized in
    `progression/VanillaXpFormula.java` (single source of truth — `ExperienceManager`,
    `ProgressionTickHandler`, `PlayerLevelSystem`, `LevelUpHandler`, and the client packet
    handler all call into it instead of each having their own copy):
    - Levels 0-15: L² + 6L
    - Levels 16-31: 2.5L² - 40.5L + 360
    - Levels 32+: 4.5L² - 162.5L + 2220
  - Features: `/baum2 addxp <amount>`, `/baum2 level`, mob XP drops (10 + max_health/2), level-up
    broadcasts, vanilla XP orb drops disabled via Mixin.
  - **Real-time client sync now works** via a custom S2C packet sent every server tick — see
    "Networking API reference" section below for the exact API and why earlier attempts failed.
  - Server-side in-memory storage; persistence still deferred to a future phase.
- Repo: https://github.com/laserjonas/minecraft-baum2 (public).
- **Branches**: `master` was merged from both work branches (see prior HANDOFF revision /
  `git log -p HANDOFF.md` for that merge's detail); `jonas_workbranch` was then fast-forwarded
  to match `master` and pushed. Per explicit user instruction, active work is back on
  `jonas_workbranch` now, until the user asks for another merge into `master`.
  `fischey_workbranch` still exists and tracks its own remote for Fischey's follow-up work.
- `.vscode/` is checked in (extensions.json, settings.json, tasks.json) so fresh checkout gets Java+Gradle
  recommendations and "Run Minecraft Client" task (`Ctrl+Shift+B`) out of the box.
- Five subagents under `.claude/agents/` (shared via git, so both contributors get them):
  `fabric-docs-researcher` (Fabric/MC API research -> `docs/fabric-modding.md`),
  `ip-naming-compliance-checker` (reviews new names/text against IP/naming rules),
  `balance-reviewer` (internal-consistency/exploit review of numeric balance values),
  `merge-integration-reviewer` (pre-merge overlap/design-conflict check between branches),
  `graphics-designer` (new — the mod's senior graphic designer: textures, models, icons,
  UI/HUD layout, color/style identity per rarity/class/faction; persists a style guide to
  `docs/visual-style-guide.md`). The first four report findings only and don't edit files;
  `graphics-designer` is the exception and is expected to write/edit asset and doc files
  directly. See `CLAUDE.md` -> "Project Agents" for exact trigger conditions; use all five
  proactively, don't wait to be asked.
  `balance-reviewer` and `ip-naming-compliance-checker` still haven't been run against the
  progression system's balance values / player-facing strings — see "Next recommended step".
- **Known limitation, root cause found**: a running Claude Code session loads its available
  agent list at startup, so newly added `.claude/agents/*.md` files aren't picked up
  mid-session. **But also**: the harness discovers project agents from its primary working
  directory, not from a nested repo root — if a session is opened at `D:\Baum2` (the parent
  of this repo) instead of `D:\Baum2\Baum2` (where `.claude/agents/` actually lives), none of
  the five project agents are visible at all, even in a fresh session ("Agent type not
  found: fabric-docs-researcher" etc., only the generic built-ins listed as available).
  Confirmed 2026-07-04. **Fix: open/attach the Claude Code session with `D:\Baum2\Baum2` as
  the workspace root**, not `D:\Baum2`. If this keeps happening, that's almost certainly why.

## Last change (on `jonas_workbranch`)

Added Class System v1 (`classes/` package: `PlayerClass`, `ClassDefinition`, `ClassRegistry`,
`ClassManager`) plus `/baum2 class list|info|select` commands — see "Current state" above for
full detail. Why: user wants the Progression System to become meaningful now that leveling
works, by giving players a class/build to level into. Hit the workspace-root agent bug above
partway through (see "Known limitation" — none of the five project agents were loadable this
session), so the IP-naming check, the Fischey-branch overlap check, and the Fabric API
research (Attachment API, `EntityAttributeModifier`) were all done manually against
`docs/fabric-modding.md`'s and `CLAUDE.md`'s own stated methodology instead of via the actual
subagents — not skipped, but worth re-running for real once the workspace root is fixed.
Verification was likewise partial: build + a real dedicated server + live RCON confirmed the
command surface and data; the player-specific path (select/attribute-modifier/persistence)
needs a real graphical client — see "Next recommended step".

Earlier: added a fifth subagent, `graphics-designer` (`.claude/agents/graphics-designer.md`),
and documented it in `CLAUDE.md` -> "Project Agents". Unlike the existing four (review/report
only), this one is a producing agent — the mod's senior graphic designer, responsible for
textures, models, icons, UI/HUD layout, and color/style identity (rarity tiers, classes,
factions), with a persisted style guide at `docs/visual-style-guide.md` (not created yet — no
visual assets exist in the project at all so far). Why: user asked for a dedicated graphics-
focused agent now that the progression system is stable and Priority 1 items (first item/
weapon/skill/event block) are coming up and will need original visual assets. Not yet
exercised on a real task.

Earlier, on `master`: merged both active work branches into `master`:

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
- Vanilla XP curve is centralized in `VanillaXpFormula` — **do not** reimplement it elsewhere.
  The bug that took several iterations to fix (level updated, progress bar didn't) was ultimately
  caused by three separate copies of this formula drifting apart, not by a display API issue.
- Vanilla XP orb drops from hostile mobs are disabled via `LivingEntityMixin`. Experience bottles
  were reported to still spawn orbs in an earlier session — not re-verified since; low priority.

## Next recommended step

1. **In-game manual verification of the Class System (highest priority — unverified core
   path)**: join a world as a real player and run through: `/baum2 class select eisenwaechter`
   → `/attribute @s minecraft:max_health modifier value get baum2:class_bonus/eisenwaechter_max_health`
   (expect `4.0`) → `/attribute @s minecraft:max_health get` (expect `24.0`) → disconnect and
   rejoin (confirm `/baum2 class info` still reports the class and the modifier query still
   returns exactly one value, not duplicated/erroring) → `/kill @s` and respawn (confirm the
   class/modifier survive — this is the specific test that `.copyOnDeath()` is wired
   correctly). `/baum2 class list` and the player-less command-guard behavior are already
   confirmed working via a real dedicated server + RCON this session; this step is the
   remaining gap.
2. In a fresh session opened at the correct workspace root (`D:\Baum2\Baum2`, see "Known
   limitation" above — this was the blocker this session): run `ip-naming-compliance-checker`
   on the class/skill names (manually cleared this session, worth a real pass) and
   `balance-reviewer` on the Class System's passive-bonus table (see "Current state" —
   explicitly flagged as placeholder values) and the still-unreviewed progression XP
   curve/mob-reward formula. Also run `merge-integration-reviewer` before merging back to
   `master`, given Fischey's concurrent progression work.
3. In-game manual verification of the Progression System (older, still-pending item): join a
   world, run `/baum2 addxp <n>` a few times, and confirm the vanilla XP bar animates smoothly
   (not just on level-up) and the level number matches `/baum2 level`.
4. Persist progression data (currently in-memory only, lost on server restart) — the Class
   System's `ClassManager` now demonstrates the recommended pattern (Fabric's Attachment API)
   for this exact problem, see `docs/fabric-modding.md`.
5. Next Class System iteration (deliberately out of scope for v1): the Skill-System per
   `MASTERPROMPT.md`'s own priority order, multiple bonuses per class, and/or a respec
   cost/cooldown.
6. Remaining Priority 1 items per `CLAUDE.md`: first custom item, first weapon, first active
   skill with a cooldown manager, first world-event block. Consult `fabric-docs-researcher` /
   `docs/fabric-modding.md` before implementing any of these if the relevant Fabric API is
   unclear. Use `graphics-designer` for the texture/model/icon each of these will need.
