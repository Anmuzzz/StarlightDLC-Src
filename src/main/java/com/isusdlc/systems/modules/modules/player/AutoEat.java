package com.isusdlc.systems.modules.modules.player;

import java.util.function.Predicate;
import com.isusdlc.mixin.minecraft.client.IMinecraftClient;
import com.isusdlc.systems.event.EventListener;
import com.isusdlc.systems.event.impl.player.ClientPlayerTickEvent;
import com.isusdlc.systems.modules.api.ModuleCategory;
import com.isusdlc.systems.modules.api.ModuleInfo;
import com.isusdlc.systems.modules.impl.BaseModule;
import com.isusdlc.systems.setting.settings.SliderSetting;
import com.isusdlc.utility.inventory.ItemSlot;
import com.isusdlc.utility.inventory.group.SlotGroup;
import com.isusdlc.utility.inventory.group.SlotGroups;
import net.minecraft.item.ItemStack;
import net.minecraft.component.DataComponentTypes;

@ModuleInfo(
   name = "Auto Eat",
   category = ModuleCategory.PLAYER,
   desc = "Автоматически ест при низком уровне голода"
)
public class AutoEat extends BaseModule {
   private boolean eating;
   private final SliderSetting food = new SliderSetting(this, "modules.settings.auto_eat.food").step(1.0F).min(1.0F).max(20.0F).currentValue(15.0F);
   private final EventListener<ClientPlayerTickEvent> onUpdateEvent = event -> {
      if (mc.player.getHungerManager().getFoodLevel() <= this.food.getCurrentValue()) {
         SlotGroup<ItemSlot> search = SlotGroups.inventory().and(SlotGroups.hotbar());
         ItemSlot foodSlot = search.findItem((Predicate<ItemStack>)(stack -> stack.getItem().getDefaultStack().contains(DataComponentTypes.FOOD)));
         if (!mc.player.getOffHandStack().contains(DataComponentTypes.FOOD) && foodSlot != null) {
            foodSlot.moveToOffHand();
         }

         this.eating = true;
         if (mc.currentScreen != null && !mc.player.isUsingItem()) {
            ((IMinecraftClient)mc).idoItemUse();
         } else {
            mc.options.useKey.setPressed(true);
         }
      } else if (this.eating) {
         this.eating = false;
         mc.options.useKey.setPressed(false);
      }
   };

   @Override
   public void onDisable() {
      super.onDisable();
      if (mc.player != null) {
         mc.options.useKey.setPressed(false);
      }
      this.eating = false;
   }
}
