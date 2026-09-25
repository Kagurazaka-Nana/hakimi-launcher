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
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;

/**
 * 皮肤服务：sessionserver 查询角色 textures（无需认证）→ base64 JSON 解析 → 下载皮肤 PNG。
 * 下载前对 URL 过 {@link UrlGuard}（防响应注入内网地址）。
 */
public final class SkinService {

    private static final String PROFILE_URL = "https://sessionserver.mojang.com/session/minecraft/profile/";
    /** 原版默认皮肤（Steve，经典模型）：从客户端 jar 提取后随资源打包。 */
    private static final String DEFAULT_SKIN_RESOURCE = "/assets/skins/steve.png";
    /** 皮肤 PNG 上限 1MB（原版远小于此），防异常响应耗尽内存。 */
    private static final int MAX_SKIN_BYTES = 1 << 20;

    /** 解析出的 textures 信息。 */
    public record Textures(String skinUrl, boolean slim, String capeUrl) {}

    /** 已下载皮肤数据。 */
    public record SkinData(byte[] png, boolean slim) {}

    private final ObjectMapper mapper = new ObjectMapper();
    private final HttpClient http = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_2)
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();
    /** SSRF 校验函数；测试注入本地回环 mock 服务器时替换为恒通过实现。 */
    private final java.util.function.Function<String, URI> guard;

    public SkinService() {
        this(UrlGuard::validate);
    }

    public SkinService(java.util.function.Function<String, URI> guard) {
        this.guard = guard;
    }

    /** 查询角色 textures；无皮肤或角色不存在返回 empty。 */
    public Optional<Textures> fetchTextures(UUID uuid) throws IOException {
        return fetchFromUrl(PROFILE_URL + uuid.toString().replace("-", ""));
    }

    /** 从任意 session profile 端点查询（Yggdrasil 第三方服务器与 Mojang 同构）。 */
    public Optional<Textures> fetchFromUrl(String url) throws IOException {
        guard.apply(url);
        try {
            HttpResponse<String> resp = http.send(
                    HttpRequest.newBuilder(URI.create(url)).GET().build(),
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (resp.statusCode() == 204 || resp.statusCode() == 404) {
                return Optional.empty();
            }
            if (resp.statusCode() / 100 != 2) {
                throw new IOException("sessionserver 响应: HTTP " + resp.statusCode());
            }
            return Optional.ofNullable(parseProfile(resp.body()));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("皮肤查询被中断", e);
        }
    }

    /** 解析 sessionserver profile JSON（public 便于单测）。 */
    public Textures parseProfile(String profileJson) throws IOException {
        JsonNode root = mapper.readTree(profileJson);
        JsonNode properties = root.path("properties");
        if (!properties.isArray()) {
            return null;
        }
        for (JsonNode property : properties) {
            if (!"textures".equals(property.path("name").asText())) {
                continue;
            }
            String decoded = new String(Base64.getDecoder().decode(property.path("value").asText()), StandardCharsets.UTF_8);
            return parseTexturesJson(decoded);
        }
        return null;
    }

    /** 解析 base64 内的 textures JSON（public 便于单测）。 */
    public Textures parseTextures(String texturesJson) throws IOException {
        return parseTexturesJson(texturesJson);
    }

    private Textures parseTexturesJson(String json) throws IOException {
        JsonNode textures = mapper.readTree(json).path("textures");
        JsonNode skin = textures.path("SKIN");
        if (skin.isMissingNode() || skin.path("url").isMissingNode()) {
            return null;
        }
        boolean slim = "slim".equals(skin.path("metadata").path("model").asText(null));
        String cape = textures.path("CAPE").path("url").isMissingNode() ? null : textures.path("CAPE").path("url").asText();
        return new Textures(skin.path("url").asText(), slim, cape);
    }

    /** 完整加载：textures → 下载 PNG；无皮肤/查询失败回退原版默认 Steve。 */
    public SkinData loadSkin(UUID uuid) throws IOException {
        Textures textures;
        try {
            textures = fetchTextures(uuid).orElse(null);
        } catch (IOException e) {
            textures = null; // 网络/服务异常不让衣柜空着，默认皮肤兜底
        }
        if (textures == null) {
            return new SkinData(defaultSkinBytes(), false);
        }
        return new SkinData(downloadPng(textures.skinUrl()), textures.slim());
    }

    /** 从任意 session profile 端点加载皮肤（Yggdrasil 等）；失败回退默认 Steve。 */
    public SkinData loadSkinFrom(String profileUrl) {
        try {
            Optional<Textures> textures = fetchFromUrl(profileUrl);
            if (textures.isPresent()) {
                return new SkinData(downloadPng(textures.get().skinUrl()), textures.get().slim());
            }
        } catch (IOException e) {
            // 落到默认皮肤
        }
        try {
            return new SkinData(defaultSkinBytes(), false);
        } catch (IOException e) {
            return null;
        }
    }

    /** 原版默认皮肤字节（Steve / classic 模型）。 */
    public static byte[] defaultSkinBytes() throws IOException {
        try (java.io.InputStream in = SkinService.class.getResourceAsStream(DEFAULT_SKIN_RESOURCE)) {
            if (in == null) {
                throw new IOException("缺少默认皮肤资源 " + DEFAULT_SKIN_RESOURCE);
            }
            byte[] bytes = in.readAllBytes();
            if (bytes.length == 0 || bytes.length > MAX_SKIN_BYTES) {
                throw new IOException("默认皮肤资源大小异常: " + bytes.length);
            }
            return bytes;
        }
    }

    private byte[] downloadPng(String url) throws IOException {
        guard.apply(url);
        try {
            HttpResponse<byte[]> resp = http.send(
                    HttpRequest.newBuilder(URI.create(url)).GET().build(),
                    HttpResponse.BodyHandlers.ofByteArray());
            if (resp.statusCode() / 100 != 2) {
                throw new IOException("皮肤下载失败: HTTP " + resp.statusCode());
            }
            byte[] body = resp.body();
            if (body.length == 0 || body.length > MAX_SKIN_BYTES) {
                throw new IOException("皮肤大小异常: " + body.length);
            }
            return body;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("皮肤下载被中断", e);
        }
    }
}
