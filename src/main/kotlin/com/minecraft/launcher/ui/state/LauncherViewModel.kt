package com.minecraft.launcher.ui.state

import androidx.compose.ui.graphics.vector.ImageVector
import com.minecraft.launcher.backend.GameVersion
import com.minecraft.launcher.backend.HomeSnapshot
import com.minecraft.launcher.backend.LauncherBackend
import com.minecraft.launcher.backend.LoaderOption
import com.minecraft.launcher.backend.ResourceItem
import com.minecraft.launcher.backend.ResourceKind
import com.minecraft.launcher.backend.ScreenshotInfo
import com.minecraft.launcher.backend.ServerInfo
import com.minecraft.launcher.backend.SettingsSnapshot
import com.minecraft.launcher.backend.SkinInfo
import com.minecraft.launcher.backend.SystemStats
import com.minecraft.launcher.backend.WikiArticle
import com.minecraft.launcher.ui.HakimiIcons
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** 启动台分组（过滤维度）。 */
enum class LaunchpadGroup(val label: String) {
    ALL("全部"),
    RESOURCE("资源"),
    TOOL("工具"),
}

/** 导航页面枚举。 */
enum class LauncherPage(val label: String, val icon: ImageVector, val group: LaunchpadGroup) {
    HOME("首页", HakimiIcons.Home, LaunchpadGroup.TOOL),
    CREATE("创建实例", HakimiIcons.Create, LaunchpadGroup.TOOL),
    MODS("Mods", HakimiIcons.Mods, LaunchpadGroup.RESOURCE),
    RESOURCE_PACK("资源包", HakimiIcons.ResourcePack, LaunchpadGroup.RESOURCE),
    DATA_PACK("数据包", HakimiIcons.DataPack, LaunchpadGroup.RESOURCE),
    SHADER("光影", HakimiIcons.Shader, LaunchpadGroup.RESOURCE),
    MODPACK("整合包", HakimiIcons.Modpack, LaunchpadGroup.RESOURCE),
    PLUGIN("插件", HakimiIcons.Plugin, LaunchpadGroup.RESOURCE),
    SERVER("服务器", HakimiIcons.Server, LaunchpadGroup.RESOURCE),
    WIKI("Wiki", HakimiIcons.Wiki, LaunchpadGroup.TOOL),
    SCREENSHOTS("截图", HakimiIcons.Screenshot, LaunchpadGroup.TOOL),
    SKIN("皮肤选择", HakimiIcons.Skin, LaunchpadGroup.TOOL),
    SETTINGS("设置", HakimiIcons.Settings, LaunchpadGroup.TOOL),
}

/** 列表排序方式。 */
enum class SortMode(val label: String) {
    RECOMMENDED("按推荐"),
    DOWNLOADS("按下载量"),
    NAME("按名称"),
}

/** 全局 UI 状态快照。 */
data class UiState(
    val page: LauncherPage = LauncherPage.HOME,
    val openTabs: List<LauncherPage> = listOf(LauncherPage.HOME),
    val darkTheme: Boolean = false,
    val showLaunchpad: Boolean = false,
    val launchpadQuery: String = "",
    val launchpadFilter: LaunchpadGroup = LaunchpadGroup.ALL,
    val home: HomeSnapshot? = null,
    val systemStats: SystemStats? = null,
    val resources: Map<ResourceKind, List<ResourceItem>> = emptyMap(),
    val query: String = "",
    val category: String = "全部",
    val sort: SortMode = SortMode.RECOMMENDED,
    val detail: ResourceItem? = null,
    val skins: List<SkinInfo> = emptyList(),
    val servers: List<ServerInfo> = emptyList(),
    val screenshots: List<ScreenshotInfo> = emptyList(),
    val wiki: List<WikiArticle> = emptyList(),
    val settings: SettingsSnapshot? = null,
    val versions: List<GameVersion> = emptyList(),
    val loaders: List<LoaderOption> = emptyList(),
    val message: String? = null,
)

/**
 * 轻量状态容器：以 [StateFlow] 暴露单一 [UiState]，
 * 所有交互通过方法更新状态，页面订阅 [state] 即可，保证状态流转集中、可预测。
 */
class LauncherViewModel(private val backend: LauncherBackend) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    fun start() {
        scope.launch {
            val home = backend.loadHome()
            val resources = ResourceKind.entries.associateWith { backend.loadResources(it) }
            val versions = backend.loadVersions()
            val loaders = backend.loadLoaders()
            val skins = backend.loadSkins()
            val servers = backend.loadServers()
            val screenshots = backend.loadScreenshots()
            val wiki = backend.loadWiki()
            val settings = backend.loadSettings()
            _state.update {
                it.copy(
                    home = home,
                    resources = resources,
                    versions = versions,
                    loaders = loaders,
                    skins = skins,
                    servers = servers,
                    screenshots = screenshots,
                    wiki = wiki,
                    settings = settings,
                )
            }
        }
        // 系统状态栏：每秒采样的真实指标流
        scope.launch {
            backend.systemStatsFlow().collect { stats ->
                _state.update { it.copy(systemStats = stats) }
            }
        }
    }

    fun close() = scope.cancel()

    // —— 标签 / 启动台 ——

    /** 从启动台打开（或聚焦）一个标签页。 */
    fun openTab(page: LauncherPage) = _state.update {
        val tabs = if (it.openTabs.contains(page)) it.openTabs else it.openTabs + page
        it.copy(page = page, openTabs = tabs, showLaunchpad = false, query = "", category = "全部", detail = null)
    }

    /** 点击已打开标签切换选中。 */
    fun selectTab(page: LauncherPage) = _state.update {
        if (it.openTabs.contains(page)) it.copy(page = page) else it
    }

    /** 关闭标签（保留至少一个）。 */
    fun closeTab(page: LauncherPage) = _state.update { s ->
        if (s.openTabs.size <= 1) s
        else {
            val tabs = s.openTabs - page
            val selected = if (s.page == page) (tabs.firstOrNull() ?: LauncherPage.HOME) else s.page
            s.copy(openTabs = tabs, page = selected)
        }
    }

    fun toggleLaunchpad() = _state.update { it.copy(showLaunchpad = !it.showLaunchpad) }

    fun closeLaunchpad() = _state.update { it.copy(showLaunchpad = false) }

    fun setLaunchpadQuery(q: String) = _state.update { it.copy(launchpadQuery = q) }

    fun setLaunchpadFilter(group: LaunchpadGroup) = _state.update { it.copy(launchpadFilter = group) }

    /** 启动台里按分组与关键词过滤后的页面。 */
    fun visibleLaunchpadTabs(): List<LauncherPage> {
        val s = _state.value
        return LauncherPage.entries.filter { page ->
            (s.launchpadFilter == LaunchpadGroup.ALL || page.group == s.launchpadFilter) &&
                (s.launchpadQuery.isBlank() || page.label.contains(s.launchpadQuery, ignoreCase = true))
        }
    }

    // —— 主题 ——

    fun toggleTheme() = _state.update { it.copy(darkTheme = !it.darkTheme) }

    // —— 列表页 ——

    fun setQuery(q: String) = _state.update { it.copy(query = q) }

    fun setCategory(c: String) = _state.update { it.copy(category = c) }

    fun setSort(s: SortMode) = _state.update { it.copy(sort = s) }

    fun openDetail(item: ResourceItem) = _state.update { it.copy(detail = item) }

    fun closeDetail() = _state.update { it.copy(detail = null) }

    fun toggleResource(kind: ResourceKind, id: String) = _state.update { state ->
        val list = state.resources[kind].orEmpty().map { if (it.id == id) it.copy(enabled = !it.enabled) else it }
        state.copy(resources = state.resources + (kind to list))
    }

    fun selectSkin(id: String) = _state.update {
        it.copy(skins = it.skins.map { s -> s.copy(selected = s.id == id) })
    }

    fun createInstance(name: String, version: String, loader: String) {
        scope.launch {
            backend.createInstance(name, version, loader)
            _state.update {
                val tabs = if (it.openTabs.contains(LauncherPage.HOME)) it.openTabs else it.openTabs + LauncherPage.HOME
                it.copy(page = LauncherPage.HOME, openTabs = tabs, message = "已创建实例：$name")
            }
        }
    }

    fun launch() {
        scope.launch {
            backend.launch(_state.value.home?.instanceName ?: "latest")
            _state.update { it.copy(message = "正在启动 ${_state.value.home?.instanceName ?: ""}…") }
        }
    }

    fun consumeMessage() = _state.update { it.copy(message = null) }

    /** 当前页面对应分类下、经搜索/筛选/排序后的可见资源。 */
    fun visibleResources(kind: ResourceKind): List<ResourceItem> {
        val s = _state.value
        val base = s.resources[kind].orEmpty()
        val filtered = base.filter { item ->
            (s.category == "全部" || item.categories.contains(s.category)) &&
                (s.query.isBlank() || item.name.contains(s.query, true) || item.summary.contains(s.query, true))
        }
        return when (s.sort) {
            SortMode.RECOMMENDED -> filtered
            SortMode.DOWNLOADS -> filtered.sortedByDescending { it.downloads }
            SortMode.NAME -> filtered.sortedBy { it.name }
        }
    }
}
