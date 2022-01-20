package com.lilypuree.connectiblechains.events;

import com.lilypuree.connectiblechains.ConnectibleChains;
import com.lilypuree.connectiblechains.entity.ChainKnotEntity;
import com.lilypuree.connectiblechains.entity.ModEntityTypes;
import com.lilypuree.connectiblechains.network.ModPacketHandler;
import com.lilypuree.connectiblechains.network.S2CMultiChainAttachPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;

@Mod.EventBusSubscriber(modid = ConnectibleChains.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class EventHandler {

    @SubscribeEvent
    public static void onEntitySpawn(EntityJoinWorldEvent event) {
        Entity entity = event.getEntity();
        if (entity.getType() == ModEntityTypes.CHAIN_KNOT.get()) {
            entity.setDeltaMovement(Vec3.ZERO);
        }
    }

    @SubscribeEvent
    public static void onStartTracking(PlayerEvent.StartTracking event) {
        if (event.getTarget() instanceof ChainKnotEntity chainKnotEntity) {
            //Write our id and the id of the one we connect to.
            int[] ids = chainKnotEntity.getHoldingEntities().stream().mapToInt(Entity::getId).toArray();
            if (ids.length > 0) {
                S2CMultiChainAttachPacket packet = new S2CMultiChainAttachPacket(chainKnotEntity.getId(), ids);
                ModPacketHandler.INSTANCE.send(PacketDistributor.PLAYER.with(() -> (ServerPlayer) event.getPlayer()), packet);
            }
        }
    }
}
