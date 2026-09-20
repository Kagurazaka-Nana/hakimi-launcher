package com.minecraft.launcher.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import com.minecraft.launcher.ui.components.PageHeader
import com.minecraft.launcher.ui.components.SoftCard
import com.minecraft.launcher.ui.state.LauncherViewModel

@Composable
fun CreateInstanceScreen(vm: LauncherViewModel) {
    val state by vm.state.collectAsState()
    var name by remember { mutableStateOf("") }
    var version by remember { mutableStateOf(state.versions.firstOrNull()?.id ?: "") }
    var loader by remember { mutableStateOf(state.loaders.firstOrNull()?.id ?: "") }
    var iconIndex by remember { mutableStateOf(0) }
    val iconColors = listOf(
        Color(0xFF6C5CE7), Color(0xFF3FB6A8), Color(0xFFE86AA6),
        Color(0xFFF5A623), Color(0xFF5B8DEF), Color(0xFF9B6FE0),
    )

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        PageHeader(icon = HakimiIcons.Create, title = "创建实例", subtitle = "选择版本与加载器，开始你的新冒险！")

        SoftCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("实例名称", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("例如：我的生存世界", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                    shape = RoundedCornerShape(14.dp),
                    singleLine = true,
                )

                PickerField(
                    label = "游戏版本",
                    value = version,
                    options = state.versions.map { it.id to it.type },
                ) { version = it }

                PickerField(
                    label = "加载器",
                    value = state.loaders.firstOrNull { it.id == loader }?.label ?: loader,
                    options = state.loaders.map { it.id to it.label },
                ) { loader = it }

                Text("实例图标", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    iconColors.forEachIndexed { index, color ->
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(color.copy(alpha = if (index == iconIndex) 1f else 0.35f))
                                .clickable { iconIndex = index },
                            contentAlignment = Alignment.Center,
                        ) {
                            if (index == iconIndex) Icon(HakimiIcons.Check, contentDescription = null, tint = Color.White)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
                Button(
                    onClick = { vm.createInstance(name.ifBlank { "未命名实例" }, version, loader) },
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                ) {
                    Icon(HakimiIcons.Create, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("创建实例", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun PickerField(label: String, value: String, options: List<Pair<String, String>>, onSelect: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
        Box {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .clickable { expanded = true }
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(value.ifBlank { "请选择" }, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
                Icon(HakimiIcons.Sort, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                options.forEach { (id, desc) ->
                    DropdownMenuItem(text = { Text("$desc") }, onClick = { onSelect(id); expanded = false })
                }
            }
        }
    }
}
