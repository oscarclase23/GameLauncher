package com.oscarrial.gamelauncher.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.oscarrial.gamelauncher.data.AppInfo // Importa el modelo
import com.oscarrial.gamelauncher.data.sampleApps // Importa los datos de prueba
import com.oscarrial.gamelauncher.system.PlatformService // Importa el servicio de sistema
import java.io.IOException

/**
 * ViewModel que gestiona el estado y la lógica de la pantalla del lanzador.
 */
class LauncherViewModel {

    // ESTADO 1: Lista de aplicaciones. Inicializada con datos de prueba.
    var apps by mutableStateOf(sampleApps())
        private set

    // ESTADO 2: Query de búsqueda.
    var searchQuery by mutableStateOf("")
        private set

    // Lógica para filtrar las apps basándose en la búsqueda
    val filteredApps: List<AppInfo>
        get() = apps.filter { it.name.contains(searchQuery, ignoreCase = true) }


    // ACCIÓN 1: Actualizar la búsqueda
    fun updateSearchQuery(query: String) {
        searchQuery = query
        // Aquí no necesitamos actualizar 'apps' porque filteredApps es un 'getter'
        // que se recalcula automáticamente cuando se accede.
    }

    // ACCIÓN 2: Lógica de lanzamiento de la aplicación (implementada de forma simple).
    fun launchApp(app: AppInfo) {
        val launchPrefix = PlatformService.getLaunchPrefix()
        val fullCommand = launchPrefix + app.path

        println("--- EJECUTANDO COMANDO: $fullCommand")

        try {
            // Requisito del proyecto: Usar ProcessBuilder
            ProcessBuilder(*fullCommand.split(" ").toTypedArray()).start()

        } catch (e: IOException) {
            println("ERROR al lanzar ${app.name}: ${e.message}. Verifique la ruta del ejecutable.")
            // TO-DO: Mostrar un mensaje de error visible en la UI.
        }
    }

    // ACCIÓN 3: Añadir una aplicación (por ahora solo actualiza la lista local)
    fun addApp(app: AppInfo) {
        apps = apps + app
    }

    // ACCIÓN 4: Eliminar una aplicación (solo las añadidas manualmente)
    fun removeApp(app: AppInfo) {
        if (app.isCustom) {
            apps = apps - app
        }
    }

    // TO-DO: Aquí es donde la Lógica de Escaneo se integrará en el próximo commit.
}