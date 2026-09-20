package com.minecraft.launcher.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.minecraft.launcher.ui.components.PageHeader
import com.minecraft.launcher.ui.components.StatBar
import com.minecraft.launcher.ui.theme.HakimiButton
import com.minecraft.launcher.ui.theme.HakimiCard
import com.minecraft.launcher.ui.theme.HakimiChip
import com.minecraft.launcher.ui.theme.HakimiIcon
import com.minecraft.launcher.ui.theme.HakimiText
import com.minecraft.launcher.ui.theme.HakimiTheme
import com.minecraft.launcher.ui.state.LauncherViewModel

@Composable
fun HomeScreen(vm: LauncherViewModel) {
    val state by vm.state.collectAsState()
    val home = state.home
    val stats = state.systemStats
    val c = HakimiTheme.colors

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        PageHeader(
            icon = HakimiIcons.Home,
            title = home?.welcomeTitle ?: "欢迎回来！",
            subtitle = home?.welcomeSubtitle ?: "",
        )

        HakimiCard(modifier = Modifier.fillMaxWidth()) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                Column(modifier = Modifier.weight(1.6f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    HakimiChip("当前实例", color = c.primary)
                    HakimiText(home?.instanceName ?: "…", style = HakimiTheme.type.display)
                    HakimiText(home?.instanceDescription ?: "", style = HakimiTheme.type.body, color = c.textMuted)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        home?.modeTags?.forEach { HakimiChip(it, color = c.accent) }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    HakimiButton(
                        text = "启动游戏  ${home?.version ?: ""}",
                        icon = HakimiIcons.Launch,
                        onClick = { vm.launch() },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    InfoTile("版本", home?.version ?: "-", HakimiIcons.Mods, c.primary)
                    InfoTile("加载器", home?.loader ?: "-", HakimiIcons.Plugin, c.accent)
                    InfoTile("资源数", "${home?.resourceCount ?: 0} 个", HakimiIcons.Modpack, c.success)
                    InfoTile("总大小", "${home?.totalSizeGb ?: 0.0} GB", HakimiIcons.Disk, c.warning)
                }
            }
        }

        HakimiCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    HakimiIcon(HakimiIcons.Cpu, null, c.primary, size = 18.dp)
                    HakimiText("系统监控", style = HakimiTheme.type.title, modifier = Modifier.weight(1f))
                    HakimiText("实时", style = HakimiTheme.type.caption, color = c.textMuted)
                }
                if (stats != null) {
                    StatBar(HakimiIcons.Cpu, "CPU", "${stats.cpuPercent}%", stats.cpuPercent / 100f, c.primary)
                    StatBar(HakimiIcons.Memory, "内存", "%.1f / %.0f GB".format(stats.memUsedGb, stats.memTotalGb), (stats.memUsedGb / stats.memTotalGb).toFloat(), c.accent)
                    StatBar(HakimiIcons.Shader, "显存", "${stats.vramUsedMb} / ${stats.vramTotalMb} MB", stats.vramUsedMb.toFloat() / stats.vramTotalMb, c.success)
                    StatBar(HakimiIcons.Disk, "磁盘 IO", "读 %.0f · 写 %.0f MB/s".format(stats.diskReadMbps, stats.diskWriteMbps), ((stats.diskReadMbps + stats.diskWriteMbps) / 200f).toFloat(), c.warning)
                    StatBar(
                        HakimiIcons.Network,
                        "网络",
                        if (stats.networkOnline) "${stats.networkLatencyMs} ms 正常" else "离线",
                        if (stats.networkOnline) (1f - (stats.networkLatencyMs / 200f)).coerceIn(0f, 1f) else 0f,
                        if (stats.networkOnline) c.success else c.error,
                    )
                }
            }
        }
    }
}

@Composable
private fun InfoTile(label: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector, color: androidx.compose.ui.graphics.Color) {
    val c = HakimiTheme.colors
    HakimiCard(modifier = Modifier.fillMaxWidth(), contentPadding = 14.dp) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(modifier = Modifier.size(36.dp), contentAlignment = Alignment.Center) { HakimiIcon(icon, null, color, size = 20.dp) }
            Column(modifier = Modifier.weight(1f)) {
                HakimiText(label, style = HakimiTheme.type.caption, color = c.textMuted)
                HakimiText(value, style = HakimiTheme.type.title)
            }
        }
    }
}
