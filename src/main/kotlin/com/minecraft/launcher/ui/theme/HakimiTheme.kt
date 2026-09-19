package com.minecraft.launcher.ui.theme

import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// 参考 docs/export 视觉稿的配色：深色导航栏 + 淡紫内容区 + 靓紫主色。
val SidebarDark = Color(0xFF26243A)
val SidebarActive = Color(0xFF6C5CE7)
val SidebarTextMuted = Color(0xFF9A97B5)

val ContentBackground = Color(0xFFEDEFFA)
val CardWhite = Color(0xFFFFFFFF)
val CardSoft = Color(0xFFFBFAFF)

val PrimaryIndigo = Color(0xFF6C5CE7)
val PrimaryIndigoDark = Color(0xFF5A4BD1)
val OnPrimary = Color(0xFFFFFFFF)

val TextDark = Color(0xFF2D2A44)
val TextMuted = Color(0xFF8E8CA6)

val SuccessGreen = Color(0xFF3FA34D)
val SuccessBg = Color(0xFFE4F5E8)
val InfoBlue = Color(0xFF5B8DEF)
val AccentPurple = Color(0xFF9B6FE0)
val AccentOrange = Color(0xFFF5A623)
val AccentTeal = Color(0xFF3FB6A8)
val AccentPink = Color(0xFFE86AA6)

val ChipBg = Color(0xFFECE9FB)
val ChipText = Color(0xFF6C5CE7)
val FrameBlue = Color(0xFF6C8CF0)

// 世界插画调色板（浮空岛）。
val SkyTop = Color(0xFFBFE3F2)
val SkyBottom = Color(0xFFEDEFFA)
val GrassGreen = Color(0xFF8FCB6B)
val GrassDark = Color(0xFF6FA84E)
val DirtBrown = Color(0xFFB07A4A)
val DirtDark = Color(0xFF8A5A32)
val WaterBlue = Color(0xFF6FB6E0)
val LeafGreen = Color(0xFF5E9C46)
val SunYellow = Color(0xFFFFE08A)
val CloudWhite = Color(0xFFFFFDF9)

val HakimiColorScheme = lightColorScheme(
    primary = PrimaryIndigo,
    onPrimary = OnPrimary,
    secondary = PrimaryIndigoDark,
    onSecondary = OnPrimary,
    background = ContentBackground,
    onBackground = TextDark,
    surface = CardWhite,
    onSurface = TextDark,
    surfaceVariant = ChipBg,
    onSurfaceVariant = TextMuted,
    error = PrimaryIndigoDark,
)
