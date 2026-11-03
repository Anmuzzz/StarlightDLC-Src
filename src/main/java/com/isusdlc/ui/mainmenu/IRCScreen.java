package com.isusdlc.ui.mainmenu;

import com.isusdlc.utility.colors.ColorRGBA;
import com.isusdlc.utility.interfaces.IMinecraft;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;

import java.util.concurrent.CopyOnWriteArrayList;

public class IRCScreen extends Screen implements IMinecraft {

    private final CopyOnWriteArrayList<Text> messages = new CopyOnWriteArrayList<>();
    private TextFieldWidget inputField;
    private final IRCHandler handler;
    private String status = "§7Подключение...";
    private boolean connected = false;
    private int scrollOffset;
    private String currentInput = "";

    public IRCScreen(String host, int port, String nickname) {
        super(Text.of("IRC Chat"));
        this.handler = new IRCHandler(host, port, nickname, new IRCHandler.IRCListener() {
            @Override
            public void onMessage(String line) {
                addServerMessage(line);
            }

            @Override
            public void onDisconnect(String reason) {
                connected = false;
                status = "§c" + reason;
                addMessage(Text.of("§c[!] " + reason));
            }

            @Override
            public void onConnect() {
                connected = true;
                status = "§aПодключено";
                addMessage(Text.of("§a[!] Подключено к чату"));
            }
        });
        this.handler.connect();
    }

    @Override
    protected void init() {
        super.init();
        int fieldWidth = width - 20;
        int fieldX = 10;
        int fieldY = height - 30;
        inputField = new TextFieldWidget(textRenderer, fieldX, fieldY, fieldWidth, 20, Text.of(""));
        inputField.setMaxLength(200);
        inputField.setEditable(true);
        inputField.setVisible(true);
        inputField.setFocused(true);
        setInitialFocus(inputField);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context, mouseX, mouseY, delta);

        int textColor = 0xFFFFFFFF;
        context.fill(5, 5, width - 5, height - 40, 0x80000000);

        int y = height - 45;
        int lineHeight = textRenderer.fontHeight + 2;
        int maxLines = (height - 90) / lineHeight;

        int startIndex = Math.max(0, messages.size() - maxLines - scrollOffset);
        for (int i = messages.size() - 1; i >= startIndex; i--) {
            context.drawText(textRenderer, messages.get(i), 10, y, textColor, false);
            y -= lineHeight;
        }

        inputField.render(context, mouseX, mouseY, delta);

        context.drawText(textRenderer, Text.of(status), 10, 5, 0xFFFFFFFF, false);
        context.drawText(
            textRenderer,
            Text.of("F8 - очистить | Esc - выход"),
            width / 2 - 80, 5, 0xFF888888, false
        );

        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (inputField.keyPressed(keyCode, scanCode, modifiers)) return true;

        if (keyCode == 257 || keyCode == 335) {
            sendCurrentMessage();
            return true;
        }

        if (keyCode == 258) {
            inputField.setFocused(!inputField.isFocused());
            return true;
        }

        if (keyCode == 266) {
            scrollOffset = Math.min(scrollOffset + 1, Math.max(0, messages.size() - (height - 90) / (textRenderer.fontHeight + 2)));
            return true;
        }

        if (keyCode == 267) {
            scrollOffset = Math.max(0, scrollOffset - 1);
            return true;
        }

        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        inputField.mouseClicked(mouseX, mouseY, button);
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (inputField.charTyped(chr, modifiers)) return true;
        return super.charTyped(chr, modifiers);
    }

    private void sendCurrentMessage() {
        String text = inputField.getText().trim();
        if (!text.isEmpty()) {
            addMessage(Text.of("§7Я: §f" + text));
            handler.sendMessage(text);
            inputField.setText("");
        }
    }

    private void addServerMessage(String line) {
        if (line.startsWith("SYSTEM|")) {
            String sysMsg = line.substring(7);
            addMessage(Text.of("§e[!] §7" + sysMsg));
        } else if (line.startsWith("CHAT|")) {
            String[] parts = line.split("\\|", 3);
            if (parts.length >= 3) {
                addMessage(Text.of("§b" + parts[1] + "§8: §f" + parts[2]));
            }
        }
    }

    private void addMessage(Text text) {
        messages.add(text);
        if (messages.size() > 500) {
            messages.remove(0);
        }
    }

    public void disconnect() {
        handler.disconnect();
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public void close() {
        handler.disconnect();
        super.close();
    }
}
