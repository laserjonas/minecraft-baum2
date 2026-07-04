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

### HUD element registry (removing/replacing/adding vanilla HUD layers, MC 1.21.11 / Fabric API 0.141.4)

As of Fabric API 0.141.4+1.21.11 (`fabric-rendering-v1` 16.2.10+0290ad933e — confirmed as the
exact bundled version via that jar's own `.pom`), **there is no `HudLayerRegistrationCallback`
/ `IdentifiedLayer` API** (that's from older Fabric API / older MC versions and no longer
exists in this version's sources). It's been replaced by a new, more capable API:
`net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry` +
`net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements`. Verified against the
decompiled `fabric-rendering-v1-16.2.10+0290ad933e-sources.jar` (Maven artifact
`net.fabricmc.fabric-api:fabric-rendering-v1:16.2.10+0290ad933e`, pulled in transitively by
`fabric-api-0.141.4+1.21.11`) plus `./gradlew genSources` output for vanilla classes.

- **Removing the vanilla health bar** (client-side, e.g. in your `ClientModInitializer`):
  ```java
  HudElementRegistry.removeElement(VanillaHudElements.HEALTH_BAR);
  ```
  `VanillaHudElements` (in the same `hud` package) has one `Identifier` constant per vanilla
  HUD piece, including `HEALTH_BAR`, `ARMOR_BAR`, `FOOD_BAR`, `AIR_BAR`, `MOUNT_HEALTH`,
  `HOTBAR`, `EXPERIENCE_LEVEL`, `INFO_BAR` (the XP-bar/locator/jump-bar slot),
  `HELD_ITEM_TOOLTIP`, `BOSS_BAR`, `STATUS_EFFECTS`, `CROSSHAIR`, `MISC_OVERLAYS`, `SLEEP`,
  `SCOREBOARD`, `CHAT`, `PLAYER_LIST`, `SUBTITLES`, etc. Each is
  `Identifier.ofVanilla("health_bar")` etc. (i.e. namespace `minecraft`). To keep hunger and
  XP bar as-is, only remove `HEALTH_BAR` — don't touch `FOOD_BAR` or `EXPERIENCE_LEVEL`/
  `INFO_BAR`.
  - `HudElementRegistry` also has `addFirst`/`addLast`/`attachElementBefore`/
    `attachElementAfter`/`replaceElement`, all keyed by the same `Identifier` ids — no separate
    "layer" object/interface to construct beyond `HudElement`.
- **No Mixin needed for either removal or addition.** Fabric API itself already Mixins into
  vanilla `InGameHud` (`GuiMixin` in `fabric-rendering-v1`, `@Mixin(InGameHud.class)`, using
  MixinExtras `@WrapOperation` on calls like `Gui.renderHearts(...)`, `Gui.renderArmor(...)`,
  `Gui.renderFood(...)` inside `InGameHud.renderPlayerHealth(...)`) and routes every one of
  those calls through `HudElementRegistryImpl.getRoot(<id>)`, which is exactly what
  `HudElementRegistry.removeElement`/`replaceElement`/`addFirst` etc. manipulate. So the
  public `HudElementRegistry` API is a complete, supported replacement for what would
  otherwise require a hand-rolled Mixin/redirect on `InGameHud`. Don't write your own Mixin
  against `InGameHud.renderPlayerHealth`/`renderHearts` for this — use the registry.
- **Registering a new custom HUD element to draw Life/Mana bars**: implement
  `net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement`:
  ```java
  public interface HudElement {
      void render(DrawContext context, RenderTickCounter tickCounter);
  }
  ```
  then register it, e.g. in place of the removed health bar:
  ```java
  HudElementRegistry.attachElementBefore(
      VanillaHudElements.ARMOR_BAR,
      Identifier.of("baum2", "life_mana_bars"),
      (context, tickCounter) -> { /* draw here */ }
  );
  ```
  (`attachElementBefore`/`attachElementAfter` inherit the render condition — e.g. hidden in
  spectator/F1 — of the vanilla element they're anchored to; `addFirst`/`addLast` do not
  inherit any condition.) This `HudElement`-based registration is the current preferred
  mechanism over raw `HudRenderCallback.EVENT` for anything that should behave like a real HUD
  layer (ordering relative to other layers, inheriting vanilla's hidden-HUD/F1 and spectator
  render conditions). Plain `HudRenderCallback.EVENT` still exists and still works (Fabric's
  own `GuiMixin` fires it via `@Inject(method = "render", at = @At("TAIL"))` — i.e. it always
  renders last, on top of everything, unconditionally) — fine for a simple always-on overlay,
  but `HudElementRegistry` is the better fit here since we want the new bars to sit where the
  health bar used to be and to respect vanilla's hide-HUD condition.
  - If custom bars occupy left/right space above the hotbar (like vanilla's health/armor/food/
    air bars do) and should push other status bars out of the way, also register a
    `net.fabricmc.fabric.api.client.rendering.v1.hud.StatusBarHeightProvider` via
    `HudStatusBarHeightRegistry.addLeft(id, player -> height)` /
    `.addRight(id, player -> height)`, using the same `Identifier` as the `HudElement`
    registration. Not required just to draw a bar — only needed for correct auto-layout
    alongside other status bars.
- **`DrawContext.fill(int x1, int y1, int x2, int y2, int color)` still exists unchanged** in
  1.21.11 (confirmed via `javap` on the named `minecraft-clientonly` jar) — still fine for
  simple filled-rectangle bars, exactly as it worked for the old `ProgressionHud` rectangle
  fills. Two other `fill(RenderPipeline, ...)` overloads also exist for custom-pipeline use
  cases; not needed for plain solid-color bars.
- Source note: `VanillaHudElements` identifiers use `Identifier.ofVanilla(String)` (Yarn
  `method_60656` on `class_2960`/`Identifier`) — a shortcut for
  `Identifier.of("minecraft", name)`. Use `Identifier.of("baum2", "...")` for your own ids.

## Attributes

- **Max health attribute constant**: `net.minecraft.entity.attribute.EntityAttributes.MAX_HEALTH`
  — type `RegistryEntry<EntityAttribute>`. Confirmed via decompiled
  `EntityAttributes.java`/`javap` against the named 1.21.11 jar: there is **no
  `GENERIC_MAX_HEALTH`** in this mapping (the `GENERIC_` prefix was dropped in earlier 1.21.x
  Yarn revisions; don't use stale tutorial names like `EntityAttributes.GENERIC_MAX_HEALTH`,
  they don't exist here). Same de-prefixing applies to the other `EntityAttributes` constants
  (`ATTACK_DAMAGE`, `MOVEMENT_SPEED`, `ARMOR`, etc.).
- **Setting base max health server-side**:
  ```java
  player.getAttributeInstance(EntityAttributes.MAX_HEALTH).setBaseValue(40.0);
  ```
  `LivingEntity.getAttributeInstance(RegistryEntry<EntityAttribute>)` returns
  `EntityAttributeInstance`, which has `setBaseValue(double)` / `getBaseValue()` (confirmed via
  `javap` on `EntityAttributeInstance`). `PlayerEntity` extends `LivingEntity` so this works
  directly on a `ServerPlayerEntity`.
- **Persistence is automatic — confirmed, not assumed.** Decompiled
  `LivingEntity.writeCustomData(WriteView)` / `readCustomData(ReadView)` (the 1.21.5+
  Codec/`WriteView`/`ReadView`-based replacement for raw `NbtCompound` read/write) does:
  ```java
  // write
  view.put("attributes", EntityAttributeInstance.Packed.LIST_CODEC, this.getAttributes().pack());
  // read
  view.read("attributes", EntityAttributeInstance.Packed.LIST_CODEC).ifPresent(this.getAttributes()::unpack);
  ```
  unconditionally for every `LivingEntity` (including players), under NBT key
  `LivingEntity.ATTRIBUTES_KEY = "attributes"`. So a `setBaseValue(...)` call on max health is
  saved/restored through vanilla's own player-data serialization with **no extra persistence
  code needed** on our side. Still re-apply the intended base value after level-up and on
  player join (as planned) purely as a defense against desync (e.g. player joining before our
  level-derived value would otherwise be (re)computed), not because persistence itself is
  missing.

## Open questions

- What is the correct, current way to fully suppress vanilla XP orb drops/pickup in 1.21.11
  without fighting the vanilla XP bar (Fischey's in-progress work on `fischey_workbranch`)?
  Needs a verified Mixin target method + confirmation it doesn't break unrelated vanilla
  mechanics (anvils, enchanting tables, which also read player XP).
- Persistence: `PlayerProgressData` already has `writeNbt`/`readNbt` but nothing calls them
  yet — need to confirm the correct 1.21.11 hook for per-player persistent data (Fabric's
  attachment API vs. classic `PlayerEntity` NBT read/write mixin) before implementing.
