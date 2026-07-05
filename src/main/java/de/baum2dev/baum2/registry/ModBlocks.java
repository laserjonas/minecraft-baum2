package de.baum2dev.baum2.registry;

import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;
import de.baum2dev.baum2.block.RissobeliskBlock;

public class ModBlocks {

    public static final RegistryKey<Block> RISSOBELISK_KEY =
            RegistryKey.of(RegistryKeys.BLOCK, Identifier.of("baum2", "rissobelisk"));

    public static final Block RISSOBELISK = Registry.register(
            Registries.BLOCK,
            RISSOBELISK_KEY,
            new RissobeliskBlock(AbstractBlock.Settings.create()
                    .registryKey(RISSOBELISK_KEY)
                    .strength(-1.0F, 3_600_000.0F)
                    .dropsNothing())
    );

    public static final RegistryKey<Item> RISSOBELISK_ITEM_KEY =
            RegistryKey.of(RegistryKeys.ITEM, Identifier.of("baum2", "rissobelisk"));

    public static final Item RISSOBELISK_ITEM = Registry.register(
            Registries.ITEM,
            RISSOBELISK_ITEM_KEY,
            // useBlockPrefixedTranslationKey() - without it this BlockItem looks for its own
            // "item.baum2.rissobelisk" lang key instead of the block's "block.baum2.rissobelisk"
            // one, and falls back to showing the raw untranslated key. Same rule vanilla's own
            // Items.register(Block, ...) helper follows for every vanilla BlockItem.
            new BlockItem(RISSOBELISK, new Item.Settings().registryKey(RISSOBELISK_ITEM_KEY).useBlockPrefixedTranslationKey())
    );

    public static void registerItemGroups() {
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.NATURAL).register(entries -> entries.add(RISSOBELISK_ITEM));
    }

    /** No-op - calling this forces this class (and its static Block/Item registrations) to load. */
    public static void bootstrap() {
    }
}
