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

### Custom `EntityType<T>` registration (MC 1.21.11) — researched 2026-07-05

Researched ahead of this project's first custom hostile mob. Verified against decompiled
`net/minecraft/entity/EntityType.java`, `net/minecraft/entity/mob/HostileEntity.java`,
`net/minecraft/entity/mob/MobEntity.java`, `net/minecraft/entity/SpawnGroup.java` (from
`minecraft-common-6dd721cd7d-1.21.11-net.fabricmc.yarn.1_21_11.1.21.11+build.6-v2-sources.jar`,
generated fresh via `gradlew.bat genSources` — no prior decompile existed in this checkout) and
`net/fabricmc/fabric/api/object/builder/v1/entity/{FabricEntityTypeBuilder,FabricEntityType,
FabricDefaultAttributeRegistry}.java` (from `fabric-object-builder-api-v1` `21.1.40+4fc5413f3e`,
pulled in by `fabric-api-0.141.4+1.21.11`).

- **`FabricEntityTypeBuilder` is `@Deprecated` in this API version — do not use it.** Its own
  javadoc points at the replacement: vanilla's own `EntityType.Builder<T>` now directly
  implements Fabric's extension interface (`EntityType.java` line ~1736:
  `public static class Builder<T extends Entity> implements
  net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityType.Builder<T>`), so the
  Fabric-specific extras (`alwaysUpdateVelocity`, `canPotentiallyExecuteCommands`,
  `createLiving(...)`, `createMob(...)`) are just extra methods available directly on the
  vanilla builder via Mixin/interface-injection — there is no separate Fabric type to
  construct. **Use `net.minecraft.entity.EntityType.Builder` directly.**
- **Exact builder call chain, confirmed real vanilla usage** (`EntityType.SPIDER`'s own
  definition in `EntityType.java`, adapted):
  ```java
  RegistryKey<EntityType<?>> MY_MOB_KEY =
      RegistryKey.of(RegistryKeys.ENTITY_TYPE, Identifier.of("baum2", "my_mob"));

  EntityType<MyMobEntity> MY_MOB = Registry.register(
      Registries.ENTITY_TYPE,
      MY_MOB_KEY,
      EntityType.Builder.create(MyMobEntity::new, SpawnGroup.MONSTER)
          .dimensions(3.0F, 3.0F)          // width, height — confirmed via EntityType.Builder.dimensions(float, float)
          .build(MY_MOB_KEY)                // build(RegistryKey<EntityType<?>>) — the key is required, not optional
  );
  ```
  `EntityType.Builder.create(EntityType.EntityFactory<T> factory, SpawnGroup spawnGroup)` is the
  confirmed static entry point (there's also a key-less `create(SpawnGroup)` overload with a
  no-op factory, not useful here). `EntityType.EntityFactory<T>` is
  `@FunctionalInterface { @Nullable T create(EntityType<T> type, World world); }` — matches a
  constructor reference `MyMobEntity::new` only if the entity has a
  `(EntityType<? extends X>, World)` constructor (see `HostileEntity`'s own constructor below,
  which does). **`.build(registryKey)` requires the same `RegistryKey` used for
  `Registry.register(...)` — build it once, reuse the local for both.** Vanilla's own pattern
  wraps this in a `register(String id, Builder<T> type)` helper that does exactly the
  `RegistryKey.of(RegistryKeys.ENTITY_TYPE, Identifier...)` + `Registry.register(...)` +
  `.build(key)` dance — worth mirroring as a small helper method in a `ModEntityTypes` registry
  class rather than inlining at each registration site.
- **`SpawnGroup.MONSTER` confirmed still the correct constant name and value**, unchanged —
  `net/minecraft/entity/SpawnGroup.java`: `MONSTER("monster", 70, false, false, 128)`. (The
  constructor args are `id, despawnLimit/capacity-ish int, isPeaceful, isAnimal/rare-ish
  boolean, immediateDespawnRange` per the enum's own field order — not otherwise needed for
  registration, just confirming the constant itself is present and named `MONSTER`.)
- **`HostileEntity` constructor, confirmed exact signature**
  (`net/minecraft/entity/mob/HostileEntity.java`):
  ```java
  protected HostileEntity(EntityType<? extends HostileEntity> entityType, World world) {
      super(entityType, world);
      this.experiencePoints = 5;
  }
  ```
  `HostileEntity extends PathAwareEntity implements Monster` (still the right base class for a
  "normal" pathfinding hostile mob — confirmed via the class declaration itself). A custom mob
  subclass just needs to expose the same `(EntityType<? extends MyMobEntity>, World)`
  constructor shape and call `super(entityType, world)` (plus whatever extra one-time setup),
  so it's directly usable as `MyMobEntity::new` for the `EntityType.EntityFactory<T>` above.
- **`FabricDefaultAttributeRegistry.register(...)` — exact confirmed shape, two overloads**
  (`net/fabricmc/fabric/api/object/builder/v1/entity/FabricDefaultAttributeRegistry.java`):
  ```java
  public static void register(EntityType<? extends LivingEntity> type, DefaultAttributeContainer.Builder builder);
  public static void register(EntityType<? extends LivingEntity> type, DefaultAttributeContainer container);
  ```
  The `Builder` overload is the one to use directly (it calls `.build()` for you). Practical
  call, following `HostileEntity.createHostileAttributes()`'s own pattern
  (`MobEntity.createMobAttributes().add(EntityAttributes.ATTACK_DAMAGE)`):
  ```java
  FabricDefaultAttributeRegistry.register(
      ModEntityTypes.MY_MOB,
      HostileEntity.createHostileAttributes()
          .add(EntityAttributes.MAX_HEALTH, 40.0)
          // .add(...) any other attribute needed — see "Attributes" section above for
          // computeValue()/modifier semantics if adding modifiers instead of base values
  );
  ```
  **Every `LivingEntity` subclass must have a default-attribute registration or it throws at
  spawn time** (`DefaultAttributeRegistry` javadoc, confirmed on the registry method itself) —
  do this in the mod's common init path (`Baum2.onInitialize()`), not lazily.
- **Full minimal registration shape for this project's first custom hostile mob**, combining
  all of the above:
  ```java
  // registry/ModEntityTypes.java
  public final class ModEntityTypes {
      public static final RegistryKey<EntityType<?>> MY_MOB_KEY =
          RegistryKey.of(RegistryKeys.ENTITY_TYPE, Identifier.of("baum2", "my_mob"));

      public static final EntityType<MyMobEntity> MY_MOB = Registry.register(
          Registries.ENTITY_TYPE, MY_MOB_KEY,
          EntityType.Builder.create(MyMobEntity::new, SpawnGroup.MONSTER)
              .dimensions(3.0F, 3.0F)
              .build(MY_MOB_KEY)
      );

      public static void init() {
          FabricDefaultAttributeRegistry.register(MY_MOB, HostileEntity.createHostileAttributes()
              .add(EntityAttributes.MAX_HEALTH, 40.0));
      }
  }
  // call ModEntityTypes.init() from Baum2.onInitialize() (a static field reference alone
  // triggers class-load/registration; init() just needs to run once too, same as this
  // project's other registry init() patterns)
  ```

### Making a `HostileEntity` completely immovable — researched 2026-07-05

Researched for a stationary/turret-style boss-like hostile mob: can't walk, can't be
knocked back or shoved by other entities, stays fixed at its spawn position, but remains a
normal damageable/killable `LivingEntity` (health, death, loot, etc. all still work
normally). Verified against decompiled `LivingEntity.java`, `Entity.java`,
`entity/mob/MobEntity.java`, `entity/attribute/EntityAttributes.java` (same
`minecraft-common-6dd721cd7d-...` sources jar as above).

- **(a) Overriding `travel(Vec3d)` as a no-op — confirmed still correct, and actually stronger
  than it looks.** `LivingEntity.travel(Vec3d movementInput)` (not `final`) is the single
  dispatch point for all three movement modes (`travelInFluid`/`travelGliding`/`travelMidAir`),
  and **every one of vanilla's own `this.move(MovementType.SELF, this.getVelocity())` calls
  that actually change position lives inside `travel()`'s own call tree** (inside
  `travelFlying`/`travelInWater`/`travelInLava`; confirmed by grepping every `this.move(...)`
  call in the file — all seven hits are within `travel()`'s reachable methods). `travel()` is
  only even invoked from `tickMovement()` when
  `this.canMoveVoluntarily() && this.canActVoluntarily()` (or via a controlling passenger, not
  relevant for a hostile mob). **A fully empty `@Override public void travel(Vec3d
  movementInput) {}` therefore doesn't just skip AI-driven walking — it also skips gravity
  application and skips the position-changing `move()` call that would otherwise apply
  knockback-added velocity to the entity's actual position.** This is the single most
  effective override for "never changes position, period."
- **(b) Exact attribute identifiers for zero movement / max knockback resistance**, confirmed
  via `EntityAttributes.java`:
  ```java
  EntityAttributes.MOVEMENT_SPEED       // default 0.7, ClampedEntityAttribute range [0.0, 1024.0]
  EntityAttributes.KNOCKBACK_RESISTANCE // default 0.0, ClampedEntityAttribute range [0.0, 1.0]
  ```
  Set in the mob's `FabricDefaultAttributeRegistry.register(...)` builder:
  ```java
  HostileEntity.createHostileAttributes()
      .add(EntityAttributes.MAX_HEALTH, 40.0)
      .add(EntityAttributes.MOVEMENT_SPEED, 0.0)
      .add(EntityAttributes.KNOCKBACK_RESISTANCE, 1.0)   // 1.0 is the max the ClampedEntityAttribute allows
  ```
  Confirmed exactly how `KNOCKBACK_RESISTANCE` is consumed
  (`LivingEntity.takeKnockback(double strength, double x, double z)`):
  ```java
  public void takeKnockback(double strength, double x, double z) {
      strength *= 1.0 - this.getAttributeValue(EntityAttributes.KNOCKBACK_RESISTANCE);
      if (!(strength <= 0.0)) { /* ...apply velocity... */ }
  }
  ```
  At `KNOCKBACK_RESISTANCE = 1.0`, `strength` becomes exactly `0.0`, and the entire
  velocity-applying branch is skipped — **knockback resistance alone already fully
  neutralizes knockback**, even before considering the `travel()` override above. Both
  together is belt-and-suspenders (and the `travel()` override also covers non-knockback
  velocity sources, e.g. explosions, fluids push, etc., which don't go through
  `takeKnockback` at all).
- **(c) Goal-init method name: still `initGoals()`, unchanged, confirmed** —
  `MobEntity.java`: `protected void initGoals() {}` (empty body in the base class), called
  conditionally from `MobEntity`'s own constructor: `if (world instanceof ServerWorld) {
  this.initGoals(); }`. No rename occurred.
- **(d) Leaving goal selectors empty is sufficient to prevent AI-driven movement — confirmed,
  but not sufficient on its own for full immovability.** Simply not overriding `initGoals()`
  (or overriding it with an empty body / only adding non-movement goals, e.g. a look-at-player
  goal that doesn't implement `Goal.Control.MOVE`) means `goalSelector`/`targetSelector` never
  queue a movement-driving `Goal`, so nothing will ever set `sidewaysSpeed`/`upwardSpeed`/
  `forwardSpeed` or call `navigation.startMovingTo(...)`. **This alone stops self-directed
  walking, but does NOT stop knockback-driven displacement or gravity** (those don't go
  through goals at all) — hence (a) and (b) above are still both needed for "completely
  immovable" in the full sense the task describes, not just "doesn't walk on its own."
- **(e) Collision-push method name: `Entity.pushAwayFrom(Entity)`, confirmed unchanged**, gated
  by `Entity.isPushable()`:
  ```java
  public void pushAwayFrom(Entity entity) {
      ...
      if (!this.hasPassengers() && this.isPushable()) { this.addVelocity(-d, 0.0, -e); }
      if (!entity.hasPassengers() && entity.isPushable()) { entity.addVelocity(d, 0.0, e); }
      ...
  }
  ```
  `Entity.isPushable()` itself defaults to `false`, but **`LivingEntity` overrides it**:
  ```java
  @Override
  public boolean isPushable() {
      return this.isAlive() && !this.isSpectator() && !this.isClimbing();
  }
  ```
  i.e. every ordinary living mob is pushable by default. **Override `isPushable()` to return
  `false` unconditionally** in the custom mob to stop other entities from ever adding push
  velocity to it via `pushAwayFrom`. Given the `travel()` no-op above already blocks the
  position-changing `move()` call that any added velocity would otherwise drive, this is
  defense-in-depth (also prevents the small `addVelocity` calls from silently accumulating on
  an entity that otherwise never gets a chance to bleed them off via normal movement code).
- **Practical minimal immovable-mob shape**, combining all five:
  ```java
  public class MyMobEntity extends HostileEntity {
      protected MyMobEntity(EntityType<? extends MyMobEntity> entityType, World world) {
          super(entityType, world);
      }

      @Override
      protected void initGoals() {
          // intentionally empty, or only non-movement goals (e.g. look-at-target)
      }

      @Override
      public void travel(Vec3d movementInput) {
          // intentionally empty — no gravity, no fluid/air movement, no velocity-driven
          // position change of any kind
      }

      @Override
      public boolean isPushable() {
          return false;
      }
  }
  ```
  Combined with `MOVEMENT_SPEED = 0.0` and `KNOCKBACK_RESISTANCE = 1.0` in the default
  attributes (see (b) above), damage/health/death/loot all continue to work completely
  normally since none of that logic routes through movement/goals/travel at all.

## Custom `Item`s in 1.21.11 — `SwordItem` class no longer exists — researched 2026-07-05

Researched ahead of this project's first custom item (a sword). **Major, confirmed rework:
verified by listing every top-level class in `net/minecraft/item/` inside
`minecraft-common-6dd721cd7d-...-sources.jar` — there is no `SwordItem.java`, no
`PickaxeItem.java`, no `ToolItem.java`/`MiningToolItem.java` at all in 1.21.11.** (`AxeItem`,
`HoeItem`, `ShovelItem`, `MaceItem`, `TridentItem` still exist as dedicated classes — swords
and pickaxes specifically were folded into plain `Item` + data components, not the other
tool types.) Any tutorial/training-data reference to `new SwordItem(ToolMaterial, Item.Settings)`
**will not compile** against this version. Verified against decompiled `Item.java` and
`ToolMaterial.java` from the same sources jar, cross-checked against `Items.java`'s own
`DIAMOND_SWORD` definition.

- **Vanilla's own construction, confirmed exact** (`Items.java`):
  ```java
  public static final Item DIAMOND_SWORD =
      register("diamond_sword", new Item.Settings().sword(ToolMaterial.DIAMOND, 3.0F, -2.4F));
  ```
  i.e. a **plain `Item`**, built via `Item.Settings().sword(ToolMaterial material, float
  attackDamage, float attackSpeed)` — no dedicated sword class at all. `attackDamage`/
  `attackSpeed` here are added on top of the material's own bonuses (see below), matching the
  two trailing numeric args every vanilla sword definition passes (diamond: `3.0F` damage,
  `-2.4F` speed literal — vanilla's own diamond sword tooltip attack speed of 1.6 comes from
  `4.0 (base ATTACK_SPEED) + (-2.4)`).
- **`Item.Settings.sword(...)` confirmed exact signature and delegation**
  (`Item.java`, inside `public static class Settings`):
  ```java
  public Item.Settings sword(ToolMaterial material, float attackDamage, float attackSpeed) {
      return material.applySwordSettings(this, attackDamage, attackSpeed);
  }
  ```
  Sibling methods with the identical `(ToolMaterial, float attackDamage, float attackSpeed)`
  shape exist for the tool types that do still have their own item class:
  `pickaxe(...)`/`axe(...)`/`hoe(...)`/`shovel(...)` all delegate to a shared
  `tool(ToolMaterial, TagKey<Block> effectiveBlocks, float attackDamage, float attackSpeed,
  float disableBlockingForSeconds)`. (There's also a `spear(...)` builder used for the
  trident-like weapon shape, unrelated to swords/tools, not relevant here.)
- **`ToolMaterial` is a `record`, confirmed, not a builder or enum-like class you extend** —
  full confirmed definition (`ToolMaterial.java`):
  ```java
  public record ToolMaterial(
      TagKey<Block> incorrectBlocksForDrops, int durability, float speed,
      float attackDamageBonus, int enchantmentValue, TagKey<Item> repairItems
  ) {
      public static final ToolMaterial WOOD     = new ToolMaterial(BlockTags.INCORRECT_FOR_WOODEN_TOOL,   59,  2.0F, 0.0F, 15, ItemTags.WOODEN_TOOL_MATERIALS);
      public static final ToolMaterial STONE    = new ToolMaterial(BlockTags.INCORRECT_FOR_STONE_TOOL,   131,  4.0F, 1.0F,  5, ItemTags.STONE_TOOL_MATERIALS);
      public static final ToolMaterial COPPER   = new ToolMaterial(BlockTags.INCORRECT_FOR_COPPER_TOOL,  190,  5.0F, 1.0F, 13, ItemTags.COPPER_TOOL_MATERIALS);
      public static final ToolMaterial IRON     = new ToolMaterial(BlockTags.INCORRECT_FOR_IRON_TOOL,    250,  6.0F, 2.0F, 14, ItemTags.IRON_TOOL_MATERIALS);
      public static final ToolMaterial DIAMOND  = new ToolMaterial(BlockTags.INCORRECT_FOR_DIAMOND_TOOL, 1561, 8.0F, 3.0F, 10, ItemTags.DIAMOND_TOOL_MATERIALS);
      public static final ToolMaterial GOLD     = new ToolMaterial(BlockTags.INCORRECT_FOR_GOLD_TOOL,     32, 12.0F, 0.0F, 22, ItemTags.GOLD_TOOL_MATERIALS);
      public static final ToolMaterial NETHERITE= new ToolMaterial(BlockTags.INCORRECT_FOR_NETHERITE_TOOL,2031, 9.0F, 4.0F, 15, ItemTags.NETHERITE_TOOL_MATERIALS);
  }
  ```
  So yes, it's a fixed constant set (`WOOD`/`STONE`/`COPPER`/`IRON`/`DIAMOND`/`GOLD`/
  `NETHERITE`) **but nothing prevents constructing an entirely custom `new
  ToolMaterial(...)` record instance** for an original weapon tier — the two `TagKey` fields
  (`incorrectBlocksForDrops`, `repairItems`) are the only friction point, since they're block/
  item *tags* (`BlockTags.INCORRECT_FOR_X_TOOL`, `ItemTags.X_TOOL_MATERIALS`) that must
  resolve to something at runtime. **Practical recommendation for a first custom sword**:
  reuse an existing vanilla `ToolMaterial` constant (e.g. `ToolMaterial.IRON` or
  `ToolMaterial.DIAMOND`) rather than authoring a new record — it sidesteps needing to define
  new block/item tags just to get a working "incorrect blocks" mining-speed rule, and nothing
  about the IP/naming rules is violated by reusing a numeric tier internally (only player-
  facing names/text need to be original, not internal material constants). A genuinely
  original custom tier is possible later once a matching original ore/repair-material item
  and its own tags exist.
- **`ToolMaterial.applySwordSettings(...)` confirmed exact — this is where the sword's
  `AttributeModifiersComponent` actually gets built**:
  ```java
  public Item.Settings applySwordSettings(Item.Settings settings, float attackDamage, float attackSpeed) {
      return this.applyBaseSettings(settings)   // maxDamage(durability) + repairable(repairItems) + enchantable(enchantmentValue)
          .component(DataComponentTypes.TOOL, new ToolComponent(List.of(
              ToolComponent.Rule.ofAlwaysDropping(RegistryEntryList.of(Blocks.COBWEB.getRegistryEntry()), 15.0F),
              ToolComponent.Rule.of(<SWORD_INSTANTLY_MINES tag>, Float.MAX_VALUE),
              ToolComponent.Rule.of(<SWORD_EFFICIENT tag>, 1.5F)
          ), 1.0F, 2, false))
          .attributeModifiers(this.createSwordAttributeModifiers(attackDamage, attackSpeed))
          .component(DataComponentTypes.WEAPON, new WeaponComponent(1));
  }

  private AttributeModifiersComponent createSwordAttributeModifiers(float attackDamage, float attackSpeed) {
      return AttributeModifiersComponent.builder()
          .add(EntityAttributes.ATTACK_DAMAGE,
               new EntityAttributeModifier(Item.BASE_ATTACK_DAMAGE_MODIFIER_ID,
                   attackDamage + this.attackDamageBonus, EntityAttributeModifier.Operation.ADD_VALUE),
               AttributeModifierSlot.MAINHAND)
          .add(EntityAttributes.ATTACK_SPEED,
               new EntityAttributeModifier(Item.BASE_ATTACK_SPEED_MODIFIER_ID,
                   attackSpeed, EntityAttributeModifier.Operation.ADD_VALUE),
               AttributeModifierSlot.MAINHAND)
          .build();
  }
  ```
  **This *is* the confirmed current idiomatic shape for giving a custom item
  attack-damage/attack-speed `EntityAttributeModifier`s: a `DataComponentTypes
  .ATTRIBUTE_MODIFIERS` component holding an `AttributeModifiersComponent`, built via
  `AttributeModifiersComponent.builder().add(RegistryEntry<EntityAttribute>,
  EntityAttributeModifier, AttributeModifierSlot).build()`, attached via `Item.Settings
  .attributeModifiers(AttributeModifiersComponent)`.** `AttributeModifierSlot.MAINHAND` is
  what scopes the modifier to "only applies while this item is the held main-hand item" —
  confirmed as the slot vanilla itself uses for sword/tool attack stats (as opposed to e.g.
  armor's `AttributeModifierSlot` variants for worn equipment slots). `Item
  .BASE_ATTACK_DAMAGE_MODIFIER_ID`/`Item.BASE_ATTACK_SPEED_MODIFIER_ID` are vanilla's own fixed
  `Identifier` constants (`Identifier.ofVanilla("base_attack_damage")`/`"base_attack_speed"`)
  — reuse a **different**, mod-owned `Identifier` for a custom item's modifiers (e.g.
  `Identifier.of("baum2", "base_attack_damage")`) so it doesn't collide with/get silently
  overwritten by vanilla's own bookkeeping for the same fixed id.
- **Practical minimal custom sword shape**, combining all of the above (plain `Item`
  subclass only needed if custom *behavior* — e.g. a special right-click ability — is wanted;
  a vanilla-behavior custom sword needs no subclass at all, just a configured `Item`
  instance):
  ```java
  // No custom behavior — a plain vanilla-behavior sword, no subclass:
  RegistryKey<Item> MY_SWORD_KEY = RegistryKey.of(RegistryKeys.ITEM, Identifier.of("baum2", "my_sword"));
  Item.Settings settings = new Item.Settings()
      .registryKey(MY_SWORD_KEY)                 // MUST be set before constructing the Item — see above
      .sword(ToolMaterial.IRON, 4.0F, -2.2F);     // reuse IRON tier; tune damage/speed originally
  public static final Item MY_SWORD = Registry.register(Registries.ITEM, MY_SWORD_KEY, new Item(settings));

  // With custom behavior — subclass plain Item (NOT any "SwordItem" — it doesn't exist):
  public class MySwordItem extends Item {
      public MySwordItem(Item.Settings settings) {
          super(settings); // Item(Item.Settings settings) — confirmed exact ctor, Item.java:171
      }
      // override e.g. postHit(...)/useOnBlock(...)/etc. for custom behavior
  }
  ```
  **Ordering matters here, confirmed by reading both sides**: vanilla's own
  `Items.register(RegistryKey<Item> key, Function<Item.Settings, Item> factory, Item.Settings
  settings)` helper does `Item item = factory.apply(settings.registryKey(key));` — i.e. it
  calls `settings.registryKey(key)` **before** constructing the `Item`, then separately
  `Registry.register(Registries.ITEM, key, item)` after. This isn't just a convention: `Item`'s
  own constructor reads `settings.getTranslationKey()` (and `settings.getModelId()`) — both
  derived from whatever registry key was set on the settings at that point — so calling
  `.registryKey(key)` on the `Item.Settings` **before** passing it to the `Item`/subclass
  constructor is required, not optional, or the translation key and model id will be wrong/
  missing. `Registry.register(...)`'s own key argument does not retroactively fix this up.

## Loot / Guaranteed Item Drops — researched 2026-07-05

Researched for a guaranteed single-item drop from this project's first custom hostile mob.
Verified against decompiled `LivingEntity.java` (same sources jar as above) and
`net.fabricmc.fabric.api.datagen.v1.provider.{FabricLootTableProvider,
FabricEntityLootTableProvider}` (`fabric-data-generation-api-v1` `23.4.1+69974c4e3e`, pulled
in by `fabric-api-0.141.4+1.21.11`). Also checked this project's actual
`Baum2DataGenerator.java` (`src/client/java/de/baum2dev/baum2/client/Baum2DataGenerator.java`).

- **Overriding `dropLoot(...)` is still valid and is the current override point, confirmed
  unchanged name** — `LivingEntity.java`:
  ```java
  protected void dropLoot(ServerWorld world, DamageSource damageSource, boolean causedByPlayer) {
      Optional<RegistryKey<LootTable>> optional = this.getLootTableKey();
      if (!optional.isEmpty()) {
          this.dropLoot(world, damageSource, causedByPlayer, optional.get());
      }
  }
  ```
  Called from `LivingEntity.drop(ServerWorld, DamageSource)` (itself invoked from the
  entity's death handling), gated by `this.shouldDropLoot(world)` (which `HostileEntity`
  already overrides to check `GameRules.DO_MOB_LOOT`, so a custom `HostileEntity` subclass
  gets that check for free). **For a guaranteed single item, override `dropLoot(...)` directly
  and skip the loot-table system entirely**:
  ```java
  @Override
  protected void dropLoot(ServerWorld world, DamageSource damageSource, boolean causedByPlayer) {
      this.dropStack(world, new ItemStack(ModItems.MY_DROP));
  }
  ```
  `Entity.dropStack(ServerWorld world, ItemStack stack)` confirmed exact signature
  (`Entity.java`), returns `@Nullable ItemEntity`. This is simpler than a datagen loot table
  for a fixed, always-the-same, single-item drop, and needs zero data-pack JSON.
- **Datagen (`FabricLootTableProvider`/`FabricEntityLootTableProvider`) confirmed to exist as
  the alternative, idiomatic path for a real (weighted/conditional/multi-entry) loot table**,
  but **this project's `Baum2DataGenerator` is confirmed an effectively-empty stub — it IS a
  real blocker for that specific path, though not for the direct-override path above.** Read
  directly:
  ```java
  public class Baum2DataGenerator implements DataGeneratorEntrypoint {
      @Override
      public void onInitializeDataGenerator(FabricDataGenerator fabricDataGenerator) {
          FabricDataGenerator.Pack pack = fabricDataGenerator.createPack();
          // <- nothing registered here; pack.addProvider(...) is never called
      }
  }
  ```
  Since no provider is registered, running the datagen entrypoint currently emits nothing —
  for the loot-table route to work at all, either (a) wire up
  `pack.addProvider(MyEntityLootTableProvider::new)` here (a subclass of
  `FabricEntityLootTableProvider`) and run datagen to emit the JSON, or (b) hand-author the
  raw loot table JSON directly under
  `src/main/resources/data/baum2/loot_table/entities/my_mob.json` and point
  `getLootTableKey()` (or the `EntityType.Builder`'s implicit default, which already resolves
  to `entities/<id>` per `EntityType.Builder`'s `lootTable` field default — see the Registries
  section above) at it, with no datagen involved at all. **Recommendation for a single
  guaranteed drop: skip loot tables entirely and use the direct `dropLoot(...)` override
  above** — it's strictly simpler, needs no JSON, no datagen wiring, and isn't blocked by the
  empty `Baum2DataGenerator` stub. Reach for the datagen path only once an actual
  probability/weighted/multi-item table is needed, at which point wiring up
  `Baum2DataGenerator` becomes necessary regardless of this specific mob.

## Combat / Damage — `LivingEntity.damage(...)` signature and `AFTER_DAMAGE` event — researched 2026-07-05

Researched for "run custom logic exactly once per health-threshold crossing" on a custom
hostile mob (e.g. an enrage effect at 50% health). Verified against decompiled
`LivingEntity.java` (same sources jar) and
`net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents`
(`fabric-entity-events-v1` `3.1.1+1d0ab4303e`, pulled in by `fabric-api-0.141.4+1.21.11`).

- **`LivingEntity.damage(...)` confirmed reworked — `ServerWorld` is now the first
  parameter, exactly as suspected**:
  ```java
  @Override
  public boolean damage(ServerWorld world, DamageSource source, float amount)
  ```
  (`Entity`'s own abstract/base declaration matches this same shape — `LivingEntity`
  `@Override`s it, and it is **not** `final`, so a custom mob subclass can override it
  directly.) This is a legitimate, simple override point: call `super.damage(world, source,
  amount)` to get the boolean "was any damage actually applied" result, then read
  `this.getHealth()` immediately after — the field is already mutated by the time `damage(...)`
  returns (health mutation happens synchronously inside `applyDamage(...)`, called from within
  `damage(...)` itself, confirmed by reading the full method body).
- **`ServerLivingEntityEvents.AFTER_DAMAGE` confirmed to exist, exact functional signature**
  (`net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents`):
  ```java
  Event<AfterDamage> AFTER_DAMAGE = ...;
  @FunctionalInterface
  interface AfterDamage {
      void afterDamage(LivingEntity entity, DamageSource source,
                        float baseDamageTaken, float damageTaken, boolean blocked);
  }
  ```
  Fired from inside `LivingEntity.damage(...)` itself after the health mutation is applied (or
  after a shield fully/partially blocked it) — **confirmed caveat, straight from the event's
  own javadoc: "This event is not fired if the entity was killed by the damage."** So relying
  on `AFTER_DAMAGE` alone would silently miss the exact tick a threshold-crossing hit is also
  the killing blow. There's also `ServerLivingEntityEvents.ALLOW_DAMAGE` (before mitigation,
  cancellable) and `.AFTER_DEATH`/`.ALLOW_DEATH` for the death-adjacent cases, all in the same
  class, all confirmed present with the shapes documented in the class's own file.
- **Recommendation for this project's use case (a custom mob's own threshold logic, not a
  generic "any entity" hook): override `damage(ServerWorld, DamageSource, float)` directly
  in the mob's own class, not the Fabric event.** Reasons: (1) it's the mob's own class, so no
  event registration/`instanceof` filtering is needed to scope the logic to just this mob; (2)
  it naturally also covers the lethal-hit case (`AFTER_DAMAGE`'s documented gap above) since
  the override can check `this.getHealth() <= 0` or `this.isDead()` itself, inline, right where
  `super.damage(...)` returns; (3) "exactly once per threshold crossing" is trivial to
  guarantee with a simple boolean/enum instance field on the mob (e.g. `private boolean
  enraged = false;`) checked and set inside the override, with no ordering/registration
  concerns relative to other listeners. Reach for `ServerLivingEntityEvents.AFTER_DAMAGE`
  instead only if the same logic must also apply to entities this project doesn't own the
  class of (e.g. reacting to *any* mob or player crossing a threshold) — confirmed available
  and correctly shaped for that broader case, just not the simplest tool for a mob-owned
  effect.

## Spawning additional hostile mobs server-side near a position — researched 2026-07-05

Verified against decompiled `EntityType.java` and `server/world/ServerWorld.java` (same
sources jar).

- **`EntityType<T>.spawn(ServerWorld world, BlockPos pos, SpawnReason reason)` confirmed as
  the single-call convenience factory+position+spawn method** — the most direct fit for
  "spawn a mob near a position":
  ```java
  public @Nullable T spawn(ServerWorld world, BlockPos pos, SpawnReason reason) {
      return this.spawn(world, null, pos, reason, false, false);
  }
  // internally: creates the entity, positions it at pos (+0.5/+0.5 center offset, random yaw),
  // calls world.spawnEntityAndPassengers(entity), and for MobEntity also calls
  // mobEntity.initialize(world, world.getLocalDifficulty(...), reason, null) + playAmbientSound()
  ```
  Usage: `EntityType.SPIDER.spawn(serverWorld, blockPos, SpawnReason.MOB_SUMMONED)` (or your own
  custom `EntityType`). Returns `@Nullable T` — null if the entity type is feature-gated off
  for that world's enabled features (irrelevant for a mod-added type) — no null-check skip
  needed for a normal mod entity type, but the signature is still `@Nullable` so a defensive
  null-check is good practice either way.
- **Lower-level factory, `EntityType<T>.create(World world, SpawnReason reason)`, confirmed
  exact signature** (just constructs the entity via the registered `EntityFactory`, no
  position/spawn-into-world):
  ```java
  public @Nullable T create(World world, SpawnReason reason) {
      return !this.isEnabled(world.getEnabledFeatures()) ? null : this.factory.create(this, world);
  }
  ```
  Use this (plus manual `entity.refreshPositionAndAngles(...)` and `world.spawnEntity(entity)`)
  only if finer control over positioning/initialization is needed than the one-call `spawn(...)`
  above provides — for a plain "put a mob near this position" case, prefer `spawn(...)`.
- **`ServerWorld.spawnEntity(Entity entity)` confirmed still exact, unchanged**:
  ```java
  public boolean spawnEntity(Entity entity)
  ```
  This is what `EntityType.spawn(...)`'s internal `world.spawnEntityAndPassengers(entity)`
  itself ultimately routes through for the plain (non-passenger) case — still correct to call
  directly if constructing+positioning an entity by hand instead of via `EntityType.spawn(...)`.

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

### Custom entity model + renderer registration (MC 1.21.11 / Fabric API 0.141.4) — researched 2026-07-05

Researched implementing `StoneOfSpidersEntityRenderer`/`StoneOfSpidersEntityModel` (this
project's first custom-rendered mob). Verified against decompiled
`fabric-rendering-v1-16.2.10+0290ad933e-sources.jar` and vanilla's
`net/minecraft/client/render/entity/{EntityRendererFactories,EntityModel,Model}.java` /
`net/minecraft/client/render/entity/model/{EndermiteEntityModel,ShulkerEntityModel,
EntityModelLayer}.java`.

- **`net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry` is `@Deprecated`** in
  this Fabric API version (confirmed via its own javadoc: `@deprecated Replaced with transitive
  access wideners in Fabric Transitive Access Wideners (v1).`) — it still compiles and works,
  but `-Xlint:deprecation` flags it. **Use vanilla's own
  `net.minecraft.client.render.entity.EntityRendererFactories.register(EntityType<? extends T>,
  EntityRendererFactory<T>)` directly instead** (same call shape, just a different holder
  class) — confirmed this is exactly how every vanilla mob renderer is registered
  (`EntityRendererFactories`'s static initializer registers all ~150 vanilla entities this
  way).
- `net.fabricmc.fabric.api.client.rendering.v1.EntityModelLayerRegistry.registerModelLayer(
  EntityModelLayer, TexturedModelDataProvider)` is **not** deprecated — still the correct way
  to register a custom `EntityModelLayer`'s `TexturedModelData` supplier.
  `TexturedModelDataProvider` is a nested `@FunctionalInterface` on that same class (`
  TexturedModelData createModelData()`), so a static no-arg `getTexturedModelData()` method
  reference matches it directly.
- **Model-space ground-line convention, confirmed via `LivingEntityRenderer.render()`'s actual
  matrix ops**: after `matrixStack.scale(-1.0F, -1.0F, 1.0F)` (the Y-flip from model-space to
  world-space), every `LivingEntityRenderer` subclass applies a fixed
  `matrixStack.translate(0.0F, -1.501F, 0.0F)` — confirmed as a genuine engine-wide constant,
  not per-mob (`EntityModel.java` even names it `field_52908 = -1.501F`, an unmapped Yarn
  intermediate constant kept for exactly this value). Net effect: **absolute model-space Y=24
  is always ground level, for every mob regardless of its actual height** (24 units = 1.5
  blocks, the `-1.501F` constant's origin) — smaller Y values are higher up, larger Y values
  go underground. Confirmed by cross-referencing `ShulkerEntityModel` (base cuboid's bottom
  edge sits at exactly `pivot(0,24,0) + offsetY(-8) + sizeY(8) = 24`) and `EndermiteEntityModel`
  (body segments pivoted at `24 - segmentHeight`, i.e. bottom edge also always resolves to 24).
  Useful for any future stationary/ground-fused custom mob model, not just this one.
- **Minimal custom mob renderer shape** (no animation): model class `extends
  EntityModel<EntityRenderState>` using the 1-arg `protected EntityModel(ModelPart root)`
  constructor (defaults to `RenderLayers::entityCutoutNoCull`, fine for an opaque texture; skip
  entirely if no per-tick pose animation is needed — `Model`'s own default `setAngles` just
  calls `resetTransforms()`). Renderer class `extends MobEntityRenderer<YourMobEntity,
  LivingEntityRenderState, YourEntityModel>`, constructor `super(context, new
  YourEntityModel(context.getPart(YOUR_LAYER)), shadowRadius)`, must override
  `getTexture(LivingEntityRenderState)` (abstract on `LivingEntityRenderer`) and
  `createRenderState()` (abstract on `EntityRenderer`, returns `new LivingEntityRenderState()`
  if no custom render-state fields are needed).

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

### `ATTACK_DAMAGE` / `ATTACK_SPEED` constants — confirmed, same de-prefixed naming as `MAX_HEALTH`

Researched 2026-07-04 for scaling real melee combat off Strength/Dexterity. Verified straight
from decompiled `EntityAttributes.java` (same file already read for the `MAX_HEALTH`
precedent, `minecraft-common-6dd721cd7d-...-sources.jar`):

```java
public static final RegistryEntry<EntityAttribute> ATTACK_DAMAGE = register(
    "attack_damage", new ClampedEntityAttribute("attribute.name.attack_damage", 2.0, 0.0, 2048.0)
);
public static final RegistryEntry<EntityAttribute> ATTACK_SPEED = register(
    "attack_speed", new ClampedEntityAttribute("attribute.name.attack_speed", 4.0, 0.0, 1024.0).setTracked(true)
);
```

- **No `GENERIC_` prefix, exactly like `MAX_HEALTH`** — `EntityAttributes.ATTACK_DAMAGE` and
  `EntityAttributes.ATTACK_SPEED` are both correct, confirmed real fields in this Yarn mapping.
  Every constant in this file follows the same de-prefixed pattern; don't reach for
  `GENERIC_ATTACK_DAMAGE`/`GENERIC_ATTACK_SPEED` from older tutorials.
- Base/clamp values: `ATTACK_DAMAGE` default `2.0`, clamped `[0.0, 2048.0]`, **not**
  `.setTracked(true)` (i.e. not synced to tracking clients by default — matches vanilla, since
  other players don't need to see your exact attack-damage attribute value). `ATTACK_SPEED`
  default `4.0` (this is the *attribute's* internal unit — vanilla's familiar "attacks per
  second" number on the tooltip is a display transform of this, not a 1:1 reading), clamped
  `[0.0, 1024.0]`, **is** `.setTracked(true)`.

### `EntityAttributeInstance.computeValue()` — confirmed exact operation order and semantics

Verified straight from the decompiled method body (`EntityAttributeInstance.java`, same file
already used for the `addPersistentModifier`/`removeModifier` findings above):

```java
private double computeValue() {
    double d = this.getBaseValue();
    for (var m : getModifiersByOperation(ADD_VALUE))          d += m.value();
    double e = d;
    for (var m : getModifiersByOperation(ADD_MULTIPLIED_BASE)) e += d * m.value();
    for (var m : getModifiersByOperation(ADD_MULTIPLIED_TOTAL)) e *= 1.0 + m.value();
    return this.type.value().clamp(e);
}
```

- **Three-phase, fixed order, confirmed**: all `ADD_VALUE` modifiers sum onto the base value
  first (giving `d`); then all `ADD_MULTIPLIED_BASE` modifiers each add `d * value()` onto a
  running total `e` that starts at `d`; then all `ADD_MULTIPLIED_TOTAL` modifiers each multiply
  `e` by `1.0 + value()`, one after another (so two `ADD_MULTIPLIED_TOTAL` modifiers of `0.5`
  each compound: `e * 1.5 * 1.5`, not `e * 2.0`). Final result is clamped by the attribute's own
  `EntityAttribute.clamp(double)` (the widened range for `MAX_HEALTH` via
  `ClampedEntityAttributeAccessor` applies here too, for any attribute this project widens).
- **Confirms the semantics assumed for a percentage "Attack Speed Multiplier" bonus**: an
  `ADD_MULTIPLIED_TOTAL` modifier with `value() = 0.5` means "+50% of the total computed so far
  at this phase" (`e *= 1.0 + 0.5`), i.e. exactly "add 1 to the modifier's own value, then
  multiply" per the method's own javadoc on `EntityAttributeModifier.Operation` — **not** "the
  modifier's raw value is used directly as the multiplier" (that would need `value() = 1.5` for
  a +50% effect, which is wrong). Use `ADD_MULTIPLIED_TOTAL` with `value() = (multiplier - 1.0)`
  for a "Dexterity gives +X% attack speed" style bonus, or use `ADD_VALUE` directly on the
  attribute's own unit if a flat additive bonus is wanted instead (e.g. `Base Attack` as a flat
  `+N` on `ATTACK_DAMAGE` — use `ADD_VALUE`, matching the existing `addPersistentModifier`
  pattern already documented above for class bonuses).
- `Operation` enum constants confirmed exact names, unchanged from what's already documented
  above for the Class System: `ADD_VALUE`, `ADD_MULTIPLIED_BASE`, `ADD_MULTIPLIED_TOTAL` — no
  `GENERIC_`-style variants, no renames.
- `addPersistentModifier(EntityAttributeModifier)` and `removeModifier(Identifier)` (returns
  `boolean`, `true` if a modifier with that id existed and was removed) both confirmed present
  on `EntityAttributeInstance` with these exact names — reusable for a "Base Attack"/"Attack
  Speed Multiplier" bonus that needs to be added once and later updated (e.g. on attribute-point
  respend): call `removeModifier(id)` then `addPersistentModifier(new EntityAttributeModifier(id,
  newValue, operation))` — there is no direct "update in place" method other than
  `updateModifier(EntityAttributeModifier)`, which is package-private (`EntityAttributeInstance`
  itself, no access modifier) and not usable from mod code; `overwritePersistentModifier(modifier)`
  is the actual public one-call replace-or-add method (does `removeModifier` then `addModifier`
  then re-registers as persistent) — **prefer `overwritePersistentModifier(...)` over the
  manual remove-then-add pair** for updating an existing bonus, since it's the same net effect in
  one call and is what vanilla/Fabric API code itself would reach for.

## Combat / Damage

Researched 2026-07-04 for a custom Crit Chance % roll that adds bonus damage on top of
whatever vanilla computes for a player's melee attack, independent of vanilla's own
fall-based critical hit. Verified against decompiled `PlayerEntity.java`
(`minecraft-common-6dd721cd7d-...-sources.jar`) and the `fabric-entity-events-v1` /
`fabric-events-interaction-v0` source jars (`3.1.1+1d0ab4303e` and `4.1.1+3b89ecf63e`
respectively, both pulled in transitively by `fabric-api-0.141.4+1.21.11`).

- **The real method is still `PlayerEntity.attack(Entity target)`, unchanged name.** Full body
  read and confirmed. The exact final-damage local variable, right before the entity is
  actually damaged, is `i` in:
  ```java
  float i = f + h;                                  // <- final melee damage, pre-crit-roll insertion point
  ...
  boolean bl5 = target.sidedDamage(damageSource, i); // <- this is where it's applied
  ```
  (`f` = base attack damage after cooldown scaling + weapon bonus + vanilla's own 1.5x
  fall-crit multiplier already folded in; `h` = the attack-cooldown sweep/partial-damage term.
  `i` is the true final float that reaches the target — this is the value to multiply/add onto
  for a custom Crit Chance roll.)
- **`Entity.sidedDamage` signature, confirmed** (`Entity.java`, same jar):
  ```java
  public final boolean sidedDamage(DamageSource source, float amount)
  ```
  `final` — cannot be overridden, must be intercepted via Mixin at the call site inside
  `PlayerEntity.attack`, not by subclassing/overriding.
- **No Fabric API event modifies this float — confirmed by reading both plausible
  candidates:**
  - `net.fabricmc.fabric.api.event.player.AttackEntityCallback` (package
    `net.fabricmc.fabric.api.event.player`, module `fabric-events-interaction-v0`) fires
    *before* `PlayerEntity.attack` even runs and only returns an `ActionResult`
    (`SUCCESS`/`PASS`/`FAIL`) — cancel-or-allow only, no damage value passes through it at all.
  - `net.fabricmc.fabric.api.entity.event.v1.ServerEntityCombatEvents.AFTER_KILLED_OTHER_ENTITY`
    (module `fabric-entity-events-v1`) only fires *after* a kill has already happened, with no
    damage-amount parameter — not useful for pre-damage modification either.
  - Neither `net.fabricmc.fabric.api.entity.event.v1` nor
    `net.fabricmc.fabric.api.event.player` (both fully listed/checked via each jar's file
    listing) contains anything shaped like "modify attack damage" — this is a real gap in
    Fabric API's public event surface, a Mixin is required.
- **Recommended Mixin: plain Sponge Mixin `@ModifyArg`, not `@ModifyVariable`/
  `@ModifyExpressionValue`.** Since the final damage value is passed as the second argument to
  a call with a `final`, uniquely-signatured target method (`Entity.sidedDamage`), targeting
  the **call site's argument** avoids the ordinal-guessing risk of `@ModifyVariable` (the
  decompiled method reuses short float-letter locals `f`/`g`/`h`/`i`/`j` — brittle to target by
  local-variable ordinal if the compiled bytecode's local slot layout doesn't match the
  decompiled source 1:1). `@ModifyArg` targets the call, not the local, and needs only the
  target method's own descriptor:
  ```java
  @Mixin(PlayerEntity.class)
  public class PlayerAttackDamageMixin {
      @ModifyArg(
          method = "attack",
          at = @At(
              value = "INVOKE",
              target = "Lnet/minecraft/entity/Entity;sidedDamage(Lnet/minecraft/entity/damage/DamageSource;F)Z"
          ),
          index = 1
      )
      private float baum2$applyCritRoll(float amount) {
          // roll Crit Chance here (server-side only — (PlayerEntity)(Object)this is a
          // ServerPlayerEntity when this runs on the logical server), return boosted amount
          return amount;
      }
  }
  ```
  `@ModifyArg` is a core `org.spongepowered.asm.mixin.injection.ModifyArg` annotation — **not**
  MixinExtras-specific, so it needs nothing beyond the Mixin setup this project already has.
  MixinExtras' `@ModifyExpressionValue` would also work here in principle (targeting the same
  `INVOKE` slice) but offers no advantage over `@ModifyArg` for a single-argument case like
  this — `@ModifyArg` is the more direct, standard tool.
- **MixinExtras availability confirmed, but not via an explicit `build.gradle` dependency
  line** — grepped `build.gradle` and `gradle.properties` for `mixinextras`/`MixinExtras`,
  found nothing explicit. However `io.github.llamalad7:mixinextras-fabric:0.5.4` **is** present
  in this machine's Gradle module cache (`~/.gradle/caches/modules-2/files-2.1/io.github.llamalad7/mixinextras-fabric/0.5.4/`), confirming **Fabric Loom 1.17.13 bundles MixinExtras
  transparently** (transitively injected by the Loom plugin itself since Loom 1.1+, no explicit
  dependency declaration needed) — so `@ModifyExpressionValue`/`@WrapOperation`/etc. are
  available if ever needed for a trickier injection than this one, without adding anything to
  `build.gradle`.
- Existing mixin wiring precedent to follow: add the new mixin class under
  `de.baum2dev.baum2.mixin` and list its simple name in `src/main/resources/baum2.mixins.json`
  (`"mixins": [...]`), same as `LivingEntityMixin`/`ExperienceOrbSpawnMixin`/
  `ClampedEntityAttributeAccessor` already are.

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

## Combat / Skill effects (researched for the Active Skill System — 8 spells / 4 classes, 2026-07-04)

Researched ahead of writing the server-side spell-cast handler (damage, AoE query, knockback,
status effects, healing, a dash/short-teleport, and cooldowns). **Verified directly against
this project's own already-Yarn-mapped 1.21.11 jars** via `javap -p`, plus targeted Vineflower
decompiles for a few methods where the *implementation* (not just the signature) mattered —
not from training data, since this project has repeatedly hit stale-API issues this session.

- Jars used: `.gradle/loom-cache/minecraftMaven/net/minecraft/minecraft-common-6dd721cd7d/1.21.11-net.fabricmc.yarn.1_21_11.1.21.11+build.6-v2/...jar`
  (the project's own local `1.21.11` + `1.21.11+build.6` mapped jar, hash `6dd721cd7d` — same
  jar cited in the "Custom UI" section above; ignore the sibling `043a8b3edf`-hash jar, that's
  the earlier abandoned 26.2 attempt).
- Decompiled with Vineflower (`~/.gradle/caches/forge_gradle/maven_downloader/org/vineflower/vineflower/1.10.1/vineflower-1.10.1.jar`,
  already present locally from an unrelated project — same tool/technique as the "Custom UI"
  section): `net.minecraft.entity.Entity`, `net.minecraft.entity.LivingEntity`,
  `net.minecraft.server.network.ServerPlayerEntity`,
  `net.minecraft.server.network.ServerPlayNetworkHandler`,
  `net.minecraft.server.network.EntityTrackerEntry` (the last one specifically to settle the
  velocity-sync question below — the signature alone doesn't tell you when a packet actually
  gets sent, only the decompiled tracker logic does).

Quick-reference table:

| Concept | Wrong name / stale assumption | Correct Yarn 1.21.11 name |
|---|---|---|
| Deal damage from code | `entity.damage(DamageSource, float)` (2-arg) | `livingEntity.damage(ServerWorld, DamageSource, float)` — confirmed, the `ServerWorld` param is real and required. The old 2-arg shape survives only as `@Deprecated` `Entity.serverDamage(DamageSource, float)` (void, silently no-ops off-thread) / `Entity.sidedDamage(DamageSource, float)` (boolean) — both are compatibility shims, not the current idiom; call the 3-arg method directly. |
| AoE "entities near a point" query | `world.getEntitiesByClass(Class<T>, Box, Predicate)` | **Does not exist in this mapping at all.** Use `world.getEntitiesByType(TypeFilter<Entity, T>, Box, Predicate<? super T>)`, with `TypeFilter.instanceOf(LivingEntity.class)` as the filter. |
| Push/knockback an entity | `entity.setVelocity(vec)` alone | Compiles and *does* move the entity server-side, but **silently fails to visually sync to already-connected clients** — see full gotcha below. Use `takeKnockback(strength, dx, dz)` for combat-style knockback (respects `EntityAttributes.KNOCKBACK_RESISTANCE` and self-marks the sync flag), or `addVelocity(Vec3d)` for a raw push (also self-marks the flag). Either way, if the target is a `ServerPlayerEntity`, you additionally need to manually push an `EntityVelocityUpdateS2CPacket` to that player's own connection — see below. |
| Status effect duration | Assumed seconds | Ticks, unchanged (20 ticks = 1 second) — `StatusEffectInstance(RegistryEntry<StatusEffect>, int durationTicks, int amplifier)`. |
| Heal an entity | Assumed renamed/moved like `damage` was | Unchanged: `LivingEntity.heal(float amount)`, still exactly this signature. |
| Dash/reposition a player | Directly mutate position fields, or assume a packet must be hand-sent | `ServerPlayerEntity.requestTeleport(double, double, double)` (absolute) / `.requestTeleportOffset(double, double, double)` (relative — perfect for a dash) — both **self-syncing**, confirmed by decompile, no manual packet needed for this specific class. |

### 1. Dealing damage — `LivingEntity.damage(ServerWorld, DamageSource, float)`, confirmed

`javap -p` on `net.minecraft.entity.LivingEntity`:

```java
public boolean damage(net.minecraft.server.world.ServerWorld, net.minecraft.entity.damage.DamageSource, float);
```

Confirmed exact match to what the parent task suspected: yes, a `ServerWorld` first parameter
is really there in 1.21.11 (this is the actual current shape — not a hypothetical). It's
declared `public abstract boolean damage(ServerWorld, DamageSource, float)` on the base
`Entity` class; `LivingEntity` supplies the concrete implementation. Returns `boolean` — whether
the damage was actually applied (`false` if blocked, invulnerable, in creative mode, etc.), so
check it if a spell effect (e.g. lifesteal) should only trigger on an actual hit.

**Don't reach for the look-alike deprecated overloads.** Decompiled `Entity.java` also has:

```java
@Deprecated
public final void serverDamage(DamageSource source, float amount) {
    if (this.world instanceof ServerWorld serverWorld) { this.damage(serverWorld, source, amount); }
}

@Deprecated
public final boolean sidedDamage(DamageSource source, float amount) {
    return this.world instanceof ServerWorld serverWorld ? this.damage(serverWorld, source, amount) : this.clientDamage(source);
}
```

Both exist purely as old-call-site compatibility shims (both explicitly `@Deprecated`) — new
code should call the 3-arg `damage(ServerWorld, DamageSource, float)` directly, not either of
these. Getting a `ServerWorld` from a `ServerPlayerEntity` needs no cast:
`ServerPlayerEntity.getEntityWorld()` is overridden with a covariant return type of
`ServerWorld` (confirmed via `javap`), unlike the base `Entity.getEntityWorld()` (returns plain
`World`) — so inside a C2S packet handler, `context.player().getEntityWorld()` is already a
`ServerWorld`, ready to pass straight into `damage(...)`. For an arbitrary `LivingEntity` target
(e.g. a mob a spell just hit), cast: `(ServerWorld) target.getEntityWorld()` — safe since spell
logic only ever runs on the server/logical-server thread.

**`DamageSource` construction — no custom `DamageType`/tag needed for a spell.** Confirmed via
`net.minecraft.entity.damage.DamageSources` (obtained via `world.getDamageSources()`, a
per-world cached instance, `World.getDamageSources()` confirmed present): it already exposes a
convenience factory built for exactly this case:

```java
public net.minecraft.entity.damage.DamageSource indirectMagic(Entity attacker, Entity source);
public net.minecraft.entity.damage.DamageSource magic(); // no attacker attribution
```

`DamageTypes.INDIRECT_MAGIC` (backing `indirectMagic(...)`) is vanilla's own damage type for
"magic damage attributed to a caster, but not a direct melee/projectile hit" — it's what
Evokers' fangs/vex conjuring already use internally. It gives correct death messages ("Foo was
killed by magic" style, attributed to the caster for kill-credit/aggro purposes) and reads
correctly as "this is a spell/ability" cause with **zero custom registry content**. Practical
call for a player-cast spell:

```java
DamageSource spellDamage = world.getDamageSources().indirectMagic(casterPlayer, casterPlayer);
target.damage(world, spellDamage, damageAmount);
```

Use plain `.magic()` instead if a spell shouldn't give the caster kill-credit/aggro (rare for a
player ability). A custom `DamageType` is only worth adding later if a *specific skill needs its
own unique death message or resistance/scaling tag* — `DamageType` is a fully data-driven
registry now (`DamageTypes.bootstrap(Registerable<DamageType>)`, confirmed present), so that
would mean a datapack JSON under `data/baum2/damage_type/<name>.json` plus a `RegistryKey`
constant, not a hardcoded enum entry — not needed for the initial 8 spells.

### 2. AoE nearby-entity query — `World.getEntitiesByType`, `getEntitiesByClass` is gone

Confirmed via `javap -p` on `net.minecraft.world.World`: there is **no method named
`getEntitiesByClass` anywhere in this mapping** (a very common shape in older-version
tutorials/training data). The two methods that exist for this purpose:

```java
public List<Entity> getOtherEntities(Entity except, Box box, Predicate<? super Entity> predicate);
public <T extends Entity> List<T> getEntitiesByType(TypeFilter<Entity, T> filter, Box box, Predicate<? super T> predicate);
```

`getOtherEntities` returns plain `Entity` (needs manual `instanceof` filtering) and excludes one
given entity (handy for "exclude the caster" but not type-safe). `getEntitiesByType` is the
direct modern replacement for the old `getEntitiesByClass(Class<T>, ...)` shape — generic,
returns `List<T>` directly. Build the type filter via `net.minecraft.util.TypeFilter` (confirmed
present, functional-shaped):

```java
public static <B, T extends B> TypeFilter<B, T> instanceOf(Class<T> klass);
```

`net.minecraft.util.math.Box` has a convenience centered-box factory (confirmed via `javap`):

```java
public static Box of(Vec3d center, double xSize, double ySize, double zSize); // full side lengths, not radius/half-extent
```

Full AoE query for a spell, e.g. "all living entities within `radius` blocks of `centerPos`,
excluding the caster":

```java
Box aoeBox = Box.of(centerPos, radius * 2, radius * 2, radius * 2);
List<LivingEntity> targets = world.getEntitiesByType(
    TypeFilter.instanceOf(LivingEntity.class),
    aoeBox,
    e -> e != casterPlayer && e.isAlive()
);
```

(`getEntitiesByType` does its own box-intersection test internally — the predicate is an
*additional* filter on top of the box, not a replacement for it, same as the old
`getEntitiesByClass` behaved.)

### 3. Knockback/velocity — the real "silent no-op" gotcha here

This is the one place in this batch with a genuine DrawContext-alpha-style trap, confirmed by
decompiling the actual tracker logic (`EntityTrackerEntry.tick()`), not just reading signatures.

**Fact 1 — `Entity.setVelocity(Vec3d)` does NOT mark anything dirty.** Decompiled:

```java
public void setVelocity(Vec3d velocity) {
    if (velocity.isFinite()) { this.velocity = velocity; }
}
```

No `velocityDirty` write at all. Compare `addVelocity`:

```java
public void addVelocity(double deltaX, double deltaY, double deltaZ) {
    if (Double.isFinite(deltaX) && Double.isFinite(deltaY) && Double.isFinite(deltaZ)) {
        this.setVelocity(this.getVelocity().add(deltaX, deltaY, deltaZ));
        this.velocityDirty = true;   // <- only addVelocity does this, not setVelocity
    }
}
```

`velocityDirty` is a **public** field (`public boolean velocityDirty` on `Entity`), so it's
directly settable by mod code with no reflection if `setVelocity(...)` is used directly for some
reason — but the simplest fix is to just call `addVelocity(...)` instead of `setVelocity(...)`
for anything that's meant to be visible to observers, since it self-flags.

**Fact 2 — even a correctly-dirtied push does not reach the pushed player's own client if the
target is a player.** This is the subtle part. Decompiled `EntityTrackerEntry.tick()`:

```java
if (this.entity.velocityDirty || this.alwaysUpdateVelocity || (entity is LivingEntity && isGliding())) {
    ...
    this.packetSender.sendToListeners(new EntityVelocityUpdateS2CPacket(this.entity.getId(), this.velocity));
    ...
    this.entity.velocityDirty = false;
}
...
if (this.entity.knockedBack) {
    this.entity.knockedBack = false;
    this.packetSender.sendToSelfAndListeners(new EntityVelocityUpdateS2CPacket(this.entity));
}
```

`TrackerPacketSender` (confirmed inner interface of `EntityTrackerEntry`) has **two distinct
methods**: `sendToListeners(...)` (other players who can see this entity — i.e. *observers*,
NOT the entity's own controlling client) and `sendToSelfAndListeners(...)` (observers **plus**
the entity's own client, if it's a `ServerPlayerEntity`). The `velocityDirty` path only uses
`sendToListeners` — so **other nearby players will see a pushed player fly backward, but the
pushed player's own client never gets told**, since their own client is authoritative over its
own movement/physics and needs an explicit push to incorporate a server-side velocity change.
The `sendToSelfAndListeners` path is gated behind a *different*, `protected` field/method
(`entity.knockedBack`, set via `protected void scheduleVelocityUpdate()`) that mod code outside
`net.minecraft.entity` **cannot call directly** (no Mixin used in this codebase currently). This
is exactly the combo vanilla combat itself uses — decompiled `LivingEntity.damage(...)` calls
both `this.scheduleVelocityUpdate()` *and* `this.takeKnockback(0.4F, d, e)` together; calling
only the public `takeKnockback(...)` (or `addVelocity`) reproduces only half of that.

**The fix**: after pushing a target that might be a player, manually send the same packet vanilla
uses directly to that player's own connection:

```java
target.takeKnockback(strength, dx, dz);  // sets velocityDirty itself, pushes non-player observers fine
if (target instanceof ServerPlayerEntity targetPlayer) {
    targetPlayer.networkHandler.sendPacket(new EntityVelocityUpdateS2CPacket(targetPlayer));
}
```

`EntityVelocityUpdateS2CPacket(Entity)` has a public constructor (confirmed via `javap`) and
`ServerPlayerEntity.networkHandler` (type `ServerPlayNetworkHandler`) is a public field whose
`sendPacket(Packet<?>)` method is public (inherited from `ServerCommonNetworkHandler`, confirmed
via `javap`) — no Mixin/reflection needed, this is all directly callable from ordinary mod code.
**This self-sync step is only needed for `ServerPlayerEntity` targets** — mobs have no
"controlling client" of their own, so `sendToListeners` alone already reaches every relevant
observer for them; skip the extra packet for non-player targets, it's a no-op waste at best.

**`LivingEntity.takeKnockback(double strength, double x, double z)`** — confirmed signature, and
confirmed it already self-flags (`this.velocityDirty = true;` is the first line of its body) and
respects `EntityAttributes.KNOCKBACK_RESISTANCE` automatically
(`strength *= 1.0 - this.getAttributeValue(EntityAttributes.KNOCKBACK_RESISTANCE)`) — prefer it
over raw `addVelocity` for anything that should feel like "combat knockback" and honor armor/
attributes. **Sign convention** (confirmed from vanilla's own call site inside
`LivingEntity.damage(...)`): `(x, z)` is a vector pointing **from the target toward the
knockback source** — the method pushes the target in the *opposite* direction internally. For an
outward AoE knockback from a spell's effect center:

```java
double dx = centerPos.getX() - target.getX();
double dz = centerPos.getZ() - target.getZ();
target.takeKnockback(strength, dx, dz); // pushes target away from centerPos; magnitude of (dx,dz) doesn't matter, only direction — takeKnockback normalizes internally
```

### 4. Status effects — `StatusEffectInstance` + `LivingEntity.addStatusEffect`, unchanged shape

Confirmed via `javap -p` on `net.minecraft.entity.effect.StatusEffectInstance`, several
constructor overloads, the two simplest:

```java
public StatusEffectInstance(RegistryEntry<StatusEffect> type);                              // duration/amplifier default
public StatusEffectInstance(RegistryEntry<StatusEffect> type, int duration);                 // amplifier 0
public StatusEffectInstance(RegistryEntry<StatusEffect> type, int duration, int amplifier);  // the one to use
```

**Duration is in ticks, not seconds** — unchanged from every prior version (20 ticks = 1 real
second at normal tick rate). `amplifier` is 0-based (0 = level I / "no numeral shown", matching
vanilla's own potion effect display convention).

Vanilla effect constants are `RegistryEntry<StatusEffect>` (confirmed via `javap` on
`net.minecraft.entity.effect.StatusEffects` — e.g. `StatusEffects.RESISTANCE`, `.SPEED`,
`.SLOWNESS`, `.LUCK` are all `RegistryEntry<StatusEffect>`, not a raw `StatusEffect`), so they
plug directly into the constructor above with no unwrapping needed.

Applying it: `LivingEntity.addStatusEffect(StatusEffectInstance)` — confirmed present, `final`,
returns `boolean` (whether it was actually applied; can be blocked by
`canHaveStatusEffect(...)`, e.g. Undead mobs rejecting Regeneration in vanilla). Example:

```java
target.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, 20 * 5, 1)); // Resistance II, 5 seconds
```

(There's also a 2-arg overload `addStatusEffect(StatusEffectInstance, Entity source)` if a
death/log message should attribute the effect to the caster — not required for basic use.)

### 5. Healing — `LivingEntity.heal(float)` is unchanged

Confirmed via `javap -p` and a Vineflower decompile of the actual body:

```java
public void heal(float amount) {
    float f = this.getHealth();
    if (f > 0.0F) { this.setHealth(f + amount); }
}
```

Exact same signature and behavior as older Minecraft versions — no change to verify against
here, this one really is as simple as it looks. Note it silently no-ops if the entity is already
at/below 0 health (dead) — fine for spell logic, no extra dead-check needed before calling it.

### 6. Dash/short teleport for a caster — `ServerPlayerEntity.requestTeleport`/`requestTeleportOffset`, confirmed self-syncing

This is the other place with a real, confirmed version-current gotcha, but it resolves in the
project's favor: **for `ServerPlayerEntity` specifically, the safe method already handles
syncing internally — no manual packet needed**, which is *not* true of the equivalent method on
the base `Entity` class (relevant if this ever gets reused for a non-player caster).

Confirmed via `javap -p` on `net.minecraft.entity.Entity` and
`net.minecraft.server.network.ServerPlayerEntity`, both declare these (the player class
overrides them):

```java
// Entity (base) — NOT self-syncing on its own:
public void requestTeleport(double destX, double destY, double destZ);
public void requestTeleportOffset(double offsetX, double offsetY, double offsetZ);

// ServerPlayerEntity — overridden, IS self-syncing:
public void requestTeleport(double destX, double destY, double destZ);          // @Override
public void requestTeleportOffset(double offsetX, double offsetY, double offsetZ); // @Override
```

Decompiled bodies confirm the difference matters. Base `Entity.requestTeleport(...)`:

```java
public void requestTeleport(double destX, double destY, double destZ) {
    if (this.getEntityWorld() instanceof ServerWorld) {
        this.refreshPositionAndAngles(destX, destY, destZ, this.getYaw(), this.getPitch());
        this.teleportPassengers();
    }
}
```

— this only mutates the entity's own internal position fields; it does **not** send any packet
itself. For a non-player entity this is fine because the normal per-tick entity-tracker position
sync (the same system covered in the knockback section above) will pick up the new position and
broadcast it as a move packet on the next tracker tick. But `ServerPlayerEntity` overrides both
methods to route through the network handler instead of calling `super`:

```java
// ServerPlayerEntity.requestTeleport (decompiled)
public void requestTeleport(double destX, double destY, double destZ) {
    this.networkHandler.requestTeleport(
        new EntityPosition(new Vec3d(destX, destY, destZ), Vec3d.ZERO, 0.0F, 0.0F),
        PositionFlag.combine(PositionFlag.DELTA, PositionFlag.ROT)
    );
}

// ServerPlayerEntity.requestTeleportOffset (decompiled) — relative move, exactly a "dash"
public void requestTeleportOffset(double offsetX, double offsetY, double offsetZ) {
    this.networkHandler.requestTeleport(
        new EntityPosition(new Vec3d(offsetX, offsetY, offsetZ), Vec3d.ZERO, 0.0F, 0.0F),
        PositionFlag.VALUES
    );
}
```

And `ServerPlayNetworkHandler.requestTeleport(EntityPosition, Set<PositionFlag>)` (decompiled)
is what actually does the work — **updates the real server-side position immediately AND sends
the client-sync packet, in the same call**:

```java
public void requestTeleport(EntityPosition pos, Set<PositionFlag> flags) {
    this.lastTeleportCheckTicks = this.ticks;
    if (++this.requestedTeleportId == Integer.MAX_VALUE) { this.requestedTeleportId = 0; }
    this.player.setPosition(pos, flags);                 // server-side position updated synchronously, no waiting on client ack
    this.requestedTeleportPos = this.player.getEntityPos();
    this.sendPacket(PlayerPositionLookS2CPacket.of(this.requestedTeleportId, pos, flags)); // client sync, sent here
}
```

So calling `player.requestTeleportOffset(dx, dy, dz)` on a **`ServerPlayerEntity`** is
sufficient by itself — the position is authoritative server-side immediately, and the client is
notified in the same call, no extra packet, no dirty flag, no waiting for a teleport-confirm
round-trip before the server's own state reflects the move. (The client does still send back a
`TeleportConfirmC2SPacket`, but that's purely a display-side "stop resisting the correction"
acknowledgement — it doesn't gate the server-side position, which was already updated.)

**`requestTeleportOffset` uses `PositionFlag.VALUES`** (confirmed via `javap` on
`net.minecraft.network.packet.s2c.play.PositionFlag`: `X, Y, Z, Y_ROT, X_ROT, DELTA_X, DELTA_Y,
DELTA_Z, ROTATE_DELTA` individual flags, plus pre-combined sets `VALUES`, `ROT`, `DELTA`) — i.e.
every component of the given `EntityPosition` is interpreted as a delta relative to the player's
current position/rotation, which is exactly "dash offset" semantics: pass a small vector, not an
absolute destination.

**Computing the forward-dash offset**: `Entity.getRotationVector()` (confirmed via `javap`, a
no-arg overload distinct from `getRotationVector(float, float)`) returns the entity's current
look-direction unit `Vec3d`. Full dash sketch:

```java
Vec3d look = casterPlayer.getRotationVector();
Vec3d offset = new Vec3d(look.x, 0, look.z).normalize().multiply(5.0); // flatten to horizontal-only 5-block dash
casterPlayer.requestTeleportOffset(offset.x, offset.y, offset.z);
```

(Flattening the Y component to 0 before normalizing keeps a dash from launching the player into
the sky/ground while looking up/down — a deliberate design choice, not an API requirement; drop
the flattening if a skill should let players look-angle their dash vertically too, e.g. a
"blink" spell.)

There's also a heavier `teleport(ServerWorld, double, double, double, Set<PositionFlag>, float, float, boolean)` (confirmed present on both `Entity` and overridden on `ServerPlayerEntity`) —
this is the full cross-dimension-capable teleport pipeline (goes through `TeleportTarget`/
`teleportTo(...)`, wakes sleeping players, can reset the camera entity, handles portal-adjacent
bookkeeping). It's also confirmed self-syncing (its `ServerPlayerEntity.teleportTo(...)`
implementation calls the same `networkHandler.requestTeleport(...)` + `syncWithPlayerPosition()`
pair internally), but it's meaningfully heavier machinery than a same-dimension short dash needs
— **`requestTeleportOffset` is the right-sized tool for this specific ability**, reach for the
full `teleport(...)` only if a future skill needs to move a player across dimensions or reset
their camera.

### 7. Per-player-per-ability cooldowns — plain in-memory map is the right call, not Attachment API

This one is a design judgment, not an API-signature question, but it's worth confirming
explicitly since the project already has a documented precedent (the old `PlayerLevelSystem`'s
`HashMap<UUID, PlayerProgressData>`, since migrated to the Attachment API — see "Player data /
attributes" above) and cooldowns look superficially similar (per-player, keyed by UUID).

**Confirmed reasoning for why cooldowns should NOT follow progression data onto the Attachment
API**: the entire point of `AttachmentRegistry.createPersistent(...)` (used for class selection,
attribute points, etc.) is that the value survives relog and server restart, backed by NBT
read/write vanilla already wires up for `AttachmentTarget`s. A cooldown is explicitly the
opposite requirement — it's *supposed* to reset on relog/restart (nobody expects a spell
cooldown to persist through a server reboot), so reaching for the persistent-by-design API and
then working around its persistence would be fighting the tool. A non-persistent attachment
(`AttachmentRegistry.create(id, builder -> {})` with no `.persistent(...)` call) is closer, but
still adds registration ceremony (an `AttachmentType`, a generic value type) for a feature this
project's own `HashMap<UUID, PlayerProgressData>` pattern already proved sufficient for
in-memory per-player state before persistence was ever a requirement.

**Recommended shape**: `Map<UUID, Map<Identifier, Long>>` (or a per-player small class if
preferred), storing the *server tick* of the last cast per (player, ability) pair, compared
against `MinecraftServer.getTicks()` — confirmed present via `javap` on
`net.minecraft.server.MinecraftServer`:

```java
public int getTicks(); // total ticks since server start — monotonic, NOT affected by /time set or sleep-skipped nights
```

`getTicks()` (not `ServerWorld.getTime()`/day-time) is the right clock to compare against
specifically because it can't be manipulated by `/time set` or day-skipping, unlike world time —
a cooldown gate should be immune to those.

```java
private static final Map<UUID, Map<Identifier, Long>> LAST_CAST_TICK = new HashMap<>();

boolean isOnCooldown(ServerPlayerEntity player, Identifier abilityId, int cooldownTicks, MinecraftServer server) {
    long last = LAST_CAST_TICK.getOrDefault(player.getUuid(), Map.of()).getOrDefault(abilityId, Long.MIN_VALUE);
    return server.getTicks() - last < cooldownTicks;
}
```

**One hygiene note, not a correctness issue**: unlike the Attachment-backed data, nothing
automatically cleans up a departed player's entry here — over a very long-running server with
many distinct players this map grows unboundedly (bounded only by total distinct UUIDs ever
seen, not concurrent players). Not a real problem at this project's scale, but if it's ever
worth tidying, clear a player's sub-map on `ServerPlayConnectionEvents.DISCONNECT` (already used
elsewhere in this project, see "Events" section above) — purely optional cleanup, not required
for correctness since a stale entry for an offline player is harmless (just a few bytes sitting
in a map keyed by a UUID nobody will look up again unless that exact player rejoins, at which
point the entry becomes useful again — arguably not even worth clearing since it'd have to be
recreated on their next cast anyway).

## Target nameplate / health-bar HUD — researched 2026-07-04

Researched for a custom "target nameplate + health bar" HUD element (showing whatever
entity the player is currently looking at), drawn via `HudElementRegistry` as a new,
separate element — not replacing/attaching to `VanillaHudElements.BOSS_BAR`. Verified
against the named `minecraft-clientOnly-6dd721cd7d-...-sources.jar`
(`net/minecraft/client/MinecraftClient.java`, `net/minecraft/util/hit/EntityHitResult.java`,
`net/minecraft/client/gui/hud/BossBarHud.java`) and `minecraft-common-6dd721cd7d-...-sources.jar`
(`net/minecraft/entity/LivingEntity.java`, `net/minecraft/entity/data/DataTracker.java`).

- **`MinecraftClient.crosshairTarget` confirmed present, exact field (not a method)**:
  ```java
  public @Nullable HitResult crosshairTarget;
  ```
  Public, nullable, plain field — updated each frame by `MinecraftClient`'s own raycast logic
  (used internally for block/entity interaction, e.g. the same field driving attack/use-item
  handling). Read it directly from a `HudElement`'s `render(...)` — no accessor mixin needed
  since it's already public.
- **`EntityHitResult.getEntity()` confirmed**, exact signature `public Entity getEntity()` —
  trivial getter, backed by a `private final Entity entity` field. Check
  `crosshairTarget instanceof EntityHitResult` and that its type is `HitResult.Type.ENTITY`
  before casting (mirrors the existing vanilla call-site pattern in `MinecraftClient` itself,
  e.g. `switch (this.crosshairTarget.getType()) { case ENTITY -> ((EntityHitResult)
  this.crosshairTarget).getEntity(); ... }`).
- **`LivingEntity.getHealth()`/`getMaxHealth()`/`getName()` are reliably readable
  client-side for *any* nearby tracked entity, not just boss-bar-eligible ones — confirmed,
  no special-casing found.** `LivingEntity.java` registers health as a perfectly ordinary
  `TrackedData`:
  ```java
  private static final TrackedData<Float> HEALTH = DataTracker.registerData(LivingEntity.class, TrackedDataHandlerRegistry.FLOAT);
  ...
  builder.add(HEALTH, 1.0F);              // default value in initDataTracker
  public float getHealth() { return this.dataTracker.get(HEALTH); }
  public void setHealth(float health) { this.dataTracker.set(HEALTH, MathHelper.clamp(health, 0.0F, this.getMaxHealth())); }
  ```
  `DataTracker.java` (the sync engine itself) has **no filtering/"isPrivate" concept at all** —
  every registered `TrackedData` for an entity is synced to every client that has that entity
  loaded/tracked within normal entity-tracking range, unconditionally. Boss bars are a
  completely separate, opt-in mechanism (`BossBarS2CPacket`, driven by `BossBar`/
  `ServerBossBar`, manually sent) — nothing about vanilla's boss-bar system restricts or
  special-cases regular `HEALTH` tracked-data sync. So reading `getHealth()`/`getMaxHealth()`
  (a derived value off the synced `EntityAttributes.MAX_HEALTH` attribute instance, also
  ordinarily synced) or `getName()` (an `Entity`-level field, not even gated by `LivingEntity`)
  on an arbitrary nearby mob/player works exactly the same as it does for the local player —
  no extra sync work needed for a target-nameplate HUD element.
- **Vanilla boss-bar HUD placement, for visual-reference sizing only** (`BossBarHud.render`,
  confirmed exact values, not paraphrased):
  ```java
  int i = context.getScaledWindowWidth();
  int j = 12;                              // <- first boss bar's top Y, confirmed
  for (ClientBossBar clientBossBar : this.bossBars.values()) {
      int k = i / 2 - 91;                  // <- bar X: horizontally centered, bar width 182 (91 = 182/2)
      int l = j;                           // bar's own top Y
      this.renderBossBar(context, k, l, clientBossBar);   // bar height WIDTH=182, HEIGHT=5 (5px tall)
      ...
      int o = l - 9;                       // <- name text drawn 9px ABOVE the bar's top edge
      context.drawTextWithShadow(this.client.textRenderer, text, n, o, Colors.WHITE);
      j += 10 + 9;                         // <- next bar's top Y: +19px per additional bar
      if (j >= context.getScaledWindowHeight() / 3) break;  // stops filling past 1/3 screen height
  }
  ```
  So: bar top starts at **y = 12** from the top of the screen, is **182px wide × 5px tall**,
  horizontally centered (`screenWidth/2 - 91`), with its name label drawn **9px above** the
  bar's own top edge (i.e. the label's baseline sits around y = 3), and each additional
  stacked bar adds **19px** of vertical spacing. A custom target-nameplate element wanting a
  similar "sits naturally near the top-center, doesn't collide with the boss bar" look should
  either anchor below whatever boss bars are currently showing (dynamic, since bar count varies)
  or pick a fixed y in the same rough neighborhood (e.g. `y ≈ 12`–`40` depending on whether boss
  bars are expected to co-exist with it) — there's no vanilla API that reports "current boss bar
  stack height" directly, so if avoiding overlap with a *variable* number of active boss bars
  matters, compute it the same way `BossBarHud` does (`12 + bossBars.size() * 19`, capped at
  `screenHeight/3`) rather than hardcoding a single y.

### "Last entity the local player actually attacked" — `AttackEntityCallback` fires client-side, confirmed (researched 2026-07-04)

Researched for a real bug report: attacking a `minecraft:spider` sometimes shows no
`MobNameplateHud` nameplate. Root cause: `MobNameplateHud.render()` only reads
`MinecraftClient.crosshairTarget` live, once per rendered frame — spiders are fast/erratic
(wall-climbing, jumping), so the crosshair frequently isn't exactly on the spider by the time
a given `render()` call runs, even on a tick where the attack itself landed. Fix direction:
cache "the last entity the local player actually attacked" (with a short expiry, e.g. a few
seconds) as a HUD fallback, driven by an actual attack event instead of re-sampling
`crosshairTarget`. Verified against the decompiled `fabric-events-interaction-v0`
`4.1.1+3b89ecf63e` **client and common** source jars (both pulled in transitively by
`fabric-api-0.141.4+1.21.11`, found at
`.gradle/loom-cache/remapped_mods/remapped/net/fabricmc/fabric-api/fabric-events-interaction-v0-16c8840d-{client,common}/4.1.1+3b89ecf63e/...-sources.jar`) and the named
`minecraft-clientOnly-6dd721cd7d-...-sources.jar` (`ClientPlayerInteractionManager.java`,
`MinecraftClient.java`).

- **Yes — `net.fabricmc.fabric.api.event.player.AttackEntityCallback` fires on the CLIENT
  logical side for the local player's own attack, confirmed by reading the actual call
  site.** It is invoked from **two** separate Mixin call sites, not one:
  - **Client-side**: `net.fabricmc.fabric.mixin.event.interaction.client.MultiPlayerGameModeMixin`
    (`@Mixin(ClientPlayerInteractionManager.class)`), injected into the Yarn-mapped
    `ClientPlayerInteractionManager.attackEntity(PlayerEntity player, Entity target)` method,
    right before the interact packet is sent:
    ```java
    // MultiPlayerGameModeMixin (client jar), @Inject before ClientPlayNetworkHandler.sendPacket(...)
    public void attackEntity(PlayerEntity player, Entity entity, CallbackInfo info) {
        ActionResult result = AttackEntityCallback.EVENT.invoker().interact(player, player.getEntityWorld(), Hand.MAIN_HAND, entity, null);
        if (result != ActionResult.PASS) {
            if (result == ActionResult.SUCCESS) {
                this.connection.sendPacket(PlayerInteractEntityC2SPacket.attack(entity, player.isSneaking()));
            }
            info.cancel();
        }
    }
    ```
    Confirmed real, current Yarn 1.21.11 method it targets by reading
    `ClientPlayerInteractionManager.java` directly:
    ```java
    public void attackEntity(PlayerEntity player, Entity target) {
        this.syncSelectedSlot();
        this.networkHandler.sendPacket(PlayerInteractEntityC2SPacket.attack(target, player.isSneaking()));
        if (this.gameMode != GameMode.SPECTATOR) {
            player.attack(target);
            player.resetTicksSince();
        }
    }
    ```
    And confirmed the only caller of that method, `MinecraftClient.doAttack()` (called once per
    queued left-click from `handleInputEvents()`, i.e. the real "player just attacked" moment,
    not a render-frame sample):
    ```java
    switch (this.crosshairTarget.getType()) {
        case ENTITY:
            AttackRangeComponent attackRangeComponent = (AttackRangeComponent) itemStack.get(DataComponentTypes.ATTACK_RANGE);
            if (attackRangeComponent == null || attackRangeComponent.isWithinRange(this.player, this.crosshairTarget.getPos())) {
                this.interactionManager.attackEntity(this.player, ((EntityHitResult) this.crosshairTarget).getEntity());
            }
            break;
        ...
    }
    ```
    **Important nuance**: this is still gated on `this.crosshairTarget.getType() == ENTITY` at
    the moment `doAttack()` runs — it's the *same* `crosshairTarget` field the HUD already
    reads, not an independent raycast. The reliability win isn't a different targeting method;
    it's that `doAttack()`/`AttackEntityCallback` fires exactly once, exactly at the instant the
    game itself resolved "you just attacked this entity" (once per queued attack-key press,
    each client tick), whereas `MobNameplateHud.render()` re-samples the same field on every
    rendered frame and can catch it mid-flicker right before/after a fast entity moves off
    crosshair. Caching the entity at the moment `AttackEntityCallback` fires (instead of only
    trusting whatever `render()` currently sees) is what actually fixes the "landed the hit but
    no nameplate" symptom.
  - **Server-side**: `net.fabricmc.fabric.mixin.event.interaction.ServerPlayerMixin`
    (`@Mixin(ServerPlayerEntity.class)`), injected at `HEAD` of `ServerPlayerEntity.attack(Entity target)`:
    ```java
    public void onPlayerInteractEntity(Entity target, CallbackInfo info) {
        ServerPlayerEntity player = (ServerPlayerEntity) (Object) this;
        ActionResult result = AttackEntityCallback.EVENT.invoker().interact(player, player.getEntityWorld(), Hand.MAIN_HAND, target, null);
        if (result != ActionResult.PASS) info.cancel();
    }
    ```
  - **Practical consequence — the same static `Event<AttackEntityCallback>` is shared per JVM,
    so in singleplayer (integrated server in the same process) a listener registered once fires
    TWICE per attack**: once from the client-side mixin (`world.isClient()` true, fires first,
    before the packet is even sent) and once from the server-side mixin when the integrated
    server processes that packet (`world.isClient()` false). On a real dedicated-server +
    remote-client setup this doesn't happen (the server-side mixin only runs in the server's own
    JVM, which a remote client never shares) — but code should not assume single-fire. **Guard
    with `world.isClient()`** and only react to the client-side firing for a client-only HUD
    cache (it's also the earliest, most input-latency-free of the two).
- **No Mixin needed — `AttackEntityCallback` is the correct, sufficient, already-available
  hook.** Since it's already invoked from the real client-side attack path
  (`ClientPlayerInteractionManager.attackEntity`), there's no need for a custom Mixin into
  `ClientPlayerInteractionManager`/`MinecraftClient.doAttack()` just to observe "the local
  player attacked entity X" — register a listener on the existing Fabric API event instead.
- **Exact functional interface signature, confirmed** (`net.fabricmc.fabric.api.event.player.AttackEntityCallback`, module `fabric-events-interaction-v0`, unchanged from the "Combat / Damage" section's earlier read of the same file):
  ```java
  public interface AttackEntityCallback {
      Event<AttackEntityCallback> EVENT = EventFactory.createArrayBacked(...);
      ActionResult interact(PlayerEntity player, World world, Hand hand, Entity entity, @Nullable EntityHitResult hitResult);
  }
  ```
  Returning anything other than `ActionResult.PASS` **cancels the attack** (client: suppresses
  the packet send + local `player.attack(target)` call; server: cancels `ServerPlayerEntity.attack`
  processing) — a passive "just observe and cache the target" listener **must always return
  `ActionResult.PASS`**, never `SUCCESS`/`FAIL`, or real combat breaks.
- **`hitResult` is always passed as `null` at both current call sites** — confirmed, both
  `MultiPlayerGameModeMixin` and `ServerPlayerMixin` call `.interact(player, world, hand, target, null)`
  literally. Don't rely on a non-null `hitResult` from this event in practice, even though the
  parameter exists on the interface.
- **Recommended registration** (client-only code, e.g. `Baum2Client.onInitializeClient()`,
  registering the common-module event from client code is fine — `AttackEntityCallback` lives
  in `fabric-events-interaction-v0`'s common package and is usable from either environment):
  ```java
  AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
      if (world.isClient() && player == MinecraftClient.getInstance().player && entity instanceof LivingEntity living) {
          MobNameplateHud.setLastAttacked(living); // cache with a short expiry (e.g. a few seconds)
      }
      return ActionResult.PASS;
  });
  ```
  `player == MinecraftClient.getInstance().player` is technically redundant given the
  `ClientPlayerInteractionManager` call site can only ever be the physical client's own player,
  but cheap and self-documenting against ever accidentally registering this in a
  non-client-only file later.

## Custom `Block`s and `BlockEntity`s — researched 2026-07-05

Researched ahead of this project's first custom `Block` (a stationary "world event" mini-boss
block, working name "Rissobelisk": high-HP, damaged by the player left-clicking/attacking it
directly rather than through vanilla mining, spawns mob waves at HP thresholds — that part is a
port of existing entity logic, not researched here — and is destroyed/looted by our own code
once HP hits 0). **No `Block` or `BlockEntity` existed anywhere in this codebase before this** —
every prior piece of custom content was an `Entity` or `Item`. Verified against a freshly-run
`gradlew.bat genSources` (no decompiled sources existed in this checkout beforehand; generated
`minecraft-common-6dd721cd7d-...-sources.jar` and `minecraft-clientOnly-6dd721cd7d-...-sources.jar`
for Yarn `1.21.11+build.6`), plus the exact Fabric API submodule versions **confirmed bundled
inside this project's pinned `fabric-api-0.141.4+1.21.11.jar`** by listing its own
`META-INF/jars/` directory directly (the reliable way to know which submodule build a pinned
Fabric API version actually uses, rather than assuming Maven's latest): `fabric-events-
interaction-v0-4.1.1+3b89ecf63e.jar`, `fabric-object-builder-api-v1-21.1.40+4fc5413f3e.jar` —
cross-checked against this project's own per-checkout Yarn-remapped copies of those same jars
under `.gradle/loom-cache/remapped_mods/remapped/net/fabricmc/fabric-api/...` (hash prefix
`16c8840d`, this checkout's specific Minecraft/Yarn combination).

### Q1 — `AttackBlockCallback`: exact shape, and does it really pre-empt vanilla mining

**Confirmed to exist, exact package/shape** —
`net.fabricmc.fabric.api.event.player.AttackBlockCallback` (`fabric-events-interaction-v0`, the
same submodule/package that already hosts this doc's existing `AttackEntityCallback` entry
above):
```java
public interface AttackBlockCallback {
    Event<AttackBlockCallback> EVENT = ...;
    ActionResult interact(PlayerEntity player, World world, Hand hand, BlockPos pos, Direction direction);
}
```
- Per its own javadoc, confirmed: *"Is hooked in before the spectator check, so make sure to
  check for the player's game mode as well!"* On the client, `SUCCESS`/`CONSUME`/`FAIL`/`PASS`
  have distinct hand-swing/packet-send nuances (see the interface's full javadoc); **on the
  server, any return value other than `PASS` cancels further processing** — i.e. `PASS` falls
  through to vanilla, anything else means vanilla mining never starts at all.
- **Confirmed exactly where/how it pre-empts vanilla mining**, by reading the actual mixins that
  fire it (`net.fabricmc.fabric.mixin.event.interaction.ServerPlayerGameModeMixin`, targeting
  `ServerPlayerInteractionManager`; client counterpart `...interaction.client
  .MultiPlayerGameModeMixin`, targeting `ClientPlayerInteractionManager`):
  - **Server side**: `@Inject(at = @At("HEAD"), method = "handleBlockBreakAction", cancellable =
    true)` fires `AttackBlockCallback.EVENT` only for `PlayerActionC2SPacket.Action
    .START_DESTROY_BLOCK`, and if the result isn't `PASS`, calls `info.cancel()` —
    **aborting the entire handler method before any of its own logic runs**, including the
    creative-mode instant-destroy branch (see Q2).
  - **Client side**: injected immediately before the very first `getAbilities()` call inside the
    method that starts a left-click-on-block (i.e. before either the creative-mode branch or
    survival mining-start runs), and again, creative-mode-only, inside the per-tick "continue
    breaking" method.
  - **Real, verified method-name drift, worth flagging generally for this Fabric API
    version**: the actual Yarn `1.21.11+build.6` names (confirmed via the fresh `genSources`
    output) are **`ServerPlayerInteractionManager.processBlockBreakingAction(...)`** and
    **`ClientPlayerInteractionManager.attackBlock(...)`** / `.updateBlockBreakingProgress(...)`
    — not `handleBlockBreakAction`/`startDestroyBlock`/`continueDestroyBlock`, which is what
    Fabric API's own compiled mixin (and its bundled sources jar) still calls them. This is a
    benign Yarn-rename-vs-Mixin-target mismatch — Mixin resolves `@Inject(method = "...")`
    targets via an intermediary-based refmap recorded at Fabric API's own build time, so it
    still binds correctly at runtime regardless of the later Yarn rename; **not a bug**, but it
    means grepping the decompiled Minecraft source for the literal name a Fabric API mixin file
    uses will not find it — grep for the behavior instead (the `START_DESTROY_BLOCK` handling /
    the method that calls `getAbilities().creativeMode` first).
- **Fires exactly once per discrete left-click, not once per tick held down** — confirmed by
  tracing the full client call chain: `MinecraftClient`'s main loop drives `doAttack()` (which
  calls `interactionManager.attackBlock(...)`, our hook) from `while (this.options.attackKey
  .wasPressed())` — the same edge-triggered, once-per-queued-press loop this doc already
  documents for keybind polling (`KeyBinding.wasPressed()`) — and **entity melee attacks go
  through the identical loop** (`case ENTITY: ... interactionManager.attackEntity(...)` sits
  right next to the `case BLOCK` branch that calls `attackBlock`, both inside the same
  `wasPressed()` loop). Continuously holding the mouse button down does **not** repeatedly
  re-fire `AttackBlockCallback` for a block whose break we've cancelled — the separate per-tick
  `updateBlockBreakingProgress` path only continues progress on a block that already has
  `breakingBlock = true` internally, which our cancellation prevents from ever being set. This
  confirms the "attack the block like a mob" analogy is structurally accurate (same input loop,
  same one-hit-per-click semantics as melee), not just a superficial similarity.
- **Recommended listener shape** for the boss-block use case (health decrement server-side,
  still lets the client swing its hand for feedback):
  ```java
  AttackBlockCallback.EVENT.register((player, world, hand, pos, direction) -> {
      if (!(world.getBlockState(pos).getBlock() instanceof RissobeliskBlock)) return ActionResult.PASS;
      if (world instanceof ServerWorld serverWorld) {
          // health/threshold logic here, e.g. via (RissobeliskBlockEntity) serverWorld.getBlockEntity(pos)
      }
      return ActionResult.SUCCESS; // unconditionally non-PASS once it's confirmed to be our block —
                                    // this is also what fully blocks vanilla mining/creative insta-break, see Q2
  });
  ```

### Q2 — Making a placed block unbreakable by vanilla means, but destroyable by our own code

- **The builder class is `AbstractBlock.Settings`, not `Block.Settings`.** Confirmed by reading
  `Block.java` directly: there is no nested `Block.Settings` class in 1.21.11 at all; `Block`'s
  own constructor is `public Block(AbstractBlock.Settings settings)`, and the settings builder
  lives on the shared superclass — exactly parallel to `Item.Settings` for items (this project's
  own prior finding). Don't assume a `Block.Settings` alias exists just because `Item.Settings`
  does.
- **Exact hardness/resistance builder methods, confirmed** (`AbstractBlock.Settings`, all
  fluent):
  ```java
  public AbstractBlock.Settings hardness(float hardness);
  public AbstractBlock.Settings resistance(float resistance);       // clamped to Math.max(0.0F, resistance)
  public AbstractBlock.Settings strength(float hardness, float resistance); // = hardness(h).resistance(r)
  public AbstractBlock.Settings strength(float strength);            // both set to the same value
  public AbstractBlock.Settings breakInstantly();                    // = strength(0.0F)
  public AbstractBlock.Settings dropsNothing();                      // suppresses the default loot-table drop
  public AbstractBlock.Settings registryKey(RegistryKey<Block> key); // MUST be set before construction, same rule as Item.Settings
  ```
  Vanilla's own bedrock definition (`Blocks.java`), confirmed exact:
  ```java
  Blocks.BEDROCK = register("bedrock", AbstractBlock.Settings.create()
      .mapColor(MapColor.STONE_GRAY).instrument(NoteBlockInstrument.BASEDRUM)
      .strength(-1.0F, 3600000.0F).dropsNothing().allowsSpawning(Blocks::never));
  ```
- **`hardness == -1.0F` fully blocks *survival* mining — confirmed at the exact mechanism**,
  `AbstractBlock.calcBlockBreakingDelta(...)`:
  ```java
  protected float calcBlockBreakingDelta(BlockState state, PlayerEntity player, BlockView world, BlockPos pos) {
      float f = state.getHardness(world, pos);
      if (f == -1.0F) return 0.0F;
      ...
  }
  ```
  Mining progress is driven entirely by this delta (the server accumulates
  `calcBlockBreakingDelta(...) * ticksHeld` and only actually breaks the block once that reaches
  a threshold) — hardcoding `0.0F` means **survival mining progress can never advance, for any
  tool, ever.**
- **`resistance` is the independent axis for explosion survival** (`AbstractBlock.resistance`,
  read via `Block.getBlastResistance()`), confirmed unrelated to the hardness/mining-delta check
  above — `3_600_000.0F` (bedrock's own constant) is high enough that no vanilla explosion (even
  the Wither or an end crystal) has any realistic chance of destroying it.
- **Fire isn't a separate thing to defend against**: a block only burns/is destroyed by fire if
  its `Settings` opts in via `.burnable()`. Simply never calling `.burnable()` already makes the
  block fireproof by default — no negative flag needed.
- **Important, directly-confirmed caveat — `hardness = -1.0F` alone does NOT block
  creative-mode instant-break.** Reading `ServerPlayerInteractionManager
  .processBlockBreakingAction(...)` directly:
  ```java
  if (action == PlayerActionC2SPacket.Action.START_DESTROY_BLOCK) {
      ...
      if (this.player.getAbilities().creativeMode) {
          this.finishMining(pos, sequence, "creative destroy");   // unconditional, no hardness check at all
          return;
      }
      ... // only survival reaches calcBlockBreakingDelta() / the -1.0F check above
  }
  ```
  `finishMining` → `tryBreakBlock(pos)` never reads hardness either (only `ItemStack.canMine`,
  an `OperatorBlock` check, and `isBlockBreakingRestricted`). **This is exactly how vanilla
  bedrock itself actually behaves** — a creative-mode player really can mine bedrock by
  left-clicking it (well-known, intentional vanilla behavior) — so `-1.0F` hardness is a
  *survival-mining-only* guarantee, not a universal one.
  - **The fix is the same `AttackBlockCallback` hook from Q1, and it fully closes this gap.**
    Since the server-side event fires at the very `HEAD` of `processBlockBreakingAction` —
    *before* the `creativeMode` branch is even reached — a listener returning non-`PASS` for our
    block cancels the entire method, meaning **the creative-instant-break branch never runs
    either.** One hook (already needed for the melee-style damage mechanic in Q1) therefore also
    fully solves "unbreakable by any player, in any game mode, with any tool" — no separate
    Mixin is needed just for that.
  - **Net recommended recipe**: `.strength(-1.0F, 3_600_000.0F).dropsNothing()` (belt — survival
    mining and explosion immunity, holds even if the listener below were ever missing/
    misregistered) **plus** the `AttackBlockCallback` listener from Q1 always returning
    non-`PASS` for this block (suspenders — closes the creative-mode gap, and is the same hook
    the health-damage feature needs anyway).
- **Our own code destroying it is completely unaffected by any of the above** — confirmed:
  `World.removeBlock(BlockPos pos, boolean move)` and `World.breakBlock(BlockPos pos, boolean
  drop, @Nullable Entity breakingEntity, int maxUpdateDepth)` (`net.minecraft.world.World`)
  operate directly on world/chunk state and never consult `calcBlockBreakingDelta`/hardness/
  `AttackBlockCallback` at all — those only exist in the *player-interaction* code paths
  (`ServerPlayerInteractionManager`/`ClientPlayerInteractionManager`), not in `World` itself.
  Calling `world.removeBlock(pos, false)` from our own server-side code (e.g. once tracked HP
  hits 0) works regardless of how unbreakable the block is made against players.

### Q3 — Do we need a `BlockEntity`, and is the Data Attachment API the right tool for it

- **Yes, a `BlockEntity` is needed.** A plain `Block` has no per-instance/per-position mutable
  state at all — it's a single shared object per registered block *type*; per-position data
  either lives in `BlockState` properties (small, enumerable, essentially-immutable-per-
  placement values — not a fit for a changing `int` health counter) or in a `BlockEntity`. An
  `int` health, a wave counter, and a set of spawned-mob UUIDs all need the latter.
- **`BlockEntity implements AttachmentTarget` is now directly visible in this project's own
  decompiled/compiled Minecraft classes, not just documented as a general Fabric fact.** The
  fresh `genSources` output for `net/minecraft/block/entity/BlockEntity.java` literally shows:
  ```java
  /**
   * ...
   * <p>Interface {@link net.fabricmc.fabric.api.attachment.v1.AttachmentTarget} injected by mod fabric-data-attachment-api-v1</p>
   */
  public abstract class BlockEntity implements DebugTrackable, RenderDataBlockEntity, AttachmentTarget {
  ```
  This is Fabric Loom's compile-time **interface injection** (declared via Fabric API's own
  `fabric.mod.json`), not just a runtime-only Mixin trick — `BlockEntity` genuinely implements
  the interface at the source level for any code compiled against this project's mapped jars, no
  cast needed.
  - **Correction to this project's own previously-recorded phrasing** (see the "Player data /
    attributes" section above, which said "`Entity`, `BlockEntity`, `ServerLevel`,
    `ChunkAccess`"): `AttachmentTarget`'s own class javadoc (`fabric-data-attachment-api-v1`
    `1.8.48+eed0806f3e`, bundled in `fabric-api-0.141.4+1.21.11`) actually says *"Fabric
    implements this on `Entity`, `BlockEntity`, `ServerWorld` and `Chunk` via mixin"* — those
    last two are **Yarn** names (`ServerWorld`, `Chunk`), not the Mojang-mapped names
    (`ServerLevel`, `ChunkAccess`) the earlier entry used. Same targets, just the Yarn spelling
    to actually grep for in this codebase's own mapped sources.
- **Whether to actually *use* the Attachment API for a `BlockEntity` we're authoring ourselves
  (vs. plain fields + an NBT-read/write override) is a judgment call — plain fields are simpler
  here, and are the recommendation for Rissobelisk.** The Attachment API's main value is
  retrofitting persistence onto a type this project doesn't own the source of (e.g.
  `ServerPlayerEntity`, as the Progression/Class systems already do). For a **brand-new custom
  `BlockEntity` subclass being written from scratch anyway**, plain fields
  (`private int health;`, `private final Set<UUID> spawnedMobs = new HashSet<>();`) plus the
  block entity's own NBT override points (below) need no `AttachmentType` registration step, and
  have no persistence-safety disadvantage: the Attachment API's documented gotcha that
  **mutating attached data in place requires manually calling `markDirty()`** (`setAttached(...)`
  does this for you; direct field mutation does not) means a plain field also just needs a
  `markDirty()` call after mutation — identical ceremony either way. Reach for the Attachment
  API on a `BlockEntity` instead only if the data needs to be read by unrelated mod code that
  doesn't hold a reference to the concrete `BlockEntity` subclass, or needs independent
  `.syncWith(...)`-style client sync — neither applies to Rissobelisk's self-contained state.
- **Confirmed exact 1.21.11 NBT-equivalent override points on `BlockEntity` — a real,
  version-specific rename worth flagging**: it is **`protected void readData(ReadView view)`**
  / **`protected void writeData(WriteView view)`**, not `readNbt(NbtCompound, RegistryWrapper
  .WrapperLookup)`/`writeNbt(...)` like older-version tutorials show (the class's own javadoc
  prose still *refers* to them by the old `readNbt`/`writeNbt` names, but the actual overridable
  signatures use the newer `ReadView`/`WriteView` types). Minimal usage:
  ```java
  @Override protected void readData(ReadView view) {
      this.health = view.getInt("health", this.maxHealth);
  }
  @Override protected void writeData(WriteView view) {
      view.putInt("health", this.health);
  }
  ```
  (`ReadView.getInt(String key, int fallback)` / `WriteView.putInt(String key, int value)`
  confirmed exact, `net.minecraft.storage.{ReadView,WriteView}`.)
- **`BlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state)`** is the confirmed
  constructor — matches `BlockEntityProvider.createBlockEntity(BlockPos pos, BlockState state)`'s
  own two params (the type isn't passed by the factory interface; the subclass supplies it itself
  via `super(MY_BE_TYPE, pos, state)`).
- **Simplest way to get the `BlockEntity` hook at all: implement `BlockEntityProvider` directly
  on the plain `Block` subclass — don't extend `BlockWithEntity`.** Confirmed via
  `BlockEntityProvider`'s own javadoc: *"blocks with block entity only have to implement
  `BlockEntityProvider` and do not have to subclass [`BlockWithEntity`]... it is generally
  easier to just implement `BlockEntityProvider`."* Concretely: `BlockWithEntity` re-declares
  `getCodec()` as **abstract**, forcing every subclass to hand-author a `MapCodec`, whereas a
  plain `Block` subclass already gets a working default `getCodec()` (returns `Block.CODEC`,
  built from `Block::new`) for free:
  ```java
  public class RissobeliskBlock extends Block implements BlockEntityProvider {
      public RissobeliskBlock(AbstractBlock.Settings settings) { super(settings); }
      @Override public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
          return new RissobeliskBlockEntity(pos, state);
      }
  }
  ```
- **`FabricBlockEntityTypeBuilder` — confirmed exact and current (not deprecated, unlike
  `FabricEntityTypeBuilder` — see the Registries section above)**
  (`net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder`,
  `fabric-object-builder-api-v1` `21.1.40+4fc5413f3e`):
  ```java
  public static <T extends BlockEntity> FabricBlockEntityTypeBuilder<T> create(Factory<? extends T> factory, Block... blocks);
  // FabricBlockEntityTypeBuilder.Factory<T>: T create(BlockPos blockPos, BlockState blockState);
  public FabricBlockEntityTypeBuilder<T> addBlock(Block block);   // additional supported blocks, chainable
  public BlockEntityType<T> build();                              // no registry key needed, unlike EntityType.Builder
  ```
  `BlockEntityType` registration is simpler than `EntityType`'s (no `.build(registryKey)` dance)
  — confirmed via vanilla's own private registration helper in `BlockEntityType.java`:
  `Registry.register(Registries.BLOCK_ENTITY_TYPE, id, new BlockEntityType<>(...))`, i.e. a
  plain `Identifier`/`String`-keyed `Registry.register(...)` overload is enough.
- **Full minimal registration shape**, combining all of the above:
  ```java
  // registry/ModBlocks.java
  public final class ModBlocks {
      public static final RegistryKey<Block> RISSOBELISK_KEY =
          RegistryKey.of(RegistryKeys.BLOCK, Identifier.of("baum2", "rissobelisk"));
      public static final Block RISSOBELISK = Registry.register(Registries.BLOCK, RISSOBELISK_KEY,
          new RissobeliskBlock(AbstractBlock.Settings.create()
              .registryKey(RISSOBELISK_KEY)
              .strength(-1.0F, 3_600_000.0F)
              .dropsNothing()));
  }

  // registry/ModBlockEntities.java
  public final class ModBlockEntities {
      public static final BlockEntityType<RissobeliskBlockEntity> RISSOBELISK_BE = Registry.register(
          Registries.BLOCK_ENTITY_TYPE, Identifier.of("baum2", "rissobelisk"),
          FabricBlockEntityTypeBuilder.create(RissobeliskBlockEntity::new, ModBlocks.RISSOBELISK).build());
  }
  ```

### Q4 — Pairing a `BlockItem` for survival/creative-tab placement

- **`BlockItem` is still the correct, unchanged class**: `net.minecraft.item.BlockItem extends
  Item`, constructor confirmed exact: `public BlockItem(Block block, Item.Settings settings)`.
- **Yes, `Item.Settings.registryKey(...)` is required, the same way this project's existing
  items already need it** — confirmed by reading vanilla's own block-to-item registration helper
  (`Items.java`):
  ```java
  public static Item register(Block block, BiFunction<Block, Item.Settings, Item> factory, Item.Settings settings) {
      return register(keyOf(block.getRegistryEntry().registryKey()), // item key = same path as the block's own key
          itemSettings -> factory.apply(block, itemSettings),
          settings.useBlockPrefixedTranslationKey());
  }
  // ... register(RegistryKey<Item> key, ...) internally does:
  Item item = factory.apply(settings.registryKey(key));   // registryKey() set BEFORE construction — same rule as Item
  if (item instanceof BlockItem blockItem) blockItem.appendBlocks(Item.BLOCK_ITEMS, item); // wires up Block.asItem()
  ```
  Practical registration, mirroring this project's own established "`.registryKey(key)` before
  constructing" rule:
  ```java
  RegistryKey<Item> RISSOBELISK_ITEM_KEY = RegistryKey.of(RegistryKeys.ITEM, Identifier.of("baum2", "rissobelisk"));
  public static final Item RISSOBELISK_ITEM = Registry.register(Registries.ITEM, RISSOBELISK_ITEM_KEY,
      new BlockItem(ModBlocks.RISSOBELISK, new Item.Settings().registryKey(RISSOBELISK_ITEM_KEY)));
  ```
  Skipping the `BlockItem` registration entirely is also legitimate for a boss-only "world
  event" block never meant to be manually placed by a player (spawned via command/structure/
  worldgen instead) — nothing in the Block/BlockEntity registration above depends on a paired
  `BlockItem` existing.

### Q5 — Dropping a custom `ItemStack` on our own forced destruction

- **`Block.dropStack(World world, BlockPos pos, ItemStack stack)` confirmed exact, public
  static**, in `net.minecraft.block.Block` (mirrors the entity-side `Entity.dropStack
  (ServerWorld, ItemStack)` this project's `StoneOfSpidersEntity.dropLoot` already uses, with
  two small differences: it takes `World`, not `ServerWorld`, and returns `void`, not
  `@Nullable ItemEntity`):
  ```java
  public static void dropStack(World world, BlockPos pos, ItemStack stack) {
      // spawns an ItemEntity centered on pos (± small random offset), gated internally by
      // the DO_TILE_DROPS game rule and stack.isEmpty()
  }
  ```
  A `dropStack(World, BlockPos, Direction, ItemStack)` overload also exists (offsets the drop
  toward a face) — not needed for a plain "drop at the block's own position" case.
- **Recommended forced-destruction shape**, combining Q2's `World.removeBlock` finding with
  this:
  ```java
  // once tracked health <= 0, server-side:
  world.removeBlock(pos, false);                              // silent removal — no vanilla drop/particle/loot-table logic
  Block.dropStack(world, pos, new ItemStack(ModItems.RISSSPLITTER));
  // grant XP / play custom death effects here too, same pattern as StoneOfSpidersEntity.onDeath
  ```
  `World.removeBlock(pos, false)` intentionally does none of vanilla's own break-effects, unlike
  `World.breakBlock(pos, drop, entity, maxUpdateDepth)` — that method plays the vanilla
  block-break sound/particle (`syncWorldEvent(WorldEvents.BLOCK_BROKEN, ...)`) and, if
  `drop=true`, drops items via the vanilla **loot-table** system, not a guaranteed custom stack.
  For a guaranteed single-item drop, prefer `removeBlock` + an explicit `dropStack` call over
  `breakBlock(..., true, ...)`, mirroring the "skip the loot-table system for a fixed drop"
  recommendation this doc already gives for mob loot (see "Loot / Guaranteed Item Drops" above).

## Custom two-handed held-item rendering (bespoke `FeatureRenderer`, extra scale/position) — researched 2026-07-05

Researched ahead of a new `DrevathisEntity` boss (extends `HostileEntity`, `BipedEntityModel`-
based custom model) whose held mainhand weapon needs to render **independently scaled larger**
than a normal one-handed item (an extra multiplier on top of the boss's own body scale) and
**positioned/rotated as a centered two-handed grip** instead of vanilla's normal per-hand
offset. Verified by decompiling the actual project-local sources jars generated by this
project's own `genSources` run — **not** the machine-wide `~/.gradle/caches/fabric-loom`
cache (that one only holds the pre-decompile remapped jars for this project; the actual
decompiled `-sources.jar`s live under the **project-local** `.gradle/loom-cache/minecraftMaven/
net/minecraft/minecraft-clientOnly-6dd721cd7d/1.21.11-net.fabricmc.yarn.1_21_11.1.21.11+
build.6-v2/...-sources.jar`, i.e. inside the repo's own `.gradle/` directory, not the user
profile's global Gradle cache — worth remembering for future research sessions on this
project specifically, in case the global-cache path search comes up empty again).
Files actually decompiled and read: `net/minecraft/client/render/entity/feature/
{FeatureRenderer,HeldItemFeatureRenderer}.java`, `net/minecraft/client/render/item/
{ItemRenderer,ItemRenderState}.java`, `net/minecraft/client/render/entity/EntityRendererFactory.java`,
`net/minecraft/client/render/entity/LivingEntityRenderer.java`, `net/minecraft/client/render/entity/
model/{BipedEntityModel,ModelWithArms}.java`, `net/minecraft/client/render/entity/state/
ArmedEntityRenderState.java`, `net/minecraft/client/item/ItemModelManager.java`,
`net/minecraft/client/model/ModelPart.java`, `net/minecraft/item/ItemDisplayContext.java`.

- **Major rendering-pipeline rework versus older Yarn/tutorial-era assumptions — the whole
  "call `ItemRenderer.renderItem(...)` with a `VertexConsumerProvider`" pattern that most
  online tutorials (and stale training data) describe no longer matches this version's actual
  plumbing.** Two separate reworks stacked together, both confirmed by direct decompile:
  1. **`VertexConsumerProvider` is gone from the per-frame render call chain** — replaced by
     `net.minecraft.client.render.command.OrderedRenderCommandQueue` (a batching/ordering queue
     you submit draw commands into, not a buffer you write vertices to directly).
  2. **Item rendering itself no longer goes through a raw `BakedModel`/`ItemRenderer.renderItem`
     call at the point of use.** There's now an intermediate object, `ItemRenderState`
     (`net.minecraft.client.render.item.ItemRenderState`), which is *populated once* (via
     `ItemModelManager.updateForLivingEntity(...)`, see below) and then *rendered* separately
     (via its own `.render(...)` instance method) — the actual per-quad tint/glint/transform
     logic already baked into `ItemRenderState.LayerRenderState` at populate-time, not at
     render-time.
- **(1) `FeatureRenderer<S extends EntityRenderState, M extends EntityModel<? super S>>` —
  exact confirmed shape** (`net/minecraft/client/render/entity/feature/FeatureRenderer.java`):
  ```java
  public abstract class FeatureRenderer<S extends EntityRenderState, M extends EntityModel<? super S>> {
      public FeatureRenderer(FeatureRendererContext<S, M> context) { ... }
      public M getContextModel() { return this.context.getModel(); }
      public abstract void render(
          MatrixStack matrices, OrderedRenderCommandQueue queue, int light, S state,
          float limbAngle, float limbDistance
      );
  }
  ```
  **No `VertexConsumerProvider` parameter at all** — the abstract `render(...)` method takes
  `OrderedRenderCommandQueue queue` instead (5th positional slot in `HeldItemFeatureRenderer`'s
  own override, confirmed same order: `matrices, queue, light, state, limbAngle/limbDistance`
  — note the two trailing `float` params are yaw/pitch-shaped historically but in this class are
  actually just `limbAngle`/`limbDistance` per the parameter names in the decompiled source, not
  camera yaw/pitch). Constructor takes `FeatureRendererContext<S, M> context` (the parent
  `EntityRenderer`/`LivingEntityRenderer` itself — implements this interface — **not** an
  `EntityRendererFactory.Context`; a `FeatureRenderer` subclass has no direct access to
  `EntityRendererFactory.Context`, only to the parent renderer via `getContextModel()`). Also
  worth noting: `FeatureRenderer` has protected static helpers `render(...)`/`renderModel(...)`
  for rendering an entire `Model<? super S>` (used for things like the whole-second-model
  overlays), which internally call `queue.getBatchingQueue(queueOrder).submitModel(...)` — not
  relevant for single-item rendering, just confirming the general "submit into the queue,
  don't draw directly" pattern is pervasive in this version.
- **(2) `HeldItemFeatureRenderer<S extends ArmedEntityRenderState, M extends EntityModel<S> &
  ModelWithArms>` — full confirmed internals**
  (`net/minecraft/client/render/entity/feature/HeldItemFeatureRenderer.java`):
  ```java
  public void render(MatrixStack matrixStack, OrderedRenderCommandQueue queue, int light, S state, float f, float g) {
      this.renderItem(state, state.rightHandItemState, state.rightHandItem, Arm.RIGHT, matrixStack, queue, light);
      this.renderItem(state, state.leftHandItemState, state.leftHandItem, Arm.LEFT, matrixStack, queue, light);
  }

  protected void renderItem(S entityState, ItemRenderState itemState, ItemStack stack, Arm arm,
                             MatrixStack matrices, OrderedRenderCommandQueue queue, int light) {
      if (!itemState.isEmpty()) {
          matrices.push();
          this.getContextModel().setArmAngle(entityState, arm, matrices);   // <- per-arm transform, see (4) below
          matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-90.0F));
          matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180.0F));
          boolean isLeft = arm == Arm.LEFT;
          matrices.translate((isLeft ? -1 : 1) / 16.0F, 0.125F, -0.625F);
          // ...swing/use-item pose adjustments omitted (Lancing spear-pose, use-item arm pose)...
          itemState.render(matrices, queue, light, OverlayTexture.DEFAULT_UV, entityState.outlineColor);
          matrices.pop();
      }
  }
  ```
  **It does NOT call `ItemRenderer.renderItem(...)` at all.** It reads the *already-populated*
  `ItemRenderState` straight off the render state (`state.rightHandItemState`/
  `state.leftHandItemState` — plain fields on `ArmedEntityRenderState`, not looked up via any
  entity/world reference at render time), applies a fixed per-arm matrix dance (arm's own
  transform via `setArmAngle`, then a `-90°` X rotation + `180°` Y rotation to reorient the
  item from "held flat" to "held upright", then a small per-hand translate), and finally calls
  `itemState.render(matrices, queue, light, overlay, tintColor)` — **rendering is a plain
  instance method on `ItemRenderState` itself**, not a static utility call.
- **Where the `ItemRenderState` actually gets populated** — confirmed in
  `ArmedEntityRenderState.updateRenderState(...)` (called from `BipedEntityRenderer
  .updateBipedRenderState`, which this project's `ZombieColossusEntityRenderer` already calls
  directly — see that class):
  ```java
  public static void updateRenderState(LivingEntity entity, ArmedEntityRenderState state,
                                        ItemModelManager itemModelManager, float tickProgress) {
      state.mainArm = entity.getMainArm();
      state.handSwingProgress = entity.getHandSwingProgress(tickProgress);
      itemModelManager.updateForLivingEntity(state.rightHandItemState, entity.getStackInArm(Arm.RIGHT),
          ItemDisplayContext.THIRD_PERSON_RIGHT_HAND, entity);
      itemModelManager.updateForLivingEntity(state.leftHandItemState, entity.getStackInArm(Arm.LEFT),
          ItemDisplayContext.THIRD_PERSON_LEFT_HAND, entity);
      state.rightHandItem = entity.getStackInArm(Arm.RIGHT).copy();
      state.leftHandItem = entity.getStackInArm(Arm.LEFT).copy();
  }
  ```
  I.e. **by the time any `FeatureRenderer.render(...)` runs, `state.rightHandItemState` is
  already a fully-baked `ItemRenderState`** (quads/tints/glint/particle sprite all resolved,
  displayContext fixed to `THIRD_PERSON_RIGHT_HAND`) — a custom feature renderer that reuses
  this project's existing `BipedEntityRenderer.updateBipedRenderState(entity, state, tickDelta,
  this.itemModelResolver)` call in its own `updateRenderState(...)` override (same pattern
  `ZombieColossusEntityRenderer` already uses) gets a ready-to-render `ItemRenderState` for
  free and **does not need to call `ItemModelManager` itself at all** — it only needs to add
  its own `MatrixStack` transforms before calling `state.rightHandItemState.render(...)`.
- **(3) `ItemDisplayContext` confirmed as the real, current enum name** (`net/minecraft/item/
  ItemDisplayContext.java`) — **not** `ModelTransformationMode` (an older-version name, doesn't
  exist in this version's decompiled `net/minecraft/item/` package at all). Full confirmed
  value set: `NONE, THIRD_PERSON_LEFT_HAND, THIRD_PERSON_RIGHT_HAND, FIRST_PERSON_LEFT_HAND,
  FIRST_PERSON_RIGHT_HAND, HEAD, GUI, GROUND, FIXED, ON_SHELF`. There is **no dedicated
  two-handed variant** — vanilla's own two-handed-looking items (e.g. maps, tridents in some
  poses) still resolve to `THIRD_PERSON_RIGHT_HAND`/`THIRD_PERSON_LEFT_HAND` and get their
  "looks two-handed" read purely from extra `MatrixStack` transforms layered on top by the
  feature renderer (or, for vanilla items, from the item's own model JSON `display` block) —
  **there's no API-level "render this as two-handed" flag to opt into; it's just matrix
  composition**, confirming the practical approach below is the correct/only way to do it.
  Confirmed **no separate static `ItemRenderer.renderItem(...)` call is the current idiomatic
  path either** — `net.minecraft.client.render.item.ItemRenderer` still exists and still has a
  `public static void renderItem(ItemDisplayContext, MatrixStack, VertexConsumerProvider, int
  light, int overlay, int[] tints, List<BakedQuad> quads, RenderLayer layer, ItemRenderState.Glint
  glint)` static method, but it (a) still takes the old `VertexConsumerProvider` shape (not the
  `OrderedRenderCommandQueue` the rest of this version's entity-rendering pipeline uses), and
  (b) requires already-resolved `quads`/`tints`/`layer` you'd otherwise have to extract from a
  `BakedModel` by hand — i.e. it's a low-level helper `ItemRenderState.LayerRenderState.render(...)`
  itself calls into internally, not something a feature renderer should call directly anymore.
  **For "render an arbitrary `ItemStack` at a `MatrixStack` transform, held by a living
  entity", the current correct call sequence is: `ItemModelManager.updateForLivingEntity(
  ItemRenderState, ItemStack, ItemDisplayContext, LivingEntity)` once to populate/refresh a
  render-state-owned `ItemRenderState`, then `ItemRenderState.render(MatrixStack, 
  OrderedRenderCommandQueue, int light, int overlay, int color)` each frame to actually draw
  it** — as shown above, this project's existing `ArmedEntityRenderState.updateRenderState(...)`
  (already invoked via `BipedEntityRenderer.updateBipedRenderState`) already does the first half
  for the mainhand item automatically, so a new two-handed-weapon feature renderer typically
  only needs the second half.
- **(4) `ModelPart` field/accessor confirmed, and a real correction to a plausible-sounding
  guess**: **there is no `ModelPart.rotate(MatrixStack)` overload.** `ModelPart` has two
  *different, easily-confused* methods (`net/minecraft/client/model/ModelPart.java`):
  ```java
  public void rotate(Vector3f vec3f);          // ADDS to this part's own pitch/yaw/roll fields — mutates the part's pose, no MatrixStack involved
  public void rotate(Quaternionf quaternion);  // ADDS a quaternion rotation to this part's own pitch/yaw/roll fields — also no MatrixStack
  public void applyTransform(MatrixStack matrices); // <- THIS is the one that pushes the part's current origin/pitch/yaw/roll/scale onto a MatrixStack
  ```
  `applyTransform(MatrixStack matrices)`'s exact body: `matrices.translate(originX/16, originY/16,
  originZ/16); matrices.multiply(new Quaternionf().rotationZYX(roll, yaw, pitch));` (plus a scale
  multiply if `xScale`/`yScale`/`zScale` != 1). **This — `modelPart.applyTransform(matrixStack)`
  — is the correct call to "position/rotate at this part's current pose", not `.rotate(...)`.**
  Confirmed as exactly what `BipedEntityModel.setArmAngle(...)` itself does:
  ```java
  public void setArmAngle(BipedEntityRenderState state, Arm arm, MatrixStack matrixStack) {
      this.root.applyTransform(matrixStack);
      this.getArm(arm).applyTransform(matrixStack);
  }
  ```
  (Note it also applies `this.root`'s own transform first, not just the arm's — needed since
  the arm's `originX/Y/Z` are relative to the model root, not world space.) **`BipedEntityModel`'s
  `head`/`hat`/`body`/`rightArm`/`leftArm`/`rightLeg`/`leftLeg` fields are all `public final
  ModelPart`, confirmed directly on `BipedEntityModel` itself** (not merely `protected` —
  fully public, inherited unchanged by any subclass, including this project's own
  `AbstractZombieModel`-based `ZombieColossusEntityModel`, and equally available on a model
  that extends `BipedEntityModel` directly instead). `BipedEntityModel.getArm(Arm arm)` is
  also public, returning `leftArm`/`rightArm` by enum, if resolving by `Arm` rather than a
  hardcoded field reference is preferred.
- **(5) `EntityRendererFactory.Context` confirmed exact accessor for the item-model plumbing**
  (`net/minecraft/client/render/entity/EntityRendererFactory.java`):
  ```java
  public ItemModelManager getItemModelManager();   // NOT "getItemRenderer()"/"itemModelResolver()" — that's a field name, see below
  ```
  There is no `ItemRenderer` instance exposed on `Context` at all in this version — item
  rendering is entirely mediated through `ItemModelManager` (populate an `ItemRenderState`) +
  the render state's own `.render(...)`, per (2)/(3) above. **`itemModelResolver` (the name
  used in this project's `ZombieColossusEntityRenderer`) is not a `Context` method — it's
  `LivingEntityRenderer`'s own `protected final ItemModelManager itemModelResolver` field**,
  confirmed (`net/minecraft/client/render/entity/LivingEntityRenderer.java`):
  ```java
  protected final ItemModelManager itemModelResolver;
  // constructor: this.itemModelResolver = ctx.getItemModelManager();
  ```
  So **any `EntityRenderer` subclass extending `LivingEntityRenderer` (directly or via
  `MobEntityRenderer`/`BipedEntityRenderer`) already has `this.itemModelResolver` available for
  free**, same as `ZombieColossusEntityRenderer` already relies on. A bespoke `FeatureRenderer`
  subclass itself does **not** get an `EntityRendererFactory.Context` at all (see (1)) — if it
  needed to call `ItemModelManager` directly (rather than reusing the render state's
  already-populated `ItemRenderState`, which is the recommended approach here), the parent
  renderer would need to pass its own `this.itemModelResolver` into the feature renderer's
  constructor manually.
- **Practical recommended shape for "scaled-up, centered two-handed grip" — combining all of
  the above.** Reuses the render state's already-populated `rightHandItemState` (no need to
  call `ItemModelManager` from the feature renderer itself) and layers extra scale/position on
  top of a chosen `ModelPart`'s own transform, exactly the same "push, transform, render,
  pop" shape `HeldItemFeatureRenderer` itself uses, just with a different transform recipe:
  ```java
  public class TwoHandedWeaponFeatureRenderer<S extends ArmedEntityRenderState, M extends EntityModel<S> & ModelWithArms<S>>
          extends FeatureRenderer<S, M> {
      private static final float EXTRA_SCALE = 1.8F; // on top of whatever the boss's own body scale already is

      public TwoHandedWeaponFeatureRenderer(FeatureRendererContext<S, M> context) {
          super(context);
      }

      @Override
      public void render(MatrixStack matrices, OrderedRenderCommandQueue queue, int light, S state, float limbAngle, float limbDistance) {
          if (state.rightHandItemState.isEmpty()) return;
          matrices.push();
          // Anchor at the model root + main (right) arm's own current pose instead of vanilla's
          // per-hand offset - gives a stable "in front of the torso" base position to build from.
          // (DrevathisEntityModel's own `rightArm` field - public final, inherited from
          // BipedEntityModel directly, same as confirmed above. The root part itself is only
          // `protected final ModelPart root` on the base `Model<S>` class - not accessible from
          // outside the model's own class/package - so external code must go through the public
          // `getRootPart()` accessor instead, confirmed on `Model<S>`: `public final ModelPart
          // getRootPart() { return this.root; }`. There is no `getRoot()` method - that name
          // doesn't exist in this version, `getRootPart()` is the real one.)
          this.getContextModel().getRootPart().applyTransform(matrices);
          // Re-orient from "held flat" (vanilla per-hand default) to upright, same base
          // reorientation HeldItemFeatureRenderer itself applies:
          matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-90.0F));
          matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180.0F));
          // Centered-in-front-of-torso offset instead of vanilla's per-hand +-1/16 lateral
          // translate - tune the X/Y/Z to taste against the model's own arm span.
          matrices.translate(0.0, 0.25, -0.9);
          matrices.scale(EXTRA_SCALE, EXTRA_SCALE, EXTRA_SCALE);
          state.rightHandItemState.render(matrices, queue, light, OverlayTexture.DEFAULT_UV, state.outlineColor);
          matrices.pop();
      }
  }
  ```
  Attach it the same way `ZombieColossusEntityRenderer` attaches `HeldItemFeatureRenderer` —
  `this.addFeature(new TwoHandedWeaponFeatureRenderer<>(this))` — **instead of**
  `this.addFeature(new HeldItemFeatureRenderer<>(this))` (don't add both; they'd both render
  the same mainhand item). The renderer's own `updateRenderState(...)` still needs to call
  `BipedEntityRenderer.updateBipedRenderState(entity, state, tickDelta, this.itemModelResolver)`
  exactly as `ZombieColossusEntityRenderer` already does, purely so `state.rightHandItemState`
  gets populated each frame — this new feature renderer only replaces *how* that already-baked
  `ItemRenderState` gets positioned/scaled at render time, not *whether* it gets populated.
  Tune the `applyTransform`/rotate/translate/scale recipe by trial in-game (exact numbers
  depend on this specific model's arm span and the weapon item's own baked model bounding box)
  — there is no single "correct" numeric offset, only the mechanism above is confirmed correct.

## Open questions
