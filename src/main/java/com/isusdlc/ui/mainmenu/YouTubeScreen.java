package com.isusdlc.ui.mainmenu;

import com.google.gson.JsonParser;
import com.isusdlc.elegant;
import com.isusdlc.framework.base.CustomScreen;
import com.isusdlc.framework.base.UIContext;
import com.isusdlc.framework.msdf.Font;
import com.isusdlc.framework.msdf.Fonts;
import com.isusdlc.framework.objects.BorderRadius;
import com.isusdlc.framework.objects.MouseButton;
import com.isusdlc.utility.colors.ColorRGBA;
import com.isusdlc.utility.colors.Colors;
import com.isusdlc.utility.interfaces.IMinecraft;
import com.isusdlc.systems.modules.modules.other.YouTubeModule;
import com.isusdlc.utility.render.ScissorUtility;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import net.minecraft.util.math.MathHelper;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Поиск и просмотр YouTube внутри клиента. Плеер — YouTubePlayerScreen
 * (встроенный через JavaFX, при его отсутствии — системный браузер).
 */
public class YouTubeScreen extends CustomScreen implements IMinecraft {
    private static final Pattern VIDEO_ID = Pattern.compile("\"videoId\":\"([A-Za-z0-9_-]{11})\"");
    private static final Pattern TITLE_TEXT = Pattern.compile("\"text\":\"((?:[^\"\\\\]|\\\\.)*)\"");
    private static final HttpClient HTTP = HttpClient.newBuilder()
        .followRedirects(HttpClient.Redirect.NORMAL)
        .connectTimeout(Duration.ofSeconds(10))
        .build();
    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0 Safari/537.36";

    private final record VideoResult(String id, String title, String channel) {
    }

    private final List<VideoResult> results = new ArrayList<>();
    private final Map<String, Identifier> thumbnails = new HashMap<>();
    private final Map<String, Boolean> thumbnailLoading = new HashMap<>();

    private TextFieldWidget searchField;
    private volatile boolean searching;
    private String status = "";
    private long statusTime;
    private boolean statusError;

    private int scroll;
    private float cardWidth;
    private float cardHeight;
    private float gridY;
    private Font uiFont;
    private Font smallFont;

    @Override
    protected void init() {
        if (this.searchField == null) {
            this.searchField = new TextFieldWidget(this.textRenderer, 0, 0, 0, 0, Text.of(""));
            this.searchField.setMaxLength(200);
            this.searchField.setPlaceholder(Text.of("Что посмотреть? (Enter — искать)"));
            this.addSelectableChild(this.searchField);
            this.setInitialFocus(this.searchField);
            this.status = "Поиск видео или вставь ссылку";
            this.statusTime = System.currentTimeMillis();
        }
        this.searchField.setWidth(this.width - 20);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    private void search(String query) {
        if (query == null || query.isBlank() || this.searching) return;

        String videoId = this.extractVideoId(query);
        if (videoId != null) {
            this.play(videoId, query);
            return;
        }

        this.searching = true;
        this.status = "Поиск: " + query;
        this.statusTime = System.currentTimeMillis();
        this.destroyThumbnails();
        this.results.clear();
        this.scroll = 0;

        String encoded = URLEncoder.encode(query, StandardCharsets.UTF_8);
        HttpRequest request = HttpRequest.newBuilder(URI.create("https://www.youtube.com/results?search_query=" + encoded + "&hl=en&sp=EgIQAQ%3D%3D"))
            .header("User-Agent", USER_AGENT)
            .header("Accept-Language", "en-US,en;q=0.9")
            .timeout(Duration.ofSeconds(12))
            .GET()
            .build();

        HTTP.sendAsync(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8))
            .thenAccept(response -> {
                List<VideoResult> parsed = this.parseResults(response.body());
                mc.execute(() -> {
                    this.results.addAll(parsed);
                    this.searching = false;
                    this.status = parsed.isEmpty() ? "Ничего не найдено" : "Найдено: " + parsed.size();
                    this.statusError = parsed.isEmpty();
                    this.statusTime = System.currentTimeMillis();
                    for (VideoResult result : parsed) {
                        this.requestThumbnail(result.id());
                    }
                });
            })
            .exceptionally(throwable -> {
                mc.execute(() -> {
                    this.searching = false;
                    this.status = "Ошибка сети: " + throwable.getMessage();
                    this.statusError = true;
                    this.statusTime = System.currentTimeMillis();
                });
                return null;
            });
    }

    private String extractVideoId(String input) {
        Matcher matcher = VIDEO_ID.matcher(input);
        if (input.contains("youtube.com") || input.contains("youtu.be")) {
            if (matcher.find()) {
                return matcher.group(1);
            }

            int beIndex = input.indexOf("youtu.be/");
            if (beIndex != -1 && input.length() >= beIndex + 20) {
                return input.substring(beIndex + 9, beIndex + 20);
            }
        }
        return null;
    }

    private List<VideoResult> parseResults(String html) {
        List<VideoResult> parsed = new ArrayList<>();
        for (String chunk : html.split("\"videoRenderer\":")) {
            if (parsed.size() >= 12) break;
            Matcher idMatcher = VIDEO_ID.matcher(chunk);
            if (!idMatcher.find() || idMatcher.start() > 200) continue;
            String id = idMatcher.group(1);

            String title = this.jsonTextAfter(chunk, "\"title\":");
            if (title.isEmpty()) continue;
            String channel = this.jsonTextAfter(chunk, "\"ownerText\":");
            if (channel.isEmpty()) {
                channel = this.jsonTextAfter(chunk, "\"longBylineText\":");
            }

            parsed.add(new VideoResult(id, title, channel));
        }
        return parsed;
    }

    private String jsonTextAfter(String chunk, String key) {
        int keyIndex = chunk.indexOf(key);
        if (keyIndex == -1) return "";
        Matcher matcher = TITLE_TEXT.matcher(chunk.substring(keyIndex, Math.min(chunk.length(), keyIndex + 2000)));
        if (!matcher.find()) return "";
        try {
            String unescaped = JsonParser.parseString("\"" + matcher.group(1) + "\"").getAsString();
            return unescaped.length() > 90 ? unescaped.substring(0, 90) + "…" : unescaped;
        } catch (Exception ignored) {
            return "";
        }
    }

    private void requestThumbnail(String videoId) {
        if (this.thumbnailLoading.containsKey(videoId)) return;
        this.thumbnailLoading.put(videoId, true);

        HttpRequest request = HttpRequest.newBuilder(URI.create("https://i.ytimg.com/vi/" + videoId + "/mqdefault.jpg"))
            .header("User-Agent", USER_AGENT)
            .timeout(Duration.ofSeconds(10))
            .GET()
            .build();

        HTTP.sendAsync(request, HttpResponse.BodyHandlers.ofByteArray())
            .thenAccept(response -> mc.execute(() -> {
                if (mc.currentScreen != this) return;  // экран закрыт — не регистрируем текстуру в никуда
                if (this.thumbnails.containsKey(videoId)) return;
                try {
                    NativeImage image = NativeImage.read(response.body());
                    NativeImageBackedTexture texture = new NativeImageBackedTexture(image);
                    Identifier id = Identifier.of("isusdlc", "yt_thumb_" + videoId);
                    mc.getTextureManager().registerTexture(id, texture);
                    this.thumbnails.put(videoId, id);
                } catch (Exception ignored) {
                }
            }))
            .exceptionally(throwable -> null);
    }

    private void play(String videoId, String title) {
        YouTubeModule module = elegant.getInstance().getModuleManager().getModuleSafe(YouTubeModule.class);
        if (module != null) {
            module.startVideo(videoId, title);
            mc.setScreen(null);
        } else {
            Util.getOperatingSystem().open("https://www.youtube.com/watch?v=" + videoId);
            this.status = "Модуль YouTube выключен — открыл в браузере";
            this.statusError = true;
            this.statusTime = System.currentTimeMillis();
        }
    }

    @Override
    public void render(UIContext context) {
        if (this.uiFont == null) {
            this.uiFont = Fonts.MEDIUM.getFont(9.0F);
            this.smallFont = Fonts.REGULAR.getFont(6.0F);
        }

        context.drawRect(0.0F, 0.0F, this.width, this.height, ColorRGBA.BLACK.withAlpha(230.0F));
        context.drawText(this.uiFont, "YouTube", 10.0F, 8.0F, Colors.getTextColor());

        if (!this.status.isEmpty() && System.currentTimeMillis() - this.statusTime < 6000L) {
            float statusWidth = this.uiFont.width(this.status);
            context.drawText(this.smallFont, this.status, this.width - 10.0F - statusWidth, 12.0F,
                this.statusError ? ColorRGBA.RED.withAlpha(220.0F) : Colors.getTextColor().withAlpha(160.0F));
        }

        // поле поиска
        this.searchField.setX(10);
        this.searchField.setY(24);
        this.searchField.setWidth(this.width - 20);
        context.drawRoundedRect(9.0F, 23.0F, this.width - 18.0F, 18.0F, BorderRadius.all(6.0F), Colors.getBackgroundColor().withAlpha(220.0F));
        this.searchField.render(context, (int) context.getMouseX(), (int) context.getMouseY(), context.getDelta());

        this.renderGrid(context);
    }

    private void renderGrid(UIContext context) {
        this.cardWidth = (this.width - 30.0F) / 2.0F;
        this.cardHeight = this.cardWidth * 9.0F / 16.0F + 24.0F;
        this.gridY = 50.0F;
        int columns = 2;

        ScissorUtility.push(0.0F, this.gridY, this.width, this.height - this.gridY);
        for (int i = 0; i < this.results.size(); i++) {
            VideoResult result = this.results.get(i);
            int row = i / columns;
            int column = i % columns;
            float cardX = 10.0F + column * (this.cardWidth + 10.0F);
            float cardY = this.gridY + row * (this.cardHeight + 12.0F) - this.scroll;
            if (cardY > this.height) break;
            if (cardY + this.cardHeight < this.gridY) continue;

            boolean hovered = context.getMouseX() >= cardX && context.getMouseX() <= cardX + this.cardWidth
                && context.getMouseY() >= cardY && context.getMouseY() <= cardY + this.cardHeight;

            Identifier thumb = this.thumbnails.get(result.id());
            context.drawRoundedRect(cardX, cardY, this.cardWidth, this.cardHeight, BorderRadius.all(8.0F),
                Colors.getBackgroundColor().mix(ColorRGBA.WHITE, hovered ? 0.1F : 0.0F).withAlpha(220.0F));
            if (thumb != null) {
                context.drawTexture(thumb, cardX + 1.0F, cardY + 1.0F, this.cardWidth - 2.0F, this.cardWidth * 9.0F / 16.0F - 2.0F);
            } else {
                context.drawRoundedRect(cardX + 1.0F, cardY + 1.0F, this.cardWidth - 2.0F, this.cardWidth * 9.0F / 16.0F - 2.0F,
                    BorderRadius.all(4.0F), ColorRGBA.BLACK.withAlpha(120.0F));
                context.drawCenteredText(this.smallFont, this.searching && this.results.size() - 1 == i ? "…" : "▶",
                    cardX + this.cardWidth / 2.0F, cardY + this.cardWidth * 9.0F / 32.0F - 4.0F, Colors.getTextColor().withAlpha(120.0F));
            }

            ScissorUtility.push(cardX + 4.0F, cardY + this.cardWidth * 9.0F / 16.0F + 2.0F, this.cardWidth - 8.0F, this.cardHeight - this.cardWidth * 9.0F / 16.0F - 4.0F);
            context.drawText(this.smallFont, result.title(), cardX + 5.0F, cardY + this.cardWidth * 9.0F / 16.0F + 4.0F, Colors.getTextColor());
            context.drawText(this.smallFont, result.channel(), cardX + 5.0F, cardY + this.cardWidth * 9.0F / 16.0F + 4.0F + this.smallFont.height() + 1.0F,
                Colors.getTextColor().withAlpha(140.0F));
            ScissorUtility.pop();
        }
        ScissorUtility.pop();

        if (this.results.size() > 2) {
            int rows = (this.results.size() + 1) / 2;
            float contentHeight = rows * (this.cardHeight + 12.0F);
            float visible = this.height - this.gridY;
            if (contentHeight > visible) {
                float barHeight = Math.max(20.0F, visible * visible / contentHeight);
                float barY = this.gridY + (visible - barHeight) * MathHelper.clamp((float) this.scroll / (contentHeight - visible), 0.0F, 1.0F);
                context.drawRoundedRect(this.width - 4.0F, barY, 2.5F, barHeight, BorderRadius.all(2.0F), Colors.getTextColor().withAlpha(90.0F));
            }
        }
    }

    @Override
    public void onMouseClicked(double mouseX, double mouseY, MouseButton button) {
        if (button == MouseButton.LEFT) {
            for (int i = 0; i < this.results.size(); i++) {
                VideoResult result = this.results.get(i);
                int row = i / 2;
                int column = i % 2;
                float cardX = 10.0F + column * (this.cardWidth + 10.0F);
                float cardY = this.gridY + row * (this.cardHeight + 12.0F) - this.scroll;
                if (mouseX >= cardX && mouseX <= cardX + this.cardWidth && mouseY >= cardY && mouseY <= cardY + this.cardHeight) {
                    this.play(result.id(), result.title());
                    return;
                }
            }
        }

        super.onMouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (!this.results.isEmpty()) {
            int rows = (this.results.size() + 1) / 2;
            float contentHeight = rows * (this.cardHeight + 12.0F);
            float visible = this.height - this.gridY;
            int maxScroll = Math.max(0, (int) (contentHeight - visible));
            this.scroll = MathHelper.clamp(this.scroll - (int) (verticalAmount * 40.0), 0, maxScroll);
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public void onKeyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 257 || keyCode == 335) {
            this.search(this.searchField.getText().trim());
        }
    }

    @Override
    public void close() {
        this.destroyThumbnails();
        mc.setScreen(null);
    }

    private void destroyThumbnails() {
        for (Identifier id : this.thumbnails.values()) {
            try {
                mc.getTextureManager().destroyTexture(id);
            } catch (Exception ignored) {
            }
        }

        this.thumbnails.clear();
        this.thumbnailLoading.clear();
    }
}
