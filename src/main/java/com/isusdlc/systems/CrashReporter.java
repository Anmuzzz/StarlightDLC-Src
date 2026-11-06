package com.isusdlc.systems;

import com.isusdlc.elegant;
import net.fabricmc.loader.api.FabricLoader;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * Отправка краш-репортов в Discord webhook.
 * Включается заполнением DISCORD_WEBHOOK; пока пусто — репорты не отправляются.
 */
public final class CrashReporter {
    /** Вставь сюда Discord webhook канала для репортов. Пусто = выключено. */
    private static final String DISCORD_WEBHOOK = "";

    private static volatile long lastReportMs;

    private CrashReporter() {
    }

    public static void install() {
        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
            elegant.LOGGER.error("Uncaught exception on thread {}", thread.getName(), throwable);
            report("Uncaught exception on thread **" + thread.getName() + "**\n```\n" + throwable + "\n```");
        });
    }

    /** Вызывается из MinecraftClientMixin при показе краш-экрана. */
    public static void report(String crashReport) {
        String webhook = DISCORD_WEBHOOK;
        if (webhook == null || webhook.isBlank()) return;

        long now = System.currentTimeMillis();
        if (now - lastReportMs < 10_000L) return;  // анти-спам: не чаще раза в 10с
        lastReportMs = now;

        String content = buildContent(crashReport);
        Thread thread = new Thread(() -> send(webhook, content), "Starlight-CrashReporter");
        thread.setDaemon(true);
        thread.start();
    }

    private static String buildContent(String crashReport) {
        StringBuilder sb = new StringBuilder();
        sb.append("**Краш-репорт** — isusdlc ").append(currentVersion());
        sb.append(" | MC ").append(mcVersion());
        sb.append(" | ").append(System.getProperty("os.name"));
        sb.append(" | Java ").append(System.getProperty("java.version"));
        sb.append("\n```\n");
        String report = crashReport == null ? "" : crashReport;
        if (report.length() > 1700) {
            report = report.substring(0, 1700) + "\n... (обрезано)";
        }
        sb.append(report);
        sb.append("\n```");
        return sb.toString();
    }

    private static void send(String webhook, String content) {
        try {
            HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
            HttpRequest request = HttpRequest.newBuilder(URI.create(webhook))
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(15))
                .POST(HttpRequest.BodyPublishers.ofString(
                    "{\"content\": " + jsonEscape(content) + "}", StandardCharsets.UTF_8))
                .build();
            client.send(request, HttpResponse.BodyHandlers.discarding());
        } catch (Exception ignored) {
            // мы уже падаем — тише воды
        }
    }

    private static String jsonEscape(String value) {
        StringBuilder sb = new StringBuilder("\"");
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> {
                    if (c < 0x20) sb.append(String.format("\\u%04x", (int) c));
                    else sb.append(c);
                }
            }
        }
        return sb.append('"').toString();
    }

    private static String currentVersion() {
        String version = FabricLoader.getInstance()
            .getModContainer(elegant.MOD_ID)
            .map(c -> c.getMetadata().getVersion().getFriendlyString())
            .orElse("?");
        return version.replaceFirst("-no-youtube$", "");
    }

    private static String mcVersion() {
        try {
            return FabricLoader.getInstance()
                .getModContainer("minecraft")
                .map(c -> c.getMetadata().getVersion().getFriendlyString())
                .orElse("?");
        } catch (Exception e) {
            return "?";
        }
    }
}
