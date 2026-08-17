package lol.duckyyy.api;

import lol.duckyyy.CoduhLink;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.animal.Animal;

public record ClientboundRenameAnimalPayload(int entityId) implements CustomPacketPayload {
    public static final Identifier RENAME_ANIMAL_PAYLOAD_ID = Identifier.fromNamespaceAndPath(CoduhLink.MOD_ID, "rename_animal");
    public static final CustomPacketPayload.Type TYPE = new CustomPacketPayload.Type<>(RENAME_ANIMAL_PAYLOAD_ID);

    public static final StreamCodec<RegistryFriendlyByteBuf, ClientboundRenameAnimalPayload> CODEC = StreamCodec.composite(ByteBufCodecs.VAR_INT, ClientboundRenameAnimalPayload::entityId, ClientboundRenameAnimalPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
