package com.isusdlc.systems.lua;

import com.isusdlc.elegant;
import com.isusdlc.systems.event.EventListener;
import com.isusdlc.systems.event.EventManager;
import com.isusdlc.systems.event.impl.game.GameTickEvent;
import com.isusdlc.systems.event.impl.player.ClientPlayerTickEvent;
import com.isusdlc.systems.event.impl.render.HudRenderEvent;
import com.isusdlc.systems.event.impl.render.Render3DEvent;
import com.isusdlc.systems.event.impl.window.KeyPressEvent;
import com.isusdlc.systems.modules.ModuleManager;
import org.luaj.vm2.Globals;
import org.luaj.vm2.LuaError;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.OneArgFunction;
import org.luaj.vm2.lib.TwoArgFunction;
import org.luaj.vm2.lib.VarArgFunction;
import org.luaj.vm2.lib.jse.JsePlatform;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class LuaScriptEngine {
    private final Map<String, LuaScript> loadedScripts = new ConcurrentHashMap<>();
    private final EventManager eventManager;

    private EventListener<ClientPlayerTickEvent> tickListener;
    private EventListener<GameTickEvent> gameTickListener;
    private EventListener<HudRenderEvent> renderListener;
    private EventListener<Render3DEvent> render3DListener;
    private EventListener<KeyPressEvent> keyPressListener;

    public LuaScriptEngine() {
        this.eventManager = elegant.getInstance().getEventManager();
        registerEventListeners();
    }

    private void registerEventListeners() {
        tickListener = event -> fireEvent("tick", LuaValue.NIL);
        gameTickListener = event -> fireEvent("gametick", LuaValue.NIL);
        renderListener = event -> {
            LuaTable args = new LuaTable();
            args.set("tickDelta", LuaValue.valueOf(event.getTickDelta()));
            fireEvent("render", args);
        };
        render3DListener = event -> {
            LuaTable args = new LuaTable();
            args.set("tickDelta", LuaValue.valueOf(event.getTickDelta()));
            fireEvent("render3d", args);
        };
        keyPressListener = event -> {
            LuaTable args = new LuaTable();
            args.set("key", LuaValue.valueOf(event.getKey()));
            args.set("action", LuaValue.valueOf(event.getAction()));
            fireEvent("keypress", args);
        };

        eventManager.subscribe(this);
    }

    private void fireEvent(String eventName, LuaValue args) {
        for (LuaScript script : loadedScripts.values()) {
            if (!script.isRunning()) continue;
            List<LuaValue> callbacks = script.getCallbacks(eventName);
            if (callbacks == null || callbacks.isEmpty()) continue;
            for (LuaValue callback : callbacks) {
                try {
                    callback.call(args);
                } catch (LuaError e) {
                    elegant.LOGGER.error("Lua error in script '{}' on '{}': {}", script.getName(), eventName, e.getMessage());
                } catch (Exception e) {
                    elegant.LOGGER.error("Error in Lua event '{}' of script '{}': {}", eventName, script.getName(), e.getMessage());
                }
            }
        }
    }

    /**
     * Загружает и запускает скрипт. Кидает LuaError при ошибке синтаксиса/выполнения —
     * вызывающий код обязан обрабатывать её сам (см. loadScriptFromFile).
     */
    public void loadScript(String name, String code) {
        LuaScript script = new LuaScript(name);
        LuaValue chunk = script.getGlobals().load(code, name);
        script.setChunk(chunk);
        script.run();
        loadedScripts.put(name, script);
        elegant.LOGGER.info("Loaded Lua script: {}", name);
    }

    public void loadScriptFromFile(String name, File file) {
        try {
            String code = Files.readString(file.toPath());
            loadScript(name, code);
        } catch (IOException e) {
            elegant.LOGGER.error("Failed to read Lua script {}: {}", name, e.getMessage());
        } catch (LuaError e) {
            elegant.LOGGER.error("Lua error in script {}: {}", name, e.getMessage());
        } catch (Exception e) {
            elegant.LOGGER.error("Failed to load Lua script {}: {}", name, e.getMessage());
        }
    }

    public void unloadScript(String name) {
        LuaScript script = loadedScripts.remove(name);
        if (script != null) {
            script.unload();
            elegant.LOGGER.info("Unloaded Lua script: {}", name);
        }
    }

    public void reloadAllScripts() {
        for (String name : new java.util.ArrayList<>(loadedScripts.keySet())) {
            unloadScript(name);
        }
        loadScriptsFromDirectory();
    }

    public void loadScriptsFromDirectory() {
        File scriptsDir = getScriptsDirectory();
        File[] files = scriptsDir.listFiles((dir, name) -> name.endsWith(".lua"));
        if (files != null) {
            for (File file : files) {
                loadScriptFromFile(file.getName(), file);
            }
        }
    }

    public static File getScriptsDirectory() {
        File scriptsDir = new File(com.isusdlc.systems.file.FileManager.DIRECTORY, "scripts");
        if (!scriptsDir.exists()) {
            scriptsDir.mkdirs();
        }
        return scriptsDir;
    }

    public void shutdown() {
        eventManager.unsubscribe(this);
        for (String name : new java.util.ArrayList<>(loadedScripts.keySet())) {
            unloadScript(name);
        }
    }

    public Map<String, LuaScript> getLoadedScripts() {
        return loadedScripts;
    }

    public static class LuaScript {
        private final String name;
        private final Globals globals;
        private final Map<String, List<LuaValue>> eventCallbacks = new ConcurrentHashMap<>();
        private LuaValue chunk;
        private boolean running = false;

        public LuaScript(String name) {
            this.name = name;
            this.globals = JsePlatform.standardGlobals();
            applySandbox(this.globals);
            setupClientTable(this.globals, this);
        }

        /**
         * Отключает опасные функции: доступ к файловой системе, exec и загрузку
         * произвольного кода. os.time/os.clock/os.date и стандартная библиотека
         * (math, string, table) остаются доступны.
         */
        private static void applySandbox(Globals globals) {
            globals.set("dofile", LuaValue.NIL);
            globals.set("loadfile", LuaValue.NIL);
            globals.set("load", LuaValue.NIL);
            globals.set("loadstring", LuaValue.NIL);
            globals.set("require", LuaValue.NIL);

            LuaValue os = globals.get("os");
            if (os.istable()) {
                os.set("execute", LuaValue.NIL);
                os.set("exit", LuaValue.NIL);
                os.set("remove", LuaValue.NIL);
                os.set("rename", LuaValue.NIL);
                os.set("tmpname", LuaValue.NIL);
                os.set("getenv", LuaValue.NIL);
                os.set("setlocale", LuaValue.NIL);
            }

            LuaValue io = globals.get("io");
            if (io.istable()) {
                io.set("open", LuaValue.NIL);
                io.set("popen", LuaValue.NIL);
                io.set("lines", LuaValue.NIL);
                io.set("input", LuaValue.NIL);
                io.set("output", LuaValue.NIL);
                io.set("read", LuaValue.NIL);
                io.set("write", LuaValue.NIL);
            }
        }

        private static void setupClientTable(Globals globals, LuaScript owner) {
            LuaTable clientTable = new LuaTable();

            // client.print(text) - print to chat (client-side only)
            clientTable.set("print", new OneArgFunction() {
                @Override
                public LuaValue call(LuaValue arg) {
                    if (elegant.mc.player != null) {
                        elegant.mc.player.sendMessage(net.minecraft.text.Text.literal(arg.tojstring()), false);
                    }
                    return LuaValue.NIL;
                }
            });

            // client.log(text) - print to console
            clientTable.set("log", new OneArgFunction() {
                @Override
                public LuaValue call(LuaValue arg) {
                    elegant.LOGGER.info("[Lua] {}", arg.tojstring());
                    return LuaValue.NIL;
                }
            });

            // client.chat(text) - send chat message
            clientTable.set("chat", new OneArgFunction() {
                @Override
                public LuaValue call(LuaValue arg) {
                    if (elegant.mc.player != null) {
                        elegant.mc.player.networkHandler.sendChatMessage(arg.tojstring());
                    }
                    return LuaValue.NIL;
                }
            });

            // client.toggle(name) - toggle module
            clientTable.set("toggle", new OneArgFunction() {
                @Override
                public LuaValue call(LuaValue arg) {
                    String name = arg.tojstring();
                    try {
                        ModuleManager mgr = elegant.getInstance().getModuleManager();
                        var mod = mgr.getModule(name);
                        mod.toggle();
                        return LuaValue.valueOf(mod.isEnabled());
                    } catch (Exception e) {
                        elegant.LOGGER.error("Failed to toggle module {}: {}", name, e.getMessage());
                        return LuaValue.FALSE;
                    }
                }
            });

            // client.isEnabled(name) - check if module enabled
            clientTable.set("isEnabled", new OneArgFunction() {
                @Override
                public LuaValue call(LuaValue arg) {
                    String name = arg.tojstring();
                    try {
                        ModuleManager mgr = elegant.getInstance().getModuleManager();
                        var mod = mgr.getModule(name);
                        return LuaValue.valueOf(mod.isEnabled());
                    } catch (Exception e) {
                        return LuaValue.FALSE;
                    }
                }
            });

            // client.getModules() - get list of all modules
            clientTable.set("getModules", new VarArgFunction() {
                @Override
                public LuaValue invoke(LuaValue[] args) {
                    LuaTable result = new LuaTable();
                    ModuleManager mgr = elegant.getInstance().getModuleManager();
                    for (var mod : mgr.getModules()) {
                        result.add(LuaValue.valueOf(mod.getName()));
                    }
                    return result;
                }
            });

            // client.getPlayer() - get player info table
            clientTable.set("getPlayer", new VarArgFunction() {
                @Override
                public LuaValue invoke(LuaValue[] args) {
                    LuaTable player = new LuaTable();
                    if (elegant.mc.player != null) {
                        player.set("name", elegant.mc.player.getName().getString());
                        player.set("x", LuaValue.valueOf(elegant.mc.player.getX()));
                        player.set("y", LuaValue.valueOf(elegant.mc.player.getY()));
                        player.set("z", LuaValue.valueOf(elegant.mc.player.getZ()));
                        player.set("health", LuaValue.valueOf(elegant.mc.player.getHealth()));
                        player.set("food", LuaValue.valueOf(elegant.mc.player.getHungerManager().getFoodLevel()));
                        player.set("pitch", LuaValue.valueOf(elegant.mc.player.getPitch()));
                        player.set("yaw", LuaValue.valueOf(elegant.mc.player.getYaw()));
                        player.set("grounded", LuaValue.valueOf(elegant.mc.player.isOnGround()));
                    } else {
                        player.set("name", "");
                        player.set("x", LuaValue.ZERO);
                        player.set("y", LuaValue.ZERO);
                        player.set("z", LuaValue.ZERO);
                        player.set("health", LuaValue.ZERO);
                        player.set("food", LuaValue.ZERO);
                        player.set("pitch", LuaValue.ZERO);
                        player.set("yaw", LuaValue.ZERO);
                        player.set("grounded", LuaValue.FALSE);
                    }
                    return player;
                }
            });

            // client.jump() - make player jump
            clientTable.set("jump", new VarArgFunction() {
                @Override
                public LuaValue invoke(LuaValue[] args) {
                    if (elegant.mc.player != null) {
                        elegant.mc.player.jump();
                    }
                    return LuaValue.NIL;
                }
            });

            // client.isJumpKeyDown() - check if jump key is pressed
            clientTable.set("isJumpKeyDown", new VarArgFunction() {
                @Override
                public LuaValue invoke(LuaValue[] args) {
                    return LuaValue.valueOf(elegant.mc.options.jumpKey.isPressed());
                }
            });

            // client.on(eventName, callback) - subscribe to event (только этот скрипт)
            clientTable.set("on", new TwoArgFunction() {
                @Override
                public LuaValue call(LuaValue eventName, LuaValue callback) {
                    String name = eventName.tojstring();
                    if (!callback.isfunction()) {
                        elegant.LOGGER.error("Lua: callback for {} is not a function", name);
                        return LuaValue.NIL;
                    }

                    owner.eventCallbacks.computeIfAbsent(name, k -> new java.util.concurrent.CopyOnWriteArrayList<>()).add(callback);
                    return LuaValue.NIL;
                }
            });

            // client.off(eventName) - unsubscribe from event (только этот скрипт)
            clientTable.set("off", new OneArgFunction() {
                @Override
                public LuaValue call(LuaValue eventName) {
                    owner.eventCallbacks.remove(eventName.tojstring());
                    return LuaValue.NIL;
                }
            });

            // client.clear() - unsubscribe from all events (только этот скрипт)
            clientTable.set("clear", new VarArgFunction() {
                @Override
                public LuaValue invoke(LuaValue[] args) {
                    owner.eventCallbacks.clear();
                    return LuaValue.NIL;
                }
            });

            globals.set("client", clientTable);
        }

        void setChunk(LuaValue chunk) {
            this.chunk = chunk;
        }

        public void run() {
            if (this.running || this.chunk == null) return;
            this.chunk.call();
            this.running = true;
        }

        /** Полная выгрузка: скрипт останавливается, все его подписки на события снимаются. */
        public void unload() {
            this.running = false;
            this.chunk = null;
            this.eventCallbacks.clear();
        }

        public String getName() {
            return this.name;
        }

        public boolean isRunning() {
            return this.running;
        }

        public Globals getGlobals() {
            return this.globals;
        }

        public List<LuaValue> getCallbacks(String eventName) {
            return this.eventCallbacks.get(eventName);
        }
    }
}
