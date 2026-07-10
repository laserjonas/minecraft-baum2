package de.baum2dev.baum2.items;

import net.minecraft.item.Item;
import de.baum2dev.baum2.mounts.MountTier;

/**
 * One of the three horse flutes. The flute does nothing when used from the hotbar - it must
 * sit in the mount slot of the equipment inventory (see mounts/MountEquipmentManager); the
 * Ctrl+H mount toggle then summons/dismisses the horse of this flute's tier.
 */
public class HorseFluteItem extends Item {

    private final MountTier tier;

    public HorseFluteItem(Settings settings, MountTier tier) {
        super(settings);
        this.tier = tier;
    }

    public MountTier tier() {
        return tier;
    }
}
