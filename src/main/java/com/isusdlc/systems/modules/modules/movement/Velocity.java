package com.isusdlc.systems.modules.modules.movement;

import com.isusdlc.systems.event.EventListener;
import com.isusdlc.systems.event.impl.network.ReceivePacketEvent;
import com.isusdlc.systems.modules.api.ModuleCategory;
import com.isusdlc.systems.modules.api.ModuleInfo;
import com.isusdlc.systems.modules.impl.BaseModule;
import com.isusdlc.systems.setting.settings.ModeSetting;
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
   private final EventListener<ReceivePacketEvent> onPacket = event -> {
      if (mc.world != null && mc.player != null && event.getPacket() instanceof EntityVelocityUpdateS2CPacket packet) {
         if (packet.getEntityId() == mc.player.getId()) {
            if (this.mode.is(this.cancel)) {
               event.cancel();
            } else if (this.mode.is(this.reduce)) {
               event.cancel();
               mc.player.setVelocity(
                  packet.getVelocityX() * 0.5,
                  packet.getVelocityY() * 0.5,
                  packet.getVelocityZ() * 0.5
               );
            }
         }
      }
   };
}
