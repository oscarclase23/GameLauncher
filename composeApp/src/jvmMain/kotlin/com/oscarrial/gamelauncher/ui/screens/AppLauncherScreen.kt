package com.oscarrial.gamelauncher.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppLauncherScreen() {
    val viewModel = remember { LauncherViewModel() }
    val scope = rememberCoroutineScope()

    val filteredApps = viewModel.filteredApps
    val searchQuery = viewModel.searchQuery
    val isLoading = viewModel.isLoading
    val totalAppsCount = viewModel.totalAppsCount
    val osName = viewModel.currentOSInfo

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.Background)
            .padding(24.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

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

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
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
            }
        }
    }
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
 * Soporta Windows (.exe) y Linux (.desktop)
 */
private suspend fun openNativeFileExplorer(): AppInfo? = withContext(Dispatchers.IO) {
    try {
        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName())

        val osName = System.getProperty("os.name", "").lowercase()
        val isWindows = osName.contains("win")
        val isLinux = osName.contains("nux") || osName.contains("nix")

        val fileChooser = JFileChooser().apply {
            dialogTitle = "Seleccionar aplicación ejecutable"
            fileSelectionMode = JFileChooser.FILES_ONLY

            // Carpeta inicial según el SO
            currentDirectory = when {
                isWindows -> File("C:\\Program Files")
                isLinux -> File("/usr/share/applications")
                else -> File(System.getProperty("user.home"))
            }

            // Filtros según el SO
            if (isWindows) {
                fileFilter = FileNameExtensionFilter(
                    "Archivos ejecutables (*.exe)",
                    "exe"
                )
            } else if (isLinux) {
                fileFilter = object : javax.swing.filechooser.FileFilter() {
                    override fun accept(f: File): Boolean {
                        return f.isDirectory || f.name.endsWith(".desktop")
                    }
                    override fun getDescription(): String {
                        return "Aplicaciones de Linux (*.desktop)"
                    }
                }
            }

            isAcceptAllFileFilterUsed = false
            isMultiSelectionEnabled = false
        }

        val result = fileChooser.showOpenDialog(null)

        if (result == JFileChooser.APPROVE_OPTION) {
            val selectedFile = fileChooser.selectedFile

            // Validar según el SO
            val isValid = when {
                isWindows -> selectedFile.exists() && selectedFile.name.endsWith(".exe", ignoreCase = true)
                isLinux -> selectedFile.exists() && selectedFile.name.endsWith(".desktop")
                else -> false
            }

            if (!isValid) {
                println("❌ El archivo seleccionado no es válido")
                return@withContext null
            }

            // Procesar según el tipo de archivo
            when {
                selectedFile.name.endsWith(".desktop") -> {
                    // Parsear archivo .desktop de Linux
                    val desktopInfo = parseDesktopFileForManualAdd(selectedFile)
                    if (desktopInfo == null) {
                        println("❌ No se pudo parsear el archivo .desktop")
                        return@withContext null
                    }

                    println("📂 Archivo seleccionado: ${selectedFile.name}")
                    println("✅ Aplicación añadida: ${desktopInfo.first}")

                    AppInfo(
                        name = desktopInfo.first,
                        path = desktopInfo.second,
                        icon = "🎮",
                        isCustom = true,
                        description = "Añadida manualmente",
                        iconBytes = desktopInfo.third
                    )
                }
                isWindows -> {
                    // Procesar .exe de Windows
                    val appName = selectedFile.nameWithoutExtension
                        .replace("-", " ")
                        .replace("_", " ")
                        .split(" ")
                        .joinToString(" ") { word -> word.replaceFirstChar { it.uppercase() } }

                    println("📂 Archivo seleccionado: ${selectedFile.name}")
                    println("🎯 Extrayendo icono de alta calidad...")

                    val iconBytes = IconExtractor.extractIconAsBytes(selectedFile.absolutePath, size = 128)

                    if (iconBytes != null) {
                        println("✅ Icono extraído correctamente (${iconBytes.size} bytes)")
                    } else {
                        println("⚠️ No se pudo extraer el icono, se usará fallback")
                    }

                    AppInfo(
                        name = appName,
                        path = selectedFile.absolutePath,
                        icon = "🎮",
                        isCustom = true,
                        description = "Añadida manualmente",
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
 * Parsea un archivo .desktop seleccionado manualmente.
 */
private fun parseDesktopFileForManualAdd(desktopFile: File): Triple<String, String, ByteArray?>? {
    try {
        var name: String? = null
        var exec: String? = null
        var iconName: String? = null

        desktopFile.readLines().forEach { line ->
            val trimmedLine = line.trim()
            when {
                trimmedLine.startsWith("Name=") && name == null -> {
                    name = trimmedLine.substringAfter("Name=")
                }
                trimmedLine.startsWith("Exec=") -> {
                    exec = trimmedLine.substringAfter("Exec=")
                }
                trimmedLine.startsWith("Icon=") -> {
                    iconName = trimmedLine.substringAfter("Icon=")
                }
            }
        }

        if (name.isNullOrBlank() || exec.isNullOrBlank()) {
            return null
        }

        val cleanExec = exec!!
            .replace(Regex("%[a-zA-Z]"), "")
            .trim()
            .split(" ")
            .firstOrNull() ?: return null

        val iconBytes = if (!iconName.isNullOrBlank()) {
            IconExtractor.extractLinuxIcon(iconName!!)
        } else {
            null
        }

        return Triple(name!!, cleanExec, iconBytes)

    } catch (e: Exception) {
        println("Error parseando .desktop: ${e.message}")
        return null
    }
}

// ============================================================================
// COMPONENTES DE UI (SIN CAMBIOS)
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
                if (app.iconBytes != null) {
                    AppIcon(iconBytes = app.iconBytes, size = 80)
                } else {
                    Text(
                        text = app.icon,
                        style = MaterialTheme.typography.displayLarge
                    )
                }
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
fun AppIcon(iconBytes: ByteArray, size: Int = 80) {
    val imageBitmap = remember(iconBytes) {
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
            modifier = Modifier.clip(RoundedCornerShape(8.dp)),
            contentScale = androidx.compose.ui.layout.ContentScale.None
        )
    } else {
        Box(
            modifier = Modifier
                .size(size.dp)
                .background(AppColors.SurfaceVariant, RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text("🎮", style = MaterialTheme.typography.titleLarge)
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