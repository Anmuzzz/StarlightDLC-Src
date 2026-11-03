package com.isusdlc.systems.modules.modules.combat;

import com.isusdlc.systems.event.EventListener;
import com.isusdlc.systems.event.impl.player.ClientPlayerTickEvent;
import com.isusdlc.systems.modules.api.ModuleCategory;
import com.isusdlc.systems.modules.api.ModuleInfo;
import com.isusdlc.systems.modules.impl.BaseModule;
import com.isusdlc.systems.setting.settings.SliderSetting;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.slot.SlotActionType;

@ModuleInfo(
   name = "Auto Totem",
   category = ModuleCategory.COMBAT,
   desc = "modules.descriptions.auto_totem"
)
@Environment(EnvType.CLIENT)
public class AutoTotem extends BaseModule {

   private final SliderSetting health = new SliderSetting(this, "Health")
      .min(0.5F).max(20.0F).step(0.5F).currentValue(3.5F);
   private final SliderSetting delay = new SliderSetting(this, "Delay")
      .min(0.0F).max(4.0F).step(1.0F).currentValue(1.0F);

   private int swapTimer;
   private boolean swapping;

   private final EventListener<ClientPlayerTickEvent> onTick = event -> {
      if (mc.world == null || mc.player == null) return;
      if (mc.currentScreen != null && !(mc.currentScreen instanceof net.minecraft.client.gui.screen.ingame.InventoryScreen)) return;

      float hp = mc.player.getHealth() + mc.player.getAbsorptionAmount();
      ItemStack offhand = mc.player.getOffHandStack();

      if (hp <= health.getCurrentValue()) {
         if (offhand.getItem() != Items.TOTEM_OF_UNDYING) {
            int slot = findTotem();
            if (slot != -1 && !swapping) {
               swapping = true;
               swapTimer = 0;
            }
         }
      } else {
         if (offhand.getItem() == Items.TOTEM_OF_UNDYING && swapping) {
            swapping = false;
         }
      }

      if (swapping) {
         swapTimer++;
         if (swapTimer > (int)delay.getCurrentValue()) {
            int slot = findTotem();
            if (slot != -1) {
               mc.interactionManager.clickSlot(
                  mc.player.currentScreenHandler.syncId,
                  slot < 9 ? slot + 36 : slot,
                  40,
                  SlotActionType.SWAP,
                  mc.player
               );
            }
            swapping = false;
         }
      }
   };

   private int findTotem() {
      if (mc.player == null) return -1;
      for (int i = 0; i < 36; i++) {
         ItemStack stack = mc.player.getInventory().getStack(i);
         if (stack.getItem() == Items.TOTEM_OF_UNDYING) {
            return i;
         }
      }
      return -1;
   }
}
