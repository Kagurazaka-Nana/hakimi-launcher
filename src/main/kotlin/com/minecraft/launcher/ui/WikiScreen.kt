package com.minecraft.launcher.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.minecraft.launcher.ui.components.PageHeader
import com.minecraft.launcher.ui.components.Pill
import com.minecraft.launcher.ui.components.SoftCard
import com.minecraft.launcher.ui.state.LauncherViewModel
import com.minecraft.launcher.ui.theme.HakimiColors

@Composable
fun WikiScreen(vm: LauncherViewModel) {
    val state by vm.state.collectAsState()
    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        PageHeader(icon = HakimiIcons.Wiki, title = "Wiki", subtitle = "启动器使用指南与常见问题")
        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(state.wiki, key = { it.id }) { article ->
                SoftCard(modifier = Modifier.fillMaxWidth().clickable { }) {
                    Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(article.title, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, fontSize = 15.sp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Pill(article.category, HakimiColors.Info)
                            }
                            Text(article.excerpt, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                        }
                        Text(article.updated, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                    }
                }
            }
        }
    }
}
