package com.minecraft.launcher.ui.svg

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.dp

/**
 * 极简 SVG → [ImageVector] 解析器，覆盖本项目 assets/icons 用到的子集：
 * 仅 `<path>` 元素（命令 M/L/H/V/C/Z，绝对与相对），以及 fill / stroke /
 * stroke-width / stroke-linecap / stroke-linejoin。路径命令交给 [PathParser]。
 */
object SvgImageVector {

    private val pathRegex = Regex("<path\\b[^>]*?/?>")
    private val viewBoxRegex = Regex("viewBox\\s*=\\s*\"([^\"]+)\"")

    fun parse(svg: String, name: String = "svg"): ImageVector {
        val vb = viewBoxRegex.find(svg)?.groupValues?.get(1)
            ?.trim()?.split(Regex("[\\s,]+"))?.mapNotNull { it.toFloatOrNull() }
        val vw = vb?.getOrNull(2) ?: 24f
        val vh = vb?.getOrNull(3) ?: 24f
        val builder = ImageVector.Builder(
            name = name,
            defaultWidth = vw.dp,
            defaultHeight = vh.dp,
            viewportWidth = vw,
            viewportHeight = vh,
        )

        for (match in pathRegex.findAll(svg)) {
            val tag = match.value
            val d = attribute(tag, "d") ?: continue
            val nodes = PathParser().parsePathString(d).toNodes()
            if (nodes.isEmpty()) continue

            val fill = parseColor(attribute(tag, "fill"))?.let { SolidColor(it) }
            val stroke = parseColor(attribute(tag, "stroke"))?.let { SolidColor(it) }
            val strokeWidth = attribute(tag, "stroke-width")?.toFloatOrNull() ?: 0f
            val cap = when (attribute(tag, "stroke-linecap")) {
                "round" -> StrokeCap.Round
                "square" -> StrokeCap.Square
                else -> StrokeCap.Butt
            }
            val join = when (attribute(tag, "stroke-linejoin")) {
                "round" -> StrokeJoin.Round
                "bevel" -> StrokeJoin.Bevel
                else -> StrokeJoin.Miter
            }

            builder.addPath(
                pathData = nodes,
                fill = fill,
                stroke = stroke,
                strokeLineCap = cap,
                strokeLineJoin = join,
                strokeLineWidth = strokeWidth,
            )
        }
        return builder.build()
    }

    private fun attribute(tag: String, key: String): String? {
        val m = Regex("$key\\s*=\\s*\"([^\"]*)\"").find(tag) ?: return null
        return m.groupValues.getOrNull(1)
    }

    private fun parseColor(s: String?): Color? {
        if (s.isNullOrBlank() || s == "none" || s == "transparent") return null
        val hex = s.removePrefix("#")
        return try {
            when (hex.length) {
                3 -> Color(
                    0xFF000000L or
                        (hex[0].toString().repeat(2).toLong(16) shl 16) or
                        (hex[1].toString().repeat(2).toLong(16) shl 8) or
                        hex[2].toString().repeat(2).toLong(16)
                )
                6 -> Color(0xFF000000L or hex.toLong(16))
                8 -> Color(hex.toLong(16))
                else -> Color(0xFF000000L or hex.toLong(16))
            }
        } catch (e: Exception) {
            null
        }
    }
}
