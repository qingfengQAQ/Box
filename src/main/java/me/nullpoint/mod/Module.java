package me.nullpoint.mod;

import me.nullpoint.mod.Category;
import me.nullpoint.mod.Event;

public abstract class Module {
    private final String name;
    private final String description;
    private final Category category;

    public Module(String name, String description, Category category) {
        this.name = name;
        this.description = description;
        this.category = category;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public Category getCategory() {
        return category;
    }

    public abstract void onEvent(Event event);
}