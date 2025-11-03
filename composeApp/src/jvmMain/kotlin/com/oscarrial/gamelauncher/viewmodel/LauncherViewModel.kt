package com.oscarrial.gamelauncher.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.oscarrial.gamelauncher.data.AppInfo
import com.oscarrial.gamelauncher.system.AppScanner
import com.oscarrial.gamelauncher.system.OperatingSystem
import com.oscarrial.gamelauncher.system.PlatformService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException

/**
 * ViewModel que gestiona el estado y la lógica de la pantalla del lanzador.
 */
class LauncherViewModel {

    // 🔧 FIX: Usar Main como dispatcher principal
    private val viewModelScope = CoroutineScope(Dispatchers.Main)

    // ESTADOS
    var apps by mutableStateOf(emptyList<AppInfo>())
        private set

    var searchQuery by mutableStateOf("")
        private set

    var isLoading by mutableStateOf(true)
        private set

    init {
        loadApps()
    }

    // Lógica para filtrar las apps (getter)
    val filteredApps: List<AppInfo>
        get() = apps.filter { it.name.contains(searchQuery, ignoreCase = true) }

    /**
     * 🔧 FIX: Carga las aplicaciones ASÍNCRONAMENTE correctamente
     */
    private fun loadApps() {
        viewModelScope.launch {
            try {
                // Ejecutar el escaneo en el hilo de I/O
                val scannedApps = withContext(Dispatchers.IO) {
                    AppScanner.scanSystemApps(OperatingSystem.Windows)
                }

                // ✅ Actualización en el hilo Main (automático porque viewModelScope usa Main)
                apps = scannedApps
                println("✅ Apps cargadas correctamente. Total: ${apps.size}")

            } catch (e: Exception) {
                println("❌ ERROR en el escaneo: ${e.message}")
                e.printStackTrace()
                apps = emptyList()

            } finally {
                // Siempre desactivar el loading
                isLoading = false
            }
        }
    }

    // --- ACCIONES ---

    fun updateSearchQuery(query: String) {
        searchQuery = query
    }

    fun launchApp(app: AppInfo) {
        val os = PlatformService.getCurrentOS()

        val command: List<String> = if (os == OperatingSystem.Windows) {
            listOf("cmd.exe", "/c", "start", "\"\"", "\"${app.path}\"")
        } else {
            listOf("/bin/bash", "-c", app.path)
        }

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