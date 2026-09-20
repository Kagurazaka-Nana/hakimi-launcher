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

/** 导航页面枚举。 */
enum class LauncherPage(val label: String, val icon: ImageVector) {
    HOME("首页", HakimiIcons.Home),
    CREATE("创建实例", HakimiIcons.Create),
    MODS("Mods", HakimiIcons.Mods),
    RESOURCE_PACK("资源包", HakimiIcons.ResourcePack),
    DATA_PACK("数据包", HakimiIcons.DataPack),
    SHADER("光影", HakimiIcons.Shader),
    MODPACK("整合包", HakimiIcons.Modpack),
    PLUGIN("插件", HakimiIcons.Plugin),
    SERVER("服务器", HakimiIcons.Server),
    WIKI("Wiki", HakimiIcons.Wiki),
    SCREENSHOTS("截图", HakimiIcons.Screenshot),
    SKIN("皮肤选择", HakimiIcons.Skin),
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
    val darkTheme: Boolean = false,
    val sidebarExpanded: Boolean = true,
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
            val stats = backend.loadSystemStats()
            val resources = ResourceKind.entries.associateWith { backend.loadResources(it) }
            val versions = backend.loadVersions()
            val loaders = backend.loadLoaders()
            val skins = backend.loadSkins()
            val servers = backend.loadServers()
            val screenshots = backend.loadScreenshots()
            val wiki = backend.loadWiki()
            _state.update {
                it.copy(
                    home = home,
                    systemStats = stats,
                    resources = resources,
                    versions = versions,
                    loaders = loaders,
                    skins = skins,
                    servers = servers,
                    screenshots = screenshots,
                    wiki = wiki,
                )
            }
        }
    }

    fun close() = scope.cancel()

    fun selectPage(page: LauncherPage) = _state.update {
        it.copy(page = page, query = "", category = "全部", detail = null)
    }

    fun toggleTheme() = _state.update { it.copy(darkTheme = !it.darkTheme) }

    fun toggleSidebar() = _state.update { it.copy(sidebarExpanded = !it.sidebarExpanded) }

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
            _state.update { it.copy(page = LauncherPage.HOME, message = "已创建实例：$name") }
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
