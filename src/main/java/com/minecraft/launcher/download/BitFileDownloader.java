package com.minecraft.launcher.download;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * FileDownloader 的默认实现：委托 BitDownloader（虚拟线程 + 动态分片）。
 * close 时取消全部在途下载。
 */
public final class BitFileDownloader implements FileDownloader {

    private final BitDownloader downloader;
    private final Set<BitDownloader.DownloadJob> inFlight = ConcurrentHashMap.newKeySet();
    private volatile boolean closed;

    public BitFileDownloader() {
        this(DownloadConfig.defaults());
    }

    public BitFileDownloader(DownloadConfig config) {
        this(config, UrlGuard::validate);
    }

    /** urlGuard 可注入替换（测试连本地回环服务器时用恒通过实现）。 */
    public BitFileDownloader(DownloadConfig config, Function<String, URI> urlGuard) {
        this.downloader = new BitDownloader(config, urlGuard);
    }

    @Override
    public BitDownloader.DownloadJob download(String url, Path into) {
        if (closed) {
            throw new IllegalStateException("下载器已关闭");
        }
        BitDownloader.DownloadJob job = downloader.download(url, into);
        inFlight.add(job);
        Thread.ofVirtual().start(() -> {
            try {
                job.awaitCompletion();
            } catch (Exception ignored) {
                // 结果由调用方经句柄获取，这里只做在途清理
            } finally {
                inFlight.remove(job);
            }
        });
        return job;
    }

    @Override
    public Path downloadBlocking(String url, Path into) throws IOException {
        BitDownloader.DownloadJob job = downloader.download(url, into);
        try {
            inFlight.add(job);
            job.awaitCompletion();
        } finally {
            inFlight.remove(job);
        }
        return into;
    }

    @Override
    public void setProxy(String host, int port) {
        downloader.setProxy(host, port);
    }

    @Override
    public void close() {
        closed = true;
        for (BitDownloader.DownloadJob job : inFlight) {
            job.cancel();
        }
    }
}
