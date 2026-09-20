package com.minecraft.launcher.ui

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DataObject
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.minecraft.launcher.backend.SettingsSnapshot
import com.minecraft.launcher.ui.components.AvatarCircle
import com.minecraft.launcher.ui.components.IconBadge
import com.minecraft.launcher.ui.components.PageHeader
import com.minecraft.launcher.ui.components.Pill
import com.minecraft.launcher.ui.components.SectionHeaderRow
import com.minecraft.launcher.ui.components.SoftCard
import com.minecraft.launcher.ui.state.LauncherViewModel
import com.minecraft.launcher.ui.theme.AccentOrange
import com.minecraft.launcher.ui.theme.AccentPurple
import com.minecraft.launcher.ui.theme.InfoBlue
import com.minecraft.launcher.ui.theme.SuccessBg
import com.minecraft.launcher.ui.theme.SuccessGreen
import io.github.composefluent.FluentTheme
import io.github.composefluent.component.AccentButton

@Composable
fun SettingsScreen(vm: LauncherViewModel) {
    val state by vm.state.collectAsState()
    val data = state.settings

    FluentTheme {
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            PageHeader(
                icon = Icons.Filled.Settings,
                title = "设置",
                subtitle = "调整你的启动器偏好，让 Minecraft 之旅更顺畅！",
                trailing = {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        AvatarCircle(Icons.Filled.Person, AccentPurple)
                        Column {
                            Text("hakimi 👑", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp)
                            Text("陪你探索更多方块世界～", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                        }
                    }
                }
            )

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(18.dp)) {
                    AppearanceCard(data)
                    DownloadSourceCard(data)
                }
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(18.dp)) {
                    JavaMemoryCard(data)
                    LaunchArgsCard(data)
                    AccountPrivacyCard(data)
                }
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                AccentButton(onClick = { /* 保存设置：待接入后端持久化 */ }) {
                    Icon(Icons.Filled.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("保存设置")
                }
            }
        }
    }
}

@Composable
private fun AppearanceCard(data: SettingsSnapshot?) {
    SoftCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            SectionHeaderRow(icon = Icons.Filled.Palette, title = "外观", color = AccentPurple)
            Text("主题切换", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface, fontSize = 13.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ThemeOption("浅色", selected = data?.theme == "浅色")
                ThemeOption("深色", selected = false)
                ThemeOption("跟随系统", selected = false)
            }
            Text("语言选择", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface, fontSize = 13.sp)
            LabeledField(icon = Icons.Filled.Language, value = data?.language ?: "")
        }
    }
}

@Composable
private fun ThemeOption(label: String, selected: Boolean) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(
            modifier = Modifier
                .size(width = 72.dp, height = 48.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant)
        )
        Text(label, color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal)
    }
}

@Composable
private fun JavaMemoryCard(data: SettingsSnapshot?) {
    SoftCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            SectionHeaderRow(icon = Icons.Filled.Memory, title = "Java 与内存", color = InfoBlue)
            Text("Java 路径", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface, fontSize = 13.sp)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = data?.javaPath ?: "", onValueChange = {}, modifier = Modifier.weight(1f), enabled = false, singleLine = true, shape = RoundedCornerShape(10.dp))
                Icon(Icons.Filled.Folder, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text("Java 版本：${data?.javaVersion ?: ""}", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
            Text("最大内存", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface, fontSize = 13.sp)
            var mem by remember { mutableStateOf(data?.maxMemoryMb?.toFloat() ?: 4096f) }
            Slider(value = mem, onValueChange = { mem = it }, valueRange = (data?.memoryMinMb ?: 512).toFloat()..(data?.memoryMaxMb ?: 8192).toFloat())
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("${data?.memoryMinMb ?: 512} MB", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                Text("${mem.toInt()} MB", color = MaterialTheme.colorScheme.primary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Text("${data?.memoryMaxMb ?: 8192} MB", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
            }
        }
    }
}

@Composable
private fun DownloadSourceCard(data: SettingsSnapshot?) {
    SoftCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            SectionHeaderRow(icon = Icons.Filled.Download, title = "下载源", color = MaterialTheme.colorScheme.primary)
            Text("下载源地址", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface, fontSize = 13.sp)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                LabeledField(icon = Icons.Filled.DataObject, value = data?.downloadSource ?: "", modifier = Modifier.weight(1f))
                Pill("连接正常", color = SuccessGreen)
            }
            Text("并发下载数", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface, fontSize = 13.sp)
            var conc by remember { mutableStateOf(data?.concurrency?.toFloat() ?: 4f) }
            Slider(value = conc, onValueChange = { conc = it }, valueRange = (data?.concurrencyMin ?: 1).toFloat()..(data?.concurrencyMax ?: 16).toFloat())
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("${data?.concurrencyMin ?: 1}", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                Text("${conc.toInt()} 个", color = MaterialTheme.colorScheme.primary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Text("${data?.concurrencyMax ?: 16}", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
            }
        }
    }
}

@Composable
private fun LaunchArgsCard(data: SettingsSnapshot?) {
    var debug by remember { mutableStateOf(data?.debugMode ?: false) }
    SoftCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            SectionHeaderRow(icon = Icons.Filled.Terminal, title = "启动参数", color = AccentPurple)
            Text("JVM 参数（可选）", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface, fontSize = 13.sp)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = data?.jvmArgs ?: "", onValueChange = {}, modifier = Modifier.weight(1f), singleLine = true, shape = RoundedCornerShape(10.dp))
                OutlinedButton(onClick = {}, shape = RoundedCornerShape(10.dp)) { Text("重置", fontSize = 12.sp) }
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Filled.BugReport, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("启用调试模式", color = MaterialTheme.colorScheme.onSurface, fontSize = 13.sp)
                    Text("启动时显示更多日志信息，便于排查问题。", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                }
                Switch(checked = debug, onCheckedChange = { debug = it })
            }
        }
    }
}

@Composable
private fun AccountPrivacyCard(data: SettingsSnapshot?) {
    SoftCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SectionHeaderRow(icon = Icons.Filled.Lock, title = "账户与隐私", color = AccentOrange)
            SettingLink(Icons.Filled.Person, "登录账户", data?.account ?: "")
            SettingLink(Icons.Filled.Lock, "隐私设置", data?.privacy ?: "")
        }
    }
}

@Composable
private fun SettingLink(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        IconBadge(icon = icon, color = MaterialTheme.colorScheme.onSurfaceVariant, size = 32.dp, iconSize = 16.dp)
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = MaterialTheme.colorScheme.onSurface, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Text(value, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
        }
        Icon(Icons.AutoMirrored.Filled.ArrowForwardIos, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(14.dp))
    }
}

@Composable
private fun LabeledField(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    value: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.clip(RoundedCornerShape(10.dp)).background(MaterialTheme.colorScheme.surfaceVariant).padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
        Text(value, color = MaterialTheme.colorScheme.onSurface, fontSize = 13.sp, modifier = Modifier.weight(1f))
        Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = SuccessGreen, modifier = Modifier.size(16.dp))
    }
}
