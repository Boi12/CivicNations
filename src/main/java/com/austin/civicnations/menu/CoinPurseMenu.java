package com.austin.civicnations.menu;

import com.austin.civicnations.registry.ModMenus;
import com.austin.civicnations.util.PurseStorage;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.UUID;

public final class CoinPurseMenu extends AbstractContainerMenu {
    private final Inventory playerInventory;
    private final UUID purseId;

    private CoinPurseMenu(int containerId, Inventory inventory, UUID purseId) {
        super(ModMenus.COIN_PURSE.get(), containerId);
        this.playerInventory = inventory;
        this.purseId = purseId;

        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(inventory, column + row * 9 + 9,
                        69 + column * 18, 163 + row * 18));
            }
        }
        for (int column = 0; column < 9; column++) {
            addSlot(new Slot(inventory, column, 69 + column * 18, 221));
        }
    }

    public static CoinPurseMenu serverMenu(int containerId, Inventory inventory, UUID purseId) {
        return new CoinPurseMenu(containerId, inventory, purseId);
    }

    public static CoinPurseMenu fromNetwork(int containerId, Inventory inventory,
                                             FriendlyByteBuf buffer) {
        return new CoinPurseMenu(containerId, inventory, buffer.readUUID());
    }

    public UUID purseId() {
        return purseId;
    }

    public ItemStack purseStack() {
        return PurseStorage.findPurse(playerInventory, purseId);
    }

    @Override
    public boolean stillValid(Player player) {
        return !purseStack().isEmpty();
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }
}
