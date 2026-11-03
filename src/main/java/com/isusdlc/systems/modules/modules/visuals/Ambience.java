package com.isusdlc.systems.modules.modules.visuals;

import lombok.Generated;
import com.isusdlc.systems.event.EventListener;
import com.isusdlc.systems.event.impl.network.ReceivePacketEvent;
import com.isusdlc.systems.modules.api.ModuleCategory;
import com.isusdlc.systems.modules.api.ModuleInfo;
import com.isusdlc.systems.modules.impl.BaseModule;
import com.isusdlc.systems.setting.settings.BooleanSetting;
import com.isusdlc.systems.setting.settings.SliderSetting;
import com.isusdlc.utility.game.EntityUtility;
import net.minecraft.network.packet.s2c.play.WorldTimeUpdateS2CPacket;

@ModuleInfo(
   name = "Ambience",
   category = ModuleCategory.VISUALS,
   desc = "modules.descriptions.ambience"
)
public class Ambience extends BaseModule {
   public final BooleanSetting endSky = new BooleanSetting(this, "modules.settings.ambience.end_sky");
   private final BooleanSetting customTime = new BooleanSetting(this, "modules.settings.ambience.custom_time");
   private final SliderSetting time = new SliderSetting(this, "modules.settings.ambience.time", () -> !this.customTime.isEnabled())
      .step(1000.0F)
      .min(0.0F)
      .max(24000.0F)
      .currentValue(12000.0F);
   public final BooleanSetting bright = new BooleanSetting(this, "modules.settings.ambience.bright").enable();
   private long oldTime;
   private final EventListener<ReceivePacketEvent> onReceivePacket = event -> {
      if (event.getPacket() instanceof WorldTimeUpdateS2CPacket && this.customTime.isEnabled()) {
         event.cancel();
      }
   };

   @Override
   public void tick() {
      if (mc.world != null) {
         if (this.customTime.isEnabled()) {
            mc.world.getLevelProperties().setTimeOfDay((long)this.time.getCurrentValue());
         }

         super.tick();
      }
   }

   @Override
   public void onEnable() {
      if (EntityUtility.isInGame() && mc.world != null) {
         this.oldTime = mc.world.getTimeOfDay();
         super.onEnable();
      }
   }

   @Override
   public void onDisable() {
      if (EntityUtility.isInGame() && mc.world != null) {
         mc.world.getLevelProperties().setTimeOfDay(this.oldTime);
         super.onDisable();
      }
   }

   @Generated
   public BooleanSetting getEndSky() {
      return this.endSky;
   }

   @Generated
   public BooleanSetting getCustomTime() {
      return this.customTime;
   }

   @Generated
   public SliderSetting getTime() {
      return this.time;
   }

   @Generated
   public BooleanSetting getBright() {
      return this.bright;
   }

   @Generated
   public long getOldTime() {
      return this.oldTime;
   }

   @Generated
   public EventListener<ReceivePacketEvent> getOnReceivePacket() {
      return this.onReceivePacket;
   }
}
