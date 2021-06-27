package com.lilypuree.connectiblechains.events;

import com.lilypuree.connectiblechains.ConnectibleChains;
import com.lilypuree.connectiblechains.client.render.entity.ChainCollisionEntityRenderer;
import com.lilypuree.connectiblechains.client.render.entity.ChainKnotEntityRenderer;
import com.lilypuree.connectiblechains.entity.ChainCollisionEntity;
import com.lilypuree.connectiblechains.entity.ModEntityTypes;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.client.registry.RenderingRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(modid = ConnectibleChains.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ClientEventHandler {

    public static void init(final FMLClientSetupEvent event) {
        RenderingRegistry.registerEntityRenderingHandler(ModEntityTypes.CHAIN_KNOT.get(), ChainKnotEntityRenderer::new);
        RenderingRegistry.registerEntityRenderingHandler(ModEntityTypes.CHAIN_COLLISION.get(), ChainCollisionEntityRenderer::new);
    }

}
