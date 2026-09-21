package com.minecraft.launcher.backend

/**
 * 后端能力接口：UI 只依赖这里定义的操作，
 * 真实下载 / 登录 / 启动 / 扫描等实现后续接入，UI 不直接写业务细节。
 */
interface LauncherBackend {

    /** 首页快照 */
    suspend fun loadHome(): HomeSnapshot

    /** 系统运行监控（内存、显存、CPU、磁盘 IO、网络） */
    suspend fun loadSystemStats(): SystemStats

    /** 指定分类的资源列表 */
    suspend fun loadResources(kind: ResourceKind): List<ResourceItem>

    /** 可选游戏版本 */
    suspend fun loadVersions(): List<GameVersion>

    /** 可选加载器 */
    suspend fun loadLoaders(): List<LoaderOption>

    /** 皮肤列表 */
    suspend fun loadSkins(): List<SkinInfo>

    /** 服务器列表 */
    suspend fun loadServers(): List<ServerInfo>

    /** 截图列表 */
    suspend fun loadScreenshots(): List<ScreenshotInfo>

    /** Wiki 文章列表 */
    suspend fun loadWiki(): List<WikiArticle>

    /** 设置页快照 */
    suspend fun loadSettings(): SettingsSnapshot

    /** 创建实例 */
    suspend fun createInstance(name: String, version: String, loader: String)

    /** 启动指定实例 */
    suspend fun launch(instanceName: String)
}
