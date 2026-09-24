package com.minecraft.launcher.ui.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.ui.graphics.Shape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Hakimi 自建设计系统 —— 像素风 · 粉绿主基调。
 * 视觉层不依赖 Material3 / Fluent：硬描边 + 偏移实心阴影（贴纸感）+ 像素字体 + 少量像素角标。
 */
@Immutable
data class HakimiColors(
    val background: Color,
    val surface: Color,
    val surfaceMuted: Color,
    val ink: Color,
    val text: Color,
    val textMuted: Color,
    val primary: Color,
    val primarySoft: Color,
    val onPrimary: Color,
    val accent: Color,
    val success: Color,
    val warning: Color,
    val error: Color,
    val scrim: Color,
)

private val LightColors = HakimiColors(
    background = Color(0xFFF3ECDA),
    surface = Color(0xFFFBF6EA),
    surfaceMuted = Color(0xFFEADFC6),
    ink = Color(0xFF2A2622),
    text = Color(0xFF2A2622),
    textMuted = Color(0xFF7C746A),
    primary = Color(0xFFF2A6BC),
    primarySoft = Color(0xFFF7D3DD),
    onPrimary = Color(0xFF2A2622),
    accent = Color(0xFF8FBF9F),
    success = Color(0xFF6FA86F),
    warning = Color(0xFFE8B04B),
    error = Color(0xFFE5484D),
    scrim = Color(0xE6F3ECDA),
)

private val DarkColors = HakimiColors(
    background = Color(0xFF211D19),
    surface = Color(0xFF2B2621),
    surfaceMuted = Color(0xFF37312A),
    ink = Color(0xFFE9E1CE),
    text = Color(0xFFF3ECDA),
    textMuted = Color(0xFFA99F8F),
    primary = Color(0xFFE58BA6),
    primarySoft = Color(0xFF5A3038),
    onPrimary = Color(0xFF211D19),
    accent = Color(0xFF7FB08F),
    success = Color(0xFF7FBF7F),
    warning = Color(0xFFE8B04B),
    error = Color(0xFFFF7A7E),
    scrim = Color(0xE6211D19),
)

@Immutable
data class HakimiShapes(
    val small: Shape = PixelShape(6.dp),
    val medium: Shape = PixelShape(10.dp),
    val large: Shape = PixelShape(14.dp),
    val pill: Shape = PixelShape(8.dp),
)

/** 像素贴纸几何：硬描边宽度。 */
@Immutable
data class HakimiMetrics(
    val stroke: Dp = 2.dp,
)

@Immutable
data class HakimiType(
    val display: TextStyle = TextStyle(fontFamily = PixelDisplayFont, fontSize = 22.sp, fontWeight = FontWeight.Bold),
    val title: TextStyle = TextStyle(fontFamily = PixelDisplayFont, fontSize = 16.sp, fontWeight = FontWeight.Bold),
    val body: TextStyle = TextStyle(fontFamily = PixelBodyFont, fontSize = 17.sp, fontWeight = FontWeight.Normal),
    val label: TextStyle = TextStyle(fontFamily = PixelBodyFont, fontSize = 15.sp, fontWeight = FontWeight.Normal),
    val caption: TextStyle = TextStyle(fontFamily = PixelBodyFont, fontSize = 13.sp, fontWeight = FontWeight.Normal),
)

val LocalHakimiColors: ProvidableCompositionLocal<HakimiColors> = staticCompositionLocalOf { LightColors }
val LocalHakimiShapes: ProvidableCompositionLocal<HakimiShapes> = staticCompositionLocalOf { HakimiShapes() }
val LocalHakimiType: ProvidableCompositionLocal<HakimiType> = staticCompositionLocalOf { HakimiType() }
val LocalHakimiMetrics: ProvidableCompositionLocal<HakimiMetrics> = staticCompositionLocalOf { HakimiMetrics() }

object HakimiTheme {
    val colors: HakimiColors
        @Composable get() = LocalHakimiColors.current
    val shapes: HakimiShapes
        @Composable get() = LocalHakimiShapes.current
    val type: HakimiType
        @Composable get() = LocalHakimiType.current
    val metrics: HakimiMetrics
        @Composable get() = LocalHakimiMetrics.current
}

/** Emil Kowalski 动效曲线：强 ease-out 进入，ease-in-out 位移；退出更快。 */
object HakimiMotion {
    val EaseOut: Easing = CubicBezierEasing(0.23f, 1f, 0.32f, 1f)
    val EaseInOut: Easing = CubicBezierEasing(0.77f, 0f, 0.175f, 1f)
    const val PressMs = 120
    const val EnterMs = 220
    const val ExitMs = 140
    const val StaggerMs = 45
}

@Composable
fun HakimiTheme(darkTheme: Boolean, content: @Composable () -> Unit) {
    androidx.compose.runtime.CompositionLocalProvider(
        LocalHakimiColors provides if (darkTheme) DarkColors else LightColors,
        LocalHakimiShapes provides HakimiShapes(),
        LocalHakimiType provides HakimiType(),
        LocalHakimiMetrics provides HakimiMetrics(),
        content = content,
    )
}
