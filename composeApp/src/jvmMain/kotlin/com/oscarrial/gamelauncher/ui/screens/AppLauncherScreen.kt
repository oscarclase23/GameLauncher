package com.oscarrial.gamelauncher.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.ScrollbarStyle


import com.oscarrial.gamelauncher.ui.theme.AppColors
import com.oscarrial.gamelauncher.viewmodel.LauncherViewModel
import com.oscarrial.gamelauncher.data.AppInfo
import com.oscarrial.gamelauncher.system.IconExtractor
import org.jetbrains.skia.Image as SkiaImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.swing.JFileChooser
import javax.swing.UIManager
import javax.swing.filechooser.FileNameExtensionFilter
import java.io.File

// Define el tamaño del icono a mostrar en la interfaz.
private val ICON_DISPLAY_SIZE = 64.dp
private val ICON_EXTRACTION_SIZE = 64

// Composable principal que construye la pantalla completa del lanzador de aplicaciones.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppLauncherScreen() {
    val viewModel = remember { LauncherViewModel() }
    val scope = rememberCoroutineScope()

    val snackbarHostState = remember { SnackbarHostState() }

    // Efecto lanzado que escucha los eventos del ViewModel para mostrar mensajes (SnackBar).
    LaunchedEffect(Unit) {
        // MODIFICADO: Recibir UiEvent en lugar de String
        viewModel.uiEvents.collect { event ->
            snackbarHostState.showSnackbar(
                message = event.message,
                duration = SnackbarDuration.Short,
                withDismissAction = true
            )
        }
    }

    val filteredApps = viewModel.filteredApps
    val searchQuery = viewModel.searchQuery
    val isLoading = viewModel.isLoading
    val totalAppsCount = viewModel.totalAppsCount
    val osName = viewModel.currentOSInfo

    // Estructura Scaffold (esqueleto) de la pantalla.
    Scaffold(
        snackbarHost = {
            // Define cómo se mostrarán los SnackBar (incluyendo la lógica de color por mensaje).
            // MODIFICADO: SnackbarHost para determinar el color
            SnackbarHost(snackbarHostState) { data ->
                // ELIMINAR ESTA LÍNEA QUE CAUSA EL ERROR:
                // val isError = viewModel.uiEvents.receiveAsFlow().collectAsState(initial = null).value?.isError ?: false

                Snackbar(
                    snackbarData = data,
                    // LÓGICA DE COLOR CORREGIDA: Usamos el mensaje para determinar el color.
                    // Esto asume que el ViewModel siempre incluye una palabra o emoji de error.
                    containerColor = if (data.visuals.message.contains("Error") || data.visuals.message.contains("🚫") || data.visuals.message.contains("❌")) {
                        AppColors.Error.copy(alpha = 0.9f)
                    } else {
                        AppColors.Primary.copy(alpha = 0.9f) // Color para éxito/información
                    },
                    contentColor = AppColors.OnPrimary,
                )
            }
        },
        containerColor = AppColors.Background,
        modifier = Modifier.fillMaxSize()
    ) { paddingValues ->

        // Columna principal que contiene el Header, SearchBar y la Lista de Apps.
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp)
        ) {

            // Header
            AppHeader(
                totalAppsCount = totalAppsCount,
                osName = osName,
                onAddAppClicked = {
                    scope.launch {
                        val selectedApp = openNativeFileExplorer()
                        if (selectedApp != null) {
                            viewModel.addApp(selectedApp)
                        }
                    }
                }
            )

            Spacer(Modifier.height(20.dp))

            // Barra de búsqueda
            SearchBar(
                query = searchQuery,
                onQueryChange = viewModel::updateSearchQuery
            )

            Spacer(Modifier.height(24.dp))

            // Control de visualización: Muestra loading, lista o vista vacía.
            if (isLoading) {
                LoadingView()
            } else if (filteredApps.isEmpty()) {
                EmptyView()
            } else {
                AppListHeader()
                Spacer(Modifier.height(8.dp))

                val listState = rememberLazyListState()

                // Contenedor para la lista de aplicaciones y su scrollbar.
                Box(modifier = Modifier.fillMaxSize()) {

                    // Lista de aplicaciones cargadas y filtradas.
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize().padding(end = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(
                            items = filteredApps,
                            key = { it.path }
                        ) { app ->
                            AppListItem(
                                app = app,
                                onLaunch = { viewModel.launchApp(app) },
                                onRemove = if (app.isCustom) {
                                    { viewModel.removeApp(app) }
                                } else null
                            )
                        }
                    }

                    // Barra de desplazamiento lateral personalizada.
// Barra de Desplazamiento Lateral (Scrollbar) con color blanco
                    VerticalScrollbar(
                        modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight(),
                        adapter = rememberScrollbarAdapter(listState),
                        style = ScrollbarStyle(
                            minimalHeight = 16.dp,
                            thickness = 8.dp,
                            shape = RoundedCornerShape(4.dp), // CORRECCIÓN 1: Se pasa el valor DP directamente.
                            // CORRECCIÓN 2: Usar hoverColor y unhoverColor (con C mayúscula) y añadir hoverDurationMillis
                            hoverColor = Color.White, // Color al pasar el ratón (Blanco sólido)
                            unhoverColor = Color.White.copy(alpha = 0.7f), // Blanco semitransparente
                            hoverDurationMillis = 500 // Tiempo de animación al pasar el ratón (500ms)
                        )
                    )
                } // Fin Box
            }
        }
    } // Fin Scaffold
}

// ============================================================================
// HEADER
// ============================================================================

// Componente Composable para la cabecera de la aplicación.
@Composable
fun AppHeader(
    totalAppsCount: Int,
    osName: String,
    onAddAppClicked: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        // Fila superior con el título y el botón "Añadir App".
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "🎮 Lanzador de Aplicaciones",
                style = MaterialTheme.typography.headlineMedium,
                color = AppColors.OnBackground
            )

            Button(
                onClick = onAddAppClicked,
                colors = ButtonDefaults.buttonColors(AppColors.Primary),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("➕ Añadir App", color = AppColors.OnPrimary)
            }
        }

        Spacer(Modifier.height(8.dp))

        // Fila inferior con el contador de aplicaciones y la información del SO.
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Total de Apps Encontradas: $totalAppsCount",
                style = MaterialTheme.typography.titleSmall,
                color = AppColors.OnSurface.copy(alpha = 0.8f)
            )

            Text(
                text = "Sistema Operativo: $osName",
                style = MaterialTheme.typography.titleSmall,
                color = AppColors.OnSurface.copy(alpha = 0.8f)
            )
        }
    }
}

// ============================================================================
// EXPLORADOR DE ARCHIVOS MULTIPLATAFORMA
// ============================================================================

/**
 * Abre el explorador de archivos nativo del sistema para que el usuario seleccione una aplicación.
 * Se ejecuta en un contexto de corrutina (Dispatchers.IO).
 */
private suspend fun openNativeFileExplorer(): AppInfo? = withContext(Dispatchers.IO) {
    try {
        // Configuración para usar la apariencia nativa del sistema operativo.
        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName())

        val osName = System.getProperty("os.name", "").lowercase()
        val isWindows = osName.contains("win")
        val isLinux = osName.contains("nux") || osName.contains("nix")

        // Inicializa y configura el selector de archivos (JFileChooser).
        val fileChooser = JFileChooser().apply {
            dialogTitle = "Seleccionar aplicación ejecutable o .desktop"
            fileSelectionMode = JFileChooser.FILES_ONLY

            // Configura la carpeta inicial según el SO.
            currentDirectory = when {
                isWindows -> File("C:\\Program Files")
                isLinux -> File(System.getProperty("user.home"))
                else -> File(System.getProperty("user.home"))
            }

            // Aplica filtros de archivo específicos para Windows (.exe) y Linux (.desktop).
            fileFilter = when {
                isWindows -> FileNameExtensionFilter("Archivos ejecutables (*.exe)", "exe")
                isLinux -> object : javax.swing.filechooser.FileFilter() {
                    override fun accept(f: File): Boolean {
                        return f.isDirectory || f.name.endsWith(".desktop")
                    }
                    override fun getDescription(): String {
                        return "Aplicaciones (*.desktop)"
                    }
                }
                else -> fileFilter
            }

            isAcceptAllFileFilterUsed = false
            isMultiSelectionEnabled = false
        }

        val result = fileChooser.showOpenDialog(null)

        // Procesa la selección del usuario.
        if (result == JFileChooser.APPROVE_OPTION) {
            val selectedFile = fileChooser.selectedFile

            // Verifica si el archivo es válido para el SO.
            val isValid = when {
                isWindows -> selectedFile.exists() && selectedFile.name.endsWith(".exe", ignoreCase = true)
                isLinux -> selectedFile.exists() && selectedFile.name.endsWith(".desktop")
                else -> false
            }

            if (!isValid) {
                println("❌ El archivo seleccionado no es válido")
                return@withContext null
            }

            // --- PROCESAMIENTO DE ARCHIVO ---
            when {
                // Lógica específica para archivos .desktop (Linux).
                selectedFile.name.endsWith(".desktop") -> {
                    // Lógica para Linux: Parsear .desktop
                    val desktopInfo = parseDesktopFileForManualAdd(selectedFile)
                    if (desktopInfo == null) {
                        println("❌ No se pudo parsear el archivo .desktop")
                        return@withContext null
                    }
                    val iconBytes = if (!desktopInfo.third.isNullOrBlank()) {
                        IconExtractor.extractLinuxIcon(desktopInfo.third!!)
                    } else null

                    // Construye el objeto AppInfo a partir de los datos parseados.
                    AppInfo(
                        name = desktopInfo.first,
                        path = desktopInfo.second,
                        icon = "🎮",
                        isCustom = true,
                        description = "Añadida manualmente (Linux)",
                        iconBytes = iconBytes
                    )
                }
                // Lógica específica para ejecutables .exe (Windows).
                isWindows -> {
                    // Lógica para Windows: Procesar .exe

                    // Genera un nombre de aplicación limpio.
                    val appName = selectedFile.nameWithoutExtension
                        .replace("-", " ")
                        .replace("_", " ")
                        .split(" ")
                        .joinToString(" ") { word -> word.replaceFirstChar { it.uppercase() } }

                    // Extrae el icono del ejecutable.
                    // Usar el tamaño constante 64px para la extracción
                    val iconBytes = IconExtractor.extractIconAsBytes(selectedFile.absolutePath, size = ICON_EXTRACTION_SIZE)

                    // Construye el objeto AppInfo.
                    AppInfo(
                        name = appName,
                        path = selectedFile.absolutePath,
                        icon = "🎮",
                        isCustom = true,
                        description = "Añadida manualmente (Windows)",
                        iconBytes = iconBytes
                    )
                }
                else -> null
            }
        } else {
            // El usuario canceló la selección.
            println("🚫 Selección cancelada por el usuario")
            null
        }
    } catch (e: Exception) {
        // Manejo de errores durante la apertura del explorador.
        println("❌ Error abriendo explorador de archivos: ${e.message}")
        e.printStackTrace()
        null
    }
}

/**
 * Función auxiliar para leer y extraer el Nombre, Ejecutable (Exec) y Icono de un archivo .desktop.
 */
private fun parseDesktopFileForManualAdd(desktopFile: File): Triple<String, String, String?>? {
    try {
        var name: String? = null
        var exec: String? = null
        var iconName: String? = null

        // Se recorre el archivo y se extrae el valor del último Name, Exec e Icon
        desktopFile.readLines().forEach { line ->
            val trimmedLine = line.trim()
            when {
                // Se asegura de que Name sólo se tome del grupo [Desktop Entry] (el primero)
                trimmedLine.startsWith("Name=") && name == null -> name = trimmedLine.substringAfter("Name=")
                trimmedLine.startsWith("Exec=") -> exec = trimmedLine.substringAfter("Exec=")
                trimmedLine.startsWith("Icon=") -> iconName = trimmedLine.substringAfter("Icon=")
            }
        }

        if (name.isNullOrBlank() || exec.isNullOrBlank()) {
            println("❌ Error de parseo: Name o Exec no encontrados en el .desktop.")
            return null
        }

        // Limpia el comando de ejecución (Exec) de argumentos (%f, %u, etc.).
        val cleanExec = exec!!
            .replace(Regex("%[a-zA-Z]"), "") // Limpiar códigos como %f, %u, etc.
            .trim()
            .split(" ")
            .firstOrNull() ?: return null

        // --- CORRECCIÓN: Lógica de resolución de la ruta con fallback ---
        // Intenta obtener la ruta absoluta y canónica del ejecutable.
        val executablePath = resolveExecutablePathFromCommand(cleanExec)

        // Si la resolución falla (executablePath es nulo), usamos el comando limpio original (cleanExec)
        // como fallback, permitiendo que la AppInfo se cree y el ViewModel intente lanzarlo.
        val finalPath = if (executablePath.isNullOrBlank()) {
            println("⚠️ Advertencia: No se pudo resolver la ruta absoluta ejecutable para: '$cleanExec'. Usando el comando original como fallback.")
            cleanExec
        } else {
            executablePath
        }
        // --- FIN de la corrección ---

        // Devuelve el nombre, la ruta final y el nombre del icono.
        return Triple(name!!, finalPath, iconName)

    } catch (e: Exception) {
        println("❌ Error grave parseando .desktop: ${e.message}")
        return null
    }
}

/**
 * Resuelve la ruta completa y canónica de un ejecutable de Linux buscando en el PATH del sistema.
 */
private fun resolveExecutablePathFromCommand(command: String): String? {
    // Lista de ubicaciones a verificar, incluyendo el comando directo si es una ruta absoluta
    val searchFiles = mutableListOf<File>()

    if (command.startsWith("/")) {
        // Si es una ruta absoluta, se añade como primer candidato
        searchFiles.add(File(command))
    }

    // Busca el comando dentro de las carpetas definidas en la variable de entorno PATH.
    // Buscar en el PATH si es solo un comando (ej: "vim")
    val pathEnv = System.getenv("PATH") ?: ""
    for (path in pathEnv.split(":")) {
        searchFiles.add(File(path, command))
    }

    // Itera sobre las posibles rutas hasta encontrar un archivo existente y ejecutable.
    for (file in searchFiles) {
        if (file.exists() && file.canExecute()) {
            return try {
                // Intentar obtener la ruta canónica para resolver symlinks
                file.canonicalPath
            } catch (e: Exception) {
                // Si falla (IOException al resolver), volvemos a la ruta absoluta
                file.absolutePath
            }
        }
    }

    // Si no se pudo encontrar o resolver una ruta ejecutable existente, descartamos.
    return null
}

// ============================================================================
// COMPONENTES DE UI
// ============================================================================

// Composable que define la fila de encabezado de la lista de aplicaciones.
@Composable
fun AppListHeader() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = AppColors.Surface,
        tonalElevation = 2.dp
    ) {
        // Define las columnas: Icono, Nombre, Ruta de instalación y Acciones.
        Row(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.width(100.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "Icono",
                    style = MaterialTheme.typography.labelLarge,
                    color = AppColors.OnSurface.copy(alpha = 0.7f)
                )
            }

            Box(modifier = Modifier.weight(0.3f).padding(start = 12.dp)) {
                Text(
                    "Nombre",
                    style = MaterialTheme.typography.labelLarge,
                    color = AppColors.OnSurface.copy(alpha = 0.7f)
                )
            }

            Box(modifier = Modifier.weight(0.5f).padding(start = 12.dp)) {
                Text(
                    "Ruta de instalación",
                    style = MaterialTheme.typography.labelLarge,
                    color = AppColors.OnSurface.copy(alpha = 0.7f)
                )
            }

            Box(modifier = Modifier.width(160.dp)) {
                // Espacio para botones
            }
        }
    }
}

// Composable que representa una única fila de aplicación en la lista.
@Composable
fun AppListItem(
    app: AppInfo,
    onLaunch: () -> Unit,
    onRemove: (() -> Unit)? = null
) {
    val interactionSource = remember { MutableInteractionSource() }
    // Detecta si el ratón está sobre el elemento para cambiar su estilo visual.
    val hovered by interactionSource.collectIsHoveredAsState()

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .hoverable(interactionSource),
        shape = RoundedCornerShape(12.dp),
        // Cambia el color si el ratón está encima.
        color = if (hovered) AppColors.Surface.copy(alpha = 0.8f) else AppColors.Surface,
        tonalElevation = if (hovered) 4.dp else 2.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .width(100.dp)
                    .height(80.dp),
                contentAlignment = Alignment.Center
            ) {
                // Muestra el icono de la aplicación.
                // Usar AppIcon directamente, que maneja el fallback
                AppIcon(iconBytes = app.iconBytes, size = ICON_EXTRACTION_SIZE)
            }

            // Muestra el nombre de la aplicación.
            Text(
                text = app.name,
                modifier = Modifier.weight(0.3f).padding(start = 12.dp),
                style = MaterialTheme.typography.titleMedium,
                color = AppColors.OnSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            // Muestra la ruta de instalación.
            Text(
                text = app.path,
                modifier = Modifier.weight(0.5f).padding(start = 12.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = AppColors.OnSurface.copy(alpha = 0.6f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            // Contenedor para los botones de acción (Lanzar y Eliminar).
            Row(
                modifier = Modifier.width(160.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Botón para lanzar la aplicación.
                Button(
                    onClick = onLaunch,
                    colors = ButtonDefaults.buttonColors(AppColors.Primary),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text("▶ Lanzar", style = MaterialTheme.typography.labelMedium)
                }

                // Muestra el botón de eliminar solo para apps personalizadas.
                if (onRemove != null) {
                    IconButton(
                        onClick = onRemove,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Text("🗑️", style = MaterialTheme.typography.titleMedium)
                    }
                }
            }
        }
    }
}

// Componente Composable encargado de cargar y mostrar el icono de la aplicación.
@Composable
fun AppIcon(iconBytes: ByteArray?, size: Int = ICON_EXTRACTION_SIZE) {
    // Recuerda y convierte los bytes del icono a un ImageBitmap.
    val imageBitmap = remember(iconBytes) {
        if (iconBytes == null) return@remember null

        try {
            val skiaImage = SkiaImage.makeFromEncoded(iconBytes)
            skiaImage.toComposeImageBitmap()
        } catch (e: Exception) {
            println("Error cargando icono: ${e.message}")
            null
        }
    }

    // Muestra el icono cargado o un fallback si la carga falla.
    if (imageBitmap != null) {
        Image(
            painter = BitmapPainter(imageBitmap),
            contentDescription = "App Icon",
            modifier = Modifier
                .size(size.dp)
                .clip(RoundedCornerShape(8.dp)),
            contentScale = androidx.compose.ui.layout.ContentScale.Fit
        )
    } else {
        // Vista de fallback (icono de interrogación).
        Box(
            modifier = Modifier
                .size(size.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(AppColors.SurfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "❓",
                style = MaterialTheme.typography.titleLarge,
                color = AppColors.Error
            )
        }
    }
}

// Composable que implementa la barra de búsqueda.
@Composable
fun SearchBar(query: String, onQueryChange: (String) -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = AppColors.SurfaceVariant
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("🔍", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.width(12.dp))
            // Campo de texto básico para la entrada de búsqueda.
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                modifier = Modifier.weight(1f),
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = AppColors.OnSurface),
                singleLine = true,
                cursorBrush = SolidColor(AppColors.Primary),
                // Muestra un texto de marcador de posición si la entrada está vacía.
                decorationBox = { inner ->
                    if (query.isEmpty()) {
                        Text(
                            "Buscar aplicaciones...",
                            color = AppColors.OnSurface.copy(alpha = 0.5f)
                        )
                    }
                    inner()
                }
            )
            // Botón para borrar la búsqueda si hay texto.
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Text("❌")
                }
            }
        }
    }
}

// Vista que se muestra cuando no hay aplicaciones o la búsqueda no arroja resultados.
@Composable
fun EmptyView() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("🔍", style = MaterialTheme.typography.displayLarge)
            Spacer(Modifier.height(8.dp))
            Text(
                "No se encontraron aplicaciones",
                color = AppColors.OnSurface,
                style = MaterialTheme.typography.titleLarge
            )
            Text(
                "Intenta otra búsqueda o agrega una nueva manualmente.",
                color = AppColors.OnSurface.copy(alpha = 0.7f),
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

// Vista que se muestra mientras se escanean y cargan las aplicaciones iniciales.
@Composable
fun LoadingView() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            // Indicador de progreso circular.
            CircularProgressIndicator(color = AppColors.Primary)
            Spacer(Modifier.height(16.dp))
            Text(
                "Escaneando aplicaciones y extrayendo iconos de alta calidad...",
                color = AppColors.OnSurface,
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}