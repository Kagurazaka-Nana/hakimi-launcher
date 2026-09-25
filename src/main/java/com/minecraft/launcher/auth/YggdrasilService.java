package com.minecraft.launcher.auth;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.minecraft.launcher.download.UrlGuard;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;

/**
 * Yggdrasil（authlib-injector 生态）第三方认证服务：
 * authenticate / refresh / validate，session 端点皮肤查询由 {@link SkinService#loadSkinFrom} 复用。
 * 协议见 docs/Authentication.md §1.4。
 */
public final class YggdrasilService {

    /** 角色（id 为 UUID 字符串）。 */
    public record Profile(String id, String name) {}

    /** 一次登录/刷新结果。 */
    public record LoginResult(String accessToken, String clientToken, List<Profile> profiles, Profile selected) {
        public LoginResult {
            profiles = List.copyOf(profiles);
        }
    }

    private final ObjectMapper mapper = new ObjectMapper();
    private final HttpClient http = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_2)
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();
    /** SSRF 校验函数；测试注入本地 mock 服务器时替换为恒通过实现。 */
    private final Function<String, URI> guard;

    public YggdrasilService() {
        this(UrlGuard::validate);
    }

    public YggdrasilService(Function<String, URI> guard) {
        this.guard = guard;
    }

    /** 用户名密码登录；clientToken 随机生成。 */
    public LoginResult login(String serverBase, String username, String password)
            throws IOException, AuthenticationException {
        String clientToken = UUID.randomUUID().toString();
        JsonNode resp = post(authEndpoint(serverBase, "/authenticate"), Map.of(
                "username", username,
                "password", password,
                "clientToken", clientToken,
                "requestUser", Boolean.TRUE));
        return parseSession(resp, clientToken);
    }

    /** 用 accessToken+clientToken 刷新会话（token 失效时）。 */
    public LoginResult refresh(String serverBase, String accessToken, String clientToken)
            throws IOException, AuthenticationException {
        JsonNode resp = post(authEndpoint(serverBase, "/refresh"), Map.of(
                "accessToken", accessToken,
                "clientToken", clientToken));
        return parseSession(resp, clientToken);
    }

    /** 校验 accessToken 是否仍有效（204=true，401=false）。 */
    public boolean validate(String serverBase, String accessToken, String clientToken) throws IOException {
        try {
            int code = postForStatus(authEndpoint(serverBase, "/validate"), Map.of(
                    "accessToken", accessToken,
                    "clientToken", clientToken));
            return code == 204;
        } catch (IOException e) {
            return false;
        }
    }

    /** session profile 端点（与 Mojang sessionserver 同构），供皮肤查询（下载时由调用方过 SSRF 校验）。 */
    public static String sessionProfileUrl(String serverBase, String profileId) {
        return normalizeBase(serverBase) + "/session/minecraft/profile/" + profileId.replace("-", "") + "?unsigned=false";
    }

    /** 归一化：仅 http/https、去尾斜杠、SSRF 校验。 */
    static String normalizeBase(String serverUrl) {
        Objects.requireNonNull(serverUrl, "serverUrl");
        String trimmed = serverUrl.trim();
        while (trimmed.endsWith("/")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        return trimmed;
    }

    private String authEndpoint(String serverBase, String path) {
        String url = normalizeBase(serverBase) + path;
        guard.apply(url);
        return url;
    }

    private static LoginResult parseSession(JsonNode resp, String clientToken) throws AuthenticationException {
        String accessToken = resp.path("accessToken").asText(null);
        if (accessToken == null) {
            throw new AuthenticationException("第三方认证服务器未返回 accessToken");
        }
        List<Profile> profiles = new ArrayList<>();
        for (JsonNode p : resp.path("availableProfiles")) {
            profiles.add(new Profile(p.path("id").asText(), p.path("name").asText()));
        }
        JsonNode sel = resp.path("selectedProfile");
        Profile selected = sel.isMissingNode() || sel.isEmpty()
                ? (profiles.isEmpty() ? null : profiles.get(0))
                : new Profile(sel.path("id").asText(), sel.path("name").asText());
        if (selected == null) {
            throw new AuthenticationException("该账号下没有角色");
        }
        String realClientToken = resp.path("clientToken").asText(clientToken);
        return new LoginResult(accessToken, realClientToken, profiles, selected);
    }

    private JsonNode post(String url, Map<String, Object> fields) throws IOException, AuthenticationException {
        try {
            String json = mapper.writeValueAsString(fields);
            HttpResponse<String> resp = http.send(HttpRequest.newBuilder(URI.create(url))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8)).build(),
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (resp.statusCode() == 403 || resp.statusCode() == 401) {
                throw new AuthenticationException("用户名或密码错误（HTTP " + resp.statusCode() + "）");
            }
            if (resp.statusCode() / 100 != 2) {
                throw new AuthenticationException("第三方认证服务器响应: HTTP " + resp.statusCode());
            }
            return mapper.readTree(resp.body());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AuthenticationException("请求被中断", e);
        }
    }

    private int postForStatus(String url, Map<String, Object> fields) throws IOException {
        try {
            String json = mapper.writeValueAsString(fields);
            return http.send(HttpRequest.newBuilder(URI.create(url))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8)).build(),
                    HttpResponse.BodyHandlers.discarding()).statusCode();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("请求被中断", e);
        }
    }
}
