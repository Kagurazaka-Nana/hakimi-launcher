package com.minecraft.launcher.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.minecraft.launcher.ui.components.PageHeader
import com.minecraft.launcher.ui.theme.HakimiCard
import com.minecraft.launcher.ui.theme.HakimiChip
import com.minecraft.launcher.ui.theme.HakimiText
import com.minecraft.launcher.ui.theme.HakimiTheme
import com.minecraft.launcher.ui.state.LauncherViewModel

@Composable
fun WikiScreen(vm: LauncherViewModel) {
    val state by vm.state.collectAsState()
    val c = HakimiTheme.colors

    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        PageHeader(icon = HakimiIcons.Wiki, title = "Wiki", subtitle = "启动器使用指南与常见问题")
        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(state.wiki, key = { it.id }) { article ->
                HakimiCard(modifier = Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                HakimiText(article.title, style = HakimiTheme.type.title)
                                HakimiChip(article.category, color = c.accent)
                            }
                            HakimiText(article.excerpt, style = HakimiTheme.type.body, color = c.textMuted)
                        }
                        HakimiText(article.updated, style = HakimiTheme.type.caption, color = c.textMuted, modifier = Modifier.width(64.dp))
                    }
                }
            }
        }
    }
}
