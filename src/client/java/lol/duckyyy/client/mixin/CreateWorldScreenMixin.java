package lol.duckyyy.client.mixin;

import lol.duckyyy.client.screen.CLTitleScreen;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(CreateWorldScreen.class)
public class CreateWorldScreenMixin {
    @Redirect(method = "popScreen", at = @At(value = "INVOKE", target = "Ljava/lang/Runnable;run()V"))
    private void redirectPopScreen(Runnable onClose) {
        net.minecraft.client.Minecraft.getInstance().gui.setScreen(new CLTitleScreen());
    }
}
