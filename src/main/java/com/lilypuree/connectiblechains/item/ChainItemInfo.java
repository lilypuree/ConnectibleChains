package com.lilypuree.connectiblechains.item;


import com.lilypuree.connectiblechains.chain.ChainLink;
import com.lilypuree.connectiblechains.entity.ChainKnotEntity;
import com.lilypuree.connectiblechains.entity.ChainLinkEntity;
import com.lilypuree.connectiblechains.tag.CommonTags;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;

import java.util.List;


public class ChainItemInfo {
    /**
     * Because of how mods work, this function is called always when a player uses right click.
     * But if the right click doesn't involve this mod (No chain/block to connect to) then we ignore immediately.
     * <p>
     * If it does involve us, then we have work to do, we create connections remove items from inventory and such.
     */
    public static void chainUseEvent(PlayerInteractEvent.RightClickBlock event) {
        Player player = event.getEntity();
        InteractionHand hand = event.getHand();
        BlockHitResult hitResult = event.getHitVec();
        Level world = event.getLevel();

        if (player == null || player.isCrouching()) return;
        ItemStack stack = player.getItemInHand(hand);
        BlockPos blockPos = hitResult.getBlockPos();
        BlockState blockState = world.getBlockState(blockPos);

        if (!ChainKnotEntity.canAttachTo(blockState)) return;
        else if (world.isClientSide) {
            if (CommonTags.isChain(stack)) {
                event.setCanceled(true);
            }

            // Check if any held chains can be attached. This can be done without holding a chain item
            if (ChainKnotEntity.getHeldChainsInRange(player, blockPos).size() > 0) {
                event.setCanceled(true);
            }

            // Check if a knot exists and can be destroyed
            // Would work without this check but no swing animation would be played
            if (ChainKnotEntity.getKnotAt(player.level(), blockPos) != null && ChainLinkEntity.canDestroyWith(stack)) {
                event.setCanceled(true);
            }

            if (event.isCanceled()) {
                event.setCancellationResult(InteractionResult.SUCCESS);
            }
            return;
        }

        // 1. Try with existing knot, regardless of hand item
        ChainKnotEntity knot = ChainKnotEntity.getKnotAt(world, blockPos);
        if (knot != null) {
            if (knot.interact(player, hand) == InteractionResult.CONSUME) {
                event.setCanceled(true);
                event.setCancellationResult(InteractionResult.CONSUME);
            }
            return;
        }

        // 2. Check if any held chains can be attached.
        List<ChainLink> attachableChains = ChainKnotEntity.getHeldChainsInRange(player, blockPos);

        // Use the held item as the new knot type
        Item knotType = stack.getItem();

        // Allow default interaction behaviour.
        if (attachableChains.size() == 0 && !CommonTags.isChain(stack)) {
            return;
        }

        // Held item does not correspond to a type.
        if (!CommonTags.isChain(stack))
            knotType = attachableChains.get(0).sourceItem;

        // 3. Create new knot if none exists and delegate interaction
        knot = new ChainKnotEntity(world, blockPos, knotType);
        knot.setGraceTicks((byte) 0);
        world.addFreshEntity(knot);
//        knot.onPlace();
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        InteractionResult result = knot.interact(player, hand);
        event.setCanceled(true);
        event.setCancellationResult(result);
    }
}
