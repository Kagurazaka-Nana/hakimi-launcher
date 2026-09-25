package com.minecraft.launcher.backend;

import com.minecraft.launcher.auth.Account;
import com.minecraft.launcher.auth.AuthInfo;
import com.minecraft.launcher.auth.MicrosoftAccount;
import com.minecraft.launcher.auth.MicrosoftLoginCallback;
import com.minecraft.launcher.auth.MicrosoftService;
import com.minecraft.launcher.auth.OfflineAccount;
import com.minecraft.launcher.auth.SkinService;
import com.minecraft.launcher.download.BitFileDownloader;
import com.minecraft.launcher.download.DownloadConfig;
import com.minecraft.launcher.download.DownloadManager;
import com.minecraft.launcher.download.DownloadState;
import com.minecraft.launcher.download.FileDownloader;
import com.minecraft.launcher.download.mojang.AutoProvider;
import com.minecraft.launcher.download.mojang.BmclApiProvider;
import com.minecraft.launcher.download.mojang.DownloadProvider;
import com.minecraft.launcher.download.mojang.GameLayout;
import com.minecraft.launcher.download.mojang.InstallationService;
import com.minecraft.launcher.download.mojang.ManifestService;
import com.minecraft.launcher.download.mojang.MojangProvider;
import com.minecraft.launcher.download.mojang.VersionJsonService;
import com.minecraft.launcher.monitor.SystemMetricsMonitor;

import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Flow;
import java.util.concurrent.SubmissionPublisher;
import java.util.concurrent.TimeUnit;

/**
 * 前端框架阶段的占位实现（纯 Java）：不访问业务网络、不触碰游戏文件，
 * 仅向 UI 提供稳定的演示数据。真实逻辑后续替换本实现。
 *
 * 例外：系统指标走 SystemMetricsMonitor 的真实采样（只读 OS 计数器），
 * 下载/安装能力委托 DownloadManager 与 mojang 管线。
 */
public final class StubLauncherBackend implements LauncherBackend {

    private final SystemMetricsMonitor monitor = new SystemMetricsMonitor();
    private final com.minecraft.launcher.download.SnapshotPublisher<SystemStats> statsPublisher =
            new com.minecraft.launcher.download.SnapshotPublisher<>();
    private final FileDownloader sharedDownloader = new BitFileDownloader(
            DownloadConfig.builder().proxyHost("127.0.0.1").proxyPort(10808).build());
    private final DownloadManager downloads = new DownloadManager(sharedDownloader);

    private final Path installGameDir = Path.of("launcherTest", ".minecraft");
    private final Path installCacheDir = Path.of("temp", "install-cache");

    private volatile boolean proxyEnabled = true;
    private volatile String proxyHost = "127.0.0.1";
    private volatile int proxyPort = 10808;
    private volatile String downloadSourceKey = "official";
    private volatile boolean stopped;

    private final MicrosoftService microsoftService = new MicrosoftService();
    private final SkinService skinService = new SkinService();
    private volatile Account account;

    public StubLauncherBackend() {
        Thread.ofVirtual().name("stats-loop").start(this::statsLoop);
    }

    private void statsLoop() {
        while (!stopped) {
            try {
                SystemMetricsMonitor.Snapshot s = monitor.sample();
                statsPublisher.publish(new SystemStats(
                        s.cpuPercent(), s.memUsedGb(), s.memTotalGb(),
                        s.vramUsedMb(), s.vramTotalMb(),
                        s.netDownBps(), s.netUpBps(), s.diskReadBps(), s.diskWriteBps()));
                TimeUnit.SECONDS.sleep(1);
            } catch (InterruptedException e) {
                return;
            } catch (RuntimeException e) {
                // 单次采样失败不终止循环
                try {
                    TimeUnit.SECONDS.sleep(1);
                } catch (InterruptedException ie) {
                    return;
                }
            }
        }
        statsPublisher.close();
    }

    /** 释放后台采样线程（测试/退出时调用）。 */
    public void shutdown() {
        stopped = true;
        downloads.close();
    }

    @Override
    public Flow.Publisher<SystemStats> systemStatsPublisher() {
        return statsPublisher;
    }
    @Override
    public Flow.Publisher<List<DownloadTask>> downloadTasksPublisher() {
        return downloads.tasksPublisher();
    }

    @Override
    public String startDownload(String url, Path into) {
        return downloads.start(url, into);
    }

    @Override
    public void cancelDownload(String id) {
        downloads.cancel(id);
    }

    @Override
    public void resumeDownload(String id) {
        downloads.resume(id);
    }

    @Override
    public void removeDownloadTask(String id) {
        downloads.remove(id);
    }

    @Override
    public void setProxy(boolean enabled, String host, int port) {
        proxyEnabled = enabled;
        proxyHost = host;
        proxyPort = port;
        downloads.setProxy(enabled && !host.isBlank() ? host : null, port);
    }

    @Override
    public void setDownloadSource(String source) {
        if (!source.equals("official") && !source.equals("bmclapi") && !source.equals("auto")) {
            throw new IllegalArgumentException("未知下载源: " + source);
        }
        downloadSourceKey = source;
    }

    private DownloadProvider currentProvider() {
        return switch (downloadSourceKey) {
            case "bmclapi" -> new BmclApiProvider();
            case "auto" -> new AutoProvider(List.of(new BmclApiProvider(), new MojangProvider()));
            default -> new MojangProvider();
        };
    }

    @Override
    public void startInstall(String versionId) {
        DownloadManager.ExternalTask task = downloads.trackExternal("安装 " + versionId, "mojang://version/" + versionId);
        DownloadProvider provider = currentProvider();
        GameLayout layout = new GameLayout(installGameDir);
        InstallationService service = new InstallationService(
                new ManifestService(provider, sharedDownloader, installCacheDir.resolve("version_manifest_v2.json"), Duration.ofHours(1)),
                new VersionJsonService(provider, sharedDownloader, installCacheDir.resolve("version-jsons")),
                provider, sharedDownloader, layout, installCacheDir);
        Thread.ofVirtual().name("install-" + versionId).start(() -> {
            try {
                service.install(versionId, p -> {
                    float fraction = p.bytesTotal() > 0 ? (float) p.bytesDone() / p.bytesTotal() : 0f;
                    task.update(fraction);
                });
                task.complete();
            } catch (Exception e) {
                task.fail();
            }
        });
    }

    // —— 账户与皮肤 ——

    @Override
    public AuthInfo loginOffline(String username) {
        OfflineAccount offline = new OfflineAccount(username);
        account = offline;
        return offline.logIn();
    }

    @Override
    public void loginMicrosoft(String clientId, MicrosoftLoginCallback callback) {
        Thread.ofVirtual().name("ms-login").start(() -> {
            try {
                MicrosoftService.DeviceCodeInfo dc = microsoftService.requestDeviceCode(clientId);
                callback.onDeviceCode(dc.userCode(), dc.verificationUri());
                MicrosoftService.LiveTokens live = microsoftService.pollForLiveTokens(clientId, dc);
                MicrosoftService.MinecraftSession session = microsoftService.exchangeForMinecraft(live);
                MicrosoftAccount msAccount = new MicrosoftAccount(microsoftService, clientId, session);
                account = msAccount;
                callback.onSuccess(msAccount);
            } catch (Exception e) {
                callback.onError(e);
            }
        });
    }

    @Override
    public Account currentAccount() {
        return account;
    }

    @Override
    public void logout() {
        Account old = account;
        account = null;
        if (old != null) {
            try {
                old.close();
            } catch (Exception ignored) {
            }
        }
    }

    @Override
    public SkinService.SkinData loadCurrentSkin() {
        Account current = account;
        if (current == null) {
            return null;
        }
        try {
            return skinService.loadSkin(current.getProfileID());
        } catch (Exception e) {
            try {
                return new SkinService.SkinData(SkinService.defaultSkinBytes(), false);
            } catch (Exception ignored) {
                return null;
            }
        }
    }

    @Override
    public HomeSnapshot loadHome() {
        return new HomeSnapshot(
                "欢迎回来，旅行者！",
                "在方块的世界里，和猫咪一起开启新的冒险吧！",
                "HakimiCat", true,
                "用代码搭建属于自己的世界 —— hakimi",
                "生存世界",
                "和猫咪一起在方块世界中生存、建造、探索！",
                "1.21.1", "Fabric", "Java 21",
                List.of("生存模式", "单人", "Vanilla+"),
                true, 48, 2.8, "2025-08-30 18:42", true,
                List.of(
                        new RecentInstance("生存世界", "1.21.1", "Fabric", "2 小时前"),
                        new RecentInstance("建筑服", "1.20.4", "Forge", "1 天前"),
                        new RecentInstance("Mod 测试", "1.21.1", "Fabric", "3 天前"),
                        new RecentInstance("空岛", "1.20.1", "Quilt", "5 天前")));
    }

    @Override
    public List<ResourceItem> loadResources(ResourceKind kind) {
        return switch (kind) {
            case MODS -> List.of(
                    item("sodium", "Sodium 钠", "高性能渲染引擎，大幅提升帧数", "1.21.1", 2_400_000, List.of("性能优化"), "https://upload.wikimedia.org/wikipedia/commons/1/16/Minecraft_Turkey_Skin.png"),
                    item("iris", "Iris 光影加载器", "在 Fabric 上运行 Iris 光影", "1.7.0", 1_200_000, List.of("性能优化", "光影"), "https://upload.wikimedia.org/wikipedia/commons/d/d4/Grass_Block_%28texture%29_MCJE.png"),
                    item("jei", "Just Enough Items", "物品与合成表查询", "1.24.0", 5_000_000, List.of("建筑")),
                    item("create", "Create 机械动力", "蒸汽朋克机械自动化", "0.5.1", 3_100_000, List.of("科技")),
                    item("journeymap", "JourneyMap 小地图", "实时小地图与全屏地图", "5.9.0", 2_800_000, List.of("冒险")));
            case RESOURCE_PACK -> List.of(
                    item("faithful", "Faithful 32x", "高清重制原版材质", "1.21", 900_000, List.of("写实")),
                    item("pixelpower", "Pixel Power 16x", "精致低像素风格", "1.20", 320_000, List.of("低像素")),
                    item("cartoon", "卡通渲染包", "明亮可爱的卡通风格", "1.21", 150_000, List.of("卡通")));
            case DATA_PACK -> List.of(
                    item("recipes", "自定义配方", "调整合成配方的数据包", "1.21", 80_000, List.of("配方")),
                    item("loot", "战利品表编辑", "自定义怪物掉落", "1.21", 60_000, List.of("战利品")));
            case SHADER -> List.of(
                    item("bsl", "BSL Shaders", "均衡画质与性能", "8.1", 1_500_000, List.of("电影感")),
                    item("complementary", "Complementary", "兼容大多数模组", "4.9", 1_100_000, List.of("写实")),
                    item("seus", "SEUS PTGI", "光线追踪风格光影", "1.0", 700_000, List.of("写实")));
            case MODPACK -> List.of(
                    item("vault", "典范整合包", "科技、魔法、冒险综合", "2.4.7", 400_000, List.of("科技", "魔法")),
                    item("adventure", "玩家必备包", "实用模组合集", "1.8.9", 260_000, List.of("冒险")));
            case PLUGIN -> List.of(
                    item("essentials", "EssentialsX", "服务器基础指令插件", "2.20", 500_000, List.of("管理")),
                    item("luckperms", "LuckPerms", "权限管理插件", "5.4", 480_000, List.of("权限")));
            case SERVER -> List.of(
                    item("vanilla", "原版服务端", "纯净多人服务端", "1.21.1", 300_000, List.of("生存")),
                    item("paper", "Paper 服务端", "高性能 Bukkit 分支", "1.21", 350_000, List.of("生存")));
        };
    }

    @Override
    public List<GameVersion> loadVersions() {
        return List.of(
                new GameVersion("1.21.1", "正式版"),
                new GameVersion("1.20.6", "正式版"),
                new GameVersion("1.20.1", "正式版"),
                new GameVersion("1.19.4", "正式版"),
                new GameVersion("24w21a", "快照"));
    }

    @Override
    public List<LoaderOption> loadLoaders() {
        return List.of(
                new LoaderOption("vanilla", "原版"),
                new LoaderOption("fabric", "Fabric"),
                new LoaderOption("forge", "Forge"),
                new LoaderOption("quilt", "Quilt"),
                new LoaderOption("neoforge", "NeoForge"));
    }

    @Override
    public List<SkinInfo> loadSkins() {
        return List.of(
                new SkinInfo("steve", "Steve", true),
                new SkinInfo("alex", "Alex", false),
                new SkinInfo("cat", "猫咪套装", false),
                new SkinInfo("ninja", "忍者", false));
    }

    @Override
    public List<ServerInfo> loadServers() {
        return List.of(
                new ServerInfo("s1", "hakimi 生存服", "play.hakimi.example", "1.21.1", true, 42),
                new ServerInfo("s2", "空岛乐园", "sky.hakimi.example", "1.20.1", true, 17),
                new ServerInfo("s3", "小游戏大厅", "mini.hakimi.example", "1.21", false, 0));
    }

    @Override
    public List<ScreenshotInfo> loadScreenshots() {
        List<ScreenshotInfo> out = new ArrayList<>();
        for (int i = 1; i <= 6; i++) {
            out.add(new ScreenshotInfo("shot" + i, "screenshot_" + (2025050 + i) + ".png",
                    "2025-05-1" + (i % 9) + " 14:0" + i, (2 + i) + "." + i + " MB"));
        }
        return out;
    }

    @Override
    public List<WikiArticle> loadWiki() {
        return List.of(
                new WikiArticle("w1", "如何创建第一个实例", "入门", "从选择版本到启动游戏的完整流程。", "2 天前"),
                new WikiArticle("w2", "Fabric 与 Forge 的区别", "加载器", "两种主流加载器的生态与兼容性对比。", "5 天前"),
                new WikiArticle("w3", "光影安装指南", "光影", "在 Fabric / OptiFine 下安装光影的方法。", "1 周前"),
                new WikiArticle("w4", "常见问题排查", "支持", "启动失败、白屏、崩溃的排查思路。", "3 天前"));
    }

    @Override
    public SettingsSnapshot loadSettings() {
        return SettingsSnapshot.builder()
                .theme("浅色")
                .language("简体中文")
                .javaPath("C:\\Program Files\\Java\\jdk-21")
                .javaVersion("21.0.3 · 64 位")
                .maxMemoryMb(4096)
                .memoryMinMb(512)
                .memoryMaxMb(8192)
                .downloadSource(downloadSourceKey)
                .concurrency(4)
                .concurrencyMin(1)
                .concurrencyMax(16)
                .jvmArgs("-XX:+UseG1GC -XX:MaxGCPauseMillis=200")
                .debugMode(false)
                .account("已登录：hakimi（离线模式）")
                .privacy("不收集使用数据 · 仅本地存储")
                .proxyEnabled(proxyEnabled)
                .proxyHost(proxyHost)
                .proxyPort(proxyPort)
                .build();
    }

    @Override
    public void createInstance(String name, String version, String loader) {
        // 占位：真实逻辑后续接入
    }

    @Override
    public void launch(String instanceName) {
        // 占位：真实逻辑后续接入
    }

    private static ResourceItem item(String id, String name, String summary, String version,
                                     long downloads, List<String> cats) {
        return item(id, name, summary, version, downloads, cats, null);
    }

    private static ResourceItem item(String id, String name, String summary, String version,
                                     long downloads, List<String> cats, String iconUrl) {
        return ResourceItem.builder()
                .id(id).name(name).summary(summary)
                .description(summary + "。由社区维护，兼容当前实例版本，支持一键启用 / 禁用。")
                .version(version).downloads(downloads).categories(cats)
                .author("hakimi").iconUrl(iconUrl).enabled(false)
                .build();
    }
}
