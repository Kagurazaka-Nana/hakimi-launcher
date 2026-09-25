package com.minecraft.launcher.auth;

import java.util.Objects;
import java.util.UUID;

/**
 * 一次登录的产物：拼进游戏启动参数的账户信息。
 * accessToken 属敏感数据，toString 已脱敏。
 */
public final class AuthInfo {

    private final String username;
    private final UUID uuid;
    private final String accessToken;
    private final String userType; // msa | legacy | offline
    private final String userProperties; // JSON 字符串，离线/正版通常为 "{}"

    public AuthInfo(String username, UUID uuid, String accessToken, String userType, String userProperties) {
        this.username = Objects.requireNonNull(username, "username");
        this.uuid = Objects.requireNonNull(uuid, "uuid");
        this.accessToken = Objects.requireNonNull(accessToken, "accessToken");
        this.userType = Objects.requireNonNull(userType, "userType");
        this.userProperties = userProperties == null ? "{}" : userProperties;
    }

    public String getUsername() {
        return username;
    }

    public UUID getUuid() {
        return uuid;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public String getUserType() {
        return userType;
    }

    public String getUserProperties() {
        return userProperties;
    }

    @Override
    public String toString() {
        return "AuthInfo(" + username + ", " + uuid + ", " + userType + ", token=***)";
    }
}
