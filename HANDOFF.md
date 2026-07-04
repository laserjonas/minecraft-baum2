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

- Commit: (this commit) — "Add initial progression system: level, XP, experience manager"
- What:
  - `src/main/java/de/baum2dev/baum2/progression/PlayerProgressData.java` — stores level, XP,
    and XP requirement for next level; includes NBT serialization methods for future persistence.
  - `src/main/java/de/baum2dev/baum2/progression/ExperienceManager.java` — handles XP logic:
    adding XP to a player, triggering level-ups when threshold is met, calculating XP requirement
    per level (100 * level). Max level is 100.
  - `src/main/java/de/baum2dev/baum2/progression/PlayerLevelSystem.java` — manages progression
    per player: get/save progress, add experience. Uses in-memory UUID → PlayerProgressData map
    (temporary until proper persistence via Fabric data attachments is implemented).
  - Modified `Baum2.java` — added logger and log message on mod initialization.
- Why: Priority 1 requires a level system. This minimal implementation is functional and
  ready for the next step: adding the first item/weapon/skill. Persistence is deferred (TODO:
  use Fabric data components or event listeners to load/save player progression).
- Note: progression data is currently in-memory only (lost on server restart). Next session
  should implement proper save/load via Fabric events or data attachments.

Earlier: `2405ca7` — "Fix Java 21/25 mismatch blocking runClient; add VS Code run config"
(fixed `build.gradle` toolchain pin, both mixins.json compatibility levels, added
`.vscode/` config); `731c5a3` — "Fix commit hash reference in HANDOFF.md"; `7f228e8` — "Add MASTERPROMPT.md
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
