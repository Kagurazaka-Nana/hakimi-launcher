package com.minecraft.launcher.download.mojang;

import com.minecraft.launcher.download.FileDownloader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 执行安装计划：虚拟线程 + 信号量限并发；每个文件按候选 URL 顺序回退下载，
 * sha1 校验通过才原子改名到目标；已存在且摘要匹配的文件直接跳过（幂等差集）。
 * 任一文件所有候选都失败 → 整体失败并保留 .part（BitDownloader 层可续传）。
 */
public final class GameInstaller {

    /** 安装结果计数。 */
    public record Result(int downloaded, int skipped, long bytes) {}

    /** 进度回调（P5 接 DownloadManager 时使用）。 */
    public interface Listener {
        default void onFile(FileEntry entry, boolean skipped) {}
    }

    private final FileDownloader downloader;
    private final int maxParallel;
    /** 可选中央缓存：命中直接硬链接，未命中先下到缓存再链接（多实例共享、重复安装零流量）。 */
    private final CacheStore cache;
    private final Path gameRoot;

    public GameInstaller(FileDownloader downloader, int maxParallel) {
        this(downloader, maxParallel, null, null);
    }

    public GameInstaller(FileDownloader downloader, int maxParallel, CacheStore cache, Path gameRoot) {
        this.downloader = downloader;
        this.maxParallel = Math.max(1, maxParallel);
        this.cache = cache;
        this.gameRoot = gameRoot;
        if ((cache == null) != (gameRoot == null)) {
            throw new IllegalArgumentException("cache 与 gameRoot 必须同时提供或同时为空");
        }
    }

    /**
     * 安装：下载计划内全部文件，并把版本 JSON（source，VersionJsonService 已缓存校验过）
     * 复制到 versions/&lt;id&gt;/&lt;id&gt;.json。
     */
    public Result install(InstallPlan plan, Path versionJsonSource, Listener listener) throws IOException {
        AtomicInteger downloaded = new AtomicInteger();
        AtomicInteger skipped = new AtomicInteger();
        AtomicLong bytes = new AtomicLong();
        Semaphore permits = new Semaphore(maxParallel);

        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            List<Future<?>> futures = new ArrayList<>();
            for (FileEntry entry : plan.files()) {
                futures.add(executor.submit(() -> {
                    try {
                        permits.acquire();
                        try {
                            boolean wasSkipped = downloadOne(entry);
                            if (wasSkipped) {
                                skipped.incrementAndGet();
                            } else {
                                downloaded.incrementAndGet();
                                bytes.addAndGet(Files.size(entry.target()));
                            }
                            listener.onFile(entry, wasSkipped);
                        } finally {
                            permits.release();
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException(e);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }));
            }
            awaitAll(futures);
        }

        // 版本 JSON 直接落盘（内容已在解析阶段校验过结构）
        Path jsonTarget = plan.versionJsonTarget();
        Files.createDirectories(jsonTarget.toAbsolutePath().getParent());
        Files.copy(versionJsonSource, jsonTarget, StandardCopyOption.REPLACE_EXISTING);

        return new Result(downloaded.get(), skipped.get(), bytes.get());
    }

    /** @return true 表示因已存在且摘要匹配而跳过 */
    private boolean downloadOne(FileEntry entry) throws IOException {
        Path target = entry.target();
        Path parent = target.toAbsolutePath().getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        if (Files.isRegularFile(target) && Checksums.matches(target, Checksums.MOJANG_DIGEST, entry.sha1())) {
            return true;
        }
        if (cache != null) {
            return downloadViaCache(entry, target);
        }
        IOException last = null;
        for (String url : entry.candidateUrls()) {
            Path tmp = target.resolveSibling(target.getFileName() + ".part");
            try {
                downloader.downloadBlocking(url, tmp);
                if (!Checksums.matches(tmp, Checksums.MOJANG_DIGEST, entry.sha1())) {
                    throw new IOException("摘要不匹配: " + url);
                }
                Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING);
                return false;
            } catch (IOException | RuntimeException e) {
                Files.deleteIfExists(tmp);
                last = e instanceof IOException ioe ? ioe : new IOException(e);
            }
        }
        throw new IOException("所有候选 URL 均失败: " + target, last);
    }

    /** 缓存路径：目标已坏/缺失 → 先查缓存（硬链接）→ 未命中则下到缓存临时文件、校验、收编、链接进游戏目录。 */
    private boolean downloadViaCache(FileEntry entry, Path target) throws IOException {
        String relative = gameRoot.relativize(target).toString().replace('\\', '/');
        if (cache.contains(relative, entry.sha1())) {
            cache.linkInto(relative, target);
            return false;
        }
        IOException last = null;
        for (String url : entry.candidateUrls()) {
            Path tmp = cache.cacheFile(relative + ".part");
            Files.createDirectories(tmp.toAbsolutePath().getParent());
            try {
                downloader.downloadBlocking(url, tmp);
                if (!Checksums.matches(tmp, Checksums.MOJANG_DIGEST, entry.sha1())) {
                    throw new IOException("摘要不匹配: " + url);
                }
                cache.ingest(relative, tmp);
                cache.linkInto(relative, target);
                return false;
            } catch (IOException | RuntimeException e) {
                Files.deleteIfExists(tmp);
                last = e instanceof IOException ioe ? ioe : new IOException(e);
            }
        }
        throw new IOException("所有候选 URL 均失败: " + target, last);
    }

    private void awaitAll(List<Future<?>> futures) throws IOException {
        IOException first = null;
        for (Future<?> f : futures) {
            try {
                f.get();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                first = first == null ? new IOException("安装被中断", e) : first;
                break;
            } catch (ExecutionException e) {
                IOException ioe = e.getCause() instanceof IOException io ? io
                        : e.getCause() instanceof RuntimeException re && re.getCause() instanceof IOException io2 ? io2
                        : new IOException(e.getCause());
                if (first == null) {
                    first = ioe;
                }
            }
        }
        if (first != null) {
            throw first;
        }
    }
}
