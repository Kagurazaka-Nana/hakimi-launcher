package com.minecraft.launcher.download;

import java.io.BufferedInputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.SocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.zip.CRC32C;

/**
 * JDK 25 高并发异步下载器（纯 Java 实现）。
 *
 * - 并发：虚拟线程 worker 池 + LinkedBlockingQueue 分发分片，谁快谁多取；
 *   某片跑得慢或周期检查时把剩余区间对半切推回队列（work-stealing），消除尾部拖尾。
 *   刻意不用仍处预览的 StructuredTaskScope，阻塞式虚拟线程 + CountDownLatch 足够。
 * - 落盘：FileChannel.map(mode, off, size, Arena)（Java 22+ 重载）随 Arena.close() 显式 unmap；
 *   未知总长退化为 FileChannel.write(buf, position) 定位写。
 * - 续传：sidecar 片表（魔数 + 版本 + total + ETag/Last-Modified + 分片表 + 分片级 CRC32C），
 *   周期性落盘；重启时校验已完成分片 CRC 后续传。
 * - 限速：令牌桶（阻塞式，虚拟线程友好）；测速：滑动窗口，并据此自适应下调切分下限。
 * - 传输：原生 HttpClient（默认 HTTP/2）；代理为可变的传输层 ProxySelector。
 * - 进度：volatile 最新快照（lastProgress 轮询）+ awaitCompletion 阻塞等待。
 */
public final class BitDownloader {

    private static final String USER_AGENT = "HakimiLauncher/1.0 (JDK 25; BitDownloader)";
    private static final int IF_TYPE_SOFTWARE_LOOPBACK = 24;

    private final DownloadConfig config;
    private final Function<String, URI> urlGuard;
    private final VolatileProxySelector proxySelector;
    private final HttpClient http;

    public BitDownloader() {
        this(DownloadConfig.defaults());
    }

    public BitDownloader(DownloadConfig config) {
        this(config, UrlGuard::validate);
    }

    /** urlGuard 可注入替换（测试连本地回环服务器时用恒通过实现）。 */
    public BitDownloader(DownloadConfig config, Function<String, URI> urlGuard) {
        this.config = config;
        this.urlGuard = urlGuard;
        this.proxySelector = new VolatileProxySelector(
                config.getProxyHost() == null ? null : new InetSocketAddress(config.getProxyHost(), config.getProxyPort()));
        this.http = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_2)
                .connectTimeout(config.getConnectTimeout())
                .followRedirects(HttpClient.Redirect.NORMAL)
                .proxy(proxySelector)
                .build();
    }

    /** 切换传输层代理：host 为 null/空 表示直连；仅影响后续新连接。 */
    public void setProxy(String host, int port) {
        proxySelector.address = (host == null || host.isBlank()) ? null : new InetSocketAddress(host, port);
    }

    /** 发起下载：立即返回句柄；cancel 即暂停（保留 .part 可续传）。URL 校验失败同步抛 SecurityException。 */
    public DownloadJob download(String url, Path into) {
        DownloadJob job = new DownloadJob(urlGuard.apply(url), into);
        job.start();
        return job;
    }

    // —— 任务句柄 ——

    public final class DownloadJob {

        private final URI uri;
        private final Path into;
        private final Path partFile;
        private final Path metaFile;

        private volatile DownloadProgress last = DownloadProgress.connecting();
        private final CountDownLatch done = new CountDownLatch(1);
        private final AtomicBoolean cancelled = new AtomicBoolean();
        private volatile Throwable error;
        private volatile Thread runner;

        private DownloadJob(URI uri, Path into) {
            this.uri = uri;
            this.into = into;
            this.partFile = into.resolveSibling(into.getFileName() + ".part");
            this.metaFile = into.resolveSibling(into.getFileName() + ".part.meta");
        }

        private void start() {
            runner = Thread.ofVirtual().name("bitdl-" + into.getFileName()).start(this::run);
        }

        public Path target() {
            return into;
        }

        /** 最新进度快照（轮询消费）。 */
        public DownloadProgress lastProgress() {
            return last;
        }

        public boolean isDone() {
            return done.getCount() == 0;
        }

        public boolean isCancelled() {
            return cancelled.get();
        }

        /** 暂停：中断传输线程，保留 .part 与片表。 */
        public void cancel() {
            if (cancelled.compareAndSet(false, true)) {
                runner.interrupt();
            }
        }

        /** 阻塞直到结束；失败抛原始异常，取消抛 CancellationException。 */
        public void awaitCompletion() throws IOException {
            try {
                done.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IOException("等待下载完成时被中断", e);
            }
            Throwable t = error;
            if (cancelled.get() && t == null) {
                throw new CancellationException("下载已取消");
            }
            if (t instanceof IOException ioe) {
                throw ioe;
            }
            if (t instanceof RuntimeException re) {
                throw re;
            }
            if (t != null) {
                throw new IOException(t);
            }
        }

        private void publish(DownloadState state, long downloaded, long total, long speed, int active) {
            last = new DownloadProgress(state, downloaded, total, speed, active);
        }

        private void run() {
            SegmentTable table = null;
            DataSink sink = null;
            Probe probe = null;
            Session session = null;
            try {
                Path parent = into.toAbsolutePath().getParent();
                if (parent != null) {
                    Files.createDirectories(parent);
                }
                publish(DownloadState.CONNECTING, 0, -1, 0, 0);
                probe = probe();
                PartMeta saved = PartMeta.read(metaFile);
                boolean resumable = probe.ranges && probe.total > 0
                        && saved != null && saved.matches(uri.toString(), probe.total, probe.etag, probe.lastModified);

                if (resumable) {
                    sink = new DataSink(partFile, probe.total);
                    table = SegmentTable.fromRecords(verifyPersisted(saved, sink));
                } else {
                    Files.deleteIfExists(metaFile);
                    Files.deleteIfExists(partFile);
                    sink = new DataSink(partFile, probe.total);
                    table = new SegmentTable(freshSegments(probe));
                }

                session = new Session(probe, table, sink);
                session.run();

                long covered = 0;
                for (PartMeta.SegmentRecord r : table.records()) {
                    if (r.done()) {
                        covered += r.endInclusive() - r.start() + 1;
                    }
                }
                if (probe.total > 0 && covered != probe.total) {
                    throw new IOException("分片覆盖不完整: " + covered + " / " + probe.total);
                }
                sink.force();
                sink.close();
                sink.releaseArena();
                sink = null;
                Files.move(partFile, into, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
                Files.deleteIfExists(metaFile);
                publish(DownloadState.COMPLETED, probe.total, probe.total, session.meter.bytesPerSec(), 0);
            } catch (InterruptedException e) {
                cancelled.set(true);
                flushMeta(table, probe);
                publish(DownloadState.CANCELLED, table == null ? 0 : table.completedBytes(), -1, 0, 0);
            } catch (CancellationException e) {
                flushMeta(table, probe);
                publish(DownloadState.CANCELLED, table == null ? 0 : table.completedBytes(), -1, 0, 0);
            } catch (Throwable t) {
                error = t;
                flushMeta(table, probe);
                publish(DownloadState.FAILED, table == null ? 0 : table.completedBytes(), -1, 0, 0);
            } finally {
                if (sink != null) {
                    closeQuietly(sink);
                    // 取消/失败路径也必须 unmap，否则 Windows 上 .part 被占用无法清理/改名
                    sink.releaseArena();
                }
                done.countDown();
            }
        }

        private void flushMeta(SegmentTable table, Probe probe) {
            if (table == null || probe == null) {
                return;
            }
            try {
                table.toMeta(uri.toString(), probe.total, probe.etag, probe.lastModified).write(metaFile);
            } catch (IOException ignored) {
                // 片表落盘失败不影响收尾
            }
        }

        // —— 探测（针对本任务 URI） ——

        private Probe probe() throws IOException, InterruptedException {
            try {
                HttpResponse<Void> resp = http.send(
                        HttpRequest.newBuilder(uri).method("HEAD", HttpRequest.BodyPublishers.noBody())
                                .header("User-Agent", USER_AGENT).build(),
                        HttpResponse.BodyHandlers.discarding());
                if (resp.statusCode() >= 200 && resp.statusCode() < 300) {
                    Probe p = probeFromHeaders(resp, false);
                    if (p.total > 0) {
                        return p;
                    }
                }
            } catch (IOException e) {
                // 落到 GET 探测
            }
            HttpResponse<InputStream> resp = http.send(
                    HttpRequest.newBuilder(uri).GET()
                            .header("Range", "bytes=0-0").header("User-Agent", USER_AGENT).build(),
                    HttpResponse.BodyHandlers.ofInputStream());
            try {
                resp.body().close();
            } catch (IOException ignored) {
            }
            if (resp.statusCode() == HttpURLConnection.HTTP_PARTIAL) {
                long total = -1;
                String contentRange = resp.headers().firstValue("content-range").orElse(null);
                if (contentRange != null && contentRange.lastIndexOf('/') >= 0) {
                    try {
                        total = Long.parseLong(contentRange.substring(contentRange.lastIndexOf('/') + 1));
                    } catch (NumberFormatException ignored) {
                    }
                }
                return new Probe(total,
                        resp.headers().firstValue("etag").orElse(null),
                        resp.headers().firstValue("last-modified").orElse(null), true);
            }
            if (resp.statusCode() >= 200 && resp.statusCode() < 300) {
                return probeFromHeaders(resp, false);
            }
            throw new IOException("下载地址响应异常: HTTP " + resp.statusCode());
        }

        private Probe probeFromHeaders(HttpResponse<?> resp, boolean rangesDefault) {
            return new Probe(
                    resp.headers().firstValueAsLong("content-length").orElse(-1L),
                    resp.headers().firstValue("etag").orElse(null),
                    resp.headers().firstValue("last-modified").orElse(null),
                    resp.headers().firstValue("accept-ranges").map(v -> v.equalsIgnoreCase("bytes")).orElse(rangesDefault));
        }

        private List<Segment> freshSegments(Probe probe) {
            List<Segment> segments = new ArrayList<>();
            if (!probe.ranges || probe.total <= 0) {
                segments.add(new Segment(0, probe.total > 0 ? probe.total - 1 : -1));
                return segments;
            }
            long pieces = Math.min((long) config.getConnections() * config.getInitialSegmentsPerConnection(),
                    probe.total / config.getMinSegmentSize());
            pieces = Math.max(1, Math.min(pieces, 1024));
            long base = probe.total / pieces;
            long start = 0;
            for (int i = 0; i < pieces; i++) {
                long size = i == pieces - 1 ? probe.total - start : base;
                segments.add(new Segment(start, start + size - 1));
                start += size;
            }
            return segments;
        }

        /** 续传前校验已完成分片的 CRC32C，不匹配的退回待下载。 */
        private List<PartMeta.SegmentRecord> verifyPersisted(PartMeta saved, DataSink sink) throws IOException {
            List<PartMeta.SegmentRecord> out = new ArrayList<>();
            for (PartMeta.SegmentRecord rec : saved.segments()) {
                if (!rec.done()) {
                    out.add(rec);
                } else {
                    long crc = crcOfRange(sink, rec.start(), rec.endInclusive());
                    out.add(crc == rec.crc() ? rec : new PartMeta.SegmentRecord(rec.start(), rec.endInclusive(), false, 0));
                }
            }
            return out;
        }

        private long crcOfRange(DataSink sink, long start, long endInclusive) throws IOException {
            CRC32C crc = new CRC32C();
            byte[] buf = new byte[config.getBufferSize()];
            long pos = start;
            long limit = endInclusive + 1;
            while (pos < limit) {
                int want = (int) Math.min(buf.length, limit - pos);
                ByteBuffer bb = ByteBuffer.wrap(buf, 0, want);
                int read = 0;
                while (bb.hasRemaining()) {
                    int n = sink.channel.read(bb, pos + read);
                    if (n < 0) {
                        throw new IOException("校验时文件比片表记录短");
                    }
                    read += n;
                }
                crc.update(buf, 0, want);
                pos += want;
            }
            return crc.getValue();
        }

        // —— 会话：worker 池 + ticker ——

        private final class Session {
            private final Probe probe;
            private final SegmentTable table;
            private final DataSink sink;
            private final TokenBucketRateLimiter limiter = new TokenBucketRateLimiter(config.getMaxBytesPerSec());
            private final SpeedMeter meter = new SpeedMeter();
            private final DownloadedCounter counter;
            private final AtomicInteger active = new AtomicInteger();
            private final LinkedBlockingQueue<Segment> queue = new LinkedBlockingQueue<>();
            private final AtomicInteger inflight = new AtomicInteger();
            private final AtomicBoolean drained = new AtomicBoolean();
            private final AtomicBoolean stop = new AtomicBoolean();
            private final AtomicReference<Throwable> failure = new AtomicReference<>();
            /** 切分下限：测速停滞时自动下调，促进更细的动态分片。 */
            private final AtomicLong splitFloor = new AtomicLong(config.getMinSegmentSize() * 4);
            private volatile long bestSpeed;
            private volatile int slowTicks;

            Session(Probe probe, SegmentTable table, DataSink sink) {
                this.probe = probe;
                this.table = table;
                this.sink = sink;
                this.counter = new DownloadedCounter(table.completedBytes());
            }

            void run() throws Exception {
                List<Segment> initial = table.pending();
                for (Segment s : initial) {
                    inflight.incrementAndGet();
                    queue.add(s);
                }
                int workers = Math.max(1, Math.min(initial.size(),
                        probe.ranges && probe.total > 0 ? config.getConnections() : 1));

                Thread ticker = Thread.ofVirtual().name("bitdl-ticker").start(this::tickerLoop);
                ExecutorService pool = Executors.newVirtualThreadPerTaskExecutor();
                try {
                    for (int i = 0; i < workers; i++) {
                        pool.submit(this::workerLoop);
                    }
                    pool.shutdown();
                    while (!pool.awaitTermination(200, TimeUnit.MILLISECONDS)) {
                        if (Thread.currentThread().isInterrupted()) {
                            pool.shutdownNow();
                            stop.set(true);
                            throw new InterruptedException("download interrupted");
                        }
                    }
                } finally {
                    stop.set(true);
                    ticker.interrupt();
                }
                Throwable f = failure.get();
                if (f instanceof Exception e) {
                    throw e;
                }
                if (f != null) {
                    throw new IOException(f);
                }
            }

            private void workerLoop() {
                try {
                    while (!stop.get() && !Thread.currentThread().isInterrupted()) {
                        Segment seg = queue.poll(200, TimeUnit.MILLISECONDS);
                        if (seg == null) {
                            if (drained.get() || inflight.get() == 0) {
                                break;
                            }
                            continue;
                        }
                        active.incrementAndGet();
                        try {
                            downloadSegment(seg);
                        } catch (Exception e) {
                            failure.compareAndSet(null, e);
                            stop.set(true);
                            inflight.decrementAndGet();
                            return;
                        } finally {
                            active.decrementAndGet();
                        }
                        if (inflight.decrementAndGet() == 0) {
                            drained.set(true);
                        }
                    }
                } catch (InterruptedException e) {
                    stop.set(true);
                    Thread.currentThread().interrupt();
                }
            }

            private void downloadSegment(Segment seg) throws Exception {
                boolean unknownEnd = seg.endInclusive() < 0;
                HttpRequest.Builder builder = HttpRequest.newBuilder(uri).GET().header("User-Agent", USER_AGENT);
                if (!unknownEnd && !(seg.start() == 0 && seg.endInclusive() == probe.total - 1 && !probe.ranges)) {
                    builder.header("Range", "bytes=" + seg.start() + "-" + seg.endInclusive());
                }
                HttpResponse<InputStream> resp = http.send(builder.build(), HttpResponse.BodyHandlers.ofInputStream());
                if (resp.statusCode() < 200 || resp.statusCode() >= 300) {
                    throw new IOException("HTTP " + resp.statusCode());
                }
                if (resp.statusCode() == 200 && seg.start() != 0) {
                    throw new IOException("服务器不支持 Range，返回了完整内容");
                }
                try (InputStream body = resp.body();
                     BufferedInputStream stream = new BufferedInputStream(body, config.getBufferSize())) {
                    byte[] buf = new byte[config.getBufferSize()];
                    CRC32C crc = new CRC32C();
                    long pos = seg.start();
                    long end = seg.endInclusive();
                    int chunks = 0;
                    while (unknownEnd || pos <= end) {
                        if (Thread.currentThread().isInterrupted() || stop.get()) {
                            throw new InterruptedException();
                        }
                        int want = unknownEnd ? buf.length : (int) Math.min(buf.length, end - pos + 1);
                        long t0 = System.nanoTime();
                        int n = stream.read(buf, 0, want);
                        boolean slow = System.nanoTime() - t0 >= 500_000_000L;
                        if (n < 0) {
                            if (unknownEnd) {
                                break;
                            }
                            throw new IOException("连接提前结束");
                        }
                        if (n == 0) {
                            continue;
                        }
                        limiter.acquire(n);
                        sink.write(buf, n, pos);
                        crc.update(buf, 0, n);
                        pos += n;
                        counter.add(n);
                        meter.add(n);
                        chunks++;
                        if (!unknownEnd) {
                            long remaining = end - pos + 1;
                            if (remaining >= 2 * splitFloor.get() && (slow || chunks % 8 == 0)) {
                                long mid = pos + remaining / 2;
                                table.split(new Segment(seg.start(), end), mid);
                                inflight.incrementAndGet();
                                queue.add(new Segment(mid, end));
                                end = mid - 1;
                            }
                        }
                    }
                    if (!unknownEnd && pos != end + 1) {
                        throw new IOException("分片不完整: 期望到 " + (end + 1) + "，实际 " + pos);
                    }
                    table.markDone(new Segment(seg.start(), end), crc.getValue());
                }
            }

            private void tickerLoop() {
                long lastMeta = System.nanoTime();
                long lastAdaptive = System.nanoTime();
                while (!stop.get() && !Thread.currentThread().isInterrupted()) {
                    try {
                        Thread.sleep(config.getProgressInterval().toMillis());
                    } catch (InterruptedException e) {
                        return;
                    }
                    publish(DownloadState.DOWNLOADING, counter.get(), probe.total, meter.bytesPerSec(), active.get());
                    long now = System.nanoTime();
                    if (now - lastMeta >= config.getMetaFlushInterval().toNanos()) {
                        lastMeta = now;
                        flushMetaNow();
                    }
                    if (now - lastAdaptive >= 1_000_000_000L) {
                        lastAdaptive = now;
                        adapt();
                    }
                }
            }

            private void flushMetaNow() {
                try {
                    table.toMeta(uri.toString(), probe.total, probe.etag, probe.lastModified).write(metaFile);
                } catch (IOException ignored) {
                    // 片表落盘失败不影响传输，下次再试
                }
            }

            /** 自适应调度：速率连续停滞时下调切分下限，让慢片被更早切碎。 */
            private void adapt() {
                long speed = meter.bytesPerSec();
                if (speed > bestSpeed) {
                    bestSpeed = speed;
                    slowTicks = 0;
                } else if (bestSpeed > 0 && speed < bestSpeed / 2) {
                    if (++slowTicks >= 3) {
                        splitFloor.updateAndGet(v -> Math.max(config.getMinSegmentSize(), v / 2));
                        slowTicks = 0;
                    }
                } else {
                    slowTicks = 0;
                }
            }
        }
    }

    // —— 分片状态表 ——

    private static final class SegmentTable {
        private final List<PartMeta.SegmentRecord> states = new ArrayList<>();

        SegmentTable(List<Segment> initial) {
            for (Segment s : initial) {
                states.add(new PartMeta.SegmentRecord(s.start(), s.endInclusive(), false, 0));
            }
        }

        static SegmentTable fromRecords(List<PartMeta.SegmentRecord> records) {
            SegmentTable t = new SegmentTable(List.of());
            t.states.addAll(records);
            return t;
        }

        synchronized void split(Segment seg, long at) {
            int i = indexOfPending(seg);
            if (i < 0) {
                return;
            }
            states.remove(i);
            states.add(new PartMeta.SegmentRecord(seg.start(), at - 1, false, 0));
            states.add(new PartMeta.SegmentRecord(at, seg.endInclusive(), false, 0));
        }

        synchronized void markDone(Segment seg, long crc) {
            int i = indexOfPending(seg);
            if (i >= 0) {
                states.set(i, new PartMeta.SegmentRecord(seg.start(), seg.endInclusive(), true, crc));
            }
        }

        private int indexOfPending(Segment seg) {
            for (int i = 0; i < states.size(); i++) {
                PartMeta.SegmentRecord r = states.get(i);
                if (!r.done() && r.start() == seg.start() && r.endInclusive() == seg.endInclusive()) {
                    return i;
                }
            }
            return -1;
        }

        synchronized List<Segment> pending() {
            List<Segment> out = new ArrayList<>();
            for (PartMeta.SegmentRecord r : states) {
                if (!r.done()) {
                    out.add(r.toSegment());
                }
            }
            return out;
        }

        synchronized List<PartMeta.SegmentRecord> records() {
            return List.copyOf(states);
        }

        synchronized long completedBytes() {
            long sum = 0;
            for (PartMeta.SegmentRecord r : states) {
                if (r.done()) {
                    sum += r.endInclusive() - r.start() + 1;
                }
            }
            return sum;
        }

        synchronized PartMeta toMeta(String url, long total, String etag, String lastModified) {
            return new PartMeta(url, total, etag, lastModified, List.copyOf(states));
        }
    }

    private static final class Probe {
        final long total;
        final String etag;
        final String lastModified;
        final boolean ranges;

        Probe(long total, String etag, String lastModified, boolean ranges) {
            this.total = total;
            this.etag = etag;
            this.lastModified = lastModified;
            this.ranges = ranges;
        }
    }

    // —— 落盘 ——

    private static final class DataSink implements Closeable {
        private final RandomAccessFile raf;
        final FileChannel channel;
        private final Arena arena;
        private final MemorySegment mapped;

        DataSink(Path partFile, long total) throws IOException {
            this.raf = new RandomAccessFile(partFile.toFile(), "rw");
            this.channel = raf.getChannel();
            if (total > 0) {
                raf.setLength(total);
                this.arena = Arena.ofShared();
                this.mapped = channel.map(FileChannel.MapMode.READ_WRITE, 0, total, arena);
            } else {
                this.arena = null;
                this.mapped = null;
            }
        }

        void write(byte[] buf, int n, long pos) throws IOException {
            if (mapped != null) {
                mapped.asSlice(pos, n).asByteBuffer().put(buf, 0, n);
            } else {
                channel.write(ByteBuffer.wrap(buf, 0, n), pos);
            }
        }

        void force() throws IOException {
            channel.force(true);
        }

        /** unmap 映射（必须在 channel 关闭后调用）。 */
        void releaseArena() {
            if (arena != null) {
                arena.close();
            }
        }

        @Override
        public void close() throws IOException {
            raf.close();
        }
    }

    /** 单字段原子累计（VarHandle）。 */
    private static final class DownloadedCounter {
        private static final VarHandle VALUE;

        static {
            try {
                VALUE = MethodHandles.privateLookupIn(DownloadedCounter.class, MethodHandles.lookup())
                        .findVarHandle(DownloadedCounter.class, "value", long.class);
            } catch (ReflectiveOperationException e) {
                throw new ExceptionInInitializerError(e);
            }
        }

        @SuppressWarnings("unused")
        private volatile long value;

        DownloadedCounter(long initial) {
            VALUE.set(this, initial);
        }

        void add(long n) {
            VALUE.getAndAdd(this, n);
        }

        long get() {
            return (long) VALUE.getVolatile(this);
        }
    }

    /** 读取可变地址的 HTTP 代理选择器。 */
    private static final class VolatileProxySelector extends ProxySelector {
        volatile InetSocketAddress address;

        VolatileProxySelector(InetSocketAddress initial) {
            this.address = initial;
        }

        @Override
        public List<Proxy> select(URI uri) {
            InetSocketAddress a = address;
            return List.of(a == null ? Proxy.NO_PROXY : new Proxy(Proxy.Type.HTTP, a));
        }

        @Override
        public void connectFailed(URI uri, SocketAddress sa, IOException ioe) {
            // 无备选代理列表，忽略
        }
    }

    private static void closeQuietly(Closeable c) {
        try {
            c.close();
        } catch (IOException ignored) {
        }
    }
}
