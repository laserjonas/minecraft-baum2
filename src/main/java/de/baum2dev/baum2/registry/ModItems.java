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

    public static void registerItemGroups() {
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.COMBAT).register(entries -> {
            entries.add(GOLD_SWORD);
            entries.add(POISON_DAGGER);
            entries.add(QUEEN_SPIDER_HELMET);
            entries.add(QUEEN_SPIDER_CHESTPLATE);
            entries.add(QUEEN_SPIDER_LEGGINGS);
            entries.add(QUEEN_SPIDER_BOOTS);
        });
    }

    /** No-op - calling this forces this class (and its static Item registrations) to load. */
    public static void bootstrap() {
    }
}
