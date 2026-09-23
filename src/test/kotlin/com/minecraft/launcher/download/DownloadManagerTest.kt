package com.minecraft.launcher.download

import com.sun.net.httpserver.HttpServer
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
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
import kotlin.math.min
import kotlin.random.Random

/**
 * DownloadManager 编排层测试：任务入队/进度映射/终态移除、取消保留断点、SSRF 拒绝不入队。
 * 本地回环服务器通过注入恒通过 guard 绕过（真实拦截由 UrlGuardTest 覆盖）。
 */
class DownloadManagerTest {

    @TempDir
    lateinit var dir: Path

    private val bypassGuard: (String) -> URI = { URI(it) }

    private class Server(private val data: ByteArray, private val chunkDelayMillis: Long = 0) {
        var http: HttpServer? = null
        val url: String get() = "http://127.0.0.1:${http!!.address.port}/f.bin"

        fun start(): Server {
            val s = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
            s.createContext("/f.bin") { ex ->
                ex.use {
                    val range = it.requestHeaders.getFirst("Range")?.removePrefix("bytes=")
                    if (range != null && it.requestMethod == "GET") {
                        val parts = range.split("-")
                        val start = parts[0].toLong()
                        val end = parts.getOrNull(1)?.takeIf { e -> e.isNotEmpty() }?.toLong() ?: (data.size - 1).toLong()
                        it.responseHeaders.add("Content-Range", "bytes $start-$end/${data.size}")
                        it.sendResponseHeaders(206, end - start + 1)
                        var pos = start
                        while (pos <= end) {
                            val n = min(65536L, end - pos + 1).toInt()
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

    private fun manager() = DownloadManager(BitFileDownloader(urlGuard = bypassGuard))

    @Test
    fun `start enqueues task and marks completed in history`() = runBlocking {
        val data = ByteArray(1024 * 1024).also { Random(3).nextBytes(it) }
        val server = Server(data).start()
        val target = dir.resolve("ok.bin")
        try {
            manager().use { dm ->
                val id = dm.start(server.url, target)
                val initial = withTimeout(5_000) { dm.tasksFlow().first { it.isNotEmpty() } }
                assertEquals(1, initial.size)
                assertEquals(id, initial.first().id)
                assertEquals("ok.bin", initial.first().name)

                val done = withTimeout(30_000) { dm.tasksFlow().first { tasks -> tasks.any { it.id == id && it.state == DownloadState.COMPLETED } } }
                assertEquals(1f, done.first { it.id == id }.fraction, 0.001f)
                assertArrayEquals(data, Files.readAllBytes(target))
            }
        } finally {
            server.stop()
        }
    }

    @Test
    fun `progress fraction increases during download`() = runBlocking {
        val data = ByteArray(4 * 1024 * 1024)
        val server = Server(data, chunkDelayMillis = 30).start()
        val target = dir.resolve("slow.bin")
        try {
            manager().use { dm ->
                dm.start(server.url, target)
                val seen = mutableListOf<Float>()
                withTimeout(30_000) {
                    dm.tasksFlow().takeWhile { tasks -> tasks.any { it.state == DownloadState.CONNECTING || it.state == DownloadState.DOWNLOADING } }.collect { tasks ->
                        seen += tasks.first().fraction
                    }
                }
                assertTrue(seen.any { it > 0f }, "进度应出现非零值: $seen")
                assertArrayEquals(data, Files.readAllBytes(target))
            }
        } finally {
            server.stop()
        }
    }

    @Test
    fun `cancel keeps history entry and part file`() = runBlocking {
        val data = ByteArray(8 * 1024 * 1024)
        val server = Server(data, chunkDelayMillis = 40).start()
        val target = dir.resolve("cancel.bin")
        try {
            manager().use { dm ->
                val id = dm.start(server.url, target)
                withTimeout(5_000) { dm.tasksFlow().first { tasks -> tasks.any { it.id == id && it.fraction > 0f } } }
                dm.cancel(id)
                withTimeout(10_000) { dm.tasksFlow().first { tasks -> tasks.any { it.id == id && it.state == DownloadState.CANCELLED } } }
                assertTrue(Files.exists(target.resolveSibling("cancel.bin.part")), "取消后应保留 .part")
            }
        } finally {
            server.stop()
        }
    }

    @Test
    fun `multiple tasks tracked concurrently`() = runBlocking {
        val a = ByteArray(512 * 1024)
        val b = ByteArray(768 * 1024)
        val sa = Server(a).start()
        val sb = Server(b).start()
        try {
            manager().use { dm ->
                dm.start(sa.url, dir.resolve("a.bin"))
                dm.start(sb.url, dir.resolve("b.bin"))
                val both = withTimeout(5_000) { dm.tasksFlow().first { it.size >= 2 } }
                assertEquals(setOf("a.bin", "b.bin"), both.map { it.name }.toSet())
                withTimeout(30_000) { dm.tasksFlow().first { tasks -> tasks.size == 2 && tasks.all { it.state == DownloadState.COMPLETED } } }
                assertArrayEquals(a, Files.readAllBytes(dir.resolve("a.bin")))
                assertArrayEquals(b, Files.readAllBytes(dir.resolve("b.bin")))
            }
        } finally {
            sa.stop()
            sb.stop()
        }
    }

    @Test
    fun `invalid url throws and does not enqueue`() {
        val dm = DownloadManager() // 真实 guard
        assertThrows(SecurityException::class.java) {
            dm.start("http://127.0.0.1:9999/f.bin", dir.resolve("x.bin"))
        }
        assertTrue(runBlocking { dm.tasksFlow().value }.isEmpty())
    }

    @Test
    fun `trackExternal aggregates progress into same queue`() = runBlocking {
        manager().use { dm ->
            val task = dm.trackExternal("安装 9.9.9", "mojang://version/9.9.9")
            assertEquals(1, dm.tasksFlow().value.size)
            assertEquals("安装 9.9.9", dm.tasksFlow().value.first().name)

            task.update(0.5f)
            assertEquals(0.5f, dm.tasksFlow().value.first().fraction, 0.001f)

            task.complete()
            val done = dm.tasksFlow().value.first()
            assertEquals(DownloadState.COMPLETED, done.state)
            assertEquals(1f, done.fraction, 0.001f)
        }
    }
}
