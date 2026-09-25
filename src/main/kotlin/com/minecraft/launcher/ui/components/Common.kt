package com.minecraft.launcher.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.minecraft.launcher.backend.ResourceItem
import com.minecraft.launcher.ui.HakimiIcons
import com.minecraft.launcher.ui.theme.HakimiCard
import com.minecraft.launcher.ui.theme.HakimiChip
import com.minecraft.launcher.ui.theme.HakimiColors
import com.minecraft.launcher.ui.theme.HakimiIcon
import com.minecraft.launcher.ui.theme.HakimiIconButton
import com.minecraft.launcher.ui.theme.HakimiProgressBar
import com.minecraft.launcher.ui.theme.HakimiSearchField
import com.minecraft.launcher.ui.theme.HakimiText
import com.minecraft.launcher.ui.theme.HakimiTheme
import com.minecraft.launcher.ui.state.SortMode

/** 页面顶部标题区。 */
@Composable
fun PageHeader(
    icon: ImageVector,
    title: String,
    subtitle: String,
    trailing: (@Composable RowScope.() -> Unit)? = null,
) {
    val c = HakimiTheme.colors
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier.size(44.dp).clip(CircleShape).background(c.primarySoft),
            contentAlignment = Alignment.Center,
        ) { HakimiIcon(icon, null, c.primary, size = 22.dp) }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            HakimiText(title, style = HakimiTheme.type.display)
            HakimiText(subtitle, style = HakimiTheme.type.body, color = c.textMuted)
        }
        trailing?.invoke(this)
    }
}

/** 缩略图占位（渐变底 + 图标）。 */
@Composable
fun PlaceholderThumb(icon: ImageVector, color: Color, modifier: Modifier = Modifier, iconSize: Dp = 26.dp) {
    Box(
        modifier = modifier.clip(CircleShape).background(color.copy(alpha = 0.16f)),
        contentAlignment = Alignment.Center,
    ) {
        HakimiIcon(icon, null, color, size = iconSize)
    }
}

/** 圆形头像占位。 */
@Composable
fun AvatarCircle(icon: ImageVector, color: Color, size: Dp = 44.dp) {
    Box(
        modifier = Modifier.size(size).clip(CircleShape).background(color.copy(alpha = 0.16f)),
        contentAlignment = Alignment.Center,
    ) {
        HakimiIcon(icon, null, color, size = size * 0.5f)
    }
}

/** 统计条：图标 + 标签 + 数值 + 进度。 */
@Composable
fun StatBar(icon: ImageVector, label: String, value: String, fraction: Float, color: Color) {
    val c = HakimiTheme.colors
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            HakimiIcon(icon, null, color, size = 16.dp)
            HakimiText(label, style = HakimiTheme.type.label, modifier = Modifier.weight(1f))
            HakimiText(value, style = HakimiTheme.type.caption, color = c.textMuted)
        }
        HakimiProgressBar(fraction = fraction, color = color)
    }
}

/** 搜索 + 排序 + 分类筛选工具栏。 */
@Composable
fun SearchFilterSortBar(
    query: String,
    onQuery: (String) -> Unit,
    categories: List<String>,
    category: String,
    onCategory: (String) -> Unit,
    sort: SortMode,
    onSort: (SortMode) -> Unit,
    searchIcon: ImageVector,
) {
    val c = HakimiTheme.colors
    HakimiSearchField(
        value = query,
        onValueChange = onQuery,
        placeholder = "搜索名称或关键词…",
        icon = searchIcon,
        modifier = Modifier.fillMaxWidth(),
    )
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        categories.forEach { cat ->
            HakimiChip(text = cat, color = c.primary, selected = cat == category, onClick = { onCategory(cat) })
        }
    }
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        SortMode.entries.forEach { mode ->
            HakimiChip(text = mode.label, color = c.textMuted, selected = mode == sort, onClick = { onSort(mode) })
        }
    }
}

/** 资源卡片。 */
@Composable
fun ResourceCard(
    item: ResourceItem,
    icon: ImageVector,
    color: Color,
    onToggle: () -> Unit,
    onOpen: () -> Unit,
) {
    val c = HakimiTheme.colors
    HakimiCard(modifier = Modifier.fillMaxWidth(), onClick = onOpen) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.Top) {
            PlaceholderThumb(icon = icon, color = color, modifier = Modifier.size(60.dp))
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    HakimiText(item.name, style = HakimiTheme.type.title)
                    item.categories.firstOrNull()?.let { HakimiChip(it, color = color) }
                }
                HakimiText(item.summary, style = HakimiTheme.type.body, color = c.textMuted, maxLines = 2)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    HakimiText("v${item.version}", style = HakimiTheme.type.caption, color = c.textMuted)
                    HakimiText("⬇ ${formatCount(item.downloads)}", style = HakimiTheme.type.caption, color = c.textMuted)
                    HakimiText("@${item.author}", style = HakimiTheme.type.caption, color = c.textMuted)
                }
            }
            HakimiChip(
                text = if (item.enabled) "已启用" else "启用",
                color = if (item.enabled) c.success else c.primary,
                selected = item.enabled,
                onClick = onToggle,
            )
        }
    }
}

/** 资源详情弹层内容。 */
@Composable
fun ResourceDetailContent(
    item: ResourceItem,
    icon: ImageVector,
    color: Color,
    onToggle: () -> Unit,
    onClose: () -> Unit,
) {
    val c = HakimiTheme.colors
    HakimiCard(modifier = Modifier.width(440.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            PlaceholderThumb(icon = icon, color = color, modifier = Modifier.size(52.dp))
            Column(modifier = Modifier.weight(1f)) {
                HakimiText(item.name, style = HakimiTheme.type.title)
                HakimiText("v${item.version} · ⬇ ${formatCount(item.downloads)} · @${item.author}", style = HakimiTheme.type.caption, color = c.textMuted)
            }
            HakimiIconButton(icon = HakimiIcons.Close, contentDescription = "关闭", onClick = onClose, tint = c.textMuted)
        }
        Spacer(modifier = Modifier.height(12.dp))
        HakimiText(item.description, style = HakimiTheme.type.body, color = c.textMuted)
        Spacer(modifier = Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            item.categories.forEach { HakimiChip(it, color = color) }
        }
        Spacer(modifier = Modifier.height(16.dp))
        HakimiChip(
            text = if (item.enabled) "禁用" else "启用",
            color = if (item.enabled) c.error else c.success,
            selected = true,
            onClick = onToggle,
        )
    }
}

fun formatCount(n: Long): String = when {
    n >= 1_000_000 -> "%.1fM".format(n / 1_000_000.0)
    n >= 1_000 -> "%.0fK".format(n / 1_000.0)
    else -> n.toString()
}
