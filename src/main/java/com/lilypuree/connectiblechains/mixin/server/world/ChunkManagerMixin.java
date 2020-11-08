/*
 *     Copyright (C) 2020 legoatoom
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.lilypuree.connectiblechains.mixin.server.world;

import com.google.common.collect.Lists;
import com.lilypuree.connectiblechains.entity.ChainKnotEntity;
import com.lilypuree.connectiblechains.network.ModPacketHandler;
import com.lilypuree.connectiblechains.network.S2CMultiChainAttachPacket;
import io.netty.buffer.Unpooled;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.IPacket;
import net.minecraft.network.PacketBuffer;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.server.ChunkManager;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(ChunkManager.class)
public class ChunkManagerMixin {

    @Shadow
    @Final
    private Int2ObjectMap<ChunkManager.EntityTracker> entityMap;

    @Inject(
            method = "playerLoadedChunk",
            at = @At(value = "TAIL")
    )
    public void sendAttachChainPackets(ServerPlayerEntity player, IPacket<?>[] packets, Chunk chunk, CallbackInfo ci) {
        ObjectIterator<ChunkManager.EntityTracker> var6 = this.entityMap.values().iterator();
        List<ChainKnotEntity> list = Lists.newArrayList();

        while (var6.hasNext()) {
            ChunkManager.EntityTracker entityTracker = var6.next();
            Entity entity = entityTracker.entity;
            if (entity != player && entity.xChunk == chunk.getPos().x && entity.zChunk == chunk.getPos().z) {
                if (entity instanceof ChainKnotEntity && !((ChainKnotEntity) entity).getHoldingEntities().isEmpty()) {
                    list.add((ChainKnotEntity) entity);
                }
            }
        }

        if (!list.isEmpty()) {
            for (ChainKnotEntity chainKnotEntity : list) {
                chainKnotEntity.init();
//                PacketBuffer passedData = new PacketBuffer(Unpooled.buffer());
//                //Write our id and the id of the one we connect to.
//                int[] ids = chainKnotEntity.getHoldingEntities().stream().mapToInt(Entity::getId).toArray();
//                if (ids.length > 0) {
//                    passedData.writeInt(chainKnotEntity.getId());
//                    passedData.writeVarIntArray(ids);
//                    ModPacketHandler.sendToClient(new S2CMultiChainAttachPacket(passedData), player);
//                }
            }
        }
    }

}
