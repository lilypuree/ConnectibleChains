package com.lilypuree.connectiblechains.chain;

import com.lilypuree.connectiblechains.ConnectibleChains;
import com.lilypuree.connectiblechains.util.Helper;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.IForgeRegistry;
import net.minecraftforge.registries.RegistryBuilder;

import java.util.Collection;
import java.util.Map;
import java.util.function.Supplier;

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
    public static ChainType DEFAULT_CHAIN_TYPE = new ChainType(Items.CHAIN);
    @SuppressWarnings("unused")
    public static ChainType IRON_CHAIN;

    public static final ResourceKey<Registry<ChainType>> CHAIN_TYPE_KEY = ResourceKey.createRegistryKey(new ResourceLocation(ConnectibleChains.MODID, "chains"));
    public static final DeferredRegister<ChainType> CHAINS = DeferredRegister.create(CHAIN_TYPE_KEY, ConnectibleChains.MODID);
    public static final Supplier<IForgeRegistry<ChainType>> REGISTRY = CHAINS.makeRegistry(() -> new RegistryBuilder<ChainType>()
            .setName(new ResourceLocation(ConnectibleChains.MODID, "chain_types"))
            .setDefaultKey(DEFAULT_CHAIN_TYPE_ID));
    public static final Map<Item, ChainType> ITEM_CHAIN_TYPES = new Object2ObjectOpenHashMap<>(64);

    //---------------------------------------------------------------------------------

    public static ChainType register(String id, Item item) {
        ChainType chainType = new ChainType(item);
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
    public static ChainType register(Item item) {
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(item);
        if (id == ForgeRegistries.ITEMS.getDefaultKey()) {
            ConnectibleChains.LOGGER.error("Cannot create chain type with unregistered item: {}", item.getDescription());
            return DEFAULT_CHAIN_TYPE;
        }
        if (REGISTRY.get().containsKey(id)) {
            return REGISTRY.get().getValue(id);
        }
        ChainType chainType = new ChainType(item);
        ITEM_CHAIN_TYPES.put(item, chainType);
        return chainType;
    }


    public static void init(IEventBus bus) {
        CHAINS.register(bus);
    }
}
