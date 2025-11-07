package com.oscarrial.gamelauncher.data

/**
 * Clase de datos que define el modelo de una aplicación/juego.
 */
data class AppInfo(
    val name: String,
    val path: String,
    val icon: String = "🎮", // Emoji por defecto (fallback)
    val description: String = "",
    val isCustom: Boolean = false,
    val iconBytes: ByteArray? = null // Icono real extraído del .exe
) {
    // Sobrescribir equals y hashCode porque ByteArray no lo hace automáticamente
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
            if (!iconBytes.contentEquals(other.iconBytes)) return false
        } else if (other.iconBytes != null) return false

        return true
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + path.hashCode()
        result = 31 * result + icon.hashCode()
        result = 31 * result + description.hashCode()
        result = 31 * result + isCustom.hashCode()
        result = 31 * result + (iconBytes?.contentHashCode() ?: 0)
        return result
    }
}