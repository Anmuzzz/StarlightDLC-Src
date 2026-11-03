package com.isusdlc.systems.modules.modules.movement;
import com.isusdlc.systems.event.EventListener;
import com.isusdlc.systems.setting.settings.ModeSetting;
import com.isusdlc.systems.setting.settings.SliderSetting;

import com.isusdlc.systems.event.EventListener;
import com.isusdlc.systems.event.impl.player.ClientPlayerTickEvent;
import com.isusdlc.systems.modules.api.ModuleCategory;
import com.isusdlc.systems.modules.api.ModuleInfo;
import com.isusdlc.systems.modules.impl.BaseModule;
import com.isusdlc.systems.setting.settings.ModeSetting;
import com.isusdlc.systems.setting.settings.SliderSetting;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

@ModuleInfo(
   name = "No Web",
   category = ModuleCategory.MOVEMENT,
   desc = "Убирает замедление от паутины"
)
public class NoWeb extends BaseModule {
   private final ModeSetting mode = new ModeSetting(this, "Mode");
   private final ModeSetting.Value custom = new ModeSetting.Value(this.mode, "Custom").select();
   private final ModeSetting.Value reallyWorld = new ModeSetting.Value(this.mode, "ReallyWorld");

   private final SliderSetting speedXZ = new SliderSetting(this, "Speed XZ", () -> !this.mode.is(this.custom))
      .min(0.1F).max(1.0F).step(0.1F).currentValue(0.1F);
   private final SliderSetting speedY = new SliderSetting(this, "Speed Y", () -> !this.mode.is(this.custom))
      .min(0.1F).max(4.0F).step(0.1F).currentValue(0.1F);

   private final EventListener<ClientPlayerTickEvent> onTick = event -> {
      if (mc.player == null || mc.world == null) return;
      if (!mc.world.getBlockState(BlockPos.ofFloored(mc.player.getPos())).isOf(Blocks.COBWEB)) return;

      Vec3d vel = mc.player.getVelocity();
      mc.player.setVelocity(vel.x, 0, vel.z);

      if (this.mode.is(this.custom)) {
         if (mc.options.jumpKey.isPressed()) {
            mc.player.setVelocity(vel.x, this.speedY.getCurrentValue(), vel.z);
         }
         if (mc.options.sneakKey.isPressed()) {
            mc.player.setVelocity(vel.x, -this.speedY.getCurrentValue(), vel.z);
         }
         double yaw = Math.toRadians(mc.player.getYaw());
         Vec3d forward = new Vec3d(-Math.sin(yaw), 0, Math.cos(yaw));
         mc.player.setVelocity(forward.x * this.speedXZ.getCurrentValue(), mc.player.getVelocity().y, forward.z * this.speedXZ.getCurrentValue());
      } else if (this.mode.is(this.reallyWorld)) {
         if (mc.options.jumpKey.isPressed()) {
            mc.player.setVelocity(vel.x, 0.9, vel.z);
         }
         if (mc.options.sneakKey.isPressed()) {
            mc.player.setVelocity(vel.x, -0.9, vel.z);
         }
         double yaw = Math.toRadians(mc.player.getYaw());
         Vec3d forward = new Vec3d(-Math.sin(yaw), 0, Math.cos(yaw));
         mc.player.setVelocity(forward.x * 0.21F, mc.player.getVelocity().y, forward.z * 0.21F);
      }
   };
}
