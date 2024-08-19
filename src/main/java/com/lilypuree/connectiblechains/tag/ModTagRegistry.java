/*
 * Copyright (C) 2024 legoatoom.
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

package com.lilypuree.connectiblechains.tag;

import com.lilypuree.connectiblechains.util.Helper;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

/**
 * @see <a href="https://github.com/paulevsGitch/BCLib/blob/1.18.2/src/main/java/ru/bclib/api/tag/TagAPI.java">github.com/paulevsGitch/BCLib</>
 */
public class ModTagRegistry {
    public static final TagKey<Block> CHAIN_CONNECTIBLE = BlockTags.create(Helper.rl("chain_connectible"));
    public static final TagKey<Item> CATENARY_ITEMS = ItemTags.create(Helper.rl("catenary_items"));
}
