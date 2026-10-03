package com.onceheart.gazeofthepantheon.network;

import com.onceheart.gazeofthepantheon.GazeOfThePantheon;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public class ModNetwork {

    private static final String VERSION = "1";

    public static final SimpleChannel CHANNEL = NetworkRegistry.ChannelBuilder
            .named(new ResourceLocation(GazeOfThePantheon.MOD_ID, "main"))
            .networkProtocolVersion(() -> VERSION)
            .clientAcceptedVersions(VERSION::equals)
            .serverAcceptedVersions(VERSION::equals)
            .simpleChannel();

    public static void register() {
        int id = 0;
        CHANNEL.registerMessage(id++, ToggleHermesPacket.class,
                ToggleHermesPacket::encode,
                ToggleHermesPacket::decode,
                ToggleHermesPacket::handle);
        CHANNEL.registerMessage(id++, OpenEdictPacket.class,
                OpenEdictPacket::encode,
                OpenEdictPacket::decode,
                OpenEdictPacket::handle);
        CHANNEL.registerMessage(id++, ToggleEdictEffectPacket.class,
                ToggleEdictEffectPacket::encode,
                ToggleEdictEffectPacket::decode,
                ToggleEdictEffectPacket::handle);
    }
}