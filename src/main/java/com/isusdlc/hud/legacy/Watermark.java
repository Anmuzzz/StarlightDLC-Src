package com.isusdlc.hud.legacy;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.PlayerListEntry;

import java.awt.*;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class Watermark extends AbstractHudElement {

    private String lastFps = "";
    private String oldFps = "";
    private long fpsAnimationStart = 0;

    private String lastTime = "";
    private String oldTime = "";
    private long timeAnimationStart = 0;

    private static final long ANIMATION_DURATION = 200;
    private static final float ANIMATION_OFFSET = 8.0f;

    public Watermark() {
        super("Watermark", 10, 10, 100, 15, true);
        startAnimation();
    }

    @Override
    public void tick() {
    }

    private int clampAlpha(float alpha) {
        return Math.max(0, Math.min(255, (int) (alpha * 255)));
    }

    private int getPing() {
        if (mc.getNetworkHandler() != null && mc.player != null) {
            PlayerListEntry entry = mc.getNetworkHandler().getPlayerListEntry(mc.player.getUuid());
            if (entry != null) {
                return entry.getLatency();
            }
        }
        return 0;
    }

    private String getServerIP() {
        if (mc.getCurrentServerEntry() != null) {
            return mc.getCurrentServerEntry().address;
        }
        return "localhost";
    }

    @Override
    public void drawDraggable(DrawContext context, int alpha) {
        if (alpha <= 0) return;

        float alphaFactor = alpha / 255.0f;
        float x = getX();
        float y = getY();

        String username = mc.getSession().getUsername();
        String fpsNumber = String.valueOf(mc.getCurrentFps());
        String time = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"));
        String ping = getPing() + "ms";
        String server = getServerIP();

        long currentTime = System.currentTimeMillis();

        if (!fpsNumber.equals(lastFps)) {
            oldFps = lastFps;
            lastFps = fpsNumber;
            fpsAnimationStart = currentTime;
        }

        if (!time.equals(lastTime)) {
            oldTime = lastTime;
            lastTime = time;
            timeAnimationStart = currentTime;
        }

        float fpsAnimation = Math.min(1.0f, (currentTime - fpsAnimationStart) / (float) ANIMATION_DURATION);
        float timeAnimation = Math.min(1.0f, (currentTime - timeAnimationStart) / (float) ANIMATION_DURATION);

        float textSize = 6f;
        float spacing = 7.5f;
        float padding = 5f;
        float iconTextGap = 3.0f;

        float usernameWidth = Fonts.BOLD.getWidth(username, textSize);
        float pingWidth = Fonts.BOLD.getWidth(ping, textSize);
        float fpsWidth = Fonts.BOLD.getWidth(fpsNumber + " FPS", textSize);
        float timeWidth = Fonts.BOLD.getWidth(time, textSize);
        float serverWidth = Fonts.BOLD.getWidth(server, textSize);

        float totalWidth = padding * 2;
        totalWidth += spacing * 2;
        totalWidth += 8.5f + iconTextGap;
        totalWidth += 8.5f + iconTextGap;
        totalWidth += usernameWidth + spacing;
        totalWidth += 8.5f + iconTextGap + pingWidth + spacing;
        totalWidth += 8.5f + iconTextGap + fpsWidth + spacing;
        totalWidth += 8.5f + iconTextGap + timeWidth + spacing;
        totalWidth += 8.5f + iconTextGap + serverWidth;

        setWidth((int) Math.ceil(totalWidth));
        setHeight(15);

        int bgAlpha = (int) (230 * alphaFactor);
        int iconAlpha = (int) (255 * alphaFactor);
        int textAlpha = (int) (255 * alphaFactor);

        Color iconColor = new Color(135, 145, 255, iconAlpha);
        Color textColor = new Color(255, 255, 255, textAlpha);

        Render2D.gradientRect(x, y, totalWidth, 15,
                new int[]{
                        new Color(20, 20, 20, bgAlpha).getRGB(),
                        new Color(15, 15, 15, bgAlpha).getRGB(),
                        new Color(20, 20, 20, bgAlpha).getRGB(),
                        new Color(15, 15, 15, bgAlpha).getRGB()
                },
                5);
        Render2D.outline(x, y, totalWidth, 15, 0.35f, new Color(45, 45, 45, bgAlpha).getRGB(), 5);

        float textY = y + 5.0f;
        float iconY = y + 2.5f;
        float logoIconY = y + 3.5f;
        float logoAY = y + 2.0f;
        float offsetX = x + padding + 2.0f;

        Fonts.safinBpekax.draw("A", offsetX, logoAY, 8.5f, iconColor.getRGB());
        offsetX += 8.5f + spacing;

        Fonts.CATEGORY_ICONS.draw("d", offsetX, logoIconY, 8.5f, iconColor.getRGB());
        offsetX += 8.5f + iconTextGap;

        Fonts.BOLD.draw(username, offsetX, textY, textSize, textColor.getRGB());
        offsetX += usernameWidth + spacing;

        Fonts.HUDNEW.draw("K", offsetX, iconY, 8.5f, iconColor.getRGB());
        offsetX += 8.5f + iconTextGap;
        Fonts.BOLD.draw(ping, offsetX, textY, textSize, textColor.getRGB());
        offsetX += pingWidth + spacing;

        Fonts.HUDNEW.draw("C", offsetX, iconY, 8.5f, iconColor.getRGB());
        offsetX += 8.5f + iconTextGap;
        drawAnimatedTextPerChar(fpsNumber, oldFps, offsetX, textY, textSize, fpsAnimation);
        offsetX += Fonts.BOLD.getWidth(fpsNumber, textSize);
        Fonts.BOLD.draw(" FPS", offsetX, textY, textSize, textColor.getRGB());
        offsetX += Fonts.BOLD.getWidth(" FPS", textSize) + spacing;

        Fonts.HUDNEW.draw("A", offsetX, iconY, 8.5f, iconColor.getRGB());
        offsetX += 8.5f + iconTextGap;
        drawAnimatedTextPerChar(time, oldTime, offsetX, textY, textSize, timeAnimation);
        offsetX += timeWidth + spacing;

        Fonts.HUDNEW.draw("B", offsetX, iconY, 8.5f, iconColor.getRGB());
        offsetX += 8.5f + iconTextGap;
        Fonts.BOLD.draw(server, offsetX, textY, textSize, textColor.getRGB());
    }

    private void drawAnimatedTextPerChar(String newText, String oldText, float x, float y, float size, float progress) {
        if (oldText.isEmpty() || progress >= 1.0f) {
            Fonts.BOLD.draw(newText, x, y, size, new Color(255, 255, 255, 255).getRGB());
            return;
        }

        float offsetX = x;
        int maxLen = Math.max(newText.length(), oldText.length());
        String paddedNew = padLeft(newText, maxLen);
        String paddedOld = padLeft(oldText, maxLen);

        for (int i = 0; i < paddedNew.length(); i++) {
            char newChar = paddedNew.charAt(i);
            char oldChar = paddedOld.charAt(i);
            if (newChar == ' ' && oldChar == ' ') continue;

            float charWidth = Fonts.BOLD.getWidth(String.valueOf(newChar != ' ' ? newChar : oldChar), size);
            boolean isNewDigit = Character.isDigit(newChar) || newChar == '.';
            boolean isOldDigit = Character.isDigit(oldChar) || oldChar == '.';
            boolean hasChanged = newChar != oldChar;

            if (!hasChanged || (!isNewDigit && !isOldDigit)) {
                if (newChar != ' ') {
                    Fonts.BOLD.draw(String.valueOf(newChar), offsetX, y, size, new Color(255, 255, 255, 255).getRGB());
                }
            } else {
                float easedProgress = easeOutCubic(progress);
                if (oldChar != ' ' && isOldDigit) {
                    float oldAlpha = 1.0f - easedProgress;
                    float oldOffsetY = easedProgress * ANIMATION_OFFSET;
                    int alpha = clampAlpha(oldAlpha);
                    if (alpha > 0) Fonts.BOLD.draw(String.valueOf(oldChar), offsetX, y + oldOffsetY, size, new Color(255, 255, 255, alpha).getRGB());
                }
                if (newChar != ' ' && isNewDigit) {
                    float newAlpha = easedProgress;
                    float newOffsetY = (1.0f - easedProgress) * -ANIMATION_OFFSET;
                    int alpha = clampAlpha(newAlpha);
                    if (alpha > 0) Fonts.BOLD.draw(String.valueOf(newChar), offsetX, y + newOffsetY, size, new Color(255, 255, 255, alpha).getRGB());
                }
            }
            offsetX += charWidth;
        }
    }

    private String padLeft(String text, int length) {
        if (text.length() >= length) return text;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length - text.length(); i++) sb.append(" ");
        sb.append(text);
        return sb.toString();
    }

    private float easeOutCubic(float t) {
        return 1.0f - (float) Math.pow(1.0 - t, 3);
    }
}
