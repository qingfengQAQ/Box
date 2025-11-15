package me.nullpoint.mod;

import java.util.ArrayList;
import java.util.List;

public class EventManager {
    private final List<Module> modules = new ArrayList<>();

    public void registerModule(Module module) {
        modules.add(module);
    }

    public void callEvent(Event event) {
        for (Module module : modules) {
            module.onEvent(event);
        }
    }
}

