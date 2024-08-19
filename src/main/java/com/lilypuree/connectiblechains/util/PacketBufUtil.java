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

package com.lilypuree.connectiblechains.util;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.Vec3;

/**
 * <a href="https://fabricmc.net/wiki/tutorial:projectiles">This class is from a tutorial</a> Edited some things to make it more useful for me.
 */
public final class PacketBufUtil {
    /**
     * Writes a {@link net.minecraft.world.phys.Vec3} to a {@link net.minecraft.network.FriendlyByteBuf}.
     *
     * @param byteBuf destination buffer
     * @param vec3   vector
     */
    public static void writeVec3(FriendlyByteBuf byteBuf, Vec3 vec3) {
        byteBuf.writeDouble(vec3.x);
        byteBuf.writeDouble(vec3.y);
        byteBuf.writeDouble(vec3.z);
    }

    /**
     * Reads a {@link Vec3} from a {@link FriendlyByteBuf}.
     *
     * @param byteBuf source buffer
     * @return vector
     */
    public static Vec3 readVec3(FriendlyByteBuf byteBuf) {
        double x = byteBuf.readDouble();
        double y = byteBuf.readDouble();
        double z = byteBuf.readDouble();
        return new Vec3(x, y, z);
    }
}
