package lol.duckyyy.api;

import lol.duckyyy.CoduhLink;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record ServerboundChatMessagePayload(String username, String content, int color) implements CustomPacketPayload {
    public static final Identifier CHAT_MESSAGE_PAYLOAD_ID = Identifier.fromNamespaceAndPath(CoduhLink.MOD_ID, "chat_message");
    public static final Type TYPE = new Type<>(CHAT_MESSAGE_PAYLOAD_ID);
    public static final StreamCodec<RegistryFriendlyByteBuf, ServerboundChatMessagePayload> CODEC = StreamCodec.of(
            (buf, val) -> {
                buf.writeUtf(val.username);
                buf.writeUtf(val.content);
                buf.writeInt(val.color);
            },
            buf -> {
                String username = buf.readUtf();
                String content = buf.readUtf();
                int color = buf.readInt();
                return new ServerboundChatMessagePayload(username, content, color);
            }
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
