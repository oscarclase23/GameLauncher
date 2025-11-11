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
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow

// NUEVO: Clase de datos para eventos de UI (mensajes y su tipo/color)
data class UiEvent(
    val message: String,
    val isError: Boolean = false
)

/**
 * ViewModel que gestiona el estado y la lógica de la pantalla del lanzador.
 * Soporta Windows y Linux (incluyendo Snap/Flatpak).
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

    // Channel para eventos de UI (SnackBar) - AHORA USA UiEvent
    private val _uiEvents = Channel<UiEvent>(Channel.BUFFERED)
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

                // MODIFICADO: Notificar al usuario del error
                _uiEvents.send(UiEvent("Error al escanear aplicaciones: ${e.message}", isError = true))

            } finally {
                isLoading = false
            }
        }
    }

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
                listOf("cmd.exe", "/c", "start", "\"\"", "\"${app.path}\"")
            }
            OperatingSystem.Linux -> {
                val path = app.path

                if (path.startsWith("/")) {
                    listOf(path)
                } else if (path.endsWith(".desktop")) {
                    listOf("gtk-launch", File(app.path).nameWithoutExtension)
                } else {
                    listOf("/bin/bash", "-c", path)
                }
            }
            else -> {
                println("❌ Sistema operativo no soportado")
                viewModelScope.launch {
                    _uiEvents.send(UiEvent("Sistema operativo no soportado", isError = true))
                }
                return
            }
        }

        println("🚀 Lanzando: ${app.name}")
        println("📝 Comando: ${command.joinToString(" ")}")

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val process = ProcessBuilder(command).start()

                // OPCIONAL: Esperar un momento para verificar si el proceso se inició
                val exitCode = process.waitFor()
                if (exitCode != 0) {
                    withContext(Dispatchers.Main) {
                        _uiEvents.send(UiEvent("⚠️ ${app.name} terminó con código de error: $exitCode", isError = true))
                    }
                }

            } catch (e: IOException) {
                println("❌ ERROR: No se pudo lanzar ${app.name}")
                println("Ruta: ${app.path}")
                e.printStackTrace()

                // MODIFICADO: Notificar al usuario del error
                withContext(Dispatchers.Main) {
                    _uiEvents.send(UiEvent("❌ Error al lanzar ${app.name}: ${e.message ?: "Ruta inválida"}", isError = true))
                }
            } catch (e: Exception) {
                println("❌ ERROR inesperado: ${e.message}")
                e.printStackTrace()

                withContext(Dispatchers.Main) {
                    _uiEvents.send(UiEvent("❌ Error inesperado al lanzar ${app.name}", isError = true))
                }
            }
        }
    }

    /**
     * Añade una aplicación manualmente.
     */
    fun addApp(app: AppInfo) {
        if (apps.any { it.path.equals(app.path, ignoreCase = true) }) {
            val message = "🚫 App ya existente: ${app.name}. No se añadió."
            println(message)
            viewModelScope.launch {
                // MODIFICADO: Usar color de error (isError=true)
                _uiEvents.send(UiEvent(message, isError = true))
            }
            return
        }
        apps = apps + app
        println("✅ App añadida: ${app.name}")

        // MODIFICADO: Notificar éxito al usuario (isError=false, usa el color por defecto/Primary)
        viewModelScope.launch {
            _uiEvents.send(UiEvent("✅ ${app.name} añadida correctamente", isError = false))
        }
    }

    /**
     * Elimina una aplicación personalizada.
     */
    fun removeApp(app: AppInfo) {
        if (app.isCustom) {
            apps = apps - app
            println("🗑️ App eliminada: ${app.name}")

            // MODIFICADO: Usar color de éxito/neutral (isError=false)
            viewModelScope.launch {
                _uiEvents.send(UiEvent("🗑️ ${app.name} eliminada", isError = false))
            }
        }
    }
}