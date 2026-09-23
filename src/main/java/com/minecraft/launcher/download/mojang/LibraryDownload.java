package com.minecraft.launcher.download.mojang;

import java.util.List;

/**
 * LibraryFilter 的产物：一个需要下载并放入 libraries 目录的构件。
 *
 * @param name Maven 坐标（group:artifact:version[:classifier]）
 * @param relativePath 相对 libraries 目录的路径（即 artifact.path）
 * @param url 下载 URL（已按 library.url 或默认仓库解析）
 * @param sha1 期望摘要（可空）
 * @param size 期望大小（-1 未知）
 * @param nativeArtifact 是否为原生库 classifier 构件（启动前需解压）
 * @param extractExclude 解压排除路径（仅原生库有意义）
 */
public record LibraryDownload(
        String name,
        String relativePath,
        String url,
        String sha1,
        long size,
        boolean nativeArtifact,
        List<String> extractExclude) {

    public LibraryDownload {
        extractExclude = extractExclude == null ? List.of() : List.copyOf(extractExclude);
    }
}
