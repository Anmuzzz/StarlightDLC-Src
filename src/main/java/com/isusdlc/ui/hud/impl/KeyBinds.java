package com.isusdlc.ui.hud.impl;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import com.isusdlc.elegant;
import com.isusdlc.framework.base.UIContext;
import com.isusdlc.framework.msdf.Font;
import com.isusdlc.framework.msdf.Fonts;
import com.isusdlc.framework.objects.BorderRadius;
import com.isusdlc.systems.modules.Module;
import com.isusdlc.systems.modules.api.ModuleCategory;
import com.isusdlc.systems.modules.modules.visuals.Interface;
import com.isusdlc.systems.setting.settings.BooleanSetting;
import com.isusdlc.ui.hud.HudEditorScreen;
import com.isusdlc.ui.hud.HudList;
import com.isusdlc.utility.animation.base.Animation;
import com.isusdlc.utility.animation.base.Easing;
import com.isusdlc.utility.colors.ColorRGBA;
import com.isusdlc.utility.colors.Colors;
import com.isusdlc.utility.game.TextUtility;
import com.isusdlc.utility.gui.GuiUtility;
import com.isusdlc.utility.render.obj.CustomSprite;
import net.minecraft.client.gui.screen.ChatScreen;

public class KeyBinds extends HudList {
   int lastSize = -1;
   private final BooleanSetting alwaysDisplay = new BooleanSetting(this, "hud.always_display");

   public KeyBinds() {
      super("hud.keybinds", "icons/hud/keybinds.png");
   }

   @Override
   public void update(UIContext context) {
      this.width = 92.0F;
      this.height = 18.0F;

      for (Module module : elegant.getInstance().getModuleManager().getModules()) {
         boolean forward = module.isEnabled() && module.getKey() != -1;
         module.getKeybindsAnimation().update(forward);
         module.getKeybindsAnimation().setEasing(Easing.BAKEK);
         if (module.getKeybindsAnimation().getValue() > 0.0F) {
            float textWidth = Fonts.REGULAR.getFont(7.0F).width(module.getName() + " " + TextUtility.getKeyName(module.getKey()));
            this.width = Math.max(textWidth + 45.0F, this.width);
         }

         this.height = this.height + 18.0F * module.getKeybindsAnimation().getValue();
      }

      if (this.height > 18.0F) {
         this.height += 5.0F;
      }

      super.update(context);
   }

   @Override
   protected void renderComponent(UIContext context) {
      Font font = Fonts.REGULAR.getFont(7.0F);
      List<Module> modules = new ArrayList<>(elegant.getInstance().getModuleManager().getModules());
      if (this.lastSize != modules.size()) {
         modules.sort(Comparator.comparingDouble(m -> font.width(m.getName())));
         this.lastSize = modules.size();
      }

      context.drawClientRect(this.x, this.y, this.width, Math.max(20.0F, this.height), this.animation.getValue(), this.dragAnim.getValue(), 7.0F);
      float headerHeight = 18.0F;
      float iconSize = 0.0F;
      context.drawTexture(elegant.id(this.icon), this.x + 7.0F, this.y + 6.0F, iconSize, iconSize, Colors.getTextColor());
      context.drawText(
         Fonts.MEDIUM.getFont(8.0F),
         "KeyBinds",
         this.x + 26.0F + iconSize + 4.0F,
         this.y + GuiUtility.getMiddleOfBox(font.height(), headerHeight) + 0.5F,
         Colors.getTextColor()
      );
      if (this.height >= 23.0F) {
         context.drawRect(this.x, this.y + headerHeight, this.width, 0.5F, Colors.getTextColor().withAlpha(30.0F));
      }

      float offset = headerHeight + 4.5F;

      for (Module module : modules) {
         Animation anim = module.getKeybindsAnimation();
         if (anim.getValue() != 0.0F) {
            float itemHeight = 18.0F;
            float alpha = anim.getValue();
            float moduleIconSize = 10.0F;
            CustomSprite categorySprite = this.getCategorySprite(module.getCategory());
            context.drawSprite(
               categorySprite,
               this.x + 5.0F,
               this.y + offset + GuiUtility.getMiddleOfBox(moduleIconSize, itemHeight),
               moduleIconSize,
               moduleIconSize,
               Colors.getTextColor().withAlpha(255.0F * alpha)
            );
            float separatorX = this.x + 5.0F + moduleIconSize + 4.0F;
            context.drawText(
               font, "|", separatorX, this.y + offset + GuiUtility.getMiddleOfBox(font.height(), itemHeight), Colors.getTextColor().withAlpha(80.0F * alpha)
            );
            float nameX = separatorX + 6.0F;
            context.drawText(
               font,
               module.getName(),
               nameX,
               this.y + offset + GuiUtility.getMiddleOfBox(font.height(), itemHeight),
               Colors.getTextColor().withAlpha(255.0F * alpha)
            );
            String keyName = TextUtility.getKeyName(module.getKey());
            Font keyFont = Fonts.MEDIUM.getFont(6.5F);
            float keyWidth = keyFont.width(keyName) + 10.0F;
            float keyHeight = 13.0F;
            float keyX = this.x + this.width - 5.0F - keyWidth;
            float keyY = this.y + offset + (itemHeight - keyHeight) / 2.0F;
            context.drawRoundedRect(keyX, keyY, keyWidth, keyHeight, BorderRadius.all(4.0F), Colors.getTextColor().withAlpha(10.0F * alpha));
            float textX = keyX + (keyWidth - keyFont.width(keyName)) / 2.0F + 1.0F;
            float textY = keyY + (keyHeight - keyFont.height()) / 2.0F;
            context.drawText(keyFont, keyName, textX, textY, Colors.getTextColor().withAlpha(255.0F * alpha));
            offset += itemHeight * anim.getValue();
         }
      }

      if (this.height > 23.0F) {
         float lineWidth = this.width * 0.5F;
         float lineHeight = 1.5F;
         float lineX = this.x + (this.width - lineWidth) / 2.0F;
         float lineY = this.y + this.height - lineHeight - 0.0F;
         context.drawRoundedRect(lineX, lineY, lineWidth, lineHeight, BorderRadius.all(1.25F), ColorRGBA.WHITE);
      }
   }

   private CustomSprite getCategorySprite(ModuleCategory category) {
      return switch (category) {
         case COMBAT -> CustomSprite.BIG_COMBAT;
         case MOVEMENT -> CustomSprite.BIG_MOVEMENT;
         case VISUALS -> CustomSprite.BIG_VISUALS;
         case PLAYER -> CustomSprite.BIG_PLAYER;
         case OTHER -> CustomSprite.BIG_OTHER;
      };
   }

   @Override
   public boolean show() {
      if (!Interface.showKeyBinds()) {
         return false;
      }

      return !elegant.getInstance().getModuleManager().getModules().stream().filter(module -> module.isEnabled() && module.getKey() != -1).toList().isEmpty()
         || mc.currentScreen instanceof ChatScreen
         || mc.currentScreen instanceof HudEditorScreen
         || this.alwaysDisplay.isEnabled();
   }
}
