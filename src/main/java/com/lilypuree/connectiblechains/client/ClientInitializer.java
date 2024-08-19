/*
 * Copyright (C) 2024 legoatoom.
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.lilypuree.connectiblechains.client;

import com.lilypuree.connectiblechains.ConnectibleChains;
import com.lilypuree.connectiblechains.client.render.entity.ChainCollisionEntityRenderer;
import com.lilypuree.connectiblechains.client.render.entity.ChainKnotEntityRenderer;
import com.lilypuree.connectiblechains.client.render.entity.model.ChainKnotEntityModel;
import com.lilypuree.connectiblechains.client.render.entity.texture.ChainTextureManager;
import com.lilypuree.connectiblechains.entity.ModEntityTypes;
import com.lilypuree.connectiblechains.item.ChainItemInfo;
import com.lilypuree.connectiblechains.util.Helper;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

import java.util.Optional;

/**
 * ClientInitializer.
 * This method is called when the game starts with a client.
 * This registers the renderers for entities and how to handle packages between the server and client.
 *
 * @author legoatoom
 */
@Mod(value = ConnectibleChains.MODID, dist = Dist.CLIENT)
public class ClientInitializer {

    public static final ModelLayerLocation CHAIN_KNOT = new ModelLayerLocation(Helper.rl("chain_knot"), "main");
    private static ClientInitializer instance;
    private final ChainTextureManager chainTextureManager = new ChainTextureManager();
    private static ChainKnotEntityRenderer chainKnotEntityRenderer;


    public ClientInitializer(IEventBus modEventBus, ModContainer modContainer) {
        instance = this;
        modEventBus.addListener(this::registerRenderers);
        modEventBus.addListener(this::registerModelLayer);
        modEventBus.addListener(this::onClientConfigReload);
        modEventBus.addListener(this::registerReloadListener);

        modContainer.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
        // Tooltip for chains.
        NeoForge.EVENT_BUS.addListener(this::changeTooltip);
    }

    public void changeTooltip(ItemTooltipEvent event) {
        ChainItemInfo.infoToolTip(event.getItemStack(), event.getContext(), event.getFlags(), event.getToolTip());
    }

    public void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntityTypes.CHAIN_COLLISION.get(), ChainCollisionEntityRenderer::new);
        event.registerEntityRenderer(ModEntityTypes.CHAIN_KNOT.get(), ctx -> {
            chainKnotEntityRenderer = new ChainKnotEntityRenderer(ctx);
            return chainKnotEntityRenderer;
        });
    }

    public void registerModelLayer(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(CHAIN_KNOT, ChainKnotEntityModel::getTexturedModelData);
    }

    public void onClientConfigReload(ModConfigEvent.Reloading event) {
        if (chainKnotEntityRenderer != null)
            chainKnotEntityRenderer.getChainRenderer().purge();
    }

    public void registerReloadListener(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener(chainTextureManager);
    }

    public static ClientInitializer getInstance() {
        return instance;
    }

    public Optional<ChainKnotEntityRenderer> getChainKnotEntityRenderer() {
        return Optional.ofNullable(chainKnotEntityRenderer);
    }

    public ChainTextureManager getChainTextureManager() {
        return chainTextureManager;
    }
}
