package me.nullpoint.api.events;

import me.nullpoint.mod.modules.Module;

public class Event {
    private final Module module = null;
    private final boolean enabled = false;
    private Stage stage = null;
    private boolean cancel;
    public Event(Stage stage) {
        this.cancel = false;
        this.stage = stage;
    }

    public Event() {
    }

    public void cancel() {
        setCancelled(true);
    }

    public boolean isCancel() {
        return cancel;
    }

    public void setCancelled(boolean cancel) {
        this.cancel = cancel;
    }

    public boolean isCancelled() {
        return cancel;
    }
    public Stage getStage() {
        return stage;
    }

    public boolean isPost() {
        return stage == Stage.Post;
    }

    public boolean isPre() {
        return stage == Stage.Pre;
    }
    public Module getModule() {
        return module;
    }

    public boolean isEnabled() {
        return enabled;
    }


    public enum Stage{
        Pre, Post
    }
}
