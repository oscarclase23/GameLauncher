package com.oscarrial.gamelauncher.data

/**
 * Clase de datos que define el modelo de una aplicación/juego.
 */
data class AppInfo(
    val name: String,
    val path: String,
    val icon: String = "🎮",
    val description: String = "",
    val isCustom: Boolean = false
)