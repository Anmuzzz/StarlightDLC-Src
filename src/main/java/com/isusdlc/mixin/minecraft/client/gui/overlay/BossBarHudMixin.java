package com.isusdlc.mixin.minecraft.client.gui.overlay;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import com.isusdlc.elegant;
import com.isusdlc.systems.modules.modules.visuals.Removals;
import com.isusdlc.ui.hud.impl.island.DynamicIsland;
import com.isusdlc.ui.hud.impl.island.impl.PVPStatus;
import com.isusdlc.utility.game.server.ServerUtility;
import com.isusdlc.utility.interfaces.IMinecraft;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.BossBarHud;
import net.minecraft.client.gui.hud.ClientBossBar;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({BossBarHud.class})
public class BossBarHudMixin implements IMinecraft {
   @Shadow
   @Final
   private Map<UUID, ClientBossBar> bossBars;
   @Unique
   private static final Pattern PVP_TIME_PATTERN = Pattern.compile("(\\d+)\\s*[сc][еe][кk](?=$|\\s|\\p{Punct})", 66);
   private static final String FILTERED_TEXT = "둅ꈣꈃ둄ꈣꈅ";
   private final Map<UUID, String> lastProcessedNames = new HashMap<>();

   @Inject(
      method = {"render(Lnet/minecraft/client/gui/DrawContext;)V"},
      at = {@At("HEAD")}
   )
   private void onRenderHead(DrawContext context, CallbackInfo ci) {
      int ctTimer = 0;

      for (ClientBossBar bossBar : this.bossBars.values()) {
         if (bossBar.getName() != null) {
            String name = bossBar.getName().getString().toLowerCase();
            if (name.contains("бой") || name.contains("pvp")) {
               Matcher matcher = PVP_TIME_PATTERN.matcher(bossBar.getName().getString());
               if (matcher.find()) {
                  ctTimer = Integer.parseInt(matcher.group(1));
               }
               break;
            }
         }
      }

      ServerUtility.setHasCT(ctTimer > 0);
      ServerUtility.setCtTime(ctTimer);
      if (elegant.getInstance().getHud() == null || elegant.getInstance().getHud().getIsland() == null) {
         return;
      }
      Removals removals = elegant.getInstance().getModuleManager().getModuleSafe(Removals.class);
      boolean bossBarHidden = removals == null || !removals.isEnabled() || !removals.getBossBar().isSelected();
      if (bossBarHidden
         && elegant.getInstance().getHud().getIsland().isShowing()
         && !this.bossBars.isEmpty()
         && !ServerUtility.isCM()) {
         DynamicIsland island = elegant.getInstance().getHud().getIsland();
         boolean islandShowingPvp = island.isShowing() && island.statuses().stream().anyMatch(status -> status instanceof PVPStatus);
         if (!bossBarHidden || ServerUtility.hasCT && islandShowingPvp) {
            return;
         }

         context.getMatrices().push();
         context.getMatrices().translate(0.0F, elegant.getInstance().getHud().getIsland().getSize().height + 7.0F, 0.0F);
      }
   }

   @Inject(
      method = {"render(Lnet/minecraft/client/gui/DrawContext;)V"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void cancelRenderIfNeeded(DrawContext context, CallbackInfo ci) {
      Removals removals = elegant.getInstance().getModuleManager().getModuleSafe(Removals.class);
      if (removals != null && elegant.getInstance().getHud() != null && elegant.getInstance().getHud().getIsland() != null) {
         DynamicIsland island = elegant.getInstance().getHud().getIsland();
         boolean islandShowingPvp = island.isShowing() && island.statuses().stream().anyMatch(status -> status instanceof PVPStatus);
         if (removals.isEnabled() && removals.getBossBar().isSelected() || ServerUtility.hasCT && islandShowingPvp) {
            ci.cancel();
         }
      }
   }

   @Inject(
      method = {"render(Lnet/minecraft/client/gui/DrawContext;)V"},
      at = {@At("RETURN")}
   )
   private void onRenderReturn(DrawContext context, CallbackInfo ci) {
      int j = 19 * this.bossBars.size();
      if (elegant.getInstance().getHud() == null || elegant.getInstance().getHud().getIsland() == null) {
         return;
      }
      Removals removals = elegant.getInstance().getModuleManager().getModuleSafe(Removals.class);
      if ((removals == null || !removals.isEnabled() || !removals.getBossBar().isSelected())
         && elegant.getInstance().getHud().getIsland().isShowing()
         && !this.bossBars.isEmpty()
         && !ServerUtility.isCM()) {
         context.getMatrices().pop();
      }
   }
}
