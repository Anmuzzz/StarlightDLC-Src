package com.isusdlc.systems.modules.modules.player;

import com.isusdlc.systems.event.EventListener;
import com.isusdlc.systems.event.impl.player.ClientPlayerTickEvent;
import com.isusdlc.systems.modules.api.ModuleCategory;
import com.isusdlc.systems.modules.api.ModuleInfo;
import com.isusdlc.systems.modules.impl.BaseModule;
import net.minecraft.util.Hand;
import net.minecraft.item.BowItem;
import net.minecraft.item.CrossbowItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.PotionItem;
import net.minecraft.item.ShieldItem;
import net.minecraft.item.TridentItem;
import net.minecraft.component.DataComponentTypes;

@ModuleInfo(
   name = "DebugShift",
   category = ModuleCategory.PLAYER,
   desc = "Исправляет прерывание использования предмета при нажатии Shift"
)
public class DebugShift extends BaseModule {
   private boolean wasUsingItem = false;
   private ItemStack lastItem = ItemStack.EMPTY;
   private Hand lastHand = Hand.MAIN_HAND;
   private int itemUseTime = 0;
   private boolean shouldContinue = false;
   private final EventListener<ClientPlayerTickEvent> onTick = event -> {
      if (this.isEnabled()) {
         if (mc != null && mc.player != null) {
            boolean currentlyUsingItem = mc.player.isUsingItem();
            if (currentlyUsingItem && !this.wasUsingItem) {
               this.lastItem = mc.player.getActiveItem().copy();
               this.lastHand = mc.player.getActiveHand();
               this.itemUseTime = mc.player.getItemUseTimeLeft();
               this.shouldContinue = false;
            }

            if (currentlyUsingItem) {
               this.itemUseTime = mc.player.getItemUseTimeLeft();
            }

            if (!currentlyUsingItem && this.wasUsingItem && this.itemUseTime > 0) {
               ItemStack currentItem = mc.player.getStackInHand(this.lastHand);
               if (ItemStack.areItemsEqual(this.lastItem, currentItem) && !currentItem.isEmpty() && this.canUseItem(currentItem)) {
                  this.shouldContinue = true;
               }
            }

            if (this.shouldContinue && !currentlyUsingItem) {
               ItemStack currentItem = mc.player.getStackInHand(this.lastHand);
               if (ItemStack.areItemsEqual(this.lastItem, currentItem) && !currentItem.isEmpty()) {
                  mc.interactionManager.interactItem(mc.player, this.lastHand);
                  this.shouldContinue = false;
               } else {
                  this.shouldContinue = false;
               }
            }

            this.wasUsingItem = currentlyUsingItem;
         }
      }
   };

   public DebugShift() {
      this.setEnabled(false, true);
   }

   @Override
   public void onEnable() {
      this.reset();
   }

   @Override
   public void onDisable() {
      this.reset();
   }

   private void reset() {
      this.wasUsingItem = false;
      this.lastItem = ItemStack.EMPTY;
      this.itemUseTime = 0;
      this.shouldContinue = false;
   }

   private boolean canUseItem(ItemStack itemStack) {
      if (itemStack.isEmpty()) {
         return false;
      } else if (itemStack.getComponents().contains(DataComponentTypes.FOOD)) {
         return true;
      } else if (itemStack.getItem() instanceof BowItem
         || itemStack.getItem() instanceof CrossbowItem
         || itemStack.getItem() instanceof TridentItem) {
         return true;
      } else if (itemStack.getItem() instanceof ShieldItem) {
         return true;
      } else {
         return itemStack.getItem() instanceof PotionItem ? true : itemStack.getMaxUseTime(mc.player) > 0;
      }
   }
}
