package com.oscarrial.gamelauncher.system

/**
 * Define las plataformas de sistema operativo soportadas.
 */
enum class OperatingSystem {
    Windows,
    Linux,
    MacOS,
    Other
}

/**
 * Utilidad para identificar el sistema operativo.
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
     */
    fun getOsNameWithVersion(): String {
        val osNameProperty = System.getProperty("os.name", "Unknown OS")
        val osVersion = System.getProperty("os.version", "")

        return when (getCurrentOS()) {
            OperatingSystem.Windows -> {
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
}