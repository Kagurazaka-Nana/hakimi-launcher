package com.minecraft.launcher.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.minecraft.launcher.ui.state.LauncherViewModel
import com.minecraft.launcher.ui.theme.HakimiCard

/**
 * 首页：整个主内容区为一张卡片，内容清空待重新设计。
 * vm 保留在签名中，新设计接入交互时直接使用。
 */
@Composable
@Suppress("UNUSED_PARAMETER")
fun HomeScreen(vm: LauncherViewModel) {
    HakimiCard(modifier = Modifier.fillMaxSize()) {}
}
