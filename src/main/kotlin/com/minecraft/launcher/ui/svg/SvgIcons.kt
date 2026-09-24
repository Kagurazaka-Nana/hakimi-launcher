package com.minecraft.launcher.ui.svg

import androidx.compose.ui.graphics.vector.ImageVector

/**
 * 从 classpath `/icons/<name>.svg` 加载并缓存 SVG 图标；
 * 读取或解析失败时回退到传入的 [fallback]（Material 图标），保证 UI 永不缺图。
 */
object SvgIcons {

    private val cache = HashMap<String, ImageVector>()

    fun load(name: String, fallback: ImageVector): ImageVector = cache.getOrPut(name) {
        try {
            val stream = SvgIcons::class.java.getResourceAsStream("/icons/$name.svg")
                ?: return@getOrPut fallback
            val text = stream.use { it.readBytes().decodeToString() }
            SvgImageVector.parse(text, name)
        } catch (e: Throwable) {
            fallback
        }
    }
}
