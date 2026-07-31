package com.austin.civicnations.network;

import com.austin.civicnations.menu.CoinPressMenu;
import com.austin.civicnations.service.CoinPressService;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Requests that the server start or stop the open Coin Press. */
public record PressCoinsPacket(int containerId, BlockPos pressPos) {
    public static void encode(PressCoinsPacket packet, FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.containerId);
        buffer.writeBlockPos(packet.pressPos);
    }

    public static PressCoinsPacket decode(FriendlyByteBuf buffer) {
        return new PressCoinsPacket(buffer.readVarInt(), buffer.readBlockPos());
    }

    public static void handle(PressCoinsPacket packet,
                              Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null
                    || !(player.containerMenu instanceof CoinPressMenu menu)
                    || menu.containerId != packet.containerId
                    || !menu.pressPos().equals(packet.pressPos)) {
                return;
            }
            CoinPressService.toggleMachine(player, packet.pressPos);
        });
        context.setPacketHandled(true);
    }
}
