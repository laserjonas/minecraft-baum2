package de.baum2dev.baum2.world;

import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;

/**
 * Registry keys/identifiers for Heimgrund, the mod's starting dimension (the finite
 * village-and-monster-zones world every new game begins in). The dimension itself is
 * defined by datapack JSON under data/baum2/dimension/heimgrund.json - datapack
 * dimensions are baked into a save at world creation, so it only exists in worlds
 * created with the mod installed.
 */
public final class Baum2WorldKeys {

    public static final Identifier HEIMGRUND_ID = Identifier.of("baum2", "heimgrund");

    public static final RegistryKey<World> HEIMGRUND = RegistryKey.of(RegistryKeys.WORLD, HEIMGRUND_ID);

    /** Single source of truth for "does this world get Heimgrund rules?" (protection, spawns, ...). */
    public static boolean isHeimgrund(World world) {
        return world.getRegistryKey() == HEIMGRUND;
    }

    private Baum2WorldKeys() {
    }
}
