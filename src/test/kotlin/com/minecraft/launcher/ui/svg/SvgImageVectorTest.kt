package com.minecraft.launcher.ui.svg

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class SvgImageVectorTest {

    @Test
    fun `parses stroke-only path using viewBox dimensions`() {
        val svg = """
            <svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none">
              <path d="M3.8 11.4L12 4.8L20.2 11.4Z" fill="none" stroke="#3B3350" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"/>
            </svg>
        """.trimIndent()

        val vector = SvgImageVector.parse(svg, "home")
        assertEquals("home", vector.name)
        assertEquals(24f, vector.viewportWidth)
        assertEquals(24f, vector.viewportHeight)
    }

    @Test
    fun `parses filled path with cubic curve and hex color`() {
        val svg = """
            <svg viewBox="0 0 512 512">
              <path d="M10 10C20 20 30 30 40 40L50 50Z" fill="#FF9A6C"/>
            </svg>
        """.trimIndent()

        val vector = SvgImageVector.parse(svg, "filled")
        assertEquals(512f, vector.viewportWidth)
    }

    @Test
    fun `handles relative commands and multiple paths`() {
        val svg = """
            <svg viewBox="0 0 24 24">
              <path d="M4 4h8v8H4z" fill="#112233"/>
              <path d="m2 2l4 4 4 -4z" stroke="#445566" stroke-width="2"/>
            </svg>
        """.trimIndent()

        val vector = SvgImageVector.parse(svg, "multi")
        assertEquals(24f, vector.viewportWidth)
    }

    @Test
    fun `loads a real bundled icon resource instead of the material fallback`() {
        val vector = SvgIcons.load("ic_home", Icons.Filled.Home)
        // 资源存在时解析器以文件名命名；回退的 Material 图标名不同。
        assertEquals("ic_home", vector.name)
        assertEquals(24f, vector.viewportWidth)
    }
}
