package com.minecraft.launcher.backend

/**
 * 后端能力接口：UI 只依赖这里定义的操作，
 * 后续再接入真实的下载 / 登录 / 启动实现，不在 UI 中直接写业务细节。
 */
interface LauncherBackend {

    /** 当前账号显示名，暂无登录系统时返回占位值 */
    suspend fun getCurrentUsername(): String

    /** 已知游戏版本列表，供版本选择 UI 展示 */
    suspend fun loadVersions(): List<String>

    /** 首页快照：账号、当前实例、模组、系统状态、最近活动等展示数据 */
    suspend fun loadHome(): HomeSnapshot

    /** 刷新 Mojang version manifest */
    suspend fun refreshManifest()

    /** 确保指定版本已下载完整并可启动 */
    suspend fun ensureVersionReady(versionId: String)

    /** 启动指定版本的游戏 */
    suspend fun launch(versionId: String)
}
