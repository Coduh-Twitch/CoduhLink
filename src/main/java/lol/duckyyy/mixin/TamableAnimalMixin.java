package lol.duckyyy.mixin;

import lol.duckyyy.CoduhLink;
import lol.duckyyy.api.ClientboundRenameAnimalPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TamableAnimal.class)
public class TamableAnimalMixin {
	@Inject(at = @At("TAIL"), method = "tame")
	private void onTame(Player player, CallbackInfo callback) {
        TamableAnimal animal = (TamableAnimal) (Object) this;
        CoduhLink.ANIMALS.put(animal.getId(), false);
        CoduhLink.RENAMING_ANIMAL = animal.getId();
        CoduhLink.LOGGER.info(String.format("%s tamed %s", player.getName().getString(), animal.getName().getString()));
        for(ServerPlayer p : PlayerLookup.level((ServerLevel) player.level())) {
            ServerPlayNetworking.send(p, new ClientboundRenameAnimalPayload(animal.getId()));
            player.level().getServer().tickRateManager().setFrozen(true);
        }
    }
}