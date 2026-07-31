package com.austin.civicnations.block.entity;

import com.austin.civicnations.block.CoinPressBlock;
import com.austin.civicnations.menu.CoinPressMenu;
import com.austin.civicnations.registry.ModBlockEntities;
import com.austin.civicnations.util.CoinPressRecipe;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;
import java.util.Optional;

public final class CoinPressBlockEntity extends BlockEntity implements Container, MenuProvider {
    public static final int PRIMARY_SLOT = 0;
    public static final int MATERIAL_SLOT = 1;
    public static final int OUTPUT_SLOT = 2;
    public static final int SLOT_COUNT = 3;

    public static final int PROCESS_TIME = 60;
    public static final int DATA_PROGRESS = 0;
    public static final int DATA_PROCESS_TIME = 1;
    public static final int DATA_RUNNING = 2;
    public static final int DATA_RECIPE_VALUE = 3;
    public static final int DATA_COUNT = 4;

    private final NonNullList<ItemStack> items =
            NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);
    private int progress;
    private boolean running;
    private int activeRecipeValue;

    private final ContainerData dataAccess = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case DATA_PROGRESS -> progress;
                case DATA_PROCESS_TIME -> PROCESS_TIME;
                case DATA_RUNNING -> running ? 1 : 0;
                case DATA_RECIPE_VALUE -> activeRecipeValue;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case DATA_PROGRESS -> progress = value;
                case DATA_RUNNING -> running = value != 0;
                case DATA_RECIPE_VALUE -> activeRecipeValue = value;
                default -> {
                }
            }
        }

        @Override
        public int getCount() {
            return DATA_COUNT;
        }
    };

    public CoinPressBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.COIN_PRESS.get(), pos, state);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("menu.civicnations.coin_press");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return CoinPressMenu.serverMenu(containerId, inventory, worldPosition, this, dataAccess);
    }

    public ContainerData dataAccess() {
        return dataAccess;
    }

    public boolean isRunning() {
        return running;
    }

    public Optional<CoinPressRecipe> detectedRecipe() {
        return CoinPressRecipe.find(getItem(PRIMARY_SLOT), getItem(MATERIAL_SLOT));
    }

    public boolean canOutput(CoinPressRecipe recipe) {
        ItemStack output = getItem(OUTPUT_SLOT);
        if (output.isEmpty()) {
            return recipe.outputCount() <= recipe.denomination().blankItem().getDefaultInstance()
                    .getMaxStackSize();
        }
        return output.is(recipe.denomination().blankItem())
                && output.getCount() + recipe.outputCount() <= output.getMaxStackSize();
    }

    public boolean startProcessing() {
        Optional<CoinPressRecipe> recipe = detectedRecipe();
        if (recipe.isEmpty() || !canOutput(recipe.get())) {
            return false;
        }
        running = true;
        progress = 0;
        activeRecipeValue = recipe.get().denomination().value();
        setChanged();
        return true;
    }

    public void stopProcessing() {
        running = false;
        progress = 0;
        activeRecipeValue = 0;
        setChanged();
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state,
                                  CoinPressBlockEntity press) {
        if (!press.running) {
            return;
        }

        Optional<CoinPressRecipe> recipeOptional = press.detectedRecipe();
        if (recipeOptional.isEmpty() || !press.canOutput(recipeOptional.get())) {
            press.stopProcessing();
            return;
        }

        CoinPressRecipe recipe = recipeOptional.get();
        if (press.activeRecipeValue != recipe.denomination().value()) {
            press.progress = 0;
            press.activeRecipeValue = recipe.denomination().value();
        }

        press.progress++;
        press.setChanged();
        if (press.progress < PROCESS_TIME) {
            return;
        }

        press.finishRecipe(recipe);
        press.progress = 0;
        press.activeRecipeValue = 0;

        Optional<CoinPressRecipe> nextRecipe = press.detectedRecipe();
        if (nextRecipe.isEmpty() || !press.canOutput(nextRecipe.get())) {
            press.running = false;
        } else {
            press.activeRecipeValue = nextRecipe.get().denomination().value();
        }
        press.setChanged();
    }

    private void finishRecipe(CoinPressRecipe recipe) {
        getItem(PRIMARY_SLOT).shrink(recipe.primaryCount());
        if (recipe.materialCount() > 0) {
            getItem(MATERIAL_SLOT).shrink(recipe.materialCount());
        }

        ItemStack output = getItem(OUTPUT_SLOT);
        if (output.isEmpty()) {
            setItem(OUTPUT_SLOT,
                    new ItemStack(recipe.denomination().blankItem(), recipe.outputCount()));
        } else {
            output.grow(recipe.outputCount());
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        ContainerHelper.saveAllItems(tag, items);
        tag.putInt("Progress", progress);
        tag.putBoolean("Running", running);
        tag.putInt("ActiveRecipeValue", activeRecipeValue);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        items.clear();
        ContainerHelper.loadAllItems(tag, items);
        progress = tag.getInt("Progress");
        running = tag.getBoolean("Running");
        activeRecipeValue = tag.getInt("ActiveRecipeValue");
    }

    @Override
    public int getContainerSize() {
        return SLOT_COUNT;
    }

    @Override
    public boolean isEmpty() {
        for (ItemStack stack : items) {
            if (!stack.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public ItemStack getItem(int slot) {
        return items.get(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        ItemStack removed = ContainerHelper.removeItem(items, slot, amount);
        if (!removed.isEmpty()) {
            setChanged();
        }
        return removed;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        ItemStack removed = ContainerHelper.takeItem(items, slot);
        if (!removed.isEmpty()) {
            setChanged();
        }
        return removed;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        items.set(slot, stack);
        if (stack.getCount() > getMaxStackSize()) {
            stack.setCount(getMaxStackSize());
        }
        setChanged();
    }

    @Override
    public boolean stillValid(Player player) {
        if (level == null || level.getBlockEntity(worldPosition) != this
                || !CoinPressBlock.isComplete(level, worldPosition)) {
            return false;
        }
        return player.distanceToSqr(worldPosition.getX() + 0.5D,
                worldPosition.getY() + 1.0D,
                worldPosition.getZ() + 0.5D) <= 64.0D;
    }

    @Override
    public void clearContent() {
        items.clear();
        stopProcessing();
    }
}
