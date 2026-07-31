package com.austin.civicnations.client.screen;

import com.austin.civicnations.data.BannerPatternLayer;
import com.austin.civicnations.menu.NationCreationMenu;
import com.austin.civicnations.network.CreateNationPacket;
import com.austin.civicnations.network.ModNetwork;
import com.austin.civicnations.util.BannerDesign;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class NationCreationScreen extends AbstractContainerScreen<NationCreationMenu> {
    private final List<Button> layerButtons = new ArrayList<>();
    private final List<BannerPatternLayer> selectedLayers = new ArrayList<>();

    private EditBox nameBox;
    private EditBox abbreviationBox;
    private Button bannerButton;
    private Button createButton;
    private Button confirmButton;
    private Button cancelConfirmButton;

    private Button baseColorButton;
    private Button patternButton;
    private Button dyeColorButton;
    private Button addLayerButton;
    private Button moveLayerUpButton;
    private Button moveLayerDownButton;
    private Button removeLayerButton;
    private Button finishBannerButton;
    private Button cancelBannerButton;

    private DyeColor selectedBannerColor;
    private DyeColor bannerColorBeforeEditing;
    private DyeColor selectedPatternColor = DyeColor.WHITE;
    private List<BannerPatternLayer> layersBeforeEditing = List.of();
    private int selectedPatternIndex;
    private int selectedLayerIndex = -1;
    private boolean confirming;
    private boolean editingBanner;

    public NationCreationScreen(NationCreationMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 320;
        imageHeight = 252;
    }

    @Override
    protected void init() {
        super.init();

        nameBox = new EditBox(font, leftPos + 126, topPos + 38, 168, 18,
                Component.literal("Nation Name"));
        nameBox.setMaxLength(24);
        nameBox.setHint(Component.literal("Ironvale"));
        addRenderableWidget(nameBox);

        abbreviationBox = new EditBox(font, leftPos + 126, topPos + 68, 76, 18,
                Component.literal("Abbreviation"));
        abbreviationBox.setMaxLength(5);
        abbreviationBox.setFilter(value -> value.chars().allMatch(Character::isLetterOrDigit));
        abbreviationBox.setResponder(value -> {
            String upper = value.toUpperCase(Locale.ROOT);
            if (!upper.equals(value)) {
                abbreviationBox.setValue(upper);
            }
        });
        abbreviationBox.setHint(Component.literal("IV"));
        addRenderableWidget(abbreviationBox);

        bannerButton = addRenderableWidget(Button.builder(
                Component.literal("Create Banner..."),
                button -> openBannerEditor()
        ).bounds(leftPos + 126, topPos + 98, 168, 20).build());

        createButton = addRenderableWidget(Button.builder(
                Component.literal("Create Nation"),
                button -> openConfirmation()
        ).bounds(leftPos + 126, topPos + 132, 168, 20).build());

        confirmButton = addRenderableWidget(Button.builder(
                Component.literal("Confirm Creation"),
                button -> submit()
        ).bounds(leftPos + 72, topPos + 214, 86, 20).build());

        cancelConfirmButton = addRenderableWidget(Button.builder(
                Component.literal("Go Back"),
                button -> closeConfirmation()
        ).bounds(leftPos + 164, topPos + 214, 86, 20).build());

        baseColorButton = addRenderableWidget(Button.builder(
                Component.empty(), button -> cycleBaseColor()
        ).bounds(leftPos + 18, topPos + 44, 132, 20).build());

        addLayerButton = addRenderableWidget(Button.builder(
                Component.literal("Add Layer"), button -> addLayer()
        ).bounds(leftPos + 224, topPos + 44, 78, 20).build());

        patternButton = addRenderableWidget(Button.builder(
                Component.empty(), button -> cyclePattern()
        ).bounds(leftPos + 18, topPos + 74, 190, 20).build());

        dyeColorButton = addRenderableWidget(Button.builder(
                Component.empty(), button -> cyclePatternColor()
        ).bounds(leftPos + 214, topPos + 74, 88, 20).build());

        for (int index = 0; index < BannerDesign.MAX_LAYERS; index++) {
            final int layerIndex = index;
            Button button = addRenderableWidget(Button.builder(
                    Component.empty(), ignored -> selectLayer(layerIndex)
            ).bounds(leftPos + 18, topPos + 108 + index * 18, 214, 17).build());
            layerButtons.add(button);
        }

        moveLayerUpButton = addRenderableWidget(Button.builder(
                Component.literal("Up"), button -> moveSelectedLayer(-1)
        ).bounds(leftPos + 240, topPos + 108, 62, 18).build());

        moveLayerDownButton = addRenderableWidget(Button.builder(
                Component.literal("Down"), button -> moveSelectedLayer(1)
        ).bounds(leftPos + 240, topPos + 130, 62, 18).build());

        removeLayerButton = addRenderableWidget(Button.builder(
                Component.literal("Remove"), button -> removeSelectedLayer()
        ).bounds(leftPos + 240, topPos + 152, 62, 18).build());

        finishBannerButton = addRenderableWidget(Button.builder(
                Component.literal("Use Banner"), button -> finishBannerEditor()
        ).bounds(leftPos + 72, topPos + 224, 86, 20).build());

        cancelBannerButton = addRenderableWidget(Button.builder(
                Component.literal("Cancel"), button -> cancelBannerEditor()
        ).bounds(leftPos + 164, topPos + 224, 86, 20).build());

        showForm();
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        createButton.active = !confirming && !editingBanner && isLocallyValid();
        finishBannerButton.active = selectedBannerColor != null;
        addLayerButton.active = editingBanner && selectedLayers.size() < BannerDesign.MAX_LAYERS;
        boolean hasSelectedLayer = selectedLayerIndex >= 0 && selectedLayerIndex < selectedLayers.size();
        removeLayerButton.active = hasSelectedLayer;
        moveLayerUpButton.active = hasSelectedLayer && selectedLayerIndex > 0;
        moveLayerDownButton.active = hasSelectedLayer && selectedLayerIndex < selectedLayers.size() - 1;

        bannerButton.setMessage(selectedBannerColor == null
                ? Component.literal("Create Banner...")
                : Component.literal("Edit Banner (" + selectedLayers.size() + " pattern"
                + (selectedLayers.size() == 1 ? "" : "s") + ")"));

        baseColorButton.setMessage(Component.literal("Base: " + displayColorName(selectedBannerColor)));
        BannerDesign.PatternChoice choice = BannerDesign.PATTERNS.get(selectedPatternIndex);
        patternButton.setMessage(Component.literal("Pattern: " + choice.displayName()));
        dyeColorButton.setMessage(Component.literal(displayColorName(selectedPatternColor)));

        for (int index = 0; index < layerButtons.size(); index++) {
            Button layerButton = layerButtons.get(index);
            if (index < selectedLayers.size()) {
                BannerPatternLayer layer = selectedLayers.get(index);
                String prefix = index == selectedLayerIndex ? "> " : "";
                layerButton.setMessage(Component.literal(prefix + (index + 1) + ". "
                        + displayColorName(DyeColor.byId(layer.colorId())) + " "
                        + BannerDesign.displayName(layer.patternId())));
                layerButton.active = true;
            } else {
                layerButton.setMessage(Component.literal((index + 1) + ". Empty"));
                layerButton.active = false;
            }
        }
    }

    private boolean isLocallyValid() {
        String name = nameBox.getValue().trim();
        String abbreviation = abbreviationBox.getValue().trim();
        return name.length() >= 3
                && name.length() <= 24
                && abbreviation.length() >= 2
                && abbreviation.length() <= 5
                && selectedBannerColor != null;
    }

    private void openBannerEditor() {
        bannerColorBeforeEditing = selectedBannerColor;
        layersBeforeEditing = List.copyOf(selectedLayers);
        if (selectedBannerColor == null) {
            selectedBannerColor = DyeColor.WHITE;
        }
        editingBanner = true;
        confirming = false;
        selectedLayerIndex = selectedLayers.isEmpty() ? -1 : selectedLayers.size() - 1;
        setFormVisible(false);
        setConfirmationVisible(false);
        setBannerEditorVisible(true);
    }

    private void cycleBaseColor() {
        selectedBannerColor = nextColor(selectedBannerColor == null ? DyeColor.WHITE : selectedBannerColor);
    }

    private void cyclePattern() {
        selectedPatternIndex = (selectedPatternIndex + 1) % BannerDesign.PATTERNS.size();
    }

    private void cyclePatternColor() {
        selectedPatternColor = nextColor(selectedPatternColor);
    }

    private void addLayer() {
        if (selectedLayers.size() >= BannerDesign.MAX_LAYERS) {
            return;
        }
        BannerDesign.PatternChoice choice = BannerDesign.PATTERNS.get(selectedPatternIndex);
        selectedLayers.add(new BannerPatternLayer(choice.id(), selectedPatternColor.getId()));
        selectedLayerIndex = selectedLayers.size() - 1;
    }

    private void selectLayer(int index) {
        if (index >= 0 && index < selectedLayers.size()) {
            selectedLayerIndex = index;
        }
    }

    private void moveSelectedLayer(int direction) {
        int destination = selectedLayerIndex + direction;
        if (selectedLayerIndex < 0 || selectedLayerIndex >= selectedLayers.size()
                || destination < 0 || destination >= selectedLayers.size()) {
            return;
        }
        BannerPatternLayer layer = selectedLayers.remove(selectedLayerIndex);
        selectedLayers.add(destination, layer);
        selectedLayerIndex = destination;
    }

    private void removeSelectedLayer() {
        if (selectedLayerIndex < 0 || selectedLayerIndex >= selectedLayers.size()) {
            return;
        }
        selectedLayers.remove(selectedLayerIndex);
        if (selectedLayers.isEmpty()) {
            selectedLayerIndex = -1;
        } else if (selectedLayerIndex >= selectedLayers.size()) {
            selectedLayerIndex = selectedLayers.size() - 1;
        }
    }

    private void finishBannerEditor() {
        if (selectedBannerColor == null) {
            return;
        }
        editingBanner = false;
        showForm();
    }

    private void cancelBannerEditor() {
        selectedBannerColor = bannerColorBeforeEditing;
        selectedLayers.clear();
        selectedLayers.addAll(layersBeforeEditing);
        editingBanner = false;
        showForm();
    }

    private void openConfirmation() {
        if (!isLocallyValid()) {
            return;
        }
        confirming = true;
        editingBanner = false;
        setFormVisible(false);
        setBannerEditorVisible(false);
        setConfirmationVisible(true);
    }

    private void closeConfirmation() {
        confirming = false;
        showForm();
    }

    private void showForm() {
        confirming = false;
        editingBanner = false;
        setFormVisible(true);
        setBannerEditorVisible(false);
        setConfirmationVisible(false);
    }

    private void setFormVisible(boolean visible) {
        nameBox.visible = visible;
        nameBox.setEditable(visible);
        abbreviationBox.visible = visible;
        abbreviationBox.setEditable(visible);
        bannerButton.visible = visible;
        createButton.visible = visible;
    }

    private void setConfirmationVisible(boolean visible) {
        confirmButton.visible = visible;
        confirmButton.active = visible;
        cancelConfirmButton.visible = visible;
    }

    private void setBannerEditorVisible(boolean visible) {
        baseColorButton.visible = visible;
        patternButton.visible = visible;
        dyeColorButton.visible = visible;
        addLayerButton.visible = visible;
        layerButtons.forEach(button -> button.visible = visible);
        moveLayerUpButton.visible = visible;
        moveLayerDownButton.visible = visible;
        removeLayerButton.visible = visible;
        finishBannerButton.visible = visible;
        cancelBannerButton.visible = visible;
    }

    private void submit() {
        if (selectedBannerColor == null) {
            return;
        }
        ModNetwork.CHANNEL.sendToServer(new CreateNationPacket(
                menu.containerId,
                nameBox.getValue().trim(),
                abbreviationBox.getValue().trim().toUpperCase(Locale.ROOT),
                selectedBannerColor.getId(),
                List.copyOf(selectedLayers)
        ));
        confirmButton.active = false;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (editingBanner && keyCode == 256) {
            cancelBannerEditor();
            return true;
        }
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
        graphics.fill(leftPos + 4, topPos + 4, leftPos + imageWidth - 4,
                topPos + imageHeight - 4, 0xFF303740);

        if (!confirming && !editingBanner) {
            graphics.fill(leftPos + 14, topPos + 188, leftPos + imageWidth - 14,
                    topPos + 231, 0xFF262C33);
        }
        if (confirming) {
            graphics.fill(leftPos + 18, topPos + 20, leftPos + imageWidth - 18,
                    topPos + 204, 0xFF252A31);
            graphics.renderItem(currentBanner(), leftPos + 268, topPos + 76);
        }
        if (editingBanner) {
            graphics.fill(leftPos + 12, topPos + 34, leftPos + imageWidth - 12,
                    topPos + 218, 0xFF252A31);
            graphics.renderItem(currentBanner(), leftPos + 276, topPos + 12);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        if (editingBanner) {
            graphics.drawCenteredString(font, "BANNER DESIGNER", imageWidth / 2, 9, 0xFFFFFF);
            graphics.drawString(font, "Click Base, Pattern, or Dye to cycle choices.",
                    18, 25, 0xB8D8FF, false);
            graphics.drawString(font, "Layers are applied from top to bottom in this list.",
                    18, 214, 0xB8D8FF, false);
            graphics.drawString(font, selectedLayers.size() + " / " + BannerDesign.MAX_LAYERS
                    + " layers", 240, 178, 0xD8D8D8, false);
            return;
        }

        if (confirming) {
            graphics.drawCenteredString(font, "CREATE THIS NATION?", imageWidth / 2, 29, 0xFFFFFF);
            graphics.drawString(font, "Name: " + nameBox.getValue(), 42, 61, 0xE0E0E0, false);
            graphics.drawString(font, "Abbreviation: " + abbreviationBox.getValue(),
                    42, 81, 0xE0E0E0, false);
            graphics.drawString(font, "Banner Base: " + displayColorName(selectedBannerColor),
                    42, 101, 0xE0E0E0, false);
            graphics.drawString(font, "Banner Patterns: " + selectedLayers.size(),
                    42, 121, 0xE0E0E0, false);
            graphics.drawString(font, "Starting Claims: 25", 42, 141, 0xA9D18E, false);
            graphics.drawString(font, "Force Loading: Disabled", 42, 159, 0xE6A6A6, false);
            return;
        }

        graphics.drawCenteredString(font, "CREATE NATION", imageWidth / 2, 11, 0xFFFFFF);
        graphics.drawString(font, "Nation Name", 24, 43, 0xD8D8D8, false);
        graphics.drawString(font, "Abbreviation", 24, 73, 0xD8D8D8, false);
        graphics.drawString(font, "Banner Design", 24, 103, 0xD8D8D8, false);
        graphics.drawCenteredString(font, "Starting Claims: 25", imageWidth / 2, 197, 0xA9D18E);
        graphics.drawCenteredString(font, "Force Loading: Disabled", imageWidth / 2, 215, 0xE6A6A6);
    }

    private ItemStack currentBanner() {
        return selectedBannerColor == null
                ? ItemStack.EMPTY
                : BannerDesign.create(selectedBannerColor, selectedLayers);
    }

    private static DyeColor nextColor(DyeColor current) {
        DyeColor[] colors = DyeColor.values();
        return colors[(current.getId() + 1) % colors.length];
    }

    private static String displayColorName(DyeColor color) {
        if (color == null) {
            return "None";
        }
        String[] words = color.getName().split("_");
        StringBuilder result = new StringBuilder();
        for (String word : words) {
            if (result.length() > 0) {
                result.append(' ');
            }
            result.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
        }
        return result.toString();
    }
}
