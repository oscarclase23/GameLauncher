package com.oscarrial.gamelauncher.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.oscarrial.gamelauncher.data.AppInfo
import com.oscarrial.gamelauncher.system.AppScanner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException

/**
 * ViewModel que gestiona el estado y la lógica de la pantalla del lanzador.
 */
class LauncherViewModel {

    private val viewModelScope = CoroutineScope(Dispatchers.Main)

    // Estados
    var apps by mutableStateOf(emptyList<AppInfo>())
        private set

    var searchQuery by mutableStateOf("")
        private set

    var isLoading by mutableStateOf(true)
        private set

    init {
        loadApps()
    }

    // Aplicaciones filtradas por búsqueda
    val filteredApps: List<AppInfo>
        get() = apps.filter { it.name.contains(searchQuery, ignoreCase = true) }

    /**
     * Carga las aplicaciones asíncronamente
     */
    private fun loadApps() {
        viewModelScope.launch {
            try {
                // Ejecutar el escaneo en el hilo de I/O
                val scannedApps = withContext(Dispatchers.IO) {
                    AppScanner.scanWindowsApps()
                }

                // Actualización en el hilo Main
                apps = scannedApps
                println("✅ Apps cargadas correctamente. Total: ${apps.size}")

            } catch (e: Exception) {
                println("❌ ERROR en el escaneo: ${e.message}")
                e.printStackTrace()
                apps = emptyList()

            } finally {
                isLoading = false
            }
        }
    }

    // --- ACCIONES ---

    fun updateSearchQuery(query: String) {
        searchQuery = query
    }

    fun launchApp(app: AppInfo) {
        // Comando para Windows
        val command = listOf("cmd.exe", "/c", "start", "\"\"", "\"${app.path}\"")

        println("🚀 Lanzando: ${app.name}")
        println("📝 Comando: ${command.joinToString(" ")}")

        viewModelScope.launch(Dispatchers.IO) {
            try {
                ProcessBuilder(command).start()
            } catch (e: IOException) {
                println("❌ ERROR: No se pudo lanzar ${app.name}")
                println("Ruta: ${app.path}")
                e.printStackTrace()
            }
        }
    }

    fun addApp(app: AppInfo) {
        apps = apps + app
        println("✅ App añadida: ${app.name}")
    }

    fun removeApp(app: AppInfo) {
        if (app.isCustom) {
            apps = apps - app
            println("🗑️ App eliminada: ${app.name}")
        }
    }
}