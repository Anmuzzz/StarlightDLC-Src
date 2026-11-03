package com.isusdlc.systems.modules.modules.other;

import com.isusdlc.elegant;
import com.isusdlc.systems.event.EventListener;
import com.isusdlc.systems.event.impl.network.ReceivePacketEvent;
import com.isusdlc.systems.modules.api.ModuleCategory;
import com.isusdlc.systems.modules.api.ModuleInfo;
import com.isusdlc.systems.modules.impl.BaseModule;
import com.isusdlc.systems.setting.settings.ModeSetting;
import com.isusdlc.utility.game.server.ServerUtility;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;

@ModuleInfo(
   name = "Auto Accept",
   category = ModuleCategory.OTHER,
   desc = "Автоматически принимает телепортацию"
)
public class AutoAccept extends BaseModule {
   private final ModeSetting acceptMode = new ModeSetting(this, "Принимать");
   private final ModeSetting.Value acceptAll = new ModeSetting.Value(this.acceptMode, "Всех");
   private final ModeSetting.Value friendsOnly = new ModeSetting.Value(this.acceptMode, "Только друзей");
   private final EventListener<ReceivePacketEvent> onReceivePacketEvent = event -> {
      if (event.getPacket() instanceof GameMessageS2CPacket packet
         && mc.player != null
         && packet.content().getString().contains("телепортироваться")
         && !ServerUtility.hasCT
         && this.canAccept(packet.content().getString())) {
         mc.player.networkHandler.sendChatCommand("tpaccept");
      }
   };

   private boolean canAccept(String message) {
      if (this.acceptMode.is(this.acceptAll)) {
         return true;
      } else {
         if (this.acceptMode.is(this.friendsOnly)) {
            String[] parts = message.split(" ");
            if (parts.length >= 2 && elegant.getInstance().getFriendManager().isFriend(parts[1])
               || elegant.getInstance()
                  .getFriendManager()
                  .isFriend(message.replace("\u0a77 просит телепортироваться к Вам.\u0a77§l [ੲ§l✔\u0a77§l]\u0a77§l [\u0a7c§l✗\u0a77§l]", "").replace("੶", ""))
               || elegant.getInstance().getFriendManager().isFriend(message.replace("➝ Ник: ", ""))) {
               return true;
            }

            if (message.contains("телепортироваться")) {
               return parts.length >= 3 && elegant.getInstance().getFriendManager().isFriend(parts[2]);
            }
         }

         return false;
      }
   }
}
