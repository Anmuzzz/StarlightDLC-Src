package com.isusdlc.mixin.minecraft.client.input;

import com.mojang.brigadier.suggestion.Suggestions;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import javax.annotation.Nullable;
import com.isusdlc.elegant;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.gui.screen.ChatInputSuggestor;
import net.minecraft.client.gui.screen.ChatInputSuggestor.SuggestionWindow;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({ChatInputSuggestor.class})
public abstract class ChatInputSuggestorMixin {
   @Shadow
   @Final
   TextFieldWidget textField;
   @Shadow
   private CompletableFuture<Suggestions> pendingSuggestions;
   @Shadow
   @Nullable
   private SuggestionWindow window;

   @Shadow
   public abstract void show(boolean var1);

   @Inject(
      method = {"refresh()V"},
      at = {@At(
         value = "INVOKE",
         target = "Lcom/mojang/brigadier/StringReader;canRead()Z",
         remap = false
      )},
      cancellable = true
   )
   private void injectAutoCompletion(CallbackInfo ci) {
      String text = this.textField.getText();
      String prefix = elegant.getInstance().getCommandManager().getPrefix();
      if (text.startsWith(prefix)) {
         this.pendingSuggestions = elegant.getInstance().getCommandManager().autoComplete(text, this.textField.getCursor());
         this.pendingSuggestions.thenRun(() -> {
            try {
               if (this.pendingSuggestions.isDone() && !this.pendingSuggestions.get().isEmpty() && this.window == null) {
                  this.show(false);
               }
            } catch (InterruptedException | ExecutionException var3x) {
            }
         });
         // отменяем остаток ванильного refresh(), чтобы не показывалось её окно подсказок
         ci.cancel();
      }
   }
}
