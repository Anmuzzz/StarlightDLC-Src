package com.isusdlc.systems.modules.modules.visuals;

import com.isusdlc.systems.event.EventListener;
import com.isusdlc.systems.event.impl.player.ClientPlayerTickEvent;
import com.isusdlc.systems.event.impl.render.Render3DEvent;
import com.isusdlc.systems.modules.api.ModuleCategory;
import com.isusdlc.systems.modules.api.ModuleInfo;
import com.isusdlc.systems.modules.impl.BaseModule;
import com.isusdlc.systems.setting.settings.ColorSetting;
import com.isusdlc.systems.setting.settings.SliderSetting;
import com.isusdlc.utility.colors.ColorRGBA;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@ModuleInfo(
   name = "Jump Circle",
   category = ModuleCategory.VISUALS,
   desc = "Рисует круг под игроком при прыжке"
)
public class JumpCircle extends BaseModule {

   private final SliderSetting time = new SliderSetting(this, "Time")
      .min(200.0F).max(2000.0F).step(100.0F).currentValue(1000.0F);
   private final SliderSetting radius = new SliderSetting(this, "Radius")
      .min(0.2F).max(2.0F).step(0.1F).currentValue(0.7F);
   private final ColorSetting color = new ColorSetting(this, "Color").color(ColorRGBA.WHITE);

   private final List<CircleData> circles = new ArrayList<>();
   private boolean wasOnGround = true;

   private final EventListener<ClientPlayerTickEvent> onTick = event -> {
      if (mc.player == null) return;
      boolean onGround = mc.player.isOnGround();
      if (!wasOnGround && onGround) {
         circles.add(new CircleData(
            new Vec3d(mc.player.getX(), Math.floor(mc.player.getY()) + 0.05, mc.player.getZ()),
            System.currentTimeMillis()
         ));
      }
      wasOnGround = onGround;

      long now = System.currentTimeMillis();
      circles.removeIf(c -> now - c.birth > time.getCurrentValue());
   };

   private final EventListener<Render3DEvent> onRender = event -> {
      if (circles.isEmpty()) return;

      Vec3d cam = event.getCamera().getPos();
      var matrices = event.getMatrices();
      ColorRGBA col = color.getColor();
      int r = (int) col.getRed();
      int g = (int) col.getGreen();
      int b2 = (int) col.getBlue();
      float maxTime = time.getCurrentValue();

      matrices.push();

      for (CircleData c : circles) {
         float progress = (System.currentTimeMillis() - c.birth) / maxTime;
         float alpha = MathHelper.clamp(1.0F - progress, 0.0F, 1.0F) * 0.8F;
         float rad = radius.getCurrentValue() * (0.5F + progress * 0.5F);

         double x = c.pos.x - cam.x;
         double y = c.pos.y - cam.y;
         double z = c.pos.z - cam.z;

         // отдельный буфер на круг, иначе LINE_STRIP свяжет последний круг со следующим линией
         var buf = Tessellator.getInstance().begin(VertexFormat.DrawMode.LINE_STRIP, VertexFormats.POSITION_COLOR);
         int segs = 48;
         for (int i = 0; i <= segs; i++) {
            double angle = Math.PI * 2 * i / segs;
            float cos = (float) Math.cos(angle);
            float sin = (float) Math.sin(angle);
            buf.vertex(matrices.peek().getPositionMatrix(), (float) x + cos * rad, (float) y, (float) z + sin * rad)
               .color(r, g, b2, (int) (alpha * 255));
         }
         BufferRenderer.drawWithGlobalProgram(buf.end());
      }

      matrices.pop();
   };

   private record CircleData(Vec3d pos, long birth) {}
}
