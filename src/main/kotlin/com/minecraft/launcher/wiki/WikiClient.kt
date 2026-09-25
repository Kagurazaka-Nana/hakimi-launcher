package com.minecraft.launcher.wiki

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import java.net.URI
import java.net.http.HttpClient
import java.net.http.WebSocket
import java.util.concurrent.CompletionStage
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** 与 wiki 服务端的连接状态。 */
enum class WikiConnection { DISCONNECTED, CONNECTING, CONNECTED }

/** 百科组件的远端状态快照。 */
data class WikiUiState(
    val connection: WikiConnection = WikiConnection.DISCONNECTED,
    val serverVersion: String? = null,
    val articles: List<WikiArticleDto> = emptyList(),
    val detail: WikiArticleDetailDto? = null,
    val error: String? = null,
)

/**
 * Wiki WebSocket 客户端（docs/Wiki.md §6.1）：
 * JDK java.net.http.WebSocket（零新依赖）、指数退避重连（0.5s×2 封顶 10s）、
 * 20s 心跳、入站帧上限 2 MiB（协议约定，docs/Wiki.md §3）。
 * 帧 → 状态为纯函数 [applyFrame]，可脱离网络单测。
 */
class WikiClient(
    private val serverUrl: String = System.getenv("HAKIMI_WIKI_URL")?.takeIf { it.isNotBlank() } ?: DEFAULT_URL,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
    private val mapper: ObjectMapper = ObjectMapper(),
) {

    companion object {
        const val DEFAULT_URL = "ws://127.0.0.1:8730/ws"
        const val MAX_FRAME_BYTES = 2L * 1024 * 1024
        const val HEARTBEAT_MS = 20_000L
        const val RECONNECT_BASE_MS = 500L
        const val RECONNECT_MAX_MS = 10_000L
        const val CONNECT_TIMEOUT_MS = 5_000L

        /**
         * 纯函数：一帧服务端消息 → 新状态。
         * hello 前不并入数据帧；articles 只更新列表（详情由用户主动开合，不做隐式清除——
         * 搜索结果帧同样以 articles 下发，若按成员关系清 detail 会误伤）。
         */
        fun applyFrame(state: WikiUiState, node: JsonNode): WikiUiState = when (node.path("type").asText("")) {
            "hello" -> state.copy(
                connection = WikiConnection.CONNECTED,
                serverVersion = node.path("version").asText(null),
                error = null,
            )
            "articles" -> state.copy(articles = parseArticles(node))
            "article" -> {
                val a = node.path("article")
                if (a.isMissingNode || a.isNull) state else state.copy(detail = parseDetail(a))
            }
            "error" -> state.copy(error = node.path("message").asText("wiki 服务端错误"))
            else -> state // pong / 未知类型忽略
        }

        fun parseArticles(node: JsonNode): List<WikiArticleDto> =
            node.path("items").mapNotNull { item ->
                val id = item.path("id").asText("")
                if (id.isEmpty()) null else WikiArticleDto(
                    id = id,
                    title = item.path("title").asText(""),
                    category = item.path("category").asText("MISC"),
                    summary = item.path("summary").asText(""),
                    updatedAt = item.path("updatedAt").asText(""),
                )
            }

        fun parseDetail(node: JsonNode): WikiArticleDetailDto = WikiArticleDetailDto(
            node.path("id").asText(""),
            node.path("title").asText(""),
            node.path("category").asText("MISC"),
            node.path("html").asText(""),
            node.path("author").asText(""),
            node.path("updatedAt").asText(""),
            node.path("version").asInt(1),
        )

        /** ws(s)://host[:port]/… → http(s)://host[:port]（生产形态 wss 对应 https）。 */
        fun httpOriginOf(wsUrl: String): String {
            val https = wsUrl.startsWith("wss://")
            val hostPort = wsUrl.substringAfter("://").substringBefore('/')
            return (if (https) "https://" else "http://") + hostPort
        }

        /** 站内相对媒体地址按 wiki origin 补全；外链原样（服务端清洗已限定协议）。 */
        fun resolveMediaUrl(url: String, wsUrl: String): String =
            if (url.startsWith("/")) httpOriginOf(wsUrl) + url else url
    }

    private val _state = MutableStateFlow(WikiUiState())
    val state: StateFlow<WikiUiState> = _state.asStateFlow()

    val serverUrlValue: String get() = serverUrl

    private val webSocket = AtomicReference<WebSocket?>(null)
    private val outgoing = Channel<String>(Channel.UNLIMITED)
    private var job: Job? = null

    fun start() {
        if (job?.isActive == true) return
        job = scope.launch {
            var backoff = RECONNECT_BASE_MS
            while (isActive) {
                _state.update { it.copy(connection = WikiConnection.CONNECTING) }
                val cause = runSession()
                if (!isActive) break
                _state.update { it.copy(connection = WikiConnection.DISCONNECTED, error = cause ?: _state.value.error) }
                delay(backoff)
                backoff = (backoff * 2).coerceAtMost(RECONNECT_MAX_MS)
            }
        }
    }

    fun stop() {
        job?.cancel()
        job = null
        webSocket.get()?.abort()
        _state.update { it.copy(connection = WikiConnection.DISCONNECTED) }
    }

    fun refresh() = send("""{"type":"list"}""")

    fun search(query: String, category: String?) {
        val cat = if (category == null) "" else ""","category":${jsonString(category)}"""
        send("""{"type":"search","q":${jsonString(query)}$cat}""")
    }

    fun open(id: String) = send("""{"type":"open","id":${jsonString(id)}}""")

    fun closeDetail() = _state.update { it.copy(detail = null) }

    /** 出站帧经单一泵协程串行写，避免并发 sendText。 */
    private fun send(frame: String) {
        scope.launch { outgoing.send(frame) }
    }

    private suspend fun runSession(): String? {
        val inbound = Channel<String>(Channel.UNLIMITED)
        val ws = try {
            val future = HttpClient.newHttpClient().newWebSocketBuilder()
                .connectTimeout(java.time.Duration.ofMillis(CONNECT_TIMEOUT_MS))
                .buildAsync(URI(serverUrl), Listener(inbound))
            withContext(Dispatchers.IO) { future.get(CONNECT_TIMEOUT_MS, TimeUnit.MILLISECONDS) }
        } catch (e: Exception) {
            return e.message ?: e.javaClass.simpleName
        }
        webSocket.set(ws)
        val pump = scope.launch {
            for (frame in outgoing) {
                runCatching { ws.sendText(frame, true).get() }.getOrNull() ?: break
            }
        }
        val heartbeat = scope.launch {
            while (isActive) {
                delay(HEARTBEAT_MS)
                runCatching { ws.sendText("""{"type":"ping"}""", true).get() }.getOrNull() ?: break
            }
        }
        return try {
            while (true) {
                val payload = inbound.receive()
                if (payload.isEmpty()) break // 空串 = 关闭信号
                val node = runCatching { mapper.readTree(payload) }.getOrNull() ?: continue
                _state.update { applyFrame(it, node) }
            }
            null
        } catch (e: Exception) {
            e.message ?: "连接中断"
        } finally {
            heartbeat.cancel()
            pump.cancel()
            webSocket.set(null)
            runCatching { ws.abort() }
        }
    }

    private class Listener(private val inbound: Channel<String>) : WebSocket.Listener {
        private val text = StringBuilder()

        override fun onOpen(webSocket: WebSocket) {
            webSocket.request(1)
        }

        /** GraalVM JDK 25 的 Listener 仍为 onText（无 onTextData/maxFrameSize），帧长超协议上限应用层防御。 */
        override fun onText(webSocket: WebSocket, data: CharSequence, last: Boolean): CompletionStage<*>? {
            text.append(data)
            if (text.length <= WikiClient.MAX_FRAME_BYTES) {                if (last) {
                    inbound.trySend(text.toString())
                    text.setLength(0)
                }
                webSocket.request(1)
            } else {
                // 超协议帧上限：丢弃并断开
                inbound.trySend("")
                inbound.close()
                webSocket.abort()
            }
            return null
        }

        override fun onClose(webSocket: WebSocket, statusCode: Int, reason: String): CompletionStage<*>? {
            inbound.trySend("")
            inbound.close()
            return null
        }

        override fun onError(webSocket: WebSocket, error: Throwable) {
            inbound.trySend("")
            inbound.close()
        }
    }
}

/** JSON 字符串字面量（含引号与控制字符转义）。 */
internal fun jsonString(value: String): String {
    val sb = StringBuilder("\"")
    for (c in value) {
        when (c) {
            '"' -> sb.append("\\\"")
            '\\' -> sb.append("\\\\")
            '\n' -> sb.append("\\n")
            '\r' -> sb.append("\\r")
            '\t' -> sb.append("\\t")
            else -> if (c < ' ') sb.append("\\u%04x".format(c.code)) else sb.append(c)
        }
    }
    return sb.append('"').toString()
}
