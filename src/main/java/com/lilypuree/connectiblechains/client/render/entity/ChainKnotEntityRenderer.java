/*
 * Copyright (C) 2022 legoatoom
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.lilypuree.connectiblechains.client.render.entity;

import com.lilypuree.connectiblechains.ConnectibleChains;
import com.lilypuree.connectiblechains.client.render.entity.model.ChainKnotEntityModel;
import com.lilypuree.connectiblechains.entity.ChainKnotEntity;
import com.lilypuree.connectiblechains.client.ClientEventHandler;
import com.lilypuree.connectiblechains.util.Helper;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Matrix4f;
import com.mojang.math.Quaternion;
import com.mojang.math.Vector3f;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.HangingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.ArrayList;

/**
 * <p>This class renders the chain you see in game. The block around the fence and the chain.
 * You could use this code to start to understand how this is done.
 * I tried to make it as easy to understand as possible, mainly for myself, since the MobEntityRenderer has a lot of
 * unclear code and shortcuts made.</p>
 *
 * <p>Following is the formula used. h is the height difference, d is the distance and α is a scaling factor</p>
 *
 * <img src="./doc-files/formula.png">
 *
 * @author legoatoom
 * @see net.minecraft.client.renderer.entity.LeashKnotRenderer
 * @see net.minecraft.client.renderer.entity.MobRenderer
 */
@OnlyIn(Dist.CLIENT)
public class ChainKnotEntityRenderer extends EntityRenderer<ChainKnotEntity> {
    private static final ResourceLocation KNOT_TEXTURE = Helper.identifier("textures/entity/chain_knot.png");
    private static final ResourceLocation CHAIN_TEXTURE = new ResourceLocation("textures/block/chain.png");
    private final ChainKnotEntityModel<ChainKnotEntity> model;
    private final ChainRenderer chainRenderer = new ChainRenderer();

    public ChainKnotEntityRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.model = new ChainKnotEntityModel<>(context.bakeLayer(ClientEventHandler.CHAIN_KNOT));
    }

    @Override
    public boolean shouldRender(ChainKnotEntity entity, Frustum frustum, double x, double y, double z) {
        boolean should = entity.getHoldingEntities().stream().anyMatch(entity1 -> {
            if (entity1 instanceof ChainKnotEntity) {
                if (!entity1.shouldRender(x, y, z)) {
                    return false;
                } else if (entity1.noCulling) {
                    return true;
                } else {
                    AABB box = entity1.getBoundingBoxForCulling().inflate(entity.distanceTo(entity1) / 2D);
                    if (box.hasNaN() || box.getSize() == 0.0D) {
                        box = new AABB(entity1.getX() - 2.0D, entity1.getY() - 2.0D, entity1.getZ() - 2.0D, entity1.getX() + 2.0D, entity1.getY() + 2.0D, entity1.getZ() + 2.0D);
                    }

                    return frustum.isVisible(box);
                }
            } else return entity1 instanceof Player;
        });
        return super.shouldRender(entity, frustum, x, y, z) || should;
    }

    @Override
    public ResourceLocation getTextureLocation(ChainKnotEntity pEntity) {
        return KNOT_TEXTURE;
    }

    @Override
    public void render(ChainKnotEntity chainKnotEntity, float yaw, float partialTicks, PoseStack matrices, MultiBufferSource vertexConsumers, int light) {
        matrices.pushPose();
        Vec3 leashOffset = chainKnotEntity.getRopeHoldPosition(partialTicks).subtract(chainKnotEntity.getPosition(partialTicks));
        matrices.translate(leashOffset.x, leashOffset.y + 6.5 / 16f, leashOffset.z);
        matrices.scale(5 / 6f, 1, 5 / 6f);
        this.model.setupAnim(chainKnotEntity, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F);
        VertexConsumer vertexConsumer = vertexConsumers.getBuffer(this.model.renderType(KNOT_TEXTURE));
        this.model.renderToBuffer(matrices, vertexConsumer, light, OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);
        matrices.popPose();
        ArrayList<Entity> entities = chainKnotEntity.getHoldingEntities();
        for (Entity entity : entities) {
            this.createChainLine(chainKnotEntity, partialTicks, matrices, vertexConsumers, entity);
            if (ConnectibleChains.runtimeConfig.doDebugDraw()) {
                this.drawDebugVector(matrices, chainKnotEntity, entity, vertexConsumers.getBuffer(RenderType.LINES));
            }
        }
        if (ConnectibleChains.runtimeConfig.doDebugDraw()) {
            matrices.pushPose();
            // F stands for "from"
            Component holdingCount = new TextComponent("F: " + chainKnotEntity.getHoldingEntities().size());
            matrices.translate(0, 0.25, 0);
            this.renderNameTag(chainKnotEntity, holdingCount, matrices, vertexConsumers, light);
            matrices.popPose();
        }
        super.render(chainKnotEntity, yaw, partialTicks, matrices, vertexConsumers, light);
    }

    /**
     * Draws a line fromEntity - toEntity, from green to red.
     */
    private void drawDebugVector(PoseStack matrices, Entity fromEntity, Entity toEntity, VertexConsumer buffer) {
        if (toEntity == null) return;
        Matrix4f modelMat = matrices.last().pose();
        Vec3 vec = toEntity.position().subtract(fromEntity.position());
        Vec3 normal = vec.normalize();
        buffer.vertex(modelMat, 0, 0, 0)
                .color(0, 255, 0, 255)
                .normal((float) normal.x, (float) normal.y, (float) normal.z).endVertex();
        buffer.vertex(modelMat, (float) vec.x, (float) vec.y, (float) vec.z)
                .color(255, 0, 0, 255)
                .normal((float) normal.x, (float) normal.y, (float) normal.z).endVertex();
    }

    /**
     * If I am honest I do not really know what is happening here most of the time, most of the code was 'inspired' by
     * the {@link net.minecraft.client.renderer.entity.LeashKnotRenderer}.
     * Many variables therefore have simple names. I tried my best to comment and explain what everything does.
     *
     * @param fromEntity             The origin Entity
     * @param tickDelta              Delta tick
     * @param matrices               The render matrix stack.
     * @param vertexConsumerProvider The VertexConsumerProvider, whatever it does.
     * @param toEntity               The entity that we connect the chain to, this can be a {@link Player} or a {@link ChainKnotEntity}.
     */
    private void createChainLine(ChainKnotEntity fromEntity, float tickDelta, PoseStack matrices, MultiBufferSource vertexConsumerProvider, Entity toEntity) {
        if (toEntity == null) return; // toEntity can be null, this will return the function if it is null.
        matrices.pushPose();

        // Don't have to lerp knot position as it can't move
        // Also lerping the position of an entity that was just created
        // causes visual bugs because the position is lerped from 0/0/0.
        Vec3 srcPos = fromEntity.position().add(fromEntity.getLeashOffset());
        Vec3 dstPos;

        if (toEntity instanceof HangingEntity) {
            dstPos = toEntity.position().add(toEntity.getLeashOffset());
        } else {
            dstPos = toEntity.getRopeHoldPosition(tickDelta);
        }

        // The leash pos offset
        Vec3 leashOffset = fromEntity.getLeashOffset();
        matrices.translate(leashOffset.x, leashOffset.y, leashOffset.z);

        // Some further performance improvements can be made here:
        // Create a rendering layer that:
        // - does not have normals
        // - does not have an overlay
        // - does not have vertex color
        // - uses a tri strp instead of quads
        VertexConsumer buffer = vertexConsumerProvider.getBuffer(RenderType.entityCutoutNoCull(CHAIN_TEXTURE));
        if (ConnectibleChains.runtimeConfig.doDebugDraw()) {
            buffer = vertexConsumerProvider.getBuffer(RenderType.lines());
        }

        Vector3f offset = Helper.getChainOffset(srcPos, dstPos);
        matrices.translate(offset.x(), 0, offset.z());

        // Now we gather light information for the chain. Since the chain is lighter if there is more light.
        BlockPos blockPosOfStart = new BlockPos(fromEntity.getEyePosition(tickDelta));
        BlockPos blockPosOfEnd = new BlockPos(toEntity.getEyePosition(tickDelta));
        int blockLightLevelOfStart = fromEntity.level.getBrightness(LightLayer.BLOCK, blockPosOfStart);
        int blockLightLevelOfEnd = toEntity.level.getBrightness(LightLayer.BLOCK, blockPosOfEnd);
        int skylightLevelOfStart = fromEntity.level.getBrightness(LightLayer.SKY, blockPosOfStart);
        int skylightLevelOfEnd = fromEntity.level.getBrightness(LightLayer.SKY, blockPosOfEnd);

        Vec3 startPos = srcPos.add(offset.x(), 0, offset.z());
        Vec3 endPos = dstPos.add(-offset.x(), 0, -offset.z());
        Vector3f chainVec = new Vector3f((float) (endPos.x - startPos.x), (float) (endPos.y - startPos.y), (float) (endPos.z - startPos.z));

        float angleY = -(float) Math.atan2(chainVec.z(), chainVec.x());
        matrices.mulPose(Quaternion.fromXYZ(0, angleY, 0));

        if (toEntity instanceof HangingEntity) {
            ChainRenderer.BakeKey key = new ChainRenderer.BakeKey(fromEntity.position(), toEntity.position());
            chainRenderer.renderBaked(buffer, matrices, key, chainVec, blockLightLevelOfStart, blockLightLevelOfEnd, skylightLevelOfStart, skylightLevelOfEnd);
        } else {
            chainRenderer.render(buffer, matrices, chainVec, blockLightLevelOfStart, blockLightLevelOfEnd, skylightLevelOfStart, skylightLevelOfEnd);
        }

        matrices.popPose();
    }

    public ChainRenderer getChainRenderer() {
        return chainRenderer;
    }
}
