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
import java.io.File
import java.io.IOException
// NUEVAS IMPORTACIONES PARA EVENTOS DE UI (SNACKBAR)
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow

/**
 * ViewModel que gestiona el estado y la lógica de la pantalla del lanzador.
 * Soporta Windows y Linux.
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

    // PROPIEDADES NUEVAS: Channel para eventos de UI (como SnackBar)
    private val _uiEvents = Channel<String>(Channel.BUFFERED)
    val uiEvents = _uiEvents.receiveAsFlow()

    // Propiedades computadas
    val totalAppsCount: Int
        get() = apps.size

    val currentOSInfo: String = PlatformService.getOsNameWithVersion()

    init {
        loadApps()
    }

    // Aplicaciones filtradas por búsqueda
    val filteredApps: List<AppInfo>
        get() = apps.filter { it.name.contains(searchQuery, ignoreCase = true) }

    /**
     * Carga las aplicaciones del sistema asíncronamente
     */
    private fun loadApps() {
        viewModelScope.launch {
            try {
                val scannedApps = withContext(Dispatchers.IO) {
                    AppScanner.scanSystemApps()
                }

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

    /**
     * Lanza una aplicación según el sistema operativo.
     */
    fun launchApp(app: AppInfo) {
        val os = PlatformService.getCurrentOS()

        val command: List<String> = when (os) {
            OperatingSystem.Windows -> {
                // Windows: usar cmd.exe /c start
                listOf("cmd.exe", "/c", "start", "\"\"", "\"${app.path}\"")
            }
            OperatingSystem.Linux -> {
                val path = app.path

                // Usamos bash -c si la ruta NO es absoluta (es decir, es un comando en el PATH, como 'vim').
                // Si la ruta absoluta fue resuelta por parseDesktopFileForManualAdd, se ejecuta directamente.
                if (path.startsWith("/")) {
                    // Ruta absoluta (e.g., /usr/bin/vim). Ejecutar directamente.
                    listOf(path)
                } else if (path.endsWith(".desktop")) {
                    // Si la ruta original del .desktop se pasó como fallback (en caso de que el 'Exec=' no fuera resoluble),
                    // usamos gtk-launch o fallamos, aunque la lógica del parseo manual debería evitar esto.
                    listOf("gtk-launch", File(app.path).nameWithoutExtension)
                } else {
                    // Comando simple (e.g., vim). Ejecutar a través de bash -c.
                    listOf("/bin/bash", "-c", path)
                }
            }
            else -> {
                println("❌ Sistema operativo no soportado")
                return
            }
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
        if (apps.any { it.path.equals(app.path, ignoreCase = true) }) {
            val message = "🚫 App ya existente: ${app.name}. No se añadió."
            println(message)
            viewModelScope.launch {
                _uiEvents.send(message) // <-- ENVIAR EVENTO DE DUPLICADO A LA UI
            }
            return
        }
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