package com.lilypuree.connectiblechains;

import com.lilypuree.connectiblechains.chain.ChainLink;
import com.lilypuree.connectiblechains.chain.ChainType;
import com.lilypuree.connectiblechains.chain.ChainTypesRegistry;
import com.lilypuree.connectiblechains.compat.BuiltinCompat;
import com.lilypuree.connectiblechains.entity.ChainKnotEntity;
import com.lilypuree.connectiblechains.entity.ChainLinkEntity;
import com.lilypuree.connectiblechains.entity.ModEntityTypes;
import com.lilypuree.connectiblechains.network.ModPacketHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;

// The value here should match an entry in the META-INF/mods.toml file
@Mod(ConnectibleChains.MODID)
public class ConnectibleChains {
    public static final String MODID = "connectiblechains";
    public static final Logger LOGGER = LogManager.getLogger(MODID);
    public static CCConfig runtimeConfig;

    public ConnectibleChains() {
        ModEntityTypes.register();
        ChainTypesRegistry.init();
        BuiltinCompat.init();

        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::setup);
        MinecraftForge.EVENT_BUS.addListener(this::chainUseEvent);
//        MinecraftForge.EVENT_BUS.addListener(this::onBlockBreak);

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

        if (player == null || player.isCrouching()) return;
        ItemStack stack = player.getItemInHand(hand);
        BlockPos blockPos = hitResult.getBlockPos();
        BlockState block = world.getBlockState(blockPos);

        if (!ChainKnotEntity.canAttachTo(block)) return;
        if (world.isClientSide) {
            Item handItem = player.getItemInHand(hand).getItem();
            if (ChainTypesRegistry.ITEM_CHAIN_TYPES.containsKey(handItem)) {
                event.setCanceled(true);
            }
            // Check if any held chains can be attached. This can be done without holding a chain item
            else if (ChainKnotEntity.getHeldChainsInRange(player, blockPos).size() > 0) {
                event.setCanceled(true);
            }
            // Check if a knot exists and can be destroyed
            // Would work without this check but no swing animation would be played
            else if (ChainKnotEntity.getKnotAt(player.level, blockPos) != null && ChainLinkEntity.canDestroyWith(stack)) {
                event.setCanceled(true);
            }
            if (event.isCanceled()) event.setCancellationResult(InteractionResult.SUCCESS);
            return;
        }

        // 1. Try with existing knot, regardless of hand item
        ChainKnotEntity knot = ChainKnotEntity.getKnotAt(world, blockPos);
        if (knot != null) {
            if (knot.interact(player, hand) == InteractionResult.CONSUME) {
                event.setCanceled(true);
                event.setCancellationResult(InteractionResult.CONSUME);
            }
        } else {
            // 2. Check if any held chains can be attached.
            List<ChainLink> attachableChains = ChainKnotEntity.getHeldChainsInRange(player, blockPos);

            // Use the held item as the new knot type
            ChainType knotType = ChainTypesRegistry.ITEM_CHAIN_TYPES.get(stack.getItem());

            // Allow default interaction behaviour.
            if (attachableChains.size() == 0 && knotType == null) {
                return;
            }

            // Held item does not correspond to a type.
            if (knotType == null)
                knotType = attachableChains.get(0).chainType;

            // 3. Create new knot if none exists and delegate interaction
            knot = new ChainKnotEntity(world, blockPos, knotType);
            knot.setGraceTicks((byte) 0);
            world.addFreshEntity(knot);
            knot.playPlacementSound();
            InteractionResult result = knot.interact(player, hand);
            event.setCanceled(true);
            event.setCancellationResult(result);
        }
    }
}
