package com.oscarrial.gamelauncher

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.oscarrial.gamelauncher.ui.screens.AppLauncherScreen
import com.oscarrial.gamelauncher.ui.theme.GameLauncherTheme

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "Lanzador de Aplicaciones - Oscar Rial"
    ) {
        GameLauncherTheme {
            AppLauncherScreen()
        }
    }
}