package com.minecraft.launcher.auth;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Yggdrasil 第三方认证集成测试：本地 mock 认证服务器（authenticate/validate/refresh/session 皮肤端点）。
 * 本地回环通过注入恒通过 guard 绕过（真实拦截由 UrlGuardTest 覆盖）。
 */
class YggdrasilServiceTest {

    private static final String P1_ID = "11111111-1111-1111-1111-111111111111";
    private static final String P2_ID = "22222222-2222-2222-2222-222222222222";

    private HttpServer server;
    private final Set<String> validTokens = new HashSet<>();
    private String base;
    private YggdrasilService service;
    private SkinService skinService;

    @BeforeEach
    void startServer() throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/authenticate", ex -> {
            String body = new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            if (body.contains("\"hakimi\"") && body.contains("\"secret\"")) {
                validTokens.add("tok-1");
                respond(ex, 200, sessionJson("tok-1"));
            } else {
                respond(ex, 403, "{\"error\":\"Bad credentials\"}");
            }
        });
        server.createContext("/validate", ex -> {
            String body = new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            ex.sendResponseHeaders(validTokens.stream().anyMatch(body::contains) ? 204 : 401, -1);
            ex.close();
        });
        server.createContext("/refresh", ex -> {
            validTokens.clear();
            validTokens.add("tok-2");
            respond(ex, 200, sessionJson("tok-2"));
        });
        server.createContext("/session/minecraft/profile/", ex -> {
            String textures = Base64.getEncoder().encodeToString((
                    "{\"textures\":{\"SKIN\":{\"url\":\"" + base + "/skin.png\"}}}").getBytes(StandardCharsets.UTF_8));
            respond(ex, 200, """
                    {"id":"%s","name":"hakimi","properties":[{"name":"textures","value":"%s"}]}
                    """.formatted(P1_ID.replace("-", ""), textures));
        });
        server.createContext("/skin.png", ex -> {
            byte[] png = SkinService.defaultSkinBytes();
            ex.getResponseHeaders().add("Content-Type", "image/png");
            ex.sendResponseHeaders(200, png.length);
            ex.getResponseBody().write(png);
            ex.close();
        });
        server.setExecutor(Executors.newVirtualThreadPerTaskExecutor());
        server.start();
        base = "http://127.0.0.1:" + server.getAddress().getPort();
        service = new YggdrasilService(URI::create);
        skinService = new SkinService(URI::create);
    }

    @AfterEach
    void stopServer() {
        server.stop(0);
    }

    private static void respond(com.sun.net.httpserver.HttpExchange ex, int code, String json) throws java.io.IOException {
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().add("Content-Type", "application/json");
        ex.sendResponseHeaders(code, bytes.length);
        ex.getResponseBody().write(bytes);
        ex.close();
    }

    private static String sessionJson(String token) {
        return """
                {"accessToken":"%s","clientToken":"ct-1",
                 "availableProfiles":[{"id":"%s","name":"hakimi"},{"id":"%s","name":"nana"}],
                 "selectedProfile":{"id":"%s","name":"hakimi"}}
                """.formatted(token, P1_ID, P2_ID, P1_ID);
    }

    @Test
    void loginParsesSessionAndProfiles() throws Exception {
        YggdrasilService.LoginResult r = service.login(base, "hakimi", "secret");
        assertEquals("tok-1", r.accessToken());
        assertEquals("ct-1", r.clientToken());
        assertEquals(2, r.profiles().size());
        assertEquals("hakimi", r.selected().name());
    }

    @Test
    void wrongPasswordThrows() {
        assertThrows(AuthenticationException.class, () -> service.login(base, "hakimi", "wrong"));
    }

    @Test
    void accountLogInReturnsAuthInfo() throws Exception {
        YggdrasilAccount account = new YggdrasilAccount(service, base, "hakimi", service.login(base, "hakimi", "secret"));
        AuthInfo info = account.logIn();
        assertEquals("hakimi", info.getUsername());
        assertEquals(UUID.fromString(P1_ID), info.getUuid());
        assertEquals("tok-1", info.getAccessToken());
        assertEquals("yggdrasil", info.getUserType());
        assertEquals("yggdrasil", account.type());
    }

    @Test
    void accountLogInRefreshesWhenTokenInvalidated() throws Exception {
        YggdrasilAccount account = new YggdrasilAccount(service, base, "hakimi", service.login(base, "hakimi", "secret"));
        validTokens.clear(); // 服务器吊销 token → validate 401 → 走 refresh
        AuthInfo info = account.logIn();
        assertEquals("tok-2", info.getAccessToken());
    }

    @Test
    void selectProfileSwitchesCharacter() throws Exception {
        YggdrasilAccount account = new YggdrasilAccount(service, base, "hakimi", service.login(base, "hakimi", "secret"));
        account.selectProfile(P2_ID);
        assertEquals("nana", account.selected().name());
        assertEquals(UUID.fromString(P2_ID), account.getProfileID());
        assertTrue(account.sessionProfileUrl().contains(P2_ID.replace("-", "")));
        assertThrows(AuthenticationException.class, () -> account.selectProfile("unknown-id"));
    }

    @Test
    void skinLoadsFromSessionEndpoint() throws Exception {
        YggdrasilService.LoginResult r = service.login(base, "hakimi", "secret");
        YggdrasilAccount account = new YggdrasilAccount(service, base, "hakimi", r);
        SkinService.SkinData skin = skinService.loadSkinFrom(account.sessionProfileUrl());
        assertNotEquals(null, skin);
        assertArrayEquals(SkinService.defaultSkinBytes(), skin.png());
        assertEquals(false, skin.slim());
    }

    @Test
    void skinFallsBackToDefaultWhenEndpointDead() throws Exception {
        SkinService.SkinData skin = skinService.loadSkinFrom("http://127.0.0.1:1/dead");
        assertArrayEquals(SkinService.defaultSkinBytes(), skin.png());
    }
}
