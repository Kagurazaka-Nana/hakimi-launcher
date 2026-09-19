package com.minecraft.launcher.backend

/**
 * 前端框架阶段的占位实现：不访问网络、不触碰文件系统，
 * 仅向 UI 提供稳定的演示数据（对应 docs/export 首页视觉稿）。
 */
class StubLauncherBackend : LauncherBackend {

    override suspend fun getCurrentUsername(): String = "HakimiCat"

    override suspend fun loadVersions(): List<String> = listOf("1.21.1", "1.20.1", "1.19.4")

    override suspend fun loadHome(): HomeSnapshot = HomeSnapshot(
        welcomeTitle = "欢迎回来，旅行者！",
        welcomeSubtitle = "在方块的世界里，和猫咪一起开启新的冒险吧！",
        profileName = "HakimiCat",
        profileBadge = "👑",
        profileOnline = true,
        profileTagline = "用代码搭建属于自己的世界 —— hakimi",
        instanceName = "生存世界",
        instanceDescription = "和猫咪一起在方块世界中生存、建造、探索！",
        version = "1.21.1",
        loader = "Fabric",
        modeTags = listOf("生存模式", "单人", "Java 21"),
        quickActions = listOf(
            QuickAction("cat", "创建实例", 0xFF9B6FE0),
            QuickAction("folder", "导入实例", 0xFF5B8DEF),
            QuickAction("puzzle", "管理模组", 0xFF3FA34D),
            QuickAction("download", "前往下载", 0xFFF5A623),
        ),
        loadingPercent = 68,
        loadingText = "加载中…",
        loadingHint = "正在准备资源…请稍候~",
        recentPlay = RecentPlay("生存世界", "1.21.1 · Fabric", "3 小时前"),
        resourceStatus = listOf(
            ResourceRow("cube", "游戏文件", true, 0xFF6C5CE7),
            ResourceRow("puzzle", "加载器", true, 0xFF9B6FE0),
            ResourceRow("folder", "模组", true, 0xFFF5A623),
            ResourceRow("image", "资源包", true, 0xFFE86AA6),
        ),
    )

    override suspend fun refreshManifest() = Unit

    override suspend fun ensureVersionReady(versionId: String) = Unit

    override suspend fun launch(versionId: String) = Unit
}
