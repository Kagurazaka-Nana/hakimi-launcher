package com.minecraft.launcher.download.mojang;

import com.minecraft.launcher.model.version.downloads.DownloadType;

import java.util.ArrayList;
import java.util.List;

/**
 * 由解析结果生成安装计划：客户端 JAR（downloads.client）+ 已过滤的 libraries 构件，
 * URL 统一经 DownloadProvider 做候选改写（镜像/回退在此注入）。
 */
public final class InstallPlanner {

    private final DownloadProvider provider;
    private final GameLayout layout;

    public InstallPlanner(DownloadProvider provider, GameLayout layout) {
        this.provider = provider;
        this.layout = layout;
    }

    public InstallPlan plan(ResolvedVersion resolved, List<LibraryDownload> libraries) {
        List<FileEntry> files = new ArrayList<>();

        DownloadType client = resolved.meta().getDownloads() == null
                ? null
                : resolved.meta().getDownloads().getClient();
        if (client != null && client.getUrl() != null) {
            files.add(new FileEntry(
                    provider.injectURLCandidates(client.getUrl()),
                    layout.versionJar(resolved.effectiveJarId()),
                    client.getSha1(),
                    client.getSize() == null ? -1 : client.getSize()));
        }

        for (LibraryDownload lib : libraries) {
            files.add(new FileEntry(
                    provider.injectURLCandidates(lib.url()),
                    layout.library(lib.relativePath()),
                    lib.sha1(),
                    lib.size()));
        }

        return new InstallPlan(files, layout.versionJson(resolved.meta().getClientVersion()), resolved.effectiveJarId());
    }
}
