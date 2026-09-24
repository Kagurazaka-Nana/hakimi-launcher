package com.minecraft.launcher.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import com.minecraft.launcher.backend.SystemStats
import com.minecraft.launcher.ui.HakimiIcons
import com.minecraft.launcher.ui.theme.HakimiIcon
import com.minecraft.launcher.ui.theme.HakimiText
import com.minecraft.launcher.ui.theme.HakimiTheme

/** 精简系统状态栏：图标 + 迷你进度条（有上限的指标）/ 上下行速率（网络、磁盘 IO）。图标悬浮显示含义。 */
@Composable
fun StatusBar(stats: SystemStats?, modifier: Modifier = Modifier) {
    val c = HakimiTheme.colors
    Row(
        modifier = modifier
            .height(34.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(c.surface)
            .border(2.dp, c.ink, RoundedCornerShape(10.dp))
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        GaugeItem(HakimiIcons.Cpu, "CPU 占用", stats?.let { it.cpuPercent / 100f }, c.primary)
        GaugeItem(HakimiIcons.Memory, "内存占用", stats?.let { (it.memUsedGb / it.memTotalGb).toFloat() }, c.success)
        val vramFraction = stats?.let { s ->
            val used = s.vramUsedMb
            val total = s.vramTotalMb
            if (used != null && total != null && total > 0) used.toFloat() / total else null
        }
        if (vramFraction != null) {
            GaugeItem(HakimiIcons.Shader, "显存占用", vramFraction, c.accent)
        }
        RateItem(HakimiIcons.Network, "网络速度（↑上行 ↓下行）", stats?.netUpBps, stats?.netDownBps)
        RateItem(HakimiIcons.Disk, "磁盘 IO（↑写入 ↓读取）", stats?.diskWriteBps, stats?.diskReadBps)
    }
}

@Composable
private fun GaugeItem(icon: ImageVector, tip: String, fraction: Float?, color: Color) {
    val c = HakimiTheme.colors
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        HoverTip(label = tip) { HakimiIcon(icon, tip, c.textMuted, size = 12.dp) }
        Box(
            modifier = Modifier
                .width(38.dp)
                .height(8.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(c.surfaceMuted)
                .border(2.dp, c.ink, RoundedCornerShape(3.dp)),
        ) {
            if (fraction != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(fraction.coerceIn(0f, 1f))
                        .height(8.dp)
                        .background(color),
                )
            }
        }
    }
}

@Composable
private fun RateItem(icon: ImageVector, tip: String, upBps: Long?, downBps: Long?) {
    val c = HakimiTheme.colors
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        HoverTip(label = tip) { HakimiIcon(icon, tip, c.textMuted, size = 12.dp) }
        // 固定宽度 + 单行：宽度取实测值（formatRate 已改为整数格式，最长 "↑9999M" ≈ 44px），
        // 避免速率数字位数变化时状态栏整体宽度抖动
        HakimiText(
            "↑${formatRate(upBps)}",
            modifier = Modifier.width(46.dp),
            style = HakimiTheme.type.caption,
            color = c.text,
            maxLines = 1,
        )
        HakimiText(
            "↓${formatRate(downBps)}",
            modifier = Modifier.width(46.dp),
            style = HakimiTheme.type.caption,
            color = c.text,
            maxLines = 1,
        )
    }
}

/** 悬浮提示：鼠标悬停在内容上时，在其下方弹出说明气泡。 */
@Composable
private fun HoverTip(label: String, content: @Composable () -> Unit) {
    val c = HakimiTheme.colors
    val source = remember { MutableInteractionSource() }
    val hovered by source.collectIsHoveredAsState()
    val density = LocalDensity.current
    // 自定义定位：气泡始终在锚点下方（留出间隙），避免气泡窗口覆盖鼠标位置
    // 导致 hover 反复进出（悬浮提示闪烁）。同时钳制在窗口范围内防止截断。
    val tipPosition = remember(density) {
        object : PopupPositionProvider {
            override fun calculatePosition(
                anchorBounds: IntRect,
                windowSize: IntSize,
                layoutDirection: LayoutDirection,
                popupContentSize: IntSize,
            ): IntOffset {
                val gap = with(density) { 6.dp.roundToPx() }
                val x = anchorBounds.left + (anchorBounds.width - popupContentSize.width) / 2
                val y = anchorBounds.bottom + gap
                return IntOffset(
                    x.coerceIn(0, (windowSize.width - popupContentSize.width).coerceAtLeast(0)),
                    y.coerceAtMost((windowSize.height - popupContentSize.height).coerceAtLeast(0)),
                )
            }
        }
    }
    Box(modifier = Modifier.hoverable(source)) {
        content()
        if (hovered) {
            Popup(
                popupPositionProvider = tipPosition,
                properties = PopupProperties(focusable = false),
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(c.surface)
                        .border(2.dp, c.ink, RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                ) {
                    HakimiText(label, style = HakimiTheme.type.caption, color = c.text)
                }
            }
        }
    }
}

/** 字节/秒 → 紧凑整数文本（B / K / M / G），无小数位以节省状态栏空间。 */
private fun formatRate(bps: Long?): String = when {
    bps == null -> "-"
    bps >= 1_000_000_000 -> "%.0fG".format(bps / 1_000_000_000.0)
    bps >= 1_000_000 -> "%.0fM".format(bps / 1_000_000.0)
    bps >= 1_000 -> "%.0fK".format(bps / 1_000.0)
    else -> "${bps}B"
}
