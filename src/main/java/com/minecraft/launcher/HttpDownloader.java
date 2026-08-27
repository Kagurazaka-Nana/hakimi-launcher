package com.minecraft.launcher;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;

public class HttpDownloader {
    private final HttpClient httpClient;

    private final int connectTimeout;
    private final int readTimeout;
    private final int maxRetries;
    private final String userAgent;

    public int getConnectTimeout() {
        return connectTimeout;
    }
    public int getReadTimeout() {
        return readTimeout;
    }
    public int getMaxRetries() {
        return maxRetries;
    }
    public String getUserAgent() {
        return userAgent;
    }

    public HttpDownloader () {
        this(10, 30, 3, "MinecraftLauncher/1.0 (Java 21)");
    }

    public HttpDownloader(String userAgent) {
        this(10, 30, 3, userAgent);
    }

    public HttpDownloader(int connectTimeout, int readTimeout, int maxRetries, String userAgent) {
        this.connectTimeout = connectTimeout;
        this.readTimeout = readTimeout;
        this.maxRetries = maxRetries;
        this.userAgent = userAgent;

        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(this.connectTimeout))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    public void downloadFile(String url, Path fileDestination) throws IOException, InterruptedException {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(readTimeout))
                .setHeader("User-Agent", userAgent)
                .build();

        HttpResponse<InputStream> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofInputStream());

        int code = resp.statusCode();

        if (code < 200 || code >= 300) {
            try (InputStream errStream = resp.body()) {
                String errorBody = new String(errStream.readAllBytes(), StandardCharsets.UTF_8);
                String preview = errorBody.length() > 200 ? errorBody.substring(0, 200) + "..." : errorBody;
                throw new IOException("HTTP " + code + " Error: " + preview);
            }
        }

        if (fileDestination.getParent() != null) {
            Files.createDirectories(fileDestination.getParent());
        }

        try (InputStream in = resp.body()) {
            Files.copy(in, fileDestination, StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception e) {
            Files.deleteIfExists(fileDestination);
            throw e;
        }
    }

    public String downloadString(String url) throws IOException, InterruptedException {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(readTimeout))
                .setHeader("User-Agent", userAgent)
                .build();
        HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        int code = resp.statusCode();
        if (code < 200 || code >= 300) {
            throw new IOException("HTTP " + code + " Failed. Server Response: " + resp.body());
        }

        return resp.body();
    }

    private void retryHttpRequest() {
    }
}
