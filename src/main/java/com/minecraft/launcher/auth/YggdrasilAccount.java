package com.minecraft.launcher.auth;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Yggdrasil 第三方账户：accessToken+clientToken 会话、多角色可切换。
 * logIn() 先 validate，失效则 refresh（与 HMCL 行为一致）。
 */
public final class YggdrasilAccount extends Account {

    private final YggdrasilService service;
    private final String serverBase;
    private final String username;
    private final String clientToken;
    private final List<YggdrasilService.Profile> profiles;
    private volatile String accessToken;
    private volatile YggdrasilService.Profile selected;

    public YggdrasilAccount(YggdrasilService service, String serverBase, String username,
                            YggdrasilService.LoginResult result) {
        super(result.selected().name(), parseUuid(result.selected().id()));
        this.service = Objects.requireNonNull(service, "service");
        this.serverBase = YggdrasilService.normalizeBase(serverBase);
        this.username = Objects.requireNonNull(username, "username");
        this.accessToken = result.accessToken();
        this.clientToken = result.clientToken();
        this.profiles = result.profiles();
        this.selected = result.selected();
    }

    private static UUID parseUuid(String id) {
        try {
            return UUID.fromString(id);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("角色 id 不是合法 UUID: " + id, e);
        }
    }

    @Override
    public String type() {
        return "yggdrasil";
    }

    /** 角色可切换：profile 信息以 selected 为准。 */
    @Override
    public String getProfileName() {
        return selected.name();
    }

    @Override
    public UUID getProfileID() {
        return UUID.fromString(selected.id());
    }

    public String serverBase() {
        return serverBase;
    }

    public String username() {
        return username;
    }

    public List<YggdrasilService.Profile> profiles() {
        return profiles;
    }

    public YggdrasilService.Profile selected() {
        return selected;
    }

    /** 皮肤 session 端点（第三方服务器，与 Mojang 同构）。 */
    public String sessionProfileUrl() {
        return YggdrasilService.sessionProfileUrl(serverBase, selected.id());
    }

    /** 切换角色（id 必须属于本账户角色列表）。 */
    public void selectProfile(String profileId) throws AuthenticationException {
        YggdrasilService.Profile target = profiles.stream()
                .filter(p -> p.id().equals(profileId))
                .findFirst()
                .orElseThrow(() -> new AuthenticationException("角色不属于当前账户: " + profileId));
        this.selected = target;
    }

    @Override
    public synchronized AuthInfo logIn() throws AuthenticationException {
        try {
            if (!service.validate(serverBase, accessToken, clientToken)) {
                accessToken = service.refresh(serverBase, accessToken, clientToken).accessToken();
            }
        } catch (java.io.IOException e) {
            throw new AuthenticationException("第三方认证服务器不可达", e);
        }
        return new AuthInfo(selected.name(), UUID.fromString(selected.id()), accessToken, "yggdrasil", "{}");
    }
}
