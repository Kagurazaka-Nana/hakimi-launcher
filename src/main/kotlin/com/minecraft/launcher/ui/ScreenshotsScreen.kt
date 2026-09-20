package com.minecraft.launcher.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.minecraft.launcher.ui.components.PageHeader
import com.minecraft.launcher.ui.theme.HakimiCard
import com.minecraft.launcher.ui.theme.HakimiIcon
import com.minecraft.launcher.ui.theme.HakimiText
import com.minecraft.launcher.ui.theme.HakimiTheme
import com.minecraft.launcher.ui.state.LauncherViewModel

@Composable
fun ScreenshotsScreen(vm: LauncherViewModel) {
    val state by vm.state.collectAsState()
    val c = HakimiTheme.colors

    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        PageHeader(icon = HakimiIcons.Screenshot, title = "截图", subtitle = "共 ${state.screenshots.size} 张截图")
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 200.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            items(state.screenshots, key = { it.id }) { shot ->
                HakimiCard(modifier = Modifier.fillMaxWidth(), contentPadding = 0.dp) {
                    Box(
                        modifier = Modifier.fillMaxWidth().height(120.dp).clip(HakimiTheme.shapes.medium).background(c.primarySoft),
                        contentAlignment = Alignment.Center,
                    ) {
                        HakimiIcon(HakimiIcons.Screenshot, null, c.primary, size = 32.dp)
                    }
                    Column(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        HakimiText(shot.name, style = HakimiTheme.type.label, maxLines = 1)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            HakimiText(shot.time, style = HakimiTheme.type.caption, color = c.textMuted, modifier = Modifier.weight(1f))
                            HakimiText(shot.size, style = HakimiTheme.type.caption, color = c.textMuted)
                        }
                    }
                }
            }
        }
    }
}
