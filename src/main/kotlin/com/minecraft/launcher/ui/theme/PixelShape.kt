package com.minecraft.launcher.ui.theme

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

/**
 * 像素风阶梯圆角：每个角以两级台阶（8-bit 风格）切角，替代平滑圆角。
 * clip / border / drawPath 共用同一轮廓，保证描边与裁剪边缘一致。
 */
class PixelShape(private val corner: Dp = 8.dp) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val path = pixelPath(size, with(density) { corner.toPx() })
        return if (path == null) Outline.Rectangle(Rect(Offset.Zero, size)) else Outline.Generic(path)
    }
}

/** 生成阶梯角轮廓（左上角起顺时针）；尺寸过小或角小于 2px 时返回 null（退化为矩形）。 */
fun pixelPath(size: Size, cornerPx: Float): Path? {
    val c = cornerPx.coerceAtMost(minOf(size.width, size.height) / 4f)
    if (c < 2f) {
        return null
    }
    val s = c / 2f
    val w = size.width
    val h = size.height
    return Path().apply {
        moveTo(0f, c)
        lineTo(s, c); lineTo(s, s); lineTo(c, s); lineTo(c, 0f)
        lineTo(w - c, 0f); lineTo(w - c, s); lineTo(w - s, s); lineTo(w - s, c); lineTo(w, c)
        lineTo(w, h - c); lineTo(w - s, h - c); lineTo(w - s, h - s); lineTo(w - c, h - s); lineTo(w - c, h)
        lineTo(c, h); lineTo(c, h - s); lineTo(s, h - s); lineTo(s, h - c); lineTo(0f, h - c)
        close()
    }
}
