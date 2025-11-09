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
 * Clase de utilidad para identificar el SO.
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
     * Devuelve el nombre amigable del sistema operativo con versión.
     * Esto será visible en la interfaz de usuario.
     */
    fun getOsNameWithVersion(): String {
        val osNameProperty = System.getProperty("os.name", "Unknown OS")
        val osVersion = System.getProperty("os.version", "")

        return when (getCurrentOS()) {
            OperatingSystem.Windows -> {
                // Intentar obtener un nombre más amigable para Windows 10/11
                val friendlyName = when {
                    osNameProperty.contains("11") -> "Windows 11"
                    osNameProperty.contains("10") -> "Windows 10"
                    else -> osNameProperty
                }
                "$friendlyName (v${osVersion})"
            }
            OperatingSystem.MacOS -> "macOS (v${osVersion})"
            OperatingSystem.Linux -> "Linux (${osNameProperty})"
            OperatingSystem.Other -> osNameProperty
        }
    }

    /**
     * Devuelve el prefijo base del comando de shell.
     */
    fun getLaunchPrefix(): String {
        return when (getCurrentOS()) {
            OperatingSystem.Windows -> "cmd.exe /c start "
            OperatingSystem.Linux, OperatingSystem.MacOS -> "/bin/bash -c "
            OperatingSystem.Other -> ""
        }
    }
}