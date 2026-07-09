package de.baum2dev.baum2.registry;

import java.util.List;
import java.util.function.Supplier;

import net.minecraft.entity.EntityType;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.particle.ParticleTypes;
import de.baum2dev.baum2.entity.FallenCometStoneDefinition;
import de.baum2dev.baum2.entity.FallenCometStoneDefinition.WaveMob;

/**
 * The definition table for every fallen-comet-stone mini-boss - one stone per normal vanilla
 * hostile monster, on a difficulty ladder in 5-level steps (roughly two stones per tier, every
 * tier from 45 up is a pair; health is always 20 HP per level, the ratio the two original
 * stones established). Registration/attributes/renderers all loop over {@link #ALL} - adding a
 * stone here (plus a palette in tools/gen_fallen_comet_stone.py and a lang entry) is the
 * complete recipe.
 *
 * Deliberately excluded, with reasons:
 * - Bosses per the design brief: Ender Dragon, Wither, Warden, Elder Guardian.
 * - Guardian: water-bound - it flops helplessly on land, where stone fights happen (the
 *   Zombie Nautilus is included instead; it has real on-land behavior).
 * - Creaking: its invulnerability is tied to the creaking-heart block, so a freestanding wave
 *   spawn either breaks the mob's own mechanic or spawns unkillable adds.
 * - Giant, Illusioner, Zombie Horse: unused/AI-less leftovers in vanilla.
 * - Zombie Villager: gameplay duplicate of the Zombie (the cure mechanic doesn't survive the
 *   death-cascade anyway).
 *
 * Drops are themed vanilla loot (each stone pays out in its monster's own currency); the two
 * original stones keep their custom weapon drops unchanged.
 */
public final class FallenCometStones {

    public static final List<FallenCometStoneDefinition> ALL = List.of(
            // ---- tier 5-15: skittering starters ----
            // 4/3 per wave, not more: balance review flagged that the LOWEST-level stones
            // must not flood more adds than the endgame ones - swarm flavor, capped.
            stone("stone_of_silverfish", 5,
                    List.of(WaveMob.of(EntityType.SILVERFISH, 4)),
                    drops(stack(Items.IRON_NUGGET, 12))),
            stone("stone_of_endermites", 10,
                    List.of(WaveMob.of(EntityType.ENDERMITE, 3)),
                    drops(stack(Items.ENDER_PEARL, 2))),
            stone("stone_of_spiders", 10,
                    List.of(WaveMob.of(EntityType.SPIDER, 3)),
                    drops(stack(ModItems.GOLD_SWORD, 1))),
            stone("stone_of_skeletons", 15,
                    List.of(WaveMob.of(EntityType.SKELETON, 2)),
                    drops(stack(Items.BONE, 16), stack(Items.ARROW, 16))),

            // ---- tier 20-45: the undead belt ----
            new FallenCometStoneDefinition("stone_of_zombies", 20,
                    List.of(WaveMob.of(EntityType.ZOMBIE, 2),
                            WaveMob.of(EntityType.ZOMBIE, 1, zombie -> zombie.setBaby(true))),
                    drops(stack(ModItems.POISON_DAGGER, 1)),
                    ParticleTypes.LARGE_SMOKE),
            stone("stone_of_the_parched", 20,
                    List.of(WaveMob.of(EntityType.PARCHED, 2)),
                    drops(stack(Items.ARROW, 16), stack(Items.SANDSTONE, 8))),
            stone("stone_of_cave_spiders", 25,
                    List.of(WaveMob.of(EntityType.CAVE_SPIDER, 3)),
                    drops(stack(Items.COBWEB, 8), stack(Items.SPIDER_EYE, 6))),
            stone("stone_of_the_drowned", 30,
                    List.of(WaveMob.of(EntityType.DROWNED, 3)),
                    drops(stack(Items.COPPER_INGOT, 12), stack(Items.NAUTILUS_SHELL, 1))),
            stone("stone_of_zombie_nautiluses", 30,
                    List.of(WaveMob.of(EntityType.ZOMBIE_NAUTILUS, 3)),
                    drops(stack(Items.NAUTILUS_SHELL, 3))),
            stone("stone_of_husks", 35,
                    List.of(WaveMob.of(EntityType.HUSK, 2),
                            WaveMob.of(EntityType.HUSK, 1, husk -> husk.setBaby(true))),
                    drops(stack(Items.GOLD_INGOT, 4), stack(Items.SAND, 16))),
            stone("stone_of_strays", 40,
                    List.of(WaveMob.of(EntityType.STRAY, 2)),
                    drops(stack(Items.BLUE_ICE, 4), stack(Items.BONE, 16))),
            stone("stone_of_the_bogged", 45,
                    List.of(WaveMob.of(EntityType.BOGGED, 2)),
                    drops(stack(Items.MOSS_BLOCK, 8), stack(Items.ARROW, 16))),
            stone("stone_of_camel_husks", 45,
                    List.of(WaveMob.of(EntityType.CAMEL_HUSK, 2)),
                    drops(stack(Items.LEATHER, 10))),

            // ---- tier 50-65: overworld menaces + first nether visitors ----
            stone("stone_of_creepers", 50,
                    List.of(WaveMob.of(EntityType.CREEPER, 2)),
                    drops(stack(Items.GUNPOWDER, 16), stack(Items.TNT, 2))),
            stone("stone_of_piglins", 50,
                    List.of(WaveMob.of(EntityType.PIGLIN, 3, piglin -> piglin.setImmuneToZombification(true))),
                    drops(stack(Items.GOLD_INGOT, 12))),
            stone("stone_of_witches", 55,
                    List.of(WaveMob.of(EntityType.WITCH, 2)),
                    drops(stack(Items.GLOWSTONE_DUST, 8), stack(Items.REDSTONE, 8))),
            stone("stone_of_breezes", 55,
                    List.of(WaveMob.of(EntityType.BREEZE, 2)),
                    drops(stack(Items.BREEZE_ROD, 4))),
            // Fixed size 2: consistent wave pressure and only ONE split generation. Split
            // children are new entities the stone can't track, so they escape the
            // death-cascade - a known, size-bounded leak (max 2-4 size-1 slimes per kill).
            stone("stone_of_slimes", 60,
                    List.of(WaveMob.of(EntityType.SLIME, 4, slime -> slime.setSize(2, true))),
                    drops(stack(Items.SLIME_BALL, 16))),
            stone("stone_of_hoglins", 60,
                    List.of(WaveMob.of(EntityType.HOGLIN, 2, hoglin -> hoglin.setImmuneToZombification(true))),
                    drops(stack(Items.COOKED_PORKCHOP, 12), stack(Items.LEATHER, 8))),
            stone("stone_of_phantoms", 65,
                    List.of(WaveMob.of(EntityType.PHANTOM, 3)),
                    drops(stack(Items.PHANTOM_MEMBRANE, 6))),
            stone("stone_of_zombified_piglins", 65,
                    List.of(WaveMob.of(EntityType.ZOMBIFIED_PIGLIN, 3)),
                    drops(stack(Items.GOLD_NUGGET, 24), stack(Items.GOLD_INGOT, 4))),

            // ---- tier 70-80: heavy hitters ----
            stone("stone_of_endermen", 70,
                    List.of(WaveMob.of(EntityType.ENDERMAN, 2)),
                    drops(stack(Items.ENDER_PEARL, 6))),
            // same fixed-size-2 reasoning as Stone of Slimes
            stone("stone_of_magma_cubes", 70,
                    List.of(WaveMob.of(EntityType.MAGMA_CUBE, 3, cube -> cube.setSize(2, true))),
                    drops(stack(Items.MAGMA_CREAM, 12))),
            stone("stone_of_pillagers", 75,
                    List.of(WaveMob.of(EntityType.PILLAGER, 3)),
                    drops(stack(Items.EMERALD, 8), stack(Items.CROSSBOW, 1))),
            stone("stone_of_blazes", 75,
                    List.of(WaveMob.of(EntityType.BLAZE, 3)),
                    drops(stack(Items.BLAZE_ROD, 6))),
            stone("stone_of_vindicators", 80,
                    List.of(WaveMob.of(EntityType.VINDICATOR, 2)),
                    drops(stack(Items.EMERALD, 12), stack(Items.IRON_AXE, 1))),
            stone("stone_of_ghasts", 80,
                    List.of(WaveMob.of(EntityType.GHAST, 1)),
                    drops(stack(Items.GHAST_TEAR, 3))),

            // ---- tier 85-95: the endgame ladder ----
            stone("stone_of_vexes", 85,
                    List.of(WaveMob.of(EntityType.VEX, 4)),
                    drops(stack(Items.EMERALD, 12))),
            stone("stone_of_wither_skeletons", 85,
                    List.of(WaveMob.of(EntityType.WITHER_SKELETON, 2)),
                    drops(stack(Items.WITHER_ROSE, 4), stack(Items.COAL_BLOCK, 8))),
            stone("stone_of_shulkers", 90,
                    List.of(WaveMob.of(EntityType.SHULKER, 2)),
                    drops(stack(Items.SHULKER_SHELL, 2))),
            stone("stone_of_evokers", 90,
                    List.of(WaveMob.of(EntityType.EVOKER, 1)),
                    drops(stack(Items.TOTEM_OF_UNDYING, 1), stack(Items.EMERALD, 8))),
            stone("stone_of_piglin_brutes", 95,
                    List.of(WaveMob.of(EntityType.PIGLIN_BRUTE, 2, brute -> brute.setImmuneToZombification(true))),
                    drops(stack(Items.GOLD_BLOCK, 2), stack(Items.NETHERITE_SCRAP, 1))),
            stone("stone_of_ravagers", 95,
                    List.of(WaveMob.of(EntityType.RAVAGER, 1)),
                    drops(stack(Items.SADDLE, 1), stack(Items.EMERALD, 16))));

    private static FallenCometStoneDefinition stone(String name, int level, List<WaveMob<?>> wave,
            Supplier<List<ItemStack>> drops) {
        return new FallenCometStoneDefinition(name, level, wave, drops, null);
    }

    /** Drop-list supplier from (item, count) pairs; suppliers so every death gets fresh stacks. */
    @SafeVarargs
    private static Supplier<List<ItemStack>> drops(Supplier<ItemStack>... stacks) {
        return () -> List.of(java.util.Arrays.stream(stacks).map(Supplier::get).toArray(ItemStack[]::new));
    }

    private static Supplier<ItemStack> stack(Item item, int count) {
        return () -> new ItemStack(item, count);
    }

    private FallenCometStones() {
    }
}
