package de.baum2dev.baum2.networking;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import de.baum2dev.baum2.classes.PlayerClass;

/**
 * Client-to-Server payload for selecting a class from the Class Screen GUI.
 * Mirrors {@link ExperienceSyncPayload}'s S2C pattern, just in the other direction.
 */
public record ClassSelectPayload(PlayerClass playerClass) implements CustomPayload {
    public static final Identifier ID = Identifier.of("baum2", "class_select");
    public static final CustomPayload.Id<ClassSelectPayload> TYPE = new CustomPayload.Id<>(ID);

    public static final PacketCodec<RegistryByteBuf, ClassSelectPayload> CODEC = PacketCodec.tuple(
        PacketCodecs.VAR_INT, payload -> payload.playerClass().ordinal(),
        ordinal -> new ClassSelectPayload(PlayerClass.values()[ordinal])
    );

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() {
        return TYPE;
    }
}
