package com.isusdlc.systems.modules.modules.combat;

import java.util.List;
import com.isusdlc.systems.event.EventListener;
import com.isusdlc.systems.event.impl.player.ClientPlayerTickEvent;
import com.isusdlc.systems.modules.api.ModuleCategory;
import com.isusdlc.systems.modules.api.ModuleInfo;
import com.isusdlc.systems.modules.impl.BaseModule;
import com.isusdlc.systems.setting.settings.BooleanSetting;
import com.isusdlc.systems.setting.settings.SliderSetting;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.WardenEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

@ModuleInfo(
   name = "AutoWarden",
   category = ModuleCategory.COMBAT,
   desc = "Автоматически убегает от Вардена, использует инвиз и еду"
)
public class AutoWarden extends BaseModule {
   private final SliderSetting range = new SliderSetting(this, "Range").min(10).max(64).step(1).currentValue(32);
   private final BooleanSetting autoInvis = new BooleanSetting(this, "Auto Invis").enable();
   private final BooleanSetting autoEat = new BooleanSetting(this, "Auto Eat").enable();
   private final BooleanSetting runAway = new BooleanSetting(this, "Run Away").enable();

   private final EventListener<ClientPlayerTickEvent> onTick = event -> {
      if (mc.player == null || mc.world == null) return;

      List<WardenEntity> wardens = mc.world.getEntitiesByClass(
         WardenEntity.class,
         new Box(mc.player.getBlockPos()).expand(range.getCurrentValue()),
         w -> true
      );
      if (wardens.isEmpty()) return;

      WardenEntity nearest = wardens.stream()
         .min(java.util.Comparator.comparingDouble(mc.player::distanceTo))
         .orElse(null);
      if (nearest == null) return;
      double dist = mc.player.distanceTo(nearest);
      if (dist > range.getCurrentValue()) return;

      if (autoInvis.isEnabled() && !mc.player.hasStatusEffect(StatusEffects.INVISIBILITY)) {
         for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().main.get(i);
            if (stack.getItem() == Items.POTION) {
               var contents = stack.get(DataComponentTypes.POTION_CONTENTS);
               if (contents == null) continue;
               boolean isInvisPotion = false;
               for (StatusEffectInstance effect : contents.getEffects()) {
                  if (effect.getEffectType() == StatusEffects.INVISIBILITY) {
                     isInvisPotion = true;
                     break;
                  }
               }
               if (isInvisPotion) {
                  mc.player.getInventory().selectedSlot = i;
                  mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
                  mc.player.swingHand(Hand.MAIN_HAND);
                  break;
               }
            }
         }
      }

      if (autoEat.isEnabled() && mc.player.getHungerManager().getFoodLevel() < 18) {
         for (int i = 0; i < 9; i++) {
            if (mc.player.getInventory().main.get(i).contains(DataComponentTypes.FOOD)) {
               mc.player.getInventory().selectedSlot = i;
               mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
               mc.player.swingHand(Hand.MAIN_HAND);
               break;
            }
         }
      }

      if (runAway.isEnabled()) {
         Vec3d away = mc.player.getPos().subtract(nearest.getPos()).normalize();
         mc.player.setVelocity(away.x * 0.3, mc.player.getVelocity().y, away.z * 0.3);
         float targetYaw = (float) Math.toDegrees(MathHelper.atan2(-away.x, away.z));
         mc.player.setYaw(targetYaw);
      }
   };
}
