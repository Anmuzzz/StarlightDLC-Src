package com.isusdlc.mixin.minecraft.client;

import com.isusdlc.elegant;
import com.isusdlc.systems.event.impl.window.MouseEvent;
import com.isusdlc.systems.event.impl.window.MouseScrollEvent;
import com.isusdlc.systems.modules.modules.player.Freelook;
import com.isusdlc.utility.game.cursor.CursorType;
import com.isusdlc.utility.game.cursor.CursorUtility;
import com.isusdlc.utility.interfaces.IMinecraft;
import net.minecraft.client.Mouse;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({Mouse.class})
public class MouseMixin implements IMinecraft {
   @Shadow
   private double cursorDeltaX;
   @Shadow
   private double cursorDeltaY;

   @Inject(
      method = {"tick()V"},
      at = {@At("RETURN")}
   )
   private void tick(CallbackInfo ci) {
      if (CursorUtility.getCurrentType() != CursorUtility.getPrev()) {
         GLFW.glfwSetCursor(mc.getWindow().getHandle(), CursorUtility.getCurrentType().getCode());
      }

      CursorUtility.setPrev(CursorUtility.getCurrentType());
      CursorUtility.set(CursorType.DEFAULT);
   }

   @Inject(
      method = {"onMouseButton(JIII)V"},
      at = {@At("HEAD")}
   )
   private void onMouseButton(long window, int button, int action, int mods, CallbackInfo ci) {
      if (action == 1 || action == 0) {
         elegant.getInstance().getEventManager().triggerEvent(new MouseEvent(button, action));
      }
   }

   @Inject(
      method = {"onMouseScroll(JDD)V"},
      at = {@At("HEAD")}
   )
   private void onMouseScroll(long window, double horizontal, double vertical, CallbackInfo ci) {
      if (vertical != 0.0) {
         elegant.getInstance().getEventManager().triggerEvent(new MouseScrollEvent(vertical));
      }
   }

   @Inject(
      method = {"updateMouse(D)V"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/network/ClientPlayerEntity;changeLookDirection(DD)V"
      )},
      cancellable = true
   )
   private void onUpdateMouse(double timeDelta, CallbackInfo ci) {
      if (Freelook.isActive && mc.player != null) {
         double d = (Double)mc.options.getMouseSensitivity().getValue() * 0.6 + 0.2;
         double e = d * d * d;
         double f = e * 8.0;
         double deltaX = this.cursorDeltaX * f;
         double deltaY = this.cursorDeltaY * f;
         double var5 = deltaY * 0.15;
         double var7 = deltaX * 0.15;
         Freelook.prevX = Freelook.x;
         Freelook.prevY = Freelook.y;
         Freelook.x = (float)(Freelook.x + var7);
         Freelook.y = (float)MathHelper.clamp(Freelook.y + var5, -90.0, 90.0);
         ci.cancel();
      }
   }
}
