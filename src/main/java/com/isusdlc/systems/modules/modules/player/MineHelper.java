package com.isusdlc.systems.modules.modules.player;

import com.isusdlc.systems.event.EventListener;
import com.isusdlc.systems.event.impl.game.StartBreakBlockEvent;
import com.isusdlc.systems.modules.api.ModuleCategory;
import com.isusdlc.systems.modules.api.ModuleInfo;
import com.isusdlc.systems.modules.impl.BaseModule;
import com.isusdlc.systems.setting.settings.BooleanSetting;
import com.isusdlc.systems.setting.settings.SliderSetting;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.PickaxeItem;

@ModuleInfo(
   name = "Mine Helper",
   category = ModuleCategory.PLAYER,
   desc = "Помощник в шахте"
)
public class MineHelper extends BaseModule {
   private final BooleanSetting save = new BooleanSetting(this, "Сохранять кирку", "Не дает сломать блок, если предмет достиг определенной прочности").enable();
   public final SliderSetting percent = new SliderSetting(this, "Прочность").step(1.0F).min(1.0F).max(70.0F).currentValue(10.0F).suffix("%%");
   private final EventListener<StartBreakBlockEvent> onStartBreakBlockEvent = event -> {
      if (mc.player != null) {
         ItemStack currentStack = mc.player.getMainHandStack();
         if (this.isValidPickaxe(currentStack)) {
            double durabilityPercent = this.getDurabilityPercent(currentStack);
            if (this.save.isEnabled() && !(durabilityPercent >= this.percent.getCurrentValue())) {
               event.cancel();
            }
         }
      }
   };
   private boolean isValidPickaxe(ItemStack stack) {
      return stack != null && stack.isDamageable() && stack.getItem() instanceof PickaxeItem;
   }

   private double getDurabilityPercent(ItemStack stack) {
      return (double)(stack.getMaxDamage() - stack.getDamage()) / stack.getMaxDamage() * 100.0;
   }
}
