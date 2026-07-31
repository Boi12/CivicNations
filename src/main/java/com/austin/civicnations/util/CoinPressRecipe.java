package com.austin.civicnations.util;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.Optional;

/**
 * Fixed Coin Press recipes. The machine detects the recipe directly from its
 * material slots instead of asking the player to select a denomination or production count.
 */
public enum CoinPressRecipe {
    COPPER(CoinDenomination.COPPER, 1, 0,
            "1 Copper Ingot", "8 Blank Copper Coins"),
    IRON(CoinDenomination.IRON, 1, 0,
            "1 Iron Ingot", "8 Blank Iron Coins"),
    SILVER(CoinDenomination.SILVER, 1, 0,
            "1 Silver Ingot", "8 Blank Silver Coins"),
    GOLD(CoinDenomination.GOLD, 1, 0,
            "1 Gold Ingot", "8 Blank Gold Coins"),
    DIAMOND_PLATED_GOLD(CoinDenomination.DIAMOND_PLATED_GOLD, 1, 1,
            "1 Gold Ingot + 1 Diamond", "4 Blank Diamond-Plated Gold Coins"),
    HEXAGONAL_DIAMOND_PLATED_GOLD(CoinDenomination.HEXAGONAL_DIAMOND_PLATED_GOLD, 1, 2,
            "1 Gold Ingot + 2 Diamonds", "2 Blank Hexagonal Diamond-Plated Gold Coins");

    private static final TagKey<Item> SILVER_INGOTS = TagKey.create(
            Registries.ITEM, new ResourceLocation("forge", "ingots/silver"));

    private final CoinDenomination denomination;
    private final int primaryCount;
    private final int materialCount;
    private final String inputText;
    private final String outputText;

    CoinPressRecipe(CoinDenomination denomination, int primaryCount, int materialCount,
                    String inputText, String outputText) {
        this.denomination = denomination;
        this.primaryCount = primaryCount;
        this.materialCount = materialCount;
        this.inputText = inputText;
        this.outputText = outputText;
    }

    public CoinDenomination denomination() {
        return denomination;
    }

    public int primaryCount() {
        return primaryCount;
    }

    public int materialCount() {
        return materialCount;
    }

    public int outputCount() {
        return denomination.pressOutputCount();
    }

    public String inputText() {
        return inputText;
    }

    public String outputText() {
        return outputText;
    }

    public boolean matches(ItemStack primary, ItemStack materials) {
        if (primary.getCount() < primaryCount || materials.getCount() < materialCount) {
            return false;
        }
        return switch (this) {
            case COPPER -> primary.is(Items.COPPER_INGOT) && materials.isEmpty();
            case IRON -> primary.is(Items.IRON_INGOT) && materials.isEmpty();
            case SILVER -> isSilverIngot(primary) && materials.isEmpty();
            case GOLD -> primary.is(Items.GOLD_INGOT) && materials.isEmpty();
            case DIAMOND_PLATED_GOLD -> primary.is(Items.GOLD_INGOT)
                    && materials.is(Items.DIAMOND) && materials.getCount() == 1;
            case HEXAGONAL_DIAMOND_PLATED_GOLD -> primary.is(Items.GOLD_INGOT)
                    && materials.is(Items.DIAMOND) && materials.getCount() >= 2;
        };
    }

    public static Optional<CoinPressRecipe> find(ItemStack primary, ItemStack materials) {
        // Check the two-diamond recipe before the one-diamond recipe.
        if (HEXAGONAL_DIAMOND_PLATED_GOLD.matches(primary, materials)) {
            return Optional.of(HEXAGONAL_DIAMOND_PLATED_GOLD);
        }
        for (CoinPressRecipe recipe : values()) {
            if (recipe != HEXAGONAL_DIAMOND_PLATED_GOLD && recipe.matches(primary, materials)) {
                return Optional.of(recipe);
            }
        }
        return Optional.empty();
    }

    private static boolean isSilverIngot(ItemStack stack) {
        if (stack.is(SILVER_INGOTS)) {
            return true;
        }
        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
        return itemId.getNamespace().equals("geolosys")
                && itemId.getPath().equals("silver_ingot");
    }
}
