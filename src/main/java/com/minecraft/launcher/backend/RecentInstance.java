package com.minecraft.launcher.backend;

/** 最近游玩条目。 */
public final class RecentInstance {

    private final String name;
    private final String version;
    private final String loader;
    private final String time;

    public RecentInstance(String name, String version, String loader, String time) {
        this.name = name;
        this.version = version;
        this.loader = loader;
        this.time = time;
    }

    public String getName() { return name; }
    public String getVersion() { return version; }
    public String getLoader() { return loader; }
    public String getTime() { return time; }
}
