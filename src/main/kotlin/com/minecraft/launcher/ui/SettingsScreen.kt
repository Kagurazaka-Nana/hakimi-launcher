package com.minecraft.launcher.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.minecraft.launcher.ui.components.AvatarCircle
import com.minecraft.launcher.ui.components.PageHeader
import com.minecraft.launcher.backend.SettingsSnapshot
import com.minecraft.launcher.ui.theme.HakimiButton
import com.minecraft.launcher.ui.theme.HakimiCard
import com.minecraft.launcher.ui.theme.HakimiChip
import com.minecraft.launcher.ui.theme.HakimiIcon
import com.minecraft.launcher.ui.theme.HakimiSearchField
import com.minecraft.launcher.ui.theme.HakimiText
import com.minecraft.launcher.ui.theme.HakimiTheme
import com.minecraft.launcher.ui.theme.HakimiToggle
import com.minecraft.launcher.ui.state.LauncherViewModel
import kotlin.math.roundToInt

@Composable
fun SettingsScreen(vm: LauncherViewModel) {
    val state by vm.state.collectAsState()
    val data = state.settings
    val c = HakimiTheme.colors

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        PageHeader(
            icon = HakimiIcons.Settings,
            title = "设置",
            subtitle = "调整你的启动器偏好，让 Minecraft 之旅更顺畅！",
            trailing = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AvatarCircle(HakimiIcons.Person, c.primary)
                    Column {
                        HakimiText("hakimi 👑", style = HakimiTheme.type.label)
                        HakimiText("陪你探索更多方块世界～", style = HakimiTheme.type.caption, color = c.textMuted)
                    }
                }
            }
        )

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(18.dp)) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(18.dp)) {
                AppearanceCard(data?.theme ?: "", data?.language ?: "")
                DownloadSourceCard(data?.downloadSource ?: "", data?.concurrency ?: 4, data?.concurrencyMin ?: 1, data?.concurrencyMax ?: 16)
                NetworkProxyCard(vm, data)
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(18.dp)) {
                JavaMemoryCard(data?.javaPath ?: "", data?.javaVersion ?: "", data?.maxMemoryMb ?: 4096, data?.memoryMinMb ?: 512, data?.memoryMaxMb ?: 8192)
                LaunchArgsCard(data?.jvmArgs ?: "", data?.debugMode ?: false)
                AccountPrivacyCard(data?.account ?: "", data?.privacy ?: "")
            }
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            HakimiButton(text = "保存设置", icon = HakimiIcons.Check, onClick = { /* 保存：待接入持久化 */ })
        }
    }
}

@Composable
private fun AppearanceCard(theme: String, language: String) {
    HakimiCard(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            SectionTitle(HakimiIcons.Settings, "外观")
            HakimiText("主题切换", style = HakimiTheme.type.label)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                listOf("浅色", "深色", "跟随系统").forEach { HakimiChip(it, selected = it == theme) }
            }
            HakimiText("语言选择", style = HakimiTheme.type.label)
            ReadOnlyField(language.ifBlank { "简体中文" })
        }
    }
}

@Composable
private fun JavaMemoryCard(javaPath: String, javaVersion: String, maxMem: Int, min: Int, max: Int) {
    val c = HakimiTheme.colors
    HakimiCard(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            SectionTitle(HakimiIcons.Memory, "Java 与内存")
            HakimiText("Java 路径", style = HakimiTheme.type.label)
            ReadOnlyField(javaPath.ifBlank { "C:\\Program Files\\Java\\jdk-21" })
            HakimiText("Java 版本：${javaVersion.ifBlank { "21.0.3 · 64 位" }}", style = HakimiTheme.type.caption, color = c.textMuted)
            HakimiText("最大内存", style = HakimiTheme.type.label)
            HakimiSlider(initialValue = maxMem.toFloat(), valueRange = min.toFloat()..max.toFloat()) { v ->
                HakimiText("${v.toInt()} MB", style = HakimiTheme.type.caption, color = c.primary)
            }
        }
    }
}

@Composable
private fun DownloadSourceCard(source: String, concurrency: Int, min: Int, max: Int) {
    val c = HakimiTheme.colors
    HakimiCard(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            SectionTitle(HakimiIcons.Download, "下载源")
            HakimiText("下载源地址", style = HakimiTheme.type.label)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ReadOnlyField(source.ifBlank { "官方源（推荐）" }, modifier = Modifier.weight(1f))
                HakimiChip("连接正常", color = c.success, selected = true)
            }
            HakimiText("并发下载数", style = HakimiTheme.type.label)
            HakimiSlider(initialValue = concurrency.toFloat(), valueRange = min.toFloat()..max.toFloat()) { v ->
                HakimiText("${v.toInt()} 个", style = HakimiTheme.type.caption, color = c.primary)
            }
        }
    }
}

@Composable
private fun LaunchArgsCard(jvmArgs: String, debugMode: Boolean) {
    val c = HakimiTheme.colors
    var debug by remember { mutableFloatStateOf(if (debugMode) 1f else 0f) }
    HakimiCard(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            SectionTitle(HakimiIcons.Settings, "启动参数")
            HakimiText("JVM 参数（可选）", style = HakimiTheme.type.label)
            ReadOnlyField(jvmArgs.ifBlank { "-XX:+UseG1GC -XX:MaxGCPauseMillis=200" })
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Column(modifier = Modifier.weight(1f)) {
                    HakimiText("启用调试模式", style = HakimiTheme.type.label)
                    HakimiText("启动时显示更多日志信息，便于排查问题。", style = HakimiTheme.type.caption, color = c.textMuted)
                }
                HakimiToggle(checked = debug > 0.5f, onCheckedChange = { debug = if (it) 1f else 0f })
            }
        }
    }
}

@Composable
private fun AccountPrivacyCard(account: String, privacy: String) {
    val c = HakimiTheme.colors
    HakimiCard(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SectionTitle(HakimiIcons.Person, "账户与隐私")
            SettingLink("登录账户", account.ifBlank { "已登录：hakimi（离线模式）" })
            SettingLink("隐私设置", privacy.ifBlank { "不收集使用数据 · 仅本地存储" })
        }
    }
}

@Composable
private fun NetworkProxyCard(vm: LauncherViewModel, settings: SettingsSnapshot?) {
    val c = HakimiTheme.colors
    var enabled by remember(settings?.proxyEnabled) { mutableStateOf(settings?.proxyEnabled ?: false) }
    var host by remember(settings?.proxyHost) { mutableStateOf(settings?.proxyHost ?: "") }
    var port by remember(settings?.proxyPort) { mutableStateOf((settings?.proxyPort ?: 0).toString()) }
    fun apply() = vm.setProxy(enabled, host, port.toIntOrNull() ?: 0)
    HakimiCard(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            SectionTitle(HakimiIcons.Network, "网络代理")
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Column(modifier = Modifier.weight(1f)) {
                    HakimiText("启用 HTTP 代理", style = HakimiTheme.type.label)
                    HakimiText("下载请求经代理转发，仅影响新连接", style = HakimiTheme.type.caption, color = c.textMuted)
                }
                HakimiToggle(checked = enabled, onCheckedChange = { enabled = it; apply() })
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                HakimiSearchField(
                    value = host,
                    onValueChange = { host = it; apply() },
                    placeholder = "代理地址，如 127.0.0.1",
                    icon = HakimiIcons.Server,
                    modifier = Modifier.weight(1f),
                )
                HakimiSearchField(
                    value = port,
                    onValueChange = { v -> port = v.filter(Char::isDigit).take(5); apply() },
                    placeholder = "端口",
                    icon = HakimiIcons.Plugin,
                    modifier = Modifier.width(140.dp),
                )
            }
        }
    }
}

@Composable
private fun SectionTitle(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        HakimiIcon(icon, null, HakimiTheme.colors.primary, size = 18.dp)
        HakimiText(title, style = HakimiTheme.type.title)
    }
}

@Composable
private fun ReadOnlyField(value: String, modifier: Modifier = Modifier) {
    val c = HakimiTheme.colors
    Row(
        modifier = modifier.fillMaxWidth().clip(HakimiTheme.shapes.medium).background(c.surfaceMuted).padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        HakimiText(value, style = HakimiTheme.type.body, color = c.text, modifier = Modifier.weight(1f), maxLines = 1)
    }
}

@Composable
private fun SettingLink(title: String, value: String) {
    val c = HakimiTheme.colors
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Column(modifier = Modifier.weight(1f)) {
            HakimiText(title, style = HakimiTheme.type.label)
            HakimiText(value, style = HakimiTheme.type.caption, color = c.textMuted)
        }
        HakimiText("›", style = HakimiTheme.type.title, color = c.textMuted)
    }
}

/** 自定义横向滑块（Foundation，无 Material）。 */
@Composable
private fun HakimiSlider(
    initialValue: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    onValue: @Composable (Float) -> Unit,
) {
    val c = HakimiTheme.colors
    var value by remember { mutableFloatStateOf(initialValue) }
    val trackWidth = 240.dp
    val span = (valueRange.endInclusive - valueRange.start).let { if (it <= 0f) 1f else it }
    val fraction = ((value - valueRange.start) / span).coerceIn(0f, 1f)

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(
            modifier = Modifier
                .width(trackWidth)
                .height(28.dp)
                .pointerInput(valueRange) {
                    detectHorizontalDragGestures { change, _ ->
                        val w = size.width
                        val f = (change.position.x / w).coerceIn(0f, 1f)
                        value = valueRange.start + f * span
                    }
                },
            contentAlignment = Alignment.CenterStart,
        ) {
            Box(modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape).background(c.surfaceMuted))
            Box(modifier = Modifier.fillMaxWidth(fraction).height(8.dp).clip(CircleShape).background(c.primary))
            Box(
                modifier = Modifier
                    .offset { IntOffset((trackWidth.toPx() * fraction).roundToInt() - 12, 0) }
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(c.surface),
            )
        }
        Row(modifier = Modifier.width(trackWidth), horizontalArrangement = Arrangement.SpaceBetween) {
            HakimiText("${valueRange.start.toInt()}", style = HakimiTheme.type.caption, color = c.textMuted)
            onValue(value)
            HakimiText("${valueRange.endInclusive.toInt()}", style = HakimiTheme.type.caption, color = c.textMuted)
        }
    }
}
