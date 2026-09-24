package com.minecraft.launcher.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.minecraft.launcher.backend.DownloadTask
import com.minecraft.launcher.download.DownloadState
import com.minecraft.launcher.ui.HakimiIcons
import com.minecraft.launcher.ui.theme.HakimiCard
import com.minecraft.launcher.ui.theme.HakimiIcon
import com.minecraft.launcher.ui.theme.HakimiOverlay
import com.minecraft.launcher.ui.theme.HakimiText
import com.minecraft.launcher.ui.theme.HakimiTheme
import com.minecraft.launcher.ui.theme.PixelShape
import kotlin.math.roundToInt

/**
 * 下载任务弹窗：展示全部任务（活跃 + 历史）。
 * 每行仅图标操作（悬浮显示文字提示）：暂停（活跃）、继续（已暂停/失败）、删除（终态）。
 */
@Composable
fun DownloadTasksDialog(
    tasks: List<DownloadTask>,
    onPause: (String) -> Unit,
    onResume: (String) -> Unit,
    onRemove: (String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = HakimiTheme.colors
    HakimiOverlay(onDismiss = onDismiss, modifier = modifier) {
        HakimiCard(modifier = Modifier.width(640.dp), contentPadding = 20.dp) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                HakimiText("下载任务", style = HakimiTheme.type.title, modifier = Modifier.weight(1f))
                HakimiText(
                    "✕",
                    style = HakimiTheme.type.label,
                    color = c.textMuted,
                    modifier = Modifier
                        .clip(PixelShape(6.dp))
                        .clickable(onClick = onDismiss)
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                )
            }
            Spacer(modifier = Modifier.height(14.dp))
            if (tasks.isEmpty()) {
                HakimiText(
                    "暂无下载任务",
                    style = HakimiTheme.type.body,
                    color = c.textMuted,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 28.dp),
                    align = TextAlign.Center,
                )
            } else {
                Column(
                    modifier = Modifier.fillMaxWidth().height(420.dp).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    tasks.forEach { task -> TaskRow(task, onPause, onResume, onRemove) }
                }
            }
        }
    }
}

@Composable
private fun TaskRow(
    task: DownloadTask,
    onPause: (String) -> Unit,
    onResume: (String) -> Unit,
    onRemove: (String) -> Unit,
) {
    val c = HakimiTheme.colors
    val active = task.state == DownloadState.CONNECTING || task.state == DownloadState.DOWNLOADING
    val (label, color) = when (task.state) {
        DownloadState.CONNECTING -> "连接中" to c.accent
        DownloadState.DOWNLOADING -> "下载中" to c.primary
        DownloadState.COMPLETED -> "已完成" to c.success
        DownloadState.CANCELLED -> "已暂停" to c.textMuted
        DownloadState.FAILED -> "失败" to c.error
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(PixelShape(8.dp))
            .background(c.surfaceMuted.copy(alpha = 0.35f))
            .border(2.dp, c.ink, PixelShape(8.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            HakimiText(task.name, style = HakimiTheme.type.label, maxLines = 1)
            HakimiText(task.url, style = HakimiTheme.type.caption, color = c.textMuted, maxLines = 1)
        }
        if (active) {
            Box(
                modifier = Modifier
                    .width(96.dp)
                    .height(8.dp)
                    .clip(PixelShape(3.dp))
                    .background(c.surface)
                    .border(2.dp, c.ink, PixelShape(3.dp)),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(task.fraction.coerceIn(0f, 1f))
                        .height(8.dp)
                        .background(c.primary),
                )
            }
            HakimiText("${(task.fraction * 100).roundToInt()}%", style = HakimiTheme.type.caption, color = c.text)
        }
        HakimiText(label, style = HakimiTheme.type.caption, color = color)
        if (active && task.isPauseable) {
            ActionIcon(HakimiIcons.Pause, "暂停", c.primary) { onPause(task.id) }
        }
        if (!active && task.state != DownloadState.COMPLETED && task.isPauseable) {
            ActionIcon(HakimiIcons.Resume, "继续", c.success) { onResume(task.id) }
        }
        if (!active) {
            ActionIcon(HakimiIcons.Delete, "删除", c.error) { onRemove(task.id) }
        }
    }
}

/** 仅图标的行内操作按钮，悬浮显示文字提示。 */
@Composable
private fun ActionIcon(icon: ImageVector, tip: String, tint: Color, onClick: () -> Unit) {
    HoverTip(label = tip) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(PixelShape(5.dp))
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            HakimiIcon(icon, tip, tint, size = 15.dp)
        }
    }
}
