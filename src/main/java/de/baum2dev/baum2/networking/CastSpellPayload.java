package de.baum2dev.baum2.networking;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/**
 * Client-to-Server payload for casting a spell via keybind (slot 0 = "Cast Spell 1", slot 1 =
 * "Cast Spell 2" - see ui/SpellCastKeyBindings.java). Mirrors ClassSelectPayload's pattern.
 */
public record CastSpellPayload(int slot) implements CustomPayload {
    public static final Identifier ID = Identifier.of("baum2", "cast_spell");
    public static final CustomPayload.Id<CastSpellPayload> TYPE = new CustomPayload.Id<>(ID);

    public static final PacketCodec<RegistryByteBuf, CastSpellPayload> CODEC = PacketCodec.tuple(
        PacketCodecs.VAR_INT, CastSpellPayload::slot,
        CastSpellPayload::new
    );

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() {
        return TYPE;
    }
}
