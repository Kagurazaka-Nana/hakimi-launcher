package com.minecraft.launcher.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DataObject
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import com.minecraft.launcher.backend.InstanceItem
import com.minecraft.launcher.ui.components.AvatarCircle
import com.minecraft.launcher.ui.components.PageHeader
import com.minecraft.launcher.ui.components.Pill
import com.minecraft.launcher.ui.components.PlaceholderThumb
import com.minecraft.launcher.ui.components.SoftCard
import com.minecraft.launcher.ui.theme.ChipBg
import com.minecraft.launcher.ui.theme.ChipText
import com.minecraft.launcher.ui.theme.OnPrimary
import com.minecraft.launcher.ui.theme.PrimaryIndigo
import com.minecraft.launcher.ui.theme.SuccessBg
import com.minecraft.launcher.ui.theme.SuccessGreen
import com.minecraft.launcher.ui.theme.TextDark
import com.minecraft.launcher.ui.theme.TextMuted

@Composable
fun InstancesScreen(instances: List<InstanceItem>) {
    var query by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        PageHeader(
            icon = Icons.Filled.Layers,
            title = "实例管理",
            subtitle = "管理你的 Minecraft 实例，开启不同的冒险旅程！",
            right = {
                Button(
                    onClick = {},
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo, contentColor = OnPrimary)
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("新建实例")
                }
            }
        )

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("搜索实例名称、版本号或关键词…", color = TextMuted) },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, tint = TextMuted) },
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
            )
            FilterChip("全部版本")
            FilterChip("全部加载器")
            FilterChip("全部状态")
        }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            items(instances) { instance -> InstanceRow(instance) }
        }

        Text("🐾 共 ${instances.size} 个实例 · ${instances.count { it.running }} 个正在运行", color = TextMuted, fontSize = 12.sp)
    }
}

@Composable
private fun FilterChip(text: String) {
    OutlinedButton(onClick = {}, shape = RoundedCornerShape(12.dp)) {
        Text(text, color = TextDark, fontSize = 13.sp)
    }
}

@Composable
private fun InstanceRow(instance: InstanceItem) {
    SoftCard(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
            PlaceholderThumb(Icons.Filled.Home, modifier = Modifier.size(84.dp))
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(instance.name, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextDark)
                    Icon(Icons.Filled.Edit, contentDescription = null, tint = TextMuted, modifier = Modifier.size(15.dp))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MetaPill(Icons.Filled.DataObject, instance.version)
                    MetaPill(Icons.Filled.Extension, instance.loader)
                    Pill(instance.modeTag, bg = SuccessBg, fg = SuccessGreen)
                    Pill(instance.playerTag)
                }
                Text(instance.description, color = TextMuted, fontSize = 12.sp)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (instance.running) {
                        Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = SuccessGreen, modifier = Modifier.size(14.dp))
                        Text("运行中", color = SuccessGreen, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    } else {
                        Icon(Icons.Filled.RadioButtonUnchecked, contentDescription = null, tint = TextMuted, modifier = Modifier.size(14.dp))
                        Text("未启动", color = TextMuted, fontSize = 12.sp)
                    }
                    Text("· 上次启动：${instance.lastPlayed}", color = TextMuted, fontSize = 12.sp)
                }
            }
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    AvatarCircle(Icons.Filled.Person, PrimaryIndigo, size = 26.dp)
                    Text(instance.owner, color = TextMuted, fontSize = 12.sp)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Button(
                        onClick = {},
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo, contentColor = OnPrimary)
                    ) {
                        Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("进入游戏", fontSize = 13.sp)
                    }
                    IconButton(onClick = {}) { Icon(Icons.Filled.Settings, contentDescription = null, tint = TextMuted) }
                    IconButton(onClick = {}) { Icon(Icons.Filled.ContentCopy, contentDescription = null, tint = TextMuted) }
                    IconButton(onClick = {}) { Icon(Icons.Filled.MoreVert, contentDescription = null, tint = TextMuted) }
                }
            }
        }
    }
}

@Composable
private fun MetaPill(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
    Row(
        modifier = Modifier.clip(RoundedCornerShape(999.dp)).background(ChipBg).padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(icon, contentDescription = null, tint = ChipText, modifier = Modifier.size(13.dp))
        Text(text, color = ChipText, fontSize = 12.sp)
    }
}
