package lol.duckyyy.api;

import lol.duckyyy.CoduhLink;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record ServerboundRaidPayload(String raider, int viewers, String game) implements CustomPacketPayload {
    public static final Identifier RAID_PAYLOAD_ID = Identifier.fromNamespaceAndPath(CoduhLink.MOD_ID, "raid");
    public static final Type TYPE = new Type<>(RAID_PAYLOAD_ID);
    public static final StreamCodec<RegistryFriendlyByteBuf, ServerboundRaidPayload> CODEC = StreamCodec.of(
            (buf, val) -> {
                buf.writeUtf(val.raider);
                buf.writeInt(val.viewers);
                buf.writeUtf(val.game);
            },
            buf -> {
                String raider = buf.readUtf();
                int viewers = buf.readInt();
                String game = buf.readUtf();
                return new ServerboundRaidPayload(raider, viewers, game);
            }
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
