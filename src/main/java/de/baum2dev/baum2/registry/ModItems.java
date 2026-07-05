package de.baum2dev.baum2.registry;

import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.item.ToolMaterial;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;

public class ModItems {

    public static final RegistryKey<Item> GOLD_SWORD_KEY =
            RegistryKey.of(RegistryKeys.ITEM, Identifier.of("baum2", "gold_sword"));

    public static final Item GOLD_SWORD = Registry.register(
            Registries.ITEM,
            GOLD_SWORD_KEY,
            new Item(new Item.Settings().registryKey(GOLD_SWORD_KEY).sword(ToolMaterial.GOLD, 5.0F, -2.2F))
    );

    public static void registerItemGroups() {
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.COMBAT).register(entries -> entries.add(GOLD_SWORD));
    }

    /** No-op - calling this forces this class (and its static Item registrations) to load. */
    public static void bootstrap() {
    }
}
