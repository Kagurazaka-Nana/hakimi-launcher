package com.minecraft.launcher.wiki

import com.fasterxml.jackson.databind.ObjectMapper
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** WS 帧 → 状态应用、媒体地址解析（协议契约 docs/Wiki.md §3）。 */
class WikiProtocolTest {

    private val mapper = ObjectMapper()

    private fun node(json: String) = mapper.readTree(json)

    @Test
    fun `hello 置为已连接并记录版本`() {
        val s = WikiClient.applyFrame(WikiUiState(), node("""{"type":"hello","server":"hakimi-wiki","version":"1"}"""))
        assertEquals(WikiConnection.CONNECTED, s.connection)
        assertEquals("1", s.serverVersion)
    }

    @Test
    fun `articles 快照解析`() {
        val s = WikiClient.applyFrame(
            WikiUiState(connection = WikiConnection.CONNECTED),
            node("""{"type":"articles","items":[{"id":"a1","title":"T","category":"MOD","summary":"S","updatedAt":"2026-01-01T00:00:00Z"}]}"""),
        )
        assertEquals(1, s.articles.size)
        assertEquals("a1", s.articles.first().id)
        assertEquals("MOD", s.articles.first().category)
    }

    @Test
    fun `article 详情与坏帧容错`() {
        val s = WikiClient.applyFrame(
            WikiUiState(),
            node("""{"type":"article","article":{"id":"x","title":"标题","category":"GUIDE","html":"<p>hi</p>","author":"我","updatedAt":"u"}}"""),
        )
        assertEquals("<p>hi</p>", s.detail?.html)
        // 未知类型与缺字段不炸、不覆盖
        val same = WikiClient.applyFrame(s, node("""{"type":"pong"}"""))
        assertEquals(s, same)
        assertTrue(WikiClient.applyFrame(s, node("""{"type":"articles","items":[]}""")).articles.isEmpty())
    }

    @Test
    fun `error 帧进入状态`() {
        val s = WikiClient.applyFrame(WikiUiState(), node("""{"type":"error","message":"词条不存在或未通过审核"}"""))
        assertEquals("词条不存在或未通过审核", s.error)
    }

    @Test
    fun `相对媒体按 ws 地址解析为同源 http`() {
        assertEquals("http://127.0.0.1:8730/media/a.png", WikiClient.resolveMediaUrl("/media/a.png", "ws://127.0.0.1:8730/ws"))
        assertEquals("https://wiki.example.com/media/a.png", WikiClient.resolveMediaUrl("/media/a.png", "wss://wiki.example.com/ws"))
        assertEquals("https://cdn.example/x.png", WikiClient.resolveMediaUrl("https://cdn.example/x.png", "ws://127.0.0.1:8730/ws"))
    }

    @Test
    fun `出站帧引号转义正确`() {
        val raw = "a\"b\\c"
        val frame = """{"type":"open","id":${jsonString(raw)}}"""
        val parsed = node(frame)
        assertEquals(raw, parsed.path("id").asText())
    }
}
