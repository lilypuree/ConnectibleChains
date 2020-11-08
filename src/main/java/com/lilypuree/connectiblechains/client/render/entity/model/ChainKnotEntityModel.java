package com.lilypuree.connectiblechains.client.render.entity.model;

import com.google.common.collect.ImmutableList;
import net.minecraft.client.renderer.entity.model.SegmentedModel;
import net.minecraft.client.renderer.model.ModelRenderer;
import net.minecraft.entity.Entity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

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
