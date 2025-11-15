package me.nullpoint.api.events.impl;

import me.nullpoint.api.events.Event;
import me.nullpoint.mod.modules.Module;

public class ModuleStateEvent extends Event {
    private Module module;
    private boolean enabled;

    // 无参构造函数（必须）
    public ModuleStateEvent() {}

    // 带参数的便捷构造函数
    public ModuleStateEvent(Module module, boolean enabled) {
        this.module = module;
        this.enabled = enabled;
    }

    // Getter 和 Setter 方法
    public Module getModule() {
        return module;
    }

    public void setModule(Module module) {
        this.module = module;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}