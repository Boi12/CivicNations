package com.austin.civicnations.client.screen;

import com.austin.civicnations.menu.CoinMintMenu;
import com.austin.civicnations.network.MintCurrencyPacket;
import com.austin.civicnations.network.ModNetwork;
import com.austin.civicnations.util.CoinDenomination;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import java.util.List;

public final class CoinMintScreen extends AbstractContainerScreen<CoinMintMenu> {
    private static final int[] COUNTS = {1, 4, 8, 16, 32, 64};
    private int denominationIndex;
    private int countIndex;
    private Button denominationButton;
    private Button countButton;

    public CoinMintScreen(CoinMintMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 280;
        this.imageHeight = 180;
    }

    @Override
    protected void init() {
        super.init();
        denominationIndex = 0;
        countIndex = 0;

        addRenderableWidget(Button.builder(Component.literal("<"), ignored -> previousDenomination())
                .bounds(leftPos + 20, topPos + 62, 24, 20).build());
        denominationButton = addRenderableWidget(Button.builder(Component.empty(), ignored -> nextDenomination())
                .bounds(leftPos + 48, topPos + 62, 184, 20).build());
        addRenderableWidget(Button.builder(Component.literal(">"), ignored -> nextDenomination())
                .bounds(leftPos + 236, topPos + 62, 24, 20).build());

        addRenderableWidget(Button.builder(Component.literal("-"), ignored -> previousCount())
                .bounds(leftPos + 69, topPos + 94, 24, 20).build());
        countButton = addRenderableWidget(Button.builder(Component.empty(), ignored -> nextCount())
                .bounds(leftPos + 97, topPos + 94, 86, 20).build());
        addRenderableWidget(Button.builder(Component.literal("+"), ignored -> nextCount())
                .bounds(leftPos + 187, topPos + 94, 24, 20).build());

        addRenderableWidget(Button.builder(Component.literal("Mint National Coins"), ignored -> mint())
                .bounds(leftPos + 68, topPos + 140, 144, 20).build());
        refreshLabels();
    }

    private void previousDenomination() {
        List<Integer> denominations = menu.denominations();
        if (denominations.isEmpty()) {
            return;
        }
        denominationIndex = Math.floorMod(denominationIndex - 1, denominations.size());
        refreshLabels();
    }

    private void nextDenomination() {
        List<Integer> denominations = menu.denominations();
        if (denominations.isEmpty()) {
            return;
        }
        denominationIndex = (denominationIndex + 1) % denominations.size();
        refreshLabels();
    }

    private void previousCount() {
        countIndex = Math.floorMod(countIndex - 1, COUNTS.length);
        refreshLabels();
    }

    private void nextCount() {
        countIndex = (countIndex + 1) % COUNTS.length;
        refreshLabels();
    }

    private void refreshLabels() {
        int denomination = selectedDenomination();
        String issuedName = CoinDenomination.fromValue(denomination)
                .map(type -> type.issuedName(menu.currencyCode()))
                .orElse(denomination + " " + menu.currencyCode() + " Coin");
        denominationButton.setMessage(Component.literal(issuedName));
        countButton.setMessage(Component.literal("Coins: " + COUNTS[countIndex]));
    }

    private int selectedDenomination() {
        if (menu.denominations().isEmpty()) {
            return 1;
        }
        return menu.denominations().get(Math.min(denominationIndex,
                menu.denominations().size() - 1));
    }

    private void mint() {
        ModNetwork.CHANNEL.sendToServer(new MintCurrencyPacket(
                menu.containerId,
                menu.mintPos(),
                selectedDenomination(),
                COUNTS[countIndex]
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
        graphics.fill(leftPos + 6, topPos + 30, leftPos + imageWidth - 6,
                topPos + imageHeight - 6, 0xFF292F37);
        graphics.drawCenteredString(font, "COIN MINT", leftPos + imageWidth / 2,
                topPos + 12, 0xFFFFFF);
        graphics.drawCenteredString(font, "Stamp matching blanks as official national currency.",
                leftPos + imageWidth / 2, topPos + 40, 0xB8D8FF);
        CoinDenomination.fromValue(selectedDenomination()).ifPresent(type ->
                graphics.drawCenteredString(font, "Requires: " + type.blankName(),
                        leftPos + imageWidth / 2, topPos + 121, 0xD8B77A));
        graphics.drawCenteredString(font,
                "Leader/Treasurer permission and claimed national territory required.",
                leftPos + imageWidth / 2, topPos + 165, 0xA9D18E);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        // Labels are rendered in renderBg so vanilla inventory labels never appear.
    }
}
