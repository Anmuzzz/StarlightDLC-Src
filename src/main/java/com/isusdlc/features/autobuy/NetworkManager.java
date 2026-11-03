package com.isusdlc.features.autobuy;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.isusdlc.features.autobuy.items.AutoBuyableItem;
import com.isusdlc.features.autobuy.manager.AutoBuyManager;
import com.isusdlc.features.autobuy.parser.FunTimePriceParser;
import com.isusdlc.display.screens.autobuy.AutoBuyScreen;
import net.minecraft.client.MinecraftClient;

public class NetworkManager {
   private static final int PORT = 20001;
   private static final int RECONNECT_DELAY = 2000;
   private ServerSocket serverSocket = null;
   private Socket clientSocket = null;
   private PrintWriter clientOut = null;
   private BufferedReader clientIn = null;
   private List<Socket> connections = new ArrayList<>();
   private Map<Socket, PrintWriter> outs = new ConcurrentHashMap<>();
   private Map<Socket, BufferedReader> ins = new ConcurrentHashMap<>();
   private Map<Socket, Boolean> clientInAuction = new ConcurrentHashMap<>();
   private ExecutorService executorService = Executors.newFixedThreadPool(10);
   private volatile boolean running = false;
   private volatile boolean isClientMode = false;
   private long lastReconnectAttempt = 0L;
   private ConcurrentLinkedQueue<BuyRequest> queue = new ConcurrentLinkedQueue<>();
   private ConcurrentLinkedQueue<BuyRequest> priorityQueue = new ConcurrentLinkedQueue<>();

   public void start(String var1) {
      if (this.running) return;
      this.running = true;
      this.isClientMode = var1.equals("Проверяющий");
      this.executorService.execute(() -> this.connectionLoop(var1));
   }

   public void stop() {
      this.running = false;
      this.executorService.shutdownNow();
      this.executorService = Executors.newFixedThreadPool(10);
      this.stopAll();
   }

   private void connectionLoop(String var1) {
      while (this.running) {
         if (var1.equals("Покупающий")) {
            this.startServer();
         } else if (var1.equals("Проверяющий")) {
            long var2 = System.currentTimeMillis();
            if ((this.clientSocket == null || this.clientSocket.isClosed()) && var2 - this.lastReconnectAttempt >= 2000L) {
               this.startClient();
               this.lastReconnectAttempt = var2;
            }
         }

         try {
            Thread.sleep(500L);
         } catch (InterruptedException var4) {
         }
      }
   }

   private void startServer() {
      if (this.serverSocket == null || this.serverSocket.isClosed()) {
         try {
            this.serverSocket = new ServerSocket(20001);
            System.out.println("[NetworkManager] Сервер запущен");
            this.executorService.execute(this::listenerThread);
         } catch (IOException var2) {
            System.err.println("[NetworkManager] Ошибка запуска сервера: " + var2.getMessage());
            System.out.println("[NetworkManager] Ошибка запуска сервера: " + var2.getMessage());
         }
      }
   }

   private void startClient() {
      try {
         this.clientSocket = new Socket("localhost", 20001);
         this.clientSocket.setTcpNoDelay(true);
         this.clientSocket.setSoTimeout(0);
         this.clientSocket.setKeepAlive(true);
         this.clientOut = new PrintWriter(this.clientSocket.getOutputStream(), true);
         this.clientIn = new BufferedReader(new InputStreamReader(this.clientSocket.getInputStream()));
         this.clientOut.println("connect");
         this.executorService.execute(this::clientReaderThread);
         System.out.println("[NetworkManager] Подключено к покупающему аккаунту");
      } catch (IOException var2) {
         this.clientSocket = null;
         this.clientOut = null;
         this.clientIn = null;
         System.out.println("[NetworkManager] Ошибка подключения к серверу: " + var2.getMessage());
      }
   }

   private void listenerThread() {
      try {
         while (this.running && this.serverSocket != null && !this.serverSocket.isClosed()) {
            Socket var1 = this.serverSocket.accept();
            var1.setTcpNoDelay(true);
            var1.setKeepAlive(true);
            var1.setSoTimeout(0);
            this.connections.add(var1);
            PrintWriter var2 = new PrintWriter(var1.getOutputStream(), true);
            BufferedReader var3 = new BufferedReader(new InputStreamReader(var1.getInputStream()));
            this.outs.put(var1, var2);
            this.ins.put(var1, var3);
            this.clientInAuction.put(var1, false);
            System.out.println("[NetworkManager] Подключен аккаунт с проверяющим");
            this.executorService.execute(() -> this.readerThread(var1));
         }
      } catch (IOException var4) {
      }
   }

   private void readerThread(Socket var1) {
      try {
         BufferedReader var2 = this.ins.get(var1);

         String var3;
         while ((var3 = var2.readLine()) != null) {
            if (var3.startsWith("buy:")) {
               this.handleBuyMessage(var3);
            } else if (var3.equals("connect") || var3.equals("prices_request")) {
               PrintWriter var14 = this.outs.get(var1);
               if (var14 != null) {
                  this.sendPricesTo(var14);
               }
            } else if (var3.startsWith("parser_toggle:")) {
               boolean var4 = var3.endsWith("1") || var3.toLowerCase().endsWith("true") || var3.toLowerCase().endsWith("on");
               FunTimePriceParser.setEnabled(var4);
               this.sendToAllClients("parser_toggle:" + (var4 ? "1" : "0"));
            } else if (var3.startsWith("parser_percent:")) {
               try {
                  int var12 = Integer.parseInt(var3.substring("parser_percent:".length()).trim());
                  FunTimePriceParser.setDiscountPercent(var12);
                  this.sendToAllClients("parser_percent:" + FunTimePriceParser.getDiscountPercent());
               } catch (Exception var9) {
               }
            } else if (var3.startsWith("parser_result:")) {
               this.handleParserResult(var3);
            } else if (var3.equals("enter_auction")) {
               this.clientInAuction.put(var1, true);
            } else if (var3.equals("leave_auction")) {
               this.clientInAuction.put(var1, false);
            } else if (var3.equals("ping")) {
               PrintWriter var13 = this.outs.get(var1);
               if (var13 != null) {
                  var13.println("pong");
               }
            }
         }
      } catch (IOException var10) {
      } finally {
         this.removeConnection(var1);
      }
   }

   private void handleBuyMessage(String var1) {
      try {
         String[] var2 = var1.substring(4).split("\\|");
         if (var2.length == 2) {
            String var3 = var2[0];
            int var4 = Integer.parseInt(var2[1]);
            BuyRequest var5 = new BuyRequest(var3, var4);
            this.priorityQueue.add(var5);
         }
      } catch (NumberFormatException var6) {
      }
   }

   private void handleParserResult(String var1) {
      try {
         String var2 = var1.substring("parser_result:".length());
         String[] var3 = var2.split("\\|", 3);
         if (var3.length == 3) {
            String var4 = var3[0];
            int var5 = Integer.parseInt(var3[1]);
            int var6 = Integer.parseInt(var3[2]);
            MinecraftClient.getInstance().execute(() -> {
               if (var5 > 0 && var6 >= 0) {
                  AutoBuyableItem var3x = FunTimePriceParser.findItemByDisplayNameStatic(var4);
                  if (var3x != null && var6 > 0) {
                     var3x.getSettings().setBuyBelow(var6);
                     System.out.println("[NetworkManager] Цена для \"" + var4 + "\" установлена: " + formatPrice(var6));
                  }
               }
            });
         }
      } catch (Exception var7) {
      }
   }

   private void clientReaderThread() {
      try {
         String var1;
         try {
            while ((var1 = this.clientIn.readLine()) != null) {
               if (var1.equals("update_now")) {
                  ClientUpdateHandler.handleUpdate();
               } else if (var1.startsWith("price_sync:")) {
                  this.handlePriceSync(var1);
               } else if (var1.startsWith("parser_toggle:")) {
                  boolean var2 = var1.endsWith("1") || var1.toLowerCase().endsWith("true") || var1.toLowerCase().endsWith("on");
                  MinecraftClient.getInstance().execute(() -> FunTimePriceParser.setEnabledUIOnly(var2));
               } else if (var1.startsWith("parser_percent:")) {
                  try {
                     int var17 = Integer.parseInt(var1.substring("parser_percent:".length()).trim());
                     MinecraftClient.getInstance().execute(() -> FunTimePriceParser.setDiscountPercent(var17));
                  } catch (Exception var12) {
                  }
               } else if (var1.startsWith("parser_search:")) {
                  String var16 = var1.substring("parser_search:".length());
                  MinecraftClient.getInstance().execute(() -> {
                     if (MinecraftClient.getInstance().player != null) {
                        CommandSender.sendCommand(MinecraftClient.getInstance().player, "/ah search " + var16);
                     }
                  });
               } else if (var1.equals("autobuy_enable:1")) {
                  AutoBuyManager.getInstance().setEnabled(true);
               } else if (var1.startsWith("switch_server:")) {
                  String var15 = var1.substring(14);
                  CommandSender.handleServerSwitch(var15);
               } else if (var1.equals("open_auction")) {
                  CommandSender.openAuction();
               } else if (var1.equals("pong")) {
               }
            }
         } catch (IOException var13) {
         }
      } finally {
         this.stopClient();
         if (this.running && this.isClientMode) {
            try {
               Thread.sleep(2000L);
            } catch (InterruptedException var11) {
            }
         }
      }
   }

   private void sendPricesTo(PrintWriter var1) {
      try {
         for (AutoBuyableItem item : AutoBuyManager.getInstance().getAllItems()) {
            if (item != null && item.getDisplayName() != null && item.getSettings().getBuyBelow() > 0) {
               var1.println("price_sync:" + item.getDisplayName() + "|" + item.getSettings().getBuyBelow());
            }
         }

         var1.println("price_sync_done");
      } catch (Exception var5) {
      }
   }

   private void handlePriceSync(String var1) {
      String var2 = var1.substring("price_sync:".length());
      int var3 = var2.indexOf('|');
      if (var3 > 0) {
         String var4 = var2.substring(0, var3);

         try {
            int var5 = Integer.parseInt(var2.substring(var3 + 1).trim());
            MinecraftClient.getInstance().execute(() -> AutoBuyScreen.INSTANCE.applySyncedPrice(var4, var5));
         } catch (NumberFormatException var6) {
         }
      }
   }

   private void removeConnection(Socket var1) {
      this.connections.remove(var1);
      this.outs.remove(var1);
      this.ins.remove(var1);
      this.clientInAuction.remove(var1);

      try {
         var1.close();
      } catch (IOException var3) {
      }
   }

   private void stopAll() {
      this.queue.clear();
      this.priorityQueue.clear();
      if (this.serverSocket != null) {
         try {
            this.serverSocket.close();
         } catch (IOException var3) {
         }

         this.serverSocket = null;
      }

      for (Socket var2 : new ArrayList<>(this.connections)) {
         this.removeConnection(var2);
      }

      this.stopClient();
   }

   private void stopClient() {
      if (this.clientSocket != null) {
         try {
            this.clientSocket.close();
         } catch (IOException var2) {
         }

         this.clientSocket = null;
      }

      this.clientOut = null;
      this.clientIn = null;
   }

   public void sendToAllClients(String var1) {
      ArrayList<Socket> var2 = new ArrayList<>();

      for (Socket var4 : new ArrayList<>(this.connections)) {
         PrintWriter var5 = this.outs.get(var4);
         if (var5 != null) {
            try {
               var5.println(var1);
               if (var5.checkError()) {
                  var2.add(var4);
               }
            } catch (Exception var7) {
               var2.add(var4);
            }
         }
      }

      for (Socket var9 : var2) {
         this.removeConnection(var9);
      }
   }

   public void sendParserResult(String var1, int var2, int var3) {
      if (this.clientOut != null) {
         try {
            this.clientOut.println("parser_result:" + var1 + "|" + var2 + "|" + var3);
         } catch (Exception var5) {
         }
      }
   }

   public void sendBuy(String var1, int var2) {
      if (this.clientOut != null) {
         try {
            this.clientOut.println("buy:" + var1 + "|" + var2);
            if (this.clientOut.checkError()) {
               this.stopClient();
            }
         } catch (Exception var4) {
         }
      }
   }

   public void notifyAuctionEnter() {
      if (this.clientOut != null) {
         try {
            this.clientOut.println("enter_auction");
         } catch (Exception var2) {
         }
      }
   }

   public void notifyAuctionLeave() {
      if (this.clientOut != null) {
         try {
            this.clientOut.println("leave_auction");
         } catch (Exception var2) {
         }
      }
   }

   public void sendUpdateToClients() {
      this.sendToAllClients("update_now");
   }

   public void sendToServer(String var1) {
      if (this.clientOut != null) {
         try {
            this.clientOut.println(var1);
            if (this.clientOut.checkError()) {
               this.stopClient();
            }
         } catch (Exception var3) {
         }
      }
   }

   public void requestAuctionOpen() {
      this.sendToAllClients("open_auction");
   }

   public long getClientInAuctionCount() {
      return this.clientInAuction.values().stream().filter(Boolean::booleanValue).count();
   }

   public boolean hasConnectedClients() {
      return !this.connections.isEmpty();
   }

   public boolean isConnectedToServer() {
      return this.clientSocket != null && !this.clientSocket.isClosed() && this.clientOut != null;
   }

   public BuyRequest pollRequest() {
      BuyRequest var1 = this.priorityQueue.poll();
      if (var1 == null) {
         var1 = this.queue.poll();
      }

      return var1;
   }

   public int getQueueSize() {
      return this.priorityQueue.size() + this.queue.size();
   }

   public boolean isQueuesEmpty() {
      return this.priorityQueue.isEmpty() && this.queue.isEmpty();
   }

   public void clearQueues() {
      this.queue.clear();
      this.priorityQueue.clear();
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
}
