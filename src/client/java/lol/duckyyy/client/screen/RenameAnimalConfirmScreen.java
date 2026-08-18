package lol.duckyyy.client.screen;

import lol.duckyyy.CoduhLink;
import net.minecraft.ChatFormatting;
import net.minecraft.client.InputType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.InputWithModifiers;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import net.minecraft.server.dialog.Input;
import net.minecraft.server.dialog.input.InputControl;
import net.minecraft.server.dialog.input.InputControlTypes;
import net.minecraft.world.entity.TamableAnimal;
import org.jetbrains.annotations.NotNull;

public class RenameAnimalConfirmScreen extends Screen {
    private  Component message;
    private final Component subheading;
    private final Component chosenName;
    private Button reroll;
    private Button cancel;
    private int clicks = 0;
    private String cancelText = "Cancel";
    private int animalId;
    private boolean open = true;

    public RenameAnimalConfirmScreen(Component message, Component subheading, Component chosenName, int animalId) {
        super(Component.empty());
        this.message = message;
        this.subheading = subheading;
        this.chosenName = chosenName;
        this.animalId = animalId;
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
    protected void init() {
        super.init();


        int buttonWidth = 150;
        int buttonHeight = 20;
        int buttonX = (this.width / 2) - (buttonWidth/2);
        int buttonY = ((this.height / 2) + 60);

        this.cancel = this.addRenderableWidget(Button.builder(Component.literal(this.cancelText).withColor(TextColor.RED), b -> {
            clicks+=1;
            if(clicks < 2) {
                this.cancelText = "Are you Sure? (Click again)";
                b.setWidth(b.getWidth() + 20);
                b.setX((this.width / 2) - (b.getWidth()/2));
                b.setMessage(Component.literal(this.cancelText).withColor(TextColor.YELLOW));
                b.setFocused(false);
            } else CoduhLink.ANIMALS.put(animalId, true);
        }).bounds(buttonX, buttonY, buttonWidth, buttonHeight).build());

    }

    @Override
    public void extractRenderState(@NotNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        super.extractRenderState(graphics, mouseX, mouseY, delta);
        graphics.centeredText(this.font, this.message, this.width/2, this.height/2, 0xFFFFFFFF);
        graphics.centeredText(this.font, this.subheading, this.width/2, (this.height/2)+20, 0xFFFFFFFF);
        if (this.reroll != null) {
            graphics.centeredText(this.font, chosenName, this.width/2, (this.height/2)+40, 0xFFFFFFFF);
        }
    }

    @Override
    public void tick() {
        super.tick();
        if(!CoduhLink.ANIMALS.containsKey(animalId) && this.open) {
            CoduhLink.LOGGER.info("Closing GUI...");
            this.open = false;
            Minecraft.getInstance().gui.setScreen(null);
        }
    }
}
