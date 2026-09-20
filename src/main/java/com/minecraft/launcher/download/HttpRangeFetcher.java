package com.minecraft.launcher.download;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * 基于 JDK HttpClient 的真实 Range 抓取实现。
 * 每次请求前用 {@link UrlValidator} 做 SSRF 校验，仅允许 http/https 与公网地址。
 */
public final class HttpRangeFetcher implements RangeFetcher {

    private final HttpClient client;
    private final String userAgent;

    public HttpRangeFetcher() {
        this("hakimi-launcher/1.0", Duration.ofSeconds(30));
    }

    public HttpRangeFetcher(String userAgent, Duration connectTimeout) {
        this.userAgent = userAgent;
        this.client = HttpClient.newBuilder()
                .connectTimeout(connectTimeout)
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    @Override
    public long size(String url) {
        URI uri = UrlValidator.validate(url);
        HttpRequest request = HttpRequest.newBuilder(uri)
                .header("User-Agent", userAgent)
                .header("Range", "bytes=0-0")
                .GET()
                .build();
        try {
            HttpResponse<byte[]> response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());
            int code = response.statusCode();
            if (code == 206) {
                return parseTotalFromContentRange(response.headers().firstValue("Content-Range").orElse(null));
            }
            if (code >= 200 && code < 300) {
                return response.headers().firstValueAsLong("Content-Length").orElse(-1L);
            }
            throw new IOException("获取大小失败 HTTP " + code);
        } catch (IOException e) {
            throw new DownloadException("获取资源大小失败: " + url, e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new DownloadException("获取资源大小被中断: " + url, e);
        }
    }

    @Override
    public byte[] fetch(String url, long start, long end) {
        URI uri = UrlValidator.validate(url);
        HttpRequest.Builder builder = HttpRequest.newBuilder(uri)
                .header("User-Agent", userAgent)
                .timeout(Duration.ofMinutes(5))
                .GET();
        if (end >= 0) {
            builder.header("Range", "bytes=" + start + "-" + end);
        }
        HttpRequest request = builder.build();
        try {
            HttpResponse<byte[]> response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());
            int code = response.statusCode();
            if (code != 206 && code != 200) {
                throw new IOException("HTTP " + code);
            }
            byte[] body = response.body();
            if (end >= 0 && code == 206) {
                long expected = end - start + 1;
                if (body.length != expected) {
                    throw new IOException("分块长度不符: 期望 " + expected + " 实际 " + body.length);
                }
            }
            return body;
        } catch (IOException e) {
            throw new DownloadException("分块下载失败 [" + start + "," + end + "]: " + url, e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new DownloadException("分块下载被中断: " + url, e);
        }
    }

    private static long parseTotalFromContentRange(String header) {
        if (header == null) {
            return -1L;
        }
        int slash = header.lastIndexOf('/');
        if (slash < 0 || slash == header.length() - 1) {
            return -1L;
        }
        String total = header.substring(slash + 1).trim();
        if (total.equals("*")) {
            return -1L;
        }
        try {
            return Long.parseLong(total);
        } catch (NumberFormatException e) {
            return -1L;
        }
    }
}
