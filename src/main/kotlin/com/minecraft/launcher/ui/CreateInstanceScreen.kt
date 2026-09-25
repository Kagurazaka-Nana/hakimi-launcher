package com.minecraft.launcher.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.minecraft.launcher.ui.components.PageHeader
import com.minecraft.launcher.ui.theme.HakimiButton
import com.minecraft.launcher.ui.theme.HakimiCard
import com.minecraft.launcher.ui.theme.HakimiChip
import com.minecraft.launcher.ui.theme.HakimiSearchField
import com.minecraft.launcher.ui.theme.HakimiText
import com.minecraft.launcher.ui.theme.HakimiTheme
import com.minecraft.launcher.ui.state.LauncherViewModel

@Composable
fun CreateInstanceScreen(vm: LauncherViewModel) {
    val state by vm.state.collectAsState()
    var name by remember { mutableStateOf("") }
    var version by remember { mutableStateOf(state.versions.firstOrNull()?.id ?: "") }
    var loader by remember { mutableStateOf(state.loaders.firstOrNull()?.id ?: "") }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        PageHeader(icon = HakimiIcons.Create, title = "创建实例", subtitle = "选择版本与加载器，开始你的新冒险！")

        HakimiCard(modifier = Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                HakimiText("实例名称", style = HakimiTheme.type.label)
                HakimiSearchField(
                    value = name,
                    onValueChange = { name = it },
                    placeholder = "例如：我的生存世界",
                    icon = HakimiIcons.Create,
                    modifier = Modifier.fillMaxWidth(),
                )

                HakimiText("游戏版本", style = HakimiTheme.type.label)
                ChipRow(options = state.versions.map { it.id to it.type }, selected = version) { version = it }

                HakimiText("加载器", style = HakimiTheme.type.label)
                ChipRow(options = state.loaders.map { it.id to it.label }, selected = loader) { loader = it }

                HakimiText("实例图标", style = HakimiTheme.type.label)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    listOf(0xFFEC6A9C, 0xFF3FB6A8, 0xFFE86AA6, 0xFFF5A623, 0xFF5B8DEF, 0xFF9B6FE0).forEachIndexed { _, argb ->
                        HakimiChip(text = "●", color = androidx.compose.ui.graphics.Color(0xFF000000 or argb.toLong()))
                    }
                }

                HakimiButton(
                    text = "创建实例",
                    icon = HakimiIcons.Create,
                    onClick = { vm.createInstance(name.ifBlank { "未命名实例" }, version, loader) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun ChipRow(options: List<Pair<String, String>>, selected: String, onSelect: (String) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(bottom = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        options.forEach { (id, label) ->
            HakimiChip(text = label, selected = id == selected, onClick = { onSelect(id) })
        }
    }
}
