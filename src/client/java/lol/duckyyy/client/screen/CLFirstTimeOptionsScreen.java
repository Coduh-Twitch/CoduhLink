package lol.duckyyy.client.screen;

import lol.duckyyy.CoduhLink;
import lol.duckyyy.client.CoduhLinkClient;
import lol.duckyyy.util.PlayedBefore;
import net.minecraft.ChatFormatting;
import net.minecraft.client.*;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.components.toasts.NowPlayingToast;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.client.gui.components.toasts.ToastManager;
import net.minecraft.client.gui.layouts.FrameLayout;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.MusicManager;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.*;
import net.minecraft.util.ARGB;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.Nullable;

import java.lang.reflect.Field;
import java.util.*;

public class CLFirstTimeOptionsScreen extends Screen {
    private int textY = 30;
    private Map<String, Boolean> optionsToSet = new HashMap<>();
    private Map<String, String> optionIdKeyMap = new HashMap<>();
    private List<String> renderedWarnings = new ArrayList<>();
    private FontDescription icons;
    private int recommendedRenderDistance = 12;
    private int warnings = 0;
    private int recommendedRefreshRate = 0;
    private int musicVolume = 0;
    private int newMusicVolume = musicVolume;
    private Music backgroundMusic;

    public CLFirstTimeOptionsScreen() {
        super(Component.literal("First Time Options"));

        this.backgroundMusic = CoduhLinkClient.SUBWOOFER_LULLABY;
    }

    @Override
    public boolean canInterruptWithAnotherScreen() {
        return false;
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    @Override
    public @Nullable Music getBackgroundMusic() {
        return this.backgroundMusic;
    }

    @Override
    public void init() {
        super.init();

        this.optionIdKeyMap.put("vsync", "enableVsync");
        this.optionIdKeyMap.put("framerateLimit", "maxFps");
        this.optionIdKeyMap.put("music", "soundCategory_music");


        if(this.minecraft.getMusicManager().getCurrentMusicTranslationKey() != null && !this.minecraft.getMusicManager().getCurrentMusicTranslationKey().equalsIgnoreCase(CoduhLink.MOD_ID + ".subwoofer_lullaby")) {
            this.minecraft.getSoundManager().stop(null, SoundSource.MUSIC);
            this.minecraft.getMusicManager().stopPlaying();
            this.minecraft.getMusicManager().startPlaying(this.backgroundMusic);
            this.minecraft.getMusicManager().setMinutesBetweenSongs(MusicManager.MusicFrequency.CONSTANT);
            this.minecraft.gui.toastManager().setMusicToastDisplayState(MusicToastDisplayState.PAUSE_AND_TOAST);
            this.minecraft.gui.toastManager().addToast(new NowPlayingToast());
        }
        this.minecraft.getMusicManager().showNowPlayingToastIfNeeded();

//        SystemToast.add(this.minecraft.gui.toastManager(), SystemToast.SystemToastId.);

        this.musicVolume = (int) (this.minecraft.options.getSoundSourceVolume(SoundSource.MUSIC) * 100);
        this.newMusicVolume = this.musicVolume;
        this.recommendedRefreshRate = Objects.requireNonNull(this.minecraft.getWindow().findBestMonitor()).currentMode().getRefreshRate();
        this.icons = new FontDescription.Resource(Identifier.fromNamespaceAndPath(CoduhLink.MOD_ID, "icons"));
        int textY = this.textY + 150;

        this.minecraft.options.guiScale().set(2);



        this.drawOptionList(textY);
    }

    public Object getOptionValue(String fieldName) {
        try {
            Field field = this.minecraft.options.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            Object optionField = field.get(Minecraft.getInstance().options);

            if (optionField instanceof OptionInstance<?> optionInstance) {
                return optionInstance.get();
            }
            return optionField;
        } catch (NoSuchFieldException | IllegalAccessException e) {
            CoduhLink.LOGGER.error(e.getMessage());
            return null;
        }
    }

    public Checkbox optionCheckbox(String option, String newValue, boolean defaultValue, int y) {
        boolean initialValue = this.optionsToSet.getOrDefault(option, defaultValue);
        if (!this.optionsToSet.containsKey(option)) this.optionsToSet.put(option, initialValue);

        Object currentOptionValue = getOptionValue(option);
        if(currentOptionValue == null && this.optionIdKeyMap.containsKey(option)) currentOptionValue = getOptionValue(this.optionIdKeyMap.get(option));

        if(currentOptionValue instanceof Boolean) {
            currentOptionValue = ((Boolean) currentOptionValue) ? "Enabled" : "Disabled";
        }

        if(option.equalsIgnoreCase("music")) currentOptionValue = String.valueOf(this.musicVolume) + "%";

        return (Checkbox) Checkbox.builder(Component.literal(String.format("%s: ", Component.translatable(String.format("%s.%s",option.equalsIgnoreCase("music") ? "soundCategory" : "options", option)).getString())).append(Component.literal(String.format("%s", currentOptionValue)).withStyle(ChatFormatting.STRIKETHROUGH)).append(Component.literal(String.format(" -> %s", newValue + (option.equalsIgnoreCase("music") ? "%" : "")))), this.font).selected(initialValue).onValueChange((c, v) -> {
            this.optionsToSet.put(option, v);
            this.clearWidgets();
            this.drawOptionList(y);

            if(option.equalsIgnoreCase("music")) {
                this.minecraft.options.getSoundSourceOptionInstance(SoundSource.MUSIC).set(!v ? 1.0 : 0.0);
                this.newMusicVolume = !v ? 1 : 0;
            }

            // TODO: Remove logs

            for(Map.Entry<String, Boolean> entry : this.optionsToSet.entrySet()) {
                CoduhLink.LOGGER.info(String.format("%s: %s", entry.getKey(), entry.getValue()));
            }
        }).build();
    }

    public void drawOptionList(int y) {
        FrameLayout layout = new FrameLayout(0, y, this.width, this.height-y);
        LinearLayout content = (LinearLayout) layout.addChild(LinearLayout.vertical().spacing(8));
        LinearLayout footer = (LinearLayout) layout.addChild(LinearLayout.horizontal().spacing(8));


        content.addChild(optionCheckbox("framerateLimit", this.recommendedRefreshRate >= 260 ? "Unlimited" : String.valueOf(this.recommendedRefreshRate), true, y));
        if(!((Boolean) getOptionValue(this.optionIdKeyMap.get("vsync")))) content.addChild(optionCheckbox("vsync", "Enabled", true, y));
        if(((int) getOptionValue("renderDistance")) != this.recommendedRenderDistance) content.addChild(optionCheckbox("renderDistance", String.valueOf(this.recommendedRenderDistance), true, y));
        if(((int) getOptionValue("simulationDistance")) != this.recommendedRenderDistance) content.addChild(optionCheckbox("simulationDistance", String.valueOf(this.recommendedRenderDistance), true, y));
        if(((NarratorStatus) getOptionValue("narrator")) != NarratorStatus.OFF) content.addChild(optionCheckbox("narrator", "OFF", true, y));
        content.addChild(optionCheckbox("music", String.valueOf(0), false, y));

        int enabledApplies = this.optionsToSet.values().stream().filter(b -> b).toList().size();
        int applyButtonWidth = enabledApplies > 0 ? 130 : 250;

        TextColor col = TextColor.GREEN;
        if(enabledApplies == 0) col = TextColor.RED;

        Button applyButton = Button.builder(Component.literal(enabledApplies > 0 ? String.format("Apply %s Change%s", enabledApplies, enabledApplies == 1 ? "" : "s") : "Continue Without Changes (Not Recommended)").withColor(col), b -> {
            b.active = false;
            b.setMessage(Component.literal("Applying Changes..."));
            for(String option : this.optionsToSet.keySet()) {
                boolean toSet = this.optionsToSet.get(option);
                if(toSet) {
                    String key = option;
                    if(this.optionIdKeyMap.containsKey(option)) key = this.optionIdKeyMap.get(option);

                    if(key.equalsIgnoreCase("maxFps")) {
                        this.minecraft.options.framerateLimit().set(Math.min(this.recommendedRefreshRate, 260));
                    } else if(key.equalsIgnoreCase("renderDistance")) {
                        this.minecraft.options.renderDistance().set(this.recommendedRenderDistance);
                    } else if(key.equalsIgnoreCase("simulationDistance")) {
                        this.minecraft.options.simulationDistance().set(this.recommendedRenderDistance);
                    } else if(key.equalsIgnoreCase("narrator")) {
                        this.minecraft.options.narrator().set(NarratorStatus.OFF);
                    } else if(key.equalsIgnoreCase("enableVsync")) {
                        this.minecraft.options.enableVsync().set(true);
                    } else if(key.equalsIgnoreCase("soundCategory_music")) {
                        this.minecraft.options.getSoundSourceOptionInstance(SoundSource.MUSIC).set(0.0);
                    }
                }
            }

            PlayedBefore.set(true);
            this.minecraft.gui.setScreen(new CLTitleScreen());
            SystemToast.add(this.minecraft.gui.toastManager(), SystemToast.SystemToastId.FRIEND_SYSTEM_NOTIFICATION, Component.literal("Settings Applied"), Component.literal("Successfully applied recommended settings! You can change these any time from the \"Options...\" button."));
            this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.EXPERIENCE_ORB_PICKUP, 1.0F));
        }).bounds((this.width/2)-(applyButtonWidth/2), this.height-30, applyButtonWidth, 20).build();
        this.addRenderableWidget(applyButton);

        footer.defaultCellSetting().alignVerticallyBottom();

        footer.arrangeElements();
        layout.arrangeElements();
        FrameLayout.centerInRectangle(layout, this.getRectangle());
        layout.visitWidgets(this::addRenderableWidget);
    }

    public void renderWarningText(GuiGraphicsExtractor graphics) {
        this.warnings = 0;
        int textY = this.height - 50;
        int spacer = 24;
        if(this.optionsToSet.containsKey("vsync") && !this.optionsToSet.get("vsync") && !((Boolean) getOptionValue(this.optionIdKeyMap.get("vsync")))) {
            this.warnings = 1;
            graphics.centeredText(this.font, Component.empty().append(Component.literal("\uE000").withStyle(s -> s.withFont(this.icons))).append(Component.literal(" You should enable VSync for a smoother experience!").withStyle(ChatFormatting.ITALIC, ChatFormatting.YELLOW)), this.width/2, textY, 0xFFFFFFFF);
            textY-=spacer;
        }

        if(this.optionsToSet.containsKey("renderDistance") && !this.optionsToSet.get("renderDistance") && ((int) getOptionValue("renderDistance")) > this.recommendedRenderDistance) {
            this.warnings = 1;
            graphics.centeredText(this.font, Component.empty().append(Component.literal("\uE000").withStyle(s -> s.withFont(this.icons))).append(Component.literal(" Setting your render distance higher than 12 may negatively impact performance while streaming.").withStyle(ChatFormatting.ITALIC, ChatFormatting.YELLOW)), this.width/2, textY, 0xFFFFFFFF);
            textY-=spacer;
        }

        if(this.optionsToSet.containsKey("simulationDistance") && !this.optionsToSet.get("simulationDistance") && ((int) getOptionValue("simulationDistance")) != this.recommendedRenderDistance) {
            this.warnings = 1;
            graphics.centeredText(this.font, Component.empty().append(Component.literal("\uE000").withStyle(s -> s.withFont(this.icons))).append(Component.literal(" Simulation distance should match render distance.").withStyle(ChatFormatting.ITALIC, ChatFormatting.YELLOW)), this.width/2, textY, 0xFFFFFFFF);
            textY-=spacer;
        }

        if(this.optionsToSet.containsKey("framerateLimit") && !this.optionsToSet.get("framerateLimit") && ((int) getOptionValue("framerateLimit")) != this.recommendedRefreshRate) {
            this.warnings = 1;
            graphics.centeredText(this.font, Component.empty().append(Component.literal("\uE000").withStyle(s -> s.withFont(this.icons))).append(Component.literal(String.format(" Your max framerate should be at or slightly above your monitor's refresh rate (%s detected).", this.recommendedRefreshRate)).withStyle(ChatFormatting.ITALIC, ChatFormatting.YELLOW)), this.width/2, textY, 0xFFFFFFFF);
            textY-=spacer;
        }

        if(this.optionsToSet.containsKey("music") && !this.optionsToSet.get("music") && this.musicVolume != 0) {
            graphics.centeredText(this.font, Component.empty().append(Component.literal("\uE000").withStyle(s -> s.withFont(this.icons))).append(Component.literal(" You should mute the in-game music, as it can be quite loud at times. Alternatively, mute your music and keep it on instead.").withStyle(ChatFormatting.ITALIC, ChatFormatting.YELLOW)), this.width/2, textY, 0xFFFFFFFF);
            textY-=spacer;
        }

    }

    @Override
    public void extractRenderState(@NotNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        super.extractRenderState(graphics, mouseX, mouseY, delta);
        int textY = this.textY;
        graphics.centeredText(this.font, Component.literal("Welcome!").withStyle(ChatFormatting.BOLD, ChatFormatting.GREEN), this.width/2, textY, 0xFFFFFFFF);
        textY+=20;
        graphics.centeredText(this.font, Component.literal("It looks like it's your first time opening the game with CoduhLink installed."), this.width/2, textY, 0xFFFFFFFF);
        textY+=20;
        graphics.centeredText(this.font, Component.literal("Would you like to apply the recommended game settings?").withStyle(ChatFormatting.ITALIC), this.width/2, textY, 0xFFFFFFFF);
        textY+=30;

        this.renderWarningText(graphics);
    }
}
