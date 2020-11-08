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
import net.minecraft.world.LightType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@OnlyIn(Dist.CLIENT)
public class ChainKnotEntityRenderer extends EntityRenderer<ChainKnotEntity> {
    private static final ResourceLocation TEXTURE = new ResourceLocation(ConnectibleChains.MODID, "textures/entity/chain_knot.png");
    private final ChainKnotEntityModel<ChainKnotEntity> model = new ChainKnotEntityModel<>();

    public ChainKnotEntityRenderer(EntityRendererManager rendererManager) {
        super(rendererManager);
    }

    @Override
    public void render(ChainKnotEntity chainKnotEntity, float f, float g, MatrixStack matrixStack, IRenderTypeBuffer renderTypeBuffer, int i) {
        super.render(chainKnotEntity, f, g, matrixStack, renderTypeBuffer, i);
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

    private void createChainLine(ChainKnotEntity fromEntity, float f, MatrixStack matrixStack, IRenderTypeBuffer renderTypeBuffer, Entity chainOrPlayerEntity) {
        if (chainOrPlayerEntity == null) return;

        matrixStack.pushPose();
        double d = MathHelper.lerp(f * 0.5F, chainOrPlayerEntity.yRot, chainOrPlayerEntity.yRotO) * 0.017453292F;
        double e = MathHelper.lerp(f * 0.5F, chainOrPlayerEntity.xRot, chainOrPlayerEntity.xRotO) * 0.017453292F;
        double g = Math.cos(d);
        double h = Math.sin(d);
        double i = Math.sin(e);
        float t;
        float r;
        float s;
        if (chainOrPlayerEntity instanceof HangingEntity) {
            g = 0.0D;
            h = 0.0D;
            double k = MathHelper.lerp(f, chainOrPlayerEntity.xOld, chainOrPlayerEntity.getX());
            double l = MathHelper.lerp(f, chainOrPlayerEntity.yOld, chainOrPlayerEntity.getY());
            double m = MathHelper.lerp(f, chainOrPlayerEntity.zOld, chainOrPlayerEntity.getZ());
            double o = MathHelper.lerp(f, fromEntity.xOld, fromEntity.getX());
            double p = MathHelper.lerp(f, fromEntity.yOld, fromEntity.getY());
            double q = MathHelper.lerp(f, fromEntity.zOld, fromEntity.getZ());
            matrixStack.translate(g, 0.3F, h);
            r = (float) (k - o);
            s = (float) (l - p);
            t = (float) (m - q);
        } else {
            double j = Math.cos(e);
            double k = MathHelper.lerp(f, chainOrPlayerEntity.xOld, chainOrPlayerEntity.getX()) - g * 0.7D - h * 0.5D * j;
            double l = MathHelper.lerp(f, chainOrPlayerEntity.yOld + (double) chainOrPlayerEntity.getEyeHeight() * 0.7D, chainOrPlayerEntity.getY() + (double) chainOrPlayerEntity.getEyeHeight() * 0.7D) - i * 0.5D - 0.5D;
            double m = MathHelper.lerp(f, chainOrPlayerEntity.zOld, chainOrPlayerEntity.getZ()) - h * 0.7D + g * 0.5D * j;
            double o = MathHelper.lerp(f, fromEntity.xOld, fromEntity.getX());
            double p = MathHelper.lerp(f, fromEntity.yOld, fromEntity.getY()) + 0.3F;
            double q = MathHelper.lerp(f, fromEntity.zOld, fromEntity.getZ());
            matrixStack.translate(0, 0.3F, 0);
            r = (float) (k - o);
            s = (float) (l - p);
            t = (float) (m - q);
        }
        IVertexBuilder vertexBuilder = renderTypeBuffer.getBuffer(RenderType.leash());
        Matrix4f matrix4f = matrixStack.last().pose();
        //Create offset based on the location. Example that a line that does not travel in the x then the xOffset will be 0.
        float v = MathHelper.fastInvSqrt(r * r + t * t) * 0.025F / 2.0F;
        float xOffset = t * v;
        float zOffset = r * v;
        BlockPos zzz = new BlockPos(fromEntity.getEyePosition(f));
        int y = this.getBlockLightLevel(fromEntity, zzz);
        int z = chainOrPlayerEntity.isOnFire() ? 15 : chainOrPlayerEntity.level.getBrightness(LightType.BLOCK, new BlockPos(chainOrPlayerEntity.getEyePosition(f)));
        int aa = fromEntity.level.getBrightness(LightType.SKY, zzz);
        int ab = fromEntity.level.getBrightness(LightType.SKY, new BlockPos(chainOrPlayerEntity.getEyePosition(f)));
        float distance = chainOrPlayerEntity.distanceTo(fromEntity);
        lineBuilder(distance, vertexBuilder, matrix4f, r, s, t, y, z, aa, ab, xOffset, zOffset);
        matrixStack.popPose();
    }

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

    private static void lineBuilder(float distance, IVertexBuilder vertexConsumer, Matrix4f matrix4f, float cordX, float cordY, float cordZ, int i, int j, int k, int l, float xOffset, float zOffset) {
        List<Integer> mPatternA = Arrays.asList(1, 3, 6, 9, 12, 14);
        List<Integer> stbPatternA = Arrays.asList(1, 12);
        List<Integer> ltbPatternA = Collections.singletonList(6);
        List<Integer> stbPatternB = Arrays.asList(0, 14);
        List<Integer> ltbPatternB = Arrays.asList(3, 9);

        int length = (int) Math.floor(distance * 24);
        for (int p = 0; p < length; ++p) {
            float s = (float) l / (length - 1);
            int t = (int) MathHelper.lerp(s, (float) i, (float) j);
            int u = (int) MathHelper.lerp(s, (float) k, (float) l);
            int pack = LightTexture.pack(t, u);
            if (mPatternA.contains(p % 16)) {
                middle(length, vertexConsumer, matrix4f, pack, cordX, cordY, cordZ, length, p, false, xOffset, zOffset, false);
                middle(length, vertexConsumer, matrix4f, pack, cordX, cordY, cordZ, length, p + 1, true, xOffset, zOffset, false);
                middle(length, vertexConsumer, matrix4f, pack, cordX, cordY, cordZ, length, p, false, xOffset, zOffset, true);
                middle(length, vertexConsumer, matrix4f, pack, cordX, cordY, cordZ, length, p + 1, true, xOffset, zOffset, true);
            }
            if (stbPatternA.contains(p % 16)) {
                for (int T = 0; T < 3; T++) {
                    top(length, vertexConsumer, matrix4f, pack, cordX, cordY, cordZ, length, p + T, false, xOffset, zOffset, false);
                    top(length, vertexConsumer, matrix4f, pack, cordX, cordY, cordZ, length, p + 1 + T, true, xOffset, zOffset, false);
                    bot(length, vertexConsumer, matrix4f, pack, cordX, cordY, cordZ, length, p + T, false, xOffset, zOffset, false);
                    bot(length, vertexConsumer, matrix4f, pack, cordX, cordY, cordZ, length, p + 1 + T, true, xOffset, zOffset, false);
                }
            }
            if (ltbPatternA.contains(p % 16)) {
                for (int T = 0; T < 4; T++) {
                    top(length, vertexConsumer, matrix4f, pack, cordX, cordY, cordZ, length, p + T, false, xOffset, zOffset, false);
                    top(length, vertexConsumer, matrix4f, pack, cordX, cordY, cordZ, length, p + 1 + T, true, xOffset, zOffset, false);
                    bot(length, vertexConsumer, matrix4f, pack, cordX, cordY, cordZ, length, p + T, false, xOffset, zOffset, false);
                    bot(length, vertexConsumer, matrix4f, pack, cordX, cordY, cordZ, length, p + 1 + T, true, xOffset, zOffset, false);
                }
            }
            if (stbPatternB.contains(p % 16)) {
                for (int T = 0; T < 2; T++) {
                    top(length, vertexConsumer, matrix4f, pack, cordX, cordY, cordZ, length, p + T, false, xOffset, zOffset, true);
                    top(length, vertexConsumer, matrix4f, pack, cordX, cordY, cordZ, length, p + 1 + T, true, xOffset, zOffset, true);
                    bot(length, vertexConsumer, matrix4f, pack, cordX, cordY, cordZ, length, p + T, false, xOffset, zOffset, true);
                    bot(length, vertexConsumer, matrix4f, pack, cordX, cordY, cordZ, length, p + 1 + T, true, xOffset, zOffset, true);
                }
            }
            if (ltbPatternB.contains(p % 16)) {
                for (int T = 0; T < 4; T++) {
                    top(length, vertexConsumer, matrix4f, pack, cordX, cordY, cordZ, length, p + T, false, xOffset, zOffset, true);
                    top(length, vertexConsumer, matrix4f, pack, cordX, cordY, cordZ, length, p + 1 + T, true, xOffset, zOffset, true);
                    bot(length, vertexConsumer, matrix4f, pack, cordX, cordY, cordZ, length, p + T, false, xOffset, zOffset, true);
                    bot(length, vertexConsumer, matrix4f, pack, cordX, cordY, cordZ, length, p + 1 + T, true, xOffset, zOffset, true);
                }
            }
        }
    }

    private static void middle(int V, IVertexBuilder vertexConsumer, Matrix4f matrix4f, int i, float cordX, float cordY,
                               float cordZ, int l, int step, boolean bl, float n, float o, boolean shift) {
        double drip = drip(step, V);
        float s = ((float) step / (float) l);
        float[] rotate = rotator(cordX, cordY, cordZ);
        float v1 = 1.0F;
        if (rotate[3] == 1.0F) {
            v1 *= -1;
        }
        float t = cordX * s;
        float u = cordY * s;
        float v = cordZ * s;
        float x1 = t + (rotate[0]) + n;
        float y1 = u + (float) 0.0125 - rotate[1];
        float z1 = v + (rotate[2]) - o;
        float x2 = t - (rotate[0]) - n;
        float y2 = u + (float) 0.0125 + rotate[1];
        float z2 = v - (rotate[2]) + o;
        float R = 0.12F * 0.7F;
        float G = 0.12F * 0.7F;
        float B = 0.17F * 0.7F;
        if (shift) {
            R = 0.12F;
            G = 0.12F;
            B = 0.17F;
            x1 = t + (rotate[0] * v1) - n;
            z1 = v + (rotate[2]) + o;
            x2 = t - (rotate[0] * v1) + n;
            z2 = v - (rotate[2]) - o;
        }
        y1 += drip;
        y2 += drip;
        renderPart(vertexConsumer, matrix4f, i, bl, R, G, B, x1, y1, z1, x2, y2, z2);
    }

    private static void renderPart(IVertexBuilder vertexConsumer, Matrix4f matrix4f, int i, boolean bl, float R, float G, float B, float x1, float y1, float z1, float x2, float y2, float z2) {
        if (bl) {
            vertexConsumer.vertex(matrix4f, x1, y1, z1).color(R, G, B, 1.0F).uv2(i).endVertex();
        }
        vertexConsumer.vertex(matrix4f, x2, y2, z2).color(R, G, B, 1.0F).uv2(i).endVertex();
        if (!bl) {
            vertexConsumer.vertex(matrix4f, x1, y1, z1).color(R, G, B, 1.0F).uv2(i).endVertex();
        }
    }

    private static float drip(int x, float V) {
        float c = 0.6F;
        float b = -c / V;
        float a = c / (V * V);
        return (a * (x * x) + b * x);
    }

    private static void top(int V, IVertexBuilder vertexConsumer, Matrix4f matrix4f, int i, float cordX, float cordY,
                            float cordZ, int l, int step, boolean bl, float n, float o, boolean shift) {
        double drip = drip(step, V);
        float s = ((float) step / (float) l);
        float[] rotate = rotator(cordX, cordY, cordZ);
        float v1 = 1.0F;
        if (rotate[3] == 1.0F) {
            v1 *= -1;
        }
        float t = cordX * s;
        float u = cordY * s;
        float v = cordZ * s;
        float x1 = t - (rotate[0]) + n;
        float y1 = u + (float) 0.0125 + rotate[1];
        float z1 = v - (rotate[2]) - o;
        float x2 = t - ((rotate[0]) - n) * 3;
        float y2 = u + (float) 0.0125 + rotate[1] * 3;
        float z2 = v - ((rotate[2]) + o) * 3;
        float R = 0.16F;
        float G = 0.17F;
        float B = 0.21F;
        if (shift) {
            R *= 0.8F;
            G *= 0.8F;
            B *= 0.8F;
            x1 = t - (rotate[0] * v1) - n;
            z1 = v - (rotate[2]) + o;
            x2 = t - ((rotate[0] * v1) + n) * 3;
            z2 = v - ((rotate[2]) - o) * 3;
        }
        y1 += drip;
        y2 += drip;
        renderPart(vertexConsumer, matrix4f, i, bl, R, G, B, x1, y1, z1, x2, y2, z2);
    }

    private static void bot(int V, IVertexBuilder vertexConsumer, Matrix4f matrix4f, int i, float cordX, float cordY,
                            float cordZ, int l, int step, boolean bl, float n, float o,
                            boolean shift) {
        double drip = drip(step, V);
        float s = ((float) step / (float) l);
        float[] rotate = rotator(cordX, cordY, cordZ);
        float v1 = 1.0F;
        if (rotate[3] == 1.0F) {
            v1 *= -1;
        }
        float t = cordX * s;
        float u = cordY * s;
        float v = cordZ * s;
        float x1 = t + ((rotate[0]) - n) * 3;
        float y1 = u + (float) 0.0125 - rotate[1] * 3;
        float z1 = v + ((rotate[2]) + o) * 3;
        float x2 = t + (rotate[0]) - n;
        float y2 = u + (float) 0.0125 - rotate[1];
        float z2 = v + (rotate[2]) + o;
        float R = 0.16F;
        float G = 0.17F;
        float B = 0.21F;
        if (shift) {
            R *= 0.8F;
            G *= 0.8F;
            B *= 0.8F;
            x1 = t + ((rotate[0] * v1) + n) * 3;
            z1 = v + ((rotate[2]) - o) * 3;
            x2 = t + (rotate[0] * v1) + n;
            z2 = v + (rotate[2]) - o;
        }
        y1 += drip;
        y2 += drip;
        renderPart(vertexConsumer, matrix4f, i, bl, R, G, B, x1, y1, z1, x2, y2, z2);
    }

}
