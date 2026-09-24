package com.minecraft.launcher.download.mojang;

import java.nio.file.Path;
import java.util.List;

/**
 * 安装计划：待下载文件清单、版本 JSON 的目标落盘位置、实际客户端 JAR 所属版本 id。
 *
 * @param versionJsonTarget versions/&lt;id&gt;/&lt;id&gt;.json（内容来自 VersionJsonService 缓存，安装时复制）
 */
public record InstallPlan(List<FileEntry> files, Path versionJsonTarget, String effectiveJarId) {

    public InstallPlan {
        files = List.copyOf(files);
    }
}
