package com.minecraft.launcher.ui

import androidx.compose.foundation.background
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
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.minecraft.launcher.backend.LauncherBackend
import kotlinx.coroutines.launch

@Composable
fun LauncherApp(backend: LauncherBackend) {
    var username by remember { mutableStateOf("加载中...") }
    var versions by remember { mutableStateOf(listOf<String>()) }
    var selectedVersion by remember { mutableStateOf<String?>(null) }
    var status by remember { mutableStateOf("准备就绪") }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        username = backend.getCurrentUsername()
        versions = backend.loadVersions()
        selectedVersion = versions.firstOrNull()
    }

    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier.fillMaxSize().padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                HomeHeader()

                Row(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(18.dp)
                ) {
                    Column(
                        modifier = Modifier.weight(2f).fillMaxHeight(),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        AccountCard(username = username)
                        VersionCard(
                            versions = versions,
                            selectedVersion = selectedVersion,
                            onVersionSelected = {
                                selectedVersion = it
                                status = "已选择版本：$it"
                            }
                        )
                        LaunchCard(
                            selectedVersion = selectedVersion,
                            onRefresh = {
                                scope.launch {
                                    backend.refreshManifest()
                                    status = "版本清单刷新入口已预留"
                                }
                            },
                            onDownload = {
                                scope.launch {
                                    val version = selectedVersion
                                    if (version == null) {
                                        status = "请先选择版本"
                                        return@launch
                                    }
                                    backend.ensureVersionReady(version)
                                    status = "资源准备入口已预留：$version"
                                }
                            },
                            onLaunch = {
                                scope.launch {
                                    val version = selectedVersion
                                    if (version == null) {
                                        status = "请先选择版本"
                                        return@launch
                                    }
                                    backend.launch(version)
                                    status = "启动入口已预留：$version"
                                }
                            }
                        )
                    }

                    Column(
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        StatusCard(status = status)
                        PlanCard()
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeHeader() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "Hakimi Launcher",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "Kotlin Compose Multiplatform Material 桌面首页",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        StatusBadge(text = "Frontend Preview")
    }
}

@Composable
private fun AccountCard(username: String) {
    HomeCard(title = "账号") {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = username,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "当前为离线占位账号，正版登录接口待接入。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            OutlinedButton(onClick = { /* 账号管理：待设计 */ }) {
                Text("切换账号")
            }
        }
    }
}

@Composable
private fun VersionCard(
    versions: List<String>,
    selectedVersion: String?,
    onVersionSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    HomeCard(title = "版本") {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = selectedVersion ?: "暂无版本",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "版本列表来自 LauncherBackend，当前为 Stub 数据。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Box {
                OutlinedButton(
                    onClick = { expanded = true },
                    enabled = versions.isNotEmpty()
                ) {
                    Text("选择版本")
                }
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    versions.forEach { version ->
                        DropdownMenuItem(
                            text = { Text(version) },
                            onClick = {
                                onVersionSelected(version)
                                expanded = false
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LaunchCard(
    selectedVersion: String?,
    onRefresh: () -> Unit,
    onDownload: () -> Unit,
    onLaunch: () -> Unit
) {
    HomeCard(title = "启动") {
        Text(
            text = "先搭建首页框架，真实下载、校验、登录和启动逻辑后续通过后端接口接入。",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(14.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedButton(onClick = onRefresh) { Text("刷新清单") }
            OutlinedButton(onClick = onDownload) { Text("准备资源") }
            Button(
                onClick = onLaunch,
                enabled = selectedVersion != null
            ) {
                Text("启动游戏")
            }
        }
    }
}

@Composable
private fun StatusCard(status: String) {
    HomeCard(title = "状态") {
        StatusBadge(text = status)
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "当前页面不会访问网络、不会写入文件、不会拉起游戏进程。",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun PlanCard() {
    HomeCard(title = "后续接入") {
        PlaceholderLine("Microsoft 账号登录")
        PlaceholderLine("Mojang manifest 刷新")
        PlaceholderLine("版本资源下载与校验")
        PlaceholderLine("Jvm/Game arguments 生成")
        PlaceholderLine("Minecraft 进程启动")
    }
}

@Composable
private fun HomeCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        )
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold
            )
            content()
        }
    }
}

@Composable
private fun StatusBadge(text: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(MaterialTheme.colorScheme.primaryContainer)
            .padding(horizontal = 12.dp, vertical = 7.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}

@Composable
private fun PlaceholderLine(text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(RoundedCornerShape(50))
                .background(MaterialTheme.colorScheme.primary)
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}
