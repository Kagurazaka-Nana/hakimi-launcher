package com.minecraft.launcher.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.minecraft.launcher.download.DownloadState
import com.minecraft.launcher.ui.state.LauncherViewModel
import com.minecraft.launcher.ui.theme.HakimiButton
import com.minecraft.launcher.ui.theme.HakimiCard
import com.minecraft.launcher.ui.theme.HakimiText
import com.minecraft.launcher.ui.theme.HakimiTheme

/**
 * 首页：整块内容区为一张卡片（待重新设计）。
 * 目前放安装测试按钮，验证 startInstall → InstallationService → 底部指示器 的完整管线。
 */

private const val TEST_INSTALL_VERSION = "1.21.1"

@Composable
fun HomeScreen(vm: LauncherViewModel) {
    val state by vm.state.collectAsState()
    val installing = state.downloads.any { it.name == "安装 $TEST_INSTALL_VERSION" && (it.state == DownloadState.CONNECTING || it.state == DownloadState.DOWNLOADING) }
    HakimiCard(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
            ) {
                HakimiButton(
                    text = if (installing) "安装中…（看底部进度）" else "测试安装 $TEST_INSTALL_VERSION",
                    icon = HakimiIcons.Download,
                    onClick = {
                        if (!installing) {
                            vm.startInstall(TEST_INSTALL_VERSION)
                            vm.notifyMessage("开始安装 $TEST_INSTALL_VERSION（launcherTest/.minecraft）")
                        }
                    },
                )
                HakimiText(
                    "全链路：清单 → 版本 JSON → JAR/libraries → assets → JRE",
                    style = HakimiTheme.type.caption,
                    color = HakimiTheme.colors.textMuted,
                )
            }
        }
    }
}
