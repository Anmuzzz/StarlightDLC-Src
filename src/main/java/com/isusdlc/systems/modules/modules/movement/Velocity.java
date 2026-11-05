package com.isusdlc.systems.modules.modules.movement;

import com.isusdlc.systems.event.EventListener;
import com.isusdlc.systems.event.impl.network.ReceivePacketEvent;
import com.isusdlc.systems.modules.api.ModuleCategory;
import com.isusdlc.systems.modules.api.ModuleInfo;
import com.isusdlc.systems.modules.impl.BaseModule;
import com.isusdlc.systems.setting.settings.ModeSetting;
import com.isusdlc.systems.setting.settings.SliderSetting;
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@ModuleInfo(
   name = "Velocity",
   category = ModuleCategory.MOVEMENT,
   desc = "modules.descriptions.velocity"
)
@Environment(EnvType.CLIENT)
public class Velocity extends BaseModule {
   private final ModeSetting mode = new ModeSetting(this, "modules.settings.velocity.mode");
   private final ModeSetting.Value cancel = new ModeSetting.Value(this.mode, "Cancel").select();
   private final ModeSetting.Value reduce = new ModeSetting.Value(this.mode, "Reduce");
   private final SliderSetting reduceFactor = new SliderSetting(this, "Reduce factor", () -> !this.mode.is(this.reduce))
      .min(0.1F).max(1.0F).step(0.05F).currentValue(0.5F);
   private final EventListener<ReceivePacketEvent> onPacket = event -> {
      if (mc.world != null && mc.player != null && event.getPacket() instanceof EntityVelocityUpdateS2CPacket packet) {
         if (packet.getEntityId() == mc.player.getId()) {
            if (this.mode.is(this.cancel)) {
               event.cancel();
            } else if (this.mode.is(this.reduce)) {
               event.cancel();
               float factor = this.reduceFactor.getCurrentValue();
               mc.player.setVelocity(
                  packet.getVelocityX() * factor,
                  packet.getVelocityY() * factor,
                  packet.getVelocityZ() * factor
               );
            }
         }
      }
   };
}
