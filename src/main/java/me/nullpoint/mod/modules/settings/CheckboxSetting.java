package me.nullpoint.mod.modules.settings;

public class CheckboxSetting {
    private String name;
    private boolean value;

    public CheckboxSetting(String name, boolean defaultValue) {
        this.name = name;
        this.value = defaultValue;
    }

    public String getName() {
        return name;
    }

    public boolean isChecked() {
        return value;
    }

    public void setChecked(boolean checked) {
        this.value = checked;
    }
}
