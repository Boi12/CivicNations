package com.austin.civicnations.network;

import com.austin.civicnations.data.NationRecord;
import com.austin.civicnations.data.NationRole;
import com.austin.civicnations.data.NationSavedData;
import com.austin.civicnations.menu.NationOverviewMenu;
import com.austin.civicnations.service.NationActionResult;
import com.austin.civicnations.service.NationService;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

public record ManageMemberPacket(int containerId, UUID nationId, UUID targetId, Action action) {
    public static void encode(ManageMemberPacket packet, FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.containerId);
        buffer.writeUUID(packet.nationId);
        buffer.writeUUID(packet.targetId);
        buffer.writeEnum(packet.action);
    }

    public static ManageMemberPacket decode(FriendlyByteBuf buffer) {
        return new ManageMemberPacket(
                buffer.readVarInt(),
                buffer.readUUID(),
                buffer.readUUID(),
                buffer.readEnum(Action.class)
        );
    }

    public static void handle(ManageMemberPacket packet,
                              Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer actor = context.getSender();
            if (actor == null
                    || !(actor.containerMenu instanceof NationOverviewMenu menu)
                    || menu.containerId != packet.containerId
                    || !menu.nationId().equals(packet.nationId)) {
                return;
            }

            NationSavedData data = NationSavedData.get(actor.server);
            Optional<NationRecord> nationOptional = data.getNation(packet.nationId);
            ServerPlayer target = actor.server.getPlayerList().getPlayer(packet.targetId);
            if (nationOptional.isEmpty() || target == null
                    || !nationOptional.get().isMember(packet.targetId)) {
                NationService.sendResult(actor, NationActionResult.failure(
                        net.minecraft.network.chat.Component.literal(
                                "That citizen must be online to manage their role.")));
                return;
            }

            NationRecord nation = nationOptional.get();
            NationActionResult result;
            if (nation.serverOwned() && actor.hasPermissions(2)) {
                result = switch (packet.action) {
                    case CITIZEN -> NationService.adminSetRole(actor, target, NationRole.CITIZEN);
                    case OFFICIAL -> NationService.adminSetRole(actor, target, NationRole.OFFICIAL);
                    case TREASURER -> NationService.adminSetRole(actor, target, NationRole.TREASURER);
                    case REMOVE -> NationService.adminRemoveMember(actor, target);
                    case TRANSFER -> NationActionResult.failure(
                            net.minecraft.network.chat.Component.literal(
                                    "Server-owned nations do not transfer leadership."));
                };
                if (packet.action == Action.TRANSFER) {
                    NationService.sendResult(actor, result);
                }
            } else {
                result = switch (packet.action) {
                    case CITIZEN -> NationService.setRole(actor, target, NationRole.CITIZEN);
                    case OFFICIAL -> NationService.setRole(actor, target, NationRole.OFFICIAL);
                    case TREASURER -> NationService.setRole(actor, target, NationRole.TREASURER);
                    case REMOVE -> NationService.removeMember(actor, target);
                    case TRANSFER -> NationService.transferLeadership(actor, target);
                };
            }

            if (result.success()) {
                actor.closeContainer();
            }
        });
        context.setPacketHandled(true);
    }

    public enum Action {
        CITIZEN,
        OFFICIAL,
        TREASURER,
        REMOVE,
        TRANSFER
    }
}
