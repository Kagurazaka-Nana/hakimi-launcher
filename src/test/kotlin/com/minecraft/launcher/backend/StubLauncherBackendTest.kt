package com.minecraft.launcher.backend

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * 校验 StubLauncherBackend 的首页快照契约：
 * 前端框架阶段保证返回稳定、非空、字段完整的演示数据。
 */
class StubLauncherBackendTest {

    @Test
    fun `loadHome returns a fully populated snapshot`() = runBlocking {
        val snapshot = StubLauncherBackend().loadHome()

        assertTrue(snapshot.welcomeTitle.isNotBlank())
        assertTrue(snapshot.welcomeSubtitle.isNotBlank())
        assertEquals("HakimiCat", snapshot.profileName)
        assertTrue(snapshot.profileOnline)
        assertEquals("生存世界", snapshot.instanceName)
        assertEquals("1.21.1", snapshot.version)
        assertEquals("Fabric", snapshot.loader)
        assertTrue(snapshot.modeTags.isNotEmpty())
        assertEquals(4, snapshot.quickActions.size)
        assertTrue(snapshot.loadingPercent in 0..100)
        assertTrue(snapshot.recentPlay.name.isNotBlank())
        assertEquals(4, snapshot.resourceStatus.size)
        assertTrue(snapshot.resourceStatus.all { it.ready })
    }

    @Test
    fun `other stub operations stay side-effect free`() = runBlocking {
        val backend = StubLauncherBackend()
        assertEquals("HakimiCat", backend.getCurrentUsername())
        assertTrue(backend.loadVersions().isNotEmpty())
        backend.refreshManifest()
        backend.ensureVersionReady("1.21.1")
        backend.launch("1.21.1")
    }
}
