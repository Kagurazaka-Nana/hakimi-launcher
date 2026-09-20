package com.minecraft.launcher.api;

import com.minecraft.launcher.download.UrlValidator;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 只读的 HTTP/JSON 客户端：所有请求前经 {@link UrlValidator} 做 SSRF 校验，
 * 仅允许 http/https 与公网地址。
 */
public final class HttpJsonClient implements HttpTextFetcher {

    private final HttpClient client;
    private final String userAgent;

    public HttpJsonClient() {
        this("hakimi-launcher/1.0", Duration.ofSeconds(20));
    }

    public HttpJsonClient(String userAgent, Duration connectTimeout) {
        this.userAgent = userAgent;
        this.client = HttpClient.newBuilder()
                .connectTimeout(connectTimeout)
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    /** GET 指定 URL，返回 UTF-8 响应体；非 2xx 抛异常。 */
    @Override
    public String get(String url, Map<String, String> headers) {
        URI uri = UrlValidator.validate(url);
        HttpRequest.Builder builder = HttpRequest.newBuilder(uri)
                .GET()
                .timeout(Duration.ofSeconds(30))
                .header("User-Agent", userAgent)
                .header("Accept", "application/json, text/html;q=0.8, */*;q=0.5");
        if (headers != null) {
            headers.forEach(builder::header);
        }
        try {
            HttpResponse<String> response = client.send(builder.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            int code = response.statusCode();
            if (code < 200 || code >= 300) {
                throw new IOException("HTTP " + code + " for " + uri.getHost() + uri.getPath());
            }
            return response.body();
        } catch (IOException e) {
            throw new RuntimeException("请求失败: " + uri, e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("请求被中断: " + uri, e);
        }
    }

    /** 构造带查询参数的 URL（对 key/value 做 URL 编码）。 */
    public static String withQuery(String base, Map<String, String> params) {
        if (params == null || params.isEmpty()) {
            return base;
        }
        StringBuilder sb = new StringBuilder(base);
        sb.append(base.contains("?") ? '&' : '?');
        boolean first = true;
        for (Map.Entry<String, String> entry : new LinkedHashMap<>(params).entrySet()) {
            if (!first) {
                sb.append('&');
            }
            sb.append(encode(entry.getKey())).append('=').append(encode(entry.getValue()));
            first = false;
        }
        return sb.toString();
    }

    private static String encode(String value) {
        return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8);
    }
}
