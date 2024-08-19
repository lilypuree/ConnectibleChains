/*
 * Copyright (C) 2024 legoatoom.
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

import com.lilypuree.connectiblechains.util.Helper;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.List;

public record MultiChainAttachPayload(List<ChainAttachPayload> packets) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<MultiChainAttachPayload> TYPE = new CustomPacketPayload.Type<>(Helper.rl("s2c_multi_chain_attach_packet_id"));
    public static final StreamCodec<RegistryFriendlyByteBuf, MultiChainAttachPayload> STREAM_CODEC = StreamCodec.composite(
            ChainAttachPayload.STREAM_CODEC.apply(ByteBufCodecs.list()),
            MultiChainAttachPayload::packets,
            MultiChainAttachPayload::new
    );

    public static void apply(MultiChainAttachPayload payload, IPayloadContext context) {
        try {
            payload.packets.forEach(packet -> packet.apply(packet, context));
        } catch (Throwable ignored) {}
    }

    @Override
    public Type<MultiChainAttachPayload> type() {
        return TYPE;
    }
}
