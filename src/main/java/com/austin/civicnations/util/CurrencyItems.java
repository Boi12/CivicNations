package com.austin.civicnations.util;

import com.austin.civicnations.data.NationCurrency;
import com.austin.civicnations.data.NationRecord;
import com.austin.civicnations.registry.ModItems;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;
import java.util.UUID;

public final class CurrencyItems {
    private static final String ROOT = "CivicNationsCurrency";

    private CurrencyItems() {}

    public static ItemStack create(NationRecord nation, int denomination, int count) {
        NationCurrency currency = nation.currency().orElseThrow();
        return create(currency.currencyId(), nation.nationId(), nation.name(), currency.name(),
                currency.code(), denomination, count);
    }

    /** Recreates already-issued currency without requiring the issuing nation to still exist. */
    public static ItemStack create(UUID currencyId, UUID nationId, String nationName,
                                   String currencyName, String code, int denomination, int count) {
        ItemStack stack = new ItemStack(ModItems.NATION_CURRENCY.get(), Math.max(1, count));
        CompoundTag tag = new CompoundTag();
        tag.putUUID("CurrencyId", currencyId);
        tag.putUUID("NationId", nationId);
        tag.putString("NationName", nationName);
        tag.putString("CurrencyName", currencyName);
        tag.putString("Code", code);
        tag.putString("Symbol", "");
        tag.putInt("Denomination", denomination);
        stack.getOrCreateTag().put(ROOT, tag);
        stack.getOrCreateTag().putInt("CustomModelData", denomination);
        return stack;
    }

    public static boolean isCurrency(ItemStack stack) {
        return !stack.isEmpty() && stack.is(ModItems.NATION_CURRENCY.get())
                && stack.hasTag() && stack.getTag().contains(ROOT);
    }

    private static CompoundTag data(ItemStack stack) {
        return isCurrency(stack) ? stack.getTag().getCompound(ROOT) : new CompoundTag();
    }

    public static Optional<UUID> currencyIdOptional(ItemStack stack) {
        CompoundTag tag = data(stack);
        return tag.hasUUID("CurrencyId") ? Optional.of(tag.getUUID("CurrencyId")) : Optional.empty();
    }

    public static UUID currencyId(ItemStack stack) {
        return currencyIdOptional(stack).orElse(new UUID(0L, 0L));
    }

    public static Optional<UUID> nationIdOptional(ItemStack stack) {
        CompoundTag tag = data(stack);
        return tag.hasUUID("NationId") ? Optional.of(tag.getUUID("NationId")) : Optional.empty();
    }

    public static UUID nationId(ItemStack stack) {
        return nationIdOptional(stack).orElse(new UUID(0L, 0L));
    }

    public static boolean matches(ItemStack stack, UUID currencyId) {
        return currencyIdOptional(stack).filter(currencyId::equals).isPresent();
    }

    public static int denomination(ItemStack stack) { return data(stack).getInt("Denomination"); }
    public static String nationName(ItemStack stack) { return data(stack).getString("NationName"); }
    public static String currencyName(ItemStack stack) { return data(stack).getString("CurrencyName"); }
    public static String code(ItemStack stack) { return data(stack).getString("Code"); }
    public static String symbol(ItemStack stack) { return data(stack).getString("Symbol"); }
}
