package com.isusdlc.systems.modules.modules.visuals;

import com.mojang.blaze3d.platform.GlStateManager.DstFactor;
import com.mojang.blaze3d.platform.GlStateManager.SrcFactor;
import com.mojang.blaze3d.systems.RenderSystem;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import com.isusdlc.elegant;
import com.isusdlc.systems.event.EventListener;
import com.isusdlc.systems.event.impl.game.EntityDeathEvent;
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
import com.isusdlc.utility.math.MathUtility;
import com.isusdlc.utility.render.DrawUtility;
import com.isusdlc.utility.render.RenderUtility;
import com.isusdlc.utility.sounds.ClientSoundInstance;
import com.isusdlc.utility.sounds.ClientSounds;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.util.Identifier;
import net.minecraft.client.render.Camera;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.render.BuiltBuffer;
import net.minecraft.client.render.VertexFormat.DrawMode;

@ModuleInfo(
   name = "Kill Effects",
   category = ModuleCategory.VISUALS,
   desc = "modules.descriptions.kill_effects"
)
public class KillEffects extends BaseModule {
   private final List<KillEffects.Lightning> lightnings = new ArrayList<>();
   private final Random random = new Random();
   private final ModeSetting colorMode = new ModeSetting(this, "modules.settings.kill_effects.color_mode");
   private final ModeSetting.Value clientColor = new ModeSetting.Value(this.colorMode, "modules.settings.kill_effects.color_mode.client").select();
   private final ModeSetting.Value customColor = new ModeSetting.Value(this.colorMode, "modules.settings.kill_effects.color_mode.custom");
   private final ColorSetting color = new ColorSetting(this, "modules.settings.kill_effects.color", () -> !this.customColor.isSelected())
      .color(new ColorRGBA(151.0F, 71.0F, 255.0F, 255.0F));
   private final BooleanSetting playSound = new BooleanSetting(this, "modules.settings.kill_effects.play_sound").enabled(true);
   private final SliderSetting volume = new SliderSetting(this, "modules.settings.kill_effects.volume", () -> !this.playSound.isEnabled())
      .min(0.0F)
      .max(100.0F)
      .step(1.0F)
      .currentValue(100.0F);
   private final BooleanSetting mobs = new BooleanSetting(this, "modules.settings.kill_effects.mobs").enabled(false);
   private final ModeSetting soundType = new ModeSetting(this, "modules.settings.kill_effects.sound_type", () -> !this.playSound.isEnabled());
   private final ModeSetting.Value kill1 = new ModeSetting.Value(this.soundType, "Kill1");
   private final ModeSetting.Value kill2 = new ModeSetting.Value(this.soundType, "Kill2");
   private final ModeSetting.Value kill3 = new ModeSetting.Value(this.soundType, "Kill3");
   private final ModeSetting.Value kill4 = new ModeSetting.Value(this.soundType, "Kill4");
   private final ModeSetting.Value kill5 = new ModeSetting.Value(this.soundType, "Kill5");
   private final ModeSetting.Value kill6 = new ModeSetting.Value(this.soundType, "Kill6");
   private final ModeSetting.Value randomMode = new ModeSetting.Value(this.soundType, "Random").select();
   private final EventListener<EntityDeathEvent> onEntityDeath = event -> {
      if (mc.world != null && mc.player != null && event.getEntity() instanceof LivingEntity) {
         boolean isPlayerKiller = false;
         if (event.getSource() != null
            && event.getSource().getAttacker() != null
            && event.getSource().getAttacker().getUuid().equals(mc.player.getUuid())) {
            isPlayerKiller = true;
         }

         if (!isPlayerKiller) {
            return;
         }

         if ((this.mobs.isEnabled() || event.getEntity() instanceof PlayerEntity) && event.getEntity() != mc.player && !event.getEntity().isRemoved()) {
            ColorRGBA effectColor = this.clientColor.isSelected() ? Colors.getAccentColor() : this.color.getColor();
            this.lightnings.add(new KillEffects.Lightning(event.getEntity().getPos(), effectColor));
            if (this.playSound.isEnabled()) {
               ClientSoundInstance sound = this.getSelectedSound();
               sound.play(this.volume.getCurrentValue() / 100.0F);
            }
         }
      }
   };
   private final EventListener<Render3DEvent> on3DRender = event -> {
      MatrixStack ms = event.getMatrices();
      Camera camera = mc.gameRenderer.getCamera();
      Vec3d cameraPos = camera.getPos();
      ms.push();
      RenderSystem.enableBlend();
      RenderSystem.blendFunc(SrcFactor.SRC_ALPHA, DstFactor.ONE);
      RenderSystem.enableDepthTest();
      RenderSystem.disableCull();
      RenderSystem.depthMask(false);
      Identifier id = elegant.id("textures/bloom.png");
      RenderSystem.setShaderTexture(0, id);
      RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
      BufferBuilder builder = RenderSystem.renderThreadTesselator().begin(DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);

      for (KillEffects.Lightning lightning : this.lightnings) {
         lightning.render(builder, event.getMatrices(), camera);
         if (lightning.animation.getValue() == 1.0F) {
            lightning.showing = false;
         }
      }

      BuiltBuffer builtBuffer = builder.endNullable();
      if (builtBuffer != null) {
         BufferRenderer.drawWithGlobalProgram(builtBuffer);
      }

      RenderSystem.depthMask(true);
      RenderSystem.setShaderTexture(0, 0);
      RenderSystem.disableBlend();
      RenderSystem.enableCull();
      RenderSystem.enableDepthTest();
      ms.pop();
      this.lightnings.removeIf(lightningx -> !lightningx.showing && lightningx.animation.getValue() == 0.0F);
   };

   private ClientSoundInstance getSelectedSound() {
      if (this.soundType.is(this.kill1)) {
         return ClientSounds.KILL1;
      } else if (this.soundType.is(this.kill2)) {
         return ClientSounds.KILL2;
      } else if (this.soundType.is(this.kill3)) {
         return ClientSounds.KILL3;
      } else if (this.soundType.is(this.kill4)) {
         return ClientSounds.KILL4;
      } else if (this.soundType.is(this.kill5)) {
         return ClientSounds.KILL5;
      } else if (this.soundType.is(this.kill6)) {
         return ClientSounds.KILL6;
      } else if (this.soundType.is(this.randomMode)) {
         ClientSoundInstance[] killSounds = new ClientSoundInstance[]{
            ClientSounds.KILL1, ClientSounds.KILL2, ClientSounds.KILL3, ClientSounds.KILL4, ClientSounds.KILL5, ClientSounds.KILL6
         };
         return killSounds[this.random.nextInt(killSounds.length)];
      } else {
         return ClientSounds.KILL1;
      }
   }

   static class Lightning {
      final Vec3d pos;
      final ColorRGBA color;
      boolean showing = true;
      final Animation animation = new Animation(300L, 0.0F, Easing.FIGMA_EASE_IN_OUT);
      final List<Vec3d> poses = new ArrayList<>();

      public Lightning(Vec3d pos, ColorRGBA color) {
         this.pos = pos;
         this.color = color;
         Vec3d lastPos = pos;

         for (int i = 0; i < 200; i++) {
            this.poses.add(lastPos = lastPos.add(MathUtility.random(-0.4F, 0.4F), 0.25, MathUtility.random(-0.4F, 0.4F)));
         }
      }

      void render(BufferBuilder builder, MatrixStack ms, Camera camera) {
         this.animation.setEasing(Easing.BOUNCE_IN);
         this.animation.setDuration(500L);
         this.animation.update(this.showing);

         for (Vec3d pos : this.poses) {
            float size = (float)(2.0 + 5.0 * (pos.y - this.pos.y) / 50.0);
            ms.push();
            RenderUtility.prepareMatrices(ms, pos);
            ms.multiply(camera.getRotation());
            DrawUtility.drawImage(
               ms,
               builder,
               (double)(-size / 2.0F),
               (double)(-size / 2.0F),
               0.0,
               (double)size,
               (double)size,
               this.color.withAlpha(255.0F * this.animation.getValue() * 0.4F)
            );
            ms.pop();
         }
      }
   }
}
