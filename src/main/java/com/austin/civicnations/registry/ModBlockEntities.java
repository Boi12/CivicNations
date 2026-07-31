package com.austin.civicnations.registry;

import com.austin.civicnations.CivicNations;
import com.austin.civicnations.block.entity.CoinPressBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModBlockEntities {
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, CivicNations.MOD_ID);

    public static final RegistryObject<BlockEntityType<CoinPressBlockEntity>> COIN_PRESS =
            BLOCK_ENTITIES.register("coin_press", () -> BlockEntityType.Builder.of(
                    CoinPressBlockEntity::new, ModBlocks.COIN_PRESS.get()).build(null));

    private ModBlockEntities() {}

    public static void register(IEventBus bus) {
        BLOCK_ENTITIES.register(bus);
    }
}
