package com.minecraft.launcher.ui

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.minecraft.launcher.backend.LauncherBackend
import com.minecraft.launcher.backend.ResourceKind
import com.minecraft.launcher.ui.components.ResourceDetailDialog
import com.minecraft.launcher.ui.state.LauncherPage
import com.minecraft.launcher.ui.state.LauncherViewModel
import com.minecraft.launcher.ui.theme.HakimiTheme
import kotlinx.coroutines.delay

@Composable
fun LauncherApp(backend: LauncherBackend) {
    val vm = remember { LauncherViewModel(backend) }
    DisposableEffect(Unit) {
        vm.start()
        onDispose { vm.close() }
    }
    val state by vm.state.collectAsState()

    HakimiTheme(darkTheme = state.darkTheme) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column(modifier = Modifier.fillMaxSize()) {
                TopBar(vm = vm, title = state.page.label)
                Row(modifier = Modifier.weight(1f)) {
                    Sidebar(vm = vm)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .padding(24.dp)
                    ) {
                        NavHost(vm = vm)
                    }
                }
            }
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
    }
    // 详情弹窗覆盖在内容之上
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
}

@Composable
private fun TopBar(vm: LauncherViewModel, title: String) {
    val state by vm.state.collectAsState()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = { vm.toggleSidebar() }) {
            Icon(HakimiIcons.Menu, contentDescription = "切换导航", tint = MaterialTheme.colorScheme.onSurface)
        }
        Icon(HakimiIcons.Home, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.width(8.dp))
        Text("hakimi-launcher", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, fontSize = 16.sp)
        Text("  ·  $title", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
        Spacer(modifier = Modifier.weight(1f))
        IconButton(onClick = { vm.toggleTheme() }) {
            Icon(
                if (state.darkTheme) HakimiIcons.LightMode else HakimiIcons.DarkMode,
                contentDescription = "切换主题",
                tint = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
private fun Sidebar(vm: LauncherViewModel) {
    val state by vm.state.collectAsState()
    val expanded = state.sidebarExpanded
    val width by animateDpAsState(if (expanded) 210.dp else 72.dp, tween(220), label = "sidebarWidth")

    Column(
        modifier = Modifier
            .width(width)
            .fillMaxHeight()
            .background(MaterialTheme.colorScheme.surface)
            .verticalScroll(rememberScrollState())
            .padding(vertical = 12.dp, horizontal = 10.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        LauncherPage.entries.forEach { page ->
            val active = page == state.page
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface.copy(alpha = 0f))
                    .clickable { vm.selectPage(page) }
                    .padding(horizontal = 14.dp, vertical = 11.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = if (expanded) Arrangement.spacedBy(12.dp) else Arrangement.Center,
            ) {
                Icon(page.icon, contentDescription = page.label, tint = if (active) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant)
                if (expanded) {
                    Text(page.label, color = if (active) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface, fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal)
                }
            }
        }
    }
}

@Composable
private fun MessageBar(text: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
        Box(
            modifier = Modifier
                .padding(bottom = 28.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(MaterialTheme.colorScheme.inverseSurface.copy(alpha = 0.92f))
                .padding(horizontal = 18.dp, vertical = 10.dp)
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
    ResourceKind.MODS -> androidx.compose.ui.graphics.Color(0xFF6C5CE7)
    ResourceKind.RESOURCE_PACK -> androidx.compose.ui.graphics.Color(0xFFE86AA6)
    ResourceKind.DATA_PACK -> androidx.compose.ui.graphics.Color(0xFF3FB6A8)
    ResourceKind.SHADER -> androidx.compose.ui.graphics.Color(0xFFF5A623)
    ResourceKind.MODPACK -> androidx.compose.ui.graphics.Color(0xFF9B6FE0)
    ResourceKind.PLUGIN -> androidx.compose.ui.graphics.Color(0xFF5B8DEF)
    ResourceKind.SERVER -> androidx.compose.ui.graphics.Color(0xFF3FA34D)
}
