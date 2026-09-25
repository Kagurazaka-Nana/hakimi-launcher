package com.minecraft.launcher.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.minecraft.launcher.ui.theme.HakimiText
import com.minecraft.launcher.ui.theme.HakimiTheme
import com.minecraft.launcher.ui.theme.PixelShape
import org.jetbrains.skia.Image

/**
 * 衣柜 3D 视图：把 64×64 皮肤贴到原版玩家盒模型（头/身/双臂/双腿）上，
 * 正交投影 + 背面剔除 + 画家算法（按深度排序），绕竖直轴缓慢旋转。
 * 模型尺寸按 Minecraft 单位（1 纹理像素 = 1 单位），slim 模型手臂/腿宽 3。
 */

/** 一个可见面：4 角点（模型空间，TL,TR,BR,BL 对应纹理左上起）+ 纹理源矩形。 */
private class Face3D(
    val corners: Array<DoubleArray>, // [4][3] x,y(up),z(朝观察者)
    val u: Double,
    val v: Double,
    val w: Double,
    val h: Double,
    val overlay: Boolean = false, // 帽子/外层（同深度时后画）
)

private class Box3D(
    val x0: Double, val x1: Double,
    val y0: Double, val y1: Double,
    val z0: Double, val z1: Double,
    val u: Double, val v: Double,
) {
    /** 外层（hat）盒：四周外扩 0.5 像素（参考 skinview 的 Box.extra）。 */
    fun expanded(margin: Double = 0.5) = Box3D(
        x0 - margin, x1 + margin, y0 - margin, y1 + margin, z0 - margin, z1 + margin, u, v,
    )

    /** 按原版盒模型 UV 展开（top/bottom/front/back/right/left）。 */
    fun faces(overlay: Boolean = false): List<Face3D> {
        val w = x1 - x0
        val h = y1 - y0
        val d = z1 - z0
        fun dv(x: Double, y: Double, z: Double) = doubleArrayOf(x, y, z)
        return listOf(
            // top
            Face3D(arrayOf(dv(x0, y1, z1), dv(x1, y1, z1), dv(x1, y1, z0), dv(x0, y1, z0)), u + d, v, w, d, overlay),
            // bottom
            Face3D(arrayOf(dv(x0, y0, z0), dv(x1, y0, z0), dv(x1, y0, z1), dv(x0, y0, z1)), u + d + w, v, w, d, overlay),
            // front (+z)
            Face3D(arrayOf(dv(x0, y1, z1), dv(x1, y1, z1), dv(x1, y0, z1), dv(x0, y0, z1)), u + d, v + d, w, h, overlay),
            // back (-z)
            Face3D(arrayOf(dv(x1, y1, z0), dv(x0, y1, z0), dv(x0, y0, z0), dv(x1, y0, z0)), u + 2 * d + w, v + d, d, h, overlay),
            // right (+x)
            Face3D(arrayOf(dv(x1, y1, z1), dv(x1, y1, z0), dv(x1, y0, z0), dv(x1, y0, z1)), u, v + d, d, h, overlay),
            // left (-x)
            Face3D(arrayOf(dv(x0, y1, z0), dv(x0, y1, z1), dv(x0, y0, z1), dv(x0, y0, z0)), u + d + w, v + d, d, h, overlay),
        )
    }
}

/** 外层（hat）UV 原点，按部位（minecraft.wiki/w/Skin 几何规范）。 */
private const val U_HEAD_OVERLAY = 32.0
private const val V_HEAD_OVERLAY = 0.0
private const val U_BODY_OVERLAY = 16.0
private const val V_BODY_OVERLAY = 32.0
private const val U_RARM_OVERLAY = 40.0
private const val V_RARM_OVERLAY = 32.0
private const val U_LARM_OVERLAY = 48.0
private const val V_LARM_OVERLAY = 48.0
private const val U_RLEG_OVERLAY = 0.0
private const val V_RLEG_OVERLAY = 32.0
private const val U_LLEG_OVERLAY = 48.0
private const val V_LLEG_OVERLAY = 32.0

private fun playerBoxes(slim: Boolean): List<Face3D> {
    val aw = if (slim) 3.0 else 4.0 // 手臂/腿宽
    // (基础盒, 外层UV)
    val parts = listOf(
        // 腿：y 0..12
        Triple(Box3D(-4.0, 0.0, 0.0, 12.0, -2.0, 2.0, 0.0, 16.0), U_RLEG_OVERLAY, V_RLEG_OVERLAY),
        Triple(Box3D(0.0, 4.0, 0.0, 12.0, -2.0, 2.0, 16.0, 48.0), U_LLEG_OVERLAY, V_LLEG_OVERLAY),
        // 身体：y 12..24
        Triple(Box3D(-4.0, 4.0, 12.0, 24.0, -2.0, 2.0, 16.0, 16.0), U_BODY_OVERLAY, V_BODY_OVERLAY),
        // 手臂：y 12..24，贴在身体两侧
        Triple(Box3D(4.0, 4.0 + aw, 12.0, 24.0, -2.0, 2.0, 40.0, 16.0), U_RARM_OVERLAY, V_RARM_OVERLAY),
        Triple(Box3D(-4.0 - aw, -4.0, 12.0, 24.0, -2.0, 2.0, 32.0, 48.0), U_LARM_OVERLAY, V_LARM_OVERLAY),
        // 头：y 24..32
        Triple(Box3D(-4.0, 4.0, 24.0, 32.0, -4.0, 4.0, 0.0, 0.0), U_HEAD_OVERLAY, V_HEAD_OVERLAY),
    )
    return parts.flatMap { (box, ou, ov) ->
        box.faces() + box.expanded().copy(ou, ov).faces(overlay = true)
    }
}

/** Box3D 的 UV 替换副本（外层用第二排 UV）。 */
private fun Box3D.copy(u: Double, v: Double) = Box3D(x0, x1, y0, y1, z0, z1, u, v)

/**
 * 解码皮肤；64×32 旧皮肤按 skinview LegacySkinLayer 的镜像表补全到 64×64 第二排
 * （左臂/左腿/头部顶底等区域水平镜像复制）。
 */
private val LEGACY_MIRROR_REGIONS = arrayOf(
    // sx, sy, dx, dy, w, h（源区域 → 目标区域，水平镜像）
    intArrayOf(0, 20, 16, 52, 12, 12),
    intArrayOf(12, 20, 28, 52, 4, 12),
    intArrayOf(4, 16, 20, 48, 4, 4),
    intArrayOf(8, 16, 24, 48, 4, 4),
    intArrayOf(40, 20, 32, 52, 12, 12),
    intArrayOf(52, 20, 44, 52, 4, 12),
    intArrayOf(44, 16, 36, 48, 4, 4),
    intArrayOf(48, 16, 40, 48, 4, 4),
)

private fun decodeSkin(bytes: ByteArray): ImageBitmap {
    val image = Image.makeFromEncoded(bytes)
    if (image.width != 64 || image.height != 32) {
        return image.toComposeImageBitmap()
    }
    val dest = org.jetbrains.skia.Bitmap().apply { allocN32Pixels(64, 64, true) }
    val canvas = org.jetbrains.skia.Canvas(dest)
    canvas.drawImageRect(image, org.jetbrains.skia.Rect.makeXYWH(0f, 0f, 64f, 32f), org.jetbrains.skia.Rect.makeXYWH(0f, 0f, 64f, 32f))
    for (r in LEGACY_MIRROR_REGIONS) {
        canvas.save()
        // 水平镜像：平移到目标右边界并翻转 x
        canvas.translate((r[2] + r[4]).toFloat(), r[3].toFloat())
        canvas.scale(-1f, 1f)
        canvas.drawImageRect(
            image,
            org.jetbrains.skia.Rect.makeXYWH(r[0].toFloat(), r[1].toFloat(), r[4].toFloat(), r[5].toFloat()),
            org.jetbrains.skia.Rect.makeXYWH(0f, 0f, r[4].toFloat(), r[5].toFloat()),
        )
        canvas.restore()
    }
    return Image.makeFromBitmap(dest).toComposeImageBitmap()
}

/** 衣柜：3D 玩家模型展示（登录后的皮肤；无皮肤显示占位）。 */
@Composable
fun Wardrobe3D(png: ByteArray?, slim: Boolean, modifier: Modifier = Modifier) {
    val c = HakimiTheme.colors
    val bitmap: ImageBitmap? = remember(png) { png?.let { decodeSkin(it) } }
    val paint = remember { Paint().apply { filterQuality = FilterQuality.None } }
    val transition = rememberInfiniteTransition(label = "yaw")
    val autoYaw by transition.animateFloat(
        initialValue = 25f,
        targetValue = 335f,
        animationSpec = infiniteRepeatable(tween(14_000, easing = LinearEasing), RepeatMode.Restart),
        label = "yaw",
    )
    // NaN = 自动旋转；首次拖拽后接管为手动
    var manualYaw by remember { mutableStateOf(Float.NaN) }
    var pitch by remember { mutableFloatStateOf(0f) }
    val yaw = if (manualYaw.isNaN()) autoYaw else manualYaw
    val faces = remember(slim) { playerBoxes(slim) }

    Box(
        modifier = modifier
            .clip(PixelShape(10.dp))
            .background(c.surfaceMuted)
            .border(2.dp, c.ink, PixelShape(10.dp))
            .pointerInput(Unit) {
                detectDragGestures { change, drag ->
                    change.consume()
                    if (manualYaw.isNaN()) manualYaw = autoYaw
                    manualYaw = (manualYaw + drag.x * 0.4f + 360f) % 360f
                    pitch = (pitch - drag.y * 0.25f).coerceIn(-40f, 40f)
                }
            }
            .drawBehind {
                val img = bitmap ?: return@drawBehind
                val s = size.height / 36f // 32 单位模型 + 上下留白
                val cx = size.width / 2f
                val radY = Math.toRadians(yaw.toDouble())
                val cosY = Math.cos(radY)
                val sinY = Math.sin(radY)
                val radX = Math.toRadians(pitch.toDouble())
                val cosX = Math.cos(radX)
                val sinX = Math.sin(radX)

                // 先绕 y 轴（yaw）再绕 x 轴（pitch，以模型中心 y=16 为枢轴），正交投影
                val rot = Array(faces.size) { fi ->
                    Array(4) { i ->
                        val p = faces[fi].corners[i]
                        val x1 = p[0] * cosY + p[2] * sinY
                        val z1 = -p[0] * sinY + p[2] * cosY
                        val dy = p[1] - 16.0
                        doubleArrayOf(
                            x1,
                            dy * cosX - z1 * sinX + 16.0,
                            dy * sinX + z1 * cosX,
                        )
                    }
                }

                // 背面剔除：法线 z 分量（模型空间）<=0 不可见；画家算法按质心深度升序（远→近），
                // 同深度时基础层先画、外层（hat）后画
                val order = faces.indices.mapNotNull { fi ->
                    val r = rot[fi]
                    val e1x = r[3][0] - r[0][0]; val e1y = r[3][1] - r[0][1]
                    val e2x = r[1][0] - r[0][0]; val e2y = r[1][1] - r[0][1]
                    val nz = e1x * e2y - e1y * e2x
                    if (nz <= 0.0) return@mapNotNull null
                    fi to (r[0][2] + r[1][2] + r[2][2] + r[3][2]) / 4.0
                }.sortedWith(compareBy({ it.second }, { if (faces[it.first].overlay) 1 else 0 }))

                val canvas = drawContext.canvas
                order.forEach { (fi, _) ->
                    val f = faces[fi]
                    val r = rot[fi]
                    fun sx(i: Int) = cx + r[i][0].toFloat() * s
                    fun sy(i: Int) = (32.0 - r[i][1]).toFloat() * s + s
                    val p0x = sx(0); val p0y = sy(0)
                    val ux = (sx(1) - p0x) / f.w.toFloat(); val uy = (sy(1) - p0y) / f.w.toFloat()
                    val vx = (sx(3) - p0x) / f.h.toFloat(); val vy = (sy(3) - p0y) / f.h.toFloat()
                    val m = Matrix()
                    m.values[Matrix.ScaleX] = ux
                    m.values[Matrix.SkewX] = vx
                    m.values[Matrix.TranslateX] = p0x
                    m.values[Matrix.SkewY] = uy
                    m.values[Matrix.ScaleY] = vy
                    m.values[Matrix.TranslateY] = p0y
                    canvas.save()
                    canvas.concat(m)
                    // 目标外扩 1 像素覆盖接缝，最近邻采样保持像素锐利
                    canvas.drawImageRect(
                        img,
                        IntOffset(f.u.toInt(), f.v.toInt()),
                        IntSize(f.w.toInt(), f.h.toInt()),
                        IntOffset(-1, -1),
                        IntSize(f.w.toInt() + 2, f.h.toInt() + 2),
                        paint,
                    )
                    canvas.restore()
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
