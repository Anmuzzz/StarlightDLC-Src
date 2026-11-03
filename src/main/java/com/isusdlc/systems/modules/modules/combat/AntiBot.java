package com.isusdlc.systems.modules.modules.combat;

import com.isusdlc.systems.event.EventListener;
import com.isusdlc.systems.event.impl.player.ClientPlayerTickEvent;
import com.isusdlc.systems.modules.api.ModuleCategory;
import com.isusdlc.systems.modules.api.ModuleInfo;
import com.isusdlc.systems.modules.impl.BaseModule;
import com.isusdlc.systems.setting.settings.BooleanSetting;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

import java.util.ArrayList;
import java.util.List;

@ModuleInfo(
   name = "Anti Bot",
   category = ModuleCategory.COMBAT,
   desc = "Убирает бота от античита"
)
public class AntiBot extends BaseModule {
    private final BooleanSetting removeWorld = new BooleanSetting(this, "Удалить из мира");
    private final List<Entity> bots = new ArrayList<>();

    private final EventListener<ClientPlayerTickEvent> onTick = event -> {
        if (mc.world == null || mc.player == null) return;
        detectBots();
    };

    private void detectBots() {
        for (PlayerEntity entity : new ArrayList<>(mc.world.getPlayers())) {
            if (entity == mc.player) continue;

            if (isBotCandidate(entity)) {
                if (!bots.contains(entity)) {
                    bots.add(entity);
                    if (removeWorld.isEnabled()) {
                        mc.world.removeEntity(entity.getId(), Entity.RemovalReason.KILLED);
                    }
                }
            } else {
                bots.remove(entity);
            }
        }
    }

    private boolean isBotCandidate(PlayerEntity entity) {
        ItemStack boots = entity.getInventory().armor.get(0);
        ItemStack leggings = entity.getInventory().armor.get(1);
        ItemStack chestplate = entity.getInventory().armor.get(2);
        ItemStack helmet = entity.getInventory().armor.get(3);

        boolean fullArmor = !boots.isEmpty() && !leggings.isEmpty() && !chestplate.isEmpty() && !helmet.isEmpty();
        boolean enchantable = boots.isEnchantable() && leggings.isEnchantable() && chestplate.isEnchantable() && helmet.isEnchantable();

        boolean validArmorTypes =
                boots.getItem() == Items.LEATHER_BOOTS || leggings.getItem() == Items.LEATHER_LEGGINGS
                        || chestplate.getItem() == Items.LEATHER_CHESTPLATE || helmet.getItem() == Items.LEATHER_HELMET
                        || boots.getItem() == Items.IRON_BOOTS || leggings.getItem() == Items.IRON_LEGGINGS
                        || chestplate.getItem() == Items.IRON_CHESTPLATE || helmet.getItem() == Items.IRON_HELMET;

        boolean offhandEmpty = entity.getOffHandStack().isEmpty();
        boolean mainHandNotEmpty = !entity.getMainHandStack().isEmpty();
        boolean armorNotDamaged = !boots.isDamaged() && !leggings.isDamaged() && !chestplate.isDamaged() && !helmet.isDamaged();
        boolean foodFull = entity.getHungerManager().getFoodLevel() == 20;

        return fullArmor && enchantable && validArmorTypes && offhandEmpty && mainHandNotEmpty && armorNotDamaged && foodFull;
    }

    public boolean check(LivingEntity entity) {
        return entity instanceof PlayerEntity && bots.contains(entity);
    }

    @Override
    public void onDisable() {
        super.onDisable();
        bots.clear();
    }
}
