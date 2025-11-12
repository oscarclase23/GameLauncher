package com.oscarrial.gamelauncher

import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.oscarrial.gamelauncher.ui.screens.AppLauncherScreen
import com.oscarrial.gamelauncher.ui.theme.GameLauncherTheme

fun main() = application {
    val windowState = rememberWindowState(
        size = DpSize(1400.dp, 900.dp)
    )

    Window(
        onCloseRequest = ::exitApplication,
        title = "Lanzador de Aplicaciones - Oscar Rial",
        state = windowState
    ) {
        window.minimumSize = java.awt.Dimension(1200, 700)

        GameLauncherTheme {
            AppLauncherScreen()
        }
    }
}