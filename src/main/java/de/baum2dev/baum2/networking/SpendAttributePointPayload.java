package de.baum2dev.baum2.networking;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import de.baum2dev.baum2.progression.AttributeType;

/**
 * Client-to-Server payload: sent when the player clicks a "+1" button in the Character
 * Stats screen. The server validates unspent points itself (AttributeManager.trySpendPoint)
 * rather than trusting the client - this payload only names which attribute was clicked.
 */
public record SpendAttributePointPayload(AttributeType attribute) implements CustomPayload {
    public static final Identifier ID = Identifier.of("baum2", "spend_attribute_point");
    public static final CustomPayload.Id<SpendAttributePointPayload> TYPE = new CustomPayload.Id<>(ID);

    public static final PacketCodec<RegistryByteBuf, SpendAttributePointPayload> CODEC =
            PacketCodec.tuple(
                    PacketCodecs.indexed(i -> AttributeType.values()[i], AttributeType::ordinal),
                    SpendAttributePointPayload::attribute,
                    SpendAttributePointPayload::new
            );

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() {
        return TYPE;
    }
}
