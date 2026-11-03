package com.isusdlc.ui.menu.api;

import com.isusdlc.elegant;
import com.isusdlc.framework.base.UIContext;
import com.isusdlc.systems.event.EventListener;
import com.isusdlc.systems.event.impl.render.HudRenderEvent;
import com.isusdlc.systems.modules.modules.visuals.MenuModule;
import com.isusdlc.ui.menu.MenuScreen;
import com.isusdlc.ui.menu.dropdown.DropDownScreen;
import com.isusdlc.utility.interfaces.IMinecraft;
import net.minecraft.client.MinecraftClient;

public class MenuCloseListener implements IMinecraft {
   private final EventListener<HudRenderEvent> onHudRender = event -> {
      MenuScreen menuScreen = elegant.getInstance().getMenuScreen();
      if (mc.currentScreen == null) {
         MenuModule menuModule = elegant.getInstance().getModuleManager().getModule(MenuModule.class);
         if (!(menuScreen instanceof DropDownScreen)) {
            elegant.getInstance().setMenuScreen(new DropDownScreen());
         }
      }

      if (menuScreen != null) {
         menuScreen.getMenuAnimation().update(menuScreen.isClosing() ? 0.0F : 1.0F);
         if (!(mc.currentScreen instanceof MenuScreen) && elegant.getInstance().getModuleManager().getModule(MenuModule.class).isEnabled()) {
            elegant.getInstance().getModuleManager().getModule(MenuModule.class).setEnabled(false, true);
         }

         if (menuScreen.getMenuAnimation().getValue() > 0.1F && !(mc.currentScreen instanceof MenuScreen) && menuScreen.isClosing()) {
            UIContext context = UIContext.of(event.getContext(), -1, -1, MinecraftClient.getInstance().getRenderTickCounter().getTickDelta(false));
            menuScreen.render(context);
         }
      }
   };

   public MenuCloseListener() {
      elegant.getInstance().getEventManager().subscribe(this);
   }
}
