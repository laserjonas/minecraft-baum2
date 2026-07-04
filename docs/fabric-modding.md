# Fabric Modding Reference — Minecraft 1.21.11

Living reference for Minecraft 1.21.11 / Fabric Loader / Fabric API / Fabric Loom / Yarn /
Mixin facts relevant to this project. Maintained by the `fabric-docs-researcher` agent (see
`.claude/agents/fabric-docs-researcher.md`) — both contributors should consult it before
guessing at an API, and ask the agent (not just search blindly) whenever something modding-
related is unclear. See `CLAUDE.md` for the rule.

Append new findings under the relevant section. Don't delete verified entries just because
they look obvious in hindsight — they're here so nobody re-derives them a second time.

## Project's pinned versions

- Minecraft: `1.21.11`
- Yarn mappings: `1.21.11+build.6`
- Fabric Loader: `0.19.3`
- Fabric API: `0.141.4+1.21.11`
- Fabric Loom: `1.17.13`
- Java target: `21`

## Version / mapping gotchas

- **Minecraft 26.2 has no Yarn mappings yet.** 26.2 is the actual latest stable Minecraft
  release (Mojang's year.release versioning), but as of project setup, the Yarn mappings
  project had not published mappings for it — `https://meta.fabricmc.net/v2/versions/yarn/26.2`
  returned an empty array. Building against `minecraft_version=26.2` with
  `yarn_mappings=1.21.11+build.6` fails with `IllegalArgumentException: Could not find
  namespace "official" in provided tiny tree` during source remapping — the mappings simply
  don't match the game version. Fix: use the newest Minecraft version that *does* have
  published Yarn mappings (currently 1.21.11), and revisit once Yarn catches up. Check
  `https://meta.fabricmc.net/v2/versions/yarn/<version>` before bumping `minecraft_version`.
- **Fabric API artifact versions are per-Minecraft-version**, not global — check
  `https://maven.fabricmc.net/net/fabricmc/fabric-api/fabric-api/maven-metadata.xml` for the
  exact `+<mc_version>` suffix. For 1.21.11 the last/highest is `0.141.4+1.21.11`; after that
  the project moved to the `26.x` version line.
- **Fabric Loom `1.17-SNAPSHOT` is a floating version** — it resolved to a timestamped
  snapshot build (jar version `1.17.13` at time of writing) rather than a fixed release. Pin
  the actual stable release (`1.17.13`) instead of tracking `-SNAPSHOT`, for build
  reproducibility.
- **Loom 1.17.13 requires Gradle's plugin `api-version` >= 9.5.0.** Gradle 9.2.1 fails to
  resolve the plugin variant at all ("No matching variant of net.fabricmc:fabric-loom...").
  Use Gradle 9.5.1+ in `gradle-wrapper.properties`.

## Gradle / Loom

- `./gradlew genSources` (or `gradlew.bat genSources` on Windows) decompiles Minecraft +
  Fabric API sources — the most authoritative reference for exact method signatures on this
  version, more reliable than any wiki page for a brand-new Minecraft release.
- `def targetJavaVersion` in `build.gradle`: setting `toolchain.languageVersion` only when
  `JavaVersion.current() < javaVersion` means Loom's `runClient`/`runServer` `JavaExec` tasks
  can silently pick up a different, higher-versioned JDK auto-detected on the machine (e.g. a
  stray IDE-managed JDK 25) instead of the pinned target. Pin `toolchain.languageVersion`
  **unconditionally** to defend against this — see `build.gradle` history (`2405ca7`).
- Mixin config `compatibilityLevel` (in `*.mixins.json`) must match the actual toolchain
  Java version (`JAVA_21` for this project), not whatever an IDE mixin-config generator
  defaulted to locally — mismatches here can cause confusing runtime failures.

## Mixins

_(Fischey is actively working with Mixins on `fischey_workbranch` — e.g. redirecting
`ExperienceOrbEntity.spawn` to block vanilla XP drops. Add SpongePowered Mixin API findings
here as they come up: `@Inject`, `@Redirect`, `@ModifyVariable`, shadow fields, etc.)_

## Registries

_(Nothing registered yet beyond the mod itself — add `Registry`/`Registries` API findings
here once `registry/ModItems` etc. exist.)_

## Events (Fabric API)

- `ServerPlayConnectionEvents.JOIN` / `.DISCONNECT` — used in
  `events/LevelUpHandler.java` to track a player's last-known level across sessions.
- `ServerLivingEntityEvents.AFTER_DEATH` — used in `events/MobDeathHandler.java` to grant XP
  when a `HostileEntity` dies to a `ServerPlayerEntity` attacker.
- `CommandRegistrationCallback.EVENT` — used in `commands/Baum2Commands.java` to register the
  `/baum2` command tree via Brigadier.

## Rendering / HUD

- `HudRenderCallback.EVENT` (client-side) was used for a custom XP HUD overlay
  (`ui/ProgressionHud.java`) but text rendering proved unreliable across several attempts
  (see commit history `caa8325`..`9c6c202`). The project switched to driving Minecraft's
  built-in XP/level bar via `ServerPlayerEntity.setExperienceLevel(...)` instead of a custom
  HUD. If a custom HUD is revisited, research the correct 1.21.11 text-drawing API
  (`DrawContext` methods) before reattempting — this is exactly the kind of thing this doc
  should have caught earlier.

## Open questions

- What is the correct, current way to fully suppress vanilla XP orb drops/pickup in 1.21.11
  without fighting the vanilla XP bar (Fischey's in-progress work on `fischey_workbranch`)?
  Needs a verified Mixin target method + confirmation it doesn't break unrelated vanilla
  mechanics (anvils, enchanting tables, which also read player XP).
- Persistence: `PlayerProgressData` already has `writeNbt`/`readNbt` but nothing calls them
  yet — need to confirm the correct 1.21.11 hook for per-player persistent data (Fabric's
  attachment API vs. classic `PlayerEntity` NBT read/write mixin) before implementing.
