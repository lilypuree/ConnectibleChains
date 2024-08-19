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

package com.lilypuree.connectiblechains.item;

import com.lilypuree.connectiblechains.ConnectibleChains;
import com.lilypuree.connectiblechains.chain.ChainLink;
import com.lilypuree.connectiblechains.entity.ChainKnotEntity;
import com.lilypuree.connectiblechains.entity.ChainLinkEntity;
import com.lilypuree.connectiblechains.tag.ModTagRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.List;

/**
 * Some static settings and functions for the chainItem.
 */
public class ChainItemInfo {


    /**
     * Because of how mods work, this function is called always when a player uses right click.
     * But if the right click doesn't involve this mod (No chain/block to connect to) then we ignore immediately.
     * <p>
     * If it does involve us, then we have work to do, we create connections remove items from inventory and such.
     *
     * @param player    Player that right-clicked on a block.
     * @param level     The level the player is in.
     * @param hand      What hand the player used.
     * @param blockPos General information about the block that was clicked.
     * @return An ActionResult.
     */
    public static InteractionResult chainUseEvent(Player player, Level level, InteractionHand hand, BlockPos blockPos) {
        if (player == null || player.isShiftKeyDown()) return InteractionResult.PASS;
        ItemStack stack = player.getItemInHand(hand);
        BlockState blockState = level.getBlockState(blockPos);
        if (!ChainKnotEntity.canAttachTo(blockState)) {
            return InteractionResult.PASS;
        } else if (level.isClientSide()) {
            ItemStack handItem = player.getItemInHand(hand);
            if (handItem.is(ModTagRegistry.CATENARY_ITEMS)) {
                return InteractionResult.SUCCESS;
            }

            // Check if any held chains can be attached. This can be done without holding a chain item
            if (!ChainKnotEntity.getHeldChainsInRange(player, blockPos).isEmpty()) {
                return InteractionResult.SUCCESS;
            }

            // Check if a knot exists and can be destroyed
            // Would work without this check but no swing animation would be played
            if (ChainKnotEntity.getKnotAt(player.level(), blockPos) != null && ChainLinkEntity.canDestroyWith(stack)) {
                return InteractionResult.SUCCESS;
            }

            return InteractionResult.PASS;
        }


        // 1. Try with existing knot, regardless of hand item
        ChainKnotEntity knot = ChainKnotEntity.getKnotAt(level, blockPos);
        if (knot != null) {
            if (knot.interact(player, hand) == InteractionResult.CONSUME) {
                return InteractionResult.CONSUME;
            }
            return InteractionResult.PASS;
        }

        // 2. Check if any held chains can be attached.
        List<ChainLink> attachableChains = ChainKnotEntity.getHeldChainsInRange(player, blockPos);

        // Use the held item as the new knot type
        Item knotType = stack.getItem();

        // Allow default interaction behaviour.
        if (attachableChains.isEmpty() && !stack.is(ModTagRegistry.CATENARY_ITEMS)) {
            return InteractionResult.PASS;
        }

        // Held item does not correspond to a type.
        if (!stack.is(ModTagRegistry.CATENARY_ITEMS)) {
            knotType = attachableChains.getFirst().getSourceItem();
        }

        // 3. Create new knot if none exists and delegate interaction
        knot = new ChainKnotEntity(level, blockPos, knotType);
        knot.setGraceTicks((byte) 0);
        level.addFreshEntity(knot);
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        return knot.interact(player, hand);
    }

    @OnlyIn(Dist.CLIENT)
    public static void infoToolTip(ItemStack itemStack, Item.TooltipContext tooltipContext, TooltipFlag tooltipType, List<Component> texts) {
        if (ConnectibleChains.runtimeConfig.doShowToolTip()) {
            if (itemStack.is(ModTagRegistry.CATENARY_ITEMS)) {
                if (Screen.hasShiftDown()) {
                    texts.add(1, Component.translatable("message.connectiblechains.connectible_chain_detailed").withStyle(ChatFormatting.AQUA));
                } else {
                    texts.add(1, Component.translatable("message.connectiblechains.connectible_chain").withStyle(ChatFormatting.YELLOW));
                }
            }
        }
    }
}
