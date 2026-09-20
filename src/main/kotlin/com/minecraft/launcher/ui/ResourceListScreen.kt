package com.minecraft.launcher.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.minecraft.launcher.backend.ResourceKind
import com.minecraft.launcher.ui.components.PageHeader
import com.minecraft.launcher.ui.components.ResourceCard
import com.minecraft.launcher.ui.components.SearchFilterSortBar
import com.minecraft.launcher.ui.state.LauncherViewModel

@Composable
fun ResourceListScreen(vm: LauncherViewModel, kind: ResourceKind) {
    val state by vm.state.collectAsState()
    val items = vm.visibleResources(kind)

    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        PageHeader(icon = kind.icon(), title = kind.label, subtitle = "共 ${items.size} 项 · 支持搜索、分类筛选与排序")

        SearchFilterSortBar(
            query = state.query,
            onQuery = { vm.setQuery(it) },
            categories = kind.categories,
            category = state.category,
            onCategory = { vm.setCategory(it) },
            sort = state.sort,
            onSort = { vm.setSort(it) },
        )

        if (items.isEmpty()) {
            Text("没有匹配的资源", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(items, key = { it.id }) { item ->
                    ResourceCard(
                        item = item,
                        icon = kind.icon(),
                        color = kind.color(),
                        onToggle = { vm.toggleResource(kind, item.id) },
                        onOpen = { vm.openDetail(item) },
                    )
                }
            }
        }
    }
}
