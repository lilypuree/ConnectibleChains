package com.lilypuree.connectiblechains.chain;

import com.lilypuree.connectiblechains.ConnectibleChains;
import com.lilypuree.connectiblechains.util.Helper;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.*;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

@Mod.EventBusSubscriber(modid = ConnectibleChains.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ChainTypesRegistry {

    public static ResourceLocation getKey(ChainType type) {
        return REGISTRY.get().getKey(type);
    }

    public static ChainType getValue(ResourceLocation id) {
        return REGISTRY.get().getValue(id);
    }

    public static ChainType getValue(String id) {
        return REGISTRY.get().getValue(ResourceLocation.tryParse(id));
    }

    public static Collection<ResourceLocation> getKeys() {
        return REGISTRY.get().getKeys();
    }

    public static final ResourceLocation DEFAULT_CHAIN_TYPE_ID = Helper.identifier("iron_chain");
    public static Supplier<ChainType> DEFAULT_CHAIN_TYPE;
    public static final ResourceKey<Registry<ChainType>> CHAIN_TYPES =  ResourceKey.createRegistryKey(new ResourceLocation(ConnectibleChains.MODID, DEFAULT_CHAIN_TYPE_ID.getPath()));
    public static final DeferredRegister CHAINS = DeferredRegister.create(CHAIN_TYPES, ConnectibleChains.MODID);
    private static Supplier<IForgeRegistry<ChainType>> REGISTRY = CHAINS.makeRegistry(() -> new RegistryBuilder<>().setName(new ResourceLocation(ConnectibleChains.MODID, "chain_types")).setDefaultKey(DEFAULT_CHAIN_TYPE_ID));
    @SuppressWarnings("unused")
    public static Supplier<ChainType> IRON_CHAIN;

    public static final Map<Item, Supplier<ChainType>> ITEM_CHAIN_TYPES = new HashMap<>();

    //---------------------------------------------------------------------------------

    @SubscribeEvent
    public static void onNewRegistry(NewRegistryEvent event) {
        IRON_CHAIN = DEFAULT_CHAIN_TYPE = register(DEFAULT_CHAIN_TYPE_ID.getPath(), Items.CHAIN);
        CHAINS.register("iron_chain", () -> Items.CHAIN);
    }

    public static Supplier<ChainType> register(String id, Item item) {
        Supplier<ChainType> chainType = ()->new ChainType(item);
        ITEM_CHAIN_TYPES.put(item, chainType);
        return chainType;
    }

    /**
     * Used to register chain types on initialization. Cannot register a type twice.
     *
     * @param item the chain type's item
     * @return the new {@link ChainType}
     */
    @SuppressWarnings("UnusedReturnValue")
    public static Supplier<ChainType> register(Item item) {
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(item);
        if (id == ForgeRegistries.ITEMS.getDefaultKey()) {
            ConnectibleChains.LOGGER.error("Cannot create chain type with unregistered item: {}", item.getDescription());
            return DEFAULT_CHAIN_TYPE;
        }
        if (REGISTRY.get().containsKey(id)) {
            return ()->REGISTRY.get().getValue(id);
        }
        Supplier<ChainType> chainType = ()->new ChainType(item);
        ITEM_CHAIN_TYPES.put(item, chainType);
        return chainType;
    }

    public static void init(IEventBus bus) {
        CHAINS.register(bus);
    }
}