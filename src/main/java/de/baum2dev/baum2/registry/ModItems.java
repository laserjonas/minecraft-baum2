package de.baum2dev.baum2.registry;

import java.util.Map;

import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.item.ToolMaterial;
import net.minecraft.item.equipment.ArmorMaterial;
import net.minecraft.item.equipment.EquipmentAssetKeys;
import net.minecraft.item.equipment.EquipmentType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;

public class ModItems {

    public static final RegistryKey<Item> GOLD_SWORD_KEY =
            RegistryKey.of(RegistryKeys.ITEM, Identifier.of("baum2", "gold_sword"));

    public static final Item GOLD_SWORD = Registry.register(
            Registries.ITEM,
            GOLD_SWORD_KEY,
            new Item(new Item.Settings().registryKey(GOLD_SWORD_KEY).sword(ToolMaterial.GOLD, 5.0F, -2.2F))
    );

    public static final RegistryKey<Item> POISON_DAGGER_KEY =
            RegistryKey.of(RegistryKeys.ITEM, Identifier.of("baum2", "poison_dagger"));

    public static final Item POISON_DAGGER = Registry.register(
            Registries.ITEM,
            POISON_DAGGER_KEY,
            new Item(new Item.Settings().registryKey(POISON_DAGGER_KEY).sword(ToolMaterial.IRON, 1.0F, 0.0F))
    );

    /**
     * "Colossal Warclub" - dropped by the Zombie Colossus boss. Great damage, slow speed per
     * the boss's own design brief; built on the generic .sword() builder like Gold Sword/Poison
     * Dagger (this codebase's established pattern for non-sword-shaped melee weapons - there is
     * no dedicated blunt-weapon Item type in this API version). Originally named "Colossus
     * Club" - renamed after ip-naming-compliance-checker found that exact name is a real,
     * if minor/non-iconic, existing item in EverQuest 2.
     */
    public static final RegistryKey<Item> COLOSSAL_WARCLUB_KEY =
            RegistryKey.of(RegistryKeys.ITEM, Identifier.of("baum2", "colossal_warclub"));

    public static final Item COLOSSAL_WARCLUB = Registry.register(
            Registries.ITEM,
            COLOSSAL_WARCLUB_KEY,
            new Item(new Item.Settings().registryKey(COLOSSAL_WARCLUB_KEY).sword(ToolMaterial.IRON, 12.0F, -3.0F))
    );

    /**
     * "Queen Spider Set" - dropped by the Spider Queen boss. Stats sit between Iron and
     * Diamond pending a real balance pass (see HANDOFF.md); repair material and equip sound
     * are placeholder choices (no dedicated spider-silk material exists yet in this mod).
     */
    public static final ArmorMaterial QUEEN_SPIDER_ARMOR_MATERIAL = new ArmorMaterial(
            30,
            Map.of(
                    EquipmentType.BOOTS, 3,
                    EquipmentType.LEGGINGS, 6,
                    EquipmentType.CHESTPLATE, 8,
                    EquipmentType.HELMET, 3
            ),
            10,
            SoundEvents.ITEM_ARMOR_EQUIP_LEATHER,
            1.0F,
            0.0F,
            ItemTags.REPAIRS_IRON_ARMOR,
            RegistryKey.of(EquipmentAssetKeys.REGISTRY_KEY, Identifier.of("baum2", "queen_spider"))
    );

    public static final RegistryKey<Item> QUEEN_SPIDER_HELMET_KEY =
            RegistryKey.of(RegistryKeys.ITEM, Identifier.of("baum2", "queen_spider_helmet"));
    public static final Item QUEEN_SPIDER_HELMET = Registry.register(
            Registries.ITEM,
            QUEEN_SPIDER_HELMET_KEY,
            new Item(new Item.Settings().registryKey(QUEEN_SPIDER_HELMET_KEY).armor(QUEEN_SPIDER_ARMOR_MATERIAL, EquipmentType.HELMET))
    );

    public static final RegistryKey<Item> QUEEN_SPIDER_CHESTPLATE_KEY =
            RegistryKey.of(RegistryKeys.ITEM, Identifier.of("baum2", "queen_spider_chestplate"));
    public static final Item QUEEN_SPIDER_CHESTPLATE = Registry.register(
            Registries.ITEM,
            QUEEN_SPIDER_CHESTPLATE_KEY,
            new Item(new Item.Settings().registryKey(QUEEN_SPIDER_CHESTPLATE_KEY).armor(QUEEN_SPIDER_ARMOR_MATERIAL, EquipmentType.CHESTPLATE))
    );

    public static final RegistryKey<Item> QUEEN_SPIDER_LEGGINGS_KEY =
            RegistryKey.of(RegistryKeys.ITEM, Identifier.of("baum2", "queen_spider_leggings"));
    public static final Item QUEEN_SPIDER_LEGGINGS = Registry.register(
            Registries.ITEM,
            QUEEN_SPIDER_LEGGINGS_KEY,
            new Item(new Item.Settings().registryKey(QUEEN_SPIDER_LEGGINGS_KEY).armor(QUEEN_SPIDER_ARMOR_MATERIAL, EquipmentType.LEGGINGS))
    );

    public static final RegistryKey<Item> QUEEN_SPIDER_BOOTS_KEY =
            RegistryKey.of(RegistryKeys.ITEM, Identifier.of("baum2", "queen_spider_boots"));
    public static final Item QUEEN_SPIDER_BOOTS = Registry.register(
            Registries.ITEM,
            QUEEN_SPIDER_BOOTS_KEY,
            new Item(new Item.Settings().registryKey(QUEEN_SPIDER_BOOTS_KEY).armor(QUEEN_SPIDER_ARMOR_MATERIAL, EquipmentType.BOOTS))
    );

    /**
     * "Risssplitter" - the rare material dropped by the Rissobelisk world-event block
     * (`block/RissobeliskBlock.java`). Plain crafting-material item, no recipes/equipment
     * behavior yet - the full Upgrade-Materialien system (MASTERPROMPT.md Priority 2) is a
     * separate, larger task this doesn't attempt.
     */
    public static final RegistryKey<Item> RISSSPLITTER_KEY =
            RegistryKey.of(RegistryKeys.ITEM, Identifier.of("baum2", "risssplitter"));

    public static final Item RISSSPLITTER = Registry.register(
            Registries.ITEM,
            RISSSPLITTER_KEY,
            new Item(new Item.Settings().registryKey(RISSSPLITTER_KEY))
    );

    public static void registerItemGroups() {
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.COMBAT).register(entries -> {
            entries.add(GOLD_SWORD);
            entries.add(POISON_DAGGER);
            entries.add(COLOSSAL_WARCLUB);
            entries.add(QUEEN_SPIDER_HELMET);
            entries.add(QUEEN_SPIDER_CHESTPLATE);
            entries.add(QUEEN_SPIDER_LEGGINGS);
            entries.add(QUEEN_SPIDER_BOOTS);
        });
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.INGREDIENTS).register(entries -> entries.add(RISSSPLITTER));
    }

    /** No-op - calling this forces this class (and its static Item registrations) to load. */
    public static void bootstrap() {
    }
}
