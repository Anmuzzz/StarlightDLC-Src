package com.isusdlc.ui.hud.impl.island.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.Generated;
import com.isusdlc.elegant;
import com.isusdlc.framework.base.CustomDrawContext;
import com.isusdlc.systems.event.EventListener;
import com.isusdlc.systems.event.impl.game.WorldChangeEvent;
import com.isusdlc.systems.event.impl.network.ReceivePacketEvent;
import com.isusdlc.systems.setting.settings.SelectSetting;
import com.isusdlc.ui.hud.impl.island.TimerStatus;
import com.isusdlc.utility.colors.ColorRGBA;
import com.isusdlc.utility.game.server.ServerUtility;
import com.isusdlc.utility.time.Timer;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;

public class EventStatus extends TimerStatus {
   private static final Pattern EVENT_NAME = Pattern.compile("\\[([^\\]]+)\\]");
   private static final Pattern EVENT_COORDS = Pattern.compile("координатах\\s+(-?\\d+)\\s+(-?\\d+)\\s+(-?\\d+)");
   private static final Pattern DEATH_COORDS = Pattern.compile("координаты:?\\s*(-?\\d+)\\s+(-?\\d+)\\s+(-?\\d+)", Pattern.CASE_INSENSITIVE);
   private final List<ActiveEvent> activeEvents = new ArrayList<>();
   private final Timer timer = new Timer();
   private String pendingEvent;
   private int an = -1;
   private final EventListener<ReceivePacketEvent> onReceivePacket = event -> {
      if (event.getPacket() instanceof GameMessageS2CPacket packet) {
         String message = packet.content().getString().replaceAll("\\n", " ").replaceAll("[^\\p{L}\\p{N}\\s\\[\\]:.-]", "").replaceAll("\\s{2,}", " ").trim();
         if (packet.content().getString().contains("Появился")) {
            Matcher eventMatcher = EVENT_NAME.matcher(message);
            Matcher coordMatcher = EVENT_COORDS.matcher(message);
            if (eventMatcher.find() && coordMatcher.find()) {
               String eventName = eventMatcher.group(1);

               for (EventType value : EventType.values()) {
                  if (eventName.toLowerCase().contains(value.getName().toLowerCase())) {
                     this.activeEvents.removeIf(e -> e.type == value);
                     this.activeEvents.add(new ActiveEvent(value, value.getTime()));
                     this.an = ServerUtility.ftAn;
                     this.timer.reset();
                     if (this.an == ServerUtility.ftAn) {
                        elegant.getInstance()
                           .getWayPointsManager()
                           .add(
                              this.activeEvents.getFirst().type.getName(),
                              Integer.parseInt(coordMatcher.group(1)),
                              Integer.parseInt(coordMatcher.group(2)),
                              Integer.parseInt(coordMatcher.group(3))
                           );
                     }
                     break;
                  }
               }
            }
         } else {
            for (EventType valuex : EventType.values()) {
               if (message.equalsIgnoreCase(valuex.getName())) {
                  this.pendingEvent = valuex.getName();
                  break;
               }
            }

            if (message.toLowerCase().startsWith("координаты")) {
               Matcher coordMatcher = DEATH_COORDS.matcher(message);
               if (coordMatcher.find() && this.pendingEvent != null) {
                  for (EventType valuexx : EventType.values()) {
                     if (this.pendingEvent.equalsIgnoreCase(valuexx.getName())) {
                        this.activeEvents.removeIf(e -> e.type == valuexx);
                        this.activeEvents.add(new ActiveEvent(valuexx, valuexx.getTime()));
                        this.timer.reset();
                        elegant.getInstance()
                           .getWayPointsManager()
                           .add(
                              valuexx.getName(),
                              Integer.parseInt(coordMatcher.group(1)),
                              Integer.parseInt(coordMatcher.group(2)),
                              Integer.parseInt(coordMatcher.group(3))
                           );
                        break;
                     }
                  }

                  this.pendingEvent = null;
               }
            }
         }
      }
   };
   private final EventListener<WorldChangeEvent> worldChangeEvent = event -> {
      if (ServerUtility.ftAn != this.an) {
         this.activeEvents.forEach(e -> elegant.getInstance().getWayPointsManager().del(e.type().getName()));
         this.activeEvents.clear();
      }
   };

   public EventStatus(SelectSetting setting) {
      super(setting, "events");
      elegant.getInstance().getEventManager().subscribe(this);
   }

   @Override
   public void draw(CustomDrawContext context) {
      this.activeEvents.removeIf(event -> {
         if (this.timer.getElapsedTime() >= event.type().getTime()) {
            elegant.getInstance().getWayPointsManager().del(event.type().getName());
            return true;
         } else {
            return false;
         }
      });
      if (!this.activeEvents.isEmpty()) {
         ActiveEvent currentEvent = this.activeEvents.getFirst();
         long remaining = currentEvent.type.getTime() - this.timer.getElapsedTime();
         if (remaining > 0L) {
            int timer = (int)(remaining / 1000L);
            int min = timer / 60;
            int sec = timer % 60;
            String time = String.format("%d:%02d", min, sec);
            ColorRGBA color = this.getEventColor(currentEvent.type);
            this.update(Integer.parseInt(time.split(":")[0]) + ":", "", Integer.parseInt(time.split(":")[1]), currentEvent.type.name, color);
            super.draw(context);
         }
      }
   }

   private ColorRGBA getEventColor(EventType eventType) {
      return switch (eventType) {
         case ALTAR -> new ColorRGBA(138.0F, 43.0F, 226.0F);
         case BEACON -> new ColorRGBA(255.0F, 69.0F, 0.0F);
         case VULCAN -> new ColorRGBA(255.0F, 140.0F, 0.0F);
         case METEOR -> new ColorRGBA(70.0F, 130.0F, 180.0F);
         case PACKAGE -> new ColorRGBA(243.0F, 196.0F, 82.0F);
         case BOSS -> new ColorRGBA(139.0F, 222.0F, 221.0F);
         case CONTAINER -> new ColorRGBA(141.0F, 99.0F, 184.0F);
         case GRUZ -> new ColorRGBA(41.0F, 253.0F, 5.0F);
         case MYSTERIOUS_SHIP -> new ColorRGBA(90.0F, 158.0F, 152.0F);
      };
   }

   @Override
   public boolean canShow() {
      return !this.activeEvents.isEmpty();
   }

   private record ActiveEvent(EventType type, long time) {
   }

   static enum EventType {
      ALTAR("Мистический Алтарь", 360000L),
      BEACON("Маяк Убийца", 360000L),
      VULCAN("Вулкан", 300000L),
      METEOR("Метеоритный дождь", 180000L),
      PACKAGE("Посылка", 180000L),
      BOSS("Босс", 180000L),
      CONTAINER("Контейнер", 180000L),
      GRUZ("Груз", 180000L),
      MYSTERIOUS_SHIP("Таинственный корабль", 300000L);

      final String name;
      final long time;

      @Generated
      public String getName() {
         return this.name;
      }

      @Generated
      public long getTime() {
         return this.time;
      }

      @Generated
      private EventType(final String name, final long time) {
         this.name = name;
         this.time = time;
      }
   }
}
