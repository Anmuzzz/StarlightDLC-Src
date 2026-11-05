package com.isusdlc.framework.base;

import com.isusdlc.elegant;
import com.isusdlc.framework.msdf.Font;
import com.isusdlc.framework.msdf.MsdfRenderer;
import com.isusdlc.framework.objects.BorderRadius;
import com.isusdlc.framework.objects.gradient.Gradient;
import com.isusdlc.mixin.accessors.DrawContextAccessor;
import com.isusdlc.systems.modules.modules.visuals.Interface;
import com.isusdlc.systems.theme.Theme;
import com.isusdlc.utility.colors.ColorRGBA;
import com.isusdlc.utility.colors.Colors;
import com.isusdlc.utility.interfaces.IMinecraft;
import com.isusdlc.utility.render.DrawUtility;
import com.isusdlc.utility.render.obj.CustomSprite;
import com.isusdlc.utility.render.obj.Rect;
import net.minecraft.client.render.item.ItemRenderState;
import net.minecraft.util.crash.CrashReport;
import net.minecraft.util.crash.CrashReportSection;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.crash.CrashException;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import net.minecraft.util.math.Vec2f;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.client.render.DiffuseLighting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.item.ModelTransformationMode;
import net.minecraft.client.render.VertexConsumerProvider.Immediate;
import org.jetbrains.annotations.Nullable;

public class CustomDrawContext extends DrawContext implements IMinecraft {
   private final DrawContext originalContext;

   protected CustomDrawContext(DrawContext originalContext) {
      super(MinecraftClient.getInstance(), ((DrawContextAccessor)originalContext).getVertexConsumers());
      this.originalContext = originalContext;
   }

   public static CustomDrawContext of(DrawContext originalContext) {
      return new CustomDrawContext(originalContext);
   }


   private void beginFontBatch(Font font) {
      if (!com.isusdlc.utility.render.batching.Batching.AUTO) return;
      var active = com.isusdlc.utility.render.batching.Batching.getActive();
      if (active instanceof com.isusdlc.utility.render.batching.impl.FontBatching) return;
      com.isusdlc.utility.render.batching.Batching.flushActive();
      new com.isusdlc.utility.render.batching.impl.FontBatching(net.minecraft.client.render.VertexFormats.POSITION_TEXTURE_COLOR, font.getFont());
   }

   private void flushHudBatch() {
      com.isusdlc.utility.render.batching.Batching.flushActive();
   }

   public void drawClientRect(float x, float y, float width, float height, float alpha, float dragAnim, float squircle) {
      this.flushHudBatch();
      if (Interface.showMinimalizm()) {
         this.drawBlurredRect(x, y, width, height, 11.25F, squircle, BorderRadius.all(8.0F), ColorRGBA.WHITE.withAlpha(255.0F * alpha * Interface.minimalizm()));
      }

      if (Interface.showGlass()) {
         this.drawLiquidGlass(
            x,
            y,
            width,
            height,
            squircle,
            Interface.getDistortion() - 0.07F * dragAnim,
            BorderRadius.all(8.0F),
            Colors.getLiquidGlassColor().withAlpha(255.0F * alpha * Interface.glass())
         );
      }

      boolean dark;
       if (Theme.DARK == elegant.getInstance().getThemeManager().getCurrentTheme()) dark = true;
       else dark = false;
       this.drawSquircle(
         x, y, width, height, squircle, BorderRadius.all(8.0F), Colors.getBackgroundColor().withAlpha(255.0F * (dark ? 0.8F - 0.6F * Interface.glass() : 0.7F))
      );
   }

   public void pushMatrix() {
      this.getMatrices().push();
   }

   public void popMatrix() {
      this.getMatrices().pop();
   }

   public void drawRect(float x, float y, float width, float height, ColorRGBA color) {
      this.flushHudBatch();
      DrawUtility.drawRect(this.getMatrices(), x, y, width, height, color);
   }

   public void drawLine(Vec2f from, Vec2f to, ColorRGBA color) {
      this.flushHudBatch();
      DrawUtility.drawLine(this.getMatrices(), from, to, color);
   }

   public void drawBezier(Vec2f p0, Vec2f p1, Vec2f p2, Vec2f p3, ColorRGBA color, int resolution) {
      this.flushHudBatch();
      DrawUtility.drawBezier(this.getMatrices(), p0, p1, p2, p3, color, resolution);
   }

   public void drawSquircle(float x, float y, float width, float height, float squirt, BorderRadius borderRadius, ColorRGBA color) {
      this.flushHudBatch();
      DrawUtility.drawSquircle(this.getMatrices(), x, y, width, height, squirt, borderRadius, color);
   }

   public void drawRoundedRect(float x, float y, float width, float height, BorderRadius borderRadius, ColorRGBA color) {
      this.flushHudBatch();
      DrawUtility.drawRoundedRect(this.getMatrices(), x, y, width, height, borderRadius, color);
   }

   public void drawRoundedRect(float x, float y, float width, float height, BorderRadius borderRadius, Gradient gradient) {
      this.flushHudBatch();
      DrawUtility.drawRoundedRect(this.getMatrices(), x, y, width, height, borderRadius, gradient);
   }

   public void drawLiquidGlass(float x, float y, float width, float height, float squirt, float power, BorderRadius borderRadius, ColorRGBA color) {
      this.flushHudBatch();
      borderRadius = new BorderRadius(
         borderRadius.topLeftRadius() * squirt / 2.0F,
         borderRadius.topRightRadius() * squirt / 2.0F,
         borderRadius.bottomLeftRadius() * squirt / 2.0F,
         borderRadius.bottomRightRadius() * squirt / 2.0F
      );
      DrawUtility.drawLiquidGlass(
         this.getMatrices(),
         x - 5.0F * Interface.minimalizm(),
         y - 5.0F * Interface.minimalizm(),
         width + 10.0F * Interface.minimalizm(),
         height + 10.0F * Interface.minimalizm(),
         borderRadius,
         color,
         color.getAlpha() / 255.0F * Interface.glass(),
         (height == 240.0F ? 100 : 50) * Interface.glass(),
         color.withAlpha(255.0F),
         1.0F,
         true,
         0.0F,
         power * Interface.glass(),
         squirt,
         false
      );
   }

   public void drawLiquidGlass(float x, float y, float width, float height, float squirt, BorderRadius borderRadius, ColorRGBA color, boolean clean) {
      this.flushHudBatch();
      borderRadius = new BorderRadius(
         borderRadius.topLeftRadius() * squirt / 2.0F,
         borderRadius.topRightRadius() * squirt / 2.0F,
         borderRadius.bottomLeftRadius() * squirt / 2.0F,
         borderRadius.bottomRightRadius() * squirt / 2.0F
      );
      DrawUtility.drawLiquidGlass(
         this.getMatrices(),
         x,
         y,
         width,
         height,
         borderRadius,
         color,
         color.getAlpha() / 255.0F,
         height == 240.0F ? 100.0F : 50.0F,
         color.withAlpha(255.0F),
         1.0F,
         true,
         0.0F,
         0.08F,
         squirt,
         clean
      );
   }

   public void drawLoadingRect(float x, float y, float width, float height, float progress, BorderRadius borderRadius, ColorRGBA color) {
      this.flushHudBatch();
      DrawUtility.drawLoadingRect(this.getMatrices(), x, y, width, height, progress, borderRadius, color);
   }

   public void drawRoundedBorder(float x, float y, float width, float height, float borderThickness, BorderRadius borderRadius, ColorRGBA borderColor) {
      this.flushHudBatch();
      DrawUtility.drawRoundedBorder(this.getMatrices(), x, y, width, height, borderThickness, borderRadius, borderColor);
   }

   public void drawTexture(Identifier identifier, Rect rect) {
      this.flushHudBatch();
      this.drawTexture(identifier, rect, ColorRGBA.WHITE);
   }

   public void drawTexture(Identifier identifier, Rect rect, ColorRGBA color) {
      this.flushHudBatch();
      DrawUtility.drawTexture(this.getMatrices(), identifier, rect.getX(), rect.getY(), rect.getWidth(), rect.getHeight(), color);
   }

   public void drawTexture(Identifier identifier, float x, float y, float width, float height) {
      this.flushHudBatch();
      DrawUtility.drawTexture(this.getMatrices(), identifier, x, y, width, height, ColorRGBA.WHITE);
   }

   public void drawTexture(Identifier identifier, float x, float y, float width, float height, float u1, float u2, float v1, float v2, ColorRGBA color) {
      this.flushHudBatch();
      DrawUtility.drawTexture(this.getMatrices(), identifier, x, y, width, height, u1, u2, v1, v2, color);
   }

   public void drawTexture(Identifier identifier, float x, float y, float width, float height, ColorRGBA textureColor) {
      this.flushHudBatch();
      DrawUtility.drawTexture(this.getMatrices(), identifier, x, y, width, height, textureColor);
   }

   public void drawSprite(CustomSprite sprite, float x, float y, float width, float height, ColorRGBA textureColor) {
      this.flushHudBatch();
      DrawUtility.drawSprite(this.getMatrices(), sprite, x, y, width, height, textureColor);
   }

   public void drawRoundedTexture(Identifier identifier, float x, float y, float width, float height, BorderRadius borderRadius) {
      this.flushHudBatch();
      DrawUtility.drawRoundedTexture(this.getMatrices(), identifier, x, y, width, height, borderRadius);
   }

   public void drawRoundedTexture(Identifier identifier, float x, float y, float width, float height, BorderRadius borderRadius, ColorRGBA color) {
      this.flushHudBatch();
      DrawUtility.drawRoundedTexture(this.getMatrices(), identifier, x, y, width, height, borderRadius, color);
   }

   public void drawShadow(float x, float y, float width, float height, float softness, BorderRadius borderRadius, ColorRGBA color) {
      this.flushHudBatch();
      DrawUtility.drawShadow(this.getMatrices(), x, y, width, height, softness, borderRadius, color);
   }

   public void drawBlurredRect(float x, float y, float width, float height, float blurRadius, BorderRadius borderRadius, ColorRGBA color) {
      this.flushHudBatch();
      DrawUtility.drawBlur(this.getMatrices(), x, y, width, height, blurRadius, borderRadius, color);
   }

   public void drawBlurredRect(float x, float y, float width, float height, float blurRadius, float squirt, BorderRadius borderRadius, ColorRGBA color) {
      this.flushHudBatch();
      DrawUtility.drawBlur(this.getMatrices(), x, y, width, height, blurRadius, squirt, borderRadius, color);
   }

   public void drawText(Font font, String text, float x, float y, ColorRGBA color) {
      this.beginFontBatch(font);
      MsdfRenderer.renderText(font.getFont(), text, font.getSize(), color.getRGB(), this.getMatrices().peek().getPositionMatrix(), x, y, 0.0F);
   }

   public void drawText(Font font, Text text, float x, float y) {
      this.beginFontBatch(font);
      MsdfRenderer.renderText(font.getFont(), text, font.getSize(), this.getMatrices().peek().getPositionMatrix(), x, y, 0.0F);
   }

   public void drawFadeoutText(Font font, String text, float x, float y, ColorRGBA color, float fadeoutStart, float fadeoutEnd) {
      this.beginFontBatch(font);
      MsdfRenderer.renderText(
         font.getFont(), text, font.getSize(), color.getRGB(), this.getMatrices().peek().getPositionMatrix(), x, y, 0.0F, true, fadeoutStart, fadeoutEnd
      );
   }

   public void drawFadeoutText(Font font, String text, float x, float y, ColorRGBA color, float fadeoutStart, float fadeoutEnd, float maxWidth) {
      this.beginFontBatch(font);
      MsdfRenderer.renderText(
         font.getFont(),
         text,
         font.getSize(),
         color.getRGB(),
         this.getMatrices().peek().getPositionMatrix(),
         x,
         y,
         0.0F,
         true,
         fadeoutStart,
         fadeoutEnd,
         maxWidth
      );
   }

   public void drawCenteredText(Font font, String text, float x, float y, ColorRGBA color) {
      this.beginFontBatch(font);
      this.drawText(font, text, x - font.getFont().getWidth(text, font.getSize()) / 2.0F, y, color);
   }

   public void drawRightText(Font font, String text, float x, float y, ColorRGBA color) {
      this.beginFontBatch(font);
      this.drawText(font, text, x - font.getFont().getWidth(text, font.getSize()), y, color);
   }

   public void drawItem(Item item, float x, float y, float size) {
      this.flushHudBatch();
      this.drawItem(item.getDefaultStack(), x, y, size);
   }

   public void drawItem(ItemStack item, float x, float y, float size) {
      this.flushHudBatch();
      this.getMatrices().push();
      this.getMatrices().translate(x, y, 0.0F);
      this.getMatrices().scale(size, size, size);
      DiffuseLighting.disableGuiDepthLighting();
      this.drawItem(item, 0, 0);
      DiffuseLighting.disableGuiDepthLighting();
      this.getMatrices().pop();
   }

   public void drawHead(AbstractClientPlayerEntity player, float x, float y, float size, BorderRadius borderRadius, ColorRGBA color) {
      this.flushHudBatch();
      DrawUtility.drawPlayerHeadWithHat(this.getMatrices(), player, x, y, size, borderRadius, color);
   }

   public void drawHead(LivingEntity entity, float x, float y, float size, BorderRadius borderRadius, ColorRGBA color) {
      this.flushHudBatch();
      DrawUtility.drawEntityHeadWithHat(this.getMatrices(), entity, x, y, size, borderRadius, color);
   }

   public void drawBatchItem(ItemStack item, int x, int y) {
      this.flushHudBatch();
      this.drawBatchItem(mc.player, mc.world, item, x, y, 0);
   }

   private void drawBatchItem(@Nullable LivingEntity entity, @Nullable World world, ItemStack stack, int x, int y, int seed) {
      this.drawBatchItem(entity, world, stack, x, y, seed, 0);
   }

   private void drawBatchItem(@Nullable LivingEntity entity, @Nullable World world, ItemStack stack, int x, int y, int seed, int z) {
      MatrixStack matrices = this.getMatrices();
      ItemRenderState itemRenderState = ((DrawContextAccessor)this.originalContext).getItemRenderState();
      Immediate vertexConsumers = ((DrawContextAccessor)this.originalContext).getVertexConsumers();
      if (!stack.isEmpty()) {
         mc.getItemModelManager().update(itemRenderState, stack, ModelTransformationMode.GUI, false, world, entity, seed);
         matrices.push();
         matrices.translate(x + 8, y + 8, 150 + (itemRenderState.hasDepth() ? z : 0));

         try {
            matrices.scale(16.0F, -16.0F, 16.0F);
            boolean bl = !itemRenderState.isSideLit();
            if (bl) {
               DiffuseLighting.disableGuiDepthLighting();
            }

            itemRenderState.render(matrices, vertexConsumers, 15728880, OverlayTexture.DEFAULT_UV);
            if (bl) {
               DiffuseLighting.enableGuiDepthLighting();
            }
         } catch (Throwable var14) {
            CrashReport crashReport = CrashReport.create(var14, "Rendering item");
            CrashReportSection crashReportSection = crashReport.addElement("Item being rendered");
            crashReportSection.add("Item Type", () -> String.valueOf(stack.getItem()));
            crashReportSection.add("Item Components", () -> String.valueOf(stack.getComponents()));
            crashReportSection.add("Item Foil", () -> String.valueOf(stack.hasGlint()));
            throw new CrashException(crashReport);
         }

         matrices.pop();
      }
   }
}
