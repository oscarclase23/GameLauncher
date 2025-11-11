package com.oscarrial.gamelauncher.system

import com.oscarrial.gamelauncher.data.AppInfo
import java.io.File

/**
 * Servicio encargado de escanear aplicaciones del sistema.
 * Soporta Windows y Linux (incluyendo Snap y Flatpak).
 */
object AppScanner {

    private val USER_HOME = System.getProperty("user.home")
    private const val DEFAULT_FALLBACK_ICON = "🎮"

    // ==================== RUTAS DE WINDOWS ====================

    private val WINDOWS_PROGRAM_FILES = listOf(
        "C:\\Program Files",
        "C:\\Program Files (x86)",
    )

    private val WINDOWS_APP_DATA_PATHS = listOf(
        "$USER_HOME\\AppData\\Local\\Programs",
        "$USER_HOME\\AppData\\Local",
        "$USER_HOME\\AppData\\Roaming\\Microsoft\\Windows\\Start Menu\\Programs"
    )

    private val WINDOWS_SYSTEM_PATHS = listOf(
        "C:\\Windows\\System32",
        "C:\\Windows"
    )

    private val ALL_WINDOWS_PATHS = WINDOWS_PROGRAM_FILES + WINDOWS_APP_DATA_PATHS

    private val KNOWN_APPS = mapOf(
        "intellij" to listOf("idea64.exe", "idea.exe"),
        "androidstudio" to listOf("studio64.exe", "studio.exe"),
        "visualstudio" to listOf("devenv.exe"),
        "vscode" to listOf("code.exe"),
        "pycharm" to listOf("pycharm64.exe", "pycharm.exe"),
        "chrome" to listOf("chrome.exe"),
        "firefox" to listOf("firefox.exe"),
        "edge" to listOf("msedge.exe"),
        "discord" to listOf("discord.exe"),
        "slack" to listOf("slack.exe"),
        "teams" to listOf("teams.exe"),
        "steam" to listOf("steam.exe"),
        "notepad++" to listOf("notepad++.exe"),
        "7zip" to listOf("7zfm.exe"),
        "winrar" to listOf("winrar.exe"),
        "gimp" to listOf("gimp-2.10.exe", "gimp.exe"),
        "photoshop" to listOf("photoshop.exe"),
        "word" to listOf("winword.exe"),
        "excel" to listOf("excel.exe"),
        "powerpoint" to listOf("powerpnt.exe"),
        "outlook" to listOf("outlook.exe"),
        "notion" to listOf("notion.exe")
    )

    private val SYSTEM_APPS = mapOf(
        "Calculadora" to "calc.exe",
        "Paint" to "mspaint.exe",
        "Notepad" to "notepad.exe",
        "Explorador" to "explorer.exe",
        "CMD" to "cmd.exe",
        "PowerShell" to "powershell.exe"
    )

    private val IGNORED_NAMES = setOf(
        "unins000.exe", "uninstall.exe", "uninst.exe", "setup.exe", "install.exe",
        "updater.exe", "update.exe", "launcher.exe", "helper.exe", "crashhandler.exe",
        "agent.exe", "service.exe", "daemon.exe", "background.exe",
        "autorun.exe", "runtime.exe", "redist.exe", "prerequisite.exe"
    ).map { it.lowercase() }.toSet()

    private val IGNORED_KEYWORDS = setOf(
        "uninstall", "setup", "install", "update", "updater", "crash", "redist",
        "helper", "maintenance", "prerequisite", "launcher", "bootstrapper"
    )

    private val IGNORED_FOLDERS = setOf(
        "common", "commonfiles", "shared", "lib", "libs", "library", "bin32",
        "data", "temp", "cache", "logs", "resources", "assets", "locales",
        "uninstall", "old", "backup", "system", "windows nt", "windowsapps"
    ).map { it.lowercase().replace(" ", "").replace("-", "") }.toSet()

    // ==================== RUTAS DE LINUX ====================

    private val LINUX_APPLICATION_PATHS = listOf(
        // Rutas estándar
        "/usr/share/applications",
        "/usr/local/share/applications",
        "$USER_HOME/.local/share/applications",

        // Snap
        "/var/lib/snapd/desktop/applications",
        // Flatpak (Sistema)
        "/var/lib/flatpak/exports/share/applications",
        // Flatpak (Usuario)
        "$USER_HOME/.local/share/flatpak/exports/share/applications"
    )

    private val ESSENTIAL_LINUX_NAMES = listOf(
        "nautilus", "firefox", "terminal", "settings", "calculator",
        "clock", "calendar", "maps", "photos", "evince",
        "totem", "rhythmbox", "screenshot", "gedit", "monitor",
        "disks", "konsole", "xfce4-terminal", "tilix", "software", "update",
        // ✅ AÑADIDO: Apps de desarrollo conocidas (para evitar filtros)
        "code", "vscode", "intellij", "androidstudio", "pycharm", "clion"
    ).map { it.lowercase() }

    // ==================== FUNCIÓN PRINCIPAL ====================

    fun scanSystemApps(): List<AppInfo> {
        val osName = System.getProperty("os.name", "").lowercase()

        return when {
            osName.contains("win") -> scanWindowsApps()
            osName.contains("nux") || osName.contains("nix") -> scanLinuxApps()
            else -> {
                println("⚠️ Sistema operativo no soportado: $osName")
                emptyList()
            }
        }
    }

    // ==================== WINDOWS SCANNING ====================

    private fun scanWindowsApps(): List<AppInfo> {
        val foundApps = mutableListOf<AppInfo>()

        println("🔍 Iniciando escaneo de aplicaciones de Windows...")

        foundApps.addAll(scanWindowsSystemApps())
        foundApps.addAll(scanKnownApps())

        for (basePath in ALL_WINDOWS_PATHS) {
            val baseDir = File(basePath)
            if (baseDir.exists() && baseDir.isDirectory) {
                baseDir.listFiles()?.forEach { entry ->
                    if (entry.isDirectory && !isIgnoredFolder(entry)) {
                        scanAppFolder(entry, foundApps, basePath in WINDOWS_PROGRAM_FILES)
                    }
                }
            }
        }

        println("✅ Escaneo completado. Se encontraron ${foundApps.size} aplicaciones.")

        return foundApps
            .distinctBy { it.path.lowercase() }
            .groupBy { it.name }
            .mapValues { it.value.first() }
            .values
            .toList()
            .sortedBy { it.name }
    }

    private fun scanWindowsSystemApps(): List<AppInfo> {
        val apps = mutableListOf<AppInfo>()

        for ((name, exeName) in SYSTEM_APPS) {
            for (basePath in WINDOWS_SYSTEM_PATHS) {
                val exeFile = File(basePath, exeName)
                if (exeFile.exists()) {
                    apps.add(createAppInfoFromWindowsFile(exeFile, name))
                    break
                }
            }
        }

        return apps
    }

    private fun scanKnownApps(): List<AppInfo> {
        val apps = mutableListOf<AppInfo>()

        for ((appKey, exeNames) in KNOWN_APPS) {
            val foundApp = findKnownApp(appKey, exeNames)
            if (foundApp != null) {
                apps.add(foundApp)
            }
        }

        return apps
    }

    private fun findKnownApp(appKey: String, exeNames: List<String>): AppInfo? {
        for (basePath in ALL_WINDOWS_PATHS) {
            val baseDir = File(basePath)
            if (!baseDir.exists()) continue

            baseDir.listFiles()?.forEach { folder ->
                if (!folder.isDirectory) return@forEach

                val folderName = folder.name.lowercase().replace(" ", "").replace("-", "")
                if (folderName.contains(appKey) || appKey.contains(folderName)) {
                    val foundExe = searchInFolderRecursive(folder, exeNames, maxDepth = 3)
                    if (foundExe != null) {
                        return createAppInfoFromWindowsFile(foundExe, folder.name)
                    }
                }
            }
        }
        return null
    }

    private fun searchInFolderRecursive(
        folder: File,
        exeNames: List<String>,
        maxDepth: Int,
        currentDepth: Int = 0
    ): File? {
        if (currentDepth > maxDepth) return null

        folder.listFiles()?.forEach { file ->
            if (file.isFile && exeNames.any { it.equals(file.name, ignoreCase = true) }) {
                return file
            }
            if (file.isDirectory && currentDepth < maxDepth) {
                val found = searchInFolderRecursive(file, exeNames, maxDepth, currentDepth + 1)
                if (found != null) return found
            }
        }
        return null
    }

    private fun isIgnoredFolder(folder: File): Boolean {
        val folderName = folder.name.lowercase().replace(" ", "").replace("-", "")

        if (folderName.length <= 2) return true
        if (IGNORED_FOLDERS.any { folderName.contains(it) }) return true
        if (folderName.startsWith("microsoft") || folderName.startsWith("windows")) return true
        if (folderName.contains("uninstall") || folderName.contains("setup")) return true

        return false
    }

    private fun scanAppFolder(
        appFolder: File,
        foundApps: MutableList<AppInfo>,
        isProgramFiles: Boolean
    ) {
        val appFolderName = appFolder.name.lowercase().replace(" ", "").replace("-", "")

        val searchDirs = mutableListOf(appFolder)
        appFolder.listFiles()?.filter { it.isDirectory }?.forEach { dir ->
            val dirName = dir.name.lowercase()
            when {
                dirName in setOf("bin", "app", "application") -> searchDirs.add(dir)
                dirName.startsWith("app-") && !isProgramFiles -> searchDirs.add(dir)
                dirName.matches(Regex("\\d+\\.\\d+.*")) -> searchDirs.add(dir)
            }
        }

        val candidates = searchDirs.flatMap { dir ->
            dir.listFiles()?.filter {
                it.isFile &&
                        it.name.endsWith(".exe", true) &&
                        !isIgnoredExecutable(it)
            } ?: emptyList()
        }

        val mainExe = candidates.minByOrNull { file ->
            val exeName = file.nameWithoutExtension.lowercase().replace(" ", "").replace("-", "")

            val matchScore = when {
                exeName == appFolderName -> 0
                exeName.contains(appFolderName) || appFolderName.contains(exeName) -> 1
                else -> 10
            }

            val lengthScore = file.name.length / 10

            matchScore + lengthScore
        }

        mainExe?.let { file ->
            foundApps.add(createAppInfoFromWindowsFile(file, appFolder.name))
        }
    }

    private fun isIgnoredExecutable(file: File): Boolean {
        val fileName = file.name.lowercase()

        if (IGNORED_NAMES.contains(fileName)) return true

        val nameWithoutExt = file.nameWithoutExtension.lowercase()
        if (IGNORED_KEYWORDS.any { nameWithoutExt.contains(it) }) return true

        if (file.name.length > 50) return true

        return false
    }

    private fun createAppInfoFromWindowsFile(file: File, folderName: String? = null): AppInfo {
        val name = (folderName ?: file.nameWithoutExtension)
            .replace("-", " ")
            .replace("_", " ")
            .split(" ")
            .joinToString(" ") { it.capitalize() }

        println("  🎨 Extrayendo icono de: ${file.name}")
        val iconBytes = try {
            IconExtractor.extractIconAsBytes(file.absolutePath, size = 64)
        } catch (e: Exception) {
            println("  ⚠️ Error extrayendo icono de ${file.name}: ${e.message}")
            null
        }

        if (iconBytes != null) {
            println("  ✅ Icono extraído correctamente (${iconBytes.size} bytes)")
        } else {
            println("  ⚠️ No se pudo extraer el icono, usando fallback")
        }

        return AppInfo(
            name = name,
            path = file.absolutePath,
            icon = DEFAULT_FALLBACK_ICON,
            description = "Aplicación de Windows",
            iconBytes = iconBytes
        )
    }

    // ==================== LINUX SCANNING ====================

    private fun scanLinuxApps(): List<AppInfo> {
        val foundApps = mutableListOf<AppInfo>()

        println("🔍 Iniciando escaneo de aplicaciones de Linux (incluyendo Snap/Flatpak)...")

        for (appPath in LINUX_APPLICATION_PATHS) {
            val appDir = File(appPath)
            if (!appDir.exists() || !appDir.isDirectory) {
                println("  ⏭️ Ruta no encontrada: $appPath")
                continue
            }

            println("📂 Escaneando: $appPath")

            appDir.listFiles()?.filter { it.name.endsWith(".desktop") }?.forEach { desktopFile ->
                try {
                    val appInfo = parseDesktopFile(desktopFile)
                    if (appInfo != null) {
                        foundApps.add(appInfo)
                    }
                } catch (e: Exception) {
                    println("  ⚠️ Error procesando ${desktopFile.name}: ${e.message}")
                }
            }
        }

        println("✅ Escaneo completado. Se encontraron ${foundApps.size} aplicaciones.")

        return foundApps
            .distinctBy { it.path.lowercase() }
            .sortedBy { it.name }
    }

    private fun parseDesktopFile(desktopFile: File): AppInfo? {
        var name: String? = null
        var exec: String? = null
        var iconName: String? = null
        var comment: String? = null
        var isHidden = false
        var cleanExec: String? = null

        desktopFile.readLines().forEach { line ->
            val trimmedLine = line.trim()
            when {
                trimmedLine.startsWith("[Desktop Entry]") -> {}
                trimmedLine.startsWith("Name=") && name == null -> name = trimmedLine.substringAfter("Name=")
                trimmedLine.startsWith("Exec=") -> {
                    exec = trimmedLine.substringAfter("Exec=")
                    cleanExec = exec!!
                        .replace(Regex("%[a-zA-Z]"), "")
                        .trim()
                        .split(" ")
                        .firstOrNull()
                }
                trimmedLine.startsWith("Icon=") -> iconName = trimmedLine.substringAfter("Icon=")
                trimmedLine.startsWith("Comment=") -> comment = trimmedLine.substringAfter("Comment=")
                trimmedLine.startsWith("NoDisplay=") && trimmedLine.substringAfter("NoDisplay=").equals("true", ignoreCase = true) -> isHidden = true
                trimmedLine.startsWith("Hidden=") && trimmedLine.substringAfter("Hidden=").equals("true", ignoreCase = true) -> isHidden = true
            }
        }

        if (name.isNullOrBlank() || cleanExec.isNullOrBlank() || isHidden) {
            return null
        }

        val normalizedExec = cleanExec!!.lowercase()
        val normalizedName = name!!.lowercase().replace(" ", "").replace("-", "")

        // 🔍 Evitar subprocesos irrelevantes (daemons, helpers, etc.)
        val isNotMainApp = normalizedExec.contains("handler") ||
                normalizedExec.contains("agent") ||
                normalizedExec.contains("daemon") ||
                normalizedExec.contains("crash")

        if (isNotMainApp && ESSENTIAL_LINUX_NAMES.none { normalizedExec.contains(it) || normalizedName.contains(it) }) {
            return null
        }

        val finalPath = resolveExecutablePath(cleanExec!!)


        if (finalPath.isNullOrBlank()) {
            println("  ⚠️ No se pudo resolver el ejecutable de $name (exec=$cleanExec)")
            return null
        }

        // 🎨 Cargar icono (mejorado)
        val iconBytes = iconName?.let { IconExtractor.extractLinuxIcon(it) }

        if (iconBytes == null) {
            println("  ⚠️ Sin icono, usando fallback para: $name")
        } else {
            println("  ✅ Aplicación detectada: $name")
        }

        return AppInfo(
            name = name!!,
            path = finalPath,
            icon = DEFAULT_FALLBACK_ICON,
            description = comment ?: "Aplicación de Linux",
            iconBytes = iconBytes
        )
    }


    private fun resolveExecutablePath(exec: String): String? {
        val file = File(exec)
        if (file.exists()) return file.absolutePath

        // 🔍 Si no empieza por /, buscamos en el PATH del sistema
        val pathDirs = System.getenv("PATH")?.split(":") ?: emptyList()
        for (dir in pathDirs) {
            val candidate = File(dir, exec)
            if (candidate.exists()) return candidate.absolutePath
        }

        // ⚙️ Si es un comando flatpak o snap, mantenemos el texto tal cual
        return when {
            exec.startsWith("flatpak") || exec.startsWith("snap") -> exec
            else -> null
        }
    }

    private fun String.capitalize(): String {
        return replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
    }
}