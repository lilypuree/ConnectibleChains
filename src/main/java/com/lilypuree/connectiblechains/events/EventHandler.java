package com.lilypuree.connectiblechains.events;

import com.lilypuree.connectiblechains.ConnectibleChains;
import com.lilypuree.connectiblechains.entity.ChainKnotEntity;
import com.lilypuree.connectiblechains.entity.ModEntityTypes;
import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.ActionResult;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityEvent;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

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
    public static void onTick(TickEvent.WorldTickEvent event){

    }

    @SubscribeEvent
    public static void onItemUse(PlayerInteractEvent.RightClickBlock event) {
        if (event.getItemStack().getItem() == Items.CHAIN) {
            PlayerEntity playerEntity = event.getPlayer();
            World world = playerEntity.level;
            BlockPos blockPos = event.getPos();
            Block block = world.getBlockState(blockPos).getBlock();
            if (block.is(BlockTags.FENCES) && playerEntity != null && !playerEntity.isCrouching()) {
                event.setUseItem(Event.Result.DENY);
                event.setUseBlock(Event.Result.DENY);
                if (!world.isClientSide()) {
                    if (!attachHeldMobsToBlock(playerEntity, world, blockPos).consumesAction()) {
                        // Create new ChainKnot
                        ChainKnotEntity knot = ChainKnotEntity.getOrCreate(world, blockPos);
                        if (knot.getHoldingEntities().contains(playerEntity)) {
                            knot.detachChain(playerEntity, false);
                            knot.dropItem(null);
                            if (!playerEntity.isCreative())
                                event.getItemStack().grow(1);
                        } else {
                            knot.attachChain(playerEntity, 0);
                            knot.playPlacementSound();
                            if (!playerEntity.isCreative())
                                event.getItemStack().shrink(1);
                        }
                    }
                }
            }
        }
    }

    public static ActionResultType attachHeldMobsToBlock(PlayerEntity playerEntity, World world, BlockPos blockPos) {
        ChainKnotEntity leashKnotEntity = null;
        boolean bl = false;
        double d = ChainKnotEntity.MAX_RANGE;
        int i = blockPos.getX();
        int j = blockPos.getY();
        int k = blockPos.getZ();
        List<ChainKnotEntity> list = world.getEntitiesOfClass(ChainKnotEntity.class, new AxisAlignedBB((double) i - d, (double) j - d, (double) k - d, (double) i + d, (double) j + d, (double) k + d));

        for (ChainKnotEntity chainEntity : list) {
            if (chainEntity.getHoldingEntities().contains(playerEntity)) {
                if (leashKnotEntity == null) {
                    leashKnotEntity = ChainKnotEntity.getOrCreate(world, blockPos);
                }

                if (!chainEntity.equals(leashKnotEntity)) {
                    chainEntity.attachChain(leashKnotEntity, playerEntity.getId());
                    bl = true;
                }
            }
        }

        return bl ? ActionResultType.SUCCESS : ActionResultType.PASS;
    }
}
