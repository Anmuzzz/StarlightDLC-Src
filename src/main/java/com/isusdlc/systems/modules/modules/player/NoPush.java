package com.isusdlc.systems.modules.modules.player;

import com.isusdlc.systems.modules.api.ModuleCategory;
import com.isusdlc.systems.modules.api.ModuleInfo;
import com.isusdlc.systems.modules.impl.BaseModule;
import com.isusdlc.systems.setting.settings.BooleanSetting;

@ModuleInfo(name = "NoPush", category = ModuleCategory.PLAYER, desc = "Убирает коллизию от разных типов")
public class NoPush extends BaseModule {

    private final BooleanSetting water = new BooleanSetting(this, "Вода").enabled(true);
    private final BooleanSetting players = new BooleanSetting(this, "Игроки").enabled(true);
    private final BooleanSetting blocks = new BooleanSetting(this, "Блоки").enabled(true);

    public BooleanSetting getWater() {
        return water;
    }

    public BooleanSetting getPlayers() {
        return players;
    }

    public BooleanSetting getBlocks() {
        return blocks;
    }
}
