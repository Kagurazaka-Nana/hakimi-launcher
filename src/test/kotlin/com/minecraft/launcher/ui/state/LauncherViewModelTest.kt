package com.minecraft.launcher.ui.state

import com.minecraft.launcher.backend.StubLauncherBackend
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class LauncherViewModelTest {

    private fun vm() = LauncherViewModel(StubLauncherBackend())

    @Test
    fun `toggleLaunchpad flips visibility`() {
        val vm = vm()
        assertFalse(vm.state.value.showLaunchpad)
        vm.toggleLaunchpad()
        assertTrue(vm.state.value.showLaunchpad)
        vm.toggleLaunchpad()
        assertFalse(vm.state.value.showLaunchpad)
        vm.closeLaunchpad()
        assertFalse(vm.state.value.showLaunchpad)
    }

    @Test
    fun `openTab switches page and closes launchpad and resets filters`() {
        val vm = vm()
        vm.toggleLaunchpad()
        vm.setQuery("x")
        vm.openTab(LauncherPage.MODS)
        val s = vm.state.value
        assertEquals(LauncherPage.MODS, s.page)
        assertFalse(s.showLaunchpad)
        assertEquals("", s.query)
        assertEquals("全部", s.category)
    }

    @Test
    fun `launchpad filter and query narrow visible tabs`() {
        val vm = vm()
        // 默认全部可见
        assertEquals(LauncherPage.entries.size, vm.visibleLaunchpadTabs().size)

        vm.setLaunchpadFilter(LaunchpadGroup.RESOURCE)
        val resourceTabs = vm.visibleLaunchpadTabs()
        assertTrue(resourceTabs.isNotEmpty())
        assertTrue(resourceTabs.all { it.group == LaunchpadGroup.RESOURCE })

        vm.setLaunchpadFilter(LaunchpadGroup.ALL)
        vm.setLaunchpadQuery("皮肤")
        assertEquals(listOf(LauncherPage.SKIN), vm.visibleLaunchpadTabs())
    }

    @Test
    fun `toggleResource flips enabled state`() {
        val vm = vm()
        vm.start()
        // 等待后台加载完成（start 在 Default 调度器异步执行）
        val deadline = System.currentTimeMillis() + 2000
        while (vm.state.value.resources.isEmpty() && System.currentTimeMillis() < deadline) {
            Thread.sleep(20)
        }
        val mods = vm.state.value.resources[com.minecraft.launcher.backend.ResourceKind.MODS].orEmpty()
        assertTrue(mods.isNotEmpty())
        val target = mods.first()
        vm.toggleResource(com.minecraft.launcher.backend.ResourceKind.MODS, target.id)
        val updated = vm.state.value.resources[com.minecraft.launcher.backend.ResourceKind.MODS]!!
            .first { it.id == target.id }
        assertEquals(!target.enabled, updated.enabled)
        vm.close()
    }
}
