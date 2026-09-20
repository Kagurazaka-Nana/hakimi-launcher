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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.composeunstyled.EscapeHandler
import com.minecraft.launcher.backend.ResourceKind
import com.minecraft.launcher.ui.components.ResourceDetailContent
import com.minecraft.launcher.ui.state.LaunchpadGroup
import com.minecraft.launcher.ui.state.LauncherPage
import com.minecraft.launcher.ui.state.LauncherViewModel
import com.minecraft.launcher.ui.theme.HakimiChip
import com.minecraft.launcher.ui.theme.HakimiIcon
import com.minecraft.launcher.ui.theme.HakimiIconButton
import com.minecraft.launcher.ui.theme.HakimiOverlay
import com.minecraft.launcher.ui.theme.HakimiSearchField
import com.minecraft.launcher.ui.theme.HakimiTab
import com.minecraft.launcher.ui.theme.HakimiText
import com.minecraft.launcher.ui.theme.HakimiTheme
import kotlinx.coroutines.delay

@Composable
fun LauncherApp(vm: LauncherViewModel) {
    val state by vm.state.collectAsState()

    HakimiTheme(darkTheme = state.darkTheme) {
        val c = HakimiTheme.colors
        Box(modifier = Modifier.fillMaxSize().background(c.background)) {
            // 1) 内容区（启动台打开时强模糊）
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 64.dp, bottom = 16.dp, start = 24.dp, end = 24.dp)
                    .blur(if (state.showLaunchpad) 46.dp else 0.dp)
            ) {
                NavHost(vm = vm)
            }

            // 2) 顶部标签栏 + 主题按钮：启动台打开时隐藏
            if (!state.showLaunchpad) {
                TopTabBar(
                    openTabs = state.openTabs,
                    selected = state.page,
                    onSelect = { vm.selectTab(it) },
                    onClose = { vm.closeTab(it) },
                    onAdd = { vm.toggleLaunchpad() },
                    modifier = Modifier.align(Alignment.TopCenter).padding(top = 12.dp),
                )
                HakimiIconButton(
                    icon = if (state.darkTheme) HakimiIcons.LightMode else HakimiIcons.DarkMode,
                    contentDescription = "切换主题",
                    onClick = { vm.toggleTheme() },
                    modifier = Modifier.align(Alignment.TopEnd).padding(12.dp).shadow(4.dp, CircleShape),
                    background = c.surface,
                    tint = c.text,
                )
            }

            // 3) 左下角圆形启动台按钮
            HakimiIconButton(
                icon = HakimiIcons.Launchpad,
                contentDescription = "启动台",
                onClick = { vm.toggleLaunchpad() },
                modifier = Modifier.align(Alignment.BottomStart).padding(24.dp).shadow(10.dp, CircleShape, ambientColor = c.primary, spotColor = c.primary),
                size = 56.dp,
                background = c.primary,
                tint = c.onPrimary,
            )

            // 4) 启动台覆盖层（亚克力磨砂，背景不可见）
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

            // 5) 详情弹层
            state.detail?.let { item ->
                val kind = state.page.resourceKind()
                if (kind != null) {
                    HakimiOverlay(onDismiss = { vm.closeDetail() }) {
                        ResourceDetailContent(
                            item = item,
                            icon = kind.icon(),
                            color = kind.color(),
                            onToggle = { vm.toggleResource(kind, item.id) },
                            onClose = { vm.closeDetail() },
                        )
                    }
                }
            }

            // 6) 消息条
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
    val c = HakimiTheme.colors
    Row(
        modifier = modifier
            .shadow(6.dp, HakimiTheme.shapes.pill)
            .clip(HakimiTheme.shapes.pill)
            .background(c.surface)
            .padding(horizontal = 6.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
            items(openTabs, key = { it.name }) { page ->
                HakimiTab(
                    label = page.label,
                    icon = page.icon,
                    selected = page == selected,
                    onClick = { onSelect(page) },
                    onClose = { onClose(page) },
                )
            }
        }
        HakimiIconButton(icon = HakimiIcons.Create, contentDescription = "新建标签", onClick = onAdd, size = 30.dp, tint = c.textMuted)
    }
}

@Composable
private fun LaunchpadOverlay(vm: LauncherViewModel, filter: LaunchpadGroup, query: String, tabs: List<LauncherPage>) {
    EscapeHandler(callback = { vm.closeLaunchpad() })
    val c = HakimiTheme.colors
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(c.scrim)
            .clickable { vm.closeLaunchpad() },
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(top = 80.dp, bottom = 32.dp, start = 32.dp, end = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            HakimiText("启动台", style = HakimiTheme.type.display)
            HakimiSearchField(
                value = query,
                onValueChange = { vm.setLaunchpadQuery(it) },
                placeholder = "搜索页面…",
                icon = HakimiIcons.Search,
                modifier = Modifier.width(380.dp),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                LaunchpadGroup.entries.forEach { group ->
                    HakimiChip(
                        text = group.label,
                        color = c.primary,
                        selected = group == filter,
                        onClick = { vm.setLaunchpadFilter(group) },
                    )
                }
            }
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 120.dp),
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(24.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp),
                contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp),
            ) {
                items(tabs, key = { it.name }) { page ->
                    LaunchpadItem(icon = page.icon, label = page.label, onClick = { vm.openTab(page) })
                }
            }
        }
    }
}

@Composable
private fun LaunchpadItem(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    val c = HakimiTheme.colors
    Column(
        modifier = Modifier.clip(HakimiTheme.shapes.large).clickable(onClick = onClick).padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            modifier = Modifier.size(72.dp).clip(HakimiTheme.shapes.large).background(c.primarySoft),
            contentAlignment = Alignment.Center,
        ) {
            HakimiIcon(icon, null, c.primary, size = 34.dp)
        }
        HakimiText(label, style = HakimiTheme.type.label)
    }
}

@Composable
private fun MessageBar(text: String) {
    val c = HakimiTheme.colors
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
        Box(
            modifier = Modifier
                .padding(bottom = 96.dp)
                .clip(HakimiTheme.shapes.pill)
                .background(c.text)
                .padding(horizontal = 18.dp, vertical = 10.dp),
        ) {
            HakimiText(text, style = HakimiTheme.type.label, color = c.background)
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

/** 资源种类 → 语义色。 */
@Composable
fun ResourceKind.color(): Color = when (this) {
    ResourceKind.MODS -> HakimiTheme.colors.primary
    ResourceKind.RESOURCE_PACK -> HakimiTheme.colors.accent
    ResourceKind.DATA_PACK -> HakimiTheme.colors.success
    ResourceKind.SHADER -> HakimiTheme.colors.warning
    ResourceKind.MODPACK -> HakimiTheme.colors.primary
    ResourceKind.PLUGIN -> HakimiTheme.colors.accent
    ResourceKind.SERVER -> HakimiTheme.colors.success
}
