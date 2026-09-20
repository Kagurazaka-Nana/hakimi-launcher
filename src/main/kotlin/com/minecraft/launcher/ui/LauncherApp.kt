package com.minecraft.launcher.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.composeunstyled.EscapeHandler
import com.minecraft.launcher.backend.ResourceKind
import com.minecraft.launcher.ui.components.ResourceDetailDialog
import com.minecraft.launcher.ui.state.LaunchpadGroup
import com.minecraft.launcher.ui.state.LauncherPage
import com.minecraft.launcher.ui.state.LauncherViewModel
import com.minecraft.launcher.ui.theme.HakimiTheme
import kotlinx.coroutines.delay

@Composable
fun LauncherApp(vm: LauncherViewModel) {
    val state by vm.state.collectAsState()

    HakimiTheme(darkTheme = state.darkTheme) {
        Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
            // 1) 底层：当前标签内容（启动台打开时模糊）
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 56.dp, bottom = 8.dp)
                    .blur(if (state.showLaunchpad) 24.dp else 0.dp)
            ) {
                NavHost(vm = vm)
            }

            // 2) 顶部悬浮标签栏（仅显示已打开标签，可关闭）
            TopTabBar(
                openTabs = state.openTabs,
                selected = state.page,
                onSelect = { vm.selectTab(it) },
                onClose = { vm.closeTab(it) },
                onAdd = { vm.toggleLaunchpad() },
                modifier = Modifier.align(Alignment.TopCenter).padding(top = 12.dp),
            )

            // 3) 右上角主题切换
            ThemeToggle(
                dark = state.darkTheme,
                onToggle = { vm.toggleTheme() },
                modifier = Modifier.align(Alignment.TopEnd).padding(12.dp),
            )

            // 4) 左下角圆形启动台按钮
            LaunchpadButton(
                onClick = { vm.toggleLaunchpad() },
                modifier = Modifier.align(Alignment.BottomStart).padding(24.dp),
            )

            // 5) 启动台覆盖层
            AnimatedVisibility(
                visible = state.showLaunchpad,
                enter = fadeIn(tween(180)) + scaleIn(tween(220), initialScale = 0.96f),
                exit = fadeOut(tween(140)) + scaleOut(tween(160), targetScale = 0.96f),
            ) {
                LaunchpadOverlay(
                    vm = vm,
                    filter = state.launchpadFilter,
                    query = state.launchpadQuery,
                    tabs = vm.visibleLaunchpadTabs(),
                )
            }

            // 详情弹窗
            state.detail?.let { item ->
                val kind = state.page.resourceKind()
                if (kind != null) {
                    ResourceDetailDialog(
                        item = item,
                        icon = kind.icon(),
                        color = kind.color(),
                        onToggle = { vm.toggleResource(kind, item.id) },
                        onClose = { vm.closeDetail() },
                    )
                }
            }

            // 消息条
            state.message?.let { msg ->
                MessageBar(text = msg)
                LaunchedEffect(msg) {
                    delay(2200)
                    vm.consumeMessage()
                }
            }
        }
    }
}

@Composable
private fun NavHost(vm: LauncherViewModel) {
    val state by vm.state.collectAsState()
    when (state.page) {
        LauncherPage.HOME -> HomeScreen(vm)
        LauncherPage.CREATE -> CreateInstanceScreen(vm)
        LauncherPage.MODS -> ResourceListScreen(vm, ResourceKind.MODS)
        LauncherPage.RESOURCE_PACK -> ResourceListScreen(vm, ResourceKind.RESOURCE_PACK)
        LauncherPage.DATA_PACK -> ResourceListScreen(vm, ResourceKind.DATA_PACK)
        LauncherPage.SHADER -> ResourceListScreen(vm, ResourceKind.SHADER)
        LauncherPage.MODPACK -> ResourceListScreen(vm, ResourceKind.MODPACK)
        LauncherPage.PLUGIN -> ResourceListScreen(vm, ResourceKind.PLUGIN)
        LauncherPage.SERVER -> ResourceListScreen(vm, ResourceKind.SERVER)
        LauncherPage.WIKI -> WikiScreen(vm)
        LauncherPage.SCREENSHOTS -> ScreenshotsScreen(vm)
        LauncherPage.SKIN -> SkinSelectorScreen(vm)
        LauncherPage.SETTINGS -> SettingsScreen(vm)
    }
}

@Composable
private fun TopTabBar(
    openTabs: List<LauncherPage>,
    selected: LauncherPage,
    onSelect: (LauncherPage) -> Unit,
    onClose: (LauncherPage) -> Unit,
    onAdd: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyRow(
        modifier = modifier
            .clip(RoundedCornerShape(999.dp))
            .background(Color.Black.copy(alpha = 0.32f))
            .padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        items(openTabs, key = { it.name }) { page ->
            val active = page == selected
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(if (active) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.12f))
                    .clickable { onSelect(page) }
                    .padding(start = 14.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(page.icon, contentDescription = page.label, tint = if (active) MaterialTheme.colorScheme.onPrimary else Color.White, modifier = Modifier.size(16.dp))
                Text(page.label, color = if (active) MaterialTheme.colorScheme.onPrimary else Color.White, fontSize = 13.sp, fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal)
                Box(
                    modifier = Modifier
                        .size(18.dp)
                        .clip(CircleShape)
                        .clickable { onClose(page) },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(HakimiIcons.Close, contentDescription = "关闭标签", tint = if (active) MaterialTheme.colorScheme.onPrimary else Color.White, modifier = Modifier.size(12.dp))
                }
            }
        }
        item(key = "add-tab") {
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.12f))
                    .clickable(onClick = onAdd),
                contentAlignment = Alignment.Center,
            ) {
                Icon(HakimiIcons.Create, contentDescription = "新建标签（打开启动台）", tint = Color.White, modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
private fun ThemeToggle(dark: Boolean, onToggle: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = 0.32f)),
        contentAlignment = Alignment.Center,
    ) {
        IconButton(onClick = onToggle) {
            Icon(if (dark) HakimiIcons.LightMode else HakimiIcons.DarkMode, contentDescription = "切换主题", tint = Color.White)
        }
    }
}

@Composable
private fun LaunchpadButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(56.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primary)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(Icons.Filled.GridView, contentDescription = "启动台", tint = MaterialTheme.colorScheme.onPrimary)
    }
}

@Composable
private fun LaunchpadOverlay(vm: LauncherViewModel, filter: LaunchpadGroup, query: String, tabs: List<LauncherPage>) {
    // Compose Unstyled：可访问的 Esc 关闭处理
    EscapeHandler(callback = { vm.closeLaunchpad() })
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.55f))
            .clickable { vm.closeLaunchpad() },
    ) {
        // 内部容器吞掉点击，避免误关闭
        Column(
            modifier = Modifier
                .fillMaxSize()
                .clickable(enabled = false) {}
                .padding(top = 72.dp, bottom = 24.dp, start = 24.dp, end = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = { vm.setLaunchpadQuery(it) },
                modifier = Modifier.width(360.dp),
                placeholder = { Text("搜索页面…", color = Color.White.copy(alpha = 0.6f)) },
                leadingIcon = { Icon(HakimiIcons.Search, contentDescription = null, tint = Color.White) },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                LaunchpadGroup.entries.forEach { group ->
                    val active = group == filter
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(if (active) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.15f))
                            .clickable { vm.setLaunchpadFilter(group) }
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                    ) {
                        Text(group.label, color = if (active) MaterialTheme.colorScheme.onPrimary else Color.White, fontSize = 13.sp)
                    }
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 112.dp),
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(20.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
                contentPadding = PaddingValues(24.dp),
            ) {
                items(tabs, key = { it.name }) { page ->
                    LaunchpadItem(page = page, onClick = { vm.openTab(page) })
                }
            }
        }
    }
}

@Composable
private fun LaunchpadItem(page: LauncherPage, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.85f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(page.icon, contentDescription = page.label, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(30.dp))
        }
        Text(page.label, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun MessageBar(text: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
        Box(
            modifier = Modifier
                .padding(bottom = 96.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(MaterialTheme.colorScheme.inverseSurface.copy(alpha = 0.92f))
                .padding(horizontal = 18.dp, vertical = 10.dp),
        ) {
            Text(text, color = MaterialTheme.colorScheme.inverseOnSurface, fontSize = 13.sp)
        }
    }
}

/** 页面 → 资源种类（列表页才有）。 */
private fun LauncherPage.resourceKind(): ResourceKind? = when (this) {
    LauncherPage.MODS -> ResourceKind.MODS
    LauncherPage.RESOURCE_PACK -> ResourceKind.RESOURCE_PACK
    LauncherPage.DATA_PACK -> ResourceKind.DATA_PACK
    LauncherPage.SHADER -> ResourceKind.SHADER
    LauncherPage.MODPACK -> ResourceKind.MODPACK
    LauncherPage.PLUGIN -> ResourceKind.PLUGIN
    LauncherPage.SERVER -> ResourceKind.SERVER
    else -> null
}

/** 资源种类 → 图标。 */
fun ResourceKind.icon() = when (this) {
    ResourceKind.MODS -> HakimiIcons.Mods
    ResourceKind.RESOURCE_PACK -> HakimiIcons.ResourcePack
    ResourceKind.DATA_PACK -> HakimiIcons.DataPack
    ResourceKind.SHADER -> HakimiIcons.Shader
    ResourceKind.MODPACK -> HakimiIcons.Modpack
    ResourceKind.PLUGIN -> HakimiIcons.Plugin
    ResourceKind.SERVER -> HakimiIcons.Server
}

/** 资源种类 → 主题色。 */
fun ResourceKind.color() = when (this) {
    ResourceKind.MODS -> Color(0xFF6C5CE7)
    ResourceKind.RESOURCE_PACK -> Color(0xFFE86AA6)
    ResourceKind.DATA_PACK -> Color(0xFF3FB6A8)
    ResourceKind.SHADER -> Color(0xFFF5A623)
    ResourceKind.MODPACK -> Color(0xFF9B6FE0)
    ResourceKind.PLUGIN -> Color(0xFF5B8DEF)
    ResourceKind.SERVER -> Color(0xFF3FA34D)
}

internal fun iconFor(key: String) = when (key) {
    "cat" -> HakimiIcons.Person
    "folder" -> HakimiIcons.ResourcePack
    "puzzle" -> HakimiIcons.Mods
    "download" -> HakimiIcons.Download
    "cube" -> HakimiIcons.Modpack
    "image" -> HakimiIcons.Screenshot
    "add" -> HakimiIcons.Create
    "home" -> HakimiIcons.Home
    "coffee" -> HakimiIcons.Create
    "person" -> HakimiIcons.Person
    else -> HakimiIcons.Settings
}
