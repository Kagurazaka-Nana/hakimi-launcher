package com.minecraft.launcher.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DataObject
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.minecraft.launcher.backend.DownloadItem
import com.minecraft.launcher.backend.DownloadTask
import com.minecraft.launcher.backend.DownloadsSnapshot
import com.minecraft.launcher.ui.components.PageHeader
import com.minecraft.launcher.ui.components.Pill
import com.minecraft.launcher.ui.components.PlaceholderThumb
import com.minecraft.launcher.ui.components.SectionHeaderRow
import com.minecraft.launcher.ui.components.SoftCard
import com.minecraft.launcher.ui.theme.ChipBg
import com.minecraft.launcher.ui.theme.OnPrimary
import com.minecraft.launcher.ui.theme.PrimaryIndigo
import com.minecraft.launcher.ui.theme.SuccessBg
import com.minecraft.launcher.ui.theme.SuccessGreen
import com.minecraft.launcher.ui.theme.TextDark
import com.minecraft.launcher.ui.theme.TextMuted

@Composable
fun DownloadsScreen(data: DownloadsSnapshot?) {
    var query by remember { mutableStateOf("") }
    var category by remember { mutableStateOf(data?.selectedCategory ?: "") }

    Column(modifier = Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        PageHeader(icon = Icons.Filled.Download, title = "下载", subtitle = "海量资源 · 一键获取 · 让你的世界更丰富")

        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("搜索版本、模组、整合包等…", color = TextMuted) },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, tint = TextMuted) },
            shape = RoundedCornerShape(12.dp),
            singleLine = true,
        )

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            data?.categories?.forEach { cat ->
                val active = cat == category
                Text(
                    text = cat,
                    color = if (active) OnPrimary else TextDark,
                    fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(if (active) PrimaryIndigo else ChipBg)
                        .clickable { category = cat }
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }
        }

        Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(18.dp)) {
            LazyColumn(modifier = Modifier.weight(1.6f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(data?.items ?: emptyList()) { item -> DownloadItemRow(item) }
            }
            QueuePanel(data?.queue ?: emptyList(), modifier = Modifier.width(300.dp))
        }
    }
}

@Composable
private fun DownloadItemRow(item: DownloadItem) {
    SoftCard(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
            PlaceholderThumb(iconForCategory(item.badge), modifier = Modifier.size(64.dp))
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(item.name, fontWeight = FontWeight.Bold, color = TextDark, fontSize = 15.sp)
                    Pill(item.badge)
                }
                Text("版本：${item.version}    兼容加载器：${item.loaders}", color = TextMuted, fontSize = 12.sp)
                Text(item.description, color = TextMuted, fontSize = 12.sp)
            }
            Button(
                onClick = {},
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo, contentColor = OnPrimary)
            ) {
                Icon(Icons.Filled.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Column {
                    Text("下载", fontSize = 13.sp)
                    Text(item.size, fontSize = 10.sp)
                }
            }
        }
    }
}

@Composable
private fun QueuePanel(queue: List<DownloadTask>, modifier: Modifier = Modifier) {
    SoftCard(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            SectionHeaderRow(icon = Icons.Filled.Layers, title = "下载队列", trailing = { Text("${queue.size} 个任务", color = TextMuted, fontSize = 12.sp) })
            queue.forEach { task -> QueueItem(task) }
        }
    }
}

@Composable
private fun QueueItem(task: DownloadTask) {
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PlaceholderThumb(Icons.Filled.DataObject, modifier = Modifier.size(40.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(task.name, fontWeight = FontWeight.SemiBold, color = TextDark, fontSize = 13.sp)
                if (task.percent != null) {
                    LinearProgressIndicator(
                        progress = { task.percent / 100f },
                        modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                        color = PrimaryIndigo,
                        trackColor = ChipBg,
                    )
                }
                Text(task.detail, color = TextMuted, fontSize = 11.sp)
            }
            when (task.action) {
                "pause" -> IconButton(onClick = {}) { Icon(Icons.Filled.Pause, contentDescription = null, tint = PrimaryIndigo) }
                "start" -> SmallAction(Icons.Filled.PlayArrow, "开始")
                "install" -> SmallAction(Icons.Filled.Download, "安装")
            }
            if (task.percent != null) {
                IconButton(onClick = {}) { Icon(Icons.Filled.Close, contentDescription = null, tint = TextMuted, modifier = Modifier.size(16.dp)) }
            }
        }
        if (task.percent == null) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                if (task.status == "下载完成") {
                    Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = SuccessGreen, modifier = Modifier.size(14.dp))
                    Text(task.status, color = SuccessGreen, fontSize = 12.sp)
                } else {
                    Text("◷ ${task.status}", color = TextMuted, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun SmallAction(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String) {
    OutlinedButton(onClick = {}, shape = RoundedCornerShape(10.dp)) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(14.dp), tint = PrimaryIndigo)
        Spacer(modifier = Modifier.width(4.dp))
        Text(label, fontSize = 12.sp, color = PrimaryIndigo)
    }
}

private fun iconForCategory(badge: String) = when (badge) {
    "光影" -> Icons.Filled.Image
    "热门", "推荐" -> Icons.Filled.Extension
    else -> Icons.Filled.DataObject
}
