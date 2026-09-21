package com.minecraft.launcher.backend

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * 校验 StubLauncherBackend 的快照契约：
 * 前端框架阶段保证返回稳定、非空、字段完整的演示数据。
 */
class StubLauncherBackendTest {

    private val backend = StubLauncherBackend()

    @Test
    fun `loadHome returns populated snapshot`() = runBlocking {
        val home = backend.loadHome()
        assertTrue(home.welcomeTitle.isNotBlank())
        assertEquals("HakimiCat", home.profileName)
        assertEquals("生存世界", home.instanceName)
        assertEquals("1.21.1", home.version)
        assertTrue(home.resourceCount > 0)
    }

    @Test
    fun `loadSystemStats returns sane ranges`() = runBlocking {
        val stats = backend.loadSystemStats()
        assertTrue(stats.cpuPercent in 0..100)
        assertTrue(stats.memUsedGb <= stats.memTotalGb)
        assertTrue(stats.vramUsedMb <= stats.vramTotalMb)
        assertTrue(stats.networkOnline)
    }

    @Test
    fun `loadResources returns items for every kind`() = runBlocking {
        ResourceKind.entries.forEach { kind ->
            val items = backend.loadResources(kind)
            assertTrue(items.isNotEmpty(), "$kind should not be empty")
            assertTrue(items.all { it.name.isNotBlank() && it.version.isNotBlank() })
        }
    }

    @Test
    fun `loaders versions skins servers screenshots wiki populated`() = runBlocking {
        assertTrue(backend.loadVersions().isNotEmpty())
        assertTrue(backend.loadLoaders().isNotEmpty())
        assertTrue(backend.loadSkins().any { it.selected })
        assertTrue(backend.loadServers().isNotEmpty())
        assertTrue(backend.loadScreenshots().isNotEmpty())
        assertNotNull(backend.loadWiki().firstOrNull())
    }

    @Test
    fun `side-effect operations complete`() = runBlocking {
        backend.createInstance("测试", "1.21.1", "fabric")
        backend.launch("测试")
    }
}
