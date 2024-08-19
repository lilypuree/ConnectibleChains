package com.lilypuree.connectiblechains.networking;

import com.lilypuree.connectiblechains.ConnectibleChains;
import com.lilypuree.connectiblechains.networking.packet.ChainAttachPayload;
import com.lilypuree.connectiblechains.networking.packet.KnotChangePayload;
import com.lilypuree.connectiblechains.networking.packet.MultiChainAttachPayload;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

@EventBusSubscriber(modid = ConnectibleChains.MODID, bus = EventBusSubscriber.Bus.MOD)
public class NetworkHandler {
    private static final String PROTOCOL_VERSION = "3";

    @SubscribeEvent
    public static void onRegisterPayloads(final RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(PROTOCOL_VERSION);

        registrar.playToClient(
                ChainAttachPayload.TYPE,
                ChainAttachPayload.STREAM_CODEC,
                ChainAttachPayload::apply).playToClient(
                KnotChangePayload.TYPE,
                KnotChangePayload.STREAM_CODEC,
                KnotChangePayload::apply).playToClient(
                MultiChainAttachPayload.TYPE,
                MultiChainAttachPayload.STREAM_CODEC,
                MultiChainAttachPayload::apply);
    }
}
