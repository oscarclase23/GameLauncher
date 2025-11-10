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

// Tamaño del icono en Compose (basado en el tamaño de extracción de 64px)
private val ICON_DISPLAY_SIZE = 64.dp
private val ICON_EXTRACTION_SIZE = 64

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppLauncherScreen() {
    val viewModel = remember { LauncherViewModel() }
    val scope = rememberCoroutineScope()

    // NUEVO: Estado para el SnackbarHost
    val snackbarHostState = remember { SnackbarHostState() }

    // NUEVO: Colectar eventos del ViewModel para mostrar el Snackbar (Toast)
    LaunchedEffect(Unit) {
        viewModel.uiEvents.collect { message ->
            snackbarHostState.showSnackbar(
                message = message,
                duration = SnackbarDuration.Short, // Mostrar por poco tiempo
                withDismissAction = true // Permitir al usuario cerrarlo
            )
        }
    }

    val filteredApps = viewModel.filteredApps
    val searchQuery = viewModel.searchQuery
    val isLoading = viewModel.isLoading
    val totalAppsCount = viewModel.totalAppsCount
    val osName = viewModel.currentOSInfo

    Scaffold( // <-- Usar Scaffold para añadir SnackbarHost
        snackbarHost = {
            SnackbarHost(snackbarHostState) { data ->
                Snackbar(
                    snackbarData = data,
                    containerColor = AppColors.Error.copy(alpha = 0.9f), // Usar color de error para notificación
                    contentColor = AppColors.OnPrimary,
                )
            }
        },
        containerColor = AppColors.Background, // Fondo oscuro
        modifier = Modifier.fillMaxSize()
    ) { paddingValues ->

        Column(
            // Aplicar el padding del Scaffold y el padding de diseño original de 24.dp
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

            // Lista de aplicaciones
            if (isLoading) {
                LoadingView()
            } else if (filteredApps.isEmpty()) {
                EmptyView()
            } else {
                AppListHeader()
                Spacer(Modifier.height(8.dp))

                val listState = rememberLazyListState() // Estado para controlar el scroll

                Box(modifier = Modifier.fillMaxSize()) { // Box para contener la lista y el scrollbar

                    LazyColumn(
                        state = listState, // Asignar estado
                        modifier = Modifier.fillMaxSize().padding(end = 12.dp), // Añadir padding para que el scrollbar no tape el contenido
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

                    // Barra de Desplazamiento Lateral (Scrollbar)
                    VerticalScrollbar(
                        modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight(),
                        adapter = rememberScrollbarAdapter(listState),
                    )
                } // Fin Box
            }
        }
    } // Fin Scaffold
}

// ============================================================================
// HEADER
// ============================================================================

@Composable
fun AppHeader(
    totalAppsCount: Int,
    osName: String,
    onAddAppClicked: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
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
 * Abre el explorador de archivos nativo del sistema.
 */
private suspend fun openNativeFileExplorer(): AppInfo? = withContext(Dispatchers.IO) {
    try {
        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName())

        val osName = System.getProperty("os.name", "").lowercase()
        val isWindows = osName.contains("win")
        val isLinux = osName.contains("nux") || osName.contains("nix")

        val fileChooser = JFileChooser().apply {
            dialogTitle = "Seleccionar aplicación ejecutable o .desktop"
            fileSelectionMode = JFileChooser.FILES_ONLY

            // Carpeta inicial según el SO
            currentDirectory = when {
                isWindows -> File("C:\\Program Files")
                isLinux -> File(System.getProperty("user.home"))
                else -> File(System.getProperty("user.home"))
            }

            // Filtros según el SO
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

        if (result == JFileChooser.APPROVE_OPTION) {
            val selectedFile = fileChooser.selectedFile

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

                    AppInfo(
                        name = desktopInfo.first,
                        path = desktopInfo.second,
                        icon = "🎮",
                        isCustom = true,
                        description = "Añadida manualmente (Linux)",
                        iconBytes = iconBytes
                    )
                }
                isWindows -> {
                    // Lógica para Windows: Procesar .exe
                    val appName = selectedFile.nameWithoutExtension
                        .replace("-", " ")
                        .replace("_", " ")
                        .split(" ")
                        .joinToString(" ") { word -> word.replaceFirstChar { it.uppercase() } }

                    // Usar el tamaño constante 64px para la extracción
                    val iconBytes = IconExtractor.extractIconAsBytes(selectedFile.absolutePath, size = ICON_EXTRACTION_SIZE)

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
            println("🚫 Selección cancelada por el usuario")
            null
        }
    } catch (e: Exception) {
        println("❌ Error abriendo explorador de archivos: ${e.message}")
        e.printStackTrace()
        null
    }
}

/**
 * Parsea un archivo .desktop seleccionado manualmente para obtener el nombre, la ruta de ejecución y el icono.
 * Incluye la lógica de resolución de ruta con fallback.
 * Devuelve Triple<Name, ExecutablePath, IconName>.
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

        val cleanExec = exec!!
            .replace(Regex("%[a-zA-Z]"), "") // Limpiar códigos como %f, %u, etc.
            .trim()
            .split(" ")
            .firstOrNull() ?: return null

        // --- CORRECCIÓN: Lógica de resolución de la ruta con fallback ---
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

        return Triple(name!!, finalPath, iconName)

    } catch (e: Exception) {
        println("❌ Error grave parseando .desktop: ${e.message}")
        return null
    }
}

/**
 * Resuelve la ruta completa y canónica de un ejecutable de Linux buscando en el PATH.
 * Retorna NULL si no puede resolver una ruta ejecutable existente.
 * (Copia de la lógica de AppScanner.kt para uso local)
 */
private fun resolveExecutablePathFromCommand(command: String): String? {
    // Lista de ubicaciones a verificar, incluyendo el comando directo si es una ruta absoluta
    val searchFiles = mutableListOf<File>()

    if (command.startsWith("/")) {
        // Si es una ruta absoluta, se añade como primer candidato
        searchFiles.add(File(command))
    }

    // Buscar en el PATH si es solo un comando (ej: "vim")
    val pathEnv = System.getenv("PATH") ?: ""
    for (path in pathEnv.split(":")) {
        searchFiles.add(File(path, command))
    }

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

@Composable
fun AppListHeader() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = AppColors.Surface,
        tonalElevation = 2.dp
    ) {
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

@Composable
fun AppListItem(
    app: AppInfo,
    onLaunch: () -> Unit,
    onRemove: (() -> Unit)? = null
) {
    val interactionSource = remember { MutableInteractionSource() }
    val hovered by interactionSource.collectIsHoveredAsState()

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .hoverable(interactionSource),
        shape = RoundedCornerShape(12.dp),
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
                // Usar AppIcon directamente, que maneja el fallback
                AppIcon(iconBytes = app.iconBytes, size = ICON_EXTRACTION_SIZE)
            }

            Text(
                text = app.name,
                modifier = Modifier.weight(0.3f).padding(start = 12.dp),
                style = MaterialTheme.typography.titleMedium,
                color = AppColors.OnSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = app.path,
                modifier = Modifier.weight(0.5f).padding(start = 12.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = AppColors.OnSurface.copy(alpha = 0.6f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Row(
                modifier = Modifier.width(160.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = onLaunch,
                    colors = ButtonDefaults.buttonColors(AppColors.Primary),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text("▶ Lanzar", style = MaterialTheme.typography.labelMedium)
                }

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

@Composable
fun AppIcon(iconBytes: ByteArray?, size: Int = ICON_EXTRACTION_SIZE) {
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

    if (imageBitmap != null) {
        Image(
            painter = BitmapPainter(imageBitmap),
            contentDescription = "App Icon",
            modifier = Modifier
                .size(size.dp) // Asegura que se dibuje al tamaño correcto (64dp)
                .clip(RoundedCornerShape(8.dp)),
            contentScale = androidx.compose.ui.layout.ContentScale.Fit
        )
    } else {
        // FALLBACK: Icono predeterminado (❓) con color de error
        Box(
            modifier = Modifier
                .size(size.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(AppColors.SurfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "❓", // Icono de interrogación
                style = MaterialTheme.typography.titleLarge,
                color = AppColors.Error // Color de error para alta visibilidad
            )
        }
    }
}

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
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                modifier = Modifier.weight(1f),
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = AppColors.OnSurface),
                singleLine = true,
                cursorBrush = SolidColor(AppColors.Primary),
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
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Text("❌")
                }
            }
        }
    }
}

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

@Composable
fun LoadingView() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
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