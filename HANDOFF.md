# Handoff

Read this first. It reflects the state as of the latest commit so a fresh Claude Code
session (yours or a co-author's) can pick up work without re-deriving context from
`git log`. Updated after every commit — see "Git Rules" in `CLAUDE.md`.

## Current state

- Fabric mod scaffold builds successfully (`./gradlew build` / `gradlew.bat build` passes).
- The client also **runs** now: `./gradlew runClient` / `gradlew.bat runClient` loads all mods
  (including `baum2`) and reaches the main menu with no crash.
- Package: `de.baum2dev.baum2`, main class `Baum2`, client class `Baum2Client`.
- Minecraft 1.21.11 / Yarn 1.21.11+build.6 / Fabric API 0.141.4+1.21.11 / Fabric Loom 1.17.13.
- No gameplay features implemented yet — `Baum2` and `Baum2Client` are empty entrypoints.
- Repo: https://github.com/laserjonas/minecraft-baum2 (public), pushed to `master`.
- `.vscode/` is now checked in (extensions.json, settings.json, tasks.json) so a fresh VS Code
  checkout gets Java+Gradle extension recommendations and a "Run Minecraft Client" build task
  (`Ctrl+Shift+B`) out of the box.

## Last change

- Commit: (this commit) — "Fix Java 21/25 mismatch blocking runClient; add VS Code run config"
- What:
  - `build.gradle`: the `java { toolchain.languageVersion = ... }` pin was conditional
    (`if (JavaVersion.current() < javaVersion)`), so once Gradle itself ran on a JDK 21, the
    pin was skipped entirely and Fabric Loom's `runClient`/`runServer` JavaExec tasks fell back
    to auto-detecting *any* installed JDK on the machine — which picked up a stray
    IntelliJ-managed `openjdk-25.0.1` under `~/.jdks`. Made the pin unconditional so the run
    tasks always resolve JDK 21.
  - `src/main/resources/baum2.mixins.json` and `src/client/resources/baum2.client.mixins.json`:
    both had `"compatibilityLevel": "JAVA_25"` hardcoded from whatever JDK generated the
    template. Sponge Mixin/ASM on this project's actual Java 21 target cannot set that level
    and crashed on startup (`IllegalArgumentException: The requested compatibility level
    JAVA_25 could not be set`). Changed both to `JAVA_21`.
  - Added `.vscode/extensions.json`, `.vscode/settings.json`, `.vscode/tasks.json`.
- Why: a fresh clone had no JDK installed at all, and once one was installed (Temurin 21, to
  match `gradle.properties`/`build.gradle`'s Java 21 target) `runClient` still failed for the
  two reasons above — both stemming from the template having been generated under a local
  JDK 25 rather than the project's pinned JDK 21.

Earlier: `731c5a3` — "Fix commit hash reference in HANDOFF.md"; `7f228e8` — "Add MASTERPROMPT.md
and reference it from CLAUDE.md" (full original project brief, feature roadmap, legal/naming
guidelines); `4923a85` — "Initial Minecraft MMORPG mod setup" (project scaffold, package renamed
from `baum2dev.baum2` to `de.baum2dev.baum2`, `CLAUDE.md`/`.gitignore` added, version mismatch
in the generated template fixed — see Decisions below).

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

## Next recommended step

Priority 1 per `CLAUDE.md`: basic player-progression package (`progression/`), a minimal level
system, first custom item, first weapon, first active skill with a cooldown manager, first
world-event block.
