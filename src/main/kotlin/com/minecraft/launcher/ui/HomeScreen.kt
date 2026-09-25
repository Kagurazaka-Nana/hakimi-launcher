package com.minecraft.launcher.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.minecraft.launcher.ui.components.HoverTip
import com.minecraft.launcher.ui.state.LauncherViewModel
import com.minecraft.launcher.ui.theme.HakimiButton
import com.minecraft.launcher.ui.theme.HakimiCard
import com.minecraft.launcher.ui.theme.HakimiChip
import com.minecraft.launcher.ui.theme.HakimiIcon
import com.minecraft.launcher.ui.theme.HakimiSearchField
import com.minecraft.launcher.ui.theme.HakimiText
import com.minecraft.launcher.ui.theme.HakimiTheme
import com.minecraft.launcher.ui.theme.PixelShape
import kotlin.math.roundToInt

/**
 * 首页：整块内容区为一张卡片（待重新设计）。
 * 临时测试区：安装按钮 + 正版/离线登录选择 + 长方形衣柜皮肤预览（docs/Authentication.md §2.3）。
 */

private const val TEST_INSTALL_VERSION = "1.21.1"

@Composable
fun HomeScreen(vm: LauncherViewModel) {
    val state by vm.state.collectAsState()
    var username by remember { mutableStateOf("hakimi") }
    val installing = state.downloads.any {
        it.name == "安装 $TEST_INSTALL_VERSION" &&
            (it.state == com.minecraft.launcher.download.DownloadState.CONNECTING ||
                it.state == com.minecraft.launcher.download.DownloadState.DOWNLOADING)
    }
    HakimiCard(modifier = Modifier.fillMaxSize(), contentPadding = 24.dp) {
        Column(
            modifier = Modifier.fillMaxSize().padding(top = 8.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                HakimiIcon(HakimiIcons.Person, null, HakimiTheme.colors.primary, size = 18.dp)
                HakimiText("账户测试区（临时）", style = HakimiTheme.type.title)
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(24.dp),
                verticalAlignment = Alignment.Top,
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    HakimiSearchField(
                        value = username,
                        onValueChange = { username = it },
                        placeholder = "离线用户名",
                        icon = HakimiIcons.Person,
                        modifier = Modifier.width(280.dp),
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        HakimiButton(text = "离线登录", icon = HakimiIcons.Person, onClick = { vm.loginOffline(username) })
                        HakimiButton(text = "正版登录", icon = HakimiIcons.Check, onClick = { vm.beginMicrosoftLogin() })
                        if (state.accountName != null) {
                            HakimiButton(text = "登出", icon = HakimiIcons.Close, onClick = { vm.logout() })
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        HakimiButton(
                            text = if (installing) "安装中…" else "测试安装 $TEST_INSTALL_VERSION",
                            icon = HakimiIcons.Download,
                            onClick = {
                                if (!installing) {
                                    vm.startInstall(TEST_INSTALL_VERSION)
                                    vm.notifyMessage("开始安装 $TEST_INSTALL_VERSION（launcherTest/.minecraft）")
                                }
                            },
                        )
                    }
                    when {
                        state.loginError != null -> HakimiText(
                            "✗ ${state.loginError}",
                            style = HakimiTheme.type.label,
                            color = HakimiTheme.colors.error,
                        )
                        state.deviceCode != null -> {
                            val dc = state.deviceCode!!
                            HakimiText("到 ${dc.verificationUri} 输入代码：", style = HakimiTheme.type.label, color = HakimiTheme.colors.textMuted)
                            HakimiText(dc.userCode, style = HakimiTheme.type.display, color = HakimiTheme.colors.primary)
                        }
                        state.accountName != null -> HakimiText(
                            "已登录：${state.accountName}（${state.accountType}）",
                            style = HakimiTheme.type.label,
                            color = HakimiTheme.colors.success,
                        )
                        else -> HakimiText("未登录", style = HakimiTheme.type.label, color = HakimiTheme.colors.textMuted)
                    }
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Wardrobe(state.skinPng)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        HakimiChip(if (state.skinSlim) "Slim 模型" else "Classic 模型", color = HakimiTheme.colors.accent, selected = state.skinSlim)
                        HakimiText("衣柜·皮肤正面", style = HakimiTheme.type.caption, color = HakimiTheme.colors.textMuted)
                    }
                }
            }
        }
    }
}

/**
 * 衣柜：长方形面板，按 8-bit 皮肤布局合成正面视图。
 * 纹理坐标 → 16×32 网格（头8 / 身12 / 腿12 高，臂宽4），见 docs/Authentication.md §2.3 表。
 */
private val SkinFrontRegions = listOf(
    // srcX, srcY, srcW, srcH, gridX, gridY, gridW, gridH
    intArrayOf(8, 8, 8, 8, 4, 0, 8, 8),      // 头正面
    intArrayOf(20, 20, 8, 12, 4, 8, 8, 12),  // 身体正面
    intArrayOf(44, 20, 4, 12, 0, 8, 4, 12),  // 右臂正面
    intArrayOf(40, 52, 4, 12, 12, 8, 4, 12), // 左臂正面（64x64 第二排）
    intArrayOf(4, 20, 4, 12, 4, 20, 4, 12),  // 右腿正面
    intArrayOf(20, 52, 4, 12, 8, 20, 4, 12), // 左腿正面（64x64 第二排）
)

@Composable
private fun Wardrobe(png: ByteArray?, modifier: Modifier = Modifier) {
    val c = HakimiTheme.colors
    val bitmap = remember(png) { png?.let { org.jetbrains.skia.Image.makeFromEncoded(it).toComposeImageBitmap() } }
    HoverTip(label = if (bitmap == null) "衣柜（暂无皮肤）" else "衣柜：当前账户皮肤正面") {
        Box(
            modifier = modifier
                .size(160.dp, 320.dp)
                .clip(PixelShape(10.dp))
                .background(c.surfaceMuted)
                .border(2.dp, c.ink, PixelShape(10.dp))
                .drawBehind {
                    val img = bitmap ?: return@drawBehind
                    val cell = size.width / 16f
                    for (r in SkinFrontRegions) {
                        drawImage(
                            image = img,
                            dstOffset = IntOffset((r[4] * cell).roundToInt(), (r[5] * cell).roundToInt()),
                            srcOffset = IntOffset(r[0], r[1]),
                            srcSize = IntSize(r[2], r[3]),
                            dstSize = IntSize((r[6] * cell).roundToInt(), (r[7] * cell).roundToInt()),
                        )
                    }
                },
            contentAlignment = Alignment.Center,
        ) {
            if (bitmap == null) {
                HakimiText(
                    "衣柜\n\n暂无皮肤\n（登录后自动加载）",
                    style = HakimiTheme.type.caption,
                    color = c.textMuted,
                    align = TextAlign.Center,
                )
            }
        }
    }
}
