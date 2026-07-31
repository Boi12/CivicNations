package com.austin.civicnations.registry;

import com.austin.civicnations.CivicNations;
import com.austin.civicnations.menu.CoinMintMenu;
import com.austin.civicnations.menu.CoinPressMenu;
import com.austin.civicnations.menu.NationCreationMenu;
import com.austin.civicnations.menu.NationOverviewMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModMenus {
    private static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(ForgeRegistries.MENU_TYPES, CivicNations.MOD_ID);

    public static final RegistryObject<MenuType<NationCreationMenu>> NATION_CREATION = MENUS.register(
            "nation_creation",
            () -> IForgeMenuType.create(NationCreationMenu::fromNetwork)
    );

    public static final RegistryObject<MenuType<NationOverviewMenu>> NATION_OVERVIEW = MENUS.register(
            "nation_overview",
            () -> IForgeMenuType.create(NationOverviewMenu::fromNetwork)
    );

    public static final RegistryObject<MenuType<CoinMintMenu>> COIN_MINT = MENUS.register(
            "coin_mint",
            () -> IForgeMenuType.create(CoinMintMenu::fromNetwork)
    );

    public static final RegistryObject<MenuType<CoinPressMenu>> COIN_PRESS = MENUS.register(
            "coin_press",
            () -> IForgeMenuType.create(CoinPressMenu::fromNetwork)
    );

    private ModMenus() {}

    public static void register(IEventBus bus) {
        MENUS.register(bus);
    }
}
