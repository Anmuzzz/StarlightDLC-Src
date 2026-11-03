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

    private final ButtonSetting reloadScripts =
        new ButtonSetting(this, "Reload Scripts", () -> false).action(() -> {
            if (this.engine != null) {
                this.engine.reloadAllScripts();
                elegant.getInstance().getNotificationManager()
                    .addNotification(NotificationType.INFO, "Lua: scripts reloaded");
            }
        });

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
        if (this.engine != null) {
            this.engine.shutdown();
        }
        this.engine = new LuaScriptEngine();
        this.engine.loadScriptsFromDirectory();
        elegant.getInstance().getNotificationManager()
            .addNotification(NotificationType.SUCCESS,
                "Lua: loaded " + this.engine.getLoadedScripts().size() + " script(s)");
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
