/*
 * Copyright (C) 2024 legoatoom
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

package com.lilypuree.connectiblechains.client.render.entity.texture;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.lilypuree.connectiblechains.ConnectibleChains;
import com.lilypuree.connectiblechains.client.ClientInitializer;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.Tuple;
import net.minecraft.util.profiling.ProfilerFiller;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

/**
 * The manager loads the chain models that contain the texture information for all chain types.
 * It looks for models at models/entity/chain/ within the same namespace as the chain type.
 */
public class ChainTextureManager extends SimplePreparableReloadListener<Map<ResourceLocation, JsonElement>> {
    private static final Gson GSON = new GsonBuilder().setLenient().create();
    private static final String MODEL_FILE_LOCATION = "models/entity/" + ConnectibleChains.MODID;
    /**
     * How many different chain items do we expect?
     */
    private static final int EXPECTED_UNIQUE_CHAIN_COUNT = 64;
    /**
     * Maps chain types to chain texture ids.
     */
    private final Object2ObjectMap<ResourceLocation, ResourceLocation> chainTextures = new Object2ObjectOpenHashMap<>(EXPECTED_UNIQUE_CHAIN_COUNT);
    /**
     * Maps chain types to knot texture ids.
     */
    private final Object2ObjectMap<ResourceLocation, ResourceLocation> knotTextures = new Object2ObjectOpenHashMap<>(EXPECTED_UNIQUE_CHAIN_COUNT);


    @Override
    protected Map<ResourceLocation, JsonElement> prepare(ResourceManager resourceManager, ProfilerFiller profiler) {
        HashMap<ResourceLocation, JsonElement> map = new HashMap<>();
        SimpleJsonResourceReloadListener.scanDirectory(resourceManager, MODEL_FILE_LOCATION, GSON, map);
        return map;
    }

    @Override
    public void apply(Map<ResourceLocation, JsonElement> data, ResourceManager manager, ProfilerFiller profiler) {
            clearCache();
            data.forEach((ResourceLocation, jsonElement) -> {
                Tuple<ResourceLocation, ResourceLocation> textures = extractChainTextures(ResourceLocation, jsonElement);
                chainTextures.put(ResourceLocation, textures.getA());
                knotTextures.put(ResourceLocation, textures.getB());
            });
    }

    private static Tuple<ResourceLocation, ResourceLocation> extractChainTextures(ResourceLocation itemId, JsonElement jsonElement) {
        //Default
        ResourceLocation chainTextureId = defaultChainTextureId(itemId);
        ResourceLocation knotTextureId = defaultKnotTextureId(itemId);

        if (jsonElement.isJsonObject()) {
            JsonObject jsonObject = jsonElement.getAsJsonObject();
            JsonObject texturesObject = jsonObject.getAsJsonObject("textures");

            if (texturesObject.has("chain") && texturesObject.get("chain").isJsonPrimitive()) {
                chainTextureId = ResourceLocation.tryParse(texturesObject.get("chain").getAsString()+ ".png");
            }
            if (texturesObject.has("knot") && texturesObject.get("knot").isJsonPrimitive()) {
                knotTextureId = ResourceLocation.tryParse(texturesObject.get("knot").getAsString()+ ".png");
            }
        }

        return new Tuple<>(chainTextureId, knotTextureId);
    }

    public void clearCache() {
        ClientInitializer.getInstance()
                .getChainKnotEntityRenderer()
                .ifPresent(it -> it.getChainRenderer().purge());
        chainTextures.clear();
        knotTextures.clear();

    }


    private static @NotNull ResourceLocation defaultChainTextureId(ResourceLocation itemId) {
        return ResourceLocation.fromNamespaceAndPath(itemId.getNamespace(), "textures/block/%s.png".formatted(itemId.getPath()));
    }
    private static @NotNull ResourceLocation defaultKnotTextureId(ResourceLocation itemId) {
        return ResourceLocation.fromNamespaceAndPath(itemId.getNamespace(), "textures/item/%s.png".formatted(itemId.getPath()));
    }

    public ResourceLocation getChainTexture(ResourceLocation sourceItemId) {
        return chainTextures.computeIfAbsent(sourceItemId, (ResourceLocation id) -> {
            // Default location.
            ConnectibleChains.LOGGER.warn("Did not find a model file for the chain '%s', assuming default path.".formatted(sourceItemId));
            return defaultChainTextureId(id);
        });
    }

    public ResourceLocation getKnotTexture(ResourceLocation sourceItemId) {
        return knotTextures.computeIfAbsent(sourceItemId, (ResourceLocation id) -> {
            // Default location.
            ConnectibleChains.LOGGER.warn("Did not find a model file for the chain '%s', assuming default path.".formatted(sourceItemId));
            return defaultKnotTextureId(id);
        });
    }
}
