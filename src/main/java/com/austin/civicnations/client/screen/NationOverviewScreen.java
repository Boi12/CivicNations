package com.austin.civicnations.client.screen;

import com.austin.civicnations.menu.NationOverviewMenu;
import com.austin.civicnations.network.ManageMemberPacket;
import com.austin.civicnations.network.ModNetwork;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public final class NationOverviewScreen extends AbstractContainerScreen<NationOverviewMenu> {
    private static final DateTimeFormatter HISTORY_TIME =
            DateTimeFormatter.ofPattern("MM/dd HH:mm").withZone(ZoneId.systemDefault());

    private final List<Button> memberButtons = new ArrayList<>();
    private Tab selectedTab = Tab.OVERVIEW;
    private Button overviewTab;
    private Button citizensTab;
    private Button historyTab;
    private Button currencyTab;
    private Button citizenRoleButton;
    private Button officialRoleButton;
    private Button treasurerRoleButton;
    private Button removeButton;
    private Button transferButton;
    private Button previousPageButton;
    private Button nextPageButton;
    private int memberPage;
    private int historyPage;
    private int selectedMemberIndex = -1;
    private ManageMemberPacket.Action pendingConfirmation;
    private java.util.UUID pendingTargetId;

    public NationOverviewScreen(NationOverviewMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 320;
        imageHeight = 220;
    }

    @Override
    protected void init() {
        super.init();

        overviewTab = addRenderableWidget(Button.builder(Component.literal("Overview"),
                button -> setTab(Tab.OVERVIEW)).bounds(leftPos + 8, topPos + 8, 70, 20).build());
        citizensTab = addRenderableWidget(Button.builder(Component.literal("Citizens"),
                button -> setTab(Tab.CITIZENS)).bounds(leftPos + 86, topPos + 8, 70, 20).build());
        historyTab = addRenderableWidget(Button.builder(Component.literal("History"),
                button -> setTab(Tab.HISTORY)).bounds(leftPos + 164, topPos + 8, 70, 20).build());
        currencyTab = addRenderableWidget(Button.builder(Component.literal("Currency"),
                button -> setTab(Tab.CURRENCY)).bounds(leftPos + 242, topPos + 8, 70, 20).build());

        for (int index = 0; index < 7; index++) {
            final int row = index;
            Button button = addRenderableWidget(Button.builder(Component.empty(),
                    ignored -> selectMemberRow(row))
                    .bounds(leftPos + 20, topPos + 48 + index * 20, 190, 18).build());
            memberButtons.add(button);
        }

        citizenRoleButton = addRenderableWidget(Button.builder(Component.literal("Citizen"),
                ignored -> manageSelected(ManageMemberPacket.Action.CITIZEN))
                .bounds(leftPos + 222, topPos + 50, 80, 18).build());
        officialRoleButton = addRenderableWidget(Button.builder(Component.literal("Official"),
                ignored -> manageSelected(ManageMemberPacket.Action.OFFICIAL))
                .bounds(leftPos + 222, topPos + 72, 80, 18).build());
        treasurerRoleButton = addRenderableWidget(Button.builder(Component.literal("Treasurer"),
                ignored -> manageSelected(ManageMemberPacket.Action.TREASURER))
                .bounds(leftPos + 222, topPos + 94, 80, 18).build());
        removeButton = addRenderableWidget(Button.builder(Component.literal("Remove"),
                ignored -> manageSelected(ManageMemberPacket.Action.REMOVE))
                .bounds(leftPos + 222, topPos + 126, 80, 18).build());
        transferButton = addRenderableWidget(Button.builder(Component.literal("Transfer"),
                ignored -> manageSelected(ManageMemberPacket.Action.TRANSFER))
                .bounds(leftPos + 222, topPos + 148, 80, 18).build());

        previousPageButton = addRenderableWidget(Button.builder(Component.literal("<"),
                ignored -> previousPage()).bounds(leftPos + 116, topPos + 188, 36, 18).build());
        nextPageButton = addRenderableWidget(Button.builder(Component.literal(">"),
                ignored -> nextPage()).bounds(leftPos + 168, topPos + 188, 36, 18).build());

        setTab(Tab.OVERVIEW);
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        overviewTab.active = selectedTab != Tab.OVERVIEW;
        citizensTab.active = selectedTab != Tab.CITIZENS;
        historyTab.active = selectedTab != Tab.HISTORY;
        currencyTab.active = selectedTab != Tab.CURRENCY;

        if (selectedTab == Tab.CITIZENS) {
            updateMemberButtons();
        }
        updateManagementButtons();
        removeButton.setMessage(Component.literal(pendingConfirmation == ManageMemberPacket.Action.REMOVE
                ? "Confirm Remove" : "Remove"));
        transferButton.setMessage(Component.literal(pendingConfirmation == ManageMemberPacket.Action.TRANSFER
                ? "Confirm Transfer" : "Transfer"));
        updatePageButtons();
    }

    private void setTab(Tab tab) {
        selectedTab = tab;
        selectedMemberIndex = -1;
        clearConfirmation();
        boolean citizens = tab == Tab.CITIZENS;
        memberButtons.forEach(button -> button.visible = citizens);
        citizenRoleButton.visible = citizens;
        officialRoleButton.visible = citizens;
        treasurerRoleButton.visible = citizens;
        removeButton.visible = citizens;
        transferButton.visible = citizens && !menu.serverOwned();
        boolean paged = tab == Tab.CITIZENS || tab == Tab.HISTORY;
        previousPageButton.visible = paged;
        nextPageButton.visible = paged;
    }

    private void updateMemberButtons() {
        int start = memberPage * memberButtons.size();
        for (int row = 0; row < memberButtons.size(); row++) {
            Button button = memberButtons.get(row);
            int memberIndex = start + row;
            if (memberIndex < menu.members().size()) {
                NationOverviewMenu.MemberView member = menu.members().get(memberIndex);
                String prefix = memberIndex == selectedMemberIndex ? "> " : "";
                button.setMessage(Component.literal(prefix + member.name() + " - "
                        + member.role().displayName()));
                button.active = true;
            } else {
                button.setMessage(Component.empty());
                button.active = false;
            }
        }
    }

    private void updateManagementButtons() {
        boolean selected = selectedMemberIndex >= 0 && selectedMemberIndex < menu.members().size();
        citizenRoleButton.active = selected && menu.canAssignRoles();
        officialRoleButton.active = selected && menu.canAssignRoles();
        treasurerRoleButton.active = selected && menu.canAssignRoles();
        removeButton.active = selected && menu.canRemoveMembers();
        transferButton.active = selected && menu.canTransferLeadership();
    }

    private void updatePageButtons() {
        if (selectedTab == Tab.CITIZENS) {
            int maxPage = Math.max(0, (menu.members().size() - 1) / memberButtons.size());
            previousPageButton.active = memberPage > 0;
            nextPageButton.active = memberPage < maxPage;
        } else if (selectedTab == Tab.HISTORY) {
            int maxPage = Math.max(0, (menu.history().size() - 1) / 8);
            previousPageButton.active = historyPage > 0;
            nextPageButton.active = historyPage < maxPage;
        }
    }

    private void selectMemberRow(int row) {
        int index = memberPage * memberButtons.size() + row;
        if (index < menu.members().size()) {
            selectedMemberIndex = index;
            clearConfirmation();
        }
    }

    private void manageSelected(ManageMemberPacket.Action action) {
        if (selectedMemberIndex < 0 || selectedMemberIndex >= menu.members().size()) {
            return;
        }
        NationOverviewMenu.MemberView member = menu.members().get(selectedMemberIndex);
        if (action == ManageMemberPacket.Action.REMOVE
                || action == ManageMemberPacket.Action.TRANSFER) {
            if (pendingConfirmation != action || !member.playerId().equals(pendingTargetId)) {
                pendingConfirmation = action;
                pendingTargetId = member.playerId();
                return;
            }
        }
        ModNetwork.CHANNEL.sendToServer(new ManageMemberPacket(
                menu.containerId,
                menu.nationId(),
                member.playerId(),
                action
        ));
        clearConfirmation();
    }

    private void clearConfirmation() {
        pendingConfirmation = null;
        pendingTargetId = null;
    }

    private void previousPage() {
        if (selectedTab == Tab.CITIZENS && memberPage > 0) {
            memberPage--;
            selectedMemberIndex = -1;
            clearConfirmation();
        } else if (selectedTab == Tab.HISTORY && historyPage > 0) {
            historyPage--;
        }
    }

    private void nextPage() {
        if (selectedTab == Tab.CITIZENS) {
            int maxPage = Math.max(0, (menu.members().size() - 1) / memberButtons.size());
            if (memberPage < maxPage) {
                memberPage++;
                selectedMemberIndex = -1;
                clearConfirmation();
            }
        } else if (selectedTab == Tab.HISTORY) {
            int maxPage = Math.max(0, (menu.history().size() - 1) / 8);
            if (historyPage < maxPage) {
                historyPage++;
            }
        }
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
        graphics.fill(leftPos + 5, topPos + 34, leftPos + imageWidth - 5,
                topPos + imageHeight - 5, 0xFF252A31);

        if (selectedTab == Tab.OVERVIEW) {
            renderOverview(graphics);
        } else if (selectedTab == Tab.HISTORY) {
            renderHistory(graphics);
        } else if (selectedTab == Tab.CURRENCY) {
            renderCurrency(graphics);
        }
    }

    private void renderOverview(GuiGraphics graphics) {
        graphics.fill(leftPos + 14, topPos + 45, leftPos + imageWidth - 14,
                topPos + 93, 0xFF303740);
        graphics.renderItem(menu.banner(), leftPos + 28, topPos + 60);
        graphics.drawString(font, menu.nationName(), leftPos + 58, topPos + 55,
                0xFFFFFF, false);
        graphics.drawString(font, "[" + menu.abbreviation() + "]", leftPos + 58,
                topPos + 73, 0xB8D8FF, false);
        graphics.drawString(font, "Owner: " + (menu.serverOwned() ? "Server" : "Player Leader"),
                leftPos + 190, topPos + 64, 0xD8D8D8, false);

        graphics.drawString(font, "Your Role: " + menu.playerRole().displayName(),
                leftPos + 28, topPos + 111, 0xE0E0E0, false);
        graphics.drawString(font, "Citizens: " + menu.memberCount(),
                leftPos + 28, topPos + 132, 0xE0E0E0, false);
        graphics.drawString(font, "Claims: " + menu.claimedChunks() + " / " + menu.claimLimit(),
                leftPos + 28, topPos + 153, 0xA9D18E, false);
        graphics.drawString(font, "Force-loaded Chunks: Disabled",
                leftPos + 28, topPos + 174, 0xE6A6A6, false);
        graphics.drawCenteredString(font, "Use the tabs above to manage citizens and view history.",
                leftPos + imageWidth / 2, topPos + 200, 0xB8D8FF);
    }

    private void renderCurrency(GuiGraphics graphics) {
        graphics.drawCenteredString(font, "NATIONAL CURRENCY", leftPos + imageWidth / 2,
                topPos + 43, 0xFFFFFF);
        if (!menu.currencyConfigured()) {
            graphics.drawCenteredString(font, "No national currency has been established.",
                    leftPos + imageWidth / 2, topPos + 86, 0xE6A6A6);
            graphics.drawCenteredString(font, "Leader: /nation currency create <code> <name>",
                    leftPos + imageWidth / 2, topPos + 108, 0xB8D8FF);
            graphics.drawCenteredString(font, "Server nation: use the OP admin command.",
                    leftPos + imageWidth / 2, topPos + 128, 0x999999);
            return;
        }

        graphics.drawString(font, menu.currencyName() + " (" + menu.currencyCode() + ")",
                leftPos + 20, topPos + 60, 0xFFD37F, false);
        graphics.drawString(font, "Physical coins display [" + menu.currencyCode() + "]",
                leftPos + 202, topPos + 60, 0xE0E0E0, false);
        graphics.drawString(font, "Your Digital Balance: "
                        + menu.viewerBalance() + " " + menu.currencyCode(),
                leftPos + 20, topPos + 82, 0x9AD9FF, false);
        graphics.drawString(font, "Nation Treasury: "
                        + menu.treasuryBalance() + " " + menu.currencyCode(),
                leftPos + 20, topPos + 102, 0xA9D18E, false);
        graphics.drawString(font, "Total Issued: "
                        + menu.totalIssued() + " " + menu.currencyCode(),
                leftPos + 20, topPos + 122, 0xE0E0E0, false);
        graphics.drawString(font, "Mint physical currency at a Coin Mint in claimed territory.",
                leftPos + 20, topPos + 154, 0xD8B77A, false);
        graphics.drawString(font, "Denominations: " + menu.currencyDenominations(),
                leftPos + 20, topPos + 140, 0xD8D8D8, false);

        graphics.drawString(font, "Recent financial activity:",
                leftPos + 20, topPos + 168, 0xFFFFFF, false);
        int rows = Math.min(3, menu.currencyTransactions().size());
        for (int row = 0; row < rows; row++) {
            NationOverviewMenu.CurrencyView entry = menu.currencyTransactions().get(row);
            String amount = entry.amount() > 0L
                    ? entry.amount() + " " + menu.currencyCode()
                    : "";
            String line = entry.actor() + " - " + entry.type()
                    + (amount.isBlank() ? "" : " - " + amount);
            graphics.drawString(font, font.plainSubstrByWidth(line, 278),
                    leftPos + 20, topPos + 182 + row * 12, 0xBDBDBD, false);
        }
    }

    private void renderHistory(GuiGraphics graphics) {
        graphics.drawCenteredString(font, "NATION HISTORY", leftPos + imageWidth / 2,
                topPos + 43, 0xFFFFFF);
        int start = historyPage * 8;
        for (int row = 0; row < 8; row++) {
            int index = start + row;
            if (index >= menu.history().size()) {
                break;
            }
            NationOverviewMenu.HistoryView entry = menu.history().get(index);
            int y = topPos + 64 + row * 15;
            graphics.drawString(font, HISTORY_TIME.format(Instant.ofEpochMilli(entry.timestamp())),
                    leftPos + 18, y, 0x999999, false);
            String shortened = font.plainSubstrByWidth(entry.message(), 220);
            graphics.drawString(font, shortened, leftPos + 84, y, 0xE0E0E0, false);
        }
        if (menu.history().isEmpty()) {
            graphics.drawCenteredString(font, "No recorded nation activity yet.",
                    leftPos + imageWidth / 2, topPos + 106, 0x999999);
        }
        graphics.drawCenteredString(font, "Newest entries appear first.",
                leftPos + imageWidth / 2, topPos + 174, 0xB8D8FF);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        if (selectedTab == Tab.CITIZENS) {
            graphics.drawCenteredString(font, "CITIZENS & ROLES", imageWidth / 2, 34, 0xFFFFFF);
            graphics.drawString(font, menu.serverOwned()
                            ? "Owner: Server (OP management enabled)"
                            : "Select an online citizen to manage.",
                    20, 179, 0xB8D8FF, false);
            if (!menu.canAssignRoles() && !menu.canRemoveMembers()) {
                graphics.drawString(font, "Your role cannot manage citizens.",
                        20, 194, 0xE6A6A6, false);
            }
        }
    }

    private enum Tab {
        OVERVIEW,
        CITIZENS,
        HISTORY,
        CURRENCY
    }
}
