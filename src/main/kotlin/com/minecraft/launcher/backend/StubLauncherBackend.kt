package com.minecraft.launcher.backend

/**
 * 前端框架阶段的占位实现：不访问网络、不触碰文件系统，
 * 仅向 UI 提供稳定的演示数据。
 */
class StubLauncherBackend : LauncherBackend {

    override suspend fun getCurrentUsername(): String = "Steve"

    override suspend fun loadVersions(): List<String> = listOf("latest-release")

    override suspend fun loadHome(): HomeSnapshot = HomeSnapshot(
        username = "Alex xx",
        profileType = "本地档案",
        worldName = "杉木谷",
        worldDescription = "一个有山、有河，还有无限可能的宁静世界。",
        worldMode = "生存",
        ready = true,
        instanceLabel = "杉木谷 · Fabric 1.21.1",
        launchVersion = "1.21.1",
        modTags = listOf("钠", "光影", "机械动力"),
        modCount = 48,
        instanceSize = "2.8 GB",
        memoryUsedGb = 5.2,
        memoryTotalGb = 16.0,
        javaVersion = "17.0.10",
        javaOk = true,
        loaderName = "Fabric",
        loaderVersion = "1.21.1",
        loaderCompatible = true,
        recentActivities = listOf(
            ActivityItem("puzzle", "已加载 48 个模组", "10:42", 0xFF4CAF7D),
            ActivityItem("download", "已下载 3 个文件", "10:37", 0xFF6FA8DC),
            ActivityItem("cube", "已更新实例", "10:21", 0xFFB39DDB),
            ActivityItem("mountain", "已创建世界", "09:58", 0xFFF6C453),
            ActivityItem("folder", "已导入资源包", "09:44", 0xFFF4795B),
        ),
    )

    override suspend fun refreshManifest() = Unit

    override suspend fun ensureVersionReady(versionId: String) = Unit

    override suspend fun launch(versionId: String) = Unit
}
