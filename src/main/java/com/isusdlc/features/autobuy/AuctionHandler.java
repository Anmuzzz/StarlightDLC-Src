package com.isusdlc.features.autobuy;

import com.isusdlc.display.screens.autobuy.history.HistoryManager;
import com.isusdlc.features.autobuy.items.AutoBuyableItem;
import com.isusdlc.features.autobuy.manager.AutoBuyManager;
import com.isusdlc.features.autobuy.util.AuctionUtils;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.MinecraftClient;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;

public class AuctionHandler {
   private final Set<String> notFoundItems = ConcurrentHashMap.newKeySet();
   private final Set<String> processedItems = ConcurrentHashMap.newKeySet();
   private final Set<String> sentItems = ConcurrentHashMap.newKeySet();
   private final Map<String, Long> sentItemsTime = new ConcurrentHashMap<>();
   private final Map<String, Long> lastMessageTime = new ConcurrentHashMap<>();
   private int failedCount = 0;
   private long lastSentItemsClear = System.currentTimeMillis();
   private final Map<String, AuctionHandler.PendingPurchase> pendingPurchases = new ConcurrentHashMap<>();
   private final AutoBuyManager autoBuyManager;

   public AuctionHandler(AutoBuyManager var1) {
      this.autoBuyManager = var1;
   }

   public void clear() {
      this.notFoundItems.clear();
      this.processedItems.clear();
      this.sentItems.clear();
      this.sentItemsTime.clear();
      this.lastMessageTime.clear();
      this.pendingPurchases.clear();
      this.failedCount = 0;
      this.lastSentItemsClear = System.currentTimeMillis();
   }

   public void confirmPurchase(String var1, int var2) {
      AuctionHandler.PendingPurchase var3 = null;
      String var4 = null;

      for (Entry var6 : this.pendingPurchases.entrySet()) {
         AuctionHandler.PendingPurchase var7 = (AuctionHandler.PendingPurchase)var6.getValue();
         int var8 = var7.price;
         if (var8 == var2) {
            var3 = var7;
            var4 = (String)var6.getKey();
            break;
         }
      }

      if (var3 != null) {
         HistoryManager.getInstance().addPurchaseWithItem(var3.itemStack, var3.itemName, var2);
         this.pendingPurchases.remove(var4);
      } else if (!this.pendingPurchases.isEmpty()) {
         Entry var9 = this.pendingPurchases.entrySet().iterator().next();
         long var10 = System.currentTimeMillis() - ((AuctionHandler.PendingPurchase)var9.getValue()).timestamp;
         if (var10 < 5000L) {
            HistoryManager.getInstance()
               .addPurchaseWithItem(
                  ((AuctionHandler.PendingPurchase)var9.getValue()).itemStack, ((AuctionHandler.PendingPurchase)var9.getValue()).itemName, var2
               );
            this.pendingPurchases.remove(var9.getKey());
            return;
         }
      }
   }

   public void handleBuyRequest(MinecraftClient var1, int var2, List<Slot> var3, BuyRequest var4, NetworkManager var5) {
      if (var1 != null && var1.player != null && var1.interactionManager != null) {
         AutoBuyableItem var6 = this.findMatchingItem(var4.itemName);
         if (var6 != null) {
            Slot var7 = this.findSlotByItemAndPrice(var3, var4.itemName, var4.price);
            if (var7 == null) {
               String var15 = var4.itemName + "|" + var4.price;
               if (!this.notFoundItems.contains(var15)) {
                  this.notFoundItems.add(var15);
               }

               this.failedCount++;
            } else {
               int var8 = var7.getStack().getCount();
               if (var8 <= 0) {
                  var8 = 1;
               }

               int var9 = var4.price / var8;
               int var10 = this.getMaxPrice(var6);
               if (var10 <= 0 || var9 <= var10) {
                  ItemStack var11 = var7.getStack().copy();
                  String var12 = var4.itemName.toLowerCase().trim();
                  this.pendingPurchases.put(var12, new AuctionHandler.PendingPurchase(var11, var4.itemName, var4.price));
                  long var13 = System.currentTimeMillis();
                  this.pendingPurchases.entrySet().removeIf(var2x -> var13 - var2x.getValue().timestamp > 10000L);
                  var1.interactionManager.clickSlot(var2, var7.id, 0, SlotActionType.QUICK_MOVE, var1.player);
                  this.failedCount = 0;
               }
            }
         }
      }
   }

   public boolean shouldUpdate() {
      return this.failedCount > 3;
   }

   public void updateAuction(MinecraftClient var1, int var2) {
      if (var1 != null && var1.player != null && var1.interactionManager != null) {
         var1.interactionManager.clickSlot(var2, 49, 0, SlotActionType.QUICK_MOVE, var1.player);
         this.notFoundItems.clear();
         this.failedCount = 0;
      }
   }

   public void handleSuspiciousPrice(MinecraftClient var1, int var2, List<Slot> var3) {
      if (var1 != null && var1.player != null && var1.interactionManager != null) {
         var1.interactionManager.clickSlot(var2, 0, 0, SlotActionType.QUICK_MOVE, var1.player);
      }
   }

   public List<Slot> findMatchingSlots(List<Slot> var1, List<AutoBuyableItem> var2) {
      try {
         if (var1 != null && var2 != null) {
            HashMap<Item, List<AutoBuyableItem>> var3 = new HashMap<>();
            ArrayList<AutoBuyableItem> var4 = new ArrayList<>();

            for (AutoBuyableItem var6 : var2) {
               if (var6 != null && var6.isEnabled()) {
                  ItemStack var7 = var6.createItemStack();
                  NbtComponent var8 = (NbtComponent)var7.get(DataComponentTypes.CUSTOM_DATA);
                  boolean var9 = false;
                  if (var8 != null) {
                     NbtCompound var10 = var8.copyNbt();
                     if (var10 != null && var10.getBoolean("HolyWorldSphere")) {
                        var9 = true;
                        var4.add(var6);
                     }
                  }

                  if (!var9) {
                     Item var26 = var6.getItem();
                     var3.computeIfAbsent(var26, var0 -> new ArrayList<>()).add(var6);
                  }
               }
            }

            ArrayList<Slot> var21 = new ArrayList<>();

            for (int var22 = 0; var22 <= 44 && var22 < var1.size(); var22++) {
               Slot var23 = (Slot)var1.get(var22);
               if (var23 != null && !var23.getStack().isEmpty()) {
                  ItemStack var24 = var23.getStack();
                  String var25 = var24.getName().getString();
                  if (!AuctionUtils.isArmorItem(var24) || !AuctionUtils.hasThornsEnchantment(var24)) {
                     int var27 = AuctionUtils.getPrice(var24);
                     if (var27 <= 0) {
                        var27 = AuctionUtils.getPriceFromNearbySlots(var1, var22);
                     }

                     if (var27 > 0) {
                        Item var11 = var24.getItem();
                        List<AutoBuyableItem> var12 = var3.get(var11);
                        if ((var12 == null || var12.isEmpty()) && (var11 == Items.END_CRYSTAL || var11 == Items.PLAYER_HEAD)) {
                           var12 = var4;
                        }

                        if (var12 != null && !var12.isEmpty()) {
                           for (AutoBuyableItem var14 : var12) {
                              if (var14 != null && var14.isEnabled()) {
                                 boolean var15 = var14.needsAdditionalCheck();
                                 boolean var16 = false;
                                 if (!var15) {
                                    var16 = true;
                                 } else {
                                    ItemStack var17 = var14.createItemStack();
                                    var16 = AuctionUtils.compareItem(var24, var17);
                                 }

                                 if (var16) {
                                    int var29 = var24.getCount();
                                    if (var29 <= 0) {
                                       var29 = 1;
                                    }

                                    int var18 = var27 / var29;
                                    int var19 = this.getMaxPrice(var14);
                                    if (var19 > 0
                                       && var18 <= var19
                                       && (!var14.getSettings().isCanHaveQuantity() || var29 >= var14.getSettings().getMinQuantity())) {
                                       var21.add(var23);
                                       break;
                                    }
                                 }
                              }
                           }
                        }
                     }
                  }
               }
            }

            var21.sort(Comparator.comparingInt(var1x -> {
               int var2x = AuctionUtils.getPrice(var1x.getStack());
               if (var2x <= 0) {
                  var2x = AuctionUtils.getPriceFromNearbySlots(var1, var1x.id);
               }

               return var2x <= 0 ? Integer.MAX_VALUE : var2x;
            }));
            return var21;
         } else {
            return new ArrayList<>();
         }
      } catch (Exception var20) {
         return new ArrayList<>();
      }
   }

   public void processBestSlots(List<Slot> var1, NetworkManager var2) {
      long var3 = System.currentTimeMillis();
      if (var3 - this.lastSentItemsClear > 30000L) {
         this.sentItems.removeIf(var3x -> {
            Long var4 = this.sentItemsTime.get(var3x);
            if (var4 != null && var3 - var4 > 30000L) {
               this.sentItemsTime.remove(var3x);
               return true;
            } else {
               return false;
            }
         });
         this.lastSentItemsClear = var3;
      }

      for (Slot var6 : var1) {
         if (var6 != null && !var6.getStack().isEmpty()) {
            ItemStack var7 = var6.getStack();
            String var8 = var7.getName().getString();
            String var9 = AuctionUtils.funTimePricePattern.matcher(var8).replaceAll("").trim();
            int var10 = AuctionUtils.getPrice(var7);
            if (var10 > 0) {
               String var11 = var9 + "|" + var10;
               if (!this.sentItems.contains(var11)) {
                  this.sentItems.add(var11);
                  this.sentItemsTime.put(var11, var3);
                  var2.sendBuy(var9, var10);
               }
            }
         }
      }

      if (this.sentItems.size() > 500) {
         this.sentItems.clear();
         this.sentItemsTime.clear();
      }
   }

   private Slot findSlotByItemAndPrice(List<Slot> var1, String var2, int var3) {
      String var4 = var2.toLowerCase().trim();

      for (int var5 = 0; var5 <= 44 && var5 < var1.size(); var5++) {
         Slot var6 = (Slot)var1.get(var5);
         if (var6 != null && !var6.getStack().isEmpty()) {
            ItemStack var7 = var6.getStack();
            if (!AuctionUtils.isArmorItem(var7) || !AuctionUtils.hasThornsEnchantment(var7)) {
               String var8 = var7.getName().getString();
               String var9 = AuctionUtils.funTimePricePattern.matcher(var8).replaceAll("").trim();
               String var10 = var9.toLowerCase().trim();
               int var11 = AuctionUtils.getPrice(var7);
               if (var11 <= 0) {
                  var11 = AuctionUtils.getPriceFromNearbySlots(var1, var5);
               }

               if (var11 > 0) {
                  boolean var12 = var10.equals(var4) || var10.contains(var4) || !var10.isEmpty() && var4.contains(var10);
                  boolean var13 = var11 == var3;
                  if (var12 && var13) {
                     return var6;
                  }
               }
            }
         }
      }

      return null;
   }

   private AutoBuyableItem findMatchingItem(String var1) {
      if (var1 != null && !var1.isEmpty()) {
         String var2 = var1.toLowerCase().trim();
         String var3 = AuctionUtils.funTimePricePattern.matcher(var1).replaceAll("").trim().toLowerCase();

         for (AutoBuyableItem var6 : this.autoBuyManager.getAllItems()) {
            if (var6 != null && var6.isEnabled()) {
               String var7 = var6.getDisplayName() != null ? var6.getDisplayName().replaceAll("§[0-9a-fk-or]", "").trim().toLowerCase() : "";
               String var8 = var6.getSearchName() != null ? var6.getSearchName().replaceAll("§[0-9a-fk-or]", "").trim().toLowerCase() : "";
               if (var7.equals(var3) || var8.equals(var3) || !var7.isEmpty() && var3.contains(var7) || !var7.isEmpty() && var7.contains(var3) || !var8.isEmpty() && var3.contains(var8) || !var8.isEmpty() && var8.contains(var3)) {
                  return var6;
               }
            }
         }

         return null;
      } else {
         return null;
      }
   }

   private int getMaxPrice(AutoBuyableItem var1) {
      int var3 = var1.getSettings().getBuyBelow();
      return var3 > 0 ? var3 : var1.getPrice();
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
