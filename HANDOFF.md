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
- Four subagents under `.claude/agents/` (shared via git, so both contributors get them):
  `fabric-docs-researcher` (Fabric/MC API research -> `docs/fabric-modding.md`),
  `ip-naming-compliance-checker` (reviews new names/text against IP/naming rules),
  `balance-reviewer` (internal-consistency/exploit review of numeric balance values),
  `merge-integration-reviewer` (pre-merge overlap/design-conflict check between branches).
  All four report findings only — they don't edit files. See `CLAUDE.md` -> "Project Agents"
  for exact trigger conditions; use them proactively, don't wait to be asked.
  Still not yet run against the progression system's balance values / player-facing strings —
  see "Next recommended step".
- **Known limitation**: a running Claude Code session loads its available agent list at
  startup, so newly added `.claude/agents/*.md` files aren't picked up mid-session — they
  become available the next time a session starts fresh (restart, or a fresh session after
  pulling). If an agent invocation fails with "Agent type not found" right after one was
  added, that's why — not a bug in the agent definition.

## Last change (on `fischey_workbranch`, not yet merged to `master`)

Persisted progression data via Fabric's Data Attachment API instead of an in-memory `HashMap`:

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
- Vanilla XP curve is centralized in `VanillaXpFormula` — **do not** reimplement it elsewhere.
  The bug that took several iterations to fix (level updated, progress bar didn't) was ultimately
  caused by three separate copies of this formula drifting apart, not by a display API issue.
- Vanilla XP orb drops from hostile mobs are disabled via `LivingEntityMixin`. Experience bottles
  were reported to still spawn orbs in an earlier session — not re-verified since; low priority.
- Progression persistence uses Fabric's Data Attachment API (see "Attachment API reference"
  above) — **do not** reintroduce a manual `HashMap<UUID, PlayerProgressData>`, that was the
  previous approach and it's why data was lost on restart.

## Next recommended step

1. In-game manual verification, two things now pending together since neither has been visually
   confirmed yet in a real play session:
   - The vanilla XP bar animates smoothly as `/baum2 addxp <n>` is run repeatedly (not just on
     level-up), and the level number matches `/baum2 level`.
   - Progression survives a real disconnect/reconnect and a full server restart (gain some XP,
     stop the server, restart it, rejoin, confirm `/baum2 level` still shows the same values).
   Both are believed correct from a code-and-clean-boot-log standpoint but nobody has driven the
   actual GUI to watch either happen yet — do this before considering either feature fully closed.
2. In a fresh session (so the four agents are actually loaded): run `balance-reviewer` on the
   progression system's XP curve/mob-reward formula and `ip-naming-compliance-checker` on
   existing player-facing strings (command output, level-up broadcast text) — neither has been
   reviewed yet.
3. Merge `fischey_workbranch` back into `master` once step 1 above confirms this session's
   persistence work is solid (it hasn't been merged yet — this session's commit is still only
   on `fischey_workbranch`).
4. Remaining Priority 1 items per `CLAUDE.md`: first custom item, first weapon, first active
   skill with a cooldown manager, first world-event block. Consult `fabric-docs-researcher` /
   `docs/fabric-modding.md` before implementing any of these if the relevant Fabric API is
   unclear.
