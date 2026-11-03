package com.isusdlc.systems.modules.modules.visuals.cosmetic;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.SwordItem;
import net.minecraft.util.Arm;
import net.minecraft.util.Hand;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;

public final class CosmeticHeldItemRenderer {
    private static final MinecraftClient MC = MinecraftClient.getInstance();

    private CosmeticHeldItemRenderer() {
    }

    public static boolean tryRenderHeldAccessory(AbstractClientPlayerEntity player, float tickDelta, float pitch,
                                                   Hand hand, float swingProgress, ItemStack item,
                                                   float equipProgress, MatrixStack matrices,
                                                   VertexConsumerProvider consumers, int light) {
        if (!CosmeticModule.getInstance().isEnabled()) return false;

        CosmeticModel accessory = CosmeticManager.getInstance().getActiveAccessory();
        if (accessory == null) return false;

        if (accessory.isBowReplacementAccessory()) {
            if (!item.isOf(Items.BOW)) return false;
            return renderBow(accessory, player, tickDelta, hand, swingProgress, item, equipProgress, matrices, consumers, light);
        }

        if (accessory.isSwordReplacementAccessory()) {
            if (!(item.getItem() instanceof SwordItem)) return false;
            return renderSword(accessory, player, tickDelta, hand, swingProgress, item, equipProgress, matrices, consumers, light);
        }

        return false;
    }

    private static boolean renderBow(CosmeticModel accessory, AbstractClientPlayerEntity player, float tickDelta,
                                     Hand hand, float swingProgress, ItemStack item, float equipProgress,
                                     MatrixStack matrices, VertexConsumerProvider consumers, int light) {
        boolean rightHand = (hand == Hand.MAIN_HAND) == (player.getMainArm() == Arm.RIGHT);
        int side = rightHand ? 1 : -1;

        matrices.push();
        applyEquipOffset(matrices, rightHand ? Arm.RIGHT : Arm.LEFT, equipProgress);

        boolean drawing = player.isUsingItem() && player.getActiveHand() == hand;
        if (drawing) {
            applyBowDrawTransform(matrices, side, item, tickDelta);
        } else {
            applyBowIdleTransform(matrices, side, swingProgress);
        }

        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-90f));
        matrices.scale(0.7f, 0.7f, 0.7f);

        float pull = 0f;
        if (drawing && MC.player != null) {
            float max = item.getMaxUseTime(MC.player);
            float left = MC.player.getItemUseTimeLeft() - tickDelta + 1f;
            // натяжение считается от ванильного времени зарядки лука (20 тиков), а не от max use time (72000)
            pull = MathHelper.clamp((max - left) / 20f, 0f, 1f);
        }

        CosmeticManager mgr = CosmeticManager.getInstance();
        mgr.getAnimator().applyBow(accessory, tickDelta, pull, swingProgress,
                player.limbAnimator.getPos(tickDelta), player.limbAnimator.getSpeed(tickDelta));
        mgr.getRenderer().render(matrices, consumers, accessory, light, OverlayTexture.DEFAULT_UV);

        matrices.pop();
        return true;
    }

    private static boolean renderSword(CosmeticModel accessory, AbstractClientPlayerEntity player, float tickDelta,
                                       Hand hand, float swingProgress, ItemStack item, float equipProgress,
                                       MatrixStack matrices, VertexConsumerProvider consumers, int light) {
        boolean rightHand = (hand == Hand.MAIN_HAND) == (player.getMainArm() == Arm.RIGHT);
        int side = rightHand ? 1 : -1;

        matrices.push();
        applyEquipOffset(matrices, rightHand ? Arm.RIGHT : Arm.LEFT, equipProgress);

        float n = -0.4f * MathHelper.sin(MathHelper.sqrt(swingProgress) * (float) Math.PI);
        float m = 0.2f * MathHelper.sin(MathHelper.sqrt(swingProgress) * (float) (Math.PI * 2));
        float f = -0.2f * MathHelper.sin(swingProgress * (float) Math.PI);
        matrices.translate(side * n, m, f);
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(side * (45f + MathHelper.sin(swingProgress * swingProgress * (float) Math.PI) * -20f)));
        float g = MathHelper.sin(MathHelper.sqrt(swingProgress) * (float) Math.PI);
        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(side * g * -20f));
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(g * -80f));
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(side * -45f));

        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(side * 90f));
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(10f));
        matrices.scale(0.5f, 0.5f, 0.5f);

        CosmeticManager mgr = CosmeticManager.getInstance();
        mgr.getAnimator().apply(accessory, tickDelta, swingProgress,
                player.limbAnimator.getPos(tickDelta), player.limbAnimator.getSpeed(tickDelta));
        mgr.getRenderer().render(matrices, consumers, accessory, light, OverlayTexture.DEFAULT_UV);

        matrices.pop();
        return true;
    }

    private static void applyEquipOffset(MatrixStack matrices, Arm arm, float equipProgress) {
        int side = arm == Arm.RIGHT ? 1 : -1;
        matrices.translate(side * 0.56f, -0.52f + equipProgress * -0.6f, -0.72f);
    }

    private static void applyBowIdleTransform(MatrixStack matrices, int side, float swingProgress) {
        float n = -0.4f * MathHelper.sin(MathHelper.sqrt(swingProgress) * (float) Math.PI);
        float m = 0.2f * MathHelper.sin(MathHelper.sqrt(swingProgress) * (float) (Math.PI * 2));
        float f = -0.2f * MathHelper.sin(swingProgress * (float) Math.PI);
        matrices.translate(side * n, m, f);
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(side * (45f + MathHelper.sin(swingProgress * swingProgress * (float) Math.PI) * -20f)));
        float g = MathHelper.sin(MathHelper.sqrt(swingProgress) * (float) Math.PI);
        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(side * g * -20f));
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(g * -80f));
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(side * -45f));
    }

    private static void applyBowDrawTransform(MatrixStack matrices, int side, ItemStack item, float tickDelta) {
        matrices.translate(side * -0.2785682f, 0.18344387f, 0.15731531f);
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-13.935f));
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(side * 35.3f));
        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(side * -9.785f));

        if (MC.player == null) return;
        float mx = item.getMaxUseTime(MC.player) - (MC.player.getItemUseTimeLeft() - tickDelta + 1f);
        float g = mx / 20f;
        float pull = MathHelper.clamp((g * g + g * 2f) / 3f, 0f, 1f);
        if (pull > 0.1f) {
            float gx = MathHelper.sin((mx - 0.1f) * 1.3f);
            float h = pull - 0.1f;
            float j = gx * h;
            matrices.translate(j * 0f, j * 0.004f, j * 0f);
            matrices.translate(0f, 0f, pull * 0.04f);
            matrices.scale(1f, 1f, 1f + pull * 0.2f);
            matrices.multiply(RotationAxis.NEGATIVE_Y.rotationDegrees(side * 45f));
        }
    }
}
