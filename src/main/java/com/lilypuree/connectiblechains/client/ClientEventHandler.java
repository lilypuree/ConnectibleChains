package com.lilypuree.connectiblechains.client;

import com.lilypuree.connectiblechains.ConnectibleChains;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(value = Dist.CLIENT, modid = ConnectibleChains.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ClientEventHandler {
    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        ClientInitializer.chainPacketHandler.tick();
    }


}
