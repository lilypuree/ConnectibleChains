package com.lilypuree.connectiblechains.network;

import com.lilypuree.connectiblechains.ConnectibleChains;
import com.lilypuree.connectiblechains.entity.ChainKnotEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.network.NetworkEvent;


import java.util.function.Supplier;

public class S2CChainAttachPacket {

    int[] fromTo;
    int fromPlayer;

    public static ResourceLocation S2C_CHAIN_ATTACH_PACKET_ID = new ResourceLocation(ConnectibleChains.MODID, "s2c_chain_attach_packet_id");

    public S2CChainAttachPacket(int[] fromTo, int fromPlayer) {
        this.fromTo = fromTo;
        this.fromPlayer = fromPlayer;
    }


    public S2CChainAttachPacket(PacketBuffer buf) {
        fromTo = buf.readVarIntArray();
        fromPlayer = buf.readInt();
    }

    public void toBytes(PacketBuffer buf) {
        buf.writeVarIntArray(fromTo);
        buf.writeInt(fromPlayer);
    }

    public boolean handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            Entity entity = Minecraft.getInstance().level.getEntity(fromTo[0]);
            if (entity instanceof ChainKnotEntity) {
                ((ChainKnotEntity) entity).addHoldingEntityId(fromTo[1], fromPlayer);
            }
        });
        return true;
    }
}
