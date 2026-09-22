package com.minecraft.launcher.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.runDesktopComposeUiTest
import androidx.compose.ui.unit.dp
import com.minecraft.launcher.backend.SystemStats
import com.minecraft.launcher.ui.components.StatusBar
import com.minecraft.launcher.ui.theme.HakimiTheme
import io.github.takahirom.roborazzi.captureRoboImage
import org.junit.Test

/**
 * 状态栏显存项的条件渲染验证：
 * vramUsedMb/vramTotalMb 有值时显示"显存占用"仪表，null 时隐藏（本机 Glenfly 显卡即此情形）。
 * 记录：./gradlew test -Proborazzi.test.record=true
 */
@OptIn(ExperimentalTestApi::class)
class StatusBarTest {

    private fun renderAndCapture(name: String, stats: SystemStats) = runDesktopComposeUiTest {
        setContent {
            HakimiTheme(darkTheme = false) {
                Box(modifier = Modifier.size(460.dp, 64.dp).background(HakimiTheme.colors.background)) {
                    StatusBar(stats, modifier = Modifier.align(Alignment.Center))
                }
            }
        }
        waitForIdle()
        captureToImage().captureRoboImage("build/roborazzi/$name.png")
    }

    @Test
    fun `vram gauge shown when available`() = renderAndCapture(
        "statusbar-vram-shown",
        SystemStats(
            cpuPercent = 42, memUsedGb = 8.0, memTotalGb = 16.0,
            vramUsedMb = 1800, vramTotalMb = 8192,
            netDownBps = 1_500_000, netUpBps = 29_000,
            diskReadBps = 0, diskWriteBps = 858_000,
        ),
    )

    @Test
    fun `vram gauge hidden when unavailable`() = renderAndCapture(
        "statusbar-vram-hidden",
        SystemStats(
            cpuPercent = 42, memUsedGb = 8.0, memTotalGb = 16.0,
            vramUsedMb = null, vramTotalMb = null,
            netDownBps = 1_500_000, netUpBps = 29_000,
            diskReadBps = 0, diskWriteBps = 858_000,
        ),
    )
}
