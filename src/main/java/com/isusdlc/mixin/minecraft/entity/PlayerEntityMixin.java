package com.isusdlc.mixin.minecraft.entity;

import com.isusdlc.elegant;
import com.isusdlc.systems.event.impl.game.AttackEvent;
import com.isusdlc.systems.event.impl.game.PostAttackEvent;
import com.isusdlc.systems.modules.modules.combat.Reach;
import com.isusdlc.utility.rotations.RotationHandler;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({PlayerEntity.class})
public class PlayerEntityMixin {
   @Inject(
      method = {"attack(Lnet/minecraft/entity/Entity;)V"},
      at = {@At("HEAD")},
       cancellable = true
    )
    private void attackAHook2(Entity target, CallbackInfo ci) {
      AttackEvent event = new AttackEvent(target);
      elegant.getInstance().getEventManager().triggerEvent(event);
      if (event.isCancelled()) {
         ci.cancel();
      }
   }

   @Inject(
      method = {"attack(Lnet/minecraft/entity/Entity;)V"},
      at = {@At("RETURN")},
       cancellable = true
    )
    private void attackAHook(Entity target, CallbackInfo ci) {
      PostAttackEvent event = new PostAttackEvent(target);
      elegant.getInstance().getEventManager().triggerEvent(event);
   }

   @Inject(
      method = {"isPushedByFluids()Z"},
      at = {@At("HEAD")},
             cancellable = true
    )
    private void removePushFromFluids(CallbackInfoReturnable<Boolean> cir) {
      com.isusdlc.systems.modules.modules.player.NoPush noPush =
         elegant.getInstance().getModuleManager().getModuleSafe(com.isusdlc.systems.modules.modules.player.NoPush.class);
      if (noPush != null && noPush.isEnabled() && noPush.getWater().isEnabled()) {
         cir.setReturnValue(false);
      }
    }

   @Redirect(
      method = {"travel(Lnet/minecraft/util/math/Vec3d;)V"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/entity/player/PlayerEntity;getRotationVector()Lnet/minecraft/util/math/Vec3d;"
       )
    )
    private Vec3d redirectGetRotationVectorInTravel(PlayerEntity instance) {
      RotationHandler rotationHandler = elegant.getInstance().getRotationHandler();
      return rotationHandler.isIdling() ? instance.getRotationVector() : rotationHandler.getCurrentRotation().getRotationVector();
   }

   @Inject(method = "getBlockInteractionRange", at = @At("HEAD"), cancellable = true)
   private void reach$getBlockInteractionRange(CallbackInfoReturnable<Double> cir) {
      Reach reach = elegant.getInstance().getModuleManager().getModuleSafe(Reach.class);
      if (reach != null && reach.isEnabled()) {
         cir.setReturnValue((double) reach.blocksRange.getCurrentValue());
      }
   }

   @Inject(method = "getEntityInteractionRange", at = @At("HEAD"), cancellable = true)
   private void reach$getEntityInteractionRange(CallbackInfoReturnable<Double> cir) {
      Reach reach = elegant.getInstance().getModuleManager().getModuleSafe(Reach.class);
      if (reach != null && reach.isEnabled()) {
         cir.setReturnValue((double) reach.entityRange.getCurrentValue());
      }
   }
}
