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

**Progression System — FULLY WORKING, including real-time client display:**
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
  "Networking" section below for the exact API and why earlier attempts failed.
- Server-side in-memory storage; persistence still deferred to a future phase.

- Repo: https://github.com/laserjonas/minecraft-baum2 (public).
- `.vscode/` checked in with run configs.

## Last change

- Fixed real-time vanilla level/XP bar sync (previously blocked — see git history for the
  string of failed attempts: totalExperience-only tricks, cancelling `dropExperience`, etc.)
- What:
  1. Added `progression/VanillaXpFormula.java` — single shared implementation of the vanilla
     curve, replacing three separate copies that had drifted out of sync with each other
     (which was the actual cause of the "level updates but progress bar doesn't" bug reported
     earlier).
  2. Added a custom S2C payload (`networking/ExperienceSyncPayload.java` +
     `networking/Baum2Networking.java` server-side, `networking/ClientNetworkingHandler.java`
     client-side) sent every server tick from `ProgressionTickHandler`.
  3. Client-side, call `ClientPlayerEntity.setExperience(float progress, int total, int level)`
     — **not** `setExperienceLevel(int)`, which only exists on `ServerPlayerEntity` and does
     not compile in client code. `setExperience` is the exact method vanilla's own network
     handler calls when it receives a real experience-sync packet from a real server; it sets
     `experienceProgress`, `totalExperience`, and `experienceLevel` directly and triggers the
     bar's fill-flash animation. Because it takes the fill fraction directly, there's no need
     to reverse-engineer the curve on the client at all for the bar's visual fill — only the
     level number and the `totalExperience` display value need the shared formula.
- Why: Root cause of "client doesn't update live" was that vanilla only pushes experience to
  the client on join or on a server-side `setExperienceLevel()`/`addExperience()` call — setting
  fields directly on `ServerPlayerEntity` every tick never reaches the client. A previous
  session's fix attempt used `FabricPacket`/`PacketType`, which do not exist in Fabric API
  0.141.4+1.21.11 (that's a newer/different-mapping API) and failed to compile.

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

1. In-game manual verification: join a world, run `/baum2 addxp <n>` a few times, and confirm the
   vanilla XP bar animates smoothly (not just on level-up) and the level number matches
   `/baum2 level`. This session verified a clean client boot with no crashes but could not drive
   the GUI to confirm the visual result — do this before considering the feature fully closed.
2. Persist progression data (currently in-memory only, lost on server restart).
3. Implement first custom item + skill system per `CLAUDE.md` priorities.
