package com.minecraft.launcher.backend

/**
 * 前端框架阶段的占位实现：不访问网络、不触碰文件系统，
 * 仅向 UI 提供稳定的演示数据。
 */
class StubLauncherBackend : LauncherBackend {

    override suspend fun getCurrentUsername(): String = "Steve"

    override suspend fun loadVersions(): List<String> = listOf("latest-release")

    override suspend fun refreshManifest() = Unit

    override suspend fun ensureVersionReady(versionId: String) = Unit

    override suspend fun launch(versionId: String) = Unit
}
