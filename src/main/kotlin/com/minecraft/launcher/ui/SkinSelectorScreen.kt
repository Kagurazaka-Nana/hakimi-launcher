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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.minecraft.launcher.ui.components.PageHeader
import com.minecraft.launcher.ui.theme.HakimiButton
import com.minecraft.launcher.ui.theme.HakimiCard
import com.minecraft.launcher.ui.theme.HakimiIcon
import com.minecraft.launcher.ui.theme.HakimiText
import com.minecraft.launcher.ui.theme.HakimiTheme
import com.minecraft.launcher.ui.state.LauncherViewModel

@Composable
fun SkinSelectorScreen(vm: LauncherViewModel) {
    val state by vm.state.collectAsState()
    val c = HakimiTheme.colors
    val selected = state.skins.firstOrNull { it.selected } ?: state.skins.firstOrNull()

    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        PageHeader(icon = HakimiIcons.Skin, title = "皮肤选择", subtitle = "预览并选择你的角色皮肤，或上传新皮肤")

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(18.dp)) {
            HakimiCard(modifier = Modifier.width(260.dp)) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Box(
                        modifier = Modifier.size(width = 160.dp, height = 220.dp).clip(HakimiTheme.shapes.large).background(c.primarySoft),
                        contentAlignment = Alignment.Center,
                    ) {
                        HakimiIcon(HakimiIcons.Person, null, c.primary, size = 96.dp)
                    }
                    HakimiText(selected?.name ?: "—", style = HakimiTheme.type.title)
                    HakimiText("当前预览", style = HakimiTheme.type.caption, color = c.textMuted)
                }
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                HakimiText("可用皮肤", style = HakimiTheme.type.title)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(state.skins, key = { it.id }) { skin ->
                        val active = skin.selected
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(
                                modifier = Modifier
                                    .size(96.dp)
                                    .clip(HakimiTheme.shapes.large)
                                    .background(if (active) c.primarySoft else c.surfaceMuted)
                                    .clickable { vm.selectSkin(skin.id) },
                                contentAlignment = Alignment.Center,
                            ) {
                                HakimiIcon(HakimiIcons.Person, null, if (active) c.primary else c.textMuted, size = 48.dp)
                            }
                            HakimiText(skin.name, style = HakimiTheme.type.caption)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    HakimiButton(text = "上传皮肤", icon = HakimiIcons.Upload, onClick = { /* 上传：待接入后端 */ })
                    HakimiButton(text = "刷新皮肤库", onClick = { /* 刷新：待接入后端 */ }, filled = false)
                }
                HakimiText("上传功能为前端占位，真实皮肤上传后续接入后端。", style = HakimiTheme.type.caption, color = c.textMuted)
            }
        }
    }
}
