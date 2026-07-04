# Masterprompt

This is the original project brief that this repository was bootstrapped from. It is the
long-form source of truth for vision, feature roadmap, legal/IP guardrails, naming style, and
working process. `CLAUDE.md` is the short, practical distillation of the rules from this
document for day-to-day work; read `CLAUDE.md` first for quick reference, and come back here
for the full feature roadmap, rationale, and examples whenever you need more than the short
version gives you.

Status: the interview below has already been answered and actioned (see "Resolved project
values"). Priority 1 (stable build, CLAUDE.md, GitHub repo, registries, level system, first
item/weapon/skill/event block) is not finished yet — most of it is still open. Treat the
feature sections below as the backlog, not as already implemented.

## Resolved project values

These replace the placeholders used throughout the rest of this document:

```text
<MOD_DISPLAY_NAME>    -> Baum2
<MOD_ID>              -> baum2
<MAVEN_GROUP>         -> de.baum2dev
<BASE_PACKAGE>        -> de.baum2dev.baum2
<GITHUB_REPO_NAME>    -> minecraft-baum2
<GITHUB_VISIBILITY>   -> public
<MAIN_MOD_CLASS>      -> Baum2
<AUTHOR_NAME>         -> baum2dev
<MINECRAFT_VERSION>   -> 1.21.11 (see note below)
<MODLOADER>           -> Fabric
<LANGUAGE>            -> Java
```

Note on Minecraft version: 26.2 is the actual latest stable Minecraft release, but Yarn has not
published mappings for it yet (verified against the Fabric meta API when the project was set
up). The project targets 1.21.11 instead — the newest version with full Yarn + Fabric API
support — until 26.2 mappings exist. See `HANDOFF.md` for the current state and next steps.

---

## Ursprünglicher Auftrag

Entwickle einen eigenständigen Minecraft-Fabric-Mod mit MMORPG-Progression. Der Mod darf sich
nur an allgemeinen Action-MMORPG-Mechaniken orientieren, zum Beispiel Klassen, Skills,
Welt-Events, Bossen, Loot, Fraktionen, Item-Upgrades, Dungeons und Quests.

Wichtig: Dieses Projekt darf kein Klon, Remake, Nachbau oder getarnter Nachbau eines
bestehenden Spiels sein. Es darf insbesondere keine geschützten Namen, Assets, Logos, Icons,
Sounds, Musik, Modelle, Texturen, Questtexte, Kartenlayouts, UI-Designs, Itemlisten,
Monsterlisten, Skillnamen, Fraktionsnamen, Bossnamen, Lore, Balancing-Daten oder sonstige
wiedererkennbare Inhalte aus bestehenden Spielen übernehmen.

Das Ergebnis soll sich anfühlen wie ein eigenständiger Minecraft-Fantasy-MMORPG-Mod, nicht wie
eine leicht umbenannte Kopie eines existierenden Spiels.

## Rechtliche und kreative Leitlinien

* Erstelle eine vollständig eigenständige Spielidentität.
* Verwende keine Namen, Begriffe oder Bezeichnungen aus bestehenden MMORPGs.
* Verwende keine Begriffe wie „Metin", „Metinstein", „Drachengott", „Shinsoo", „Chunjo",
  „Jinno" oder andere wiedererkennbare Namen aus Metin2 oder anderen bestehenden Spielen.
* Kopiere keine Itemnamen, Waffennamen, Rüstungsnamen, Monster, Bosse, NPCs, Quests, Maps,
  Skills, Icons, Sounds, Musik, Modelle, Texturen, UI-Elemente oder visuellen Designs.
* Orientiere dich nur an allgemeinen Spielmechaniken, nicht an konkreten Inhalten.
* Alle Namen, Texte, Balancing-Werte, Items, Mobs, Skills, Fraktionen, Bosse, Welt-Elemente,
  Questtexte und UI-Konzepte müssen neu erfunden werden.
* Wenn eine Idee zu ähnlich zu einem bekannten Spiel wirkt, erstelle eine eigenständigere
  Alternative.
* Keine fremden Assets verwenden.
* Keine extrahierten Spieldaten verwenden.
* Keine Private-Server-Dateien, Client-Dateien oder dekompilierte Inhalte verwenden.
* Keine geschützten Marken, Logos oder Spielnamen im Code, in Assets, in Dateinamen, in
  Package-Namen oder in öffentlicher Beschreibung verwenden.

## Technisches Setup

* Modloader: Fabric
* Sprache: Java
* Build-System: Gradle / Fabric Loom
* IDE: IntelliJ IDEA oder VS Code
* Ziel: saubere, modulare Codebasis
* Minecraft-Version: 1.21.11 (siehe Hinweis oben zu 26.2)
* Keine unnötigen Dependencies hinzufügen
* Nach relevanten Änderungen immer `./gradlew build` ausführen
* Bestehende Package-Struktur respektieren
* Neue Features klein, testbar und inkrementell implementieren

## Projektidentität

```text
Anzeigename: Baum2
Mod-ID: baum2
Maven Group: de.baum2dev
Base Package: de.baum2dev.baum2
GitHub Repository: minecraft-baum2
Repository Sichtbarkeit: public
Hauptklasse: Baum2
Autor: baum2dev
Minecraft-Version: 1.21.11
Modloader: Fabric
Sprache: Java
```

Der Mod soll Minecraft um ein eigenständiges Fantasy-MMORPG-System erweitern.

## Kernfeatures

### 1. Klassen-System

Implementiere mehrere spielbare Archetypen mit eigener Identität.

Beispielklassen:

* Eisenwächter
* Schattenläufer
* Runenwirker
* Wesenswahrer

Anforderungen:

* Jede Klasse hat eigene passive Boni.
* Jede Klasse kann aktive Skills erhalten.
* Klassen sollen sich spielerisch unterscheiden.
* Klassennamen, Skillnamen und Beschreibungen müssen vollständig original sein.
* Die erste Version darf simpel sein und später erweitert werden.

### 2. Skill-System

Implementiere ein erweiterbares Skill-System.

Anforderungen:

* Aktive Fähigkeiten mit Cooldowns
* Passive Fähigkeiten
* Level-basierte Freischaltung
* Mana-, Energie- oder Fokus-Kosten optional
* Serverseitige Validierung
* Clientseitige Anzeige später möglich
* Keine kopierten Skillnamen oder Skill-Identitäten aus bestehenden Spielen

Beispiel-Skills:

* Eisenwächter: Schildstoß, Standhafte Aura
* Schattenläufer: Nebelschritt, Klingenwirbel
* Runenwirker: Runenfunke, Arkaner Kreis
* Wesenswahrer: Lebensband, Geisterwoge

Diese Namen sind Platzhalter und dürfen bei Bedarf verbessert werden, solange sie eigenständig
bleiben.

### 3. Progression

Implementiere ein eigenes Progressionssystem.

Anforderungen:

* Spieler-Level
* Klassen-Level
* Erfahrungspunkte
* Talentpunkte optional
* Erfahrung durch Gegner, Quests, Events und Dungeons
* Serverseitige Speicherung
* Kompatibel mit Minecraft-Saves
* Kleine erste Version, später erweiterbar

Die Progressionskurve muss eigenständig erstellt werden. Keine Wertekurven aus bestehenden
Spielen übernehmen.

### 4. Welt-Events

Implementiere seltene zerstörbare Weltobjekte als Events.

Beispielnamen:

* Rissobelisk
* Chaosmonolith
* Sternsplitter
* Verderbter Altar
* Echosäule
* Sturmsiegel

Anforderungen:

* Weltobjekt spawnt selten oder wird für Tests manuell platzierbar gemacht.
* Beim Angriff können Gegnerwellen erscheinen.
* Beim Zerstören gibt es Erfahrung, Loot und seltene Materialien.
* Optik, Name, Verhalten und Loot müssen eigenständig sein.
* Die erste Version darf ein manuell platzierbarer Block sein.

### 5. Fraktionen

Implementiere später drei eigenständige Fraktionen.

Beispiel-Fraktionen:

* Haus Solvyr
* Orden Myrkan
* Bund Avarra

Anforderungen:

* Eigene Namen
* Eigene Farben
* Eigene Symbole
* Eigene Lore
* Keine Übernahme bekannter Reiche, Fraktionen, Logos oder Farbcodes aus bestehenden Spielen

### 6. Gegner und Bosse

Implementiere neue Fantasy-Gegner und Bosse.

Anforderungen:

* Keine kopierten Monsterdesigns
* Eigene Bossmechaniken
* Eigene Drop-Tabellen
* Eigene Namen
* Minecraft-kompatible KI
* Erst einfache Gegner, später komplexere Bosse

Beispiel-Gegner:

* Rissling
* Aschenwolf
* Splitterwächter
* Hohlritter
* Sturmkultist

Beispiel-Bosse:

* Varok der Gebrochene
* Elyra vom Sternenfall
* Ghorun Aschenherz

### 7. Item- und Upgrade-System

Implementiere ein eigenes Ausrüstungs- und Upgrade-System.

Anforderungen:

* Eigene Waffen- und Rüstungsnamen
* Item-Raritäten
* Upgrade-Stufen
* Materialkosten
* Risiko- oder Fortschrittssystem optional
* Keine Kopie bestehender Itemlisten
* Keine identischen Wertekurven aus bestehenden Spielen
* Keine übernommenen Icons, Texturen oder Modelle

Beispiel-Raritäten:

* Gewöhnlich
* Selten
* Veredelt
* Mythisch
* Astral

Beispiel-Materialien:

* Risssplitter
* Sternenstaub
* Eisenholz
* Runenkern
* Verdichtete Essenz

### 8. Dungeons

Implementiere später instanzähnliche Dungeon-Strukturen innerhalb von Minecraft.

Anforderungen:

* Gegnerwellen
* Mini-Bosse
* Endboss
* Loot-Truhen
* seltene Materialien
* eigene Namen, Themen und Layouts
* keine kopierten Kartenlayouts aus bestehenden Spielen

Beispiel-Dungeons:

* Die Hallen von Nyr
* Aschenbruch-Katakomben
* Sternenfall-Ruinen
* Der Hohlhain

### 9. Quests

Implementiere später ein eigenes Quest-System.

Anforderungen:

* Hauptquests optional
* tägliche Aufgaben
* Fraktionsaufgaben
* Bossjagden
* Sammelquests
* Erkundungsquests
* keine kopierten Questtexte
* keine kopierten Questketten
* keine übernommenen NPC-Namen

### 10. UI und Spielerführung

Implementiere später eigene Menüs und Anzeigen.

Anforderungen:

* Eigenständiges Interface
* Keine Nachahmung bekannter MMORPG-UIs
* Minecraft-kompatible Menüs
* Klare Tooltips
* Übersicht für Skills, Klassen, Level und Upgrades
* Keine kopierten Layouts, Icons oder visuellen Stile

## Architektur

Strukturiere den Code modular. Verwende nach Möglichkeit folgende Package-Bereiche unterhalb
von `de.baum2dev.baum2`:

```text
registry/
  ModItems
  ModBlocks
  ModEntities
  ModSounds
  ModEffects

progression/
  PlayerLevelSystem
  ClassLevelSystem
  ExperienceManager
  PlayerProgressData

classes/
  PlayerClass
  ClassRegistry
  ClassDefinition
  ClassManager

skills/
  Skill
  ActiveSkill
  PassiveSkill
  SkillRegistry
  SkillCooldownManager
  SkillExecutionContext

events/
  WorldEventManager
  RiftObeliskEvent
  EventSpawnManager

combat/
  DamageScaling
  StatusEffects
  CustomCombatHooks

items/
  UpgradeableItem
  UpgradeSystem
  ItemRarity
  UpgradeMaterial

quests/
  Quest
  QuestObjective
  QuestManager

dungeons/
  DungeonDefinition
  DungeonManager
  BossEncounter

networking/
  ClientPackets
  ServerPackets

ui/
  SkillScreen
  ClassScreen
  UpgradeScreen

data/
  PlayerPersistentData
  ModDataComponents

config/
  ModConfig
```

Passe die Struktur an die verwendete Minecraft-/Fabric-Version an. Vermeide veraltete APIs.

## Arbeitsweise

Arbeite immer in kleinen, überprüfbaren Schritten.

Bei jedem Feature:

1. Prüfe die bestehende Projektstruktur.
2. Lies `CLAUDE.md` (und bei Bedarf `HANDOFF.md` für den aktuellen Stand).
3. Plane kurz die notwendigen Dateien.
4. Implementiere nur den nächsten sinnvollen Schritt.
5. Verwende klare, sprechende Namen.
6. Vermeide unnötige Komplexität.
7. Führe `./gradlew build` aus.
8. Behebe Buildfehler.
9. Aktualisiere `HANDOFF.md` mit dem aktuellen Stand.
10. Erstelle einen Git-Commit, wenn der Build erfolgreich ist.
11. Fasse die Änderung kurz zusammen.

## Commit-Regeln

Nutze klare Commit-Messages.

Beispiele:

```text
Add CLAUDE project rules
Add initial progression system
Add player class registry
Add first active skill
Add rift obelisk block
Add basic upgrade materials
Fix Fabric registration errors
```

Committe nur, wenn:

* der Build erfolgreich ist,
* keine offensichtlichen temporären Dateien enthalten sind,
* keine fremden Assets enthalten sind,
* keine Secrets enthalten sind,
* die Änderung sinnvoll zusammengehört,
* `HANDOFF.md` aktualisiert wurde.

## Entwicklungsprioritäten

### Priorität 1

* Stabiler Build
* Saubere Projektstruktur
* `CLAUDE.md`
* GitHub-Repository
* Grundlegende Registries
* Levelsystem
* Erstes Item
* Erste Waffe
* Erster Skill
* Erster Event-Block

### Priorität 2

* Klassen-System
* Skill-Auswahl
* bessere Persistenz
* Gegner-Spawns
* Loot-System
* Upgrade-Materialien

### Priorität 3

* Dungeons
* Fraktionen
* Quests
* UI
* Bossmechaniken
* Balancing
* Konfiguration

## Naming-Stil

Verwende eine eigenständige Fantasy-Namenswelt.

Gute Beispiele:

```text
Rissobelisk
Chaosmonolith
Sternsplitter
Runenkern
Eisenwächter
Schattenläufer
Runenwirker
Wesenswahrer
Haus Solvyr
Orden Myrkan
Bund Avarra
Aschenbruch-Katakomben
Sturmsiegel
```

Schlechte Beispiele:

```text
leicht veränderte Namen aus bestehenden Spielen
ähnlich klingende Namen bekannter Items
ähnlich klingende Namen bekannter Fraktionen
ähnlich klingende Namen bekannter Bosse
Namen mit erkennbarer Nähe zu Metin2, Webzen, Gameforge oder anderen bestehenden MMORPGs
```

## Balancing-Regeln

Erstelle eigene Balancing-Werte.

* Keine Drop-Tabellen aus bestehenden Spielen übernehmen.
* Keine Levelkurven aus bestehenden Spielen übernehmen.
* Keine Upgrade-Chancen aus bestehenden Spielen übernehmen.
* Keine Itemwerte aus bestehenden Spielen übernehmen.
* Werte sollen zuerst einfach und testbar sein.
* Später können Werte in Config-Dateien ausgelagert werden.

## Asset-Regeln

Bis eigene Assets existieren:

* Verwende einfache Platzhalter.
* Verwende Minecraft-kompatible eigene Texturen.
* Verwende keine kopierten Texturen.
* Verwende keine extrahierten Icons.
* Verwende keine Sounds oder Musik aus anderen Spielen.
* Kennzeichne Platzhalter klar als eigene temporäre Platzhalter.

## Sicherheits- und Qualitätsregeln

* Keine Secrets in Code oder Configs.
* Keine API Keys.
* Keine Tokens.
* Keine Zugangsdaten.
* Keine verdächtigen Downloads.
* Keine externen Binärdateien ohne ausdrücklichen Grund.
* Keine unnötigen Dependencies.
* Keine obskuren Libraries.
* Keine automatischen Lizenzverletzungen durch kopierte Assets.
* Keine Builds committen.
* Keine IDE-Caches committen.
* Keine temporären Dateien committen.

## Build- und Test-Regeln

Nach relevanten Änderungen:

```bash
./gradlew build
```

Unter Windows:

```bat
gradlew.bat build
```

Falls der Build fehlschlägt:

1. Fehlermeldung lesen.
2. Ursache identifizieren.
3. Minimalen Fix implementieren.
4. Build erneut ausführen.
5. Erst nach erfolgreichem Build committen.

## Ausgabeformat nach jeder Arbeitsphase

Fasse nach jeder abgeschlossenen Arbeitsphase kurz zusammen:

```text
Summary:
- Implemented: ...
- Changed files: ...
- Build: passed/failed
- Git: committed/not committed
- Repository: pushed/not pushed
- Next recommended step: ...
```

## Wichtige Grundregel

Mechaniken dürfen von allgemeinen MMORPG-Konzepten inspiriert sein.

Konkrete Inhalte aus bestehenden Spielen dürfen nicht kopiert, nachgebaut oder leicht
umbenannt werden.

Das Projekt muss eigenständig genug sein, dass es ohne Bezug auf ein bestehendes Spiel
beschrieben, veröffentlicht und weiterentwickelt werden kann.
