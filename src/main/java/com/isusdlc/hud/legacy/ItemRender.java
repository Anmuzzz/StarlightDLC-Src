package com.isusdlc.hud.legacy;

import com.mojang.blaze3d.systems.RenderSystem;
import com.isusdlc.utility.interfaces.IMinecraft;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.item.ItemStack;

public final class ItemRender implements IMinecraft {

    public static void drawItemCenteredWithContext(DrawContext context, ItemStack stack, float centerX, float centerY, float scale, int alpha) {
        context.getMatrices().push();
        context.getMatrices().translate(centerX, centerY, 0);
        context.getMatrices().scale(scale, scale, scale);
        int size = 16;
        if (alpha < 255) {
            RenderSystem.setShaderColor(1, 1, 1, alpha / 255.0f);
        }
        context.drawItem(stack, -size / 2, -size / 2);
        RenderSystem.setShaderColor(1, 1, 1, 1);
        context.getMatrices().pop();
    }

    public static void drawItemCenteredWithContext(DrawContext context, ItemStack stack, float centerX, float centerY, float scale, float alpha) {
        drawItemCenteredWithContext(context, stack, centerX, centerY, scale, (int) (alpha * 255));
    }

    public static void drawItemWithContext(DrawContext context, ItemStack stack, float x, float y, float scale, float alphaFactor) {
        context.getMatrices().push();
        context.getMatrices().translate(x, y, 0);
        context.getMatrices().scale(scale, scale, scale);
        if (alphaFactor < 1.0f) {
            RenderSystem.setShaderColor(1, 1, 1, alphaFactor);
        }
        context.drawItem(stack, 0, 0);
        RenderSystem.setShaderColor(1, 1, 1, 1);
        context.getMatrices().pop();
    }

    public static void drawItem(ItemStack stack, float x, float y, float scale, float alphaFactor) {
        DrawContext context = Render2D.getContext();
        if (context == null) return;
        context.getMatrices().push();
        context.getMatrices().translate(x, y, 0);
        context.getMatrices().scale(scale, scale, scale);
        if (alphaFactor < 1.0f) {
            RenderSystem.setShaderColor(1, 1, 1, alphaFactor);
        }
        context.drawItem(stack, 0, 0);
        RenderSystem.setShaderColor(1, 1, 1, 1);
        context.getMatrices().pop();
    }

    public static boolean needsContextRender(ItemStack stack) {
        return !stack.isEmpty();
    }

    private ItemRender() {
        throw new UnsupportedOperationException("Utility class");
    }
}
