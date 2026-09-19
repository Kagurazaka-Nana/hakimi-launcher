package com.minecraft.launcher.backend

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
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

        assertNotNull(snapshot)
        assertTrue(snapshot.username.isNotBlank())
        assertEquals("杉木谷", snapshot.worldName)
        assertTrue(snapshot.worldDescription.isNotBlank())
        assertTrue(snapshot.ready)
        assertEquals("1.21.1", snapshot.launchVersion)
        assertTrue(snapshot.modTags.isNotEmpty())
        assertTrue(snapshot.modCount > 0)
        assertTrue(snapshot.memoryTotalGb > snapshot.memoryUsedGb)
        assertTrue(snapshot.javaOk)
        assertTrue(snapshot.loaderCompatible)
        assertTrue(snapshot.recentActivities.isNotEmpty())
    }

    @Test
    fun `other stub operations stay side-effect free`() = runBlocking {
        val backend = StubLauncherBackend()
        assertEquals("Steve", backend.getCurrentUsername())
        assertEquals(listOf("latest-release"), backend.loadVersions())
        backend.refreshManifest()
        backend.ensureVersionReady("1.21.1")
        backend.launch("1.21.1")
    }
}
