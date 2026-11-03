package com.isusdlc.systems.modules.modules.movement;
import com.isusdlc.systems.event.EventListener;
import com.isusdlc.systems.setting.settings.BooleanSetting;

import com.isusdlc.systems.event.EventListener;
import com.isusdlc.systems.event.impl.player.ClientPlayerTickEvent;
import com.isusdlc.systems.modules.api.ModuleCategory;
import com.isusdlc.systems.modules.api.ModuleInfo;
import com.isusdlc.systems.modules.impl.BaseModule;
import com.isusdlc.systems.setting.settings.BooleanSetting;
import net.minecraft.util.math.Vec3d;

@ModuleInfo(
   name = "Air Stuck",
   category = ModuleCategory.MOVEMENT,
   desc = "Позволяет зависать в воздухе"
)
public class AirStuck extends BaseModule {
   private final BooleanSetting cancelMovement = new BooleanSetting(this, "Cancel movement").enable();
   private Vec3d freezePosition = Vec3d.ZERO;

   @Override
   public void onEnable() {
      if (mc.player != null) {
         freezePosition = mc.player.getPos();
      }
   }

   private final EventListener<ClientPlayerTickEvent> onTick = event -> {
      if (mc.player == null) return;
      if (!cancelMovement.isEnabled()) return;
      mc.player.setPosition(freezePosition);
      mc.player.setVelocity(Vec3d.ZERO);
   };
}
