package com.isusdlc.systems.modules.modules.visuals;

import com.isusdlc.framework.msdf.Fonts;
import com.isusdlc.systems.event.EventListener;
import com.isusdlc.systems.event.impl.render.PreHudRenderEvent;
import com.isusdlc.systems.localization.Localizator;
import com.isusdlc.systems.modules.api.ModuleCategory;
import com.isusdlc.systems.modules.api.ModuleInfo;
import com.isusdlc.systems.modules.impl.BaseModule;
import com.isusdlc.utility.animation.base.Animation;
import com.isusdlc.utility.animation.base.Easing;
import com.isusdlc.utility.colors.ColorRGBA;
import com.isusdlc.utility.render.Utils;
import com.isusdlc.utility.render.batching.Batching;
import com.isusdlc.utility.render.batching.impl.FontBatching;
import com.isusdlc.utility.render.batching.impl.RectBatching;
import net.minecraft.entity.Entity;
import net.minecraft.entity.TntEntity;
import net.minecraft.item.Items;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.util.math.MathHelper;
import net.minecraft.client.util.math.MatrixStack;
import java.util.ArrayList;
import java.util.List;

@ModuleInfo(
   name = "TNT Timer",
   category = ModuleCategory.VISUALS,
   desc = "modules.descriptions.tnt_timer"
)
public class TNTTimer extends BaseModule {
   private static final net.minecraft.item.ItemStack TNT_STACK = Items.TNT.getDefaultStack();
   private final Animation pulseAnim = new Animation(300L, 0.0F, Easing.SINE_IN_OUT);

   private final EventListener<PreHudRenderEvent> onHudRenderEvent = event -> {
      if (mc.world == null) return;
      MatrixStack matrices = event.getContext().getMatrices();
      List<TntEntity> tnts = new ArrayList<>();
      for (Entity entity : mc.world.getEntities()) {
         if (entity instanceof TntEntity tnt) {
            tnts.add(tnt);
         }
      }

      Batching rect = new RectBatching(VertexFormats.POSITION_COLOR, event.getContext().getMatrices());
      for (TntEntity tnt : tnts) {
         this.renderBack(event, matrices, tnt);
      }
      rect.draw();

      FontBatching batching = new FontBatching(VertexFormats.POSITION_TEXTURE_COLOR, Fonts.MEDIUM);
      for (TntEntity tnt : tnts) {
         this.renderText(event, matrices, tnt);
      }
      batching.draw();
   };

   private void renderBack(PreHudRenderEvent event, MatrixStack matrices, TntEntity entity) {
      int fuse = entity.getFuse();
      float seconds = fuse / 20.0F;
      String text = Localizator.translate("modules.tnt_timer.format", seconds);
      Vec3d renderPos = entity.getLerpedPos(event.getTickDelta()).add(0.0, 0.5, 0.0);
      Vec2f screenPos = Utils.worldToScreen(renderPos);
      if (screenPos != null) {
         float distance = (float)mc.player.getPos().distanceTo(renderPos);
         float scale = MathHelper.clamp(1.0F - distance / 20.0F, 0.5F, 1.0F);
         float pulse = getPulseAlpha(fuse);
         matrices.push();
         matrices.translate(screenPos.x - 6.0F, screenPos.y, 0.0F);
         matrices.scale(scale, scale, 1.0F);
         int width = (int)Fonts.MEDIUM.getFont(11.0F).width(text);
         int x = -width / 2;
         event.getContext().drawRect(x - 3, 1.0F, width + 26, Fonts.MEDIUM.getFont(11.0F).height() + 8.0F, new ColorRGBA(0.0F, 0.0F, 0.0F, 100.0F * pulse));
         matrices.pop();
      }
   }

   private void renderText(PreHudRenderEvent event, MatrixStack matrices, TntEntity entity) {
      int fuse = entity.getFuse();
      float seconds = fuse / 20.0F;
      String text = Localizator.translate("modules.tnt_timer.format", seconds);
      Vec3d renderPos = entity.getLerpedPos(event.getTickDelta()).add(0.0, 0.5, 0.0);
      Vec2f screenPos = Utils.worldToScreen(renderPos);
      if (screenPos != null) {
         float distance = (float)mc.player.getPos().distanceTo(renderPos);
         float scale = MathHelper.clamp(1.0F - distance / 20.0F, 0.5F, 1.0F);
         float pulse = getPulseAlpha(fuse);
         matrices.push();
         matrices.translate(screenPos.x - 6.0F, screenPos.y, 0.0F);
         matrices.scale(scale, scale, 1.0F);
         int width = (int)Fonts.MEDIUM.getFont(11.0F).width(text);
         int x = -width / 2;
         event.getContext().drawText(Fonts.MEDIUM.getFont(11.0F), text, x + 16, 5.0F, ColorRGBA.WHITE.withAlpha(ColorRGBA.WHITE.getAlpha() * pulse));
         event.getContext().drawItem(TNT_STACK, (float)x, 3.0F, 0.75F);
         matrices.pop();
      }
   }

   private float getPulseAlpha(int fuse) {
      if (fuse > 40) return 1.0F;
      pulseAnim.nonono();
      float dark = Math.min(1.0F, pulseAnim.getValue());
      return fuse <= 20 ? dark : 1.0F - (1.0F - dark) * ((fuse - 20) / 20.0F);
   }
}
