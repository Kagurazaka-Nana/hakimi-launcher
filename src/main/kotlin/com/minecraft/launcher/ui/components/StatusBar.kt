package com.minecraft.launcher.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.minecraft.launcher.backend.SystemStats
import com.minecraft.launcher.ui.HakimiIcons
import com.minecraft.launcher.ui.theme.HakimiIcon
import com.minecraft.launcher.ui.theme.HakimiText
import com.minecraft.launcher.ui.theme.HakimiTheme

/** 精简系统状态栏：图标 + 迷你进度条（有上限的指标）/ 上下行速率（网络、磁盘 IO）。 */
@Composable
fun StatusBar(stats: SystemStats?, modifier: Modifier = Modifier) {
    val c = HakimiTheme.colors
    Row(
        modifier = modifier
            .height(34.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(c.surface)
            .border(2.dp, c.ink, RoundedCornerShape(10.dp))
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        GaugeItem(HakimiIcons.Cpu, stats?.let { it.cpuPercent / 100f }, c.primary)
        GaugeItem(HakimiIcons.Memory, stats?.let { (it.memUsedGb / it.memTotalGb).toFloat() }, c.success)
        val vramFraction = stats?.let { s ->
            val used = s.vramUsedMb
            val total = s.vramTotalMb
            if (used != null && total != null && total > 0) used.toFloat() / total else null
        }
        if (vramFraction != null) {
            GaugeItem(HakimiIcons.Shader, vramFraction, c.accent)
        }
        RateItem(HakimiIcons.Network, stats?.netUpBps, stats?.netDownBps)
        RateItem(HakimiIcons.Disk, stats?.diskWriteBps, stats?.diskReadBps)
    }
}

@Composable
private fun GaugeItem(icon: ImageVector, fraction: Float?, color: Color) {
    val c = HakimiTheme.colors
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        HakimiIcon(icon, null, c.textMuted, size = 14.dp)
        Box(
            modifier = Modifier
                .width(52.dp)
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
private fun RateItem(icon: ImageVector, upBps: Long?, downBps: Long?) {
    val c = HakimiTheme.colors
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        HakimiIcon(icon, null, c.textMuted, size = 14.dp)
        HakimiText("↑${formatRate(upBps)}", style = HakimiTheme.type.caption, color = c.text)
        HakimiText("↓${formatRate(downBps)}", style = HakimiTheme.type.caption, color = c.text)
    }
}

/** 字节/秒 → 紧凑文本（B / K / M / G）。 */
private fun formatRate(bps: Long?): String = when {
    bps == null -> "-"
    bps >= 1_000_000_000 -> "%.1fG".format(bps / 1_000_000_000.0)
    bps >= 1_000_000 -> "%.1fM".format(bps / 1_000_000.0)
    bps >= 1_000 -> "%.0fK".format(bps / 1_000.0)
    else -> "${bps}B"
}
