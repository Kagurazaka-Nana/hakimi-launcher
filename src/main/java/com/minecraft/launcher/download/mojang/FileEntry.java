package com.minecraft.launcher.download.mojang;

import java.nio.file.Path;
import java.util.List;

/**
 * 安装计划中的一个文件条目：按候选 URL 顺序尝试下载到 target，
 * 完成后用 sha1（可空则不校验）验证，原子改名。size 为 -1 表示未知。
 */
public record FileEntry(List<String> candidateUrls, Path target, String sha1, long size) {

    public FileEntry {
        candidateUrls = List.copyOf(candidateUrls);
        if (candidateUrls.isEmpty()) {
            throw new IllegalArgumentException("FileEntry 至少需要一个候选 URL");
        }
    }
}
