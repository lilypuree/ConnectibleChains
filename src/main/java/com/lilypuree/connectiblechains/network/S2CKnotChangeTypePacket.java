package com.lilypuree.connectiblechains.network;

import com.lilypuree.connectiblechains.ConnectibleChains;
import com.lilypuree.connectiblechains.client.ClientInitializer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class S2CKnotChangeTypePacket {

    private final int knotId;
    private final ResourceLocation typeId;


    public static ResourceLocation S2C_KNOT_CHANGE_TYPE_PACKET_ID = new ResourceLocation(ConnectibleChains.MODID, "s2c_knot_change_type_packet_id");

    public S2CKnotChangeTypePacket(int knotId, ResourceLocation typeId) {
        this.knotId = knotId;
        this.typeId = typeId;
    }

    public S2CKnotChangeTypePacket(FriendlyByteBuf buf) {
        knotId = buf.readVarInt();
        typeId = buf.readResourceLocation();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeVarInt(knotId);
        buf.writeResourceLocation(typeId);
    }

    public boolean handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ClientInitializer.chainPacketHandler.changeKnotType(knotId, typeId);
        });
        return true;
    }
}
