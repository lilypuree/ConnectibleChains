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

import com.lilypuree.connectiblechains.chain.ChainLink;
import com.lilypuree.connectiblechains.chain.IncompleteChainLink;
import com.lilypuree.connectiblechains.entity.ChainKnotEntity;
import com.lilypuree.connectiblechains.util.Helper;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import static com.lilypuree.connectiblechains.ConnectibleChains.LOGGER;

public record ChainAttachPayload(int primaryEntityId, int secondaryEntityId, int chainTypeId,
                                 boolean attach) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<ChainAttachPayload> TYPE = new CustomPacketPayload.Type<>(Helper.rl("s2c_chain_attach_packet_id"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ChainAttachPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, ChainAttachPayload::primaryEntityId,
                    ByteBufCodecs.VAR_INT, ChainAttachPayload::secondaryEntityId,
                    ByteBufCodecs.VAR_INT, ChainAttachPayload::chainTypeId,
                    ByteBufCodecs.BOOL, ChainAttachPayload::attach,
                    ChainAttachPayload::new);

    /**
     * Links where this is the primary and the secondary doesn't yet exist / hasn't yet loaded.
     * They are kept in a separate list to prevent accidental accesses of the secondary which would
     * result in a NPE. The links will try to be completed each world tick.
     */
    public static final ObjectList<IncompleteChainLink> incompleteLinks = new ObjectArrayList<>(256);

    public ChainAttachPayload(ChainLink link, boolean attach) {
        this(link.getPrimary().getId(), link.getSecondary().getId(), BuiltInRegistries.ITEM.getId(link.getSourceItem()), attach);
    }

    private static void applyDetach(ChainAttachPayload payload, LocalPlayer localPlayer) {
        ClientLevel world = localPlayer.clientLevel;
        Entity primary = world.getEntity(payload.primaryEntityId);

        if (!(primary instanceof ChainKnotEntity primaryKnot)) {
            LOGGER.warn(String.format("Tried to detach from %s (#%d) which is not a chain knot",
                    primary, payload.primaryEntityId
            ));
            return;
        }
        Entity secondary = world.getEntity(payload.secondaryEntityId);
        incompleteLinks.removeIf(link -> {
            if (link.primary == primaryKnot && link.secondaryId == payload.secondaryEntityId) {
                link.destroy();
                return true;
            }
            return false;
        });

        if (secondary == null) {
            return;
        }

        for (ChainLink link : primaryKnot.getLinks()) {
            if (link.getSecondary() == secondary) {
                link.destroy(true);
            }
        }
    }

    private static void applyAttach(ChainAttachPayload payload, LocalPlayer clientPlayer) {
        ClientLevel world = clientPlayer.clientLevel;
        Entity primary = world.getEntity(payload.primaryEntityId);

        if (!(primary instanceof ChainKnotEntity primaryKnot)) {
            LOGGER.warn(String.format("Tried to attach from %s (#%d) which is not a chain knot",
                    primary, payload.primaryEntityId
            ));
            return;
        }
        Entity secondary = world.getEntity(payload.secondaryEntityId);

        Item chainType = BuiltInRegistries.ITEM.getHolder(payload.chainTypeId).get().value();

        if (secondary == null) {
            incompleteLinks.add(new IncompleteChainLink(primaryKnot, payload.secondaryEntityId, chainType));
        } else {
            ChainLink.create(primaryKnot, secondary, chainType);
        }
    }

    @Override
    public CustomPacketPayload.Type<ChainAttachPayload> type() {
        return TYPE;
    }

    @OnlyIn(Dist.CLIENT)
    public static void apply(ChainAttachPayload payload,IPayloadContext context) {
        try {
            if (payload.attach){
                applyAttach(payload, (LocalPlayer) context.player());
                return;
            }
            applyDetach(payload, (LocalPlayer) context.player());
        } catch (Throwable ignored) {}
    }
}
