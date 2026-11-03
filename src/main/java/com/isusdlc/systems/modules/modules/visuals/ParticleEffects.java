package com.isusdlc.systems.modules.modules.visuals;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import com.isusdlc.systems.event.EventListener;
import com.isusdlc.systems.event.impl.game.AttackEvent;
import com.isusdlc.systems.event.impl.render.Render3DEvent;
import com.isusdlc.systems.modules.api.ModuleCategory;
import com.isusdlc.systems.modules.api.ModuleInfo;
import com.isusdlc.systems.modules.impl.BaseModule;
import com.isusdlc.systems.setting.settings.BooleanSetting;
import com.isusdlc.systems.setting.settings.ColorSetting;
import com.isusdlc.systems.setting.settings.ModeSetting;
import com.isusdlc.systems.setting.settings.SliderSetting;
import com.isusdlc.utility.colors.Colors;
import com.isusdlc.utility.particle.ParticleUtil;
import net.minecraft.util.math.Vec3d;

@ModuleInfo(
   name = "Particle Effects",
   category = ModuleCategory.VISUALS,
   desc = "Кастомные партиклы при различных событиях"
)
public class ParticleEffects extends BaseModule {
   private final List<ParticleUtil.Particle> particles = new ArrayList<>();
   private final Random random = new Random();
   private final BooleanSetting onHit = new BooleanSetting(this, "При ударе", "Партиклы при атаке").enable();
   private final BooleanSetting onMove = new BooleanSetting(this, "При движении", "Партиклы при движении");
   private final BooleanSetting onJump = new BooleanSetting(this, "При прыжке", "Партиклы при прыжке");
   private final ModeSetting particleType = new ModeSetting(this, "Тип партикла");
   private final ModeSetting.Value heartMode = new ModeSetting.Value(this.particleType, "Heart");
   private final ModeSetting.Value starMode = new ModeSetting.Value(this.particleType, "Star").select();
   private final ModeSetting.Value snowMode = new ModeSetting.Value(this.particleType, "Snow");
   private final ModeSetting.Value bloomMode = new ModeSetting.Value(this.particleType, "Bloom");
   private final ModeSetting.Value dollarMode = new ModeSetting.Value(this.particleType, "Dollar");
   private final ModeSetting.Value triangleMode = new ModeSetting.Value(this.particleType, "Triangle");
   private final ModeSetting.Value sakuraMode = new ModeSetting.Value(this.particleType, "Sakura");
   private final ModeSetting.Value geminiMode = new ModeSetting.Value(this.particleType, "Gemini");
   private final ModeSetting.Value simsMode = new ModeSetting.Value(this.particleType, "Sims");
   private final ColorSetting particleColor = new ColorSetting(this, "Цвет партикла").color(Colors.getAccentColor());
   private final SliderSetting particleCount = new SliderSetting(this, "Количество", "Количество партиклов за раз")
      .min(1.0F)
      .max(50.0F)
      .step(1.0F)
      .currentValue(10.0F);
   private final SliderSetting particleSize = new SliderSetting(this, "Размер", "Размер партиклов").min(0.1F).max(1.0F).step(0.05F).currentValue(0.3F);
   private final SliderSetting particleSpeed = new SliderSetting(this, "Скорость", "Скорость движения партиклов")
      .min(0.1F)
      .max(2.0F)
      .step(0.1F)
      .currentValue(0.1F);
   private final SliderSetting fadeInTime = new SliderSetting(this, "Время появления", "Время появления в мс")
      .min(100.0F)
      .max(2000.0F)
      .step(100.0F)
      .currentValue(500.0F);
   private final SliderSetting fadeOutTime = new SliderSetting(this, "Время жизни", "Время до исчезания в мс")
      .min(1000.0F)
      .max(10000.0F)
      .step(500.0F)
      .currentValue(3000.0F);
   private Vec3d lastPlayerPos = Vec3d.ZERO;
   private boolean wasOnGround = false;
   private final EventListener<AttackEvent> onAttack = event -> {
      if (mc.player != null && event.getEntity() != null) {
         if (this.onHit.isEnabled()) {
            Vec3d hitPos = event.getEntity().getPos().add(0.0, event.getEntity().getHeight() / 2.0F, 0.0);
            Vec3d playerPos = mc.player.getPos().add(0.0, mc.player.getHeight() / 2.0F, 0.0);
            Vec3d direction = hitPos.subtract(playerPos).normalize();
            this.spawnHitParticles(hitPos, direction, (int)this.particleCount.getCurrentValue());
         }
      }
   };
   private final EventListener<Render3DEvent> on3DRender = event -> {
      if (mc.player != null && mc.world != null) {
         Vec3d currentPos = mc.player.getPos();
         double distance = currentPos.distanceTo(this.lastPlayerPos);
         boolean isOnGround = mc.player.isOnGround();
         if (this.onMove.isEnabled() && isOnGround && distance > 0.1 && distance < 1.5) {
            Vec3d particlePos = currentPos.add((this.random.nextDouble() - 0.5) * 0.5, 0.1, (this.random.nextDouble() - 0.5) * 0.5);
            this.spawnParticles(particlePos, 1);
         }

         this.lastPlayerPos = currentPos;
         if (this.onJump.isEnabled()) {
            if (!isOnGround && this.wasOnGround) {
               Vec3d jumpPos = mc.player.getPos();
               this.spawnParticles(jumpPos, (int)(this.particleCount.getCurrentValue() / 2.0F));
            }

            this.wasOnGround = isOnGround;
         }

         this.renderParticles(event);
      }
   };

   private void spawnParticles(Vec3d position, int count) {
      ParticleUtil.ParticleType type = this.getParticleType();
      int color = this.particleColor.getColor().getRGB();
      float size = this.particleSize.getCurrentValue();
      double speed = this.particleSpeed.getCurrentValue();

      for (int i = 0; i < count; i++) {
         Vec3d velocity = new Vec3d((this.random.nextDouble() - 0.5) * 2.0, this.random.nextDouble() * 2.0, (this.random.nextDouble() - 0.5) * 2.0);
         ParticleUtil.Particle particle = new ParticleUtil.Particle(
            mc,
            type,
            position.add((this.random.nextDouble() - 0.5) * 0.5, (this.random.nextDouble() - 0.5) * 0.5, (this.random.nextDouble() - 0.5) * 0.5),
            velocity,
            this.particles.size(),
            0,
            color,
            size,
            speed
         );
         this.particles.add(particle);
      }

      while (this.particles.size() > 500) {
         this.particles.remove(0);
      }
   }

   private void spawnHitParticles(Vec3d position, Vec3d direction, int count) {
      ParticleUtil.ParticleType type = this.getParticleType();
      int color = this.particleColor.getColor().getRGB();
      float size = this.particleSize.getCurrentValue();
      double speed = this.particleSpeed.getCurrentValue();

      for (int i = 0; i < count; i++) {
         double theta = this.random.nextDouble() * Math.PI * 2.0;
         double phi = this.random.nextDouble() * Math.PI;
         double x = Math.sin(phi) * Math.cos(theta);
         double y = Math.sin(phi) * Math.sin(theta);
         double z = Math.cos(phi);
         double baseSpeed = 0.2 + this.random.nextDouble() * 0.3;
         Vec3d velocity = new Vec3d(x, y, z).normalize().multiply(baseSpeed);
         ParticleUtil.Particle particle = new ParticleUtil.Particle(
            mc,
            type,
            position.add((this.random.nextDouble() - 0.5) * 0.2, (this.random.nextDouble() - 0.5) * 0.2, (this.random.nextDouble() - 0.5) * 0.2),
            velocity,
            this.particles.size(),
            0,
            color,
            size,
            speed
         );
         this.particles.add(particle);
      }

      while (this.particles.size() > 500) {
         this.particles.remove(0);
      }
   }

   private void renderParticles(Render3DEvent event) {
      if (!this.particles.isEmpty()) {
         Vec3d cameraPos = mc.gameRenderer.getCamera().getPos();
         ParticleUtil.renderParticles(
            event.getMatrices(),
            cameraPos,
            this.particles,
            (long)this.fadeInTime.getCurrentValue(),
            (long)this.fadeOutTime.getCurrentValue(),
            event.getTickDelta()
         );
         long totalLifetime = (long)this.fadeOutTime.getCurrentValue() + 1000L;
         this.particles.removeIf(particle -> particle.time().finished(totalLifetime) && particle.animation().getValue() <= 0.0);
      }
   }

   private ParticleUtil.ParticleType getParticleType() {
      if (this.particleType.is(this.heartMode)) {
         return ParticleUtil.ParticleType.HEART;
      } else if (this.particleType.is(this.starMode)) {
         return ParticleUtil.ParticleType.STAR;
      } else if (this.particleType.is(this.snowMode)) {
         return ParticleUtil.ParticleType.SNOW;
      } else if (this.particleType.is(this.bloomMode)) {
         return ParticleUtil.ParticleType.BLOOM;
      } else if (this.particleType.is(this.dollarMode)) {
         return ParticleUtil.ParticleType.DOLLAR;
      } else if (this.particleType.is(this.triangleMode)) {
         return ParticleUtil.ParticleType.TRIANGLE;
      } else if (this.particleType.is(this.sakuraMode)) {
         return ParticleUtil.ParticleType.SAKURA;
      } else if (this.particleType.is(this.geminiMode)) {
         return ParticleUtil.ParticleType.GEMINI;
      } else {
         return this.particleType.is(this.simsMode) ? ParticleUtil.ParticleType.SIMS : ParticleUtil.ParticleType.STAR;
      }
   }

   @Override
   public void onEnable() {
      if (mc.player != null) {
         this.lastPlayerPos = mc.player.getPos();
         this.wasOnGround = mc.player.isOnGround();
      }
   }

   @Override
   public void onDisable() {
      this.particles.clear();
   }
}
