package com.minecraft.launcher.download

import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.net.InetSocketAddress
import java.net.URI
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.Executors
import kotlin.math.min
import kotlin.random.Random

class BitDownloaderTest {

    @TempDir
    lateinit var dir: Path

    private val bypassGuard: (String) -> URI = { URI(it) }

    private fun downloader(config: DownloadConfig = DownloadConfig.defaults()) =
        BitDownloader(config, bypassGuard)

    // —— 本地 Range HTTP 服务器 ——

    private class TestFileServer(
        private val data: ByteArray,
        private val etag: String,
        private val supportRanges: Boolean = true,
    ) {
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
                        sendBody(ex, 200, 0L, (data.size - 1).toLong())
                        return
                    }
                    // 跳过 bytes=0-0 的能力探测请求，只记录真实分片取数
                    if (!(range.first == 0L && (range.second ?: 0L) == 0L)) {
                        servedRangeStarts.add(range.first)
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
                val n = min(chunk, end - pos + 1).toInt()
                ex.responseBody.write(data, pos.toInt(), n)
                pos += n
                if (chunkDelayMillis > 0) Thread.sleep(chunkDelayMillis)
            }
        }
    }

    private fun randomBytes(size: Int): ByteArray = ByteArray(size).also { Random(42).nextBytes(it) }

    private fun crcOf(data: ByteArray, from: Int, toInclusive: Int): Long {
        val crc = java.util.zip.CRC32C()
        crc.update(data, from, toInclusive - from + 1)
        return crc.value
    }

    // —— 用例 ——

    @Test
    fun `multi-connection download completes with exact content`() {
        val data = randomBytes(2 * 1024 * 1024)
        val server = TestFileServer(data, etag = "\"v1\"").start()
        try {
            val target = dir.resolve("file.bin")
            val job = downloader().download(server.url, target)
            job.awaitCompletionWithin(30)
            assertArrayEquals(data, Files.readAllBytes(target))
            assertFalse(Files.exists(target.resolveSibling("file.bin.part")))
            assertFalse(Files.exists(target.resolveSibling("file.bin.part.meta")))
            val last = job.lastProgress()
            assertEquals(DownloadState.COMPLETED, last.state)
            assertEquals(data.size.toLong(), last.totalBytes)
            assertEquals(data.size.toLong(), last.downloadedBytes)
        } finally {
            server.stop()
        }
    }

    @Test
    fun `single stream fallback when server ignores ranges`() {
        val data = randomBytes(512 * 1024)
        val server = TestFileServer(data, etag = "\"v1\"", supportRanges = false).start()
        try {
            val target = dir.resolve("single.bin")
            val job = downloader().download(server.url, target)
            job.awaitCompletionWithin(30)
            assertArrayEquals(data, Files.readAllBytes(target))
        } finally {
            server.stop()
        }
    }

    @Test
    fun `cancel keeps part then resume completes`() {
        val data = randomBytes(4 * 1024 * 1024)
        val slow = TestFileServer(data, etag = "\"v1\"").apply { chunkDelayMillis = 120 }.start()
        val target = dir.resolve("resume.bin")
        val partFile = target.resolveSibling("resume.bin.part")
        val metaFile = target.resolveSibling("resume.bin.part.meta")
        try {
            val first = downloader().download(slow.url, target)
            // 等到确实开始传输再取消
            awaitUntil(5_000) { first.lastProgress().state == DownloadState.DOWNLOADING }
            first.cancel()
            assertThrows(java.util.concurrent.CancellationException::class.java) { first.awaitCompletion() }
            assertTrue(Files.exists(partFile), "取消后 .part 应保留")
            assertNotNull(PartMeta.read(metaFile), "取消后片表应保留")

            val fast = TestFileServer(data, etag = "\"v1\"").start()
            try {
                // 把片表 URL 改指 fast（同一资源另一镜像的语义），验证真实断点续传
                val m = PartMeta.read(metaFile)!!
                PartMeta(fast.url, m.total(), m.etag(), m.lastModified(), m.segments()).write(metaFile)
                val second = downloader().download(fast.url, target)
                second.awaitCompletionWithin(30)
                assertArrayEquals(data, Files.readAllBytes(target))
                assertFalse(Files.exists(partFile))
                assertTrue(fast.servedRangeStarts.isNotEmpty())
            } finally {
                fast.stop()
            }
        } finally {
            slow.stop()
        }
    }

    @Test
    fun `resume with pre-crafted half-done part skips completed segment`() {
        val data = randomBytes(1024 * 1024)
        val half = data.size / 2
        val target = dir.resolve("crafted.bin")
        val partFile = target.resolveSibling("crafted.bin.part")
        val metaFile = target.resolveSibling("crafted.bin.part.meta")
        Files.write(partFile, data.copyOfRange(0, half))
        val server = TestFileServer(data, etag = "\"v1\"").start()
        try {
            PartMeta(
                server.url,
                data.size.toLong(),
                "\"v1\"",
                null,
                listOf(
                    PartMeta.SegmentRecord(0, half - 1L, true, crcOf(data, 0, half - 1)),
                    PartMeta.SegmentRecord(half.toLong(), (data.size - 1).toLong(), false, 0),
                ),
            ).write(metaFile)

            val job = downloader().download(server.url, target)
            job.awaitCompletionWithin(30)
            assertArrayEquals(data, Files.readAllBytes(target))
            assertTrue(
                server.servedRangeStarts.isNotEmpty() && server.servedRangeStarts.all { it >= half },
                "不应重复下载已完成分片: starts=${server.servedRangeStarts}, half=$half",
            )
        } finally {
            server.stop()
        }
    }

    @Test
    fun `etag change forces fresh download`() {
        val data = randomBytes(256 * 1024)
        val target = dir.resolve("etag.bin")
        val metaFile = target.resolveSibling("etag.bin.part.meta")
        val server = TestFileServer(data, etag = "\"new\"").start()
        try {
            PartMeta(
                server.url, data.size.toLong(), "\"old\"", null,
                listOf(PartMeta.SegmentRecord(0, (data.size - 1).toLong(), false, 0)),
            ).write(metaFile)
            val job = downloader().download(server.url, target)
            job.awaitCompletionWithin(30)
            assertArrayEquals(data, Files.readAllBytes(target))
            assertFalse(Files.exists(metaFile))
        } finally {
            server.stop()
        }
    }

    @Test
    fun `default guard rejects loopback url before any io`() {
        val d = BitDownloader(DownloadConfig.defaults()) // 真实 UrlGuard
        assertThrows(SecurityException::class.java) {
            d.download("http://127.0.0.1:9999/x", dir.resolve("x.bin"))
        }
        assertThrows(SecurityException::class.java) {
            d.download("ftp://example.com/x", dir.resolve("x.bin"))
        }
    }

    @Test
    fun `progress reports downloading then completed`() {
        val data = randomBytes(1024 * 1024)
        val server = TestFileServer(data, etag = "\"v1\"").apply { chunkDelayMillis = 30 }.start()
        try {
            val target = dir.resolve("progress.bin")
            val job = downloader().download(server.url, target)
            awaitUntil(10_000) { job.lastProgress().state == DownloadState.DOWNLOADING }
            job.awaitCompletionWithin(30)
            assertEquals(DownloadState.COMPLETED, job.lastProgress().state)
            assertArrayEquals(data, Files.readAllBytes(target))
        } finally {
            server.stop()
        }
    }

    @Test
    fun `rate limiter slows real download`() {
        val data = randomBytes(600 * 1024)
        val server = TestFileServer(data, etag = "\"v1\"").start()
        try {
            val target = dir.resolve("limited.bin")
            val config = DownloadConfig.builder().maxBytesPerSec(200L * 1024).connections(2).build()
            val start = System.nanoTime()
            downloader(config).download(server.url, target).awaitCompletionWithin(30)
            val elapsedSec = (System.nanoTime() - start) / 1e9
            assertArrayEquals(data, Files.readAllBytes(target))
            // 总量 600KB，桶容量 200KB → 理想 (600-200)/200 = 2s
            assertTrue(elapsedSec > 1.0, "限速应生效，实际 %.2fs".format(elapsedSec))
        } finally {
            server.stop()
        }
    }
}
