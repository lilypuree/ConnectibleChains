package com.lilypuree.connectiblechains.client;

import com.lilypuree.connectiblechains.ConnectibleChains;
import com.lilypuree.connectiblechains.client.render.entity.ChainCollisionEntityRenderer;
import com.lilypuree.connectiblechains.client.render.entity.ChainKnotEntityRenderer;
import com.lilypuree.connectiblechains.client.render.entity.model.ChainKnotEntityModel;
import com.lilypuree.connectiblechains.entity.ChainKnotEntity;
import com.lilypuree.connectiblechains.entity.ModEntityTypes;
import com.lilypuree.connectiblechains.util.Helper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(value = Dist.CLIENT, modid = ConnectibleChains.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ClientEventHandler {
    public static final ModelLayerLocation CHAIN_KNOT = new ModelLayerLocation(Helper.identifier("chain_knot"), "main");

    private static ChainKnotEntityRenderer chainKnotEntityRenderer;

    @SubscribeEvent
    public static void onRegisterLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(CHAIN_KNOT, ChainKnotEntityModel::getTexturedModelData);
    }

    @SubscribeEvent
    public static void onRegisterRendereres(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntityTypes.CHAIN_COLLISION.get(), ChainCollisionEntityRenderer::new);
        event.registerEntityRenderer(ModEntityTypes.CHAIN_KNOT.get(), ctx -> {
            chainKnotEntityRenderer = new ChainKnotEntityRenderer(ctx);
            return chainKnotEntityRenderer;
        });
    }

    @SubscribeEvent
    public static void onClientConfigReload(ModConfigEvent.Reloading event) {
        chainKnotEntityRenderer.getChainRenderer().purge();
    }
}
