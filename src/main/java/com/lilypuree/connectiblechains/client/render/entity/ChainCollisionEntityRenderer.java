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

package com.lilypuree.connectiblechains.client.render.entity;

import com.lilypuree.connectiblechains.entity.ChainCollisionEntity;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * Renderer for the {@link ChainCollisionEntity}.
 * Entities are required to have a renderer. So this is the class that "renders" the entity.
 * Since this entity does not have a texture, it does not need to render anything.
 *
 * @author legoatoom
 */
@OnlyIn(Dist.CLIENT)
public class ChainCollisionEntityRenderer extends EntityRenderer<ChainCollisionEntity> {

    public ChainCollisionEntityRenderer(EntityRendererProvider.Context dispatcher) {
        super(dispatcher);
    }

    @Override
    public ResourceLocation getTextureLocation(ChainCollisionEntity entity) {
        return null;
    }
}
