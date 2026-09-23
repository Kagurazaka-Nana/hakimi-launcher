package com.minecraft.launcher.backend

import com.minecraft.launcher.download.BitFileDownloader
import com.minecraft.launcher.download.DownloadConfig
import com.minecraft.launcher.download.DownloadManager
import com.minecraft.launcher.download.FileDownloader
import com.minecraft.launcher.monitor.SystemMetricsMonitor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

/**
 * 前端框架阶段的占位实现：不访问网络、不触碰文件系统，
 * 仅向 UI 提供稳定的演示数据。真实逻辑后续替换本实现。
 *
 * 例外：系统指标走 [SystemMetricsMonitor] 的真实采样（只读 OS 计数器），
 * 以便首页状态栏展示实际负载。
 */
class StubLauncherBackend : LauncherBackend {

    override suspend fun loadHome(): HomeSnapshot = HomeSnapshot(
        welcomeTitle = "欢迎回来，旅行者！",
        welcomeSubtitle = "在方块的世界里，和猫咪一起开启新的冒险吧！",
        profileName = "HakimiCat",
        profileOnline = true,
        profileTagline = "用代码搭建属于自己的世界 —— hakimi",
        instanceName = "生存世界",
        instanceDescription = "和猫咪一起在方块世界中生存、建造、探索！",
        version = "1.21.1",
        loader = "Fabric",
        javaVersion = "Java 21",
        modeTags = listOf("生存模式", "单人", "Vanilla+"),
        ready = true,
        resourceCount = 48,
        totalSizeGb = 2.8,
        lastPlayed = "2025-08-30 18:42",
        systemHealthy = true,
        recentInstances = listOf(
            RecentInstance("生存世界", "1.21.1", "Fabric", "2 小时前"),
            RecentInstance("建筑服", "1.20.4", "Forge", "1 天前"),
            RecentInstance("Mod 测试", "1.21.1", "Fabric", "3 天前"),
            RecentInstance("空岛", "1.20.1", "Quilt", "5 天前"),
        ),
    )

    private val monitor = SystemMetricsMonitor()

    override fun systemStatsFlow(): Flow<SystemStats> = flow {
        while (true) {
            val s = monitor.sample()
            emit(
                SystemStats(
                    cpuPercent = s.cpuPercent(),
                    memUsedGb = s.memUsedGb(),
                    memTotalGb = s.memTotalGb(),
                    vramUsedMb = s.vramUsedMb(),
                    vramTotalMb = s.vramTotalMb(),
                    netDownBps = s.netDownBps(),
                    netUpBps = s.netUpBps(),
                    diskReadBps = s.diskReadBps(),
                    diskWriteBps = s.diskWriteBps(),
                ),
            )
            delay(1_000)
        }
    }.flowOn(Dispatchers.IO)

    private val sharedDownloader: FileDownloader = BitFileDownloader(
        // 本机开发默认代理 127.0.0.1:10808，可在设置页修改
        DownloadConfig(proxyHost = "127.0.0.1", proxyPort = 10808),
    )

    private val downloads = DownloadManager(sharedDownloader)

    @Volatile
    private var proxyEnabled = true

    @Volatile
    private var proxyHost = "127.0.0.1"

    @Volatile
    private var proxyPort = 10808

    private val installGameDir: java.nio.file.Path = java.nio.file.Path.of("launcherTest", ".minecraft")
    private val installCacheDir: java.nio.file.Path = java.nio.file.Path.of("temp", "install-cache")

    @Volatile
    private var downloadSourceKey = "official"

    private fun currentProvider(): com.minecraft.launcher.download.mojang.DownloadProvider =
        when (downloadSourceKey) {
            "bmclapi" -> com.minecraft.launcher.download.mojang.BmclApiProvider()
            "auto" -> com.minecraft.launcher.download.mojang.AutoProvider(
                listOf(
                    com.minecraft.launcher.download.mojang.BmclApiProvider(),
                    com.minecraft.launcher.download.mojang.MojangProvider(),
                ),
            )
            else -> com.minecraft.launcher.download.mojang.MojangProvider()
        }

    override fun setDownloadSource(source: String) {
        require(source in setOf("official", "bmclapi", "auto")) { "未知下载源: $source" }
        downloadSourceKey = source
    }

    override fun startInstall(versionId: String) {
        val task = downloads.trackExternal("安装 $versionId", "mojang://version/$versionId")
        val provider = currentProvider()
        val layout = com.minecraft.launcher.download.mojang.GameLayout(installGameDir)
        val service = com.minecraft.launcher.download.mojang.InstallationService(
            com.minecraft.launcher.download.mojang.ManifestService(
                provider, sharedDownloader, installCacheDir.resolve("version_manifest_v2.json"),
                java.time.Duration.ofHours(1),
            ),
            com.minecraft.launcher.download.mojang.VersionJsonService(
                provider, sharedDownloader, installCacheDir.resolve("version-jsons"),
            ),
            provider,
            sharedDownloader,
            layout,
            installCacheDir,
        )
        Thread.ofVirtual().start {
            try {
                service.install(versionId) { p ->
                    val fraction = if (p.bytesTotal > 0) p.bytesDone.toFloat() / p.bytesTotal else 0f
                    task.update(fraction)
                }
                task.complete()
            } catch (e: Exception) {
                task.fail()
            }
        }
    }

    override fun downloadTasksFlow(): Flow<List<DownloadTask>> = downloads.tasksFlow()

    override fun startDownload(url: String, into: java.nio.file.Path): String = downloads.start(url, into)

    override fun cancelDownload(id: String) = downloads.cancel(id)

    override fun setProxy(enabled: Boolean, host: String, port: Int) {
        proxyEnabled = enabled
        proxyHost = host
        proxyPort = port
        downloads.setProxy(if (enabled) host.ifBlank { null } else null, port)
    }

    override suspend fun loadResources(kind: ResourceKind): List<ResourceItem> = when (kind) {
        ResourceKind.MODS -> listOf(
            item("sodium", "Sodium 钠", "高性能渲染引擎，大幅提升帧数", "1.21.1", 2_400_000, listOf("性能优化"), "https://upload.wikimedia.org/wikipedia/commons/1/16/Minecraft_Turkey_Skin.png"),
            item("iris", "Iris 光影加载器", "在 Fabric 上运行 Iris 光影", "1.7.0", 1_200_000, listOf("性能优化", "光影"), "https://upload.wikimedia.org/wikipedia/commons/d/d4/Grass_Block_%28texture%29_MCJE.png"),
            item("jei", "Just Enough Items", "物品与合成表查询", "1.24.0", 5_000_000, listOf("建筑")),
            item("create", "Create 机械动力", "蒸汽朋克机械自动化", "0.5.1", 3_100_000, listOf("科技")),
            item("journeymap", "JourneyMap 小地图", "实时小地图与全屏地图", "5.9.0", 2_800_000, listOf("冒险")),
        )
        ResourceKind.RESOURCE_PACK -> listOf(
            item("faithful", "Faithful 32x", "高清重制原版材质", "1.21", 900_000, listOf("写实")),
            item("pixelpower", "Pixel Power 16x", "精致低像素风格", "1.20", 320_000, listOf("低像素")),
            item("cartoon", "卡通渲染包", "明亮可爱的卡通风格", "1.21", 150_000, listOf("卡通")),
        )
        ResourceKind.DATA_PACK -> listOf(
            item("recipes", "自定义配方", "调整合成配方的数据包", "1.21", 80_000, listOf("配方")),
            item("loot", "战利品表编辑", "自定义怪物掉落", "1.21", 60_000, listOf("战利品")),
        )
        ResourceKind.SHADER -> listOf(
            item("bsl", "BSL Shaders", "均衡画质与性能", "8.1", 1_500_000, listOf("电影感")),
            item("complementary", "Complementary", "兼容大多数模组", "4.9", 1_100_000, listOf("写实")),
            item("seus", "SEUS PTGI", "光线追踪风格光影", "1.0", 700_000, listOf("写实")),
        )
        ResourceKind.MODPACK -> listOf(
            item("vault", "典范整合包", "科技、魔法、冒险综合", "2.4.7", 400_000, listOf("科技", "魔法")),
            item("adventure", "玩家必备包", "实用模组合集", "1.8.9", 260_000, listOf("冒险")),
        )
        ResourceKind.PLUGIN -> listOf(
            item("essentials", "EssentialsX", "服务器基础指令插件", "2.20", 500_000, listOf("管理")),
            item("luckperms", "LuckPerms", "权限管理插件", "5.4", 480_000, listOf("权限")),
        )
        ResourceKind.SERVER -> listOf(
            item("vanilla", "原版服务端", "纯净多人服务端", "1.21.1", 300_000, listOf("生存")),
            item("paper", "Paper 服务端", "高性能 Bukkit 分支", "1.21", 350_000, listOf("生存")),
        )
    }

    override suspend fun loadVersions(): List<GameVersion> = listOf(
        GameVersion("1.21.1", "正式版"),
        GameVersion("1.20.6", "正式版"),
        GameVersion("1.20.1", "正式版"),
        GameVersion("1.19.4", "正式版"),
        GameVersion("24w21a", "快照"),
    )

    override suspend fun loadLoaders(): List<LoaderOption> = listOf(
        LoaderOption("vanilla", "原版"),
        LoaderOption("fabric", "Fabric"),
        LoaderOption("forge", "Forge"),
        LoaderOption("quilt", "Quilt"),
        LoaderOption("neoforge", "NeoForge"),
    )

    override suspend fun loadSkins(): List<SkinInfo> = listOf(
        SkinInfo("steve", "Steve", true),
        SkinInfo("alex", "Alex", false),
        SkinInfo("cat", "猫咪套装", false),
        SkinInfo("ninja", "忍者", false),
    )

    override suspend fun loadServers(): List<ServerInfo> = listOf(
        ServerInfo("s1", "hakimi 生存服", "play.hakimi.example", "1.21.1", true, 42),
        ServerInfo("s2", "空岛乐园", "sky.hakimi.example", "1.20.1", true, 17),
        ServerInfo("s3", "小游戏大厅", "mini.hakimi.example", "1.21", false, 0),
    )

    override suspend fun loadScreenshots(): List<ScreenshotInfo> = (1..6).map { i ->
        ScreenshotInfo("shot$i", "screenshot_${2025050 + i}.png", "2025-05-1${i % 9} 14:0$i", "${2 + i}.${i} MB")
    }

    override suspend fun loadWiki(): List<WikiArticle> = listOf(
        WikiArticle("w1", "如何创建第一个实例", "入门", "从选择版本到启动游戏的完整流程。", "2 天前"),
        WikiArticle("w2", "Fabric 与 Forge 的区别", "加载器", "两种主流加载器的生态与兼容性对比。", "5 天前"),
        WikiArticle("w3", "光影安装指南", "光影", "在 Fabric / OptiFine 下安装光影的方法。", "1 周前"),
        WikiArticle("w4", "常见问题排查", "支持", "启动失败、白屏、崩溃的排查思路。", "3 天前"),
    )

    override suspend fun loadSettings(): SettingsSnapshot = SettingsSnapshot(
        theme = "浅色",
        language = "简体中文",
        javaPath = "C:\\Program Files\\Java\\jdk-21",
        javaVersion = "21.0.3 · 64 位",
        maxMemoryMb = 4096,
        memoryMinMb = 512,
        memoryMaxMb = 8192,
        downloadSource = downloadSourceKey,
        concurrency = 4,
        concurrencyMin = 1,
        concurrencyMax = 16,
        jvmArgs = "-XX:+UseG1GC -XX:MaxGCPauseMillis=200",
        debugMode = false,
        account = "已登录：hakimi（离线模式）",
        privacy = "不收集使用数据 · 仅本地存储",
        proxyEnabled = proxyEnabled,
        proxyHost = proxyHost,
        proxyPort = proxyPort,
    )

    override suspend fun createInstance(name: String, version: String, loader: String) = Unit

    override suspend fun launch(instanceName: String) = Unit

    private fun item(id: String, name: String, summary: String, version: String, downloads: Long, cats: List<String>, iconUrl: String? = null) =
        ResourceItem(
            id = id,
            name = name,
            summary = summary,
            description = "$summary。由社区维护，兼容当前实例版本，支持一键启用 / 禁用。",
            version = version,
            downloads = downloads,
            categories = cats,
            author = "hakimi",
            iconUrl = iconUrl,
            enabled = false,
        )
}
