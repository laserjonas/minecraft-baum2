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
- New: four subagents under `.claude/agents/` (shared via git, so both contributors get them):
  `fabric-docs-researcher` (Fabric/MC API research -> `docs/fabric-modding.md`),
  `ip-naming-compliance-checker` (reviews new names/text against IP/naming rules),
  `balance-reviewer` (internal-consistency/exploit review of numeric balance values),
  `merge-integration-reviewer` (pre-merge overlap/design-conflict check between branches).
  All four report findings only — they don't edit files. See `CLAUDE.md` -> "Project Agents"
  for exact trigger conditions; use them proactively, don't wait to be asked.
- **Known limitation**: a running Claude Code session loads its available agent list at
  startup, so newly added `.claude/agents/*.md` files aren't picked up mid-session — they
  become available the next time a session starts fresh (restart, or a fresh session after
  pulling). If an agent invocation fails with "Agent type not found" right after one was
  added, that's why — not a bug in the agent definition.

## Last change (on `jonas_workbranch`)

- Commit: (this commit) — "Add ip-naming-compliance-checker, balance-reviewer, and
  merge-integration-reviewer agents"
- What: Added three more subagents alongside `fabric-docs-researcher` (see Current State
  above for what each does), and a "Project Agents" section in `CLAUDE.md` listing all four
  with their proactive-use trigger conditions. Attempted to run `balance-reviewer` and
  `ip-naming-compliance-checker` against the existing progression system (Fischey's XP curve/
  mob-reward formula, command/broadcast text) as a first real pass, but the current session
  couldn't invoke them — new custom agents aren't picked up until a fresh session starts (see
  "Known limitation" above). Re-run that review in a fresh session; nothing in the existing
  progression code has been balance/IP-reviewed yet.
- Why: User asked for these agents to be implemented and used automatically once relevant,
  based on ideas raised in the previous session (IP/naming compliance, cross-branch merge
  risk, balance consistency — all recurring concerns for a two-contributor MMORPG mod).
- This is on `jonas_workbranch`, not yet merged to `master`.

Earlier: `fd9e385`/`787cfbf` — "Add fabric-docs-researcher agent and shared Fabric modding
docs"; on `master`: `49f5208` — "Use Minecraft's vanilla XP/level system instead of custom
HUD"; `2405ca7` — "Fix Java 21/25 mismatch blocking runClient; add VS Code run config";
`731c5a3` — "Fix commit hash reference in HANDOFF.md"; `7f228e8` — "Add MASTERPROMPT.md and
reference it from CLAUDE.md"; `4923a85` — "Initial Minecraft MMORPG mod setup". See prior
HANDOFF.md revisions in `git log -p HANDOFF.md` for the full detail on each.

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

In a fresh session (so the new agents are actually loaded): run `balance-reviewer` on the
existing progression system and `ip-naming-compliance-checker` on existing player-facing
strings — neither has been reviewed yet. Then: progression/XP is Fischey's active area
(`fischey_workbranch`), avoid duplicating that work. Remaining Priority 1 items per
`CLAUDE.md` that don't overlap: first custom item, first weapon, first active skill with a
cooldown manager, first world-event block. Consult `fabric-docs-researcher` /
`docs/fabric-modding.md` before implementing any of these if the relevant Fabric API is
unclear.
