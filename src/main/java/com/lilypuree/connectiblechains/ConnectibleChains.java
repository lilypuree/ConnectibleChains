package com.lilypuree.connectiblechains;

import com.lilypuree.connectiblechains.entity.ChainKnotEntity;
import com.lilypuree.connectiblechains.entity.ModEntityTypes;
import com.lilypuree.connectiblechains.events.ClientEventHandler;
import com.lilypuree.connectiblechains.network.ModPacketHandler;
import net.minecraft.block.Block;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.event.server.FMLServerStartingEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

// The value here should match an entry in the META-INF/mods.toml file
@Mod(ConnectibleChains.MODID)
public class ConnectibleChains {

    public static final String MODID = "connectiblechains";

    public ConnectibleChains() {
        ModEntityTypes.register();
        DataSerializers.registerSerializer(ChainKnotEntity.INTEGER_LIST);
        FMLJavaModLoadingContext.get().getModEventBus().addListener(ConnectibleChains::setup);
        FMLJavaModLoadingContext.get().getModEventBus().addListener(ClientEventHandler::init);
    }

    public static void setup(FMLCommonSetupEvent event){
        ModPacketHandler.registerMessages();
    }
}
