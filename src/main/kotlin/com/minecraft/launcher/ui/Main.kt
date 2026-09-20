package com.minecraft.launcher.ui

import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.minecraft.launcher.backend.StubLauncherBackend
import com.minecraft.launcher.ui.state.LauncherViewModel

fun main() = application {
    val vm = remember { LauncherViewModel(StubLauncherBackend()) }
    DisposableEffect(Unit) {
        vm.start()
        onDispose { vm.close() }
    }

    Window(
        onCloseRequest = ::exitApplication,
        title = "Hakimi Launcher",
        state = rememberWindowState(size = DpSize(1200.dp, 760.dp)),
        onPreviewKeyEvent = { event ->
            if (event.type == KeyEventType.KeyDown) {
                val combo = event.isMetaPressed || event.isCtrlPressed
                when {
                    event.key == Key.L && combo -> {
                        vm.toggleLaunchpad()
                        true
                    }
                    event.key == Key.Escape -> {
                        vm.closeLaunchpad()
                        false
                    }
                    else -> false
                }
            } else {
                false
            }
        },
    ) {
        LauncherApp(vm = vm)
    }
}
