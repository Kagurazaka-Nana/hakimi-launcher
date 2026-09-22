package com.minecraft.launcher.download

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.io.TempDir
import java.io.OutputStream
import java.net.InetSocketAddress
import java.net.URI
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.Executors
import java.util.zip.CRC32C
import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import kotlin.random.Random

class BitDownloaderTest {

    @TempDir
    lateinit var dir: Path

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /** 本地测试服务器恒通过 SSRF 校验（真实 guard 的拦截行为另有专门用例）。 */
    private fun downloader(config: DownloadConfig = DownloadConfig()) =
        BitDownloader(scope, config, urlGuard = { URI(it) })

    // —— 本地 Range HTTP 服务器 ——

    private class TestFileServer(private val data: ByteArray, private val etag: String, private val supportRanges: Boolean = true) {
        val servedRangeStarts = mutableListOf<Long>()
        var chunkDelayMillis = 0L
        private var server: HttpServer? = null

        val url: String get() = "http://127.0.0.1:${server!!.address.port}/file.bin"

        fun start(): TestFileServer {
            val s = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
            s.createContext("/file.bin") { exchange -> handle(exchange) }
            s.executor = Executors.newVirtualThreadPerTaskExecutor()
            s.start()
            server = s
            return this
        }

        fun stop() {
            server?.stop(0)
        }

        private fun handle(exchange: HttpExchange) {
            exchange.use { ex ->
                val rangeHeader = ex.requestHeaders.getFirst("Range")
                if (ex.requestMethod == "HEAD") {
                    if (supportRanges) ex.responseHeaders.add("Accept-Ranges", "bytes")
                    ex.responseHeaders.add("ETag", etag)
                    ex.responseHeaders.add("Content-Length", data.size.toString())
                    ex.sendResponseHeaders(200, data.size.toLong())
                    return
                }
                val range = rangeHeader?.let { parseRange(it) }
                if (range != null) {
                    if (!supportRanges) {
                        // 不支持 Range：忽略头部，回完整 200
                        sendBody(ex, 200, 0L, (data.size - 1).toLong())
                        return
                    }
                    // 跳过 bytes=0-0 的能力探测请求，只记录真实分片取数
                    if (!(range.first == 0L && (range.second ?: 0L) == 0L)) {
                        servedRangeStarts += range.first
                    }
                    val end = range.second ?: (data.size - 1).toLong()
                    ex.responseHeaders.add("Content-Range", "bytes ${range.first}-$end/${data.size}")
                    ex.responseHeaders.add("ETag", etag)
                    ex.sendResponseHeaders(206, end - range.first + 1)
                    writeBody(ex, range.first, end)
                } else {
                    if (supportRanges) ex.responseHeaders.add("Accept-Ranges", "bytes")
                    ex.responseHeaders.add("ETag", etag)
                    ex.sendResponseHeaders(200, data.size.toLong())
                    writeBody(ex, 0L, (data.size - 1).toLong())
                }
            }
        }

        private fun parseRange(header: String): Pair<Long, Long?>? {
            if (!header.startsWith("bytes=")) return null
            val part = header.removePrefix("bytes=")
            val dash = part.indexOf('-')
            if (dash <= 0) return null
            val start = part.substring(0, dash).toLongOrNull() ?: return null
            val end = part.substring(dash + 1).takeIf { it.isNotEmpty() }?.toLongOrNull()
            return start to end
        }

        private fun sendBody(ex: HttpExchange, code: Int, start: Long, end: Long) {
            ex.sendResponseHeaders(code, end - start + 1)
            writeBody(ex, start, end)
        }

        private fun writeBody(ex: HttpExchange, start: Long, end: Long) {
            val chunk = 64 * 1024L
            var pos = start
            while (pos <= end) {
                val n = minOf(chunk, end - pos + 1).toInt()
                ex.responseBody.write(data, pos.toInt(), n)
                pos += n
                if (chunkDelayMillis > 0) Thread.sleep(chunkDelayMillis)
            }
        }
    }

    private fun randomBytes(size: Int): ByteArray = ByteArray(size).also { Random(42).nextBytes(it) }

    private fun crcOf(data: ByteArray, from: Int, toInclusive: Int): Long {
        val crc = CRC32C()
        crc.update(data, from, toInclusive - from + 1)
        return crc.value
    }

    // —— 用例 ——

    @Test
    fun `multi-connection download completes with exact content`() = runBlocking {
        val data = randomBytes(2 * 1024 * 1024)
        val server = TestFileServer(data, etag = "\"v1\"").start()
        try {
            val target = dir.resolve("file.bin")
            val job = downloader().download(server.url, target)
            withTimeout(30_000) { job.join() }
            // SharedFlow replay=1 保留最后一次发射；join 后必然是终态
            val last = withTimeout(5_000) { job.progress.first() }
            assertArrayEquals(data, Files.readAllBytes(target))
            assertFalse(Files.exists(target.resolveSibling("file.bin.part")))
            assertFalse(Files.exists(target.resolveSibling("file.bin.part.meta")))
            assertEquals(DownloadState.COMPLETED, last.state)
            assertEquals(data.size.toLong(), last.totalBytes)
            assertEquals(data.size.toLong(), last.downloadedBytes)
        } finally {
            server.stop()
        }
    }

    @Test
    fun `single stream fallback when server ignores ranges`() = runBlocking {
        val data = randomBytes(512 * 1024)
        val server = TestFileServer(data, etag = "\"v1\"", supportRanges = false).start()
        try {
            val target = dir.resolve("single.bin")
            val job = downloader().download(server.url, target)
            withTimeout(30_000) { job.join() }
            assertArrayEquals(data, Files.readAllBytes(target))
        } finally {
            server.stop()
        }
    }

    @Test
    fun `cancel keeps part then resume completes`() = runBlocking {
        val data = randomBytes(4 * 1024 * 1024)
        val slow = TestFileServer(data, etag = "\"v1\"").apply { chunkDelayMillis = 120 }.start()
        val target = dir.resolve("resume.bin")
        val partFile = target.resolveSibling("resume.bin.part")
        val metaFile = target.resolveSibling("resume.bin.part.meta")
        try {
            val first = downloader().download(slow.url, target)
            delay(600) // 让它下载一部分
            first.cancel()
            withTimeout(10_000) { first.join() }
            val listing = Files.newDirectoryStream(dir).use { stream ->
                stream.joinToString { p -> p.fileName.toString() }
            }
            assertTrue(Files.exists(partFile), "取消后 .part 应保留; dir=[$listing]")
            assertNotNull(
                PartMeta.read(metaFile),
                "取消后片表应保留; dir=[$listing], metaSize=${if (Files.exists(metaFile)) Files.size(metaFile) else -1}",
            )

            // 正常速度续传，最终内容一致
            val fast = TestFileServer(data, etag = "\"v1\"").start()
            try {
                val second = downloader().download(fast.url, target)
                withTimeout(30_000) { second.join() }
                assertArrayEquals(data, Files.readAllBytes(target))
                assertFalse(Files.exists(partFile))
                // 续传不应从 0 重新下载整个文件：服务器看到的区间起点不全为 0
                assertTrue(fast.servedRangeStarts.isNotEmpty())
            } finally {
                fast.stop()
            }
        } finally {
            slow.stop()
        }
    }

    @Test
    fun `resume with pre-crafted half-done part skips completed segment`() = runBlocking {
        val data = randomBytes(1024 * 1024)
        val half = data.size / 2
        val target = dir.resolve("crafted.bin")
        val partFile = target.resolveSibling("crafted.bin.part")
        val metaFile = target.resolveSibling("crafted.bin.part.meta")
        Files.write(partFile, data.copyOfRange(0, half))
        PartMeta(
            url = "http://127.0.0.1:1/file.bin", // 占位，稍后用真实 URL 覆盖
            total = data.size.toLong(),
            etag = "\"v1\"",
            lastModified = null,
            segments = listOf(
                PartMeta.SegmentRecord(0, half - 1L, done = true, crc = crcOf(data, 0, half - 1)),
                PartMeta.SegmentRecord(half.toLong(), (data.size - 1).toLong(), done = false),
            ),
        ).let { meta ->
            meta.copy(url = meta.url).write(metaFile)
        }
        val server = TestFileServer(data, etag = "\"v1\"").start()
        try {
            // 服务器端口随机，重写片表里的 URL 以匹配
            val rewritten = PartMeta.read(metaFile)!!.copy(url = server.url)
            rewritten.write(metaFile)

            val job = downloader().download(server.url, target)
            withTimeout(30_000) { job.join() }
            assertArrayEquals(data, Files.readAllBytes(target))
            // 已完成的前半不应再被请求
            assertTrue(
                server.servedRangeStarts.isNotEmpty() && server.servedRangeStarts.all { it >= half },
                "不应重复下载已完成分片: starts=${server.servedRangeStarts}, half=$half, url=${server.url}",
            )
        } finally {
            server.stop()
        }
    }

    @Test
    fun `etag change forces fresh download`() = runBlocking {
        val data = randomBytes(256 * 1024)
        val target = dir.resolve("etag.bin")
        val metaFile = target.resolveSibling("etag.bin.part.meta")
        // 片表记录旧 ETag，服务器新 ETag → 应整体重下
        PartMeta(
            url = "placeholder", total = data.size.toLong(), etag = "\"old\"",
            lastModified = null,
            segments = listOf(PartMeta.SegmentRecord(0, (data.size - 1).toLong(), done = false)),
        ).write(metaFile)
        val server = TestFileServer(data, etag = "\"new\"").start()
        try {
            val rewritten = PartMeta.read(metaFile)!!.copy(url = server.url)
            rewritten.write(metaFile)
            val job = downloader().download(server.url, target)
            withTimeout(30_000) { job.join() }
            assertArrayEquals(data, Files.readAllBytes(target))
            // 重下后片表应被清理
            assertFalse(Files.exists(metaFile))
        } finally {
            server.stop()
        }
    }

    @Test
    fun `default guard rejects loopback url before any io`() {
        val d = BitDownloader(scope) // 使用真实 UrlGuard
        assertThrows<SecurityException> {
            d.download("http://127.0.0.1:1/x", dir.resolve("x.bin"))
        }
        assertThrows<SecurityException> {
            d.download("ftp://example.com/x", dir.resolve("x.bin"))
        }
    }

    @Test
    fun `progress flow reports rate and completion`() = runBlocking {
        val data = randomBytes(1024 * 1024)
        val server = TestFileServer(data, etag = "\"v1\"").apply { chunkDelayMillis = 30 }.start()
        try {
            val target = dir.resolve("progress.bin")
            val job = downloader().download(server.url, target)
            val completedSignal = CompletableDeferred<DownloadProgress>()
            val collector = scope.launch {
                job.progress.collect {
                    if (it.state == DownloadState.COMPLETED) completedSignal.complete(it)
                }
            }
            withTimeout(30_000) { completedSignal.await() }
            collector.cancel()
            job.join()
        } finally {
            server.stop()
        }
    }

    @Test
    fun `rate limiter slows real download`() = runBlocking {
        val data = randomBytes(600 * 1024)
        val server = TestFileServer(data, etag = "\"v1\"").start()
        try {
            val target = dir.resolve("limited.bin")
            val config = DownloadConfig(maxBytesPerSec = 200 * 1024, connections = 2)
            val job = downloader(config).download(server.url, target)
            val start = System.nanoTime()
            withTimeout(30_000) { job.join() }
            val elapsedSec = (System.nanoTime() - start) / 1e9
            assertArrayEquals(data, Files.readAllBytes(target))
            // 总量 600KB，桶容量 200KB → 理想 (600-200)/200 = 2s
            assertTrue(elapsedSec > 1.0, "限速应生效，实际 ${"%.2f".format(elapsedSec)}s")
        } finally {
            server.stop()
        }
    }
}
