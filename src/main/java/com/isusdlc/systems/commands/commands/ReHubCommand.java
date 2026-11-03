package com.isusdlc.systems.commands.commands;

import com.isusdlc.elegant;
import com.isusdlc.systems.commands.Command;
import com.isusdlc.systems.commands.CommandBuilder;
import com.isusdlc.systems.commands.CommandContext;
import com.isusdlc.systems.event.EventListener;
import com.isusdlc.systems.event.impl.player.ClientPlayerTickEvent;
import com.isusdlc.systems.localization.Localizator;
import com.isusdlc.utility.game.MessageUtility;
import com.isusdlc.utility.game.server.ServerUtility;
import com.isusdlc.utility.interfaces.IMinecraft;
import com.isusdlc.utility.time.Timer;
import net.minecraft.world.Difficulty;
import net.minecraft.text.Text;

public class ReHubCommand implements IMinecraft {
   private boolean processing;
   private final Timer timer = new Timer();
   private final EventListener<ClientPlayerTickEvent> onUpdateEvent = event -> {
      if (this.processing
         && mc.world != null
         && mc.player != null
         && (ServerUtility.isFT() || ServerUtility.isRW() || ServerUtility.isFS())
         && mc.world.getDifficulty() == Difficulty.EASY
         && this.timer.finished(1000L)) {
         mc.player.networkHandler.sendChatCommand("an" + ServerUtility.ftAn);
         this.timer.reset();
         this.processing = false;
      }
   };

   public ReHubCommand() {
      elegant.getInstance().getEventManager().subscribe(this);
   }

   public Command command() {
      return CommandBuilder.begin("rct").aliases("reconnect").desc("commands.rehub.description").handler(this::handle).build();
   }

   private void handle(CommandContext ctx) {
      if (mc.player != null && mc.world != null) {
         if (ServerUtility.hasCT) {
            MessageUtility.error(Text.of(Localizator.translate("commands_rehub.ct")));
         } else {
            this.timer.reset();
            mc.player.networkHandler.sendChatCommand("hub");
            this.processing = true;
         }
      }
   }
}
