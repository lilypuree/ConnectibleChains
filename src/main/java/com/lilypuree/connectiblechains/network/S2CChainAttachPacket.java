package com.lilypuree.connectiblechains.network;

import com.lilypuree.connectiblechains.ConnectibleChains;
import com.lilypuree.connectiblechains.client.ClientInitializer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkEvent;

import java.util.Collections;
import java.util.function.Supplier;

public class S2CChainAttachPacket {

    public static ResourceLocation S2C_CHAIN_ATTACH_PACKET_ID = new ResourceLocation(ConnectibleChains.MODID, "s2c_chain_attach_packet_id");
    private final int fromId;
    private final int toId;
    private final ResourceLocation chainType;

    public S2CChainAttachPacket(int fromId, int toId, ResourceLocation chainType) {
        this.fromId = fromId;
        this.toId = toId;
        this.chainType = chainType;
    }

    public S2CChainAttachPacket(FriendlyByteBuf buf) {
        fromId = buf.readInt();
        toId = buf.readInt();
        chainType = buf.readResourceLocation();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeInt(fromId);
        buf.writeInt(toId);
        buf.writeResourceLocation(chainType);
    }

    public boolean handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            try {
                ClientInitializer.chainPacketHandler.createLinks(fromId, new int[]{toId}, Collections.singletonList(chainType));
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        return true;
    }
}
