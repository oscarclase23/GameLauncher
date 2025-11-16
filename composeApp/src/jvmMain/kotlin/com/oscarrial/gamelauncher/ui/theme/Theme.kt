package com.oscarrial.gamelauncher.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Paleta de colores personalizada de la app
object AppColors {
    val Background = Color(0xFF0F172A)      // Fondo principal
    val Surface = Color(0xFF1E293B)         // Fondos de tarjetas y contenedores
    val SurfaceVariant = Color(0xFF334155)  // Fondos secundarios (barra de búsqueda, paneles)
    val Primary = Color(0xFF3B82F6)         // Color de acento (botones, elementos activos)
    val Error = Color(0xFFEF4444)           // Color para errores o acciones destructivas
    val OnPrimary = Color.White                     // Texto sobre Primary
    val OnBackground = Color(0xFFF1F5F9)    // Texto sobre Background
    val OnSurface = Color(0xFFE2E8F0)       // Texto sobre Surface
}

// Esquema de colores oscuro usando Material3
val DarkColorScheme = darkColorScheme(
    primary = AppColors.Primary,
    background = AppColors.Background,
    surface = AppColors.Surface,
    onSurface = AppColors.OnSurface,
    surfaceVariant = AppColors.SurfaceVariant,
    onBackground = AppColors.OnBackground,
    error = AppColors.Error
)

// Composable para aplicar el tema a toda la UI
@Composable
fun GameLauncherTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        content = content
    )
}
