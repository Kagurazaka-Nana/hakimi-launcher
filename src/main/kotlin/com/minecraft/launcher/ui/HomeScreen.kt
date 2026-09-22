package com.minecraft.launcher.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.minecraft.launcher.backend.RecentInstance
import com.minecraft.launcher.ui.components.PageHeader
import com.minecraft.launcher.ui.theme.HakimiButton
import com.minecraft.launcher.ui.theme.HakimiCard
import com.minecraft.launcher.ui.theme.HakimiChip
import com.minecraft.launcher.ui.theme.HakimiIcon
import com.minecraft.launcher.ui.theme.HakimiIconButton
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
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(28.dp),
    ) {
        PageHeader(
            icon = HakimiIcons.Home,
            title = home?.welcomeTitle ?: "欢迎回来！",
            subtitle = home?.welcomeSubtitle ?: "",
        )

        StatusBar(stats, modifier = Modifier.fillMaxWidth())

        HeroCard(home, onLaunch = { vm.launch() }, modifier = Modifier.fillMaxWidth())

        if (home != null && home.recentInstances.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    HakimiIcon(HakimiIcons.Launch, null, c.primary, size = 18.dp)
                    HakimiText("最近游玩", style = HakimiTheme.type.title)
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    home.recentInstances.take(4).forEach { instance ->
                        RecentCard(instance, modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun HeroCard(home: com.minecraft.launcher.backend.HomeSnapshot?, onLaunch: () -> Unit, modifier: Modifier = Modifier) {
    val c = HakimiTheme.colors
    HakimiCard(modifier = modifier, contentPadding = 24.dp) {
        Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
            Box(
                modifier = Modifier
                    .size(180.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(c.primarySoft)
                    .border(2.dp, c.ink, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center,
            ) {
                HakimiIcon(HakimiIcons.Home, null, c.primary, size = 64.dp)
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                HakimiChip("当前实例", color = c.success)
                HakimiText(home?.instanceName ?: "…", style = HakimiTheme.type.display)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    MetaItem(HakimiIcons.Version, home?.version ?: "")
                    MetaItem(HakimiIcons.Plugin, home?.loader ?: "")
                    MetaItem(HakimiIcons.Memory, home?.javaVersion ?: "")
                }
                HakimiText(home?.instanceDescription ?: "", style = HakimiTheme.type.body, color = c.textMuted)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    home?.modeTags?.forEach { HakimiChip(it, color = c.accent) }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    HakimiButton(text = "启动游戏 ${home?.version ?: ""}", icon = HakimiIcons.Launch, onClick = onLaunch)
                    HakimiIconButton(icon = HakimiIcons.More, contentDescription = "更多", onClick = { }, size = 44.dp)
                }
            }
        }
        Spacer(modifier = Modifier.height(20.dp))
        Box(modifier = Modifier.fillMaxWidth().height(2.dp).background(c.ink.copy(alpha = 0.12f)))
        Spacer(modifier = Modifier.height(16.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            StatCell(HakimiIcons.Version, "上次游玩", home?.lastPlayed ?: "-")
            StatCell(HakimiIcons.Modpack, "资源包", "${home?.resourceCount ?: 0}")
            StatCell(HakimiIcons.Disk, "存储大小", "${home?.totalSizeGb ?: 0.0} GB")
        }
    }
}

/** 精简系统状态栏：图标 + 迷你进度条（有上限的指标）/ 上下行速率（网络、磁盘 IO）。 */
@Composable
private fun StatusBar(stats: com.minecraft.launcher.backend.SystemStats?, modifier: Modifier = Modifier) {
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
private fun GaugeItem(icon: androidx.compose.ui.graphics.vector.ImageVector, fraction: Float?, color: androidx.compose.ui.graphics.Color) {
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
private fun RateItem(icon: androidx.compose.ui.graphics.vector.ImageVector, upBps: Long?, downBps: Long?) {
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

@Composable
private fun MetaItem(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
    val c = HakimiTheme.colors
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        HakimiIcon(icon, null, c.textMuted, size = 15.dp)
        HakimiText(text, style = HakimiTheme.type.label, color = c.text)
    }
}

@Composable
private fun StatCell(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, value: String) {
    val c = HakimiTheme.colors
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        HakimiIcon(icon, null, c.textMuted, size = 18.dp)
        Column {
            HakimiText(label, style = HakimiTheme.type.caption, color = c.textMuted)
            HakimiText(value, style = HakimiTheme.type.label)
        }
    }
}

@Composable
private fun RecentCard(instance: RecentInstance, modifier: Modifier = Modifier) {
    val c = HakimiTheme.colors
    HakimiCard(modifier = modifier, contentPadding = 16.dp) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(
                modifier = Modifier.size(56.dp).clip(RoundedCornerShape(10.dp)).background(c.primarySoft).border(2.dp, c.ink, RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center,
            ) { HakimiIcon(HakimiIcons.Home, null, c.primary, size = 26.dp) }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                HakimiText(instance.name, style = HakimiTheme.type.label)
                HakimiText("${instance.version} · ${instance.loader}", style = HakimiTheme.type.caption, color = c.textMuted)
                HakimiText(instance.time, style = HakimiTheme.type.caption, color = c.textMuted)
            }
            HakimiIcon(HakimiIcons.More, null, c.textMuted, size = 16.dp)
        }
    }
}
