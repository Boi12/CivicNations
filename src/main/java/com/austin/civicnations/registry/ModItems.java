package com.austin.civicnations.registry;

import com.austin.civicnations.CivicNations;
import com.austin.civicnations.item.NationCurrencyItem;
import com.austin.civicnations.item.NationLedgerItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModItems {
    private static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, CivicNations.MOD_ID);

    public static final RegistryObject<Item> NATION_LEDGER = ITEMS.register(
            "nation_ledger",
            () -> new NationLedgerItem(new Item.Properties().stacksTo(1))
    );

    public static final RegistryObject<Item> NATION_CURRENCY = ITEMS.register(
            "nation_currency",
            () -> new NationCurrencyItem(new Item.Properties().stacksTo(64))
    );

    public static final RegistryObject<Item> COIN_MINT = ITEMS.register(
            "coin_mint",
            () -> new BlockItem(ModBlocks.COIN_MINT.get(), new Item.Properties())
    );

    public static final RegistryObject<Item> COIN_PRESS = ITEMS.register(
            "coin_press",
            () -> new BlockItem(ModBlocks.COIN_PRESS.get(), new Item.Properties())
    );

    /**
     * Legacy generic blank retained so existing worlds do not lose registered items.
     * It is accepted as a copper blank, but cannot be crafted anymore.
     */
    public static final RegistryObject<Item> COIN_BLANK = ITEMS.register(
            "coin_blank",
            () -> new Item(new Item.Properties().stacksTo(64))
    );

    public static final RegistryObject<Item> BLANK_COPPER_COIN = blankCoin("blank_copper_coin");
    public static final RegistryObject<Item> BLANK_IRON_COIN = blankCoin("blank_iron_coin");
    public static final RegistryObject<Item> BLANK_SILVER_COIN = blankCoin("blank_silver_coin");
    public static final RegistryObject<Item> BLANK_GOLD_COIN = blankCoin("blank_gold_coin");
    public static final RegistryObject<Item> BLANK_DIAMOND_PLATED_GOLD_COIN =
            blankCoin("blank_diamond_plated_gold_coin");
    public static final RegistryObject<Item> BLANK_HEXAGONAL_DIAMOND_PLATED_GOLD_COIN =
            blankCoin("blank_hexagonal_diamond_plated_gold_coin");

    private ModItems() {}

    private static RegistryObject<Item> blankCoin(String name) {
        return ITEMS.register(name, () -> new Item(new Item.Properties().stacksTo(64)));
    }

    public static void register(IEventBus bus) {
        ITEMS.register(bus);
    }

    public static void addToCreativeTabs(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey().equals(CreativeModeTabs.TOOLS_AND_UTILITIES)) {
            event.accept(NATION_LEDGER.get());
            event.accept(COIN_PRESS.get());
            event.accept(COIN_MINT.get());
            event.accept(BLANK_COPPER_COIN.get());
            event.accept(BLANK_IRON_COIN.get());
            event.accept(BLANK_SILVER_COIN.get());
            event.accept(BLANK_GOLD_COIN.get());
            event.accept(BLANK_DIAMOND_PLATED_GOLD_COIN.get());
            event.accept(BLANK_HEXAGONAL_DIAMOND_PLATED_GOLD_COIN.get());
        }
    }
}
