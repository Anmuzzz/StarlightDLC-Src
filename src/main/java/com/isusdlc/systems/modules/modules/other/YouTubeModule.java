package com.isusdlc.systems.modules.modules.other;

import com.isusdlc.elegant;
import com.isusdlc.framework.base.CustomDrawContext;
import com.isusdlc.framework.msdf.Font;
import com.isusdlc.framework.msdf.Fonts;
import com.isusdlc.framework.objects.BorderRadius;
import com.isusdlc.systems.event.EventListener;
import com.isusdlc.systems.event.impl.render.HudRenderEvent;
import com.isusdlc.systems.event.impl.window.MouseEvent;
import com.isusdlc.systems.modules.api.ModuleCategory;
import com.isusdlc.systems.modules.api.ModuleInfo;
import com.isusdlc.systems.modules.impl.BaseModule;
import com.isusdlc.systems.notifications.NotificationType;
import com.isusdlc.ui.mainmenu.JavaFxVideoSurface;
import com.isusdlc.ui.mainmenu.YouTubeScreen;
import com.isusdlc.utility.colors.ColorRGBA;
import com.isusdlc.utility.colors.Colors;
import com.isusdlc.utility.render.ScissorUtility;
import net.minecraft.util.Util;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec2f;

@ModuleInfo(
    name = "YouTube",
    category = ModuleCategory.OTHER,
    desc = "Плавающее окно YouTube поверх мира: перетаскивание, ресайз, звук"
)
public class YouTubeModule extends BaseModule {
    private static final float TITLE_BAR = 16.0F;
    private static final float MIN_WIDTH = 240.0F;
    private static final float MAX_WIDTH = 960.0F;
    private static final float EDGE = 12.0F;
    private static final float BTN_SIZE = 12.0F;

    private String videoId;
    private String videoTitle = "YouTube";
    private boolean playing = true;

    private float windowX = 30.0F;
    private float windowY = 30.0F;
    private float windowWidth = 427.0F;
    private boolean positionInitialized;

    private boolean dragging;
    private boolean resizing;
    private float grabX;
    private float grabY;

    private float btnPauseX;
    private float btnBrowserX;
    private float btnCloseX;
    private float btnY;
    private boolean hoverPause;
    private boolean hoverBrowser;
    private boolean hoverClose;
    private boolean hoverResize;

    @Override
    public void onEnable() {
        if (this.videoId == null) {
            mc.setScreen(new YouTubeScreen());
        }
        // если видео уже загружено — окно просто снова показывается поверх мира
    }

    @Override
    public void onDisable() {
        if (this.videoId != null) {
            JavaFxVideoSurface.getInstance().setPlaying(this.videoId, false);
        }
        this.dragging = false;
        this.resizing = false;
    }

    /** Вызывается из экрана поиска. Без JavaFX — фолбэк в системный браузер. */
    public void startVideo(String id, String title) {
        if (!JavaFxVideoSurface.isAvailable()) {
            Util.getOperatingSystem().open("https://www.youtube.com/watch?v=" + id);
            elegant.getInstance().getNotificationManager()
                .addNotification(NotificationType.INFO, "YouTube: JavaFX не найден — открыл в браузере");
            return;
        }

        this.videoId = id;
        this.videoTitle = title == null || title.isEmpty() ? "YouTube" : title;
        this.playing = true;
        JavaFxVideoSurface.getInstance().load(id);
    }

    private boolean hasVideo() {
        return this.videoId != null;
    }

    private float videoHeight() {
        return this.windowWidth * 9.0F / 16.0F;
    }

    private float totalHeight() {
        return videoHeight() + TITLE_BAR;
    }

    private float mouseX() {
        return (float) (mc.mouse.getX() / mc.getWindow().getScaleFactor());
    }

    private float mouseY() {
        return (float) (mc.mouse.getY() / mc.getWindow().getScaleFactor());
    }

    private boolean inRect(float mx, float my, float x, float y, float w, float h) {
        return mx >= x && mx <= x + w && my >= y && my <= y + h;
    }

    private final EventListener<MouseEvent> onMouse = event -> {
        if (!hasVideo()) return;
        if (mc.currentScreen != null) {
            this.dragging = false;
            this.resizing = false;
            return;
        }

        float mx = mouseX();
        float my = mouseY();

        if (event.getAction() == 1 && event.getButton() == 0) {
            if (inRect(mx, my, this.btnCloseX, this.btnY, BTN_SIZE, BTN_SIZE)) {
                this.closeVideo();
                return;
            }
            if (inRect(mx, my, this.btnBrowserX, this.btnY, BTN_SIZE, BTN_SIZE)) {
                Util.getOperatingSystem().open("https://www.youtube.com/watch?v=" + this.videoId);
                return;
            }
            if (inRect(mx, my, this.btnPauseX, this.btnY, BTN_SIZE, BTN_SIZE)) {
                this.playing = !this.playing;
                JavaFxVideoSurface.getInstance().setPlaying(this.videoId, this.playing);
                return;
            }
            if (inRect(mx, my, this.windowX + this.windowWidth - EDGE, this.windowY + totalHeight() - EDGE, EDGE, EDGE)) {
                this.resizing = true;
                return;
            }
            if (inRect(mx, my, this.windowX, this.windowY, this.windowWidth, TITLE_BAR)) {
                this.dragging = true;
                this.grabX = mx - this.windowX;
                this.grabY = my - this.windowY;
            }
        } else if (event.getAction() == 0 && event.getButton() == 0) {
            this.dragging = false;
            this.resizing = false;
        }
    };

    private void closeVideo() {
        if (this.videoId != null) {
            JavaFxVideoSurface.getInstance().setPlaying(this.videoId, false);
        }
        this.videoId = null;
        this.dragging = false;
        this.resizing = false;
    }

    private final EventListener<HudRenderEvent> onHud = event -> {
        if (!hasVideo() || mc.currentScreen != null) return;

        JavaFxVideoSurface surface = JavaFxVideoSurface.getInstance();
        surface.updateTexture(mc.getTextureManager());

        float scaledWidth = mc.getWindow().getScaledWidth();
        float scaledHeight = mc.getWindow().getScaledHeight();

        if (!this.positionInitialized) {
            this.windowWidth = MathHelper.clamp(scaledWidth * 0.32F, MIN_WIDTH, MAX_WIDTH);
            this.windowX = Math.max(10.0F, scaledWidth - this.windowWidth - 24.0F);
            this.windowY = Math.max(10.0F, scaledHeight - totalHeight() - 60.0F);
            this.positionInitialized = true;
        }

        if (this.dragging) {
            this.windowX = MathHelper.clamp(mouseX() - this.grabX, 0.0F, Math.max(0.0F, scaledWidth - this.windowWidth));
            this.windowY = MathHelper.clamp(mouseY() - this.grabY, 0.0F, Math.max(0.0F, scaledHeight - totalHeight()));
        }
        if (this.resizing) {
            this.windowWidth = MathHelper.clamp(mouseX() - this.windowX, MIN_WIDTH, MAX_WIDTH);
        }

        float videoH = videoHeight();
        float totalH = totalHeight();

        this.hoverPause = inRect(mouseX(), mouseY(), this.btnPauseX, this.btnY, BTN_SIZE, BTN_SIZE);
        this.hoverBrowser = inRect(mouseX(), mouseY(), this.btnBrowserX, this.btnY, BTN_SIZE, BTN_SIZE);
        this.hoverClose = inRect(mouseX(), mouseY(), this.btnCloseX, this.btnY, BTN_SIZE, BTN_SIZE);
        this.hoverResize = inRect(mouseX(), mouseY(), this.windowX + this.windowWidth - EDGE, this.windowY + totalH - EDGE, EDGE, EDGE);

        var context = event.getContext();

        context.drawShadow(this.windowX - 3.0F, this.windowY - 3.0F, this.windowWidth + 6.0F, totalH + 6.0F, 12.0F, BorderRadius.all(8.0F), ColorRGBA.BLACK.withAlpha(90.0F));
        context.drawSquircle(this.windowX, this.windowY, this.windowWidth, totalH, 6.0F, BorderRadius.all(8.0F), Colors.getBackgroundColor().withAlpha(245.0F));

        // тайтл-бар: заголовок + кнопки
        Font font = Fonts.MEDIUM.getFont(6.5F);
        float titleWidth = this.windowWidth - 3.0F * (BTN_SIZE + 4.0F) - 12.0F;
        ScissorUtility.push(this.windowX + 6.0F, this.windowY, titleWidth, TITLE_BAR);
        context.drawText(font, this.videoTitle, this.windowX + 6.0F, this.windowY + (TITLE_BAR - font.height()) / 2.0F, Colors.getTextColor().withAlpha(200.0F));
        ScissorUtility.pop();

        float bx = this.windowX + this.windowWidth - 6.0F;
        this.btnCloseX = bx - BTN_SIZE;
        this.btnBrowserX = this.btnCloseX - 4.0F - BTN_SIZE;
        this.btnPauseX = this.btnBrowserX - 4.0F - BTN_SIZE;
        this.btnY = this.windowY + (TITLE_BAR - BTN_SIZE) / 2.0F;
        this.drawTitleButton(context, this.btnPauseX, this.playing ? "II" : "▶", this.hoverPause);
        this.drawTitleButton(context, this.btnBrowserX, "⤢", this.hoverBrowser);
        this.drawTitleButton(context, this.btnCloseX, "✕", this.hoverClose);

        // видео
        if (surface.hasFrame()) {
            ScissorUtility.push(this.windowX, this.windowY + TITLE_BAR, this.windowWidth, videoH);
            context.drawTexture(surface.getTextureId(), this.windowX, this.windowY + TITLE_BAR, this.windowWidth, videoH);
            ScissorUtility.pop();
        } else {
            context.drawRect(this.windowX, this.windowY + TITLE_BAR, this.windowWidth, videoH, ColorRGBA.BLACK.withAlpha(160.0F));
            String loading = "Загрузка плеера...";
            context.drawText(font, loading, this.windowX + (this.windowWidth - font.width(loading)) / 2.0F,
                this.windowY + TITLE_BAR + videoH / 2.0F - font.height() / 2.0F, Colors.getTextColor().withAlpha(150.0F));
        }

        // ручка ресайза в правом нижнем углу
        ColorRGBA handleColor = Colors.getTextColor().withAlpha(this.hoverResize ? 230.0F : 120.0F);
        for (int i = 0; i < 3; i++) {
            float offset = 4.0F + i * 3.5F;
            context.drawLine(
                new Vec2f(this.windowX + this.windowWidth - offset, this.windowY + totalH - 2.0F),
                new Vec2f(this.windowX + this.windowWidth - 2.0F, this.windowY + totalH - offset),
                handleColor
            );
        }
    };

    private void drawTitleButton(CustomDrawContext context, float x, String glyph, boolean hovered) {
        ColorRGBA bg = hovered ? Colors.getBackgroundColor().mix(ColorRGBA.WHITE, 0.2F).withAlpha(230.0F)
            : Colors.getBackgroundColor().withAlpha(0.0F);
        if (hovered) {
            context.drawRoundedRect(x, this.btnY, BTN_SIZE, BTN_SIZE, BorderRadius.all(4.0F), bg);
        }

        Font font = Fonts.REGULAR.getFont(6.0F);
        context.drawText(font, glyph, x + (BTN_SIZE - font.width(glyph)) / 2.0F,
            this.btnY + (BTN_SIZE - font.height()) / 2.0F, Colors.getTextColor().withAlpha(hovered ? 255.0F : 170.0F));
    }
}
