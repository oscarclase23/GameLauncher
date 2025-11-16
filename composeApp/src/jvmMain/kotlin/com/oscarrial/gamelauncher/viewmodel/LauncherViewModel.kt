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

/**
 * Evento genérico para comunicar mensajes a la UI (errores, avisos, éxitos).
 */
data class UiEvent(
    val message: String,
    val isError: Boolean = false
)

/**
 * ViewModel principal del lanzador.
 * Gestiona:
 * - Carga de aplicaciones del sistema
 * - Búsqueda y filtrado
 * - Ejecución de aplicaciones según el sistema operativo
 * - Notificaciones a la interfaz mediante eventos
 */
class LauncherViewModel {

    // Alcance de corrutinas asociado al ViewModel.
    private val viewModelScope = CoroutineScope(Dispatchers.Main)

    // Estado principal de la lista de aplicaciones.
    var apps by mutableStateOf(emptyList<AppInfo>())
        private set

    // Valor actual del texto de búsqueda.
    var searchQuery by mutableStateOf("")
        private set

    // Indica si se está realizando el escaneo inicial.
    var isLoading by mutableStateOf(true)
        private set

    // Canal para enviar mensajes a la UI (snackbars).
    private val _uiEvents = Channel<UiEvent>(Channel.BUFFERED)
    val uiEvents = _uiEvents.receiveAsFlow()

    // Información del sistema operativo actual.
    val currentOSInfo: String = PlatformService.getOsNameWithVersion()

    // Total de aplicaciones cargadas.
    val totalAppsCount: Int
        get() = apps.size

    init {
        loadApps()
    }

    /**
     * Devuelve la lista de apps filtrada según la búsqueda.
     */
    val filteredApps: List<AppInfo>
        get() = apps.filter { it.name.contains(searchQuery, ignoreCase = true) }

    /**
     * Escanea el sistema en segundo plano y carga las aplicaciones detectadas.
     * Maneja errores y los notifica a la UI.
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
                apps = emptyList()

                _uiEvents.send(
                    UiEvent("Error al escanear aplicaciones: ${e.message}", isError = true)
                )

            } finally {
                isLoading = false
            }
        }
    }

    /**
     * Actualiza la consulta del buscador.
     */
    fun updateSearchQuery(query: String) {
        searchQuery = query
    }

    /**
     * Ejecuta una aplicación según el sistema operativo del usuario.
     * Construye el comando adecuado para Windows o Linux.
     */
    fun launchApp(app: AppInfo) {
        val os = PlatformService.getCurrentOS()

        val command: List<String> = when (os) {

            OperatingSystem.Windows -> {
                // Ejecutar mediante 'start' para abrir programas .exe y accesos directos.
                listOf("cmd.exe", "/c", "start", "\"\"", "\"${app.path}\"")
            }

            OperatingSystem.Linux -> {
                val path = app.path

                when {
                    path.startsWith("/") ->
                        listOf(path)  // Ruta directa a ejecutable

                    path.endsWith(".desktop") ->
                        listOf("gtk-launch", File(path).nameWithoutExtension)

                    else ->
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

        // Lanzamiento en hilo de fondo.
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val process = ProcessBuilder(command).start()
                val exitCode = process.waitFor()

                // Notificar si la aplicación devolvió un error.
                if (exitCode != 0) {
                    withContext(Dispatchers.Main) {
                        _uiEvents.send(
                            UiEvent("⚠️ ${app.name} terminó con un código de error: $exitCode", isError = true)
                        )
                    }
                }

            } catch (e: IOException) {
                println("❌ ERROR al lanzar ${app.name}: ${e.message}")

                withContext(Dispatchers.Main) {
                    _uiEvents.send(
                        UiEvent("❌ Error al lanzar ${app.name}: ${e.message}", isError = true)
                    )
                }

            } catch (e: Exception) {
                println("❌ ERROR inesperado: ${e.message}")

                withContext(Dispatchers.Main) {
                    _uiEvents.send(
                        UiEvent("❌ Error inesperado al lanzar ${app.name}", isError = true)
                    )
                }
            }
        }
    }

    /**
     * Añade una aplicación personalizada al listado.
     * Evita duplicados comparando las rutas.
     */
    fun addApp(app: AppInfo) {
        if (apps.any { it.path.equals(app.path, ignoreCase = true) }) {
            val message = "🚫 App ya existente: ${app.name}. No se añadió."
            println(message)

            viewModelScope.launch {
                _uiEvents.send(UiEvent(message, isError = true))
            }
            return
        }

        apps = apps + app
        println("✅ App añadida: ${app.name}")

        viewModelScope.launch {
            _uiEvents.send(UiEvent("✅ ${app.name} añadida correctamente"))
        }
    }

    /**
     * Elimina una aplicación añadida manualmente por el usuario.
     */
    fun removeApp(app: AppInfo) {
        if (app.isCustom) {
            apps = apps - app
            println("🗑️ App eliminada: ${app.name}")

            viewModelScope.launch {
                _uiEvents.send(UiEvent("🗑️ ${app.name} eliminada"))
            }
        }
    }
}
