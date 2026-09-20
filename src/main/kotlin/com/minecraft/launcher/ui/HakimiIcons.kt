package com.minecraft.launcher.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BrightnessHigh
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DeveloperBoard
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Power
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Style
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.ui.graphics.vector.ImageVector
import com.minecraft.launcher.ui.svg.SvgIcons

/**
 * 集中图标层。优先使用 assets/icons 下的 SVG（经 [SvgIcons] 解析为 ImageVector），
 * 读取/解析失败或没有对应 SVG 时回退到 Material 图标。
 */
object HakimiIcons {
    // —— 使用项目 SVG 的图标 ——
    val Home: ImageVector = SvgIcons.load("ic_home", Icons.Filled.Home)
    val Mods: ImageVector = SvgIcons.load("ic_mod", Icons.Filled.Extension)
    val ResourcePack: ImageVector = SvgIcons.load("ic_store", Icons.Filled.Image)
    val Server: ImageVector = SvgIcons.load("ic_server", Icons.Filled.Dns)
    val Skin: ImageVector = SvgIcons.load("ic_skin", Icons.Filled.Style)
    val Settings: ImageVector = SvgIcons.load("ic_settings", Icons.Filled.Settings)
    val Download: ImageVector = SvgIcons.load("ic_download", Icons.Filled.Download)
    val Launch: ImageVector = SvgIcons.load("ic_launch", Icons.Filled.PlayArrow)
    val Refresh: ImageVector = SvgIcons.load("ic_refresh", Icons.Filled.Refresh)
    val Person: ImageVector = SvgIcons.load("ic_account", Icons.Filled.Person)
    val Wiki: ImageVector = SvgIcons.load("ic_news", Icons.Filled.MenuBook)
    val Memory: ImageVector = SvgIcons.load("ic_memory", Icons.Filled.Memory)

    // —— 无对应 SVG，使用 Material ——
    val Create: ImageVector = Icons.Filled.Add
    val DataPack: ImageVector = Icons.Filled.Storage
    val Shader: ImageVector = Icons.Filled.BrightnessHigh
    val Modpack: ImageVector = Icons.Filled.Category
    val Plugin: ImageVector = Icons.Filled.Power
    val Screenshot: ImageVector = Icons.Filled.PhotoCamera

    val Menu: ImageVector = Icons.Filled.Menu
    val Collapse: ImageVector = Icons.Filled.KeyboardArrowLeft
    val Search: ImageVector = Icons.Filled.Search
    val Filter: ImageVector = Icons.Filled.Tune
    val Sort: ImageVector = Icons.Filled.Sort
    val Upload: ImageVector = Icons.Filled.Upload
    val Close: ImageVector = Icons.Filled.Close
    val Check: ImageVector = Icons.Filled.Check
    val More: ImageVector = Icons.Filled.MoreVert
    val Edit: ImageVector = Icons.Filled.Edit
    val Back: ImageVector = Icons.AutoMirrored.Filled.ArrowBack
    val LightMode: ImageVector = Icons.Filled.LightMode
    val DarkMode: ImageVector = Icons.Filled.DarkMode

    val Cpu: ImageVector = Icons.Filled.DeveloperBoard
    val Disk: ImageVector = Icons.Filled.Storage
    val Network: ImageVector = Icons.Filled.Wifi
    val Logout: ImageVector = Icons.AutoMirrored.Filled.Logout
}
