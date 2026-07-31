package com.austin.civicnations.menu;

import com.austin.civicnations.block.CoinMintBlock;
import com.austin.civicnations.data.NationCurrency;
import com.austin.civicnations.registry.ModMenus;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public final class CoinMintMenu extends AbstractContainerMenu {
    private final BlockPos mintPos;
    private final String currencyCode;
    private final List<Integer> denominations;

    private CoinMintMenu(int containerId, Inventory inventory, BlockPos mintPos,
                         String currencyCode, List<Integer> denominations) {
        super(ModMenus.COIN_MINT.get(), containerId);
        this.mintPos = mintPos.immutable();
        this.currencyCode = currencyCode;
        this.denominations = List.copyOf(denominations);
    }

    public static CoinMintMenu serverMenu(int containerId, Inventory inventory,
                                          BlockPos mintPos, NationCurrency currency) {
        return new CoinMintMenu(containerId, inventory, mintPos, currency.code(),
                currency.denominations());
    }

    public static CoinMintMenu fromNetwork(int containerId, Inventory inventory,
                                           FriendlyByteBuf buffer) {
        BlockPos pos = buffer.readBlockPos();
        String code = buffer.readUtf(5);
        int size = Math.min(buffer.readVarInt(), 16);
        List<Integer> denominations = new ArrayList<>();
        for (int index = 0; index < size; index++) {
            denominations.add(buffer.readVarInt());
        }
        return new CoinMintMenu(containerId, inventory, pos, code, denominations);
    }

    public static void writeOpenData(FriendlyByteBuf buffer, BlockPos pos,
                                     NationCurrency currency) {
        buffer.writeBlockPos(pos);
        buffer.writeUtf(currency.code(), 5);
        buffer.writeVarInt(currency.denominations().size());
        for (int denomination : currency.denominations()) {
            buffer.writeVarInt(denomination);
        }
    }

    public BlockPos mintPos() {
        return mintPos;
    }

    public String currencyCode() {
        return currencyCode;
    }

    public List<Integer> denominations() {
        return denominations;
    }

    @Override
    public boolean stillValid(Player player) {
        return player.distanceToSqr(mintPos.getX() + 1.0D, mintPos.getY() + 0.5D,
                mintPos.getZ() + 0.5D) <= 64.0D
                && CoinMintBlock.isComplete(player.level(), mintPos);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }
}
