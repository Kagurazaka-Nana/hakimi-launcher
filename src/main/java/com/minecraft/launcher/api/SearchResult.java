package com.minecraft.launcher.api;

import java.util.List;

/**
 * 统一搜索结果模型（跨 Modrinth / CurseForge / MC百科）。
 *
 * @param id          来源内唯一标识
 * @param name        显示名
 * @param summary     简介
 * @param source      来源标识：modrinth / curseforge / mcmod
 * @param projectType 类型：mod / modpack / resourcepack / shader / datapack / plugin
 * @param downloads   下载量（未知为 -1）
 * @param categories  分类标签
 * @param iconUrl     图标地址（可为空）
 */
public record SearchResult(
        String id,
        String name,
        String summary,
        String source,
        String projectType,
        long downloads,
        List<String> categories,
        String iconUrl
) {
}
