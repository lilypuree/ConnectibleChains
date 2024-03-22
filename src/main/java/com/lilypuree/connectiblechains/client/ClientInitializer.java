package com.lilypuree.connectiblechains.client;

import com.lilypuree.connectiblechains.ConnectibleChains;
import com.lilypuree.connectiblechains.client.render.entity.ChainCollisionEntityRenderer;
import com.lilypuree.connectiblechains.client.render.entity.ChainKnotEntityRenderer;
import com.lilypuree.connectiblechains.client.render.entity.ChainTextureManager;
import com.lilypuree.connectiblechains.client.render.entity.model.ChainKnotEntityModel;
import com.lilypuree.connectiblechains.entity.ChainCollisionEntity;
import com.lilypuree.connectiblechains.entity.ModEntityTypes;
import com.lilypuree.connectiblechains.util.Helper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.player.LocalPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.common.Tags;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;

@Mod.EventBusSubscriber(value = Dist.CLIENT, modid = ConnectibleChains.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ClientInitializer {
    public static final ModelLayerLocation CHAIN_KNOT = new ModelLayerLocation(Helper.identifier("chain_knot"), "main");

    public static final ChainTextureManager textureManager = new ChainTextureManager();
    protected static ChainKnotEntityRenderer chainKnotEntityRenderer;
    public static ChainPacketHandler chainPacketHandler;

    static {
        chainPacketHandler = new ChainPacketHandler();
    }

    @SubscribeEvent
    public static void onRegisterLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(CHAIN_KNOT, ChainKnotEntityModel::getTexturedModelData);
    }

    @SubscribeEvent
    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntityTypes.CHAIN_COLLISION.get(), ChainCollisionEntityRenderer::new);
        event.registerEntityRenderer(ModEntityTypes.CHAIN_KNOT.get(), ctx -> {
            chainKnotEntityRenderer = new ChainKnotEntityRenderer(ctx);
            return chainKnotEntityRenderer;
        });
    }

    @SubscribeEvent
    public static void onClientConfigReload(ModConfigEvent.Reloading event) {
        if (chainKnotEntityRenderer != null)
            chainKnotEntityRenderer.getChainRenderer().purge();
    }

    public static boolean checkCollisionEntityWithinRenderDistance(ChainCollisionEntity entity, double distance) {
        LocalPlayer player = Minecraft.getInstance().player;
        return player != null && player.isHolding(item -> item.is(Tags.Items.SHEARS));
    }
}
