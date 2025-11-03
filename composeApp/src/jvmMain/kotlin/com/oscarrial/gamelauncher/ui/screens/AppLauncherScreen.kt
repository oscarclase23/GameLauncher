package com.oscarrial.gamelauncher.ui.screens

import androidx.compose.animation.core.spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.oscarrial.gamelauncher.ui.theme.AppColors
import com.oscarrial.gamelauncher.viewmodel.LauncherViewModel
import com.oscarrial.gamelauncher.data.AppInfo

// ----------- COMPOSABLE PRINCIPAL DE LA PANTALLA -----------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppLauncherScreen() {
    // 1. INICIALIZAR EL VIEWMODEL: Centraliza la lógica y el estado.
    val viewModel = remember { LauncherViewModel() }

    // 2. OBTENER ESTADOS Y LÓGICA DEL VIEWMODEL:
    val filteredApps = viewModel.filteredApps
    val searchQuery = viewModel.searchQuery
    var showAddDialog by remember { mutableStateOf(false) }
    val isLoading = viewModel.isLoading

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.Background)
            .padding(16.dp)
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
                    style = MaterialTheme.typography.headlineSmall,
                    color = AppColors.OnBackground
                )
                Button(
                    onClick = { showAddDialog = true },
                    colors = ButtonDefaults.buttonColors(AppColors.Primary)
                ) {
                    Text("➕ Añadir App", color = AppColors.OnPrimary)
                }
            }

            Spacer(Modifier.height(16.dp))

            // --- 2. SEARCH BAR ---
            SearchBar(
                query = searchQuery,
                onQueryChange = viewModel::updateSearchQuery
            )

            Spacer(Modifier.height(24.dp))

            // --- 3. GRID DE APPS (Contenido principal con manejo de carga) ---

            if (isLoading) {
                LoadingView()
            } else if (filteredApps.isEmpty()) {
                EmptyView()
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 280.dp),
                    horizontalArrangement = Arrangement.spacedBy(20.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    // 🔧 FIX: Usar path como key única en lugar de name
                    items(
                        items = filteredApps,
                        key = { it.path } // ✅ Cada path es único
                    ) { app ->
                        AppCard(
                            app = app,
                            onClick = { viewModel.launchApp(app) },
                            onRemove = { viewModel.removeApp(app) }
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

// ----------- COMPONENTES ADICIONALES -----------

@Composable
fun SearchBar(query: String, onQueryChange: (String) -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = AppColors.SurfaceVariant
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("🔍", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.width(8.dp))
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                modifier = Modifier.weight(1f),
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = AppColors.OnSurface),
                singleLine = true,
                cursorBrush = SolidColor(AppColors.Primary),
                decorationBox = { inner ->
                    if (query.isEmpty()) Text(
                        "Buscar apps (ej: chrome, spotify, code)...",
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
fun AppCard(app: AppInfo, onClick: () -> Unit, onRemove: (() -> Unit)? = null) {
    val interactionSource = remember { MutableInteractionSource() }
    val hovered by interactionSource.collectIsHoveredAsState()
    val scale by animateFloatAsState(if (hovered) 1.02f else 1f, spring(dampingRatio = 0.8f))

    Card(
        modifier = Modifier
            .heightIn(min = 200.dp)
            .scale(scale)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = AppColors.Surface),
        elevation = CardDefaults.cardElevation(defaultElevation = if (hovered) 8.dp else 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Icono
            Text(text = app.icon, style = MaterialTheme.typography.displayMedium)
            Spacer(Modifier.height(12.dp))

            // Nombre de la app
            Text(
                text = app.name,
                color = AppColors.OnSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.titleMedium
            )

            // --- ESPACIO EXTRA PARA COMPENSAR LA DESCRIPCIÓN ELIMINADA ---
            Spacer(Modifier.height(16.dp))

            // 💡 RUTA DE INSTALACIÓN (Ahora aparece inmediatamente después del nombre)
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = AppColors.SurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = app.path,
                    color = AppColors.OnSurface.copy(alpha = 0.6f),
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(8.dp)
                )
            }

            // Botón de eliminar (si aplica)
            if (app.isCustom && onRemove != null) {
                Spacer(Modifier.height(12.dp))
                OutlinedButton(
                    onClick = onRemove,
                    border = BorderStroke(1.dp, AppColors.Error),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text("🗑️ Eliminar", color = AppColors.Error, style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

@Composable
fun AddAppDialog(onDismiss: () -> Unit, onConfirm: (AppInfo) -> Unit) {
    var name by remember { mutableStateOf("") }
    var path by remember { mutableStateOf("") }
    var icon by remember { mutableStateOf("🎮") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.width(400.dp),
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

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nombre de la App") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent
                    )
                )
                OutlinedTextField(
                    value = path,
                    onValueChange = { path = it },
                    label = { Text("Ruta del ejecutable (C:\\...)") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent
                    )
                )
                OutlinedTextField(
                    value = icon,
                    onValueChange = { icon = it },
                    label = { Text("Emoji o Icono") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent
                    )
                )

                Spacer(Modifier.height(8.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                        Text("Cancelar")
                    }

                    Button(
                        onClick = {
                            if (name.isNotBlank() && path.isNotBlank())
                                onConfirm(AppInfo(name, path, icon, isCustom = true, description = "Añadida manualmente"))
                        },
                        modifier = Modifier.weight(1f),
                        enabled = name.isNotBlank() && path.isNotBlank(),
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
                "Escaneando aplicaciones...",
                color = AppColors.OnSurface,
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}