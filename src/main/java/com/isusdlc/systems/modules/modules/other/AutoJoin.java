package com.isusdlc.systems.modules.modules.other;

import com.isusdlc.elegant;
import com.isusdlc.systems.event.EventListener;
import com.isusdlc.systems.event.impl.network.ReceivePacketEvent;
import com.isusdlc.systems.event.impl.player.ClientPlayerTickEvent;
import com.isusdlc.systems.modules.api.ModuleCategory;
import com.isusdlc.systems.modules.api.ModuleInfo;
import com.isusdlc.systems.modules.impl.BaseModule;
import com.isusdlc.systems.notifications.NotificationType;
import com.isusdlc.systems.setting.settings.ModeSetting;
import com.isusdlc.utility.game.server.ServerUtility;
import com.isusdlc.utility.inventory.group.SlotGroup;
import com.isusdlc.utility.inventory.group.SlotGroups;
import com.isusdlc.utility.inventory.slots.HotbarSlot;
import com.isusdlc.utility.time.Timer;
import net.minecraft.util.Hand;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;

@ModuleInfo(
   name = "Auto Join",
   category = ModuleCategory.OTHER,
   desc = "Автоматически заходит на режим"
)
public class AutoJoin extends BaseModule {
   private final String griefString = "306";
   private final ModeSetting mode = new ModeSetting(this, "Режим");
   private final ModeSetting.Value duels = new ModeSetting.Value(this.mode, "Дуэли SpookyTime").select();
   private final ModeSetting.Value grief = new ModeSetting.Value(this.mode, "Гриф RW/FT/Spooky");
   private final Timer timer = new Timer();
   private final EventListener<ClientPlayerTickEvent> onUpdateEvent = event -> {
      if (ServerUtility.isST() && this.duels.isSelected()) {
         SlotGroup<HotbarSlot> search = SlotGroups.hotbar();
         HotbarSlot compass = search.findItem(Items.COMPASS);
         HotbarSlot sword = search.findItem(Items.DIAMOND_SWORD);
         if (compass != null && this.timer.finished(300L)) {
            mc.player.getInventory().selectedSlot = compass.getSlotId();
            mc.interactionManager
               .sendSequencedPacket(
                  mc.world, sequence -> new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, sequence, mc.player.getYaw(), mc.player.getPitch())
               );
            if (mc.player.currentScreenHandler instanceof GenericContainerScreenHandler && mc.currentScreen.getTitle().getString().contains("Выберите режим")) {
               mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, 14, 0, SlotActionType.QUICK_MOVE, mc.player);
            }

            this.timer.reset();
         }

         if (sword != null) {
            elegant.getInstance().getNotificationManager().addNotificationOther(NotificationType.SUCCESS, "Успешный вход", "Вы успешно вошли на дуэли");
            this.toggle();
         }
      }

      if ((ServerUtility.isFT() || ServerUtility.isRW() || ServerUtility.isST()) && this.grief.isSelected() && this.timer.finished(500L)) {
         this.timer.reset();
         mc.player.networkHandler.sendChatCommand("an" + this.griefString);
      }
   };
   private final EventListener<ReceivePacketEvent> onReceivePacketEvent = event -> {
      if (event.getPacket() instanceof GameMessageS2CPacket packet && (ServerUtility.isFT() || ServerUtility.isRW() || ServerUtility.isST()) && this.grief.isSelected()) {
         String message = packet.content().getString().toLowerCase();
         if (ServerUtility.isFT() ? message.contains("вы уже подключены к этому серверву!") : message.contains("вы уже подключены на этот сервер!")) {
            this.toggle();
         }
      }
   };
}
