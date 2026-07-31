package com.austin.civicnations.registry;

import com.austin.civicnations.CivicNations;
import com.austin.civicnations.block.CoinMintBlock;
import com.austin.civicnations.block.CoinPressBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModBlocks {
    private static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, CivicNations.MOD_ID);

    public static final RegistryObject<Block> COIN_MINT = BLOCKS.register(
            "coin_mint",
            () -> new CoinMintBlock(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK)
                    .strength(5.0F, 6.0F)
                    .requiresCorrectToolForDrops())
    );

    public static final RegistryObject<Block> COIN_PRESS = BLOCKS.register(
            "coin_press",
            () -> new CoinPressBlock(BlockBehaviour.Properties.copy(Blocks.ANVIL)
                    .strength(5.0F, 6.0F)
                    .requiresCorrectToolForDrops()
                    .noOcclusion())
    );

    private ModBlocks() {}

    public static void register(IEventBus bus) {
        BLOCKS.register(bus);
    }
}
