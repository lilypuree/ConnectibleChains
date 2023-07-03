package com.lilypuree.connectiblechains.compat;

import com.lilypuree.connectiblechains.chain.ChainTypesRegistry;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.HashSet;
import java.util.Set;

public class BuiltinCompat {

    /**
     * A list of item ids that this mod provides basic support for by default
     */
    public static final Set<ResourceLocation> BUILTIN_TYPES = new HashSet<>() {{
        add(new ResourceLocation("betterend:thallasium_chain"));
        add(new ResourceLocation("betterend:terminite_chain"));
        add(new ResourceLocation("betternether:cincinnasite_chain"));
        add(new ResourceLocation("valley:golden_chain"));
        add(new ResourceLocation("valley:netherite_chain"));
        add(new ResourceLocation("valley:copper_chain"));
        add(new ResourceLocation("valley:exposed_copper_chain"));
        add(new ResourceLocation("valley:weathered_copper_chain"));
        add(new ResourceLocation("valley:oxidized_copper_chain"));
        add(new ResourceLocation("valley:waxed_copper_chain"));
        add(new ResourceLocation("valley:waxed_exposed_copper_chain"));
        add(new ResourceLocation("valley:waxed_weathered_copper_chain"));
        add(new ResourceLocation("valley:waxed_oxidized_copper_chain"));
    }};

    private static final Set<ResourceLocation> REGISTERED_BUILTIN_TYPES = new HashSet<>();

    /**
     * sets up a listener for future item registrations. (fabric)
     */
    public static void init() {
//        RegistryEntryAddedCallback.event(Registry.ITEM).register((rawId, id, object) -> registerTypeForBuiltin(id));
    }

    /**
     * Checks if a builtin type exists for {@code itemId} and then registers a type for it once.
     *
     * @param itemId The id of an item
     */
    public static void registerTypeForBuiltin(ResourceLocation itemId) {
        if (!BUILTIN_TYPES.contains(itemId) || REGISTERED_BUILTIN_TYPES.contains(itemId)) return;
        if (!ForgeRegistries.ITEMS.containsKey(itemId)) return;
        ChainTypesRegistry.register(ForgeRegistries.ITEMS.getValue(itemId));
        REGISTERED_BUILTIN_TYPES.add(itemId);
    }
}
