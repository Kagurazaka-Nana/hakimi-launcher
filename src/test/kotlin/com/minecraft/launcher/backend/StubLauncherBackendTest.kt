package com.minecraft.launcher.backend

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
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
    fun `systemStatsFlow emits sane real snapshot`() = runBlocking {
        val stats = backend.systemStatsFlow().first()
        assertTrue(stats.cpuPercent in 0..100)
        assertTrue(stats.memUsedGb <= stats.memTotalGb)
        assertTrue(stats.memTotalGb > 0)
        assertTrue(stats.netDownBps >= 0 && stats.netUpBps >= 0)
        assertTrue(stats.diskReadBps >= 0 && stats.diskWriteBps >= 0)
        // 显存允许探测不到（null），有值时必须自洽
        if (stats.vramUsedMb != null && stats.vramTotalMb != null) {
            assertTrue(stats.vramUsedMb <= stats.vramTotalMb)
        }
    }

    @Test
    fun `downloadTasksFlow is empty in stub`() = runBlocking {
        assertTrue(backend.downloadTasksFlow().toList().isEmpty())
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
