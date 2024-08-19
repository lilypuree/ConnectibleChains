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

package com.lilypuree.connectiblechains.client.render.entity.model;

import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.world.entity.Entity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * Model for the {@link com.lilypuree.connectiblechains.entity.ChainKnotEntity}.
 * Similar to the {@link net.minecraft.client.model.LeashKnotModel} code.
 * <p>
 * The model is 6x3x6 pixels big.
 *
 * @author legoatoom
 * @see net.minecraft.client.renderer.entity.LeashKnotRenderer
 */
@OnlyIn(Dist.CLIENT)
public class ChainKnotEntityModel<T extends Entity> extends HierarchicalModel<T> {
    private final ModelPart chainKnot;

    public ChainKnotEntityModel(ModelPart root) {
        this.chainKnot = root.getChild("knot");
    }

    public static LayerDefinition getTexturedModelData() {
        MeshDefinition modelData = new MeshDefinition();
        PartDefinition modelPartData = modelData.getRoot();
        PartDefinition bb_main = modelPartData.addOrReplaceChild("knot", CubeListBuilder.create(), PartPose.offset(0.0F, -12.5F, 0.0F));

        bb_main.addOrReplaceChild("knot_child", CubeListBuilder.create().texOffs(3, 1).addBox(-1.0F, -1.5F, 3.0F, 3.0F, 6.0F, 0.0F, new CubeDeformation(0.0F))
                .texOffs(0, 1).addBox(-1.0F, -1.5F, -3.0F, 3.0F, 0.0F, 6.0F, new CubeDeformation(0.0F))
                .texOffs(0, 9).mirror().addBox(-1.0F, 4.5F, -3.0F, 3.0F, 0.0F, 6.0F, new CubeDeformation(0.0F)).mirror(false)
                .texOffs(3, 6).addBox(-1.0F, -1.5F, -3.0F, 3.0F, 6.0F, 0.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-1.5F, 7.0F, 0.0F, 0.0F, 0.0F, -1.5708F));
        return LayerDefinition.create(modelData, 16, 16);
    }

    @Override
    public ModelPart root() {
        return chainKnot;
    }

    @Override
    public void setupAnim(T entity, float limbAngle, float limbDistance, float animationProgress, float headYaw, float headPitch) {
    }
}
