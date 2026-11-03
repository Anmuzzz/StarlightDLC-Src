package com.isusdlc.systems.modules.modules.movement;

import com.isusdlc.systems.event.EventListener;
import com.isusdlc.systems.event.impl.player.ClientPlayerTickEvent;
import com.isusdlc.systems.modules.api.ModuleCategory;
import com.isusdlc.systems.modules.api.ModuleInfo;
import com.isusdlc.systems.modules.impl.BaseModule;

@ModuleInfo(
   name = "Auto Sprint",
   category = ModuleCategory.MOVEMENT,
   desc = "Автоматически зажимает спринт",
   enabledByDefault = true
)
public class AutoSprint extends BaseModule {
   private final EventListener<ClientPlayerTickEvent> onUpdateEvent = event -> mc.options.sprintKey.setPressed(true);

   @Override
   public void onDisable() {
      if (mc.player != null) {
         mc.options.sprintKey.setPressed(false);
      }
   }
}
