package com.minecraft.launcher.ui

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.minecraft.launcher.backend.StubLauncherBackend

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "Hakimi Launcher"
    ) {
        LauncherApp(backend = StubLauncherBackend())
    }
}
