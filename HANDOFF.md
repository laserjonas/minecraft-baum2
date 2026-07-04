# Handoff

Read this first. It reflects the state as of the latest commit so a fresh Claude Code
session (yours or a co-author's) can pick up work without re-deriving context from
`git log`. Updated after every commit — see "Git Rules" in `CLAUDE.md`.

## Current state

- Fabric mod scaffold builds successfully (`./gradlew build` / `gradlew.bat build` passes).
- Package: `de.baum2dev.baum2`, main class `Baum2`, client class `Baum2Client`.
- Minecraft 1.21.11 / Yarn 1.21.11+build.6 / Fabric API 0.141.4+1.21.11 / Fabric Loom 1.17.13.
- No gameplay features implemented yet — `Baum2` and `Baum2Client` are empty entrypoints.
- Repo: https://github.com/laserjonas/minecraft-baum2 (public), pushed to `master`.

## Last change

- Commit: `4923a85` — "Initial Minecraft MMORPG mod setup"
- What: Set up the project scaffold, renamed the package from `baum2dev.baum2` to
  `de.baum2dev.baum2`, added `CLAUDE.md` and `.gitignore`, fixed a version mismatch in the
  generated template (see Decisions below).
- Why: Project bootstrap — nothing gameplay-related implemented yet.

## Decisions worth knowing about

- Minecraft 26.2 is the actual latest stable release, but Yarn has not published mappings for
  it yet (confirmed against `meta.fabricmc.net`). We target 1.21.11 instead — the newest
  version with full Yarn + Fabric API support. Bump `minecraft_version`, `yarn_mappings`, and
  `fabric_version` in `gradle.properties` together once 26.2 mappings exist.
- Fabric Loom pinned to stable `1.17.13` (the generated template shipped a floating
  `1.17-SNAPSHOT`, which is unsafe to keep).
- Gradle wrapper bumped from 9.2.1 to 9.5.1 — Loom 1.17.13 requires Gradle's plugin
  `api-version` >= 9.5.0, so 9.2.1 cannot resolve it.
- Java target set to 21 (matches what Minecraft 1.21.11 needs) even though the JDK installed
  on this machine is 25 — the toolchain compiles down via `options.release`.

## Next recommended step

Priority 1 per `CLAUDE.md`: basic player-progression package (`progression/`), a minimal level
system, first custom item, first weapon, first active skill with a cooldown manager, first
world-event block.
