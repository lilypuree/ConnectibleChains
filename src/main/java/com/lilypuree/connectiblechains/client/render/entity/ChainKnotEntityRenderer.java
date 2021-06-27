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

package com.lilypuree.connectiblechains.client.render.entity;

import com.lilypuree.connectiblechains.ConnectibleChains;
import com.lilypuree.connectiblechains.client.render.entity.model.ChainKnotEntityModel;
import com.lilypuree.connectiblechains.entity.ChainKnotEntity;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.ClippingHelper;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererManager;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.HangingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Matrix4f;
import net.minecraft.util.math.vector.Vector3f;
import net.minecraft.world.LightType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static com.lilypuree.connectiblechains.util.Helper.drip;

@OnlyIn(Dist.CLIENT)
public class ChainKnotEntityRenderer extends EntityRenderer<ChainKnotEntity> {
    private static final ResourceLocation TEXTURE = new ResourceLocation(ConnectibleChains.MODID, "textures/entity/chain_knot.png");
    private final ChainKnotEntityModel<ChainKnotEntity> model = new ChainKnotEntityModel<>();

    public ChainKnotEntityRenderer(EntityRendererManager rendererManager) {
        super(rendererManager);
    }

    @Override
    public void render(ChainKnotEntity chainKnotEntity, float f, float g, MatrixStack matrixStack, IRenderTypeBuffer renderTypeBuffer, int i) {
        matrixStack.pushPose();
        matrixStack.scale(-0.9F, -0.9F, 0.9F);
        this.model.setupAnim(chainKnotEntity, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F);
        IVertexBuilder vertexConsumer = renderTypeBuffer.getBuffer(this.model.renderType(TEXTURE));
        this.model.renderToBuffer(matrixStack, vertexConsumer, i, OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);
        matrixStack.popPose();
        ArrayList<Entity> entities = chainKnotEntity.getHoldingEntities();
        for (Entity entity : entities) {
            this.createChainLine(chainKnotEntity, g, matrixStack, renderTypeBuffer, entity);
        }
        super.render(chainKnotEntity, f, g, matrixStack, renderTypeBuffer, i);
    }

    @Override
    public boolean shouldRender(ChainKnotEntity entity, ClippingHelper clippingHelper, double x, double y, double z) {
        boolean should = entity.getHoldingEntities().stream().anyMatch(entity1 -> {
            if (entity1 instanceof ChainKnotEntity)
                return super.shouldRender((ChainKnotEntity) entity1, clippingHelper, x, y, z);
            else return entity1 instanceof PlayerEntity;
        });
        return super.shouldRender(entity, clippingHelper, x, y, z) || should;
    }

    @Override
    public ResourceLocation getTextureLocation(ChainKnotEntity entity) {
        return TEXTURE;
    }

    /**
     * If I am honest I do not really know what is happening here most of the time, most of the code was 'inspired' by
     * the {@link net.minecraft.client.renderer.entity.LeashKnotRenderer}.
     * Many variables therefore have simple names. I tried my best to comment and explain what everything does.
     *
     * @param fromEntity             The origin Entity
     * @param tickDelta              Delta tick
     * @param matrices               The render matrix stack.
     * @param renderTypeBuffer The IRenderTypeBuffer, whatever it does.
     * @param toEntity               The entity that we connect the chain to, this can be a {@link PlayerEntity} or a {@link ChainKnotEntity}.
     */
    private void createChainLine(ChainKnotEntity fromEntity, float tickDelta, MatrixStack matrices, IRenderTypeBuffer renderTypeBuffer, Entity toEntity) {
        if (toEntity == null) return;

        matrices.pushPose();
        // Some math that has to do with the direction and yaw of the entity to know where to start and end.
        double d = MathHelper.lerp(tickDelta * 0.5F, toEntity.yRot, toEntity.yRotO) * 0.017453292F;
        double e = MathHelper.lerp(tickDelta * 0.5F, toEntity.xRot, toEntity.xRotO) * 0.017453292F;
        double g = Math.cos(d);
        double h = Math.sin(d);
        double i = Math.sin(e);

        // Now we have to know whether we connect to a player or a chain to define the start and end points of the chain.
        float lerpDistanceZ;
        float lerpDistanceX;
        float lerpDistanceY;
        if (toEntity instanceof HangingEntity) {
            // If the chain is connected to another chain
            double toLerpX = MathHelper.lerp(tickDelta, toEntity.xOld, toEntity.getX());
            double toLerpY = MathHelper.lerp(tickDelta, toEntity.yOld, toEntity.getY());
            double toLerpZ = MathHelper.lerp(tickDelta, toEntity.zOld, toEntity.getZ());
            double fromLerpX = MathHelper.lerp(tickDelta, fromEntity.xOld, fromEntity.getX());
            double fromLerpY = MathHelper.lerp(tickDelta, fromEntity.yOld, fromEntity.getY());
            double fromLerpZ = MathHelper.lerp(tickDelta, fromEntity.zOld, fromEntity.getZ());
            lerpDistanceX = (float) (toLerpX - fromLerpX);
            lerpDistanceY = (float) (toLerpY - fromLerpY);
            lerpDistanceZ = (float) (toLerpZ - fromLerpZ);
        } else {
            // If the chain is connected to a player.
            double j = Math.cos(e);
            double k = MathHelper.lerp(tickDelta, toEntity.xOld, toEntity.getX()) - g * 0.7D - h * 0.5D * j;
            double l = MathHelper.lerp(tickDelta, toEntity.yOld
                    + (double) toEntity.getEyeHeight() * 0.7D, toEntity.getY()
                    + (double) toEntity.getEyeHeight() * 0.7D) - i * 0.5D - 0.5D;
            double m = MathHelper.lerp(tickDelta, toEntity.zOld, toEntity.getZ()) - h * 0.7D + g * 0.5D * j;
            double o = MathHelper.lerp(tickDelta, fromEntity.xOld, fromEntity.getX());
            double p = MathHelper.lerp(tickDelta, fromEntity.yOld, fromEntity.getY()) + 0.3F;
            double q = MathHelper.lerp(tickDelta, fromEntity.zOld, fromEntity.getZ());
            lerpDistanceX = (float) (k - o);
            lerpDistanceY = (float) (l - p);
            lerpDistanceZ = (float) (m - q);
        }
        matrices.translate(0, 0.3F, 0);// TODO is this needed?

        IVertexBuilder vertexBuilder = renderTypeBuffer.getBuffer(RenderType.leash());
        //Create offset based on the location. Example that a line that does not travel in the x then the xOffset will be 0.
        float v = MathHelper.fastInvSqrt(lerpDistanceX * lerpDistanceX + lerpDistanceZ * lerpDistanceZ) * 0.025F / 2.0F;
        float xOffset = lerpDistanceZ * v;
        float zOffset = lerpDistanceX * v;

        // Now we gather light information for the chain. Since the chain is lighter if there is more light.

        BlockPos blockPosOfStart = new BlockPos(fromEntity.getEyePosition(tickDelta));
        BlockPos blockPosOfEnd = new BlockPos(toEntity.getEyePosition(tickDelta));
        int blockLightLevelOfStart = fromEntity.level.getBrightness(LightType.BLOCK, blockPosOfStart);
        int blockLightLevelOfEnd = toEntity.level.getBrightness(LightType.BLOCK, blockPosOfEnd);
        int skylightLevelOfStart = fromEntity.level.getBrightness(LightType.SKY, blockPosOfStart);
        int skylightLevelOfEnd = fromEntity.level.getBrightness(LightType.SKY, blockPosOfEnd);


//        int y = this.getBlockLightLevel(fromEntity, blockPosOfStart);
//        int z = toEntity.isOnFire() ? 15 : toEntity.level.getBrightness(LightType.BLOCK, new BlockPos(toEntity.getEyePosition(tickDelta)));
//        int aa = fromEntity.level.getBrightness(LightType.SKY, blockPosOfStart);
//        int ab = fromEntity.level.getBrightness(LightType.SKY, new BlockPos(toEntity.getEyePosition(tickDelta)));
//
        float distance = toEntity.distanceTo(fromEntity);
        Matrix4f matrix4f = matrices.last().pose();

//        chainDrawer(distance, vertexBuilder, matrix4f, lerpDistanceX, lerpDistanceY, lerpDistanceZ, y, z, aa, ab, xOffset, zOffset);
        chainDrawer(distance, vertexBuilder, matrix4f, lerpDistanceX, lerpDistanceY, lerpDistanceZ, blockLightLevelOfStart, blockLightLevelOfEnd, skylightLevelOfStart, skylightLevelOfEnd, xOffset, zOffset);

        matrices.popPose();
    }

    /**
     * This method is the big drawer of the chain.
     */
    @SuppressWarnings("DuplicateExpressions")
    private static void chainDrawer(float distance, IVertexBuilder vertexBuilder, Matrix4f matrix4f,
                                    float lerpDistanceX, float lerpDistanceY, float lerpDistanceZ,
                                    int blockLightLevelOfStart, int blockLightLevelOfEnd,
                                    int skylightLevelOfStart, int skylightLevelOfEnd,
                                    float xOffset, float zOffset) {

        /*Can you see the chain here?*/
        List<Integer> topLineA, middleLineA, bottomLineA, topLineB, middleLineB, bottomLineB;
        topLineA    = Arrays.asList(   1, 2, 3,       6, 7, 8, 9,         12, 13, 14);
        middleLineA = Arrays.asList(   1,    3,       6,       9,         12,     14);
        bottomLineA = Arrays.asList(   1, 2, 3,       6, 7, 8, 9,         12, 13, 14);

        topLineB    = Arrays.asList(0, 1,    3, 4, 5, 6,       9, 10, 11, 12,     14, 15);
        middleLineB = Arrays.asList(   1,    3,       6,       9,         12,     14    );
        bottomLineB = Arrays.asList(0, 1,    3, 4, 5, 6,       9, 10, 11, 12,     14, 15);

        int length = (int) Math.floor(distance * 24); //This number specifies the number of pixels on the chain.

        // LightLevel Stuff
        float s = (float) skylightLevelOfEnd / (length - 1);
        int t = (int) MathHelper.lerp(s, (float) blockLightLevelOfStart, (float) blockLightLevelOfEnd);
        int u = (int) MathHelper.lerp(s, (float) skylightLevelOfStart, (float) skylightLevelOfEnd);
        int pack = LightTexture.pack(t, u);

        float[] rotate = rotator(lerpDistanceX, lerpDistanceY, lerpDistanceZ);

        for (int step = 0; step < length; ++step) {
            float startDrip = (float) drip(step, length);
            float endDrip = (float) drip(step + 1, length);
            float startStepFraction = ((float) step / (float) length);
            float endStepFraction = ((float) (step + 1) / (float) length);
            float v1 = (rotate[3] != 1.0F) ? 1.0F : -1.0F;
            float startRootX = lerpDistanceX * startStepFraction;
            float startRootY = lerpDistanceY * startStepFraction;
            float startRootZ = lerpDistanceZ * startStepFraction;
            float endRootX = lerpDistanceX * endStepFraction;
            float endRootY = lerpDistanceY * endStepFraction;
            float endRootZ = lerpDistanceZ * endStepFraction;
            float R, G, B;

            float rotate0 = rotate[0];
            float rotate1 = rotate[1];
            float rotate2 = rotate[2];

            // First Line
            float chainHeight = 0.0125F;
            if (topLineA.contains(step % 16)) {
                Vector3f startA, endA, startB, endB;
                startA = new Vector3f(
                        startRootX - rotate0 + xOffset,
                        startRootY + chainHeight + rotate1 + startDrip,
                        startRootZ - rotate2 - zOffset
                );
                startB = new Vector3f(
                        startRootX - (rotate0 - xOffset) * 3,
                        startRootY + chainHeight + rotate1 * 3 + startDrip,
                        startRootZ - (rotate2 + zOffset) * 3
                );
                endA = new Vector3f(
                        endRootX - rotate0 + xOffset,
                        endRootY + chainHeight + rotate1 + endDrip,
                        endRootZ - rotate2 - zOffset
                );
                endB = new Vector3f(
                        endRootX - (rotate0 - xOffset) * 3,
                        endRootY + chainHeight + rotate1 * 3 + endDrip,
                        endRootZ - (rotate2 + zOffset) * 3
                );
                R = 0.16F;
                G = 0.17F;
                B = 0.21F;
                renderPixel(startA, startB, endA, endB, vertexBuilder, matrix4f, pack, R, G, B);
            }
            if (middleLineA.contains(step % 16)) {
                Vector3f startA, endA, startB, endB;
                startA = new Vector3f(
                        startRootX + rotate0 + xOffset,
                        startRootY + chainHeight - rotate1 + startDrip,
                        startRootZ + rotate2 - zOffset
                );
                startB = new Vector3f(
                        startRootX - rotate0 - xOffset,
                        startRootY + chainHeight + rotate1 + startDrip,
                        startRootZ - rotate2 + zOffset
                );
                endA = new Vector3f(
                        endRootX + rotate0 + xOffset,
                        endRootY + chainHeight - rotate1 + endDrip,
                        endRootZ + rotate2 - zOffset
                );
                endB = new Vector3f(
                        endRootX - rotate0 - xOffset,
                        endRootY + chainHeight + rotate1 + endDrip,
                        endRootZ - rotate2 + zOffset
                );
                R = 0.12F * 0.7F;
                G = 0.12F * 0.7F;
                B = 0.17F * 0.7F;
                renderPixel(startA, startB, endA, endB, vertexBuilder, matrix4f, pack, R, G, B);
            }
            if (bottomLineA.contains(step % 16)) {
                Vector3f startA, endA, startB, endB;
                startA = new Vector3f(
                        startRootX + (rotate0 - xOffset) * 3,
                        startRootY + chainHeight - rotate1 * 3 + startDrip,
                        startRootZ + (rotate2 + zOffset) * 3
                );
                startB = new Vector3f(
                        startRootX + rotate0 - xOffset,
                        startRootY + chainHeight - rotate1 + startDrip,
                        startRootZ + rotate2 + zOffset
                );
                endA = new Vector3f(
                        endRootX + (rotate0 - xOffset) * 3,
                        endRootY + chainHeight - rotate1 * 3 + endDrip,
                        endRootZ + (rotate2 + zOffset) * 3
                );
                endB = new Vector3f(
                        endRootX + rotate0 - xOffset,
                        endRootY + chainHeight - rotate1 + endDrip,
                        endRootZ + rotate2 + zOffset
                );
                R = 0.16F;
                G = 0.17F;
                B = 0.21F;
                renderPixel(startA, startB, endA, endB, vertexBuilder, matrix4f, pack, R, G, B);
            }
            // Second Line
            if (topLineB.contains(step % 16)) {
                Vector3f startA, endA, startB, endB;
                startA = new Vector3f(
                        startRootX - (rotate0 * v1) - xOffset,
                        startRootY + chainHeight + rotate1 + startDrip,
                        startRootZ - rotate2 + zOffset
                );
                startB = new Vector3f(
                        startRootX - ((rotate0 * v1) + xOffset) * 3,
                        startRootY + chainHeight + rotate1 * 3 + startDrip,
                        startRootZ - (rotate2 - zOffset) * 3
                );
                endA = new Vector3f(
                        endRootX - (rotate0 * v1) - xOffset,
                        endRootY + chainHeight + rotate1 + endDrip,
                        endRootZ - rotate2 + zOffset
                );
                endB = new Vector3f(
                        endRootX - ((rotate0 * v1) + xOffset) * 3,
                        endRootY + chainHeight + rotate1 * 3 + endDrip,
                        endRootZ - (rotate2 - zOffset) * 3
                );
                R = 0.16F * 0.8F;
                G = 0.17F * 0.8F;
                B = 0.21F * 0.8F;
                renderPixel(startA, startB, endA, endB, vertexBuilder, matrix4f, pack, R, G, B);
            }
            if (middleLineB.contains(step % 16)) {
                Vector3f startA, endA, startB, endB;
                startA = new Vector3f(
                        startRootX + (rotate0 * v1) - xOffset,
                        startRootY + chainHeight - rotate1 + startDrip,
                        startRootZ + rotate2 + zOffset
                );
                startB = new Vector3f(
                        startRootX - (rotate0 * v1) + xOffset,
                        startRootY + chainHeight + rotate1 + startDrip,
                        startRootZ - rotate2 - zOffset
                );
                endA = new Vector3f(
                        endRootX + (rotate0 * v1) - xOffset,
                        endRootY + chainHeight - rotate1 + endDrip,
                        endRootZ + rotate2 + zOffset
                );
                endB = new Vector3f(
                        endRootX - (rotate0 * v1) + xOffset,
                        endRootY + chainHeight + rotate1 + endDrip,
                        endRootZ - rotate2 - zOffset
                );
                R = 0.12F;
                G = 0.12F;
                B = 0.17F;
                renderPixel(startA, startB, endA, endB, vertexBuilder, matrix4f, pack, R, G, B);
            }
            if (bottomLineB.contains(step % 16)) {
                Vector3f startA, endA, startB, endB;
                startA = new Vector3f(
                        startRootX + ((rotate0 * v1) + xOffset) * 3,
                        startRootY + chainHeight - rotate1 * 3 + startDrip,
                        startRootZ + (rotate2 - zOffset) * 3
                );
                startB = new Vector3f(
                        startRootX + (rotate0 * v1) + xOffset,
                        startRootY + chainHeight - rotate1 + startDrip,
                        startRootZ + rotate2 - zOffset
                );
                endA = new Vector3f(
                        endRootX + ((rotate0 * v1) + xOffset) * 3,
                        endRootY + chainHeight - rotate1 * 3 + endDrip,
                        endRootZ + (rotate2 - zOffset) * 3
                );
                endB = new Vector3f(
                        endRootX + (rotate0 * v1) + xOffset,
                        endRootY + chainHeight - rotate1 + endDrip,
                        endRootZ + rotate2 - zOffset
                );
                R = 0.16F * 0.8F;
                G = 0.17F * 0.8F;
                B = 0.21F * 0.8F;
                renderPixel(startA, startB, endA, endB, vertexBuilder, matrix4f, pack, R, G, B);
            }
        }
    }


    /**
     * Fancy math, it deals with the rotation of the x,y,z coordinates based on the direction of the chain. So that
     * in every direction the pixels look the same size.
     *
     */
    private static float[] rotator(double x, double y, double z) {
        double x2 = x * x;
        double z2 = z * z;
        double zx = Math.sqrt(x2 + z2);
        double arc1 = Math.atan2(y, zx);
        double arc2 = Math.atan2(x, z);
        double d = Math.sin(arc1) * 0.0125F;
        float y_new = (float) (Math.cos(arc1) * 0.0125F);
        float z_new = (float) (Math.cos(arc2) * d);
        float x_new = (float) (Math.sin(arc2) * d);
        float v = 0.0F;
        if (zx == 0.0F) {
            x_new = z_new;
            v = 1.0F;
        }
        return new float[]{x_new, y_new, z_new, v};
    }

    /**
     * Draw a pixel with 4 vector locations and the other information.
     */
    private static void renderPixel(Vector3f startA, Vector3f startB, Vector3f endA, Vector3f endB,
                                    IVertexBuilder vertexConsumer, Matrix4f matrix4f, int lightPack,
                                    float R, float G, float B) {
        vertexConsumer.vertex(matrix4f, startA.x(), startA.y(), startA.z()).color(R, G, B, 1.0F).uv2(lightPack).endVertex();
        vertexConsumer.vertex(matrix4f, startB.x(), startB.y(), startB.z()).color(R, G, B, 1.0F).uv2(lightPack).endVertex();

        vertexConsumer.vertex(matrix4f, endB.x(), endB.y(), endB.z()).color(R, G, B, 1.0F).uv2(lightPack).endVertex();
        vertexConsumer.vertex(matrix4f, endA.x(), endA.y(), endA.z()).color(R, G, B, 1.0F).uv2(lightPack).endVertex();
    }
}
