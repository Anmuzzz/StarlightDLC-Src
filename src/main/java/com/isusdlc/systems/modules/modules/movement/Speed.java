package com.isusdlc.systems.modules.modules.movement;

import com.isusdlc.systems.event.EventListener;
import com.isusdlc.systems.event.impl.player.ClientPlayerTickEvent;
import com.isusdlc.systems.modules.api.ModuleCategory;
import com.isusdlc.systems.modules.api.ModuleInfo;
import com.isusdlc.systems.modules.impl.BaseModule;
import com.isusdlc.systems.setting.settings.BooleanSetting;
import com.isusdlc.systems.setting.settings.ModeSetting;
import com.isusdlc.systems.setting.settings.SliderSetting;
import net.minecraft.util.math.Vec3d;

@ModuleInfo(
   name = "Speed",
   category = ModuleCategory.MOVEMENT,
   desc = "Увеличивает скорость передвижения"
)
public class Speed extends BaseModule {
   private final ModeSetting mode = new ModeSetting(this, "Mode");
   private final ModeSetting.Value forward = new ModeSetting.Value(this.mode, "Forward").select();
   private final ModeSetting.Value strafe = new ModeSetting.Value(this.mode, "Strafe");
   private final ModeSetting.Value yPort = new ModeSetting.Value(this.mode, "Y-Port");

   private final SliderSetting speed = new SliderSetting(this, "Speed")
      .min(1.0F).max(5.0F).step(0.1F).currentValue(1.5F);

   private final BooleanSetting onGroundOnly = new BooleanSetting(this, "On ground only").enable();

   private int ticks;

   private final EventListener<ClientPlayerTickEvent> onTick = event -> {
      if (mc.player == null || mc.world == null) return;
      if (mc.player.isGliding() || mc.player.isUsingItem()) return;
      if (onGroundOnly.isEnabled() && !mc.player.isOnGround()) return;

      if (yPort.isSelected()) {
         yPortSpeed();
         return;
      }

      if (mc.player.forwardSpeed == 0 && mc.player.sidewaysSpeed == 0) return;

      if (forward.isSelected()) {
         forwardSpeed();
      } else if (strafe.isSelected()) {
         strafeSpeed();
      }
   };

   private void forwardSpeed() {
      if (!mc.player.isSprinting()) mc.player.setSprinting(true);
      Vec3d vel = mc.player.getVelocity();
      double mult = speed.getCurrentValue() * 0.2873;
      float yaw = mc.player.getYaw() * 0.017453292F;
      Vec3d dir = new Vec3d(-Math.sin(yaw), 0, Math.cos(yaw));
      mc.player.setVelocity(dir.x * mult, vel.y, dir.z * mult);
   }

   private void strafeSpeed() {
      Vec3d vel = mc.player.getVelocity();
      double mult = speed.getCurrentValue() * 0.2873;
      float yaw = mc.player.getYaw() * 0.017453292F;
      Vec3d forward = new Vec3d(-Math.sin(yaw), 0, Math.cos(yaw));
      Vec3d strafeDir = new Vec3d(Math.cos(yaw), 0, Math.sin(yaw));

      float f = mc.player.input.movementForward;
      float s = mc.player.input.movementSideways;

      Vec3d result = forward.multiply(f).add(strafeDir.multiply(s)).normalize().multiply(mult);
      mc.player.setVelocity(result.x, vel.y, result.z);
   }

   private void yPortSpeed() {
      if (!mc.player.isOnGround()) {
         if (ticks == 0) {
            Vec3d vel = mc.player.getVelocity();
            mc.player.setVelocity(vel.x, -0.1, vel.z);
            ticks = 1;
         } else {
            ticks = 0;
         }
         return;
      }

      ticks = 0;
      if (mc.player.forwardSpeed == 0 && mc.player.sidewaysSpeed == 0) return;
      if (!mc.player.isSprinting()) mc.player.setSprinting(true);

      Vec3d vel = mc.player.getVelocity();
      double mult = speed.getCurrentValue() * 0.2873;
      float yaw = mc.player.getYaw() * 0.017453292F;
      Vec3d dir = new Vec3d(-Math.sin(yaw), 0, Math.cos(yaw));
      mc.player.setVelocity(dir.x * mult, vel.y, dir.z * mult);

      mc.player.jump();
      mc.player.setVelocity(mc.player.getVelocity().x * 1.2, mc.player.getVelocity().y, mc.player.getVelocity().z * 1.2);
   }
}
