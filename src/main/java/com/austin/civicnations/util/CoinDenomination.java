package com.austin.civicnations.util;

import com.austin.civicnations.registry.ModItems;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * Fixed physical coin denominations used by every national currency.
 * The material/shape determines value; nations cannot redefine these values.
 */
public enum CoinDenomination {
    COPPER(1, "Copper Coin", "Blank Copper Coin", 8),
    IRON(5, "Iron Coin", "Blank Iron Coin", 8),
    SILVER(10, "Silver Coin", "Blank Silver Coin", 8),
    GOLD(25, "Gold Coin", "Blank Gold Coin", 8),
    DIAMOND_PLATED_GOLD(50, "Diamond-Plated Gold Coin",
            "Blank Diamond-Plated Gold Coin", 4),
    HEXAGONAL_DIAMOND_PLATED_GOLD(100, "Hexagonal Diamond-Plated Gold Coin",
            "Blank Hexagonal Diamond-Plated Gold Coin", 2);

    private final int value;
    private final String issuedName;
    private final String blankName;
    private final int pressOutputCount;

    CoinDenomination(int value, String issuedName, String blankName, int pressOutputCount) {
        this.value = value;
        this.issuedName = issuedName;
        this.blankName = blankName;
        this.pressOutputCount = pressOutputCount;
    }

    public int value() {
        return value;
    }

    public String issuedName() {
        return issuedName;
    }

    public String issuedName(String currencyCode) {
        return issuedName + " [" + currencyCode + "]";
    }

    public String blankName() {
        return blankName;
    }

    public int pressOutputCount() {
        return pressOutputCount;
    }

    public Item blankItem() {
        return switch (this) {
            case COPPER -> ModItems.BLANK_COPPER_COIN.get();
            case IRON -> ModItems.BLANK_IRON_COIN.get();
            case SILVER -> ModItems.BLANK_SILVER_COIN.get();
            case GOLD -> ModItems.BLANK_GOLD_COIN.get();
            case DIAMOND_PLATED_GOLD -> ModItems.BLANK_DIAMOND_PLATED_GOLD_COIN.get();
            case HEXAGONAL_DIAMOND_PLATED_GOLD ->
                    ModItems.BLANK_HEXAGONAL_DIAMOND_PLATED_GOLD_COIN.get();
        };
    }

    public boolean matchesBlank(ItemStack stack) {
        if (stack.is(blankItem())) {
            return true;
        }
        // Preserve old worlds: the former generic Blank Coin acts as a copper blank.
        return this == COPPER && stack.is(ModItems.COIN_BLANK.get());
    }

    public static Optional<CoinDenomination> fromValue(int value) {
        return Arrays.stream(values()).filter(type -> type.value == value).findFirst();
    }

    public static Optional<CoinDenomination> fromBlank(ItemStack stack) {
        return Arrays.stream(values()).filter(type -> type.matchesBlank(stack)).findFirst();
    }

    public static List<Integer> valuesList() {
        return Arrays.stream(values()).map(CoinDenomination::value).toList();
    }
}
