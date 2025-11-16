package com.oscarrial.gamelauncher.data

/**
 * Clase de datos que define el modelo de una aplicación/juego.
 */
// Clase principal que almacena toda la información relevante de una aplicación o juego.
data class AppInfo(
    val name: String, // Nombre visible de la aplicación.
    val path: String, // Ruta absoluta del ejecutable o archivo de lanzamiento.
    val icon: String = "❓", // Emoji de icono por defecto (fallback).
    val description: String = "", // Descripción corta de la aplicación.
    val isCustom: Boolean = false, // Indica si la app fue añadida manualmente por el usuario.
    val iconBytes: ByteArray? = null // Datos binarios del icono extraído (usado en Compose).
) {
    // Sobrescribir equals y hashCode porque ByteArray no lo hace automáticamente
    // Implementación personalizada de `equals` para manejar correctamente la comparación de ByteArray.
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as AppInfo

        if (name != other.name) return false
        if (path != other.path) return false
        if (icon != other.icon) return false
        if (description != other.description) return false
        if (isCustom != other.isCustom) return false
        if (iconBytes != null) {
            if (other.iconBytes == null) return false
            if (!iconBytes.contentEquals(other.iconBytes)) return false // Compara el contenido del array.
        } else if (other.iconBytes != null) return false

        return true
    }

    // Implementación personalizada de `hashCode` para incluir correctamente el ByteArray.
    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + path.hashCode()
        result = 31 * result + icon.hashCode()
        result = 31 * result + description.hashCode()
        result = 31 * result + isCustom.hashCode()
        result = 31 * result + (iconBytes?.contentHashCode() ?: 0) // Usa contentHashCode para el array.
        return result
    }
}