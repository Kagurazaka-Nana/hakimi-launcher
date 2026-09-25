package com.minecraft.launcher.wiki

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.sp

/**
 * 后端 Jsoup 白名单产物（docs/Wiki.md §2.5）→ Compose 块的轻量解析器。
 * 只支持清洗器保证会出现的标签集合：h1-h4 / p / ul / ol / li / blockquote / pre
 * 与行内 strong|b / em|i / u / s / code / a[href] / br，及 img[src]、video[src,poster]。
 * 输入不受信但已被服务端白名单约束，这里做防御性解析：未知标签剥离、实体解码。
 */
sealed interface RichBlock {
    data class Heading(val level: Int, val text: AnnotatedString) : RichBlock
    data class Paragraph(val text: AnnotatedString) : RichBlock
    data class Quote(val text: AnnotatedString) : RichBlock
    data class CodeBlock(val code: String) : RichBlock
    data class BulletList(val items: List<AnnotatedString>) : RichBlock
    data class NumberedList(val items: List<AnnotatedString>) : RichBlock
    data class Image(val url: String, val alt: String) : RichBlock
    data class Video(val url: String, val poster: String?) : RichBlock
}

object RichTextHtml {

    /** 块级标签：按出现顺序切分。Jsoup 输出扁平化，块内无嵌套块（列表项仅行内）。 */
    private val BLOCK = Regex(
        """(?s)<(h[1-4])>(.*?)</\1>|<p>(.*?)</p>|<ul>(.*?)</ul>|<ol>(.*?)</ol>|<blockquote>(.*?)</blockquote>|<pre>(.*?)</pre>|<img([^>]*?)/?>|<video([^>]*?)></video>""",
    )

    private val INLINE_TOKEN = Regex("""(?s)<(/?)(a|strong|em|b|i|u|s|code)(\s[^>]*)?>|<br\s*/?>|([^<]+)""")

    private val ATTR = Regex("""([a-zA-Z-]+)="([^"]*)"""")

    /** 解析清洗后的 HTML 为块列表。 */
    fun parse(html: String): List<RichBlock> {
        val blocks = mutableListOf<RichBlock>()
        for (m in BLOCK.findAll(html)) {
            val (tag, open, attrs) = blockParts(m)
            when (tag) {
                "h1", "h2", "h3", "h4" -> blocks += RichBlock.Heading(tag[1] - '0', inline(open))
                "p" -> {
                    val t = inline(open)
                    if (t.text.isNotBlank()) blocks += RichBlock.Paragraph(t)
                }
                "blockquote" -> blocks += RichBlock.Quote(inline(open))
                "pre" -> {
                    val code = decodeEntities(stripTags(open)).trimEnd('\n')
                    if (code.isNotBlank()) blocks += RichBlock.CodeBlock(code)
                }
                "ul" -> blocks += RichBlock.BulletList(listItems(open))
                "ol" -> blocks += RichBlock.NumberedList(listItems(open))
                "img" -> attr(attrs, "src")?.let { blocks += RichBlock.Image(it, attr(attrs, "alt") ?: "") }
                "video" -> attr(attrs, "src")?.let { blocks += RichBlock.Video(it, attr(attrs, "poster")) }
            }
        }
        return blocks
    }

    /** 匹配组顺序与 BLOCK 分支一一对应；返回 (标签名, 内容, 属性串)。 */
    private fun blockParts(m: MatchResult): Triple<String, String, String> {
        val g = m.groupValues
        return when {
            g[1].isNotEmpty() -> Triple(g[1], g[2], "")
            g[3].isNotEmpty() || m.value.startsWith("<p>") -> Triple("p", g[3], "")
            g[4].isNotEmpty() || m.value.startsWith("<ul>") -> Triple("ul", g[4], "")
            g[5].isNotEmpty() || m.value.startsWith("<ol>") -> Triple("ol", g[5], "")
            g[6].isNotEmpty() || m.value.startsWith("<blockquote") -> Triple("blockquote", g[6], "")
            g[7].isNotEmpty() || m.value.startsWith("<pre>") -> Triple("pre", g[7], "")
            m.value.startsWith("<img") -> Triple("img", "", g[8])
            else -> Triple("video", "", g[9])
        }
    }

    private fun listItems(inner: String): List<AnnotatedString> =
        Regex("""(?s)<li>(.*?)</li>""").findAll(inner).map { inline(it.groupValues[1]) }.toList()

    /** 行内解析：样式栈 → AnnotatedString。 */
    fun inline(html: String): AnnotatedString =
        AnnotatedString.Builder().apply {
            val stack = ArrayDeque<Pair<String, String?>>() // 标签 → href
            for (tok in INLINE_TOKEN.findAll(html)) {
                val closing = tok.groupValues[1] == "/"
                val tag = tok.groupValues[2]
                val attrs = tok.groupValues[3]
                val text = tok.groupValues[4]
                val full = tok.value
                when {
                    text.isNotEmpty() -> appendStyled(decodeEntities(text), stack)
                    full == "<br>" || full.startsWith("<br ") -> appendStyled("\n", stack)
                    tag.isNotEmpty() && !closing -> stack.addLast(tag to attr(attrs, "href"))
                    tag.isNotEmpty() && closing -> {
                        val at = stack.withIndex().lastOrNull { it.value.first == tag }?.index
                        if (at != null) stack.removeAt(at)
                    }
                }
            }
        }.toAnnotatedString()

    private fun AnnotatedString.Builder.appendStyled(text: String, stack: List<Pair<String, String?>>) {
        if (text.isEmpty()) return
        val start = length
        append(text)
        val styles = stack.map { (tag, href) -> styleFor(tag, href) }
        styles.forEach { addStyle(it, start, length) }
    }

    private fun styleFor(tag: String, href: String?): SpanStyle = when (tag) {
        "strong", "b" -> SpanStyle(fontWeight = FontWeight.Bold)
        "em", "i" -> SpanStyle(fontStyle = FontStyle.Italic)
        "u" -> SpanStyle(textDecoration = TextDecoration.Underline)
        "s" -> SpanStyle(textDecoration = TextDecoration.LineThrough)
        "code" -> SpanStyle(fontFamily = FontFamily.Monospace, fontSize = 14.sp)
        "a" -> SpanStyle(
            color = LINK_COLOR,
            textDecoration = TextDecoration.Underline,
            fontWeight = FontWeight.SemiBold,
        )
        else -> SpanStyle()
    }

    /** 属性串取值（大小写不敏感键）。 */
    fun attr(attrs: String, key: String): String? =
        ATTR.findAll(attrs).firstOrNull { it.groupValues[1].equals(key, true) }?.groupValues?.get(2)

    private val LINK_COLOR = Color(0xFF2E7D32)

    private val ENTITY = Regex("""&(amp|lt|gt|quot|#39|apos|#(\d+)|#x([0-9a-fA-F]+));""")

    fun decodeEntities(s: String): String = ENTITY.replace(s) { m ->
        when {
            m.groupValues[1] == "amp" -> "&"
            m.groupValues[1] == "lt" -> "<"
            m.groupValues[1] == "gt" -> ">"
            m.groupValues[1] == "quot" -> "\""
            m.groupValues[1] == "apos" || m.groupValues[1] == "#39" -> "'"
            m.groupValues[2].isNotEmpty() -> m.groupValues[2].toIntOrNull()?.let { String(Character.toChars(it)) } ?: m.value
            m.groupValues[3].isNotEmpty() -> m.groupValues[3].toIntOrNull(16)?.let { String(Character.toChars(it)) } ?: m.value
            else -> m.value
        }
    }

    private val TAG = Regex("""<[^>]*>""")

    fun stripTags(s: String): String = TAG.replace(s, "")
}
