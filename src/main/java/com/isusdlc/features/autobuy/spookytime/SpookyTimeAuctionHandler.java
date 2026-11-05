package com.isusdlc.features.autobuy.spookytime;

import com.isusdlc.display.screens.autobuy.history.HistoryManager;
import com.isusdlc.features.autobuy.AutoBuyBanList;
import com.isusdlc.features.autobuy.TimerUtil;
import com.isusdlc.features.autobuy.items.AutoBuyableItem;
import com.isusdlc.features.autobuy.manager.AutoBuyManager;
import com.isusdlc.features.autobuy.util.AuctionUtils;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.client.MinecraftClient;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;

public class SpookyTimeAuctionHandler {
   private static final MinecraftClient mc = MinecraftClient.getInstance();
   private static final Pattern SPOOKYTIME_PRICE_PATTERN = Pattern.compile("(?:\\$\\s*([\\d,.\\s]+)|([\\d,.\\s]+)\\s*\\$|(?:Цена|Price)[:\\s]*([\\d,.\\s]+))");
   private static final int ITEMS_START_SLOT = 0;
   private static final int ITEMS_END_SLOT = 44;
   private final AutoBuyManager autoBuyManager;
   private final TimerUtil refreshTimer = TimerUtil.create();
   private final TimerUtil buyTimer = TimerUtil.create();
   private final TimerUtil refreshCooldown = TimerUtil.create();
   private final Random random = new Random();
   private static final long REFRESH_COOLDOWN_MS = 3000L;
   private final Set<String> processedItems = ConcurrentHashMap.newKeySet();
   private final Map<String, Long> processedItemsTime = new ConcurrentHashMap<>();
   private final Map<String, SpookyTimeAuctionHandler.PendingPurchase> pendingPurchases = new ConcurrentHashMap<>();
   private long lastProcessedClear = System.currentTimeMillis();

   public SpookyTimeAuctionHandler(AutoBuyManager var1) {
      this.autoBuyManager = var1;
   }

   public void clear() {
      this.processedItems.clear();
      this.processedItemsTime.clear();
      this.pendingPurchases.clear();
      this.refreshTimer.resetCounter();
      this.buyTimer.resetCounter();
      this.lastProcessedClear = System.currentTimeMillis();
   }

   public void tick(int var1, List<Slot> var2, List<AutoBuyableItem> var3, long var4, long var6) {
      if (mc.player != null && mc.interactionManager != null) {
         if (var3 != null && !var3.isEmpty()) {
            this.clearOldProcessedItems();
            this.clearOldPendingPurchases();
            boolean var8 = this.scanAndBuy(var1, var2, var3);
            if (var8) {
               this.buyTimer.resetCounter();
            }

            if (this.refreshTimer.hasTimeElapsed(var4 > 0L ? var4 : 150L)) {
               this.refreshAuction(var1, var2);
               this.refreshTimer.resetCounter();
               this.refreshCooldown.resetCounter();
            }
         }
      }
   }

   public void refreshAuction(int var1, List<Slot> var2) {
      if (mc.player != null && mc.interactionManager != null) {
         mc.interactionManager.clickSlot(var1, 49, 0, SlotActionType.QUICK_MOVE, mc.player);
         this.processedItems.clear();
         this.processedItemsTime.clear();
      }
   }

   private boolean scanAndBuy(int var1, List<Slot> var2, List<AutoBuyableItem> var3) {
      HashMap<Item, List<AutoBuyableItem>> var4 = new HashMap<>();
      ArrayList<AutoBuyableItem> var5 = new ArrayList<>();

      for (AutoBuyableItem var7 : var3) {
         if (var7 != null && var7.isEnabled()) {
            ItemStack var8 = var7.createItemStack();
            NbtComponent var9 = (NbtComponent)var8.get(DataComponentTypes.CUSTOM_DATA);
            boolean var10 = false;
            boolean var11 = false;
            if (var9 != null) {
               NbtCompound var12 = var9.copyNbt();
               if (var12 != null) {
                  if (var12.getBoolean("SpookyTimeSphere")) {
                     var10 = true;
                     var5.add(var7);
                  }

                  if (var12.getBoolean("SpookyTimePotion")) {
                     var11 = true;
                  }
               }
            }

            Item var34 = var7.getItem();
            var4.computeIfAbsent(var34, var0 -> new ArrayList<>()).add(var7);
         }
      }

      for (int var26 = 0; var26 <= 44 && var26 < var2.size(); var26++) {
         Slot var27 = (Slot)var2.get(var26);
         if (var27 != null && !var27.getStack().isEmpty()) {
            ItemStack var28 = var27.getStack();
            String var29 = var28.getName().getString();
            if (!AuctionUtils.isArmorItem(var28) || !AuctionUtils.hasThornsEnchantment(var28)) {
               int var31 = this.getSpookyTimePrice(var28, var2, var26);
               if (var31 > 0) {
                  String var33 = var29 + "|" + var31 + "|" + var26;
                  if (!this.processedItems.contains(var33)) {
                     Item var35 = var28.getItem();
                     List<AutoBuyableItem> var13 = var4.get(var35);
                     if (var35 != Items.SPLASH_POTION && var35 != Items.POTION) {
                        boolean var40 = false;
                     } else {
                        boolean var10000 = true;
                     }

                     if ((var13 == null || var13.isEmpty()) && var35 == Items.PLAYER_HEAD) {
                        var13 = var5;
                     }

                     if (var13 != null && !var13.isEmpty()) {
                        for (AutoBuyableItem var16 : var13) {
                           if (var16 != null && var16.isEnabled()) {
                              boolean var17 = var16.needsAdditionalCheck();
                              boolean var18 = false;
                              if (!var17) {
                                 var18 = true;
                              } else {
                                 ItemStack var19 = var16.createItemStack();
                                 var18 = AuctionUtils.compareItem(var28, var19);
                              }

                              if (var18) {
                                 int var37 = var28.getCount();
                                 if (var37 <= 0) {
                                    var37 = 1;
                                 }

                                 int var20 = var31 / var37;
                                 int var21 = this.getMaxPrice(var16);
                                 if (var21 > 0
                                    && var20 <= var21
                                    && (!var16.getSettings().isCanHaveQuantity() || var37 >= var16.getSettings().getMinQuantity())) {
                                    String var22 = this.getSellerName(var28);
                                    if (var22 != null && !var22.isEmpty()) {
                                    }

                                    if (var22 != null && !var22.isEmpty()) {
                                       String var23 = mc.player != null ? mc.player.getName().getString() : null;
                                       if (var23 != null) {
                                          String var24 = var23.replaceAll("§[0-9a-fk-or]", "").replaceAll("§.", "").trim();
                                          String var25 = var22.replaceAll("§[0-9a-fk-or]", "").replaceAll("§.", "").trim();
                                          if (var24.equalsIgnoreCase(var25)) {
                                             continue;
                                          }
                                       }

                                       if (AutoBuyBanList.isBanned(var22)) {
                                          System.out.println("§c[SpookyTime] Пропуск забаненного продавца: §f" + var22 + " §7| Предмет: §f" + var29);
                                          continue;
                                       }
                                    }

                                    String var38 = var29.toLowerCase().trim();
                                    this.pendingPurchases.put(var38, new SpookyTimeAuctionHandler.PendingPurchase(var28.copy(), var29, var31));
                                    int var39 = var27.id;
                                    if (var27.id == 0 || var27.id != var26) {
                                       var39 = var26;
                                    }

                                    mc.interactionManager.clickSlot(var1, var39, 0, SlotActionType.QUICK_MOVE, mc.player);
                                    this.processedItems.add(var33);
                                    this.processedItemsTime.put(var33, System.currentTimeMillis());
                                    return true;
                                 }
                              }
                           }
                        }
                     }
                  }
               }
            }
         }
      }

      return false;
   }

   private int getSpookyTimePrice(ItemStack var1, List<Slot> var2, int var3) {
      String var4 = var1.getName().getString();
      int var5 = AuctionUtils.getPrice(var1);
      if (var5 > 0) {
         return var5;
      }

      LoreComponent var6 = (LoreComponent)var1.get(DataComponentTypes.LORE);
      if (var6 != null && !var6.lines().isEmpty()) {
         for (Text var8 : var6.lines()) {
            String var9 = var8.getString();
            if (var9.contains("Цена:") || var9.contains("Price:")) {
               try {
                  int var10 = Math.max(var9.indexOf("Цена:"), var9.indexOf("Price:"));
                  if (var10 != -1) {
                     String var11 = var9.substring(var10);
                     String var12 = var11.replaceAll("[^0-9.]", "").trim();

                     try {
                        var5 = (int)Double.parseDouble(var12);
                        if (var5 > 0) {
                           return var5;
                        }
                     } catch (NumberFormatException var17) {
                        try {
                           var5 = Integer.parseInt(var12);
                           if (var5 > 0) {
                              return var5;
                           }
                        } catch (NumberFormatException var16) {
                        }
                     }
                  }
               } catch (Exception var18) {
               }
            }
         }
      }

      if (var6 != null && !var6.lines().isEmpty()) {
         for (Text var24 : var6.lines()) {
            String var25 = var24.getString();
            Matcher var26 = SPOOKYTIME_PRICE_PATTERN.matcher(var25);
            if (var26.find()) {
               String var27 = null;

               for (int var29 = 1; var29 <= 3; var29++) {
                  if (var26.group(var29) != null) {
                     var27 = var26.group(var29);
                     break;
                  }
               }

               if (var27 != null) {
                  try {
                     var27 = var27.replaceAll("[\\s,.]", "");
                     var5 = Integer.parseInt(var27);
                     if (var5 > 0) {
                        return var5;
                     }
                  } catch (NumberFormatException var15) {
                  }
               }
            }
         }
      }

      var5 = AuctionUtils.getPriceFromNearbySlots(var2, var3);
      return var5 > 0 ? var5 : -1;
   }

   private int getMaxPrice(AutoBuyableItem var1) {
      int var9 = var1.getSettings().getBuyBelow();
      return var9 > 0 ? var9 : var1.getPrice();
   }

   private void clearOldProcessedItems() {
      long var1 = System.currentTimeMillis();
      if (var1 - this.lastProcessedClear > 30000L) {
         this.processedItems.removeIf(var3 -> {
            Long var4 = this.processedItemsTime.get(var3);
            if (var4 != null && var1 - var4 > 30000L) {
               this.processedItemsTime.remove(var3);
               return true;
            } else {
               return false;
            }
         });
         this.lastProcessedClear = var1;
      }
   }

   private void clearOldPendingPurchases() {
      long var1 = System.currentTimeMillis();
      this.pendingPurchases.entrySet().removeIf(var2 -> var1 - var2.getValue().timestamp > 10000L);
   }

   public void confirmPurchase(String var1, int var2) {
      SpookyTimeAuctionHandler.PendingPurchase var3 = null;
      String var4 = null;

      for (Entry var6 : this.pendingPurchases.entrySet()) {
         SpookyTimeAuctionHandler.PendingPurchase var7 = (SpookyTimeAuctionHandler.PendingPurchase)var6.getValue();
         if (var7.price == var2) {
            var3 = var7;
            var4 = (String)var6.getKey();
            break;
         }
      }

      if (var3 != null) {
         HistoryManager.getInstance().addPurchaseWithItem(var3.itemStack, var3.itemName, var2);
         this.pendingPurchases.remove(var4);
      } else if (!this.pendingPurchases.isEmpty()) {
         Entry var10 = null;
         long var11 = 0L;

         for (Entry var9 : this.pendingPurchases.entrySet()) {
            if (((SpookyTimeAuctionHandler.PendingPurchase)var9.getValue()).timestamp > var11) {
               var11 = ((SpookyTimeAuctionHandler.PendingPurchase)var9.getValue()).timestamp;
               var10 = var9;
            }
         }

         if (var10 != null && System.currentTimeMillis() - ((SpookyTimeAuctionHandler.PendingPurchase)var10.getValue()).timestamp < 10000L) {
            HistoryManager.getInstance()
               .addPurchaseWithItem(
                  ((SpookyTimeAuctionHandler.PendingPurchase)var10.getValue()).itemStack,
                  ((SpookyTimeAuctionHandler.PendingPurchase)var10.getValue()).itemName,
                  var2
               );
            this.pendingPurchases.remove(var10.getKey());
         }
      }
   }

   public void resetTimers() {
      this.refreshTimer.resetCounter();
      this.buyTimer.resetCounter();
      this.refreshCooldown.resetCounter();
   }

   private String getSellerName(ItemStack var1) {
      if (var1 != null && !var1.isEmpty()) {
         LoreComponent var2 = (LoreComponent)var1.get(DataComponentTypes.LORE);
         if (var2 != null && !var2.lines().isEmpty()) {
            Pattern var3 = Pattern.compile("(?i)(?:Продавец|Seller)[:\\s]+(.+)");

            for (Text var5 : var2.lines()) {
               String var6 = var5.getString();
               Matcher var7 = var3.matcher(var6);
               if (var7.find()) {
                  String var8 = var7.group(1).trim();
                  var8 = var8.replaceAll("§[0-9a-fk-or]", "");
                  var8 = var8.replaceAll("§.", "");
                  var8 = var8.trim();
                  if (!var8.isEmpty()) {
                     return var8;
                  }
               }
            }

            return null;
         } else {
            return null;
         }
      } else {
         return null;
      }
   }

   public static class PendingPurchase {
      public final ItemStack itemStack;
      public final String itemName;
      public final int price;
      public final long timestamp;

      public PendingPurchase(ItemStack var1, String var2, int var3) {
         this.itemStack = var1;
         this.itemName = var2;
         this.price = var3;
         this.timestamp = System.currentTimeMillis();
      }
   }
}
