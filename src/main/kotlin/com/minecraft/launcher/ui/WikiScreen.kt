package com.minecraft.launcher.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.minecraft.launcher.ui.components.PageHeader
import com.minecraft.launcher.ui.state.LauncherViewModel
import com.minecraft.launcher.ui.theme.HakimiCard
import com.minecraft.launcher.ui.theme.HakimiChip
import com.minecraft.launcher.ui.theme.HakimiIcon
import com.minecraft.launcher.ui.theme.HakimiIconButton
import com.minecraft.launcher.ui.theme.HakimiSearchField
import com.minecraft.launcher.ui.theme.HakimiText
import com.minecraft.launcher.ui.theme.HakimiTheme
import com.minecraft.launcher.ui.theme.PixelShape
import com.minecraft.launcher.wiki.RichBlock
import com.minecraft.launcher.wiki.RichTextHtml
import com.minecraft.launcher.wiki.WikiArticleDetailDto
import com.minecraft.launcher.wiki.WikiArticleDto
import com.minecraft.launcher.wiki.WikiConnection
import com.minecraft.launcher.wiki.WikiUiState
import java.awt.Desktop
import java.net.URI
import kotlinx.coroutines.delay

/** 分类 → 中文标签（与服务端 ArticleCategory 枚举对齐）。 */
private val CATEGORY_LABELS = linkedMapOf(
    "ALL" to "全部",
    "LAUNCHER" to "启动器",
    "MOD" to "模组",
    "VERSION" to "版本",
    "GUIDE" to "攻略",
    "MISC" to "其他",
)

private fun categoryLabel(key: String) = CATEGORY_LABELS[key] ?: key

/**
 * Wiki 百科：经 WebSocket 速查 hakimi-wiki 已批准词条（docs/Wiki.md §6.2）。
 * 在线 → 服务端列表/详情；断线 → 回退本地 Stub 文章，页面不空。
 */
@Composable
fun WikiScreen(vm: LauncherViewModel) {
    val state by vm.state.collectAsState()
    val wiki by vm.wikiState.collectAsState()
    val c = HakimiTheme.colors

    var query by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("ALL") }

    // 搜索/切类防抖：300ms 后经服务端过滤
    LaunchedEffect(query, category) {
        delay(300)
        vm.wiki.search(query, category.takeIf { it != "ALL" })
    }

    val detail = wiki.detail
    if (detail != null) {
        WikiDetailView(wiki = wiki, detail = detail, onBack = { vm.wiki.closeDetail() })
        return
    }

    val online = wiki.connection == WikiConnection.CONNECTED && (wiki.articles.isNotEmpty() || query.isEmpty())
    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        PageHeader(icon = HakimiIcons.Wiki, title = "Wiki", subtitle = "模组 · 版本 · 攻略 · 启动器百科", trailing = { ConnectionBadge(wiki) })

        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            HakimiSearchField(
                value = query,
                onValueChange = { query = it },
                placeholder = "搜索词条…",
                icon = HakimiIcons.Search,
                modifier = Modifier.weight(1f),
            )
            HakimiChip(
                text = "本地",
                color = if (online) c.accent else c.error,
                onClick = { vm.wiki.refresh() },
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CATEGORY_LABELS.forEach { (key, label) ->
                HakimiChip(text = label, color = c.primary, selected = category == key, onClick = { category = key })
            }
        }

        if (!online && wiki.articles.isEmpty()) {
            // 断线回退：展示本地 Stub 文章
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(state.wiki, key = { it.id }) { article ->
                    HakimiCard(modifier = Modifier.fillMaxWidth()) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    HakimiText(article.title, style = HakimiTheme.type.title)
                                    HakimiChip(article.category, color = c.accent)
                                }
                                HakimiText(article.excerpt, style = HakimiTheme.type.body, color = c.textMuted)
                            }
                            HakimiText(article.updated, style = HakimiTheme.type.caption, color = c.textMuted, modifier = Modifier.width(64.dp))
                        }
                    }
                }
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(wiki.articles, key = { it.id }) { article ->
                    WikiArticleCard(article) { vm.wiki.open(article.id) }
                }
                if (wiki.articles.isEmpty()) {
                    item {
                        HakimiCard(modifier = Modifier.fillMaxWidth()) {
                            HakimiText("没有匹配的词条", style = HakimiTheme.type.body, color = c.textMuted)
                        }
                    }
                }
            }
        }
    }
}

/** 连接徽标：绿=已连接 / 黄=连接中 / 灰=离线。 */
@Composable
private fun ConnectionBadge(wiki: WikiUiState) {
    val c = HakimiTheme.colors
    val (color, label) = when (wiki.connection) {
        WikiConnection.CONNECTED -> c.success to "已连接 wiki"
        WikiConnection.CONNECTING -> c.warning to "连接中…"
        WikiConnection.DISCONNECTED -> c.textMuted to "离线（本地缓存）"
    }
    Row(
        modifier = Modifier
            .clip(PixelShape(6.dp))
            .background(c.surface)
            .border(2.dp, c.ink, PixelShape(6.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(PixelShape(3.dp))
                .background(color)
                .border(1.dp, c.ink, PixelShape(3.dp)),
        )
        HakimiText(label, style = HakimiTheme.type.caption, color = c.textMuted)
    }
}

@Composable
private fun WikiArticleCard(article: WikiArticleDto, onClick: () -> Unit) {
    val c = HakimiTheme.colors
    HakimiCard(modifier = Modifier.fillMaxWidth(), onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    HakimiText(article.title, style = HakimiTheme.type.title)
                    HakimiChip(categoryLabel(article.category), color = c.accent)
                }
                HakimiText(article.summary, style = HakimiTheme.type.body, color = c.textMuted, maxLines = 2)
            }
            HakimiText(article.updatedAt.take(10), style = HakimiTheme.type.caption, color = c.textMuted, modifier = Modifier.width(88.dp))
        }
    }
}

/** 词条详情：正文按 Jsoup 白名单子集渲染；图片走 Coil，视频给 Web 端引导。 */
@Composable
private fun WikiDetailView(wiki: WikiUiState, detail: WikiArticleDetailDto, onBack: () -> Unit) {
    val c = HakimiTheme.colors
    val blocks = remember(detail.html) { RichTextHtml.parse(detail.html) }
    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            HakimiIconButton(icon = HakimiIcons.Close, contentDescription = "返回列表", onClick = onBack, size = 36.dp)
            Column(modifier = Modifier.weight(1f)) {
                HakimiText(detail.title, style = HakimiTheme.type.display)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    HakimiChip(categoryLabel(detail.category), color = c.accent)
                    HakimiText("作者 ${detail.author} · ${detail.updatedAt.take(10)}", style = HakimiTheme.type.caption, color = c.textMuted)
                }
            }
            ConnectionBadge(wiki)
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            blocks.forEach { block -> RichBlockView(block, wiki.serverUrl()) }
        }
    }
}

private fun WikiUiState.serverUrl(): String = System.getenv("HAKIMI_WIKI_URL")?.takeIf { it.isNotBlank() } ?: com.minecraft.launcher.wiki.WikiClient.DEFAULT_URL

@Composable
private fun RichBlockView(block: RichBlock, wsUrl: String) {
    val c = HakimiTheme.colors
    when (block) {
        is RichBlock.Heading -> RichAnnotatedText(
            block.text,
            style = if (block.level == 1) HakimiTheme.type.display else HakimiTheme.type.title,
        )
        is RichBlock.Paragraph -> RichAnnotatedText(block.text)
        is RichBlock.Quote -> Row(
            modifier = Modifier
                .background(c.primarySoft.copy(alpha = 0.35f))
                .padding(horizontal = 14.dp, vertical = 10.dp),
        ) {
            Box(modifier = Modifier.width(4.dp).background(c.primary))
            Spacer(Modifier.width(10.dp))
            RichAnnotatedText(block.text, color = c.textMuted)
        }
        is RichBlock.CodeBlock -> Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(PixelShape(8.dp))
                .background(c.surfaceMuted)
                .border(2.dp, c.ink, PixelShape(8.dp))
                .padding(12.dp),
        ) {
            HakimiText(block.code, style = HakimiTheme.type.label.copy(fontFamily = FontFamily.Monospace, fontSize = 14.sp))
        }
        is RichBlock.BulletList -> Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            block.items.forEach {
                Row {
                    HakimiText("•  ", style = HakimiTheme.type.body, color = c.primary)
                    RichAnnotatedText(it)
                }
            }
        }
        is RichBlock.NumberedList -> Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            block.items.forEachIndexed { i, it ->
                Row {
                    HakimiText("${i + 1}. ", style = HakimiTheme.type.body, color = c.primary)
                    RichAnnotatedText(it)
                }
            }
        }
        is RichBlock.Image -> Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 360.dp)
                .clip(PixelShape(10.dp))
                .border(2.dp, c.ink, PixelShape(10.dp)),
        ) {
            AsyncImage(
                model = com.minecraft.launcher.wiki.WikiClient.resolveMediaUrl(block.url, wsUrl),
                contentDescription = block.alt,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        is RichBlock.Video -> HakimiCard(modifier = Modifier.fillMaxWidth(), background = c.surfaceMuted) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                HakimiIcon(HakimiIcons.Launch, null, c.primary, size = 26.dp)
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    HakimiText("视频词条内容", style = HakimiTheme.type.label)
                    HakimiText("桌面端暂不内嵌播放，请在 Web 端查看", style = HakimiTheme.type.caption, color = c.textMuted)
                }
                HakimiChip(
                    text = "浏览器打开",
                    color = c.accent,
                    onClick = {
                        runCatching {
                            if (Desktop.isDesktopSupported()) {
                                Desktop.getDesktop().browse(URI(com.minecraft.launcher.wiki.WikiClient.resolveMediaUrl(block.url, wsUrl)))
                            }
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun RichAnnotatedText(text: AnnotatedString, color: Color? = null, style: TextStyle = HakimiTheme.type.body) {
    val c = HakimiTheme.colors
    androidx.compose.foundation.text.BasicText(
        text = text,
        style = style.copy(color = color ?: c.text),
        modifier = Modifier.fillMaxWidth(),
    )
}
