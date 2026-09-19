package com.minecraft.launcher.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.minecraft.launcher.backend.LauncherBackend

@Composable
fun LauncherApp(backend: LauncherBackend) {
    var username by remember { mutableStateOf("...") }
    var versions by remember { mutableStateOf(listOf<String>()) }
    var selectedVersion by remember { mutableStateOf<String?>(null) }
    var status by remember { mutableStateOf("空闲") }

    LaunchedEffect(Unit) {
        username = backend.getCurrentUsername()
        versions = backend.loadVersions()
        selectedVersion = versions.firstOrNull()
    }

    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier.fillMaxSize().padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Hakimi Launcher",
                    style = MaterialTheme.typography.headlineMedium
                )

                AccountSection(username = username)

                VersionSection(
                    versions = versions,
                    selectedVersion = selectedVersion,
                    onVersionSelected = { selectedVersion = it }
                )

                LaunchSection(
                    onRefresh = { status = "刷新 manifest（尚未接入后端）" },
                    onDownload = { status = "下载资源（尚未接入后端）" },
                    onLaunch = {
                        status = selectedVersion
                            ?.let { "启动 $it（尚未接入后端）" }
                            ?: "请先选择版本"
                    }
                )

                Spacer(modifier = Modifier.weight(1f))

                Text(
                    text = "状态：$status",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun AccountSection(username: String) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("账号", style = MaterialTheme.typography.labelMedium)
                Text(username, style = MaterialTheme.typography.bodyLarge)
            }
            OutlinedButton(onClick = { /* 账号管理：待设计 */ }) {
                Text("切换账号")
            }
        }
    }
}

@Composable
private fun VersionSection(
    versions: List<String>,
    selectedVersion: String?,
    onVersionSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("版本", style = MaterialTheme.typography.labelMedium)
                Text(
                    selectedVersion ?: "加载中...",
                    style = MaterialTheme.typography.bodyLarge
                )
            }
            Column {
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
private fun LaunchSection(
    onRefresh: () -> Unit,
    onDownload: () -> Unit,
    onLaunch: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedButton(onClick = onRefresh) { Text("刷新清单") }
        OutlinedButton(onClick = onDownload) { Text("下载资源") }
        Button(onClick = onLaunch) { Text("启动游戏") }
    }
}
