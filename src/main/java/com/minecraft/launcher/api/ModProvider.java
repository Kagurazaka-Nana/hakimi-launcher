package com.minecraft.launcher.api;

import java.util.List;

/** 资源提供方（Modrinth / CurseForge / MC百科）统一接口。 */
public interface ModProvider {

    /** 来源标识。 */
    String name();

    /**
     * 搜索资源。
     *
     * @param query       关键词
     * @param projectType 类型（mod / modpack / resourcepack / shader / datapack / plugin）
     * @param limit       返回条数上限
     */
    List<SearchResult> search(String query, String projectType, int limit);
}
