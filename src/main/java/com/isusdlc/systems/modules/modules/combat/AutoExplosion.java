package com.isusdlc.systems.modules.modules.combat;

import com.isusdlc.systems.event.EventListener;
import com.isusdlc.systems.event.impl.game.AttackEvent;
import com.isusdlc.systems.event.impl.network.SendPacketEvent;
import com.isusdlc.systems.event.impl.player.ClientPlayerTickEvent;
import com.isusdlc.systems.modules.api.ModuleCategory;
import com.isusdlc.systems.modules.api.ModuleInfo;
import com.isusdlc.systems.modules.impl.BaseModule;
import com.isusdlc.systems.setting.settings.BooleanSetting;
import com.isusdlc.systems.setting.settings.SliderSetting;
import net.minecraft.block.Blocks;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@ModuleInfo(
   name = "Auto Explosion",
   category = ModuleCategory.COMBAT,
   desc = "Автоматически ставит и взрывает кристаллы"
)
public class AutoExplosion extends BaseModule {

   private final BooleanSetting protectSelf = new BooleanSetting(this, "Protect self").enable();
   private final BooleanSetting protectFriends = new BooleanSetting(this, "Protect friends").enable();
   private final SliderSetting placeRange = new SliderSetting(this, "Place range")
      .min(1.0F).max(6.0F).step(0.5F).currentValue(4.0F);
   private final SliderSetting breakRange = new SliderSetting(this, "Break range")
      .min(1.0F).max(6.0F).step(0.5F).currentValue(4.5F);

   private BlockPos targetObsidian;
   private long lastAction;
   private final List<BlockPos> placedCrystals = new ArrayList<>();

   private final EventListener<ClientPlayerTickEvent> onTick = event -> {
      if (mc.player == null || mc.world == null) return;

      long now = System.currentTimeMillis();
      if (now - lastAction < 100) return;

      if (targetObsidian == null) {
         targetObsidian = findObsidian();
      }

      if (targetObsidian != null) {
         if (mc.player.getInventory().contains(Items.END_CRYSTAL.getDefaultStack())) {
            BlockPos up = targetObsidian.up();
            if (mc.world.isAir(up) && isSafePlace(targetObsidian)) {
               placeCrystal(up);
               lastAction = now;
               return;
            }
         }
         targetObsidian = null;
      }

      breakCrystals();
   };

   private final EventListener<SendPacketEvent> onPacket = event -> {
      if (event.getPacket() instanceof PlayerInteractBlockC2SPacket packet) {
         BlockPos pos = packet.getBlockHitResult().getBlockPos();
         BlockPos above = pos.up();
         if (mc.world.getBlockState(pos).isOf(Blocks.OBSIDIAN) && mc.world.isAir(above)) {
            placedCrystals.add(pos);
         }
      }
   };

   private BlockPos findObsidian() {
      BlockPos playerPos = mc.player.getBlockPos();
      double range = placeRange.getCurrentValue();
      BlockPos.Mutable mutable = new BlockPos.Mutable();

      for (int x = (int) -range; x <= range; x++) {
         for (int y = (int) -range; y <= range; y++) {
            for (int z = (int) -range; z <= range; z++) {
               mutable.set(playerPos.getX() + x, playerPos.getY() + y, playerPos.getZ() + z);
               if (mc.world.getBlockState(mutable).isOf(Blocks.OBSIDIAN)) {
                  BlockPos up = mutable.up();
                  if (mc.world.isAir(up) && mc.player.getBlockPos().getSquaredDistance(up) <= range * range) {
                     if (isSafePlace(mutable.toImmutable())) {
                        return mutable.toImmutable();
                     }
                  }
               }
            }
         }
      }
      return null;
   }

   private boolean isSafePlace(BlockPos pos) {
      if (protectSelf.isEnabled() && mc.player.getY() > pos.getY()) return false;
      if (protectFriends.isEnabled()) {
         for (PlayerEntity player : mc.world.getPlayers()) {
            if (player != mc.player && player.getY() > pos.getY()) return false;
         }
      }
      return true;
   }

   private void placeCrystal(BlockPos pos) {
      int slot = findCrystalSlot();
      if (slot == -1) return;

      int prevSlot = mc.player.getInventory().selectedSlot;
      mc.player.getInventory().selectedSlot = slot;

      Vec3d vec = Vec3d.ofCenter(pos);
      mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND,
         new BlockHitResult(vec, Direction.UP, pos.down(), false));
      mc.player.swingHand(Hand.MAIN_HAND);

      mc.player.getInventory().selectedSlot = prevSlot;
      placedCrystals.add(pos.down());
   }

   private int findCrystalSlot() {
      for (int i = 0; i < 9; i++) {
         if (mc.player.getInventory().getStack(i).getItem() == Items.END_CRYSTAL) return i;
      }
      return -1;
   }

   private void breakCrystals() {
      double range = breakRange.getCurrentValue();
      Box box = mc.player.getBoundingBox().expand(range);

      for (EndCrystalEntity crystal : mc.world.getEntitiesByClass(EndCrystalEntity.class, box,
            e -> !e.isRemoved() && placedCrystals.contains(e.getBlockPos().down()))) {
         if (isSafeDamage(crystal)) {
            mc.interactionManager.attackEntity(mc.player, crystal);
            mc.player.swingHand(Hand.MAIN_HAND);
            lastAction = System.currentTimeMillis();
         }
      }

      placedCrystals.removeIf(pos -> {
         for (EndCrystalEntity crystal : mc.world.getEntitiesByClass(EndCrystalEntity.class, box, e -> !e.isRemoved())) {
            if (crystal.getBlockPos().down().equals(pos)) return false;
         }
         return true;
      });
   }

   private boolean isSafeDamage(EndCrystalEntity crystal) {
      BlockPos down = crystal.getBlockPos().down();
      if (protectSelf.isEnabled() && mc.player.getY() > down.getY()) return false;
      if (protectFriends.isEnabled()) {
         for (PlayerEntity player : mc.world.getPlayers()) {
            if (player != mc.player && player.getY() > down.getY()) return false;
         }
      }
      return true;
   }
}
