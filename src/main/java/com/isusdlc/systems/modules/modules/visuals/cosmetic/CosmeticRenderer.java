package com.isusdlc.systems.modules.modules.visuals.cosmetic;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.*;
import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;

public class CosmeticRenderer {
    private static final MinecraftClient mc = MinecraftClient.getInstance();
    private static final float S = 1.8f / 24f;

    public void render(MatrixStack matrices, VertexConsumerProvider consumers,
                       CosmeticModel model, int light, int overlay) {
        render(matrices, consumers, model, light, overlay, false, CosmeticRenderSpace.PLAYER_MODEL);
    }

    public void render(MatrixStack matrices, VertexConsumerProvider consumers,
                       CosmeticModel model, int light, int overlay, boolean flipY) {
        render(matrices, consumers, model, light, overlay, flipY, CosmeticRenderSpace.PLAYER_MODEL);
    }

    public void render(MatrixStack matrices, VertexConsumerProvider consumers,
                       CosmeticModel model, int light, int overlay, boolean flipY,
                       CosmeticRenderSpace space) {
        if (model == null) return;
        if (mc.world == null) return;

        model.loadTexture();
        if (model.getRenderRoots().isEmpty()) return;

        BufferBuilder buf = null;
        matrices.push();
        try {
            RenderSystem.enableBlend();
            RenderSystem.blendFuncSeparate(
                    GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA,
                    GL11.GL_ONE, GL11.GL_ONE_MINUS_SRC_ALPHA);
            RenderSystem.disableCull();
            RenderSystem.enableDepthTest();
            RenderSystem.depthMask(true);
            RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
            RenderSystem.setShaderColor(1f, 1f, 1f, 1f);

            buf = Tessellator.getInstance().begin(
                    VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);

            float sy = flipY ? -S : S;
            matrices.scale(S, sy, S);

            if (space == CosmeticRenderSpace.PLAYER_MODEL && mc.player != null) {
                float partialTicks = mc.getRenderTickCounter().getTickDelta(false);
                float bodyYaw = MathHelper.lerp(partialTicks, mc.player.prevBodyYaw, mc.player.bodyYaw);
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180f - bodyYaw));
            }

            int boundIndex = Integer.MIN_VALUE;
            for (CosmeticBone bone : model.getRenderRoots()) {
                boundIndex = renderBone(buf, matrices, model, bone, boundIndex);
            }

            BufferRenderer.drawWithGlobalProgram(buf.end());

            RenderSystem.enableCull();
            RenderSystem.disableBlend();
            RenderSystem.depthMask(true);
            RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        } catch (Exception ignored) {
            if (buf != null) {
                try {
                    buf.end();
                } catch (Exception suppressed) {
                }
            }
        } finally {
            matrices.pop();
        }
    }

    private int renderBone(BufferBuilder buf, MatrixStack matrices, CosmeticModel model,
                           CosmeticBone bone, int boundIndex) {
        if (model.shouldSkipBone(bone)) {
            return boundIndex;
        }
        matrices.push();
        applyBoneTransform(matrices, bone);

        for (CosmeticCube cube : bone.getCubes()) {
            boundIndex = renderCube(buf, matrices, model, cube, boundIndex);
        }

        for (CosmeticBone child : bone.getChildren()) {
            boundIndex = renderBone(buf, matrices, model, child, boundIndex);
        }

        matrices.pop();
        return boundIndex;
    }

    private static void applyBoneTransform(MatrixStack matrices, CosmeticBone bone) {
        float px = bone.getPivot().x;
        float py = bone.getPivot().y;
        float pz = bone.getPivot().z;

        float apx = bone.getAnimPosition().x;
        float apy = bone.getAnimPosition().y;
        float apz = bone.getAnimPosition().z;
        if (apx != 0f || apy != 0f || apz != 0f) {
            matrices.translate(apx, apy, apz);
        }

        float rx = bone.getDefaultRotation().x + bone.getAnimRotation().x;
        float ry = bone.getDefaultRotation().y + bone.getAnimRotation().y;
        float rz = bone.getDefaultRotation().z + bone.getAnimRotation().z;

        if (rx != 0f || ry != 0f || rz != 0f) {
            matrices.translate(px, py, pz);
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(rx));
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(ry));
            matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(rz));
            matrices.translate(-px, -py, -pz);
        }
    }

    private int renderCube(BufferBuilder buf, MatrixStack matrices, CosmeticModel model,
                           CosmeticCube cube, int boundIndex) {
        matrices.push();

        float ox = cube.getOrigin().x;
        float oy = cube.getOrigin().y;
        float oz = cube.getOrigin().z;
        float rx = cube.getRotation().x;
        float ry = cube.getRotation().y;
        float rz = cube.getRotation().z;

        if (rx != 0f || ry != 0f || rz != 0f) {
            matrices.translate(ox, oy, oz);
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(rx));
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(ry));
            matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(rz));
            matrices.translate(-ox, -oy, -oz);
        }

        float inf = cube.getInflate();
        float x1 = cube.getFrom().x - inf;
        float y1 = cube.getFrom().y - inf;
        float z1 = cube.getFrom().z - inf;
        float x2 = cube.getTo().x + inf;
        float y2 = cube.getTo().y + inf;
        float z2 = cube.getTo().z + inf;

        Matrix4f m = matrices.peek().getPositionMatrix();
        CosmeticCube.CubeFace[] faces = cube.getFaces();

        for (int side = 0; side < 6; side++) {
            CosmeticCube.CubeFace face = faces[side];
            if (face == null) continue;

            CosmeticTextureSlot slot = model.getTextureSlot(face.getTextureIndex());
            if (slot == null) continue;
            if (!slot.isLoaded()) {
                model.ensureTextureSlotLoaded(slot);
            }
            if (!slot.isLoaded()) continue;

            if (face.getTextureIndex() != boundIndex) {
                bindTexture(slot);
                boundIndex = face.getTextureIndex();
            }

            quadFace(buf, m, face, slot.getWidth(), slot.getHeight(),
                    x1, y1, z1, x2, y2, z2, side);
        }

        matrices.pop();
        return boundIndex;
    }

    private static void bindTexture(CosmeticTextureSlot slot) {
        Identifier id = slot.getId();
        RenderSystem.setShaderTexture(0, id);
        AbstractTexture boundTex = mc.getTextureManager().getTexture(id);
        if (boundTex != null) {
            boundTex.bindTexture();
            GlStateManager._texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
            GlStateManager._texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
            GlStateManager._texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL11.GL_REPEAT);
            GlStateManager._texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL11.GL_REPEAT);
        }
    }

    private void quadFace(BufferBuilder buf, Matrix4f m,
                          CosmeticCube.CubeFace face, float tw, float th,
                          float x1, float y1, float z1,
                          float x2, float y2, float z2,
                          int side) {
        float u1 = face.getU1();
        float v1 = face.getV1();
        float u2 = face.getU2();
        float v2 = face.getV2();

        switch (side) {
            case 0 -> quad(buf, m, tw, th,
                    x2, y2, z1, u1, v1,
                    x1, y2, z1, u2, v1,
                    x1, y1, z1, u2, v2,
                    x2, y1, z1, u1, v2);
            case 1 -> quad(buf, m, tw, th,
                    x2, y2, z2, u1, v1,
                    x2, y2, z1, u2, v1,
                    x2, y1, z1, u2, v2,
                    x2, y1, z2, u1, v2);
            case 2 -> quad(buf, m, tw, th,
                    x1, y2, z2, u1, v1,
                    x2, y2, z2, u2, v1,
                    x2, y1, z2, u2, v2,
                    x1, y1, z2, u1, v2);
            case 3 -> quad(buf, m, tw, th,
                    x1, y2, z1, u1, v1,
                    x1, y2, z2, u2, v1,
                    x1, y1, z2, u2, v2,
                    x1, y1, z1, u1, v2);
            case 4 -> quad(buf, m, tw, th,
                    x1, y2, z2, u1, v1,
                    x2, y2, z2, u2, v1,
                    x2, y2, z1, u2, v2,
                    x1, y2, z1, u1, v2);
            case 5 -> quad(buf, m, tw, th,
                    x1, y1, z1, u1, v1,
                    x2, y1, z1, u2, v1,
                    x2, y1, z2, u2, v2,
                    x1, y1, z2, u1, v2);
            default -> {
            }
        }
    }

    private void quad(BufferBuilder buf, Matrix4f m, float tw, float th,
                      float x1, float y1, float z1, float pu1, float pv1,
                      float x2, float y2, float z2, float pu2, float pv2,
                      float x3, float y3, float z3, float pu3, float pv3,
                      float x4, float y4, float z4, float pu4, float pv4) {
        float u1 = pu1 / tw;
        float v1 = pv1 / th;
        float u2 = pu2 / tw;
        float v2 = pv2 / th;
        float u3 = pu3 / tw;
        float v3 = pv3 / th;
        float u4 = pu4 / tw;
        float v4 = pv4 / th;

        buf.vertex(m, x1, y1, z1).texture(u1, v1).color(1f, 1f, 1f, 1f);
        buf.vertex(m, x2, y2, z2).texture(u2, v2).color(1f, 1f, 1f, 1f);
        buf.vertex(m, x3, y3, z3).texture(u3, v3).color(1f, 1f, 1f, 1f);
        buf.vertex(m, x4, y4, z4).texture(u4, v4).color(1f, 1f, 1f, 1f);
    }
}
