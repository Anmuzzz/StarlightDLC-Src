package com.isusdlc.utility.particle;

import com.mojang.blaze3d.systems.RenderSystem;
import java.util.List;
import com.isusdlc.elegant;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.util.Identifier;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.render.VertexFormat.DrawMode;
import net.minecraft.client.util.math.MatrixStack.Entry;
import org.joml.Matrix4f;

public class ParticleUtil {
   public static void renderParticle(MatrixStack matrix, ParticleUtil.Particle particle, float x, float y, float z, float pos, int color, int alpha) {
      matrix.push();
      setupOrientationMatrix(matrix, x, y, z);
      matrix.multiply(particle.mc.gameRenderer.getCamera().getRotation());
      Identifier texture = particle.type().texture();
      RenderSystem.enableBlend();
      RenderSystem.defaultBlendFunc();
      RenderSystem.disableCull();
      RenderSystem.enableDepthTest();
      RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
      RenderSystem.setShaderTexture(0, texture);
      Entry entry = matrix.peek();
      Matrix4f matrix4f = entry.getPositionMatrix();
      int r = color >> 16 & 0xFF;
      int g = color >> 8 & 0xFF;
      int b = color & 0xFF;
      BufferBuilder builder = RenderSystem.renderThreadTesselator().begin(DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
      builder.vertex(matrix4f, -pos, -pos, 0.0F).texture(0.0F, 0.0F).color(r, g, b, alpha);
      builder.vertex(matrix4f, -pos, pos, 0.0F).texture(0.0F, 1.0F).color(r, g, b, alpha);
      builder.vertex(matrix4f, pos, pos, 0.0F).texture(1.0F, 1.0F).color(r, g, b, alpha);
      builder.vertex(matrix4f, pos, -pos, 0.0F).texture(1.0F, 0.0F).color(r, g, b, alpha);
      BufferRenderer.drawWithGlobalProgram(builder.end());
      if (particle.type == ParticleUtil.ParticleType.BLOOM) {
         builder = RenderSystem.renderThreadTesselator().begin(DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
         float halfPos = pos / 2.0F;
         builder.vertex(matrix4f, -halfPos, -halfPos, 0.0F).texture(0.0F, 0.0F).color(r, g, b, alpha);
         builder.vertex(matrix4f, -halfPos, halfPos, 0.0F).texture(0.0F, 1.0F).color(r, g, b, alpha);
         builder.vertex(matrix4f, halfPos, halfPos, 0.0F).texture(1.0F, 1.0F).color(r, g, b, alpha);
         builder.vertex(matrix4f, halfPos, -halfPos, 0.0F).texture(1.0F, 0.0F).color(r, g, b, alpha);
         BufferRenderer.drawWithGlobalProgram(builder.end());
      }

      RenderSystem.setShaderTexture(0, 0);
      RenderSystem.disableDepthTest();
      RenderSystem.enableCull();
      RenderSystem.disableBlend();
      matrix.pop();
   }

   public static void renderParticles(
      MatrixStack matrix, Vec3d cameraPos, List<ParticleUtil.Particle> particles, long fadeInTime, long fadeOutTime, double deltaTime
   ) {
      if (!particles.isEmpty()) {
         matrix.push();

         for (ParticleUtil.Particle particle : particles) {
            particle.update(true, deltaTime);
            boolean notFinishedFadeIn = !particle.time().finished(fadeInTime);
            boolean finishedFadeOut = particle.time().finished(fadeOutTime);
            if (notFinishedFadeIn) {
               particle.animation().run(1.0, 0.4, ParticleEasings.QUAD_OUT, true);
            } else if (finishedFadeOut) {
               particle.animation().run(0.0, 0.4, ParticleEasings.QUAD_OUT, true);
            }

            if (particle.animation.isAlive()) {
               particle.animation.update();
            }

            float animValue = particle.animation.get();
            int alpha = (int)(animValue * 255.0F);
            if (alpha > 0) {
               Vec3d v = particle.position();
               int color = replaceAlpha(particle.color(), alpha);
               renderParticle(matrix, particle, (float)v.x, (float)v.y, (float)v.z, particle.size, color, alpha);
            }
         }

         matrix.pop();
      }
   }

   private static void setupOrientationMatrix(MatrixStack matrix, float x, float y, float z) {
      MinecraftClient mc = MinecraftClient.getInstance();
      Vec3d renderPos = mc.getEntityRenderDispatcher().camera.getPos();
      matrix.translate(x - renderPos.x, y - renderPos.y, z - renderPos.z);
   }

   private static int replaceAlpha(int color, int alpha) {
      int r = color >> 16 & 0xFF;
      int g = color >> 8 & 0xFF;
      int b = color & 0xFF;
      return alpha << 24 | r << 16 | g << 8 | b;
   }

   public static class Particle {
      private MinecraftClient mc;
      private Box box;
      private final ParticleUtil.ParticleType type;
      private Vec3d position;
      private Vec3d velocity;
      private final int index;
      private final int rotate;
      private final int color;
      private final float size;
      private static final double BASE_VELOCITY = 0.05;
      private final double speedMultiplier;
      private final ParticleStopWatch time = new ParticleStopWatch();
      private final ParticleAnimation animation = new ParticleAnimation();

      public Particle(
         MinecraftClient mc,
         ParticleUtil.ParticleType type,
         Vec3d position,
         Vec3d velocity,
         int index,
         int rotate,
         int color,
         float size,
         double speedMultiplier
      ) {
         this.mc = mc;
         double halfSize = size / 2.0;
         this.box = new Box(
            new Vec3d(position.x - halfSize, position.y - halfSize, position.z - halfSize),
            new Vec3d(position.x + halfSize, position.y + halfSize, position.z + halfSize)
         );
         this.type = type;
         this.position = position;
         this.velocity = velocity.multiply(0.05);
         this.index = index;
         this.rotate = rotate;
         this.color = color;
         this.size = size;
         this.speedMultiplier = speedMultiplier;
         this.time.reset();
      }

      public Box box() {
         return this.box;
      }

      public ParticleUtil.ParticleType type() {
         return this.type;
      }

      public Vec3d position() {
         return this.position;
      }

      public Vec3d velocity() {
         return this.velocity;
      }

      public int index() {
         return this.index;
      }

      public int rotate() {
         return this.rotate;
      }

      public int color() {
         return this.color;
      }

      public float size() {
         return this.size;
      }

      public double speedMultiplier() {
         return this.speedMultiplier;
      }

      public ParticleStopWatch time() {
         return this.time;
      }

      public ParticleAnimation animation() {
         return this.animation;
      }

      public void update(boolean physic, double deltaTime) {
         if (physic && this.mc.world != null) {
            double velMagSq = this.velocity.x * this.velocity.x
               + this.velocity.y * this.velocity.y
               + this.velocity.z * this.velocity.z;
            if (velMagSq > 1.0E-4) {
               if (this.isBlockSolid(this.position.x, this.position.y, this.position.z + this.velocity.z)) {
                  this.velocity = new Vec3d(this.velocity.x * 0.8F, this.velocity.y * 0.8F, this.velocity.z * -0.5);
               }

               if (this.isBlockSolid(this.position.x, this.position.y + this.velocity.y, this.position.z)) {
                  this.velocity = new Vec3d(this.velocity.x * 0.8F, this.velocity.y * -0.5, this.velocity.z * 0.8F);
               }

               if (this.isBlockSolid(this.position.x + this.velocity.x, this.position.y, this.position.z)) {
                  this.velocity = new Vec3d(this.velocity.x * -0.5, this.velocity.y * 0.8F, this.velocity.z * 0.8F);
               }
            }

            double friction = Math.pow(0.999, deltaTime * 60.0);
            this.velocity = this.velocity.multiply(friction).subtract(0.0, 2.0E-5, 0.0);
         }

         double deltaMultiplier = deltaTime * 60.0 * this.speedMultiplier;
         this.position = new Vec3d(
            this.position.x + this.velocity.x * deltaMultiplier,
            this.position.y + this.velocity.y * deltaMultiplier,
            this.position.z + this.velocity.z * deltaMultiplier
         );
         double halfSize = this.size / 2.0;
         this.box = new Box(
            new Vec3d(this.position.x - halfSize, this.position.y - halfSize, this.position.z - halfSize),
            new Vec3d(this.position.x + halfSize, this.position.y + halfSize, this.position.z + halfSize)
         );
      }

      private boolean isBlockSolid(double x, double y, double z) {
         BlockPos pos = BlockPos.ofFloored(x, y, z);
         return this.mc.world.getBlockState(pos).isFullCube(this.mc.world, pos);
      }
   }

   public static enum ParticleType {
      HEART("heart", false),
      STAR("star", false),
      SNOW("snowflake", false),
      BLOOM("firefly", false),
      DOLLAR("dollar", false),
      TRIANGLE("triangle", false),
      SAKURA("sakura", false),
      GEMINI("genshin", false),
      SIMS("rhombus", false);

      private final Identifier texture;
      private final boolean rotatable;

      private ParticleType(String name, boolean rotatable) {
         this.texture = elegant.id("textures/world/particles/" + name + ".png");
         this.rotatable = rotatable;
      }

      public Identifier texture() {
         return this.texture;
      }

      public boolean rotatable() {
         return this.rotatable;
      }
   }
}
