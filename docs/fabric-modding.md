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

## Input / Keybindings

Researched for the 'C' character-stats-screen keybind. Verified against the decompiled
`fabric-key-binding-api-v1-1.1.7+4fc5413f3e-sources.jar` (pulled in transitively by
`fabric-api-0.141.4+1.21.11`) and the named `minecraft-clientOnly` 1.21.11 sources jar
(`net/minecraft/client/option/KeyBinding.java`, `net/minecraft/client/util/InputUtil.java`).

- **Registering a keybinding**: `net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper`
  still exists, unchanged shape — one method that matters:
  `KeyBindingHelper.registerKeyBinding(KeyBinding keyBinding)` (there's also
  `getBoundKeyOf(KeyBinding)` for reading the currently-configured key). Call it once, e.g. in
  your `ClientModInitializer.onInitializeClient()`, and keep the returned `KeyBinding` in a
  static field.
- **`KeyBinding` constructors** (all in `net.minecraft.client.option.KeyBinding`):
  - `KeyBinding(String id, int code, KeyBinding.Category category)` — implicit
    `InputUtil.Type.KEYSYM`.
  - `KeyBinding(String id, InputUtil.Type type, int code, KeyBinding.Category category)` — the
    one used in Fabric API's own javadoc example.
  - `KeyBinding(String id, InputUtil.Type type, int code, KeyBinding.Category category, int priority)`
    — full constructor; `priority` only affects sort order within a category, pass `0` unless
    you care.
  - `id` is a translation key you supply directly, e.g. `"key.baum2.open_stats"` (add the
    matching `lang` JSON entry yourself — there's no separate "register the id" step).
- **`KeyBinding.Category` changed shape in this version — do not treat it as a plain string.**
  It is now `public record Category(Identifier id)`, not a bare translation-key `String` like
  older tutorials show. Vanilla constants: `KeyBinding.Category.MOVEMENT`, `.MISC`,
  `.MULTIPLAYER`, `.GAMEPLAY`, `.INVENTORY`, `.CREATIVE`, `.SPECTATOR`, `.DEBUG`. For a mod-owned
  category, call `KeyBinding.Category.create(Identifier.of("baum2", "general"))` once (it
  self-registers into a global list and throws `IllegalArgumentException` if that `Identifier`
  is already registered, so don't call `create` more than once per id — store the returned
  `Category` in a static field instead). For a single first keybind, reusing
  `KeyBinding.Category.MISC` is fine and simplest.
- **GLFW key constant**: `org.lwjgl.glfw.GLFW.GLFW_KEY_C` — confirmed the import path is still
  plain `org.lwjgl.glfw.GLFW` (vanilla's own `InputUtil.java` imports and uses `GLFW.*`
  constants the same way, e.g. `GLFW.GLFW_KEY_UNKNOWN`). Not Fabric/Yarn-mapped, so this is
  stable across mapping updates.
- **`InputUtil.Type` enum** (`net.minecraft.client.util.InputUtil.Type`): `KEYSYM`, `SCANCODE`,
  `MOUSE` all still exist unchanged. Use `KEYSYM` for a regular keyboard key like 'C'.
- **`wasPressed()` vs `isPressed()`** (both on `KeyBinding`, both confirmed present and
  unchanged): `isPressed()` reports "is currently held right now" and **can miss a fast
  press-and-release that happens between polls** (documented on the method itself, citing
  MC-118107). `wasPressed()` is queue-based — `KeyBinding` internally counts key-down events
  (`timesPressed`), and each call to `wasPressed()` decrements the counter and returns `true`
  once per queued press until the counter hits zero. For a toggle-a-screen keybind, poll
  `wasPressed()` (not `isPressed()`) so a quick tap is never silently dropped. If more than one
  press could plausibly queue up between polls, vanilla's own javadoc suggests draining with
  `while (keyBinding.wasPressed()) { ... }`; for a simple open/close toggle a single
  `if (keyBinding.wasPressed())` per tick is fine since only the last state matters.
- **Where to poll**: `net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents` still
  exists at this exact package/name, confirmed unchanged in the decompiled
  `fabric-lifecycle-events-v1-2.6.15+4ebb5c083e` sources. Register on
  `ClientTickEvents.END_CLIENT_TICK` (`Event<EndTick>`, functional method
  `onEndTick(MinecraftClient client)`) and call `wasPressed()` there — this is still the
  standard place, same as older-version tutorials describe.
- Minimal end-to-end sketch:
  ```java
  private static final KeyBinding OPEN_STATS_KEY = KeyBindingHelper.registerKeyBinding(
      new KeyBinding("key.baum2.open_stats", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_C, KeyBinding.Category.MISC)
  );
  // in onInitializeClient():
  ClientTickEvents.END_CLIENT_TICK.register(client -> {
      while (OPEN_STATS_KEY.wasPressed()) {
          if (client.currentScreen == null) {
              client.setScreen(new CharacterStatsScreen());
          }
      }
  });
  ```
  (Closing on a second 'C' press while the screen is open needs the screen's own `keyPressed`
  override instead, since `wasPressed()` on a global keybind isn't polled while a `Screen` has
  focus in the same way — vanilla screens typically close via Escape or a Done button; a
  same-key toggle-close would need explicit handling in `CharacterStatsScreen.keyPressed`.)

## GUI / Screens

Researched for the 'C' character-stats screen with a tab bar. Verified against the named
`minecraft-clientOnly` 1.21.11 sources jar — `net/minecraft/client/gui/screen/Screen.java`,
`net/minecraft/client/gui/DrawContext.java`, `net/minecraft/client/gui/tab/*.java`,
`net/minecraft/client/gui/widget/TabNavigationWidget.java`, and vanilla's own
`net/minecraft/client/gui/screen/StatsScreen.java` (the "Statistics" screen — a near-exact
analog for what we're building, since it's a vanilla screen with a tab bar).

### Basic `Screen` conventions

- `protected Screen(Text title)` — confirmed, protected (not public), delegates to
  `Screen(MinecraftClient, TextRenderer, Text)`. Subclass constructors call `super(title)`.
- `protected void init()` — confirmed override point, called when the screen is opened
  (`MinecraftClient.setScreen(...)`) or on window resize. Build/add your widgets here.
- `public void render(DrawContext context, int mouseX, int mouseY, float deltaTicks)` —
  confirmed exact signature: **plain `float deltaTicks`, not `RenderTickCounter`.** The
  `RenderTickCounter`-based signature (`HudElement.render(DrawContext, RenderTickCounter)`,
  see the Rendering/HUD section above) is specific to `HudElement`/`InGameHud`, not `Screen`.
  `Screen`'s own base `render()` just iterates registered `drawables` and renders each.
- `public void renderBackground(DrawContext context, int mouseX, int mouseY, float deltaTicks)`
  — confirmed still exists, same signature. Renders the translucent in-world darkening or the
  panorama/menu background texture depending on whether `client.world == null`. Call it from
  your own `render()` override (typically `super.render(...)` first, or call it directly, then
  draw your own content on top).
- **Closing a screen**: `close()` is `public void close() { this.client.setScreen(null); }` —
  confirmed. Default Escape-key handling (`shouldCloseOnEsc()` returns `true` by default) already
  calls `close()` for you, so a plain Escape-to-close needs no extra code. For a custom
  toggle-key close, call `client.setScreen(null)` (or the screen's own `close()`) directly from
  wherever you detect the second key press.
- **`DrawContext` text-drawing overloads** (all `void`, confirmed via decompiled
  `DrawContext.java`, one triplet per text type — `String` / `OrderedText` / `Text`):
  ```java
  drawContext.drawTextWithShadow(textRenderer, Text text, int x, int y, int color);
  drawContext.drawText(textRenderer, Text text, int x, int y, int color, boolean shadow);
  drawContext.drawCenteredTextWithShadow(textRenderer, Text text, int centerX, int y, int color);
  ```
  Use the `Text`-typed overload for anything going through `Text.translatable(...)`/
  `Text.literal(...)` (which is everything player-facing per this project's i18n conventions).

### Tabbed screens — vanilla's real `Tab`/`TabManager`/`TabNavigationWidget` API

Vanilla exposes a genuine, mod-usable tab-navigation system, used by its own "Statistics"
screen (`StatsScreen`) among others (also Realms screens). It is **not** overkill boilerplate
from an old version — it's the exact widget vanilla renders as the row of tab buttons under a
screen's title bar.

- `net.minecraft.client.gui.tab.Tab` — the interface a tab implements:
  ```java
  public interface Tab {
      Text getTitle();
      Text getNarratedHint();
      void forEachChild(Consumer<ClickableWidget> consumer);
      void refreshGrid(ScreenRect tabArea);
  }
  ```
- `net.minecraft.client.gui.tab.GridScreenTab` — the practical base class to extend instead of
  implementing `Tab` raw. Constructor `GridScreenTab(Text title)`; it owns a `protected final
  GridWidget grid` you add your content widgets to (`this.grid.add(widget, col, row)`), and its
  `refreshGrid` centers the grid in the tab area (`SimplePositioningWidget.setPos(grid, tabArea,
  0.5F, 0.16666667F)`). Override `refreshGrid` if a child widget needs explicit re-positioning/
  resizing first (see `StatsScreen.StatsTab`, which resizes its list widget before calling
  `super.refreshGrid(tabArea)`).
- `net.minecraft.client.gui.tab.TabManager` — owns "which tab is currently shown" and wires
  widget add/remove:
  ```java
  private final TabManager tabManager = new TabManager(this::addDrawableChild, this::remove);
  ```
  (2-arg constructor: widget-load consumer, widget-unload consumer; there's also a 4-arg
  variant with extra `Consumer<Tab>` load/unload callbacks if you need tab-level lifecycle
  hooks.) Call `tabManager.setTabArea(ScreenRect)` whenever the screen lays itself out, and
  `setCurrentTab(Tab, boolean playClickSound)` to switch (this is normally done for you by
  `TabNavigationWidget`, not called directly).
- `net.minecraft.client.gui.widget.TabNavigationWidget` — the actual clickable tab-button row.
  Built via a builder, not a public constructor:
  ```java
  this.tabNavigationWidget = TabNavigationWidget.builder(this.tabManager, this.width)
      .tabs(new StatsTab(...))   // one or more Tab instances, vararg
      .build();
  this.addDrawableChild(this.tabNavigationWidget);
  this.tabNavigationWidget.selectTab(0, false);   // false = no UI click sound on initial select
  ```
  Other methods used by `StatsScreen`: `setWidth(int)` + `init()` (call both together on
  resize, before reading `getNavigationFocus()` to find where the tab bar ends and content
  should start), `setTabActive(int index, boolean)` (enable/disable a tab button, e.g. while
  data for it is still loading), `setTabTooltip(int index, @Nullable Tooltip)`, `getTabs()`.
  Forward keyboard input so Ctrl+Tab / Ctrl+number tab switching works:
  ```java
  @Override
  public boolean keyPressed(KeyInput input) {
      return this.tabNavigationWidget != null && this.tabNavigationWidget.keyPressed(input)
          ? true : super.keyPressed(input);
  }
  ```
- **Minimal wiring example**, distilled from `StatsScreen.init()`/`refreshWidgetPositions()`:
  ```java
  private final TabManager tabManager = new TabManager(this::addDrawableChild, this::remove);
  private TabNavigationWidget tabNavigationWidget;

  @Override
  protected void init() {
      this.tabNavigationWidget = TabNavigationWidget.builder(this.tabManager, this.width)
          .tabs(new StatsTabPlaceholder(Text.translatable("gui.baum2.stats.tab")))
          .build();
      this.addDrawableChild(this.tabNavigationWidget);
      this.tabNavigationWidget.selectTab(0, false);
      this.refreshWidgetPositions();
  }

  private void refreshWidgetPositions() {
      this.tabNavigationWidget.setWidth(this.width);
      this.tabNavigationWidget.init();
      int headerBottom = this.tabNavigationWidget.getNavigationFocus().getBottom();
      this.tabManager.setTabArea(new ScreenRect(0, headerBottom, this.width, this.height - headerBottom));
  }
  ```
- **Practical verdict for the first version**: the real API is not hard to wire up (the
  snippet above is essentially the whole thing), and using it now means adding a second tab
  later is just another `Tab` instance in `.tabs(...)` — no rewrite. It also gives you Ctrl+Tab/
  Ctrl+number keyboard switching, tooltips, and narration for free. The only meaningful
  overhead versus a hand-rolled row of `ButtonWidget`s is the `GridScreenTab`/`ScreenRect`
  layout dance in `refreshGrid`/`refreshWidgetPositions`. Recommendation: **use the real
  `Tab`/`TabManager`/`TabNavigationWidget` trio from the start** — for a screen explicitly
  planned to grow more tabs, it's the same amount of code as a hand-rolled version would need
  once you also hand-roll active/inactive styling and highlight state, and it avoids a later
  migration.

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
