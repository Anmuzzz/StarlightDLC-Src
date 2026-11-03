package com.isusdlc.systems.modules.modules.visuals;

import com.mojang.blaze3d.systems.RenderSystem;
import java.util.ArrayList;
import java.util.List;
import com.isusdlc.elegant;
import com.isusdlc.framework.msdf.Font;
import com.isusdlc.utility.animation.base.Animation;
import com.isusdlc.utility.animation.base.Easing;
import com.isusdlc.framework.msdf.Fonts;
import com.isusdlc.framework.objects.BorderRadius;
import com.isusdlc.systems.event.EventListener;
import com.isusdlc.systems.event.impl.render.PreHudRenderEvent;
import com.isusdlc.systems.modules.api.ModuleCategory;
import com.isusdlc.systems.modules.api.ModuleInfo;
import com.isusdlc.systems.modules.impl.BaseModule;
import com.isusdlc.systems.setting.settings.SliderSetting;
import com.isusdlc.systems.theme.Theme;
import com.isusdlc.utility.colors.ColorRGBA;
import com.isusdlc.utility.colors.Colors;
import com.isusdlc.utility.render.Utils;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.block.Block;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;
import net.minecraft.block.ShulkerBoxBlock;
import net.minecraft.util.math.MathHelper;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.component.type.ContainerComponent;
import net.minecraft.component.DataComponentTypes;

@ModuleInfo(
   name = "Shulker Preview",
   category = ModuleCategory.VISUALS,
   desc = "Показывает содержимое шалкеров"
)
public class ShulkerPreview extends BaseModule {
   private final SliderSetting scale = new SliderSetting(this, "Размер").min(0.5F).max(2.0F).step(0.1F).currentValue(1.0F);
   private final SliderSetting maxDistance = new SliderSetting(this, "Дистанция").min(10.0F).max(100.0F).step(5.0F).currentValue(50.0F);
   private final Animation scaleAnim = new Animation(300L, 0.0F, Easing.CUBIC_OUT);
   private final EventListener<PreHudRenderEvent> onHudRender = event -> {
      if (event.getContext() != null && mc.player != null && mc.world != null) {
         int totalEntities = 0;
         int shulkerCount = 0;
         int renderedCount = 0;
         int itemEntities = 0;
         int playerEntities = 0;

         for (Entity entity : mc.world.getEntities()) {
            totalEntities++;
            if (entity instanceof ItemEntity itemEntity) {
               itemEntities++;
               ItemStack stack = itemEntity.getStack();
               if (Block.getBlockFromItem(stack.getItem()) instanceof ShulkerBoxBlock) {
                  shulkerCount++;
                  double distance = mc.player.getPos().distanceTo(itemEntity.getPos());
                  if (distance <= this.maxDistance.getCurrentValue() && this.shouldRenderShulker(stack)) {
                     this.renderDroppedShulkerPreview(event, itemEntity, stack, event.getTickDelta());
                     renderedCount++;
                  }
               }
            } else if (entity instanceof PlayerEntity player) {
               playerEntities++;
               if (player != mc.player || !mc.options.getPerspective().isFirstPerson()) {
                  double distance = mc.player.getPos().distanceTo(player.getPos());
                  if (!(distance > this.maxDistance.getCurrentValue())) {
                     ItemStack stack = player.getMainHandStack();
                     if (Block.getBlockFromItem(stack.getItem()) instanceof ShulkerBoxBlock) {
                        shulkerCount++;
                        if (this.shouldRenderShulker(stack)) {
                           this.renderShulkerPreview(event, player, stack, event.getTickDelta());
                           renderedCount++;
                        }
                     }
                  }
               }
            }
         }
      }
   };

   private boolean shouldRenderShulker(ItemStack stack) {
      if (stack.isEmpty()) {
         return false;
      } else if (!(Block.getBlockFromItem(stack.getItem()) instanceof ShulkerBoxBlock)) {
         return false;
      } else {
         ContainerComponent container = (ContainerComponent)stack.get(DataComponentTypes.CONTAINER);
         if (container != null) {
            return container.stream().anyMatch(s -> !s.isEmpty());
         } else {
            return stack.contains(DataComponentTypes.BLOCK_ENTITY_DATA);
         }
      }
   }

   private void renderShulkerPreview(PreHudRenderEvent event, PlayerEntity player, ItemStack stack, float tickDelta) {
      double x = this.interpolate(player.prevX, player.getX(), tickDelta);
      double y = this.interpolate(player.prevY, player.getY(), tickDelta) + player.getHeight() + 0.5;
      double z = this.interpolate(player.prevZ, player.getZ(), tickDelta);
      Vec2f screenPos = Utils.worldToScreen(new Vec3d(x, y, z));
      if (screenPos != null) {
         this.renderShulkerInventory(event, stack, screenPos.x, screenPos.y, player.getPos());
      }
   }

   private void renderDroppedShulkerPreview(PreHudRenderEvent event, ItemEntity itemEntity, ItemStack stack, float tickDelta) {
      double x = this.interpolate(itemEntity.prevX, itemEntity.getX(), tickDelta);
      double y = this.interpolate(itemEntity.prevY, itemEntity.getY(), tickDelta) + 0.5;
      double z = this.interpolate(itemEntity.prevZ, itemEntity.getZ(), tickDelta);
      Vec2f screenPos = Utils.worldToScreen(new Vec3d(x, y, z));
      if (screenPos != null) {
         this.renderShulkerInventory(event, stack, screenPos.x, screenPos.y, itemEntity.getPos());
      }
   }

   private void renderShulkerInventory(PreHudRenderEvent event, ItemStack shulkerStack, float screenX, float screenY, Vec3d entityPos) {
      float animScale = scaleAnim.update(1.0F);
      if (animScale < 0.01F) return;

      ContainerComponent container = (ContainerComponent)shulkerStack.get(DataComponentTypes.CONTAINER);
      List<ItemStack> items = new ArrayList<>();
      if (container != null) {
         items = container.stream().toList();
      }

      while (items.size() < 27) {
         items.add(ItemStack.EMPTY);
      }

      double distance = mc.player.getPos().distanceTo(entityPos);
      double distanceScale = MathHelper.clamp(1.0 - distance / this.maxDistance.getCurrentValue(), 0.3, 1.0);
      float finalScale = (float)(distanceScale * this.scale.getCurrentValue() * animScale);
      float itemSize = 13.0F;
      float gap = 2.0F;
      float paddingX = 7.0F;
      float paddingY = 7.0F;
      int cols = 9;
      int rows = 3;
      float guiWidth = cols * itemSize + (cols - 1) * gap + paddingX * 2.0F;
      float guiHeight = rows * itemSize + (rows - 1) * gap + paddingY * 2.0F;
      float scaledWidth = guiWidth * finalScale;
      float scaledHeight = guiHeight * finalScale;
      float x = screenX - scaledWidth / 2.0F;
      MatrixStack matrices = event.getContext().getMatrices();
      matrices.push();
      matrices.translate(x, screenY, 0.0F);
      matrices.scale(finalScale, finalScale, 1.0F);
      ColorRGBA shulkerColor = this.getShulkerColor(shulkerStack);
      boolean dark = elegant.getInstance().getThemeManager().getCurrentTheme() == Theme.DARK;
      ColorRGBA bgColor = shulkerColor.withAlpha(255.0F * (dark ? 0.8F - 0.6F * Interface.glass() : 0.7F));
      float prev = RenderSystem.getShaderColor()[3];
      event.getContext().drawShadow(-5.0F, -5.0F, guiWidth + 10.0F, guiHeight + 10.0F, 15.0F, BorderRadius.all(6.0F), ColorRGBA.BLACK.withAlpha(63.75F));
      if (Interface.showMinimalizm()) {
         ColorRGBA liquidGlassColor = Colors.getLiquidGlassColor();
         ColorRGBA minimColor = liquidGlassColor.mix(shulkerColor, 0.3F);
         event.getContext()
            .drawBlurredRect(0.0F, 0.0F, guiWidth, guiHeight, 11.25F, 7.0F, BorderRadius.all(6.0F), minimColor.withAlpha(255.0F * Interface.minimalizm()));
      }

      if (Interface.showGlass()) {
         event.getContext()
            .drawLiquidGlass(
               0.0F, 0.0F, guiWidth, guiHeight, 7.0F, Interface.getDistortion(), BorderRadius.all(6.0F), shulkerColor.withAlpha(255.0F * Interface.glass())
            );
      }

      event.getContext().drawSquircle(0.0F, 0.0F, guiWidth, guiHeight, 7.0F, BorderRadius.all(6.0F), bgColor);
      float startX = paddingX;
      float startY = paddingY;

      for (int index = 0; index < 27; index++) {
         int row = index / 9;
         int col = index % 9;
         float itemX = startX + col * (itemSize + gap);
         float itemY = startY + row * (itemSize + gap);
         ItemStack item = index < items.size() ? items.get(index) : ItemStack.EMPTY;
         boolean hasItem = !item.isEmpty();
         RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, prev);
         event.getContext().drawBlurredRect(itemX, itemY, itemSize, itemSize, 1.25F, BorderRadius.all(1.5F), ColorRGBA.WHITE.withAlpha(255.0F));
         RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, prev);
         ColorRGBA slotBgColor = hasItem ? shulkerColor : shulkerColor.mix(ColorRGBA.BLACK, 0.5F);
         event.getContext().drawRoundedRect(itemX, itemY, itemSize, itemSize, BorderRadius.all(1.5F), slotBgColor);
         event.getContext()
            .drawRoundedBorder(itemX, itemY, itemSize, itemSize, 0.3F, BorderRadius.all(1.5F), shulkerColor.mix(ColorRGBA.WHITE, 0.3F).withAlpha(80.0F));
         if (hasItem) {
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, prev);
            event.getContext().drawItem(item, itemX - 11.0F + itemSize / 2.0F + 5.5F, itemY, 0.7F);
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, prev);
            if (item.getCount() > 1) {
               String count = String.valueOf(item.getCount());
               Font countFont = Fonts.REGULAR.getFont(6.0F);
               event.getContext().drawRightText(countFont, count, itemX + itemSize - 2.0F, itemY + itemSize - countFont.height() - 1.0F, Colors.WHITE);
            }
         }
      }

      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
      matrices.pop();
   }

   private double interpolate(double prev, double current, float delta) {
      return prev + (current - prev) * delta;
   }

   private ColorRGBA getShulkerColor(ItemStack shulkerStack) {
      Block block = Block.getBlockFromItem(shulkerStack.getItem());
      if (block instanceof ShulkerBoxBlock shulkerBox) {
         String blockName = block.getTranslationKey();
         if (blockName.contains("white")) {
            return new ColorRGBA(240.0F, 240.0F, 240.0F, 255.0F);
         } else if (blockName.contains("orange")) {
            return new ColorRGBA(240.0F, 140.0F, 70.0F, 255.0F);
         } else if (blockName.contains("magenta")) {
            return new ColorRGBA(190.0F, 80.0F, 190.0F, 255.0F);
         } else if (blockName.contains("light_blue")) {
            return new ColorRGBA(100.0F, 150.0F, 210.0F, 255.0F);
         } else if (blockName.contains("yellow")) {
            return new ColorRGBA(240.0F, 220.0F, 60.0F, 255.0F);
         } else if (blockName.contains("lime")) {
            return new ColorRGBA(110.0F, 190.0F, 30.0F, 255.0F);
         } else if (blockName.contains("pink")) {
            return new ColorRGBA(240.0F, 160.0F, 180.0F, 255.0F);
         } else if (blockName.contains("light_gray")) {
            return new ColorRGBA(150.0F, 150.0F, 150.0F, 255.0F);
         } else if (blockName.contains("gray")) {
            return new ColorRGBA(70.0F, 70.0F, 70.0F, 255.0F);
         } else if (blockName.contains("cyan")) {
            return new ColorRGBA(40.0F, 120.0F, 140.0F, 255.0F);
         } else if (blockName.contains("purple")) {
            return new ColorRGBA(130.0F, 50.0F, 180.0F, 255.0F);
         } else if (blockName.contains("blue")) {
            return new ColorRGBA(50.0F, 60.0F, 170.0F, 255.0F);
         } else if (blockName.contains("brown")) {
            return new ColorRGBA(120.0F, 80.0F, 50.0F, 255.0F);
         } else if (blockName.contains("green")) {
            return new ColorRGBA(80.0F, 100.0F, 30.0F, 255.0F);
         } else if (blockName.contains("red")) {
            return new ColorRGBA(160.0F, 40.0F, 40.0F, 255.0F);
         } else {
            return blockName.contains("black") ? new ColorRGBA(30.0F, 30.0F, 30.0F, 255.0F) : new ColorRGBA(140.0F, 90.0F, 140.0F, 255.0F);
         }
      } else {
         return Colors.getBackgroundColor();
      }
   }
}
