package com.austin.civicnations.network;

import com.austin.civicnations.data.BannerPatternLayer;
import com.austin.civicnations.menu.NationCreationMenu;
import com.austin.civicnations.service.NationService;
import com.austin.civicnations.util.BannerDesign;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.DyeColor;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public record CreateNationPacket(int containerId, String name, String abbreviation,
                                 int bannerColorId, List<BannerPatternLayer> layers) {
    public CreateNationPacket {
        layers = List.copyOf(layers);
    }

    public static void encode(CreateNationPacket packet, FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.containerId);
        buffer.writeUtf(packet.name, 24);
        buffer.writeUtf(packet.abbreviation, 5);
        buffer.writeVarInt(packet.bannerColorId);
        buffer.writeVarInt(Math.min(packet.layers.size(), BannerDesign.MAX_LAYERS));
        packet.layers.stream().limit(BannerDesign.MAX_LAYERS).forEach(layer -> {
            buffer.writeUtf(layer.patternId(), 8);
            buffer.writeVarInt(layer.colorId());
        });
    }

    public static CreateNationPacket decode(FriendlyByteBuf buffer) {
        int containerId = buffer.readVarInt();
        String name = buffer.readUtf(24);
        String abbreviation = buffer.readUtf(5);
        int bannerColorId = buffer.readVarInt();
        int count = buffer.readVarInt();
        if (count < 0 || count > BannerDesign.MAX_LAYERS) {
            throw new IllegalArgumentException("Invalid banner layer count: " + count);
        }
        List<BannerPatternLayer> layers = new ArrayList<>();
        for (int index = 0; index < count; index++) {
            layers.add(new BannerPatternLayer(buffer.readUtf(8), buffer.readVarInt()));
        }
        return new CreateNationPacket(containerId, name, abbreviation, bannerColorId, layers);
    }

    public static void handle(CreateNationPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) {
                return;
            }

            if (!(player.containerMenu instanceof NationCreationMenu menu)
                    || menu.containerId != packet.containerId) {
                return;
            }

            if (packet.bannerColorId < 0 || packet.bannerColorId >= DyeColor.values().length) {
                return;
            }
            List<BannerPatternLayer> sanitized = BannerDesign.sanitize(packet.layers);
            if (sanitized.size() != packet.layers.size()) {
                return;
            }

            DyeColor color = DyeColor.byId(packet.bannerColorId);
            NationService.createNation(
                    player,
                    packet.name,
                    packet.abbreviation,
                    BannerDesign.create(color, sanitized)
            );
        });
        context.setPacketHandled(true);
    }
}
