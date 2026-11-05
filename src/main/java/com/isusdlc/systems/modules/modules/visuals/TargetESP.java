package com.isusdlc.systems.modules.modules.visuals;

import com.mojang.blaze3d.platform.GlStateManager.DstFactor;
import com.mojang.blaze3d.platform.GlStateManager.SrcFactor;
import com.mojang.blaze3d.systems.RenderSystem;
import com.isusdlc.elegant;
import com.isusdlc.systems.event.EventListener;
import com.isusdlc.systems.event.impl.render.Render3DEvent;
import com.isusdlc.systems.modules.api.ModuleCategory;
import com.isusdlc.systems.modules.api.ModuleInfo;
import com.isusdlc.systems.modules.impl.BaseModule;
import com.isusdlc.systems.setting.settings.BooleanSetting;
import com.isusdlc.systems.setting.settings.ColorSetting;
import com.isusdlc.systems.setting.settings.ModeSetting;
import com.isusdlc.systems.setting.settings.SliderSetting;
import com.isusdlc.utility.animation.base.Animation;
import com.isusdlc.utility.animation.base.Easing;
import com.isusdlc.utility.colors.ColorRGBA;
import com.isusdlc.utility.colors.Colors;
import com.isusdlc.utility.game.EntityUtility;
import com.isusdlc.utility.math.MathUtility;
import com.isusdlc.utility.render.CrystalRenderer;
import com.isusdlc.utility.render.DrawUtility;
import com.isusdlc.utility.render.RenderUtility;
import com.isusdlc.utility.time.Timer;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.passive.PigEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.util.Identifier;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.RaycastContext;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.client.render.Camera;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.util.hit.HitResult.Type;
import net.minecraft.client.render.VertexFormat.DrawMode;
import net.minecraft.world.RaycastContext.FluidHandling;
import net.minecraft.world.RaycastContext.ShapeType;
import net.minecraft.client.render.VertexConsumerProvider.Immediate;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

@ModuleInfo(
   name = "Target ESP",
   category = ModuleCategory.VISUALS,
   desc = "Помечает активную цель"
)
public class TargetESP extends BaseModule {
   private final ModeSetting mode = new ModeSetting(this, "modules.settings.target_esp.mode");
   private final ModeSetting.Value souls = new ModeSetting.Value(this.mode, "modules.settings.target_esp.mode.souls");
   private final ModeSetting.Value crystals = new ModeSetting.Value(this.mode, "modules.settings.target_esp.mode.crystals").select();
   private final ModeSetting.Value chains = new ModeSetting.Value(this.mode, "modules.settings.target_esp.mode.chains");
   private final ModeSetting.Value circles = new ModeSetting.Value(this.mode, "modules.settings.target_esp.mode.circles");
   private final ModeSetting.Value marker = new ModeSetting.Value(this.mode, "modules.settings.target_esp.mode.marker");
   private final ModeSetting.Value ring = new ModeSetting.Value(this.mode, "modules.settings.target_esp.mode.ring");
   private final ModeSetting.Value pigs = new ModeSetting.Value(this.mode, "modules.settings.target_esp.mode.pigs");
   private final ModeSetting.Value gradient = new ModeSetting.Value(this.mode, "modules.settings.target_esp.mode.gradient");
   private final ModeSetting.Value halo = new ModeSetting.Value(this.mode, "modules.settings.target_esp.mode.halo");
   private final ColorSetting colorTarget = new ColorSetting(this, "color").color(Colors.ACCENT);
   private final BooleanSetting changeColorOnDamage = new BooleanSetting(this, "modules.settings.target_esp.change_color_on_damage").enable();
   private final SliderSetting speed = new SliderSetting(this, "modules.settings.target_esp.speed").min(0.1F).max(5.0F).step(0.1F).currentValue(0.5F);
   private final SliderSetting size = new SliderSetting(
         this,
         "modules.settings.target_esp.size",
         () -> this.souls.isSelected() || this.crystals.isSelected() || this.ring.isSelected() || this.pigs.isSelected()
      )
      .min(0.5F)
      .max(3.0F)
      .step(0.1F)
      .currentValue(1.5F);
   private final SliderSetting particleCount = new SliderSetting(
         this,
         "modules.settings.target_esp.particle_count",
         () -> this.souls.isSelected() || this.crystals.isSelected() || this.ring.isSelected() || this.pigs.isSelected()
      )
      .min(1.0F)
      .max(50.0F)
      .step(1.0F)
      .currentValue(20.0F);
   private final SliderSetting particleThickness = new SliderSetting(
         this,
         "modules.settings.target_esp.particle_thickness",
         () -> this.souls.isSelected() || this.crystals.isSelected() || this.ring.isSelected() || this.pigs.isSelected()
      )
      .min(0.1F)
      .max(2.0F)
      .step(0.1F)
      .currentValue(1.0F);
   private final Animation animation = new Animation(400L, 0.0F, Easing.FIGMA_EASE_IN_OUT);
   private final Animation hurtAnimation = new Animation(250L, 0.0F, Easing.FIGMA_EASE_IN_OUT);
   private float movingValue;
   private float markerRotationValue;
   private LivingEntity prevTarget;
   private boolean occluded;
   private long occludedAt;
   private float chainRotationAngle = 0.0F;
   private final Timer targetLostTimer = new Timer();
   private double ringStep = 0.0;
   private float ringDamageFlashIntensity = 0.0F;
   private long lastRingDamageTime = 0L;
   private static final long RING_DAMAGE_FLASH_DURATION = 300L;
   private final EventListener<Render3DEvent> onRender3D = event -> {
      if (EntityUtility.isInGame()) {
         LivingEntity target = null;
         HitResult hit = mc.crosshairTarget;
         if (hit != null
            && hit.getType() == Type.ENTITY
            && hit instanceof EntityHitResult ehr
            && ehr.getEntity() instanceof LivingEntity living
            && living != mc.player
            && !living.isInvisible()
            && !(living instanceof PlayerEntity player && (player.isInvisibleTo(mc.player) || player.isSpectator()))) {
            double distance = mc.player.distanceTo(living);
            if (distance <= 3.0) {
               target = living;
            }
         }

         if (target != null) {
            this.prevTarget = target;
            this.targetLostTimer.reset();
         }

          boolean shouldShow = target != null || !this.targetLostTimer.finished(1000L);
         this.animation.update(shouldShow);
         float speedMultiplier = this.speed.getCurrentValue();
         this.movingValue += 8.0F * speedMultiplier;
         this.markerRotationValue += 4.0F * speedMultiplier;
         this.chainRotationAngle += 3.0F * speedMultiplier;
         if (target != null && this.prevTarget == target) {
            boolean isHurt = target.hurtTime > 0;
            this.hurtAnimation.update(isHurt);
            if (isHurt && this.ring.isSelected()) {
               this.lastRingDamageTime = System.currentTimeMillis();
            }
         } else {
            this.hurtAnimation.update(false);
         }

         if (this.prevTarget != null && this.animation.getValue() != 0.0F) {
            MatrixStack ms = event.getMatrices();
            ms.push();
            RenderSystem.enableBlend();
            RenderSystem.blendFunc(SrcFactor.SRC_ALPHA, DstFactor.ONE);
            RenderSystem.enableDepthTest();
            // рейкаст видимости — не чаще 200 мс
            long nowMs = System.currentTimeMillis();
            if (nowMs - this.occludedAt > 200L) {
               this.occluded = mc.world
                  .raycast(
                     new RaycastContext(
                        mc.gameRenderer.getCamera().getPos(),
                        this.prevTarget.getEyePos(),
                        ShapeType.COLLIDER,
                        FluidHandling.NONE,
                        mc.player
                     )
                  )
                  .getType()
               != Type.MISS;
               this.occludedAt = nowMs;
            }
            if (this.occluded) {
               RenderSystem.disableDepthTest();
            }

            RenderSystem.disableCull();
            RenderSystem.depthMask(false);
            if (this.chains.isSelected()) {
               this.drawChains(ms, this.prevTarget);
            } else if (this.circles.isSelected()) {
               this.drawCircles(ms, this.prevTarget);
            } else if (this.marker.isSelected()) {
               this.drawMarker(ms, this.prevTarget);
            } else if (this.crystals.isSelected()) {
               this.drawCrystals(ms, this.prevTarget);
            } else if (this.gradient.isSelected()) {
               this.drawGradientCircle(ms, this.prevTarget);
            } else if (this.halo.isSelected()) {
               this.drawHalo(ms, this.prevTarget);
            } else if (this.ring.isSelected()) {
               this.drawRing(ms, this.prevTarget);
            } else if (this.pigs.isSelected()) {
               this.drawPigs(ms, this.prevTarget);
            } else {
               this.drawGhosts(ms, this.prevTarget);
            }

            RenderSystem.depthMask(true);
            RenderSystem.setShaderTexture(0, 0);
            RenderSystem.disableBlend();
            RenderSystem.enableCull();
            RenderSystem.enableDepthTest();
            ms.pop();
         }
      }
   };

   private ColorRGBA getTargetColor() {
      float hitProgress = this.hurtAnimation.getValue();
      ColorRGBA baseColor = this.colorTarget.getColor();
      if (this.changeColorOnDamage.isEnabled() && hitProgress > 0.0F) {
         ColorRGBA redColor = new ColorRGBA(255.0F, 0.0F, 0.0F, 255.0F);
         return baseColor.mix(redColor, hitProgress);
      } else {
         return baseColor;
      }
   }

   private void drawChains(MatrixStack ms, LivingEntity target) {
      Camera camera = mc.gameRenderer.getCamera();
      Vec3d targetPos = this.getRenderPos(target);
      double centerX = targetPos.x;
      double centerY = targetPos.y + target.getHeight() / 2.0F;
      double centerZ = targetPos.z;
      double renderX = centerX - camera.getPos().getX();
      double renderY = centerY - camera.getPos().getY();
      double renderZ = centerZ - camera.getPos().getZ();
      this.renderChainCylinder(ms, renderX, renderY, renderZ, 0.0F);
      this.renderChainCylinder(ms, renderX, renderY, renderZ, 90.0F);
   }

   private void renderChainCylinder(MatrixStack stack, double x, double y, double z, float offsetAngle) {
      stack.push();
      stack.translate(x, y, z);
      float currentAngle = this.chainRotationAngle;
      stack.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(currentAngle + offsetAngle));
      stack.multiply(RotationAxis.POSITIVE_X.rotationDegrees(currentAngle + offsetAngle));
      float radius = this.prevTarget.getWidth() * 1.5F * this.size.getCurrentValue();
      int segments = Math.max(10, (int)this.particleCount.getCurrentValue());
      ColorRGBA blendedColor = this.getTargetColor();
      int alpha = (int)(this.animation.getValue() * 255.0F);
      Identifier chainTexture = elegant.id("textures/chain.png");
      RenderSystem.setShaderTexture(0, chainTexture);
      RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
      Matrix4f matrix = stack.peek().getPositionMatrix();
      BufferBuilder buffer = RenderSystem.renderThreadTesselator().begin(DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
      float textureRepeat = 4.0F * this.particleThickness.getCurrentValue();
      float chainHeight = this.prevTarget.getHeight() * 0.8F * this.size.getCurrentValue();
      float halfHeight = chainHeight / 2.0F;

      for (int i = 0; i < segments; i++) {
         float angle1 = (float)((Math.PI * 2) * i / segments);
         float angle2 = (float)((Math.PI * 2) * (i + 1) / segments);
         float x1 = (float)(Math.cos(angle1) * radius);
         float z1 = (float)(Math.sin(angle1) * radius);
         float x2 = (float)(Math.cos(angle2) * radius);
         float z2 = (float)(Math.sin(angle2) * radius);
         float u1 = (float)i / segments * textureRepeat;
         float u2 = (float)(i + 1) / segments * textureRepeat;
         ColorRGBA color1 = blendedColor.withAlpha(alpha);
         ColorRGBA color2 = blendedColor.withAlpha(alpha);
         buffer.vertex(matrix, x1, -halfHeight, z1).texture(u1, 1.0F).color(color1.getRGB());
         buffer.vertex(matrix, x2, -halfHeight, z2).texture(u2, 1.0F).color(color2.getRGB());
         buffer.vertex(matrix, x2, halfHeight, z2).texture(u2, 0.0F).color(color2.getRGB());
         buffer.vertex(matrix, x1, halfHeight, z1).texture(u1, 0.0F).color(color1.getRGB());
      }

      BufferRenderer.drawWithGlobalProgram(buffer.end());
      stack.pop();
   }

   private void drawMarker(MatrixStack ms, LivingEntity target) {
      Camera camera = mc.gameRenderer.getCamera();
      Vec3d targetPos = this.getRenderPos(target);
      double entX = targetPos.x - camera.getPos().getX();
      double entY = targetPos.y - camera.getPos().getY() + target.getHeight() / 2.0;
      double entZ = targetPos.z - camera.getPos().getZ();
      float size = 1.2F * this.size.getCurrentValue() * this.particleThickness.getCurrentValue();
      float rotationValue = this.markerRotationValue;
      RenderSystem.enableBlend();
      RenderSystem.blendFunc(SrcFactor.SRC_ALPHA, DstFactor.ONE);
      RenderSystem.disableDepthTest();
      RenderSystem.disableCull();
      ColorRGBA blendedColor = this.getTargetColor();
      ms.push();
      ms.translate(entX, entY, entZ);
      ms.multiply(camera.getRotation());
      ms.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(rotationValue));
      int alpha = (int)(this.animation.getValue() * 255.0F * 0.7F);
      ColorRGBA color = blendedColor.withAlpha(alpha);
      Identifier markerTexture = elegant.id("textures/marker.png");
      RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
      int layers = Math.max(1, (int)(this.particleCount.getCurrentValue() / 10.0F));

      for (int i = 0; i < layers; i++) {
         float layerSize = size * (1.0F + i * 0.1F);
         float layerAlpha = alpha / (i + 1);
         DrawUtility.drawImage(
            ms,
            markerTexture,
            (double)(-layerSize / 2.0F),
            (double)(-layerSize / 2.0F),
            0.0,
            (double)layerSize,
            (double)layerSize,
            blendedColor.withAlpha((int)(layerAlpha * 0.3F))
         );
      }

      DrawUtility.drawImage(ms, markerTexture, (double)(-size / 2.0F), (double)(-size / 2.0F), 0.0, (double)size, (double)size, color);
      ms.pop();
      RenderSystem.enableDepthTest();
      RenderSystem.enableCull();
      RenderSystem.defaultBlendFunc();
      RenderSystem.disableBlend();
   }

   private void drawCircles(MatrixStack ms, LivingEntity target) {
      Camera camera = mc.gameRenderer.getCamera();
      Vec3d cameraPos = camera.getPos();
      Vec3d targetPos = this.getRenderPos(target);
      double entX = targetPos.x - cameraPos.getX();
      double entY = targetPos.y - cameraPos.getY();
      double entZ = targetPos.z - cameraPos.getZ();
      float movingValue = this.movingValue;
      float width = target.getWidth() * 1.45F * this.size.getCurrentValue();
      float baseVal = Math.max(0.5F, 0.7F - 0.1F * this.hurtAnimation.getValue() + 0.1F - 0.1F * this.animation.getValue());
      RenderSystem.enableBlend();
      RenderSystem.blendFunc(SrcFactor.SRC_ALPHA, DstFactor.ONE);
      RenderSystem.disableDepthTest();
      RenderSystem.disableCull();
      ColorRGBA blendedColor = this.getTargetColor();
      Identifier bloomTexture = elegant.id("textures/bloom.png");
      RenderSystem.setShaderTexture(0, bloomTexture);
      RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
      int step = Math.max(1, (int)(360.0F / this.particleCount.getCurrentValue()));
      float size = 0.4F * this.size.getCurrentValue() * this.particleThickness.getCurrentValue();
      float bigSize = 0.8F * this.size.getCurrentValue() * this.particleThickness.getCurrentValue();

      for (int i = 0; i < 360; i += step) {
         if ((int)(i / 45.0F) % 2 != 0) {
            double rad = Math.toRadians(i + movingValue);
            float sin = (float)(entX + Math.sin(rad) * width * baseVal);
            float cos = (float)(entZ + Math.cos(rad) * width * baseVal);
            double radAngle = Math.toRadians(movingValue);
            float waveValue = (float)((1.0 - Math.cos(radAngle)) / 2.0);
            float yPos = (float)(entY + target.getHeight() * waveValue);
            ms.push();
            ms.translate(sin, yPos, cos);
            ms.multiply(camera.getRotation());
            int alpha = (int)(this.animation.getValue() * 255.0F);
            ColorRGBA color = blendedColor.withAlpha(alpha);
            BufferBuilder buffer = RenderSystem.renderThreadTesselator().begin(DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
            int bloomAlpha = (int)(alpha * 0.1F);
            ColorRGBA bloomColor = color.withAlpha(bloomAlpha);
            DrawUtility.drawImage(ms, buffer, (double)(-bigSize / 2.0F), (double)(-bigSize / 2.0F), 0.0, (double)bigSize, (double)bigSize, bloomColor);
            BufferRenderer.drawWithGlobalProgram(buffer.end());
            buffer = RenderSystem.renderThreadTesselator().begin(DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
            DrawUtility.drawImage(ms, buffer, (double)(-size / 2.0F), (double)(-size / 2.0F), 0.0, (double)size, (double)size, color);
            BufferRenderer.drawWithGlobalProgram(buffer.end());
            ms.pop();
         }
      }

      RenderSystem.enableDepthTest();
      RenderSystem.enableCull();
      RenderSystem.defaultBlendFunc();
      RenderSystem.disableBlend();
   }

   private void drawCrystals(MatrixStack ms, LivingEntity target) {
      Camera camera = mc.gameRenderer.getCamera();
      Vec3d cameraPos = camera.getPos();
      ColorRGBA color = this.getTargetColor();
      float width = this.prevTarget.getWidth() * 1.5F;
      RenderUtility.prepareMatrices(ms, this.getRenderPos(this.prevTarget));
      BufferBuilder builder = CrystalRenderer.createBuffer();

      for (int i = 0; i < 360; i += 20) {
         float val = 1.2F - 0.5F * this.animation.getValue();
         float sin = (float)(MathUtility.sin((float)Math.toRadians(i + this.movingValue * 0.3F)) * width * val);
         float cos = (float)(MathUtility.cos((float)Math.toRadians(i + this.movingValue * 0.3F)) * width * val);
         float size = 0.1F;
         ms.push();
         ms.translate(sin, 0.1F + target.getHeight() * Math.abs(MathUtility.sin((float)Math.toRadians(i))), cos);
         Vec3d crystalPos = this.getRenderPos(this.prevTarget).add(sin, 1.0, cos);
         Vec3d targetPos = target.getPos().add(0.0, target.getHeight() / 2.0, 0.0);
         Vector3f directionToTarget = new Vector3f(
               (float)(targetPos.x - crystalPos.x),
               (float)(targetPos.y - crystalPos.y),
               (float)(targetPos.z - crystalPos.z)
            )
            .normalize();
         Vector3f initialDirection = new Vector3f(0.0F, 1.0F, 0.0F);
         Quaternionf rotation = new Quaternionf().rotationTo(initialDirection, directionToTarget);
         ms.multiply(rotation);
         CrystalRenderer.render(ms, builder, 0.0F, 0.0F, 0.0F, size, color.withAlpha(255.0F * this.animation.getValue()));
         ms.pop();
      }

      BufferRenderer.drawWithGlobalProgram(builder.end());
      Identifier id = elegant.id("textures/bloom.png");
      RenderSystem.setShaderTexture(0, id);
      RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
      BufferBuilder buffer = RenderSystem.renderThreadTesselator().begin(DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
      float bigSize = 1.0F;

      for (int i = 0; i < 360; i += 20) {
         float val = 1.2F - 0.5F * this.animation.getValue();
         float sin = (float)(MathUtility.sin((float)Math.toRadians(i + this.movingValue * 0.3F)) * width * val);
         float cos = (float)(MathUtility.cos((float)Math.toRadians(i + this.movingValue * 0.3F)) * width * val);
         ms.push();
         ms.translate(sin, 0.1F + target.getHeight() * Math.abs(MathUtility.sin((float)Math.toRadians(i))), cos);
         ms.multiply(camera.getRotation());
         DrawUtility.drawImage(
            ms,
            buffer,
            (double)(-bigSize / 2.0F),
            (double)(-bigSize / 2.0F),
            0.0,
            (double)bigSize,
            (double)bigSize,
            color.withAlpha(255.0F * this.animation.getValue() * 0.2F)
         );
         ms.pop();
      }

      RenderUtility.buildBuffer(buffer);
   }

   private void drawGhosts(MatrixStack ms, LivingEntity target) {
      Camera camera = mc.gameRenderer.getCamera();
      ColorRGBA color = this.getTargetColor();
      Identifier id = elegant.id("textures/bloom.png");
      float width = this.prevTarget.getWidth() * 1.5F;
      RenderSystem.setShaderTexture(0, id);
      RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
      BufferBuilder builder = RenderSystem.renderThreadTesselator().begin(DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
      RenderUtility.prepareMatrices(ms, this.getRenderPos(this.prevTarget));
      int step = 2;
      int wormTick = 0;
      int wormCD = 0;
      int wormCount = 0;

      for (int i = 0; i < 360; i += step) {
         float size = 0.13F + 0.005F * wormTick;
         float bigSize = 0.7F + 0.005F * wormTick;
         if (wormCD > 0) {
            wormCD -= step;
         } else {
            wormTick += step;
            if (wormTick > 50) {
               wormCD = 100;
               wormTick = 0;
               wormCount++;
            } else {
               float val = Math.max(0.5F, 1.2F - 0.5F * this.animation.getValue());
               float sin = (float)(MathUtility.sin((float)Math.toRadians(i + this.movingValue * 1.0F)) * width * val);
               float cos = (float)(MathUtility.cos((float)Math.toRadians(i + this.movingValue * 1.0F)) * width * val);
               ms.push();
               ms.translate(
                  sin,
                  this.prevTarget.getHeight() / 1.5F
                     + this.prevTarget.getHeight() / 3.0F * MathUtility.sin(Math.toRadians(i / 2.0F + this.movingValue / 5.0F)),
                  cos
               );
               ms.multiply(camera.getRotation());
               DrawUtility.drawImage(
                  ms,
                  builder,
                  (double)(-bigSize / 2.0F),
                  (double)(-bigSize / 2.0F),
                  (double)(-size / 2.0F),
                  (double)bigSize,
                  (double)bigSize,
                  color.withAlpha(color.getAlpha() * this.animation.getValue() * 0.05F)
               );
               DrawUtility.drawImage(
                  ms,
                  builder,
                  (double)(-size / 2.0F),
                  (double)(-size / 2.0F),
                  (double)(-size / 2.0F),
                  (double)size,
                  (double)size,
                  color.withAlpha(color.getAlpha() * this.animation.getValue())
               );
               ms.pop();
            }
         }
      }

      BufferRenderer.drawWithGlobalProgram(builder.end());
   }

   private Vec3d getRenderPos(LivingEntity target) {
      float tickDelta = MinecraftClient.getInstance().getRenderTickCounter().getTickDelta(false);
      return new Vec3d(
         MathHelper.lerp(tickDelta, target.prevX, target.getX()),
         MathHelper.lerp(tickDelta, target.prevY, target.getY()),
         MathHelper.lerp(tickDelta, target.prevZ, target.getZ())
      );
   }

   private void drawRing(MatrixStack ms, LivingEntity target) {
      Camera camera = mc.gameRenderer.getCamera();
      Vec3d targetPos = this.getRenderPos(target);
      Vec3d cameraPos = camera.getPos();
      float entityWidth = target.getWidth() * 0.7F;
      float entityHeight = target.getHeight();
      float animationAlpha = this.easeOutCubic(this.animation.getValue());
      double renderX = targetPos.x - cameraPos.getX();
      double renderY = targetPos.y - cameraPos.getY();
      double renderZ = targetPos.z - cameraPos.getZ();
      RenderSystem.enableBlend();
      RenderSystem.blendFunc(SrcFactor.SRC_ALPHA, DstFactor.ONE);
      RenderSystem.disableCull();
      RenderSystem.enableDepthTest();
      RenderSystem.depthMask(false);
      float ringSpeed = this.speed.getCurrentValue() * 0.02F;
      this.ringStep += ringSpeed;
      double currentStep = this.ringStep;
      double headY = this.absSinAnimation(currentStep) * entityHeight;
      double tailBaseY = this.absSinAnimation(currentStep - 0.4) * entityHeight;
      float headSize = 0.2F * this.size.getCurrentValue() * this.particleThickness.getCurrentValue();
      float tailSize = 0.14F * this.size.getCurrentValue() * this.particleThickness.getCurrentValue();
      int totalPoints = Math.max(40, (int)this.particleCount.getCurrentValue() * 2);
      int tailSegments = 6;
      long currentTime = System.currentTimeMillis();
      Identifier glowTexture = elegant.id("textures/bloom.png");
      RenderSystem.setShaderTexture(0, glowTexture);
      RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
      BufferBuilder buffer = RenderSystem.renderThreadTesselator().begin(DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);

      for (int i = 0; i < totalPoints; i++) {
         double angleRadians = (Math.PI * 2) * i / totalPoints;
         float xOffset = (float)(Math.cos(angleRadians) * entityWidth);
         float zOffset = (float)(Math.sin(angleRadians) * entityWidth);
         int baseColor = this.getThemeColorForRing(i * (360 / totalPoints), currentTime);
         ms.push();
         ms.translate(renderX + xOffset, renderY + headY, renderZ + zOffset);
         ms.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-camera.getYaw()));
         ms.multiply(RotationAxis.POSITIVE_X.rotationDegrees(camera.getPitch()));
         ColorRGBA coreColor = ColorRGBA.fromInt(baseColor).withAlpha(animationAlpha * 255.0F);
         DrawUtility.drawImage(ms, buffer, (double)(-headSize / 2.0F), (double)(-headSize / 2.0F), 0.0, (double)headSize, (double)headSize, coreColor);
         ms.pop();

         for (int t = 1; t <= tailSegments; t++) {
            float tailProgress = (float)t / (tailSegments + 1);
            double currentTailY = headY + (tailBaseY - headY) * tailProgress;
            float currentTailAlpha = animationAlpha * (1.0F - tailProgress) * 200.0F;
            ms.push();
            ms.translate(renderX + xOffset, renderY + currentTailY, renderZ + zOffset);
            ms.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-camera.getYaw()));
            ms.multiply(RotationAxis.POSITIVE_X.rotationDegrees(camera.getPitch()));
            ColorRGBA tailColor = ColorRGBA.fromInt(baseColor).withAlpha(currentTailAlpha);
            DrawUtility.drawImage(ms, buffer, (double)(-tailSize / 2.0F), (double)(-tailSize / 2.0F), 0.0, (double)tailSize, (double)tailSize, tailColor);
            ms.pop();
         }
      }

      BufferRenderer.drawWithGlobalProgram(buffer.end());
      RenderSystem.depthMask(true);
      RenderSystem.enableDepthTest();
      RenderSystem.enableCull();
      RenderSystem.defaultBlendFunc();
      RenderSystem.disableBlend();
   }

   private int getThemeColorForRing(int offsetAngle, long currentTime) {
      return this.applyRingDamageFlash(this.getTargetColor().getRGB());
   }

   private int applyRingDamageFlash(int color) {
      if (!this.changeColorOnDamage.isEnabled()) {
         return color;
      } else {
         float targetIntensity = 0.0F;
         long timeSinceDamage = System.currentTimeMillis() - this.lastRingDamageTime;
         if (timeSinceDamage < 300L) {
            float progress = (float)timeSinceDamage / 300.0F;
            targetIntensity = 1.0F - this.easeOutCubic(progress);
         }

         this.ringDamageFlashIntensity = MathHelper.lerp(1.0F, this.ringDamageFlashIntensity, targetIntensity);
         if (this.ringDamageFlashIntensity < 0.05F) {
            return color;
         } else {
            int alpha = color >> 24 & 0xFF;
            int red = color >> 16 & 0xFF;
            int green = color >> 8 & 0xFF;
            int blue = color & 0xFF;
            int finalRed = MathHelper.lerp(this.ringDamageFlashIntensity, red, 255);
            int finalGreen = MathHelper.lerp(this.ringDamageFlashIntensity, green, 50);
            int finalBlue = MathHelper.lerp(this.ringDamageFlashIntensity, blue, 50);
            return alpha << 24 | finalRed << 16 | finalGreen << 8 | finalBlue;
         }
      }
   }

   private float easeOutCubic(float x) {
      return 1.0F - (float)Math.pow(1.0F - x, 3.0);
   }

   private double absSinAnimation(double step) {
      return Math.abs(Math.sin(step));
   }

   private void drawPigs(MatrixStack ms, LivingEntity target) {
      Camera camera = mc.gameRenderer.getCamera();
      Vec3d targetPos = this.getRenderPos(target);
      Vec3d cameraPos = camera.getPos();
      double centerX = targetPos.x - cameraPos.getX();
      double centerY = targetPos.y - cameraPos.getY();
      double centerZ = targetPos.z - cameraPos.getZ();
      float radius = 0.7F;
      float height = 1.0F;
      float speedMultiplier = 2.5E-4F * this.speed.getCurrentValue();
      long currentTime = System.currentTimeMillis();
      float timeRotation = (float)(-(currentTime % 1000000L)) * speedMultiplier;
      int pigCount = 8;
      double[] pigX = new double[pigCount];
      double[] pigY = new double[pigCount];
      double[] pigZ = new double[pigCount];
      float angleOffset = timeRotation * 360.0F;

      for (int i = 0; i < pigCount; i++) {
         float angle = angleOffset + (float)i / pigCount * 360.0F;
         double rad = Math.toRadians(angle);
         float yOffset = i % 2 == 0 ? 0.1F : -0.1F;
         double offsetX = Math.cos(rad) * radius;
         double offsetZ = Math.sin(rad) * radius;
         pigX[i] = centerX + offsetX;
         pigY[i] = centerY + height + yOffset - 0.2F;
         pigZ[i] = centerZ + offsetZ;
      }

      double centerPigX = centerX;
      double centerPigY = centerY + 2.2F;
      double centerPigZ = centerZ;
      float timeAnim = (float)(currentTime % 1000000L) * this.speed.getCurrentValue() * 0.001F;
      float yaw = timeAnim * 180.0F;
      float pitch = (float)(Math.sin(timeAnim * 1.5) * 120.0);
      float roll = (float)(Math.cos(timeAnim * 1.2) * 90.0);
      ColorRGBA targetColor = this.getTargetColor();
      float colorR = targetColor.getRed() / 255.0F;
      float colorG = targetColor.getGreen() / 255.0F;
      float colorB = targetColor.getBlue() / 255.0F;
      float colorA = this.animation.getValue();
      RenderSystem.enableDepthTest();
      RenderSystem.depthMask(true);
      RenderSystem.enableBlend();
      RenderSystem.defaultBlendFunc();
      RenderSystem.disableCull();
      RenderSystem.setShaderColor(colorR, colorG, colorB, colorA);

      try {
         PigEntity pigEntity = new PigEntity(EntityType.PIG, mc.world);
         pigEntity.age = 0;
         EntityRenderDispatcher dispatcher = mc.getEntityRenderDispatcher();
         Immediate buffer = mc.getBufferBuilders().getEntityVertexConsumers();

         for (int i = 0; i <= pigCount; i++) {
            double worldX;
            double worldY;
            double worldZ;
            double nextWorldX;
            double nextWorldY;
            double nextWorldZ;
            if (i < pigCount) {
               worldX = pigX[i];
               worldY = pigY[i];
               worldZ = pigZ[i];
               int nextIndex = (i + 1) % pigCount;
               nextWorldX = pigX[nextIndex];
               nextWorldY = pigY[nextIndex];
               nextWorldZ = pigZ[nextIndex];
            } else {
               worldX = centerPigX;
               worldY = centerPigY;
               worldZ = centerPigZ;
               nextWorldX = pigX[0];
               nextWorldY = pigY[0];
               nextWorldZ = pigZ[0];
            }

            ms.push();
            ms.translate(worldX, worldY, worldZ);
            if (i == pigCount) {
               ms.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(yaw));
               ms.multiply(RotationAxis.POSITIVE_X.rotationDegrees(pitch));
               ms.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(roll));
            } else {
               double dirX = nextWorldX - worldX;
               double dirY = nextWorldY - worldY;
               double dirZ = nextWorldZ - worldZ;
               float pigYaw = (float)Math.toDegrees(Math.atan2(dirX, dirZ));
               ms.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(pigYaw));
            }

            float scale = i == pigCount
               ? 0.4F * this.animation.getValue() * this.particleThickness.getCurrentValue()
               : 0.3F * this.animation.getValue() * this.particleThickness.getCurrentValue();
            ms.scale(scale, scale, scale);
            pigEntity.bodyYaw = 0.0F;
            pigEntity.prevBodyYaw = 0.0F;
            pigEntity.headYaw = 0.0F;
            pigEntity.prevHeadYaw = 0.0F;
            dispatcher.render(pigEntity, 0.0, 0.0, 0.0, mc.getRenderTickCounter().getTickDelta(false), ms, buffer, 15728880);
            ms.pop();
         }

         buffer.draw();
      } catch (Exception var61) {
      }

      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
      RenderSystem.enableCull();
      RenderSystem.disableBlend();
      RenderSystem.depthMask(true);
   }

   /** Плоский градиентный диск под целью (стиль LiquidBounce "Circle"): яркий край, плавное затухание к центру. */
   private void drawGradientCircle(MatrixStack ms, LivingEntity target) {
      float alpha = this.animation.getValue();
      if (alpha <= 0.01F) return;
      ColorRGBA color = this.getTargetColor();
      Vec3d camPos = mc.gameRenderer.getCamera().getPos();
      float radius = target.getWidth() * 0.9F + 0.3F;
      int cr = (int) color.getRed();
      int cg = (int) color.getGreen();
      int cb = (int) color.getBlue();

      ms.push();
      ms.translate(target.getX() - camPos.x, target.getY() - camPos.y + 0.02, target.getZ() - camPos.z);
      RenderSystem.enableBlend();
      RenderSystem.defaultBlendFunc();
      RenderSystem.disableCull();
      RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
      Matrix4f matrix = ms.peek().getPositionMatrix();
      BufferBuilder buffer = RenderSystem.renderThreadTesselator().begin(DrawMode.TRIANGLE_FAN, VertexFormats.POSITION_COLOR);
      buffer.vertex(matrix, 0.0F, 0.0F, 0.0F).color(cr, cg, cb, 0);
      int segments = 48;
      for (int i = 0; i <= segments; i++) {
         double angle = Math.PI * 2.0 * i / segments;
         buffer.vertex(matrix, (float) (Math.cos(angle) * radius), 0.0F, (float) (Math.sin(angle) * radius))
            .color(cr, cg, cb, (int) (alpha * 110.0F));
      }
      BufferRenderer.drawWithGlobalProgram(buffer.end());
      RenderSystem.enableCull();
      ms.pop();
   }

   /** Вращающееся волновое кольцо-нимб над головой цели с бегущими огнями. */
   private void drawHalo(MatrixStack ms, LivingEntity target) {
      float alpha = this.animation.getValue();
      if (alpha <= 0.01F) return;
      ColorRGBA color = this.getTargetColor();
      Vec3d camPos = mc.gameRenderer.getCamera().getPos();
      float radius = target.getWidth() * 0.75F + 0.25F;
      int cr = (int) color.getRed();
      int cg = (int) color.getGreen();
      int cb = (int) color.getBlue();
      double phase = System.currentTimeMillis() % 4000L / 4000.0 * Math.PI * 2.0;

      ms.push();
      ms.translate(target.getX() - camPos.x, target.getY() + target.getHeight() + 0.35, target.getZ() - camPos.z);
      RenderSystem.enableBlend();
      RenderSystem.defaultBlendFunc();
      RenderSystem.disableDepthTest();
      RenderSystem.disableCull();
      RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
      RenderSystem.lineWidth(2.5F);
      Matrix4f matrix = ms.peek().getPositionMatrix();

      for (int ring = 0; ring < 2; ring++) {
         float ringRadius = radius * (ring == 0 ? 1.0F : 0.72F);
         double ringPhase = ring == 0 ? phase : -phase * 1.35;
         BufferBuilder buffer = RenderSystem.renderThreadTesselator().begin(DrawMode.LINE_STRIP, VertexFormats.POSITION_COLOR);
         int segments = 40;
         for (int i = 0; i <= segments; i++) {
            double angle = Math.PI * 2.0 * i / segments;
            float wave = (float) Math.sin(angle * 3.0 + ringPhase) * 0.05F;
            int segmentAlpha = (int) (alpha * (ring == 0 ? 170.0F : 110.0F));
            buffer.vertex(matrix, (float) (Math.cos(angle) * ringRadius), wave, (float) (Math.sin(angle) * ringRadius))
               .color(cr, cg, cb, segmentAlpha);
         }
         BufferRenderer.drawWithGlobalProgram(buffer.end());
      }

      // бегущие огни: три ярких билборда, едущие по кольцу
      Identifier bloom = elegant.id("textures/bloom.png");
      RenderSystem.setShaderTexture(0, bloom);
      RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
      BufferBuilder dots = RenderSystem.renderThreadTesselator().begin(DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
      float dotSize = 0.14F;
      for (int dot = 0; dot < 3; dot++) {
         double angle = phase * 1.5 + Math.PI * 2.0 * dot / 3.0;
         float px = (float) (Math.cos(angle) * radius);
         float pz = (float) (Math.sin(angle) * radius);
         float py = (float) Math.sin(angle * 3.0 + phase) * 0.05F;
         ms.push();
         ms.translate(px, py, pz);
         ms.multiply(mc.gameRenderer.getCamera().getRotation());
         Matrix4f dotMatrix = ms.peek().getPositionMatrix();
         dots.vertex(dotMatrix, -dotSize, -dotSize, 0.0F).texture(0.0F, 1.0F).color(cr, cg, cb, (int) (alpha * 220.0F));
         dots.vertex(dotMatrix, -dotSize, dotSize, 0.0F).texture(0.0F, 0.0F).color(cr, cg, cb, (int) (alpha * 220.0F));
         dots.vertex(dotMatrix, dotSize, dotSize, 0.0F).texture(1.0F, 0.0F).color(cr, cg, cb, (int) (alpha * 220.0F));
         dots.vertex(dotMatrix, dotSize, -dotSize, 0.0F).texture(1.0F, 1.0F).color(cr, cg, cb, (int) (alpha * 220.0F));
         ms.pop();
      }
      BufferRenderer.drawWithGlobalProgram(dots.end());

      RenderSystem.enableDepthTest();
      RenderSystem.enableCull();
      ms.pop();
   }

   @Override
   public void tick() {
      super.tick();
   }
}
