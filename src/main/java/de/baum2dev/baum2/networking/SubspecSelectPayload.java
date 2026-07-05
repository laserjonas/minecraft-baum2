package de.baum2dev.baum2.networking;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import de.baum2dev.baum2.classes.ClassSubspec;

/**
 * Client-to-Server payload for selecting a sub-spec from the Class tab GUI. Mirrors
 * {@link ClassSelectPayload}'s pattern exactly, one level down (sub-spec instead of class).
 */
public record SubspecSelectPayload(ClassSubspec subspec) implements CustomPayload {
    public static final Identifier ID = Identifier.of("baum2", "subspec_select");
    public static final CustomPayload.Id<SubspecSelectPayload> TYPE = new CustomPayload.Id<>(ID);

    public static final PacketCodec<RegistryByteBuf, SubspecSelectPayload> CODEC = PacketCodec.tuple(
        PacketCodecs.VAR_INT, payload -> payload.subspec().ordinal(),
        ordinal -> new SubspecSelectPayload(ClassSubspec.values()[ordinal])
    );

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() {
        return TYPE;
    }
}
