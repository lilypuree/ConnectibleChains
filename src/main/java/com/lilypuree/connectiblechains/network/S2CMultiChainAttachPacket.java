package com.lilypuree.connectiblechains.network;

import com.lilypuree.connectiblechains.ConnectibleChains;
import com.lilypuree.connectiblechains.entity.ChainKnotEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.function.Supplier;

public class S2CMultiChainAttachPacket {

    int from;
    int[] tos;

    public static ResourceLocation S2C_MULTI_CHAIN_ATTACH_PACKET_ID = new ResourceLocation(ConnectibleChains.MODID, "s2c_multi_chain_attach_packet_id");

    public S2CMultiChainAttachPacket(int from, int[] tos) {
        this.from = from;
        this.tos = tos;
    }

    public S2CMultiChainAttachPacket(PacketBuffer buf) {
        from = buf.readInt();
        tos = buf.readVarIntArray();
    }

    public void toBytes(PacketBuffer buf) {
        buf.writeInt(from);
        buf.writeVarIntArray(tos);
    }

    public boolean handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            Entity entity = Minecraft.getInstance().level.getEntity(from);
            if (entity instanceof ChainKnotEntity) {
                ((ChainKnotEntity) entity).addHoldingEntityIds(tos);
            }
        });
        return true;
    }
}
