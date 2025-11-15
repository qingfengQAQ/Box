package me.nullpoint.mod.modules.settings.impl;

import java.util.ArrayList;
import java.util.List;

import me.nullpoint.mod.modules.settings.Setting;

public class Module {
    protected String name;
    protected ModuleCategory category;
    protected List<Setting> settings;

    public Module(String name, ModuleCategory category) {
        this.name = name;
        this.category = category;
        this.settings = new ArrayList<>();
    }

    public String getName() {
        return name;
    }

    public ModuleCategory getCategory() {
        return category;
    }

    public <T extends Setting> T addSetting(T setting) {
        settings.add(setting);
        return setting;
    }

    public List<Setting> getSettings() {
        return settings;
    }
}
