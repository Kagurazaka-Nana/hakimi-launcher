package com.minecraft.launcher.auth;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.minecraft.launcher.download.UrlGuard;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * 微软正版认证链（docs/Authentication.md §1.1）：
 * 设备码 → Live token → XBL → XSTS → login_with_xbox → entitlements → profile。
 *
 * client_id 需为 Azure 门户注册、开启设备流的公开应用（本项目经构造参数注入，
 * 临时测试从环境变量读取）。
 */
public final class MicrosoftService {

    private static final String SCOPE = "XboxLive.signin offline_access";
    private static final String DEVICE_CODE_URL = "https://login.microsoftonline.com/consumers/oauth2/v2.0/devicecode";
    private static final String TOKEN_URL = "https://login.microsoftonline.com/consumers/oauth2/v2.0/token";
    private static final String DEVICE_GRANT = "urn:ietf:params:oauth:grant-type:device_code";

    private static final String XBL_URL = "https://user.auth.xboxlive.com/user/authenticate";
    private static final String XSTS_URL = "https://xsts.auth.xboxlive.com/xsts/authorize";
    private static final String MC_LOGIN_URL = "https://api.minecraftservices.com/authentication/login_with_xbox";
    private static final String MC_ENTITLEMENTS_URL = "https://api.minecraftservices.com/entitlements/mcstore";
    private static final String MC_PROFILE_URL = "https://api.minecraftservices.com/minecraft/profile";

    /** 设备码申请结果。 */
    public record DeviceCodeInfo(String deviceCode, String userCode, String verificationUri,
                                 int intervalSeconds, int expiresInSeconds) {}

    /** Live token（微软账户层）。 */
    public record LiveTokens(String accessToken, String refreshToken) {}

    /** 最终 MC 会话。 */
    public record MinecraftSession(String accessToken, String tokenType, long notAfterMillis,
                                   String refreshToken, String username, UUID uuid) {}

    private final ObjectMapper mapper = new ObjectMapper();
    private final HttpClient http = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_2)
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    /** ① 申请设备码；调用方负责把 userCode/verificationUri 展示给用户。 */
    public DeviceCodeInfo requestDeviceCode(String clientId) throws IOException, AuthenticationException {
        requireClientId(clientId);
        JsonNode json = postForm(DEVICE_CODE_URL, Map.of(
                "client_id", clientId,
                "scope", SCOPE));
        return new DeviceCodeInfo(
                json.path("device_code").asText(null),
                json.path("user_code").asText(null),
                json.path("verification_uri").asText("https://www.microsoft.com/link"),
                Math.max(1, json.path("interval").asInt(5)),
                Math.max(1, json.path("expires_in").asInt(900)));
    }

    /** ② 轮询设备码授权结果（阻塞，尊重 interval / slow_down / expired_token）。 */
    public LiveTokens pollForLiveTokens(String clientId, DeviceCodeInfo info)
            throws IOException, AuthenticationException, InterruptedException {
        requireClientId(clientId);
        long deadline = System.currentTimeMillis() + info.expiresInSeconds() * 1000L;
        long intervalMillis = info.intervalSeconds() * 1000L;
        while (true) {
            if (System.currentTimeMillis() > deadline) {
                throw new AuthenticationException("设备码已过期，请重新登录");
            }
            Thread.sleep(intervalMillis);
            JsonNode json = postForm(TOKEN_URL, Map.of(
                    "client_id", clientId,
                    "grant_type", DEVICE_GRANT,
                    "code", info.deviceCode()));
            if (json.hasNonNull("access_token")) {
                return new LiveTokens(json.get("access_token").asText(), json.path("refresh_token").asText(null));
            }
            String error = json.path("error").asText("");
            switch (error) {
                case "authorization_pending" -> { /* 继续轮询 */ }
                case "slow_down" -> intervalMillis += 5_000;
                case "expired_token", "code_expired" -> throw new AuthenticationException("设备码已过期，请重新登录");
                default -> throw new AuthenticationException("微软登录被拒绝: " + error
                        + (json.hasNonNull("error_description") ? " - " + json.get("error_description").asText() : ""));
            }
        }
    }

    /** 用 refresh_token 静默刷新 Live token。 */
    public LiveTokens refreshLiveTokens(String clientId, String refreshToken) throws IOException, AuthenticationException {
        requireClientId(clientId);
        Objects.requireNonNull(refreshToken, "refreshToken");
        JsonNode json = postForm(TOKEN_URL, Map.of(
                "client_id", clientId,
                "grant_type", "refresh_token",
                "refresh_token", refreshToken,
                "scope", SCOPE));
        if (!json.hasNonNull("access_token")) {
            throw new AuthenticationException("刷新失败: " + json.path("error").asText("unknown"));
        }
        return new LiveTokens(json.get("access_token").asText(),
                json.path("refresh_token").asText(refreshToken));
    }

    /** ③~⑦ Live token → MC 会话。 */
    public MinecraftSession exchangeForMinecraft(LiveTokens live) throws IOException, AuthenticationException {
        // XBL
        var xblProps = mapper.createObjectNode();
        xblProps.put("AuthMethod", "RPS")
                .put("SiteName", "user.auth.xboxlive.com")
                .put("RpsTicket", "d=" + live.accessToken());
        var xblBody = mapper.createObjectNode();
        xblBody.set("Properties", xblProps);
        xblBody.put("RelyingParty", "http://auth.xboxlive.com");
        xblBody.put("TokenType", "JWT");
        JsonNode xbl = postJson(XBL_URL, xblBody);
        String uhs = extractUhs(xbl, null);

        // XSTS
        var xstsProps = mapper.createObjectNode();
        xstsProps.put("SandboxId", "RETAIL");
        xstsProps.putArray("UserTokens").add(xbl.path("Token").asText());
        var xstsBody = mapper.createObjectNode();
        xstsBody.set("Properties", xstsProps);
        xstsBody.put("RelyingParty", "rp://api.minecraftservices.com/");
        xstsBody.put("TokenType", "JWT");
        JsonNode xsts = postJson(XSTS_URL, xstsBody);
        uhs = extractUhs(xsts, uhs);

        // MC token
        JsonNode mc = postJson(MC_LOGIN_URL, mapper.createObjectNode()
                .put("identityToken", "XBL3.0 x=" + uhs + ";" + xsts.path("Token").asText()));
        String mcToken = mc.path("access_token").asText(null);
        if (mcToken == null) {
            throw new AuthenticationException("login_with_xbox 未返回 access_token");
        }
        long notAfter = System.currentTimeMillis() + Math.max(0, mc.path("expires_in").asLong(86400) - 60) * 1000L;

        // 权属校验（GitHub#2979：不查会拿到进不了服的 token）
        int entitlementStatus = getWithBearer(MC_ENTITLEMENTS_URL, mcToken);
        if (entitlementStatus != 200) {
            throw new AuthenticationException("此账号未购买 Minecraft（entitlements: " + entitlementStatus + "）");
        }

        // MC profile（uuid）
        JsonNode profile = getJsonWithBearer(MC_PROFILE_URL, mcToken);
        UUID mcUuid;
        try {
            mcUuid = UUID.fromString(profile.path("id").asText());
        } catch (IllegalArgumentException e) {
            throw new AuthenticationException("MC profile uuid 格式异常", e);
        }
        return new MinecraftSession(mcToken, mc.path("token_type").asText("Bearer"), notAfter,
                live.refreshToken(), profile.path("name").asText(), mcUuid);
    }

    /** 完整链：refresh + 换票（供 MicrosoftAccount 过期刷新用）。 */
    public MinecraftSession refreshMinecraft(String clientId, String refreshToken)
            throws IOException, AuthenticationException {
        return exchangeForMinecraft(refreshLiveTokens(clientId, refreshToken));
    }

    // —— HTTP 与解析辅助 ——

    private static void requireClientId(String clientId) throws AuthenticationException {
        if (clientId == null || clientId.isBlank()) {
            throw new AuthenticationException("未配置 Microsoft client_id（环境变量 HAKIMI_MS_CLIENT_ID）");
        }
    }

    private JsonNode postForm(String url, Map<String, String> form) throws IOException, AuthenticationException {
        UrlGuard.validate(url);
        String body = form.entrySet().stream()
                .map(e -> URLEncoder.encode(e.getKey(), StandardCharsets.UTF_8) + "="
                        + URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8))
                .reduce((a, b) -> a + "&" + b).orElse("");
        try {
            HttpResponse<String> resp = http.send(HttpRequest.newBuilder(URI.create(url))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8)).build(),
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            // 设备码轮询的 400/401/402 带 JSON error 体，属正常协议信号，统一交给上层按 error 字段判断
            return mapper.readTree(resp.body());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("请求被中断: " + url, e);
        }
    }

    private JsonNode postJson(String url, Object body) throws IOException, AuthenticationException {
        UrlGuard.validate(url);
        try {
            String json = mapper.writeValueAsString(body);
            HttpResponse<String> resp = http.send(HttpRequest.newBuilder(URI.create(url))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8)).build(),
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (resp.statusCode() == 400) {
                throw new AuthenticationException("Xbox 认证被拒（400）：可能为地区/年龄限制，或账号类型不支持");
            }
            if (resp.statusCode() / 100 != 2) {
                throw new AuthenticationException("Xbox 认证失败: HTTP " + resp.statusCode());
            }
            return mapper.readTree(resp.body());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("请求被中断: " + url, e);
        }
    }

    private int getWithBearer(String url, String token) throws IOException {
        UrlGuard.validate(url);
        try {
            return http.send(HttpRequest.newBuilder(URI.create(url))
                    .header("Authorization", "Bearer " + token).GET().build(),
                    HttpResponse.BodyHandlers.discarding()).statusCode();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("请求被中断: " + url, e);
        }
    }

    private JsonNode getJsonWithBearer(String url, String token) throws IOException, AuthenticationException {
        UrlGuard.validate(url);
        try {
            HttpResponse<String> resp = http.send(HttpRequest.newBuilder(URI.create(url))
                    .header("Authorization", "Bearer " + token).GET().build(),
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (resp.statusCode() / 100 != 2) {
                throw new AuthenticationException("获取 MC profile 失败: HTTP " + resp.statusCode());
            }
            return mapper.readTree(resp.body());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("请求被中断: " + url, e);
        }
    }

    /** 提取 displayClaims.xui[0].uhs，并与既有值比对（防串号，HMCL 同款校验）。 */
    static String extractUhs(JsonNode response, String existingUhs) throws AuthenticationException {
        if (response.path("Token").isMissingNode()) {
            long xerr = response.path("XErr").asLong(0);
            throw new AuthenticationException(xerr != 0
                    ? "Xbox 认证错误 XErr=" + xerr + "（可能为年龄/地区限制）"
                    : "Xbox 响应缺少 Token");
        }
        JsonNode xui = response.path("DisplayClaims").path("xui");
        if (!xui.isArray() || xui.isEmpty() || !xui.get(0).has("uhs")) {
            throw new AuthenticationException("Xbox 响应缺少 uhs");
        }
        String uhs = xui.get(0).get("uhs").asText();
        if (existingUhs != null && !existingUhs.equals(uhs)) {
            throw new AuthenticationException("XBL/XSTS uhs 不一致");
        }
        return uhs;
    }
}
