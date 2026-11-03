package com.isusdlc.ui.mainmenu;

import com.isusdlc.framework.base.CustomScreen;
import com.isusdlc.framework.base.UIContext;
import com.isusdlc.framework.msdf.Font;
import com.isusdlc.framework.msdf.Fonts;
import com.isusdlc.framework.objects.BorderRadius;
import com.isusdlc.framework.objects.MouseButton;
import com.isusdlc.utility.colors.ColorRGBA;
import com.isusdlc.utility.colors.Colors;
import com.isusdlc.utility.gui.GuiUtility;
import com.isusdlc.utility.interfaces.IMinecraft;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;

public class ChangelogScreen extends CustomScreen implements IMinecraft {
    private static final String[] CHANGES = {
        "2.0.0-Alpha",
        "",
        "Добавлено:",
        "- Новая AimAssist (Speed, FOV, Distance, Shake, типы целей)",
        "- LegitTarget для KillAura (поворот камеры к цели)",
        "- ShiftTap (авто-шифт после удара)",
        "- NoInteract (блокировка взаимодействия с блоками)",
        "- WindJump (заряд ветра по кнопке)",
        "- ChestStealer (авто-грабёж сундуков)",
        "- AutoPotion (авто-бросок зелий)",
        "- AutoWeapon (авто-выбор лучшего оружия)",
        "- AutoSwap (свап предметов в оффхенд)",
        "- JumpCircle (круг при прыжке)",
        "- SoulESP (призрак убитого игрока)",
        "- AutoExplosion (авто-кристаллы)",
        "- TargetPearl (авто-бросок жемчуга)",
        "",
        "Изменено:",
        "- Переписана система поворота KillAura",
        "- Полностью переписан AimAssist со всеми настройками",
        "- Улучшена производительность",
        "",
        "Исправлено:",
        "- Исправлен Refmap краш в PrismLauncher",
        "- KillAura бьёт правой рукой",
        "- Исправлены различные баги"
    };

    private final Screen parent;

    public ChangelogScreen(Screen parent) {
        this.parent = parent;
    }

    @Override
    public void render(UIContext context) {
        context.drawRect(0, 0, this.width, this.height, ColorRGBA.BLACK.withAlpha(140));

        float panelWidth = 300;
        float panelHeight = 280;
        float x = (this.width - panelWidth) / 2;
        float y = (this.height - panelHeight) / 2;

        context.drawShadow(x - 5, y - 5, panelWidth + 10, panelHeight + 10, 18, BorderRadius.all(10), ColorRGBA.BLACK.withAlpha(80));
        context.drawSquircle(x, y, panelWidth, panelHeight, 10, BorderRadius.all(10), Colors.getBackgroundColor().withAlpha(230));

        Font titleFont = Fonts.MEDIUM.getFont(10);
        Font textFont = Fonts.REGULAR.getFont(7);
        Font buttonFont = Fonts.MEDIUM.getFont(7.5F);

        context.drawCenteredText(titleFont, "Ченджлог", this.width / 2, y + 14, Colors.getTextColor());

        float textY = y + 36;
        for (String line : CHANGES) {
            if (line.isEmpty()) {
                textY += 4;
                continue;
            }
            boolean isHeader = line.equals("2.0.0-Alpha");
            Font font = isHeader ? Fonts.BOLD.getFont(9) : textFont;
            context.drawText(font, line, x + 16, textY, isHeader ? Colors.getAccentColor() : Colors.getTextColor());
            textY += font.height() + 2;
        }

        float backWidth = 60;
        float backHeight = 16;
        float backX = x + panelWidth - backWidth - 14;
        float backY = y + panelHeight - backHeight - 10;
        drawButton(context, backX, backY, backWidth, backHeight, "Назад", buttonFont);
    }

    private void drawButton(UIContext context, float x, float y, float w, float h, String text, Font font) {
        boolean hovered = isHovered(x, y, w, h, context);
        ColorRGBA bg = Colors.getBackgroundColor().mix(ColorRGBA.WHITE, hovered ? 0.15F : 0).withAlpha(200);
        context.drawRoundedRect(x, y, w, h, BorderRadius.all(6), bg);
        context.drawCenteredText(font, text, x + w / 2, y + GuiUtility.getMiddleOfBox(font.height(), h), Colors.getTextColor());
    }

    private boolean isHovered(float x, float y, float w, float h, UIContext context) {
        double mx = context.getMouseX();
        double my = context.getMouseY();
        return mx >= x && mx <= x + w && my >= y && my <= y + h;
    }

    @Override
    public void onMouseClicked(double mouseX, double mouseY, MouseButton button) {
        if (button == MouseButton.LEFT) {
            float panelWidth = 300;
            float panelHeight = 280;
            float x = (this.width - panelWidth) / 2;
            float y = (this.height - panelHeight) / 2;

            float backWidth = 60;
            float backHeight = 16;
            float backX = x + panelWidth - backWidth - 14;
            float backY = y + panelHeight - backHeight - 10;

            if (mouseX >= backX && mouseX <= backX + backWidth && mouseY >= backY && mouseY <= backY + backHeight) {
                mc.setScreen(this.parent);
            }
        }
        super.onMouseClicked(mouseX, mouseY, button);
    }

    public boolean shouldPause() {
        return false;
    }

    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
    }
}
