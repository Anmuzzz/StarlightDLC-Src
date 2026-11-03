package com.isusdlc.systems.modules.modules.visuals.cosmetic;

import com.isusdlc.framework.base.CustomDrawContext;
import com.isusdlc.framework.msdf.Font;
import com.isusdlc.framework.msdf.Fonts;
import com.isusdlc.framework.objects.BorderRadius;
import com.isusdlc.utility.animation.base.Animation;
import com.isusdlc.utility.animation.base.Easing;
import com.isusdlc.utility.colors.ColorRGBA;
import com.isusdlc.utility.interfaces.IMinecraft;
import com.isusdlc.utility.interfaces.IWindow;
import com.isusdlc.utility.render.DrawUtility;
import com.isusdlc.utility.render.ScissorUtility;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import org.joml.Quaternionf;
import org.lwjgl.glfw.GLFW;

import java.util.List;

public class ScreenCosmetic extends Screen implements IMinecraft, IWindow {

    private final Animation openAnim = new Animation(280, 0.0f, Easing.EXPO_OUT);
    private boolean open = false;

    private final Animation pvAnim = new Animation(320, 0.0f, Easing.EXPO_OUT);
    private boolean pvVisible = false;
    private CosmeticModel previewModel = null;

    private float previewRotY = 25f;
    private float previewRotYTarget = 25f;
    private boolean dragging = false;
    private double lastMouseX = 0;

    private final Animation scrollAnim = new Animation(350, 0.0f, Easing.EXPO_OUT);
    private float scroll = 0f;

    private CosmeticType selectedTab = CosmeticType.MODEL;

    private static final float PANEL_W_FRAC = 0.82f;
    private static final float PANEL_H_FRAC = 0.82f;
    private static final float CAT_BAR_W = 52f;
    private static final float PV_TARGET_W = 200f;
    private static final float HEADER_H = 36f;
    private static final float CARD_H = 32f;
    private static final float CARD_GAP = 5f;
    private static final float CARDS_PAD = 7f;
    private static final float COL_GAP = 5f;

    public ScreenCosmetic() {
        super(Text.of("Cosmetic"));
    }

    @Override
    protected void init() {
        open = true;
        CosmeticManager.getInstance().init();
        pvVisible = false;
        previewModel = null;
        pvAnim.setValue(0.0f);
    }

    @Override
    public void close() {
        open = false;
        super.close();
    }

    private float scaled(float v) {
        return v;
    }

    private float panelScale() {
        return 0.90f + openAnim.getValue() * 0.10f;
    }

    private float[] panelMouse(double mx, double my) {
        float sw = mw.getScaledWidth();
        float sh = mw.getScaledHeight();
        float sc = panelScale();
        return new float[]{
                (float) ((mx - sw / 2.0) / sc + sw / 2.0),
                (float) ((my - sh / 2.0) / sc + sh / 2.0)
        };
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        float openVal = openAnim.update(open ? 1.0f : 0.0f);
        if (!open && openVal < 0.05f) {
            mc.setScreen(null);
            return;
        }

        float pvVal = pvAnim.update(pvVisible ? 1.0f : 0.0f);
        float scrollVal = scrollAnim.update(scroll);

        previewRotY += (previewRotYTarget - previewRotY) * 0.12f;

        int a = (int) (255 * openVal);

        MatrixStack ms = context.getMatrices();
        float sw = mw.getScaledWidth();
        float sh = mw.getScaledHeight();

        DrawUtility.drawRect(ms, 0, 0, sw, sh, new ColorRGBA(6, 6, 10, 220 * openVal));

        float panelW = sw * PANEL_W_FRAC;
        float panelH = sh * PANEL_H_FRAC;
        float panelX = (sw - panelW) / 2f;
        float panelY = (sh - panelH) / 2f;

        float scale = panelScale();
        ms.push();
        ms.translate(sw / 2f, sh / 2f, 0);
        ms.scale(scale, scale, 1f);
        ms.translate(-sw / 2f, -sh / 2f, 0);

        DrawUtility.drawRoundedRect(ms, panelX, panelY, panelW, panelH,
                BorderRadius.all(10f), new ColorRGBA(14, 14, 20, 195 * openVal));

        float headerH = scaled(HEADER_H);
        float catBarW = scaled(CAT_BAR_W);
        float pvW = pvVal * scaled(PV_TARGET_W);
        float catBarX = panelX + panelW - catBarW;
        float contentX = panelX + pvW;
        float contentW = catBarX - contentX;
        float contentY = panelY + headerH;
        float contentH = panelH - headerH;

        CustomDrawContext ctx = CustomDrawContext.of(context);

        drawHeader(ctx, ms, mouseX, mouseY, panelX, panelY, panelW, headerH, a, openVal);

        if (pvW > scaled(4f)) {
            drawPreviewPanel(ctx, ms, mouseX, mouseY, panelX, contentY, pvW, contentH, a, openVal, pvVal, delta);
        }

        drawCardsArea(ctx, ms, mouseX, mouseY, contentX, contentY, contentW, contentH, a);

        drawCategoryBar(ctx, ms, mouseX, mouseY, catBarX, contentY, catBarW, contentH, a, openVal);

        ms.pop();
        super.render(context, mouseX, mouseY, delta);
    }

    private void drawHeader(CustomDrawContext ctx, MatrixStack ms, int mouseX, int mouseY,
                            float panelX, float panelY, float panelW,
                            float headerH, int a, float anim) {
        DrawUtility.drawRoundedRect(ms, panelX, panelY, panelW, headerH,
                BorderRadius.top(10f, 10f), new ColorRGBA(18, 18, 26, 230 * anim));
        DrawUtility.drawRect(ms, panelX, panelY + headerH - 10f, panelW, 10f,
                new ColorRGBA(18, 18, 26, 230 * anim));

        Font titleFont = Fonts.BOLD.getFont(9f);
        String title = "Cosmetics";
        ctx.drawText(titleFont, title, panelX + 14f, panelY + headerH / 2f - titleFont.height() / 2f,
                new ColorRGBA(255, 200, 0, a));

        Font crumbFont = Fonts.MEDIUM.getFont(5.5f);
        String crumb = selectedTab.getDisplayName();
        float crumbX = panelX + 14f + titleFont.width(title) + 8f;
        float crumbY = panelY + headerH / 2f - crumbFont.height() / 2f + 0.5f;
        DrawUtility.drawRoundedRect(ms, crumbX - 5f, crumbY - 3f,
                crumbFont.width(crumb) + 10f, crumbFont.height() + 6f,
                BorderRadius.all(3f), new ColorRGBA(40, 35, 60, 180 * anim));
        ctx.drawText(crumbFont, crumb, crumbX, crumbY, new ColorRGBA(255, 200, 0, 200 * anim / 255f * 255));

        Font closeFont = Fonts.BOLD.getFont(7f);
        float closeSize = closeFont.height();
        float closeX = panelX + panelW - 16f - closeSize;
        float closeY = panelY + headerH / 2f - closeSize / 2f;
        boolean hoverClose = mouseX >= closeX - 3f && mouseX <= closeX - 3f + closeSize + 6f
                && mouseY >= closeY - 3f && mouseY <= closeY - 3f + closeSize + 6f;
        ColorRGBA closeCol = hoverClose ? new ColorRGBA(255, 60, 60) : new ColorRGBA(120, 120, 140);
        ctx.drawText(closeFont, "x", closeX, closeY, closeCol.withAlpha(a));
    }

    private void drawPreviewPanel(CustomDrawContext ctx, MatrixStack ms, int mouseX, int mouseY,
                                  float x, float y, float w, float h,
                                  int a, float anim, float pvF, float delta) {
        int pvA = (int) (a * pvF);
        if (pvA < 5) return;

        DrawUtility.drawRect(ms, x, y, w, h, new ColorRGBA(12, 12, 18, 220 * pvF));
        DrawUtility.drawRect(ms, x + w - 1f, y, 1f, h, new ColorRGBA(255, 255, 255, 14 * pvF));

        if (previewModel == null) return;

        CosmeticModel model = previewModel;
        CosmeticModel equip = CosmeticManager.getInstance().getActive(selectedTab);
        boolean worn = equip != null && equip.getId().equals(model.getId());
        float cx = x + w / 2f;

        Font closeFont = Fonts.BOLD.getFont(5.5f);
        float closeSize = closeFont.height();
        float cvx = x + w - 10f - closeSize;
        float cvy = y + 10f;
        ctx.drawText(closeFont, "x", cvx, cvy, new ColorRGBA(120, 120, 140, pvA));

        Font nameFont = Fonts.BOLD.getFont(7.5f);
        float nameY = y + 24f;
        ctx.drawCenteredText(nameFont, model.getName(), cx, nameY, new ColorRGBA(220, 220, 230, pvA));

        float lineW = nameFont.width(model.getName()) + 8f;
        float lineY = nameY + nameFont.height() + 3f;
        DrawUtility.drawRect(ms, cx - lineW / 2f, lineY, lineW, 1.5f,
                new ColorRGBA(255, 200, 0, 180 * pvF));

        float btnH = 18f;
        float btnY = y + h - 8f - btnH;
        float descBoxH = !model.getDescription().isEmpty() ? 5.5f + 10f : 0f;
        float descY = btnY - 6f - descBoxH;
        float workTop = lineY + 10f;
        float workBot = descY - 6f;
        float workH = Math.max(40f, workBot - workTop);

        float modelScale = workH * 0.50f / 1.8f;
        float modelY = workTop + workH * 0.70f;

        ScissorUtility.push(ms, x + 4f, workTop, w - 8f, workH);
        ms.push();
        try {
            ms.translate(cx, modelY, 200f);
            ms.scale(modelScale, modelScale, modelScale);
            ms.multiply(new Quaternionf().rotationY((float) Math.toRadians(previewRotY)));
            if (model.getType() != CosmeticType.MODEL) {
                CosmeticAttachmentTransforms.applyPreview(ms, model);
            }

            VertexConsumerProvider.Immediate consumers = mc.getBufferBuilders().getEntityVertexConsumers();
            CosmeticAnimator animator = CosmeticManager.getInstance().getAnimator();

            if (model.hasBbAnimations()) {
                CosmeticBbAnimation idle = model.findBbAnimation(
                        "idling", "idle", "idle_arms", "idleMonsBob", "defaultstate");
                if (idle != null) {
                    for (CosmeticBone root : model.getRootBones()) root.resetAnim();
                    CosmeticBbAnimation.apply(model, idle, 0f);
                } else {
                    animator.apply(model, delta, 0f, 0f, 0.02f);
                }
            } else {
                animator.apply(model, delta, 0f, 0f, 0.02f);
            }

            CosmeticManager.getInstance().getRenderer()
                    .render(ms, consumers, model,
                            LightmapTextureManager.MAX_LIGHT_COORDINATE,
                            OverlayTexture.DEFAULT_UV, true, CosmeticRenderSpace.PREVIEW);
            consumers.draw();
        } catch (Exception ignored) {
        } finally {
            ms.pop();
            ScissorUtility.pop();
        }

        Font hintFont = Fonts.MEDIUM.getFont(4.5f);
        ctx.drawCenteredText(hintFont, "drag to rotate",
                cx, workTop + 3f, new ColorRGBA(120, 120, 140, 80 * pvF));

        if (!model.getDescription().isEmpty()) {
            DrawUtility.drawRoundedRect(ms, x + 8f, descY, w - 16f, descBoxH,
                    BorderRadius.all(4f), new ColorRGBA(20, 18, 28, 160 * pvF));
            Font descFont = Fonts.MEDIUM.getFont(5.5f);
            ctx.drawCenteredText(descFont, model.getDescription(),
                    cx, descY + 4f, new ColorRGBA(120, 120, 140, pvA));
        }

        String src = model.getSource();
        if (src != null && !src.isBlank()) {
            Font tagFont = Fonts.MEDIUM.getFont(4.5f);
            float tagS = 4.5f;
            float tagW = tagFont.width(src) + 8f;
            DrawUtility.drawRoundedRect(ms, cx - tagW / 2f, descY - 14f,
                    tagW, 11f, BorderRadius.all(3f), new ColorRGBA(35, 30, 50, 160 * pvF));
            ctx.drawCenteredText(tagFont, src, cx, descY - 14f + 2f, new ColorRGBA(120, 120, 140, pvA));
        }

        float btnW = w - 16f;
        float btnX = x + 8f;
        ColorRGBA btnCol = worn ? new ColorRGBA(255, 60, 60) : new ColorRGBA(255, 200, 0);
        String label = worn ? "Unequip" : "Equip";
        boolean hvBtn = mouseX >= btnX && mouseX <= btnX + btnW && mouseY >= btnY && mouseY <= btnY + btnH;

        DrawUtility.drawRoundedRect(ms, btnX, btnY, btnW, btnH,
                BorderRadius.all(5f), btnCol.withAlpha((hvBtn ? 70 : 45) * pvF / 255f * 255));
        DrawUtility.drawRoundedRect(ms, btnX, btnY, btnW, btnH,
                BorderRadius.all(5f), btnCol.withAlpha((hvBtn ? 55 : 35) * pvF / 255f * 255));

        Font btnFont = Fonts.BOLD.getFont(7f);
        ctx.drawCenteredText(btnFont, label,
                cx, btnY + btnH / 2f - btnFont.height() / 2f, new ColorRGBA(255, 255, 255, pvA));
    }

    private void drawCardsArea(CustomDrawContext ctx, MatrixStack ms, int mouseX, int mouseY,
                               float x, float y, float w, float h, int a) {
        List<CosmeticModel> models = CosmeticRepository.getInstance().byType(selectedTab);
        CosmeticModel active = CosmeticManager.getInstance().getActive(selectedTab);

        ScissorUtility.push(ms, x, y, w, h);

        float pad = scaled(CARDS_PAD);
        float colGap = scaled(COL_GAP);
        float itemH = scaled(CARD_H);
        float rowGap = scaled(CARD_GAP);
        float colW = (w - pad * 2f - colGap) / 2f;
        float scrollY = scrollAnim.getValue();

        float startY = y + pad - scrollY;

        if (models.isEmpty()) {
            Font emptyFont = Fonts.MEDIUM.getFont(5.5f);
            float cy = y + h / 2f - 16f;
            ctx.drawCenteredText(emptyFont, "No cosmetics found",
                    x + w / 2f, cy, new ColorRGBA(120, 120, 140, a));
            ctx.drawCenteredText(emptyFont, "Place models in cosmetics folder",
                    x + w / 2f, cy + 9f, new ColorRGBA(120, 120, 140, (int) (170 * a / 255f)));
        }

        for (int i = 0; i < models.size(); i++) {
            CosmeticModel model = models.get(i);
            int col = i % 2;
            int row = i / 2;

            float cardX = x + pad + col * (colW + colGap);
            float cardY = startY + row * (itemH + rowGap);

            boolean isActive = model == active;
            boolean isPreview = model == previewModel;
            boolean inView = cardY + itemH > y && cardY < y + h;
            if (!inView) continue;

            boolean isHovered = mouseX >= cardX && mouseX <= cardX + colW
                    && mouseY >= cardY && mouseY <= cardY + itemH;

            ColorRGBA cardBg;
            if (isActive || isPreview) {
                cardBg = new ColorRGBA(32, 26, 46, 180 * a / 255);
            } else if (isHovered) {
                cardBg = new ColorRGBA(26, 26, 36, 160 * a / 255);
            } else {
                cardBg = new ColorRGBA(18, 18, 26, 130 * a / 255);
            }
            DrawUtility.drawRoundedRect(ms, cardX, cardY, colW, itemH,
                    BorderRadius.all(5f), cardBg);

            if (isActive || isPreview) {
                DrawUtility.drawRoundedRect(ms, cardX, cardY, 2f, itemH,
                        BorderRadius.all(1f), new ColorRGBA(255, 200, 0, a));
            }

            float swR = 10f;
            float swCX = cardX + 8f + swR;
            float swCY = cardY + itemH / 2f;

            if (model.getAccentColor() != null) {
                try {
                    ColorRGBA accent = ColorRGBA.fromHex(model.getAccentColor());
                    DrawUtility.drawRoundedRect(ms, swCX - swR - 2f, swCY - swR - 2f,
                            swR * 2f + 4f, swR * 2f + 4f, BorderRadius.all(swR + 2f),
                            accent.withAlpha(20 * a / 255));
                    DrawUtility.drawRoundedRect(ms, swCX - swR, swCY - swR,
                            swR * 2f, swR * 2f, BorderRadius.all(swR),
                            accent.withAlpha(160 * a / 255));
                } catch (Exception e) {
                    DrawUtility.drawRoundedRect(ms, swCX - swR, swCY - swR,
                            swR * 2f, swR * 2f, BorderRadius.all(swR),
                            new ColorRGBA(60, 60, 80, 120 * a / 255));
                }
            }

            float textX = swCX + swR + 6f;
            float maxTextW = colW - (textX - cardX) - 12f;
            Font nameFont = Fonts.BOLD.getFont(6.5f);
            Font descFont = Fonts.MEDIUM.getFont(5f);
            float nameY = cardY + 6.5f;
            float descY2 = nameY + nameFont.height() + 2.5f;

            ColorRGBA nameCol = isActive ? new ColorRGBA(255, 200, 0) : new ColorRGBA(220, 220, 230);
            String name = model.getName();
            while (name.length() > 2 && nameFont.width(name) > maxTextW)
                name = name.substring(0, name.length() - 1);
            if (!name.equals(model.getName())) name += "...";
            ctx.drawText(nameFont, name, textX, nameY, nameCol.withAlpha(a));

            if (!model.getDescription().isEmpty()) {
                String desc = model.getDescription();
                while (desc.length() > 2 && descFont.width(desc) > maxTextW)
                    desc = desc.substring(0, desc.length() - 1);
                if (!desc.equals(model.getDescription())) desc += "...";
                ctx.drawText(descFont, desc, textX, descY2, new ColorRGBA(120, 120, 140, 170 * a / 255));
            }

            String src = model.getSource();
            if (src != null && !src.isBlank()) {
                Font srcFont = Fonts.MEDIUM.getFont(4f);
                float tagW = srcFont.width(src) + 5f;
                float tagX = cardX + colW - 5f - tagW;
                float tY = cardY + itemH - 4f - 9f;
                DrawUtility.drawRoundedRect(ms, tagX, tY, tagW, 9f,
                        BorderRadius.all(2.5f), new ColorRGBA(38, 35, 55, 130 * a / 255));
                ctx.drawText(srcFont, src, tagX + 2.5f, tY + 1.5f,
                        new ColorRGBA(120, 120, 140, 190 * a / 255));
            }

            if (isActive) {
                Font chkFont = Fonts.BOLD.getFont(6f);
                ctx.drawText(chkFont, "+",
                        cardX + colW - 8f - chkFont.height(),
                        cardY + 4f, new ColorRGBA(255, 200, 0, a));
            }
        }

        ScissorUtility.pop();

        float rows = (float) Math.ceil(models.size() / 2.0);
        float totalH = rows * (itemH + rowGap) + pad * 2f;
        float maxSc = Math.max(0, totalH - h);
        scroll = MathHelper.clamp(scroll, 0, maxSc);
    }

    private void drawCategoryBar(CustomDrawContext ctx, MatrixStack ms, int mouseX, int mouseY,
                                 float x, float y, float w, float h,
                                 int a, float anim) {
        DrawUtility.drawRect(ms, x, y, w, h, new ColorRGBA(16, 16, 22, 220 * anim));
        DrawUtility.drawRect(ms, x, y, 1f, h, new ColorRGBA(255, 255, 255, 14 * anim));

        CosmeticType[] types = CosmeticType.values();
        float btnH = 52f;
        float btnW = w - 8f;
        float btnX = x + 4f;
        float topPad = 8f;

        for (int i = 0; i < types.length; i++) {
            CosmeticType type = types[i];
            boolean selected = type == selectedTab;
            float btnY = y + topPad + i * (btnH + 3f);
            boolean hov = mouseX >= btnX && mouseX <= btnX + btnW
                    && mouseY >= btnY && mouseY <= btnY + btnH;

            if (selected) {
                DrawUtility.drawRoundedRect(ms, btnX, btnY, btnW, btnH,
                        BorderRadius.all(5f), new ColorRGBA(32, 26, 52, 200 * anim));
                DrawUtility.drawRect(ms, btnX, btnY + 8f, 2f, btnH - 16f,
                        new ColorRGBA(255, 200, 0, a));
            } else if (hov) {
                DrawUtility.drawRoundedRect(ms, btnX, btnY, btnW, btnH,
                        BorderRadius.all(5f), new ColorRGBA(24, 24, 34, 140 * anim));
            }

            Font iconFont = Fonts.BOLD.getFont(8f);
            String icon = type.getDisplayName().substring(0, Math.min(2, type.getDisplayName().length()));
            float iconY = btnY + 10f;
            ColorRGBA iconCol = selected ? new ColorRGBA(255, 200, 0) : new ColorRGBA(120, 120, 140);
            ctx.drawCenteredText(iconFont, icon, btnX + btnW / 2f, iconY, iconCol.withAlpha(a));

            Font labelFont = Fonts.MEDIUM.getFont(4.8f);
            String label = type.getDisplayName();
            ctx.drawCenteredText(labelFont, label, btnX + btnW / 2f,
                    iconY + iconFont.height() + 3f,
                    selected ? new ColorRGBA(220, 220, 230, a) : new ColorRGBA(120, 120, 140, 170 * a / 255));

            int cnt = CosmeticRepository.getInstance().byType(type).size();
            if (cnt > 0) {
                String cntStr = String.valueOf(cnt);
                Font cntFont = Fonts.MEDIUM.getFont(4f);
                float badgeW = cntFont.width(cntStr) + 5f;
                float badgeX = btnX + btnW / 2f - badgeW / 2f;
                float badgeY = iconY + iconFont.height() + labelFont.height() + 6f;
                DrawUtility.drawRoundedRect(ms, badgeX, badgeY, badgeW, 8f,
                        BorderRadius.all(4f), new ColorRGBA(40, 35, 65, 160 * anim));
                ctx.drawCenteredText(cntFont, cntStr, btnX + btnW / 2f,
                        badgeY + 1.5f, new ColorRGBA(120, 120, 140, 200 * anim));
            }
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        float[] pm = panelMouse(mouseX, mouseY);
        double mx = pm[0];
        double my = pm[1];

        float sw = mw.getScaledWidth();
        float sh = mw.getScaledHeight();
        float panelW = sw * PANEL_W_FRAC;
        float panelH = sh * PANEL_H_FRAC;
        float panelX = (sw - panelW) / 2f;
        float panelY = (sh - panelH) / 2f;
        float headerH = scaled(HEADER_H);
        float catBarW = scaled(CAT_BAR_W);
        float catBarX = panelX + panelW - catBarW;
        float pvW = pvAnim.getValue() * scaled(PV_TARGET_W);
        float contentY = panelY + headerH;
        float contentH = panelH - headerH;

        float closeSize = scaled(7f);
        float closeX = panelX + panelW - scaled(16f) - closeSize;
        float closeY = panelY + headerH / 2f - closeSize / 2f;
        if (mx >= closeX - 3f && mx <= closeX - 3f + closeSize + 6f
                && my >= closeY - 3f && my <= closeY - 3f + closeSize + 6f) {
            close();
            return true;
        }

        if (pvVisible && pvW > scaled(8f)) {
            float pvCloseX = panelX + pvW - scaled(10f) - scaled(5.5f);
            float pvCloseY = contentY + scaled(10f);
            if (mx >= pvCloseX - 3f && mx <= pvCloseX - 3f + scaled(5.5f) + 6f
                    && my >= pvCloseY - 3f && my <= pvCloseY - 3f + scaled(5.5f) + 6f) {
                pvVisible = false;
                previewModel = null;
                return true;
            }

            if (previewModel != null) {
                float btnH = scaled(18f);
                float btnY = contentY + contentH - scaled(8f) - btnH;
                float btnW = pvW - scaled(16f);
                float btnBX = panelX + scaled(8f);
                if (mx >= btnBX && mx <= btnBX + btnW && my >= btnY && my <= btnY + btnH) {
                    CosmeticModel equip = CosmeticManager.getInstance().getActive(selectedTab);
                    boolean worn = equip != null && equip.getId().equals(previewModel.getId());
                    if (worn) {
                        CosmeticManager.getInstance().clear(selectedTab);
                    } else {
                        equipModel(previewModel);
                    }
                    return true;
                }
            }

            if (mx >= panelX && mx <= panelX + pvW && my >= contentY && my <= contentY + contentH) {
                dragging = true;
                lastMouseX = mouseX;
                return true;
            }
        }

        if (clickCategoryBar(mx, my, catBarX, contentY, catBarW, contentH)) return true;

        float contentX = panelX + pvW;
        float contentW = catBarX - contentX;
        CosmeticModel clicked = findCardAt(mx, my, contentX, contentY, contentW, contentH);
        if (clicked != null && button == 0) {
            previewModel = clicked;
            pvVisible = true;
            previewRotYTarget = 25f;
            return true;
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    private boolean clickCategoryBar(double mx, double my,
                                     float barX, float barY, float barW, float barH) {
        CosmeticType[] types = CosmeticType.values();
        float btnH = 52f;
        float btnW = barW - 8f;
        float btnX = barX + 4f;
        float topPad = 8f;

        for (int i = 0; i < types.length; i++) {
            float btnY = barY + topPad + i * (btnH + 3f);
            if (mx >= btnX && mx <= btnX + btnW && my >= btnY && my <= btnY + btnH) {
                if (selectedTab != types[i]) {
                    selectedTab = types[i];
                    scroll = 0f;
                    previewModel = null;
                    pvVisible = false;
                }
                return true;
            }
        }
        return false;
    }

    private CosmeticModel findCardAt(double mx, double my,
                                     float x, float y, float w, float h) {
        List<CosmeticModel> models = CosmeticRepository.getInstance().byType(selectedTab);
        float pad = scaled(CARDS_PAD);
        float colGap = scaled(COL_GAP);
        float itemH = scaled(CARD_H);
        float rowGap = scaled(CARD_GAP);
        float colW = (w - pad * 2f - colGap) / 2f;
        float scrollY = scrollAnim.getValue();
        float startY = y + pad - scrollY;

        for (int i = 0; i < models.size(); i++) {
            int col = i % 2;
            int row = i / 2;
            float cx = x + pad + col * (colW + colGap);
            float cy = startY + row * (itemH + rowGap);
            if (cy + itemH > y && cy < y + h
                    && mx >= cx && mx <= cx + colW && my >= cy && my <= cy + itemH) {
                return models.get(i);
            }
        }
        return null;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        dragging = false;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        if (dragging) {
            previewRotYTarget += (float) (mouseX - lastMouseX) * 0.6f;
            lastMouseX = mouseX;
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double hAmount, double vAmount) {
        float sw = mw.getScaledWidth();
        float sh = mw.getScaledHeight();
        float panelW = sw * PANEL_W_FRAC;
        float panelX = (sw - panelW) / 2f;
        float catBarX = panelX + panelW - scaled(CAT_BAR_W);
        float pvW = pvAnim.getValue() * scaled(PV_TARGET_W);

        if (mouseX > panelX + pvW && mouseX < catBarX) {
            scroll -= (float) (vAmount * scaled(18f));
        }
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            close();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private void equipModel(CosmeticModel model) {
        CosmeticManager.getInstance().setActive(selectedTab, model);
        previewModel = CosmeticManager.getInstance().getActive(selectedTab);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    @Override
    public void renderBackground(DrawContext ctx, int mouseX, int mouseY, float delta) {
    }
}
