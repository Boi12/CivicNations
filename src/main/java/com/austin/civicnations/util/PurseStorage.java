package com.austin.civicnations.util;

import com.austin.civicnations.registry.ModItems;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;
import java.util.UUID;

/** Server-authoritative storage for a Coin Purse. Denominations remain exact. */
public final class PurseStorage {
    private static final String ROOT = "CivicNationsPurse";
    private static final String COUNTS = "Counts";

    private PurseStorage() {}

    public static boolean isPurse(ItemStack stack) {
        return !stack.isEmpty() && stack.is(ModItems.COIN_PURSE.get());
    }

    public static UUID ensurePurseId(ItemStack stack) {
        CompoundTag root = root(stack);
        if (!root.hasUUID("PurseId")) {
            root.putUUID("PurseId", UUID.randomUUID());
        }
        return root.getUUID("PurseId");
    }

    public static Optional<UUID> purseId(ItemStack stack) {
        if (!isPurse(stack) || !stack.hasTag()) {
            return Optional.empty();
        }
        CompoundTag root = stack.getTag().getCompound(ROOT);
        return root.hasUUID("PurseId") ? Optional.of(root.getUUID("PurseId")) : Optional.empty();
    }

    public static ItemStack findPurse(Inventory inventory, UUID purseId) {
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (purseId(stack).filter(purseId::equals).isPresent()) {
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }

    public static boolean isBound(ItemStack purse) {
        return isPurse(purse) && purse.hasTag() && root(purse).hasUUID("CurrencyId");
    }

    public static Optional<UUID> currencyId(ItemStack purse) {
        CompoundTag root = rootOrEmpty(purse);
        return root.hasUUID("CurrencyId") ? Optional.of(root.getUUID("CurrencyId")) : Optional.empty();
    }

    public static Optional<UUID> nationId(ItemStack purse) {
        CompoundTag root = rootOrEmpty(purse);
        return root.hasUUID("NationId") ? Optional.of(root.getUUID("NationId")) : Optional.empty();
    }

    public static String nationName(ItemStack purse) { return rootOrEmpty(purse).getString("NationName"); }
    public static String currencyName(ItemStack purse) { return rootOrEmpty(purse).getString("CurrencyName"); }
    public static String code(ItemStack purse) { return rootOrEmpty(purse).getString("Code"); }

    public static long count(ItemStack purse, CoinDenomination denomination) {
        return Math.max(0L, counts(rootOrEmpty(purse)).getLong(key(denomination)));
    }

    public static long totalCoins(ItemStack purse) {
        long total = 0L;
        for (CoinDenomination denomination : CoinDenomination.values()) {
            total = saturatedAdd(total, count(purse, denomination));
        }
        return total;
    }

    public static long totalValue(ItemStack purse) {
        long total = 0L;
        for (CoinDenomination denomination : CoinDenomination.values()) {
            long value = saturatedMultiply(count(purse, denomination), denomination.value());
            total = saturatedAdd(total, value);
        }
        return total;
    }

    /** Deposits every matching physical coin in the player's inventory. */
    public static long depositAll(ServerPlayer player, ItemStack purse) {
        if (!isPurse(purse)) {
            return 0L;
        }
        ensurePurseId(purse);
        if (!isBound(purse)) {
            ItemStack first = firstCurrency(player.getInventory());
            if (first.isEmpty()) {
                return 0L;
            }
            bindFromCoin(purse, first);
        }

        UUID acceptedCurrency = currencyId(purse).orElseThrow();
        long deposited = 0L;
        Inventory inventory = player.getInventory();
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (!CurrencyItems.matches(stack, acceptedCurrency)) {
                continue;
            }
            Optional<CoinDenomination> denomination =
                    CoinDenomination.fromValue(CurrencyItems.denomination(stack));
            if (denomination.isEmpty()) {
                continue;
            }
            int amount = stack.getCount();
            add(purse, denomination.get(), amount);
            deposited = saturatedAdd(deposited, amount);
            inventory.setItem(slot, ItemStack.EMPTY);
        }
        if (deposited > 0L) {
            inventory.setChanged();
        }
        return deposited;
    }

    /** Withdraws up to one vanilla stack of a single stored denomination. */
    public static int withdraw(ServerPlayer player, ItemStack purse,
                               CoinDenomination denomination) {
        if (!isBound(purse)) {
            return 0;
        }
        long stored = count(purse, denomination);
        if (stored <= 0L) {
            return 0;
        }
        int amount = (int) Math.min(64L, stored);
        ItemStack coins = CurrencyItems.create(
                currencyId(purse).orElseThrow(),
                nationId(purse).orElse(new UUID(0L, 0L)),
                nationName(purse),
                currencyName(purse),
                code(purse),
                denomination.value(),
                amount
        );
        if (!player.getInventory().add(coins)) {
            player.drop(coins, false);
        } else if (!coins.isEmpty()) {
            player.drop(coins, false);
        }
        setCount(purse, denomination, stored - amount);
        player.getInventory().setChanged();
        return amount;
    }

    public static boolean unbind(ItemStack purse) {
        if (!isPurse(purse) || totalCoins(purse) != 0L) {
            return false;
        }
        CompoundTag root = root(purse);
        root.remove("CurrencyId");
        root.remove("NationId");
        root.remove("NationName");
        root.remove("CurrencyName");
        root.remove("Code");
        root.remove(COUNTS);
        return true;
    }

    private static ItemStack firstCurrency(Inventory inventory) {
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (CurrencyItems.isCurrency(stack)
                    && CoinDenomination.fromValue(CurrencyItems.denomination(stack)).isPresent()) {
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }

    private static void bindFromCoin(ItemStack purse, ItemStack coin) {
        CompoundTag root = root(purse);
        root.putUUID("CurrencyId", CurrencyItems.currencyId(coin));
        root.putUUID("NationId", CurrencyItems.nationId(coin));
        root.putString("NationName", CurrencyItems.nationName(coin));
        root.putString("CurrencyName", CurrencyItems.currencyName(coin));
        root.putString("Code", CurrencyItems.code(coin));
    }

    private static void add(ItemStack purse, CoinDenomination denomination, long amount) {
        setCount(purse, denomination, saturatedAdd(count(purse, denomination), amount));
    }

    private static void setCount(ItemStack purse, CoinDenomination denomination, long amount) {
        counts(root(purse)).putLong(key(denomination), Math.max(0L, amount));
    }

    private static String key(CoinDenomination denomination) {
        return "Value" + denomination.value();
    }

    private static CompoundTag root(ItemStack purse) {
        return purse.getOrCreateTagElement(ROOT);
    }

    private static CompoundTag rootOrEmpty(ItemStack purse) {
        if (!isPurse(purse) || !purse.hasTag() || !purse.getTag().contains(ROOT)) {
            return new CompoundTag();
        }
        return purse.getTag().getCompound(ROOT);
    }

    private static CompoundTag counts(CompoundTag root) {
        if (!root.contains(COUNTS)) {
            root.put(COUNTS, new CompoundTag());
        }
        return root.getCompound(COUNTS);
    }

    private static long saturatedAdd(long left, long right) {
        if (right > 0L && left > Long.MAX_VALUE - right) {
            return Long.MAX_VALUE;
        }
        return left + right;
    }

    private static long saturatedMultiply(long left, long right) {
        if (left == 0L || right == 0L) {
            return 0L;
        }
        return left > Long.MAX_VALUE / right ? Long.MAX_VALUE : left * right;
    }
}
