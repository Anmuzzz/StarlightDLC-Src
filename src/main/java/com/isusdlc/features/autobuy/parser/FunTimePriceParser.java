package com.isusdlc.features.autobuy.parser;

import com.isusdlc.features.autobuy.CommandSender;
import com.isusdlc.features.autobuy.items.AutoBuyableItem;
import com.isusdlc.features.autobuy.manager.AutoBuyManager;
import com.isusdlc.features.autobuy.originalitems.ItemRegistry;
import com.isusdlc.features.autobuy.util.AuctionUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.client.MinecraftClient;
import net.minecraft.screen.slot.Slot;

public final class FunTimePriceParser {
   private static volatile boolean enabled = false;
   private static volatile int discountPercent = 0;
   private static FunTimePriceParser.Stage stage = FunTimePriceParser.Stage.IDLE;
   private static long stageUntilMs = 0L;
   private static int currentIndex = 0;
   private static List<String> items = new ArrayList<>();
   private static String currentItemName = null;
   private static long lastEmptyItemsNotifyMs = 0L;
   private static int retryCount = 0;
   private static final int MAX_RETRIES = 10;
   private static final Pattern LEVEL_PATTERN = Pattern.compile("(.+?)\\s*\\[(\\d+)\\s*[уУ]р\\.?\\]");

   private FunTimePriceParser() {
   }

   public static boolean isEnabled() {
      return enabled;
   }

   public static void setEnabledUIOnly(boolean var0) {
      enabled = var0;
      log("[Parser] UI sync: " + (var0 ? "ON" : "OFF"));
   }

   public static void setEnabled(boolean var0) {
      if (enabled != var0) {
         if (var0) {
            List var1 = resolveItemsToParse();
            if (var1 == null || var1.isEmpty()) {
               log("[Parser] Start requested, but item list is empty -> OFF");
               enabled = false;
               stage = FunTimePriceParser.Stage.IDLE;
               stageUntilMs = 0L;
               currentIndex = 0;
               currentItemName = null;
               items = new ArrayList<>();
               return;
            }

            enabled = true;
            stage = FunTimePriceParser.Stage.SEND_SEARCH;
            stageUntilMs = 0L;
            currentIndex = 0;
            currentItemName = null;
            items = var1;
            log("[Parser] ON. Discount=" + discountPercent + "%, items=" + items.size());
            AutoBuyManager.getInstance().setEnabled(false);
         } else {
            log("[Parser] OFF");
            enabled = false;
            stage = FunTimePriceParser.Stage.IDLE;
            stageUntilMs = 0L;
            currentIndex = 0;
            currentItemName = null;
         }
      }
   }

   public static int getDiscountPercent() {
      return discountPercent;
   }

   public static String getCurrentItemName() {
      return currentItemName;
   }

   public static int findCheapestPerItemStatic(List<Slot> var0, String var1) {
      return findCheapestPerItem(var0, var1);
   }

   public static void setDiscountPercent(int var0) {
      int var1 = discountPercent;
      discountPercent = Math.max(0, Math.min(100, var0));
      if (var1 != discountPercent) {
         log("[Parser] Скидка изменена: " + var1 + "% -> " + discountPercent + "%");
      }
   }

   public static void tickNoContainer(MinecraftClient var0) {
      if (enabled) {
         if (var0 != null && var0.player != null) {
            if (stage != FunTimePriceParser.Stage.IDLE && stage != FunTimePriceParser.Stage.FINISH_WAIT) {
               long var3 = System.currentTimeMillis();
               if (var3 >= stageUntilMs) {
                  if (currentItemName != null && !currentItemName.isEmpty()) {
                     retryCount++;
                     if (retryCount < 10) {
                        String var5 = cleanSearchName(currentItemName);
                        log("[Parser] Контейнер не открыт, повтор (" + retryCount + "/10): /ah search " + var5);
                        CommandSender.sendCommand(var0.player, "/ah search " + var5);
                        stageUntilMs = var3 + 1000L;
                     } else {
                        log("[Parser] " + currentItemName + ": контейнер не открылся после 10 попыток, пропуск");
                        currentIndex++;
                        retryCount = 0;
                        stage = FunTimePriceParser.Stage.SEND_SEARCH;
                        stageUntilMs = 0L;
                     }
                  }
               }
            }
         }
      }
   }

   public static void tickAuction(MinecraftClient var0, List<Slot> var1) {
      if (enabled) {
         if (var0 != null && var0.player != null) {
            if (items == null || items.isEmpty()) {
               items = resolveItemsToParse();
               currentIndex = 0;
            }

            if (items != null && !items.isEmpty()) {
               long var13 = System.currentTimeMillis();
               switch (stage) {
                  case SEND_SEARCH:
                     if (currentIndex >= items.size()) {
                        stage = FunTimePriceParser.Stage.FINISH_WAIT;
                        stageUntilMs = var13 + 3000L;
                        log("[Parser] SEND_SEARCH: все предметы обработаны (" + items.size() + "), перехожу в FINISH_WAIT");
                        log("[Parser] Завершено. Ожидание 3сек перед включением AutoBuy...");
                        return;
                     }

                     currentItemName = items.get(currentIndex);
                     String var14 = cleanSearchName(currentItemName);
                     retryCount = 0;
                     log("[Parser] (" + (currentIndex + 1) + "/" + items.size() + ") Поиск: " + currentItemName);
                     CommandSender.sendCommand(var0.player, "/ah search " + var14);
                     stage = FunTimePriceParser.Stage.WAIT_RESULTS;
                     stageUntilMs = var13 + 500L;
                     break;
                  case WAIT_RESULTS:
                     if (var13 < stageUntilMs) {
                        return;
                     }

                     int var7 = findCheapestPerItem(var1, currentItemName);
                     if (var7 > 0) {
                        int var8 = (int)Math.floor(var7 * (100.0 - discountPercent) / 100.0);
                        if (var8 < 0) {
                           var8 = 0;
                        }

                        AutoBuyableItem var9 = findItemByDisplayName(currentItemName);
                        if (var9 != null) {
                           log("[Parser] " + currentItemName + ": " + var7 + " -> " + var8 + " (-" + discountPercent + "%)");
                           System.out.println("Цена для \"" + currentItemName + "\" установлена: " + formatPrice(var8));
                        }

                        currentIndex++;
                        stage = FunTimePriceParser.Stage.WAIT_BETWEEN;
                        stageUntilMs = var13 + 3000L;
                     } else {
                        retryCount++;
                        if (retryCount < 10) {
                           String var15 = cleanSearchName(currentItemName);
                           CommandSender.sendCommand(var0.player, "/ah search " + var15);
                           stageUntilMs = var13 + 500L;
                        } else {
                           log("[Parser] " + currentItemName + ": не найдено после 10 попыток, пропуск");
                           currentIndex++;
                           retryCount = 0;
                           stage = FunTimePriceParser.Stage.WAIT_BETWEEN;
                           stageUntilMs = var13 + 3000L;
                        }
                     }
                     break;
                  case WAIT_BETWEEN:
                     if (var13 < stageUntilMs) {
                        return;
                     }

                     stage = FunTimePriceParser.Stage.SEND_SEARCH;
                     break;
                  case FINISH_WAIT:
                     if (var13 < stageUntilMs) {
                        return;
                     }

                     log("[Parser] Готово! Включаю AutoBuy.");
                     CommandSender.sendCommand(var0.player, "/ah");
                     AutoBuyManager.getInstance().setEnabled(true);
                     setEnabled(false);
               }
            } else {
               long var5 = System.currentTimeMillis();
               if (var5 - lastEmptyItemsNotifyMs > 2000L) {
                  lastEmptyItemsNotifyMs = var5;
               }

               setEnabled(false);
            }
         }
      }
   }

   private static void log(String var0) {
   }

   private static AutoBuyableItem findItemByDisplayName(String var0) {
      return findItemByDisplayNameStatic(var0);
   }

   public static AutoBuyableItem findItemByDisplayNameStatic(String var0) {
      if (var0 == null) {
         return null;
      }

      for (AutoBuyableItem var2 : ItemRegistry.getAllItems()) {
         if (var2 != null) {
            String var3 = var2.getDisplayName();
            if (var3 != null && var3.equalsIgnoreCase(var0)) {
               return var2;
            }
         }
      }

      return null;
   }

   private static int findCheapestPerItem(List<Slot> var0, String var1) {
      if (var0 != null && var1 != null) {
         String var2 = var1.toLowerCase().trim();
         int var3 = Integer.MAX_VALUE;

         for (int var4 = 0; var4 <= 44 && var4 < var0.size(); var4++) {
            Slot var5 = (Slot)var0.get(var4);
            if (var5 != null && !var5.getStack().isEmpty()) {
               String var6 = var5.getStack().getName().getString();
               String var7 = AuctionUtils.funTimePricePattern.matcher(var6).replaceAll("").trim();
               String var8 = var7.toLowerCase().trim();
               boolean var9 = var8.equals(var2) || var8.contains(var2) || !var8.isEmpty() && var2.contains(var8);
               if (var9) {
                  int var10 = AuctionUtils.getPrice(var5.getStack());
                  if (var10 <= 0) {
                     var10 = AuctionUtils.getPriceFromNearbySlots(var0, var4);
                  }

                  if (var10 > 0) {
                     int var11 = var5.getStack().getCount();
                     if (var11 <= 0) {
                        var11 = 1;
                     }

                     int var12 = var10 / var11;
                     if (var12 > 0 && var12 < var3) {
                        var3 = var12;
                     }
                  }
               }
            }
         }

         return var3 == Integer.MAX_VALUE ? -1 : var3;
      } else {
         return -1;
      }
   }

   private static List<String> resolveItemsToParse() {
      ArrayList var6 = new ArrayList();

      try {
         for (AutoBuyableItem var2 : AutoBuyManager.getInstance().getAllItems()) {
            if (var2 != null && var2.isEnabled()) {
               String var3 = var2.getDisplayName();
               if (var3 != null && !var3.trim().isEmpty()) {
                  var6.add(var3);
               }
            }
         }
      } catch (Exception var5) {
      }

      if (!var6.isEmpty()) {
         log("[Parser] Items source=AUTOBUY_ENABLED size=" + var6.size());
      } else {
         log("[Parser] Items source=AUTOBUY_ENABLED size=0");
      }

      return var6;
   }

   private static String cleanSearchName(String var0) {
      if (var0 == null) {
         return "";
      }

      String var1 = var0.replace("[★] ", "").replace("[★]", "").trim();
      Matcher var2 = LEVEL_PATTERN.matcher(var1);
      if (var2.find()) {
         String var3 = var2.group(1).trim();
         String var4 = var2.group(2);
         var3 = var3.replace(" опыта", "").replace(" Опыта", "").trim();
         var1 = var3 + " с уровнем " + var4;
      }

      return var1.trim();
   }

   private static String formatPrice(int var0) {
      StringBuilder var1 = new StringBuilder();
      String var2 = String.valueOf(var0);
      int var3 = 0;

      for (int var4 = var2.length() - 1; var4 >= 0; var4--) {
         if (var3 > 0 && var3 % 3 == 0) {
            var1.insert(0, '.');
         }

         var1.insert(0, var2.charAt(var4));
         var3++;
      }

      return var1.toString();
   }

   private enum Stage {
      IDLE,
      SEND_SEARCH,
      WAIT_RESULTS,
      WAIT_BETWEEN,
      FINISH_WAIT;
   }
}
