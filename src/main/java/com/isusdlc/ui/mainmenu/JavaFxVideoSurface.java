package com.isusdlc.ui.mainmenu;

import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.client.texture.TextureManager;
import net.minecraft.util.Identifier;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.function.BooleanSupplier;

/**
 * Экспериментальный встроенный плеер: рендерит JavaFX WebView в GL-текстуру.
 * JavaFX подхватывается через reflection — если его нет в classpath, доступен
 * только фолбэк (открытие в системном браузере).
 */
public final class JavaFxVideoSurface {
    private static boolean checked = false;
    private static boolean available = false;

    private static JavaFxVideoSurface instance;

    private Object jfxPanel;
    private Object webView;
    private Object webEngine;
    private Object snapshotImage;
    private int surfaceWidth = 640;
    private int surfaceHeight = 360;
    private int[] sharedPixels;
    private volatile boolean frameReady;
    private boolean fxLoopRunning;

    private NativeImage nativeImage;
    private NativeImageBackedTexture texture;
    private Identifier textureId;

    private JavaFxVideoSurface() {
    }

    public static synchronized boolean isAvailable() {
        if (!checked) {
            checked = true;
            try {
                Class.forName("javafx.embed.swing.JFXPanel");
                Class.forName("javafx.scene.web.WebView");
                available = true;
                System.out.println("[YouTube] JavaFX найден — встроенный плеер доступен");
            } catch (Throwable throwable) {
                available = false;
                System.out.println("[YouTube] JavaFX НЕ найден: " + throwable.getClass().getSimpleName()
                    + ": " + throwable.getMessage()
                    + " | Java: " + System.getProperty("java.version")
                    + " (" + System.getProperty("java.vendor") + ")");
            }
        }
        return available;
    }

    public static synchronized JavaFxVideoSurface getInstance() {
        if (instance == null) {
            instance = new JavaFxVideoSurface();
        }
        return instance;
    }

    public Identifier getTextureId() {
        return this.textureId;
    }

    public boolean hasFrame() {
        return this.frameReady && this.textureId != null;
    }

    /** Загружает страницу плеера YouTube для видео. Безопасно вызывать с любого потока. */
    public void load(String videoId) {
        if (!isAvailable()) return;
        this.ensureStarted();
        this.runOnFx(() -> {
            try {
                Method engineMethod = this.webView.getClass().getMethod("getEngine");
                if (this.webEngine == null) {
                    this.webEngine = engineMethod.invoke(this.webView);
                }

                Method loadMethod = this.webEngine.getClass().getMethod("load", String.class);
                String url = "https://www.youtube.com/embed/" + videoId + "?autoplay=1&playsinline=1";
                loadMethod.invoke(this.webEngine, url);
            } catch (Throwable throwable) {
                System.err.println("[YouTube] Не удалось загрузить видео: " + throwable);
            }
        });
    }

    /** Переключает воспроизведение перезагрузкой embed-плеера. */
    public void setPlaying(String videoId, boolean playing) {
        if (!isAvailable()) return;
        this.runOnFx(() -> {
            try {
                Method loadMethod = this.webEngine.getClass().getMethod("load", String.class);
                loadMethod.invoke(this.webEngine, "https://www.youtube.com/embed/" + videoId
                    + (playing ? "?autoplay=1&playsinline=1" : "?playsinline=1"));
            } catch (Throwable throwable) {
                System.err.println("[YouTube] Не удалось переключить воспроизведение: " + throwable);
            }
        });
    }

    /** Вызывается каждый кадр рендера: заливает последний кадр FX в GL-текстуру. */
    public void updateTexture(TextureManager textureManager) {
        if (!isAvailable() || !this.frameReady) return;
        synchronized (this) {
            try {
                if (this.nativeImage == null || this.texture == null) {
                    this.nativeImage = new NativeImage(NativeImage.Format.RGBA, this.surfaceWidth, this.surfaceHeight, false);
                    this.texture = new NativeImageBackedTexture(this.nativeImage);
                    this.textureId = Identifier.of("isusdlc", "youtube_surface");
                    textureManager.registerTexture(this.textureId, this.texture);
                }

                for (int y = 0; y < this.surfaceHeight; y++) {
                    for (int x = 0; x < this.surfaceWidth; x++) {
                        int argb = this.sharedPixels[y * this.surfaceWidth + x];
                        // ARGB (JavaFX) -> ABGR (память NativeImage RGBA)
                        int a = argb >>> 24 & 0xFF;
                        int r = argb >>> 16 & 0xFF;
                        int g = argb >>> 8 & 0xFF;
                        int b = argb & 0xFF;
                        this.nativeImage.setColor(x, y, a << 24 | b << 16 | g << 8 | r);
                    }
                }

                this.texture.upload();
                this.frameReady = false;
            } catch (Throwable throwable) {
                this.frameReady = false;
                System.err.println("[YouTube] Ошибка обновления кадра: " + throwable);
            }
        }
    }

    private void ensureStarted() {
        if (this.jfxPanel != null) {
            if (!this.fxLoopRunning) {
                this.startFxLoop();
            }
            return;
        }

        // Swing/AWT-объекты создаются строго на EDT (контракт JFXPanel)
        javax.swing.SwingUtilities.invokeLater(() -> {
            try {
                if (this.jfxPanel != null) {
                    return;
                }

                // JFXPanel + скрытый JFrame: FX-тулкит должен быть привязан к Swing
                Class<?> jfxPanelClass = Class.forName("javafx.embed.swing.JFXPanel");
                Class<?> frameClass = Class.forName("javax.swing.JFrame");
                Constructor<?> frameCtor = frameClass.getConstructor();
                Object frame = frameCtor.newInstance();
                frameClass.getMethod("setUndecorated", boolean.class).invoke(frame, true);
                frameClass.getMethod("setBounds", int.class, int.class, int.class, int.class)
                    .invoke(frame, -16000, -16000, this.surfaceWidth, this.surfaceHeight);
                Object panel = jfxPanelClass.getConstructor().newInstance();
                frameClass.getMethod("add", java.awt.Component.class).invoke(frame, panel);
                frameClass.getMethod("setVisible", boolean.class).invoke(frame, true);
                this.jfxPanel = panel;

                Class<?> platformClass = Class.forName("javafx.application.Platform");
                Method runLater = platformClass.getMethod("runLater", Runnable.class);

                runLater.invoke(null, (Runnable) () -> {
                    try {
                        Class<?> webViewClass = Class.forName("javafx.scene.web.WebView");
                        Constructor<?> webViewCtor = webViewClass.getConstructor(double.class, double.class);
                        Object view = webViewCtor.newInstance((double) this.surfaceWidth, (double) this.surfaceHeight);
                        this.webView = view;

                        Class<?> sceneClass = Class.forName("javafx.scene.Scene");
                        Class<?> groupClass = Class.forName("javafx.scene.Group");
                        Object group = groupClass.getConstructor().newInstance();
                        javafxAdd(group, view);
                        Object scene = sceneClass.getConstructor(groupClass).newInstance(group);
                        jfxPanelClass.getMethod("setScene", sceneClass).invoke(this.jfxPanel, scene);

                        this.sharedPixels = new int[this.surfaceWidth * this.surfaceHeight];
                        Class<?> writableImageClass = Class.forName("javafx.scene.image.WritableImage");
                        this.snapshotImage = writableImageClass
                            .getConstructor(int.class, int.class).newInstance(this.surfaceWidth, this.surfaceHeight);
                        this.startFxLoop();
                    } catch (Throwable throwable) {
                        System.err.println("[YouTube] Не удалось инициализировать WebView: " + throwable);
                    }
                });
            } catch (Throwable throwable) {
                System.err.println("[YouTube] Не удалось запустить JavaFX: " + throwable);
            }
        });
    }

    private static void javafxAdd(Object group, Object node) throws Exception {
        Class<?> groupClass = Class.forName("javafx.scene.Group");
        Class<?> childrenClass = Class.forName("javafx.collections.ObservableList");
        Object children = groupClass.getMethod("getChildren").invoke(group);
        childrenClass.getMethod("add", Object.class).invoke(children, node);
    }

    /** FX-цикл: делает снапшот WebView в общий буфер примерно 30 раз в секунду. */
    private void startFxLoop() {
        if (this.fxLoopRunning) return;
        this.fxLoopRunning = true;
        try {
            Class<?> platformClass = Class.forName("javafx.application.Platform");
            Method runLater = platformClass.getMethod("runLater", Runnable.class);
            BooleanSupplier stillRunning = () -> this.fxLoopRunning;

            Runnable[] loop = new Runnable[1];
            loop[0] = (Runnable) () -> {
                try {
                    this.snapshotFrame();
                } catch (Throwable ignored) {
                }
                if (stillRunning.getAsBoolean()) {
                    try {
                        runLater.invoke(null, loop[0]);
                    } catch (Throwable ignored) {
                        this.fxLoopRunning = false;
                    }
                }
            };
            runLater.invoke(null, loop[0]);
        } catch (Throwable throwable) {
            this.fxLoopRunning = false;
            System.err.println("[YouTube] Не удалось запустить FX-цикл: " + throwable);
        }
    }

    private void snapshotFrame() throws Exception {
        if (this.webView == null || this.snapshotImage == null) return;
        Class<?> nodeClass = Class.forName("javafx.scene.Node");
        Class<?> snapshotParamsClass = Class.forName("javafx.scene.SnapshotParameters");
        Method snapshot = nodeClass.getMethod("snapshot", snapshotParamsClass, Class.forName("javafx.scene.image.WritableImage"));
        Object result = snapshot.invoke(this.webView, (Object) null, this.snapshotImage);

        Class<?> imageClass = Class.forName("javafx.scene.image.Image");
        Method readerMethod = imageClass.getMethod("getPixelReader");
        Object reader = readerMethod.invoke(result);
        Class<?> readerClass = Class.forName("javafx.scene.image.PixelReader");
        Class<?> pixelFormatClass = Class.forName("javafx.scene.image.PixelFormat");
        Method intInstance = pixelFormatClass.getMethod("getIntInstance");
        Object format = intInstance.invoke(null);
        Class<?> writableImageClass = Class.forName("javafx.scene.image.WritableImage");
        int width = (Integer) imageClass.getMethod("getWidth").invoke(result);
        int height = (Integer) imageClass.getMethod("getHeight").invoke(result);
        if (width != this.surfaceWidth || height != this.surfaceHeight || this.sharedPixels == null) return;

        Method getPixels = readerClass.getMethod("getPixels", int.class, int.class, int.class, int.class,
            pixelFormatClass, int[].class, int.class, int.class);
        int[] buffer = new int[this.surfaceWidth * this.surfaceHeight];
        getPixels.invoke(reader, 0, 0, this.surfaceWidth, this.surfaceHeight, format, buffer, 0, this.surfaceWidth);
        this.sharedPixels = buffer;
        this.frameReady = true;
    }

    private void runOnFx(Runnable runnable) {
        try {
            Class<?> platformClass = Class.forName("javafx.application.Platform");
            platformClass.getMethod("runLater", Runnable.class).invoke(null, runnable);
        } catch (Throwable throwable) {
            System.err.println("[YouTube] runLater недоступен: " + throwable.getMessage());
        }
    }

}
