package com.lilypuree.connectiblechains.chain;

import com.lilypuree.connectiblechains.ConnectibleChains;
import com.lilypuree.connectiblechains.util.Helper;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.*;

import java.util.Collection;
import java.util.Map;
import java.util.function.Supplier;

import static com.lilypuree.connectiblechains.compat.BuiltinCompat.BUILTIN_TYPES;
import static com.lilypuree.connectiblechains.compat.BuiltinCompat.registerTypeForBuiltin;

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
    public static ChainType DEFAULT_CHAIN_TYPE;
    @SuppressWarnings("unused")
    public static ChainType IRON_CHAIN;

    public static final Map<Item, ChainType> ITEM_CHAIN_TYPES = new Object2ObjectOpenHashMap<>(64);

    //---------------------------------------------------------------------------------

    private static Supplier<IForgeRegistry<ChainType>> REGISTRY;

    @SubscribeEvent
    public static void onNewRegistry(NewRegistryEvent event) {
        RegistryBuilder<ChainType> registryBuilder = new RegistryBuilder<>();
        registryBuilder.setName(new ResourceLocation(ConnectibleChains.MODID, "chain_types"))
                .setType(ChainType.class)
                .setDefaultKey(DEFAULT_CHAIN_TYPE_ID);
        REGISTRY = event.create(registryBuilder);
    }

    @SubscribeEvent
    public static void onRegistry(RegistryEvent.Register<ChainType> event) {
        IRON_CHAIN = DEFAULT_CHAIN_TYPE = register(DEFAULT_CHAIN_TYPE_ID.getPath(), Items.CHAIN);
        event.getRegistry().register(IRON_CHAIN);
//        for (ResourceLocation itemId : BUILTIN_TYPES) {
//            registerTypeForBuiltin(itemId);
//        }
    }

    private static ChainType register(String id, Item item) {
        ChainType chainType = new ChainType(item).setRegistryName(Helper.identifier(id));
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
        ChainType chainType = new ChainType(item).setRegistryName(id);
        ITEM_CHAIN_TYPES.put(item, chainType);
        return chainType;
    }


    @SuppressWarnings("EmptyMethod")
    public static void init() {
        // Static fields are now initialized
    }
}
