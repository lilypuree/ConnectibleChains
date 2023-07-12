package com.lilypuree.connectiblechains.chain;

import com.lilypuree.connectiblechains.client.ClientInitializer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;

/**
 * The 'material' of a chain
 */
public class ChainType {

    private Item item;

    public ChainType(Item item) {
        this.item = item;
    }

    public Item item() {
        return item;
    }

    public void setItem(Item item) {
        this.item = item;
    }

    public ResourceLocation getKnotTexture() {
        return ClientInitializer.textureManager.getKnotTexture(ChainTypesRegistry.getKey(this));
    }

    public ResourceLocation getChainTexture() {
        return ClientInitializer.textureManager.getChainTexture(ChainTypesRegistry.getKey(this));
    }
}
