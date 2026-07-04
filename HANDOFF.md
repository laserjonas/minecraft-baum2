# Handoff

Read this first. It reflects the state as of the latest commit so a fresh Claude Code
session (yours or a co-author's) can pick up work without re-deriving context from
`git log`. Updated after every commit — see "Git Rules" in `CLAUDE.md`.

## Current state

- Fabric mod scaffold builds successfully (`./gradlew build` / `gradlew.bat build` passes).
- The client **runs**: `./gradlew runClient` / `gradlew.bat runClient` loads the mod and reaches the main menu.
- Package: `de.baum2dev.baum2`, main class `Baum2`, client class `Baum2Client`.
- Minecraft 1.21.11 / Yarn 1.21.11+build.6 / Fabric API 0.141.4+1.21.11 / Fabric Loom 1.17.13 / Java 21.
- **Progression system**: Working and integrated with vanilla Minecraft. Commands (`/baum2 addxp`, `/baum2 level`),
  mob XP drops (10 + max_health/2 on hostile mob kills), level-up broadcasts, and vanilla XP/level bar display.
  Data stored server-side in-memory; persistence deferred to next phase.
- Repo: https://github.com/laserjonas/minecraft-baum2 (public).
- **Branches**: `master` (integration), `fischey_workbranch` (Fischey — Mixin-based XP-drop
  suppression, in progress, 12 commits ahead of master, not yet merged), `jonas_workbranch`
  (Jonas — infra/docs work, tracks `origin/jonas_workbranch`). Working split so both
  contributors can work in parallel without stepping on the same files; merge into `master`
  when a branch's work is ready.
- `.vscode/` is checked in (extensions.json, settings.json, tasks.json) so fresh checkout gets Java+Gradle
  recommendations and "Run Minecraft Client" task (`Ctrl+Shift+B`) out of the box.
- New: `.claude/agents/fabric-docs-researcher.md` — a subagent whose only job is researching
  Minecraft 1.21.11/Fabric/Loom/Mixin APIs and persisting findings to `docs/fabric-modding.md`.
  Consult it (or that doc) before guessing at any modding API — see `CLAUDE.md`.

## Last change (on `jonas_workbranch`)

- Commit: (this commit) — "Add fabric-docs-researcher agent and shared Fabric modding docs"
- What: Added `.claude/agents/fabric-docs-researcher.md` (a subagent that researches Fabric/MC
  1.21.11 APIs from primary sources — decompiled sources, Fabric API GitHub, Fabric meta API,
  Mixin wiki — rather than guessing from training data) and `docs/fabric-modding.md` (the
  living reference it maintains, seeded with what this session already learned: the 26.2
  Yarn-mapping gap, the Loom/Gradle version requirement, the Java toolchain pinning gotcha,
  and open questions relevant to Fischey's in-progress Mixin work). Added a `CLAUDE.md` rule
  to consult this agent proactively whenever a modding API is unclear.
- Why: Both contributors run separate Claude Code sessions; Fabric APIs for a version this new
  (1.21.11) aren't reliably covered by training data or even the Fabric wiki yet, so ad-hoc
  guessing was a risk. A shared, append-only doc means whoever hits an API question first
  saves the other person from re-researching it.
- This is on `jonas_workbranch`, not yet merged to `master`.

Earlier (on `master`): `49f5208` — "Use Minecraft's vanilla XP/level system instead of custom
HUD" (vanilla XP sync via `player.addExperience()`, removed custom HUD rendering after text-
rendering issues); `2405ca7` — "Fix Java 21/25 mismatch blocking runClient; add VS Code run config"
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

Progression/XP is Fischey's active area (`fischey_workbranch`) — avoid duplicating that work.
Remaining Priority 1 items per `CLAUDE.md` that don't overlap: first custom item, first
weapon, first active skill with a cooldown manager, first world-event block. Consult the
`fabric-docs-researcher` agent / `docs/fabric-modding.md` before implementing any of these if
the relevant Fabric API (item registration, custom entity, block) is unclear.
