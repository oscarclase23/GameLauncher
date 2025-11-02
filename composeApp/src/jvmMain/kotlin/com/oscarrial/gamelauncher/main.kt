package com.oscarrial.gamelauncher

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.oscarrial.gamelauncher.ui.screens.SimpleAppLauncherScreen // Asegúrate de que esta importación sea correcta
import com.oscarrial.gamelauncher.ui.theme.GameLauncherTheme // Importa tu tema


fun main() = application {
    Window(onCloseRequest = ::exitApplication, title = "Lanzador de Aplicaciones - Oscar Rial") {
        // 1. Aplica el tema de color
        GameLauncherTheme {
            // 2. Muestra tu pantalla principal
            SimpleAppLauncherScreen()
        }
    }
}