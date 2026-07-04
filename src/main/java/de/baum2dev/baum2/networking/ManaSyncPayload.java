package de.baum2dev.baum2.networking;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/**
 * Server-to-Client payload for syncing the player's current/max Mana for HUD display.
 * Follows the same PacketCodec/RegistryByteBuf pattern as {@link ExperienceSyncPayload}.
 */
public record ManaSyncPayload(int mana, int maxMana) implements CustomPayload {
    public static final Identifier ID = Identifier.of("baum2", "mana_sync");
    public static final CustomPayload.Id<ManaSyncPayload> TYPE = new CustomPayload.Id<>(ID);

    public static final PacketCodec<RegistryByteBuf, ManaSyncPayload> CODEC =
            PacketCodec.tuple(
                    PacketCodecs.VAR_INT, ManaSyncPayload::mana,
                    PacketCodecs.VAR_INT, ManaSyncPayload::maxMana,
                    ManaSyncPayload::new
            );

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() {
        return TYPE;
    }
}
