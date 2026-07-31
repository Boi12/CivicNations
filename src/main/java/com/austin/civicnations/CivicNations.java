package com.austin.civicnations;

import com.austin.civicnations.command.NationCommands;
import com.austin.civicnations.integration.FTBIntegration;
import com.austin.civicnations.network.ModNetwork;
import com.austin.civicnations.registry.ModBlockEntities;
import com.austin.civicnations.registry.ModBlocks;
import com.austin.civicnations.registry.ModItems;
import com.austin.civicnations.registry.ModMenus;
import com.mojang.logging.LogUtils;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

@Mod(CivicNations.MOD_ID)
public final class CivicNations {
    public static final String MOD_ID = "civicnations";
    public static final Logger LOGGER = LogUtils.getLogger();

    public CivicNations() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();

        ModBlocks.register(modBus);
        ModBlockEntities.register(modBus);
        ModItems.register(modBus);
        modBus.addListener(ModItems::addToCreativeTabs);
        ModMenus.register(modBus);
        ModNetwork.register();

        MinecraftForge.EVENT_BUS.addListener(this::onServerStarted);
        MinecraftForge.EVENT_BUS.addListener(NationCommands::onRegister);
        FTBIntegration.register();
    }

    private void onServerStarted(ServerStartedEvent event) {
        FTBIntegration.applyServerRules(event.getServer());
    }
}
