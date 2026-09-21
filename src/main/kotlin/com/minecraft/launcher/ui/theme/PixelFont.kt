package com.minecraft.launcher.ui.theme

import androidx.compose.ui.text.font.FontFamily

/**
 * 字体族。当前用等宽字体（中文由系统字体回退，保证可读）。
 * 真·中文像素字体（Fusion Pixel，已支持中文）需通过 Compose Resources 加载，
 * 而纯 kotlin("jvm") 工程不生成 Res —— 需迁移到 KMP 的 jvm() target（见 README/后续 PR）。
 */
val PixelDisplayFont = FontFamily.Monospace
val PixelBodyFont = FontFamily.Monospace
