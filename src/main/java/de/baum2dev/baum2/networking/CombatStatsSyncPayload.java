package de.baum2dev.baum2.networking;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/**
 * Server-to-Client payload for syncing Base Damage / Base Magic Damage for the character
 * stats screen. Follows the same PacketCodec/RegistryByteBuf pattern as
 * {@link ExperienceSyncPayload}/{@link ManaSyncPayload}. Sent once on join rather than every
 * tick like Mana, since these are flat values that don't change on their own.
 */
public record CombatStatsSyncPayload(float baseDamage, float baseMagicDamage) implements CustomPayload {
    public static final Identifier ID = Identifier.of("baum2", "combat_stats_sync");
    public static final CustomPayload.Id<CombatStatsSyncPayload> TYPE = new CustomPayload.Id<>(ID);

    public static final PacketCodec<RegistryByteBuf, CombatStatsSyncPayload> CODEC =
            PacketCodec.tuple(
                    PacketCodecs.FLOAT, CombatStatsSyncPayload::baseDamage,
                    PacketCodecs.FLOAT, CombatStatsSyncPayload::baseMagicDamage,
                    CombatStatsSyncPayload::new
            );

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() {
        return TYPE;
    }
}
