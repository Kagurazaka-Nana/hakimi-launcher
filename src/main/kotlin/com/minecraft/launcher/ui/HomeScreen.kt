package com.minecraft.launcher.ui

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.minecraft.launcher.ui.components.PageHeader
import com.minecraft.launcher.ui.components.Pill
import com.minecraft.launcher.ui.components.SoftCard
import com.minecraft.launcher.ui.components.StatBar
import com.minecraft.launcher.ui.state.LauncherViewModel
import com.minecraft.launcher.ui.theme.HakimiColors

@Composable
fun HomeScreen(vm: LauncherViewModel) {
    val state by vm.state.collectAsState()
    val home = state.home
    val stats = state.systemStats

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        PageHeader(
            icon = HakimiIcons.Home,
            title = home?.welcomeTitle ?: "欢迎回来！",
            subtitle = home?.welcomeSubtitle ?: "",
        )

        // 当前实例
        SoftCard(modifier = Modifier.fillMaxWidth()) {
            Row(modifier = Modifier.fillMaxWidth().padding(20.dp), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                Column(modifier = Modifier.weight(1.6f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Pill("当前实例")
                    Text(home?.instanceName ?: "…", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    Text(home?.instanceDescription ?: "", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        home?.modeTags?.forEach { Pill(it, HakimiColors.Info) }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Button(
                        onClick = { vm.launch() },
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                    ) {
                        Icon(HakimiIcons.Launch, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("启动游戏  ${home?.version ?: ""}", fontSize = 17.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    InfoTile("版本", home?.version ?: "-", HakimiIcons.Mods, MaterialTheme.colorScheme.primary)
                    InfoTile("加载器", home?.loader ?: "-", HakimiIcons.Plugin, HakimiColors.Info)
                    InfoTile("资源数", "${home?.resourceCount ?: 0} 个", HakimiIcons.Modpack, HakimiColors.Pink)
                    InfoTile("总大小", "${home?.totalSizeGb ?: 0.0} GB", HakimiIcons.Disk, HakimiColors.Success)
                }
            }
        }

        // 系统监控
        SoftCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(HakimiIcons.Cpu, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text("系统监控", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, fontSize = 16.sp)
                    Spacer(modifier = Modifier.weight(1f))
                    Text("实时", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                }
                if (stats != null) {
                    StatBar(HakimiIcons.Cpu, "CPU", "${stats.cpuPercent}%", stats.cpuPercent / 100f, MaterialTheme.colorScheme.primary)
                    StatBar(HakimiIcons.Memory, "内存", "%.1f / %.0f GB".format(stats.memUsedGb, stats.memTotalGb), (stats.memUsedGb / stats.memTotalGb).toFloat(), HakimiColors.Info)
                    StatBar(HakimiIcons.Shader, "显存", "${stats.vramUsedMb} / ${stats.vramTotalMb} MB", stats.vramUsedMb.toFloat() / stats.vramTotalMb, HakimiColors.Pink)
                    StatBar(HakimiIcons.Disk, "磁盘 IO", "读 %.0f · 写 %.0f MB/s".format(stats.diskReadMbps, stats.diskWriteMbps), ((stats.diskReadMbps + stats.diskWriteMbps) / 200f).toFloat(), HakimiColors.Warning)
                    StatBar(
                        HakimiIcons.Network,
                        "网络",
                        if (stats.networkOnline) "${stats.networkLatencyMs} ms 正常" else "离线",
                        if (stats.networkOnline) (1f - (stats.networkLatencyMs / 200f).coerceIn(0f, 1f)) else 0f,
                        if (stats.networkOnline) HakimiColors.Success else HakimiColors.Warning,
                    )
                }
            }
        }
    }
}

@Composable
private fun InfoTile(label: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector, color: Color) {
    SoftCard(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                Text(value, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            }
        }
    }
}
