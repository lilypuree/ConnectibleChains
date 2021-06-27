package com.lilypuree.connectiblechains.events;

import com.lilypuree.connectiblechains.ConnectibleChains;
import com.lilypuree.connectiblechains.entity.ChainKnotEntity;
import com.lilypuree.connectiblechains.entity.ModEntityTypes;
import net.minecraft.block.Block;
import net.minecraft.block.FenceBlock;
import net.minecraft.block.WallBlock;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.DamageSource;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.IWorld;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;

@Mod.EventBusSubscriber(modid = ConnectibleChains.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class EventHandler {

    @SubscribeEvent
    public static void onEntitySpawn(EntityJoinWorldEvent event) {
        Entity entity = event.getEntity();
        if (entity.getType() == ModEntityTypes.CHAIN_KNOT.get()) {
            entity.setDeltaMovement(Vector3d.ZERO);
        }
    }

    @SubscribeEvent
    public static void onBlockUpdate(BlockEvent.BreakEvent event) {
        IWorld world = event.getWorld();
        BlockPos pos = event.getPos();
        if (world.getBlockState(event.getPos()).is(BlockTags.FENCES)) {
            int x = pos.getX();
            int y = pos.getY();
            int z = pos.getZ();
            List<ChainKnotEntity> list = world.getEntitiesOfClass(ChainKnotEntity.class, new AxisAlignedBB((double) x - 1.0D, (double) y - 1.0D, (double) z - 1.0D, (double) x + 1.0D, (double) y + 1.0D, (double) z + 1.0D));
            boolean isCreative = event.getPlayer().isCreative();
            for (ChainKnotEntity entity : list) {
                entity.breakChain(isCreative);
            }
        }
    }

    /**
     * When used on a block that is allowed to create a chain on, we get the chainKnot or make one and
     * either connect it to the player or, if the player has already done this, connect the other chainKnot to it.
     */
    @SubscribeEvent
    public static void onItemUse(PlayerInteractEvent.RightClickBlock event) {
        PlayerEntity playerEntity = event.getPlayer();
        World world = event.getWorld();
        BlockPos blockPos = event.getPos();
        Block block = world.getBlockState(blockPos).getBlock();
        boolean isChain = event.getItemStack().getItem() == Items.CHAIN;

        if (isChain) {
            ItemStack itemStack = event.getItemStack();
            if (ChainKnotEntity.canConnectTo(block) && playerEntity != null && !playerEntity.isCrouching()) {
                if (!world.isClientSide()) {
                    ChainKnotEntity knot = ChainKnotEntity.getOrCreate(world, blockPos, false);
                    if (!ChainKnotEntity.tryAttachHeldChainsToBlock(playerEntity, world, blockPos, knot)) {
                        // If this didn't work connect the player to the new chain instead.
                        if (knot.getHoldingEntities().contains(playerEntity)) {
                            knot.detachChain(playerEntity, true, false);
                            knot.dropItem(null);
                            if (!playerEntity.isCreative())
                                itemStack.grow(1);
                        } else {
                            knot.attachChain(playerEntity, true, 0);
                            knot.playPlacementSound();
                            if (!playerEntity.isCreative())
                                itemStack.shrink(1);
                        }
                    }
                }
                playerEntity.swing(event.getHand());
                event.setUseItem(Event.Result.DENY);
            }
        } else if (block instanceof FenceBlock || block instanceof WallBlock) {
            if (!world.isClientSide()) {
                boolean attached = ChainKnotEntity.tryAttachHeldChainsToBlock(playerEntity, world, blockPos, ChainKnotEntity.getOrCreate(world, blockPos, true));
                if (attached) {
                    event.setUseBlock(Event.Result.ALLOW);
                    event.setUseItem(Event.Result.DENY);
                } else {
                    event.setUseBlock(Event.Result.DEFAULT);
                }
            } else {
                if (isChain) {
                    event.setUseBlock(Event.Result.ALLOW);
                }
            }
        }
    }


}
