package com.minecraft.launcher.download.mojang;

import com.minecraft.launcher.download.FileDownloader;
import com.minecraft.launcher.model.manifest.VersionManifest;
import com.minecraft.launcher.util.JsonUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.time.Instant;

/**
 * 版本清单服务：经候选 URL 拉取 version_manifest_v2，落盘缓存并按 TTL 复用。
 * 网络全部失败时回退到过期缓存（离线可用），缓存也无效才抛出最后一个异常。
 */
public final class ManifestService {

    private final DownloadProvider provider;
    private final FileDownloader downloader;
    private final Path cacheFile;
    private final Duration ttl;

    public ManifestService(DownloadProvider provider, FileDownloader downloader, Path cacheFile, Duration ttl) {
        this.provider = provider;
        this.downloader = downloader;
        this.cacheFile = cacheFile;
        this.ttl = ttl;
    }

    /** 获取清单：TTL 内直接用缓存，否则按候选 URL 顺序拉取。 */
    public VersionManifest fetch() throws IOException {
        if (isFresh()) {
            VersionManifest cached = tryReadCache();
            if (cached != null) {
                return cached;
            }
        }
        IOException last = null;
        for (String url : provider.getVersionListUrls()) {
            try {
                return download(url);
            } catch (IOException | RuntimeException e) {
                last = e instanceof IOException ioe ? ioe : new IOException("候选 URL 下载失败: " + url, e);
            }
        }
        VersionManifest stale = tryReadCache();
        if (stale != null) {
            return stale;
        }
        if (last != null) {
            throw last;
        }
        throw new IOException("Provider 未给出任何版本清单 URL");
    }

    private VersionManifest download(String url) throws IOException {
        Path parent = cacheFile.toAbsolutePath().getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        Path tmp = cacheFile.resolveSibling(cacheFile.getFileName() + ".tmp");
        downloader.downloadBlocking(url, tmp);
        VersionManifest manifest = JsonUtils.readValue(tmp, VersionManifest.class); // 先验结构再提交，坏数据不覆盖缓存
        Files.move(tmp, cacheFile, StandardCopyOption.REPLACE_EXISTING);
        return manifest;
    }

    private VersionManifest tryReadCache() {
        try {
            return JsonUtils.readValue(cacheFile, VersionManifest.class);
        } catch (IOException e) {
            return null;
        }
    }

    private boolean isFresh() {
        if (!Files.isRegularFile(cacheFile)) {
            return false;
        }
        try {
            Instant modified = Files.getLastModifiedTime(cacheFile).toInstant();
            return Duration.between(modified, Instant.now()).compareTo(ttl) < 0;
        } catch (IOException e) {
            return false;
        }
    }
}
