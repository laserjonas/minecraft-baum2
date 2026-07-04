package de.baum2dev.baum2.networking;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/**
 * Server-to-Client payload syncing the four raw attributes plus unspent points. Derived
 * stats (Base Attack, Physical Defence, Crit Chance, etc.) are NOT synced separately - they
 * are pure functions of these raw values, computed client-side via VitalsCurve (shared
 * common-sourceset code), the same way the client already computes total XP from level.
 */
public record AttributeSyncPayload(int endurance, int intelligence, int strength, int dexterity,
                                    int unspentAttributePoints) implements CustomPayload {
    public static final Identifier ID = Identifier.of("baum2", "attribute_sync");
    public static final CustomPayload.Id<AttributeSyncPayload> TYPE = new CustomPayload.Id<>(ID);

    public static final PacketCodec<RegistryByteBuf, AttributeSyncPayload> CODEC =
            PacketCodec.tuple(
                    PacketCodecs.VAR_INT, AttributeSyncPayload::endurance,
                    PacketCodecs.VAR_INT, AttributeSyncPayload::intelligence,
                    PacketCodecs.VAR_INT, AttributeSyncPayload::strength,
                    PacketCodecs.VAR_INT, AttributeSyncPayload::dexterity,
                    PacketCodecs.VAR_INT, AttributeSyncPayload::unspentAttributePoints,
                    AttributeSyncPayload::new
            );

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() {
        return TYPE;
    }
}
