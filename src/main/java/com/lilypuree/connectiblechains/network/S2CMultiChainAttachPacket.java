package com.lilypuree.connectiblechains.network;

import com.lilypuree.connectiblechains.ConnectibleChains;
import com.lilypuree.connectiblechains.client.ClientInitializer;
import com.lilypuree.connectiblechains.entity.ChainKnotEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.network.NetworkEvent;

import java.util.List;
import java.util.function.Supplier;

public class S2CMultiChainAttachPacket {

    int fromId;
    int[] toIds;
    List<ResourceLocation> types;

    public static ResourceLocation S2C_MULTI_CHAIN_ATTACH_PACKET_ID = new ResourceLocation(ConnectibleChains.MODID, "s2c_multi_chain_attach_packet_id");


    public S2CMultiChainAttachPacket(int fromId, int[] toIds, List<ResourceLocation> types) {
        this.fromId = fromId;
        this.toIds = toIds;
        this.types = types;
    }

    public S2CMultiChainAttachPacket(FriendlyByteBuf buf) {
        fromId = buf.readInt();
        toIds = buf.readVarIntArray();
        types = buf.readList(FriendlyByteBuf::readResourceLocation);
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeInt(fromId);
        buf.writeVarIntArray(toIds);
        buf.writeCollection(types, FriendlyByteBuf::writeResourceLocation);
    }

    public boolean handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ClientInitializer.chainPacketHandler.createLinks(fromId, toIds, types);
        });
        return true;
    }
}
