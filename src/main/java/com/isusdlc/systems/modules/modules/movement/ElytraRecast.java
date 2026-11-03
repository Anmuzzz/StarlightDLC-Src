package com.isusdlc.systems.modules.modules.movement;
import com.isusdlc.systems.event.EventListener;
import com.isusdlc.systems.setting.settings.SliderSetting;
import com.isusdlc.systems.setting.settings.BooleanSetting;

import com.isusdlc.systems.event.EventListener;
import com.isusdlc.systems.event.impl.player.ClientPlayerTickEvent;
import com.isusdlc.systems.modules.api.ModuleCategory;
import com.isusdlc.systems.modules.api.ModuleInfo;
import com.isusdlc.systems.modules.impl.BaseModule;
import com.isusdlc.systems.setting.settings.BooleanSetting;
import com.isusdlc.systems.setting.settings.SliderSetting;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;

@ModuleInfo(
   name = "Elytra Recast",
   category = ModuleCategory.MOVEMENT,
   desc = "Автоматический перезапуск полёта на элитрах"
)
public class ElytraRecast extends BaseModule {
   private final BooleanSetting changePitch = new BooleanSetting(this, "Change Pitch").enable();
   private final SliderSetting pitchValue = new SliderSetting(this, "Pitch value", () -> !this.changePitch.isEnabled())
      .min(-90.0F).max(90.0F).step(1.0F).currentValue(55.0F);
   private final BooleanSetting autoJump = new BooleanSetting(this, "Auto Jump").enable();

   @Override
   public void onDisable() {
      if (mc.options.forwardKey.isPressed()) {
         mc.options.forwardKey.setPressed(false);
      }
      if (mc.options.jumpKey.isPressed()) {
         mc.options.jumpKey.setPressed(false);
      }
   }

   private final EventListener<ClientPlayerTickEvent> onTick = event -> {
      if (mc.player == null || mc.world == null) return;

      if (this.changePitch.isEnabled() && !mc.player.isGliding() && checkElytra()) {
         mc.player.setPitch(this.pitchValue.getCurrentValue());
      }

      if (!mc.player.isGliding() && checkElytra()) {
         if (this.autoJump.isEnabled() && mc.player.isOnGround()) {
            mc.player.jump();
         }
      }

      if (!mc.player.isGliding() && mc.player.fallDistance > 0 && checkElytra()) {
         castElytra();
      }
   };

   private void castElytra() {
      if (checkElytra() && check()) {
         mc.player.networkHandler.sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.START_FALL_FLYING));
      }
   }

   private boolean checkElytra() {
      if (mc.player == null) return false;
      ItemStack chest = mc.player.getEquippedStack(EquipmentSlot.CHEST);
      return chest.getItem() == Items.ELYTRA
         && isUsable(chest)
         && !mc.player.getAbilities().flying
         && mc.player.getVehicle() == null
         && !mc.player.isClimbing();
   }

   private static boolean isUsable(ItemStack stack) {
      if (stack == null || stack.isEmpty()) return false;
      if (stack.getItem() != Items.ELYTRA) return false;
      return stack.getDamage() < stack.getMaxDamage() - 1;
   }

   private boolean check() {
      return !mc.player.isCreative()
         && !mc.player.isSpectator()
         && !mc.player.hasStatusEffect(StatusEffects.LEVITATION);
   }
}
