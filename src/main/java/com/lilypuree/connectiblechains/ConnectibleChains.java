package com.lilypuree.connectiblechains;

import com.lilypuree.connectiblechains.entity.ModEntityTypes;
import com.lilypuree.connectiblechains.item.ChainItemInfo;
import com.lilypuree.connectiblechains.network.ModPacketHandler;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

// The value here should match an entry in the META-INF/mods.toml file
@Mod(ConnectibleChains.MODID)
public class ConnectibleChains {
    public static final String MODID = "connectiblechains";
    public static final Logger LOGGER = LogManager.getLogger(MODID);
    public static CCConfig runtimeConfig;

    public ConnectibleChains() {
        ModEntityTypes.register();

        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::setup);
        MinecraftForge.EVENT_BUS.addListener(ChainItemInfo::chainUseEvent);
//        MinecraftForge.EVENT_BUS.addListener(this::onBlockBreak);

        runtimeConfig = new CCConfig();
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, CCConfig.COMMON_CONFIG);
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, CCConfig.CLIENT_CONFIG);
    }

    public void setup(FMLCommonSetupEvent event) {
        ModPacketHandler.registerMessages();
    }


}
