package com.austin.civicnations.client.screen;

import com.austin.civicnations.menu.CoinPressMenu;
import com.austin.civicnations.network.ModNetwork;
import com.austin.civicnations.network.PressCoinsPacket;
import com.austin.civicnations.util.CoinPressRecipe;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import java.util.Optional;

public final class CoinPressScreen extends AbstractContainerScreen<CoinPressMenu> {
    private Button powerButton;

    public CoinPressScreen(CoinPressMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 286;
        this.imageHeight = 260;
    }

    @Override
    protected void init() {
        super.init();
        powerButton = addRenderableWidget(Button.builder(Component.empty(), ignored -> toggleMachine())
                .bounds(leftPos + 82, topPos + 124, 122, 20).build());
        refreshButton();
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        refreshButton();
    }

    private void refreshButton() {
        if (powerButton != null) {
            powerButton.setMessage(Component.literal(
                    menu.isRunning() ? "Stop Machine" : "Start Machine"));
        }
    }

    private void toggleMachine() {
        ModNetwork.CHANNEL.sendToServer(new PressCoinsPacket(
                menu.containerId,
                menu.pressPos()
        ));
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (minecraft != null && minecraft.options.keyInventory.matches(keyCode, scanCode)) {
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0xEE20242A);
        graphics.fill(leftPos + 6, topPos + 28, leftPos + imageWidth - 6,
                topPos + imageHeight - 6, 0xFF292F37);
        graphics.drawCenteredString(font, "COIN PRESS", leftPos + imageWidth / 2,
                topPos + 10, 0xFFFFFF);

        drawSlot(graphics, 62, 54);
        drawSlot(graphics, 86, 54);
        drawSlot(graphics, 220, 54);
        graphics.drawCenteredString(font, "Materials", leftPos + 82, topPos + 38,
                0xB8D8FF);
        graphics.drawString(font, "Output", leftPos + 213, topPos + 38,
                0xB8D8FF, false);

        drawProgress(graphics);
        drawRecipeText(graphics);

        graphics.fill(leftPos + 56, topPos + 164, leftPos + 230, topPos + 251, 0xFF22272E);
        graphics.drawString(font, "Inventory", leftPos + 62, topPos + 157, 0xD8D8D8, false);
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                drawSlot(graphics, 62 + column * 18, 169 + row * 18);
            }
        }
        for (int column = 0; column < 9; column++) {
            drawSlot(graphics, 62 + column * 18, 227);
        }
    }

    private void drawProgress(GuiGraphics graphics) {
        int x = leftPos + 122;
        int y = topPos + 55;
        int width = 78;
        graphics.fill(x, y, x + width, y + 14, 0xFF111419);
        graphics.fill(x + 2, y + 2, x + width - 2, y + 12, 0xFF454C56);
        if (menu.isRunning()) {
            int filled = menu.progressWidth(width - 4);
            graphics.fill(x + 2, y + 2, x + 2 + filled, y + 12, 0xFFD19A43);
        }
    }

    private void drawRecipeText(GuiGraphics graphics) {
        Optional<CoinPressRecipe> recipe = CoinPressRecipe.find(
                menu.pressContainer().getItem(0), menu.pressContainer().getItem(1));
        if (recipe.isEmpty()) {
            graphics.drawCenteredString(font, "Insert a valid coin recipe",
                    leftPos + imageWidth / 2, topPos + 84, 0xC7C7C7);
            graphics.drawCenteredString(font, "Then press Start Machine",
                    leftPos + imageWidth / 2, topPos + 98, 0x8F98A3);
            return;
        }

        drawCenteredFittedString(graphics, "Recipe: " + recipe.get().inputText(),
                leftPos + imageWidth / 2, topPos + 82, 250, 0xD8B77A);
        drawCenteredFittedString(graphics, "Produces: " + recipe.get().outputText(),
                leftPos + imageWidth / 2, topPos + 96, 250, 0xD8B77A);
        graphics.drawCenteredString(font, menu.isRunning() ? "Machine running" : "Ready",
                leftPos + imageWidth / 2, topPos + 110,
                menu.isRunning() ? 0x7FD98A : 0xB8D8FF);
    }

    private void drawCenteredFittedString(GuiGraphics graphics, String text, int centerX,
                                          int y, int maximumWidth, int color) {
        int textWidth = font.width(text);
        if (textWidth <= maximumWidth) {
            graphics.drawCenteredString(font, text, centerX, y, color);
            return;
        }
        float scale = maximumWidth / (float) textWidth;
        graphics.pose().pushPose();
        graphics.pose().translate(centerX, y, 0.0F);
        graphics.pose().scale(scale, scale, 1.0F);
        graphics.drawCenteredString(font, text, 0, 0, color);
        graphics.pose().popPose();
    }

    private void drawSlot(GuiGraphics graphics, int x, int y) {
        graphics.fill(leftPos + x - 1, topPos + y - 1,
                leftPos + x + 17, topPos + y + 17, 0xFF101317);
        graphics.fill(leftPos + x, topPos + y,
                leftPos + x + 16, topPos + y + 16, 0xFF454C56);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        // Labels are rendered in renderBg so vanilla inventory labels never appear.
    }
}
