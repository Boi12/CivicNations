package com.austin.civicnations.service;

import com.austin.civicnations.block.CoinPressBlock;
import com.austin.civicnations.block.entity.CoinPressBlockEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;

public final class CoinPressService {
    private CoinPressService() {}

    public static NationActionResult toggleMachine(ServerPlayer player, BlockPos pressPos) {
        if (!CoinPressBlock.isComplete(player.serverLevel(), pressPos)) {
            return send(player, failure("The Coin Press is incomplete or no longer present."));
        }
        if (player.distanceToSqr(pressPos.getX() + 0.5D, pressPos.getY() + 1.0D,
                pressPos.getZ() + 0.5D) > 64.0D) {
            return send(player, failure("You are too far away from the Coin Press."));
        }

        BlockEntity blockEntity = player.serverLevel().getBlockEntity(pressPos);
        if (!(blockEntity instanceof CoinPressBlockEntity press)) {
            return send(player, failure("The Coin Press inventory is unavailable."));
        }

        if (press.isRunning()) {
            press.stopProcessing();
            return send(player, success("Coin Press stopped."));
        }
        if (press.detectedRecipe().isEmpty()) {
            return send(player, failure("Insert a valid coin recipe before starting the machine."));
        }
        if (!press.canOutput(press.detectedRecipe().orElseThrow())) {
            return send(player, failure("The output slot is full or contains a different coin."));
        }
        if (!press.startProcessing()) {
            return send(player, failure("The Coin Press could not start."));
        }
        return send(player, success("Coin Press started."));
    }

    private static NationActionResult send(ServerPlayer player, NationActionResult result) {
        Component message = result.message().copy().withStyle(
                result.success() ? ChatFormatting.GREEN : ChatFormatting.RED);
        player.sendSystemMessage(message);
        if (!result.success()) {
            player.displayClientMessage(message, true);
        }
        return result;
    }

    private static NationActionResult success(String message) {
        return NationActionResult.success(Component.literal(message));
    }

    private static NationActionResult failure(String message) {
        return NationActionResult.failure(Component.literal(message));
    }
}
