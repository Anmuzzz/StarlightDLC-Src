package com.isusdlc.systems.modules.modules.other;
import com.isusdlc.systems.event.EventListener;
import com.isusdlc.systems.setting.settings.ModeSetting;
import com.isusdlc.systems.setting.settings.SliderSetting;
import com.isusdlc.systems.setting.settings.BooleanSetting;

import com.isusdlc.systems.event.EventListener;
import com.isusdlc.systems.event.impl.network.ReceivePacketEvent;
import com.isusdlc.systems.modules.api.ModuleCategory;
import com.isusdlc.systems.modules.api.ModuleInfo;
import com.isusdlc.systems.modules.impl.BaseModule;
import com.isusdlc.systems.setting.settings.BooleanSetting;
import com.isusdlc.systems.setting.settings.ModeSetting;
import com.isusdlc.systems.setting.settings.SliderSetting;
import com.isusdlc.systems.setting.settings.StringSetting;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.SlotActionType;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@ModuleInfo(
   name = "Auto Duel",
   category = ModuleCategory.OTHER,
   desc = "Автоматически кидает запрос на дуель"
)
public class AutoDuel extends BaseModule {
   private final Pattern pattern = Pattern.compile("^\\w{3,16}$");

   private final ModeSetting mode = new ModeSetting(this, "Режим");
   private final ModeSetting.Value shield = new ModeSetting.Value(mode, "Щит");
   private final ModeSetting.Value spikes3 = new ModeSetting.Value(mode, "Шипы 3");
   private final ModeSetting.Value bow = new ModeSetting.Value(mode, "Лук");
   private final ModeSetting.Value totems = new ModeSetting.Value(mode, "Тотемы");
   private final ModeSetting.Value nodebuff = new ModeSetting.Value(mode, "Нодебафф");
   private final ModeSetting.Value balls = new ModeSetting.Value(mode, "Шары").select();
   private final ModeSetting.Value classic = new ModeSetting.Value(mode, "Классик");
   private final ModeSetting.Value cheater = new ModeSetting.Value(mode, "Читерский рай");
   private final ModeSetting.Value netherite = new ModeSetting.Value(mode, "Незеритка");

   private final SliderSetting slowTime = new SliderSetting(this, "Скорость отправки")
      .min(300F).max(1000F).step(100F).currentValue(500F);
   private final BooleanSetting babki = new BooleanSetting(this, "Играть на деньги");
   private final StringSetting money = new StringSetting(this, "Монет", () -> !babki.isEnabled()).text("10000");

   private final List<String> sent = new ArrayList<>();
   private long lastSendTime;
   private long lastResetTime;
   private long lastChoiceTime;
   private long lastToTime;
   private double lastPosX;
   private double lastPosY;
   private double lastPosZ;

   private final EventListener<ReceivePacketEvent> onPacketReceive = event -> {
      if (event.getPacket() instanceof GameMessageS2CPacket packet) {
         String text = packet.content().getString();
         if ((text.contains("начало") && text.contains("через") && text.contains("секунд!"))
            || text.equals("дуэли » во время поединка запрещено использовать команды")) {
            toggle();
         }
      }
   };

   @Override
   public void onEnable() {
      lastSendTime = 0L;
      lastResetTime = 0L;
      lastChoiceTime = 0L;
      lastToTime = 0L;
      if (mc.player != null) {
         lastPosX = mc.player.getX();
         lastPosY = mc.player.getY();
         lastPosZ = mc.player.getZ();
      }
      sent.clear();
      super.onEnable();
   }

   @Override
   public void tick() {
      if (mc.player == null || mc.world == null) return;

      long now = System.currentTimeMillis();
      List<String> players = getOnlinePlayers();

      double distance = Math.sqrt(
         Math.pow(lastPosX - mc.player.getX(), 2)
            + Math.pow(lastPosY - mc.player.getY(), 2)
            + Math.pow(lastPosZ - mc.player.getZ(), 2)
      );
      if (distance > 500.0) {
         toggle();
         return;
      }

      lastPosX = mc.player.getX();
      lastPosY = mc.player.getY();
      lastPosZ = mc.player.getZ();

      if (lastResetTime == 0L || now - lastResetTime > 800L * players.size()) {
         sent.clear();
         lastResetTime = now;
      }

      for (String player : players) {
         if (!sent.contains(player) && !player.equals(mc.player.getGameProfile().getName())) {
            if (lastSendTime == 0L || now - lastSendTime >= (long) slowTime.getCurrentValue()) {
               if (babki.isEnabled()) {
                  mc.player.networkHandler.sendCommand("duel " + player + " " + money.getText());
               } else {
                  mc.player.networkHandler.sendCommand("duel " + player);
               }
               sent.add(player);
               lastSendTime = now;
            }
         }
      }

      if (mc.currentScreen != null && mc.player.currentScreenHandler instanceof ScreenHandler chest) {
         String title = mc.currentScreen.getTitle().getString();
         if (title.contains("Выбор набора (1/1)")) {
            if (lastChoiceTime == 0L || now - lastChoiceTime >= 150L) {
               int slotID = -1;
               if (mode.is(shield)) slotID = 0;
               else if (mode.is(spikes3)) slotID = 1;
               else if (mode.is(bow)) slotID = 2;
               else if (mode.is(totems)) slotID = 3;
               else if (mode.is(nodebuff)) slotID = 4;
               else if (mode.is(balls)) slotID = 5;
               else if (mode.is(classic)) slotID = 6;
               else if (mode.is(cheater)) slotID = 7;
               else if (mode.is(netherite)) slotID = 8;

               if (slotID >= 0) {
                  mc.interactionManager.clickSlot(
                     mc.player.currentScreenHandler.syncId,
                     slotID, 0, SlotActionType.QUICK_MOVE, mc.player
                  );
               }
               lastChoiceTime = now;
            }
         } else if (title.contains("Настройка поединка")) {
            if (lastToTime == 0L || now - lastToTime >= 150L) {
               mc.interactionManager.clickSlot(chest.syncId, 0, 0, SlotActionType.QUICK_MOVE, mc.player);
               lastToTime = now;
            }
         }
      }
   }

   private List<String> getOnlinePlayers() {
      return mc.player.networkHandler.getPlayerList().stream()
         .map(entry -> entry.getProfile().getName())
         .filter(name -> pattern.matcher(name).matches())
         .collect(Collectors.toList());
   }
}
