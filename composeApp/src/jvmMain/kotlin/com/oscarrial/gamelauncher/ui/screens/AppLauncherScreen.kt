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
import androidx.compose.ui.window.Dialog
import com.oscarrial.gamelauncher.ui.theme.AppColors
import com.oscarrial.gamelauncher.viewmodel.LauncherViewModel
import com.oscarrial.gamelauncher.data.AppInfo
import com.oscarrial.gamelauncher.system.IconExtractor
import org.jetbrains.skia.Image as SkiaImage

// ----------- COMPOSABLE PRINCIPAL DE LA PANTALLA -----------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppLauncherScreen() {
    val viewModel = remember { LauncherViewModel() }

    val filteredApps = viewModel.filteredApps
    val searchQuery = viewModel.searchQuery
    var showAddDialog by remember { mutableStateOf(false) }
    val isLoading = viewModel.isLoading

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.Background)
            .padding(24.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

            // --- 1. HEADER (Título y botón de Añadir App) ---
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
                    onClick = { showAddDialog = true },
                    colors = ButtonDefaults.buttonColors(AppColors.Primary),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("➕ Añadir App", color = AppColors.OnPrimary)
                }
            }

            Spacer(Modifier.height(20.dp))

            // --- 2. SEARCH BAR ---
            SearchBar(
                query = searchQuery,
                onQueryChange = viewModel::updateSearchQuery
            )

            Spacer(Modifier.height(24.dp))

            // --- 3. LISTA DE APPS ---
            if (isLoading) {
                LoadingView()
            } else if (filteredApps.isEmpty()) {
                EmptyView()
            } else {
                // Header de la tabla
                AppListHeader()

                Spacer(Modifier.height(8.dp))

                // Lista scrolleable de apps
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

        // --- 4. DIÁLOGO PARA AÑADIR APP ---
        if (showAddDialog) {
            AddAppDialog(
                onDismiss = { showAddDialog = false },
                onConfirm = { newApp ->
                    viewModel.addApp(newApp)
                    showAddDialog = false
                }
            )
        }
    }
}

// ----------- HEADER DE LA LISTA -----------
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
            // Columna del icono
            Box(
                modifier = Modifier.width(80.dp),
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
                // Espacio vacío para alinear con los botones
            }
        }
    }
}

// ----------- ITEM DE LA LISTA -----------
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
            // --- 1. ICONO (MÁS GRANDE Y CENTRADO) ---
            Box(
                modifier = Modifier
                    .width(80.dp)
                    .height(64.dp),
                contentAlignment = Alignment.Center
            ) {
                if (app.iconBytes != null) {
                    // Mostrar icono real extraído (64x64)
                    AppIcon(iconBytes = app.iconBytes, size = 64)
                } else {
                    // Fallback al emoji
                    Text(
                        text = app.icon,
                        style = MaterialTheme.typography.displayLarge
                    )
                }
            }

            // --- 2. NOMBRE ---
            Text(
                text = app.name,
                modifier = Modifier.weight(0.3f).padding(start = 12.dp),
                style = MaterialTheme.typography.titleMedium,
                color = AppColors.OnSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            // --- 3. RUTA ---
            Text(
                text = app.path,
                modifier = Modifier.weight(0.5f).padding(start = 12.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = AppColors.OnSurface.copy(alpha = 0.6f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            // --- 4. BOTONES DE ACCIÓN ---
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

// ----------- COMPOSABLE PARA MOSTRAR EL ICONO -----------
@Composable
fun AppIcon(iconBytes: ByteArray, size: Int = 48) {
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
                .size(size.dp)
                .clip(RoundedCornerShape(8.dp))
        )
    } else {
        // Fallback: mostrar un icono genérico
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

// ----------- OTROS COMPONENTES -----------

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
                    if (query.isEmpty()) Text(
                        "Buscar aplicaciones...",
                        color = AppColors.OnSurface.copy(alpha = 0.5f)
                    )
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
fun AddAppDialog(onDismiss: () -> Unit, onConfirm: (AppInfo) -> Unit) {
    var selectedPath by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.width(500.dp),
            shape = RoundedCornerShape(20.dp),
            color = AppColors.Surface,
            tonalElevation = 10.dp
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    "➕ Agregar Nueva Aplicación",
                    style = MaterialTheme.typography.headlineSmall,
                    color = AppColors.OnSurface
                )

                Text(
                    "Selecciona el archivo ejecutable (.exe) de la aplicación",
                    style = MaterialTheme.typography.bodyMedium,
                    color = AppColors.OnSurface.copy(alpha = 0.7f)
                )

                // Campo de ruta con botón de explorar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = selectedPath,
                        onValueChange = {
                            selectedPath = it
                            errorMessage = ""
                        },
                        label = { Text("Ruta del ejecutable") },
                        modifier = Modifier.weight(1f),
                        readOnly = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent
                        ),
                        supportingText = if (errorMessage.isNotEmpty()) {
                            { Text(errorMessage, color = AppColors.Error) }
                        } else null
                    )

                    Button(
                        onClick = {
                            // Abrir selector de archivos
                            val fileChooser = javax.swing.JFileChooser()
                            fileChooser.dialogTitle = "Seleccionar ejecutable"
                            fileChooser.fileSelectionMode = javax.swing.JFileChooser.FILES_ONLY

                            // Filtro para solo mostrar .exe
                            fileChooser.fileFilter = object : javax.swing.filechooser.FileFilter() {
                                override fun accept(f: java.io.File): Boolean {
                                    return f.isDirectory || f.name.endsWith(".exe", ignoreCase = true)
                                }
                                override fun getDescription(): String = "Archivos ejecutables (*.exe)"
                            }

                            val result = fileChooser.showOpenDialog(null)
                            if (result == javax.swing.JFileChooser.APPROVE_OPTION) {
                                selectedPath = fileChooser.selectedFile.absolutePath
                                errorMessage = ""
                            }
                        },
                        colors = ButtonDefaults.buttonColors(AppColors.Primary),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        Text("📁 Explorar")
                    }
                }

                // Preview del icono si hay una ruta seleccionada
                if (selectedPath.isNotEmpty()) {
                    val file = remember(selectedPath) { java.io.File(selectedPath) }

                    if (file.exists() && file.name.endsWith(".exe", ignoreCase = true)) {
                        val iconBytes = remember(selectedPath) {
                            IconExtractor.extractIconAsBytes(selectedPath, size = 64)
                        }

                        val appName = remember(selectedPath) {
                            file.nameWithoutExtension
                                .replace("-", " ")
                                .replace("_", " ")
                                .split(" ")
                                .joinToString(" ") { it.replaceFirstChar { c ->
                                    if (c.isLowerCase()) c.titlecase() else c.toString()
                                }}
                        }

                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            color = AppColors.SurfaceVariant.copy(alpha = 0.3f)
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Icono preview
                                Box(
                                    modifier = Modifier.size(64.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (iconBytes != null) {
                                        AppIcon(iconBytes = iconBytes, size = 64)
                                    } else {
                                        Text("🎮", style = MaterialTheme.typography.displayMedium)
                                    }
                                }

                                Column {
                                    Text(
                                        "Vista previa:",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = AppColors.OnSurface.copy(alpha = 0.6f)
                                    )
                                    Text(
                                        appName,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = AppColors.OnSurface
                                    )
                                }
                            }
                        }
                    } else {
                        errorMessage = "El archivo no existe o no es un ejecutable válido"
                    }
                }

                Spacer(Modifier.height(8.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                        Text("Cancelar")
                    }

                    Button(
                        onClick = {
                            val file = java.io.File(selectedPath)
                            if (file.exists() && file.name.endsWith(".exe", ignoreCase = true)) {
                                // Extraer nombre automáticamente
                                val appName = file.nameWithoutExtension
                                    .replace("-", " ")
                                    .replace("_", " ")
                                    .split(" ")
                                    .joinToString(" ") { it.replaceFirstChar { c ->
                                        if (c.isLowerCase()) c.titlecase() else c.toString()
                                    }}

                                // Extraer icono automáticamente
                                val iconBytes = IconExtractor.extractIconAsBytes(selectedPath, size = 64)

                                onConfirm(
                                    AppInfo(
                                        name = appName,
                                        path = selectedPath,
                                        icon = "🎮", // Fallback
                                        isCustom = true,
                                        description = "Añadida manualmente",
                                        iconBytes = iconBytes
                                    )
                                )
                            } else {
                                errorMessage = "Selecciona un archivo .exe válido"
                            }
                        },
                        modifier = Modifier.weight(1f),
                        enabled = selectedPath.isNotEmpty(),
                        colors = ButtonDefaults.buttonColors(AppColors.Primary)
                    ) {
                        Text("Agregar", color = AppColors.OnPrimary)
                    }
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
                "Escaneando aplicaciones y extrayendo iconos...",
                color = AppColors.OnSurface,
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}