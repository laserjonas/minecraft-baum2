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
  HUD. **Root cause found (2026-07-04, see "Custom UI (HUD / Screens)" below): `DrawContext`'s
  text-drawing methods silently no-op if the passed color's alpha byte is `0` — a plain RGB
  hex like `0xFFFFFF` renders nothing, no error, no exception.** `ui/ProgressionHud.java` only
  ever called `fill()` (whose colors it happened to always write with a non-zero top byte,
  e.g. `0xDD1A1A1A`) and never actually called any `drawText*` method — so this was never
  hit by that specific file, but it's the most likely explanation for the vague "unreliable"
  text-rendering symptom from whatever attempt(s) came before it in that commit range. See
  the new section below for verified 1.21.11 method signatures and the additional finding
  that `HudRenderCallback` itself is now `@Deprecated` in favor of `HudElementRegistry`.

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
