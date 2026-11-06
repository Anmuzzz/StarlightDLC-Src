package com.isusdlc.systems;

import com.isusdlc.elegant;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Проверка обновлений: раз за сессию смотрит последний релиз на GitHub
 * и уведомляет, если версия отличается от текущей.
 */
public final class UpdateChecker {
    private static final String REPO = "Anmuzzz/StarlightDLC-Src";
    private static final HttpClient HTTP = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(10))
        .build();
    private volatile boolean checked;

    public void startCheck() {
        if (this.checked) return;
        this.checked = true;

        Thread thread = new Thread(this::check, "Starlight-UpdateChecker");
        thread.setDaemon(true);
        thread.start();
    }

    private void check() {
        try {
            Thread.sleep(20_000L);  // даём игре спокойно запуститься
        } catch (InterruptedException ignored) {
            return;
        }

        try {
            HttpRequest request = HttpRequest.newBuilder(
                    URI.create("https://api.github.com/repos/" + REPO + "/releases?per_page=3"))
                .header("Accept", "application/vnd.github+json")
                .timeout(Duration.ofSeconds(12))
                .GET()
                .build();

            HttpResponse<String> response = HTTP.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) return;

            // лёгкий разбор без зависимостей: первый tag_name в списке релизов (новейший)
            String body = response.body();
            int tagIdx = body.indexOf("\"tag_name\"");
            if (tagIdx == -1) return;
            int quoteOpen = body.indexOf('"', tagIdx + "\"tag_name\"".length() + 1);
            int quoteClose = body.indexOf('"', quoteOpen + 1);
            if (quoteOpen == -1 || quoteClose == -1) return;
            String latestTag = body.substring(quoteOpen + 1, quoteClose).replaceFirst("^v", "");

            String current = currentVersion();
            if (latestTag.equalsIgnoreCase(current)) return;

            elegant.LOGGER.info("[UpdateChecker] Доступна версия {}: у тебя {}", latestTag, current);
            MinecraftClient.getInstance().execute(() -> {
                try {
                    elegant.getInstance()
                        .getNotificationManager()
                        .addNotificationOther(
                            com.isusdlc.systems.notifications.NotificationType.INFO,
                            "Доступно обновление",
                            "Версия " + latestTag + " (у тебя " + current + ") — скачай на сайте"
                        );
                } catch (Exception ignored) {
                }
            });
        } catch (Exception e) {
            elegant.LOGGER.warn("[UpdateChecker] Не удалось проверить обновления: {}", e.getMessage());
        }
    }

    private static String currentVersion() {
        String version = FabricLoader.getInstance()
            .getModContainer(elegant.MOD_ID)
            .map(c -> c.getMetadata().getVersion().getFriendlyString())
            .orElse("");
        return version.replaceFirst("-no-youtube$", "");
    }

    private UpdateChecker() {
    }

    public static UpdateChecker getInstance() {
        return Holder.INSTANCE;
    }

    private static final class Holder {
        private static final UpdateChecker INSTANCE = new UpdateChecker();
    }
}
