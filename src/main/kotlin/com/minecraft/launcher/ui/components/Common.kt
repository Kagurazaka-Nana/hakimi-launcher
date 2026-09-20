package com.minecraft.launcher.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.minecraft.launcher.backend.ResourceItem
import com.minecraft.launcher.ui.HakimiIcons
import com.minecraft.launcher.ui.state.SortMode

/** 白色/表面圆角卡片。 */
@Composable
fun SoftCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = if (onClick != null) modifier.clickable { onClick() } else modifier,
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(modifier = Modifier.fillMaxWidth(), content = content)
    }
}

/** 圆角药丸标签。 */
@Composable
fun Pill(text: String, color: Color = MaterialTheme.colorScheme.primary) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(color.copy(alpha = 0.15f))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(text, color = color, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    }
}

/** 圆角图标徽标。 */
@Composable
fun IconBadge(icon: ImageVector, color: Color, size: androidx.compose.ui.unit.Dp = 40.dp, iconSize: androidx.compose.ui.unit.Dp = 20.dp) {
    Box(
        modifier = Modifier.size(size).clip(RoundedCornerShape(12.dp)).background(color.copy(alpha = 0.15f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(iconSize))
    }
}

/** 缩略图占位（渐变 + 图标）。 */
@Composable
fun PlaceholderThumb(icon: ImageVector, color: Color, modifier: Modifier = Modifier, iconSize: androidx.compose.ui.unit.Dp = 26.dp) {
    Box(
        modifier = modifier.clip(RoundedCornerShape(14.dp)).background(color.copy(alpha = 0.18f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(iconSize))
    }
}

/** 页面顶部标题区。 */
@Composable
fun PageHeader(icon: ImageVector, title: String, subtitle: String, trailing: (@Composable RowScope.() -> Unit)? = null) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        IconBadge(icon = icon, color = MaterialTheme.colorScheme.primary, size = 44.dp, iconSize = 22.dp)
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
            Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
        }
        trailing?.invoke(this)
    }
}

/** 区块标题行。 */
@Composable
fun SectionHeaderRow(icon: ImageVector, title: String, color: Color = MaterialTheme.colorScheme.primary, trailing: (@Composable RowScope.() -> Unit)? = null) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Icon(icon, contentDescription = null, tint = color)
        Text(title, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
        trailing?.invoke(this)
    }
}

/** 统计条：标签 + 数值 + 进度。 */
@Composable
fun StatBar(icon: ImageVector, label: String, value: String, fraction: Float, color: Color) {
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
            Text(label, color = MaterialTheme.colorScheme.onSurface, fontSize = 13.sp, modifier = Modifier.weight(1f))
            Text(value, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
        }
        LinearProgressIndicator(
            progress = { fraction.coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
            color = color,
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
        )
    }
}

/** 搜索 + 分类筛选 + 排序工具栏。 */
@Composable
fun SearchFilterSortBar(
    query: String,
    onQuery: (String) -> Unit,
    categories: List<String>,
    category: String,
    onCategory: (String) -> Unit,
    sort: SortMode,
    onSort: (SortMode) -> Unit,
) {
    var sortMenu by remember { mutableStateOf(false) }
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedTextField(
            value = query,
            onValueChange = onQuery,
            modifier = Modifier.weight(1f),
            placeholder = { Text("搜索名称或关键词…", color = MaterialTheme.colorScheme.onSurfaceVariant) },
            leadingIcon = { Icon(HakimiIcons.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
            shape = RoundedCornerShape(14.dp),
            singleLine = true,
        )
        Box {
            TextButton(onClick = { sortMenu = true }) {
                Icon(HakimiIcons.Sort, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(sort.label, color = MaterialTheme.colorScheme.onSurface)
            }
            DropdownMenu(expanded = sortMenu, onDismissRequest = { sortMenu = false }) {
                SortMode.entries.forEach { mode ->
                    DropdownMenuItem(text = { Text(mode.label) }, onClick = { onSort(mode); sortMenu = false })
                }
            }
        }
    }
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        categories.forEach { cat ->
            val active = cat == category
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                    .clickable { onCategory(cat) }
                    .padding(horizontal = 14.dp, vertical = 7.dp)
            ) {
                Text(cat, color = if (active) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface, fontSize = 12.sp)
            }
        }
    }
}

/** 资源卡片。 */
@Composable
fun ResourceCard(item: ResourceItem, icon: ImageVector, color: Color, onToggle: () -> Unit, onOpen: () -> Unit) {
    SoftCard(modifier = Modifier.fillMaxWidth().clickable { onOpen() }) {
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.Top) {
            PlaceholderThumb(icon = icon, color = color, modifier = Modifier.size(64.dp))
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(item.name, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, fontSize = 15.sp)
                    item.categories.firstOrNull()?.let { Pill(it, color) }
                }
                Text(item.summary, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp, maxLines = 2)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("v${item.version}", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                    Text("⬇ ${formatCount(item.downloads)}", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                    Text("@${item.author}", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                }
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(if (item.enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                    .clickable { onToggle() }
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Text(if (item.enabled) "已启用" else "启用", fontSize = 12.sp, color = if (item.enabled) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface)
            }
        }
    }
}

/** 资源详情弹窗。 */
@Composable
fun ResourceDetailDialog(item: ResourceItem, icon: ImageVector, color: Color, onToggle: () -> Unit, onClose: () -> Unit) {
    AlertDialog(
        onDismissRequest = onClose,
        confirmButton = {
            TextButton(onClick = { onToggle(); onClose() }) {
                Text(if (item.enabled) "禁用" else "启用", color = MaterialTheme.colorScheme.primary)
            }
        },
        dismissButton = { TextButton(onClick = onClose) { Text("关闭") } },
        title = { Text(item.name) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    PlaceholderThumb(icon = icon, color = color, modifier = Modifier.size(52.dp))
                    Column {
                        Text("版本 ${item.version}", color = MaterialTheme.colorScheme.onSurface)
                        Text("下载量 ${formatCount(item.downloads)}", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                        Text("作者 ${item.author}", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                    }
                }
                Text(item.description, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    item.categories.forEach { Pill(it, color) }
                }
            }
        },
    )
}

fun formatCount(n: Long): String = when {
    n >= 1_000_000 -> "%.1fM".format(n / 1_000_000.0)
    n >= 1_000 -> "%.0fK".format(n / 1_000.0)
    else -> n.toString()
}
