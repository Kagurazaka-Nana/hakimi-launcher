package com.minecraft.launcher.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Hakimi 自建设计系统 —— 视觉层不依赖 Material3 / Fluent。
 * 只定义少量语义色 + 形状 + 文字层级；行为由 Compose Unstyled + Foundation 提供。
 * 主配色：粉红。
 */
@Immutable
data class HakimiColors(
    val background: Color,
    val surface: Color,
    val surfaceMuted: Color,
    val text: Color,
    val textMuted: Color,
    val primary: Color,
    val primarySoft: Color,
    val onPrimary: Color,
    val accent: Color,
    val success: Color,
    val warning: Color,
    val error: Color,
    val outline: Color,
    val scrim: Color,
)

private val LightColors = HakimiColors(
    background = Color(0xFFFFF5F7),
    surface = Color(0xFFFFFCFD),
    surfaceMuted = Color(0xFFFCE7EE),
    text = Color(0xFF3A2E33),
    textMuted = Color(0xFF9A868E),
    primary = Color(0xFFEC6A9C),
    primarySoft = Color(0xFFFBD3E1),
    onPrimary = Color(0xFFFFFFFF),
    accent = Color(0xFFFFB3C6),
    success = Color(0xFF4CAF7D),
    warning = Color(0xFFE8A33D),
    error = Color(0xFFE5484D),
    outline = Color(0x1A3A2E33),
    scrim = Color(0x99FFF5F7),
)

private val DarkColors = HakimiColors(
    background = Color(0xFF1C1519),
    surface = Color(0xFF271E23),
    surfaceMuted = Color(0xFF33272E),
    text = Color(0xFFF3E7EC),
    textMuted = Color(0xFFB39AA4),
    primary = Color(0xFFF084AE),
    primarySoft = Color(0xFF4A2E3A),
    onPrimary = Color(0xFF241019),
    accent = Color(0xFFFFB3C6),
    success = Color(0xFF5FCB92),
    warning = Color(0xFFF0B45A),
    error = Color(0xFFFF7A7E),
    outline = Color(0x22FFFFFF),
    scrim = Color(0xB31C1519),
)

@Immutable
data class HakimiShapes(
    val small: RoundedCornerShape = RoundedCornerShape(10.dp),
    val medium: RoundedCornerShape = RoundedCornerShape(18.dp),
    val large: RoundedCornerShape = RoundedCornerShape(26.dp),
    val pill: RoundedCornerShape = RoundedCornerShape(percent = 50),
)

@Immutable
data class HakimiType(
    val display: TextStyle = TextStyle(fontSize = 26.sp, fontWeight = FontWeight.Bold),
    val title: TextStyle = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.SemiBold),
    val body: TextStyle = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Normal),
    val label: TextStyle = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Medium),
    val caption: TextStyle = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Normal),
)

val LocalHakimiColors: ProvidableCompositionLocal<HakimiColors> = staticCompositionLocalOf { LightColors }
val LocalHakimiShapes: ProvidableCompositionLocal<HakimiShapes> = staticCompositionLocalOf { HakimiShapes() }
val LocalHakimiType: ProvidableCompositionLocal<HakimiType> = staticCompositionLocalOf { HakimiType() }

/** 便捷访问当前语义色。 */
object HakimiTheme {
    val colors: HakimiColors
        @Composable get() = LocalHakimiColors.current
    val shapes: HakimiShapes
        @Composable get() = LocalHakimiShapes.current
    val type: HakimiType
        @Composable get() = LocalHakimiType.current
}

@Composable
fun HakimiTheme(darkTheme: Boolean, content: @Composable () -> Unit) {
    val colors = if (darkTheme) DarkColors else LightColors
    androidx.compose.runtime.CompositionLocalProvider(
        LocalHakimiColors provides colors,
        LocalHakimiShapes provides HakimiShapes(),
        LocalHakimiType provides HakimiType(),
        content = content,
    )
}
