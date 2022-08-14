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

import com.lilypuree.connectiblechains.entity.ChainKnotEntity;
import com.lilypuree.connectiblechains.network.ModPacketHandler;
import com.lilypuree.connectiblechains.network.S2CMultiChainAttachPacket;
import com.lilypuree.connectiblechains.util.PacketCreator;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraftforge.network.PacketDistributor;
import org.apache.commons.lang3.mutable.MutableObject;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * Mixin is used to keep track of the connections when the ChainKnot is loaded again.
 * <p>
 * If we do not do this, the client does not know about connections that are loaded in new chunks.
 *
 * @author legoatoom
 */
@Mixin(ChunkMap.class)
public class ChunkMapMixin {

    @Shadow
    @Final
    private Int2ObjectMap<ChunkMap.TrackedEntity> entityMap;

    @Inject(
            method = "playerLoadedChunk",
            at = @At(value = "TAIL")
    )
    private void sendAttachChainPackets(ServerPlayer player, MutableObject<ClientboundLevelChunkWithLightPacket> cachedDataPacket, LevelChunk chunk, CallbackInfo ci) {
        List<ChainKnotEntity> knots = new ArrayList<>();

        var trackers = this.entityMap.values().iterator();
        while (trackers.hasNext()) {
            ChunkMap.TrackedEntity entityTracker = trackers.next();
            Entity entity = entityTracker.entity;
            if (entity != player && entity.chunkPosition().equals(chunk.getPos())) {
                if (entity instanceof ChainKnotEntity && !((ChainKnotEntity) entity).getLinks().isEmpty()) {
                    knots.add((ChainKnotEntity) entity);
                }
            }
        }

        for (ChainKnotEntity knot : knots) {
            S2CMultiChainAttachPacket packet = PacketCreator.createMultiAttach(knot);
            if (packet != null) {
                ModPacketHandler.INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), packet);
            }
        }
    }
}
