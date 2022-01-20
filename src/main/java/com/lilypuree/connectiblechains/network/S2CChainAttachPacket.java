package com.lilypuree.connectiblechains.network;

import com.lilypuree.connectiblechains.ConnectibleChains;
import com.lilypuree.connectiblechains.entity.ChainKnotEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class S2CChainAttachPacket {

    int[] fromTo;
    int fromPlayer;

    public static ResourceLocation S2C_CHAIN_ATTACH_PACKET_ID = new ResourceLocation(ConnectibleChains.MODID, "s2c_chain_attach_packet_id");

    public S2CChainAttachPacket(int fromPlayer, int[] fromTo) {
        this.fromPlayer = fromPlayer;
        this.fromTo = fromTo;
    }

    public S2CChainAttachPacket(FriendlyByteBuf buf) {
        fromTo = buf.readVarIntArray();
        fromPlayer = buf.readInt();
    }

    public void toBytes(FriendlyByteBuf buf) {
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
