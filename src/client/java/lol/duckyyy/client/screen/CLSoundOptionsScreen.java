package lol.duckyyy.client.screen;

import net.minecraft.ChatFormatting;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.Options;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.OptionsSubScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;

import java.util.Arrays;
import java.util.Collections;
import java.util.Objects;

public class CLSoundOptionsScreen extends OptionsSubScreen {
    private boolean showAll;

    public CLSoundOptionsScreen(Screen lastScreen, Options options) {
        this(lastScreen, options, false);
    }

    public CLSoundOptionsScreen(Screen lastScreen, Options options, boolean showAll) {
        super(lastScreen, options, Component.translatable("options.sounds.title"));
        this.showAll = showAll;

    }

    private OptionInstance<?>[] getAllSoundOptionsExceptMaster() {
        return Arrays.stream(SoundSource.values()).filter(s -> s != SoundSource.MASTER).map(s -> Objects.requireNonNull(this.options).getSoundSourceOptionInstance(s)).toArray(OptionInstance[]::new);
    }

    private void updateOptions() {
        OptionInstance<Boolean> toggleShowAll = OptionInstance.createBoolean("Advanced Sound Options", this.showAll, val -> {
            this.minecraft.gui.setScreen(new CLSoundOptionsScreen(this.lastScreen, this.minecraft.options, val));
        });

        if(!showAll) this.list.addHeader(Component.literal("Access Non-Recommended Options").withStyle(ChatFormatting.YELLOW));
        this.list.addBig(toggleShowAll);
        this.list.addHeader(Component.literal("Change this instead of individual sources").withStyle(ChatFormatting.GREEN));
        this.list.addBig(this.options.getSoundSourceOptionInstance(SoundSource.MASTER));

        if(this.showAll) {
            this.list.addHeader(Component.literal("Extra audio settings").withStyle(ChatFormatting.GRAY));
            this.list.addBig(this.options.soundDevice());
            this.list.addSmall(new OptionInstance[]{this.options.showSubtitles(), this.options.directionalAudio()});
            this.list.addSmall(new OptionInstance[]{this.options.musicFrequency(), this.options.musicToast()});
        }

        if(showAll) {
            this.list.addHeader(Component.literal("It is recommended to leave these alone.").withStyle(ChatFormatting.YELLOW, ChatFormatting.ITALIC));
        }

        if (this.showAll) {
            this.list.addSmall(this.getAllSoundOptionsExceptMaster());
        } else {
            this.list.addHeader(Component.literal("Output Device").withStyle(ChatFormatting.GREEN));
            this.list.addBig(this.options.soundDevice());
            this.list.addHeader(Component.literal("Subtitles can be useful for locating sounds").withStyle(ChatFormatting.GREEN));
            this.list.addBig(this.options.showSubtitles());
        }


    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        super.extractRenderState(graphics, mouseX, mouseY, a);

        graphics.centeredText(this.font, Component.literal("Menu modified by CoduhLink").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC), this.width/2, this.height - 50, 0xFFFFFFFF);
    }

    @Override
    protected void addOptions() {
        updateOptions();
    }
}
