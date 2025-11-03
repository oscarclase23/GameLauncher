package com.oscarrial.gamelauncher.system

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
 * Clase de utilidad para identificar el SO y el prefijo de lanzamiento.
 */
object PlatformService {

    fun getCurrentOS(): OperatingSystem {
        val osName = System.getProperty("os.name", "generic").lowercase()
        return when {
            osName.contains("win") -> OperatingSystem.Windows
            osName.contains("nix") || osName.contains("nux") || osName.contains("aix") -> OperatingSystem.Linux
            osName.contains("mac") -> OperatingSystem.MacOS
            else -> OperatingSystem.Other
        }
    }

    /**
     * Devuelve el prefijo base del comando de shell.
     * Nota: Este prefijo ya no es necesario para ProcessBuilder en el ViewModel,
     * pero se mantiene aquí para la detección del SO.
     */
    fun getLaunchPrefix(): String {
        return when (getCurrentOS()) {
            OperatingSystem.Windows -> "cmd.exe /c start "
            OperatingSystem.Linux, OperatingSystem.MacOS -> "/bin/bash -c "
            OperatingSystem.Other -> ""
        }
    }
}