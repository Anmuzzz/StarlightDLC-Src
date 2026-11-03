package com.isusdlc.systems.modules.modules.other;

import com.isusdlc.elegant;
import com.isusdlc.systems.event.EventListener;
import com.isusdlc.systems.event.impl.player.ClientPlayerTickEvent;
import com.isusdlc.systems.modules.api.ModuleCategory;
import com.isusdlc.systems.modules.api.ModuleInfo;
import com.isusdlc.systems.modules.impl.BaseModule;
import com.isusdlc.systems.setting.settings.BooleanSetting;
import com.isusdlc.systems.waypoints.WayPointsManager;
import com.isusdlc.utility.game.MessageUtility;
import net.minecraft.text.Text;
import net.minecraft.client.gui.screen.DeathScreen;
import net.minecraft.util.math.MathHelper;

@ModuleInfo(
   name = "Death Cords",
   category = ModuleCategory.OTHER,
   desc = "Отправляет координаты смерти в чат"
)
public class DeathCords extends BaseModule {
   private boolean death;
   private final BooleanSetting wayDeath = new BooleanSetting(this, "Ставить метку");
   private final EventListener<ClientPlayerTickEvent> onUpdateEvent = event -> {
      if (!(mc.currentScreen instanceof DeathScreen) || mc.player == null) {
         this.death = true;
      } else if (this.death) {
         int xCord = MathHelper.floor(mc.player.getX());
         int yCord = MathHelper.floor(mc.player.getY());
         int zCord = MathHelper.floor(mc.player.getZ());
         MessageUtility.info(Text.of("Координаты смерти: " + xCord + " " + yCord + " " + zCord));
         if (this.wayDeath.isEnabled()) {
            WayPointsManager wayPointsManager = elegant.getInstance().getWayPointsManager();
            if (wayPointsManager.contains("Death")) {
               wayPointsManager.del("Death");
            }

            wayPointsManager.add("Death", xCord, yCord, zCord);
         }

         this.death = false;
      }
   };
}
