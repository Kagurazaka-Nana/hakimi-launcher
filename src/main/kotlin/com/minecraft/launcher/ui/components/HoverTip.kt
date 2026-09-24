package com.minecraft.launcher.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import com.minecraft.launcher.ui.theme.HakimiText
import com.minecraft.launcher.ui.theme.HakimiTheme
import com.minecraft.launcher.ui.theme.PixelShape

/**
 * 悬浮提示：鼠标悬停在内容上时，在其下方弹出说明气泡（像素阶梯角）。
 * 自定义定位：气泡始终在锚点下方（留出间隙），避免气泡窗口覆盖鼠标位置
 * 导致 hover 反复进出（悬浮提示闪烁）；同时钳制在窗口范围内防止截断。
 */
@Composable
fun HoverTip(label: String, content: @Composable () -> Unit) {
    val c = HakimiTheme.colors
    val source = remember { MutableInteractionSource() }
    val hovered by source.collectIsHoveredAsState()
    val density = LocalDensity.current
    val tipPosition = remember(density) {
        object : PopupPositionProvider {
            override fun calculatePosition(
                anchorBounds: IntRect,
                windowSize: IntSize,
                layoutDirection: LayoutDirection,
                popupContentSize: IntSize,
            ): IntOffset {
                val gap = with(density) { 6.dp.roundToPx() }
                val x = anchorBounds.left + (anchorBounds.width - popupContentSize.width) / 2
                val y = anchorBounds.bottom + gap
                return IntOffset(
                    x.coerceIn(0, (windowSize.width - popupContentSize.width).coerceAtLeast(0)),
                    y.coerceAtMost((windowSize.height - popupContentSize.height).coerceAtLeast(0)),
                )
            }
        }
    }
    Box(modifier = Modifier.hoverable(source)) {
        content()
        if (hovered) {
            Popup(
                popupPositionProvider = tipPosition,
                properties = PopupProperties(focusable = false),
            ) {
                Box(
                    modifier = Modifier
                        .clip(PixelShape(6.dp))
                        .background(c.surface)
                        .border(2.dp, c.ink, PixelShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                ) {
                    HakimiText(label, style = HakimiTheme.type.caption, color = c.text)
                }
            }
        }
    }
}
