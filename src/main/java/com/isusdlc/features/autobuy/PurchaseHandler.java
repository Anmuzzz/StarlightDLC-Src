package com.isusdlc.features.autobuy;

import com.isusdlc.features.autobuy.holyworld.HolyWorldAuctionHandler;
import com.isusdlc.features.autobuy.spookytime.SpookyTimeAuctionHandler;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PurchaseHandler {
   private static final Pattern PURCHASE_PATTERN_FUNTIME = Pattern.compile("Вы успешно купили (.+?) за \\$([\\d,.\\s]+)!");
   private static final Pattern PURCHASE_PATTERN_HOLYWORLD = Pattern.compile("Вы купили\\s+(.+?)\\s+x(\\d+)\\s+у\\s+.+?\\s+за\\s+([\\d\\s.,]+)\\s*¤", 66);

   public static void handlePurchaseMessage(String message, AuctionHandler handler) {
      handlePurchaseMessage(message, handler, null, null);
   }

   public static void handlePurchaseMessage(String message, AuctionHandler handler, HolyWorldAuctionHandler holyHandler) {
      handlePurchaseMessage(message, handler, holyHandler, null);
   }

   public static void handlePurchaseMessage(String message, AuctionHandler handler, HolyWorldAuctionHandler holyHandler, SpookyTimeAuctionHandler spookyHandler) {
      String clean = message.replaceAll("§[0-9a-fk-or]", "");
      if (clean.contains("не хватает") && (clean.contains("Монет") || clean.contains("монет"))) return;

      String itemName = null;
      String priceStr = null;

      Matcher m = PURCHASE_PATTERN_HOLYWORLD.matcher(clean);
      if (m.find()) {
         itemName = m.group(1).trim();
         priceStr = m.group(3).replaceAll("[\\s.,]", "");
      } else {
         m = PURCHASE_PATTERN_FUNTIME.matcher(clean);
         if (m.find()) {
            itemName = m.group(1).trim();
            priceStr = m.group(2).replaceAll("[,\\.\\s]", "");
         }
      }

      if (itemName != null && priceStr != null) {
         try {
            int price = Integer.parseInt(priceStr);
            if (handler != null) handler.confirmPurchase(itemName, price);
            if (holyHandler != null) holyHandler.confirmPurchase(itemName, price);
            if (spookyHandler != null) spookyHandler.confirmPurchase(itemName, price);
         } catch (NumberFormatException ignored) {}
      }
   }
}
