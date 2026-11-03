package com.isusdlc.systems.modules.modules.visuals;

import com.isusdlc.systems.event.EventListener;
import com.isusdlc.systems.event.impl.window.KeyPressEvent;
import com.isusdlc.systems.modules.api.ModuleCategory;
import com.isusdlc.systems.modules.api.ModuleInfo;
import com.isusdlc.systems.modules.impl.BaseModule;
import com.isusdlc.systems.setting.settings.BindSetting;
import com.isusdlc.systems.setting.settings.BooleanSetting;
import com.isusdlc.systems.setting.settings.SliderSetting;
import com.isusdlc.utility.animation.base.Animation;
import com.isusdlc.utility.animation.base.Easing;

@ModuleInfo(
   name = "Zoom",
   category = ModuleCategory.VISUALS,
   desc = "modules.descriptions.zoom"
)
public class Zoom extends BaseModule {
   private final BindSetting zoomKey = new BindSetting(this, "modules.settings.zoom.key");
   private final SliderSetting zoomLevel = new SliderSetting(this, "modules.settings.zoom.level").step(0.1F).min(1.0F).max(10.0F).currentValue(4.0F);
   private final BooleanSetting smooth = new BooleanSetting(this, "modules.settings.zoom.smooth").enabled(true);
   private final SliderSetting smoothSpeed = new SliderSetting(this, "modules.settings.zoom.smooth_speed", () -> !this.smooth.isEnabled())
      .step(50.0F)
      .min(100.0F)
      .max(1000.0F)
      .currentValue(300.0F);
   private boolean isZooming = false;
   private final Animation zoomAnimation = new Animation(300L, 0.0F, Easing.FIGMA_EASE_IN_OUT);
   private final EventListener<KeyPressEvent> onKeyPress = event -> {
      if (this.zoomKey.isKey(event.getKey()) && mc.currentScreen == null) {
         if (event.getAction() == 1) {
            this.isZooming = true;
         } else if (event.getAction() == 0) {
            this.isZooming = false;
         }
      }
   };
   private final EventListener<com.isusdlc.systems.event.impl.window.MouseEvent> onMouseButton = event -> {
      if (this.zoomKey.isKey(event.getButton()) && mc.currentScreen == null) {
         if (event.getAction() == 1) {
            this.isZooming = true;
         } else if (event.getAction() == 0) {
            this.isZooming = false;
         }
      }
   };

   @Override
   public void onEnable() {
      super.onEnable();
      this.zoomAnimation.setDuration((long)this.smoothSpeed.getCurrentValue());
   }

   public void updateZoom() {
      if (this.smooth.isEnabled()) {
         this.zoomAnimation.setDuration((long)this.smoothSpeed.getCurrentValue());
         this.zoomAnimation.update(this.isZooming);
      }
   }

   public float getZoomMultiplier() {
      if (!this.isEnabled()) {
         return 1.0F;
      } else if (this.smooth.isEnabled()) {
         float progress = this.zoomAnimation.getValue();
         return 1.0F + (this.zoomLevel.getCurrentValue() - 1.0F) * progress;
      } else {
         return this.isZooming ? this.zoomLevel.getCurrentValue() : 1.0F;
      }
   }

   public boolean isZooming() {
      return this.isZooming;
   }
}
