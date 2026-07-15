package de.baum2dev.baum2.registry;

import com.mojang.serialization.Codec;

import net.minecraft.component.ComponentType;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public class ModComponents {

    /**
     * Per-stack weapon upgrade tier (see items/WeaponTier). Absent = base tier. Values are
     * WeaponTier component ids ("tempered"/"perfect"). String (not enum) on purpose: the
     * item-definition JSONs dispatch their model on this component via the
     * `minecraft:select` / `"property": "minecraft:component"` mechanism, which matches the
     * codec's serialized string (see docs/fabric-modding.md "Per-ItemStack weapon upgrade
     * tier", item 2). The persistent `.codec(...)` is required twice over: for save/load AND
     * because ComponentSelectProperty rejects codec-less components.
     */
    public static final ComponentType<String> WEAPON_TIER = Registry.register(
            Registries.DATA_COMPONENT_TYPE,
            Identifier.of("baum2", "weapon_tier"),
            ComponentType.<String>builder()
                    .codec(Codec.STRING)
                    .packetCodec(PacketCodecs.STRING)
                    .build()
    );

    /** No-op - calling this forces this class (and its registrations) to load during mod init. */
    public static void bootstrap() {
    }
}
