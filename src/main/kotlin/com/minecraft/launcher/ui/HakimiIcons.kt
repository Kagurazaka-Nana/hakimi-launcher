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

/**
 * 集中图标映射。当前使用 Material 图标；后续可把 [of] 换成加载
 * assets/icons 下 SVG 的 painter，无需改动各页面。
 */
object HakimiIcons {
    val Home: ImageVector = Icons.Filled.Home
    val Create: ImageVector = Icons.Filled.Add
    val Mods: ImageVector = Icons.Filled.Extension
    val ResourcePack: ImageVector = Icons.Filled.Image
    val DataPack: ImageVector = Icons.Filled.Storage
    val Shader: ImageVector = Icons.Filled.BrightnessHigh
    val Modpack: ImageVector = Icons.Filled.Category
    val Plugin: ImageVector = Icons.Filled.Power
    val Server: ImageVector = Icons.Filled.Dns
    val Wiki: ImageVector = Icons.Filled.MenuBook
    val Screenshot: ImageVector = Icons.Filled.PhotoCamera
    val Skin: ImageVector = Icons.Filled.Style

    val Menu: ImageVector = Icons.Filled.Menu
    val Collapse: ImageVector = Icons.Filled.KeyboardArrowLeft
    val Search: ImageVector = Icons.Filled.Search
    val Filter: ImageVector = Icons.Filled.Tune
    val Sort: ImageVector = Icons.Filled.Sort
    val Launch: ImageVector = Icons.Filled.PlayArrow
    val Download: ImageVector = Icons.Filled.Download
    val Upload: ImageVector = Icons.Filled.Upload
    val Refresh: ImageVector = Icons.Filled.Refresh
    val Close: ImageVector = Icons.Filled.Close
    val Check: ImageVector = Icons.Filled.Check
    val Settings: ImageVector = Icons.Filled.Settings
    val More: ImageVector = Icons.Filled.MoreVert
    val Edit: ImageVector = Icons.Filled.Edit
    val Back: ImageVector = Icons.AutoMirrored.Filled.ArrowBack
    val Person: ImageVector = Icons.Filled.Person
    val LightMode: ImageVector = Icons.Filled.LightMode
    val DarkMode: ImageVector = Icons.Filled.DarkMode

    val Cpu: ImageVector = Icons.Filled.DeveloperBoard
    val Memory: ImageVector = Icons.Filled.Memory
    val Disk: ImageVector = Icons.Filled.Storage
    val Network: ImageVector = Icons.Filled.Wifi
    val Logout: ImageVector = Icons.AutoMirrored.Filled.Logout
}
