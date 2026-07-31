package com.austin.civicnations.client.screen;

import com.austin.civicnations.menu.CoinPurseMenu;
import com.austin.civicnations.network.ModNetwork;
import com.austin.civicnations.network.PurseActionPacket;
import com.austin.civicnations.util.CoinDenomination;
import com.austin.civicnations.util.PurseStorage;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public final class CoinPurseScreen extends AbstractContainerScreen<CoinPurseMenu> {
    private final List<Button> withdrawButtons = new ArrayList<>();

    public CoinPurseScreen(CoinPurseMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 300;
        imageHeight = 254;
    }

    @Override
    protected void init() {
        super.init();
        addRenderableWidget(Button.builder(Component.literal("Deposit Matching Coins"),
                        ignored -> send(PurseActionPacket.Action.DEPOSIT_ALL, 0))
                .bounds(leftPos + 18, topPos + 132, 166, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Unbind Empty Purse"),
                        ignored -> send(PurseActionPacket.Action.UNBIND, 0))
                .bounds(leftPos + 190, topPos + 132, 92, 20).build());

        CoinDenomination[] denominations = CoinDenomination.values();
        for (int index = 0; index < denominations.length; index++) {
            CoinDenomination denomination = denominations[index];
            Button button = Button.builder(Component.literal("Withdraw"),
                            ignored -> send(PurseActionPacket.Action.WITHDRAW,
                                    denomination.value()))
                    .bounds(leftPos + 211, topPos + 34 + index * 16, 71, 14).build();
            withdrawButtons.add(addRenderableWidget(button));
        }
    }

    private void send(PurseActionPacket.Action action, int denomination) {
        ModNetwork.CHANNEL.sendToServer(new PurseActionPacket(
                menu.containerId, menu.purseId(), action, denomination));
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        ItemStack purse = menu.purseStack();
        CoinDenomination[] denominations = CoinDenomination.values();
        for (int index = 0; index < withdrawButtons.size(); index++) {
            withdrawButtons.get(index).active = !purse.isEmpty()
                    && PurseStorage.count(purse, denominations[index]) > 0L;
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (minecraft != null && minecraft.options.keyInventory.matches(keyCode, scanCode)) {
            onClose();
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
        graphics.fill(leftPos + 6, topPos + 26, leftPos + imageWidth - 6,
                topPos + imageHeight - 6, 0xFF292F37);
        graphics.drawCenteredString(font, "COIN PURSE", leftPos + imageWidth / 2,
                topPos + 9, 0xFFFFFF);

        ItemStack purse = menu.purseStack();
        String binding = PurseStorage.isBound(purse)
                ? PurseStorage.currencyName(purse) + " [" + PurseStorage.code(purse) + "]"
                : "Unbound";
        graphics.drawString(font, "Currency: " + binding,
                leftPos + 18, topPos + 28, 0xD8B77A, false);

        CoinDenomination[] denominations = CoinDenomination.values();
        for (int index = 0; index < denominations.length; index++) {
            CoinDenomination denomination = denominations[index];
            long count = PurseStorage.count(purse, denomination);
            long value = count > Long.MAX_VALUE / denomination.value()
                    ? Long.MAX_VALUE : count * denomination.value();
            int y = topPos + 37 + index * 16;
            graphics.drawString(font, denomination.issuedName(), leftPos + 18, y,
                    0xE4E4E4, false);
            graphics.drawString(font, Long.toString(count), leftPos + 142, y,
                    0xB8D8FF, false);
            graphics.drawString(font, "(" + value + ")", leftPos + 171, y,
                    0x8F98A3, false);
        }

        graphics.drawString(font,
                "Total: " + PurseStorage.totalCoins(purse) + " coins / "
                        + PurseStorage.totalValue(purse) + " value",
                leftPos + 18, topPos + 116, 0xFFD27A, false);

        graphics.drawString(font, "Inventory", leftPos + 69, topPos + 154,
                0xD8D8D8, false);
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                drawSlot(graphics, 69 + column * 18, 163 + row * 18);
            }
        }
        for (int column = 0; column < 9; column++) {
            drawSlot(graphics, 69 + column * 18, 221);
        }
    }

    private void drawSlot(GuiGraphics graphics, int x, int y) {
        graphics.fill(leftPos + x - 1, topPos + y - 1,
                leftPos + x + 17, topPos + y + 17, 0xFF101317);
        graphics.fill(leftPos + x, topPos + y,
                leftPos + x + 16, topPos + y + 16, 0xFF454C56);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        // All labels are positioned against the custom panel in renderBg.
    }
}
