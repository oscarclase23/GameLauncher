package com.oscarrial.gamelauncher

/**
 * Define las plataformas de sistema operativo que soporta la aplicación.
 */
enum class OperatingSystem {
    Windows,
    Linux,
    MacOS,
    Other
}

/**
 * Clase de utilidad para identificar y proporcionar información
 * del sistema operativo actual.
 */
object IdentificarOS {

    /**
     * Detecta el sistema operativo actual basándose en la propiedad 'os.name'.
     * @return El enum OperatingSystem correspondiente al SO.
     */
    fun getCurrentOS(): OperatingSystem {
        val osName = System.getProperty("os.name", "generic").lowercase()
        return when {
            // Detección de Windows
            osName.contains("win") -> OperatingSystem.Windows
            // Detección de Linux (incluye Unix/Aix/Solaris)
            osName.contains("nix") || osName.contains("nux") || osName.contains("aix") -> OperatingSystem.Linux
            // Detección de macOS
            osName.contains("mac") -> OperatingSystem.MacOS
            // Otros sistemas
            else -> OperatingSystem.Other
        }
    }

    /**
     * Devuelve el comando de shell adecuado para lanzar una aplicación.
     * Esta es una simplificación inicial.
     * @return El prefijo del comando (ej: "cmd.exe /c start " para Windows).
     */
    fun getLaunchPrefix(): String {
        return when (getCurrentOS()) {
            OperatingSystem.Windows -> "cmd.exe /c start "
            OperatingSystem.Linux, OperatingSystem.MacOS -> "/bin/bash -c "
            OperatingSystem.Other -> ""
        }
    }
}