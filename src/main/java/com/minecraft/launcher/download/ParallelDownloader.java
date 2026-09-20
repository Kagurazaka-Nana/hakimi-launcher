package com.minecraft.launcher.download;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

/**
 * 并行高速分块下载器：按 HTTP Range 将文件切块并发抓取，支持断点续传
 * （已完成的分块跳过）、失败重试、SHA-1 完整性校验与进度回调。
 * 具体网络由可注入的 {@link RangeFetcher} 决定，便于离线测试。
 */
public final class ParallelDownloader {

    private static final long DEFAULT_CHUNK = 1L * 1024 * 1024; // 1 MiB
    private static final int DEFAULT_PARALLELISM = 8;
    private static final int DEFAULT_RETRIES = 3;

    private final RangeFetcher fetcher;
    private final int parallelism;
    private final long chunkSize;
    private final int maxRetries;

    public ParallelDownloader(RangeFetcher fetcher) {
        this(fetcher, DEFAULT_PARALLELISM, DEFAULT_CHUNK, DEFAULT_RETRIES);
    }

    public ParallelDownloader(RangeFetcher fetcher, int parallelism, long chunkSize, int maxRetries) {
        this.fetcher = fetcher;
        this.parallelism = Math.max(1, parallelism);
        this.chunkSize = chunkSize > 0 ? chunkSize : DEFAULT_CHUNK;
        this.maxRetries = Math.max(1, maxRetries);
    }

    /**
     * 下载 url 到 target。
     *
     * @param expectedDigest 期望的摘要（十六进制），为空则跳过校验
     * @param digestAlgorithm 摘要算法（如 SHA-256）；为空默认 SHA-256。
     *                        Mojang 官方清单使用 SHA-1，由调用方显式传入以兼容。
     * @param listener       进度回调，可为空
     */
    public Path download(String url, Path target, String expectedDigest, String digestAlgorithm, ProgressListener listener) {
        long total = fetcher.size(url);
        List<long[]> chunks = planChunks(total, chunkSize);

        Path partsDir = target.toAbsolutePath().resolveSibling(target.getFileName() + ".parts");
        try {
            Files.createDirectories(partsDir);
            if (target.getParent() != null) {
                Files.createDirectories(target.getParent());
            }
        } catch (IOException e) {
            throw new DownloadException("无法创建下载目录", e);
        }

        AtomicLong done = new AtomicLong(0);
        ExecutorService pool = Executors.newFixedThreadPool(Math.min(parallelism, Math.max(1, chunks.size())));
        try {
            List<Future<?>> futures = new ArrayList<>(chunks.size());
            for (int i = 0; i < chunks.size(); i++) {
                long[] range = chunks.get(i);
                Path part = partsDir.resolve(partName(i));
                futures.add(pool.submit(() -> downloadChunk(url, part, range[0], range[1], done, total, listener)));
            }
            for (Future<?> future : futures) {
                try {
                    future.get();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new DownloadException("下载被中断", e);
                } catch (Exception e) {
                    Throwable cause = e.getCause() != null ? e.getCause() : e;
                    throw new DownloadException("分块下载失败: " + cause.getMessage(), cause);
                }
            }

            try (OutputStream out = Files.newOutputStream(target)) {
                for (int i = 0; i < chunks.size(); i++) {
                    Files.copy(partsDir.resolve(partName(i)), out);
                }
            } catch (IOException e) {
                throw new DownloadException("合并分块失败", e);
            }
        } finally {
            pool.shutdownNow();
        }

        if (expectedDigest != null && !expectedDigest.isBlank()) {
            String algorithm = (digestAlgorithm == null || digestAlgorithm.isBlank()) ? "SHA-256" : digestAlgorithm;
            String actual = digestHex(target, algorithm);
            if (!actual.equalsIgnoreCase(expectedDigest.trim())) {
                throw new DownloadException(algorithm + " 校验失败: 期望 " + expectedDigest + " 实际 " + actual);
            }
        }

        deleteRecursively(partsDir);
        return target;
    }

    private void downloadChunk(String url, Path part, long start, long end, AtomicLong done, long total, ProgressListener listener) {
        long expectedLen = end >= 0 ? end - start + 1 : -1;
        try {
            if (expectedLen > 0 && Files.exists(part) && Files.size(part) == expectedLen) {
                done.addAndGet(expectedLen);
                report(listener, done, total);
                return; // 续传：该分块已完成
            }
        } catch (IOException e) {
            // 读取分块大小失败则按未完成处理
        }

        IOException last = null;
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                byte[] data = fetcher.fetch(url, start, end);
                Path tmp = part.resolveSibling(part.getFileName() + ".tmp");
                Files.write(tmp, data);
                Files.move(tmp, part, StandardCopyOption.REPLACE_EXISTING);
                done.addAndGet(data.length);
                report(listener, done, total);
                return;
            } catch (Exception e) {
                last = new IOException(String.valueOf(e.getMessage()), e);
                sleepBackoff(attempt);
            }
        }
        throw new DownloadException("分块重试耗尽 [" + start + "," + end + "]", last);
    }

    private static void report(ProgressListener listener, AtomicLong done, long total) {
        if (listener != null) {
            listener.onProgress(done.get(), total);
        }
    }

    private static void sleepBackoff(int attempt) {
        try {
            Thread.sleep(Math.min(2000L, 200L * attempt));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /** 将 [0, total) 按 chunkSize 切分为闭区间列表；total<=0 时返回单个整段。 */
    public static List<long[]> planChunks(long total, long chunkSize) {
        List<long[]> chunks = new ArrayList<>();
        if (total <= 0) {
            chunks.add(new long[]{0, -1});
            return chunks;
        }
        long size = chunkSize > 0 ? chunkSize : DEFAULT_CHUNK;
        for (long start = 0; start < total; start += size) {
            long end = Math.min(start + size, total) - 1;
            chunks.add(new long[]{start, end});
        }
        return chunks;
    }

    /** 计算文件的十六进制摘要（默认建议 SHA-256；算法由调用方指定）。 */
    public static String digestHex(Path file, String algorithm) {
        try {
            MessageDigest digest = MessageDigest.getInstance(algorithm);
            byte[] buffer = new byte[8192];
            try (InputStream in = Files.newInputStream(file)) {
                int read;
                while ((read = in.read(buffer)) != -1) {
                    digest.update(buffer, 0, read);
                }
            }
            StringBuilder sb = new StringBuilder();
            for (byte b : digest.digest()) {
                sb.append(Character.forDigit((b >> 4) & 0xF, 16));
                sb.append(Character.forDigit(b & 0xF, 16));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new DownloadException("计算 " + algorithm + " 摘要失败: " + file, e);
        }
    }

    private static String partName(int index) {
        return String.format("part-%05d", index);
    }

    private static void deleteRecursively(Path dir) {
        if (dir == null || !Files.exists(dir)) {
            return;
        }
        try (Stream<Path> walk = Files.walk(dir)) {
            walk.sorted(Comparator.reverseOrder()).forEach(p -> {
                try {
                    Files.deleteIfExists(p);
                } catch (IOException ignored) {
                    // 清理失败不影响主流程
                }
            });
        } catch (IOException ignored) {
            // 清理失败不影响主流程
        }
    }
}
