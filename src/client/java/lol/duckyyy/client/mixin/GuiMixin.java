package lol.duckyyy.client.mixin;

import lol.duckyyy.client.screen.CLCreditScreen;
import lol.duckyyy.client.screen.CLPauseScreen;
import lol.duckyyy.client.screen.CLTitleScreen;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.screens.CreditsAndAttributionScreen;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Gui.class)
public abstract class GuiMixin {
    @Shadow
    public abstract void setScreen(Screen screen);

    @Inject(method = "setScreen", at = @At("HEAD"), cancellable = true)
    private void interceptSetScreen(Screen screen, CallbackInfo ci) {
        if (screen instanceof TitleScreen) {
            ci.cancel();
            this.setScreen(new CLTitleScreen());
        } else if (screen instanceof CreditsAndAttributionScreen) {
            ci.cancel();
            this.setScreen(new CLCreditScreen());
        } else if (screen instanceof PauseScreen) {
            ci.cancel();
            this.setScreen(new CLPauseScreen());
        }
    }
}
