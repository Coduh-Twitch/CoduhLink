package lol.duckyyy.client.screen;

import com.github.twitch4j.helix.domain.ChannelInformation;
import com.github.twitch4j.helix.domain.ChannelInformationList;
import lol.duckyyy.CoduhLink;
import lol.duckyyy.client.CoduhLinkClient;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.layouts.FrameLayout;
import net.minecraft.client.gui.layouts.GridLayout;
import net.minecraft.client.gui.screens.MultiplayerOptionsScreen;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.achievement.StatsScreen;
import net.minecraft.client.gui.screens.advancements.AdvancementsScreen;
import net.minecraft.client.gui.screens.options.OptionsScreen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.function.Supplier;

public class CLPauseScreen extends Screen {
    public CLPauseScreen() {
        super(Component.translatable("menu.game"));
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return true;
    }

    @Override
    public boolean isPauseScreen() {
        return true;
    }

    private Button openScreenButton(final Component message, final Supplier<Screen> newScreen) {
        return Button.builder(message, (var2) -> this.minecraft.gui.setScreen((Screen)newScreen.get())).width(98).build();
    }

    @Override
    public void init() {
        super.init();
        GridLayout gridLayout = new GridLayout();
        gridLayout.defaultCellSetting().padding(4, 4, 4, 0);
        GridLayout.RowHelper helper = gridLayout.createRowHelper(2);

        helper.addChild(Button.builder(Component.translatable("menu.returnToGame"), (button) -> {
            this.minecraft.gui.setScreen((Screen)null);
            this.minecraft.mouseHandler.grabMouse();
        }).width(204).build(), 2, gridLayout.newCellSettings().paddingTop(50));
        helper.addChild(this.openScreenButton(Component.translatable("gui.advancements"), () -> new AdvancementsScreen(this.minecraft.player.connection.getAdvancements(), this)));
        helper.addChild(this.openScreenButton(Component.translatable("gui.stats"), () -> new StatsScreen(this, this.minecraft.player.getStats())));

        if (this.minecraft.hasSingleplayerServer()) {
            helper.addChild(this.openScreenButton(Component.translatable("menu.options"), () -> new OptionsScreen(this, this.minecraft.options, true)));
            Button lanButton = this.openScreenButton(Component.translatable("menu.multiplayerOptions.button"), () -> new MultiplayerOptionsScreen(this));
            lanButton.active = false;
            helper.addChild(lanButton);
        } else {
            helper.addChild(Button.builder(Component.translatable("menu.options"), (var1) -> this.minecraft.gui.setScreen(new OptionsScreen(this, this.minecraft.options, true))).width(204).build(), 2);
        }

        helper.addChild(Button.builder(CommonComponents.disconnectButtonLabel(this.minecraft.isLocalServer()), (button) -> {
            button.active = false;
            this.minecraft.getReportingContext().draftReportHandled(this.minecraft, this, () -> this.minecraft.disconnectFromWorld(ClientLevel.DEFAULT_QUIT_MESSAGE), true);
        }).width(204).build(), 2);

        gridLayout.arrangeElements();
        FrameLayout.alignInRectangle(gridLayout, 0, 0, this.width, this.height, 0.5F, 0.25F);
        gridLayout.visitWidgets(this::addRenderableWidget);
    }

    @Override
    public void extractRenderState(@NotNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        super.extractRenderState(graphics, mouseX, mouseY, delta);
        graphics.centeredText(this.font, Component.literal("Running CoduhLink by ducky").withStyle(ChatFormatting.ITALIC, ChatFormatting.DARK_AQUA), this.width/2, this.height-20, 0xFFFFFFFF);
        try {
            ChannelInformation channel = CoduhLink.twitchClient.getHelix().getChannelInformation(CoduhLinkClient.ACCESS_TOKEN, List.of(CoduhLinkClient.USER_ID)).execute().getChannels().getFirst();

            graphics.centeredText(this.font, Component.literal(String.format("⬤ Twitch Connected to #%s", channel.getBroadcasterName())).withStyle(ChatFormatting.BOLD).withColor(0xA970FF), this.width/2, this.height-40, 0xFFFFFFFF);
        } catch(Exception e) {
            graphics.centeredText(this.font, Component.literal("⬤ Twitch Disconnected").withStyle(ChatFormatting.BOLD, ChatFormatting.RED), this.width/2, this.height-40, 0xFFFFFFFF);

        }
    }
}
