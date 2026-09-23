package com.minecraft.launcher.download.mojang;

import com.minecraft.launcher.download.FileDownloader;
import com.minecraft.launcher.model.manifest.VersionInfo;
import com.minecraft.launcher.model.manifest.VersionManifest;
import com.minecraft.launcher.model.version.VersionMeta;
import com.minecraft.launcher.model.version.libraries.Library;
import com.minecraft.launcher.util.JsonUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * 单版本 JSON 服务：拉取/缓存 client.json，并沿 {@code inheritsFrom} 递归合并为 {@link ResolvedVersion}。
 *
 * 合并语义（docs/DownloadPipeline.md §3.3-3）：子 JSON 覆盖父 JSON；libraries 子优先、按 name 去重；
 * {@code jar} 字段指向复用的客户端 JAR 所属版本（fabric 等加载器复用原版 JAR）。
 */
public final class VersionJsonService {

    private static final int MAX_CHAIN_DEPTH = 10;

    private final DownloadProvider provider;
    private final FileDownloader downloader;
    private final Path cacheDir;

    public VersionJsonService(DownloadProvider provider, FileDownloader downloader, Path cacheDir) {
        this.provider = provider;
        this.downloader = downloader;
        this.cacheDir = cacheDir;
    }

    /** 拉取（或读缓存）指定清单条目的版本 JSON。 */
    public VersionMeta fetch(VersionInfo info) throws IOException {
        Path target = cacheDir.resolve(info.getId() + ".json");
        if (!Files.isRegularFile(target)) {
            IOException last = null;
            for (String url : provider.injectURLCandidates(info.getUrl())) {
                try {
                    download(url, target);
                    last = null;
                    break;
                } catch (IOException | RuntimeException e) {
                    last = e instanceof IOException ioe ? ioe : new IOException("候选 URL 下载失败: " + url, e);
                }
            }
            if (last != null) {
                throw last;
            }
        }
        return JsonUtils.readValue(target, VersionMeta.class);
    }

    /** 解析完整继承链：自身 → inheritsFrom → … → 原版根。 */
    public ResolvedVersion resolve(VersionManifest manifest, VersionInfo entry) throws IOException {
        Objects.requireNonNull(manifest, "manifest");
        Objects.requireNonNull(entry, "entry");

        VersionMeta merged = fetch(entry);
        List<String> chain = new ArrayList<>();
        chain.add(entry.getId());
        Set<String> visited = new HashSet<>();
        visited.add(entry.getId());
        String effectiveJar = merged.getJar();

        while (merged.getInheritsFrom() != null) {
            String parentId = merged.getInheritsFrom();
            if (!visited.add(parentId) || visited.size() > MAX_CHAIN_DEPTH) {
                throw new IllegalStateException("inheritsFrom 链过深或存在环: " + chain + " -> " + parentId);
            }
            VersionInfo parentInfo = manifest.findVersionById(parentId)
                    .orElseThrow(() -> new IOException("清单中找不到被继承的版本: " + parentId));
            VersionMeta parent = fetch(parentInfo);
            if (effectiveJar == null) {
                effectiveJar = parent.getJar();
            }
            merged = merge(merged, parent);
            chain.add(parentId);
        }

        if (effectiveJar == null) {
            // 无 jar 字段：客户端 JAR 属于继承链根（原版）
            effectiveJar = chain.get(chain.size() - 1);
        }
        return new ResolvedVersion(merged, effectiveJar, chain);
    }

    private void download(String url, Path target) throws IOException {
        Path parent = target.toAbsolutePath().getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        Path tmp = target.resolveSibling(target.getFileName() + ".tmp");
        downloader.downloadBlocking(url, tmp);
        JsonUtils.readValue(tmp, VersionMeta.class); // 结构校验通过才提交缓存
        Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING);
    }

    /** 子覆盖父的字段合并；libraries 子在前、按 Maven 坐标（group:artifact，不含版本）去重。 */
    static VersionMeta merge(VersionMeta child, VersionMeta parent) {
        List<Library> libraries = new ArrayList<>();
        Set<String> seenCoordinates = new HashSet<>();
        if (child.getLibraries() != null) {
            for (Library l : child.getLibraries()) {
                if (l.getName() == null || seenCoordinates.add(coordinateKey(l.getName()))) {
                    libraries.add(l);
                }
            }
        }
        if (parent.getLibraries() != null) {
            for (Library l : parent.getLibraries()) {
                if (l.getName() == null || seenCoordinates.add(coordinateKey(l.getName()))) {
                    libraries.add(l);
                }
            }
        }
        return VersionMeta.builder()
                .clientVersion(child.getClientVersion())
                .versionType(child.getVersionType() != null ? child.getVersionType() : parent.getVersionType())
                .time(child.getTime() != null ? child.getTime() : parent.getTime())
                .releaseTime(child.getReleaseTime() != null ? child.getReleaseTime() : parent.getReleaseTime())
                .mainClass(child.getMainClass() != null ? child.getMainClass() : parent.getMainClass())
                .assets(child.getAssets() != null ? child.getAssets() : parent.getAssets())
                .assetIndex(child.getAssetIndex() != null ? child.getAssetIndex() : parent.getAssetIndex())
                .downloads(child.getDownloads() != null ? child.getDownloads() : parent.getDownloads())
                .javaVersion(child.getJavaVersion() != null ? child.getJavaVersion() : parent.getJavaVersion())
                .libraries(libraries)
                .arguments(child.getArguments() != null ? child.getArguments() : parent.getArguments())
                .logging(child.getLogging() != null ? child.getLogging() : parent.getLogging())
                .complianceLevel(child.getComplianceLevel() != null ? child.getComplianceLevel() : parent.getComplianceLevel())
                .minimumLauncherVersion(child.getMinimumLauncherVersion() != null
                        ? child.getMinimumLauncherVersion() : parent.getMinimumLauncherVersion())
                .jar(child.getJar())
                .inheritsFrom(null)
                .build();
    }

    /** Maven 坐标 group:artifact:version[:classifier] → group:artifact 作为去重键。 */
    private static String coordinateKey(String name) {
        String[] parts = name.split(":");
        return parts.length >= 2 ? parts[0] + ":" + parts[1] : name;
    }
}
