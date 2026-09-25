package com.minecraft.launcher.auth;

import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.UUID;

/**
 * 离线账户：零网络登录，accessToken 为随机串（仅离线服务器接受）。
 *
 * UUID 沿用原版离线算法：MD5("OfflinePlayer" + name) 的 version-3 UUID——
 * 这是 Mojang 协议规定的内容寻址标识（非安全语义），故集中在此单点实现。
 */
public final class OfflineAccount extends Account {

    public OfflineAccount(String name) {
        this(name, nameToUuid(name));
    }

    public OfflineAccount(String name, UUID uuid) {
        super(name, uuid);
    }

    /** 原版离线 UUID 算法（与服务器 offline-mode 生成一致）。 */
    public static UUID nameToUuid(String name) {
        Objects.requireNonNull(name, "name");
        return UUID.nameUUIDFromBytes(("OfflinePlayer" + name).getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public String type() {
        return "offline";
    }

    @Override
    public AuthInfo logIn() {
        return new AuthInfo(getProfileName(), getProfileID(),
                UUID.randomUUID().toString().replace("-", ""), "msa", "{}");
    }
}
