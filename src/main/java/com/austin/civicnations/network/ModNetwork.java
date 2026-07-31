package com.austin.civicnations.network;

import com.austin.civicnations.CivicNations;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.Optional;

public final class ModNetwork {
    private static final String PROTOCOL = "9";
    private static int packetId = 0;

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(CivicNations.MOD_ID, "main"),
            () -> PROTOCOL,
            PROTOCOL::equals,
            PROTOCOL::equals
    );

    private ModNetwork() {}

    public static void register() {
        CHANNEL.registerMessage(
                packetId++,
                CreateNationPacket.class,
                CreateNationPacket::encode,
                CreateNationPacket::decode,
                CreateNationPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_SERVER)
        );
        CHANNEL.registerMessage(
                packetId++,
                ManageMemberPacket.class,
                ManageMemberPacket::encode,
                ManageMemberPacket::decode,
                ManageMemberPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_SERVER)
        );
        CHANNEL.registerMessage(
                packetId++,
                MintCurrencyPacket.class,
                MintCurrencyPacket::encode,
                MintCurrencyPacket::decode,
                MintCurrencyPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_SERVER)
        );
        CHANNEL.registerMessage(
                packetId++,
                PressCoinsPacket.class,
                PressCoinsPacket::encode,
                PressCoinsPacket::decode,
                PressCoinsPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_SERVER)
        );
        CHANNEL.registerMessage(
                packetId++,
                PurseActionPacket.class,
                PurseActionPacket::encode,
                PurseActionPacket::decode,
                PurseActionPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_SERVER)
        );
    }
}
