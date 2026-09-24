package com.minecraft.launcher.backend;

/** 游戏版本选项。 */
public final class GameVersion {

    private final String id;
    private final String type;

    public GameVersion(String id, String type) {
        this.id = id;
        this.type = type;
    }

    public String getId() { return id; }
    public String getType() { return type; }
}
