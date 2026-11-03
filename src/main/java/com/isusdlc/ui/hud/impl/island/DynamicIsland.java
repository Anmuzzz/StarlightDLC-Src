package com.isusdlc.ui.hud.impl.island;

import java.util.List;
import lombok.Generated;
import com.isusdlc.elegant;
import com.isusdlc.framework.base.UIContext;
import com.isusdlc.framework.msdf.Fonts;
import com.isusdlc.framework.objects.BorderRadius;
import com.isusdlc.framework.objects.MouseButton;
import com.isusdlc.systems.modules.modules.visuals.Interface;
import com.isusdlc.systems.setting.settings.SelectSetting;
import com.isusdlc.systems.theme.Theme;
import com.isusdlc.ui.hud.HudEditorScreen;
import com.isusdlc.ui.hud.HudElement;
import com.isusdlc.ui.hud.impl.island.impl.DefaultStatus;
import com.isusdlc.ui.hud.impl.island.impl.ElytraStatus;
import com.isusdlc.ui.hud.impl.island.impl.EventStatus;
import com.isusdlc.ui.hud.impl.island.impl.MineStatus;
import com.isusdlc.ui.hud.impl.island.impl.NotificationStatus;
import com.isusdlc.ui.hud.impl.island.impl.PVPStatus;
import com.isusdlc.ui.menu.dropdown.DropDownScreen;
import com.isusdlc.utility.animation.base.Animation;
import com.isusdlc.utility.animation.base.Easing;
import com.isusdlc.utility.animation.types.ColorAnimation;
import com.isusdlc.utility.colors.ColorRGBA;
import com.isusdlc.utility.colors.Colors;
import com.isusdlc.utility.game.TextUtility;
import com.isusdlc.utility.game.cursor.CursorType;
import com.isusdlc.utility.game.cursor.CursorUtility;
import com.isusdlc.utility.gui.GuiUtility;
import com.isusdlc.utility.interfaces.IMinecraft;
import com.isusdlc.utility.interfaces.IScaledResolution;
import com.isusdlc.utility.render.ScissorUtility;
import com.isusdlc.utility.time.Timer;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;

public class DynamicIsland extends HudElement implements IMinecraft, IScaledResolution {
   private final SelectSetting statuses = new SelectSetting(this, "hud.dynamic_island.statuses").draggable();
   private final IslandSize size = new IslandSize(48.0F, 15.0F);
   private boolean extended;
   private final Animation extendingAnim = new Animation(200L, 0.0F, Easing.LINEAR);
   private final Animation widthAnim = new Animation(500L, 0.0F, Easing.BAKEK_SIZE);
   private final Animation heightAnim = new Animation(500L, 0.0F, Easing.BAKEK_SIZE);
   private final Animation showPing = new Animation(500L, 0.0F, Easing.BAKEK);
   private final ColorAnimation backgroundColor = new ColorAnimation(300L, new ColorRGBA(0.0F, 0.0F, 0.0F), Easing.FIGMA_EASE_IN_OUT);
   private final ColorAnimation adaptColor = new ColorAnimation(300L, new ColorRGBA(255.0F, 255.0F, 255.0F), Easing.LINEAR);
   private final Timer timer = new Timer();
   private boolean dark;
   private boolean useDark;
   private IslandStatus last;


   public DynamicIsland() {
      super("hud.dynamic_island", "icons/hud/island.png");
      new NotificationStatus(this.statuses);
      new PVPStatus(this.statuses);
      new EventStatus(this.statuses);
      new MineStatus(this.statuses);
      new ElytraStatus(this.statuses);
      new DefaultStatus(this.statuses).alwaysEnabled();
      elegant.getInstance().getFileManager().readFile("client");
      elegant.getInstance().getEventManager().subscribe(this);
   }

   @Override
   protected void renderComponent(UIContext context) {
      this.width = this.size.width;
      this.height = this.size.height;
      this.x = sr.getScaledWidth() / 2.0F - this.width / 2.0F;
      this.y = 7.0F;
      BorderRadius radius = BorderRadius.all(5.0F + 8.0F * this.extendingAnim.getValue());
      String time = TextUtility.getCurrentTime();
      if (this.timer.finished(500L)) {
         float pixelX = mc.getWindow().getWidth() / 2.0F;
         float pixelY = mc.getWindow().getHeight() - (this.y + 5.0F) * (float)mc.getWindow().getScaleFactor();
         ColorRGBA pixel = ColorRGBA.fromPixel(pixelX, pixelY);
         boolean check = (pixel.getRed() + pixel.getGreen() + pixel.getBlue()) / 3.0F > 70.0F;
         this.useDark = check;
         this.timer.reset();
      }

      this.adaptColor.update(this.useDark ? new ColorRGBA(0.0F, 0.0F, 0.0F) : new ColorRGBA(255.0F, 255.0F, 255.0F));
      boolean themeDark = elegant.getInstance().getThemeManager().getCurrentTheme() == Theme.DARK;
      ColorRGBA bgColor = Colors.getBackgroundColor().withAlpha(255.0F * (themeDark ? 0.8F - 0.6F * Interface.glass() : 0.7F));
      this.dark = this.useDark;
      ColorRGBA elmtColor = this.adaptColor.getColor().withAlpha(255.0F * (1.0F - this.extendingAnim.getValue()));
      if (mc.player != null) {
         context.drawText(Fonts.MEDIUM.getFont(7.0F), time, this.x - Fonts.MEDIUM.getFont(9.0F).width(time) - 4.0F, this.y + 5.0F, elmtColor);
         if (!mc.isInSingleplayer() && mc.player.networkHandler.getPlayerListEntry(mc.player.getUuid()) != null) {
            this.showPing.update(GuiUtility.isHovered(this.x + this.width + 4.0F + 4.0F * this.showPing.getValue(), this.y + 5.0F, 12.8F, 7.0, context));
            int ping = mc.player.networkHandler.getPlayerListEntry(mc.player.getUuid()).getLatency();
            int[] pings = new int[]{450, 300, 150, 75};
            context.drawText(
               Fonts.MEDIUM.getFont(7.0F),
               ping + " ms",
               this.x + this.width + 4.0F + 4.0F * this.showPing.getValue(),
               this.y + 5.0F,
               elmtColor.mulAlpha(this.showPing.getValue())
            );

            for (int i = 0; i < 4; i++) {
               context.drawRoundedRect(
                  this.x + this.width + 9.0F + i * 2.7F + 4.0F * this.showPing.getValue(),
                  this.y + 8.0F - i,
                  2.0F,
                  3 + i,
                  BorderRadius.all(0.1F),
                  elmtColor.withAlpha(elmtColor.getAlpha() * 0.2F * (1.0F - this.showPing.getValue()))
               );
            }

            for (int i = 0; i < 4; i++) {
               if (ping < pings[i]) {
                  context.drawRoundedRect(
                     this.x + this.width + 9.0F + i * 2.7F + 4.0F * this.showPing.getValue(),
                     this.y + 8.0F - i,
                     2.0F,
                     3 + i,
                     BorderRadius.all(0.1F),
                     elmtColor.mulAlpha(1.0F - this.showPing.getValue())
                  );
               }
            }
         } else {
            context.drawTexture(elegant.id("icons/airplane.png"), this.x + this.width + 8.0F, this.y + 3.5F, 8.0F, 8.0F, elmtColor);
         }
      }

      IslandStatus status = this.active();
      this.backgroundColor.update(status.getColor());
      if (Interface.showMinimalizm()) {
         context.drawBlurredRect(
            this.x,
            this.y,
            this.width,
            this.height,
            11.25F,
            2.0F + 2.0F * this.extendingAnim.getValue(),
            radius,
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
            radius,
            Colors.getLiquidGlassColor().withAlpha(255.0F * Interface.glass())
         );
      }

      context.drawSquircle(this.x, this.y, this.width, this.height, 2.0F + 5.0F * this.extendingAnim.getValue(), radius, bgColor);

      for (SelectSetting.Value islandStatus : this.statuses.getValues()) {
         ((IslandStatus)islandStatus).getAnimation().update(status == islandStatus ? 1.0F : 0.0F);
      }

      ScissorUtility.push(context.getMatrices(), this.x, this.y, this.width, this.height);
      if (status.getAnimation().getValue() == 1.0F) {
         status.draw(context);
      } else {
         for (SelectSetting.Value islandStatus : this.statuses.getValues()) {
            if (((IslandStatus)islandStatus).getAnimation().getValue() > 0.0F) {
               ((IslandStatus)islandStatus).drawWithAlpha(context);
            }
         }
      }

      ScissorUtility.pop();
      this.widthAnim.update(status.size.width);
      this.heightAnim.update(status.size.height);
      this.size.width = this.widthAnim.getValue();
      this.size.height = this.heightAnim.getValue();
      this.extendingAnim.update(this.extended ? 1.0F : 0.0F);
      if (!(status instanceof ExtandableStatus)) {
         this.extended = false;
      }

      if (!(mc.currentScreen instanceof ChatScreen)
            && !(mc.currentScreen instanceof DropDownScreen)
            && !(mc.currentScreen instanceof HudEditorScreen)
            && mc.player != null
         || this.select) {
         this.extended = false;
      }

      if (!this.extended
         && status instanceof ExtandableStatus
         && GuiUtility.isHovered(
            (double)this.x,
            (double)this.y,
            (double)this.width,
            (double)this.height,
            (double)GuiUtility.getMouse().getX(),
            (double)GuiUtility.getMouse().getY()
         )) {
         CursorUtility.set(CursorType.HAND);
      }
   }

   @Override
   public void onMouseClicked(double mouseX, double mouseY, MouseButton button) {
      super.onMouseClicked(mouseX, mouseY, button);
      this.handleClick((float)mouseX, (float)mouseY, button.getButtonIndex());
   }

   @Override
   public void onMouseReleased(double mouseX, double mouseY, MouseButton button) {
      if (this.dragging && button == MouseButton.LEFT) {
         float dragDistance = (float)Math.sqrt(Math.pow(this.x - this.startDragX, 2.0) + Math.pow(this.y - this.startDragY, 2.0));
         if (dragDistance > 5.0F && mc.player != null && mc.currentScreen == null) {
            mc.setScreen(new InventoryScreen(mc.player));
         }
      }

      super.onMouseReleased(mouseX, mouseY, button);
   }

   public boolean handleClick(float mouseX, float mouseY, int button) {
      float x = sr.getScaledWidth() / 2.0F - this.size.width / 2.0F;
      float y = 7.0F;
      if (this.extended) {
         if (!GuiUtility.isHovered((double)x, (double)y, (double)this.size.width, (double)this.size.height, (double)mouseX, (double)mouseY)) {
            this.extended = false;
         } else {
            this.active().click(mouseX, mouseY, button);
         }

         return true;
      } else if (GuiUtility.isHovered((double)x, (double)y, (double)this.size.width, (double)this.size.height, (double)mouseX, (double)mouseY)
         && this.active() instanceof ExtandableStatus) {
         this.extended = true;
         return true;
      } else {
         return false;
      }
   }

   public IslandStatus active() {
      return this.statuses().getLast();
   }

   public List<IslandStatus> statuses() {
      return this.statuses
         .getValues()
         .stream()
         .filter(islandStatus -> ((IslandStatus)islandStatus).canShow() && islandStatus.isSelected())
         .map(islandStatus -> (IslandStatus)islandStatus)
         .toList()
         .reversed();
   }

   @Generated
   public SelectSetting getStatuses() {
      return this.statuses;
   }

   @Generated
   public IslandSize getSize() {
      return this.size;
   }

   @Generated
   public boolean isExtended() {
      return this.extended;
   }

   @Generated
   public Animation getExtendingAnim() {
      return this.extendingAnim;
   }

   @Generated
   @Override
   public Animation getWidthAnim() {
      return this.widthAnim;
   }

   @Generated
   @Override
   public Animation getHeightAnim() {
      return this.heightAnim;
   }

   @Generated
   public Animation getShowPing() {
      return this.showPing;
   }

   @Generated
   public ColorAnimation getBackgroundColor() {
      return this.backgroundColor;
   }

   @Generated
   public ColorAnimation getAdaptColor() {
      return this.adaptColor;
   }

   @Generated
   public Timer getTimer() {
      return this.timer;
   }

   @Generated
   public boolean isDark() {
      return this.dark;
   }

   @Generated
   public boolean isUseDark() {
      return this.useDark;
   }

   @Generated
   public IslandStatus getLast() {
      return this.last;
   }

}
