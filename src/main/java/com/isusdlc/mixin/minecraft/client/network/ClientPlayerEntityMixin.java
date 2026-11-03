package com.isusdlc.mixin.minecraft.client.network;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.isusdlc.elegant;
import com.isusdlc.systems.event.impl.game.CloseScreenEvent;
import com.isusdlc.systems.event.impl.player.ClientPlayerTickEndEvent;
import com.isusdlc.systems.event.impl.player.ClientPlayerTickEvent;
import com.isusdlc.systems.event.impl.player.SlowDownEvent;
import com.isusdlc.systems.modules.modules.player.Freelook;
import com.isusdlc.systems.modules.modules.player.InvUtils;
import com.isusdlc.utility.interfaces.IMinecraft;
import com.isusdlc.utility.mixins.ClientPlayerEntityAddition;
import com.isusdlc.utility.rotations.RotationHandler;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.network.ClientPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({ClientPlayerEntity.class})
public class ClientPlayerEntityMixin implements ClientPlayerEntityAddition, IMinecraft {
   @Unique
   private int groundTicks = 0;

   @Redirect(
      method = {"tickMovement()V"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/network/ClientPlayerEntity;isUsingItem()Z"
      ),
      require = 0
   )
   private boolean onIsUsingItemRedirect(ClientPlayerEntity player) {
      SlowDownEvent slowDownEvent = new SlowDownEvent();
      elegant.getInstance().getEventManager().triggerEvent(slowDownEvent);
      return player.isUsingItem() && player.getVehicle() == null && !slowDownEvent.isCancelled();
   }

   @WrapWithCondition(
      method = {"closeScreen()V"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/MinecraftClient;setScreen(Lnet/minecraft/client/gui/screen/Screen;)V"
      )}
   )
   private boolean preventCloseScreen(MinecraftClient instance, Screen screen) {
      elegant.getInstance().getEventManager().triggerEvent(new CloseScreenEvent(screen));
      return true;
   }

   @Inject(
      method = {"pushOutOfBlocks(DD)V"},
      at = {@At("HEAD")},
      cancellable = true
   )
   public void removePushOutFromBlocks(double x, double z, CallbackInfo ci) {
   }

   @Inject(
      method = {"tick()V"},
      at = {@At("HEAD")}
   )
   public void triggerTickEvent(CallbackInfo ci) {
      elegant.getInstance().getEventManager().triggerEvent(new ClientPlayerTickEvent());
      Freelook freelook = elegant.getInstance().getModuleManager().getModule(Freelook.class);
      if (freelook != null && freelook.isEnabled() && mc.player != null) {
      }
   }

   @Inject(
      method = {"tick()V"},
      at = {@At("RETURN")}
   )
   public void triggerTickEndEvent(CallbackInfo ci) {
      elegant.getInstance().getEventManager().triggerEvent(new ClientPlayerTickEndEvent());
   }

   @Inject(
      method = {"tickMovement()V"},
      at = {@At("HEAD")}
   )
   public void updateOnGroundTicks(CallbackInfo ci) {
      if (mc.player != null && mc.player.isOnGround()) {
         this.groundTicks++;
      } else {
         this.groundTicks = 0;
      }
   }

   @Redirect(
      method = {"sendMovementPackets()V"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/network/ClientPlayerEntity;getYaw()F"
      )
   )
   public float replaceMovePacketYaw(ClientPlayerEntity instance) {
      RotationHandler rotationHandler = elegant.getInstance().getRotationHandler();
      float yaw = rotationHandler.isIdling() ? instance.getYaw() : rotationHandler.getCurrentRotation().getYaw();
      rotationHandler.getServerRotation().setYaw(yaw);
      return yaw;
   }

   @Redirect(
      method = {"sendMovementPackets()V"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/network/ClientPlayerEntity;getPitch()F"
      )
   )
   public float replaceMovePacketPitch(ClientPlayerEntity instance) {
      RotationHandler rotationHandler = elegant.getInstance().getRotationHandler();
      float pitch = rotationHandler.isIdling() ? instance.getPitch() : rotationHandler.getCurrentRotation().getPitch();
      rotationHandler.getServerRotation().setPitch(pitch);
      return pitch;
   }

   @Inject(
      method = {"dropSelectedItem(Z)Z"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void onDropSelectedItem(boolean entireStack, CallbackInfoReturnable<Boolean> cir) {
      InvUtils slotLock = elegant.getInstance().getModuleManager().getModuleSafe(InvUtils.class);
      if (slotLock != null && slotLock.isEnabled() && slotLock.getSlotLock().isSelected() && slotLock.isLocked(mc.player.getInventory().selectedSlot)) {
         cir.setReturnValue(false);
         cir.cancel();
      }
   }

   @Override
   public int elegant$getOnGroundTicks() {
      return this.groundTicks;
   }
}
