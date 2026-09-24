package com.minecraft.launcher.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.runDesktopComposeUiTest
import androidx.compose.ui.unit.dp
import com.minecraft.launcher.ui.theme.HakimiText
import com.minecraft.launcher.ui.theme.HakimiTheme
import org.junit.Test

/**
 * 测量辅助：量出速率文本在 caption 样式（13sp Monospace）下的真实像素宽度，
 * 用于校准 StatusBar.kt 中 RateItem 速率文本的固定宽度（当前 52dp = "↑999.9M" 的 51px + 1px 余量）。
 * 若字体/字号调整后需要重新校准，运行：./gradlew test --tests "*StatusBarMeasure*"
 * 并查看 build/test-results 下对应 XML 的 system-out 输出。
 */
@OptIn(ExperimentalTestApi::class)
class StatusBarMeasureTest {

    @Test
    fun measureRateTextWidth() = runDesktopComposeUiTest {
        var maxPx = 0
        setContent {
            HakimiTheme(darkTheme = false) {
                Box(Modifier.size(300.dp, 120.dp)) {
                    Column {
                        listOf("↑999.9M", "↓999.9M", "↑123.4G", "↑10K", "↓120K", "↑-", "↓-").forEach { s ->
                            HakimiText(
                                s,
                                style = HakimiTheme.type.caption,
                                modifier = Modifier.onSizeChanged {
                                    println("MEASURE '$s' = ${it.width}px")
                                    if (it.width > maxPx) maxPx = it.width
                                },
                            )
                        }
                    }
                }
            }
        }
        waitForIdle()
        println("MEASURE MAX = $maxPx px")
    }
}
