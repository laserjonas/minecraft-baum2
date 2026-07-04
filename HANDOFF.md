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
- **Class System v1 — command-driven, names/values now reviewed once, still needs in-game
  player verification (see "Next recommended step"):**
  - New `classes/` package: `PlayerClass` (enum: `EISENWAECHTER`, `SCHATTENLAEUFER`,
    `RUNENWIRKER`, `WESENSWAHRER`), `ClassDefinition` (record: display name, description,
    one passive `EntityAttributeModifier` bonus per class), `ClassRegistry` (static lookup),
    `ClassManager` (persistence + apply/remove bonus + its own join listener).
  - Commands: `/baum2 class list`, `/baum2 class info [<class>]`, `/baum2 class select <class>`
    — added to `commands/Baum2Commands.java` additively (existing `addxp`/`level` untouched).
  - Persistence via **Fabric's Attachment API** (`fabric-data-attachment-api-v1`, pinned at
    `1.8.48+eed0806f3e` via the project's `fabric-api 0.141.4+1.21.11`), not a custom NBT
    mixin — `ClassManager.SELECTED_CLASS` is a persistent, `copyOnDeath()` `AttachmentType`.
    See `docs/fabric-modding.md` "Player data / attributes" for full API details.
  - Passive bonuses, **updated after the balance-reviewer pass below**: Eisenwächter +4 max
    health, Schattenläufer +10% movement speed, Runenwirker +1 luck, Wesenswahrer +10%
    knockback resistance — each via a stable-`Identifier` `EntityAttributeModifier`, swapped
    cleanly on reselection/rejoin (`ClassManager.applyBonus` always `removeModifier` before
    `addPersistentModifier`, since persistent modifiers are already restored from entity NBT
    by the time a returning player's `JOIN` event fires — skipping the removal step throws).
  - **4th class renamed `Seelenhüter` → `Wesenswahrer`** — `ip-naming-compliance-checker`
    (run for real this session, workspace-root bug from before is fixed) flagged
    `Seelenhüter` as an exact, word-for-word match to *Echo of Soul*'s (Gamigo) player
    character title ("...du wurdest durch die Götter zum Seelenhüter erwählt"), used in the
    same "chosen guardian of souls" framing — not just a generic fantasy word. Renamed in
    `PlayerClass.java`, `ClassRegistry.java` (display name, description, bonus identifier),
    and in `MASTERPROMPT.md`'s own example lists (the term originated there, not in this
    commit — the brief itself needed the fix, not just the code). Lower-confidence, not
    acted on: `Runenwirker` is somewhat close to LOTRO's "Runenbewahrer" class — worth
    watching if this class's skill kit gets fleshed out later, not a hard conflict today.
  - **Wesenswahrer's bonus attribute changed from `MAX_ABSORPTION` to
    `KNOCKBACK_RESISTANCE`** — `balance-reviewer` found the original +4 max-absorption bonus
    was a complete no-op: `MAX_ABSORPTION` is only a ceiling, and nothing in this mod grants
    absorption hearts to fill it (only vanilla golden apples/totems do, and they bundle their
    own temporary cap boost, so even that vanilla interaction never touched our permanent
    one). Swapped to `+0.10 KNOCKBACK_RESISTANCE` (`ADD_VALUE`), which — unlike absorption —
    is unconditionally live the moment the modifier is applied, matching how the other three
    classes' bonuses already behave.
  - Deliberately does not touch `events/LevelUpHandler.java` or
    `events/ProgressionTickHandler.java` (Fischey's most actively-changed files) — class-join
    resync lives in `ClassManager`'s own independent `ServerPlayConnectionEvents.JOIN`
    listener instead of being added to `LevelUpHandler`'s.
  - **Verified so far**: build passes (including after the naming/balance fixes above); a
    real (non-GUI) dedicated server boots cleanly with the new code (confirms the
    `AttachmentType`/codec registration doesn't crash at class-init); `/baum2 class list` was
    exercised over live RCON against that real server and returns correct data for all 4
    classes. **Not yet verified**: the actual player path (`select`, the attribute modifier
    being applied/visible via vanilla `/attribute`, surviving relogin, surviving
    death/respawn via `copyOnDeath()`) — this needs a real graphical client, which wasn't
    available to automate this session; the user is verifying this manually in parallel. See
    "Next recommended step".
  - **Known, logged, not fixed (design/judgment calls, not bugs)**: (1) free, instant,
    zero-cooldown class reselection lets a player swap classes contextually to capture all
    four bonuses' benefit over a session — already called out as deliberately deferred, just
    reconfirmed concretely this session (`ClassManager.selectClass` has no cost/cooldown
    guard at all); (2) Runenwirker's +1 luck is live (feeds vanilla loot/fishing rolls) but
    the mod has no custom loot tables yet for it to meaningfully act on, so it's
    comparatively weak until a loot system exists.
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
  - **`balance-reviewer` ran against this system for the first time this session.** Curve
    itself is fine — verified continuous and monotonic across both segment boundaries
    (level 15/16 and 31/32), no formula bug. One fix applied, several findings logged:
    - **Fixed**: `PlayerProgressData`'s default/NBT-fallback `experienceForNextLevel` was
      hardcoded to `100`, but the formula's actual level-1→2 requirement is `9` — an 11x
      mismatch that only affected a brand-new player's very first level-up (every later
      level already recomputes from the formula correctly). Now computed via
      `VanillaXpFormula.getXpRequiredForLevel(level + 1)` in both places instead of a magic
      number.
    - **Logged, not fixed (design call)**: mob XP is lump-sum (14-160 XP per kill) against a
      curve whose early levels need as little as 9-15 XP, so a single strong kill (e.g. a
      Wither at 160 XP) can vault a level-2 character to level 10 in one hit. Needs a
      decision: scale mob XP down at low character levels, or steepen the curve's early
      segment for this mod's lump-sum granularity (the curve's numbers were originally sized
      for vanilla's few-XP-per-orb pickup model, not one integer per kill).
    - **Logged, not fixed (dead code)**: `ExperienceManager.getMaxLevel()` declares a cap of
      100 but nothing anywhere enforces it — the level-up loop has no upper bound today.
      Decide whether to actually enforce it or remove the unused constant. (Very low-priority
      latent detail if left uncapped forever: the `long`→`int` cast in
      `PlayerLevelSystem.syncVanillaLevelDisplay` would overflow around level ~21,850 — not
      reachable in practice.)
    - **Minor**: `MobDeathHandler.calculateXpReward`'s `maxHealth / 2` truncates for odd
      `maxHealth` values (loses 0.5 XP) — no current vanilla hostile mob has odd health, so
      this doesn't manifest yet, but would silently apply to any future custom mob that does.
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
  `balance-reviewer` and `ip-naming-compliance-checker` have now both been run for real
  (workspace-root bug below is fixed) against the Class System and the progression XP
  curve/mob-reward formula — see the "Class System v1" and "Progression System" bullets above
  for their findings and which ones were fixed vs. logged for a later decision.
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

Ran `ip-naming-compliance-checker` and `balance-reviewer` for real against Class System v1
(the workspace-root bug that blocked them last session is confirmed fixed — both loaded and
ran normally this time), then applied the straightforward fixes and logged the judgment
calls:
- Renamed `Seelenhüter` → `Wesenswahrer` everywhere (`PlayerClass.java`, `ClassRegistry.java`,
  and `MASTERPROMPT.md`'s own example lists) after the naming check found it was an exact,
  word-for-word match to *Echo of Soul*'s player-character title, not just a generic fantasy
  word.
- Swapped Wesenswahrer's passive bonus from `MAX_ABSORPTION` (found to be a complete no-op —
  nothing in the mod grants absorption hearts) to `+0.10 KNOCKBACK_RESISTANCE`, which is
  immediately live like the other three classes' bonuses.
- Fixed `PlayerProgressData`'s hardcoded level-1→2 XP threshold (`100`) to match
  `VanillaXpFormula`'s actual value (`9`) instead of drifting from it.
- Logged for a later decision, not fixed: mob XP (lump-sum, 14-160/kill) can vault a
  low-level character through many levels at once against the curve's small early
  requirements; `ExperienceManager.getMaxLevel()`'s 100-level cap is declared but never
  enforced; free/instant class reselection (already known) lets a player capture all four
  classes' bonuses contextually; Runenwirker's luck bonus has no loot system to act on yet.
- Build verified passing after each change (`gradlew build -q`, no errors).
- Why: user asked for a read of all project docs and a plan for what's next. Agreed plan: the
  user is manually verifying the Class System's in-game player path (select/attribute/
  relog/death-respawn) in parallel, while this session ran the two review agents that were
  blocked last time and fixed what was unambiguous.

Earlier, on `jonas_workbranch`: added Class System v1 (`classes/` package: `PlayerClass`,
`ClassDefinition`, `ClassRegistry`, `ClassManager`) plus `/baum2 class list|info|select`
commands — see "Current state" above for full detail. Why: user wants the Progression System
to become meaningful now that leveling works, by giving players a class/build to level into.
Hit the workspace-root agent bug above partway through (see "Known limitation" — none of the
five project agents were loadable this session), so the IP-naming check, the Fischey-branch
overlap check, and the Fabric API research (Attachment API, `EntityAttributeModifier`) were
all done manually against `docs/fabric-modding.md`'s and `CLAUDE.md`'s own stated methodology
instead of via the actual subagents — not skipped, but worth re-running for real once the
workspace root is fixed. Verification was likewise partial: build + a real dedicated server +
live RCON confirmed the command surface and data; the player-specific path
(select/attribute-modifier/persistence) needs a real graphical client — see "Next recommended
step".

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

1. **Commit the naming/balance fixes from this session** (rename + Wesenswahrer bonus swap +
   level-1 XP fix, all build-verified) — not yet committed as of this handoff, pending user
   confirmation to commit/push.
2. **In-game manual verification of the Class System (highest priority — unverified core
   path)**: join a world as a real player and run through: `/baum2 class select eisenwaechter`
   → `/attribute @s minecraft:max_health modifier value get baum2:class_bonus/eisenwaechter_max_health`
   (expect `4.0`) → `/attribute @s minecraft:max_health get` (expect `24.0`) → disconnect and
   rejoin (confirm `/baum2 class info` still reports the class and the modifier query still
   returns exactly one value, not duplicated/erroring) → `/kill @s` and respawn (confirm the
   class/modifier survive — this is the specific test that `.copyOnDeath()` is wired
   correctly). Also spot-check the renamed `wesenswahrer` (new attribute is
   `minecraft:generic.knockback_resistance`, expect `0.1`). `/baum2 class list` and the
   player-less command-guard behavior are already confirmed working via a real dedicated
   server + RCON; this step is the remaining gap. (In progress as of this handoff — user is
   running this manually in parallel with the rest of this session's work.)
3. **Human decision needed on the balance-reviewer's logged (not auto-fixed) findings** — see
   "Class System v1" and "Progression System" bullets above for full detail: (a) should mob
   XP scale down at low character levels, or should the curve's early segment be steeper for
   lump-sum rewards; (b) should the 100-level cap be actually enforced or should the unused
   `getMaxLevel()` constant be removed; (c) should class reselection get a cost/cooldown now
   or stay free for longer; (d) is Runenwirker's luck bonus fine to ship before a loot system
   exists to make it matter, or should it wait.
4. Run `merge-integration-reviewer` before merging `jonas_workbranch` back to `master`, given
   Fischey's concurrent progression work.
5. In-game manual verification of the Progression System (older, still-pending item): join a
   world, run `/baum2 addxp <n>` a few times, and confirm the vanilla XP bar animates smoothly
   (not just on level-up) and the level number matches `/baum2 level`.
6. Persist progression data (currently in-memory only, lost on server restart) — the Class
   System's `ClassManager` now demonstrates the recommended pattern (Fabric's Attachment API)
   for this exact problem, see `docs/fabric-modding.md`.
7. Next Class System iteration (deliberately out of scope for v1): the Skill-System per
   `MASTERPROMPT.md`'s own priority order, multiple bonuses per class, and/or a respec
   cost/cooldown (see point 3c above).
8. Remaining Priority 1 items per `CLAUDE.md`: first custom item, first weapon, first active
   skill with a cooldown manager, first world-event block. Consult `fabric-docs-researcher` /
   `docs/fabric-modding.md` before implementing any of these if the relevant Fabric API is
   unclear. Use `graphics-designer` for the texture/model/icon each of these will need.
