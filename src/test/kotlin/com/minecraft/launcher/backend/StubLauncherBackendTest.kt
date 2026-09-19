package com.minecraft.launcher.backend

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * 校验 StubLauncherBackend 的快照契约：
 * 前端框架阶段保证返回稳定、非空、字段完整的演示数据。
 */
class StubLauncherBackendTest {

    @Test
    fun `loadHome returns a fully populated snapshot`() = runBlocking {
        val snapshot = StubLauncherBackend().loadHome()

        assertTrue(snapshot.welcomeTitle.isNotBlank())
        assertEquals("HakimiCat", snapshot.profileName)
        assertEquals("生存世界", snapshot.instanceName)
        assertEquals("1.21.1", snapshot.version)
        assertEquals(4, snapshot.quickActions.size)
        assertTrue(snapshot.loadingPercent in 0..100)
        assertEquals(4, snapshot.resourceStatus.size)
        assertTrue(snapshot.resourceStatus.all { it.ready })
    }

    @Test
    fun `loadInstances returns populated instance list`() = runBlocking {
        val instances = StubLauncherBackend().loadInstances()
        assertEquals(3, instances.size)
        assertTrue(instances.any { it.running })
        assertTrue(instances.all { it.name.isNotBlank() && it.version.isNotBlank() })
    }

    @Test
    fun `loadDownloads returns categories items and queue`() = runBlocking {
        val downloads = StubLauncherBackend().loadDownloads()
        assertEquals(5, downloads.categories.size)
        assertTrue(downloads.items.isNotEmpty())
        assertEquals(4, downloads.queue.size)
        assertTrue(downloads.queue.any { it.percent != null })
    }

    @Test
    fun `loadSettings returns populated settings`() = runBlocking {
        val settings = StubLauncherBackend().loadSettings()
        assertTrue(settings.javaPath.isNotBlank())
        assertTrue(settings.maxMemoryMb in settings.memoryMinMb..settings.memoryMaxMb)
        assertTrue(settings.concurrency in settings.concurrencyMin..settings.concurrencyMax)
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
