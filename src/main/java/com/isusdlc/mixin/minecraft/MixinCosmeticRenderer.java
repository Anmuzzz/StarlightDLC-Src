package com.isusdlc.mixin.minecraft;

import com.isusdlc.systems.modules.modules.visuals.cosmetic.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.render.entity.state.LivingEntityRenderState;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.MathHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntityRenderer.class)
public class MixinCosmeticRenderer<T extends LivingEntity, S extends LivingEntityRenderState, M extends EntityModel<? super S>> {

    private boolean shouldRenderLocal(S state) {
        CosmeticModule module = CosmeticModule.getInstance();
        if (!module.isEnabled()) return false;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return false;
        if (!(state instanceof PlayerEntityRenderState playerState)) return false;
        return playerState.name.equals(mc.player.getGameProfile().getName());
    }

    @Inject(method = "render(Lnet/minecraft/client/render/entity/state/LivingEntityRenderState;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V",
            at = @At("HEAD"), cancellable = true)
    private void isusdlc$replacePlayerModel(S state, MatrixStack matrices, VertexConsumerProvider consumers, int light, CallbackInfo ci) {
        if (!shouldRenderLocal(state)) return;
        CosmeticManager mgr = CosmeticManager.getInstance();
        CosmeticModel model = mgr.getActiveModel();
        if (model == null) return;

        renderCosmetic(state, matrices, consumers, light, model, CosmeticType.MODEL);
        if (mgr.getActivePet() != null) {
            renderCosmetic(state, matrices, consumers, light, mgr.getActivePet(), CosmeticType.PET);
        }
        ci.cancel();
    }

    @Inject(method = "render(Lnet/minecraft/client/render/entity/state/LivingEntityRenderState;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V",
            at = @At("RETURN"))
    private void isusdlc$renderAttachments(S state, MatrixStack matrices, VertexConsumerProvider consumers, int light, CallbackInfo ci) {
        if (!shouldRenderLocal(state)) return;

        CosmeticManager mgr = CosmeticManager.getInstance();
        if (mgr.getActivePet() != null && mgr.getActiveModel() == null) {
            renderCosmetic(state, matrices, consumers, light, mgr.getActivePet(), CosmeticType.PET);
        }
        if (mgr.getActiveAccessory() != null && mgr.getActiveAccessory().isBodyOnlyAccessory()) {
            renderCosmetic(state, matrices, consumers, light, mgr.getActiveAccessory(), CosmeticType.ACCESSORY);
        }
    }

    private void renderCosmetic(S state, MatrixStack matrices, VertexConsumerProvider consumers, int light, CosmeticModel model, CosmeticType type) {
        matrices.push();
        try {
            MinecraftClient mc = MinecraftClient.getInstance();
            float partialTicks = mc.getRenderTickCounter().getTickDelta(false);
            CosmeticManager mgr = CosmeticManager.getInstance();

            CosmeticAttachmentTransforms.apply(matrices, type, model);

            mgr.getAnimator().apply(model, partialTicks,
                    mc.player.getHandSwingProgress(partialTicks),
                    mc.player.limbAnimator.getPos(partialTicks),
                    mc.player.limbAnimator.getSpeed(partialTicks));

            CosmeticRenderSpace space = type == CosmeticType.MODEL ? CosmeticRenderSpace.PLAYER_MODEL : CosmeticRenderSpace.ATTACHMENT;
            mgr.getRenderer().render(matrices, consumers, model, light, OverlayTexture.DEFAULT_UV, false, space);

            if (type == CosmeticType.MODEL && mc.player != null) {
                CosmeticHandItemsRenderer.render(mc.player, model, matrices, consumers, light, partialTicks);
            }
        } catch (Exception ignored) {
        } finally {
            matrices.pop();
        }
    }
}
