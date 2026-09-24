package com.minecraft.launcher.backend;

/** 服务器信息。 */
public final class ServerInfo {

    private final String id;
    private final String name;
    private final String address;
    private final String version;
    private final boolean online;
    private final int players;

    public ServerInfo(String id, String name, String address, String version, boolean online, int players) {
        this.id = id;
        this.name = name;
        this.address = address;
        this.version = version;
        this.online = online;
        this.players = players;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getAddress() { return address; }
    public String getVersion() { return version; }
    public boolean getOnline() { return online; }
    public int getPlayers() { return players; }
}
