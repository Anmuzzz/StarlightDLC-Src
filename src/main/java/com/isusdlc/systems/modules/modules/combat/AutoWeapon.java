package com.isusdlc.systems.modules.modules.combat;

import com.isusdlc.systems.event.EventListener;
import com.isusdlc.systems.event.impl.window.MouseEvent;
import com.isusdlc.systems.modules.api.ModuleCategory;
import com.isusdlc.systems.modules.api.ModuleInfo;
import com.isusdlc.systems.modules.impl.BaseModule;
import com.isusdlc.systems.setting.settings.BooleanSetting;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.AttributeModifierSlot;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;

@ModuleInfo(
   name = "Auto Weapon",
   category = ModuleCategory.COMBAT,
   desc = "Автоматически переключает на лучшее оружие при атаке"
)
public class AutoWeapon extends BaseModule {

   private final BooleanSetting silent = new BooleanSetting(this, "Silent");

   private final EventListener<MouseEvent> onMouseEvent = event -> {
      if (mc.player == null || mc.world == null) return;
      if (event.getAction() != 1) return;
      if (event.getButton() != 0) return;
      if (mc.options.attackKey.isPressed()) return;

      int slot = findBestWeaponSlot();
      if (slot != -1 && slot != mc.player.getInventory().selectedSlot) {
         mc.player.getInventory().selectedSlot = slot;
         if (!silent.isEnabled()) {
            mc.player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(slot));
         }
      }
   };

   private int findBestWeaponSlot() {
      int best = -1;
      float bestDmg = 1;
      for (int i = 0; i < 9; i++) {
         ItemStack stack = mc.player.getInventory().getStack(i);
         float dmg = getWeaponDamage(stack);
         if (dmg > bestDmg) {
            bestDmg = dmg;
            best = i;
         }
      }
      return best;
   }

   private float getWeaponDamage(ItemStack stack) {
      if (stack.isEmpty()) return 0;
      var comp = stack.get(DataComponentTypes.ATTRIBUTE_MODIFIERS);
      if (comp == null) return 1;
      float dmg = 1;
      for (var entry : comp.modifiers()) {
         if ((entry.slot() == AttributeModifierSlot.MAINHAND || entry.slot() == AttributeModifierSlot.HAND)
            && entry.attribute() == EntityAttributes.ATTACK_DAMAGE
            && entry.modifier().operation() == EntityAttributeModifier.Operation.ADD_VALUE) {
            dmg += (float) entry.modifier().value();
         }
      }
      return dmg;
   }
}
