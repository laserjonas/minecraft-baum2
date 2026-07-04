package de.baum2dev.baum2.networking;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/**
 * Server-to-Client payload for syncing player experience level display.
 * Contains: level (int), current experience (long), max experience for level (long)
 *
 * Uses PacketCodec with RegistryByteBuf (Yarn 1.21.11+build.6 naming for the
 * CustomPayload codec API — other mappings call these StreamCodec/RegistryFriendlyByteBuf).
 */
public record ExperienceSyncPayload(int level, long experience, long maxExperience) implements CustomPayload {
    public static final Identifier ID = Identifier.of("baum2", "experience_sync");
    public static final CustomPayload.Id<ExperienceSyncPayload> TYPE = new CustomPayload.Id<>(ID);

    /**
     * Encodes: level as VAR_INT, experience as VAR_LONG, maxExperience as VAR_LONG
     */
    public static final PacketCodec<RegistryByteBuf, ExperienceSyncPayload> CODEC =
            PacketCodec.tuple(
                    PacketCodecs.VAR_INT, ExperienceSyncPayload::level,
                    PacketCodecs.VAR_LONG, ExperienceSyncPayload::experience,
                    PacketCodecs.VAR_LONG, ExperienceSyncPayload::maxExperience,
                    ExperienceSyncPayload::new
            );

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() {
        return TYPE;
    }
}
