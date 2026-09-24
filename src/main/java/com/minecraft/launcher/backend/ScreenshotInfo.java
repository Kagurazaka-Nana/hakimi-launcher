package com.minecraft.launcher.backend;

/** 截图信息。 */
public final class ScreenshotInfo {

    private final String id;
    private final String name;
    private final String time;
    private final String size;

    public ScreenshotInfo(String id, String name, String time, String size) {
        this.id = id;
        this.name = name;
        this.time = time;
        this.size = size;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getTime() { return time; }
    public String getSize() { return size; }
}
