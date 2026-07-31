package com.austin.civicnations.item;

import com.austin.civicnations.menu.CoinPurseMenu;
import com.austin.civicnations.util.PurseStorage;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.NetworkHooks;

import javax.annotation.Nullable;
import java.util.List;
import java.util.UUID;

public final class CoinPurseItem extends Item {
    public CoinPurseItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player,
                                                   InteractionHand hand) {
        ItemStack purse = player.getItemInHand(hand);
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            UUID purseId = PurseStorage.ensurePurseId(purse);
            NetworkHooks.openScreen(serverPlayer,
                    new SimpleMenuProvider(
                            (containerId, inventory, ignored) ->
                                    CoinPurseMenu.serverMenu(containerId, inventory, purseId),
                            Component.translatable("menu.civicnations.coin_purse")),
                    buffer -> buffer.writeUUID(purseId));
        }
        return InteractionResultHolder.sidedSuccess(purse, level.isClientSide);
    }

    @Override
    public Component getName(ItemStack stack) {
        if (PurseStorage.isBound(stack)) {
            return Component.literal("Coin Purse [" + PurseStorage.code(stack) + "]");
        }
        return super.getName(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level,
                                List<Component> tooltip, TooltipFlag flag) {
        if (!PurseStorage.isBound(stack)) {
            tooltip.add(Component.literal("Unbound - deposit currency to bind")
                    .withStyle(ChatFormatting.GRAY));
            tooltip.add(Component.literal("Stores exact denominations; does not make change")
                    .withStyle(ChatFormatting.DARK_GRAY));
            return;
        }
        tooltip.add(Component.literal("Currency: " + PurseStorage.currencyName(stack)
                        + " [" + PurseStorage.code(stack) + "]")
                .withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.literal("Issued by: " + PurseStorage.nationName(stack))
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("Coins: " + PurseStorage.totalCoins(stack)
                        + "   Value: " + PurseStorage.totalValue(stack))
                .withStyle(ChatFormatting.YELLOW));
    }
}
