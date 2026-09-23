package com.minecraft.launcher.download.mojang;

import com.minecraft.launcher.model.version.VersionMeta;

import java.util.List;

/**
 * 继承链合并后的版本：{@code meta} 为子 JSON 覆盖父 JSON 的合并结果
 * （libraries 子优先、按 name 去重），{@code effectiveJarId} 为实际客户端 JAR 所属版本 id，
 * {@code chain} 为从自身到根原版的 id 链。
 */
public record ResolvedVersion(VersionMeta meta, String effectiveJarId, List<String> chain) {

    public ResolvedVersion {
        chain = List.copyOf(chain);
    }
}
