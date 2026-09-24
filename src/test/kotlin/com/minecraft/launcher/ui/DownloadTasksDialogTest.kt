package com.minecraft.launcher.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.runDesktopComposeUiTest
import androidx.compose.ui.unit.dp
import com.minecraft.launcher.backend.DownloadTask
import com.minecraft.launcher.download.DownloadState
import com.minecraft.launcher.ui.components.DownloadTasksDialog
import com.minecraft.launcher.ui.theme.HakimiTheme
import io.github.takahirom.roborazzi.captureRoboImage
import org.junit.Test

/** 下载任务弹窗视觉验证：混合活跃与历史任务。 */
@OptIn(ExperimentalTestApi::class)
class DownloadTasksDialogTest {

    @Test
    fun `dialog lists active and history tasks`() = runDesktopComposeUiTest {
        val tasks = listOf(
            DownloadTask("t1", "version_manifest.json", "https://piston-meta.mojang.com/mc/game/version_manifest.json", 0.42f, DownloadState.DOWNLOADING),
            DownloadTask("t2", "fabric-loader.jar", "https://maven.fabricmc.net/fabric-loader.jar", 0.05f, DownloadState.CONNECTING),
            DownloadTask("t3", "sodium.jar", "https://cdn.modrinth.com/data/sodium.jar", 1f, DownloadState.COMPLETED),
            DownloadTask("t4", "old-partial.bin", "https://example.com/old-partial.bin", 0.3f, DownloadState.CANCELLED),
            DownloadTask("t5", "broken.bin", "https://example.com/broken.bin", 0.1f, DownloadState.FAILED),
            DownloadTask("t6", "安装 1.21.1", "mojang://version/1.21.1", 1f, DownloadState.COMPLETED, false),
        )
        setContent {
            HakimiTheme(darkTheme = false) {
                Box(modifier = Modifier.size(1200.dp, 760.dp)) {
                    DownloadTasksDialog(tasks = tasks, onPause = {}, onResume = {}, onRemove = {}, onDismiss = {})
                }
            }
        }
        waitForIdle()
        captureToImage().captureRoboImage("build/roborazzi/downloads-dialog.png")
    }
}
