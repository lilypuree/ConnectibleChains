package com.lilypuree.connectiblechains;

import com.lilypuree.connectiblechains.client.ClientEventHandler;
import com.lilypuree.connectiblechains.entity.ChainKnotEntity;
import com.lilypuree.connectiblechains.entity.ModEntityTypes;
import com.lilypuree.connectiblechains.network.ModPacketHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

// The value here should match an entry in the META-INF/mods.toml file
@Mod(ConnectibleChains.MODID)
public class ConnectibleChains {
    public static final String MODID = "connectiblechains";
    public static final Logger LOGGER = LogManager.getLogger(MODID);
    public static CCConfig runtimeConfig;

    public ConnectibleChains() {
        ModEntityTypes.register();
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::setup);
        MinecraftForge.EVENT_BUS.addListener(this::chainUseEvent);
        MinecraftForge.EVENT_BUS.addListener(this::onBlockBreak);

        runtimeConfig = new CCConfig();
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, CCConfig.COMMON_CONFIG);
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, CCConfig.CLIENT_CONFIG);
    }

    public void setup(FMLCommonSetupEvent event) {
        ModPacketHandler.registerMessages();
    }


    /**
     * Because of how mods work, this function is called always when a player uses right click.
     * But if the right click doesn't involve this mod (No chain/block to connect to) then we ignore immediately.
     * <p>
     * If it does involve us, then we have work to do, we create connections remove items from inventory and such.
     */
    private void chainUseEvent(PlayerInteractEvent.RightClickBlock event) {
        Player player = event.getPlayer();
        InteractionHand hand = event.getHand();
        BlockHitResult hitResult = event.getHitVec();
        Level world = event.getWorld();

        if (player == null) return;
        ItemStack stack = player.getItemInHand(hand);
        BlockPos blockPos = hitResult.getBlockPos();
        BlockState block = world.getBlockState(blockPos);
        if (stack.getItem() == Items.CHAIN) {
            if (ChainKnotEntity.canConnectTo(block) && !player.isShiftKeyDown()) {
                if (!world.isClientSide) {
                    ChainKnotEntity knot = ChainKnotEntity.getOrCreate(world, blockPos, false);
                    if (!ChainKnotEntity.tryAttachHeldChainsToBlock(player, world, blockPos, knot)) {
                        // If this didn't work connect the player to the new chain instead.
                        assert knot != null; // This can never happen as long as getOrCreate has false as parameter.
                        if (knot.getHoldingEntities().contains(player)) {
                            knot.detachChain(player, true, false);
                            knot.dropItem(null);
                            if (!player.isCreative())
                                stack.grow(1);
                        } else if (knot.attachChain(player, true, 0)) {
                            knot.playPlacementSound();
                            if (!player.isCreative())
                                stack.shrink(1);
                        }
                    }
                }
                event.setCanceled(true);
                event.setCancellationResult(InteractionResult.sidedSuccess(world.isClientSide));
            }
        }
        if (ChainKnotEntity.canConnectTo(block)) {
            if (world.isClientSide) {
                if (player.getItemInHand(hand).getItem() == Items.CHAIN) {
                    event.setCanceled(true);
                    event.setCancellationResult(InteractionResult.SUCCESS);
                }
            } else {
                if (ChainKnotEntity.tryAttachHeldChainsToBlock(player, world, blockPos, ChainKnotEntity.getOrCreate(world, blockPos, true))) {
                    event.setCanceled(true);
                    event.setCancellationResult(InteractionResult.SUCCESS);
                }
            }
        }
    }

    private void onBlockBreak(BlockEvent.BreakEvent event) {
        LevelAccessor level = event.getWorld();
        BlockPos pos = event.getPos();
        if (!level.isClientSide() && ChainKnotEntity.canConnectTo(level.getBlockState(pos))) {
            level.getEntitiesOfClass(ChainKnotEntity.class, new AABB(event.getPos())).forEach(ChainKnotEntity::setObstructionCheckCounter);
        }
    }
}
