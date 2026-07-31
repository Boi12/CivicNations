package com.austin.civicnations.network;

import com.austin.civicnations.menu.CoinPurseMenu;
import com.austin.civicnations.util.CoinDenomination;
import com.austin.civicnations.util.PurseStorage;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

public record PurseActionPacket(int containerId, UUID purseId, Action action,
                                int denominationValue) {
    public enum Action {
        DEPOSIT_ALL,
        WITHDRAW,
        UNBIND
    }

    public static void encode(PurseActionPacket packet, FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.containerId);
        buffer.writeUUID(packet.purseId);
        buffer.writeEnum(packet.action);
        buffer.writeVarInt(packet.denominationValue);
    }

    public static PurseActionPacket decode(FriendlyByteBuf buffer) {
        return new PurseActionPacket(buffer.readVarInt(), buffer.readUUID(),
                buffer.readEnum(Action.class), buffer.readVarInt());
    }

    public static void handle(PurseActionPacket packet,
                              Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null
                    || !(player.containerMenu instanceof CoinPurseMenu menu)
                    || menu.containerId != packet.containerId
                    || !menu.purseId().equals(packet.purseId)) {
                return;
            }
            ItemStack purse = menu.purseStack();
            if (purse.isEmpty()) {
                return;
            }

            switch (packet.action) {
                case DEPOSIT_ALL -> {
                    long deposited = PurseStorage.depositAll(player, purse);
                    player.sendSystemMessage(Component.literal(deposited > 0L
                            ? "Deposited " + deposited + " coins into the purse."
                            : "No matching currency was available to deposit."));
                }
                case WITHDRAW -> CoinDenomination.fromValue(packet.denominationValue)
                        .ifPresent(denomination -> {
                            int withdrawn = PurseStorage.withdraw(player, purse, denomination);
                            if (withdrawn == 0) {
                                player.sendSystemMessage(Component.literal(
                                        "That denomination is not stored in this purse."));
                            }
                        });
                case UNBIND -> player.sendSystemMessage(Component.literal(
                        PurseStorage.unbind(purse)
                                ? "The empty purse is now unbound."
                                : "The purse must be empty before it can be unbound."));
            }
            player.getInventory().setChanged();
            menu.broadcastChanges();
        });
        context.setPacketHandled(true);
    }
}
