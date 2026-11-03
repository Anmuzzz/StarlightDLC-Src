package com.isusdlc.ui.mainmenu;

import com.isusdlc.elegant;
import com.isusdlc.framework.base.CustomScreen;
import com.isusdlc.framework.base.UIContext;
import com.isusdlc.framework.msdf.Font;
import com.isusdlc.framework.msdf.Fonts;
import com.isusdlc.framework.objects.BorderRadius;
import com.isusdlc.framework.objects.MouseButton;
import com.isusdlc.systems.lua.LuaScriptEngine;
import com.isusdlc.systems.modules.modules.other.LuaModule;
import com.isusdlc.utility.colors.ColorRGBA;
import com.isusdlc.utility.colors.Colors;
import com.isusdlc.utility.gui.GuiUtility;
import com.isusdlc.utility.interfaces.IMinecraft;
import com.isusdlc.utility.render.ScissorUtility;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.glfw.GLFW;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class LuaScriptScreen extends CustomScreen implements IMinecraft {
    private static final float LIST_WIDTH = 130.0F;
    private static final float ROW_HEIGHT = 13.0F;
    private static final float BUTTON_WIDTH = 62.0F;
    private static final float BUTTON_HEIGHT = 15.0F;
    private static final float BUTTON_GAP = 6.0F;
    private static final String TAB = "    ";

    private final List<String> fileNames = new ArrayList<>();
    private final List<String> lines = new ArrayList<>();
    private String selectedFile;
    private boolean dirty;
    private int cursorLine;
    private int cursorCol;
    private int editorScroll;
    private int listScroll;
    private String status = "";
    private boolean statusError;
    private long statusTime;

    // layout, пересчитывается каждый кадр и используется в обработчиках ввода
    private float panelX, panelY, panelW, panelH;
    private float listX, listY, listW, listH;
    private float editorX, editorY, editorW, editorH;
    private float lineHeight;

    private Font uiFont;
    private Font editorFont;

    public LuaScriptScreen() {
    }

    @Override
    protected void init() {
        if (this.selectedFile == null) {
            this.refreshFileList();
            if (!this.fileNames.isEmpty()) {
                this.selectFile(this.fileNames.getFirst());
            }
        }
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    private File scriptsDir() {
        return LuaScriptEngine.getScriptsDirectory();
    }

    private void refreshFileList() {
        this.fileNames.clear();
        File[] files = this.scriptsDir().listFiles((dir, name) -> name.endsWith(".lua"));
        if (files != null) {
            for (File file : files) {
                this.fileNames.add(file.getName());
            }
        }
        Collections.sort(this.fileNames);
        this.listScroll = Math.min(this.listScroll, Math.max(0, this.fileNames.size() - 1));
    }

    private void selectFile(String name) {
        try {
            String code = Files.readString(this.scriptsDir().toPath().resolve(name));
            this.lines.clear();
            for (String line : code.split("\n", -1)) {
                this.lines.add(line);
            }
            if (this.lines.isEmpty()) {
                this.lines.add("");
            }

            this.selectedFile = name;
            this.dirty = false;
            this.cursorLine = 0;
            this.cursorCol = 0;
            this.editorScroll = 0;
            this.status = "";
        } catch (IOException e) {
            this.status = "Не удалось открыть: " + name;
            this.statusError = true;
            this.statusTime = System.currentTimeMillis();
        }
    }

    private void newFile() {
        int index = 1;
        String name;
        do {
            name = "script_" + index + ".lua";
            index++;
        } while (new File(this.scriptsDir(), name).exists());

        try {
            Files.writeString(this.scriptsDir().toPath().resolve(name), "-- новый скрипт\n\n");
        } catch (IOException e) {
            this.status = "Ошибка создания";
            this.statusError = true;
            this.statusTime = System.currentTimeMillis();
            return;
        }

        this.refreshFileList();
        this.selectFile(name);
        this.status = "Создан: " + name;
        this.statusTime = System.currentTimeMillis();
    }

    private void deleteSelected() {
        if (this.selectedFile == null) return;
        try {
            Files.deleteIfExists(this.scriptsDir().toPath().resolve(this.selectedFile));
        } catch (IOException e) {
            this.status = "Ошибка удаления";
            this.statusError = true;
            this.statusTime = System.currentTimeMillis();
            return;
        }

        this.status = "Удалён: " + this.selectedFile;
        this.statusTime = System.currentTimeMillis();
        this.selectedFile = null;
        this.lines.clear();
        this.dirty = false;
        this.refreshFileList();
        if (!this.fileNames.isEmpty()) {
            this.selectFile(this.fileNames.getFirst());
        }
    }

    private void saveCurrent() {
        if (this.selectedFile == null) return;
        try {
            Files.writeString(this.scriptsDir().toPath().resolve(this.selectedFile), String.join("\n", this.lines));
            this.dirty = false;
            this.status = "Сохранено";
            this.statusTime = System.currentTimeMillis();
            this.reloadEngine();
        } catch (IOException e) {
            this.status = "Ошибка сохранения";
            this.statusError = true;
            this.statusTime = System.currentTimeMillis();
        }
    }

    private void reloadEngine() {
        LuaModule lua = elegant.getInstance().getModuleManager().getModuleSafe(LuaModule.class);
        if (lua != null) {
            lua.reloadScripts();
        }
    }

    @Override
    public void render(UIContext context) {
        if (this.uiFont == null) {
            this.uiFont = Fonts.MEDIUM.getFont(8.0F);
            this.editorFont = Fonts.REGULAR.getFont(6.5F);
        }
        if (this.lines.isEmpty()) {
            this.lines.add("");
        }

        this.panelW = Math.min(470.0F, this.width - 30.0F);
        this.panelH = Math.min(310.0F, this.height - 30.0F);
        this.panelX = (this.width - this.panelW) / 2.0F;
        this.panelY = (this.height - this.panelH) / 2.0F;
        this.listX = this.panelX + 10.0F;
        this.listY = this.panelY + 26.0F;
        this.listW = LIST_WIDTH;
        this.listH = this.panelH - 26.0F - 34.0F;
        this.editorX = this.listX + this.listW + 8.0F;
        this.editorY = this.listY;
        this.editorW = this.panelX + this.panelW - 10.0F - this.editorX;
        this.editorH = this.listH;
        this.lineHeight = this.editorFont.height() + 2.0F;

        context.drawShadow(this.panelX - 4.0F, this.panelY - 4.0F, this.panelW + 8.0F, this.panelH + 8.0F, 14.0F, BorderRadius.all(10.0F), ColorRGBA.BLACK.withAlpha(70.0F));
        context.drawSquircle(this.panelX, this.panelY, this.panelW, this.panelH, 8.0F, BorderRadius.all(10.0F), Colors.getBackgroundColor().withAlpha(235.0F));

        String title = "Lua IDE" + (this.selectedFile != null ? "  —  " + this.selectedFile + (this.dirty ? " *" : "") : "");
        context.drawText(this.uiFont, title, this.panelX + 12.0F, this.panelY + 9.0F, Colors.getTextColor());

        if (!this.status.isEmpty() && System.currentTimeMillis() - this.statusTime < 4000L) {
            float statusWidth = this.uiFont.width(this.status);
            context.drawText(this.uiFont, this.status, this.panelX + this.panelW - 12.0F - statusWidth, this.panelY + 9.0F,
                this.statusError ? ColorRGBA.RED.withAlpha(230.0F) : Colors.getAccentColor());
        }

        this.renderFileList(context);
        this.renderEditor(context);
        this.renderButtons(context);
    }

    private void renderFileList(UIContext context) {
        context.drawSquircle(this.listX, this.listY, this.listW, this.listH, 6.0F, BorderRadius.all(6.0F), ColorRGBA.BLACK.withAlpha(60.0F));

        ScissorUtility.push(this.listX, this.listY, this.listW, this.listH);
        for (int i = 0; i < this.fileNames.size(); i++) {
            String name = this.fileNames.get(i);
            float rowY = this.listY + 4.0F + i * ROW_HEIGHT - this.listScroll * ROW_HEIGHT;
            if (rowY < this.listY - ROW_HEIGHT || rowY > this.listY + this.listH) continue;

            boolean selected = name.equals(this.selectedFile);
            if (selected) {
                context.drawRoundedRect(this.listX + 3.0F, rowY, this.listW - 6.0F, ROW_HEIGHT, BorderRadius.all(4.0F), Colors.getAccentColor().withAlpha(60.0F));
            }

            float textWidth = this.editorFont.width(name);
            float textX = this.listX + 7.0F;
            if (textWidth > this.listW - 12.0F) {
                textX += this.listW - 12.0F - textWidth;
            }
            context.drawText(this.editorFont, name, textX, rowY + (ROW_HEIGHT - this.editorFont.height()) / 2.0F,
                selected ? Colors.getAccentColor() : Colors.getTextColor().withAlpha(190.0F));
        }
        ScissorUtility.pop();
    }

    private void renderEditor(UIContext context) {
        context.drawSquircle(this.editorX, this.editorY, this.editorW, this.editorH, 6.0F, BorderRadius.all(6.0F), ColorRGBA.BLACK.withAlpha(60.0F));

        ScissorUtility.push(this.editorX, this.editorY, this.editorW, this.editorH);
        for (int i = this.editorScroll; i < this.lines.size(); i++) {
            float lineY = this.editorY + 6.0F + (i - this.editorScroll) * this.lineHeight;
            if (lineY > this.editorY + this.editorH) break;
            String line = this.lines.get(i);
            if (!line.isEmpty()) {
                context.drawText(this.editorFont, line, this.editorX + 8.0F, lineY, Colors.getTextColor());
            }
        }

        if (this.selectedFile != null && System.currentTimeMillis() / 500L % 2L == 0L) {
            String before = this.currentLine().substring(0, Math.min(this.cursorCol, this.currentLine().length()));
            float cursorX = this.editorX + 8.0F + this.editorFont.width(before);
            float cursorY = this.editorY + 6.0F + (this.cursorLine - this.editorScroll) * this.lineHeight;
            if (cursorY >= this.editorY && cursorY <= this.editorY + this.editorH - this.lineHeight) {
                context.drawRect(cursorX, cursorY, 1.2F, this.lineHeight, Colors.getTextColor().withAlpha(200.0F));
            }
        }
        ScissorUtility.pop();
    }

    private void renderButtons(UIContext context) {
        float buttonY = this.panelY + this.panelH - 26.0F;
        float buttonX = this.panelX + 10.0F;

        buttonX = this.drawButton(context, buttonX, buttonY, "New", false);
        buttonX = this.drawButton(context, buttonX, buttonY, "Delete", this.selectedFile != null);
        buttonX = this.drawButton(context, buttonX, buttonY, "Reload", false);
        buttonX = this.drawButton(context, buttonX, buttonY, "Save", this.dirty);
        this.drawButton(context, buttonX, buttonY, "Back", false);
    }

    private float drawButton(UIContext context, float x, float y, String text, boolean accent) {
        boolean hovered = GuiUtility.isHovered(x, y, BUTTON_WIDTH, BUTTON_HEIGHT, context.getMouseX(), context.getMouseY());
        ColorRGBA bg = accent
            ? Colors.getAccentColor().withAlpha(hovered ? 120.0F : 80.0F)
            : Colors.getBackgroundColor().mix(ColorRGBA.WHITE, hovered ? 0.15F : 0.0F).withAlpha(200.0F);
        context.drawRoundedRect(x, y, BUTTON_WIDTH, BUTTON_HEIGHT, BorderRadius.all(6.0F), bg);
        context.drawCenteredText(this.uiFont, text, x + BUTTON_WIDTH / 2.0F, y + GuiUtility.getMiddleOfBox(this.uiFont.height(), BUTTON_HEIGHT), Colors.getTextColor());
        return x + BUTTON_WIDTH + BUTTON_GAP;
    }

    @Override
    public void onMouseClicked(double mouseX, double mouseY, MouseButton button) {
        if (button == MouseButton.LEFT) {
            this.handleClicks(mouseX, mouseY);
        }
        super.onMouseClicked(mouseX, mouseY, button);
    }

    private void handleClicks(double mouseX, double mouseY) {
        float mx = (float) mouseX;
        float my = (float) mouseY;

        // кнопки
        float buttonY = this.panelY + this.panelH - 26.0F;
        if (my >= buttonY && my <= buttonY + BUTTON_HEIGHT) {
            float buttonX = this.panelX + 10.0F;
            if (mx >= buttonX && mx <= buttonX + BUTTON_WIDTH) { this.newFile(); return; }
            buttonX += BUTTON_WIDTH + BUTTON_GAP;
            if (mx >= buttonX && mx <= buttonX + BUTTON_WIDTH) { this.deleteSelected(); return; }
            buttonX += BUTTON_WIDTH + BUTTON_GAP;
            if (mx >= buttonX && mx <= buttonX + BUTTON_WIDTH) { this.reloadEngine(); this.status = "Перезагружено"; this.statusTime = System.currentTimeMillis(); return; }
            buttonX += BUTTON_WIDTH + BUTTON_GAP;
            if (mx >= buttonX && mx <= buttonX + BUTTON_WIDTH) { this.saveCurrent(); return; }
            buttonX += BUTTON_WIDTH + BUTTON_GAP;
            if (mx >= buttonX && mx <= buttonX + BUTTON_WIDTH) { mc.setScreen(null); return; }
        }

        // список файлов
        if (mx >= this.listX && mx <= this.listX + this.listW && my >= this.listY && my <= this.listY + this.listH) {
            int index = (int) ((my - this.listY - 4.0F) / ROW_HEIGHT) + this.listScroll;
            if (index >= 0 && index < this.fileNames.size()) {
                this.selectFile(this.fileNames.get(index));
            }
            return;
        }

        // редактор: установка каретки по клику
        if (this.selectedFile != null && mx >= this.editorX && mx <= this.editorX + this.editorW && my >= this.editorY && my <= this.editorY + this.editorH) {
            this.cursorLine = MathHelper.clamp((int) ((my - this.editorY - 6.0F) / this.lineHeight) + this.editorScroll, 0, this.lines.size() - 1);
            this.cursorCol = this.columnFromMouse(mx);
            this.ensureCursorVisible();
        }
    }

    private int columnFromMouse(float mouseX) {
        String line = this.currentLine();
        float target = mouseX - this.editorX - 8.0F;
        if (target <= 0.0F) return 0;
        for (int col = 0; col < line.length(); col++) {
            if (this.editorFont.width(line.substring(0, col + 1)) > target) {
                return col;
            }
        }
        return line.length();
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        float mx = (float) mouseX;
        float my = (float) mouseY;
        if (mx >= this.listX && mx <= this.listX + this.listW && my >= this.listY && my <= this.listY + this.listH) {
            int maxScroll = Math.max(0, this.fileNames.size() - (int) (this.listH / ROW_HEIGHT));
            this.listScroll = MathHelper.clamp(this.listScroll - (int) verticalAmount, 0, maxScroll);
            return true;
        }

        if (mx >= this.editorX && mx <= this.editorX + this.editorW && my >= this.editorY && my <= this.editorY + this.editorH) {
            this.editorScroll = MathHelper.clamp(this.editorScroll - (int) verticalAmount, 0, this.maxEditorScroll());
            return true;
        }

        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (this.selectedFile != null) {
            if (chr == '\t') {
                this.insert(TAB);
                return true;
            }
            if (chr >= 32) {
                this.insert(String.valueOf(chr));
                return true;
            }
        }
        return super.charTyped(chr, modifiers);
    }

    @Override
    public void onKeyPressed(int keyCode, int scanCode, int modifiers) {
        if (this.selectedFile == null) return;
        boolean ctrl = (modifiers & GLFW.GLFW_MOD_CONTROL) != 0;

        if (ctrl && keyCode == GLFW.GLFW_KEY_S) {
            this.saveCurrent();
            return;
        }

        if (ctrl && keyCode == GLFW.GLFW_KEY_V) {
            this.paste(mc.keyboard.getClipboard());
            return;
        }

        if (ctrl) return;

        switch (keyCode) {
            case GLFW.GLFW_KEY_LEFT -> {
                if (this.cursorCol > 0) {
                    this.cursorCol--;
                } else if (this.cursorLine > 0) {
                    this.cursorLine--;
                    this.cursorCol = this.currentLine().length();
                }
                this.ensureCursorVisible();
            }
            case GLFW.GLFW_KEY_RIGHT -> {
                if (this.cursorCol < this.currentLine().length()) {
                    this.cursorCol++;
                } else if (this.cursorLine < this.lines.size() - 1) {
                    this.cursorLine++;
                    this.cursorCol = 0;
                }
                this.ensureCursorVisible();
            }
            case GLFW.GLFW_KEY_UP -> {
                this.cursorLine = Math.max(0, this.cursorLine - 1);
                this.clampCursor();
                this.ensureCursorVisible();
            }
            case GLFW.GLFW_KEY_DOWN -> {
                this.cursorLine = Math.min(this.lines.size() - 1, this.cursorLine + 1);
                this.clampCursor();
                this.ensureCursorVisible();
            }
            case GLFW.GLFW_KEY_HOME -> this.cursorCol = 0;
            case GLFW.GLFW_KEY_END -> this.cursorCol = this.currentLine().length();
            case GLFW.GLFW_KEY_PAGE_UP -> {
                this.cursorLine = Math.max(0, this.cursorLine - this.visibleLines());
                this.clampCursor();
                this.ensureCursorVisible();
            }
            case GLFW.GLFW_KEY_PAGE_DOWN -> {
                this.cursorLine = Math.min(this.lines.size() - 1, this.cursorLine + this.visibleLines());
                this.clampCursor();
                this.ensureCursorVisible();
            }
            case GLFW.GLFW_KEY_BACKSPACE -> this.backspace();
            case GLFW.GLFW_KEY_DELETE -> this.deleteForward();
            case GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER -> this.splitLine();
            case GLFW.GLFW_KEY_TAB -> this.insert(TAB);
            default -> {
            }
        }
    }

    private String currentLine() {
        return this.lines.isEmpty() ? "" : this.lines.get(Math.min(this.cursorLine, this.lines.size() - 1));
    }

    private void clampCursor() {
        this.cursorLine = MathHelper.clamp(this.cursorLine, 0, Math.max(0, this.lines.size() - 1));
        this.cursorCol = MathHelper.clamp(this.cursorCol, 0, this.currentLine().length());
    }

    private int visibleLines() {
        return Math.max(1, (int) ((this.editorH - 12.0F) / this.lineHeight));
    }

    private int maxEditorScroll() {
        return Math.max(0, this.lines.size() - this.visibleLines());
    }

    private void ensureCursorVisible() {
        this.editorScroll = MathHelper.clamp(this.editorScroll, 0, this.maxEditorScroll());
        if (this.cursorLine < this.editorScroll) {
            this.editorScroll = this.cursorLine;
        } else if (this.cursorLine >= this.editorScroll + this.visibleLines()) {
            this.editorScroll = this.cursorLine - this.visibleLines() + 1;
        }
    }

    private void insert(String text) {
        String line = this.currentLine();
        int col = Math.min(this.cursorCol, line.length());
        this.lines.set(this.cursorLine, line.substring(0, col) + text + line.substring(col));
        this.cursorCol = col + text.length();
        this.dirty = true;
        this.ensureCursorVisible();
    }

    private void backspace() {
        if (this.cursorCol > 0) {
            String line = this.currentLine();
            int col = Math.min(this.cursorCol, line.length());
            this.lines.set(this.cursorLine, line.substring(0, col - 1) + line.substring(col));
            this.cursorCol = col - 1;
            this.dirty = true;
        } else if (this.cursorLine > 0) {
            String previous = this.lines.get(this.cursorLine - 1);
            String current = this.currentLine();
            this.lines.remove(this.cursorLine);
            this.cursorLine--;
            this.cursorCol = previous.length();
            this.lines.set(this.cursorLine, previous + current);
            this.dirty = true;
        }
        this.ensureCursorVisible();
    }

    private void deleteForward() {
        String line = this.currentLine();
        int col = Math.min(this.cursorCol, line.length());
        if (col < line.length()) {
            this.lines.set(this.cursorLine, line.substring(0, col) + line.substring(col + 1));
            this.dirty = true;
        } else if (this.cursorLine < this.lines.size() - 1) {
            this.lines.set(this.cursorLine, line + this.lines.get(this.cursorLine + 1));
            this.lines.remove(this.cursorLine + 1);
            this.dirty = true;
        }
        this.ensureCursorVisible();
    }

    private void splitLine() {
        String line = this.currentLine();
        int col = Math.min(this.cursorCol, line.length());
        this.lines.set(this.cursorLine, line.substring(0, col));
        this.lines.add(this.cursorLine + 1, line.substring(col));
        this.cursorLine++;
        this.cursorCol = 0;
        this.dirty = true;
        this.ensureCursorVisible();
    }

    private void paste(String clipboard) {
        if (clipboard == null || clipboard.isEmpty()) return;
        String line = this.currentLine();
        int col = Math.min(this.cursorCol, line.length());
        String before = line.substring(0, col);
        String after = line.substring(col);
        String[] parts = clipboard.split("\n", -1);

        if (parts.length == 1) {
            this.lines.set(this.cursorLine, before + parts[0] + after);
            this.cursorCol = col + parts[0].length();
        } else {
            this.lines.set(this.cursorLine, before + parts[0]);
            for (int i = 1; i < parts.length; i++) {
                this.lines.add(this.cursorLine + i, parts[i] + (i == parts.length - 1 ? after : ""));
            }
            this.cursorLine += parts.length - 1;
            this.cursorCol = parts[parts.length - 1].length();
        }

        this.dirty = true;
        this.ensureCursorVisible();
    }
}
