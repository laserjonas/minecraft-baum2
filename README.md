# Baum2

A Minecraft Fabric mod implementing original MMORPG progression systems. This is a fresh project with a complete feature roadmap and detailed design constraints documented for contributors.

## About the Project

Baum2 adds deep, original fantasy MMORPG mechanics to vanilla Minecraft 1.21.11:

- **Player progression**: class systems, leveling, skill trees, cooldown mechanics
- **Combat and skills**: active abilities, passive effects, weapon specializations
- **World events**: boss encounters, world bosses, instanced dungeons
- **Items and equipment**: custom weapons, armor, progression-gated loot
- **UI and polish**: HUD overlays, stat screens, quest logs

All names, assets, lore, UI designs, and mechanics are **original** — not copied or remixed from existing games. See [MASTERPROMPT.md](MASTERPROMPT.md) for the full vision, feature roadmap, and legal/naming constraints.

## Tech Stack

- **Game**: Minecraft Java 1.21.11
- **Modloader**: Fabric Loader 0.19.3
- **Build system**: Gradle + Fabric Loom 1.17.13
- **Language**: Java 21
- **Package**: `de.baum2dev.baum2`
- **Main mod class**: `Baum2`

## Quick Start

### Prerequisites

You need Java 21. The installer will set it up for you:

- **Windows**: Download and install [Eclipse Temurin JDK 21](https://adoptium.net/)
- **macOS/Linux**: Use your package manager or the above link

### Running the Modded Client

**From VS Code** (recommended):

1. Open the project folder in VS Code
2. **Fully quit and reopen VS Code** (not just reload) — this picks up the newly installed Java from the system environment
3. Press `Ctrl+Shift+B` to run the "Run Minecraft Client" build task, or:
   - Open Command Palette (`Ctrl+Shift+P`)
   - Type "Tasks: Run Task"
   - Select "Run Minecraft Client"

**From terminal**:

```bash
./gradlew.bat runClient          # Windows
./gradlew runClient              # macOS/Linux
```

The game window will open to the main menu. Close it to stop the task.

### Building the Mod

```bash
./gradlew.bat build              # Windows
./gradlew build                  # macOS/Linux
```

The compiled mod JAR is at `build/libs/baum2-1.0-SNAPSHOT.jar`.

## Project Structure

```
minecraft-baum2/
├── src/
│   ├── main/java/de/baum2dev/baum2/        # Shared mod code
│   ├── client/java/de/baum2dev/baum2/      # Client-only code
│   └── main/resources/                     # Assets, configs, mixins
├── CLAUDE.md                               # Short dev guidelines
├── MASTERPROMPT.md                         # Full feature roadmap & design constraints
├── HANDOFF.md                              # Latest state & decisions for multi-dev sync
└── .vscode/                                # VS Code tasks (Java/Gradle setup)
```

## For Contributors

- Read [CLAUDE.md](CLAUDE.md) for day-to-day dev rules and package structure
- Read [MASTERPROMPT.md](MASTERPROMPT.md) for the full feature roadmap, IP/naming rules, and priorities
- Read [HANDOFF.md](HANDOFF.md) **before** starting work — it reflects the latest state and any non-obvious decisions
- Always run `./gradlew build` before committing
- Update `HANDOFF.md` before every commit (see "Handoff Rule" in [CLAUDE.md](CLAUDE.md))

## Troubleshooting

**"java: command not found" when running tasks**

- Make sure Java 21 is installed and on your PATH
- On Windows: fully quit and reopen VS Code (not just "Reload Window") so it picks up the updated environment

**"JAVA_HOME is not set"**

- Same as above — the issue is VS Code's cached environment at startup time

**Mixin errors or "JAVA_25" compatibility level not supported**

- This has already been fixed in the codebase (see [commit 2405ca7](https://github.com/laserjonas/minecraft-baum2/commit/2405ca7))
- If you see this, you may have an old working copy — pull the latest

## License

See [LICENSE.txt](LICENSE.txt)
