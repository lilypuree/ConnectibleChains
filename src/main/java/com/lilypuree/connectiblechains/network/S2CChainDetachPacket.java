package com.lilypuree.connectiblechains.network;

import com.lilypuree.connectiblechains.ConnectibleChains;
import com.lilypuree.connectiblechains.chain.IncompleteChainLink;
import com.lilypuree.connectiblechains.client.ClientInitializer;
import com.lilypuree.connectiblechains.entity.ChainKnotEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class S2CChainDetachPacket {

    public static ResourceLocation S2C_CHAIN_DETACH_PACKET_ID = new ResourceLocation(ConnectibleChains.MODID, "s2c_chain_detach_packet_id");
    private int fromId;
    private int toId;

    public S2CChainDetachPacket(int fromId, int toId) {
        this.fromId = fromId;
        this.toId = toId;
    }

    public S2CChainDetachPacket(FriendlyByteBuf buf) {
        fromId = buf.readInt();
        toId = buf.readInt();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeInt(fromId);
        buf.writeInt(toId);
    }

    public boolean handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ClientInitializer.chainPacketHandler.removeLink(fromId, toId);
        });
        return true;
    }
}
