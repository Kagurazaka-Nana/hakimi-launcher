package com.minecraft.launcher.ui.state

import com.minecraft.launcher.backend.ResourceKind
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
    fun `openTab adds a tab, focuses it and closes launchpad`() {
        val vm = vm()
        vm.toggleLaunchpad()
        vm.openTab(LauncherPage.MODS)
        val s = vm.state.value
        assertEquals(LauncherPage.MODS, s.page)
        assertTrue(s.openTabs.contains(LauncherPage.MODS))
        assertFalse(s.showLaunchpad)
        // 再次打开同一页面不重复添加
        vm.openTab(LauncherPage.MODS)
        assertEquals(1, vm.state.value.openTabs.count { it == LauncherPage.MODS })
    }

    @Test
    fun `selectTab switches among open tabs`() {
        val vm = vm()
        vm.openTab(LauncherPage.WIKI)
        vm.openTab(LauncherPage.SKIN)
        vm.selectTab(LauncherPage.WIKI)
        assertEquals(LauncherPage.WIKI, vm.state.value.page)
        // 选择未打开的标签无效
        vm.selectTab(LauncherPage.SERVER)
        assertEquals(LauncherPage.WIKI, vm.state.value.page)
    }

    @Test
    fun `closeTab removes tab and keeps at least one`() {
        val vm = vm()
        vm.openTab(LauncherPage.MODS)
        assertEquals(2, vm.state.value.openTabs.size)
        vm.closeTab(LauncherPage.MODS)
        assertEquals(1, vm.state.value.openTabs.size)
        assertEquals(LauncherPage.HOME, vm.state.value.page)
        // 关闭最后一个被忽略
        vm.closeTab(LauncherPage.HOME)
        assertEquals(1, vm.state.value.openTabs.size)
    }

    @Test
    fun `launchpad filter and query narrow visible tabs`() {
        val vm = vm()
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
        val deadline = System.currentTimeMillis() + 2000
        while (vm.state.value.resources.isEmpty() && System.currentTimeMillis() < deadline) {
            Thread.sleep(20)
        }
        val mods = vm.state.value.resources[ResourceKind.MODS].orEmpty()
        assertTrue(mods.isNotEmpty())
        val target = mods.first()
        vm.toggleResource(ResourceKind.MODS, target.id)
        val updated = vm.state.value.resources[ResourceKind.MODS]!!.first { it.id == target.id }
        assertEquals(!target.enabled, updated.enabled)
        vm.close()
    }
}
