package com.austin.civicnations.item;

import com.austin.civicnations.util.CoinDenomination;
import com.austin.civicnations.util.CurrencyItems;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

public final class NationCurrencyItem extends Item {
    public NationCurrencyItem(Properties properties) {
        super(properties);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot,
                              boolean selected) {
        if (CurrencyItems.isCurrency(stack)) {
            int denomination = CurrencyItems.denomination(stack);
            if (stack.getOrCreateTag().getInt("CustomModelData") != denomination) {
                stack.getOrCreateTag().putInt("CustomModelData", denomination);
            }
        }
        super.inventoryTick(stack, level, entity, slot, selected);
    }

    @Override
    public Component getName(ItemStack stack) {
        if (!CurrencyItems.isCurrency(stack)) {
            return Component.literal("Unissued Nation Currency");
        }
        return CoinDenomination.fromValue(CurrencyItems.denomination(stack))
                .map(type -> Component.literal(type.issuedName(CurrencyItems.code(stack))))
                .orElseGet(() -> Component.literal("Coin [" + CurrencyItems.code(stack) + "]"));
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level,
                                List<Component> tooltip, TooltipFlag flag) {
        if (!CurrencyItems.isCurrency(stack)) {
            tooltip.add(Component.literal("This item has not been issued by a nation.")
                    .withStyle(ChatFormatting.RED));
            return;
        }
        tooltip.add(Component.literal("Value: " + CurrencyItems.denomination(stack)
                        + " " + CurrencyItems.code(stack))
                .withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.literal("Currency: " + CurrencyItems.currencyName(stack))
                .withStyle(ChatFormatting.YELLOW));
        tooltip.add(Component.literal("Issued by: " + CurrencyItems.nationName(stack))
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("Currency ID: " + CurrencyItems.currencyId(stack))
                .withStyle(ChatFormatting.DARK_GRAY));
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return false;
    }
}
