package com.oscarrial.gamelauncher.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color


object AppColors {
    // Fondo oscuro, similar al de tu captura de pantalla
    val Background = Color(0xFF0F172A)
    // Fondo de las tarjetas de aplicaciones
    val Surface = Color(0xFF1E293B)
    // Fondo de la barra de búsqueda
    val SurfaceVariant = Color(0xFF334155)
    // Color principal/de acento (botón 'Añadir App')
    val Primary = Color(0xFF3B82F6)
    // Color de error (botón 'Eliminar')
    val Error = Color(0xFFEF4444)
    val OnPrimary = Color.White
    // Color de texto principal
    val OnBackground = Color(0xFFF1F5F9)
    val OnSurface = Color(0xFFE2E8F0)
}

val DarkColorScheme = darkColorScheme(
    primary = AppColors.Primary,
    background = AppColors.Background,
    surface = AppColors.Surface,
    onSurface = AppColors.OnSurface,
    surfaceVariant = AppColors.SurfaceVariant, // Añadir SurfaceVariant
    onBackground = AppColors.OnBackground, // Añadir OnBackground
    error = AppColors.Error
)

// Composable de tema (necesario para envolver tu pantalla)
@Composable
fun GameLauncherTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        content = content
    )
}