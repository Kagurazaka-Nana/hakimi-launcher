package com.minecraft.launcher.download;

import com.minecraft.launcher.backend.DownloadTask;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Flow;
import java.util.concurrent.TimeUnit;

/**
 * 下载任务编排层：接收 UI 的下载事件（start/cancel/setProxy），委托 FileDownloader 执行，
 * 把每个任务的进度轮询汇总成单一快照流（tasksPublisher），供底部指示器与下载弹窗消费。
 *
 * 任务到达终态后不移除，标记状态作为历史保留（最多 MAX_HISTORY 条，新在前）；
 * 指示器据此只统计活跃任务，弹窗展示全部。
 * URL 的 SSRF 校验由下载器在 start 时同步执行（不合法直接抛 SecurityException，任务不入队）。
 */
public final class DownloadManager implements java.io.Closeable {

    private static final int MAX_HISTORY = 50;
    private static final long POLL_MILLIS = 300;

    /** 外部编排任务句柄：安装等多文件聚合任务用它在同一队列中推进度。 */
    public final class ExternalTask {
        private final String id;

        private ExternalTask(String id) {
            this.id = id;
        }

        public String id() {
            return id;
        }

        public void update(float fraction) {
            updateFraction(id, fraction, null);
        }

        public void complete() {
            markTerminal(id, DownloadState.COMPLETED, 1f);
        }

        public void fail() {
            markTerminal(id, DownloadState.FAILED, -1);
        }
    }

    private final FileDownloader downloader;
    private final Object lock = new Object();
    private final Map<String, BitDownloader.DownloadJob> jobs = new HashMap<>();
    private List<DownloadTask> tasks = new ArrayList<>();
    private final SnapshotPublisher<List<DownloadTask>> publisher = new SnapshotPublisher<>();

    public DownloadManager() {
        this(new BitFileDownloader());
    }

    public DownloadManager(FileDownloader downloader) {
        this.downloader = downloader;
        publisher.publish(List.of());
    }

    /** 任务列表快照流（活跃 + 历史，新任务在前）。 */
    public Flow.Publisher<List<DownloadTask>> tasksPublisher() {
        return publisher;
    }

    public List<DownloadTask> snapshot() {
        synchronized (lock) {
            return List.copyOf(tasks);
        }
    }

    /** 发起下载事件：返回任务 id；URL 校验失败同步抛 SecurityException。 */
    public String start(String url, Path into) {
        BitDownloader.DownloadJob job = downloader.download(url, into);
        String id = UUID.randomUUID().toString();
        String name = into.getFileName().toString();
        synchronized (lock) {
            jobs.put(id, job);
            tasks.add(0, new DownloadTask(id, name, url, 0f, DownloadState.DOWNLOADING));
            trimAndPublish();
        }
        Thread.ofVirtual().name("dl-watch-" + name).start(() -> watch(id, job));
        return id;
    }

    /** 取消（暂停）任务：保留 .part，可再次 start 同 URL 续传。 */
    public void cancel(String id) {
        BitDownloader.DownloadJob job;
        synchronized (lock) {
            job = jobs.get(id);
        }
        if (job != null) {
            job.cancel();
        }
    }

    /** 切换传输层代理（host 为 null/空 表示直连）。 */
    public void setProxy(String host, int port) {
        downloader.setProxy(host, port);
    }

    /** 登记一个外部编排任务（初始为下载中），返回进度句柄。 */
    public ExternalTask trackExternal(String name, String url) {
        String id = UUID.randomUUID().toString();
        synchronized (lock) {
            tasks.add(0, new DownloadTask(id, name, url, 0f, DownloadState.DOWNLOADING));
            trimAndPublish();
        }
        return new ExternalTask(id);
    }

    private void watch(String id, BitDownloader.DownloadJob job) {
        try {
            while (!job.isDone()) {
                TimeUnit.MILLISECONDS.sleep(POLL_MILLIS);
                DownloadProgress p = job.lastProgress();
                updateFraction(id, fractionOf(p), p.state());
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        DownloadProgress p = job.lastProgress();
        DownloadState terminal = p.state();
        if (terminal == DownloadState.CONNECTING || terminal == DownloadState.DOWNLOADING) {
            terminal = job.isCancelled() ? DownloadState.CANCELLED : DownloadState.FAILED;
        }
        markTerminal(id, terminal, terminal == DownloadState.COMPLETED ? 1f : fractionOf(p));
    }

    private static float fractionOf(DownloadProgress p) {
        return p.totalBytes() > 0 ? Math.min(1f, Math.max(0f, (float) p.downloadedBytes() / p.totalBytes())) : 0f;
    }

    private void updateFraction(String id, float fraction, DownloadState stateHint) {
        synchronized (lock) {
            boolean changed = false;
            for (int i = 0; i < tasks.size(); i++) {
                DownloadTask t = tasks.get(i);
                if (t.getId().equals(id) && isActive(t.getState())) {
                    DownloadState ns = stateHint != null ? stateHint : t.getState();
                    if (ns == DownloadState.CONNECTING && fraction > 0f) {
                        ns = DownloadState.DOWNLOADING;
                    }
                    tasks.set(i, t.toBuilder().fraction(fraction).state(ns).build());
                    changed = true;
                }
            }
            if (changed) {
                trimAndPublish();
            }
        }
    }

    private void markTerminal(String id, DownloadState state, float fraction) {
        synchronized (lock) {
            jobs.remove(id);
            for (int i = 0; i < tasks.size(); i++) {
                DownloadTask t = tasks.get(i);
                if (t.getId().equals(id)) {
                    tasks.set(i, t.toBuilder()
                            .state(state)
                            .fraction(state == DownloadState.COMPLETED ? 1f : Math.max(0f, fraction))
                            .build());
                }
            }
            trimAndPublish();
        }
    }

    private static boolean isActive(DownloadState state) {
        return state == DownloadState.CONNECTING || state == DownloadState.DOWNLOADING;
    }

    /** 调用方持锁：裁剪历史并发布新快照。 */
    private void trimAndPublish() {
        if (tasks.size() > MAX_HISTORY) {
            tasks = new ArrayList<>(tasks.subList(0, MAX_HISTORY));
        }
        publisher.publish(List.copyOf(tasks));
    }

    @Override
    public void close() {
        List<BitDownloader.DownloadJob> running;
        synchronized (lock) {
            running = new ArrayList<>(jobs.values());
        }
        for (BitDownloader.DownloadJob job : running) {
            job.cancel();
        }
        publisher.close();
        downloader.close();
    }
}
