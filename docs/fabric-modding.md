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

## Networking — Client-to-Server (C2S) payloads

Researched for the attribute-point-spend button (player clicks "+1" on Endurance/
Intelligence/Strength/Dexterity in `CharacterStatsScreen`, client tells server to spend a
point). This project already has working S2C payloads (`ExperienceSyncPayload`,
`ManaSyncPayload`, `CombatStatsSyncPayload` in `networking/`, registered/sent via
`Baum2Networking`, received via `ClientNetworkingHandler`) but this is the first C2S
direction. Verified against the decompiled `fabric-networking-api-v1-5.1.6+6b6d71a53e`
sources jars (both `-client` and `-common` variants, pulled in transitively by
`fabric-api-0.141.4+1.21.11`) — specifically `PayloadTypeRegistry.java`,
`ServerPlayNetworking.java`, and `ClientPlayNetworking.java`.

- **`PayloadTypeRegistry.playC2S()` exists, confirmed, mirrors `playS2C()` exactly**:
  ```java
  static PayloadTypeRegistry<RegistryByteBuf> playC2S();  // client-to-server play channel
  static PayloadTypeRegistry<RegistryByteBuf> playS2C();  // server-to-client play channel (already used)
  ```
  Both return the same interface shape (`register(CustomPayload.Id<T> id, PacketCodec<? super
  B, T> codec)`, plus a `registerLarge(...)` overload for oversized payloads split across
  multiple packets). Payload record + `CustomPayload.Id`/`CustomPayload.Type` + `PacketCodec`
  definition style is identical to the existing S2C payloads — no new pattern needed, just
  register the new payload record's `TYPE`/`CODEC` against `playC2S()` instead of `playS2C()`.
- **Registration must happen on both the sending and receiving side** — this is stated
  directly in `PayloadTypeRegistry`'s javadoc ("This must be done on both the sending and
  receiving side, usually during mod initialization and before registering a packet
  handler"). In practice this project already satisfies that for free: `Baum2Networking
  .registerServerPayloads()` (which calls `PayloadTypeRegistry.playS2C().register(...)`) is
  invoked from `Baum2.onInitialize()` in `src/main/java` — Fabric's **common** mod
  initializer entrypoint, which runs on *both* the dedicated server and the client's own
  process (client always also runs a logical-server-shaped init path, even in singleplayer).
  So calling `PayloadTypeRegistry.playC2S().register(...)` from that same common
  `onInitialize()` (e.g. add it to `Baum2Networking.registerServerPayloads()`, or a
  same-named sibling method) registers it on both sides automatically — **no separate
  client-only registration call is needed just for `PayloadTypeRegistry.playC2S().register(...)`
  itself.** (Registering the *receiver* is different — see below.)
- **Client-side send**: `net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking`,
  confirmed exact signature:
  ```java
  public static void send(CustomPayload payload);
  ```
  Throws `IllegalStateException` if not currently connected to a server (checked internally via
  `MinecraftClient.getInstance().getNetworkHandler() != null`). Call directly, e.g.
  `ClientPlayNetworking.send(new SpendAttributePointPayload(Attribute.STRENGTH))` — no extra
  wrapping needed, mirrors `ServerPlayNetworking.send(player, payload)` used for the existing
  S2C payloads.
- **Server-side receive**: `net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking`,
  confirmed exact signature (same file/class already used for `.send(...)` on the S2C side):
  ```java
  public static <T extends CustomPayload> boolean registerGlobalReceiver(
      CustomPayload.Id<T> type, PlayPayloadHandler<T> handler);
  ```
  `PlayPayloadHandler<T>` is `@FunctionalInterface`-shaped: `void receive(T payload,
  ServerPlayNetworking.Context context)` — **the handler already executes on the server
  thread**, so it's safe to touch player/world state directly without extra scheduling.
  **This receiver registration is server-side-only** (register it in
  `ClientNetworkingHandler`'s server-side counterpart, or wherever server-only setup already
  lives — do not call it from client code).
  `ServerPlayNetworking.Context` (confirmed, `@ApiStatus.NonExtendable` interface) exposes
  exactly three accessors:
  ```java
  public interface Context {
      MinecraftServer server();
      ServerPlayerEntity player();   // the player that sent the packet
      PacketSender responseSender();
  }
  ```
  So the sending player is `context.player()`, directly usable to look up/mutate their
  progression data (e.g. `AttributePointsComponent`/whatever holds spendable points) inside
  the handler.
- **Minimal end-to-end shape** for the attribute-point button, following this project's
  existing payload conventions:
  ```java
  // networking/SpendAttributePointPayload.java (record, C2S)
  public record SpendAttributePointPayload(Attribute attribute) implements CustomPayload {
      public static final Identifier ID = Identifier.of("baum2", "spend_attribute_point");
      public static final CustomPayload.Id<SpendAttributePointPayload> TYPE = new CustomPayload.Id<>(ID);
      public static final PacketCodec<RegistryByteBuf, SpendAttributePointPayload> CODEC = ...;
      @Override public CustomPayload.Id<? extends CustomPayload> getId() { return TYPE; }
  }

  // registration, in the common Baum2.onInitialize() path (alongside the existing playS2C() calls):
  PayloadTypeRegistry.playC2S().register(SpendAttributePointPayload.TYPE, SpendAttributePointPayload.CODEC);

  // server-side receiver registration (server-only init path):
  ServerPlayNetworking.registerGlobalReceiver(SpendAttributePointPayload.TYPE, (payload, context) -> {
      ServerPlayerEntity player = context.player();
      // spend the point for payload.attribute() on player, server thread, safe to mutate directly
  });

  // client-side send, directly inside a ButtonWidget's onPress lambda (see GUI/Screens > ButtonWidget):
  ClientPlayNetworking.send(new SpendAttributePointPayload(Attribute.STRENGTH));
  ```

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

### `ButtonWidget` — clickable buttons in a `GridWidget`

Researched for a "+1" attribute-point spend button next to a stat row in
`CharacterStatsScreen`. Verified against the decompiled named `minecraft-clientOnly` 1.21.11
sources jar, `net/minecraft/client/gui/widget/ButtonWidget.java` and `GridWidget.java`.

- `net.minecraft.client.gui.widget.ButtonWidget` still exists at this exact name/package, an
  `abstract class` extending `PressableWidget`. Build one via the static builder, not a public
  constructor:
  ```java
  ButtonWidget.builder(Text message, ButtonWidget.PressAction onPress)
      .dimensions(x, y, width, height)   // or .position(x, y) + .size(width, height) separately
      .build();
  ```
  `ButtonWidget.PressAction` is `@FunctionalInterface`-shaped: `void onPress(ButtonWidget button)`
  — the lambda passed to `builder(...)` runs on the client (render/main client thread) when
  clicked.
- Builder chain methods confirmed: `.position(int x, int y)`, `.width(int width)`,
  `.size(int width, int height)`, `.dimensions(int x, int y, int width, int height)` (= position +
  size combined), `.tooltip(Tooltip)`, `.narrationSupplier(NarrationSupplier)`, `.build()`.
  Defaults if unset: `width = 150`, `height = 20` (also exposed as constants
  `ButtonWidget.DEFAULT_WIDTH = 150`, `ButtonWidget.DEFAULT_WIDTH_SMALL = 120`,
  `ButtonWidget.DEFAULT_HEIGHT = 20`).
- **No standard "small square" button convention exists in vanilla `ButtonWidget` itself** —
  there's no `DEFAULT_WIDTH_TINY` or similar for icon-only/compact buttons; the smallest named
  constant is `DEFAULT_WIDTH_SMALL = 120` (still full-height text-button width, just narrower).
  For a compact "+1" button, just pick dimensions directly via `.size(w, h)` /
  `.dimensions(...)` — e.g. `20x20` (matching `DEFAULT_HEIGHT`) is a reasonable, unenforced
  choice; nothing in the API requires or suggests a specific compact size.
- **Works in a `GridWidget` exactly like the project's existing `TextWidget` rows.**
  `GridWidget.add` is generic over `Widget` (`public <T extends Widget> T add(T widget, int row,
  int column)`, plus overloads taking `occupiedRows`/`occupiedColumns`/`Positioner`) — since
  `ButtonWidget` (via `PressableWidget`) implements `Widget` the same as `TextWidget`, it drops
  into `CharacterStatsScreen`'s `StatsTab.grid.add(...)` calls with no special handling, e.g.
  `this.grid.add(ButtonWidget.builder(Text.literal("+"), btn -> {...}).size(20, 20).build(), row,
  column)`. No need to also call `addDrawableChild` separately — `GridScreenTab`/`TabManager`
  wiring (already used in this project) takes care of registering the grid's children as
  drawable/clickable once, same as it does for `TextWidget`.
- **Sending a C2S packet from `onPress`**: no special threading needed. The `PressAction`
  lambda already runs on the client thread from a UI callback, so `ClientPlayNetworking.send(new
  MyPayload(...))` can be called directly inside it — same as any other client-thread code (see
  the C2S networking section below for the exact `send` signature).

### Scrolling a dense `GridScreenTab` (vertical overflow inside a single tab)

Researched for a real bug: `CharacterStatsScreen`'s single `StatsTab` (~15 `GridWidget` rows)
overflows vertically at high GUI Scale, with bottom rows (e.g. Dexterity's derived stats)
completely unreachable — no scrolling exists today. Verified against the decompiled named
`minecraft-clientOnly` 1.21.11 sources jar: `net/minecraft/client/gui/widget/ScrollableWidget.java`,
`ScrollableLayoutWidget.java`, `ContainerWidget.java`, `WrapperWidget.java`, `GridWidget.java`,
`LayoutWidget.java`, `net/minecraft/client/gui/tab/{Tab,GridScreenTab}.java`, plus two real
vanilla call sites: `net/minecraft/client/gui/screen/world/ExperimentsScreen.java` (a plain
`Screen` using `ScrollableLayoutWidget` directly) and a search across the whole sources jar for
any `Tab` combining `GridScreenTab` with scrolling (none exists — no vanilla screen scrolls
*inside* a `Tab`, so there's no vanilla precedent to copy verbatim for that specific
combination; the wiring below is derived from the confirmed class APIs, not copied from an
existing vanilla tab).

- **Yes, a generic scrollable container exists: `net.minecraft.client.gui.widget.ScrollableLayoutWidget`.**
  This is the right class for wrapping an arbitrary already-built widget tree (like a
  `GridWidget`), not just a uniform list of entries (`EntryListWidget`/
  `AlwaysSelectedEntryListWidget` are for that latter case and don't fit here). Confirmed
  constructor and key methods:
  ```java
  public ScrollableLayoutWidget(MinecraftClient client, LayoutWidget layout, int height);
  public void setWidth(int width);
  public void setHeight(int height);
  // implements LayoutWidget: refreshPositions(), forEachChild(...), setX/setY/getX/getY/getWidth/getHeight
  ```
  It takes **any `LayoutWidget`**, and `GridWidget extends WrapperWidget implements LayoutWidget`
  — so **the project's existing `GridWidget` (`StatsTab.grid`) can be wrapped as-is**, no
  conversion to `DirectionalLayoutWidget`/`ThreePartsLayoutWidget` needed.
- **How it works internally** (why it needs no manual scissor/input code): it wraps the given
  `layout` in a private `Container` class that `extends ContainerWidget` (itself `abstract class
  ContainerWidget extends ScrollableWidget implements ParentElement`). `forEachElement` exposes
  **only that one `Container`** as the externally-visible widget — the wrapped grid's real
  widgets become the `Container`'s internal `ParentElement` children, invisible to the outside
  layout system. This is why only **one** widget needs to be registered as a drawable/clickable
  child for the whole scrollable region (see wiring below).
- **`GridWidget`/`WrapperWidget` themselves have no scroll-offset concept at all** — confirmed,
  neither class has any scroll field/method. Scrolling only exists at the `ScrollableWidget`
  layer; wrapping is mandatory, there's no "turn on scrolling" flag on `GridWidget`.
- **Scrollbar + input are fully automatic, no manual `DrawContext` calls needed.** Confirmed in
  `ScrollableWidget` (the abstract base of `ContainerWidget`):
  - `mouseScrolled(...)` — wheel support, built in.
  - `mouseDragged(...)` + `checkScrollbarDragged(...)`/`onRelease(...)` (wired via
    `ContainerWidget.mouseClicked`/`mouseReleased`/`mouseDragged` overrides) — scrollbar-thumb
    drag support, built in.
  - `drawScrollbar(DrawContext, mouseX, mouseY)` — called automatically from
    `ScrollableLayoutWidget.Container.renderWidget(...)`, which does
    `context.enableScissor(...)` around the children's render calls, then
    `this.drawScrollbar(context, mouseX, mouseY)` after `disableScissor()`. Draws using
    vanilla's own textures: `Identifier.ofVanilla("widget/scroller")` (thumb) and
    `.../"widget/scroller_background"` (track) via
    `context.drawGuiTexture(RenderPipelines.GUI_TEXTURED, ...)` — **the same textures/visual
    convention as vanilla's other scrollable lists** (creative inventory, stats screen list,
    server list, etc.), so it matches vanilla look automatically with zero custom drawing.
    `ScrollableWidget.SCROLLBAR_WIDTH = 6` (px) is the confirmed constant; it also reserves the
    thumb on the right edge (`getScrollbarX() = getRight() - 6`).
  - Content width budget: `ScrollableLayoutWidget.refreshPositions()` sets
    `container.setWidth(Math.max(layout.getWidth() + 20, requestedWidth))` — the wrapped
    layout is inset **10px on each side** (`Container.setX` does `layout.setX(x + 10)`), which
    is where the 6px scrollbar + a few px of breathing room live. Budget for this — don't set
    the grid's own width right up against the tab area edge.
- **Confirmed real vanilla usage sketch** (`ExperimentsScreen.init()`/`refreshWidgetPositions()`,
  a plain `Screen`, not a `Tab`, but proves the exact call pattern):
  ```java
  LayoutWidget content = /* any LayoutWidget, e.g. a GridWidget */;
  ScrollableLayoutWidget scrollable = new ScrollableLayoutWidget(this.client, content, 130);
  scrollable.setWidth(310);
  someParentLayout.add(scrollable); // or add it directly wherever a LayoutWidget can go
  // ... register the actual drawable/clickable widget(s):
  scrollable.forEachChild(widget -> this.addDrawableChild(widget)); // adds exactly ONE Container widget
  // on resize/layout refresh:
  scrollable.setHeight(newViewportHeight);
  someParentLayout.refreshPositions(); // triggers ScrollableLayoutWidget.refreshPositions() internally
  ```
- **Wiring into our existing `GridScreenTab`-based `StatsTab` — confirmed, does NOT require
  abandoning `GridScreenTab`.** `GridScreenTab` is a thin class (`protected final GridWidget
  grid` + 3 overridden `Tab` methods); nothing stops a subclass from overriding
  `forEachChild`/`refreshGrid` again to route through a `ScrollableLayoutWidget` that wraps the
  same `this.grid` field, while leaving 100% of the existing row-building constructor code
  (`this.grid.add(label(...), row, col)` etc.) completely unchanged:
  ```java
  private static class StatsTab extends GridScreenTab {
      private final ScrollableLayoutWidget scrollable;

      StatsTab(TextRenderer textRenderer) {
          super(Text.literal("Stats"));
          this.grid.setRowSpacing(5).setColumnSpacing(6);
          // ...exact same this.grid.add(...) row-building code as today, unchanged...
          this.scrollable = new ScrollableLayoutWidget(MinecraftClient.getInstance(), this.grid, 200);
      }

      @Override
      public void forEachChild(Consumer<ClickableWidget> consumer) {
          this.scrollable.forEachChild(consumer); // registers exactly one Container widget
      }

      @Override
      public void refreshGrid(ScreenRect tabArea) {
          ScreenRect padded = new ScreenRect(tabArea.getLeft(), tabArea.getTop() + TOP_PADDING,
                  tabArea.width(), Math.max(0, tabArea.height() - TOP_PADDING));
          this.scrollable.setWidth(padded.width());
          this.scrollable.setHeight(padded.height());
          this.scrollable.refreshPositions(); // internally calls this.grid.refreshPositions() too
          SimplePositioningWidget.setPos(this.scrollable, padded, 0.5F, 0.0F);
      }
  }
  ```
  Notes on this sketch: `ScrollableLayoutWidget` implements `LayoutWidget` so
  `SimplePositioningWidget.setPos(...)` accepts it exactly like it already accepts `this.grid`
  today — no new positioning API needed. `refreshPositions()` on the wrapper internally calls
  `layout.refreshPositions()` (i.e. `this.grid.refreshPositions()`) for you, so the grid's own
  row/column sizing math still runs unchanged. `refreshValues()` (called every `render()` frame
  in `CharacterStatsScreen`) needs no change at all — it already mutates the `TextWidget`s held
  by fields on `StatsTab`, which are still the same widget instances, just now scrolled/clipped
  by the wrapping `Container`.
- **Recommendation: real scrolling is straightforward enough to implement directly, given the
  confirmed API above** — it's a small, surgical, additive change (one new field + two
  overridden methods), not a rewrite, and the existing row-building code and `refreshValues()`
  logic are untouched. Scrollbar rendering, wheel input, and drag-to-scroll are all automatic
  (zero manual `DrawContext` scrollbar code needed) and match vanilla's own scrollbar look.
  **Splitting into 2-3 sub-tabs by attribute family remains a valid, even lower-risk fallback**
  if scrolling turns out to misbehave in practice (e.g. subtle focus/narration issues, or the
  10px inset math needing tuning) — it reuses the *exact* already-proven `GridScreenTab`/
  `TabNavigationWidget.builder(...).tabs(...)` pattern with **zero new API surface**: just more
  `Tab` instances passed to the existing `.tabs(...)` vararg call, no new classes to learn. The
  only real downside of sub-tabs is a UX one (splitting logically-related derived stats, e.g.
  Dexterity and its derived Attack/Cast Speed and Crit Chance rows, across a tab boundary),
  not an implementation-risk one — so choose based on whether that split reads awkwardly to a
  player, not based on implementation difficulty; both are low-risk given what's confirmed
  above.

## Rendering / HUD

- `HudRenderCallback.EVENT` (client-side) was used for a custom XP HUD overlay
  (`ui/ProgressionHud.java`) but text rendering proved unreliable across several attempts
  (see commit history `caa8325`..`9c6c202`). The project switched to driving Minecraft's
  built-in XP/level bar via `ServerPlayerEntity.setExperienceLevel(...)` instead of a custom
  HUD. **Root cause found (2026-07-04, see "Custom UI (HUD / Screens)" below): `DrawContext`'s
  text-drawing methods silently no-op if the passed color's alpha byte is `0` — a plain RGB
  hex like `0xFFFFFF` renders nothing, no error, no exception.** `ui/ProgressionHud.java` only
  ever called `fill()` (whose colors it happened to always write with a non-zero top byte,
  e.g. `0xDD1A1A1A`) and never actually called any `drawText*` method — so this was never
  hit by that specific file, but it's the most likely explanation for the vague "unreliable"
  text-rendering symptom from whatever attempt(s) came before it in that commit range. See
  the new section below for verified 1.21.11 method signatures and the additional finding
  that `HudRenderCallback` itself is now `@Deprecated` in favor of `HudElementRegistry`.

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

## Player data / attributes (researched for the Class System, 2026-07-04)

Verified against the actual jars in `~/.gradle/caches/` (fabric-api sources jar for the
Attachment API's own package, which is unmapped/stable regardless of Yarn; the Yarn
1.21.11+build.6 `mappings.tiny` for vanilla class/method names) — not from training data,
since this exact question was previously flagged as an open unknown.

**Persistence answer: use Fabric's Attachment API (`fabric-data-attachment-api-v1`), not a
custom NBT mixin.** It's present in this project's Fabric API version
(`0.141.4+1.21.11` pulls it in as a submodule — confirmed via
`net.fabricmc.fabric.api.attachment.v1.*` in the Gradle module cache). This resolves the
`PlayerProgressData.writeNbt`/`readNbt`-never-called gap noted above, and is the pattern the
Class System's class-selection data now uses too:

```java
public interface AttachmentTarget { // implemented via mixin on Entity, BlockEntity, ServerLevel, ChunkAccess
    <A> A getAttached(AttachmentType<A> type);
    <A> A setAttached(AttachmentType<A> type, A value);
    <A> A getAttachedOrCreate(AttachmentType<A> type, Supplier<A> initializer);
    boolean hasAttached(AttachmentType<?> type);
}
```

Register once via `AttachmentRegistry.createPersistent(Identifier id, Codec<A> codec)` (or
`.create(id, builder -> ...)` for finer control — e.g. `.copyOnDeath()`, or `.syncWith(...)`
if the client also needs to know the value). `ServerPlayerEntity` (an `Entity`) gets
`getAttached`/`setAttached` for free via Fabric's mixin — no custom NBT read/write code
needed, no join-time re-sync boilerplate like the progression system's tick-based XP sync
required. Persistent attachments survive relogin and server restart automatically.

**Passive stat bonuses: use `EntityAttributeModifier` with a stable `Identifier`, added via
`LivingEntity.getAttributeInstance(EntityAttribute).addPersistentModifier(...)`.** Confirmed
via Yarn 1.21.11 mappings (`mappings.tiny`, class `ciq` = `EntityAttributeModifier`, class
`cio` = `EntityAttributeInstance`):

- `EntityAttributeModifier(Identifier id, double value, EntityAttributeModifier.Operation operation)`
  — **`Identifier`, not `UUID`** (the API changed away from UUID-keyed modifiers in recent
  versions; don't reach for the older UUID constructor from stale tutorials/training data).
- `LivingEntity.getAttributeInstance(EntityAttribute type)` → `EntityAttributeInstance`
  (works on `ServerPlayerEntity` since it extends `LivingEntity`).
- On the instance: `addPersistentModifier(modifier)` (serialized with the entity — survives
  relogin/restart on its own, same guarantee as vanilla equipment/potion attribute
  modifiers) vs. `addTemporaryModifier(modifier)` (not serialized). Also:
  `hasModifier(Identifier id)`, `getModifier(Identifier id)`, `removeModifier(Identifier id)`
  / `removeModifier(EntityAttributeModifier)`, `overwritePersistentModifier(modifier)`.
- Practical pattern for a class system: give each class's bonus(es) a fixed
  `Identifier` (e.g. `baum2:class_bonus/eisenwaechter_max_health`). On class
  (re)selection, `removeModifier(oldId)` for the previous class's modifier ids, then
  `addPersistentModifier(new EntityAttributeModifier(newId, value, operation))` for the new
  class. Because it's persistent, **no per-tick or per-join reapplication is needed** (unlike
  the progression system's XP bar, which needed a custom S2C packet every tick because
  vanilla only pushes XP to the client on specific server calls) — this is vanilla-native
  entity state, not something we're re-deriving each tick.

**No built-in "class" concept exists in vanilla or Fabric API** — confirmed nothing like it
in the attachment/attribute APIs above. The `classes/` package (`PlayerClass`,
`ClassRegistry`, `ClassDefinition`, `ClassManager`) is entirely custom game logic backed by
our own registry, per the `MASTERPROMPT.md` architecture sketch — there's no existing engine
concept to hook into or misuse instead.

## Open questions

- Persistence: `PlayerProgressData` already has `writeNbt`/`readNbt` but nothing calls them
  yet — need to confirm the correct 1.21.11 hook for per-player persistent data (Fabric's
  attachment API vs. classic `PlayerEntity` NBT read/write mixin) before implementing.
  **Partially answered above**: the Attachment API is confirmed as the right tool in
  general. Whether to migrate `PlayerProgressData` itself onto it (replacing the in-memory
  `HashMap` in `PlayerLevelSystem`) is a separate decision for whoever next works on
  progression persistence — not done as part of the Class System work, to avoid touching
  files Fischey is actively iterating on.

## Custom UI (HUD / Screens) — researched 2026-07-04

Quick-reference table (detail and sources for each row in the subsections below):

| Concept | Wrong name / assumption (don't use) | Correct Yarn 1.21.11 name |
|---|---|---|
| Custom always-on HUD element | `HudRenderCallback.EVENT` | Still compiles, but `@Deprecated` — use `net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry` (`addFirst`/`addLast`/`attachElementBefore`/`attachElementAfter`) + `HudElement` |
| Text color that "should" show up | Plain RGB hex, e.g. `0xFFFFFF` | Must include a non-zero alpha byte: `0xFFFFFFFF` — `DrawContext.drawText*` silently no-ops (no exception) if `ColorHelper.getAlpha(color) == 0` |
| Custom texture/icon draw | `drawTexture(Identifier, x, y, u, v, width, height)` (no pipeline arg) | `drawTexture(RenderPipeline, Identifier, x, y, u, v, width, height, textureWidth, textureHeight[, color])` — pass `RenderPipelines.GUI_TEXTURED` |
| Keybind category | A translation-key `String` (e.g. `"key.categories.misc"`) | `KeyBinding.Category` — a record type; reuse a built-in (`KeyBinding.Category.MISC`, `.MOVEMENT`, ...) or `KeyBinding.Category.create(Identifier)` once for a custom one |
| Older Fabric keybind helper | `FabricKeyBinding` | Doesn't exist in this API version — use `net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper.registerKeyBinding(KeyBinding)` |
| Per-tick keybind check to open a screen | (concept still correct) `ClientTickEvents.END_CLIENT_TICK` + `while (kb.wasPressed())` | Unchanged — confirmed still the right event and pattern for 1.21.11 |
| GUI sprite-sheet texture draw | (same signature confusion as `drawTexture` above) | `drawGuiTexture(RenderPipeline, Identifier spriteId, x, y, width, height[, color])` — `spriteId` must be under `textures/gui/sprites/**` (GUI atlas), different from a standalone icon PNG |

Researched ahead of building a custom HUD overlay (level/XP/class indicator) and a custom
`Screen` (a "Class screen" backed by `classes/ClassRegistry` and `classes/ClassManager`).

**Methodology note**: `./gradlew genSources` had not been run in this checkout (no decompiled
sources existed yet under any Loom cache). Rather than run a full `genSources` (slow, and not
strictly required), findings below were verified two ways: (1) `javap -p` against this
project's own **already-Yarn-mapped** jars — Loom caches these per-project under
`<project>/.gradle/loom-cache/minecraftMaven/net/minecraft/minecraft-clientOnly-<hash>/` and
`minecraft-common-<hash>/` (the hash matches the specific mapping/version combo; for this
project's `1.21.11` + `1.21.11+build.6`, that's the `...-6dd721cd7d-...` hash — a `26.2`-named
sibling hash also exists from the earlier 26.2 attempt documented above, don't grab that one by
accident); (2) for a few methods where the *implementation* (not just the signature) mattered,
those specific classes were decompiled with **Vineflower** (a decompiler jar that happened to
already be present locally at
`~/.gradle/caches/forge_gradle/maven_downloader/org/vineflower/vineflower/1.10.1/vineflower-1.10.1.jar`
from an unrelated project on this machine — any CFR/Vineflower/Fernflower jar works the same
way): extract just the target `.class` file(s) into a small jar, then run
`java -jar vineflower.jar -e=<full mapped jar> <small jar> <output dir>` (the `-e=` flag
supplies the full jar as external classpath so cross-references resolve). This is faster than a
full `genSources` when you only need one or two classes and worth reusing next time. Fabric
API pieces were read straight from their already-present `-sources.jar`s under
`<project>/.gradle/loom-cache/remapped_mods/remapped/net/fabricmc/fabric-api/<artifact>-16c8840d-<client|common>/<version>/` (the `16c8840d` hash is this project's `1.21.11` fabric-api
variant; a `96811e63`-hash sibling also exists from the same earlier 26.2 attempt — ignore it).

### 1. Why the old HUD text-rendering attempt likely failed: alpha-channel gotcha

Decompiling `net.minecraft.client.gui.DrawContext` (Vineflower, from
`minecraft-clientOnly-6dd721cd7d-...jar`) shows every text-drawing method funnels through:

```java
public void drawText(TextRenderer textRenderer, OrderedText text, int x, int y, int color, boolean shadow) {
    if (ColorHelper.getAlpha(color) != 0) {
        this.state.addText(new TextGuiElementRenderState(textRenderer, text, ..., x, y, color, 0, shadow, false, ...));
    }
}
```

and `net.minecraft.util.math.ColorHelper` (decompiled from `minecraft-common-6dd721cd7d-...jar`):

```java
public static int getAlpha(int argb) {
    return argb >>> 24;
}
```

**If the color's top byte is `0x00`, the draw call is a complete, silent no-op** — no
exception, no log line, the text simply never enters the render state. Passing a "plain"
color like `0xFFFFFF` (white) or `0xFF0000` (red) — i.e. forgetting the alpha channel, which
is an extremely easy mistake coming from `fill()`-style thinking or from CSS-style hex colors
— renders literally nothing. The fix is to always include a non-zero alpha byte:
`0xFFFFFFFF` (opaque white), not `0xFFFFFF`. This matches the project's own dead
`ui/ProgressionHud.java`, whose `fill()` calls all happened to already use full ARGB colors
(`0xDD1A1A1A`, `0xFFFFAA00`, `0xFF333333`, `0xFFFFFFFF`) — `fill()` doesn't have this
particular guard reachable in the same way for opaque colors — so this exact file was never
actually broken by it, but it's the most likely explanation for the "unreliable" text
rendering referenced in this doc's older "Rendering / HUD" entry (see above), and is now
confirmed as a real, version-current gotcha to watch for in any future `drawText*` call.

### 2. `HudRenderCallback` is now `@Deprecated` — the current API is `HudElementRegistry`

Confirmed in `fabric-rendering-v1` `16.2.10+0290ad933e` (the version pulled in by this
project's `fabric-api 0.141.4+1.21.11`), source read directly from its `-sources.jar`:

```java
/**
 * @deprecated Use {@link HudElementRegistry} instead.
 */
@Deprecated
public interface HudRenderCallback {
    Event<HudRenderCallback> EVENT = ...;
    void onHudRender(DrawContext drawContext, RenderTickCounter tickCounter);
}
```

It still compiles and still fires (not removed), so the project's old dead code isn't broken
by this — but new work should target the replacement, `net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry` (+ `HudElement`, `VanillaHudElements`), which is
layer-order-aware instead of "just render after everything":

```java
public interface HudElement {
    void render(DrawContext context, RenderTickCounter tickCounter); // same signature shape as before
}

public interface HudElementRegistry {
    static void addFirst(Identifier id, HudElement element);
    static void addLast(Identifier id, HudElement element);
    static void attachElementBefore(Identifier beforeThis, Identifier id, HudElement element);
    static void attachElementAfter(Identifier afterThis, Identifier id, HudElement element);
    static void removeElement(Identifier id);
    static void replaceElement(Identifier id, Function<HudElement, HudElement> replacer);
}
```

`VanillaHudElements` (same package) exposes `Identifier`s for every vanilla layer in draw
order (bottom to top) to anchor against — the ones most relevant to a level/XP/class
indicator: `VanillaHudElements.EXPERIENCE_LEVEL`, `.INFO_BAR` (the XP bar itself, or the
locator/jump bar depending on context), `.BOSS_BAR`, `.HOTBAR`, `.MISC_OVERLAYS`. Per that
class's own javadoc table: attaching *after* `BOSS_BAR` renders after all the main HUD layers
(hotbar, status bars, XP bar, status effects, boss bar) and before the sleep overlay — a good
default anchor for a custom always-on indicator. Example:

```java
HudElementRegistry.attachElementAfter(
    VanillaHudElements.BOSS_BAR,
    Identifier.of("baum2", "progression_hud"),
    (context, tickCounter) -> { /* draw here */ }
);
```

Confirmed exact `DrawContext` text-drawing overloads (via `javap -p` against
`minecraft-clientOnly-6dd721cd7d-...jar`, `net.minecraft.client.gui.DrawContext`):

```
drawText(TextRenderer, String,       int x, int y, int color, boolean shadow)
drawText(TextRenderer, Text,         int x, int y, int color, boolean shadow)
drawText(TextRenderer, OrderedText,  int x, int y, int color, boolean shadow)
drawTextWithShadow(TextRenderer, String|Text|OrderedText, int x, int y, int color)   // shadow=true convenience, calls drawText(..., true)
drawCenteredTextWithShadow(TextRenderer, String|Text|OrderedText, int centerX, int y, int color)
drawTextWithBackground(TextRenderer, Text, int x, int y, int width, int color)        // draws a background box sized to `width`, then the text
drawWrappedText / drawWrappedTextWithShadow(TextRenderer, StringVisitable, int x, int y, int width, int color[, boolean shadow])
```

This matches the shape the parent task guessed at (`drawText(TextRenderer, Text/String, x, y,
color, shadow)`), confirmed exact for 1.21.11 — just remember the alpha-channel gotcha above.

### 3. Custom `Screen` subclass — verified shape for 1.21.11

Confirmed via `javap -p` + a targeted Vineflower decompile of
`net.minecraft.client.gui.screen.Screen` (same jar):

- Constructors: `protected Screen(Text title)` (fills in `MinecraftClient.getInstance()` and
  its `textRenderer` for you) or `protected Screen(MinecraftClient, TextRenderer, Text)`.
- Override point for one-time widget setup: `protected void init()` — **no-arg**. Don't
  confuse it with `public final void init(int width, int height)`, which is `final` (can't be
  overridden), sets the `width`/`height` fields, and then calls your no-arg `init()` for you.
- Override point for per-frame drawing: `public void render(DrawContext context, int mouseX,
  int mouseY, float deltaTicks)`. The base implementation is just:
  ```java
  public void render(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
      for (Drawable drawable : this.drawables) {
          drawable.render(context, mouseX, mouseY, deltaTicks);
      }
  }
  ```
  i.e. **if you override `render()` you must call `super.render(...)`** (or otherwise render
  each drawable yourself) or every `ButtonWidget`/widget added via `addDrawableChild` simply
  won't appear.
- The screen-darkening/blur/panorama background is already drawn for you **before** your
  `render()` runs — it happens in `public final void renderWithTooltip(DrawContext, int, int,
  float)`, the method the game actually calls each frame (not `render()` directly):
  ```java
  this.renderBackground(context, mouseX, mouseY, deltaTicks); // dims/blurs/panorama, automatic
  context.createNewRootLayer();
  this.render(context, mouseX, mouseY, deltaTicks);            // your override runs here
  context.drawDeferredElements();
  ```
  You don't need to call `renderBackground()` yourself unless you want to customize it.
- `protected <T extends Element & Drawable & Selectable> T addDrawableChild(T)` — `ButtonWidget`
  satisfies all three bounds. Also `protected <T extends Drawable> T addDrawable(T)` for a
  drawable-but-not-interactive element.
- Widget construction (confirmed via `javap` on `ButtonWidget`/`ButtonWidget$Builder`):
  ```java
  this.addDrawableChild(
      ButtonWidget.builder(Text.literal("Select"), button -> { /* onPress, runs on click */ })
          .dimensions(x, y, width, height)   // or .position(x, y) + .size(w, h) separately
          .tooltip(Tooltip.of(Text.literal("...")))   // optional
          .build()
  );
  ```
  `ButtonWidget.PressAction.onPress(ButtonWidget button)` is the click-handler shape — it still
  receives the button itself as its argument, unchanged from older MC versions. (There's a
  *different*, unrelated `ButtonWidget.onPress(AbstractInput)` instance method used internally
  to dispatch clicks vs. keyboard activation — don't implement that one, it's not the lambda
  target for `.builder(...)`.)
- Opening the screen: `MinecraftClient.getInstance().setScreen(new ClassScreen())` — confirmed
  unchanged (`public void setScreen(Screen)` on `MinecraftClient`). Closing: the default
  `close()` does `this.client.setScreen(null)`; Escape triggers this automatically unless
  `shouldCloseOnEsc()` is overridden to return `false`.

### 4. Keybinding registration — `KeyBindingHelper`, and a real API change: `KeyBinding.Category`

`KeyBindingHelper.registerKeyBinding(KeyBinding)` (package
`net.fabricmc.fabric.api.client.keybinding.v1`, module `fabric-key-binding-api-v1`, pinned at
`1.1.7+4fc5413f3e` via this project's `fabric-api 0.141.4+1.21.11`) is still the correct, only
entry point — confirmed by reading its full source. **`FabricKeyBinding` does not exist**
in this API version (that name is from a much older, pre-1.14 Fabric API and shows up in a lot
of stale tutorials/training data).

**Real, version-relevant change**: `KeyBinding.Category` is no longer a plain translation-key
`String` — it's now a dedicated record type, confirmed via `javap` + Vineflower decompile of
`net.minecraft.client.option.KeyBinding`:

```java
public static record Category(Identifier id) {
    public static final Category MOVEMENT = create("movement");
    public static final Category MISC = create("misc");
    public static final Category MULTIPLAYER = create("multiplayer");
    public static final Category GAMEPLAY = create("gameplay");
    public static final Category INVENTORY = create("inventory");
    public static final Category CREATIVE = create("creative");
    public static final Category SPECTATOR = create("spectator");
    public static final Category DEBUG = create("debug");

    public static Category create(Identifier id) { /* throws IllegalArgumentException if id already registered */ }
    public Text getLabel() { return Text.translatable(this.id.toTranslationKey("key.category")); }
}
```

For a mod's own category: `KeyBinding.Category.create(Identifier.of("baum2", "main"))` —
**call this exactly once** (e.g. assign to a `static final` field), calling `create(...)` twice
with the same `Identifier` throws `IllegalArgumentException`. Its label auto-derives from
`id.toTranslationKey("key.category")` (→ `key.category.baum2.main`), which needs a matching
lang-file entry. For a single new keybind where a bespoke category isn't worth the extra lang
entry, it's simplest to just reuse `KeyBinding.Category.MISC`.

Constructor actually used in practice (there are 3 overloads; this is the useful one):
```java
new KeyBinding(String translationKey, InputUtil.Type type, int code, KeyBinding.Category category)
// e.g. new KeyBinding("key.baum2.class_screen", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_K, KeyBinding.Category.MISC)
```

Checking it each client tick to open a screen (`ClientTickEvents.END_CLIENT_TICK`, package
`net.fabricmc.fabric.api.client.event.lifecycle.v1`, module `fabric-lifecycle-events-v1`,
confirmed unchanged shape via its source):

```java
ClientTickEvents.END_CLIENT_TICK.register(client -> {
    while (CLASS_SCREEN_KEY.wasPressed()) {
        client.setScreen(new ClassScreen());
    }
});
```

`KeyBinding.wasPressed()` decrements an internal press counter and returns `true` once per
queued press — use `while`, not `if`, so rapid presses within one tick aren't dropped (this is
vanilla's own pattern, e.g. for the inventory key).

**Project-specific gotcha found while researching this**: the codebase currently has **two**
`Baum2Client` classes — `de.baum2dev.baum2.Baum2Client` (root package) and
`de.baum2dev.baum2.client.Baum2Client` (nested `client` subpackage, empty
`onInitializeClient()`). Only the root-package one is registered as the `"client"` entrypoint
in `src/main/resources/fabric.mod.json`; the nested one is dead code that nothing loads. Wire
up the new keybinding registration and `HudElementRegistry` call in
**`de.baum2dev.baum2.Baum2Client`** — adding it to the decoy class will silently do nothing.

### 5. Drawing a custom texture/icon — `drawTexture` vs `drawGuiTexture`, both now take a `RenderPipeline`

Both methods gained a leading `com.mojang.blaze3d.pipeline.RenderPipeline` parameter as part of
this version's renderer-pipeline rewrite. Old tutorials/training data showing
`drawTexture(Identifier texture, int x, int y, float u, float v, int width, int height)` (no
pipeline argument) **will not compile** against 1.21.11 — confirmed via `javap -p` on
`DrawContext`, every `drawTexture`/`drawGuiTexture` overload takes `RenderPipeline` first.

- **For a simple, standalone icon PNG** (e.g. one per class, not part of any UI sprite sheet),
  use `drawTexture`, which binds an arbitrary registered texture resource directly — no atlas
  registration needed, any file under `assets/<namespace>/textures/...` works out of the box:
  ```java
  context.drawTexture(
      RenderPipelines.GUI_TEXTURED,
      Identifier.of("baum2", "textures/gui/class_icons/eisenwaechter.png"),
      x, y,             // screen position
      0.0F, 0.0F,       // u, v (source texel offset — 0,0 for a whole, dedicated icon file)
      16, 16,           // width, height on screen
      16, 16            // textureWidth, textureHeight (the PNG's actual pixel size)
  );
  ```
  Confirmed straight from vanilla's own usage — `Screen.renderBackgroundTexture(...)` (used to
  draw the menu background) is implemented as exactly this call shape (decompiled from the same
  jar):
  ```java
  public static void renderBackgroundTexture(DrawContext context, Identifier texture, int x, int y, float u, float v, int width, int height) {
      context.drawTexture(RenderPipelines.GUI_TEXTURED, texture, x, y, u, v, width, height, 32, 32);
  }
  ```
- `drawGuiTexture(RenderPipeline, Identifier spriteId, x, y, width, height[, color|alpha])`
  draws a **GUI sprite-atlas** entry instead — internally it's
  `this.spriteAtlasTexture.getSprite(spriteId)`, so `spriteId` must resolve to a texture that
  is actually part of the GUI atlas (anything under `assets/<namespace>/textures/gui/sprites/**`
  is auto-stitched into it — the same mechanism vanilla uses for widgets like
  `Identifier.ofVanilla("widget/button")`). Prefer this only if you want automatic 9-slice/tiled
  scaling (driven by that sprite's `.mcmeta` `scaling` metadata) — for a flat, fixed-size class
  icon, plain `drawTexture` above is simpler and has no atlas-registration step to get wrong.
- `RenderPipelines.GUI_TEXTURED` (`net.minecraft.client.gl.RenderPipelines`) is the standard
  alpha-blended pipeline constant for both — confirmed present alongside sibling constants
  `GUI`, `GUI_TEXT`, `GUI_TEXTURED_PREMULTIPLIED_ALPHA`, `GUI_INVERT`, `GUI_OPAQUE_TEX_BG`,
  `GUI_NAUSEA_OVERLAY`; `GUI_TEXTURED` is what vanilla itself reaches for to draw plain textured
  UI elements, so it's the right default choice.
- A no-pipeline-argument convenience overload, `drawTexturedQuad(Identifier sprite, int x1, y1,
  x2, y2, float u1, u2, v1, v2)`, does still exist and defaults internally to
  `RenderPipelines.GUI_TEXTURED` — but note its **UV convention is different** (normalized
  `0..1` corner coordinates, not pixel `u, v, textureWidth, textureHeight` like `drawTexture`).
  Easy to mix the two up; prefer the explicit `drawTexture(pipeline, ...)` overloads above.

### 6. Class screen data-flow blocker found: the client doesn't know its own class yet

Not directly asked for, but discovered while checking whether a Class screen can actually read
"the player's selected class" client-side: `classes/ClassManager.java`'s
`AttachmentType<PlayerClass> SELECTED_CLASS` is registered with `.persistent(...)` and
`.copyOnDeath()` only — **no `.syncWith(...)`** — so today this data lives server-side only and
the client has no way to know it (same category of problem the progression system already hit
and solved for XP/level, see "Networking API reference" in `HANDOFF.md`).

The Attachment API has a purpose-built fix for exactly this, confirmed via
`fabric-data-attachment-api-v1` (`1.8.48+eed0806f3e`) source, `AttachmentRegistry.Builder`:

```java
AttachmentRegistry.Builder<A> syncWith(PacketCodec<? super RegistryByteBuf, A> packetCodec, AttachmentSyncPredicate syncPredicate);
```

`AttachmentSyncPredicate` (same module) has three static factories: `all()`, `targetOnly()`
(sync only to the player the attachment is attached to — the one wanted here), and
`allButTarget()`. Applied to `ClassManager.SELECTED_CLASS`:

```java
public static final AttachmentType<PlayerClass> SELECTED_CLASS = AttachmentRegistry.create(
    Identifier.of("baum2", "selected_class"),
    builder -> builder
        .persistent(Codec.STRING.xmap(PlayerClass::valueOf, PlayerClass::name))
        .copyOnDeath()
        .syncWith(PacketCodecs.STRING.xmap(PlayerClass::valueOf, PlayerClass::name), AttachmentSyncPredicate.targetOnly())
);
```

(`PacketCodecs.STRING` is `PacketCodec<ByteBuf, String>`, confirmed present via `javap` on
`net.minecraft.network.codec.PacketCodecs`; `.xmap(...)` is a default method on `PacketCodec`
also confirmed present — same "no `FabricPacket`, no `StreamCodec`" mapping conventions already
documented in `HANDOFF.md`'s "Networking API reference" section.) Once this is in place, the
client-side `ClientPlayerEntity` (also an `AttachmentTarget`) will have `getAttached(SELECTED_CLASS)` return the correct value without any custom S2C payload needed — this is a
simpler path than the progression system's tick-based `ExperienceSyncPayload`, since Attachment
sync is a built-in Fabric API feature triggered automatically on change, not something to
hand-roll. This is a prerequisite for the Class screen to show accurate data and is not yet
done — flagging it here so whoever builds the screen doesn't discover it by the screen just
silently showing "no class selected" for a player who has, in fact, selected one.
