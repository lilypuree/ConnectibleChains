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

import com.lilypuree.connectiblechains.ConnectibleChains;
import com.lilypuree.connectiblechains.chain.ChainLink;
import com.lilypuree.connectiblechains.client.ClientInitializer;
import com.lilypuree.connectiblechains.client.render.entity.model.ChainKnotEntityModel;
import com.lilypuree.connectiblechains.client.render.entity.texture.ChainTextureManager;
import com.lilypuree.connectiblechains.entity.ChainKnotEntity;
import com.lilypuree.connectiblechains.util.Helper;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.HangingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.List;

/**
 * <p>This class renders the chain you see in game. The block around the fence and the chain.
 * You could use this code to start to understand how this is done.
 * I tried to make it as easy to understand as possible, mainly for myself, since the MobEntityRenderer has a lot of
 * unclear code and shortcuts made.</p>
 *
 *
 * @author legoatoomm, Qendolin
 * @see net.minecraft.client.renderer.entity.LeashKnotRenderer
 * @see net.minecraft.client.renderer.entity.MobRenderer
 */
@OnlyIn(Dist.CLIENT)
public class ChainKnotEntityRenderer extends EntityRenderer<ChainKnotEntity> {
    private final ChainKnotEntityModel<ChainKnotEntity> model;
    private final ChainRenderer chainRenderer = new ChainRenderer();

    public ChainKnotEntityRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.model = new ChainKnotEntityModel<>(context.bakeLayer(ClientInitializer.CHAIN_KNOT));
    }

    public ChainRenderer getChainRenderer() {
        return chainRenderer;
    }

    @Override
    public boolean shouldRender(ChainKnotEntity entity, Frustum frustum, double x, double y, double z) {
        if (entity.noCulling) return true;
        for (ChainLink link : entity.getLinks()) {
            if (link.getPrimary() != entity) continue;
            if (link.getSecondary() instanceof Player) return true;
            else if (link.getSecondary().shouldRender(x, y, z)) return true;
        }
        return super.shouldRender(entity, frustum, x, y, z);
    }

    @Override
    public void render(ChainKnotEntity chainKnotEntity, float yaw, float tickDelta, PoseStack poseStack, MultiBufferSource bufferSource, int light) {
        // Render the knot
        if (chainKnotEntity.shouldRenderKnot()) {
            poseStack.pushPose();
            Vec3 leashOffset = chainKnotEntity.getRopeHoldPosition(tickDelta).subtract(chainKnotEntity.getPosition(tickDelta));
            poseStack.translate(leashOffset.x, leashOffset.y + 6.5 / 16f, leashOffset.z);
            // The model is 6 px wide, but it should be rendered at 5px
            poseStack.scale(5 / 6f, 1, 5 / 6f);
            VertexConsumer vertexConsumer = bufferSource.getBuffer(this.model.renderType(getKnotTexture(chainKnotEntity.getChainItemSource())));
            this.model.renderToBuffer(poseStack, vertexConsumer, light, OverlayTexture.NO_OVERLAY);
            poseStack.popPose();
        }

        // Render the links
        List<ChainLink> links = chainKnotEntity.getLinks();
        for (ChainLink link : links) {
            if (link.getPrimary() != chainKnotEntity || link.isDead()) continue;
            this.renderChainLink(link, tickDelta, poseStack, bufferSource);
            if (ConnectibleChains.runtimeConfig.doDebugDraw()) {
                this.drawDebugVector(poseStack, chainKnotEntity, link.getSecondary(), bufferSource.getBuffer(RenderType.LINES));
            }
        }

        if (ConnectibleChains.runtimeConfig.doDebugDraw()) {
            poseStack.pushPose();
            // F stands for "from", T for "to"

            Component holdingCount = Component.literal("F: " + chainKnotEntity.getLinks().stream()
                    .filter(l -> l.getPrimary() == chainKnotEntity).count());
            Component heldCount = Component.literal("T: " + chainKnotEntity.getLinks().stream()
                    .filter(l -> l.getSecondary() == chainKnotEntity).count());
            poseStack.translate(0, 0.25, 0);
            this.renderNameTag(chainKnotEntity, holdingCount, poseStack, bufferSource, light, tickDelta);
            poseStack.translate(0, 0.25, 0);
            this.renderNameTag(chainKnotEntity, heldCount, poseStack, bufferSource, light, tickDelta);
            poseStack.popPose();
        }
        super.render(chainKnotEntity, yaw, tickDelta, poseStack, bufferSource, light);
    }
    private ChainTextureManager getTextureManager() {
        return ClientInitializer.getInstance().getChainTextureManager();
    }

    private ResourceLocation getKnotTexture(Item item) {
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
        return getTextureManager().getKnotTexture(id);
    }

    private ResourceLocation getChainTexture(Item item) {
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
        return getTextureManager().getChainTexture(id);
    }

    /**
     * If I am honest I do not really know what is happening here most of the time, most of the code was 'inspired' by
     * the {@link net.minecraft.client.renderer.entity.LeashKnotRenderer}.
     * Many variables therefore have simple names. I tried my best to comment and explain what everything does.
     *
     * @param link                   A link that provides the positions and type
     * @param tickDelta              Delta tick
     * @param stack               The render matrix stack.
     * @param bufferSource The VertexConsumerProvider, whatever it does.
     */
    private void renderChainLink(ChainLink link, float tickDelta, PoseStack stack, MultiBufferSource bufferSource) {
        ChainKnotEntity fromEntity = link.getPrimary();
        Entity toEntity = link.getSecondary();
        stack.pushPose();

        // Don't have to lerp knot position as it can't move
        // Also lerping the position of an entity that was just created
        // causes visual bugs because the position is lerped from 0/0/0.
        Vec3 srcPos = fromEntity.position().add(fromEntity.getLeashOffset());
        Vec3 dstPos;

        if (toEntity instanceof HangingEntity) {
            dstPos = toEntity.position().add(toEntity.getLeashOffset(tickDelta));
        } else {
            dstPos = toEntity.getRopeHoldPosition(tickDelta);
        }

        // The leash pos offset
        Vec3 leashOffset = fromEntity.getLeashOffset();
        stack.translate(leashOffset.x, leashOffset.y, leashOffset.z);

        Item sourceItem = link.getSourceItem();
        // Some further performance improvements can be made here:
        // Create a rendering layer that:
        // - does not have normals
        // - does not have an overlay
        // - does not have vertex color
        // - uses a tri strip instead of quads
        RenderType entityCutout = RenderType.entityCutoutNoCull(getChainTexture(sourceItem));

        VertexConsumer buffer = bufferSource.getBuffer(entityCutout);
        if (ConnectibleChains.runtimeConfig.doDebugDraw()) {
            buffer = bufferSource.getBuffer(RenderType.lines());
        }

        Vec3 offset = Helper.getChainOffset(srcPos, dstPos);
        stack.translate(offset.x(), 0, offset.z());

        // Now we gather light information for the chain. Since the chain is lighter if there is more light.
        BlockPos blockPosOfStart = BlockPos.containing(fromEntity.getEyePosition(tickDelta));
        BlockPos blockPosOfEnd = BlockPos.containing(toEntity.getEyePosition(tickDelta));
        int blockLightLevelOfStart = fromEntity.level().getBrightness(LightLayer.BLOCK, blockPosOfStart);
        int blockLightLevelOfEnd = toEntity.level().getBrightness(LightLayer.BLOCK, blockPosOfEnd);
        int skylightLevelOfStart = fromEntity.level().getBrightness(LightLayer.SKY, blockPosOfStart);
        int skylightLevelOfEnd = fromEntity.level().getBrightness(LightLayer.SKY, blockPosOfEnd);

        Vec3 startPos = srcPos.add(offset.x(), 0, offset.z());
        Vec3 endPos = dstPos.add(-offset.x(), 0, -offset.z());
        Vector3f chainVec = new Vector3f((float) (endPos.x - startPos.x), (float) (endPos.y - startPos.y), (float) (endPos.z - startPos.z));

        float angleY = -(float) Math.atan2(chainVec.z(), chainVec.x());

        stack.mulPose(new Quaternionf().rotateXYZ(0, angleY, 0));

        if (toEntity instanceof HangingEntity) {
            ChainRenderer.BakeKey key = new ChainRenderer.BakeKey(fromEntity.position(), toEntity.position());
            chainRenderer.renderBaked(buffer, stack, key, chainVec, blockLightLevelOfStart, blockLightLevelOfEnd, skylightLevelOfStart, skylightLevelOfEnd);
        } else {
            chainRenderer.render(buffer, stack, chainVec, blockLightLevelOfStart, blockLightLevelOfEnd, skylightLevelOfStart, skylightLevelOfEnd);
        }

        stack.popPose();
    }



    /**
     * Draws a line fromEntity - toEntity, from green to red.
     */
    private void drawDebugVector(PoseStack stack, Entity fromEntity, Entity toEntity, VertexConsumer buffer) {
        if (toEntity == null) return;
        Matrix4f modelMat = stack.last().pose();
        Vec3 vec = toEntity.position().subtract(fromEntity.position());
        Vec3 normal = vec.normalize();
        buffer.addVertex(modelMat, 0, 0, 0)
                .setColor(0, 255, 0, 255)
                .setNormal((float) normal.x, (float) normal.y, (float) normal.z);
        buffer.addVertex(modelMat, (float) vec.x, (float) vec.y, (float) vec.z)
                .setColor(255, 0, 0, 255)
                .setNormal((float) normal.x, (float) normal.y, (float) normal.z);
    }

    @Override
    public ResourceLocation getTextureLocation(ChainKnotEntity entity) {
        return getKnotTexture(entity.getChainItemSource());
    }


}
