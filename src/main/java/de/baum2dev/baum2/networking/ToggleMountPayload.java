package de.baum2dev.baum2.networking;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/**
 * Client-to-Server payload sent when the player presses Ctrl+H (see ui/MountKeyBindings):
 * summon the horse of the equipped flute's tier, or dismiss the one currently ridden. Carries
 * no data - the server decides everything from the player's own state (equipped flute,
 * current vehicle), so a tampering client can't request a tier it doesn't own.
 */
public record ToggleMountPayload() implements CustomPayload {
    public static final Identifier ID = Identifier.of("baum2", "toggle_mount");
    public static final CustomPayload.Id<ToggleMountPayload> TYPE = new CustomPayload.Id<>(ID);

    public static final PacketCodec<RegistryByteBuf, ToggleMountPayload> CODEC =
            PacketCodec.unit(new ToggleMountPayload());

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() {
        return TYPE;
    }
}
