package com.isusdlc.systems.modules.modules.visuals;

import com.mojang.blaze3d.systems.RenderSystem;
import com.isusdlc.elegant;
import com.isusdlc.systems.event.EventListener;
import com.isusdlc.systems.event.impl.game.AttackEvent;
import com.isusdlc.systems.event.impl.render.Render3DEvent;
import com.isusdlc.systems.modules.api.ModuleCategory;
import com.isusdlc.systems.modules.api.ModuleInfo;
import com.isusdlc.systems.modules.impl.BaseModule;
import com.isusdlc.systems.setting.settings.ColorSetting;
import com.isusdlc.systems.setting.settings.SelectSetting;
import com.isusdlc.systems.setting.settings.SliderSetting;
import com.isusdlc.utility.colors.ColorRGBA;
import com.isusdlc.utility.colors.Colors;
import com.isusdlc.utility.render.Draw3DUtility;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.VertexFormat.DrawMode;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@ModuleInfo(
   name = "Hit FX",
   category = ModuleCategory.VISUALS,
   desc = "Красивые эффекты при ударе по цели"
)
public class HitFX extends BaseModule {
   private final SelectSetting effects = new SelectSetting(this, "Эффекты");
   private final SelectSetting.Value shockwave = new SelectSetting.Value(this.effects, "Ударная волна").select();
   private final SelectSetting.Value burst = new SelectSetting.Value(this.effects, "Взрыв").select();
   private final SelectSetting.Value flash = new SelectSetting.Value(this.effects, "Вспышка").select();
   private final SelectSetting.Value sparks = new SelectSetting.Value(this.effects, "Искры");
   private final ColorSetting color = new ColorSetting(this, "Цвет").color(Colors.ACCENT);
   private final SliderSetting sizeMultiplier = new SliderSetting(this, "Размер")
      .min(0.5F).max(3.0F).step(0.1F).currentValue(1.0F);
   private final SliderSetting duration = new SliderSetting(this, "Длительность")
      .min(200.0F).max(1500.0F).step(50.0F).currentValue(600.0F);

   private final List<Fx> active = new ArrayList<>();
   private final Random random = new Random();
   private final Random effectRandom = new Random();

   private static final class Fx {
      final LivingEntity target;
      final long start;
      final long seed;

      Fx(LivingEntity target, long start, long seed) {
         this.target = target;
         this.start = start;
         this.seed = seed;
      }
   }

   private final EventListener<AttackEvent> onAttack = event -> {
      if (mc.world == null || event.isCancelled()) return;
      if (event.getEntity() instanceof LivingEntity living && living != mc.player) {
         this.active.add(new Fx(living, System.currentTimeMillis(), this.random.nextLong()));
         if (this.active.size() > 32) {
            this.active.remove(0);
         }
      }
   };

   private final EventListener<Render3DEvent> onRender3D = event -> {
      if (this.active.isEmpty() || mc.world == null || mc.player == null) return;

      long now = System.currentTimeMillis();
      this.active.removeIf(fx -> now - fx.start > (long) this.duration.getCurrentValue() || !fx.target.isAlive());
      if (this.active.isEmpty()) return;

      boolean shock = this.shockwave.isSelected();
      boolean burst = this.burst.isSelected();
      boolean flash = this.flash.isSelected();
      boolean sparks = this.sparks.isSelected();
      if (!shock && !burst && !flash && !sparks) return;

      MatrixStack ms = event.getMatrices();
      Camera camera = mc.gameRenderer.getCamera();
      Vec3d camPos = camera.getPos();

      RenderSystem.enableBlend();
      RenderSystem.defaultBlendFunc();
      RenderSystem.disableDepthTest();
      RenderSystem.disableCull();
      RenderSystem.depthMask(false);
      RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);

      try {
         for (Fx fx : this.active) {
            float progress = MathHelper.clamp((float) (now - fx.start) / this.duration.getCurrentValue(), 0.0F, 1.0F);
            float alpha = 1.0F - progress;
            ColorRGBA color = this.color.getColor();
            int cr = (int) color.getRed();
            int cg = (int) color.getGreen();
            int cb = (int) color.getBlue();
            Vec3d relPos = fx.target.getLerpedPos(event.getTickDelta()).subtract(camPos);
            float originY = (float) relPos.y + fx.target.getHeight() * 0.6F;
            float sizeMul = this.sizeMultiplier.getCurrentValue();

            if (shock) {
               this.drawShockwave(ms, relPos, progress, alpha, sizeMul, cr, cg, cb);
            }

            if (sparks) {
               this.drawSparks(ms, relPos, originY, progress, alpha, fx.seed, sizeMul, cr, cg, cb);
            }

            if (burst) {
               this.drawBurst(ms, camera, relPos, originY, progress, alpha, fx.seed, sizeMul, cr, cg, cb);
            }

            if (flash) {
               this.drawFlash(ms, relPos, fx.target, progress, alpha, cr, cg, cb);
            }
         }
      } finally {
         RenderSystem.depthMask(true);
         RenderSystem.enableCull();
         RenderSystem.enableDepthTest();
         RenderSystem.disableBlend();
      }
   };

   /** Расширяющееся кольцо по земле — ударная волна. */
   private void drawShockwave(MatrixStack ms, Vec3d relPos, float progress, float alpha, float sizeMul, int cr, int cg, int cb) {
      float radius = (0.4F + 2.2F * this.easeOut(progress)) * sizeMul;
      int ringAlpha = (int) (alpha * 190.0F);
      int segments = 32;

      ms.push();
      ms.translate(relPos.x, relPos.y + 0.05F, relPos.z);
      RenderSystem.lineWidth(3.0F);
      Matrix4f matrix = ms.peek().getPositionMatrix();
      BufferBuilder buffer = RenderSystem.renderThreadTesselator().begin(DrawMode.LINE_STRIP, VertexFormats.POSITION_COLOR);
      for (int i = 0; i <= segments; i++) {
         double angle = Math.PI * 2.0 * i / segments;
         float wobble = (float) Math.sin(angle * 5.0 + progress * 12.0) * 0.04F * (1.0F - progress);
         buffer.vertex(matrix, (float) (Math.cos(angle) * radius), wobble, (float) (Math.sin(angle) * radius))
            .color(cr, cg, cb, ringAlpha);
      }
      BufferRenderer.drawWithGlobalProgram(buffer.end());
      ms.pop();
   }

   /** Радиальные искры из точки удара. */
   private void drawSparks(MatrixStack ms, Vec3d relPos, float originY, float progress, float alpha, long seed, float sizeMul, int cr, int cg, int cb) {
      Random rng = this.effectRandom;
      rng.setSeed(seed);
      int count = 8;
      float length = (1.1F - 0.9F * progress) * sizeMul;
      int sparkAlpha = (int) (alpha * 220.0F);
      if (sparkAlpha <= 2) return;

      ms.push();
      RenderSystem.lineWidth(1.8F);
      Matrix4f matrix = ms.peek().getPositionMatrix();
      BufferBuilder buffer = RenderSystem.renderThreadTesselator().begin(DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);
      for (int i = 0; i < count; i++) {
         double yaw = rng.nextDouble() * Math.PI * 2.0;
         double pitch = rng.nextDouble() * 1.2 - 0.35;
         float dirX = (float) (Math.cos(yaw) * Math.cos(pitch));
         float dirY = (float) Math.sin(pitch);
         float dirZ = (float) (Math.sin(yaw) * Math.cos(pitch));
         float from = length * 0.25F;
         buffer.vertex(matrix, (float) relPos.x + dirX * from, originY + dirY * from, (float) relPos.z + dirZ * from).color(cr, cg, cb, sparkAlpha);
         buffer.vertex(matrix, (float) relPos.x + dirX * length, originY + dirY * length, (float) relPos.z + dirZ * length)
            .color(cr, cg, cb, (int) (sparkAlpha * 0.25F));
      }
      BufferRenderer.drawWithGlobalProgram(buffer.end());
      ms.pop();
   }

   /** Разлетающиеся светящиеся частицы (bloom-билборды). */
   private void drawBurst(MatrixStack ms, Camera camera, Vec3d relPos, float originY, float progress, float alpha, long seed, float sizeMul, int cr, int cg, int cb) {
      Random rng = this.effectRandom;
      rng.setSeed(seed);
      int count = 10;
      float spread = (0.3F + 1.6F * this.easeOut(progress)) * sizeMul;
      int dotAlpha = (int) ((float) (alpha * alpha) * 230.0F);
      if (dotAlpha <= 2) return;

      RenderSystem.setShaderTexture(0, elegant.id("textures/bloom.png"));
      RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
      BufferBuilder buffer = RenderSystem.renderThreadTesselator().begin(DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
      float dotSize = 0.16F * sizeMul * (1.0F - progress * 0.5F);

      for (int i = 0; i < count; i++) {
         double yaw = rng.nextDouble() * Math.PI * 2.0;
         double pitch = rng.nextDouble() * 1.4 - 0.4;
         float px = (float) relPos.x + (float) (Math.cos(yaw) * Math.cos(pitch)) * spread;
         float pz = (float) relPos.z + (float) (Math.sin(yaw) * Math.cos(pitch)) * spread;
         float py = originY + (float) Math.sin(pitch) * spread + progress * 0.4F;

         ms.push();
         ms.translate(px, py, pz);
         ms.multiply(camera.getRotation());
         Matrix4f dotMatrix = ms.peek().getPositionMatrix();
         buffer.vertex(dotMatrix, -dotSize, -dotSize, 0.0F).texture(0.0F, 1.0F).color(cr, cg, cb, dotAlpha);
         buffer.vertex(dotMatrix, -dotSize, dotSize, 0.0F).texture(0.0F, 0.0F).color(cr, cg, cb, dotAlpha);
         buffer.vertex(dotMatrix, dotSize, dotSize, 0.0F).texture(1.0F, 0.0F).color(cr, cg, cb, dotAlpha);
         buffer.vertex(dotMatrix, dotSize, -dotSize, 0.0F).texture(1.0F, 1.0F).color(cr, cg, cb, dotAlpha);
         ms.pop();
      }
      BufferRenderer.drawWithGlobalProgram(buffer.end());
      // возвращаем шейдер/текстуру для последующих POSITION_COLOR-эффектов
      RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
      RenderSystem.setShaderTexture(0, 0);
   }

   /** Короткая вспышка хитбокса цели. */
   private void drawFlash(MatrixStack ms, Vec3d relPos, LivingEntity target, float progress, float alpha, int cr, int cg, int cb) {
      if (progress > 0.5F) return;
      float flashAlpha = (1.0F - progress * 2.0F) * alpha;
      Box box = target.getBoundingBox();
      // бокс в координатах камеры: позиция цели уже смещена на -camPos
      Box relBox = new Box(
         relPos.x + (box.minX - target.getX()),
         relPos.y + (box.minY - target.getY()),
         relPos.z + (box.minZ - target.getZ()),
         relPos.x + (box.maxX - target.getX()),
         relPos.y + (box.maxY - target.getY()),
         relPos.z + (box.maxZ - target.getZ())
      ).expand(0.06F * (1.0F - progress * 2.0F));

      ms.push();
      RenderSystem.lineWidth(2.5F);
      BufferBuilder buffer = RenderSystem.renderThreadTesselator().begin(DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);
      Draw3DUtility.renderOutlinedBox(ms, buffer, relBox, new ColorRGBA(cr, cg, cb, flashAlpha * 255.0F));
      BufferRenderer.drawWithGlobalProgram(buffer.end());
      ms.pop();
   }

   private float easeOut(float progress) {
      return 1.0F - (1.0F - progress) * (1.0F - progress);
   }
}
