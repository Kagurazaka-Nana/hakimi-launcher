package com.minecraft.launcher.download

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import com.sun.net.httpserver.HttpServer
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.net.InetSocketAddress
import java.net.URI
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.Executors
import kotlin.random.Random

/**
 * FileDownloader 接口契约测试：阻塞/异步入口、失败传播、取消语义、SSRF 校验。
 * 本地回环测试服务器通过注入恒通过 guard 绕过（真实拦截行为由 UrlGuardTest 覆盖）。
 */
class FileDownloaderTest {

    @TempDir
    lateinit var dir: Path

    private val bypassGuard: (String) -> URI = { URI(it) }

    private class Server(private val data: ByteArray, private val status: Int = 200) {
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
                        it.responseBody.write(data, start.toInt(), (end - start + 1).toInt())
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

    @Test
    fun `downloadBlocking returns path and exact content`() {
        val data = ByteArray(1024 * 1024).also { Random(7).nextBytes(it) }
        val server = Server(data).start()
        val target = dir.resolve("f.bin")
        try {
            BitFileDownloader(urlGuard = bypassGuard).use { dl ->
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
            BitFileDownloader(urlGuard = bypassGuard).use { dl ->
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
    fun `async job awaitCompletion and cancel semantics`() = runBlocking {
        val data = ByteArray(2 * 1024 * 1024)
        val server = Server(data).start()
        val target = dir.resolve("async.bin")
        try {
            BitFileDownloader(urlGuard = bypassGuard).use { dl ->
                val job = dl.download(server.url, target)
                withTimeout(30_000) { job.awaitCompletion() }
                assertArrayEquals(data, Files.readAllBytes(target))

                // 取消：awaitCompletion 抛 CancellationException，.part 保留
                val slowTarget = dir.resolve("slow.bin")
                val slowServer = Server(ByteArray(8 * 1024 * 1024)).start()
                try {
                    val slowJob = dl.download(slowServer.url, slowTarget)
                    val deferred = launch {
                        runCatching { slowJob.awaitCompletion() }
                    }
                    delay(300)
                    slowJob.cancel()
                    withTimeout(10_000) { deferred.join() }
                    assertTrue(Files.exists(slowTarget.resolveSibling("slow.bin.part")))
                    assertThrows(CancellationException::class.java) {
                        runBlocking { slowJob.awaitCompletion() }
                    }
                } finally {
                    slowServer.stop()
                }
            }
        } finally {
            server.stop()
        }
    }
}
