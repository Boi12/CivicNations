package com.austin.civicnations.client;

import com.austin.civicnations.CivicNations;
import com.austin.civicnations.client.screen.CoinMintScreen;
import com.austin.civicnations.client.screen.CoinPressScreen;
import com.austin.civicnations.client.screen.CoinPurseScreen;
import com.austin.civicnations.client.screen.NationCreationScreen;
import com.austin.civicnations.client.screen.NationOverviewScreen;
import com.austin.civicnations.registry.ModMenus;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(modid = CivicNations.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientSetup {
    private ClientSetup() {}

    @SubscribeEvent
    public static void clientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            MenuScreens.register(ModMenus.NATION_CREATION.get(), NationCreationScreen::new);
            MenuScreens.register(ModMenus.NATION_OVERVIEW.get(), NationOverviewScreen::new);
            MenuScreens.register(ModMenus.COIN_MINT.get(), CoinMintScreen::new);
            MenuScreens.register(ModMenus.COIN_PRESS.get(), CoinPressScreen::new);
            MenuScreens.register(ModMenus.COIN_PURSE.get(), CoinPurseScreen::new);
            CivicNations.LOGGER.info("Registered Civic Nations screens");
        });
    }
}
