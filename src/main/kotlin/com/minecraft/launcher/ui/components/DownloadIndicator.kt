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
import androidx.compose.ui.unit.dp
import com.minecraft.launcher.backend.DownloadTask
import com.minecraft.launcher.ui.HakimiIcons
import com.minecraft.launcher.ui.theme.HakimiIcon
import com.minecraft.launcher.ui.theme.HakimiText
import com.minecraft.launcher.ui.theme.HakimiTheme
import kotlin.math.roundToInt

/**
 * 底部下载内容指示器：
 * 有下载任务时显示 图标 + 总进度条 + 百分比；无任务时收起为 图标 + “下载内容”。
 */
@Composable
fun DownloadIndicator(downloads: List<DownloadTask>, modifier: Modifier = Modifier) {
    val c = HakimiTheme.colors
    Row(
        modifier = modifier
            .height(34.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(c.surface)
            .border(2.dp, c.ink, RoundedCornerShape(10.dp))
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        HakimiIcon(HakimiIcons.Download, "下载内容", c.textMuted, size = 16.dp)
        if (downloads.isEmpty()) {
            HakimiText("下载内容", style = HakimiTheme.type.caption, color = c.text)
        } else {
            val overall = downloads.map { it.fraction.coerceIn(0f, 1f) }.average().toFloat()
            Box(
                modifier = Modifier
                    .width(72.dp)
                    .height(8.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(c.surfaceMuted)
                    .border(2.dp, c.ink, RoundedCornerShape(3.dp)),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(overall)
                        .height(8.dp)
                        .background(c.primary),
                )
            }
            HakimiText("${(overall * 100).roundToInt()}%", style = HakimiTheme.type.caption, color = c.text)
        }
    }
}
