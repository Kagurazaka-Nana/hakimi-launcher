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
import java.util.concurrent.Executors
import kotlin.math.min
import kotlin.random.Random

/**
 * DownloadManager 编排层测试：任务入队/进度映射/历史保留、取消保留断点、SSRF 拒绝不入队、外部任务聚合。
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

    private fun manager() = DownloadManager(BitFileDownloader(DownloadConfig.defaults(), bypassGuard))

    @Test
    fun `start enqueues task and marks completed in history`() {
        val data = ByteArray(1024 * 1024).also { Random(3).nextBytes(it) }
        val server = Server(data).start()
        val target = dir.resolve("ok.bin")
        try {
            manager().use { dm ->
                val id = dm.start(server.url, target)
                val initial = dm.snapshot()
                assertEquals(1, initial.size)
                assertEquals(id, initial.first().getId())
                assertEquals("ok.bin", initial.first().getName())

                awaitUntil { dm.snapshot().any { it.getId() == id && it.getState() == DownloadState.COMPLETED } }
                assertEquals(1f, dm.snapshot().first { it.getId() == id }.getFraction(), 0.001f)
                assertArrayEquals(data, Files.readAllBytes(target))
            }
        } finally {
            server.stop()
        }
    }

    @Test
    fun `progress fraction increases during download`() {
        val data = ByteArray(4 * 1024 * 1024)
        val server = Server(data, chunkDelayMillis = 30).start()
        val target = dir.resolve("slow.bin")
        try {
            manager().use { dm ->
                dm.start(server.url, target)
                awaitUntil(10_000) { dm.snapshot().firstOrNull()?.getFraction()?.let { it > 0f } == true }
                awaitUntil { dm.snapshot().none { it.getState() == DownloadState.DOWNLOADING || it.getState() == DownloadState.CONNECTING } }
                assertArrayEquals(data, Files.readAllBytes(target))
            }
        } finally {
            server.stop()
        }
    }

    @Test
    fun `cancel keeps history entry and part file`() {
        val data = ByteArray(8 * 1024 * 1024)
        val server = Server(data, chunkDelayMillis = 40).start()
        val target = dir.resolve("cancel.bin")
        try {
            manager().use { dm ->
                val id = dm.start(server.url, target)
                awaitUntil(10_000) { dm.snapshot().any { it.getId() == id && it.getFraction() > 0f } }
                dm.cancel(id)
                awaitUntil(10_000) { dm.snapshot().any { it.getId() == id && it.getState() == DownloadState.CANCELLED } }
                val part = target.resolveSibling("cancel.bin.part")
                assertTrue(Files.exists(part), "取消后应保留 .part")
                awaitUntil(10_000) {
                    if (!Files.exists(part)) return@awaitUntil true
                    try {
                        Files.delete(part)
                        true
                    } catch (e: java.io.IOException) {
                        false
                    }
                }
            }
        } finally {
            server.stop()
        }
    }

    @Test
    fun `multiple tasks tracked concurrently`() {
        val a = ByteArray(512 * 1024)
        val b = ByteArray(768 * 1024)
        val sa = Server(a).start()
        val sb = Server(b).start()
        try {
            manager().use { dm ->
                dm.start(sa.url, dir.resolve("a.bin"))
                dm.start(sb.url, dir.resolve("b.bin"))
                val both = dm.snapshot()
                assertEquals(setOf("a.bin", "b.bin"), both.map { it.getName() }.toSet())
                awaitUntil { dm.snapshot().size == 2 && dm.snapshot().all { it.getState() == DownloadState.COMPLETED } }
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
        assertTrue(dm.snapshot().isEmpty())
        dm.close()
    }

    @Test
    fun `resume continues a cancelled task under same id`() {
        val data = ByteArray(4 * 1024 * 1024).also { Random(5).nextBytes(it) }
        val server = Server(data, chunkDelayMillis = 30).start()
        val target = dir.resolve("resume.bin")
        try {
            manager().use { dm ->
                val id = dm.start(server.url, target)
                awaitUntil(10_000) { dm.snapshot().any { it.getId() == id && it.getFraction() > 0f } }
                dm.cancel(id)
                awaitUntil(10_000) { dm.snapshot().any { it.getId() == id && it.getState() == DownloadState.CANCELLED } }

                dm.resume(id)
                awaitUntil(10_000) { dm.snapshot().any { it.getId() == id && (it.getState() == DownloadState.CONNECTING || it.getState() == DownloadState.DOWNLOADING) } }
                awaitUntil(30_000) { dm.snapshot().any { it.getId() == id && it.getState() == DownloadState.COMPLETED } }
                assertArrayEquals(data, Files.readAllBytes(target))
            }
        } finally {
            server.stop()
        }
    }

    @Test
    fun `remove deletes task from queue and keeps part for restart`() {
        val data = ByteArray(4 * 1024 * 1024)
        val server = Server(data, chunkDelayMillis = 30).start()
        val target = dir.resolve("rm.bin")
        try {
            manager().use { dm ->
                val id = dm.start(server.url, target)
                awaitUntil(10_000) { dm.snapshot().any { it.getId() == id && it.getFraction() > 0f } }
                dm.remove(id)
                awaitUntil(5_000) { dm.snapshot().none { it.getId() == id } }
                val part = target.resolveSibling("rm.bin.part")
                assertTrue(Files.exists(part), "移除活跃任务应取消并保留 .part")
                // 取消是异步的：等文件句柄释放（可删除）再结束，避免 @TempDir 清理竞态
                awaitUntil(10_000) {
                    if (!Files.exists(part)) return@awaitUntil true
                    try {
                        Files.delete(part)
                        true
                    } catch (e: java.io.IOException) {
                        false
                    }
                }
            }
        } finally {
            server.stop()
        }
    }

    @Test
    fun `trackExternal aggregates progress into same queue`() {
        manager().use { dm ->
            val task = dm.trackExternal("安装 9.9.9", "mojang://version/9.9.9")
            assertEquals(1, dm.snapshot().size)
            assertEquals("安装 9.9.9", dm.snapshot().first().getName())
            assertFalse(dm.snapshot().first().isPauseable, "外部聚合任务不支持暂停/继续")

            task.update(0.5f)
            assertEquals(0.5f, dm.snapshot().first().getFraction(), 0.001f)

            task.complete()
            val done = dm.snapshot().first()
            assertEquals(DownloadState.COMPLETED, done.getState())
            assertEquals(1f, done.getFraction(), 0.001f)
        }
    }
}
