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
package com.lilypuree.connectiblechains.client.render.entity.model;

import com.google.common.collect.ImmutableList;
import net.minecraft.client.renderer.entity.model.SegmentedModel;
import net.minecraft.client.renderer.model.ModelRenderer;
import net.minecraft.entity.Entity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * Model for the {@link com.lilypuree.connectiblechains.entity.ChainKnotEntity}.
 * Similar to the {@link net.minecraft.client.renderer.entity.model.LeashKnotModel} code.
 *
 * @see net.minecraft.client.renderer.entity.LeashKnotRenderer
 * @author legoatoom
 */
@OnlyIn(Dist.CLIENT)
public class ChainKnotEntityModel<T extends Entity> extends SegmentedModel<T> {
    private final ModelRenderer chainKnot;

    public ChainKnotEntityModel() {
        this.texWidth = 32;
        this.texHeight = 32;
        this.chainKnot = new ModelRenderer(this, 0, 0);
        this.chainKnot.addBox(-3.0F, -6.0F, -3.0F, 6.0F, 3.0F, 6.0F, 0.0F);
        this.chainKnot.setPos(0.0F, 0.0F, 0.0F);
    }

    @Override
    public Iterable<ModelRenderer> parts() {
        return ImmutableList.of(this.chainKnot);
    }

    @Override
    public void setupAnim(T t, float limbAngle, float limbDistance, float customAngle, float headYaw, float headPitch) {
        this.chainKnot.yRot = headYaw * 0.017453292F;
        this.chainKnot.xRot = headPitch * 0.017453292F;
    }
}
