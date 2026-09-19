package com.minecraft.launcher.backend

/**
 * 前端框架阶段的占位实现：不访问网络、不触碰文件系统，
 * 仅向 UI 提供稳定的演示数据（对应 docs/export 视觉稿）。
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

    override suspend fun loadInstances(): List<InstanceItem> = listOf(
        InstanceItem("生存世界", "1.21.1", "Fabric", "生存模式", "单人",
            "和猫咪一起在方块世界中生存，建造，探索！", true, "2025-05-10 14:32", "hakimi"),
        InstanceItem("整合包冒险", "1.20.1", "Forge", "冒险模式", "多人",
            "大型整合包，新增数百个模组，带你体验不一样的冒险！", false, "2025-05-08 19:26", "小黑喵"),
        InstanceItem("原版建筑", "1.19.4", "Vanilla", "创造模式", "单人",
            "纯净原版，专注建筑与美学，搭建属于你的梦幻家园。", false, "2025-05-03 16:20", "建筑猫"),
    )

    override suspend fun loadDownloads(): DownloadsSnapshot = DownloadsSnapshot(
        categories = listOf("游戏版本", "整合包", "模组", "资源包", "光影"),
        selectedCategory = "游戏版本",
        items = listOf(
            DownloadItem("Minecraft 1.21.1", "最新", "1.21.1", "Fabric / Forge",
                "全新的世界生成机制，更多生物与方块，带来更丰富的探索体验。", "859.2 MB"),
            DownloadItem("Minecraft 1.20.4", "稳定版", "1.20.4", "Fabric / Forge / Quilt",
                "经典的稳定版本，适合长期生存与多人联机。", "801.7 MB"),
            DownloadItem("典范整合包 2.4.7", "热门", "1.20.1", "Forge",
                "集成了科技、魔法、冒险等多种玩法，适合喜欢探索的玩家。", "3.6 GB"),
            DownloadItem("玩家必备模组包 1.8.9", "推荐", "1.20.1", "Forge / Fabric",
                "包含小地图、背包整理、性能优化等实用模组，提升游戏体验。", "248.5 MB"),
            DownloadItem("BSL 光影 8.1.02", "光影", "1.20.1", "OptiFine",
                "真实光影效果，改进的水面与光照，带来更沉浸的视觉体验。", "248.5 MB"),
        ),
        queue = listOf(
            DownloadTask("Minecraft 1.21.1", 68, "下载中", "582.3 MB / 859.2 MB · 2.1 MB/s", "pause"),
            DownloadTask("典范整合包 2.4.7", 25, "下载中", "921.4 MB / 3.6 GB · 1.8 MB/s", "pause"),
            DownloadTask("玩家必备模组包 1.8.9", null, "下载完成", "248.5 MB", "install"),
            DownloadTask("BSL 光影 8.1.02", null, "等待中", "64.3 MB", "start"),
        ),
    )

    override suspend fun loadSettings(): SettingsSnapshot = SettingsSnapshot(
        theme = "浅色",
        language = "简体中文",
        javaPath = "C:\\Program Files\\Java\\jdk-21",
        javaVersion = "21.0.3 · 64 位",
        maxMemoryMb = 4096,
        memoryMinMb = 512,
        memoryMaxMb = 8192,
        downloadSource = "官方源（推荐）",
        concurrency = 4,
        concurrencyMin = 1,
        concurrencyMax = 16,
        jvmArgs = "-XX:+UseG1GC -XX:MaxGCPauseMillis=200",
        debugMode = false,
        account = "已登录：hakimi（离线模式）",
        privacy = "不收集使用数据 · 仅本地存储",
    )

    override suspend fun refreshManifest() = Unit

    override suspend fun ensureVersionReady(versionId: String) = Unit

    override suspend fun launch(versionId: String) = Unit
}
