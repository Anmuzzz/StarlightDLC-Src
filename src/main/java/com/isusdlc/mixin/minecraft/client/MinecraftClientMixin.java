package com.isusdlc.mixin.minecraft.client;

import com.isusdlc.elegant;
import com.isusdlc.protection.client.MinecraftClientMixinProtection;
import com.isusdlc.systems.event.impl.game.GameTickEvent;
import com.isusdlc.systems.modules.modules.combat.NoInteract;
import com.isusdlc.utility.render.penis.PenisAtlas;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.crash.ReportType;
import net.minecraft.client.RunArgs;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({MinecraftClient.class})
public class MinecraftClientMixin {
   @Shadow
   private int itemUseCooldown;

   @Shadow
   @Nullable
   private ClientPlayerEntity player;

   @Shadow
   @Nullable
   private ClientPlayerInteractionManager interactionManager;

   @Shadow
   @Final
   private GameRenderer gameRenderer;

   @Inject(
      method = {"tick()V"},
      at = {@At("HEAD")}
   )
   public void tick(CallbackInfo ci) {
      if (elegant.getInstance() == null || elegant.getInstance().getEventManager() == null) {
         return;
      }
      elegant.getInstance().getEventManager().triggerEvent(new GameTickEvent());
   }

   @Inject(
      method = {"<init>(Lnet/minecraft/client/RunArgs;)V"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/MinecraftClient;onResolutionChanged()V"
      )}
   )
   public void initializeClient(RunArgs args, CallbackInfo ci) {
      MinecraftClientMixinProtection.init();
   }

   @Inject(
      method = {"<init>(Lnet/minecraft/client/RunArgs;)V"},
      at = {@At("RETURN")}
   )
   public void endInitialize(RunArgs args, CallbackInfo ci) {
      try {
         MinecraftClient self = (MinecraftClient) (Object) this;
         self.options.getGuiScale().setValue(2);
      } catch (Exception ignored) {}

      try {
         PenisAtlas atlas = PenisAtlas.getOrCreateAtlasFor(16, 16);
         atlas.registerAnimationFromPenisFile(elegant.id("penises/combat.penis"));
         atlas.registerAnimationFromPenisFile(elegant.id("penises/movement.penis"));
         atlas.registerAnimationFromPenisFile(elegant.id("penises/visuals.penis"));
         atlas.registerAnimationFromPenisFile(elegant.id("penises/player.penis"));
         atlas.registerAnimationFromPenisFile(elegant.id("penises/other.penis"));
         atlas.registerAnimationFromPenisFile(elegant.id("penises/search.penis"));
         atlas.buildAtlas();
         PenisAtlas atlas12 = PenisAtlas.getOrCreateAtlasFor(12, 12);
         atlas12.registerAnimationFromPenisFile(elegant.id("penises/check_enable.penis"));
         atlas12.registerAnimationFromPenisFile(elegant.id("penises/check_disable.penis"));
         atlas12.buildAtlas();
      } catch (Exception var5) {
         System.err.println("Ошибка при загрузке анимаций: " + var5.getMessage());
         var5.printStackTrace();
      }
   }

   @Inject(
      method = {"crash(Lnet/minecraft/util/crash/CrashReport;)V"},
      at = {@At("HEAD")}
   )
   public void onCrash(net.minecraft.util.crash.CrashReport report, CallbackInfo ci) {
      com.isusdlc.systems.CrashReporter.report(report.asString(ReportType.MINECRAFT_CRASH_REPORT));
   }

   @Inject(
      method = {"stop()V"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/MinecraftClient;close()V",
         shift = Shift.AFTER
      )}
   )
   public void shutdownClient(CallbackInfo ci) {
      MinecraftClientMixinProtection.shutdown();
   }

   @Inject(
      method = {"getWindowTitle()Ljava/lang/String;"},
      at = {@At("HEAD")},
      cancellable = true
   )
   public void changeWindowTitle(CallbackInfoReturnable<String> cir) {
      MinecraftClientMixinProtection.updateTitle(cir);
   }

   @Inject(
      method = "doItemUse",
      at = @At(value = "INVOKE", target = "Lnet/minecraft/util/Hand;values()[Lnet/minecraft/util/Hand;"),
      cancellable = true
   )
   public void onDoItemUse(CallbackInfo ci) {
      NoInteract noInteract = elegant.getInstance().getModuleManager().getModuleSafe(NoInteract.class);
      if (noInteract != null && noInteract.isEnabled() && player != null && interactionManager != null) {
         for (Hand hand : Hand.values()) {
            if (!player.getStackInHand(hand).isEmpty()) {
               ActionResult result = interactionManager.interactItem(player, hand);
               if (result.isAccepted()) {
                  if (result instanceof ActionResult.Success success && success.swingSource().equals(ActionResult.SwingSource.CLIENT)) {
                     gameRenderer.firstPersonRenderer.resetEquipProgress(hand);
                     player.swingHand(hand);
                  }
                  ci.cancel();
               }
            }
         }
      }
   }
}
