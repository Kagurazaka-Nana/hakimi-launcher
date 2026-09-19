package com.minecraft.launcher.ui.theme

import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// 参考概念图的暖色系：奶油底 + 蜜桃侧栏 + 橙色主色 + 棕色文字。
val CreamBackground = Color(0xFFFFF6EC)
val PeachSurface = Color(0xFFFDEBD8)
val CardWhite = Color(0xFFFFFDF9)
val PrimaryOrange = Color(0xFFF4795B)
val PrimaryOrangeDark = Color(0xFFE1663F)
val OnPrimary = Color(0xFFFFFDF9)
val TextBrown = Color(0xFF5B4636)
val TextMuted = Color(0xFFB9A99A)
val ReadyGreen = Color(0xFF4CAF7D)
val InfoBlue = Color(0xFF6FA8DC)
val AccentPurple = Color(0xFFB39DDB)
val AccentYellow = Color(0xFFF6C453)
val DividerSoft = Color(0x1A5B4636)

// 世界插画 / 装饰用的调色板。
val SkyTop = Color(0xFFBFE3F2)
val SkyBottom = Color(0xFFFDEBD8)
val GrassGreen = Color(0xFF8FCB6B)
val GrassDark = Color(0xFF6FA84E)
val DirtBrown = Color(0xFFB07A4A)
val DirtDark = Color(0xFF8A5A32)
val WaterBlue = Color(0xFF6FB6E0)
val LeafGreen = Color(0xFF5E9C46)
val BlossomPink = Color(0xFFF3B6D0)
val SunYellow = Color(0xFFFFE08A)
val CloudWhite = Color(0xFFFFFDF9)

val HakimiColorScheme = lightColorScheme(
    primary = PrimaryOrange,
    onPrimary = OnPrimary,
    secondary = PrimaryOrangeDark,
    onSecondary = OnPrimary,
    background = CreamBackground,
    onBackground = TextBrown,
    surface = CardWhite,
    onSurface = TextBrown,
    surfaceVariant = PeachSurface,
    onSurfaceVariant = TextMuted,
    error = PrimaryOrangeDark,
)
