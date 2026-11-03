package com.isusdlc.systems.modules.modules.visuals;

import com.mojang.blaze3d.platform.GlStateManager.DstFactor;
import com.mojang.blaze3d.platform.GlStateManager.SrcFactor;
import com.mojang.blaze3d.systems.RenderSystem;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import lombok.Generated;
import com.isusdlc.framework.msdf.Fonts;
import com.isusdlc.framework.objects.BorderRadius;
import com.isusdlc.systems.event.EventListener;
import com.isusdlc.systems.event.impl.network.ReceivePacketEvent;
import com.isusdlc.systems.event.impl.render.PreHudRenderEvent;
import com.isusdlc.systems.event.impl.render.Render3DEvent;
import com.isusdlc.systems.modules.api.ModuleCategory;
import com.isusdlc.systems.modules.api.ModuleInfo;
import com.isusdlc.systems.modules.impl.BaseModule;
import com.isusdlc.utility.animation.base.Animation;
import com.isusdlc.utility.animation.base.Easing;
import com.isusdlc.utility.colors.ColorRGBA;
import com.isusdlc.utility.interfaces.IMinecraft;
import com.isusdlc.utility.render.Draw3DUtility;
import com.isusdlc.utility.render.Utils;
import com.isusdlc.utility.time.Timer;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;
import net.minecraft.network.packet.s2c.play.PlaySoundS2CPacket;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.util.math.MathHelper;
import net.minecraft.client.render.Camera;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.render.BuiltBuffer;
import net.minecraft.client.render.VertexFormat.DrawMode;

@ModuleInfo(
   name = "Object Info",
   category = ModuleCategory.PLAYER,
   desc = "Показывает информацию о трапках и пластах в мире"
)
public class ObjectInfo extends BaseModule {
   private final Map<BlockPos, ObjectInfo.Info> infos = new HashMap<>();
   private final Timer timer = new Timer();
   private final EventListener<ReceivePacketEvent> onSoundInstanceEvent = event -> {
      if (event.getPacket() instanceof PlaySoundS2CPacket sound) {
         String soundName = sound.getSound().getIdAsString();
         if (soundName.contains("minecraft:block.piston.extend") || soundName.contains("minecraft:block.piston.contract")) {
            BlockPos pos = BlockPos.ofFloored(sound.getX(), sound.getY(), sound.getZ());
            if ((sound.getVolume() == 0.5F || sound.getVolume() == 0.7F) && sound.getPitch() == 0.5F) {
               this.infos.put(pos, new ObjectInfo.Info(pos.up().add(0, 0, 0), ObjectInfo.ObjType.TRAP));
            }

            this.timer.reset();
         }

         if (soundName.contains("minecraft:block.anvil.place")) {
            BlockPos pos = BlockPos.ofFloored(sound.getX(), sound.getY(), sound.getZ());
            if ((sound.getVolume() == 0.5F || sound.getVolume() == 0.7F) && (sound.getPitch() == 1.1F || sound.getPitch() == 0.5F)) {
               this.infos.put(pos, new ObjectInfo.Info(pos.up().add(0, 0, 0), ObjectInfo.ObjType.PLAST));
            }

            this.timer.reset();
         }

         if (soundName.contains("entity.evoker_fangs.attack")) {
            new BlockPos((int)sound.getX(), (int)sound.getY(), (int)sound.getZ());
            if ((sound.getVolume() == 0.5F || sound.getVolume() == 0.7F) && sound.getPitch() != 0.85F && sound.getPitch() == 1.0F) {
            }

            this.timer.reset();
         }
      }
   };
   private final EventListener<PreHudRenderEvent> onRender2D = event -> {
      BlockPos toRemove = null;

      for (Entry<BlockPos, ObjectInfo.Info> entry : this.infos.entrySet()) {
         ObjectInfo.Info info = entry.getValue();
         info.draw(event);
         if (info.start.finished(info.getType().getTime())) {
            toRemove = entry.getKey();
         }
      }

      if (toRemove != null) {
         this.infos.remove(toRemove);
      }
   };
   private final EventListener<Render3DEvent> onRender3D = event -> {
      BlockPos toRemove = null;

      for (Entry<BlockPos, ObjectInfo.Info> entry : this.infos.entrySet()) {
         ObjectInfo.Info info = entry.getValue();
         info.draw3D(event);
         if (info.start.finished(info.getType().getTime())) {
            toRemove = entry.getKey();
         }
      }

      if (toRemove != null) {
         this.infos.remove(toRemove);
      }
   };

   static class Info {
      final BlockPos pos;
      final ObjectInfo.ObjType type;
      Timer start = new Timer();
      final Animation alphaAnim = new Animation(300L, 0.0F, Easing.CUBIC_OUT);

      void draw(PreHudRenderEvent e) {
         long elapsed = this.start.getElapsedTime();
         long total = this.type.getTime();
         float fadeTarget = elapsed < 300L ? 1.0F : (total - elapsed < 500L ? 0.0F : 1.0F);
         float alpha = this.alphaAnim.update(fadeTarget);
         if (alpha < 0.01F) return;

         int remained = (int)((float)(total - elapsed) / 1000.0F);
         MatrixStack matrices = e.getContext().getMatrices();
         BlockPos renderPos = this.pos;
         Vec3d renderPosAdjusted = renderPos.add(0, 1, 0).toCenterPos();
         Vec2f screenPos = Utils.worldToScreen(renderPosAdjusted);
         if (screenPos != null) {
            float distance = (float)IMinecraft.mc.player.getPos().distanceTo(Vec3d.of(renderPos));
            float scale = MathHelper.clamp(1.0F - distance / 20.0F, 0.5F, 1.0F);
            matrices.push();
            matrices.translate(screenPos.x, screenPos.y, 0.0F);
            matrices.scale(scale, scale, 1.0F);
            String text = this.type.getName() + " (" + remained + " sec)";
            int width = (int)Fonts.MEDIUM.getFont(11.0F).width(text);
            int x = -width / 2 - 9;
            e.getContext()
               .drawRoundedRect(
                  (float)(x - 3),
                  2.0F,
                  (float)(width + 24),
                  Fonts.MEDIUM.getFont(11.0F).height() + 9.0F,
                  BorderRadius.top(3.0F, 3.0F),
                  new ColorRGBA(0.0F, 0.0F, 0.0F, 100.0F * alpha)
               );
            e.getContext()
               .drawRoundedRect(
                  (float)(x - 3),
                  Fonts.MEDIUM.getFont(11.0F).height() + 9.0F,
                  (width + 24) * (1.0F - (float)elapsed / (float)total),
                  2.0F,
                  BorderRadius.bottom(0.1F, 0.1F),
                  new ColorRGBA(255.0F, 0.0F, 0.0F)
               );
            e.getContext().drawText(Fonts.MEDIUM.getFont(11.0F), text, x + 14, 5.0F, ColorRGBA.WHITE.withAlpha(ColorRGBA.WHITE.getAlpha() * alpha));
            e.getContext().drawItem(this.type.getItem(), (float)x, 3.0F, 0.75F);
            matrices.pop();
         }
      }

      void draw3D(Render3DEvent e) {
         if (IMinecraft.mc.world != null && IMinecraft.mc.player != null && this.type != ObjectInfo.ObjType.PLAST) {
            MatrixStack matrices = e.getMatrices();
            Camera camera = IMinecraft.mc.gameRenderer.getCamera();
            Vec3d cameraPos = camera.getPos();
            matrices.push();
            matrices.translate(-cameraPos.getX(), -cameraPos.getY(), -cameraPos.getZ());
            RenderSystem.enableBlend();
            RenderSystem.disableDepthTest();
            RenderSystem.disableCull();
            RenderSystem.blendFunc(SrcFactor.SRC_ALPHA, DstFactor.ONE);
            RenderSystem.lineWidth(10.0F);
            RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
            BufferBuilder buffer = Tessellator.getInstance().begin(DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);
            int radius = 2;
            ColorRGBA color = ColorRGBA.RED.withAlpha(110.0F);
            switch (this.type) {
               case TRAP:
                  color = ColorRGBA.RED.withAlpha(110.0F);
                  break;
               case DRAGON_FT:
               case DRAGON_ST:
                  radius = 4;
                  color = ColorRGBA.YELLOW.withAlpha(150.0F);
            }

            BlockPos minPos = this.pos.add(-radius, -radius, -radius);
            BlockPos maxPos = this.pos.add(radius, radius, radius);
            Draw3DUtility.renderOutlinedBox(
               e.getMatrices(),
               buffer,
               new Box(
                  minPos.getX(),
                  minPos.getY(),
                  minPos.getZ(),
                  maxPos.getX() + 1,
                  maxPos.getY() + 1,
                  maxPos.getZ() + 1
               ),
               color
            );
            BuiltBuffer builtBuffer = buffer.endNullable();
            if (builtBuffer != null) {
               BufferRenderer.drawWithGlobalProgram(builtBuffer);
            }

            RenderSystem.enableCull();
            RenderSystem.enableDepthTest();
            RenderSystem.disableBlend();
            matrices.pop();
         }
      }

      @Generated
      public BlockPos getPos() {
         return this.pos;
      }

      @Generated
      public ObjectInfo.ObjType getType() {
         return this.type;
      }

      @Generated
      public Timer getStart() {
         return this.start;
      }

      @Generated
      public Info(BlockPos pos, ObjectInfo.ObjType type) {
         this.pos = pos;
         this.type = type;
      }
   }

   static enum ObjType {
      TRAP("Трапка", Items.NETHERITE_SCRAP, 15000L),
      DRAGON_FT("Драконка", Items.NETHERITE_SCRAP, 30000L),
      DRAGON_ST("Драконка", Items.NETHERITE_SCRAP, 60000L),
      PLAST("Пласт", Items.DRIED_KELP, 20000L);

      final String name;
      final Item item;
      final long time;

      @Generated
      public String getName() {
         return this.name;
      }

      @Generated
      public Item getItem() {
         return this.item;
      }

      @Generated
      public long getTime() {
         return this.time;
      }

      @Generated
      private ObjType(final String name, final Item item, final long time) {
         this.name = name;
         this.item = item;
         this.time = time;
      }
   }
}
