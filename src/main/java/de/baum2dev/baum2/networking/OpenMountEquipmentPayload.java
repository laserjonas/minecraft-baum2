package de.baum2dev.baum2.networking;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/**
 * Client-to-Server payload asking the server to open the equipment inventory (the screen
 * holding the mount/flute slot - see mounts/MountEquipmentScreenHandler). A HandledScreen
 * with real item slots must be opened server-side via openHandledScreen, so the keybind
 * sends this instead of opening a Screen locally the way CharacterStatsScreen does.
 */
public record OpenMountEquipmentPayload() implements CustomPayload {
    public static final Identifier ID = Identifier.of("baum2", "open_mount_equipment");
    public static final CustomPayload.Id<OpenMountEquipmentPayload> TYPE = new CustomPayload.Id<>(ID);

    public static final PacketCodec<RegistryByteBuf, OpenMountEquipmentPayload> CODEC =
            PacketCodec.unit(new OpenMountEquipmentPayload());

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() {
        return TYPE;
    }
}
