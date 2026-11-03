package com.isusdlc.systems.modules.modules.other;

import com.isusdlc.elegant;
import com.isusdlc.systems.file.FileManager;
import com.isusdlc.systems.lua.LuaScriptEngine;
import com.isusdlc.systems.modules.api.ModuleCategory;
import com.isusdlc.systems.modules.api.ModuleInfo;
import com.isusdlc.systems.modules.impl.BaseModule;
import com.isusdlc.systems.notifications.NotificationType;
import com.isusdlc.systems.setting.settings.BooleanSetting;
import com.isusdlc.systems.setting.settings.ButtonSetting;
import java.io.File;

@ModuleInfo(
    name = "Lua",
    category = ModuleCategory.OTHER,
    desc = "Lua scripting system for custom scripts"
)
public class LuaModule extends BaseModule {
    private LuaScriptEngine engine;

    private final BooleanSetting autoLoad =
        new BooleanSetting(this, "Auto Load", () -> false).enabled(true);

    private final ButtonSetting openIde =
        new ButtonSetting(this, "IDE", () -> false).action(() ->
            mc.setScreen(new com.isusdlc.ui.mainmenu.LuaScriptScreen()));

    private final ButtonSetting reloadScripts =
        new ButtonSetting(this, "Reload Scripts", () -> false).action(this::reloadScripts);

    private final ButtonSetting openFolder =
        new ButtonSetting(this, "Open Scripts Folder", () -> false).action(() -> {
            try {
                File scriptsDir = new File(FileManager.DIRECTORY, "scripts");
                if (!scriptsDir.exists()) {
                    scriptsDir.mkdirs();
                }
                java.awt.Desktop.getDesktop().open(scriptsDir);
            } catch (Exception e) {
                elegant.LOGGER.error("Failed to open scripts folder: {}", e.getMessage());
            }
        });

    @Override
    public void onEnable() {
        if (!this.autoLoad.isEnabled()) {
            return;
        }
        this.startEngine();
    }

    private void startEngine() {
        if (this.engine != null) {
            this.engine.shutdown();
        }
        this.engine = new LuaScriptEngine();
        this.engine.loadScriptsFromDirectory();
        elegant.getInstance().getNotificationManager()
            .addNotification(NotificationType.SUCCESS,
                "Lua: loaded " + this.engine.getLoadedScripts().size() + " script(s)");
    }

    /** Перезагружает скрипты, создавая движок при необходимости. */
    public void reloadScripts() {
        if (this.engine == null) {
            this.engine = new LuaScriptEngine();
        }
        this.engine.reloadAllScripts();
        elegant.getInstance().getNotificationManager()
            .addNotification(NotificationType.INFO,
                "Lua: reloaded " + this.engine.getLoadedScripts().size() + " script(s)");
    }

    @Override
    public void onDisable() {
        if (this.engine != null) {
            this.engine.shutdown();
            this.engine = null;
        }
        elegant.getInstance().getNotificationManager()
            .addNotification(NotificationType.INFO, "Lua: engine stopped");
    }

    public LuaScriptEngine getEngine() {
        return this.engine;
    }
}
