package com.austin.civicnations.network;

import com.austin.civicnations.menu.CoinMintMenu;
import com.austin.civicnations.service.CurrencyService;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record MintCurrencyPacket(int containerId, BlockPos mintPos,
                                 int denomination, int count) {
    public static void encode(MintCurrencyPacket packet, FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.containerId);
        buffer.writeBlockPos(packet.mintPos);
        buffer.writeVarInt(packet.denomination);
        buffer.writeVarInt(packet.count);
    }

    public static MintCurrencyPacket decode(FriendlyByteBuf buffer) {
        return new MintCurrencyPacket(
                buffer.readVarInt(),
                buffer.readBlockPos(),
                buffer.readVarInt(),
                buffer.readVarInt()
        );
    }

    public static void handle(MintCurrencyPacket packet,
                              Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null
                    || !(player.containerMenu instanceof CoinMintMenu menu)
                    || menu.containerId != packet.containerId
                    || !menu.mintPos().equals(packet.mintPos)) {
                return;
            }
            CurrencyService.mintAtCoinMint(player, packet.mintPos,
                    packet.denomination, packet.count);
        });
        context.setPacketHandled(true);
    }
}
