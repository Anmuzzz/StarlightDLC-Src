package com.isusdlc.ui.hud.impl.island.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import com.isusdlc.framework.base.CustomDrawContext;
import com.isusdlc.systems.setting.settings.SelectSetting;
import com.isusdlc.ui.hud.impl.island.TimerStatus;
import com.isusdlc.utility.colors.ColorRGBA;
import com.isusdlc.utility.game.server.ServerUtility;
import com.isusdlc.utility.interfaces.IMinecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.util.math.Vec3d;
import net.minecraft.text.Text;

public class MineStatus extends TimerStatus implements IMinecraft {
   private static final Pattern TIME_PATTERN = Pattern.compile("\\d{1,2}:\\d{2}");
   private static final List<String> HW_TYPES = List.of("обычная", "редкая", "эпическая", "легендарная", "мифическая");
   private static final long SCAN_CACHE_MS = 500L;

   private Vec3d vec = new Vec3d(-52.0, 87.0, 3.0);
   private List<ArmorStandEntity> cachedStands = List.of();
   private Vec3d cacheVec;
   private long cacheTime;

   public MineStatus(SelectSetting setting) {
      super(setting, "mine");
   }

   @Override
   public void draw(CustomDrawContext context) {
      if (mc.world == null || mc.player == null || !ServerUtility.spawn()) return;

      String mineType = "";
      int timeMin = -1;
      int timeSec = -1;

      List<ArmorStandEntity> nearbyStands = this.getStands();

      for (int i = 0; i < nearbyStands.size(); i++) {
         ArmorStandEntity armorStand = nearbyStands.get(i);
         Text customName = armorStand.getCustomName();
         if (customName != null) {
            String name = customName.getString().trim();
            if (TIME_PATTERN.matcher(name).matches()) {
               int colon = name.indexOf(58);
               try {
                  timeMin = Integer.parseInt(name.substring(0, colon));
                  timeSec = Integer.parseInt(name.substring(colon + 1));
               } catch (NumberFormatException ignored) {
                  timeMin = -1;
               }
            } else if (name.contains("осталось:")) {
               int index = name.indexOf(58);
               if (index != -1 && index + 2 < name.length()) {
                  String timeStr = name.substring(index + 2).trim();
                  int minIndex = timeStr.indexOf(" мин.");
                  int secIndex = timeStr.indexOf(" сек.");
                  if (minIndex != -1 && secIndex != -1) {
                     try {
                        timeMin = Integer.parseInt(timeStr.substring(0, minIndex).trim());
                        timeSec = Integer.parseInt(timeStr.substring(minIndex + 5, secIndex).trim());
                     } catch (NumberFormatException ignored) {
                        timeMin = -1;
                     }
                  }
               }
            } else if (name.startsWith("Следующая:")) {
               int index = name.indexOf(58);
               if (index != -1 && index + 2 < name.length()) {
                  mineType = name.substring(index + 2).trim();
               }
            } else if (name.equals("Следующая шахта:") && i + 1 < nearbyStands.size()) {
               ArmorStandEntity nextStand = nearbyStands.get(i + 1);
               Text nextName = nextStand.getCustomName();
               if (nextName != null) {
                  String nextNameStr = nextName.getString().trim();
                  if (HW_TYPES.contains(nextNameStr.toLowerCase().trim())) {
                     mineType = nextNameStr;
                  }
               }
            }

            if (timeMin >= 0 && !mineType.isEmpty()) {
               break;
            }
         }
      }

      if (timeMin < 0 || mineType.isEmpty()) return;

      ColorRGBA color;
      if (ServerUtility.is("holyworld")) {
         String type = mineType.trim().toLowerCase();
         color = switch (type) {
            case "легендарная" -> new ColorRGBA(0.0F, 128.0F, 250.0F);
            case "эпическая" -> new ColorRGBA(231.0F, 0.0F, 250.0F);
            default -> new ColorRGBA(243.0F, 151.0F, 250.0F);
         };
      } else {
         String type = mineType.trim().toLowerCase();
         color = switch (type) {
            case "легендарная" -> new ColorRGBA(84.0F, 152.0F, 152.0F);
            case "мифическая" -> new ColorRGBA(252.0F, 84.0F, 252.0F);
            default -> new ColorRGBA(252.0F, 168.0F, 0.0F);
         };
      }

      this.update(timeMin + ":", "", timeSec, mineType, color);
      super.draw(context);
      this.timeAnim.settings(true, ColorRGBA.WHITE);
   }

   @Override
   public boolean canShow() {
      if (mc.world != null && mc.player != null && ServerUtility.spawn()) {
         if (ServerUtility.is("holyworld")) {
            this.vec = new Vec3d(23.0, 41.0, -156.0);
         } else {
            this.vec = new Vec3d(-52.0, 87.0, 3.0);
         }

         return !this.getStands().isEmpty();
      }

      return false;
   }

   /** Один кэшированный обход сущностей на 500 мс — раньше сканировалось дважды за кадр. */
   private List<ArmorStandEntity> getStands() {
      long now = System.currentTimeMillis();
      if (this.cacheVec == null || !this.cacheVec.equals(this.vec) || now - this.cacheTime > SCAN_CACHE_MS) {
         List<ArmorStandEntity> found = new ArrayList<>();
         for (Entity entity : mc.world.getEntities()) {
            if (entity instanceof ArmorStandEntity armorStand && armorStand.isAlive() && this.near(armorStand, this.vec)) {
               found.add(armorStand);
            }
         }

         found.sort((a, b) -> Double.compare(b.getY(), a.getY()));
         this.cachedStands = found;
         this.cacheVec = this.vec;
         this.cacheTime = now;
      }

      return this.cachedStands;
   }

   private boolean near(ArmorStandEntity a, Vec3d v) {
      return Math.abs(a.getX() - v.x) <= 2.0
         && Math.abs(a.getY() - v.y) <= 2.0
         && Math.abs(a.getZ() - v.z) <= 2.0;
   }
}
