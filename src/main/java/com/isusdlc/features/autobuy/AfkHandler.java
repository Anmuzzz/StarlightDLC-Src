package com.isusdlc.features.autobuy;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.gui.hud.BossBarHud;
import net.minecraft.client.gui.hud.ClientBossBar;
import java.lang.reflect.Field;
import java.util.Map;
import java.util.UUID;

public class AfkHandler {
   private final TimerUtil afkActionTimer = TimerUtil.create();
   private final TimerUtil cycleTimer = TimerUtil.create();
   private boolean wasInAfk = false;
   private boolean performingAfkAction = false;
   private int afkActionStep = 0;

   public void resetTimers() {
      afkActionTimer.resetCounter();
      cycleTimer.resetCounter();
      wasInAfk = false;
      performingAfkAction = false;
      afkActionStep = 0;
   }

   public void handle(MinecraftClient client) {
      boolean inAfk = isInAfkMode(client);
      if (inAfk && !wasInAfk) {
         performingAfkAction = false;
         afkActionStep = 0;
         cycleTimer.resetCounter();
         afkActionTimer.resetCounter();
      } else if (!inAfk && wasInAfk && performingAfkAction) {
         resetMovementKeys(client.options);
         performingAfkAction = false;
         afkActionStep = 0;
      }
      wasInAfk = inAfk;
      if (inAfk) {
         if (!performingAfkAction && cycleTimer.hasTimeElapsed(7000L)) {
            performingAfkAction = true;
            afkActionStep = 0;
            afkActionTimer.resetCounter();
            cycleTimer.resetCounter();
         }
         if (performingAfkAction) processAfkAction(client);
      }
   }

   private void processAfkAction(MinecraftClient client) {
      switch (afkActionStep) {
         case 0 -> {
            resetMovementKeys(client.options);
            client.options.forwardKey.setPressed(true);
            if (afkActionTimer.hasTimeElapsed(280L)) {
               client.options.forwardKey.setPressed(false);
               afkActionStep++;
               afkActionTimer.resetCounter();
            }
         }
         case 1 -> {
            client.options.backKey.setPressed(true);
            if (afkActionTimer.hasTimeElapsed(350L)) {
               client.options.backKey.setPressed(false);
               afkActionStep = 0;
               performingAfkAction = false;
               afkActionTimer.resetCounter();
               cycleTimer.resetCounter();
            }
         }
      }
   }

   private boolean isInAfkMode(MinecraftClient client) {
      if (client.inGameHud == null) return false;
      BossBarHud bossBar = client.inGameHud.getBossBarHud();
      if (bossBar == null) return false;
      try {
         Field field = BossBarHud.class.getDeclaredField("bossBars");
         field.setAccessible(true);
         Map<UUID, ClientBossBar> bossBars = (Map<UUID, ClientBossBar>) field.get(bossBar);
         return bossBars.values().stream()
            .map(bar -> bar.getName().getString().toLowerCase())
            .anyMatch(name -> name.contains("afk"));
      } catch (Exception e) {
         return false;
      }
   }

   public void resetMovementKeys(GameOptions options) {
      if (options != null) {
         options.forwardKey.setPressed(false);
         options.backKey.setPressed(false);
         options.leftKey.setPressed(false);
         options.rightKey.setPressed(false);
      }
   }

   public boolean isPerformingAction() { return performingAfkAction; }
}
