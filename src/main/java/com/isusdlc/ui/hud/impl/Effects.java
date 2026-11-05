package com.isusdlc.ui.hud.impl;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import com.isusdlc.elegant;
import com.isusdlc.framework.base.UIContext;
import com.isusdlc.framework.msdf.Font;
import com.isusdlc.framework.msdf.Fonts;
import com.isusdlc.framework.objects.BorderRadius;
import com.isusdlc.systems.modules.modules.visuals.Interface;
import com.isusdlc.systems.notifications.NotificationType;
import com.isusdlc.systems.setting.settings.BooleanSetting;
import com.isusdlc.ui.components.animated.AnimatedNumber;
import com.isusdlc.ui.hud.HudList;
import com.isusdlc.utility.animation.base.Animation;
import com.isusdlc.utility.animation.base.Easing;
import com.isusdlc.utility.colors.ColorRGBA;
import com.isusdlc.utility.colors.Colors;
import com.isusdlc.utility.game.server.ServerUtility;
import com.isusdlc.utility.gui.GuiUtility;
import com.isusdlc.utility.mixins.StatusEffectInstanceAddition;
import com.isusdlc.utility.render.batching.Batching;
import com.isusdlc.utility.render.batching.impl.FontBatching;
import com.isusdlc.utility.render.batching.impl.IconBatching;
import com.isusdlc.utility.render.batching.impl.RectBatching;
import net.minecraft.client.texture.Sprite;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.entity.effect.StatusEffectCategory;

public class Effects extends HudList {
   private final BooleanSetting alwaysDisplay = new BooleanSetting(this, "hud.always_display");
   int lastSize = -1;
   private final Map<String, StatusEffectInstance> effects = new TreeMap<>();
   private final Map<StatusEffect, Boolean> ended = new HashMap<>();
   private final BooleanSetting alert = new BooleanSetting(this, "hud.effects.alert");

   public Effects() {
      super("hud.effects", "icons/hud/potion.png");
   }

   @Override
   public void update(UIContext context) {
      this.width = 92.0F;
      this.height = 18.0F;
      Collection<StatusEffectInstance> original = mc.player.getStatusEffects();

      for (StatusEffectInstance eff : original) {
         StatusEffect potion = (StatusEffect)eff.getEffectType().value();
         String realName = potion.getName().getString();
         if (realName != null && !ServerUtility.isCM()) {
            if (this.effects.containsKey(realName)) {
               this.effects.replace(realName, eff);
               Animation anim = ((StatusEffectInstanceAddition)eff).elegant$getAnimPotion();
               if (anim.getValue() == 0.0F) {
                  anim.setValue(1.0F);
               }
            } else {
               this.effects.put(realName, eff);
            }
         }
      }

      if (!this.effects.isEmpty()) {
         this.height += 5.0F;
      }

      for (StatusEffectInstance effx : this.effects.values()) {
         Animation anim = ((StatusEffectInstanceAddition)effx).elegant$getAnimPotion();
         StatusEffect potion = (StatusEffect)effx.getEffectType().value();
         if (this.alert.isEnabled()) {
            String effectName = potion.getName().getString() + " " + (effx.getAmplifier() > 0 ? effx.getAmplifier() + 1 : "");
            if (!mc.player.hasStatusEffect(effx.getEffectType())) {
               if (!this.ended.getOrDefault(potion, false) && !potion.getCategory().equals(StatusEffectCategory.HARMFUL)) {
                  elegant.getInstance()
                     .getNotificationManager()
                     .addNotificationOther(NotificationType.INFO, "Эффект " + effectName + " закончился", "Действие эффекта завершено");
                  this.ended.put(potion, true);
               }
            } else {
               this.ended.put(potion, false);
            }
         }

         anim.update(original.contains(effx));
         anim.setEasing(Easing.BAKEK);
         this.width = Math.max(Fonts.REGULAR.getFont(7.0F).width(potion.getName().getString()) + 60.0F, this.width);
         this.height = this.height + 18.0F * anim.getValue();
      }

      super.update(context);
   }

   @Override
   protected void renderComponent(UIContext context) {
      if (mc.player != null && mc.world != null) {
         Font font = Fonts.REGULAR.getFont(7.0F);
         float offset = 22.0F;
         context.drawClientRect(this.x, this.y, this.width, Math.max(20.0F, this.height), this.animation.getValue(), this.dragAnim.getValue(), 7.0F);
         float headerHeight = 18.0F;
         Font headerFont = Fonts.MEDIUM.getFont(8.0F);
         String headerText = "Potions";
         float textWidth = headerFont.width(headerText);
         float textX = this.x + (this.width - textWidth) / 2.0F;
         context.drawText(headerFont, headerText, textX, this.y + GuiUtility.getMiddleOfBox(font.height(), headerHeight) + 0.5F, Colors.getTextColor());
         if (this.height >= 23.0F) {
            context.drawRect(this.x, this.y + headerHeight, this.width, 0.5F, Colors.getTextColor().withAlpha(30.0F));
         }

         StatusEffectInstance toRemove = null;
         Batching split = new RectBatching(VertexFormats.POSITION_COLOR, context.getMatrices());

         for (StatusEffectInstance eff : this.effects.values()) {
            Animation anim = ((StatusEffectInstanceAddition)eff).elegant$getAnimPotion();
            if (anim.getValue() == 0.0F) {
               toRemove = eff;
            } else {
               float off = -4.5F + 4.5F * anim.getValue();
               if (offset != 22.0F) {
                  context.drawRect(this.x, this.y + offset + off, this.width, 0.5F, Colors.getTextColor().withAlpha(5.1F * anim.getValue()));
               }

               offset += 18.0F * anim.getValue();
            }
         }

         split.draw();
         offset = 22.0F;
         Batching texture = new IconBatching(VertexFormats.POSITION_TEXTURE_COLOR, context.getMatrices());

         for (StatusEffectInstance effx : this.effects.values()) {
            Animation anim = ((StatusEffectInstanceAddition)effx).elegant$getAnimPotion();
            if (anim.getValue() != 0.0F) {
               float off = -4.5F + 4.5F * anim.getValue();
               Sprite sprite = mc.getStatusEffectSpriteManager().getSprite(effx.getEffectType());
               float iconSize = 10.0F;
               context.drawTexture(
                  sprite.getAtlasId(),
                  this.x + 5.0F,
                  this.y + offset + off + GuiUtility.getMiddleOfBox(iconSize, 18.0F),
                  iconSize,
                  iconSize,
                  sprite.getMinU(),
                  sprite.getMaxU(),
                  sprite.getMinV(),
                  sprite.getMaxV(),
                  ColorRGBA.WHITE.withAlpha(255.0F * anim.getValue())
               );
               offset += 18.0F * anim.getValue();
            }
         }

         texture.draw();
         offset = 22.0F;

         for (StatusEffectInstance effxx : this.effects.values()) {
            Animation anim = ((StatusEffectInstanceAddition)effxx).elegant$getAnimPotion();
            AnimatedNumber timeAnimation = ((StatusEffectInstanceAddition)effxx).elegant$getTimeAnimation();
            StatusEffect potion = (StatusEffect)effxx.getEffectType().value();
            if (anim.getValue() != 0.0F) {
               float off = -4.5F + 4.5F * anim.getValue();
               float itemHeight = 18.0F;
               Font timeFont = Fonts.MEDIUM.getFont(6.5F);
               float timeHeight = 13.0F;
               String timeText;
               String minutesAndSeparator = null;
               int seconds = -1;
               float timeWidth;
               if (!effxx.isInfinite() && effxx.getDuration() < 999999999) {
                  int totalSeconds = effxx.getDuration() / 20;
                  int minutes = totalSeconds / 60;
                  seconds = totalSeconds % 60;
                  timeText = String.format("%02d:%02d", minutes, seconds);
                  minutesAndSeparator = String.format("%02d:", minutes);
                  timeWidth = timeFont.width(timeText) + 10.0F;
               } else {
                  timeText = "**:**";
                  timeWidth = timeFont.width(timeText) + 10.0F;
               }

               float timeX = this.x + this.width - 5.0F - timeWidth;
               float timeY = this.y + offset + off + (itemHeight - timeHeight) / 2.0F;
               context.drawRoundedRect(timeX, timeY, timeWidth, timeHeight, BorderRadius.all(4.0F), Colors.getTextColor().withAlpha(10.0F * anim.getValue()));
               if (!effxx.isInfinite() && effxx.getDuration() < 999999999) {
                  String timeStr = timeText;
                  float minutesWidth = timeFont.width(minutesAndSeparator);
                  float totalWidth = timeFont.width(timeStr);
                  float timeStartX = timeX + (timeWidth - totalWidth) / 2.0F + 1.0F;
                  float timeTextY = timeY + (timeHeight - timeFont.height()) / 2.0F;
                  context.drawText(timeFont, minutesAndSeparator, timeStartX, timeTextY, Colors.getTextColor().withAlpha(255.0F * anim.getValue()));
                  timeAnimation.settings(true, Colors.getTextColor().withAlpha(255.0F * anim.getValue()));
                  timeAnimation.update(seconds);
                  timeAnimation.pos(timeStartX + minutesWidth, timeTextY);
                  timeAnimation.render(context);
               } else {
                  float timeTextX = timeX + (timeWidth - timeFont.width(timeText)) / 2.0F + 1.0F;
                  float timeTextY = timeY + (timeHeight - timeFont.height()) / 2.0F;
                  context.drawText(timeFont, timeText, timeTextX, timeTextY, Colors.getTextColor().withAlpha(255.0F * anim.getValue()));
               }

               offset += 18.0F * anim.getValue();
            }
         }

         Batching fontBatching = new FontBatching(VertexFormats.POSITION_TEXTURE_COLOR, font.getFont());
         offset = 22.0F;

         for (StatusEffectInstance effxxx : this.effects.values()) {
            Animation anim = ((StatusEffectInstanceAddition)effxxx).elegant$getAnimPotion();
            StatusEffect potion = (StatusEffect)effxxx.getEffectType().value();
            if (anim.getValue() != 0.0F) {
               float offx = -4.5F + 4.5F * anim.getValue();
               String effectName = potion.getName().getString() + " " + (effxxx.getAmplifier() > 0 ? effxxx.getAmplifier() + 1 : "");
               float iconSize = 10.0F;
               float separatorX = this.x + 5.0F + iconSize + 4.0F;
               context.drawText(
                  font,
                  "|",
                  separatorX,
                  this.y + offset + offx + GuiUtility.getMiddleOfBox(font.height(), 18.0F),
                  Colors.getTextColor().withAlpha(80.0F * anim.getValue())
               );
               float nameX = separatorX + 6.0F;
               context.drawText(
                  font,
                  effectName,
                  nameX,
                  this.y + offset + offx + GuiUtility.getMiddleOfBox(font.height(), 18.0F),
                  Colors.getTextColor().withAlpha(255.0F * anim.getValue())
               );
               offset += 18.0F * anim.getValue();
            }
         }

         fontBatching.draw();
         if (this.height > 23.0F) {
            float lineWidth = this.width * 0.5F;
            float lineHeight = 1.5F;
            float lineX = this.x + (this.width - lineWidth) / 2.0F;
            float lineY = this.y + this.height - lineHeight - 0.0F;
            context.drawRoundedRect(lineX, lineY, lineWidth, lineHeight, BorderRadius.all(1.25F), ColorRGBA.WHITE);
         }

         if (toRemove != null) {
            StatusEffect potion = (StatusEffect)toRemove.getEffectType().value();
            this.effects.remove(potion.getName().getString(), toRemove);
         }
      }
   }

   @Override
   public boolean show() {
      return Interface.showEffects();
   }
}
