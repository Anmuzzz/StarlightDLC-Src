package com.isusdlc.systems.modules.modules.visuals;

import com.isusdlc.framework.base.CustomDrawContext;
import com.isusdlc.framework.msdf.Font;
import com.isusdlc.framework.msdf.Fonts;
import com.isusdlc.framework.objects.BorderRadius;
import com.isusdlc.systems.event.EventListener;
import com.isusdlc.systems.event.impl.render.PreHudRenderEvent;
import com.isusdlc.systems.modules.api.ModuleCategory;
import com.isusdlc.systems.modules.api.ModuleInfo;
import com.isusdlc.systems.modules.impl.BaseModule;
import com.isusdlc.systems.setting.settings.BooleanSetting;
import com.isusdlc.systems.setting.settings.ColorSetting;
import com.isusdlc.systems.setting.settings.SliderSetting;
import com.isusdlc.utility.colors.ColorRGBA;
import com.isusdlc.utility.render.Utils;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;

@ModuleInfo(
   name = "Nametags",
   category = ModuleCategory.VISUALS,
   desc = "Улучшенные ники над игроками"
)
public class Nametags extends BaseModule {
   private final BooleanSetting players = new BooleanSetting(this, "Players").enable();
   private final BooleanSetting mobs = new BooleanSetting(this, "Mobs");
   private final BooleanSetting self = new BooleanSetting(this, "Self");
   private final BooleanSetting health = new BooleanSetting(this, "Health").enable();
   private final ColorSetting color = new ColorSetting(this, "Color")
      .color(new ColorRGBA(255.0F, 255.0F, 255.0F, 255.0F));
   private final SliderSetting scale = new SliderSetting(this, "Scale")
      .min(0.5F).max(3.0F).step(0.1F).currentValue(1.0F);
   private final SliderSetting distance = new SliderSetting(this, "Distance")
      .min(8.0F).max(64.0F).step(1.0F).currentValue(64.0F);

   private final EventListener<PreHudRenderEvent> onRender2D = event -> {
      if (mc.world == null || mc.player == null) return;

      CustomDrawContext ctx = event.getContext();
      MatrixStack ms = ctx.getMatrices();
      Font font = Fonts.MEDIUM.getFont(9.0F * scale.getCurrentValue());

      for (net.minecraft.entity.Entity entity : mc.world.getEntities()) {
         if (!(entity instanceof LivingEntity living)) continue;
         if (entity == mc.player && !self.isEnabled()) continue;
         if (entity != mc.player && !isValidTarget(living)) continue;
         if (living.isInvisible()) continue;
         if (!living.isAlive()) continue;
         if (mc.player.distanceTo(living) > this.distance.getCurrentValue()) continue;

         Vec3d pos = new Vec3d(
            MathHelper.lerp(mc.getRenderTickCounter().getTickDelta(false), living.prevX, living.getX()),
            MathHelper.lerp(mc.getRenderTickCounter().getTickDelta(false), living.prevY, living.getY()),
            MathHelper.lerp(mc.getRenderTickCounter().getTickDelta(false), living.prevZ, living.getZ())
         );

         Vec2f screen = Utils.worldToScreen(pos.add(0, living.getHeight() + 0.4, 0));
         if (screen == null) continue;

         float distance = mc.player.distanceTo(living);
         float distScale = MathHelper.clamp(1.0F - distance / 40.0F, 0.5F, 1.0F) * scale.getCurrentValue();

         String nameText = living.hasCustomName()
            ? living.getCustomName().getString()
            : living.getDisplayName().getString();

         if (health.isEnabled() && living instanceof PlayerEntity) {
            float hp = living.getHealth();
            float maxHp = living.getMaxHealth();
            nameText += " [" + Math.round(hp) + "/" + Math.round(maxHp) + "]";
         }

         if (!(living instanceof PlayerEntity) && health.isEnabled()) {
            float hp = living.getHealth();
            float maxHp = living.getMaxHealth();
            nameText += " [" + Math.round(hp) + "/" + Math.round(maxHp) + "]";
         }

         ms.push();
         ms.translate(screen.x, screen.y, 0);
         ms.scale(distScale, distScale, 1);

         float textWidth = font.width(nameText);
         float padding = 4.0F;
         float bgWidth = textWidth + padding * 2;
         float bgHeight = font.height() + padding;

         ctx.drawRoundedRect(
            -bgWidth / 2.0F, 0,
            bgWidth, bgHeight,
            BorderRadius.all(4.0F),
            new ColorRGBA(0.0F, 0.0F, 0.0F, 120.0F)
         );

         ctx.drawText(
            font, nameText,
            -textWidth / 2.0F, padding / 2.0F + 0.5F,
            color.getColor()
         );

         ms.pop();
      }
   };

   private boolean isValidTarget(LivingEntity entity) {
      if (entity instanceof PlayerEntity) return players.isEnabled();
      if (entity instanceof HostileEntity || entity instanceof AnimalEntity) return mobs.isEnabled();
      return false;
   }
}
