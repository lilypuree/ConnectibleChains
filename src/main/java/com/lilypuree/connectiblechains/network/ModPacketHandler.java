package com.lilypuree.connectiblechains.network;

import com.lilypuree.connectiblechains.ConnectibleChains;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.network.NetworkDirection;
import net.minecraftforge.fml.network.NetworkRegistry;
import net.minecraftforge.fml.network.simple.SimpleChannel;

public class ModPacketHandler {
    private static final String PROTOCOL_VERSION = "1.0";

    private static SimpleChannel INSTANCE;
    private static int ID = 0;

    private static int nextID() {
        return ID++;
    }


    public static void registerMessages() {
        INSTANCE = NetworkRegistry.newSimpleChannel(
                new ResourceLocation(ConnectibleChains.MODID, "main"),
                () -> PROTOCOL_VERSION,
                PROTOCOL_VERSION::equals,
                PROTOCOL_VERSION::equals);

        INSTANCE.messageBuilder(S2CChainAttachPacket.class, nextID())
                .encoder(S2CChainAttachPacket::toBytes)
                .decoder(S2CChainAttachPacket::new)
                .consumer(S2CChainAttachPacket::handle)
                .add();

        INSTANCE.messageBuilder(S2CChainDetachPacket.class, nextID())
                .encoder(S2CChainDetachPacket::toBytes)
                .decoder(S2CChainDetachPacket::new)
                .consumer(S2CChainDetachPacket::handle)
                .add();

        INSTANCE.messageBuilder(S2CMultiChainAttachPacket.class, nextID())
                .encoder(S2CMultiChainAttachPacket::toBytes)
                .decoder(S2CMultiChainAttachPacket::new)
                .consumer(S2CMultiChainAttachPacket::handle)
                .add();

//        INSTANCE.messageBuilder(S2CMultiChainDetachPacket.class, nextID())
//                .encoder(S2CMultiChainDetachPacket::toBytes)
//                .decoder(S2CMultiChainDetachPacket::new)
//                .consumer(S2CMultiChainDetachPacket::handle)
//                .add();
    }

    public static void sendToClient(Object packet, ServerPlayerEntity player) {
        INSTANCE.sendTo(packet, player.connection.connection, NetworkDirection.PLAY_TO_CLIENT);
    }

    public static void sendToServer(Object packet) {
        INSTANCE.sendToServer(packet);
    }

}
