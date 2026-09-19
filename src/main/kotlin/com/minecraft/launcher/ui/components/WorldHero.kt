package com.minecraft.launcher.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.minecraft.launcher.ui.theme.BlossomPink
import com.minecraft.launcher.ui.theme.CloudWhite
import com.minecraft.launcher.ui.theme.DirtBrown
import com.minecraft.launcher.ui.theme.DirtDark
import com.minecraft.launcher.ui.theme.GrassDark
import com.minecraft.launcher.ui.theme.GrassGreen
import com.minecraft.launcher.ui.theme.LeafGreen
import com.minecraft.launcher.ui.theme.SkyBottom
import com.minecraft.launcher.ui.theme.SkyTop
import com.minecraft.launcher.ui.theme.SunYellow
import com.minecraft.launcher.ui.theme.WaterBlue
import kotlin.math.min

/**
 * 用 Canvas 画一个风格化的浮空岛世界插画，呼应概念图的等距像素世界。
 * 纯装饰，不依赖任何外部图片资源。
 */
@Composable
fun WorldHeroCanvas(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        // 天空渐变
        drawRect(
            brush = Brush.verticalGradient(listOf(SkyTop, SkyBottom)),
            size = size,
        )

        // 太阳
        drawCircle(SunYellow, radius = w * 0.05f, center = Offset(w * 0.86f, h * 0.18f))

        // 云朵
        cloud(w * 0.2f, h * 0.16f, w * 0.06f)
        cloud(w * 0.62f, h * 0.12f, w * 0.05f)
        cloud(w * 0.42f, h * 0.26f, w * 0.04f)

        // 浮空岛：以等距方块堆叠
        val islandCx = w * 0.5f
        val islandTop = h * 0.42f
        val block = min(w, h) * 0.11f

        // 岛体泥土（下方梯形）
        drawPath(
            path = androidx.compose.ui.graphics.Path().apply {
                moveTo(islandCx - block * 2.4f, islandTop + block * 0.5f)
                lineTo(islandCx + block * 2.4f, islandTop + block * 0.5f)
                lineTo(islandCx + block * 1.2f, islandTop + block * 2.6f)
                lineTo(islandCx - block * 1.2f, islandTop + block * 2.6f)
                close()
            },
            color = DirtBrown,
        )
        // 泥土底部尖
        drawPath(
            path = androidx.compose.ui.graphics.Path().apply {
                moveTo(islandCx - block * 1.2f, islandTop + block * 2.6f)
                lineTo(islandCx + block * 1.2f, islandTop + block * 2.6f)
                lineTo(islandCx, islandTop + block * 3.8f)
                close()
            },
            color = DirtDark,
        )

        // 草地顶面
        drawRect(
            color = GrassGreen,
            topLeft = Offset(islandCx - block * 2.4f, islandTop),
            size = Size(block * 4.8f, block * 0.6f),
        )
        drawRect(
            color = GrassDark,
            topLeft = Offset(islandCx - block * 2.4f, islandTop + block * 0.6f),
            size = Size(block * 4.8f, block * 0.4f),
        )

        // 水塘
        drawRect(
            color = WaterBlue,
            topLeft = Offset(islandCx - block * 0.6f, islandTop - block * 0.05f),
            size = Size(block * 1.4f, block * 0.5f),
        )

        // 树
        tree(islandCx - block * 1.5f, islandTop, block)
        tree(islandCx + block * 1.3f, islandTop, block)

        // 樱花点缀
        drawCircle(BlossomPink, radius = block * 0.18f, center = Offset(islandCx - block * 0.2f, islandTop - block * 0.1f))
        drawCircle(BlossomPink, radius = block * 0.14f, center = Offset(islandCx + block * 0.4f, islandTop - block * 0.15f))
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.cloud(cx: Float, cy: Float, r: Float) {
    drawCircle(CloudWhite, radius = r, center = Offset(cx, cy))
    drawCircle(CloudWhite, radius = r * 0.8f, center = Offset(cx + r, cy + r * 0.2f))
    drawCircle(CloudWhite, radius = r * 0.7f, center = Offset(cx - r, cy + r * 0.25f))
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.tree(baseX: Float, baseY: Float, block: Float) {
    // 树干
    drawRect(
        color = DirtDark,
        topLeft = Offset(baseX - block * 0.08f, baseY - block * 0.9f),
        size = Size(block * 0.16f, block * 0.9f),
    )
    // 树冠方块
    drawRect(
        color = LeafGreen,
        topLeft = Offset(baseX - block * 0.4f, baseY - block * 1.6f),
        size = Size(block * 0.8f, block * 0.8f),
    )
    drawRect(
        color = GrassDark,
        topLeft = Offset(baseX - block * 0.28f, baseY - block * 1.9f),
        size = Size(block * 0.56f, block * 0.4f),
    )
}
