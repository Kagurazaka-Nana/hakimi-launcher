package com.minecraft.launcher.wiki

import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking

/**
 * 端到端契约测试：真实 WikiClient ↔ hakimi-wiki 后端（WebSocket 握手/首屏快照/open 详情）。
 * 后端未运行时自动跳过（不拖挂 CI）；启动方式见 hakimi-wiki/backend。
 */
class WikiClientIntegrationTest {

    private val healthUrl = "http://127.0.0.1:8730/health"

    private fun backendUp(): Boolean = runCatching {
        HttpClient.newHttpClient()
            .send(
                HttpRequest.newBuilder(URI(healthUrl)).timeout(Duration.ofSeconds(2)).GET().build(),
                HttpResponse.BodyHandlers.ofString(),
            ).statusCode() == 200
    }.getOrDefault(false)

    @Test
    fun `连接后端后收到首屏快照并可打开词条`() = runBlocking {
        org.junit.jupiter.api.Assumptions.assumeTrue(backendUp(), "hakimi-wiki 后端未启动，跳过端到端测试")
        val client = WikiClient()
        client.start()
        withTimeoutPoll { client.state.value.connection == WikiConnection.CONNECTED }
        assertTrue(client.state.value.articles.isNotEmpty(), "首屏 articles 应非空（种子词条）")

        val target = client.state.value.articles.first()
        client.open(target.id)
        withTimeoutPoll { client.state.value.detail?.id == target.id }
        val detail = client.state.value.detail!!
        assertTrue(detail.html.contains("<"))

        client.search("不存在xyzzy", null)
        withTimeoutPoll { client.state.value.articles.all { !it.title.contains("不存在xyzzy") } }
        client.stop()
    }

    private suspend fun withTimeoutPoll(
        timeoutMs: Long = 8_000,
        intervalMs: Long = 100,
        condition: suspend () -> Boolean,
    ) {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            if (condition()) return
            delay(intervalMs)
        }
        assertEquals(true, condition(), "轮询超时")
    }
}
