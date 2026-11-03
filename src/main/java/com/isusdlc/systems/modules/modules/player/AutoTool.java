package com.isusdlc.systems.modules.modules.player;
import com.isusdlc.systems.event.EventListener;

import com.isusdlc.systems.event.EventListener;
import com.isusdlc.systems.event.impl.player.ClientPlayerTickEvent;
import com.isusdlc.systems.modules.api.ModuleCategory;
import com.isusdlc.systems.modules.api.ModuleInfo;
import com.isusdlc.systems.modules.impl.BaseModule;
import net.minecraft.block.AirBlock;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.item.ItemStack;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.List;

@ModuleInfo(name = "AutoTool", category = ModuleCategory.PLAYER, desc = "Берёт в руку лучший инструмент для ломания")
public class AutoTool extends BaseModule {

    public static int itemIndex;
    private boolean swap;
    private long swapDelay;
    private final List<Integer> lastItem = new ArrayList<>();

    private final EventListener<ClientPlayerTickEvent> listener = event -> {
        BlockPos pos = null;
        if (mc.crosshairTarget instanceof BlockHitResult result) {
            BlockPos hitPos = result.getBlockPos();
            if (hitPos != null && !mc.world.getBlockState(hitPos).isAir()) {
                pos = hitPos;
            }
        }

        int toolSlot = pos == null ? -1 : getBest(pos);

        if (toolSlot != -1 && mc.options.attackKey.isPressed()) {
            if (!swap) {
                lastItem.clear();
                lastItem.add(mc.player.getInventory().selectedSlot);
            }
            mc.player.getInventory().selectedSlot = toolSlot;
            itemIndex = toolSlot;
            swap = true;
            swapDelay = System.currentTimeMillis();
        } else if (swap && !lastItem.isEmpty() && System.currentTimeMillis() >= swapDelay + 200) {
            mc.player.getInventory().selectedSlot = lastItem.get(0);
            itemIndex = lastItem.get(0);
            lastItem.clear();
            swap = false;
        }
    };

    public static int getBest(final BlockPos pos) {
        if (pos == null) return -1;

        int index = -1;
        float currentFastest = 1.0f;

        for (int i = 0; i < 9; ++i) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (stack == ItemStack.EMPTY) continue;
            if (stack.getMaxDamage() - stack.getDamage() <= 10) continue;

            float digSpeed = EnchantmentHelper.getLevel(
                mc.world.getRegistryManager()
                    .getOrThrow(Enchantments.EFFICIENCY.getRegistryRef())
                    .getEntry(Enchantments.EFFICIENCY.getValue()).get(),
                stack);
            float destroySpeed = stack.getMiningSpeedMultiplier(mc.world.getBlockState(pos));

            if (mc.world.getBlockState(pos).getBlock() instanceof AirBlock) continue;

            if (digSpeed + destroySpeed > currentFastest) {
                currentFastest = digSpeed + destroySpeed;
                index = i;
            }
        }

        return index;
    }
}
