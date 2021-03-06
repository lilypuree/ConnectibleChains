package com.lilypuree.connectiblechains.network;

import com.lilypuree.connectiblechains.ConnectibleChains;
import com.lilypuree.connectiblechains.entity.ChainKnotEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.function.Supplier;

public class S2CChainDetachPacket {

    int[] fromTo;

    public static ResourceLocation S2C_CHAIN_DETACH_PACKET_ID = new ResourceLocation(ConnectibleChains.MODID, "s2c_chain_detach_packet_id");

    public S2CChainDetachPacket(PacketBuffer buf) {
        fromTo = buf.readVarIntArray();
            }

    public void toBytes(PacketBuffer buf) {
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
