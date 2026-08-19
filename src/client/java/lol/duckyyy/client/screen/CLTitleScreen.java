package lol.duckyyy.client.screen;

import lol.duckyyy.CoduhLink;
import net.minecraft.ChatFormatting;
import net.minecraft.SharedConstants;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.LogoRenderer;
import net.minecraft.client.gui.components.PlainTextButton;
import net.minecraft.client.gui.components.SplashRenderer;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.CreditsAndAttributionScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.OptionsScreen;
import net.minecraft.client.gui.screens.worldselection.SelectWorldScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import net.minecraft.util.ARGB;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class CLTitleScreen extends Screen {
    public String COPYRIGHT_TEXT = "CoduhLink by ducky | © Mojang AB.";
    public String VERSION_STRING = "Minecraft {v} (Fabric)";
    public LogoRenderer logoRenderer;
    public SplashRenderer splash;
    public String statusText = "Connecting to Twitch...";
    public GuiGraphicsExtractor graphicsRenderer;
    private int ticks = 0;
    private int nextTopPos = 0;
    private int worldCount = 0;
    List<GuiEventListener> widgets = new ArrayList<GuiEventListener>();

    public CLTitleScreen() {
        super(Component.literal("CoduhLink"));
        this.logoRenderer = (LogoRenderer) Objects.requireNonNullElseGet(logoRenderer, () -> new LogoRenderer(false));
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void init() {
        super.init();
        if (this.splash == null) {
            this.splash = this.minecraft.gui.splashManager().getSplash();
        }

        java.nio.file.Path savesPath = net.minecraft.client.Minecraft.getInstance().gameDirectory.toPath().resolve("saves");

        if (java.nio.file.Files.exists(savesPath)) {
            try (java.util.stream.Stream<java.nio.file.Path> stream = java.nio.file.Files.list(savesPath)) {
                this.worldCount = (int) stream.filter(path ->
                        java.nio.file.Files.isDirectory(path) && java.nio.file.Files.exists(path.resolve("level.dat"))
                ).count();
            } catch (java.io.IOException ignored) {}
        }
    }


    private void createLogoAndSplash(GuiGraphicsExtractor graphics) {
        this.logoRenderer.extractRenderState(graphics, this.width, 1.0F);
        if (this.splash != null && !(Boolean)this.minecraft.options.hideSplashTexts().get()) {
            this.splash.extractRenderState(graphics, this.width, this.font, 1.0F);
        }
    }

    private void createMenuAndFooter(int topPos, final int spacing, GuiGraphicsExtractor graphics) {
        int copyrightWidth = this.font.width(COPYRIGHT_TEXT);
        int copyrightX = this.width - copyrightWidth - 2;

        Button.Builder optionsButton = Button.builder(Component.translatable("menu.options").withColor(TextColor.YELLOW), (var1) -> this.minecraft.gui.setScreen(new OptionsScreen(this, this.minecraft.options, false)));
        int var10002 = this.width / 2 - 100;
        Button singleplayerButton = (Button)this.addRenderableWidget(Button.builder((worldCount > 0 ? Component.translatable("menu.singleplayer") : Component.literal("Create New World")).withColor(TextColor.GREEN), (var1) -> this.minecraft.gui.setScreen(new SelectWorldScreen(this))).bounds(this.width / 2 - 100, topPos, 200, 20).build());
        topPos += spacing;
        this.addRenderableWidget(optionsButton.bounds(var10002, topPos, 98, 20).build());
        this.addRenderableWidget(Button.builder(Component.translatable("menu.quit").withColor(TextColor.RED), (var1) -> this.minecraft.stop()).bounds(this.width / 2 + 2, topPos, 98, 20).build());

        topPos+=spacing;
        this.nextTopPos = topPos;
        this.addRenderableWidget(new PlainTextButton(copyrightX, this.height - 10, copyrightWidth, 10, Component.literal(COPYRIGHT_TEXT), (var1) -> this.minecraft.gui.setScreen(new CreditsAndAttributionScreen(this)), this.font));
        graphics.text(this.font, VERSION_STRING.replace("{v}", SharedConstants.getCurrentVersion().name()), 2, this.height - 10, ARGB.white(100));
        this.createTwitchStatusLabels(false);

    }

    private void createTwitchStatusLabels(boolean extraDisabled) {
        for(GuiEventListener widget : this.widgets) {
            this.removeWidget(widget);
        }
        Button statusButton = (Button)this.addRenderableWidget(Button.builder(Component.literal(this.statusText), (var1) -> {
         // button clicked

        }).bounds(this.width / 2 - 100, this.nextTopPos, 200, 20).build());
        statusButton.active = false;
        if(extraDisabled) statusButton.setAlpha(0.1F);

        if(!extraDisabled) {
            Button eventSubStatusButton = (Button)this.addRenderableWidget(Button.builder(Component.literal("EventSub Connected!"), (var1) -> {
                // button clicked

            }).bounds(this.width / 2 - 100, this.nextTopPos+24, 200, 20).build());
            eventSubStatusButton.active = false;
            this.widgets.add(eventSubStatusButton);
        }

        this.widgets.add(statusButton);
    }

    @Override
    public void extractRenderState(@NotNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        super.extractRenderState(graphics, mouseX, mouseY, delta);

//        graphics.blit(MINECRAFT_LOGO, this.width/2,100,0,0,256,64,256,64);
        this.createLogoAndSplash(graphics);
        this.createMenuAndFooter(this.height/4 + 48, 24, graphics);
    }

    @Override
    public void tick() {
        this.ticks+=1;
        if(this.ticks == 20) {
            this.ticks = 0;
            if(!CoduhLink.TWITCH_ENABLED) {
                this.statusText = "Twitch Features Disabled";
                this.createTwitchStatusLabels(true);
            } else {
                if(CoduhLink.twitchClient.getChat().getChannels().isEmpty()) {
                    this.statusText = "Failed to Connect to Twitch Chat";
                } else {
                    String channel = CoduhLink.twitchClient.getChat().getChannels().stream().findFirst().get();
                    this.statusText = String.format("Connected to twitch.tv/%s", channel);
                }
                this.createTwitchStatusLabels(false);
            }
        }
    }
}
