# CLAUDE.md

## Project

This is an original Minecraft Java Fabric mod with fantasy MMORPG progression systems.

Working title: Baum2.

This project must not clone, remake, recreate, or imitate any existing MMORPG. All names, assets, lore, UI concepts, item names, skill names, mob names, boss names, maps, quest text, balancing values, sounds, models, textures, and branding must be original.

Full original project brief (vision, complete feature roadmap, naming/legal guidelines in
detail, examples): see `MASTERPROMPT.md`. This file is the short distillation for day-to-day
work; go to `MASTERPROMPT.md` for anything not covered here in enough depth. For "what changed
last / what's next", see `HANDOFF.md` instead.

## Project Values

- Mod display name: Baum2
- Mod ID: baum2
- Maven group: de.baum2dev
- Base package: de.baum2dev.baum2
- Main mod class: Baum2
- GitHub repository: minecraft-baum2
- Repository visibility: public
- Author: baum2dev
- Minecraft version: 1.21.11
- Yarn mappings: 1.21.11+build.6
- Fabric Loader: 0.19.3
- Fabric API: 0.141.4+1.21.11
- Fabric Loom: 1.17.13
- Java target: 21
- Modloader: Fabric
- Language: Java
- Build: Gradle / Fabric Loom

Note: Minecraft 26.2 is the current stable release, but the Yarn mappings project has not
published mappings for it yet (verified against the Fabric meta API). The project therefore
targets 1.21.11, the newest version with complete Yarn mappings and Fabric API builds. Bump
`minecraft_version`, `yarn_mappings`, and `fabric_version` in `gradle.properties` together once
26.2 mappings are published.

## Commands

- Build: `./gradlew build` (Windows: `gradlew.bat build`)
- Run client: `./gradlew runClient`
- Run server: `./gradlew runServer`
- Generate sources: `./gradlew genSources`

## Development Rules

- Work in small, buildable steps.
- Keep features modular.
- Do not add unnecessary dependencies.
- Preserve existing package structure.
- Prefer simple Java code over clever abstractions.
- After implementation, run `./gradlew build`.
- Fix build errors before considering the task complete.
- Keep code readable and easy to extend.
- Do not create large systems before a minimal version works.

## Architecture

Use these package areas where appropriate, under `de.baum2dev.baum2`:

- registry/
- progression/
- classes/
- skills/
- events/
- combat/
- items/
- quests/
- dungeons/
- networking/
- ui/
- data/
- config/

## IP / Legal Rules

Do not use protected names, logos, icons, sounds, music, textures, models, maps, quest text, UI designs, item lists, skill names, mob names, boss names, faction names, or lore from existing games.

Do not use names or terms from Metin2, Webzen, Gameforge, or other existing MMORPGs.

General MMORPG mechanics are allowed. Direct copying or lightly renamed copying is not allowed.

## Naming Rules

All names must be original.

Avoid names that sound like slightly modified versions of existing game names, item names, skill names, factions, bosses, or locations.

Use a consistent original fantasy naming style.

## Git Rules

- Commit only working code.
- Run `./gradlew build` before committing.
- Use clear commit messages.
- Do not commit build artifacts, IDE caches, secrets, or generated temporary files.
- Do not commit copied assets.
- Do not commit API keys, tokens, passwords, credentials, or private config files.

## Project Agents (use proactively, don't wait to be asked)

Custom subagents live in `.claude/agents/` and are shared via git — both contributors' Claude
Code sessions have them. Invoke the relevant one automatically when its trigger applies,
rather than guessing or skipping the check:

- **`fabric-docs-researcher`** — whenever a Minecraft/Fabric/Loom/Mixin API is unclear (an
  unfamiliar class, an event hook that might not exist anymore, a version-compatibility
  question). Training data can be stale for a version as new as 1.21.11. Persists findings to
  `docs/fabric-modding.md` — check that file first, the answer may already be there.
- **`ip-naming-compliance-checker`** — whenever new player-facing names or text are added or
  changed: items, weapons, skills, mobs, bosses, factions, places, quest text, lore, UI
  strings. Also before merging a branch that touched any of these. Checks against the IP/
  naming rules below and in `MASTERPROMPT.md`.
- **`balance-reviewer`** — whenever a new or changed numeric balance value is introduced: XP/
  level curves, mob rewards, item stats, upgrade costs/chances, drop rates, damage/health,
  cooldowns, resource costs. Internal-consistency and exploit review only (values must be
  original per the Balancing Rules — nothing to compare against).
- **`merge-integration-reviewer`** — before merging or rebasing one contributor's branch onto
  another (or onto `master`), and before starting work that might overlap with what's active
  on the other contributor's branch. Flags file/system overlap and design conflicts, not just
  textual git conflicts.
- **`graphics-designer`** — any time new visual/graphical identity work is needed: a texture,
  model, icon, UI/HUD layout, or color/style system (rarity, class, faction). Unlike the four
  above, it produces deliverables (specs, placeholder assets, model/blockstate JSON), not just
  findings — maintains `docs/visual-style-guide.md`.

The first four report findings only; none of them edit files or make decisions themselves —
apply their findings yourself, or ask the user when a finding needs a judgment call.
`graphics-designer` is the exception and does write files (see above).

## Handoff Rule (multi-developer sync)

This project has more than one human/Claude Code pair working on it. Before every commit,
update `HANDOFF.md` at the project root with:

- Current state (what builds, what's implemented, what isn't).
- Last change (commit summary: what and why).
- Any non-obvious decisions made (version pins, workarounds, tradeoffs).
- The next recommended step.

`HANDOFF.md` reflects the latest state only — it is overwritten each time, not appended to.
Full history lives in `git log`. Read `HANDOFF.md` first when starting work in this repo,
before exploring the codebase, so you don't re-derive context a previous session already has.
