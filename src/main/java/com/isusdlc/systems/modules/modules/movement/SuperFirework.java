package com.isusdlc.systems.modules.modules.movement;

import com.isusdlc.systems.event.EventListener;
import com.isusdlc.systems.event.impl.game.FireworkEvent;
import com.isusdlc.systems.modules.api.ModuleCategory;
import com.isusdlc.systems.modules.api.ModuleInfo;
import com.isusdlc.systems.modules.impl.BaseModule;
import com.isusdlc.systems.setting.settings.BooleanSetting;
import com.isusdlc.systems.setting.settings.ModeSetting;
import com.isusdlc.systems.setting.settings.SliderSetting;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.Vec3d;

@ModuleInfo(
   name = "Super Firework",
   category = ModuleCategory.MOVEMENT,
   desc = "Даёт больше буста от фейерверка"
)
public class SuperFirework extends BaseModule {
   private final ModeSetting mode = new ModeSetting(this, "Mode");
    private final ModeSetting.Value bravoHvH = new ModeSetting.Value(this.mode, "BravoHvH").select();
    private final ModeSetting.Value reallyWorld = new ModeSetting.Value(this.mode, "ReallyWorld");
    private final ModeSetting.Value custom = new ModeSetting.Value(this.mode, "Custom");

   private final SliderSetting speed = new SliderSetting(this, "Speed", () -> !this.mode.is(this.custom))
      .min(1.50F).max(8.00F).step(0.01F).currentValue(1.70F);

   private final BooleanSetting nearBoost = new BooleanSetting(this, "Near boost");

   private final EventListener<FireworkEvent> onFirework = event -> {
      if (mc.player == null) return;

      double boostXZ = this.mode.is(this.custom) ? this.speed.getCurrentValue() : 1.61;
      double boostY = boostXZ;
      Vec3d vel = mc.player.getVelocity();
      mc.player.setVelocity(vel.x * boostXZ, vel.y * boostY, vel.z * boostXZ);

      if (this.nearBoost.isEnabled()) {
         LivingEntity nearest = null;
         double nearestDist = Double.MAX_VALUE;
         for (LivingEntity entity : mc.world.getEntitiesByClass(LivingEntity.class, mc.player.getBoundingBox().expand(10), e -> e != mc.player)) {
            double dist = mc.player.distanceTo(entity);
            if (dist < nearestDist) {
               nearestDist = dist;
               nearest = entity;
            }
         }
         if (nearest != null && nearestDist < 5) {
            vel = mc.player.getVelocity();
            mc.player.setVelocity(vel.x * 1.3, vel.y * 1.3, vel.z * 1.3);
         }
      }
   };
}
