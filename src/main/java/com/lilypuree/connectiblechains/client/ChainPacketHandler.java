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

package com.lilypuree.connectiblechains.client;

import com.lilypuree.connectiblechains.chain.IncompleteChainLink;
import com.lilypuree.connectiblechains.networking.packet.ChainAttachPayload;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ChainPacketHandler {

    /**
     * Called on every client tick.
     * Tries to complete all links.
     * Completed links or links that are no longer valid because the primary is dead are removed.
     */
    public void tick() {
        ChainAttachPayload.incompleteLinks.removeIf(IncompleteChainLink::tryCompleteOrRemove);
    }
}
