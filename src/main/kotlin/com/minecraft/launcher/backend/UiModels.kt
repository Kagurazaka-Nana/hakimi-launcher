package com.minecraft.launcher.backend

/** 统一资源列表的分类种类。 */
enum class ResourceKind(val label: String, val categories: List<String>) {
    MODS("Mods", listOf("全部", "性能优化", "冒险", "建筑", "科技", "魔法")),
    RESOURCE_PACK("资源包", listOf("全部", "写实", "卡通", "高对比", "低像素")),
    DATA_PACK("数据包", listOf("全部", "玩法", "地图", "配方", "战利品")),
    SHADER("光影", listOf("全部", "写实", "卡通", "性能向", "电影感")),
    MODPACK("整合包", listOf("全部", "科技", "魔法", "冒险", "硬核")),
    PLUGIN("插件", listOf("全部", "经济", "权限", "小游戏", "管理")),
    SERVER("服务器", listOf("全部", "生存", "空岛", "小游戏", "RPG")),
}

/** 资源条目（Mods / 资源包 / 数据包 / 光影 / 整合包 / 插件 通用）。 */
data class ResourceItem(
    val id: String,
    val name: String,
    val summary: String,
    val description: String,
    val version: String,
    val downloads: Long,
    val categories: List<String>,
    val author: String,
    val iconUrl: String? = null,
    val enabled: Boolean = false,
)

/** 系统运行监控指标（真实采样；速率由相邻两次采样差值换算）。 */
data class SystemStats(
    val cpuPercent: Int,
    val memUsedGb: Double,
    val memTotalGb: Double,
    /** 显存占用 MB；null = 本机无可用查询接口（UI 隐藏该项）。 */
    val vramUsedMb: Long? = null,
    val vramTotalMb: Long? = null,
    val netDownBps: Long = 0,
    val netUpBps: Long = 0,
    val diskReadBps: Long = 0,
    val diskWriteBps: Long = 0,
)

/** 下载队列中的单个任务（底部下载指示器展示用）。 */
data class DownloadTask(
    val name: String,
    /** 0f..1f */
    val fraction: Float,
)

/** 皮肤信息。 */
data class SkinInfo(
    val id: String,
    val name: String,
    val selected: Boolean,
)

/** 服务器信息。 */
data class ServerInfo(
    val id: String,
    val name: String,
    val address: String,
    val version: String,
    val online: Boolean,
    val players: Int,
)

/** 截图信息。 */
data class ScreenshotInfo(
    val id: String,
    val name: String,
    val time: String,
    val size: String,
)

/** Wiki 文章。 */
data class WikiArticle(
    val id: String,
    val title: String,
    val category: String,
    val excerpt: String,
    val updated: String,
)

/** 游戏版本选项。 */
data class GameVersion(val id: String, val type: String)

/** 加载器选项。 */
data class LoaderOption(val id: String, val label: String)

/** 设置页快照。 */
data class SettingsSnapshot(
    val theme: String,
    val language: String,
    val javaPath: String,
    val javaVersion: String,
    val maxMemoryMb: Int,
    val memoryMinMb: Int,
    val memoryMaxMb: Int,
    val downloadSource: String,
    val concurrency: Int,
    val concurrencyMin: Int,
    val concurrencyMax: Int,
    val jvmArgs: String,
    val debugMode: Boolean,
    val account: String,
    val privacy: String,
)

/** 首页快照。 */
data class HomeSnapshot(
    val welcomeTitle: String,
    val welcomeSubtitle: String,
    val profileName: String,
    val profileOnline: Boolean,
    val profileTagline: String,
    val instanceName: String,
    val instanceDescription: String,
    val version: String,
    val loader: String,
    val javaVersion: String,
    val modeTags: List<String>,
    val ready: Boolean,
    val resourceCount: Int,
    val totalSizeGb: Double,
    val lastPlayed: String,
    val systemHealthy: Boolean,
    val recentInstances: List<RecentInstance>,
)

/** 最近游玩条目。 */
data class RecentInstance(
    val name: String,
    val version: String,
    val loader: String,
    val time: String,
)
