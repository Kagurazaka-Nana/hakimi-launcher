package com.minecraft.launcher.download.mojang;

import com.minecraft.launcher.download.FileDownloader;
import com.minecraft.launcher.model.manifest.VersionInfo;
import com.minecraft.launcher.model.manifest.VersionManifest;
import com.minecraft.launcher.model.rule.RuleContext;
import com.minecraft.launcher.model.rule.RuleEvaluator;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 安装编排（docs/DownloadPipeline.md §3.4 P5）：把「解析 → 过滤 → 计划 → 分阶段执行」串成一次安装。
 *
 * 阶段：game（客户端 JAR + libraries）→ assets（索引 + 对象差集）→ runtime（Java 运行时）。
 * 提交语义：版本 JSON 最后写入 versions/&lt;id&gt;/&lt;id&gt;.json 作为完成标记——
 * 中途失败只留下 .part/缓存，isInstalled 仍为 false，可安全重入（幂等差集 + 缓存命中）。
 */
public final class InstallationService {

    /** 聚合进度快照。 */
    public record Progress(long bytesDone, long bytesTotal, int filesDone, int filesTotal, String stage) {}

    public interface Listener {
        void onProgress(Progress progress);
    }

    private final ManifestService manifestService;
    private final VersionJsonService versionJsonService;
    private final DownloadProvider provider;
    private final FileDownloader downloader;
    private final GameLayout layout;
    private final Path cacheDir;

    public InstallationService(ManifestService manifestService, VersionJsonService versionJsonService,
                               DownloadProvider provider, FileDownloader downloader,
                               GameLayout layout, Path cacheDir) {
        this.manifestService = manifestService;
        this.versionJsonService = versionJsonService;
        this.provider = provider;
        this.downloader = downloader;
        this.layout = layout;
        this.cacheDir = cacheDir;
    }

    /** 该版本是否已安装（以版本 JSON 为提交标记）。 */
    public boolean isInstalled(String versionId) {
        return Files.isRegularFile(layout.versionJson(versionId));
    }

    /** 同步安装一个版本（调用方负责放到后台线程）。 */
    public void install(String versionId, Listener listener) throws IOException {
        VersionManifest manifest = manifestService.fetch();
        VersionInfo info = manifest.findVersionById(versionId)
                .orElseThrow(() -> new IOException("清单中不存在版本: " + versionId));
        ResolvedVersion resolved = versionJsonService.resolve(manifest, info);

        List<FileEntry> gameFiles = planGameFiles(resolved);
        List<FileEntry> assetFiles = planAssetFiles(resolved);
        List<FileEntry> runtimeFiles = planRuntimeFiles(resolved);

        long totalBytes = sumSizes(gameFiles) + sumSizes(assetFiles) + sumSizes(runtimeFiles);
        int totalFiles = gameFiles.size() + assetFiles.size() + runtimeFiles.size();
        AtomicLong bytesDone = new AtomicLong();
        AtomicInteger filesDone = new AtomicInteger();

        runStage(gameFiles, "game", totalBytes, totalFiles, bytesDone, filesDone, listener);
        runStage(assetFiles, "assets", totalBytes, totalFiles, bytesDone, filesDone, listener);
        runStage(runtimeFiles, "runtime", totalBytes, totalFiles, bytesDone, filesDone, listener);

        // 提交标记：全部文件就位后才写版本 JSON
        Path jsonTarget = layout.versionJson(resolved.meta().getClientVersion());
        Files.createDirectories(jsonTarget.toAbsolutePath().getParent());
        Files.copy(resolved.jsonSource(), jsonTarget, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
    }

    private List<FileEntry> planGameFiles(ResolvedVersion resolved) {
        LibraryFilter filter = new LibraryFilter(new RuleEvaluator(RuleContext.fromSystem()));
        List<LibraryDownload> libs = filter.select(resolved.meta().getLibraries());
        InstallPlan plan = new InstallPlanner(provider, layout).plan(resolved, libs);
        return plan.files();
    }

    private List<FileEntry> planAssetFiles(ResolvedVersion resolved) throws IOException {
        if (resolved.meta().getAssetIndex() == null) {
            return List.of();
        }
        AssetService assets = new AssetService(provider, downloader, layout);
        return assets.planObjects(assets.loadIndex(resolved.meta().getAssetIndex()), false);
    }

    private List<FileEntry> planRuntimeFiles(ResolvedVersion resolved) throws IOException {
        if (resolved.meta().getJavaVersion() == null || resolved.meta().getJavaVersion().getComponent() == null) {
            return List.of();
        }
        RuleContext ctx = RuleContext.fromSystem();
        RuntimeService runtime = new RuntimeService(provider, downloader, layout, cacheDir);
        RuntimeService.RuntimePlan plan = runtime.plan(
                resolved.meta().getJavaVersion().getComponent(), ctx.getOsName(), ctx.getArch());
        return plan.files();
    }

    private void runStage(List<FileEntry> files, String stage, long totalBytes, int totalFiles,
                          AtomicLong bytesDone, AtomicInteger filesDone, Listener listener) throws IOException {
        if (files.isEmpty()) {
            return;
        }
        CacheStore cache = new CacheStore(cacheDir.resolve("cache"));
        GameInstaller installer = new GameInstaller(downloader, provider.getMaxConcurrency(), cache, layout.getRoot());
        installer.installFiles(files, (entry, skipped) -> {
            bytesDone.addAndGet(Math.max(entry.size(), 0));
            listener.onProgress(new Progress(bytesDone.get(), totalBytes, filesDone.incrementAndGet(), totalFiles, stage));
        });
    }

    private static long sumSizes(List<FileEntry> files) {
        long sum = 0;
        for (FileEntry f : files) {
            if (f.size() > 0) {
                sum += f.size();
            }
        }
        return sum;
    }
}
