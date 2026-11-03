package com.isusdlc.ui.hud.impl;

import com.isusdlc.elegant;
import com.isusdlc.framework.base.UIContext;
import com.isusdlc.framework.msdf.Font;
import com.isusdlc.framework.msdf.Fonts;
import com.isusdlc.framework.objects.BorderRadius;
import com.isusdlc.framework.objects.MouseButton;
import com.isusdlc.systems.modules.modules.visuals.Interface;
import com.isusdlc.systems.setting.settings.ModeSetting;
import com.isusdlc.systems.theme.Theme;
import com.isusdlc.ui.hud.HudElement;
import com.isusdlc.utility.animation.base.Animation;
import com.isusdlc.utility.animation.base.Easing;
import com.isusdlc.utility.colors.ColorRGBA;
import com.isusdlc.utility.colors.Colors;
import com.isusdlc.utility.game.EntityUtility;
import com.isusdlc.utility.game.TextUtility;
import com.isusdlc.utility.gui.GuiUtility;
import com.isusdlc.utility.time.Timer;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.network.AbstractClientPlayerEntity;

public class TargetHud extends HudElement {
   private final ModeSetting armor = new ModeSetting(this, "hud.targethud.armor");
   private final ModeSetting.Value armorNone = new ModeSetting.Value(this.armor, "hud.targethud.armor.none");
   private final ModeSetting.Value armorIcon = new ModeSetting.Value(this.armor, "hud.targethud.armor.icon").select();
   private final Animation content = new Animation(300L, 0.0F, Easing.BAKEK_SIZE);
   private final Animation health = new Animation(300L, 0.0F, Easing.BAKEK);
   private final Animation golden = new Animation(300L, 0.0F, Easing.BAKEK);
   private final Animation number = new Animation(300L, 0.0F, Easing.FIGMA_EASE_IN_OUT);
   private final Animation itemsX = new Animation(300L, 0.0F, Easing.BAKEK);
   private final Animation copy = new Animation(300L, 0.0F, Easing.BAKEK);
   private final Animation success = new Animation(500L, 0.0F, Easing.BAKEK_SIZE);
   private final Animation eatingPulse = new Animation(150L, 0.0F, Easing.BAKEK);
   private final Animation pulseIntensity = new Animation(50L, 0.0F, Easing.SINE_IN_OUT);
   private final Animation[] items = new Animation[4];
   private LivingEntity target;
   private final Timer copyTimer = new Timer();
   private boolean copied;

   public TargetHud() {
      super("hud.targethud", "icons/hud/target.png");

      for (int i = 0; i < this.items.length; i++) {
         this.items[i] = new Animation(300L, 0.0F, Easing.BAKEK);
      }
   }

   @Override
   public void update(UIContext context) {
      this.width = 110.0F;
      this.height = 26.0F;
      super.update(context);
   }

   @Override
   protected void renderComponent(UIContext context) {
      LivingEntity target = this.getTarget();
      if (target != null) {
         this.target = target;
      }

      if (this.target != null) {
         Font nameFont = Fonts.MEDIUM.getFont(7.5F);
         Font hpFont = Fonts.SEMIBOLD.getFont(7.0F);
         boolean dark = elegant.getInstance().getThemeManager().getCurrentTheme() == Theme.DARK;
         ColorRGBA bgColor = Colors.getBackgroundColor().withAlpha(220.0F);

         this.content.update(this.animation.getValue() * this.visible.getValue() >= 1.0F);

         float baseHealth = this.target instanceof PlayerEntity player
            ? EntityUtility.getHealth(player)
            : this.target.getHealth();
         float absorption = this.target.getAbsorptionAmount();
         float displayHealth = baseHealth + absorption;
         float maxHealth = this.target.getMaxHealth() + (absorption > 0.0F ? 20.0F : 0.0F);

         this.health.update(maxHealth <= 0.0F ? 0.0F : displayHealth / maxHealth);
         this.number.update(displayHealth);

         if (this.animation.getValue() == 0.0F) {
            return;
         }

         context.drawShadow(
            this.x - 4.0F,
            this.y - 4.0F,
            this.width + 8.0F,
            this.height + 8.0F,
            15.0F,
            BorderRadius.all(7.0F),
            ColorRGBA.BLACK.withAlpha(60.0F * this.dragAnim.getValue())
         );

         if (Interface.showMinimalizm()) {
            context.drawBlurredRect(
               this.x,
               this.y,
               this.width,
               this.height,
               11.25F,
               7.0F,
               BorderRadius.all(7.0F),
               ColorRGBA.WHITE.withAlpha(255.0F * this.animation.getValue() * Interface.minimalizm())
            );
         }

         if (Interface.showGlass()) {
            context.drawLiquidGlass(
               this.x,
               this.y,
               this.width,
               this.height,
               7.0F,
               Interface.getDistortion() - 0.07F * this.dragAnim.getValue(),
               BorderRadius.all(7.0F),
               Colors.getLiquidGlassColor().withAlpha(255.0F * this.animation.getValue() * Interface.glass())
            );
         }

         context.drawSquircle(this.x, this.y, this.width, this.height, 7.0F, BorderRadius.all(7.0F), bgColor);

         float alpha = 255.0F * this.content.getValue();
         float paddingX = 6.0F;
         float headSize = 20.0F;
         float centerY = this.y + this.height / 2.0F;

         if (this.target instanceof AbstractClientPlayerEntity playerxx) {
            context.drawHead(
               playerxx,
               this.x + paddingX,
               centerY - headSize / 2.0F,
               headSize,
               BorderRadius.all(3.0F),
               Colors.WHITE.withAlpha(alpha)
            );
         } else {
            context.drawRoundedTexture(
               elegant.id(dark ? "icons/hud/whodark.png" : "icons/hud/who.png"),
               this.x + paddingX,
               centerY - headSize / 2.0F,
               headSize,
               headSize,
               BorderRadius.all(3.0F),
               Colors.WHITE.withAlpha(alpha)
            );
         }

         float textStartX = this.x + paddingX + headSize + 4.0F;

         String name = this.target.getName().getString();
         context.drawFadeoutText(
            nameFont,
            name,
            textStartX,
            this.y + 5.0F,
            Colors.getTextColor().withAlpha(alpha),
            0.7F,
            1.0F,
            this.width - textStartX + this.x - 30.0F
         );

         String hpText = "HP: " + TextUtility.formatNumber(this.number.getValue()).replace(",", ".");
         float hpWidth = hpFont.width(hpText);
         float hpX = this.x + this.width - paddingX - hpWidth;
         context.drawText(
            hpFont,
            hpText,
            hpX,
            this.y + 6.0F,
            Colors.getTextColor().withAlpha(alpha)
         );

         float barPaddingX = textStartX;
         float barY = this.y + this.height - 7.0F;
         float barWidth = this.width - barPaddingX - paddingX;
         float barHeight = 3.0F;

         context.drawRoundedRect(
            this.x + barPaddingX,
            barY,
            barWidth,
            barHeight,
            BorderRadius.all(2.0F),
            Colors.getAdditionalColor().withAlpha(alpha * 0.4F)
         );

         float healthWidth = barWidth * Math.clamp(this.health.getValue(), 0.0F, 1.0F);
         context.drawRoundedRect(
            this.x + barPaddingX,
            barY,
            healthWidth,
            barHeight,
            BorderRadius.all(2.0F),
            Colors.getAccentColor().withAlpha(alpha)
         );
      }
   }

   private LivingEntity getTarget() {
      Object current = elegant.getInstance().getTargetManager().getCurrentTarget();
      LivingEntity mainTarget = current instanceof LivingEntity ? (LivingEntity)current : null;
      if (mainTarget != null) {
         return mainTarget;
      } else if (mc.targetedEntity instanceof LivingEntity livingEntity) {
         return livingEntity;
      } else {
         return mc.currentScreen instanceof ChatScreen ? mc.player : null;
      }
   }

   @Override
   public void onMouseClicked(double mouseX, double mouseY, MouseButton button) {
      if (GuiUtility.isHovered((double)(this.x + 30.0F), (double)(this.y + 3.0F + 6.0F * this.content.getValue()), 60.0, 6.0, mouseX, mouseY)) {
         if (this.target != null) {
            TextUtility.copyText(this.target.getName().getString());
            this.copyTimer.reset();
            this.copied = true;
         }
      } else {
         super.onMouseClicked(mouseX, mouseY, button);
      }
   }

   @Override
   public boolean show() {
      if (!Interface.showTargetHud()) {
         return false;
      }

      LivingEntity target = this.getTarget();
      return target != null && !target.isInvisible();
   }
}
