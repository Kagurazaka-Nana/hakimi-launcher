package com.minecraft.launcher.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.runDesktopComposeUiTest
import androidx.compose.ui.unit.dp
import com.minecraft.launcher.backend.StubLauncherBackend
import com.minecraft.launcher.ui.state.LauncherViewModel
import io.github.takahirom.roborazzi.captureRoboImage
import org.junit.Test

/**
 * Roborazzi 桌面截图测试：用 Compose Desktop 测试框架离屏渲染，产出可见 PNG 到 build/roborazzi。
 * 记录：./gradlew test -Proborazzi.test.record=true 或 ./gradlew recordRoborazziJvm
 * 校验：./gradlew verifyRoborazziJvm
 *
 * 主题由 LauncherApp 内部依据 UiState.darkTheme 决定，因此暗色截图通过 vm.toggleTheme() 控制。
 */
@OptIn(ExperimentalTestApi::class)
class ScreenshotTest {

    private fun newVm(): LauncherViewModel = LauncherViewModel(StubLauncherBackend()).also { it.start() }

    @Test
    fun homeLight() = runDesktopComposeUiTest {
        val vm = newVm()
        // 先等真实指标流就绪再组合，保证首帧截图带数据（避免后台线程更新与测试帧时钟的竞态）
        waitUntil(timeoutMillis = 10_000L) { vm.state.value.home != null && vm.state.value.systemStats != null }
        setContent {
            Box(modifier = Modifier.size(1200.dp, 760.dp)) { LauncherApp(vm) }
        }
        waitForIdle()
        captureToImage().captureRoboImage("build/roborazzi/home-light.png")
        vm.close()
    }

    @Test
    fun homeOfflineLoginShowsDefaultSteve() = runDesktopComposeUiTest {
        val vm = newVm()
        waitUntil(timeoutMillis = 10_000L) { vm.state.value.home != null && vm.state.value.systemStats != null }
        vm.loginOffline("hakimi")
        // 离线账户 sessionserver 查不到皮肤 → 默认 Steve 兜底
        waitUntil(timeoutMillis = 10_000L) { vm.state.value.skinPng != null }
        setContent {
            Box(modifier = Modifier.size(1200.dp, 760.dp)) { LauncherApp(vm) }
        }
        waitForIdle()
        captureToImage().captureRoboImage("build/roborazzi/home-steve.png")
        vm.close()
    }

    @Test
    fun homeDark() = runDesktopComposeUiTest {
        val vm = newVm()
        waitUntil(timeoutMillis = 10_000L) { vm.state.value.home != null && vm.state.value.systemStats != null }
        setContent {
            Box(modifier = Modifier.size(1200.dp, 760.dp)) { LauncherApp(vm) }
        }
        vm.toggleTheme()
        waitForIdle()
        captureToImage().captureRoboImage("build/roborazzi/home-dark.png")
        vm.close()
    }

    @Test
    fun launchpadOpen() = runDesktopComposeUiTest {
        val vm = newVm()
        waitUntil(timeoutMillis = 10_000L) { vm.state.value.home != null && vm.state.value.systemStats != null }
        setContent {
            Box(modifier = Modifier.size(1200.dp, 760.dp)) { LauncherApp(vm) }
        }
        vm.toggleLaunchpad()
        waitForIdle()
        captureToImage().captureRoboImage("build/roborazzi/launchpad-open.png")
        vm.close()
    }
}
