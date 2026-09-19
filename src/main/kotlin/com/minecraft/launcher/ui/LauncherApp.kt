package com.minecraft.launcher.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DataObject
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.minecraft.launcher.backend.DownloadItem
import com.minecraft.launcher.backend.DownloadTask
import com.minecraft.launcher.backend.DownloadsSnapshot
import com.minecraft.launcher.backend.HomeSnapshot
import com.minecraft.launcher.backend.InstanceItem
import com.minecraft.launcher.backend.LauncherBackend
import com.minecraft.launcher.backend.SettingsSnapshot
import com.minecraft.launcher.ui.components.IconBadge
import com.minecraft.launcher.ui.components.PageHeader
import com.minecraft.launcher.ui.components.Pill
import com.minecraft.launcher.ui.components.PlaceholderThumb
import com.minecraft.launcher.ui.components.SoftCard
import com.minecraft.launcher.ui.theme.CardWhite
import com.minecraft.launcher.ui.theme.ChipBg
import com.minecraft.launcher.ui.theme.ChipText
import com.minecraft.launcher.ui.theme.ContentBackground
import com.minecraft.launcher.ui.theme.HakimiColorScheme
import com.minecraft.launcher.ui.theme.OnPrimary
import com.minecraft.launcher.ui.theme.PrimaryIndigo
import com.minecraft.launcher.ui.theme.SidebarActive
import com.minecraft.launcher.ui.theme.SidebarDark
import com.minecraft.launcher.ui.theme.SidebarTextMuted
import com.minecraft.launcher.ui.theme.TextDark
import com.minecraft.launcher.ui.theme.TextMuted
import kotlinx.coroutines.launch

@Composable
fun LauncherApp(backend: LauncherBackend) {
    var home by remember { mutableStateOf<HomeSnapshot?>(null) }
    var instances by remember { mutableStateOf<List<InstanceItem>>(emptyList()) }
    var downloads by remember { mutableStateOf<DownloadsSnapshot?>(null) }
    var settings by remember { mutableStateOf<SettingsSnapshot?>(null) }
    var selectedNav by remember { mutableStateOf("首页") }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        home = backend.loadHome()
        instances = backend.loadInstances()
        downloads = backend.loadDownloads()
        settings = backend.loadSettings()
    }

    MaterialTheme(colorScheme = HakimiColorScheme) {
        Surface(modifier = Modifier.fillMaxSize(), color = ContentBackground) {
            Column(modifier = Modifier.fillMaxSize()) {
                TopBar()
                Row(modifier = Modifier.weight(1f)) {
                    Sidebar(selected = selectedNav, onSelect = { selectedNav = it })
                    Box(
                        modifier = Modifier.weight(1f).fillMaxHeight().background(ContentBackground)
                    ) {
                        when (selectedNav) {
                            "首页" -> HomeScreen(
                                data = home,
                                onLaunch = { scope.launch { backend.launch(home?.version ?: "latest") } }
                            )
                            "实例" -> InstancesScreen(instances)
                            "下载" -> DownloadsScreen(downloads)
                            "设置" -> SettingsScreen(settings)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TopBar() {
    Row(
        modifier = Modifier.fillMaxWidth().height(46.dp).background(SidebarDark).padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(28.dp).clip(RoundedCornerShape(8.dp)).background(CardWhite),
            contentAlignment = Alignment.Center
        ) { Icon(Icons.Filled.Pets, contentDescription = null, tint = PrimaryIndigo, modifier = Modifier.size(18.dp)) }
        Spacer(modifier = Modifier.width(10.dp))
        Text("hakimi-launcher", color = CardWhite, fontWeight = FontWeight.Bold, fontSize = 15.sp)
        Spacer(modifier = Modifier.weight(1f))
        Text("—", color = SidebarTextMuted, fontSize = 16.sp, modifier = Modifier.padding(horizontal = 10.dp))
        Text("▢", color = SidebarTextMuted, fontSize = 14.sp, modifier = Modifier.padding(horizontal = 10.dp))
        Text("✕", color = SidebarTextMuted, fontSize = 14.sp, modifier = Modifier.padding(horizontal = 10.dp))
    }
}

@Composable
private fun Sidebar(selected: String, onSelect: (String) -> Unit) {
    val items = listOf("首页" to Icons.Filled.Home, "实例" to Icons.Filled.Layers, "下载" to Icons.Filled.Download, "设置" to Icons.Filled.Settings)
    Column(
        modifier = Modifier.width(190.dp).fillMaxHeight().background(SidebarDark).padding(horizontal = 14.dp, vertical = 18.dp)
    ) {
        items.forEach { (label, icon) ->
            val active = label == selected
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 5.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(if (active) SidebarActive else Color.Transparent)
                    .clickable { onSelect(label) }
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(icon, contentDescription = null, tint = if (active) OnPrimary else SidebarTextMuted)
                Text(label, color = if (active) OnPrimary else SidebarTextMuted, fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal)
            }
        }
        Spacer(modifier = Modifier.weight(1f))
        Icon(Icons.Filled.Pets, contentDescription = null, tint = SidebarTextMuted.copy(alpha = 0.5f), modifier = Modifier.size(40.dp))
    }
}

@Composable
private fun HomeScreen(data: HomeSnapshot?, onLaunch: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        PageHeader(
            icon = Icons.Filled.Pets,
            title = data?.welcomeTitle ?: "欢迎回来！",
            subtitle = data?.welcomeSubtitle ?: "",
            right = { PlaceholderThumb(Icons.Filled.Pets, modifier = Modifier.size(96.dp)) }
        )
        Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(18.dp)) {
            Column(modifier = Modifier.weight(1.7f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(18.dp)) {
                HeroCard(data, onLaunch)
                Row(modifier = Modifier.height(180.dp), horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                    QuickActionsCard(data, modifier = Modifier.weight(1f))
                    LoadingCard(data, modifier = Modifier.weight(1f))
                }
            }
            HomeRightPanel(data, modifier = Modifier.width(300.dp))
        }
    }
}

@Composable
private fun ColumnScope.HeroCard(data: HomeSnapshot?, onLaunch: () -> Unit) {
    SoftCard(modifier = Modifier.fillMaxWidth().weight(1f)) {
        Row(modifier = Modifier.fillMaxSize().padding(20.dp), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            PlaceholderThumb(Icons.Filled.Home, modifier = Modifier.width(220.dp).fillMaxHeight())
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Pill("当前实例")
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(data?.instanceName ?: "…", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = TextDark)
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(Icons.Filled.Edit, contentDescription = null, tint = TextMuted, modifier = Modifier.size(18.dp))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(28.dp)) {
                    MetaCell(Icons.Filled.DataObject, "版本", data?.version ?: "")
                    MetaCell(Icons.Filled.Extension, "加载器", data?.loader ?: "")
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    data?.modeTags?.forEach { tag -> MetaChip(tag) }
                }
                Spacer(modifier = Modifier.weight(1f))
                Button(
                    onClick = onLaunch,
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo, contentColor = OnPrimary),
                    modifier = Modifier.fillMaxWidth().height(56.dp)
                ) {
                    Icon(Icons.Filled.PlayArrow, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("启动游戏", fontSize = 17.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun QuickActionsCard(data: HomeSnapshot?, modifier: Modifier = Modifier) {
    SoftCard(modifier = modifier) {
        Column(modifier = Modifier.fillMaxSize().padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Filled.PlayArrow, contentDescription = null, tint = PrimaryIndigo)
                Text("快速操作", fontWeight = FontWeight.Bold, color = TextDark)
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                data?.quickActions?.forEach { action ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        IconBadge(icon = iconFor(action.icon), color = Color(action.colorHex), size = 52.dp)
                        Text(action.label, color = TextDark, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun LoadingCard(data: HomeSnapshot?, modifier: Modifier = Modifier) {
    SoftCard(modifier = modifier) {
        Column(modifier = Modifier.fillMaxSize().padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Filled.Download, contentDescription = null, tint = PrimaryIndigo)
                Text(data?.loadingText ?: "加载中…", fontWeight = FontWeight.Bold, color = TextDark)
            }
            Text(data?.loadingHint ?: "", color = TextMuted, fontSize = 13.sp)
            Spacer(modifier = Modifier.weight(1f))
            androidx.compose.material3.LinearProgressIndicator(
                progress = { (data?.loadingPercent ?: 0) / 100f },
                modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                color = PrimaryIndigo,
                trackColor = ChipBg,
            )
            Text("加载游戏文件中… ${data?.loadingPercent ?: 0}%", color = TextMuted, fontSize = 12.sp)
        }
    }
}

@Composable
private fun HomeRightPanel(data: HomeSnapshot?, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        ProfileCard(data)
        RecentCard(data)
        ResourceStatusCard(data, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun ProfileCard(data: HomeSnapshot?) {
    SoftCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                com.minecraft.launcher.ui.components.AvatarCircle(Icons.Filled.Home, PrimaryIndigo)
                Column {
                    Text(data?.profileName ?: "…", fontWeight = FontWeight.Bold, color = TextDark)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(8.dp).clip(RoundedCornerShape(50)).background(com.minecraft.launcher.ui.theme.SuccessGreen))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (data?.profileOnline == true) "在线" else "离线", color = TextMuted, fontSize = 12.sp)
                    }
                }
            }
            Text(data?.profileTagline ?: "", color = TextMuted, fontSize = 12.sp)
        }
    }
}

@Composable
private fun RecentCard(data: HomeSnapshot?) {
    SoftCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Filled.Home, contentDescription = null, tint = PrimaryIndigo)
                Text("最近游玩", fontWeight = FontWeight.Bold, color = TextDark, modifier = Modifier.weight(1f))
                Text("查看更多 >", color = TextMuted, fontSize = 12.sp)
            }
            data?.recentPlay?.let { rp ->
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    PlaceholderThumb(Icons.Filled.Home, modifier = Modifier.size(48.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(rp.name, fontWeight = FontWeight.SemiBold, color = TextDark, fontSize = 13.sp)
                        Text(rp.detail, color = TextMuted, fontSize = 12.sp)
                        Text(rp.time, color = TextMuted, fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun ResourceStatusCard(data: HomeSnapshot?, modifier: Modifier = Modifier) {
    SoftCard(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Filled.Layers, contentDescription = null, tint = PrimaryIndigo)
                Text("资源状态", fontWeight = FontWeight.Bold, color = TextDark, modifier = Modifier.weight(1f))
                Text("全部正常 >", color = com.minecraft.launcher.ui.theme.SuccessGreen, fontSize = 12.sp)
            }
            data?.resourceStatus?.forEach { row ->
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    IconBadge(icon = iconFor(row.icon), color = Color(row.colorHex), size = 32.dp, iconSize = 18.dp)
                    Text(row.label, color = TextDark, fontSize = 13.sp, modifier = Modifier.weight(1f))
                    Pill("已就绪", bg = com.minecraft.launcher.ui.theme.SuccessBg, fg = com.minecraft.launcher.ui.theme.SuccessGreen)
                }
            }
        }
    }
}

@Composable
private fun MetaCell(icon: ImageVector, label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Icon(icon, contentDescription = null, tint = TextMuted, modifier = Modifier.size(20.dp))
        Column {
            Text(label, color = TextMuted, fontSize = 12.sp)
            Text(value, color = TextDark, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
        }
    }
}

@Composable
private fun MetaChip(text: String) {
    Pill(text)
}

internal fun iconFor(key: String): ImageVector = when (key) {
    "cat" -> Icons.Filled.Pets
    "folder" -> Icons.Filled.Home
    "puzzle" -> Icons.Filled.Extension
    "download" -> Icons.Filled.Download
    "cube" -> Icons.Filled.DataObject
    "image" -> Icons.Filled.Layers
    "add" -> Icons.Filled.PlayArrow
    "home" -> Icons.Filled.Home
    "coffee" -> Icons.Filled.PlayArrow
    "person" -> Icons.Filled.Home
    else -> Icons.Filled.Settings
}
