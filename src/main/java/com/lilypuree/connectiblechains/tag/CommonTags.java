package com.lilypuree.connectiblechains.tag;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class CommonTags {
    public static final TagKey<Item> CHAINS = makeCommonItemTag("chains");


    public static TagKey<Item> makeCommonItemTag(String name) {
        return ItemTags.create(new ResourceLocation("c", name));
    }


    public static boolean isChain(ItemStack itemStack) {
        return itemStack.is(CHAINS);
    }

}
