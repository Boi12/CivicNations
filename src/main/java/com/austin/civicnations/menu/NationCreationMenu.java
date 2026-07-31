package com.austin.civicnations.menu;

import com.austin.civicnations.registry.ModMenus;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

public final class NationCreationMenu extends AbstractContainerMenu {
    public NationCreationMenu(int containerId, Inventory inventory) {
        super(ModMenus.NATION_CREATION.get(), containerId);
    }

    public static NationCreationMenu fromNetwork(int containerId, Inventory inventory, FriendlyByteBuf ignored) {
        return new NationCreationMenu(containerId, inventory);
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }
}
