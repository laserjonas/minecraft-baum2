# Handoff

Read this first. It reflects the state as of the latest commit so a fresh Claude Code
session (yours or a co-author's) can pick up work without re-deriving context from
`git log`. Updated after every commit — see "Git Rules" in `CLAUDE.md`.

## Current state

- Fabric mod builds successfully (`./gradlew build` passes).
- Client runs: `./gradlew runClient` loads, reaches the main menu, and joins a world cleanly
  (verified clean boot and clean world-join, no Mixin/payload/HUD-registration errors).
- **Life, Mana, and a 4-attribute system (Endurance/Intelligence/Strength/Dexterity) —
  implemented, HUD reworked, vanilla heart removed, Character Stats screen ('C' key):**
  - `progression/VitalsCurve.java` is the single source of truth for every formula below.
    Every attribute starts at `STARTING_ATTRIBUTE_POINTS` (5) and grants
    `ATTRIBUTE_POINTS_PER_LEVEL` (1) unspent point per level-up, for the player to invest via
    a "+1" button in the Stats screen (`AttributeManager.trySpendPoint`, validated
    server-side, never trust the client). Max level 100, so max attainable in one attribute
    via pure leveling is 5+99=104.
  - **Life is no longer level-based** — an earlier session's `500 + 10*level` formula was
    explicitly replaced (user-confirmed decision) with a purely **Endurance**-driven one:
    `getMaxLife(endurance) = 500 + 20*(endurance-5)` (500 at start, 2480 at max Endurance
    104). Still real vanilla health (`EntityAttributes.MAX_HEALTH`), rescaled via
    `VitalsManager.applyMaxLife` so combat/damage/death/regen keep working unchanged.
    Endurance also drives Life Regen (`getLifeRegen`, 0.25/sec at start, applied once/sec in
    `VitalsManager.regenLife`). **Vanilla clamps this attribute's effective value at 1024** —
    fixed via an accessor Mixin (`mixin/ClampedEntityAttributeAccessor.java`) widening the
    clamp once at mod init; currently set to **4096** (bumped up from an earlier session's
    2048, to cover the new higher real max of 2480 with headroom — see
    `VitalsManager.widenMaxHealthCeiling()`, called first in `Baum2.onInitialize()`).
  - **Strength** drives Base Attack + Physical Defence (`getBaseAttack`/`getPhysicalDefence`,
    both `5 + 1.0*(str-5)`). **Intelligence** drives Base Magic Attack + Magic Defence
    (`getBaseMagicAttack`/`getMagicDefence`, same shape). **Dexterity** drives Attack Speed
    Multiplier, Cast Speed Multiplier (`1.0 + 0.01*(dex-5)`, capped at `MAX_SPEED_MULTIPLIER`
    = 3.0) and Crit Chance (`5 + 0.5*(dex-5)`, capped at `MAX_CRIT_CHANCE` = 75%). **None of
    these caps actually bind via pure leveling alone** (e.g. max attainable Crit Chance is
    54.5%, not 75%) — they're intentional headroom for a future gear/skill system that might
    push an attribute's *effective* value higher, not a currently-reachable limit. **No
    `combat/` package exists yet, so none of these combat-facing formulas actually affect
    damage/defence/speed/crit in-game right now** — display-only plumbing, same as Mana.
  - Mana is **unchanged** from the previous session (still level-based, `100 + 5*level`,
    NOT attribute-driven — no attribute was ever said to drive Mana).
  - Networking: `networking/AttributeSyncPayload.java` (S2C, synced every tick alongside
    Mana) carries only the 4 raw attribute ints + unspent points — **derived stats are NOT
    synced separately**, the client computes them locally by calling the same
    `VitalsCurve` methods (shared common-sourceset code), exactly like it already does for
    total XP. `networking/SpendAttributePointPayload.java` is this project's **first C2S
    payload** (`PayloadTypeRegistry.playC2S()`, `ServerPlayNetworking.registerGlobalReceiver`
    reading `context.player()` as the sender) — sent when a Stats-screen "+1" button is
    clicked. The old `Base Damage`/`Base Magic Damage` flat-5.0 fields and
    `CombatStatsSyncPayload` from the previous session are **gone**, fully superseded by the
    Strength/Intelligence formulas above.
  - HUD (unchanged from previous session): `ui/VitalsHud.java` replaces the vanilla heart bar
    in place (`HudElementRegistry.replaceElement(VanillaHudElements.HEALTH_BAR, ...)` — **not**
    `removeElement`) and adds a Mana bar above it. Colors/dimensions in
    `docs/visual-style-guide.md`.
  - Character Stats screen: pressing 'C' (`ui/Baum2KeyBindings.java`, `KeyBinding.Category
    .MISC`) opens `ui/CharacterStatsScreen.java`, a full menu `Screen` built on vanilla's real
    `Tab`/`TabManager`/`TabNavigationWidget`/`GridScreenTab` system, one tab ("Stats") with
    ~15 rows: Life, Mana, Unspent Points, then each attribute (with its "+1" button)
    interleaved with its derived stats. Pressing 'C' again closes it
    (`CharacterStatsScreen.keyPressed` matching the same `KeyBinding`). Has its own opaque
    panel background (`renderDarkening` override) and a `refreshGrid` override that top-aligns
    content instead of vanilla's default centering anchor. **The `StatsTab`'s grid is now
    wrapped in a `ScrollableLayoutWidget`** (`net.minecraft.client.gui.widget
    .ScrollableLayoutWidget`) so all ~15 rows stay reachable (scrollbar/wheel/drag, fully
    automatic, vanilla's own scrollbar look) even when content is taller than the available
    tab area (high GUI Scale, small window) — before this, bottom rows (Dexterity's derived
    stats) were simply unreachable, a real bug caught via a second real screenshot. See "Last
    change" for both this and the header-overlap fix, both found via user screenshots, not
    guessed. Colors/row order/layout documented in `docs/visual-style-guide.md`'s "Character
    Stats Screen" section.
  - A fifth subagent, `graphics-designer`, was added (pulled in from `jonas_workbranch`, which
    had it but hadn't merged it yet) and used to produce the Life/Mana/Stats-screen visual
    specs — see `docs/visual-style-guide.md`.
- Package: `de.baum2dev.baum2` / Main: `Baum2` / Client: `Baum2Client`.
- Minecraft 1.21.11 / Yarn 1.21.11+build.6 / Fabric API 0.141.4+1.21.11 / Fabric Loom 1.17.13 / Java 21.
- **Progression System — FULLY WORKING, including real-time client display:**
  - Custom progression uses our own XP curve, centralized in `progression/ProgressionCurve.java`
    (single source of truth — `ExperienceManager`, `ProgressionTickHandler`, `PlayerLevelSystem`,
    `LevelUpHandler`, and the client packet handler all call into it instead of each having
    their own copy): `xpRequiredForLevel(L) = 80 + 40L + 8L²` — a "hardcore grind" pace chosen
    deliberately steeper than vanilla Minecraft's own curve (see "Last change" below for why).
  - Features: `/baum2 addxp <amount>`, `/baum2 level`, mob XP drops (10 + max_health/2), level-up
    broadcasts, vanilla XP orb drops disabled via Mixin.
  - **Real-time client sync now works** via a custom S2C packet sent every server tick — see
    "Networking API reference" section below for the exact API and why earlier attempts failed.
  - **Persistence now works** via Fabric's Data Attachment API (`fabric-data-attachment-api-v1`,
    already a dependency) — progression survives server restarts, disconnects, and death. See
    "Last change" and "Attachment API reference" below.
- Repo: https://github.com/laserjonas/minecraft-baum2 (public).
- **Branches**: `master` is now the merge of both work branches (see "Last change" below).
  `fischey_workbranch` and `jonas_workbranch` still exist and still track their respective
  remotes for any follow-up work, but master has absorbed everything from both as of this
  commit — start new work from `master` rather than the old work branches unless there's a
  reason to keep working on one specifically.
- `.vscode/` is checked in (extensions.json, settings.json, tasks.json) so fresh checkout gets Java+Gradle
  recommendations and "Run Minecraft Client" task (`Ctrl+Shift+B`) out of the box.
- Five subagents under `.claude/agents/` (shared via git, so both contributors get them):
  `fabric-docs-researcher` (Fabric/MC API research -> `docs/fabric-modding.md`),
  `ip-naming-compliance-checker` (reviews new names/text against IP/naming rules),
  `balance-reviewer` (internal-consistency/exploit review of numeric balance values),
  `merge-integration-reviewer` (pre-merge overlap/design-conflict check between branches),
  `graphics-designer` (produces visual/HUD specs and assets, maintains
  `docs/visual-style-guide.md` — the exception that *does* write files, see below).
  The first four report findings only — they don't edit files. See `CLAUDE.md` -> "Project
  Agents" for exact trigger conditions; use them proactively, don't wait to be asked.
  Still not yet run against the progression system's balance values / player-facing strings —
  see "Next recommended step".
- **Known limitation**: a running Claude Code session loads its available agent list at
  startup, so newly added `.claude/agents/*.md` files aren't picked up mid-session — they
  become available the next time a session starts fresh (restart, or a fresh session after
  pulling). If an agent invocation fails with "Agent type not found" right after one was
  added, that's why — not a bug in the agent definition.
  - Update: in at least one environment (the VS Code extension host), custom `.claude/agents/`
    subagents were **never** available via the `Agent` tool's `subagent_type`, even in a
    freshly started session with the files already present on disk — only the built-in types
    (`claude`, `claude-code-guide`, `Explore`, `general-purpose`, `Plan`, `statusline-setup`)
    showed up. Workaround used successfully: read the target `.claude/agents/<name>.md` file
    yourself, then dispatch a `general-purpose` agent whose prompt reproduces that file's role/
    instructions/context verbatim. This gets the same review quality without needing the
    dedicated subagent type. If a future session finds custom types *do* resolve, prefer that
    directly — this workaround is a fallback, not the preferred path.

## Last change (on `fischey_workbranch`, not yet merged to `master`)

Added scrolling to the Character Stats screen after a second round of user screenshot feedback:

- After the previous session's header-overlap and opaque-background fixes, the user sent
  another real screenshot: "The UI is still not optimal... pixels should not scale with the
  UI, it should be just readable. If there is not enough place, create scrollbar. If
  scrollbar is not supported make senseful extra sub-tabs." The screenshot itself showed the
  actual bug plainly: content was cut off mid-row at "Dexterity", with Attack Speed/Cast
  Speed/Crit Chance below it completely unreachable — not a style complaint, a real
  reachability bug (at a high GUI Scale or small window, ~15 rows can be taller than the
  available tab area).
- Researched whether vanilla 1.21.11 has a ready-to-use scrollable container before building
  anything custom (persisted to `docs/fabric-modding.md`'s "GUI / Screens" section): confirmed
  `net.minecraft.client.gui.widget.ScrollableLayoutWidget` wraps **any** `LayoutWidget`
  (`GridWidget` already implements `LayoutWidget`, so no conversion needed), and handles
  scrollbar rendering, mouse wheel, and drag-to-scroll **fully automatically** using vanilla's
  own `widget/scroller`/`widget/scroller_background` textures — no manual `DrawContext` code.
  Real vanilla precedent: `ExperimentsScreen` uses the exact same class (on a plain `Screen`,
  not inside a `Tab` — there's no vanilla example combining it with `GridScreenTab`
  specifically, but the wiring only needed the confirmed public API, not a copied pattern).
- Implementation (`ui/CharacterStatsScreen.java`'s `StatsTab`): added a
  `ScrollableLayoutWidget` field wrapping the existing `this.grid` (row-building code
  completely unchanged), overrode `forEachChild` to register the wrapper's single `Container`
  widget instead of the grid's rows directly, and changed `refreshGrid` to size/position the
  wrapper (not the grid) within the tab area. `refreshValues()` needed **no changes at all** —
  it already mutates the same `TextWidget` field instances, which are now just
  scrolled/clipped by the wrapper rather than repositioned.
- Chose real scrolling over the "sub-tabs" fallback the user also explicitly sanctioned,
  since the confirmed API made it a small additive change (one field, two overridden methods)
  with zero new UI surface for the user to navigate — sub-tabs remain a valid lower-risk
  fallback if scrolling turns out to misbehave in practice (documented in "Next recommended
  step" below), but there was no concrete reason to prefer it here.
- Did not attempt to make the screen's rendering scale independent of the player's GUI Scale
  setting (a literal reading of "pixels should not scale with the UI") — that would require
  a matrix-transform trick to counter-scale rendering while keeping mouse-click hit-testing
  coordinates correctly aligned, which is a meaningfully riskier change (misaligned
  hit-testing would silently break the "+1" buttons) for a problem that scrolling already
  solves robustly regardless of *why* content overflowed. If a future session wants literal
  GUI-Scale independence rather than "always reachable via scroll," that's a separate,
  larger piece of work — flagged in "Next recommended step," not attempted here.
- Verified: `./gradlew build` passes, `runClient` boots cleanly with no Mixin/crash errors.
  **Not re-confirmed visually** — same no-GUI-automation limitation as every prior UI change
  this session; the next person to open the Stats screen at a high GUI Scale (or shrink the
  window) should confirm the scrollbar appears and all rows through Crit Chance are reachable.

Earlier, still on `fischey_workbranch`:

Added a 4-attribute system (Endurance/Intelligence/Strength/Dexterity) and reworked the
Character Stats screen to match, then fixed two UI bugs the user caught via a real screenshot:

- User request: 4 attributes, each starting at 5, gaining 1 point per level-up (player
  chooses which to invest in). Endurance -> Life + Life Regen; Intelligence -> Base Magic
  Attack + Magic Defence; Strength -> Base Attack + Physical Defence; Dexterity -> Attack/Cast
  Speed + Crit Chance. Base values given explicitly: Life Regen 0.25, Phys/Magic Defence 5
  each, Attack/Cast Speed Multiplier 1.0x each, Base Crit Chance 5%. User explicitly delegated
  the actual scaling-per-point formulas ("make the balance on yourself").
- Two decisions confirmed with the user before implementing (both changed the shape of the
  work materially, not just numbers, so worth a quick check rather than guessing): Life
  becomes **purely** Endurance-driven, replacing the old level formula entirely (not additive)
  — this was the bigger of the two, since it discards a formula from an earlier session that
  had already been through balance review; and attribute points are 1-per-level, spent freely
  any time via the Stats screen (not a forced choice at the moment of leveling up).
- My formulas (`progression/VitalsCurve.java`, see "Current state" above for the actual
  numbers) all follow one pattern: `base_at_start + coefficient * (attribute - 5)`, calibrated
  so plugging in the starting value (5) reproduces each stat's given base value exactly.
- New files: `progression/AttributeType.java` (enum), `progression/AttributeManager.java`
  (grants points on level-up via `ExperienceManager.levelUp()` - chosen over the more fragile
  `LevelUpHandler.checkLevelUp()` detection layer, since `levelUp()` fires for every level
  gained regardless of source, mob-kill or `/baum2 addxp`, while `checkLevelUp` is only ever
  invoked from `MobDeathHandler`; validates spending server-side, never trusts the client),
  `networking/AttributeSyncPayload.java` (S2C), `networking/SpendAttributePointPayload.java`
  (this project's first C2S payload). Deleted: `networking/CombatStatsSyncPayload.java` (fully
  superseded — Base Attack/Magic Attack are now computed from Strength/Intelligence instead of
  being an independently-stored flat value).
- **`balance-reviewer` pass (via the general-purpose-agent workaround, see "Known limitation"
  above) found two real issues, both fixed**:
  1. Vanilla's `MAX_HEALTH` clamp (widened to 2048 last session) wasn't enough headroom for
     the new real max Life (2480 at Endurance 104) — bumped to 4096.
  2. Crit Chance had a 75% safety cap but Attack/Cast Speed Multiplier didn't, despite both
     being equally uncapped-by-formula — added a matching `MAX_SPEED_MULTIPLIER` (3.0) cap to
     both, for consistency. Neither cap actually binds via pure leveling alone (confirmed by
     the reviewer via real arithmetic, not eyeballed) — both exist as headroom against a future
     gear/skill system pushing an attribute's effective value higher, which is intentional, not
     a bug — just flagging so a future reader doesn't "fix" the caps to make them bind sooner.
     Also fixed a stale doc comment in `mixin/ClampedEntityAttributeAccessor.java` that still
     cited the old level-based Life formula and its old 1500 max.
- Verified: `./gradlew build` passes, `runClient` boots and joins a world cleanly, no
  Mixin/attachment/codec errors (the `optionalFieldOf` lesson from the Mana codec bug was
  applied from the start this time for all 5 new persisted fields — endurance/intelligence/
  strength/dexterity/unspentAttributePoints — so no repeat of that data-loss bug).
- **The user then sent an actual in-game screenshot of the Stats screen and said "The UI must
  be improved" — this caught two real, concrete bugs that build success and clean-boot
  verification could never have caught, since they're pure rendering/layout issues:**
  1. **The tab header visibly overlapped the Life/Mana rows.** Root cause, confirmed by
     reading `SimplePositioningWidget.setPos`'s actual source (not guessed): vanilla's
     `GridScreenTab.refreshGrid` default centers content using a vertical anchor 1/6 down from
     the top of the tab area, computed as `tabArea.top + lerp(1/6, 0, tabArea.height -
     gridHeight)`. That assumes content shorter than the available area. With ~15 rows, the
     grid is often **taller** than the tab area, making `(tabArea.height - gridHeight)`
     **negative** — the lerp then produces a negative offset, pushing the grid's top edge
     *above* the tab area's top boundary, directly into the header. Fixed by overriding
     `StatsTab.refreshGrid` to top-align (`relativeY = 0.0F`) instead of using the inherited
     default, plus a small fixed top-padding constant so content doesn't sit flush against the
     header. **General lesson: `GridScreenTab`'s default centering anchor is only safe for
     content you're confident is shorter than the tab area — override `refreshGrid` for
     anything that might grow past a single screen's worth of rows**, which a Stats screen
     with more attributes/tabs planned later definitely will.
  2. **The screen barely read as a screen** — vanilla's default `renderDarkening` is a subtle
     translucent gradient, fine for dimming gameplay slightly behind a typical menu, but far
     too weak against a bright sky (the screenshot showed clouds/trees/hotbar all clearly
     visible "through" the panel). Fixed by overriding `renderDarkening` to draw a solid
     near-opaque fill instead — this is exactly what vanilla's own `StatsScreen` does too (it
     overrides `renderDarkening` to draw an actual header-background texture plus its own
     darkening region), confirmed by reading that class's real source rather than assuming the
     default was fine. Also shrank spacer-row height and "+1" button size slightly for a
     tighter, less sparse layout.
  3. Re-verified after both fixes: `./gradlew build` passes, `runClient` boots/joins cleanly,
     no new crashes. **Still not re-confirmed visually** — same no-GUI-automation limitation as
     before, this environment can't screenshot the native Minecraft window itself. The next
     person to open the Stats screen should confirm the header no longer overlaps and the
     panel background reads as solid/opaque.

Earlier, still on `fischey_workbranch`:

Added Base Damage / Base Magic Damage stats and a 'C'-key Character Stats screen:

- User request: two new flat (not level-scaled) combat stats, both starting at `5.0`, plus a
  screen toggled by 'C' showing Current/Max Life, Current/Max Mana, Base Damage, Base Magic
  Damage, with a "Stats" tab (more tabs planned later, only one needed for now).
- New/changed files: `progression/VitalsCurve.java` (added `getBaseDamage()`/
  `getBaseMagicDamage()`, both flat `5.0f` constants), `progression/PlayerProgressData.java`
  (added `baseDamage`/`baseMagicDamage` `Codec.FLOAT` fields via `optionalFieldOf` — same
  backward-compat rule as Mana, see the entry below), `networking/CombatStatsSyncPayload.java`
  (new S2C payload, sent once on join from `events/VitalsTickHandler.java`'s JOIN handler, not
  every tick like Mana since these don't change on their own), `ui/Baum2KeyBindings.java` (new,
  registers the 'C' `KeyBinding` via `KeyBindingHelper`, category `KeyBinding.Category.MISC`),
  `ui/CharacterStatsScreen.java` (new, the actual screen), `assets/baum2/lang/en_us.json` (new
  — first lang file in the project, just the one keybinding's display name so it doesn't show
  as a raw untranslated key in the Controls menu).
- **API research before implementing** (persisted to `docs/fabric-modding.md`'s new "Input /
  Keybindings" and "GUI / Screens" sections): confirmed `KeyBinding.Category` is a
  `record Category(Identifier id)` in this version, not a bare string like older tutorials
  show; confirmed vanilla exposes a real, mod-usable tab system
  (`net.minecraft.client.gui.tab.Tab`/`GridScreenTab`/`TabManager`,
  `net.minecraft.client.gui.widget.TabNavigationWidget`) used by vanilla's own "Statistics"
  screen, and used it directly rather than hand-rolling a tab-button row, since more tabs are
  explicitly planned; confirmed `Screen.keyPressed` takes a `KeyInput` record
  (`record KeyInput(int key, int scancode, int modifiers)`) in 1.21.11, not the older
  `(int keyCode, int scanCode, int modifiers)` triplet, and that `KeyBinding.matchesKey(KeyInput)`
  is the correct way to check "did this keypress match my registered keybind" against it.
- **Bug, caught only by actually running `runClient` and opening the screen** (again: not a
  compile error, `./gradlew build` was green the whole time) — a real key press against a real
  `MinecraftClient` window during a `runClient` smoke-test session crashed the game with
  `IllegalStateException: Can only blur once per frame` inside `CharacterStatsScreen.render`.
  Root cause: `CharacterStatsScreen.render()` called `this.renderBackground(...)` explicitly —
  but the framework (`GameRenderer`'s screen-render pipeline) **already calls
  `Screen.renderBackground(...)` once before invoking `render()`**; `renderBackground()`
  internally calls `applyBlur()`, and applying the blur pass twice in one frame throws. Fix:
  removed the explicit `renderBackground()` call from `render()` entirely — a Screen's own
  `render()` override should only draw its *own* additional content (and call
  `super.render(...)` to render registered drawables), never re-invoke `renderBackground()`
  itself. **General lesson for any future custom `Screen`: don't call `renderBackground()` from
  inside `render()`— the framework already renders the background before `render()` runs.**
  Re-verified: `./gradlew build` passes after the fix; could not re-trigger an actual second
  keypress inside this environment to get a second live repro (no GUI-automation tool can
  script input into the native LWJGL window - see the HUD entry below for the same limitation),
  but the fix directly addresses the confirmed double-`applyBlur()` root cause, verified against
  decompiled `Screen.java`/`DrawContext.java` source, not guessed.
- Verified: `./gradlew build` passes. `runClient` boots and reaches a joined world cleanly with
  the new keybinding/screen code loaded (no Mixin/registration errors at boot — the crash above
  only happens when the screen is actually opened, which was caught via one real keypress
  during testing, then fixed and could not be re-triggered on demand afterward for a second
  confirmation — see limitation above). **Not visually confirmed**: the actual in-game look of
  the Stats screen (colors, spacing, tab bar) per `docs/visual-style-guide.md` — same
  no-GUI-automation limitation as the HUD bars below. Next person to play should press 'C' and
  eyeball it.

Earlier, still on `fischey_workbranch`:

Added Life and Mana systems plus a HUD rework, replacing the vanilla heart bar:

- User request: rework Life, add Mana "for future use," show both on the HUD, remove the
  vanilla heart. Formulas given explicitly: Life = 500 + 10/level, Mana = 100 + 5/level.
- Scoping decisions made with the user up front (see conversation, not re-derivable from code):
  no class-name banner/level-diamond badge yet (no `classes/` package exists — deferred, out of
  scope for this change); Life scales the *real* vanilla max-health attribute rather than being
  a fully decoupled custom stat (keeps combat/damage/death/regen working with zero extra code);
  Mana gets passive regen now even though nothing consumes it yet (a rate, not zero, so the
  system isn't inert — see `VitalsManager.regenMana`).
- New files: `progression/VitalsCurve.java` (formulas), `progression/VitalsManager.java`
  (applies Life, clamps/regens Mana, widens the health-attribute clamp), `events/
  VitalsTickHandler.java` (per-tick apply/regen/sync + join hook), `networking/
  ManaSyncPayload.java` (S2C sync, mirrors `ExperienceSyncPayload`), `ui/VitalsHud.java`
  (the actual HUD element, replaces dead `ui/ProgressionHud.java`), `mixin/
  ClampedEntityAttributeAccessor.java` (see bug below), `docs/visual-style-guide.md` (new,
  written by the `graphics-designer` agent), `.claude/agents/graphics-designer.md` (pulled in
  from `jonas_workbranch`, which had added it but hadn't merged it into `master` yet — zero
  conflict, docs-only file, brought over directly rather than via a branch merge).
- **Bug #1, caught by static/formula review (the project's own `balance-reviewer` agent, run
  via the workaround above since the subagent type doesn't resolve in this environment): vanilla
  hardcodes `EntityAttributes.MAX_HEALTH` as a `ClampedEntityAttribute` with max 1024**
  (confirmed via decompiled source, not assumption). The Life formula reaches 1500 at level
  100, so everything past level ~53 would have silently computed a bigger number with zero
  actual in-game effect — the health bar would just stop growing, with nothing logged. Fixed
  with `mixin/ClampedEntityAttributeAccessor.java` (a `@Mutable @Accessor("maxValue")` on
  `ClampedEntityAttribute`), widening the clamp to 2048 once via `VitalsManager
  .widenMaxHealthCeiling()`, called first thing in `Baum2.onInitialize()`. If a future balance
  pass pushes Life past 2048, raise that constant too — it's not derived from `VitalsCurve`
  automatically, they're two separate numbers that both need to stay ahead of the real max.
- **Bug #2, caught only by actually running `./gradlew runClient` and joining a save** (the
  `verify` workflow — this would **not** have been caught by `./gradlew build` alone, since it's
  a runtime data-loss bug, not a compile error): the initial `PlayerProgressData` Codec used
  `Codec.INT.fieldOf("Mana")` (a *required* field) for the new `mana` field. Loading any
  pre-existing save (one predating this change) failed to decode the **entire** attachment —
  not just the missing field — because `RecordCodecBuilder`'s `instance.group(...)` fails the
  whole record if any required field is absent. Confirmed directly: loaded a local level-45 dev
  test save, saw `[fabric-data-attachment-api-v1] Skipping invalid attachments: No key Mana in
  MapLike[...]` in the log, and the player fell back to the initializer default (level 1) —
  the exact same silent-reset failure mode documented earlier in this file for a completely
  different root cause (lazy class loading). **General lesson, in addition to the existing one
  about force-loading attachment classes: adding a new field to an already-`persistent()`
  attachment Codec must use `optionalFieldOf(name, default)`, never `fieldOf(name)`, or every
  save predating the new field silently loses *all* of its data, not just the new field.** Fixed
  by switching to `Codec.INT.optionalFieldOf("Mana", 100)`; re-verified with the same save file
  that the codec now decodes successfully (no "Skipping invalid attachments" warning) and
  `VitalsManager` corrects the fallback value up/down within a tick or two regardless.
- The local dev-sandbox save this was tested against (`run/saves/...`, gitignored, disposable
  test data per the project's existing convention — see the `usercache.json`/`Baum2Dev` note
  elsewhere in this file) had its level-45 test character actually reset to level 1 by Bug #2
  during verification, before the fix landed. Not attempted to recover (scratch data, not
  source-controlled) — flagging only so it isn't mistaken for a report of the bug still being
  live; the fix was verified against the same save afterward and no further resets occurred.
- HUD implementation notes (Fabric API 0.141.4+1.21.11 specifics, verified via decompiled
  sources — see `docs/fabric-modding.md`'s new "HUD element registry" section for full detail):
  the old `HudLayerRegistrationCallback`/`IdentifiedLayer` API from older Fabric API versions
  **does not exist** in this version; the current API is `HudElementRegistry` +
  `VanillaHudElements` + `HudStatusBarHeightRegistry`, all in
  `net.fabricmc.fabric.api.client.rendering.v1.hud`. Tried `HudElementRegistry.removeElement
  (VanillaHudElements.HEALTH_BAR)` first — **this crashes the client at startup** with
  `IllegalStateException: Unregistered hud elements: [minecraft:health_bar]`, because the
  framework validates that every id with a registered `StatusBarHeightProvider` still has a
  matching `HudElement`, and removing health_bar entirely leaves its built-in height provider
  dangling. Fix: use `replaceElement(VanillaHudElements.HEALTH_BAR, old -> ourRenderer)`
  instead (keeps the vanilla id "occupied" so the built-in height-provider entry stays valid),
  and explicitly re-register that id's height provider too via `HudStatusBarHeightRegistry
  .addLeft(VanillaHudElements.HEALTH_BAR, ...)` — vanilla's own default height provider for
  that id does a dynamic multi-row heart-count calculation that no longer means anything once
  hearts aren't being drawn there. The new Mana bar is a genuinely new element (own
  `Identifier`), inserted via `attachElementBefore(VanillaHudElements.ARMOR_BAR, ...)` so armor
  still renders (untouched) and gets pushed up to make room.
- Verified: `./gradlew build` passes; `./gradlew runClient` boots cleanly and joins a world
  with no Mixin/HUD/attachment errors (post-fix). **Not visually confirmed** — this environment
  has no GUI-automation tool that can drive the actual Minecraft window (it's a native LWJGL
  app, not a browser/Electron app the `run`/Playwright tooling can screenshot), so the on-
  screen bar positions/colors from `docs/visual-style-guide.md` haven't been eyeballed in-game
  yet. That's expected manual follow-up, not a gap in this session's verification.

Earlier, still on `fischey_workbranch`:

Rebalanced the XP curve and renamed `VanillaXpFormula` → `ProgressionCurve`:

- Why: with persistence and real-time sync finally both working (see entries below), the
  underlying numbers became visible as a real problem — vanilla's own curve is calibrated for
  vanilla's tiny XP sources (1-7 XP per pickup). Our `MobDeathHandler` grants 10-60+ XP per
  kill and testing routinely used `/baum2 addxp` with amounts in the hundreds, so under the old
  curve a handful of kills could jump a player from level 1 to level 8. User asked for the curve
  to be rebalanced with the explicit requirement that every level cost strictly more than the
  last (the old curve was *technically* already monotonic, just far too gently scaled for our
  reward economy — see the numbers below).
- Asked the user to pick a pacing philosophy with concrete kills-to-level numbers (assuming
  ~20 XP/kill) rather than guessing — they picked "slow/hardcore grind."
- New formula, in `progression/ProgressionCurve.java`:
  `xpRequiredForLevel(level) = 80 + 40*level + 8*level²`. Concretely: Level 1 costs 128 XP
  (~6 kills), Level 10 costs 1280 XP (~64 kills), Level 50 costs 22,080 XP (~1,104 kills),
  Level 100 costs 84,080 XP (~4,200 kills for that one level). Cumulative total to reach
  level 100 from scratch: ~2,916,800 XP (~146,000 kills lifetime) — reaching max level is meant
  to be a serious, long-term achievement.
  `getTotalXpForLevel(level)` is now a simple loop-sum of `getXpRequiredForLevel(1..level)`
  rather than a closed-form cumulative formula — simpler and safer than deriving/maintaining a
  closed form by hand, and `level` maxes out at 100 (`ExperienceManager.getMaxLevel()`) so the
  loop is trivially cheap even though it's called every server tick per player (for the
  `totalExperience` sync field — see "Networking API reference").
- Renamed the class (and file) because it's no longer vanilla's actual formula — keeping the
  old name would have misled a future reader, especially since this same file used to spell out
  vanilla's real per-tier formula in comments/docs. Updated every reference (`ExperienceManager`,
  `ProgressionTickHandler`, `PlayerLevelSystem`, `LevelUpHandler`, `ClientNetworkingHandler`).
- Note: this change is purely about the *number* of XP required per level. It does not affect
  the vanilla-bar-fill display mechanism at all — the client's bar-fill percentage is computed
  directly from our own current/max XP values (see "Attachment"/"Networking" sections), never
  from vanilla's actual formula, so any future curve rebalance is similarly safe to do freely.

Earlier, still on `fischey_workbranch`: fixed the *actual* persistence bug. The fixed
dev-username change below (previous "Last change"
entry) was real and necessary but **not sufficient** — user re-tested with the stable `Baum2Dev`
identity, gained XP, rebuilt, relaunched, and still lost progress. This is the real root cause:

- Root cause: `PlayerLevelSystem`'s `AttachmentType<PlayerProgressData> PROGRESSION` field is a
  Java static field, initialized lazily the first time something calls a static method on that
  class. Nothing in `Baum2.onInitialize()` touched `PlayerLevelSystem` directly — the only
  references were inside event/command callback *bodies* (e.g. `LevelUpHandler`'s JOIN handler
  calling `PlayerLevelSystem.getPlayerLevel(...)`), which don't execute until a player actually
  joins. But Fabric's attachment deserialization (`EntityMixin.readEntityAttachments`, hooked
  into the entity's own NBT-read process) runs *during* that same join sequence, and by decompiling
  `AttachmentSerializingImpl` it's confirmed this deserialization looks up the `AttachmentType` by
  `Identifier` in a global registry at that exact moment — if our type wasn't registered yet
  (because `PlayerLevelSystem` hadn't loaded yet), the persisted `baum2:progression` NBT entry is
  silently dropped, and the next `getAttachedOrCreate` call falls back to `initializer()`'s fresh
  `PlayerProgressData` (level 1). That fresh default then gets written back on disconnect,
  **overwriting** the real saved progress with the reset value.
- This was confirmed, not guessed: dumped the raw NBT of an affected player file with a small
  ad-hoc NBT reader (gzip + manual tag parsing, no library needed) and found `.dat_old` (the
  previous save) correctly held `Level=5`, while the current `.dat` had been overwritten back to
  `Level=1` — proving the write path was always fine and the *read* was silently failing exactly
  once, then persisting that failure.
- Fix: added `PlayerLevelSystem.bootstrap()` (a no-op method whose only purpose is to force the
  class to load) and call it as the **first line** of `Baum2.onInitialize()`, guaranteeing the
  attachment type is registered before any world or player can possibly load.
- Verified end-to-end, not just via clean-boot logs this time: restored a known
  `Level=5`/`Experience=1` player save as the "current" `.dat`, relaunched with the fix in place,
  and confirmed (both via chat log and by re-dumping the NBT afterward) that the player correctly
  loaded back in at level 5 rather than resetting. The user then independently re-tested manually
  (gain XP → rebuild → relaunch → gain more XP → rebuild → relaunch) and confirmed it holds.
- General lesson for this codebase: **any `AttachmentType` (or similar static-registered thing
  whose registration must happen before world load) must be touched by an explicit call from
  `Baum2.onInitialize()`, not merely referenced inside a lambda/callback body that registers
  itself at init time but only executes later.** If a future attachment/registry addition shows
  the same "works this session, silently resets next load" symptom, check this first.

Earlier, still on `fischey_workbranch`: fixed a dev-environment gotcha that looked exactly like a
persistence bug (**but wasn't the full story** — see above): user reported "level resets when I
rebuild, but not on a plain restart of the same world."

- Root cause: Fabric Loom's `runClient` assigns a fresh random `PlayerNNN` username (and
  therefore a fresh UUID, since offline/dev UUIDs are derived from the username) on **every
  single launch** unless a fixed one is configured. Confirmed empirically —
  `run/usercache.json` had 29 distinct `PlayerNNN` entries after one afternoon of testing.
  Since progression is (correctly) stored per-player-UUID via the attachment added in the change
  below, every fresh `runClient` launch was really a brand new player joining, not the same
  player losing data. A plain "restart the world" without stopping/restarting the Gradle task
  keeps the same client session and thus the same random username, which is why *that* case
  looked fine and only the rebuild-then-relaunch case looked broken.
- Fix: `build.gradle`'s `loom { runs { client { ... } } }` now sets a fixed dev identity via
  `programArg("--username")` / `programArg("Baum2Dev")` / `programArg("--uuid")` /
  `programArg("<fixed-offline-uuid>")`. The UUID is the standard offline-player UUID for that
  username (`UUID.nameUUIDFromBytes(("OfflinePlayer:" + name).getBytes(UTF_8))`), computed once
  and hardcoded — it doesn't need to be recomputed, it just needs to stay stable.
  - Gotcha while implementing: `RunConfigSettings.getProgramArgs()` returns an **immutable**
    `List<String>` in Loom 1.17.13 — calling `.add(...)` on it throws
    `UnsupportedOperationException` with no message. Use the singular `programArg(String)`
    method (one call per argument) instead, not `programArgs.add(...)`.
- Verified: `runClient` log now shows `Setting user: Baum2Dev` and `Baum2Dev joined the game`
  on every launch, instead of a random name.
- Note for contributors: this only fixes identity stability inside *this* dev environment's
  `run/` directory. It has no effect on a real server/launcher session, where usernames/UUIDs
  come from the actual (online-mode or configured-offline) auth flow, not this dev-only arg
  injection — nothing to reconcile there.

Earlier, still on `fischey_workbranch`: persisted progression data via Fabric's Data Attachment
API instead of an in-memory `HashMap`:

- `PlayerProgressData` gained a `com.mojang.serialization.Codec<PlayerProgressData>` (via
  `RecordCodecBuilder`), replacing the old unused manual `writeNbt`/`readNbt` methods (they were
  dead code — never called anywhere).
- `PlayerLevelSystem` now declares `PROGRESSION`, an `AttachmentType<PlayerProgressData>`
  registered via `AttachmentRegistry.create(Identifier, Consumer<Builder<A>>)` with
  `.persistent(CODEC)`, `.copyOnDeath()`, and `.initializer(PlayerProgressData::new)`.
  `getPlayerProgress`/`savePlayerProgress`/`clearPlayerProgress` now delegate to
  `player.getAttachedOrCreate(PROGRESSION)` / `setAttached(...)` / `removeAttached(...)` instead
  of a UUID-keyed map.
- Why: attachments hook directly into vanilla's own player-NBT read/write cycle (the same one
  that saves inventory, health, etc.), so progression now survives disconnects, server restarts,
  and — via `copyOnDeath()` — player death, with no manual join/disconnect save/load code needed.
- Note: used `AttachmentRegistry.create(id, consumer)`, not `AttachmentRegistry.builder()` —
  the latter compiles but is `@Deprecated` (confirmed via `javap -v`, not just IDE guesswork).
  See "Attachment API reference" below.
- Verified: clean build, and a `runClient` boot showed `fabric-data-attachment-api-v1` loading
  and the resource-manager reload starting with no crash — mod init (where the attachment is
  registered) ran successfully. Not yet verified in an actual played session that data survives
  a real restart — see "Next recommended step".

Earlier, merged both active work branches into `master`:

- Merged `origin/jonas_workbranch` (fast-forward, commit `c979769`) — adds the four
  `.claude/agents/*.md` subagents, the "Project Agents" section in `CLAUDE.md`, and
  `docs/fabric-modding.md`.
- Merged `origin/fischey_workbranch` (merge commit, tip `75dd912`) — brings in the
  Mixin-based XP-orb suppression, the shared `VanillaXpFormula`, and the real-time
  client XP-bar sync over a custom S2C packet (see "Networking API reference" below).
- Only conflict was `HANDOFF.md` itself (expected — both branches update it independently);
  resolved by hand-merging both branches' state into this version. No source-code conflicts:
  the two branches touched disjoint files (`jonas_workbranch` only touched docs/agent
  definitions, `fischey_workbranch` only touched progression/networking/mixin code), and a
  manual overlap check (the `merge-integration-reviewer` agent wasn't loaded in this session)
  found no competing design assumptions between them either.
- Why: user requested combining both contributors' branches into `master` now that
  Fischey's XP-sync work had reached a working state.

Earlier, on `fischey_workbranch` before the merge: "Fixed real-time vanilla level/XP bar sync"
— added `VanillaXpFormula.java` to replace three drifting copies of the curve (the actual
cause of "level updates but progress bar doesn't"), and a custom S2C payload
(`networking/ExperienceSyncPayload.java` + `networking/Baum2Networking.java` server-side,
`networking/ClientNetworkingHandler.java` client-side) sent every tick from
`ProgressionTickHandler`. Root cause of the earlier bug: vanilla only pushes experience to the
client on join or on a server-side `setExperienceLevel()`/`addExperience()` call — setting
fields directly on `ServerPlayerEntity` every tick never reaches the client.

Earlier, on `jonas_workbranch` before the merge: `09eefd4` — "Add ip-naming-compliance-checker,
balance-reviewer, and merge-integration-reviewer agents", alongside the pre-existing
`fabric-docs-researcher`. Attempted to run the review agents against the progression system as
a first pass but couldn't — new custom agents aren't picked up until a fresh session starts
(see "Known limitation" above). Still not done; see "Next recommended step".

See `git log -p HANDOFF.md` for the full detail on earlier revisions.

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

## Attachment API reference (persistent per-player/entity data)

For any future "store custom data on a player/entity/block-entity/chunk that should survive
restarts" need, use `fabric-data-attachment-api-v1` (already a dependency) rather than a
hand-rolled `HashMap<UUID, ...>` + manual save/load. Reference implementation:
`progression/PlayerLevelSystem.java` (the `PROGRESSION` field) + `progression/PlayerProgressData.java`
(the `CODEC` field).

**Critical gotcha, learned the hard way (cost a full debugging cycle — see "Last change"):** the
class holding your `AttachmentType` static field must be force-loaded during
`Baum2.onInitialize()`, via an explicit call to some no-op method on it (see
`PlayerLevelSystem.bootstrap()`). If the class is only ever referenced from inside an event
callback *body* (lambda registered at init time but not executed until later), Java's lazy class
initialization means the `AttachmentType` won't actually be registered until the first time a
player triggers that callback — by which point Fabric may have already tried and failed to
deserialize that player's persisted attachment data (silently, no error/warning surfaced to the
log at the level we were checking), permanently losing it on the next save. Symptom: progress
"resets" but only sometimes, and looks exactly like a persistence failure even though writes are
working fine. If you add a new attachment and see this pattern, check that its owning class is
actually being force-loaded at init, not just referenced from a lambda.

Key classes, all in `net.fabricmc.fabric.api.attachment.v1` (Fabric's own names — stable across
mapping sets, unlike the networking classes above):
- `AttachmentType<A>` — the "key" for a piece of attached data.
- `AttachmentRegistry` — creates and registers an `AttachmentType`. Use
  `AttachmentRegistry.create(Identifier, Consumer<Builder<A>>)`, **not**
  `AttachmentRegistry.builder()` — the no-arg `builder()` is `@Deprecated` (verified via
  `javap -v`, the bytecode carries a real `Deprecated` attribute, not just a Javadoc note).
- `AttachmentRegistry.Builder<A>` methods used: `.persistent(Codec<A>)` (enables save/load),
  `.copyOnDeath()` (data survives player death/respawn), `.initializer(Supplier<A>)` (default
  value for entities that have never had this attachment set).
- `AttachmentTarget` — interface that `Entity` (and therefore `ServerPlayerEntity`), `BlockEntity`,
  `Chunk`, and `World` all implement (added via Fabric's build-time interface injection, so it's
  visible at compile time even though the actual `implements` is woven in by a Mixin at runtime —
  you don't need to do anything special to get `player.getAttached(...)` etc. to resolve).
  Methods used: `getAttachedOrCreate(type)`, `setAttached(type, value)`, `removeAttached(type)`.

The `.persistent(Codec<A>)` codec is `com.mojang.serialization.Codec<A>` (Mojang's
DataFixerUpper serialization library, already on the classpath via Minecraft itself) — **not**
the `PacketCodec` used for networking above; they're unrelated codec systems despite the similar
name. For a simple data class, build one with `RecordCodecBuilder.create(...)` as in
`PlayerProgressData.CODEC` — see that file for the exact pattern.

Persisted attachment data is stored as part of the target's own save data (e.g. for a player,
inside their `playerdata/<uuid>.dat`), so it's written/read automatically by vanilla's existing
save cycle — no manual `ServerPlayConnectionEvents.JOIN`/`DISCONNECT` save/load hooks needed for
persistence itself (those events are still useful for other things, e.g. this mod's
`LevelUpHandler` uses `JOIN` to prime its level-up-tracking cache, which is a separate concern
from persistence).

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
- Our XP curve is centralized in `ProgressionCurve` (originally `VanillaXpFormula`, renamed once
  it stopped actually being vanilla's formula — see "Last change") — **do not** reimplement it
  elsewhere. The bug that took several iterations to fix (level updated, progress bar didn't) was
  ultimately caused by three separate copies of this formula drifting apart, not a display API
  issue. The curve's actual numbers are also deliberately rebalanced away from vanilla's own —
  see "Last change" for the current formula and why.
- Vanilla XP orb drops from hostile mobs are disabled via `LivingEntityMixin`. Experience bottles
  were reported to still spawn orbs in an earlier session — not re-verified since; low priority.
- Progression persistence uses Fabric's Data Attachment API (see "Attachment API reference"
  above) — **do not** reintroduce a manual `HashMap<UUID, PlayerProgressData>`, that was the
  previous approach and it's why data was lost on restart.

## Next recommended step

1. **Not yet re-confirmed in-game after the scrolling fix** (highest priority manual check):
   press 'C', confirm a scrollbar appears on the right edge of the tab content (it should only
   actually need to scroll at a high GUI Scale or small window — at a normal/low GUI Scale all
   15 rows may already fit without one, which is fine, not a bug), confirm every row through
   Crit Chance is reachable by scrolling, and that mouse wheel + scrollbar-drag both work. Also
   still open from the prior round: confirm the header no longer overlaps Life/Mana, the panel
   background reads as solid/opaque, the 4 "+1" buttons work end-to-end (click one, confirm the
   value increments and Unspent Points decrements), and a second 'C' press still closes it
   cleanly. This environment has no GUI-automation tool for the native Minecraft window — every
   UI bug found so far was only caught because the user played manually and sent a screenshot;
   that remains the only real verification path here.
1a. If scrolling turns out to misbehave in practice (e.g. focus/narration order feels wrong,
    or the scrollbar overlaps content awkwardly), the user already explicitly sanctioned a
    fallback: split the dense "Stats" tab into 2-3 sub-tabs by attribute family. This reuses
    the exact already-proven `TabNavigationWidget.builder(...).tabs(tabA, tabB, ...)` pattern
    with zero new API surface — see "Last change" for why scrolling was tried first.
1b. Not attempted: making the Stats screen's rendering scale independent of the player's GUI
    Scale setting (a literal reading of part of the user's feedback). Scrolling solves the
    practical "content unreachable" problem regardless of GUI Scale, but if a future session
    specifically wants the screen to *never* grow with a high GUI Scale setting (rather than
    just staying scrollable), that needs a matrix-transform counter-scale approach with careful
    attention to keeping mouse hit-testing coordinates aligned with the rendered scale — flagged
    as a separate, larger piece of work, not started.
2. Still not visually confirmed from earlier sessions: the Life/Mana HUD bars' position/colors/
   fill-ratio, and the vanilla XP bar animating smoothly as `/baum2 addxp <n>` runs repeatedly.
3. In a fresh session where the custom subagent types actually resolve (see "Known limitation"
   above — untested whether this environment-specific issue affects other setups too): run
   `balance-reviewer` on the progression system's original XP curve/mob-reward formula (the
   attribute formulas and Life/Mana formulas have each already been reviewed via the
   general-purpose-agent workaround — see their respective "Last change" entries — but the XP
   curve itself never has) and `ip-naming-compliance-checker` on existing player-facing strings
   (command output, level-up broadcast text, attribute/stat names) — neither has been reviewed.
4. Merge `fischey_workbranch` back into `master` — this session's commits (attribute system +
   Stats screen rework, the earlier Base Damage/Stats-screen commit, Life/Mana/HUD, and the
   earlier XP-rebalance commits) are still only on `fischey_workbranch`.
5. **The biggest open gap: none of the new combat-facing formulas (Base Attack, Base Magic
   Attack, Physical/Magic Defence, Attack/Cast Speed Multiplier, Crit Chance) actually affect
   gameplay yet** — no `combat/` package exists, so these are purely computed-and-displayed
   right now. The first real combat/skill system should read them via `VitalsCurve`'s getters
   (fed by the player's persisted attribute values), not invent parallel damage numbers.
6. A class-name banner + level-diamond badge (as pictured in the reference image from the
   Life/Mana session) was explicitly deferred — no `classes/` package exists yet. Could become
   a second tab on `CharacterStatsScreen` (which is now explicitly built to support more tabs)
   rather than a separate HUD banner — worth deciding when that work starts, not now.
7. Remaining Priority 1 items per `CLAUDE.md`: first custom item, first weapon, first active
   skill with a cooldown manager, first world-event block. Consult `fabric-docs-researcher` /
   `docs/fabric-modding.md` before implementing any of these if the relevant Fabric API is
   unclear.
