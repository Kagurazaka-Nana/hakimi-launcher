package com.minecraft.launcher.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

// —— 亮色配色（柔和、可爱、大圆角）——
private val LightBackground = Color(0xFFF4F5FB)
private val LightSurface = Color(0xFFFFFFFF)
private val LightSurfaceVariant = Color(0xFFECEAF8)
private val LightPrimary = Color(0xFF6C5CE7)
private val LightOnPrimary = Color(0xFFFFFFFF)
private val LightSecondary = Color(0xFF9B6FE0)
private val LightTertiary = Color(0xFF3FB6A8)
private val LightOnBackground = Color(0xFF26243A)
private val LightOnSurfaceVariant = Color(0xFF6B6A82)
private val LightError = Color(0xFFE5484D)

// —— 暗色配色 ——
private val DarkBackground = Color(0xFF1B1A28)
private val DarkSurface = Color(0xFF26243A)
private val DarkSurfaceVariant = Color(0xFF322F4A)
private val DarkPrimary = Color(0xFF9C8CF5)
private val DarkOnPrimary = Color(0xFF1B1A28)
private val DarkSecondary = Color(0xFFB9A6F0)
private val DarkTertiary = Color(0xFF5FD3C4)
private val DarkOnBackground = Color(0xFFECEAF6)
private val DarkOnSurfaceVariant = Color(0xFFB6B4CC)
private val DarkError = Color(0xFFFF6B6B)

private val LightScheme = lightColorScheme(
    primary = LightPrimary,
    onPrimary = LightOnPrimary,
    secondary = LightSecondary,
    onSecondary = LightOnPrimary,
    tertiary = LightTertiary,
    background = LightBackground,
    onBackground = LightOnBackground,
    surface = LightSurface,
    onSurface = LightOnBackground,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightOnSurfaceVariant,
    error = LightError,
)

private val DarkScheme = darkColorScheme(
    primary = DarkPrimary,
    onPrimary = DarkOnPrimary,
    secondary = DarkSecondary,
    onSecondary = DarkOnPrimary,
    tertiary = DarkTertiary,
    background = DarkBackground,
    onBackground = DarkOnBackground,
    surface = DarkSurface,
    onSurface = DarkOnBackground,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkOnSurfaceVariant,
    error = DarkError,
)

// 统一圆角：大圆角、柔和。
val HakimiShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(30.dp),
)

@Composable
fun HakimiTheme(darkTheme: Boolean, content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkScheme else LightScheme,
        shapes = HakimiShapes,
        content = content,
    )
}

// 语义色（成功/警告/信息），随主题取 onSurface 之外的固定强调色。
object HakimiColors {
    val Success = Color(0xFF3FA34D)
    val SuccessBg = Color(0xFFE4F5E8)
    val Warning = Color(0xFFF5A623)
    val Info = Color(0xFF5B8DEF)
    val Pink = Color(0xFFE86AA6)
}

// 顶层语义强调色，供各页面直接引用。
val SuccessGreen = Color(0xFF3FA34D)
val SuccessBg = Color(0xFFE4F5E8)
val InfoBlue = Color(0xFF5B8DEF)
val AccentOrange = Color(0xFFF5A623)
val AccentPurple = Color(0xFF9B6FE0)
val AccentPink = Color(0xFFE86AA6)
