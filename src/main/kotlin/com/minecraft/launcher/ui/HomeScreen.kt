package com.minecraft.launcher.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.minecraft.launcher.ui.state.LauncherViewModel
import com.minecraft.launcher.ui.theme.HakimiButton
import com.minecraft.launcher.ui.theme.HakimiCard
import java.nio.file.Path
import kotlin.io.path.name

/**
 * 首页：整块内容区为一张卡片（待重新设计）。
 * 目前放一个测试下载按钮，验证 UI 事件 → DownloadManager → BitDownloader → 底部指示器 的链路。
 */

private const val TEST_DOWNLOAD_URL = "https://piston-meta.mojang.com/mc/game/version_manifest.json"
private val TEST_DOWNLOAD_TARGET: Path = Path.of("temp", "download-test", "version_manifest.json")

@Composable
fun HomeScreen(vm: LauncherViewModel) {
    val state by vm.state.collectAsState()
    val active = state.downloads.any { it.name == TEST_DOWNLOAD_TARGET.name }
    HakimiCard(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            HakimiButton(
                text = if (active) "下载中…" else "测试下载",
                icon = HakimiIcons.Download,
                onClick = {
                    try {
                        vm.startDownload(TEST_DOWNLOAD_URL, TEST_DOWNLOAD_TARGET)
                        vm.notifyMessage("已加入下载队列，看底部指示器")
                    } catch (e: SecurityException) {
                        vm.notifyMessage("下载被拒绝: ${e.message}")
                    }
                },
            )
        }
    }
}
