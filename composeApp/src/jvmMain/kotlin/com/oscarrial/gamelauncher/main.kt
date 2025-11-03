package com.oscarrial.gamelauncher

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.oscarrial.gamelauncher.system.PlatformService
import com.oscarrial.gamelauncher.ui.screens.AppLauncherScreen
import com.oscarrial.gamelauncher.ui.theme.GameLauncherTheme

fun main() = application {
    // Esto se ejecuta en el Hilo Principal (UI) antes de que se dibuje la ventana.
    println("Sistema operativo detectado: ${PlatformService.getCurrentOS()}")

    Window(onCloseRequest = ::exitApplication, title = "Lanzador de Aplicaciones - Oscar Rial") {
        // La pantalla se inicializa aquí
        GameLauncherTheme {
            AppLauncherScreen()
        }
    }
}
