package lol.duckyyy.api;

import lol.duckyyy.CoduhLink;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record ServerboundRewardRedemptionPayload(String id, String username, String title,
                                                 String input, int cost) implements CustomPacketPayload {
    public static final Identifier REWARD_REDEMPTION_PAYLOAD_ID = Identifier.fromNamespaceAndPath(CoduhLink.MOD_ID, "reward_redemption");
    public static final Type TYPE = new Type<>(REWARD_REDEMPTION_PAYLOAD_ID);
    public static final StreamCodec<RegistryFriendlyByteBuf, ServerboundRewardRedemptionPayload> CODEC = StreamCodec.of(
            (buf, val) -> {
                buf.writeUtf(val.id);
                buf.writeUtf(val.username);
                buf.writeUtf(val.title);
                buf.writeUtf(val.input);
                buf.writeInt(val.cost);
            },
            buf -> {
                String id = buf.readUtf();
                String username = buf.readUtf();
                String title = buf.readUtf();
                String input = buf.readUtf();
                int cost = buf.readInt();
                return new ServerboundRewardRedemptionPayload(id, username, title, input, cost);
            }
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
