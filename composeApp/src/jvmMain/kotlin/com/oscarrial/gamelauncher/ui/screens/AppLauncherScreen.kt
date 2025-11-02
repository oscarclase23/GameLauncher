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
import com.oscarrial.gamelauncher.ui.theme.AppColors // Necesitas haber creado el archivo de colores

// ----------- MODELO DE DATOS BÁSICO -----------
/**
 * Clase de datos para representar la información de una aplicación/juego.
 * Es el modelo que usaremos, por ahora hardcodeado.
 */
data class AppInfo(
    val name: String,
    val path: String,
    val icon: String = "🎮",
    val description: String = "",
    val isCustom: Boolean = false // Indica si la app fue agregada manualmente
)

// ----------- COMPOSABLE PRINCIPAL DE LA PANTALLA -----------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SimpleAppLauncherScreen() {
    // Estado local para la lista de aplicaciones (inicialmente con datos de prueba)
    var apps by remember { mutableStateOf(sampleApps()) }
    // Estado para la barra de búsqueda
    var searchQuery by remember { mutableStateOf("") }
    // Estado para mostrar el diálogo de añadir app
    var showAddDialog by remember { mutableStateOf(false) }

    // Lógica simple de filtrado por nombre
    val filteredApps = apps.filter { it.name.contains(searchQuery, ignoreCase = true) }

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
                    "🎮 Lanzador de Aplicaciones", // Título de la app
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
                onQueryChange = { searchQuery = it }
            )

            Spacer(Modifier.height(24.dp)) // Aumentado el espacio para la rejilla

            // --- 3. GRID DE APPS (Contenido principal) ---
            if (filteredApps.isEmpty()) {
                EmptyView()
            } else {
                LazyVerticalGrid(
                    // Configuración de la rejilla responsiva (minimo 200dp por columna)
                    columns = GridCells.Adaptive(minSize = 200.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(filteredApps, key = { it.name }) { app ->
                        AppCard(
                            app = app,
                            onClick = {
                                // TO-DO: Aquí iría la llamada a ProcessBuilder
                                println("Lanzar aplicación: ${app.name} (${app.path})")
                            },
                            onRemove = {
                                // Permite remover solo las aplicaciones agregadas manualmente
                                if (app.isCustom)
                                    apps = apps - app
                            }
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
                    // Agrega la nueva app a la lista
                    apps = apps + newApp
                    showAddDialog = false
                }
            )
        }
    }
}

// ----------- COMPONENTES (Para este commit, se mantienen aquí) -----------

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
            // Icono de búsqueda
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
                    // Placeholder
                    if (query.isEmpty()) Text(
                        "Buscar apps (ej: chrome, spotify, code)...",
                        color = AppColors.OnSurface.copy(alpha = 0.5f)
                    )
                    inner()
                }
            )
            // Botón para limpiar la búsqueda
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
    // Animación de escala al pasar el ratón (efecto visual de desktop)
    val scale by animateFloatAsState(if (hovered) 1.03f else 1f, spring(dampingRatio = 0.7f))

    Card(
        modifier = Modifier
            .scale(scale)
            // Hacemos que sea clickeable y detecte el hover
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = AppColors.Surface),
        // Elevación sutilmente mayor al hacer hover
        elevation = CardDefaults.cardElevation(defaultElevation = if (hovered) 8.dp else 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Icono (como emoji hardcodeado)
            Text(app.icon, style = MaterialTheme.typography.displaySmall)
            Spacer(Modifier.height(8.dp))

            // Nombre de la App
            Text(
                app.name,
                color = AppColors.OnSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(Modifier.height(4.dp))

            // Descripción (simula la categoría o un subtítulo)
            Text(
                app.description,
                color = AppColors.OnSurface.copy(alpha = 0.7f),
                maxLines = 1,
                style = MaterialTheme.typography.bodySmall
            )

            // Botón de eliminar solo para apps agregadas manualmente
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
    // Estados para los campos de entrada del diálogo
    var name by remember { mutableStateOf("") }
    var path by remember { mutableStateOf("") }
    var icon by remember { mutableStateOf("🎮") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.width(400.dp), // Ancho fijo para el diálogo
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
                    value = name, onValueChange = { name = it }, label = { Text("Nombre de la App") },
                    colors = OutlinedTextFieldDefaults.colors(focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent)
                )
                OutlinedTextField(
                    value = path, onValueChange = { path = it }, label = { Text("Ruta del ejecutable (C:\\...)") },
                    colors = OutlinedTextFieldDefaults.colors(focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent)
                )
                OutlinedTextField(
                    value = icon, onValueChange = { icon = it }, label = { Text("Emoji o Icono") },
                    colors = OutlinedTextFieldDefaults.colors(focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent)
                )

                Spacer(Modifier.height(8.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    // Botón de cancelar
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) { Text("Cancelar") }

                    // Botón de confirmar
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
            Text("No se encontraron aplicaciones",
                color = AppColors.OnSurface,
                style = MaterialTheme.typography.titleLarge
            )
            Text("Intenta otra búsqueda o agrega una nueva manualmente.",
                color = AppColors.OnSurface.copy(alpha = 0.7f),
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

// ----------- DATOS DE PRUEBA HARDCODEADOS -----------
fun sampleApps() = listOf(
    AppInfo("Minecraft", "C:\\Games\\Minecraft.exe", "⛏️", "Explora mundos infinitos"),
    AppInfo("Visual Studio Code", "C:\\Program Files\\VSCode\\Code.exe", "💻", "Editor de código"),
    AppInfo("Spotify", "C:\\Program Files\\Spotify\\Spotify.exe", "🎵", "Música para todos los gustos")
)