package com.lilypuree.connectiblechains.client.render.entity;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.lilypuree.connectiblechains.ConnectibleChains;
import com.lilypuree.connectiblechains.chain.ChainTypesRegistry;
import com.lilypuree.connectiblechains.compat.BuiltinCompat;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.Item;
import org.jetbrains.annotations.Nullable;

import java.io.FileNotFoundException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * The manager loads the chain models that contain the texture information for all chain types.
 * It looks for models at models/entity/chain/ within the same namespace as the chain type.
 * Inspired by {@link net.minecraft.client.resources.model.ModelManager} and {@link net.minecraft.client.resources.model.ModelBakery}.
 */
public class ChainTextureManager extends SimplePreparableReloadListener<Map<ResourceLocation, ChainTextureManager.JsonModel>> {

    private static final Gson GSON = new GsonBuilder().setLenient().create();
    private static final ResourceLocation MISSING_ID = new ResourceLocation(ConnectibleChains.MODID, "textures/entity/missing.png");
    /**
     * Maps chain types to chain texture ids.
     */
    private final Object2ObjectMap<ResourceLocation, ResourceLocation> chainTextures = new Object2ObjectOpenHashMap<>(64);
    /**
     * Maps chain types to knot texture ids.
     */
    private final Object2ObjectMap<ResourceLocation, ResourceLocation> knotTextures = new Object2ObjectOpenHashMap<>(64);

//    @Override
//    public ResourceLocation getFabricId() {
//        return Helper.ResourceLocation("chain_textures");
//    }


    @Override
    protected Map<ResourceLocation, JsonModel> prepare(ResourceManager pResourceManager, ProfilerFiller pProfiler) {
        return load(pResourceManager);
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonModel> textureMap, ResourceManager pResourceManager, ProfilerFiller pProfiler) {
        chainTextures.clear();
        knotTextures.clear();

        textureMap.forEach((id, entry) -> {
            chainTextures.put(id, entry.textures.chainTextureId());
            knotTextures.put(id, entry.textures.knotTextureId());
        });
    }

    /**
     * Loads all models for all registered chain types.
     *
     * @param manager The resource manager
     * @return A map of chain type ids to model data
     */
    public Map<ResourceLocation, JsonModel> load(ResourceManager manager) {
        Map<ResourceLocation, JsonModel> map = new HashMap<>();

        for (ResourceLocation chainType : ChainTypesRegistry.getKeys()) {
            try (Resource resource = manager.getResource(getResourceId(getModelId(chainType)))) {
                Reader reader = new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8);
                JsonModel jsonModel = GSON.fromJson(reader, JsonModel.class);
                map.put(chainType, jsonModel);
            } catch (FileNotFoundException e) {
                JsonModel builtinModel = loadBuiltinModel(manager, chainType);
                if (builtinModel != null) {
                    map.put(chainType, builtinModel);
                } else {
                    ConnectibleChains.LOGGER.error("Missing model for {}.", chainType, e);
                }
            } catch (Exception e) {
                ConnectibleChains.LOGGER.error("Failed to load model for {}.", chainType, e);
            }
        }

        return map;
    }

    public static ResourceLocation getResourceId(ResourceLocation modelId) {
        return new ResourceLocation(modelId.getNamespace(), "models/" + modelId.getPath() + ".json");
    }

    /**
     * @see net.minecraft.data.models.model.ModelLocationUtils#getModelLocation(Item)
     */
    public static ResourceLocation getModelId(ResourceLocation chainType) {
        return new ResourceLocation(chainType.getNamespace(), "entity/chain/" + chainType.getPath());
    }

    /**
     * Checks if {@code chainType} is a builtin type and tries to load it's model
     *
     * @param manager   The resource manager
     * @param chainType A chain type, can be a builtin type or not
     * @return The model for {@code chainType} or null of none exists
     */
    @Nullable
    private JsonModel loadBuiltinModel(ResourceManager manager, ResourceLocation chainType) {
        if (BuiltinCompat.BUILTIN_TYPES.contains(chainType)) {
            try (Resource resource = manager.getResource(getBuiltinResourceId(getModelId(chainType)))) {
                Reader reader = new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8);
                return GSON.fromJson(reader, JsonModel.class);
            } catch (Exception e) {
                ConnectibleChains.LOGGER.error("Error for builtin type {}.", chainType, e);
            }
        }
        return null;
    }

    private static ResourceLocation getBuiltinResourceId(ResourceLocation modelId) {
        return new ResourceLocation(ConnectibleChains.MODID, "models/" + modelId.getPath() + ".json");
    }

    public ResourceLocation getChainTexture(ResourceLocation chainType) {
        return chainTextures.getOrDefault(chainType, MISSING_ID);
    }

    public ResourceLocation getKnotTexture(ResourceLocation chainType) {
        return knotTextures.getOrDefault(chainType, MISSING_ID);
    }

    /**
     * This class represents the json structure of the model file
     */
    @SuppressWarnings("unused")
    protected static final class JsonModel {
        public Textures textures;

        protected static final class Textures {
            public String chain;
            public String knot;

            public ResourceLocation chainTextureId() {
                return new ResourceLocation(chain + ".png");
            }

            public ResourceLocation knotTextureId() {
                return new ResourceLocation(knot + ".png");
            }
        }
    }
}
