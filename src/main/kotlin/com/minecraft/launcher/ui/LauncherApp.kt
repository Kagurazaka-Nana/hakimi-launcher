package com.minecraft.launcher.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.minecraft.launcher.backend.HomeSnapshot
import com.minecraft.launcher.backend.LauncherBackend
import com.minecraft.launcher.ui.components.WorldHeroCanvas
import com.minecraft.launcher.ui.theme.AccentPurple
import com.minecraft.launcher.ui.theme.AccentYellow
import com.minecraft.launcher.ui.theme.CardWhite
import com.minecraft.launcher.ui.theme.HakimiColorScheme
import com.minecraft.launcher.ui.theme.InfoBlue
import com.minecraft.launcher.ui.theme.OnPrimary
import com.minecraft.launcher.ui.theme.PeachSurface
import com.minecraft.launcher.ui.theme.PrimaryOrange
import com.minecraft.launcher.ui.theme.ReadyGreen
import com.minecraft.launcher.ui.theme.TextBrown
import com.minecraft.launcher.ui.theme.TextMuted
import kotlinx.coroutines.launch

@Composable
fun LauncherApp(backend: LauncherBackend) {
    var snapshot by remember { mutableStateOf<HomeSnapshot?>(null) }
    var status by remember { mutableStateOf("准备就绪") }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        snapshot = backend.loadHome()
    }

    MaterialTheme(colorScheme = HakimiColorScheme) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            val data = snapshot
            Row(modifier = Modifier.fillMaxSize()) {
                Sidebar()
                Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                    TopBar(data)
                    Row(
                        modifier = Modifier.fillMaxWidth().weight(1f).padding(20.dp),
                        horizontalArrangement = Arrangement.spacedBy(18.dp)
                    ) {
                        CenterColumn(
                            data = data,
                            status = status,
                            onLaunch = {
                                scope.launch {
                                    val v = data?.launchVersion ?: "latest"
                                    backend.launch(v)
                                    status = "启动 $v（后端待接入）"
                                }
                            }
                        )
                        RightPanel(data)
                    }
                }
            }
        }
    }
}

@Composable
private fun Sidebar() {
    val items = listOf("首页" to "🏠", "实例" to "📦", "模组" to "🧩", "世界" to "⛰️", "下载" to "⬇️", "设置" to "⚙️")
    var selected by remember { mutableStateOf("首页") }

    Column(
        modifier = Modifier
            .width(180.dp)
            .fillMaxHeight()
            .background(PeachSurface)
            .padding(vertical = 20.dp, horizontal = 14.dp)
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(CardWhite),
            contentAlignment = Alignment.Center
        ) {
            Text("🐱", fontSize = 30.sp)
        }
        Spacer(modifier = Modifier.height(28.dp))
        items.forEach { (label, icon) ->
            val active = label == selected
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 5.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(if (active) PrimaryOrange else Color.Transparent)
                    .clickable { selected = label }
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(icon, fontSize = 18.sp)
                    Text(
                        text = label,
                        color = if (active) OnPrimary else TextBrown,
                        fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
                        fontSize = 15.sp
                    )
                }
            }
        }
        Spacer(modifier = Modifier.weight(1f))
        Text("🐱", fontSize = 40.sp, modifier = Modifier.padding(start = 6.dp))
        Text("✨ ⭐ ✨", fontSize = 12.sp, color = TextMuted)
    }
}

@Composable
private fun TopBar(data: HomeSnapshot?) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, top = 20.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("hakimi 启动器", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = TextBrown)
        Text(" 🐾", fontSize = 22.sp)
        Spacer(modifier = Modifier.weight(1f))
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(AccentPurple),
            contentAlignment = Alignment.Center
        ) {
            Text("🙂", fontSize = 20.sp)
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column {
            Text(data?.username ?: "…", fontWeight = FontWeight.SemiBold, color = TextBrown)
            Text(data?.profileType ?: "本地档案", fontSize = 12.sp, color = TextMuted)
        }
        Text("  ⌄", color = TextMuted)
    }
}

@Composable
private fun RowScope.CenterColumn(
    data: HomeSnapshot?,
    status: String,
    onLaunch: () -> Unit
) {
    Column(
        modifier = Modifier.weight(2.4f).fillMaxHeight(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        WorldHeroCard(data)
        InstanceChipsRow(data)
        LaunchButton(version = data?.launchVersion ?: "", onClick = onLaunch)
    }
}

@Composable
private fun ColumnScope.WorldHeroCard(data: HomeSnapshot?) {
    SoftCard(modifier = Modifier.fillMaxWidth().weight(1f)) {
        Box(modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(18.dp))) {
            WorldHeroCanvas(modifier = Modifier.fillMaxSize())
            // 顶部信息遮罩
            Column(modifier = Modifier.fillMaxSize().padding(20.dp)) {
                Row(verticalAlignment = Alignment.Top) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(data?.worldName ?: "加载中…", fontSize = 30.sp, fontWeight = FontWeight.Bold, color = TextBrown)
                            Text(" ⭐", fontSize = 20.sp)
                        }
                        Text(
                            data?.worldDescription ?: "",
                            color = TextBrown,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Pill(text = if (data?.ready == true) "● 准备就绪" else "○ 未就绪", bg = ReadyGreen.copy(alpha = 0.18f), fg = ReadyGreen)
                    }
                    Pill(text = "⛰ ${data?.worldMode ?: "生存"}", bg = Color(0x33FFFFFF), fg = TextBrown)
                }
                Spacer(modifier = Modifier.weight(1f))
                Text("🧭", fontSize = 26.sp, modifier = Modifier.align(Alignment.End))
            }
        }
    }
}

@Composable
private fun InstanceChipsRow(data: HomeSnapshot?) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        SoftCard {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("🟩")
                Text(data?.instanceLabel ?: "…", fontWeight = FontWeight.SemiBold, color = TextBrown)
                Text("⌄", color = TextMuted)
            }
        }
        data?.modTags?.forEach { tag ->
            Chip(text = tag)
        }
        SoftCard {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("🧩 ${data?.modCount ?: 0} 个模组", color = TextMuted, fontSize = 13.sp)
                Text("🗄 ${data?.instanceSize ?: ""}", color = TextMuted, fontSize = 13.sp)
            }
        }
    }
}

@Composable
private fun LaunchButton(version: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        shape = RoundedCornerShape(28.dp),
        colors = ButtonDefaults.buttonColors(containerColor = PrimaryOrange, contentColor = OnPrimary),
        modifier = Modifier.fillMaxWidth().height(64.dp)
    ) {
        Text("▶", fontSize = 18.sp)
        Spacer(modifier = Modifier.width(10.dp))
        Text("启动 $version", fontSize = 20.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun RightPanel(data: HomeSnapshot?) {
    Column(
        modifier = Modifier.width(300.dp).fillMaxHeight(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("系统状态 🐾", fontWeight = FontWeight.Bold, color = TextBrown, fontSize = 16.sp)
        SoftCard {
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                if (data != null) {
                    StatRow("🖥", "内存", "%.1f / %.0f GB".format(data.memoryUsedGb, data.memoryTotalGb),
                        "%.0f%%".format(data.memoryUsedGb / data.memoryTotalGb * 100),
                        (data.memoryUsedGb / data.memoryTotalGb).toFloat(), InfoBlue)
                    StatRow("☕", "Java", data.javaVersion, if (data.javaOk) "OK" else "异常",
                        if (data.javaOk) 1f else 0.3f, ReadyGreen)
                    StatRow("📦", data.loaderName, data.loaderVersion, if (data.loaderCompatible) "兼容" else "不兼容",
                        if (data.loaderCompatible) 1f else 0.3f, AccentYellow)
                }
            }
        }
        Text("最近活动 🐾", fontWeight = FontWeight.Bold, color = TextBrown, fontSize = 16.sp)
        SoftCard(modifier = Modifier.fillMaxWidth().weight(1f)) {
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                data?.recentActivities?.forEach { act ->
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(Color(act.colorHex))
                        )
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(Color(act.colorHex).copy(alpha = 0.16f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(activityGlyph(act.icon), fontSize = 16.sp)
                        }
                        Text(act.text, modifier = Modifier.weight(1f), color = TextBrown, fontSize = 13.sp)
                        Text(act.time, color = TextMuted, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun StatRow(icon: String, label: String, value: String, badge: String, progress: Float, color: Color) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).background(color.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) { Text(icon, fontSize = 18.sp) }
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(label, fontWeight = FontWeight.SemiBold, color = TextBrown, fontSize = 14.sp)
                Text(value, color = TextMuted, fontSize = 12.sp)
            }
            Text(badge, color = color, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
        }
        LinearProgressIndicator(
            progress = { progress.coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
            color = color,
            trackColor = color.copy(alpha = 0.15f),
        )
    }
}

@Composable
private fun Chip(text: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(CardWhite)
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Text("◆ $text", color = TextBrown, fontSize = 13.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun Pill(text: String, bg: Color, fg: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(bg)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(text, color = fg, fontSize = 13.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun SoftCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = CardWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = null,
    ) {
        Column(modifier = Modifier.fillMaxWidth(), content = content)
    }
}

private fun activityGlyph(icon: String): String = when (icon) {
    "puzzle" -> "🧩"
    "download" -> "⬇️"
    "cube" -> "📦"
    "mountain" -> "⛰️"
    "folder" -> "📁"
    else -> "•"
}
