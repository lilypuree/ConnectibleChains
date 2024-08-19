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

package com.lilypuree.connectiblechains;

import com.lilypuree.connectiblechains.entity.ModEntityTypes;
import com.lilypuree.connectiblechains.item.ChainItemInfo;
import net.minecraft.world.InteractionResult;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Mod Initializer for Connectible chains.
 */
@Mod(ConnectibleChains.MODID)
public class ConnectibleChains {

    /**
     * All mods need to have an ID, that is what tells the game and fabric what each mod is.
     * These need to be unique for all mods, and always stay the same in your mod, so by creating a field
     * it will be a lot easier!
     */
    public static final String MODID = "connectiblechains";
    public static final Logger LOGGER = LogManager.getLogger("ConnectibleChains");
    public static CCConfig runtimeConfig;

    /**
     * Here is where the fun begins.
     */
    public ConnectibleChains(IEventBus modEventBus, ModContainer modContainer) {

        ModEntityTypes.register(modEventBus);

        // On Clicking with a Chain event.
        NeoForge.EVENT_BUS.addListener(this::onBlockUse);


        runtimeConfig = new CCConfig();
        modContainer.registerConfig(ModConfig.Type.COMMON, CCConfig.COMMON_CONFIG);
        modContainer.registerConfig(ModConfig.Type.CLIENT, CCConfig.CLIENT_CONFIG);
    }

    public void onBlockUse(PlayerInteractEvent.RightClickBlock event) {

        InteractionResult result = ChainItemInfo.chainUseEvent(event.getEntity(), event.getLevel(), event.getHand(), event.getHitVec().getBlockPos());

        if (result != InteractionResult.PASS) {
            event.setCanceled(true);
            event.setCancellationResult(result);
        }
    }
}
