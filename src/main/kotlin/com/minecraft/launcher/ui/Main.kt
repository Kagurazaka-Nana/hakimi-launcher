package com.minecraft.launcher.ui

import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.minecraft.launcher.backend.StubLauncherBackend

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "Hakimi Launcher",
        state = rememberWindowState(size = DpSize(1180.dp, 720.dp)),
    ) {
        LauncherApp(backend = StubLauncherBackend())
    }
}
