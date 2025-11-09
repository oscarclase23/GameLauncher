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

    // Obtener los datos del ViewModel
    val totalAppsCount = viewModel.totalAppsCount // Propiedad que se actualiza automáticamente
    val osName = viewModel.currentOSInfo

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.Background)
            .padding(24.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

            // --- HEADER MODIFICADO ---
            AppHeader(
                totalAppsCount = totalAppsCount,
                osName = osName,
                onAddAppClicked = {
                    scope.launch {
                        val selectedApp = openNativeWindowsFileExplorer()
                        if (selectedApp != null) {
                            viewModel.addApp(selectedApp)
                        }
                    }
                }
            )
            // -------------------------

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
// NUEVO COMPONENTE: HEADER (Contador y SO)
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

            // Botón Añadir App
            Button(
                onClick = onAddAppClicked,
                colors = ButtonDefaults.buttonColors(AppColors.Primary),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("➕ Añadir App", color = AppColors.OnPrimary)
            }
        }

        Spacer(Modifier.height(8.dp))

        // Fila para el contador y el SO
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Contador de aplicaciones
            Text(
                text = "Total de Apps Encontradas: $totalAppsCount",
                style = MaterialTheme.typography.titleSmall,
                color = AppColors.OnSurface.copy(alpha = 0.8f)
            )

            // Información del sistema operativo
            Text(
                text = "Sistema Operativo: $osName",
                style = MaterialTheme.typography.titleSmall,
                color = AppColors.OnSurface.copy(alpha = 0.8f)
            )
        }
    }
}


// ============================================================================
// FUNCIONES DE DIÁLOGO Y COMPONENTES DE UI (RESTO DEL ARCHIVO SIN CAMBIOS)
// ============================================================================

/**
 * Abre el explorador de archivos NATIVO de Windows y devuelve un AppInfo si se selecciona un .exe válido.
 */
private suspend fun openNativeWindowsFileExplorer(): AppInfo? = withContext(Dispatchers.IO) {
    try {
        // CRÍTICO: Forzar el uso del Look and Feel nativo de Windows
        // Esto hace que JFileChooser use el explorador de archivos nativo del sistema
        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName())

        val fileChooser = JFileChooser().apply {
            dialogTitle = "Seleccionar aplicación ejecutable"
            fileSelectionMode = JFileChooser.FILES_ONLY

            // Iniciar en la carpeta de Program Files (ubicación común de aplicaciones)
            currentDirectory = File("C:\\Program Files")

            // Filtro para mostrar SOLO archivos .exe
            fileFilter = FileNameExtensionFilter(
                "Archivos ejecutables (*.exe)",
                "exe"
            )

            isAcceptAllFileFilterUsed = false // No mostrar "Todos los archivos"
            isMultiSelectionEnabled = false    // Solo un archivo a la vez
        }

        // Mostrar el diálogo NATIVO de Windows
        val result = fileChooser.showOpenDialog(null)

        if (result == JFileChooser.APPROVE_OPTION) {
            val selectedFile = fileChooser.selectedFile

            // Validar que sea un archivo .exe válido
            if (selectedFile.exists() && selectedFile.name.endsWith(".exe", ignoreCase = true)) {

                // Extraer nombre automáticamente del archivo
                val appName = selectedFile.nameWithoutExtension
                    .replace("-", " ")
                    .replace("_", " ")
                    .split(" ")
                    .joinToString(" ") { word ->
                        word.replaceFirstChar { it.uppercase() }
                    }

                println("📂 Archivo seleccionado: ${selectedFile.name}")
                println("🎯 Extrayendo icono de alta calidad...")

                // Extraer icono de alta calidad (128x128 con escalado inteligente)
                val iconBytes = IconExtractor.extractIconAsBytes(selectedFile.absolutePath, size = 128)

                if (iconBytes != null) {
                    println("✅ Icono extraído correctamente (${iconBytes.size} bytes)")
                } else {
                    println("⚠️ No se pudo extraer el icono, se usará fallback")
                }

                // Crear el AppInfo con toda la información
                AppInfo(
                    name = appName,
                    path = selectedFile.absolutePath,
                    icon = "🎮", // Emoji fallback
                    isCustom = true,
                    description = "Añadida manualmente",
                    iconBytes = iconBytes
                )
            } else {
                println("❌ El archivo seleccionado no es válido")
                null
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

// El resto de componentes (AppListHeader, AppListItem, AppIcon, SearchBar, EmptyView, LoadingView) permanecen sin cambios.

@Composable
fun AppListHeader() {
    // ... (Tu código sin cambios)
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
            // Columna del icono (más ancha para iconos grandes)
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

            // Columna del nombre
            Box(modifier = Modifier.weight(0.3f).padding(start = 12.dp)) {
                Text(
                    "Nombre",
                    style = MaterialTheme.typography.labelLarge,
                    color = AppColors.OnSurface.copy(alpha = 0.7f)
                )
            }

            // Columna de la ruta
            Box(modifier = Modifier.weight(0.5f).padding(start = 12.dp)) {
                Text(
                    "Ruta de instalación",
                    style = MaterialTheme.typography.labelLarge,
                    color = AppColors.OnSurface.copy(alpha = 0.7f)
                )
            }

            // Columna de acciones
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
            // Icono de alta calidad (128x128 escalado a 80dp)
            Box(
                modifier = Modifier
                    .width(100.dp)
                    .height(80.dp),
                contentAlignment = Alignment.Center
            ) {
                if (app.iconBytes != null) {
                    AppIcon(iconBytes = app.iconBytes, size = 80)
                } else {
                    // Fallback: emoji grande
                    Text(
                        text = app.icon,
                        style = MaterialTheme.typography.displayLarge
                    )
                }
            }

            // Nombre de la aplicación
            Text(
                text = app.name,
                modifier = Modifier.weight(0.3f).padding(start = 12.dp),
                style = MaterialTheme.typography.titleMedium,
                color = AppColors.OnSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            // Ruta de instalación
            Text(
                text = app.path,
                modifier = Modifier.weight(0.5f).padding(start = 12.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = AppColors.OnSurface.copy(alpha = 0.6f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            // Botones de acción
            Row(
                modifier = Modifier.width(160.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Botón Lanzar
                Button(
                    onClick = onLaunch,
                    colors = ButtonDefaults.buttonColors(AppColors.Primary),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text("▶ Lanzar", style = MaterialTheme.typography.labelMedium)
                }

                // Botón Eliminar (solo para apps custom)
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

/**
 * Componente que renderiza un icono de aplicación de alta calidad.
 */
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
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp)), // quitamos .size()
            contentScale = androidx.compose.ui.layout.ContentScale.None // sin reescalar
        )
    } else {
        // Fallback: icono genérico
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