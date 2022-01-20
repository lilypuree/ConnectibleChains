package com.lilypuree.connectiblechains.network;

import com.lilypuree.connectiblechains.ConnectibleChains;
import com.lilypuree.connectiblechains.entity.ChainKnotEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class S2CChainDetachPacket {

    int[] fromTo;

    public static ResourceLocation S2C_CHAIN_DETACH_PACKET_ID = new ResourceLocation(ConnectibleChains.MODID, "s2c_chain_detach_packet_id");

    public S2CChainDetachPacket(int[] fromTo) {
        this.fromTo = fromTo;
    }

    public S2CChainDetachPacket(FriendlyByteBuf buf) {
        fromTo = buf.readVarIntArray();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeVarIntArray(fromTo);
    }

    public boolean handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            Entity entity = Minecraft.getInstance().level.getEntity(fromTo[0]);
            if (entity instanceof ChainKnotEntity) {
                ((ChainKnotEntity) entity).removeHoldingEntityId(fromTo[1]);
            }
        });
        return true;
    }
}
