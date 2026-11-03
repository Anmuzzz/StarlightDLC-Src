package com.isusdlc.systems.modules.modules.combat;

import com.isusdlc.systems.event.EventListener;
import com.isusdlc.systems.event.impl.player.ClientPlayerTickEvent;
import com.isusdlc.systems.modules.api.ModuleCategory;
import com.isusdlc.systems.modules.api.ModuleInfo;
import com.isusdlc.systems.modules.impl.BaseModule;
import com.isusdlc.systems.setting.settings.SliderSetting;
import net.minecraft.util.hit.EntityHitResult;

@ModuleInfo(
   name = "ShiftTap",
   category = ModuleCategory.COMBAT,
   desc = "modules.descriptions.shifttap"
)
public class ShiftTap extends BaseModule {
   private boolean wasAttackPressed = false;
   private int sneakTicksRemaining = 0;
   private boolean shouldReleaseNext = false;
   private final SliderSetting intervalTicks = new SliderSetting(this, "modules.settings.shifttap.interval").step(1.0F).min(1.0F).max(10.0F).currentValue(3.0F);
   private final EventListener<ClientPlayerTickEvent> onTick = event -> {
      if (this.isEnabled()) {
         if (mc != null && mc.options != null && mc.player != null) {
            boolean isUsingItem = mc.player.isUsingItem();
            if (isUsingItem) {
               this.sneakTicksRemaining = 0;
               this.shouldReleaseNext = false;
               this.wasAttackPressed = mc.options.attackKey.isPressed();
            } else {
               boolean attackPressed = mc.options.attackKey.isPressed();
               if (this.shouldReleaseNext) {
                  mc.options.sneakKey.setPressed(false);
                  this.shouldReleaseNext = false;
               }

               if (this.sneakTicksRemaining > 0) {
                  this.sneakTicksRemaining--;
                  if (this.sneakTicksRemaining == 0) {
                     mc.options.sneakKey.setPressed(false);
                  }
               }

               if (attackPressed && !this.wasAttackPressed) {
                  boolean isEntityTarget = mc.crosshairTarget instanceof EntityHitResult;
                  if (isEntityTarget) {
                     int hold = Math.max(1, Math.round(this.intervalTicks.getCurrentValue()));
                     mc.options.sneakKey.setPressed(true);
                     if (hold == 1) {
                        this.shouldReleaseNext = true;
                     } else {
                        this.sneakTicksRemaining = hold;
                     }
                  }
               }

               this.wasAttackPressed = attackPressed;
            }
         }
      }
   };

   @Override
   public void onEnable() {
   }

   @Override
   public void onDisable() {
      if (mc != null && mc.options != null && mc.player != null && !mc.player.isUsingItem()) {
         mc.options.sneakKey.setPressed(false);
      }

      this.sneakTicksRemaining = 0;
      this.wasAttackPressed = false;
      this.shouldReleaseNext = false;
   }
}
