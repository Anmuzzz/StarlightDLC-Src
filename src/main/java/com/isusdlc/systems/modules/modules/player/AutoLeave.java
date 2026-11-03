package com.isusdlc.systems.modules.modules.player;
import com.isusdlc.systems.event.EventListener;

import com.isusdlc.systems.event.EventListener;
import com.isusdlc.systems.event.impl.player.ClientPlayerTickEvent;
import com.isusdlc.systems.modules.api.ModuleCategory;
import com.isusdlc.systems.modules.api.ModuleInfo;
import com.isusdlc.systems.modules.impl.BaseModule;
import com.isusdlc.systems.setting.settings.BooleanSetting;
import com.isusdlc.systems.setting.settings.ModeSetting;
import com.isusdlc.systems.setting.settings.SliderSetting;
import net.minecraft.text.Text;

@ModuleInfo(name = "AutoLeave", category = ModuleCategory.PLAYER, desc = "Автоматически ливает с сервера")
public class AutoLeave extends BaseModule {

    private final ModeSetting mode = new ModeSetting(this, "Режим");
    private final ModeSetting.Value playerMode = new ModeSetting.Value(mode, "Рядом игрок").select();
    private final ModeSetting.Value hpMode = new ModeSetting.Value(mode, "Мало Хп");

    private final SliderSetting heal = new SliderSetting(this, "Здоровье").min(1).max(20).step(1).currentValue(3);
    private final SliderSetting radius = new SliderSetting(this, "Радиус до игрока").min(20).max(150).step(1).currentValue(60);

    private final ModeSetting action = new ModeSetting(this, "Что делать");
    private final ModeSetting.Value leaveAction = new ModeSetting.Value(action, "Выходить с сервера").select();
    private final ModeSetting.Value hubAction = new ModeSetting.Value(action, "/hub");
    private final ModeSetting.Value homeAction = new ModeSetting.Value(action, "телепортация домой");

    private final BooleanSetting pvpNoLeave = new BooleanSetting(this, "Не выходить если режим PVP").enabled(true);

    private boolean triggered = false;

    private final EventListener<ClientPlayerTickEvent> listener = event -> {
        if (mc.player == null || mc.world == null) return;

        boolean shouldTrigger = false;

        if (hpMode.isSelected() && mc.player.getHealth() <= heal.getCurrentValue()) {
            shouldTrigger = true;
        } else if (playerMode.isSelected()) {
            if (pvpNoLeave.isEnabled() && mc.player.getHealth() <= 6) return;
            shouldTrigger = mc.world.getPlayers().stream()
                .anyMatch(other -> other != mc.player && mc.player.distanceTo(other) <= radius.getCurrentValue());
        }

        if (shouldTrigger && !triggered) {
            executeAction();
            triggered = true;
        } else if (!shouldTrigger) {
            triggered = false;
        }
    };

    private void executeAction() {
        if (mc.player == null) return;

        if (leaveAction.isSelected()) {
            mc.player.networkHandler.getConnection().disconnect(Text.literal("AutoLeave"));
        } else if (hubAction.isSelected()) {
            mc.player.networkHandler.sendCommand("hub");
        } else if (homeAction.isSelected()) {
            mc.player.networkHandler.sendCommand("home home");
        }
        toggle();
    }
}
