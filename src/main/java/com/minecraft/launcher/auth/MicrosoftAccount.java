package com.minecraft.launcher.auth;

import java.io.IOException;
import java.util.Objects;

/**
 * 微软正版账户：持有 MC 会话，过期自动用 refresh_token 重跑换票链。
 */
public final class MicrosoftAccount extends Account {

    private final MicrosoftService service;
    private final String clientId;
    private MicrosoftService.MinecraftSession session;

    public MicrosoftAccount(MicrosoftService service, String clientId, MicrosoftService.MinecraftSession session) {
        super(session.username(), session.uuid());
        this.service = Objects.requireNonNull(service, "service");
        this.clientId = Objects.requireNonNull(clientId, "clientId");
        this.session = Objects.requireNonNull(session, "session");
    }

    @Override
    public String type() {
        return "microsoft";
    }

    @Override
    public synchronized AuthInfo logIn() throws AuthenticationException {
        if (System.currentTimeMillis() >= session.notAfterMillis()) {
            try {
                session = service.refreshMinecraft(clientId, session.refreshToken());
            } catch (IOException e) {
                throw new AuthenticationException("令牌过期且刷新失败", e);
            }
        }
        return new AuthInfo(getProfileName(), getProfileID(), session.accessToken(),
                session.tokenType(), "{}");
    }

    public synchronized String accessToken() {
        return session.accessToken();
    }
}
