package com.lilypuree.connectiblechains.util;

import com.lilypuree.connectiblechains.chain.ChainLink;
import com.lilypuree.connectiblechains.chain.ChainTypesRegistry;
import com.lilypuree.connectiblechains.entity.ChainKnotEntity;
import com.lilypuree.connectiblechains.network.ModPacketHandler;
import com.lilypuree.connectiblechains.network.S2CMultiChainAttachPacket;
import io.netty.buffer.Unpooled;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class PacketCreator {

    @Nullable
    public static S2CMultiChainAttachPacket createMultiAttach(ChainKnotEntity knot) {
        List<ChainLink> links = knot.getLinks();
        IntList ids = new IntArrayList(links.size());
        List<ResourceLocation> types = new ArrayList<>(links.size());
        for (ChainLink link : links) {
            if (link.primary == knot) {
                ids.add(link.secondary.getId());
                types.add(ChainTypesRegistry.getKey(link.chainType));
            }
        }
        if (ids.size() > 0) {
            return new S2CMultiChainAttachPacket(knot.getId(), ids.toIntArray(), types);
        }
        return null;
    }
}
