package com.minecraft.launcher.ui

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.unit.dp
import com.minecraft.launcher.ui.components.Wardrobe3D
import com.minecraft.launcher.ui.state.LauncherViewModel
import com.minecraft.launcher.ui.theme.HakimiButton
import com.minecraft.launcher.ui.theme.HakimiCard
import com.minecraft.launcher.ui.theme.HakimiChip
import com.minecraft.launcher.ui.theme.HakimiIcon
import com.minecraft.launcher.ui.theme.HakimiSearchField
import com.minecraft.launcher.ui.theme.HakimiText
import com.minecraft.launcher.ui.theme.HakimiTheme

/**
 * 首页：整块内容区为一张卡片（待重新设计）。
 * 临时测试区：安装按钮 + 正版/离线登录选择 + 长方形衣柜皮肤预览（docs/Authentication.md §2.3）。
 */

private const val TEST_INSTALL_VERSION = "1.21.1"

@Composable
fun HomeScreen(vm: LauncherViewModel) {
    val state by vm.state.collectAsState()
    var username by remember { mutableStateOf("hakimi") }
    var yggServer by remember { mutableStateOf("https://littleskin.cn/api/yggdrasil") }
    var yggUser by remember { mutableStateOf("") }
    var yggPass by remember { mutableStateOf("") }
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
                    // 第三方 Yggdrasil 认证（密码框为明文临时区，正式设计时替换）
                    HakimiSearchField(
                        value = yggServer,
                        onValueChange = { yggServer = it },
                        placeholder = "第三方服务器 URL（如 https://littleskin.cn/api/yggdrasil）",
                        icon = HakimiIcons.Server,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        HakimiSearchField(yggUser, { yggUser = it }, "用户名", HakimiIcons.Person, Modifier.weight(1f))
                        HakimiSearchField(yggPass, { yggPass = it }, "密码", HakimiIcons.Skin, Modifier.weight(1f))
                        HakimiButton(
                            text = "第三方登录",
                            icon = HakimiIcons.Download,
                            onClick = { vm.loginYggdrasil(yggServer, yggUser, yggPass) },
                        )
                    }
                    if (state.yggProfiles.size > 1) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            HakimiText("角色", style = HakimiTheme.type.caption, color = HakimiTheme.colors.textMuted)
                            state.yggProfiles.forEach { p ->
                                HakimiChip(
                                    p.name,
                                    color = HakimiTheme.colors.primary,
                                    selected = p.id == state.yggSelectedId,
                                    onClick = { vm.selectYggdrasilProfile(p.id) },
                                )
                            }
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
                    Wardrobe3D(state.skinPng, state.skinSlim, modifier = Modifier.size(160.dp, 320.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        HakimiChip(if (state.skinSlim) "Slim 模型" else "Classic 模型", color = HakimiTheme.colors.accent, selected = state.skinSlim)
                        HakimiText("衣柜·3D 模型（拖拽旋转）", style = HakimiTheme.type.caption, color = HakimiTheme.colors.textMuted)
                    }
                }
            }
        }
    }
}
