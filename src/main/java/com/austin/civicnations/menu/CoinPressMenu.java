package com.austin.civicnations.menu;

import com.austin.civicnations.block.CoinPressBlock;
import com.austin.civicnations.block.entity.CoinPressBlockEntity;
import com.austin.civicnations.registry.ModMenus;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public final class CoinPressMenu extends AbstractContainerMenu {
    private static final int PRESS_SLOT_COUNT = CoinPressBlockEntity.SLOT_COUNT;

    private final BlockPos pressPos;
    private final Container pressContainer;
    private final ContainerData pressData;

    private CoinPressMenu(int containerId, Inventory inventory, BlockPos pressPos,
                          Container pressContainer, ContainerData pressData) {
        super(ModMenus.COIN_PRESS.get(), containerId);
        checkContainerSize(pressContainer, PRESS_SLOT_COUNT);
        checkContainerDataCount(pressData, CoinPressBlockEntity.DATA_COUNT);
        this.pressPos = pressPos.immutable();
        this.pressContainer = pressContainer;
        this.pressData = pressData;
        pressContainer.startOpen(inventory.player);
        addDataSlots(pressData);

        addSlot(new Slot(pressContainer, CoinPressBlockEntity.PRIMARY_SLOT, 62, 54));
        addSlot(new Slot(pressContainer, CoinPressBlockEntity.MATERIAL_SLOT, 86, 54));
        addSlot(new Slot(pressContainer, CoinPressBlockEntity.OUTPUT_SLOT, 220, 54) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }
        });

        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(inventory, column + row * 9 + 9,
                        62 + column * 18, 169 + row * 18));
            }
        }
        for (int column = 0; column < 9; column++) {
            addSlot(new Slot(inventory, column, 62 + column * 18, 227));
        }
    }

    public static CoinPressMenu serverMenu(int containerId, Inventory inventory,
                                            BlockPos pressPos, Container pressContainer,
                                            ContainerData pressData) {
        return new CoinPressMenu(containerId, inventory, pressPos, pressContainer, pressData);
    }

    public static CoinPressMenu fromNetwork(int containerId, Inventory inventory,
                                             FriendlyByteBuf buffer) {
        return new CoinPressMenu(containerId, inventory, buffer.readBlockPos(),
                new SimpleContainer(PRESS_SLOT_COUNT),
                new SimpleContainerData(CoinPressBlockEntity.DATA_COUNT));
    }

    public BlockPos pressPos() {
        return pressPos;
    }

    public Container pressContainer() {
        return pressContainer;
    }

    public boolean isRunning() {
        return pressData.get(CoinPressBlockEntity.DATA_RUNNING) != 0;
    }

    public int progress() {
        return pressData.get(CoinPressBlockEntity.DATA_PROGRESS);
    }

    public int processTime() {
        return Math.max(1, pressData.get(CoinPressBlockEntity.DATA_PROCESS_TIME));
    }

    public int progressWidth(int maximumWidth) {
        return progress() * maximumWidth / processTime();
    }

    @Override
    public boolean stillValid(Player player) {
        return pressContainer.stillValid(player)
                && CoinPressBlock.isComplete(player.level(), pressPos);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (!slot.hasItem()) {
            return result;
        }

        ItemStack source = slot.getItem();
        result = source.copy();
        if (index < PRESS_SLOT_COUNT) {
            if (!moveItemStackTo(source, PRESS_SLOT_COUNT, slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else if (!moveItemStackTo(source, 0, CoinPressBlockEntity.OUTPUT_SLOT, false)) {
            return ItemStack.EMPTY;
        }

        if (source.isEmpty()) {
            slot.set(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        slot.onTake(player, source);
        return result;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        pressContainer.stopOpen(player);
    }
}
