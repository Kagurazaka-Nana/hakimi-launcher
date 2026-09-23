package com.minecraft.launcher.download.mojang;

import com.minecraft.launcher.model.version.VersionMeta;

import java.nio.file.Path;
import java.util.List;

/**
 * 继承链合并后的版本：{@code meta} 为子 JSON 覆盖父 JSON 的合并结果
 * （libraries 子优先、按 group:artifact 去重），{@code effectiveJarId} 为实际客户端 JAR 所属版本 id，
 * {@code chain} 为从自身到根原版的 id 链，{@code jsonSource} 为自身版本 JSON 的缓存文件路径（安装提交时复制）。
 */
public record ResolvedVersion(VersionMeta meta, String effectiveJarId, List<String> chain, Path jsonSource) {

    public ResolvedVersion {
        chain = List.copyOf(chain);
    }
}
