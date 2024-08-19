/*
 * Copyright (C) 2024 legoatoom
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.lilypuree.connectiblechains.networking.packet;

import com.lilypuree.connectiblechains.ConnectibleChains;
import com.lilypuree.connectiblechains.entity.ChainKnotEntity;
import com.lilypuree.connectiblechains.util.Helper;
import net.minecraft.client.Minecraft;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record KnotChangePayload(int knotId, Item sourceItem) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<KnotChangePayload> TYPE = new CustomPacketPayload.Type<>(Helper.rl("s2c_knot_change_type_packet_id"));
    public static final StreamCodec<RegistryFriendlyByteBuf, KnotChangePayload> STREAM_CODEC =
            StreamCodec.ofMember((value, buf) -> {
                buf.writeVarInt(value.knotId);
                buf.writeVarInt(Item.getId(value.sourceItem));
            }, buf -> new KnotChangePayload(buf.readVarInt(), Item.byId(buf.readVarInt())));


    @Override
    public Type<KnotChangePayload> type() {
        return TYPE;
    }

    public static void apply(KnotChangePayload payload, IPayloadContext context) {
        try {
            Minecraft client = Minecraft.getInstance();
            client.execute(() -> {
                if (client.level == null) return;
                Entity entity = client.level.getEntity(payload.knotId);
                if (entity instanceof ChainKnotEntity knot) {
                    knot.updateChainType(payload.sourceItem);
                } else {
                    logBadActionTarget(entity, payload.knotId);
                }
            });
        } catch (Throwable ignored) {}
    }

    private static void logBadActionTarget(Entity target, int targetId) {
        ConnectibleChains.LOGGER.warn(String.format("Tried to %s %s (#%d) which is not %s",
                "change type of", target, targetId, "chain knot"
        ));
    }
}
