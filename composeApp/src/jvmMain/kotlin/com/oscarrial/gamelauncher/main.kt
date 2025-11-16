package com.oscarrial.gamelauncher

import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.oscarrial.gamelauncher.ui.screens.AppLauncherScreen
import com.oscarrial.gamelauncher.ui.theme.GameLauncherTheme

/**
 * Punto de entrada de la aplicación de escritorio.
 * Aquí se inicializa Compose Desktop, se configura la ventana
 * y se carga la pantalla principal del lanzador.
 */
fun main() = application {

    // Estado inicial de la ventana (tamaño por defecto, posición, etc.)
    val windowState = rememberWindowState(
        size = DpSize(1400.dp, 900.dp)
    )

    // Ventana principal de la aplicación.
    Window(
        onCloseRequest = ::exitApplication,
        title = "Lanzador de Aplicaciones - Oscar Rial",
        state = windowState
    ) {
        // Tamaño mínimo permitido para evitar que la UI se rompa al reducir demasiado la ventana.
        window.minimumSize = java.awt.Dimension(1200, 700)

        // Tema visual global del proyecto y carga de la pantalla principal.
        GameLauncherTheme {
            AppLauncherScreen()
        }
    }
}
