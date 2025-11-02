package com.oscarrial.gamelauncher

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "GameLauncher",
    ) {
        App()
    }
}