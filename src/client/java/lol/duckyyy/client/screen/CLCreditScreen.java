package lol.duckyyy.client.screen;

import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.ConfirmLinkScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.WinScreen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.CommonLinks;

import java.net.URI;

public class CLCreditScreen extends Screen {
    private static final int BUTTON_SPACING = 8;
    private static final int BUTTON_WIDTH = 210;
    private static final Component TITLE = Component.translatable("credits_and_attribution.screen.title");
    private static final Component CREDITS_BUTTON = Component.translatable("credits_and_attribution.button.credits");
    private static final Component ATTRIBUTION_BUTTON = Component.translatable("credits_and_attribution.button.attribution");
    private static final Component LICENSES_BUTTON = Component.translatable("credits_and_attribution.button.licenses");
    private static final Component SOURCE_BUTTON = Component.literal("CoduhLink Source Code");
    private Screen lastScreen;
    private final HeaderAndFooterLayout layout = new HeaderAndFooterLayout(this);

    public CLCreditScreen() {
        super(Component.literal("Credits & Attribution"));
    }

    protected void init() {
        super.init();
        this.layout.addTitleHeader(TITLE, this.font);
        LinearLayout content = ((LinearLayout)this.layout.addToContents(LinearLayout.vertical())).spacing(8);
        content.defaultCellSetting().alignHorizontallyCenter();
        content.addChild(Button.builder(CREDITS_BUTTON, (button) -> this.openCreditsScreen()).width(210).build());
        content.addChild(Button.builder(ATTRIBUTION_BUTTON, ConfirmLinkScreen.confirmLink(this, CommonLinks.ATTRIBUTION)).width(210).build());
        content.addChild(Button.builder(LICENSES_BUTTON, ConfirmLinkScreen.confirmLink(this, CommonLinks.LICENSES)).width(210).build());
        content.addChild(Button.builder(SOURCE_BUTTON, ConfirmLinkScreen.confirmLink(this, URI.create("https://github.com/Coduh-Twitch/CoduhLink"))).width(210).build());
        this.layout.addToFooter(Button.builder(CommonComponents.GUI_DONE, (button) -> this.onClose()).width(200).build());
        this.layout.arrangeElements();
        this.layout.visitWidgets(this::addRenderableWidget);

        this.lastScreen = new CLTitleScreen();
    }

    protected void repositionElements() {
        this.layout.arrangeElements();
    }

    private void openCreditsScreen() {
        this.minecraft.gui.setScreen(new WinScreen(false, () -> this.minecraft.gui.setScreen(this)));
    }

    public void onClose() {
        this.minecraft.gui.setScreen(this.lastScreen);
    }
}
