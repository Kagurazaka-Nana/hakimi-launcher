package com.minecraft.launcher.backend;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.Flow;

/**
 * 后端能力接口（纯 Java）：UI 只依赖这里定义的操作，
 * 真实下载 / 登录 / 启动 / 扫描等实现后续接入，UI 不直接写业务细节。
 *
 * 流式数据（系统指标、下载队列）以 java.util.concurrent.Flow.Publisher 暴露，
 * Kotlin UI 侧用 callbackFlow 适配为 StateFlow。
 */
public interface LauncherBackend {

    /** 首页快照 */
    HomeSnapshot loadHome();

    /** 系统运行监控流：每秒一次真实采样（内存、显存、CPU、磁盘 IO、网络速率）。 */
    Flow.Publisher<SystemStats> systemStatsPublisher();

    /** 下载队列流：任务列表快照（活跃 + 历史），无任务时为空列表。 */
    Flow.Publisher<List<DownloadTask>> downloadTasksPublisher();

    /** 发起下载事件：返回任务 id；URL 不合法（SSRF 校验失败）同步抛 SecurityException。 */
    String startDownload(String url, Path into);

    /** 取消（暂停）下载任务：保留断点，可再次发起续传。 */
    void cancelDownload(String id);

    /** 设置网络代理（传输层）：enabled=false 或 host 为空表示直连；仅影响后续新连接。 */
    void setProxy(boolean enabled, String host, int port);

    /** 设置下载源：official | bmclapi | auto（候选回退）。非法值抛 IllegalArgumentException。 */
    void setDownloadSource(String source);

    /** 后台安装指定版本（manifest→JSON→game/assets/runtime 全链路），进度并入下载队列。 */
    void startInstall(String versionId);

    /** 指定分类的资源列表 */
    List<ResourceItem> loadResources(ResourceKind kind);

    /** 可选游戏版本 */
    List<GameVersion> loadVersions();

    /** 可选加载器 */
    List<LoaderOption> loadLoaders();

    /** 皮肤列表 */
    List<SkinInfo> loadSkins();

    /** 服务器列表 */
    List<ServerInfo> loadServers();

    /** 截图列表 */
    List<ScreenshotInfo> loadScreenshots();

    /** Wiki 文章列表 */
    List<WikiArticle> loadWiki();

    /** 设置页快照 */
    SettingsSnapshot loadSettings();

    /** 创建实例 */
    void createInstance(String name, String version, String loader);

    /** 启动指定实例 */
    void launch(String instanceName);
}
