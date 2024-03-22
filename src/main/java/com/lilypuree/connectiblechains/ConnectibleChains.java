package com.lilypuree.connectiblechains;

import com.lilypuree.connectiblechains.chain.ChainTypesRegistry;
import com.lilypuree.connectiblechains.compat.BuiltinCompat;
import com.lilypuree.connectiblechains.entity.ModEntityTypes;
import com.lilypuree.connectiblechains.item.ChainItemInfo;
import com.lilypuree.connectiblechains.network.ModPacketHandler;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
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
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();

        ModEntityTypes.register();
        ChainTypesRegistry.init(bus);
        BuiltinCompat.init();


        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::setup);
        MinecraftForge.EVENT_BUS.addListener(ChainItemInfo::chainUseEvent);

        runtimeConfig = new CCConfig();
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, CCConfig.COMMON_CONFIG);
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, CCConfig.CLIENT_CONFIG);
    }

    public void setup(FMLCommonSetupEvent event) {
        ModPacketHandler.registerMessages();
    }


    /**
     * Because of how mods work, this function is called always when a player uses right click.
     * But if the right click doesn't involve this mod (No chain/block to connect to) then we ignore immediately.
     * <p>
     * If it does involve us, then we have work to do, we create connections remove items from inventory and such.
     */
}
