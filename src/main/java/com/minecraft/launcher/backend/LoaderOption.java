package com.minecraft.launcher.backend;

/** 加载器选项。 */
public final class LoaderOption {

    private final String id;
    private final String label;

    public LoaderOption(String id, String label) {
        this.id = id;
        this.label = label;
    }

    public String getId() { return id; }
    public String getLabel() { return label; }
}
