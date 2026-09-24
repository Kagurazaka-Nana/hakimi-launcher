package com.minecraft.launcher.download

import com.sun.net.httpserver.HttpServer
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.net.InetSocketAddress
import java.net.URI
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.CancellationException
import java.util.concurrent.Executors
import kotlin.random.Random

/**
 * FileDownloader 接口契约测试：阻塞/异步入口、失败传播、取消语义、SSRF 拦截。
 * 本地回环测试服务器通过注入恒通过 guard 绕过（真实拦截行为由 UrlGuardTest 覆盖）。
 */
class FileDownloaderTest {

    @TempDir
    lateinit var dir: Path

    private val bypassGuard: (String) -> URI = { URI(it) }

    private class Server(private val data: ByteArray, private val status: Int = 200, private val chunkDelayMillis: Long = 0) {
        var http: HttpServer? = null
        val url: String get() = "http://127.0.0.1:${http!!.address.port}/f.bin"

        fun start(): Server {
            val s = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
            s.createContext("/f.bin") { ex ->
                ex.use {
                    if (status != 200) {
                        it.sendResponseHeaders(status, -1)
                        return@use
                    }
                    val range = it.requestHeaders.getFirst("Range")?.removePrefix("bytes=")
                    if (range != null && it.requestMethod == "GET") {
                        val parts = range.split("-")
                        val start = parts[0].toLong()
                        val end = parts.getOrNull(1)?.takeIf { e -> e.isNotEmpty() }?.toLong() ?: (data.size - 1).toLong()
                        it.responseHeaders.add("Content-Range", "bytes $start-$end/${data.size}")
                        it.sendResponseHeaders(206, end - start + 1)
                        var pos = start
                        while (pos <= end) {
                            val n = minOf(65536L, end - pos + 1).toInt()
                            it.responseBody.write(data, pos.toInt(), n)
                            pos += n
                            if (chunkDelayMillis > 0) Thread.sleep(chunkDelayMillis)
                        }
                    } else {
                        it.responseHeaders.add("Accept-Ranges", "bytes")
                        it.sendResponseHeaders(200, data.size.toLong())
                        if (it.requestMethod != "HEAD") it.responseBody.write(data)
                    }
                }
            }
            s.executor = Executors.newVirtualThreadPerTaskExecutor()
            s.start()
            http = s
            return this
        }

        fun stop() = http?.stop(0)
    }

    private fun downloader(config: DownloadConfig = DownloadConfig.defaults()) =
        BitFileDownloader(config, bypassGuard)

    @Test
    fun `downloadBlocking returns path and exact content`() {
        val data = ByteArray(1024 * 1024).also { Random(7).nextBytes(it) }
        val server = Server(data).start()
        val target = dir.resolve("f.bin")
        try {
            downloader().use { dl ->
                val result = dl.downloadBlocking(server.url, target)
                assertEquals(target, result)
                assertArrayEquals(data, Files.readAllBytes(target))
            }
        } finally {
            server.stop()
        }
    }

    @Test
    fun `downloadBlocking propagates server failure`() {
        val server = Server(ByteArray(0), status = 404).start()
        try {
            downloader().use { dl ->
                assertThrows(Exception::class.java) {
                    dl.downloadBlocking(server.url, dir.resolve("missing.bin"))
                }
            }
        } finally {
            server.stop()
        }
    }

    @Test
    fun `default guard rejects loopback before any io`() {
        val dl = BitFileDownloader() // 真实 UrlGuard
        assertThrows(SecurityException::class.java) {
            dl.downloadBlocking("http://127.0.0.1:9999/f.bin", dir.resolve("x.bin"))
        }
        assertThrows(SecurityException::class.java) {
            dl.download("http://localhost:9999/f.bin", dir.resolve("x.bin"))
        }
    }

    @Test
    fun `async job awaitCompletion and cancel semantics`() {
        val data = ByteArray(2 * 1024 * 1024)
        val server = Server(data).start()
        val target = dir.resolve("async.bin")
        try {
            downloader().use { dl ->
                val job = dl.download(server.url, target)
                job.awaitCompletionWithin(30)
                assertArrayEquals(data, Files.readAllBytes(target))

                // 取消：awaitCompletion 抛 CancellationException，.part 保留
                val slowTarget = dir.resolve("slow.bin")
                val slowServer = Server(ByteArray(8 * 1024 * 1024), chunkDelayMillis = 60).start()
                try {
                    val slowJob = dl.download(slowServer.url, slowTarget)
                    awaitUntil(10_000) { slowJob.lastProgress().state == DownloadState.DOWNLOADING }
                    slowJob.cancel()
                    assertThrows(CancellationException::class.java) { slowJob.awaitCompletion() }
                    assertTrue(Files.exists(slowTarget.resolveSibling("slow.bin.part")))
                } finally {
                    slowServer.stop()
                }
            }
        } finally {
            server.stop()
        }
    }

    @Test
    fun `progress flow reports completion`() {
        val data = ByteArray(1024 * 1024)
        val server = Server(data).start()
        try {
            downloader().use { dl ->
                val target = dir.resolve("progress.bin")
                val job = dl.download(server.url, target)
                job.awaitCompletionWithin(30)
                assertEquals(DownloadState.COMPLETED, job.lastProgress().state)
                assertFalse(Files.exists(target.resolveSibling("progress.bin.part")))
            }
        } finally {
            server.stop()
        }
    }
}
