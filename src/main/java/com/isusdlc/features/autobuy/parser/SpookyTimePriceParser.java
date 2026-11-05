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
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.Text;

public final class SpookyTimePriceParser {
   private static final Pattern SPOOKYTIME_PRICE_PATTERN = Pattern.compile("(?:\\$\\s*([\\d,.\\s]+)|([\\d,.\\s]+)\\s*\\$|(?:Цена|Price)[:\\s]*([\\d,.\\s]+))");
   private static volatile boolean enabled = false;
   private static volatile int discountPercent = 0;
   private static SpookyTimePriceParser.Stage stage = SpookyTimePriceParser.Stage.IDLE;
   private static long stageUntilMs = 0L;
   private static int currentIndex = 0;
   private static List<AutoBuyableItem> items = new ArrayList<>();
   private static AutoBuyableItem currentItem = null;
   private static long lastEmptyItemsNotifyMs = 0L;
   private static int retryCount = 0;
   private static final int MAX_RETRIES = 2;
   private static final Pattern LEVEL_PATTERN = Pattern.compile("(.+?)\\s*\\[(\\d+)\\s*[уУ]р\\.?\\]");

   private SpookyTimePriceParser() {
   }

   public static boolean isEnabled() {
      return enabled;
   }

   public static void setEnabled(boolean var0) {
      if (enabled != var0) {
         if (var0) {
            List var1 = resolveItemsToParse();
            if (var1 == null || var1.isEmpty()) {
               enabled = false;
               stage = SpookyTimePriceParser.Stage.IDLE;
               stageUntilMs = 0L;
               currentIndex = 0;
               currentItem = null;
               items = new ArrayList<>();
               return;
            }

            enabled = true;
            stage = SpookyTimePriceParser.Stage.SEND_SEARCH;
            stageUntilMs = 0L;
            currentIndex = 0;
            currentItem = null;
            items = var1;
            AutoBuyManager.getInstance().setEnabled(false);
            if ((MinecraftClient.getInstance().currentScreen == null || !(MinecraftClient.getInstance().currentScreen instanceof GenericContainerScreen))
               && MinecraftClient.getInstance().player != null) {
               CommandSender.sendCommand(MinecraftClient.getInstance().player, "/ah");
            }
         } else {
            enabled = false;
            stage = SpookyTimePriceParser.Stage.IDLE;
            stageUntilMs = 0L;
            currentIndex = 0;
            currentItem = null;
         }
      }
   }

   public static int getDiscountPercent() {
      return discountPercent;
   }

   public static String getCurrentItemName() {
      return currentItem != null ? currentItem.getDisplayName() : null;
   }

   public static int findCheapestPerItemStatic(List<Slot> var0, AutoBuyableItem var1) {
      return findCheapestPerItem(var0, var1);
   }

   public static void setDiscountPercent(int var0) {
      int var1 = discountPercent;
      discountPercent = Math.max(0, Math.min(100, var0));
      if (var1 != discountPercent) {
      }
   }

   public static void tickNoContainer(MinecraftClient var0) {
      if (enabled) {
         if (var0 != null && var0.player != null) {
            if (stage != SpookyTimePriceParser.Stage.IDLE) {
               long var2 = System.currentTimeMillis();
               if (stage == SpookyTimePriceParser.Stage.FINISH_WAIT) {
                  if (var2 >= stageUntilMs) {
                     var0.player.closeScreen();
                     CommandSender.sendCommand(var0.player, "/ah");
                     AutoBuyManager.getInstance().setEnabled(true);
                     setEnabled(false);
                  }
               } else if (var2 >= stageUntilMs) {
                  if (stage != SpookyTimePriceParser.Stage.SEND_SEARCH) {
                     if (currentItem != null) {
                        retryCount++;
                        if (retryCount < 2) {
                           String var5 = getSearchQuery(currentItem);
                           CommandSender.sendCommand(var0.player, "/ah search " + var5);
                           stageUntilMs = var2 + 500L;
                        } else {
                           currentIndex++;
                           retryCount = 0;
                           stage = SpookyTimePriceParser.Stage.SEND_SEARCH;
                           stageUntilMs = 0L;
                        }
                     }
                  } else {
                     if (items == null || items.isEmpty()) {
                        items = resolveItemsToParse();
                        currentIndex = 0;
                     }

                     if (items != null && !items.isEmpty()) {
                        if (currentIndex >= items.size()) {
                           stage = SpookyTimePriceParser.Stage.FINISH_WAIT;
                           stageUntilMs = var2 + 500L;
                        } else {
                           currentItem = items.get(currentIndex);
                           String var4 = getSearchQuery(currentItem);
                           retryCount = 0;
                           CommandSender.sendCommand(var0.player, "/ah search " + var4);
                           stage = SpookyTimePriceParser.Stage.WAIT_RESULTS;
                           stageUntilMs = var2 + 500L;
                        }
                     } else {
                        setEnabled(false);
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
               if (currentIndex < 0) {
                  currentIndex = 0;
                  stage = SpookyTimePriceParser.Stage.SEND_SEARCH;
                  stageUntilMs = 0L;
               } else {
                  long var4 = System.currentTimeMillis();
                  if (currentIndex >= items.size() && stage != SpookyTimePriceParser.Stage.FINISH_WAIT) {
                     var0.player.closeScreen();
                     CommandSender.sendCommand(var0.player, "/ah");
                     AutoBuyManager.getInstance().setEnabled(true);
                     setEnabled(false);
                  } else if (items != null && !items.isEmpty()) {
                     switch (stage) {
                        case SEND_SEARCH:
                           if (var4 < stageUntilMs) {
                              return;
                           }

                           if (currentIndex >= items.size()) {
                              var0.player.closeScreen();
                              CommandSender.sendCommand(var0.player, "/ah");
                              AutoBuyManager.getInstance().setEnabled(true);
                              setEnabled(false);
                              return;
                           }

                           currentItem = items.get(currentIndex);
                           String var8 = getSearchQuery(currentItem);
                           retryCount = 0;
                           CommandSender.sendCommand(var0.player, "/ah search " + var8);
                           stage = SpookyTimePriceParser.Stage.WAIT_RESULTS;
                           stageUntilMs = var4 + 500L;
                           return;
                        case WAIT_RESULTS:
                           if (var4 < stageUntilMs) {
                              return;
                           }

                           int var6 = findCheapestPerItem(var1, currentItem);
                           if (var6 > 0) {
                              int var7 = (int)Math.floor(var6 * (100.0 - discountPercent) / 100.0);
                              if (var7 < 0) {
                                 var7 = 0;
                              }

                              System.out.println("Цена для \"" + currentItem.getDisplayName() + "\" установлена: " + formatPrice(var7));
                              currentIndex++;
                              stage = SpookyTimePriceParser.Stage.WAIT_BETWEEN;
                              stageUntilMs = var4 + 500L;
                           } else {
                              retryCount++;
                              if (retryCount < 2) {
                                 String var9 = getSearchQuery(currentItem);
                                 CommandSender.sendCommand(var0.player, "/ah search " + var9);
                                 stageUntilMs = var4 + 500L;
                              } else {
                                 currentIndex++;
                                 retryCount = 0;
                                 stage = SpookyTimePriceParser.Stage.WAIT_BETWEEN;
                                 stageUntilMs = var4 + 500L;
                              }
                           }
                           break;
                        case WAIT_BETWEEN:
                           if (var4 < stageUntilMs) {
                              return;
                           }

                           if (currentIndex >= items.size()) {
                              var0.player.closeScreen();
                              CommandSender.sendCommand(var0.player, "/ah");
                              AutoBuyManager.getInstance().setEnabled(true);
                              setEnabled(false);
                              return;
                           }

                           stage = SpookyTimePriceParser.Stage.SEND_SEARCH;
                           return;
                        case FINISH_WAIT:
                           if (var4 < stageUntilMs) {
                              return;
                           }

                           var0.player.closeScreen();
                           CommandSender.sendCommand(var0.player, "/ah");
                           AutoBuyManager.getInstance().setEnabled(true);
                           setEnabled(false);
                     }
                  } else {
                     if (var4 - lastEmptyItemsNotifyMs > 2000L) {
                        lastEmptyItemsNotifyMs = var4;
                     }

                     setEnabled(false);
                  }
               }
            }
         }
      }
   }

   private static void log(String var0) {
   }

   private static String getSearchQuery(AutoBuyableItem var0) {
      if (var0 == null) {
         return "";
      }

      String var1 = var0.getSearchName();
      if (var1 == null || var1.isEmpty()) {
         var1 = var0.getDisplayName();
      }

      return cleanSearchName(var1);
   }

   private static int findCheapestPerItem(List<Slot> var0, AutoBuyableItem var1) {
      if (var0 != null && var1 != null) {
         ItemStack var2 = var1.createItemStack();
         if (var2 != null && !var2.isEmpty()) {
            NbtComponent var3 = (NbtComponent)var2.get(DataComponentTypes.CUSTOM_DATA);
            boolean var4 = false;
            if (var3 != null) {
               NbtCompound var5 = var3.copyNbt();
               if (var5 != null && var5.getBoolean("SpookyTimeSphere")) {
                  var4 = true;
               }
            }

            Item var20 = var1.getItem();
            boolean var6 = var1.needsAdditionalCheck();
            int var7 = Integer.MAX_VALUE;
            int var8 = 0;
            int var9 = 0;

            for (int var10 = 0; var10 <= 44 && var10 < var0.size(); var10++) {
               Slot var11 = (Slot)var0.get(var10);
               if (var11 != null && !var11.getStack().isEmpty()) {
                  var8++;
                  ItemStack var12 = var11.getStack();
                  if (!AuctionUtils.isArmorItem(var12) || !AuctionUtils.hasThornsEnchantment(var12)) {
                     boolean var13 = var4;
                     boolean var14 = var12.getItem() == var20;
                     if (!var14) {
                        if (!var13 || var12.getItem() != Items.PLAYER_HEAD) {
                           continue;
                        }

                        var14 = true;
                     }

                     boolean var15 = false;
                     if (var13) {
                        var15 = AuctionUtils.compareItem(var12, var2);
                     } else if (!var6) {
                        var15 = true;
                     } else {
                        var15 = AuctionUtils.compareItem(var12, var2);
                     }

                     if (var15) {
                        var9++;
                        String var16 = var12.getName().getString();
                        int var17 = getSpookyTimePrice(var12, var0, var10);
                        if (var17 > 0) {
                           int var18 = var12.getCount();
                           if (var18 <= 0) {
                              var18 = 1;
                           }

                           int var19 = var17 / var18;
                           if (var19 > 0 && var19 < var7) {
                              var7 = var19;
                           }
                        }
                     }
                  }
               }
            }

            return var7 == Integer.MAX_VALUE ? -1 : var7;
         } else {
            return -1;
         }
      } else {
         return -1;
      }
   }

   private static List<AutoBuyableItem> resolveItemsToParse() {
      ArrayList<AutoBuyableItem> items = new ArrayList<>();

      try {
         for (AutoBuyableItem item : ItemRegistry.getSpookyTime()) {
            if (item != null && item.isEnabled()) {
               items.add(item);
            }
         }
      } catch (Exception ignored) {
      }

      if (!items.isEmpty()) {
         log("[Parser] Items source=SPOOKYTIME_ENABLED size=" + items.size());
      } else {
         log("[Parser] Items source=SPOOKYTIME_ENABLED size=0");
      }

      return items;
   }

   private static int getSpookyTimePrice(ItemStack var0, List<Slot> var1, int var2) {
      int var3 = AuctionUtils.getPrice(var0);
      if (var3 > 0) {
         return var3;
      }

      LoreComponent var4 = (LoreComponent)var0.get(DataComponentTypes.LORE);
      if (var4 != null && !var4.lines().isEmpty()) {
         for (Text var6 : var4.lines()) {
            String var7 = var6.getString();
            if (var7.contains("Цена:") || var7.contains("Price:")) {
               try {
                  int var8 = Math.max(var7.indexOf("Цена:"), var7.indexOf("Price:"));
                  if (var8 != -1) {
                     String var9 = var7.substring(var8);
                     String var10 = var9.replaceAll("[^0-9.]", "").trim();

                     try {
                        var3 = (int)Double.parseDouble(var10);
                        if (var3 > 0) {
                           return var3;
                        }
                     } catch (NumberFormatException var15) {
                        try {
                           var3 = Integer.parseInt(var10);
                           if (var3 > 0) {
                              return var3;
                           }
                        } catch (NumberFormatException var14) {
                        }
                     }
                  }
               } catch (Exception var16) {
               }
            }
         }
      }

      if (var4 != null && !var4.lines().isEmpty()) {
         for (Text var21 : var4.lines()) {
            String var22 = var21.getString();
            Matcher var23 = SPOOKYTIME_PRICE_PATTERN.matcher(var22);
            if (var23.find()) {
               String var24 = null;

               for (int var26 = 1; var26 <= 3; var26++) {
                  if (var23.group(var26) != null) {
                     var24 = var23.group(var26);
                     break;
                  }
               }

               if (var24 != null) {
                  try {
                     var24 = var24.replaceAll("[\\s,.]", "");
                     var3 = Integer.parseInt(var24);
                     if (var3 > 0) {
                        return var3;
                     }
                  } catch (NumberFormatException var13) {
                  }
               }
            }
         }
      }

      return -1;
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
