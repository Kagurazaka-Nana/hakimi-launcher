package com.minecraft.launcher.backend

import kotlinx.coroutines.flow.Flow

/**
 * 后端能力接口：UI 只依赖这里定义的操作，
 * 真实下载 / 登录 / 启动 / 扫描等实现后续接入，UI 不直接写业务细节。
 */
interface LauncherBackend {

    /** 首页快照 */
    suspend fun loadHome(): HomeSnapshot

    /** 系统运行监控流：每秒一次真实采样（内存、显存、CPU、磁盘 IO、网络速率）。 */
    fun systemStatsFlow(): Flow<SystemStats>

    /** 下载队列流：任务列表快照，无任务时为空列表（底部指示器据此展开/收起）。 */
    fun downloadTasksFlow(): Flow<List<DownloadTask>>

    /** 发起下载事件：返回任务 id；URL 不合法（SSRF 校验失败）同步抛 [SecurityException]。 */
    fun startDownload(url: String, into: java.nio.file.Path): String

    /** 取消（暂停）下载任务：保留断点，可再次发起续传。 */
    fun cancelDownload(id: String)

    /** 设置网络代理（传输层）：enabled=false 或 host 为空表示直连；仅影响后续新连接。 */
    fun setProxy(enabled: Boolean, host: String, port: Int)

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
