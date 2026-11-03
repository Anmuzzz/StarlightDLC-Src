package com.isusdlc.systems.modules.modules.other;

import com.isusdlc.elegant;
import com.isusdlc.systems.event.EventListener;
import com.isusdlc.systems.event.impl.player.ClientPlayerTickEvent;
import com.isusdlc.systems.event.impl.window.KeyPressEvent;
import com.isusdlc.systems.event.impl.window.MouseEvent;
import com.isusdlc.systems.modules.api.ModuleCategory;
import com.isusdlc.systems.modules.api.ModuleInfo;
import com.isusdlc.systems.modules.impl.BaseModule;
import com.isusdlc.systems.notifications.NotificationType;
import com.isusdlc.systems.setting.settings.BindSetting;
import com.isusdlc.utility.inventory.InventoryUtility;
import com.isusdlc.utility.inventory.group.SlotGroup;
import com.isusdlc.utility.inventory.group.SlotGroups;
import com.isusdlc.utility.inventory.slots.HotbarSlot;
import net.minecraft.item.Item;
import net.minecraft.item.Items;

@ModuleInfo(
   name = "Assist",
   category = ModuleCategory.OTHER,
   desc = "Помощник для разных серверов"
)
public class Assist extends BaseModule {
   private final BindSetting dezorentKey = new BindSetting(this, "Дезориентация");
   private final BindSetting trapkaKey = new BindSetting(this, "Трапка");
   private final BindSetting smerchKey = new BindSetting(this, "Огненный смерч");
   private final BindSetting plastKey = new BindSetting(this, "Пласт");
   private final BindSetting auraKey = new BindSetting(this, "Божья аура");
   private final BindSetting pilbKey = new BindSetting(this, "Явная пыль");
   private Item pendingSwapItem = null;
   private final EventListener<MouseEvent> onMouseEvent = event -> {
      if (mc.currentScreen == null) {
         if (event.getAction() == 1) {
            this.handleButtonPress(event.getButton());
         }
      }
   };
   private final EventListener<KeyPressEvent> onKeyPressEvent = event -> {
      if (mc.currentScreen == null) {
         if (event.getAction() == 1) {
            this.handleButtonPress(event.getKey());
         }
      }
   };
   private final EventListener<ClientPlayerTickEvent> onSwapTick = event -> {
      if (this.pendingSwapItem != null && mc.player != null) {
         SlotGroup<HotbarSlot> hotbar = SlotGroups.hotbar();
         HotbarSlot slot = hotbar.findItem(this.pendingSwapItem);
         if (slot != null) {
            InventoryUtility.selectHotbarSlot(slot, false);
            elegant.getInstance()
               .getNotificationManager()
               .addNotificationOther(NotificationType.SUCCESS, "Assist", "Переключился на " + this.pendingSwapItem.getName().getString());
         } else {
            elegant.getInstance()
               .getNotificationManager()
               .addNotificationOther(
                  NotificationType.ERROR, "Предмет не найден", "Вам необходимо иметь " + this.pendingSwapItem.getName().getString() + " в хотбаре"
               );
         }

         this.pendingSwapItem = null;
      }
   };

   private void handleButtonPress(int button) {
      if (!mc.options.attackKey.isPressed()) {
         if (this.dezorentKey.isKey(button)) {
            this.switchToItem(Items.ENDER_EYE);
         } else if (this.trapkaKey.isKey(button)) {
            this.switchToItem(Items.NETHERITE_SCRAP);
         } else if (this.smerchKey.isKey(button)) {
            this.switchToItem(Items.FIRE_CHARGE);
         } else if (this.plastKey.isKey(button)) {
            this.switchToItem(Items.DRIED_KELP);
         } else if (this.auraKey.isKey(button)) {
            this.switchToItem(Items.PHANTOM_MEMBRANE);
         } else if (this.pilbKey.isKey(button)) {
            this.switchToItem(Items.SUGAR);
         }
      }
   }

   private void switchToItem(Item item) {
      if (mc.player != null) {
         if (this.pendingSwapItem == null) {
            SlotGroup<HotbarSlot> hotbar = SlotGroups.hotbar();
            HotbarSlot slot = hotbar.findItem(item);
            if (slot != null) {
               this.pendingSwapItem = item;
            } else {
               elegant.getInstance()
                  .getNotificationManager()
                  .addNotificationOther(NotificationType.ERROR, "Предмет не найден", "Вам необходимо иметь " + item.getName().getString() + " в хотбаре");
            }
         }
      }
   }
}
