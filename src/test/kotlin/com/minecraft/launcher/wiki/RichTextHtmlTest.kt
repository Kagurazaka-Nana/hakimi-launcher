package com.minecraft.launcher.wiki

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** HTML 子集 → RichBlock 解析（docs/Wiki.md §6.2）。 */
class RichTextHtmlTest {

    @Test
    fun `标题段落与行内样式`() {
        val blocks = RichTextHtml.parse("<h2>铁傀儡</h2><p>它是<strong>友军</strong>，会保护<em>村民</em>。</p>")
        assertEquals(2, blocks.size)
        val h = blocks[0] as RichBlock.Heading
        assertEquals(2, h.level)
        assertEquals("铁傀儡", h.text.text)
        val p = blocks[1] as RichBlock.Paragraph
        assertEquals("它是友军，会保护村民。", p.text.text)
        // strong/em 各有 1 个 span 样式区间
        assertTrue(p.text.spanStyles.size >= 2, "行内样式未生效")
    }

    @Test
    fun `列表与图片视频块`() {
        val html = """
            <ul><li>小麦</li><li>胡萝卜</li></ul>
            <ol><li>第一步</li></ol>
            <img src="/media/farm.png" alt="农场">
            <video src="/media/demo.mp4" controls=""></video>
        """.trimIndent()
        val blocks = RichTextHtml.parse(html)
        assertEquals(4, blocks.size)
        assertEquals(listOf("小麦", "胡萝卜"), (blocks[0] as RichBlock.BulletList).items.map { it.text })
        assertEquals(1, (blocks[1] as RichBlock.NumberedList).items.size)
        val img = blocks[2] as RichBlock.Image
        assertEquals("/media/farm.png", img.url)
        assertEquals("农场", img.alt)
        val video = blocks[3] as RichBlock.Video
        assertEquals("/media/demo.mp4", video.url)
    }

    @Test
    fun `代码块与引用保留文本并解码实体`() {
        val blocks = RichTextHtml.parse("<pre>if (a &lt; b) { x &amp;&amp; y }</pre><blockquote>注意 &quot;安全区&quot;</blockquote>")
        val code = blocks[0] as RichBlock.CodeBlock
        assertEquals("if (a < b) { x && y }", code.code)
        assertEquals("注意 \"安全区\"", (blocks[1] as RichBlock.Quote).text.text)
    }

    @Test
    fun `br 换行与未知标签防御`() {
        val p = RichTextHtml.parse("<p>第一行<br>第二行</p>")[0] as RichBlock.Paragraph
        assertEquals("第一行\n第二行", p.text.text)
        // 不在白名单输入里出现未知标签时也不崩溃
        assertTrue(RichTextHtml.parse("<div>意外</div><p>正常</p>").any { it is RichBlock.Paragraph && it.text.text == "正常" })
    }

    @Test
    fun `空行段落被丢弃`() {
        assertEquals(1, RichTextHtml.parse("<p>   </p><p>内容</p>").size)
    }
}
