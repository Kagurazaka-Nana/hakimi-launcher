package com.minecraft.launcher.auth;

import java.util.Objects;
import java.util.UUID;

/** 账户抽象：profileName/profileID 稳定，logIn() 产出启动参数。 */
public abstract class Account implements AutoCloseable {

    private final String profileName;
    private final UUID profileID;

    protected Account(String profileName, UUID profileID) {
        this.profileName = Objects.requireNonNull(profileName, "profileName");
        this.profileID = Objects.requireNonNull(profileID, "profileID");
    }

    public String getProfileName() {
        return profileName;
    }

    public UUID getProfileID() {
        return profileID;
    }

    /** 账户类型标识："offline" | "microsoft"。 */
    public abstract String type();

    /** 登录（可能触发网络换票/刷新）。 */
    public abstract AuthInfo logIn() throws AuthenticationException;

    /** 离线游玩：默认等同 logIn。 */
    public AuthInfo playOffline() throws AuthenticationException {
        return logIn();
    }

    @Override
    public void close() {
        // 子类按需释放
    }
}
