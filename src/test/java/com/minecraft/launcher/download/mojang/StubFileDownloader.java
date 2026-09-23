package com.minecraft.launcher.download.mojang;

import com.minecraft.launcher.download.BitDownloader;
import com.minecraft.launcher.download.FileDownloader;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/** 测试替身：按 URL 返回预置 JSON 内容，记录下载次数。 */
final class StubFileDownloader implements FileDownloader {

    private final Map<String, String> responses = new HashMap<>();
    private final Map<String, Integer> hits = new HashMap<>();

    StubFileDownloader respond(String url, String content) {
        responses.put(url, content);
        return this;
    }

    int hitCount(String url) {
        return hits.getOrDefault(url, 0);
    }

    @Override
    public BitDownloader.DownloadJob download(String url, Path into) {
        throw new UnsupportedOperationException("测试替身仅提供同步下载");
    }

    @Override
    public Path downloadBlocking(String url, Path into) {
        hits.merge(url, 1, Integer::sum);
        String content = responses.get(url);
        if (content == null) {
            throw new RuntimeException(new IOException("stub 未预置该 URL: " + url));
        }
        try {
            Path parent = into.toAbsolutePath().getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Files.writeString(into, content, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return into;
    }

    @Override
    public void setProxy(String host, int port) {
        // no-op
    }

    @Override
    public void close() {
        // no-op
    }
}
