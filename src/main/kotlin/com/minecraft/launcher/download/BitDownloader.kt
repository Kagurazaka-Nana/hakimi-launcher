package com.minecraft.launcher.download

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExecutorCoroutineDispatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import java.io.BufferedInputStream
import java.io.Closeable
import java.io.IOException
import java.lang.foreign.Arena
import java.lang.foreign.MemorySegment
import java.lang.invoke.MethodHandles
import java.lang.invoke.VarHandle
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.nio.file.StandardOpenOption
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger
import java.util.zip.CRC32C

/**
 * Kotlin + JDK 25 高并发异步下载器。
 *
 * - 并发：协程 + 虚拟线程调度器（`newVirtualThreadPerTaskExecutor().asCoroutineDispatcher()`），
 *   兼得协程的结构化取消与虚拟线程的阻塞读吞吐；刻意不用仍处预览的 StructuredTaskScope。
 * - 调度：`Channel<Segment>` 分发分片，N 个 worker 谁快谁多取；某片跑得慢或周期检查时
 *   把剩余区间对半切推回队列（work-stealing），消除固定等分的尾部拖尾。
 * - 落盘：`FileChannel.map(mode, off, size, Arena)`（Java 22+ 重载）拿到随 `Arena.close()`
 *   显式释放的 `MemorySegment`，摆脱 MappedByteBuffer 无法 unmap 的问题；未给出 Content-Length
 *   时退化为 `FileChannel.write(buf, position)` 定位写。
 * - 续传：sidecar 片表（魔数 + 版本 + total + ETag/Last-Modified + 分片表 + 分片级 CRC32C），
 *   周期性落盘；重启时校验已完成分片的 CRC 后续传。
 * - 校验：分片级 CRC32C（JDK 内置）；服务端 ETag/Last-Modified 变化即整体重下。
 * - 限速：令牌桶；测速：滑动窗口；传输：原生 `HttpClient`（默认 HTTP/2）。
 */
class BitDownloader(
    private val scope: CoroutineScope,
    private val config: DownloadConfig = DownloadConfig(),
    /** SSRF 校验函数；测试注入本地回环服务器时替换为恒通过实现。 */
    private val urlGuard: (String) -> URI = UrlGuard::validate,
) {

    /** 可变代理：运行时 setProxy 生效于后续新连接；SSRF 校验针对最终请求 URL，与代理无关。 */
    private val proxySelector = VolatileProxySelector(
        config.proxyHost?.let { java.net.InetSocketAddress(it, config.proxyPort) },
    )

    private val http: HttpClient = HttpClient.newBuilder()
        .version(HttpClient.Version.HTTP_2)
        .connectTimeout(config.connectTimeout)
        .followRedirects(HttpClient.Redirect.NORMAL)
        .proxy(proxySelector)
        .build()

    /** 切换传输层代理：host 为 null/空 表示直连。 */
    fun setProxy(host: String?, port: Int) {
        proxySelector.address = if (host.isNullOrBlank()) null else java.net.InetSocketAddress(host, port)
    }

    /**
     * 发起下载：立即返回 [DownloadJob]；cancel 即暂停（保留 .part 可续传）。
     * URL 校验失败同步抛 [SecurityException]。
     */
    fun download(url: String, into: Path): DownloadJob {
        val uri = urlGuard(url)
        val progress = MutableSharedFlow<DownloadProgress>(
            replay = 1,
            extraBufferCapacity = 32,
            onBufferOverflow = kotlinx.coroutines.channels.BufferOverflow.DROP_OLDEST,
        )
        val completion = CompletableDeferred<Unit>()
        val job = scope.launch {
            try {
                runDownload(uri, into, progress)
                completion.complete(Unit)
            } catch (e: CancellationException) {
                completion.cancel(e)
                throw e
            } catch (e: Exception) {
                completion.completeExceptionally(e)
                throw e
            }
        }
        return DownloadJob(job, progress.asSharedFlow().buffer(64), into, completion)
    }

    // —— 对外任务句柄 ——

    class DownloadJob internal constructor(
        val job: Job,
        val progress: Flow<DownloadProgress>,
        val target: Path,
        private val completion: CompletableDeferred<Unit>,
    ) {
        /** 暂停：取消协程但保留 .part 与片表，可对同 URL 再次 download 断点续传。 */
        fun cancel() = job.cancel()

        suspend fun join() = job.join()

        /** 挂起直到下载结束；失败时抛出原始异常（取消时抛 [CancellationException]）。 */
        suspend fun awaitCompletion() = completion.await()

        val isCompleted: Boolean get() = job.isCompleted
    }

    // —— 编排 ——

    private data class Probe(
        val total: Long,
        val etag: String?,
        val lastModified: String?,
        val ranges: Boolean,
    )

    private suspend fun runDownload(
        url: URI,
        into: Path,
        progress: MutableSharedFlow<DownloadProgress>,
    ) {
        val partFile = into.resolveSibling(into.fileName.toString() + ".part")
        val metaFile = into.resolveSibling(into.fileName.toString() + ".part.meta")
        into.toAbsolutePath().parent?.let { Files.createDirectories(it) }
        val virtual = Executors.newVirtualThreadPerTaskExecutor().asCoroutineDispatcher()
        var table: SegmentTable? = null
        var sink: DataSink? = null
        var probe: Probe? = null
        try {
            progress.tryEmit(DownloadProgress(DownloadState.CONNECTING, 0, -1, 0, 0))
            probe = probe(url)
            val saved = PartMeta.read(metaFile)
            val resumable = probe.ranges && probe.total > 0 &&
                saved != null && saved.matches(url.toString(), probe.total, probe.etag, probe.lastModified)

            table = if (resumable) {
                val s = DataSink(partFile, probe.total)
                sink = s
                SegmentTable(verifyPersisted(saved!!, s))
            } else {
                Files.deleteIfExists(metaFile)
                Files.deleteIfExists(partFile)
                val s = DataSink(partFile, probe.total)
                sink = s
                SegmentTable(
                    freshSegments(probe).map { PartMeta.SegmentRecord(it.start, it.endInclusive, done = false) },
                )
            }
            val session = Session(url, probe, table, sink, progress, virtual, metaFile, table.completedBytes())
            session.run()

            // 收尾校验：分片覆盖必须恰好等于总大小
            val covered = table.records().filter { it.done }.sumOf { it.endInclusive - it.start + 1 }
            if (probe.total > 0 && covered != probe.total) {
                throw IOException("分片覆盖不完整: $covered / ${probe.total}")
            }
            sink.force()
            sink.close()
            sink.releaseArena()
            Files.move(partFile, into, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
            Files.deleteIfExists(metaFile)
            progress.tryEmit(
                DownloadProgress(DownloadState.COMPLETED, probe.total, probe.total, session.meter.bytesPerSec(), 0),
            )
        } catch (e: CancellationException) {
            flushMetaQuietly(table, metaFile, url, probe)
            progress.tryEmit(DownloadProgress(DownloadState.CANCELLED, table?.completedBytes() ?: 0, -1, 0, 0))
            throw e
        } catch (e: Exception) {
            flushMetaQuietly(table, metaFile, url, probe)
            progress.tryEmit(DownloadProgress(DownloadState.FAILED, table?.completedBytes() ?: 0, -1, 0, 0))
            throw e
        } finally {
            runCatching { sink?.close() }
            runCatching { sink?.releaseArena() }
            virtual.close()
        }
    }

    private fun flushMetaQuietly(table: SegmentTable?, metaFile: Path, url: URI, probe: Probe?) {
        val t = table ?: return
        val p = probe ?: return
        runCatching { t.toMeta(url.toString(), p.total, p.etag, p.lastModified).write(metaFile) }
    }

    private fun probe(url: URI): Probe {
        // 先 HEAD；HEAD 不可用（异常/非 2xx/无总长）时用 Range: bytes=0-0 的 GET 探测
        try {
            val resp = http.send(headRequest(url), HttpResponse.BodyHandlers.discarding())
            if (resp.statusCode() in 200..299) {
                val p = probeFromHeaders(resp, rangesDefault = false)
                if (p.total > 0) return p
            }
        } catch (e: IOException) {
            // 落到 GET 探测
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
            throw CancellationException("probe interrupted")
        }
        val resp = http.send(rangeProbeRequest(url), HttpResponse.BodyHandlers.ofInputStream())
        resp.body().use { it.close() }
        return when {
            resp.statusCode() == 206 -> {
                val contentRange = resp.headers().firstValue("content-range").orElse(null)
                val total = contentRange?.substringAfterLast('/')?.toLongOrNull() ?: -1
                Probe(total, resp.headers().firstValue("etag").orElse(null), resp.headers().firstValue("last-modified").orElse(null), ranges = true)
            }
            resp.statusCode() in 200..299 -> probeFromHeaders(resp, rangesDefault = false)
            else -> throw IOException("下载地址响应异常: HTTP ${resp.statusCode()}")
        }
    }

    private fun probeFromHeaders(resp: HttpResponse<*>, rangesDefault: Boolean): Probe = Probe(
        total = resp.headers().firstValueAsLong("content-length").orElse(-1),
        etag = resp.headers().firstValue("etag").orElse(null),
        lastModified = resp.headers().firstValue("last-modified").orElse(null),
        ranges = resp.headers().firstValue("accept-ranges").map { it.equals("bytes", ignoreCase = true) }.orElse(rangesDefault),
    )

    private fun headRequest(url: URI): HttpRequest = HttpRequest.newBuilder(url)
        .method("HEAD", HttpRequest.BodyPublishers.noBody())
        .header("User-Agent", USER_AGENT)
        .build()

    private fun rangeProbeRequest(url: URI): HttpRequest = HttpRequest.newBuilder(url)
        .GET()
        .header("Range", "bytes=0-0")
        .header("User-Agent", USER_AGENT)
        .build()

    private fun freshSegments(probe: Probe): List<Segment> {
        if (!probe.ranges || probe.total <= 0) {
            return listOf(Segment(0, if (probe.total > 0) probe.total - 1 else -1))
        }
        val total = probe.total
        val pieces = (config.connections.toLong() * config.initialSegmentsPerConnection)
            .coerceAtMost(total / config.minSegmentSize)
            .coerceIn(1, 1024)
        val base = total / pieces
        val segments = ArrayList<Segment>(pieces.toInt())
        var start = 0L
        for (i in 0 until pieces) {
            val size = if (i == pieces - 1) total - start else base
            segments += Segment(start, start + size - 1)
            start += size
        }
        return segments
    }

    /** 续传前校验已完成分片的 CRC32C，不匹配的退回待下载。 */
    private fun verifyPersisted(saved: PartMeta, sink: DataSink): List<PartMeta.SegmentRecord> =
        saved.segments.map { rec ->
            if (!rec.done) {
                rec
            } else {
                val crc = crcOfRange(sink, rec.start, rec.endInclusive)
                if (crc == rec.crc) rec else rec.copy(done = false, crc = 0)
            }
        }

    private fun crcOfRange(sink: DataSink, start: Long, endInclusive: Long): Long {
        val crc = CRC32C()
        val buf = ByteArray(config.bufferSize)
        var pos = start
        val limit = endInclusive + 1
        while (pos < limit) {
            val want = minOf(buf.size.toLong(), limit - pos).toInt()
            val bb = ByteBuffer.wrap(buf, 0, want)
            var read = 0
            while (bb.hasRemaining()) {
                val n = sink.channel.read(bb, pos + read)
                if (n < 0) throw IOException("校验时文件比片表记录短")
                read += n
            }
            crc.update(buf, 0, want)
            pos += want
        }
        return crc.value
    }

    private companion object {
        const val USER_AGENT = "HakimiLauncher/1.0 (JDK 25; BitDownloader)"
    }

    /** 读取可变地址的 HTTP 代理选择器。 */
    private class VolatileProxySelector(initial: java.net.InetSocketAddress?) : java.net.ProxySelector() {
        @Volatile
        var address: java.net.InetSocketAddress? = initial

        override fun select(uri: URI): MutableList<java.net.Proxy> =
            mutableListOf(address?.let { java.net.Proxy(java.net.Proxy.Type.HTTP, it) } ?: java.net.Proxy.NO_PROXY)

        override fun connectFailed(uri: URI, sa: java.net.SocketAddress, e: IOException) {
            // 无备选代理列表，忽略
        }
    }

    // —— 分片状态表 ——

    private class SegmentTable(initial: List<PartMeta.SegmentRecord>) {
        private val states = initial.toMutableList()

        @Synchronized
        fun add(seg: Segment) {
            states += PartMeta.SegmentRecord(seg.start, seg.endInclusive, done = false)
        }

        /** 把待下载分片 [seg] 在 at 位置一分为二（work-stealing 动态切分）。 */
        @Synchronized
        fun split(seg: Segment, at: Long) {
            val i = states.indexOfFirst { !it.done && it.start == seg.start && it.endInclusive == seg.endInclusive }
            if (i < 0) return
            states.removeAt(i)
            states += PartMeta.SegmentRecord(seg.start, at - 1, done = false)
            states += PartMeta.SegmentRecord(at, seg.endInclusive, done = false)
        }

        @Synchronized
        fun markDone(seg: Segment, crc: Long) {
            val i = states.indexOfFirst { !it.done && it.start == seg.start && it.endInclusive == seg.endInclusive }
            if (i >= 0) states[i] = states[i].copy(done = true, crc = crc)
        }

        @Synchronized
        fun pending(): List<Segment> = states.filter { !it.done }.map { it.toSegment() }

        @Synchronized
        fun records(): List<PartMeta.SegmentRecord> = states.toList()

        @Synchronized
        fun completedBytes(): Long = states.filter { it.done }.sumOf { it.endInclusive - it.start + 1 }

        @Synchronized
        fun toMeta(url: String, total: Long, etag: String?, lastModified: String?): PartMeta =
            PartMeta(url, total, etag, lastModified, states.toList())
    }

    // —— 落盘 ——

    /** 已知总长时整文件映射（Arena 可显式释放）；未知时退化为定位写。 */
    private class DataSink(partFile: Path, total: Long) : Closeable {
        val raf = java.io.RandomAccessFile(partFile.toFile(), "rw")
        val channel: FileChannel = raf.channel
        private val arena: Arena? = if (total > 0) Arena.ofShared() else null
        private val mapped: MemorySegment? = arena?.let {
            channel.map(FileChannel.MapMode.READ_WRITE, 0, total, it)
        }

        init {
            if (total > 0) raf.setLength(total)
        }

        fun write(buf: ByteArray, n: Int, pos: Long) {
            val m = mapped
            if (m != null) {
                m.asSlice(pos, n.toLong()).asByteBuffer().put(buf, 0, n)
            } else {
                channel.write(ByteBuffer.wrap(buf, 0, n), pos)
            }
        }

        fun force() {
            channel.force(true)
        }

        /** unmap 映射（必须在 channel 关闭后调用）。 */
        fun releaseArena() {
            arena?.close()
        }

        override fun close() {
            raf.close()
        }
    }

    /** 单字段原子累计（VarHandle），下载字节计数。 */
    private class DownloadedCounter(initial: Long) {
        @Volatile
        private var value: Long = initial

        fun add(n: Long) {
            VALUE.getAndAdd(this, n)
        }

        fun get(): Long = VALUE.get(this) as Long

        companion object {
            private val VALUE: VarHandle = run {
                val lookup = MethodHandles.privateLookupIn(DownloadedCounter::class.java, MethodHandles.lookup())
                lookup.findVarHandle(DownloadedCounter::class.java, "value", Long::class.javaPrimitiveType)
            }
        }
    }

    // —— 会话 ——

    private inner class Session(
        val url: URI,
        val probe: Probe,
        val table: SegmentTable,
        val sink: DataSink,
        val progress: MutableSharedFlow<DownloadProgress>,
        val virtual: ExecutorCoroutineDispatcher,
        val metaFile: Path,
        counterStart: Long,
    ) {
        val limiter = TokenBucketRateLimiter(config.maxBytesPerSec)
        val meter = SpeedMeter()
        val counter = DownloadedCounter(counterStart)
        val active = AtomicInteger(0)
        private val queue = Channel<Segment>(Channel.UNLIMITED)
        private val inflight = AtomicInteger(0)

        /** 切分下限：测速停滞时自动下调，促进更细的动态分片。 */
        @Volatile
        var splitFloor: Long = config.minSegmentSize * 4
        private var bestSpeed: Long = 0
        private var slowTicks: Int = 0

        suspend fun run(): Unit = coroutineScope {
            val initial = table.pending()
            // 进度、片表落盘、自适应调度 ticker：worker 全部结束后取消
            val tickers = listOf(
                launch { progressLoop() },
                launch { metaFlushLoop() },
                launch { adaptiveLoop() },
            )
            initial.forEach { submit(it) }
            val workers = List(initial.size.coerceAtMost(workerCount())) {
                launch(virtual) { workerLoop() }
            }
            workers.joinAll()
            tickers.forEach { it.cancel() }
        }

        private fun workerCount(): Int =
            if (probe.ranges && probe.total > 0) config.connections else 1

        fun submit(seg: Segment) {
            inflight.incrementAndGet()
            queue.trySend(seg)
        }

        private suspend fun workerLoop() {
            for (seg in queue) {
                active.incrementAndGet()
                try {
                    downloadSegment(seg)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    throw IOException("分片下载失败 [${seg.start}..${seg.endInclusive}]", e)
                } finally {
                    active.decrementAndGet()
                    if (inflight.decrementAndGet() == 0) queue.close()
                }
            }
        }

        private suspend fun downloadSegment(seg: Segment) {
            val unknownEnd = seg.endInclusive < 0
            val builder = HttpRequest.newBuilder(url).GET().header("User-Agent", USER_AGENT)
            if (!unknownEnd && !(seg.start == 0L && seg.endInclusive == probe.total - 1 && !probe.ranges)) {
                builder.header("Range", "bytes=${seg.start}-${seg.endInclusive}")
            }
            val resp = http.send(builder.build(), HttpResponse.BodyHandlers.ofInputStream())
            if (resp.statusCode() !in 200..299) throw IOException("HTTP ${resp.statusCode()}")
            if (resp.statusCode() == 200 && seg.start != 0L) {
                throw IOException("服务器不支持 Range，返回了完整内容")
            }
            resp.body().use { body ->
                val stream = BufferedInputStream(body, config.bufferSize)
                val buf = ByteArray(config.bufferSize)
                val crc = CRC32C()
                var pos = seg.start
                var end = seg.endInclusive
                var chunks = 0
                while (unknownEnd || pos <= end) {
                    currentCoroutineContext().ensureActive()
                    val want = if (unknownEnd) buf.size else minOf(buf.size.toLong(), end - pos + 1).toInt()
                    val t0 = System.nanoTime()
                    val n = stream.read(buf, 0, want)
                    val slow = System.nanoTime() - t0 >= 500_000_000L
                    if (n < 0) {
                        if (unknownEnd) break
                        throw IOException("连接提前结束")
                    }
                    if (n == 0) continue
                    limiter.acquire(n)
                    sink.write(buf, n, pos)
                    crc.update(buf, 0, n)
                    pos += n
                    counter.add(n.toLong())
                    meter.add(n.toLong())
                    chunks++
                    if (!unknownEnd) {
                        val remaining = end - pos + 1
                        val shouldSplit = remaining >= 2 * splitFloor && (slow || chunks % 8 == 0)
                        if (shouldSplit) {
                            val mid = pos + remaining / 2
                            table.split(Segment(seg.start, end), mid)
                            submit(Segment(mid, end))
                            end = mid - 1
                        }
                    }
                }
                if (!unknownEnd && pos != end + 1) throw IOException("分片不完整: 期望到 ${end + 1}，实际 $pos")
                table.markDone(Segment(seg.start, end), crc.value)
            }
        }

        private fun emit(state: DownloadState) {
            progress.tryEmit(
                DownloadProgress(state, counter.get(), probe.total, meter.bytesPerSec(), active.get()),
            )
        }

        private suspend fun progressLoop() {
            while (true) {
                emit(DownloadState.DOWNLOADING)
                delay(config.progressInterval.toMillis())
            }
        }

        private suspend fun metaFlushLoop() {
            while (true) {
                delay(config.metaFlushInterval.toMillis())
                runCatching { table.toMeta(url.toString(), probe.total, probe.etag, probe.lastModified).write(metaFile) }
            }
        }

        /** 自适应调度：速率连续停滞时下调切分下限，让慢片被更早切碎。 */
        private suspend fun adaptiveLoop() {
            while (true) {
                delay(1_000)
                val speed = meter.bytesPerSec()
                if (speed > bestSpeed) {
                    bestSpeed = speed
                    slowTicks = 0
                } else if (bestSpeed > 0 && speed < bestSpeed / 2) {
                    if (++slowTicks >= 3) {
                        splitFloor = (splitFloor / 2).coerceAtLeast(config.minSegmentSize)
                        slowTicks = 0
                    }
                } else {
                    slowTicks = 0
                }
            }
        }
    }
}
